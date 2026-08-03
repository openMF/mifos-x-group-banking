/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.database.memberlist.entity

import androidx.room3.Entity
import androidx.room3.Index

/**
 * Room persisted representation of one cached member-list row (`GET /groups/{groupId}/clients`).
 *
 * Backs the offline cache (SourceOfTruth) for the member-list's PAGINATED NETWORK_WITH_CACHE
 * store ([org.mifos.groupbanking.core.store.memberlist.impl.provideMembersPagingStore]) so a cold
 * start with no network still renders the last-seen pages
 * (`data-flow.yaml#cache.offline: show_cached`, SC2 — never memory-only).
 *
 * The cache is per-group AND per-page: the composite primary key is [groupId] + [memberId] (a
 * member appears exactly once within a group's roster — the natural business key scoped by group),
 * so distinct groups never collide. [pageIndex] records which offset page the row belongs to so the
 * store's per-page reader/writer can slice the cache, and [rowOrder] preserves server order within
 * a page.
 *
 * [role] and [loanStatus] are persisted as the enum `.name` string (mapped back on read by the
 * store) so a server-added value never breaks the schema.
 *
 * [fetchedAt] is the epoch-millis write time — used only for TTL pruning
 * ([org.mifos.groupbanking.core.database.memberlist.dao.MemberListDao.deleteOlderThan]); freshness
 * for the UI banner is tracked separately by the framework `framework_fetched_at` table via
 * `FetchedAtRepository`.
 *
 * See API.md#stores — MemberList.
 */
@Entity(
    tableName = "member_list_cache",
    primaryKeys = ["groupId", "memberId"],
    indices = [Index(value = ["groupId", "pageIndex"])],
)
data class MemberListEntity(
    val groupId: String,
    val memberId: String,
    val pageIndex: Int,
    val rowOrder: Int,
    val fineractClientId: Long,
    val displayName: String,
    val photoUri: String?,
    val role: String,
    val savingsBalance: Double,
    val loanStatus: String,
    val fetchedAt: Long,
)
