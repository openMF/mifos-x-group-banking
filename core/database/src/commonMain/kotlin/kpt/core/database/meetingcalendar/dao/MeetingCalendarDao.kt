/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.database.meetingcalendar.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow
import kpt.core.database.meetingcalendar.entity.MeetingCalendarCacheEntity

/**
 * Room DAO for the meeting-calendar cache (SourceOfTruth for the single-key NETWORK_WITH_CACHE
 * [kpt.core.store.meetingcalendar.impl.provideMeetingCalendarStore]).
 *
 * The store keys each meetings snapshot by center ([MeetingCalendarCacheEntity.groupId]), so
 * [observeByKey] is the reactive per-center read the store's SourceOfTruth reader subscribes to and
 * [replaceForKey] is the store's SourceOfTruth writer. Because a center's whole meetings list is
 * exactly ONE row, a keyed `@Upsert` is inherently atomic — no reader ever observes a half-written
 * snapshot (guards RULE-IMPLEMENT-STORE5-001 S5-3, the delete-then-upsert race, by construction).
 *
 * See API.md#stores — MeetingCalendar.
 */
@Dao
interface MeetingCalendarDao {

    /** Reactive read of one cached center's meetings snapshot. Backs the store SoT reader. */
    @Query("SELECT * FROM meeting_calendar_cache WHERE groupId = :groupId")
    fun observeByKey(groupId: Int): Flow<MeetingCalendarCacheEntity?>

    @Upsert
    suspend fun upsert(entity: MeetingCalendarCacheEntity)

    @Query("DELETE FROM meeting_calendar_cache WHERE groupId = :groupId")
    suspend fun deleteByKey(groupId: Int)

    @Query("DELETE FROM meeting_calendar_cache")
    suspend fun deleteAll()

    /**
     * Atomically replace ONE center's cached meetings snapshot. The store SoT writer calls this so
     * the write for a [MeetingCalendarCacheEntity.groupId] lands in a single transaction — no
     * reader ever sees a partially-written row mid-refresh (S5-3).
     */
    @Transaction
    suspend fun replaceForKey(entity: MeetingCalendarCacheEntity) {
        upsert(entity)
    }

    /** TTL pruning hook — removes snapshots written before [epochMillis]. */
    @Query("DELETE FROM meeting_calendar_cache WHERE fetchedAt < :epochMillis")
    suspend fun deleteOlderThan(epochMillis: Long)
}
