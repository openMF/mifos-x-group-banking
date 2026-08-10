/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.database.organizerdashboard.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow
import kpt.core.database.organizerdashboard.entity.OrganizerDashboardCacheEntity

/**
 * Room DAO for the organizer-dashboard cache (SourceOfTruth for the single-key NETWORK_WITH_CACHE
 * [kpt.core.store.organizerdashboard.impl.provideOrganizerDashboardStore]).
 *
 * The store keys the aggregate snapshot by a constant sentinel ([OrganizerDashboardCacheEntity
 * .cacheKey]), so [observeByKey] is the reactive read the store's SourceOfTruth reader subscribes to
 * and [replaceForKey] is the store's SourceOfTruth writer. Because the whole dashboard is exactly ONE
 * row, a keyed `@Upsert` is inherently atomic — no reader ever observes a half-written snapshot
 * (guards RULE-IMPLEMENT-STORE5-001 S5-3 by construction). [replaceForKey] is a single `@Transaction`
 * for symmetry with the field-officer / personal-dashboard DAOs.
 *
 * See API.md#stores — OrganizerDashboard.
 */
@Dao
interface OrganizerDashboardDao {

    /** Reactive read of the cached organizer dashboard snapshot. Backs the store SoT reader. */
    @Query("SELECT * FROM organizer_dashboard_cache WHERE cacheKey = :cacheKey")
    fun observeByKey(cacheKey: String): Flow<OrganizerDashboardCacheEntity?>

    @Upsert
    suspend fun upsert(entity: OrganizerDashboardCacheEntity)

    @Query("DELETE FROM organizer_dashboard_cache WHERE cacheKey = :cacheKey")
    suspend fun deleteByKey(cacheKey: String)

    @Query("DELETE FROM organizer_dashboard_cache")
    suspend fun deleteAll()

    /**
     * Atomically replace the cached organizer-dashboard snapshot. The store SoT writer calls this so
     * the write lands in a single transaction — no reader ever sees a partially-written row
     * mid-refresh (S5-3).
     */
    @Transaction
    suspend fun replaceForKey(entity: OrganizerDashboardCacheEntity) {
        upsert(entity)
    }

    /** TTL pruning hook — removes snapshots written before [epochMillis]. */
    @Query("DELETE FROM organizer_dashboard_cache WHERE fetchedAt < :epochMillis")
    suspend fun deleteOlderThan(epochMillis: Long)
}
