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

import co.touchlab.kermit.Logger
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.model.LoanDetailResponse
import org.mifos.groupbanking.core.model.RecordRepaymentRequest
import org.mifos.groupbanking.core.model.RepaymentResult
import org.mifos.groupbanking.core.network.mapper.fineractTransactionDate
import org.mifos.groupbanking.core.network.mapper.toDomainModel
import org.mifos.groupbanking.core.network.mapper.toDto
import org.mifos.groupbanking.core.network.service.loanrepayment.LoanRepaymentApi
import org.mobilenativefoundation.store.store5.Store

private const val TAG = "LoanRepaymentRepository"

/**
 * See [LoanRepaymentRepository] KDoc for the Store5-branch rationale (`business_logic.kind:
 * processor`) and the loan-detail cache-invalidation contract. No try-catch here — the single
 * write is a plain `when` over [LoanRepaymentApi]'s [NetworkResult].
 *
 * [loanDetailStore] is the SAME `AppStoreRegistry.LoanDetail`-qualified `Store<Long,
 * LoanDetailResponse>` singleton the loan-detail feature's `LoanDetailRepositoryImpl` reads
 * through — this repository never constructs its own store, it only invalidates the shared one on
 * a successful write (S5-1: mutation routed through the store, never a DAO bypass).
 *
 * See API.md#repositories — LoanRepaymentRepository.
 */
class LoanRepaymentRepositoryImpl(
    private val api: LoanRepaymentApi,
    private val loanDetailStore: Store<Long, LoanDetailResponse>,
) : LoanRepaymentRepository {

    override suspend fun recordRepayment(
        loanId: Long,
        request: RecordRepaymentRequest,
    ): NetworkResult<RepaymentResult, NetworkError> {
        Logger.d(TAG) { "recordRepayment: loanId=$loanId amount=${request.amount}" }
        val dto = request.toDto(transactionDate = fineractTransactionDate())
        return when (val result = api.recordRepayment(loanId, dto)) {
            is NetworkResult.Success -> {
                // data-flow.yaml#cache.strategy: invalidate — route the mutation through the
                // shared loan-detail store (deletes the in-memory cache + the SoT row for this
                // loan) so the still-active loanDetailStream re-fetches with the new
                // balance/outstanding figures. NOT a silent DAO write (S5-1).
                loanDetailStore.clear(loanId)
                Logger.i(TAG) {
                    "recordRepayment succeeded resourceId=${result.data.resourceId}; " +
                        "loan-detail cache invalidated for loanId=$loanId"
                }
                NetworkResult.Success(result.data.toDomainModel())
            }
            is NetworkResult.Error -> {
                Logger.e(TAG) { "recordRepayment failed for loanId=$loanId: ${result.error}" }
                result
            }
        }
    }
}
