/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.data.repository

import kotlinx.coroutines.test.runTest
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.model.CreateMemberRequest
import org.mifos.groupbanking.core.model.MemberRole
import org.mifos.groupbanking.core.network.model.CreateMemberRequestDto
import org.mifos.groupbanking.core.network.model.CreateMemberResponseDto
import org.mifos.groupbanking.core.network.model.UpdateMemberRoleRequestDto
import org.mifos.groupbanking.core.network.model.UpdateMemberRoleResponseDto
import org.mifos.groupbanking.core.network.model.UploadMemberPhotoResponseDto
import org.mifos.groupbanking.core.network.service.memberadd.MemberAddApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeMemberAddApi(
    private val createClientResult: NetworkResult<CreateMemberResponseDto, NetworkError>? = null,
    private val assignRoleResult: NetworkResult<UpdateMemberRoleResponseDto, NetworkError>? = null,
    private val uploadPhotoResult: NetworkResult<UploadMemberPhotoResponseDto, NetworkError>? = null,
) : MemberAddApi {

    var lastCreateClientRequest: CreateMemberRequestDto? = null
    var lastAssignRoleClientId: String? = null
    var lastAssignRoleRequest: UpdateMemberRoleRequestDto? = null
    var lastUploadClientId: String? = null
    var lastUploadPhotoBytes: ByteArray? = null
    var lastUploadFileName: String? = null
    var uploadPhotoCalled = false
    val callOrder = mutableListOf<String>()

    override suspend fun createClient(
        request: CreateMemberRequestDto,
    ): NetworkResult<CreateMemberResponseDto, NetworkError> {
        lastCreateClientRequest = request
        callOrder += "createClient"
        return createClientResult ?: error("createClientResult not stubbed")
    }

    override suspend fun assignMemberRole(
        clientId: String,
        request: UpdateMemberRoleRequestDto,
    ): NetworkResult<UpdateMemberRoleResponseDto, NetworkError> {
        lastAssignRoleClientId = clientId
        lastAssignRoleRequest = request
        callOrder += "assignMemberRole"
        return assignRoleResult ?: error("assignRoleResult not stubbed")
    }

    override suspend fun uploadMemberPhoto(
        clientId: String,
        photoBytes: ByteArray,
        fileName: String,
    ): NetworkResult<UploadMemberPhotoResponseDto, NetworkError> {
        lastUploadClientId = clientId
        lastUploadPhotoBytes = photoBytes
        lastUploadFileName = fileName
        uploadPhotoCalled = true
        callOrder += "uploadMemberPhoto"
        return uploadPhotoResult ?: error("uploadPhotoResult not stubbed")
    }
}

/**
 * TDD RED-first coverage for [MemberAddRepository] / [MemberAddRepositoryImpl] — the member-add
 * create-chain orchestration (`createClient` -> `assignMemberRole` -> optional
 * `uploadMemberPhoto`, photo failure non-fatal). No try-catch anywhere in the repository under
 * test (Mandatory Rule 4) — every branch below is a plain `when` over the fake service's
 * [NetworkResult]. `business_logic.kind` for member-add is a mutation-orchestration flow
 * (offline-queue-backed create-chain, no read-stream) — Store5-free, same branch as
 * [InvitationRepositoryImpl] / [GroupCreateRepositoryImpl].
 */
class MemberAddRepositoryTest {

    private val request = CreateMemberRequest(
        firstName = "Amina",
        lastName = "Nabirye",
        phone = "+256700000001",
        photoUri = "file:///storage/emulated/0/DCIM/member-photo.jpg",
        role = MemberRole.TREASURER,
        groupId = "5",
        officeId = 1,
        activationDate = "22 July 2026",
    )

    private val createResponseDto = CreateMemberResponseDto(resourceId = 42, clientId = 42)
    private val assignRoleResponseDto = UpdateMemberRoleResponseDto(resourceId = 99)
    private val uploadPhotoResponseDto = UploadMemberPhotoResponseDto(resourceId = 7)

    // ---------- full chain success ----------

    @Test
    fun createMember_fullChainSuccessWithPhoto_ordersCreateThenAssignThenUploadAndMarksPhotoUploaded() = runTest {
        val api = FakeMemberAddApi(
            createClientResult = NetworkResult.Success(createResponseDto),
            assignRoleResult = NetworkResult.Success(assignRoleResponseDto),
            uploadPhotoResult = NetworkResult.Success(uploadPhotoResponseDto),
        )
        val repo = MemberAddRepositoryImpl(api, NoOpSyncQueueRepository())

        val result = repo.createMember(request, photoBytes = byteArrayOf(1, 2, 3, 4))

        check(result is NetworkResult.Success)
        assertEquals("42", result.data.memberId)
        assertEquals(42L, result.data.fineractClientId)
        assertEquals(MemberRole.TREASURER, result.data.role)
        assertTrue(result.data.photoUploaded)
        assertEquals(listOf("createClient", "assignMemberRole", "uploadMemberPhoto"), api.callOrder)
    }

