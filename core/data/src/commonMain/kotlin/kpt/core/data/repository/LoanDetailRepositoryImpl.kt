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

import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kpt.core.base.store.infra.FetchedAtRepository
import kpt.core.base.store.screen.FetchPolicy
import kpt.core.base.store.screen.ScreenDataStream
import kpt.core.base.store.screen.asScreenStream
import kpt.core.model.LoanDetailResponse
import kpt.core.store.AppStoreRegistry
import org.mobilenativefoundation.store.store5.Store

/**
 * Store5-backed implementation of [LoanDetailRepository].
 *
 * The single-key read maps the loanId straight to the store key and goes exclusively through
 * [Store.asScreenStream] so the whole offline-first pipeline (cached emit → background revalidate →
 * DecisionEngine → ScreenState) is inherited from `core-base`. The freshness [cacheKey] is per-loan
 * (`loandetail:loanDetail:{loanId}`) so each loan's TTL window is tracked independently. No
 * DAO-bypass read, no `try-catch`, no `Result` envelope (RULE-IMPLEMENT-STORE5-001 S5-2).
 *
 * See API.md#stores — LoanDetailResponse.
 */
class LoanDetailRepositoryImpl(
    private val loanDetailStore: Store<Long, LoanDetailResponse>,
    private val networkMonitor: NetworkMonitor,
    private val fetchedAtRepository: FetchedAtRepository,
) : LoanDetailRepository {

    override fun loanDetailStream(
        loanId: Long,
        scope: CoroutineScope,
        fetchPolicy: FetchPolicy,
    ): ScreenDataStream<LoanDetailResponse> {
        return loanDetailStore.asScreenStream(
            key = loanId,
            networkMonitor = networkMonitor,
            fetchedAtRepository = fetchedAtRepository,
            cacheKey = "$CACHE_KEY_PREFIX$loanId",
            scope = scope,
            // A single LoanDetailResponse snapshot is never "empty" once present — Content always.
            isEmpty = { false },
            fetchPolicy = fetchPolicy,
            ttl = AppStoreRegistry.Ttl.LOAN_DETAIL,
        )
    }

    private companion object {
        /** FetchedAtRepository key prefix — one freshness timestamp per loan. */
        const val CACHE_KEY_PREFIX = "loandetail:loanDetail:"
    }
}
