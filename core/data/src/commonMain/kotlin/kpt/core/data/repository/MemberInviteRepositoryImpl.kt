/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.data.repository

import co.touchlab.kermit.Logger
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.model.CreateInviteRequest
import kpt.core.model.GeneratedInvite
import kpt.core.model.PendingInvite
import kpt.core.network.mapper.toDomainModel
import kpt.core.network.mapper.toDto
import kpt.core.network.mapper.toJsonPayload
import kpt.core.network.service.memberinvite.MemberInviteApi

private const val TAG = "MemberInviteRepository"
private const val CREATE_INVITE_OPERATION_TYPE = "CREATE_INVITE"

/**
 * See [MemberInviteRepository] KDoc for the Store5-free (submit-mutation) branch rationale. No
 * try-catch here — every method is a plain `when` over [MemberInviteApi]'s [NetworkResult].
 *
 * See API.md#repositories — MemberInviteRepository.
 */
class MemberInviteRepositoryImpl(
    private val api: MemberInviteApi,
    private val syncQueueRepository: SyncQueueRepository,
) : MemberInviteRepository {

    override suspend fun createInvite(request: CreateInviteRequest): NetworkResult<GeneratedInvite, NetworkError> {
        Logger.d(TAG) { "createInvite: groupId=${request.groupId} role=${request.roleToAssign}" }
        return when (val result = api.createInvite(groupId = request.groupId, request = request.toDto())) {
            is NetworkResult.Success -> {
                val invite = result.data.toDomainModel()
                Logger.i(TAG) { "createInvite succeeded rowId=${invite.rowId}" }
                NetworkResult.Success(invite)
            }
            is NetworkResult.Error -> {
                Logger.e(TAG) { "createInvite failed: ${result.error}" }
                result
            }
        }
    }

    override suspend fun listPendingInvites(groupId: Long): NetworkResult<List<PendingInvite>, NetworkError> {
        Logger.d(TAG) { "listPendingInvites: groupId=$groupId" }
        return when (val result = api.listPendingInvites(groupId)) {
            is NetworkResult.Success -> {
                val invites = result.data.map { it.toDomainModel() }
                Logger.i(TAG) { "listPendingInvites succeeded count=${invites.size}" }
                NetworkResult.Success(invites)
            }
            is NetworkResult.Error -> {
                Logger.e(TAG) { "listPendingInvites failed: ${result.error}" }
                result
            }
        }
    }

    override suspend fun revokeInvite(groupId: Long, rowId: Long): NetworkResult<Unit, NetworkError> {
        Logger.d(TAG) { "revokeInvite: groupId=$groupId rowId=$rowId" }
        return when (val result = api.revokeInvite(groupId = groupId, rowId = rowId)) {
            is NetworkResult.Success -> {
                Logger.i(TAG) { "revokeInvite succeeded resourceId=${result.data.resourceId}" }
                NetworkResult.Success(Unit)
            }
            is NetworkResult.Error -> {
                Logger.e(TAG) { "revokeInvite failed: ${result.error}" }
                result
            }
        }
    }

    override suspend fun enqueueOffline(request: CreateInviteRequest): Long {
        val payloadJson = request.toDto().toJsonPayload()
        val targetRoute = "/companion/datatables/invitations/${request.groupId}"
        Logger.i(TAG) { "enqueueOffline: queuing $CREATE_INVITE_OPERATION_TYPE groupId=${request.groupId}" }
        return syncQueueRepository.enqueue(
            operationType = CREATE_INVITE_OPERATION_TYPE,
            targetTable = targetRoute,
            payloadJson = payloadJson,
        )
    }
}
