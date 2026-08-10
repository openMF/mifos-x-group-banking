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

import kpt.core.model.MeetingListItem
import kpt.core.model.MeetingStatus
import kpt.core.network.model.MeetingListItemDto
import kpt.core.network.model.MeetingRecordItemDto
import kpt.core.network.model.MeetingRecordListDto
import kpt.core.network.model.MeetingStatusDto

/**
 * Merges the two meeting-calendar reads into the domain list. Each [MeetingListItemDto] from
 * `get_meeting_schedule` is enriched, WHERE its own `attendanceCount`/`totalCollectedKES` are null,
 * with the matching-by-`meetingNumber` [MeetingRecordItemDto] financial record from
 * `get_meeting_records_datatable` (`totalCollectedKES = totalSavings + totalRepayments`). MISSED /
 * UPCOMING meetings that have no record simply keep their null figures.
 *
 * See API.md#dtos — MeetingListItem.
 */
fun toMeetingListItems(
    meetings: List<MeetingListItemDto>,
    records: MeetingRecordListDto?,
): List<MeetingListItem> {
    val recordByNumber: Map<Int, MeetingRecordItemDto> =
        records?.records?.associateBy { it.meetingNumber } ?: emptyMap()
    return meetings.map { dto ->
        val record = recordByNumber[dto.meetingNumber]
        MeetingListItem(
            meetingId = dto.meetingId,
            meetingNumber = dto.meetingNumber,
            meetingDate = dto.meetingDate,
            status = dto.status.toDomain(),
            attendanceCount = dto.attendanceCount ?: record?.attendanceCount,
            totalCollectedKES = dto.totalCollectedKES
                ?: record?.let { it.totalSavings + it.totalRepayments },
            meetingTime = dto.meetingTime,
        )
    }
}

/** Wire enum -> domain, with [MeetingStatus.MISSED] as the conservative fallback for UNKNOWN. */
fun MeetingStatusDto.toDomain(): MeetingStatus = when (this) {
    MeetingStatusDto.UPCOMING -> MeetingStatus.UPCOMING
    MeetingStatusDto.COMPLETED -> MeetingStatus.COMPLETED
    MeetingStatusDto.MISSED -> MeetingStatus.MISSED
    MeetingStatusDto.UNKNOWN -> MeetingStatus.MISSED
}
