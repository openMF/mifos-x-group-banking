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

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * JSON codec for the WHOLE group-dashboard composite folded into a single
 * [GroupDashboardCacheEntity.dashboardJson] column (COMP-GRP-001 — the 4-way parallel fan-in of
 * `get_group` + `get_viewer_role` + `get_group_corpus` + `get_group_accounts`). Lives in
 * `core/database` because that module owns the `kotlinx-serialization` dependency — `core/store`
 * does not, so the store maps its domain composite to these flat, wire-neutral payloads and
 * delegates (de)serialization here (same seam as `MemberDashboardCacheCodec`).
 *
 * The payloads are deliberately String-scalar (enums persisted as their `name`) so the JSON shape
 * stays forward-compatible; [Json.ignoreUnknownKeys] absorbs any extra field a newer build writes.
 *
 * See API.md#stores — GroupDashboard.
 */
object GroupDashboardCacheCodec {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(dashboard: CachedGroupDashboard): String = json.encodeToString(dashboard)

    fun decode(raw: String): CachedGroupDashboard = json.decodeFromString(raw)
}

/**
 * Persistence payload mirroring the domain `GroupDashboard` composite — the 4 fanned-in sections
 * ([group], [viewerRole], [corpus], [accounts]) serialized as one JSON blob per group row.
 */
@Serializable
data class CachedGroupDashboard(
    val group: CachedGroupDetail,
    val viewerRole: CachedViewerRoleInfo,
    val corpus: CachedGroupCorpus,
    val accounts: CachedGroupAccounts,
)

/** Persistence payload for the `get_group` identity section (nested [typeConfig]). */
@Serializable
data class CachedGroupDetail(
    val id: String,
    val fineractGroupId: Long,
    val name: String,
    val cycleNumber: Int,
    val cycleLengthMonths: Int,
    val meetingFrequency: String,
    val memberCount: Int,
    val overdueLoansCount: Int,
    val status: String,
    val typeConfig: CachedGroupInstanceConfig,
)

/**
 * Persistence payload for the per-group configured instance. [groupType] / [poolModel] /
 * [contributionModel] are the domain enum `name` strings (re-parsed with `UNKNOWN` fallback on
 * read for forward compatibility).
 */
@Serializable
data class CachedGroupInstanceConfig(
    val groupType: String,
    val poolModel: String,
    val contributionModel: String,
    val shareoutFormula: String,
    val payoutOrderMethod: String,
    val shareValue: Double,
    val contributionAmount: Double,
    val socialFundEnabled: Boolean,
    val cycleLengthMonths: Int,
    val loanMultiplier: Double,
    val interestRate: Double,
    val fineAmount: Double,
)

/** Persistence payload for the `get_viewer_role` section. [role] is the `ViewerRole` enum `name`. */
@Serializable
data class CachedViewerRoleInfo(
    val role: String,
    val memberId: Long,
)

/** Persistence payload for the `get_group_corpus` section (ROTATING_PAYOUT fields nullable). */
@Serializable
data class CachedGroupCorpus(
    val currentBalance: Double,
    val openingBalance: Double,
    val totalContributionsThisCycle: Double,
    val totalLoansOutstanding: Double,
    val lastUpdated: String,
    // Defaulted so an older cached JSON blob (written before this field existed) still decodes —
    // Json.ignoreUnknownKeys covers extra keys, this default covers the missing-key direction.
    val isCycleEnd: Boolean = false,
    val rotationPosition: Int?,
    val nextRecipientName: String?,
    val nextRecipientPosition: Int?,
)

/** Persistence payload for the `get_group_accounts` section (nested [recentActivity] feed). */
@Serializable
data class CachedGroupAccounts(
    val savingsBalance: Double,
    val loansOutstanding: Double,
    val activeLoanCount: Int,
    val shareOutProjection: Double?,
    val recentActivity: List<CachedActivityItem>,
)

/** Persistence payload for one recent-activity row. [type] is the `ActivityType` enum `name`. */
@Serializable
data class CachedActivityItem(
    val id: String,
    val type: String,
    val description: String,
    val amount: Double?,
    val date: String,
    val memberName: String?,
)
