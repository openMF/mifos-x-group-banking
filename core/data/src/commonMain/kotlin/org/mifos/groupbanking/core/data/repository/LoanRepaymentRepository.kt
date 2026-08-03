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

import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.model.RecordRepaymentRequest
import org.mifos.groupbanking.core.model.RepaymentResult

/**
 * `make_repayment` mutation-orchestration repository for the loan-repayment-dialog feature.
 * Wraps `LoanRepaymentApi` (core/network).
 *
 * **Store5 branch (SP-04):** `loan-repayment-dialog`'s `business_logic.kind` is a single-shot
 * mutation (`processor` — `POST /loans/{loanId}/transactions?command=repayment`, no GET/read
 * declared by this feature's own `api.yaml`); there is no read-stream for this repository to back
 * with a Store5 cache of its own. Per RULE-IMPLEMENT-STORE5-001 / RULE-IDEA-IMPL-INTELLIGENCE-001
 * this repository surfaces [NetworkResult] directly rather than `.asScreenStream()` /
 * `.asPagingScreenStream()` — same branch as
 * [org.mifos.groupbanking.core.data.repository.InvitationRepositoryImpl] /
 * [org.mifos.groupbanking.core.data.repository.GroupCreateRepositoryImpl].
 *
 * It DOES, however, hold a write-through relationship to a store it does not own: on a successful
 * repayment it invalidates the already-registered `AppStoreRegistry.LoanDetail` composite store
 * for the paid loan (`loanDetailStore.clear(loanId)`) so the still-active
 * `LoanDetailRepository.loanDetailStream` re-fetches with the new balance/outstanding figures —
 * the same `store.clear(key)` invalidation shape as
 * [org.mifos.groupbanking.core.data.repository.MemberProfileRepositoryImpl.updateMemberRole].
 *
 * No try-catch anywhere in this repository (Mandatory Rule 4) — [recordRepayment] is a plain
 * `when` over the service's sealed [NetworkResult]; `LoanRepaymentApiImpl` is the sole layer
 * allowed to catch exceptions.
 *
 * See API.md#repositories — LoanRepaymentRepository.
 */
interface LoanRepaymentRepository {

    /**
     * Records a treasurer-entered repayment of [request] against [loanId]
     * (`api.yaml#api[make_repayment]`). On [NetworkResult.Success] the loan-detail cache for
     * [loanId] is invalidated (see class KDoc) so the loan-detail screen — if open or opened next
     * — reflects the new outstanding balance rather than a stale pre-repayment snapshot. On
     * [NetworkResult.Error] the loan-detail cache is left untouched and the wire error is
     * returned verbatim for the caller (ViewModel / feature layer) to map to a user-facing
     * message (400 -> "amount exceeds outstanding", 404 -> "loan not found", etc.).
     */
    suspend fun recordRepayment(
        loanId: Long,
        request: RecordRepaymentRequest,
    ): NetworkResult<RepaymentResult, NetworkError>
}
