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
import org.mifos.groupbanking.core.model.GroupRole
import org.mifos.groupbanking.core.network.model.AssociateClientsRequestDto
import org.mifos.groupbanking.core.network.model.AssociateClientsResponseDto
import org.mifos.groupbanking.core.network.model.GroupPreviewDto
import org.mifos.groupbanking.core.network.model.GroupRoleDto
import org.mifos.groupbanking.core.network.model.GroupTypeSlugDto
import org.mifos.groupbanking.core.network.model.InvitationRowDto
import org.mifos.groupbanking.core.network.model.MarkAcceptedChangesDto
import org.mifos.groupbanking.core.network.model.MarkAcceptedRequestDto
import org.mifos.groupbanking.core.network.model.MarkAcceptedResponseDto
import org.mifos.groupbanking.core.network.service.joinwithcode.InvitationApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class FakeInvitationApi(
    private val validateResult: NetworkResult<InvitationRowDto, NetworkError>? = null,
    private val previewResult: NetworkResult<GroupPreviewDto, NetworkError>? = null,
    private val associateResult: NetworkResult<AssociateClientsResponseDto, NetworkError>? = null,
    private val markAcceptedResult: NetworkResult<MarkAcceptedResponseDto, NetworkError>? = null,
) : InvitationApi {

    var lastValidateCode: String? = null
    var lastPreviewGroupId: Long? = null
    var lastAssociateGroupId: Long? = null
    var lastAssociateRequest: AssociateClientsRequestDto? = null
    var lastMarkAcceptedCode: String? = null
    var lastMarkAcceptedRowId: Long? = null
    var markAcceptedCalled = false
    val callOrder = mutableListOf<String>()

    override suspend fun validateInviteToken(code: String): NetworkResult<InvitationRowDto, NetworkError> {
        lastValidateCode = code
        return validateResult ?: error("validateResult not stubbed")
    }

    override suspend fun getGroupPreview(groupId: Long): NetworkResult<GroupPreviewDto, NetworkError> {
        lastPreviewGroupId = groupId
        return previewResult ?: error("previewResult not stubbed")
    }

    override suspend fun associateClientToGroup(
        groupId: Long,
        request: AssociateClientsRequestDto,
    ): NetworkResult<AssociateClientsResponseDto, NetworkError> {
        lastAssociateGroupId = groupId
        lastAssociateRequest = request
        callOrder += "associate"
        return associateResult ?: error("associateResult not stubbed")
    }

    override suspend fun markInvitationAccepted(
        code: String,
        rowId: Long,
        request: MarkAcceptedRequestDto,
    ): NetworkResult<MarkAcceptedResponseDto, NetworkError> {
        lastMarkAcceptedCode = code
        lastMarkAcceptedRowId = rowId
        markAcceptedCalled = true
        callOrder += "markAccepted"
        return markAcceptedResult ?: error("markAcceptedResult not stubbed")
    }
}

/**
 * TDD RED-first coverage for [InvitationRepository] / [InvitationRepositoryImpl]. No try-catch
 * anywhere in the repository under test (Mandatory Rule 4) — every branch below is a plain
 * `when` over the fake service's [NetworkResult]. `business_logic.kind` for join-with-code is a
 * mutation-orchestration flow (`processor`, no read-stream) — Store5-free, same branch as
 * [AuthRepositoryImpl].
 */
class InvitationRepositoryTest {

    private val unusedRowDto = InvitationRowDto(
        token = "ABC123",
        groupId = 5,
        inviterClientId = 10,
        invitedEmailPhone = "amina@example.com",
        roleToAssign = GroupRoleDto.MEMBER,
        expiresAt = "2099-01-01T00:00:00Z",
        acceptedAt = null,
    )

    private val previewDto = GroupPreviewDto(
        groupId = 5,
        groupName = "Sunrise VSLA",
        groupType = GroupTypeSlugDto.VSLA,
        organizerName = "Amina",
        memberCount = 18,
        officeId = 100,
        roleToAssign = GroupRoleDto.MEMBER,
    )

    private val associateResponseDto = AssociateClientsResponseDto(
        resourceId = 200,
        groupId = 5,
        clientIds = listOf(42),
    )

    private val markAcceptedResponseDto = MarkAcceptedResponseDto(
        resourceId = 300,
        changes = MarkAcceptedChangesDto(acceptedAt = "2026-07-22T10:00:00Z"),
    )

    // ---------- validateCode ----------

    @Test
    fun validateCode_success_returnsMappedInvitationWithExpiryAndUsedHelpers() = runTest {
        val api = FakeInvitationApi(validateResult = NetworkResult.Success(unusedRowDto))
        val repo = InvitationRepositoryImpl(api)

        val result = repo.validateCode("ABC123")

        check(result is NetworkResult.Success)
        assertEquals(5L, result.data.groupId)
        assertFalse(result.data.isAlreadyUsed)
        assertFalse(result.data.isExpired())
        assertEquals("ABC123", api.lastValidateCode)
    }

    @Test
    fun validateCode_notFound_returnsNotFoundErrorForCallerToMapToInvalidCode() = runTest {
        val api = FakeInvitationApi(validateResult = NetworkResult.Error(NetworkError.NOT_FOUND))
        val repo = InvitationRepositoryImpl(api)

        val result = repo.validateCode("BOGUS0")

        assertEquals(NetworkResult.Error(NetworkError.NOT_FOUND), result)
    }

