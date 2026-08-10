/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.network.mapper

import kpt.core.model.OrganizerActivityItem
import kpt.core.model.OrganizerActivityType
import kpt.core.model.OrganizerDashboardSummary
import kpt.core.model.ScheduledMeeting
import kpt.core.network.model.OrganizerActivityItemDto
import kpt.core.network.model.OrganizerDashboardSummaryDto
import kpt.core.network.model.OrganizerScheduledMeetingDto

/**
 * DTO -> domain mappers for the organizer-dashboard wire contract (`GET
 * /companion/organizer/dashboard`). Every field on [OrganizerDashboardSummaryDto] /
 * [OrganizerScheduledMeetingDto] / [OrganizerActivityItemDto] declared in `OrganizerDashboardDto.kt`
 * is mapped — no field left unmapped.
 */

fun OrganizerDashboardSummaryDto.toDomainModel(): OrganizerDashboardSummary = OrganizerDashboardSummary(
    organizerName = organizerName,
    myGroupCount = myGroupCount,
    totalMembers = totalMembers,
    pendingShareOutCount = pendingShareOutCount,
    meetingsTodayCount = meetingsTodayCount,
    fieldOfficerEnabled = fieldOfficerEnabled,
    todaySchedule = todaySchedule.map { it.toDomainModel() },
    recentActivity = recentActivity.map { it.toDomainModel() },
)

fun OrganizerScheduledMeetingDto.toDomainModel(): ScheduledMeeting = ScheduledMeeting(
    groupId = groupId,
    groupName = groupName,
    meetingTime = meetingTime,
    memberCount = memberCount,
    location = location,
)

fun OrganizerActivityItemDto.toDomainModel(): OrganizerActivityItem = OrganizerActivityItem(
    id = id,
    type = type.toOrganizerActivityType(),
    description = description,
    amount = amount,
    date = date,
    memberName = memberName,
    groupName = groupName,
)

/** Maps the wire `type` string to the domain enum, defaulting to `UNKNOWN` for unrecognized values. */
private fun String.toOrganizerActivityType(): OrganizerActivityType =
    OrganizerActivityType.entries.firstOrNull { it.name == this } ?: OrganizerActivityType.UNKNOWN
