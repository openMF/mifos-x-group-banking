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

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Room persisted representation of the per-member attendance roster for ONE completed meeting
 * (`GET /datatables/dt_meeting_attendance/{meetingId}`, previous-meeting-review).
 *
 * Backs the offline cache (SourceOfTruth) for the single-key NETWORK_WITH_CACHE meeting-attendance
 * store ([kpt.core.store.previousmeetingreview.impl.provideMeetingAttendanceStore]):
 * one row per meeting so a cold start with no network still renders the last-seen attendance
 * (`data-flow.yaml` `stale_while_revalidate`, `error_paths[network.offline]: fallback_cache` — never
 * memory-only, RULE-IMPLEMENT-SCALE-CODEGEN-001 SC2).
 *
 * Keyed by [meetingId] (the store key). The whole attendance list is folded into
 * [attendanceJson] via
 * [kpt.core.database.previousmeetingreview.entity.MeetingAttendanceCacheCodec] —
 * `core/database` owns the `kotlinx-serialization` dependency, so the store maps its domain list to
 * the flat payloads and delegates (de)serialization here (same seam as `MeetingRecordCacheCodec`). A
 * single-row upsert is inherently atomic, so there is no delete-then-upsert race
 * (RULE-IMPLEMENT-STORE5-001 S5-3 satisfied by construction).
 *
 * [fetchedAt] is the epoch-millis write time — used only for TTL pruning ([deleteOlderThan]).
 *
 * See API.md#stores — AttendanceRecord.
 */
@Entity(tableName = "meeting_attendance_cache")
data class MeetingAttendanceCacheEntity(
    @PrimaryKey
    val meetingId: String,
    val attendanceJson: String,
    val fetchedAt: Long,
)
