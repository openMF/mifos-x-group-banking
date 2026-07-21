/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.database.personaldashboard.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow
import org.mifos.groupbanking.core.database.personaldashboard.entity.MemberDashboardCacheEntity

/**
 * Room DAO for the member-dashboard cache (SourceOfTruth for the dynamic-key NETWORK_WITH_CACHE
 * [org.mifos.groupbanking.core.store.personaldashboard.impl.provideMemberDashboardStore]).
 *
 * The store keys each dashboard snapshot by group ([MemberDashboardCacheEntity.cacheKey] =
 * `selectedGroupId ?: "__default__"`), so [observeByKey] is the reactive per-group read the
 * store's SourceOfTruth reader subscribes to and [replaceForKey] is the store's SourceOfTruth
 * writer. Because a group's whole dashboard is exactly ONE row, a keyed `@Upsert` is inherently
 * atomic — no reader ever observes a half-written snapshot (guards RULE-IMPLEMENT-STORE5-001
 * S5-3, the delete-then-upsert race, by construction). [replaceForKey] is a single `@Transaction`
 * for symmetry with the seed-catalogue / paging DAOs.
 *
 * See API.md#stores — MemberDashboard.
 */
@Dao
interface MemberDashboardDao {

    /** Reactive read of one cached group's dashboard snapshot. Backs the store SoT reader. */
    @Query("SELECT * FROM member_dashboard_cache WHERE cacheKey = :cacheKey")
    fun observeByKey(cacheKey: String): Flow<MemberDashboardCacheEntity?>

    @Upsert
    suspend fun upsert(entity: MemberDashboardCacheEntity)

    @Query("DELETE FROM member_dashboard_cache WHERE cacheKey = :cacheKey")
    suspend fun deleteByKey(cacheKey: String)

    @Query("DELETE FROM member_dashboard_cache")
    suspend fun deleteAll()

    /**
     * Atomically replace ONE group's cached dashboard snapshot. The store SoT writer calls this so
     * the write for a `cacheKey` lands in a single transaction — no reader ever sees a
     * partially-written row mid-refresh (S5-3).
     */
    @Transaction
    suspend fun replaceForKey(entity: MemberDashboardCacheEntity) {
        upsert(entity)
    }

    /** TTL pruning hook — removes snapshots written before [epochMillis]. */
    @Query("DELETE FROM member_dashboard_cache WHERE fetchedAt < :epochMillis")
    suspend fun deleteOlderThan(epochMillis: Long)
}
