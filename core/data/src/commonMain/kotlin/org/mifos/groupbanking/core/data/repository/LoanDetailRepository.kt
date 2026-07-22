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

import kotlinx.coroutines.CoroutineScope
import kpt.core.base.store.screen.FetchPolicy
import kpt.core.base.store.screen.ScreenDataStream
import org.mifos.groupbanking.core.model.LoanDetailResponse

/**
 * Read surface for the loan-detail composite view (`GET /loans/{loanId}`) backing the loan-detail
 * screen.
 *
 * Wraps the single-key NETWORK_WITH_CACHE `LoanDetailStore` and exposes exactly one read path —
 * [loanDetailStream], an offline-first [ScreenDataStream] of `LoanDetailResponse` (loan header +
 * repayment schedule + transaction history) keyed by the loanId. This is a NEW repository —
 * deliberately separate from the paginated `LoanRepository` (loan-list) so the single-key composite
 * read never overloads the paged list surface. There is no DAO-bypass read and no write path: the
 * loan-detail screen is read-only (RULE-IMPLEMENT-STORE5-001 S5-1 / S5-2). No `try-catch`, no
 * `Result<T>` envelope — the stream surfaces `ScreenState` (Loading / Content / NoNetwork / Error /
 * Empty) directly.
 *
 * See API.md#stores — LoanDetailResponse.
 */
interface LoanDetailRepository {

    /**
     * Offline-first stream of the composite loan detail for [loanId].
     *
     * Each loan is cached independently, so re-opening a loan serves that loan's per-key cache
     * immediately then background-revalidates per the store's stale-while-revalidate policy
     * (`data-flow.yaml#cache_strategy`: `stale_while_revalidate`, `ttl=120`, offline cache). Call
     * [ScreenDataStream.retry] to re-drive a failed fetch; pull-to-refresh maps to a
     * [FetchPolicy.NETWORK_ONLY] re-collection.
     *
     * @param loanId The loan to resolve (the store key + the loan header's own id).
     * @param scope CoroutineScope (typically `viewModelScope`) for the auto-refresh coroutine.
     * @param fetchPolicy Read policy. Defaults to [FetchPolicy.CACHE_FIRST_SWR] —
     *   stale-while-revalidate, matching the declared cache strategy.
     */
    fun loanDetailStream(
        loanId: Long,
        scope: CoroutineScope,
        fetchPolicy: FetchPolicy = FetchPolicy.CACHE_FIRST_SWR,
    ): ScreenDataStream<LoanDetailResponse>
}
