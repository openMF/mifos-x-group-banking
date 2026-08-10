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
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.base.store.infra.FetchedAtRepository
import kpt.core.base.store.screen.FetchPolicy
import kpt.core.base.store.screen.ScreenDataStream
import kpt.core.base.store.screen.asScreenStream
import kpt.core.model.FieldOfficerDashboard
import kpt.core.network.service.fieldofficerdashboard.FieldOfficerApi
import kpt.core.store.AppStoreRegistry
import kpt.core.store.fieldofficerdashboard.impl.fieldOfficerDashboardStoreKey
import org.mobilenativefoundation.store.store5.Store

/**
 * Store5-backed implementation of [FieldOfficerDashboardRepository].
 *
 * The composite dynamic-key read maps `"$staffId|$userRole"` to the store key and goes exclusively
 * through [Store.asScreenStream] so the whole offline-first pipeline (cached emit → background
 * revalidate → DecisionEngine → ScreenState) is inherited from `core-base`. The freshness [cacheKey]
 * is per-staff (`fieldofficerdashboard:{staffId}`) so each staff member's TTL window is tracked
 * independently. No DAO-bypass read, no `try-catch`, no `Result` envelope (RULE-IMPLEMENT-STORE5-001
 * S5-2).
 *
 * [exportReport] is Store5-FREE (no read-stream to cache) — it forwards straight to
 * [FieldOfficerApi.runReport] and surfaces the sealed [NetworkResult]; the ViewModel hands the CSV
 * bytes to the OS share sheet.
 *
 * See API.md#stores — FieldOfficerDashboard.
 */
class FieldOfficerDashboardRepositoryImpl(
    private val fieldOfficerDashboardStore: Store<String, FieldOfficerDashboard>,
    private val fieldOfficerApi: FieldOfficerApi,
    private val networkMonitor: NetworkMonitor,
    private val fetchedAtRepository: FetchedAtRepository,
) : FieldOfficerDashboardRepository {

    override fun fieldOfficerDashboardStream(
        staffId: Long,
        userRole: String,
        scope: CoroutineScope,
        fetchPolicy: FetchPolicy,
    ): ScreenDataStream<FieldOfficerDashboard> {
        return fieldOfficerDashboardStore.asScreenStream(
            key = fieldOfficerDashboardStoreKey(staffId, userRole),
            networkMonitor = networkMonitor,
            fetchedAtRepository = fetchedAtRepository,
            cacheKey = "$CACHE_KEY_PREFIX$staffId",
            scope = scope,
            // A field officer with zero assigned groups is a legitimate Empty state, surfaced by the
            // ViewModel from Content when groups.isEmpty() — the aggregate itself is never "empty"
            // once fetched, so the stream stays Content and the screen derives Empty.
            isEmpty = { false },
            fetchPolicy = fetchPolicy,
            ttl = AppStoreRegistry.Ttl.FIELD_OFFICER_DASHBOARD,
        )
    }

    override suspend fun exportReport(staffId: Long): NetworkResult<ByteArray, NetworkError> =
        fieldOfficerApi.runReport(staffId = staffId)

    private companion object {
        /** FetchedAtRepository key prefix — one freshness timestamp per staff member. */
        const val CACHE_KEY_PREFIX = "fieldofficerdashboard:"
    }
}
