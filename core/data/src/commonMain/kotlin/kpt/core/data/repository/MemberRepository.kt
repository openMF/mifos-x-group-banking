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

import kotlinx.coroutines.CoroutineScope
import kpt.core.base.store.paging.PagingScreenStream
import kpt.core.base.store.screen.FetchPolicy
import kpt.core.model.Member

/**
 * Read surface for the member-list screen — the offset-paginated roster of clients (members)
 * belonging to a savings group (`GET /groups/{groupId}/clients`).
 *
 * Wraps the PAGINATED NETWORK_WITH_CACHE `MembersPagingStore` and exposes exactly one read path —
 * [membersPagingStream], an offline-first [PagingScreenStream] of `Member` scoped to a group. There
 * is no DAO-bypass read and no write path: the list is read-only (RULE-IMPLEMENT-STORE5-001
 * S5-1 / S5-2). No `try-catch`, no `Result<T>` envelope — the stream surfaces
 * `ScreenState<List<Member>>` (Loading / Content / NoNetwork / Error / Empty) directly, with
 * load-more + pull-to-refresh built into the returned [PagingScreenStream].
 *
 * See API.md#stores — MemberList.
 */
interface MemberRepository {

    /**
     * Offline-first PAGINATED stream of a group's members.
     *
     * The returned [PagingScreenStream] exposes:
     * - [PagingScreenStream.state] — `Flow<ScreenState<List<Member>>>` (the accumulated pages).
     * - [PagingScreenStream.loadNextPage] — load-more on scroll-to-end (`OnLoadMoreTap`).
     * - [PagingScreenStream.refresh] / [PagingScreenStream.retry] — pull-to-refresh / error-retry
     *   (`OnRefresh` / `Retry`); resets to page 0 and forces a fresh fetch.
     *
     * Emits cached pages immediately then background-revalidates per the store's
     * stale-while-revalidate policy (`data-flow.yaml`: `stale_while_revalidate`, `ttl=120`,
     * `offline: show_cached`). The [groupId] is threaded into the store key so each group keeps an
     * independent per-page cache.
     *
     * @param groupId The savings group whose members to page through.
     * @param scope CoroutineScope (typically `viewModelScope`) driving the paging coroutines.
     * @param fetchPolicy Read policy. Defaults to [FetchPolicy.NETWORK_WITH_CACHE] — the paging
     *   analogue of stale-while-revalidate (cache-first page loads, network revalidation on
     *   refresh). Pass [FetchPolicy.CACHE_ONLY] for an explicit offline read.
     */
    fun membersPagingStream(
        groupId: String,
        scope: CoroutineScope,
        fetchPolicy: FetchPolicy = FetchPolicy.NETWORK_WITH_CACHE,
    ): PagingScreenStream<Member>
}
