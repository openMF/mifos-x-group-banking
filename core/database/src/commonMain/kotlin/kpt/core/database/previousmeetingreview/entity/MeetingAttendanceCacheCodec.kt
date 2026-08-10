/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.database.previousmeetingreview.entity

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * JSON codec for the attendance list folded into a single [MeetingAttendanceCacheEntity] row. Lives
 * in `core/database` because that module owns the `kotlinx-serialization` dependency — `core/store`
 * does not, so the store maps its domain list to these flat, wire-neutral payloads and delegates
 * (de)serialization here (same seam as `MeetingRecordCacheCodec`).
 *
 * [Json.ignoreUnknownKeys] absorbs any extra field a newer build writes.
 *
 * See API.md#stores — AttendanceRecord.
 */
object MeetingAttendanceCacheCodec {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(rows: List<CachedAttendanceRecord>): String = json.encodeToString(rows)

    fun decode(raw: String): List<CachedAttendanceRecord> =
        if (raw.isBlank()) emptyList() else json.decodeFromString(raw)
}

/**
 * Persistence payload for one attendance row. [status] is stored as the enum NAME string
 * (`PRESENT` / `LATE` / `ABSENT`) — the store maps it back to the domain enum on read.
 */
@Serializable
data class CachedAttendanceRecord(
    val memberId: String,
    val memberName: String,
    val status: String,
    val fineAmount: Long,
)
