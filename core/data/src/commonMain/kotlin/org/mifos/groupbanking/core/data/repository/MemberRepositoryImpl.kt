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

import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kpt.core.base.store.infra.FetchedAtRepository
import kpt.core.base.store.paging.PageKey
import kpt.core.base.store.paging.PagingScreenStream
import kpt.core.base.store.paging.asPagingScreenStream
import kpt.core.base.store.screen.FetchPolicy
import org.mifos.groupbanking.core.model.Member
import org.mobilenativefoundation.store.store5.Store

/**
 * Store5-backed implementation of [MemberRepository].
 *
 * The read goes exclusively through [Store.asPagingScreenStream] so the whole offline-first paged
 * pipeline (cached page emit → background revalidate → DecisionEngine → ScreenState + load-more +
 * pull-to-refresh) is inherited from `core-base`. The [MemberRepository.membersPagingStream]
 * `groupId` is passed as the paging `query`, which the store threads into `PageKey.query` so the
 * cache is per-group. No DAO-bypass read, no `try-catch`, no `Result` envelope
 * (RULE-IMPLEMENT-STORE5-001 S5-2).
 *
 * See API.md#stores — MemberList.
 */
class MemberRepositoryImpl(
    private val membersPagingStore: Store<PageKey, List<Member>>,
    private val networkMonitor: NetworkMonitor,
    private val fetchedAtRepository: FetchedAtRepository,
) : MemberRepository {

    override fun membersPagingStream(
        groupId: String,
        scope: CoroutineScope,
        fetchPolicy: FetchPolicy,
    ): PagingScreenStream<Member> = membersPagingStore.asPagingScreenStream(
        networkMonitor = networkMonitor,
        fetchedAtRepository = fetchedAtRepository,
        cacheKey = "$CACHE_KEY_PREFIX$groupId",
        scope = scope,
        pageSize = PAGE_SIZE,
        query = groupId,
        fetchPolicy = fetchPolicy,
    )

    private companion object {
        /** FetchedAtRepository key prefix — one freshness timestamp per group. */
        const val CACHE_KEY_PREFIX = "memberlist:members:"

        /** Page size — matches `api.yaml#pagination.page_size` (20). */
        const val PAGE_SIZE = 20
    }
}
