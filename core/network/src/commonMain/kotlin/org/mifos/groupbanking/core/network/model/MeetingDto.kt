/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire DTO for one row of `get_meeting_schedule` — `GET /fineract-provider/api/v1/datatables/dt_meeting_schedule/{groupId}`
 * (`idea-layer/screens/meeting-calendar/api.yaml#dtos.MeetingListResponse`). A single scheduled
 * meeting for the group. [attendanceCount] / [totalCollectedKES] are populated only for
 * COMPLETED meetings, null for UPCOMING/MISSED.
 *
 * See API.md#dtos — MeetingListItem.
 */
@Serializable
data class MeetingListItemDto(
    @SerialName("meetingId") val meetingId: String,
    @SerialName("meetingNumber") val meetingNumber: Int,
    @SerialName("meetingDate") val meetingDate: String,
    @SerialName("status") val status: MeetingStatusDto = MeetingStatusDto.UNKNOWN,
    @SerialName("attendanceCount") val attendanceCount: Int? = null,
    @SerialName("totalCollectedKES") val totalCollectedKES: Long? = null,
    @SerialName("meetingTime") val meetingTime: String = "",
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire enum for [MeetingListItemDto.status] (`UPCOMING | COMPLETED | MISSED` per `api.yaml`).
 * [UNKNOWN] fallback so a server-added status never crashes an old client.
 */
@Serializable
enum class MeetingStatusDto {
    @SerialName("UPCOMING")
    UPCOMING,

    @SerialName("COMPLETED")
    COMPLETED,

    @SerialName("MISSED")
    MISSED,

    @SerialName("UNKNOWN")
    UNKNOWN,
}

/**
 * Wire DTO for `get_meeting_records_datatable` —
 * `GET /fineract-provider/api/v1/datatables/dt_meeting_record/{groupId}`
 * (`api.yaml#dtos.MeetingRecordList`). The completed-meeting financial records for the group that
 * enrich the past-meeting rows with attendance + collected amounts.
 *
 * See API.md#dtos — MeetingRecordList.
 */
@Serializable
data class MeetingRecordListDto(
    @SerialName("groupId") val groupId: Int,
    @SerialName("records") val records: List<MeetingRecordItemDto> = emptyList(),
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire DTO for one row of the `dt_meeting_record` datatable (`api.yaml#dtos.MeetingRecordItem`).
 * The `totalCollectedKES` figure surfaced on the meeting-calendar past-meeting row is
 * [totalSavings] + [totalRepayments].
 *
 * See API.md#dtos — MeetingRecordItem.
 */
@Serializable
data class MeetingRecordItemDto(
    @SerialName("meetingNumber") val meetingNumber: Int,
    @SerialName("actualDate") val actualDate: String,
    @SerialName("totalSavings") val totalSavings: Long,
    @SerialName("totalRepayments") val totalRepayments: Long,
    @SerialName("attendanceCount") val attendanceCount: Int,
    @SerialName("closingCorpus") val closingCorpus: Long,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}
