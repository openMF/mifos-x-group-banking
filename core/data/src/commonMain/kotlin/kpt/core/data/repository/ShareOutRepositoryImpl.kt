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
import kpt.core.model.RotationPayoutExecuteResult
import kpt.core.model.RotationPayoutRequest
import kpt.core.model.ShareOutExecuteRequest
import kpt.core.model.ShareOutExecuteResult
import kpt.core.model.ShareOutPreview
import kpt.core.network.mapper.toDomainModel
import kpt.core.network.mapper.toDto
import kpt.core.network.mapper.toJsonPayload
import kpt.core.network.service.shareout.ShareOutApi

private const val TAG = "ShareOutRepository"

private const val SHARE_OUT_EXECUTE_OPERATION_TYPE = "SHARE_OUT_EXECUTE"
private const val ROTATION_PAYOUT_EXECUTE_OPERATION_TYPE = "ROTATION_PAYOUT_EXECUTE"

/**
 * See [ShareOutRepository] KDoc for the Store5-branch rationale (`share-out-preview` has no
 * `AppStoreRegistry` entry yet — the preview read + the execute writes both surface [NetworkResult]
 * directly rather than a Store5 stream). No try-catch here — every method is a plain `when` chain
 * over [ShareOutApi]'s [NetworkResult]; the offline enqueue helpers delegate straight to
 * [SyncQueueRepository.enqueue] (local Room, no network, no try-catch needed), exactly as
 * `LoanRequestRepositoryImpl` does.
 *
 * See API.md#repositories — ShareOutRepository.
 */
class ShareOutRepositoryImpl(
    private val api: ShareOutApi,
    private val syncQueueRepository: SyncQueueRepository,
) : ShareOutRepository {

    override suspend fun getShareOutPreview(groupId: String): NetworkResult<ShareOutPreview, NetworkError> {
        Logger.d(TAG) { "getShareOutPreview: groupId=$groupId" }
        return when (val result = api.getShareOutPreview(groupId)) {
            is NetworkResult.Error -> {
                Logger.e(TAG) { "getShareOutPreview: failed for groupId=$groupId: ${result.error}" }
                result
            }
            is NetworkResult.Success -> {
                val preview = result.data.toDomainModel()
                Logger.i(TAG) {
                    "getShareOutPreview: succeeded groupId=$groupId poolModel=${preview.poolModel} " +
                        "formula=${preview.shareoutFormula} pool=${preview.totalPool} " +
                        "payouts=${preview.memberPayouts?.size ?: "n/a"}"
                }
                NetworkResult.Success(preview)
            }
        }
    }

    override suspend fun executeShareOut(
        groupId: String,
        request: ShareOutExecuteRequest,
    ): NetworkResult<ShareOutExecuteResult, NetworkError> {
        Logger.d(TAG) { "executeShareOut: groupId=$groupId cycle=${request.cycleNumber} payouts=${request.memberPayouts.size}" }
        return when (val result = api.executeShareOut(groupId, request.toDto())) {
            is NetworkResult.Error -> {
                Logger.e(TAG) { "executeShareOut: failed groupId=$groupId: ${result.error}" }
                result
            }
            is NetworkResult.Success -> {
                val executed = result.data.toDomainModel()
                Logger.i(TAG) {
                    "executeShareOut: succeeded groupId=$groupId recordId=${executed.shareoutRecordId} " +
                        "succeeded=${executed.succeededCount} failed=${executed.failedCount}"
                }
                NetworkResult.Success(executed)
            }
        }
    }

    override suspend fun executeRotationPayout(
        groupId: String,
        request: RotationPayoutRequest,
    ): NetworkResult<RotationPayoutExecuteResult, NetworkError> {
        Logger.d(TAG) { "executeRotationPayout: groupId=$groupId recipient=${request.recipientMemberId}" }
        return when (val result = api.executeRotationPayout(groupId, request.toDto())) {
            is NetworkResult.Error -> {
                Logger.e(TAG) { "executeRotationPayout: failed groupId=$groupId: ${result.error}" }
                result
            }
            is NetworkResult.Success -> {
                val executed = result.data.toDomainModel()
                Logger.i(TAG) {
                    "executeRotationPayout: succeeded groupId=$groupId recordId=${executed.rotationRecordId} " +
                        "newPosition=${executed.newRotationPosition}"
                }
                NetworkResult.Success(executed)
            }
        }
    }

    override suspend fun enqueueShareOutExecuteOffline(groupId: String, request: ShareOutExecuteRequest): Long {
        val payloadJson = request.toDto().toJsonPayload()
        Logger.i(TAG) { "enqueueShareOutExecuteOffline: queuing $SHARE_OUT_EXECUTE_OPERATION_TYPE groupId=$groupId" }
        return syncQueueRepository.enqueue(
            operationType = SHARE_OUT_EXECUTE_OPERATION_TYPE,
            targetTable = "/companion/groups/$groupId/shareout/execute",
            payloadJson = payloadJson,
        )
    }

    override suspend fun enqueueRotationPayoutOffline(groupId: String, request: RotationPayoutRequest): Long {
        val payloadJson = request.toDto().toJsonPayload()
        Logger.i(TAG) { "enqueueRotationPayoutOffline: queuing $ROTATION_PAYOUT_EXECUTE_OPERATION_TYPE groupId=$groupId" }
        return syncQueueRepository.enqueue(
            operationType = ROTATION_PAYOUT_EXECUTE_OPERATION_TYPE,
            targetTable = "/companion/groups/$groupId/rotation/execute",
            payloadJson = payloadJson,
        )
    }
}
