/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.database.organizerdashboard.entity

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * JSON codec for the WHOLE organizer-dashboard aggregate folded into a single
 * [OrganizerDashboardCacheEntity.dashboardJson] column (KPIs + today's schedule + recent activity).
 * Lives in `core/database` because that module owns the `kotlinx-serialization` dependency —
 * `core/store` does not, so the store maps its domain composite to these flat, wire-neutral payloads
 * and delegates (de)serialization here (same seam as `FieldOfficerDashboardCacheCodec`).
 *
 * The payloads are deliberately String-scalar (enums persisted as their `name`) so the JSON shape
 * stays forward-compatible; [Json.ignoreUnknownKeys] absorbs any extra field a newer build writes.
 *
 * See API.md#stores — OrganizerDashboard.
 */
object OrganizerDashboardCacheCodec {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(dashboard: CachedOrganizerDashboard): String = json.encodeToString(dashboard)

    fun decode(raw: String): CachedOrganizerDashboard = json.decodeFromString(raw)
}

/** Persistence payload mirroring the domain `OrganizerDashboardSummary` aggregate. */
@Serializable
data class CachedOrganizerDashboard(
    val organizerName: String,
    val myGroupCount: Int,
    val totalMembers: Int,
    val pendingShareOutCount: Int,
    val meetingsTodayCount: Int,
    val fieldOfficerEnabled: Boolean,
    val todaySchedule: List<CachedScheduledMeeting>,
    val recentActivity: List<CachedOrganizerActivityItem>,
)

/** Persistence payload for one `todaySchedule` row. */
@Serializable
data class CachedScheduledMeeting(
    val groupId: String,
    val groupName: String,
    val meetingTime: String,
    val memberCount: Int,
    val location: String? = null,
)

/**
 * Persistence payload for one `recentActivity` row. [type] is the domain enum `name` string
 * (re-parsed with `UNKNOWN` fallback on read for forward compatibility).
 */
@Serializable
data class CachedOrganizerActivityItem(
    val id: String,
    val type: String,
    val description: String,
    val amount: Double? = null,
    val date: String,
    val memberName: String? = null,
    val groupName: String? = null,
)
