/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.database.grouplist.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * Room persisted representation of one cached group-list row (COMP-GRP-001 —
 * `GET /companion/groups/mine`).
 *
 * Backs the offline cache (SourceOfTruth) for the group-list's PAGINATED NETWORK_WITH_CACHE
 * store ([org.mifos.groupbanking.core.store.grouplist.impl.provideGroupsPagingStore]) so a cold
 * start with no network still renders the last-seen pages
 * (`data-flow.yaml#cache.offline: show_cached`, SC2 — never memory-only). Keyed by [groupId]
 * (the natural business key, globally unique across pages); [pageIndex] records which offset page
 * the row belongs to so the store's per-page reader/writer can slice the cache, and [rowOrder]
 * preserves server order within a page.
 *
 * [healthIndicator] is intentionally NOT persisted — per `data-flow.yaml` it is re-derived
 * client-side from [overdueRate] on read (`HealthIndicator.fromOverdueRate`) so cached/offline
 * rows stay correct even if a server-sent value drifts from the rate.
 *
 * [fetchedAt] is the epoch-millis write time — used only for TTL pruning
 * ([org.mifos.groupbanking.core.database.grouplist.dao.GroupListDao.deleteOlderThan]); freshness
 * for the UI banner is tracked separately by the framework `framework_fetched_at` table via
 * `FetchedAtRepository`.
 *
 * See API.md#stores — GroupList.
 */
@Entity(
    tableName = "group_list_cache",
    indices = [Index(value = ["pageIndex"])],
)
data class GroupListEntity(
    @PrimaryKey
    val groupId: String,
    val pageIndex: Int,
    val rowOrder: Int,
    val name: String,
    val groupType: String,
    val viewerRole: String,
    val cycleNumber: Int,
    val memberCount: Int,
    val lastMeetingDate: String,
    val overdueRate: Double,
    val status: String,
    val fineractCenterId: Long,
    val fetchedAt: Long,
)
