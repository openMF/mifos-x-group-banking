/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package kpt.core.network.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire DTO for the organizer-dashboard companion call (`GET /companion/organizer/dashboard`) —
 * `idea-layer/screens/organizer-dashboard/api.yaml#api[0]` (unified-identity companion API; resolves
 * the caller's organizer groups via `dt_member_role`, no `staffId` param — replaces the raw Fineract
 * `/staff/{id}/summary` + `/groups` + `/journal-entries` triad). Returns KPIs scoped to "my groups"
 * plus the inline [todaySchedule] + [recentActivity] collections.
 *
 * See API.md#dtos — OrganizerDashboardSummary.
 */
@Serializable
data class OrganizerDashboardSummaryDto(
    @SerialName("organizerName") val organizerName: String,
    @SerialName("myGroupCount") val myGroupCount: Int = 0,
    @SerialName("totalMembers") val totalMembers: Int = 0,
    @SerialName("pendingShareOutCount") val pendingShareOutCount: Int = 0,
    @SerialName("meetingsTodayCount") val meetingsTodayCount: Int = 0,
    @SerialName("fieldOfficerEnabled") val fieldOfficerEnabled: Boolean = false,
    @SerialName("todaySchedule") val todaySchedule: List<OrganizerScheduledMeetingDto> = emptyList(),
    @SerialName("recentActivity") val recentActivity: List<OrganizerActivityItemDto> = emptyList(),
) {
    companion object {
        /** Bumped when this DTO shape changes (registry `version:` driven). See EC30. */
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire DTO for one row of [OrganizerDashboardSummaryDto.todaySchedule] — a group with a meeting
 * scheduled today (`api.yaml#dtos.ScheduledMeeting`). [location] is nullable (optional venue).
 *
 * See API.md#dtos — ScheduledMeeting.
 */
@Serializable
data class OrganizerScheduledMeetingDto(
    @SerialName("groupId") val groupId: String,
    @SerialName("groupName") val groupName: String,
    @SerialName("meetingTime") val meetingTime: String,
    @SerialName("memberCount") val memberCount: Int = 0,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    @SerialName("location") val location: String? = null,
)

/**
 * Wire DTO for one row of [OrganizerDashboardSummaryDto.recentActivity] — a cross-group activity
 * feed row (`api.yaml#dtos.ActivityItem`). [type] is a free `String` mapped to the domain
 * `OrganizerActivityType` enum with an `UNKNOWN` fallback (EC30 forward-compat). [amount] /
 * [memberName] / [groupName] are nullable (non-monetary / anonymous / group-less rows).
 *
 * See API.md#dtos — ActivityItem.
 */
@Serializable
data class OrganizerActivityItemDto(
    @SerialName("id") val id: String,
    @SerialName("type") val type: String,
    @SerialName("description") val description: String,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    @SerialName("amount") val amount: Double? = null,
    @SerialName("date") val date: String,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    @SerialName("memberName") val memberName: String? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    @SerialName("groupName") val groupName: String? = null,
)
