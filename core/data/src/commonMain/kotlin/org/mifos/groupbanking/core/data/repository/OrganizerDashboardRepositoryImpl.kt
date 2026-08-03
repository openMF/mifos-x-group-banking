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
import kpt.core.base.store.screen.FetchPolicy
import kpt.core.base.store.screen.ScreenDataStream
import kpt.core.base.store.screen.asScreenStream
import kpt.core.store.AppStoreRegistry
import org.mifos.groupbanking.core.model.OrganizerDashboardSummary
import org.mifos.groupbanking.core.store.organizerdashboard.impl.ORGANIZER_DASHBOARD_KEY
import org.mobilenativefoundation.store.store5.Store

/**
 * Store5-backed implementation of [OrganizerDashboardRepository].
 *
 * The single-key read maps the constant [ORGANIZER_DASHBOARD_KEY] to the store key and goes
 * exclusively through [Store.asScreenStream] so the whole offline-first pipeline (cached emit →
 * background revalidate → DecisionEngine → ScreenState) is inherited from `core-base`. No DAO-bypass
 * read, no `try-catch`, no `Result` envelope (RULE-IMPLEMENT-STORE5-001 S5-2).
 *
 * See API.md#stores — OrganizerDashboard.
 */
class OrganizerDashboardRepositoryImpl(
    private val organizerDashboardStore: Store<String, OrganizerDashboardSummary>,
    private val networkMonitor: NetworkMonitor,
    private val fetchedAtRepository: FetchedAtRepository,
) : OrganizerDashboardRepository {

    override fun organizerDashboardStream(
        scope: CoroutineScope,
        fetchPolicy: FetchPolicy,
    ): ScreenDataStream<OrganizerDashboardSummary> {
        return organizerDashboardStore.asScreenStream(
            key = ORGANIZER_DASHBOARD_KEY,
            networkMonitor = networkMonitor,
            fetchedAtRepository = fetchedAtRepository,
            cacheKey = CACHE_KEY,
            scope = scope,
            // A single OrganizerDashboardSummary snapshot is never "empty" once present — Content
            // always. The genuinely-zero-groups Empty state is derived by the ViewModel from
            // myGroupCount == 0 (see OrganizerDashboardState.screenState).
            isEmpty = { false },
            fetchPolicy = fetchPolicy,
            ttl = AppStoreRegistry.Ttl.ORGANIZER_DASHBOARD,
        )
    }

    private companion object {
        /** FetchedAtRepository key — single freshness timestamp for the organizer dashboard. */
        const val CACHE_KEY = "organizerdashboard:organizerDashboard"
    }
}
