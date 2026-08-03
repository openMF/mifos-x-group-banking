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

import kotlinx.coroutines.CoroutineScope
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.base.store.paging.PagingScreenStream
import kpt.core.base.store.screen.FetchPolicy
import org.mifos.groupbanking.core.model.LoanSummary

/**
 * Read surface for the loan-list screen — the offset-paginated list of loan accounts belonging to
 * a savings group (`GET /groups/{groupId}/loans`).
 *
 * Wraps the PAGINATED NETWORK_WITH_CACHE `LoansPagingStore` and exposes exactly one read path —
 * [loansPagingStream], an offline-first [PagingScreenStream] of `LoanSummary` scoped to a group.
 * There is no DAO-bypass read and no write path: the list is read-only (RULE-IMPLEMENT-STORE5-001
 * S5-1 / S5-2). No `try-catch`, no `Result<T>` envelope — the stream surfaces
 * `ScreenState<List<LoanSummary>>` (Loading / Content / NoNetwork / Error / Empty) directly, with
 * load-more + pull-to-refresh built into the returned [PagingScreenStream]. Status-filter chips
 * filter the accumulated list client-side in the ViewModel; they never re-key this stream.
 *
 * See API.md#stores — LoanList.
 */
interface LoanRepository {

    /**
     * Offline-first PAGINATED stream of a group's loan accounts.
     *
     * The returned [PagingScreenStream] exposes:
     * - [PagingScreenStream.state] — `Flow<ScreenState<List<LoanSummary>>>` (the accumulated pages).
     * - [PagingScreenStream.loadNextPage] — load-more on scroll-to-end (`OnLoadNextPage`).
     * - [PagingScreenStream.refresh] / [PagingScreenStream.retry] — pull-to-refresh / error-retry
     *   (`OnRefresh` / `Retry`); resets to page 0 and forces a fresh fetch.
     *
     * Emits cached pages immediately then background-revalidates per the store's
     * stale-while-revalidate policy (`data-flow.yaml`: `stale_while_revalidate`, `ttl=180`,
     * `offline: show_cached`). The [groupId] is threaded into the store key so each group keeps an
     * independent per-page cache.
     *
     * @param groupId The savings group whose loans to page through.
     * @param scope CoroutineScope (typically `viewModelScope`) driving the paging coroutines.
     * @param fetchPolicy Read policy. Defaults to [FetchPolicy.NETWORK_WITH_CACHE] — the paging
     *   analogue of stale-while-revalidate (cache-first page loads, network revalidation on
     *   refresh). Pass [FetchPolicy.CACHE_ONLY] for an explicit offline read.
     */
    fun loansPagingStream(
        groupId: Long,
        scope: CoroutineScope,
        fetchPolicy: FetchPolicy = FetchPolicy.NETWORK_WITH_CACHE,
    ): PagingScreenStream<LoanSummary>

    /**
     * Member-loans read for the personal-loans feature (`GET /clients/{clientId}/loans`) —
     * ADDITIVE to [loansPagingStream], does NOT change its signature. A direct passthrough read
     * (no Store5 wrap, same Store5-free branch as [LoanRepaymentRepository]/[LoanWriteoffRepository]
     * above): calls [org.mifos.groupbanking.core.network.service.loanlist.LoanApi.getClientLoans]
     * and maps the wire [org.mifos.groupbanking.core.network.model.LoanSummaryDto] rows straight to
     * domain [LoanSummary] — a plain `when` over the wire [NetworkResult], no `try-catch`, no
     * `Result<T>` envelope. There is no per-client cache/TTL for this read yet (personal-loans'
     * `data-flow.yaml#cache_strategy` calls for `stale_while_revalidate`/`ttl=180` against a
     * DIFFERENT `/self/loans` endpoint shape — reconciling the two into a single Store5-backed
     * `PersonalLoansStore` is flagged for Station 3, same as the `LoanApi.getClientLoans` KDoc
     * divergence note).
     *
     * @param clientId The member/client whose loans to fetch.
     */
    suspend fun getLoansForClient(clientId: Long): NetworkResult<List<LoanSummary>, NetworkError>
}
