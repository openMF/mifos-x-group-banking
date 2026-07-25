/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.database.fieldofficerdashboard.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Room persisted representation of ONE field-officer-dashboard composite snapshot per staff member
 * (FR-009 — the parallel fan-in of `get_centers_for_staff` + `get_groups_for_staff` aggregated into
 * cross-group KPIs + a per-group health list).
 *
 * Backs the offline cache (SourceOfTruth) for the composite NETWORK_WITH_CACHE store
 * ([org.mifos.groupbanking.core.store.fieldofficerdashboard.impl.provideFieldOfficerDashboardStore]),
 * a **dynamic-key** read: one row per [staffKey] so a cold start with no network still renders the
 * last-seen dashboard for that staff member (`data-flow.yaml#cache.offline: show_cached_with_banner`,
 * SC2 — never memory-only).
 *
 * The whole aggregate (KPIs + group health list + regions) is persisted as ONE row via a single
 * [dashboardJson] column encoded by
 * [org.mifos.groupbanking.core.database.fieldofficerdashboard.entity.FieldOfficerDashboardCacheCodec]
 * — the `core/database` module owns `kotlinx-serialization`. A single-row upsert is inherently
 * atomic, so there is no delete-then-upsert race (RULE-IMPLEMENT-STORE5-001 S5-3 satisfied by
 * construction).
 *
 * [fetchedAt] is the epoch-millis write time — used only for TTL pruning
 * ([FieldOfficerDashboardDao.deleteOlderThan]); freshness for the UI banner is tracked separately by
 * the framework `framework_fetched_at` table via `FetchedAtRepository`.
 *
 * See API.md#stores — FieldOfficerDashboard.
 */
@Entity(tableName = "field_officer_dashboard_cache")
data class FieldOfficerDashboardCacheEntity(
    @PrimaryKey
    val staffKey: String,
    val dashboardJson: String,
    val fetchedAt: Long,
)
