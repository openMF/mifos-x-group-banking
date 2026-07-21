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
import kpt.core.base.store.paging.PagingScreenStream
import kpt.core.base.store.screen.FetchPolicy
import org.mifos.groupbanking.core.model.Group

/**
 * Read surface for the authenticated user's group list (COMP-GRP-001 — `GET
 * /companion/groups/mine`) backing the group-list screen.
 *
 * Wraps the PAGINATED NETWORK_WITH_CACHE `GroupsPagingStore` and exposes exactly one read path —
 * [groupsPagingStream], an offline-first [PagingScreenStream] of `Group`. There is no DAO-bypass
 * read and no write path: the list is read-only (RULE-IMPLEMENT-STORE5-001 S5-1 / S5-2). No
 * `try-catch`, no `Result<T>` envelope — the stream surfaces `ScreenState<List<Group>>`
 * (Loading / Content / NoNetwork / Error / Empty) directly, with load-more + pull-to-refresh
 * built into the returned [PagingScreenStream].
 *
 * See API.md#stores — GroupList.
 */
interface GroupRepository {

    /**
     * Offline-first PAGINATED stream of the authenticated user's groups.
     *
     * The returned [PagingScreenStream] exposes:
     * - [PagingScreenStream.state] — `Flow<ScreenState<List<Group>>>` (the accumulated pages).
     * - [PagingScreenStream.loadNextPage] — load-more on scroll-to-end (`OnLoadMoreTap`).
     * - [PagingScreenStream.refresh] / [PagingScreenStream.retry] — pull-to-refresh / error-retry
     *   (`OnRefresh` / `Retry`); resets to page 0 and forces a fresh fetch.
     *
     * Emits cached pages immediately then background-revalidates per the store's
     * stale-while-revalidate policy (`data-flow.yaml`: `stale_while_revalidate`, `ttl=300`,
     * `offline: show_cached`).
     *
     * @param scope CoroutineScope (typically `viewModelScope`) driving the paging coroutines.
     * @param fetchPolicy Read policy. Defaults to [FetchPolicy.NETWORK_WITH_CACHE] — the paging
     *   analogue of stale-while-revalidate (cache-first page loads, network revalidation on
     *   refresh). Pass [FetchPolicy.CACHE_ONLY] for an explicit offline read.
     */
    fun groupsPagingStream(
        scope: CoroutineScope,
        fetchPolicy: FetchPolicy = FetchPolicy.NETWORK_WITH_CACHE,
    ): PagingScreenStream<Group>
}
