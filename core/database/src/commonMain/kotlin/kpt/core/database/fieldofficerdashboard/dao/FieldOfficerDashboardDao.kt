/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.database.fieldofficerdashboard.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow
import kpt.core.database.fieldofficerdashboard.entity.FieldOfficerDashboardCacheEntity

/**
 * Room DAO for the field-officer-dashboard composite cache (SourceOfTruth for the composite
 * dynamic-key NETWORK_WITH_CACHE
 * [kpt.core.store.fieldofficerdashboard.impl.provideFieldOfficerDashboardStore]).
 *
 * The store keys each aggregate snapshot by staff ([FieldOfficerDashboardCacheEntity.staffKey]), so
 * [observeByKey] is the reactive per-staff read the store's SourceOfTruth reader subscribes to and
 * [replaceForKey] is the store's SourceOfTruth writer. Because a staff member's whole dashboard
 * aggregate is exactly ONE row, a keyed `@Upsert` is inherently atomic — no reader ever observes a
 * half-written snapshot (guards RULE-IMPLEMENT-STORE5-001 S5-3 by construction). [replaceForKey] is
 * a single `@Transaction` for symmetry with the group-dashboard / personal-dashboard DAOs.
 *
 * See API.md#stores — FieldOfficerDashboard.
 */
@Dao
interface FieldOfficerDashboardDao {

    /** Reactive read of one cached staff member's aggregate dashboard snapshot. Backs the store SoT reader. */
    @Query("SELECT * FROM field_officer_dashboard_cache WHERE staffKey = :staffKey")
    fun observeByKey(staffKey: String): Flow<FieldOfficerDashboardCacheEntity?>

    @Upsert
    suspend fun upsert(entity: FieldOfficerDashboardCacheEntity)

    @Query("DELETE FROM field_officer_dashboard_cache WHERE staffKey = :staffKey")
    suspend fun deleteByKey(staffKey: String)

    @Query("DELETE FROM field_officer_dashboard_cache")
    suspend fun deleteAll()

    /**
     * Atomically replace ONE staff member's cached aggregate snapshot. The store SoT writer calls
     * this so the write for a [FieldOfficerDashboardCacheEntity.staffKey] lands in a single
     * transaction — no reader ever sees a partially-written row mid-refresh (S5-3).
     */
    @Transaction
    suspend fun replaceForKey(entity: FieldOfficerDashboardCacheEntity) {
        upsert(entity)
    }

    /** TTL pruning hook — removes snapshots written before [epochMillis]. */
    @Query("DELETE FROM field_officer_dashboard_cache WHERE fetchedAt < :epochMillis")
    suspend fun deleteOlderThan(epochMillis: Long)
}
