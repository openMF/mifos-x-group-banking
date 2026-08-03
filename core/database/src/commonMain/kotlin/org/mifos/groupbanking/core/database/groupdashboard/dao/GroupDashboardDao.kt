/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.database.groupdashboard.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow
import org.mifos.groupbanking.core.database.groupdashboard.entity.GroupDashboardCacheEntity

/**
 * Room DAO for the group-dashboard composite cache (SourceOfTruth for the composite dynamic-key
 * NETWORK_WITH_CACHE
 * [org.mifos.groupbanking.core.store.groupdashboard.impl.provideGroupDashboardStore]).
 *
 * The store keys each composite snapshot by group ([GroupDashboardCacheEntity.groupId]), so
 * [observeByKey] is the reactive per-group read the store's SourceOfTruth reader subscribes to and
 * [replaceForKey] is the store's SourceOfTruth writer. Because a group's whole dashboard composite
 * is exactly ONE row, a keyed `@Upsert` is inherently atomic — no reader ever observes a
 * half-written snapshot (guards RULE-IMPLEMENT-STORE5-001 S5-3, the delete-then-upsert race, by
 * construction). [replaceForKey] is a single `@Transaction` for symmetry with the
 * personal-dashboard / seed-catalogue / paging DAOs.
 *
 * See API.md#stores — GroupDashboard.
 */
@Dao
interface GroupDashboardDao {

    /** Reactive read of one cached group's composite dashboard snapshot. Backs the store SoT reader. */
    @Query("SELECT * FROM group_dashboard_cache WHERE groupId = :groupId")
    fun observeByKey(groupId: String): Flow<GroupDashboardCacheEntity?>

    @Upsert
    suspend fun upsert(entity: GroupDashboardCacheEntity)

    @Query("DELETE FROM group_dashboard_cache WHERE groupId = :groupId")
    suspend fun deleteByKey(groupId: String)

    @Query("DELETE FROM group_dashboard_cache")
    suspend fun deleteAll()

    /**
     * Atomically replace ONE group's cached composite snapshot. The store SoT writer calls this so
     * the write for a [GroupDashboardCacheEntity.groupId] lands in a single transaction — no reader
     * ever sees a partially-written row mid-refresh (S5-3).
     */
    @Transaction
    suspend fun replaceForKey(entity: GroupDashboardCacheEntity) {
        upsert(entity)
    }

    /** TTL pruning hook — removes snapshots written before [epochMillis]. */
    @Query("DELETE FROM group_dashboard_cache WHERE fetchedAt < :epochMillis")
    suspend fun deleteOlderThan(epochMillis: Long)
}
