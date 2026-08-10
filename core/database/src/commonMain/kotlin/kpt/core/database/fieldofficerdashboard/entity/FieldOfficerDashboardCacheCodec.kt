/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.database.fieldofficerdashboard.entity

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * JSON codec for the WHOLE field-officer-dashboard aggregate folded into a single
 * [FieldOfficerDashboardCacheEntity.dashboardJson] column (FR-009 — KPIs + per-group health list +
 * regions). Lives in `core/database` because that module owns the `kotlinx-serialization`
 * dependency — `core/store` does not, so the store maps its domain composite to these flat,
 * wire-neutral payloads and delegates (de)serialization here (same seam as
 * `GroupDashboardCacheCodec`).
 *
 * The payloads are deliberately String-scalar (enums persisted as their `name`) so the JSON shape
 * stays forward-compatible; [Json.ignoreUnknownKeys] absorbs any extra field a newer build writes.
 *
 * See API.md#stores — FieldOfficerDashboard.
 */
object FieldOfficerDashboardCacheCodec {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(dashboard: CachedFieldOfficerDashboard): String = json.encodeToString(dashboard)

    fun decode(raw: String): CachedFieldOfficerDashboard = json.decodeFromString(raw)
}

/** Persistence payload mirroring the domain `FieldOfficerDashboard` aggregate. */
@Serializable
data class CachedFieldOfficerDashboard(
    val staffId: Long,
    val userRole: String,
    val totalGroupsCount: Int,
    val totalActiveMembers: Int,
    val totalSavingsThisMonth: Double,
    val totalLoansOutstanding: Double,
    val groups: List<CachedGroupHealthSummary>,
    val availableRegions: List<String>,
)

/**
 * Persistence payload for one group-health row. [healthIndicator] is the domain enum `name` string
 * (re-parsed with `UNKNOWN`/rate-derivation fallback on read for forward compatibility).
 */
@Serializable
data class CachedGroupHealthSummary(
    val id: Long,
    val fineractGroupId: Long,
    val name: String,
    val officeName: String,
    val status: String,
    val activeClientCount: Int,
    val totalSavingsBalance: Double,
    val totalLoansOutstanding: Double,
    val overdueRate: Double,
    val healthIndicator: String,
    val cycleNumber: Int,
)
