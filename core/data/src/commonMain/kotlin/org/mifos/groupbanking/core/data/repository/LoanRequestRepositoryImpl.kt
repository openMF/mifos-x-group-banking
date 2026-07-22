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
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.model.LoanRequestPayload
import org.mifos.groupbanking.core.model.LoanRequestResult
import org.mifos.groupbanking.core.network.mapper.toDomainModel
import org.mifos.groupbanking.core.network.mapper.toDto
import org.mifos.groupbanking.core.network.mapper.toJsonPayload
import org.mifos.groupbanking.core.network.service.loanrequest.LoanRequestApi

private const val TAG = "LoanRequestRepository"
private const val LOAN_REQUEST_OPERATION_TYPE = "LOAN_REQUEST"
private const val LOAN_REQUEST_TARGET_TABLE = "dt_loan_request"

/**
 * See [LoanRequestRepository] KDoc for the Store5-branch rationale (`business_logic.kind: crud`)
 * and the offline-queue seam. No try-catch here — [submit] is a plain `when` chain over
 * [LoanRequestApi]'s [NetworkResult]; [enqueueOffline] delegates straight to
 * [SyncQueueRepository.enqueue] (local Room, no network, no try-catch needed).
 *
 * See API.md#repositories — LoanRequestRepository.
 */
class LoanRequestRepositoryImpl(
    private val api: LoanRequestApi,
    private val syncQueueRepository: SyncQueueRepository,
) : LoanRequestRepository {

    override suspend fun submit(payload: LoanRequestPayload): NetworkResult<LoanRequestResult, NetworkError> {
        Logger.d(TAG) { "submit: submitting loan request clientId=${payload.clientId}" }
        return when (val result = api.submitLoanRequest(payload.toDto())) {
            is NetworkResult.Success -> {
                Logger.i(TAG) {
                    "submit: succeeded clientId=${payload.clientId} resourceId=${result.data.resourceId}"
                }
                NetworkResult.Success(result.data.toDomainModel())
            }
            is NetworkResult.Error -> {
                Logger.e(TAG) { "submit: failed clientId=${payload.clientId} error=${result.error}" }
                result
            }
        }
    }

    override suspend fun enqueueOffline(payload: LoanRequestPayload): Long {
        val payloadJson = payload.toDto().toJsonPayload()
        Logger.i(TAG) { "enqueueOffline: queuing $LOAN_REQUEST_OPERATION_TYPE for clientId=${payload.clientId}" }
        return syncQueueRepository.enqueue(
            operationType = LOAN_REQUEST_OPERATION_TYPE,
            targetTable = LOAN_REQUEST_TARGET_TABLE,
            payloadJson = payloadJson,
        )
    }
}
