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

import co.touchlab.kermit.Logger
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.model.GroupPreview
import org.mifos.groupbanking.core.model.GroupRole
import org.mifos.groupbanking.core.model.Invitation
import org.mifos.groupbanking.core.model.JoinGroupResult
import org.mifos.groupbanking.core.network.mapper.toDomainModel
import org.mifos.groupbanking.core.network.mapper.toDto
import org.mifos.groupbanking.core.network.model.AssociateClientsRequestDto
import org.mifos.groupbanking.core.network.model.MarkAcceptedRequestDto
import org.mifos.groupbanking.core.network.service.joinwithcode.InvitationApi

private const val TAG = "InvitationRepository"

/**
 * See [InvitationRepository] KDoc for the Store5-branch rationale (`business_logic.kind:
 * processor`) and the [InvitationRepository.joinGroup] `rowId` contract gap. No try-catch here
 * — every method is a plain `when` over [InvitationApi]'s [NetworkResult].
 *
 * See API.md#repositories — InvitationRepository.
 */
class InvitationRepositoryImpl(
    private val api: InvitationApi,
) : InvitationRepository {

    override suspend fun validateCode(code: String): NetworkResult<Invitation, NetworkError> {
        Logger.d(TAG) { "validateCode: validating invite code" }
        return when (val result = api.validateInviteToken(code)) {
            is NetworkResult.Success -> {
                val invitation = result.data.toDomainModel()
                Logger.i(TAG) {
                    "validateCode succeeded groupId=${invitation.groupId} " +
                        "expired=${invitation.isExpired()} alreadyUsed=${invitation.isAlreadyUsed}"
                }
                NetworkResult.Success(invitation)
            }
            is NetworkResult.Error -> {
                // NOT_FOUND here is the caller's signal for JoinError.InvalidCode.
                Logger.e(TAG) { "validateCode failed: ${result.error}" }
                result
            }
        }
    }

    override suspend fun fetchGroupPreview(groupId: Long): NetworkResult<GroupPreview, NetworkError> {
        Logger.d(TAG) { "fetchGroupPreview: groupId=$groupId" }
        return when (val result = api.getGroupPreview(groupId)) {
            is NetworkResult.Success -> {
                Logger.i(TAG) { "fetchGroupPreview succeeded groupId=$groupId" }
                NetworkResult.Success(result.data.toDomainModel())
            }
            is NetworkResult.Error -> {
                Logger.e(TAG) { "fetchGroupPreview failed: ${result.error}" }
                result
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun joinGroup(
        groupId: Long,
        clientId: Long,
        role: GroupRole,
        code: String,
        rowId: Long,
    ): NetworkResult<JoinGroupResult, NetworkError> {
        Logger.d(TAG) { "joinGroup: associating clientId=$clientId to groupId=$groupId" }
        val associateResult = api.associateClientToGroup(
            groupId = groupId,
            request = AssociateClientsRequestDto(clientIds = listOf(clientId), roleToAssign = role.toDto()),
        )
        return when (associateResult) {
            is NetworkResult.Error -> {
                Logger.e(TAG) { "joinGroup: associate failed, join aborted (mark-accepted skipped): ${associateResult.error}" }
                associateResult
            }
            is NetworkResult.Success -> {
                val joinResult = associateResult.data.toDomainModel()
                Logger.i(TAG) { "joinGroup: associate succeeded resourceId=${joinResult.resourceId}" }

                // Best-effort cleanup — per flow.yaml#on_confirm_join / data-flow.yaml
                // OnConfirmJoin notes, a mark-accepted failure is non-fatal: the join is already
                // complete once association succeeds, so navigation must not be blocked.
                when (
                    val markResult = api.markInvitationAccepted(
                        code = code,
                        rowId = rowId,
                        request = MarkAcceptedRequestDto(acceptedAt = Clock.System.now().toString()),
                    )
                ) {
                    is NetworkResult.Success -> Logger.i(TAG) { "joinGroup: mark-accepted succeeded" }
                    is NetworkResult.Error -> Logger.e(TAG) {
                        "joinGroup: mark-accepted failed (non-fatal, join still succeeds): ${markResult.error}"
                    }
                }

                NetworkResult.Success(joinResult)
            }
        }
    }
}