    @Test
    fun validateCode_alreadyAcceptedRow_returnsInvitationWithIsAlreadyUsedTrue() = runTest {
        val usedRow = unusedRowDto.copy(acceptedAt = "2026-07-01T00:00:00Z")
        val api = FakeInvitationApi(validateResult = NetworkResult.Success(usedRow))
        val repo = InvitationRepositoryImpl(api)

        val result = repo.validateCode("USED01")

        check(result is NetworkResult.Success)
        assertTrue(result.data.isAlreadyUsed)
    }

    @Test
    fun validateCode_expiredRow_returnsInvitationWithIsExpiredTrue() = runTest {
        val expiredRow = unusedRowDto.copy(expiresAt = "2020-01-01T00:00:00Z")
        val api = FakeInvitationApi(validateResult = NetworkResult.Success(expiredRow))
        val repo = InvitationRepositoryImpl(api)

        val result = repo.validateCode("EXP0001")

        check(result is NetworkResult.Success)
        assertTrue(result.data.isExpired())
    }

    // ---------- fetchGroupPreview ----------

    @Test
    fun fetchGroupPreview_success_returnsMappedPreview() = runTest {
        val api = FakeInvitationApi(previewResult = NetworkResult.Success(previewDto))
        val repo = InvitationRepositoryImpl(api)

        val result = repo.fetchGroupPreview(5)

        check(result is NetworkResult.Success)
        assertEquals("Sunrise VSLA", result.data.groupName)
        assertEquals(5L, api.lastPreviewGroupId)
    }

    @Test
    fun fetchGroupPreview_notFound_returnsError() = runTest {
        val api = FakeInvitationApi(previewResult = NetworkResult.Error(NetworkError.NOT_FOUND))
        val repo = InvitationRepositoryImpl(api)

        val result = repo.fetchGroupPreview(999)

        assertEquals(NetworkResult.Error(NetworkError.NOT_FOUND), result)
    }

    @Test
    fun fetchGroupPreview_serverError_returnsError() = runTest {
        val api = FakeInvitationApi(previewResult = NetworkResult.Error(NetworkError.SERVER))
        val repo = InvitationRepositoryImpl(api)

        val result = repo.fetchGroupPreview(5)

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }

    // ---------- joinGroup (associate -> markAccepted orchestration) ----------

    @Test
    fun joinGroup_success_associatesThenMarksAcceptedInOrderAndReturnsJoinResult() = runTest {
        val api = FakeInvitationApi(
            associateResult = NetworkResult.Success(associateResponseDto),
            markAcceptedResult = NetworkResult.Success(markAcceptedResponseDto),
        )
        val repo = InvitationRepositoryImpl(api)

        val result = repo.joinGroup(
            groupId = 5,
            clientId = 42,
            role = GroupRole.MEMBER,
            code = "ABC123",
            rowId = 77,
        )

        check(result is NetworkResult.Success)
        assertEquals(200L, result.data.resourceId)
        assertEquals(listOf("associate", "markAccepted"), api.callOrder)
        assertTrue(api.markAcceptedCalled)
        assertEquals("ABC123", api.lastMarkAcceptedCode)
        assertEquals(77L, api.lastMarkAcceptedRowId)
    }

    @Test
    fun joinGroup_threadsClientIdAndRoleIntoAssociateRequest() = runTest {
        val api = FakeInvitationApi(
            associateResult = NetworkResult.Success(associateResponseDto),
            markAcceptedResult = NetworkResult.Success(markAcceptedResponseDto),
        )
        val repo = InvitationRepositoryImpl(api)

        repo.joinGroup(groupId = 5, clientId = 42, role = GroupRole.MEMBER, code = "ABC123", rowId = 77)

        assertEquals(5L, api.lastAssociateGroupId)
        assertEquals(listOf(42L), api.lastAssociateRequest?.clientIds)
        assertEquals(GroupRoleDto.MEMBER, api.lastAssociateRequest?.roleToAssign)
    }

    @Test
    fun joinGroup_associateFails_shortCircuitsAndNeverCallsMarkAccepted() = runTest {
        val api = FakeInvitationApi(associateResult = NetworkResult.Error(NetworkError.BAD_REQUEST))
        val repo = InvitationRepositoryImpl(api)

        val result = repo.joinGroup(groupId = 5, clientId = 42, role = GroupRole.MEMBER, code = "ABC123", rowId = 77)

        assertEquals(NetworkResult.Error(NetworkError.BAD_REQUEST), result)
        assertFalse(api.markAcceptedCalled)
        assertEquals(listOf("associate"), api.callOrder)
    }

    @Test
    fun joinGroup_markAcceptedFails_isNonFatalAndStillReturnsSuccessJoinResult() = runTest {
        val api = FakeInvitationApi(
            associateResult = NetworkResult.Success(associateResponseDto),
            markAcceptedResult = NetworkResult.Error(NetworkError.SERVER),
        )
        val repo = InvitationRepositoryImpl(api)

        val result = repo.joinGroup(groupId = 5, clientId = 42, role = GroupRole.MEMBER, code = "ABC123", rowId = 77)

        check(result is NetworkResult.Success)
        assertEquals(200L, result.data.resourceId)
        assertTrue(api.markAcceptedCalled)
    }
}
