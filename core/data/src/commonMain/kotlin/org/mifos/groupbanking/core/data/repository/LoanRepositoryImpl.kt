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

import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.base.store.infra.FetchedAtRepository
import kpt.core.base.store.paging.PageKey
import kpt.core.base.store.paging.PagingScreenStream
import kpt.core.base.store.paging.asPagingScreenStream
import kpt.core.base.store.screen.FetchPolicy
import org.mifos.groupbanking.core.model.LoanSummary
import org.mifos.groupbanking.core.network.mapper.toDomainModels
import org.mifos.groupbanking.core.network.service.loanlist.LoanApi
import org.mobilenativefoundation.store.store5.Store

/**
 * Store5-backed implementation of [LoanRepository].
 *
 * The read goes exclusively through [Store.asPagingScreenStream] so the whole offline-first paged
 * pipeline (cached page emit → background revalidate → DecisionEngine → ScreenState + load-more +
 * pull-to-refresh) is inherited from `core-base`. The [LoanRepository.loansPagingStream] `groupId`
 * (a `Long`) is passed as the paging `query` in String form, which the store threads into
 * `PageKey.query` so the cache is per-group. No DAO-bypass read, no `try-catch`, no `Result`
 * envelope (RULE-IMPLEMENT-STORE5-001 S5-2).
 *
 * [getLoansForClient] is a second, ADDITIVE read for the personal-loans feature — a direct
 * passthrough over [loanApi] (no Store5 wrap, same Store5-free branch as
 * [LoanRepaymentRepositoryImpl]/[LoanWriteoffRepositoryImpl]), reusing [LoanApi.getClientLoans] +
 * the existing `LoanSummaryDto -> LoanSummary` mapper. It does not touch [loansPagingStore].
 *
 * See API.md#stores — LoanList.
 */
class LoanRepositoryImpl(
    private val loansPagingStore: Store<PageKey, List<LoanSummary>>,
    private val networkMonitor: NetworkMonitor,
    private val fetchedAtRepository: FetchedAtRepository,
    private val loanApi: LoanApi,
) : LoanRepository {

    override fun loansPagingStream(
        groupId: Long,
        scope: CoroutineScope,
        fetchPolicy: FetchPolicy,
    ): PagingScreenStream<LoanSummary> = loansPagingStore.asPagingScreenStream(
        networkMonitor = networkMonitor,
        fetchedAtRepository = fetchedAtRepository,
        cacheKey = "$CACHE_KEY_PREFIX$groupId",
        scope = scope,
        pageSize = PAGE_SIZE,
        query = groupId.toString(),
        fetchPolicy = fetchPolicy,
    )

    override suspend fun getLoansForClient(clientId: Long): NetworkResult<List<LoanSummary>, NetworkError> =
        when (val result = loanApi.getClientLoans(clientId)) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.toDomainModels())
            is NetworkResult.Error -> result
        }

    private companion object {
        /** FetchedAtRepository key prefix — one freshness timestamp per group. */
        const val CACHE_KEY_PREFIX = "loanlist:loans:"

        /** Page size — matches `api.yaml#pagination.page_size` (20). */
        const val PAGE_SIZE = 20
    }
}
