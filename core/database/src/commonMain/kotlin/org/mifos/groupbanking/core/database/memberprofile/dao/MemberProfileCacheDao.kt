/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.database.memberprofile.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow
import org.mifos.groupbanking.core.database.memberprofile.entity.MemberProfileCacheEntity

/**
 * Room DAO for the member-profile composite cache (SourceOfTruth for the composite dynamic-key
 * NETWORK_WITH_CACHE
 * [org.mifos.groupbanking.core.store.memberprofile.impl.provideMemberProfileStore]).
 *
 * The store keys each composite snapshot by client ([MemberProfileCacheEntity.clientId]), so
 * [observeByKey] is the reactive per-client read the store's SourceOfTruth reader subscribes to and
 * [replaceForKey] is the store's SourceOfTruth writer. Because a client's whole profile composite is
 * exactly ONE row, a keyed `@Upsert` is inherently atomic — no reader ever observes a half-written
 * snapshot (guards RULE-IMPLEMENT-STORE5-001 S5-3, the delete-then-upsert race, by construction).
 * [deleteByKey] is the store's SourceOfTruth delete — the role-update write's cache invalidation
 * (`data-flow.yaml#cache.strategy: invalidate`) routes through `store.clear(clientId)`, which calls
 * this, so the next stream collection re-fetches the profile with the new role.
 *
 * See API.md#stores — MemberProfile.
 */
@Dao
interface MemberProfileCacheDao {

    /** Reactive read of one cached client's composite profile snapshot. Backs the store SoT reader. */
    @Query("SELECT * FROM member_profile_cache WHERE clientId = :clientId")
    fun observeByKey(clientId: String): Flow<MemberProfileCacheEntity?>

    @Upsert
    suspend fun upsert(entity: MemberProfileCacheEntity)

    @Query("DELETE FROM member_profile_cache WHERE clientId = :clientId")
    suspend fun deleteByKey(clientId: String)

    @Query("DELETE FROM member_profile_cache")
    suspend fun deleteAll()

    /**
     * Atomically replace ONE client's cached composite snapshot. The store SoT writer calls this so
     * the write for a [MemberProfileCacheEntity.clientId] lands in a single transaction — no reader
     * ever sees a partially-written row mid-refresh (S5-3).
     */
    @Transaction
    suspend fun replaceForKey(entity: MemberProfileCacheEntity) {
        upsert(entity)
    }

    /** TTL pruning hook — removes snapshots written before [epochMillis]. */
    @Query("DELETE FROM member_profile_cache WHERE fetchedAt < :epochMillis")
    suspend fun deleteOlderThan(epochMillis: Long)
}
