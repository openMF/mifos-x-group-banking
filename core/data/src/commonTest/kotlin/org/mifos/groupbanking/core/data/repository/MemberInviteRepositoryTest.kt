/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.data.repository

import kotlinx.coroutines.test.runTest
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.model.CreateInviteRequest
import org.mifos.groupbanking.core.model.MemberRole
import org.mifos.groupbanking.core.network.model.CreateInviteRequestDto
import org.mifos.groupbanking.core.network.model.GeneratedInviteDto
import org.mifos.groupbanking.core.network.model.PendingInviteDto
import org.mifos.groupbanking.core.network.model.RevokeInviteResponseDto
import org.mifos.groupbanking.core.network.service.memberinvite.MemberInviteApi
import kotlin.test.Test
import kotlin.test.assertEquals

private class FakeMemberInviteApi(
    private val createResult: NetworkResult<GeneratedInviteDto, NetworkError>? = null,
    private val listResult: NetworkResult<List<PendingInviteDto>, NetworkError>? = null,
    private val revokeResult: NetworkResult<RevokeInviteResponseDto, NetworkError>? = null,
) : MemberInviteApi {

    var lastCreateGroupId: Long? = null
    var lastCreateRequest: CreateInviteRequestDto? = null
    var lastListGroupId: Long? = null
    var lastRevokeGroupId: Long? = null
    var lastRevokeRowId: Long? = null

    override suspend fun createInvite(
        groupId: Long,
        request: CreateInviteRequestDto,
    ): NetworkResult<GeneratedInviteDto, NetworkError> {
        lastCreateGroupId = groupId
        lastCreateRequest = request
        return createResult ?: error("createResult not stubbed")
    }

    override suspend fun listPendingInvites(groupId: Long): NetworkResult<List<PendingInviteDto>, NetworkError> {
        lastListGroupId = groupId
        return listResult ?: error("listResult not stubbed")
    }

    override suspend fun revokeInvite(
        groupId: Long,
        rowId: Long,
    ): NetworkResult<RevokeInviteResponseDto, NetworkError> {
        lastRevokeGroupId = groupId
        lastRevokeRowId = rowId
        return revokeResult ?: error("revokeResult not stubbed")
    }
}

/**
 * TDD RED-first coverage for [MemberInviteRepository] / [MemberInviteRepositoryImpl]. No try-catch
 * anywhere in the repository under test (Mandatory Rule 4) — every branch is a plain `when` over
 * the fake service's [NetworkResult]. Store5-free submit-mutation branch (same as
 * `MeetingConductRepository` / `LoanApplyRepository`).
 */
class MemberInviteRepositoryTest {

    private val generatedDto = GeneratedInviteDto(
        token = "K7X2P9",
        inviteLink = "https://mifos.app/join?token=K7X2P9&group=42",
        rowId = 501,
    )

    private val pendingDto = PendingInviteDto(
        rowId = 12,
        token = "AB12CD",
        invitedEmailPhone = "+254712345678",
        roleToAssign = "treasurer",
        expiresAt = "2026-07-23",
        acceptedAt = null,
    )

    // ---------- createInvite ----------

    @Test
    fun createInvite_success_mapsWireResponseAndThreadsGroupIdAndRoleIntoRequest() = runTest {
        val api = FakeMemberInviteApi(createResult = NetworkResult.Success(generatedDto))
        val repo = MemberInviteRepositoryImpl(api)

        val result = repo.createInvite(
            CreateInviteRequest(
                groupId = 42,
                invitedEmailPhone = "amina@example.com",
                roleToAssign = MemberRole.SECRETARY,
                expiresAt = "2026-07-30T00:00:00Z",
            ),
        )

        check(result is NetworkResult.Success)
        assertEquals("K7X2P9", result.data.token)
        assertEquals(501L, result.data.rowId)
        assertEquals(42L, api.lastCreateGroupId)
        // MemberRole.SECRETARY -> lowercase wire "secretary" at the mapper boundary.
        assertEquals("secretary", api.lastCreateRequest?.roleToAssign)
        assertEquals("amina@example.com", api.lastCreateRequest?.invitedEmailPhone)
    }

    @Test
    fun createInvite_conflict_returnsErrorForCallerToSurface() = runTest {
        val api = FakeMemberInviteApi(createResult = NetworkResult.Error(NetworkError.UNKNOWN))
        val repo = MemberInviteRepositoryImpl(api)

        val result = repo.createInvite(
            CreateInviteRequest(
                groupId = 42,
                invitedEmailPhone = "amina@example.com",
                roleToAssign = MemberRole.MEMBER,
                expiresAt = "2026-07-30T00:00:00Z",
            ),
        )

        assertEquals(NetworkResult.Error(NetworkError.UNKNOWN), result)
    }

    // ---------- listPendingInvites ----------

    @Test
    fun listPendingInvites_success_mapsEachRowAndParsesWireRole() = runTest {
        val api = FakeMemberInviteApi(listResult = NetworkResult.Success(listOf(pendingDto)))
        val repo = MemberInviteRepositoryImpl(api)

        val result = repo.listPendingInvites(42)

        check(result is NetworkResult.Success)
        assertEquals(1, result.data.size)
        assertEquals(12L, result.data.first().rowId)
        assertEquals(MemberRole.TREASURER, result.data.first().roleToAssign)
        assertEquals(42L, api.lastListGroupId)
    }

    @Test
    fun listPendingInvites_serverError_returnsError() = runTest {
        val api = FakeMemberInviteApi(listResult = NetworkResult.Error(NetworkError.SERVER))
        val repo = MemberInviteRepositoryImpl(api)

        val result = repo.listPendingInvites(42)

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }

    // ---------- revokeInvite ----------

    @Test
    fun revokeInvite_success_returnsUnitAndThreadsPathParams() = runTest {
        val api = FakeMemberInviteApi(revokeResult = NetworkResult.Success(RevokeInviteResponseDto(resourceId = 12)))
        val repo = MemberInviteRepositoryImpl(api)

        val result = repo.revokeInvite(groupId = 42, rowId = 12)

        assertEquals(NetworkResult.Success(Unit), result)
        assertEquals(42L, api.lastRevokeGroupId)
        assertEquals(12L, api.lastRevokeRowId)
    }

    @Test
    fun revokeInvite_notFound_returnsErrorForOptimisticUndo() = runTest {
        val api = FakeMemberInviteApi(revokeResult = NetworkResult.Error(NetworkError.NOT_FOUND))
        val repo = MemberInviteRepositoryImpl(api)

        val result = repo.revokeInvite(groupId = 42, rowId = 99)

        assertEquals(NetworkResult.Error(NetworkError.NOT_FOUND), result)
    }
}
