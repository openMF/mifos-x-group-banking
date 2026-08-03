/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.service.loandetail

import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.network.model.LoanDetailResponseDto

/**
 * Ktor client for the loan-detail feature — the single composite read that resolves the loan
 * header (product, principal, interest, disbursement, outstanding/overdue balances, status,
 * member identity) together with the full repayment schedule and transaction history in one
 * round trip. See `idea-layer/screens/loan-detail/api.yaml#api[get_loan_detail]` (`function:
 * get_loan`) + `data-flow.yaml#cache_strategy` (`stale_while_revalidate`, ttl=120) + API.md#services
 * for the endpoint contract.
 *
 * Returns [NetworkResult] — never a raw [Result] envelope, never a thrown exception (Mandatory
 * Rule 2). `NetworkResult`/`NetworkError` are the framework's `core-base/network` sealed types
 * (consumed, never edited — Hard Rule #8). The downstream `kmp-store-gen` Store5 wrapper and its
 * Repository switch on the sealed result / surface `ScreenState` directly, with no try-catch of
 * their own (Mandatory Rule 4) — this Service is SERVICE-ONLY; no repository/store is emitted
 * here.
 */
interface LoanDetailApi {

    /**
     * `GET /loans/{loanId}?associations=repaymentSchedule,transactions`
     * (`api.yaml#api[get_loan_detail]`). Fetches the loan header for [loanId] plus its embedded
     * `repaymentSchedule` (one row per installment period) and `transactions` (posted repayment
     * history), matching the operation's single-payload response shape
     * (`api.yaml#api[0].response.fields`). [associations] mirrors `api.yaml#params.associations`
     * (`default: "repaymentSchedule,transactions"`) and is always sent — omitting it would return
     * the header alone, which the loan-detail screen never wants. 401 -> [NetworkError.UNAUTHORIZED];
     * 404 -> [NetworkError.NOT_FOUND] ("loan not found"); 500 -> [NetworkError.SERVER].
     */
    suspend fun getLoanDetail(
        loanId: Long,
        associations: String = "repaymentSchedule,transactions",
    ): NetworkResult<LoanDetailResponseDto, NetworkError>
}
