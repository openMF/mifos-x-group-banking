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
import kpt.core.model.LoanDetailResponse
import kpt.core.model.WriteoffLoanRequest
import kpt.core.model.WriteoffResult
import kpt.core.network.mapper.fineractTransactionDate
import kpt.core.network.mapper.toDomainModel
import kpt.core.network.mapper.toDto
import kpt.core.network.service.loanwriteoff.LoanWriteoffApi
import org.mobilenativefoundation.store.store5.Store

private const val TAG = "LoanWriteoffRepository"

/**
 * See [LoanWriteoffRepository] KDoc for the Store5-branch rationale (`business_logic.kind:
 * processor`), the irreversible/no-offline-queue contract, and the loan-detail
 * cache-invalidation contract. No try-catch here — the single write is a plain `when` over
 * [LoanWriteoffApi]'s [NetworkResult].
 *
 * [loanDetailStore] is the SAME `AppStoreRegistry.LoanDetail`-qualified `Store<Long,
 * LoanDetailResponse>` singleton the loan-detail feature's `LoanDetailRepositoryImpl` reads
 * through — this repository never constructs its own store, it only invalidates the shared one on
 * a successful write (S5-1: mutation routed through the store, never a DAO bypass).
 *
 * See API.md#repositories — LoanWriteoffRepository.
 */
class LoanWriteoffRepositoryImpl(
    private val api: LoanWriteoffApi,
    private val loanDetailStore: Store<Long, LoanDetailResponse>,
) : LoanWriteoffRepository {

    override suspend fun writeoffLoan(loanId: Long): NetworkResult<WriteoffResult, NetworkError> {
        Logger.d(TAG) { "writeoffLoan: loanId=$loanId — irreversible, no offline queue" }
        val dto = WriteoffLoanRequest.toDto(transactionDate = fineractTransactionDate())
        return when (val result = api.writeoffLoan(loanId, dto)) {
            is NetworkResult.Success -> {
                // data-flow.yaml#cache.strategy: invalidate — route the mutation through the
                // shared loan-detail store (deletes the in-memory cache + the SoT row for this
                // loan) so the still-active loanDetailStream re-fetches with the now-defaulted
                // status. NOT a silent DAO write (S5-1).
                loanDetailStore.clear(loanId)
                Logger.i(TAG) {
                    "writeoffLoan succeeded resourceId=${result.data.resourceId}; " +
                        "loan-detail cache invalidated for loanId=$loanId"
                }
                NetworkResult.Success(result.data.toDomainModel())
            }
            is NetworkResult.Error -> {
                Logger.e(TAG) { "writeoffLoan failed for loanId=$loanId: ${result.error} — no offline queue, surfaced immediately" }
                result
            }
        }
    }
}
