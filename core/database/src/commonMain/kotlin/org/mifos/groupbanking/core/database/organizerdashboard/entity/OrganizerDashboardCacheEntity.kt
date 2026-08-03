/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.database.organizerdashboard.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Room persisted representation of the organizer-dashboard aggregate snapshot (`GET
 * /companion/organizer/dashboard`).
 *
 * Backs the offline cache (SourceOfTruth) for the single-key NETWORK_WITH_CACHE store
 * ([org.mifos.groupbanking.core.store.organizerdashboard.impl.provideOrganizerDashboardStore]).
 * Because the companion API resolves scope server-side from the auth token (no params), there is
 * exactly ONE row keyed by a constant [cacheKey] sentinel — a cold start with no network still
 * renders the last-seen organizer dashboard (`data-flow.yaml#cache.offline: serve_stale`, SC2 —
 * never memory-only).
 *
 * The whole aggregate (KPIs + today's schedule + recent activity) is persisted as ONE row via a
 * single [dashboardJson] column encoded by
 * [org.mifos.groupbanking.core.database.organizerdashboard.entity.OrganizerDashboardCacheCodec] —
 * the `core/database` module owns `kotlinx-serialization`. A single-row upsert is inherently atomic,
 * so there is no delete-then-upsert race (RULE-IMPLEMENT-STORE5-001 S5-3 satisfied by construction).
 *
 * [fetchedAt] is the epoch-millis write time — used only for TTL pruning
 * ([OrganizerDashboardDao.deleteOlderThan]); freshness for the UI banner is tracked separately by
 * the framework `framework_fetched_at` table via `FetchedAtRepository`.
 *
 * See API.md#stores — OrganizerDashboard.
 */
@Entity(tableName = "organizer_dashboard_cache")
data class OrganizerDashboardCacheEntity(
    @PrimaryKey
    val cacheKey: String,
    val dashboardJson: String,
    val fetchedAt: Long,
)
