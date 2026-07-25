/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.mapper

import org.mifos.groupbanking.core.model.AttendanceRecord
import org.mifos.groupbanking.core.model.AttendanceStatus
import org.mifos.groupbanking.core.network.model.MeetingAttendanceRowDto
import kotlin.jvm.JvmName

/**
 * DTO -> domain mappers for the meeting-attendance wire contract
 * (`GET /datatables/dt_meeting_attendance/{meetingId}`, previous-meeting-review). Every field on
 * [MeetingAttendanceRowDto] is mapped — the raw `status` string is parsed to the [AttendanceStatus]
 * enum via [AttendanceStatus.fromWire] (unknown → ABSENT).
 */
fun MeetingAttendanceRowDto.toDomainModel(): AttendanceRecord = AttendanceRecord(
    memberId = memberId,
    memberName = memberName,
    status = AttendanceStatus.fromWire(status),
    fineAmount = fineAmount,
)

/** Batch converter — maps every attendance row in declaration order. */
@JvmName("meetingAttendanceRowDtoListToDomainModels")
fun List<MeetingAttendanceRowDto>.toDomainModels(): List<AttendanceRecord> = map { it.toDomainModel() }