    @Test
    fun createMember_threadsClientIdIntoAssignRoleAndUploadPhoto() = runTest {
        val api = FakeMemberAddApi(
            createClientResult = NetworkResult.Success(createResponseDto),
            assignRoleResult = NetworkResult.Success(assignRoleResponseDto),
            uploadPhotoResult = NetworkResult.Success(uploadPhotoResponseDto),
        )
        val repo = MemberAddRepositoryImpl(api, NoOpSyncQueueRepository())

        repo.createMember(request, photoBytes = byteArrayOf(1, 2, 3))

        assertEquals("42", api.lastAssignRoleClientId)
        assertEquals("42", api.lastUploadClientId)
        assertEquals("5", api.lastAssignRoleRequest?.groupId?.toString())
    }

    @Test
    fun createMember_noPhotoBytes_skipsUploadStepAndPhotoUploadedIsFalse() = runTest {
        val api = FakeMemberAddApi(
            createClientResult = NetworkResult.Success(createResponseDto),
            assignRoleResult = NetworkResult.Success(assignRoleResponseDto),
        )
        val repo = MemberAddRepositoryImpl(api, NoOpSyncQueueRepository())

        val result = repo.createMember(request, photoBytes = null)

        check(result is NetworkResult.Success)
        assertFalse(result.data.photoUploaded)
        assertFalse(api.uploadPhotoCalled)
        assertEquals(listOf("createClient", "assignMemberRole"), api.callOrder)
    }

    // ---------- create-fail short-circuit ----------

    @Test
    fun createMember_createClientFails_shortCircuitsAndNeverCallsAssignRoleOrUploadPhoto() = runTest {
        val api = FakeMemberAddApi(createClientResult = NetworkResult.Error(NetworkError.BAD_REQUEST))
        val repo = MemberAddRepositoryImpl(api, NoOpSyncQueueRepository())

        val result = repo.createMember(request, photoBytes = byteArrayOf(1, 2, 3))

        assertEquals(NetworkResult.Error(NetworkError.BAD_REQUEST), result)
        assertEquals(listOf("createClient"), api.callOrder)
        assertFalse(api.uploadPhotoCalled)
        assertNull(api.lastAssignRoleClientId)
    }

    // ---------- role-fail ----------

    @Test
    fun createMember_assignRoleFails_returnsErrorAndNeverCallsUploadPhoto() = runTest {
        val api = FakeMemberAddApi(
            createClientResult = NetworkResult.Success(createResponseDto),
            assignRoleResult = NetworkResult.Error(NetworkError.BAD_REQUEST),
        )
        val repo = MemberAddRepositoryImpl(api, NoOpSyncQueueRepository())

        val result = repo.createMember(request, photoBytes = byteArrayOf(1, 2, 3))

        assertEquals(NetworkResult.Error(NetworkError.BAD_REQUEST), result)
        assertEquals(listOf("createClient", "assignMemberRole"), api.callOrder)
        assertFalse(api.uploadPhotoCalled)
    }

    // ---------- photo-fail-nonfatal ----------

    @Test
    fun createMember_uploadPhotoFails_isNonFatalAndStillReturnsSuccessWithPhotoUploadedFalse() = runTest {
        val api = FakeMemberAddApi(
            createClientResult = NetworkResult.Success(createResponseDto),
            assignRoleResult = NetworkResult.Success(assignRoleResponseDto),
            uploadPhotoResult = NetworkResult.Error(NetworkError.SERVER),
        )
        val repo = MemberAddRepositoryImpl(api, NoOpSyncQueueRepository())

        val result = repo.createMember(request, photoBytes = byteArrayOf(1, 2, 3))

        check(result is NetworkResult.Success)
        assertEquals("42", result.data.memberId)
        assertFalse(result.data.photoUploaded)
        assertTrue(api.uploadPhotoCalled)
        assertEquals(listOf("createClient", "assignMemberRole", "uploadMemberPhoto"), api.callOrder)
    }

    // ---------- offline (transport-level failure at the first hop) ----------

    @Test
    fun createMember_offlineTransportFailureAtCreateClient_returnsErrorForCallerToEnqueueSyncQueue() = runTest {
        // No NetworkMonitor / offline-detection lives in this repository (Mandatory Rule 4 —
        // the repository is a plain when-based passthrough, never a try-catch envelope). A
        // caller (ViewModel), informed by NetworkMonitor.isOffline, is responsible for either
        // pre-flight SyncQueueRepository.enqueue(...) BEFORE calling createMember at all, or for
        // enqueueing a retry on this NetworkResult.Error (data-flow.yaml#offline_behavior
        // strategy: queue_for_sync). This test proves the repository surfaces the raw transport
        // error untouched, so that seam can key off it.
        val api = FakeMemberAddApi(createClientResult = NetworkResult.Error(NetworkError.UNKNOWN))
        val repo = MemberAddRepositoryImpl(api, NoOpSyncQueueRepository())

        val result = repo.createMember(request, photoBytes = null)

        assertEquals(NetworkResult.Error(NetworkError.UNKNOWN), result)
        assertEquals(listOf("createClient"), api.callOrder)
    }
}
