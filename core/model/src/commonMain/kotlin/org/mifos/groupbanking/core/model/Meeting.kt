/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.model

import kotlinx.serialization.Serializable

/**
 * Recurrence cadence of a group's meeting schedule — mirror of
 * `idea-layer/screens/meeting-calendar/ui.yaml#schedule_frequency_field.options`
 * (`WEEKLY | BIWEEKLY | MONTHLY`). Drives the schedule-editor segmented selector + the
 * `RescheduleMeeting` write payload's `frequency`.
 */
enum class MeetingFrequency { WEEKLY, BIWEEKLY, MONTHLY }

/**
 * The organizer's recurring-schedule adjustment payload emitted by the meeting-calendar
 * schedule-editor sheet's Save CTA (`ui.yaml#schedule_confirm_btn`, action `RescheduleMeeting`).
 *
 * Maps to the server-gated companion `PUT /centers/{centerId}/calendars/{calendarId}?command=updateCalendar`
 * (mirrored to `dt_group_config`). The live network write is **pending-device-verify** — the companion
 * `companion_update_calendar` tool is not deployed yet — so [org.mifos.groupbanking.core.data.repository.MeetingRepository.rescheduleMeeting]
 * offline-queues this payload to the shared `sync_queue` for later drain rather than calling the network.
 *
 * [calendarId] is nullable: the meeting-calendar list read (`MeetingListItem`) carries no calendar id
 * today, so the recurrence Calendar id is unresolved at the UI seam until the companion calendar API
 * (COMP-CAL) lands — flagged, the queued payload carries `null` and the sync worker resolves it server-side.
 */
@Serializable
data class RescheduleMeetingRequest(
    val centerId: Int,
    val calendarId: String? = null,
    val day: String,
    val time: String,
    val frequency: MeetingFrequency,
)

/**
 * Lifecycle status of a single group meeting — verbatim mirror of
 * `idea-layer/screens/meeting-calendar/api.yaml#dtos.MeetingStatus`
 * (`UPCOMING | COMPLETED | MISSED`). Drives the `meeting_list_item` trailing status chip
 * (`completed -> successContainer`, `missed -> errorContainer`) and the pinned upcoming-card
 * "Start Meeting" CTA gating on the meeting-calendar screen.
 */
enum class MeetingStatus { UPCOMING, COMPLETED, MISSED }

/**
 * One meeting row for the meeting-calendar screen — the domain projection of
 * `api.yaml#dtos.MeetingListItem` (the `get_center_meetings` response row enriched with the
 * `get_meeting_records_datatable` financial record for COMPLETED meetings).
 *
 * [attendanceCount] / [totalCollectedKES] are nullable: UPCOMING (and MISSED) meetings carry no
 * attendance/collection figures, so those rows leave the trailing collected chip + `·N/5 present`
 * secondary text blank (mirrors `demo-data.yaml` — UPCOMING/MISSED rows null both fields).
 */
data class MeetingListItem(
    val meetingId: String,
    val meetingNumber: Int,
    val meetingDate: String,
    val status: MeetingStatus,
    val attendanceCount: Int? = null,
    val totalCollectedKES: Long? = null,
)
