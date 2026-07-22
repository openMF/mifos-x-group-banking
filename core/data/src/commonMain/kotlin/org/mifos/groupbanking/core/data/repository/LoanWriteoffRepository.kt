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

import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.model.WriteoffResult

/**
 * `write_off_loan` mutation-orchestration repository for the loan-mark-defaulted-dialog feature.
 * Wraps `LoanWriteoffApi` (core/network). This mutation is IRREVERSIBLE (a Fineract write-off
 * transaction cannot be undone) and has NO offline queue — a failed or offline submission
 * surfaces its [NetworkResult.Error] immediately for the caller to render, never silently
 * retried/queued for later replay.
 *
 * **Store5 branch (SP-04):** `loan-mark-defaulted-dialog`'s `business_logic.kind` is a
 * single-shot mutation (`processor` — `POST /loans/{loanId}/transactions?command=writeoff`, no
 * GET/read declared by this feature's own `api.yaml`); there is no read-stream for this
 * repository to back with a Store5 cache of its own. Per RULE-IMPLEMENT-STORE5-001 /
 * RULE-IDEA-IMPL-INTELLIGENCE-001 this repository surfaces [NetworkResult] directly rather than
 * `.asScreenStream()` / `.asPagingScreenStream()`, same branch as
 * [org.mifos.groupbanking.core.data.repository.LoanRepaymentRepositoryImpl] /
 * [org.mifos.groupbanking.core.data.repository.InvitationRepositoryImpl].
 *
 * It DOES, however, hold a write-through relationship to a store it does not own: on a
 * successful write-off it invalidates the already-registered `AppStoreRegistry.LoanDetail`
 * composite store for the defaulted loan (`loanDetailStore.clear(loanId)`) so the still-active
 * `LoanDetailRepository.loanDetailStream` re-fetches with the now-defaulted status — the same
 * `store.clear(key)` invalidation shape as
 * [org.mifos.groupbanking.core.data.repository.LoanRepaymentRepositoryImpl.recordRepayment].
 *
 * No try-catch anywhere in this repository (Mandatory Rule 4) — [writeoffLoan] is a plain `when`
 * over the service's sealed [NetworkResult]; `LoanWriteoffApiImpl` is the sole layer allowed to
 * catch exceptions.
 *
 * See API.md#repositories — LoanWriteoffRepository.
 */
interface LoanWriteoffRepository {

    /**
     * Posts an irreversible write-off transaction against [loanId]
     * (`api.yaml#api[mark_loan_defaulted]`). On [NetworkResult.Success] the loan-detail cache for
     * [loanId] is invalidated (see class KDoc) so the loan-detail screen — if open or opened next
     * — reflects the now-defaulted status rather than a stale pre-writeoff snapshot. On
     * [NetworkResult.Error] the loan-detail cache is left untouched and the wire error is
     * returned verbatim for the caller (ViewModel / feature layer) to map to a user-facing
     * message (404 -> "loan not found", 409 -> "loan not in an eligible state for write-off",
     * etc.) — there is no retry/queue path, this write is fire-once.
     */
    suspend fun writeoffLoan(loanId: Long): NetworkResult<WriteoffResult, NetworkError>
}
