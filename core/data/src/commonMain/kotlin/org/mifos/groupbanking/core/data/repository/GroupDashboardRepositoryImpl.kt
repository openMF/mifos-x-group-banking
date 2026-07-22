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
import kpt.core.base.store.infra.FetchedAtRepository
import kpt.core.base.store.screen.FetchPolicy
import kpt.core.base.store.screen.ScreenDataStream
import kpt.core.base.store.screen.asScreenStream
import kpt.core.store.AppStoreRegistry
import org.mifos.groupbanking.core.model.GroupDashboard
import org.mobilenativefoundation.store.store5.Store

/**
 * Store5-backed implementation of [GroupDashboardRepository].
 *
 * The composite dynamic-key read maps `groupId` to the store key and goes exclusively through
 * [Store.asScreenStream] so the whole offline-first pipeline (cached emit → background revalidate →
 * DecisionEngine → ScreenState) is inherited from `core-base`. The freshness [cacheKey] is
 * per-group (`groupdashboard:{groupId}`) so each group's TTL window is tracked independently. No
 * DAO-bypass read, no `try-catch`, no `Result` envelope (RULE-IMPLEMENT-STORE5-001 S5-2). The
 * error_state Retry CTA (`data-flow.yaml#error_paths`, `library_refs: [cmp-network-monitor]`)
 * re-drives via [ScreenDataStream.retry] — the injected [NetworkMonitor] pre-checks connectivity.
 *
 * See API.md#stores — GroupDashboard.
 */
class GroupDashboardRepositoryImpl(
    private val groupDashboardStore: Store<String, GroupDashboard>,
    private val networkMonitor: NetworkMonitor,
    private val fetchedAtRepository: FetchedAtRepository,
) : GroupDashboardRepository {

    override fun groupDashboardStream(
        groupId: String,
        scope: CoroutineScope,
        fetchPolicy: FetchPolicy,
    ): ScreenDataStream<GroupDashboard> {
        return groupDashboardStore.asScreenStream(
            key = groupId,
            networkMonitor = networkMonitor,
            fetchedAtRepository = fetchedAtRepository,
            cacheKey = "$CACHE_KEY_PREFIX$groupId",
            scope = scope,
            // A single GroupDashboard composite is never "empty" once present — Content always.
            isEmpty = { false },
            fetchPolicy = fetchPolicy,
            ttl = AppStoreRegistry.Ttl.GROUP_DASHBOARD,
        )
    }

    private companion object {
        /** FetchedAtRepository key prefix — one freshness timestamp per group. */
        const val CACHE_KEY_PREFIX = "groupdashboard:"
    }
}
