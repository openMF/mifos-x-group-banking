/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.database.meetingcalendar.entity

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * JSON codec for the WHOLE meeting-calendar list folded into a single
 * [MeetingCalendarCacheEntity.meetingsJson] column. Lives in `core/database` because that module
 * owns the `kotlinx-serialization` dependency — `core/store` does not, so the store maps its domain
 * `MeetingListItem`s to these flat, wire-neutral payloads and delegates (de)serialization here
 * (same seam as `GroupDashboardCacheCodec`).
 *
 * The payloads are deliberately String-scalar (the status enum persisted as its `name`) so the JSON
 * shape stays forward-compatible; [Json.ignoreUnknownKeys] absorbs any extra field a newer build
 * writes.
 *
 * See API.md#stores — MeetingCalendar.
 */
object MeetingCalendarCacheCodec {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(meetings: List<CachedMeetingItem>): String = json.encodeToString(meetings)

    fun decode(raw: String): List<CachedMeetingItem> = json.decodeFromString(raw)
}

/**
 * Persistence payload for one meeting row. [status] is the domain `MeetingStatus` enum `name`
 * (re-parsed with a conservative fallback on read for forward compatibility).
 */
@Serializable
data class CachedMeetingItem(
    val meetingId: String,
    val meetingNumber: Int,
    val meetingDate: String,
    val status: String,
    val attendanceCount: Int? = null,
    val totalCollectedKES: Long? = null,
)
