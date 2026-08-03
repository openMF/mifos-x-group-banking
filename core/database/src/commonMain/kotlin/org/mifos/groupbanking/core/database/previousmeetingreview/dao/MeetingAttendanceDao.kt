/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.database.previousmeetingreview.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow
import org.mifos.groupbanking.core.database.previousmeetingreview.entity.MeetingAttendanceCacheEntity

/**
 * Room DAO for the meeting-attendance cache (SourceOfTruth for the single-key NETWORK_WITH_CACHE
 * [org.mifos.groupbanking.core.store.previousmeetingreview.impl.provideMeetingAttendanceStore]).
 *
 * The store keys each meeting's attendance roster by [MeetingAttendanceCacheEntity.meetingId], so
 * [observeByKey] is the reactive per-meeting read the store's SourceOfTruth reader subscribes to and
 * [replaceForKey] is the store's SourceOfTruth writer. Because a meeting's whole roster is exactly
 * ONE row, a keyed `@Upsert` is inherently atomic — no reader ever observes a half-written snapshot
 * (guards RULE-IMPLEMENT-STORE5-001 S5-3 by construction).
 *
 * See API.md#stores — AttendanceRecord.
 */
@Dao
interface MeetingAttendanceDao {

    /** Reactive read of one cached meeting's attendance roster. Backs the store SoT reader. */
    @Query("SELECT * FROM meeting_attendance_cache WHERE meetingId = :meetingId")
    fun observeByKey(meetingId: String): Flow<MeetingAttendanceCacheEntity?>

    @Upsert
    suspend fun upsert(entity: MeetingAttendanceCacheEntity)

    @Query("DELETE FROM meeting_attendance_cache WHERE meetingId = :meetingId")
    suspend fun deleteByKey(meetingId: String)

    @Query("DELETE FROM meeting_attendance_cache")
    suspend fun deleteAll()

    /**
     * Atomically replace ONE meeting's cached attendance roster. The store SoT writer calls this so
     * the write for a [MeetingAttendanceCacheEntity.meetingId] lands in a single transaction — no
     * reader ever sees a partially-written row mid-refresh (S5-3).
     */
    @Transaction
    suspend fun replaceForKey(entity: MeetingAttendanceCacheEntity) {
        upsert(entity)
    }

    /** TTL pruning hook — removes rosters written before [epochMillis]. */
    @Query("DELETE FROM meeting_attendance_cache WHERE fetchedAt < :epochMillis")
    suspend fun deleteOlderThan(epochMillis: Long)
}
