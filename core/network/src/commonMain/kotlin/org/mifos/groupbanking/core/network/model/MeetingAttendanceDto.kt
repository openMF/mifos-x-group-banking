/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire DTO for one per-member attendance row of a completed meeting
 * (`GET /fineract-provider/api/v1/datatables/dt_meeting_attendance/{meetingId}`,
 * `previous-meeting-review/api.yaml#api[get_meeting_attendance]` +
 * `#dtos.AttendanceRecord`). The `dt_meeting_attendance` datatable returns one row per member with a
 * `PRESENT` / `LATE` / `ABSENT` status string and an absence/late fine amount.
 *
 * Named `MeetingAttendanceRowDto` (not `AttendanceRecordDto`) to keep the wire type distinct from
 * the domain `AttendanceRecord` and to avoid any future `@Serializable` name clash in the shared
 * `core.network.model` package (the batch-1 `MeetingRecordDetailDto` collision precedent). Money
 * field is `Long` (whole KES); status is the raw wire string parsed to the domain enum in the mapper.
 *
 * See API.md#dtos — AttendanceRecord.
 */
@Serializable
data class MeetingAttendanceRowDto(
    @SerialName("memberId") val memberId: String,
    @SerialName("memberName") val memberName: String,
    @SerialName("status") val status: String,
    @SerialName("fineAmount") val fineAmount: Long = 0L,
) {
    companion object {
        /** Bumped when this DTO shape changes (registry `version:` driven). */
        const val SCHEMA_VERSION = 1
    }
}
