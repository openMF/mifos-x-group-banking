/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.model

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
