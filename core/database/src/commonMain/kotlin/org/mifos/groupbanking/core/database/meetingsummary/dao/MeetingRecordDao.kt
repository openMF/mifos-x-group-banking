/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.database.meetingsummary.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow
import org.mifos.groupbanking.core.database.meetingsummary.entity.MeetingRecordCacheEntity

/**
 * Room DAO for the meeting-record cache (SourceOfTruth for the single-key NETWORK_WITH_CACHE
 * [org.mifos.groupbanking.core.store.meetingsummary.impl.provideMeetingSummaryStore]).
 *
 * The store keys each meeting-summary snapshot by [MeetingRecordCacheEntity.cacheKey]
 * (`"$centerId:$meetingNumber"`), so [observeByKey] is the reactive per-meeting read the store's
 * SourceOfTruth reader subscribes to and [replaceForKey] is the store's SourceOfTruth writer.
 * Because a meeting's whole summary is exactly ONE row, a keyed `@Upsert` is inherently atomic — no
 * reader ever observes a half-written snapshot (guards RULE-IMPLEMENT-STORE5-001 S5-3, the
 * delete-then-upsert race, by construction).
 *
 * See API.md#stores — MeetingSummaryData.
 */
@Dao
interface MeetingRecordDao {

    /** Reactive read of one cached meeting's summary snapshot. Backs the store SoT reader. */
    @Query("SELECT * FROM meeting_record_cache WHERE cacheKey = :cacheKey")
    fun observeByKey(cacheKey: String): Flow<MeetingRecordCacheEntity?>

    @Upsert
    suspend fun upsert(entity: MeetingRecordCacheEntity)

    @Query("DELETE FROM meeting_record_cache WHERE cacheKey = :cacheKey")
    suspend fun deleteByKey(cacheKey: String)

    @Query("DELETE FROM meeting_record_cache")
    suspend fun deleteAll()

    /**
     * Atomically replace ONE meeting's cached summary snapshot. The store SoT writer calls this so
     * the write for a [MeetingRecordCacheEntity.cacheKey] lands in a single transaction — no reader
     * ever sees a partially-written row mid-refresh (S5-3).
     */
    @Transaction
    suspend fun replaceForKey(entity: MeetingRecordCacheEntity) {
        upsert(entity)
    }

    /** TTL pruning hook — removes snapshots written before [epochMillis]. */
    @Query("DELETE FROM meeting_record_cache WHERE fetchedAt < :epochMillis")
    suspend fun deleteOlderThan(epochMillis: Long)
}
