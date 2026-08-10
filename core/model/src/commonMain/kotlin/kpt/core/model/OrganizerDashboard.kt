/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.model

/**
 * Domain composite for the organizer-dashboard screen — the organizer-per-group hub surface reached
 * by any user whose `dt_member_role` resolves an organizer/treasurer/chairperson role in at least
 * one group (there is NO admin client type; unified identity resolves scope server-side). Pure
 * business shape, no wire concerns.
 *
 * Backed by a SINGLE companion call (`GET /companion/organizer/dashboard`, no params — identity is
 * resolved server-side from the auth token) that returns the KPI totals scoped to "my groups"
 * plus the inline `todaySchedule` + `recentActivity` collections. Read-only monitoring surface:
 * there is no mutation path (`data-flow.yaml#sync_queue: []`, RULE-IMPLEMENT-STORE5-001 S5-1 / S5-2).
 *
 * [fieldOfficerEnabled] is an optional-tier flag (v1.1): the field-officer quick-nav tile is hidden
 * when it is `false` (standard community-organizer tier). [pendingShareOutCount] is the
 * savings-group analogue of the old "overdue loans" KPI (dropped in the loans-to-savings pivot).
 *
 * See API.md#models — OrganizerDashboardSummary.
 */
data class OrganizerDashboardSummary(
    val organizerName: String,
    val myGroupCount: Int,
    val totalMembers: Int,
    val pendingShareOutCount: Int,
    val meetingsTodayCount: Int,
    val fieldOfficerEnabled: Boolean,
    val todaySchedule: List<ScheduledMeeting>,
    val recentActivity: List<OrganizerActivityItem>,
)

/**
 * Domain model for one row of the organizer-dashboard "Today's Schedule" list
 * (`ui.yaml#components.todays_schedule_section`, repeats over `todaySchedule`). [location] is
 * optional — a meeting may have no fixed venue recorded.
 *
 * See API.md#models — ScheduledMeeting.
 */
data class ScheduledMeeting(
    val groupId: String,
    val groupName: String,
    val meetingTime: String,
    val memberCount: Int,
    val location: String?,
)

/**
 * Domain model for one row of the organizer-dashboard "Recent Activity" list
 * (`ui.yaml#components.recent_activity_section`, repeats over `recentActivity`). Distinct from the
 * group-dashboard [ActivityItem] domain type: this row additionally carries [groupName] (the
 * organizer feed spans multiple groups) and its [type] enum includes `NEW_MEMBER`, so the two are
 * kept as separate domain shapes rather than force-fitting one onto the other.
 *
 * [amount] is null for non-monetary events (`NEW_MEMBER`, `MEETING`); [memberName] / [groupName] are
 * null when the wire row omits them.
 *
 * See API.md#models — OrganizerActivityItem.
 */
data class OrganizerActivityItem(
    val id: String,
    val type: OrganizerActivityType,
    val description: String,
    val amount: Double?,
    val date: String,
    val memberName: String?,
    val groupName: String?,
)

/**
 * Domain enum for the kind of an [OrganizerActivityItem] row — verbatim mirror of
 * `api.yaml#dtos.ActivityItem.type` (`MEETING | DEPOSIT | SHARE_OUT | NEW_MEMBER | LOAN`).
 * [UNKNOWN] absorbs any wire value this client build does not yet recognize (forward compat).
 *
 * See API.md#models — OrganizerActivityType.
 */
enum class OrganizerActivityType {
    MEETING,
    DEPOSIT,
    SHARE_OUT,
    NEW_MEMBER,
    LOAN,
    UNKNOWN,
}
