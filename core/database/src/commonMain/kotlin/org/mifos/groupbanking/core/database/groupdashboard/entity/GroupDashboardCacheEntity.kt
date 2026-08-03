/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.database.groupdashboard.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Room persisted representation of ONE group-dashboard composite snapshot per group (COMP-GRP-001 —
 * the 4-way parallel fan-in of `get_group` + `get_viewer_role` + `get_group_corpus` +
 * `get_group_accounts`).
 *
 * Backs the offline cache (SourceOfTruth) for the composite NETWORK_WITH_CACHE store
 * ([org.mifos.groupbanking.core.store.groupdashboard.impl.provideGroupDashboardStore]), a
 * **dynamic-key** read: one row per [groupId] so a cold start with no network still renders the
 * last-seen dashboard for that group (`data-flow.yaml#cache.offline: fallback_cache`, SC2 — never
 * memory-only).
 *
 * The whole `GroupDashboard` composite (nested identity + viewer role + corpus + accounts +
 * activity feed) is persisted as ONE row per group via a single [dashboardJson] column encoded by
 * [org.mifos.groupbanking.core.database.groupdashboard.entity.GroupDashboardCacheCodec] — the
 * `core/database` module owns `kotlinx-serialization`. A single-row upsert is inherently atomic, so
 * there is no delete-then-upsert race (RULE-IMPLEMENT-STORE5-001 S5-3 satisfied by construction).
 *
 * [fetchedAt] is the epoch-millis write time — used only for TTL pruning
 * ([GroupDashboardDao.deleteOlderThan]); freshness for the UI banner is tracked separately by the
 * framework `framework_fetched_at` table via `FetchedAtRepository`.
 *
 * See API.md#stores — GroupDashboard.
 */
@Entity(tableName = "group_dashboard_cache")
data class GroupDashboardCacheEntity(
    @PrimaryKey
    val groupId: String,
    val dashboardJson: String,
    val fetchedAt: Long,
)
