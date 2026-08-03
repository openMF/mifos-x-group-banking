/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.database.personaldashboard.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Room persisted representation of ONE member-dashboard snapshot per group (COMP-DASH-001 —
 * `GET /companion/member/dashboard`).
 *
 * Backs the offline cache (SourceOfTruth) for the personal-dashboard's NETWORK_WITH_CACHE
 * store ([org.mifos.groupbanking.core.store.personaldashboard.impl.provideMemberDashboardStore]),
 * a **dynamic-key** read: one row per selected group so a cold start with no network still
 * renders the last-seen dashboard for that group (`data-flow.yaml#cache.offline: use_sqldelight`,
 * SC2 — never memory-only). Keyed by [cacheKey] = `selectedGroupId ?: "__default__"` (the first
 * group the companion resolves server-side when the caller passes `null`).
 *
 * The whole `MemberDashboard` domain object is persisted as ONE row per group (task-sanctioned
 * single-row shape): the flat balance / projection fields are scalar columns, and the two nested
 * collections `myGroups` + `recentTransactions` (the shared `savings_transactions` activity list)
 * are folded into [myGroupsJson] / [recentTransactionsJson] JSON columns via
 * [org.mifos.groupbanking.core.database.personaldashboard.entity.MemberDashboardCacheCodec]. A
 * single-row upsert is inherently atomic, so there is no delete-then-upsert race
 * (RULE-IMPLEMENT-STORE5-001 S5-3 satisfied by construction).
 *
 * [poolModel] / [selectedGroupPoolModel] are persisted as their enum `name` string and re-parsed
 * on read with an `UNKNOWN` fallback for forward compatibility. [shareOutProjection] is populated
 * for ACCUMULATING pool models; [rotationPosition] + [nextRecipientEta] for ROTATING_PAYOUT
 * (mutually exclusive, all nullable).
 *
 * [fetchedAt] is the epoch-millis write time — used only for TTL pruning ([MemberDashboardDao
 * .deleteOlderThan]); freshness for the UI banner is tracked separately by the framework
 * `framework_fetched_at` table via `FetchedAtRepository`.
 *
 * See API.md#stores — MemberDashboard.
 */
@Entity(tableName = "member_dashboard_cache")
data class MemberDashboardCacheEntity(
    @PrimaryKey
    val cacheKey: String,
    val memberName: String,
    // Member-identity + savings-account ids forwarded to personal-savings (nullable individual
    // account). New columns as of AppDatabase v16 — fresh installs pick this up via
    // fallbackToDestructiveMigration.
    val clientId: Long,
    val groupLinkedSavingsId: Long,
    val individualSavingsId: Long?,
    val selectedGroupId: String,
    val selectedGroupName: String,
    val selectedGroupPoolModel: String,
    val poolModel: String,
    val groupLinkedSavingsBalance: Double,
    val individualSavingsBalance: Double,
    val shareOutProjection: Double?,
    val rotationPosition: Int?,
    val nextRecipientEta: String?,
    val myGroupsJson: String,
    val recentTransactionsJson: String,
    val fetchedAt: Long,
)
