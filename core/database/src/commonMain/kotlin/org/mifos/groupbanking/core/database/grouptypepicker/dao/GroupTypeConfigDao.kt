/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.database.grouptypepicker.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow
import org.mifos.groupbanking.core.database.grouptypepicker.entity.GroupTypeConfigEntity

/**
 * Room DAO for the seeded group-type catalogue cache (SourceOfTruth for
 * `GroupTypeConfigStore`, NETWORK_WITH_CACHE archetype).
 *
 * [observeAll] is the reactive read the store's SourceOfTruth reader subscribes to.
 * [replaceAll] is the store's SourceOfTruth writer — a single `@Transaction` that swaps the
 * whole catalogue atomically (delete-all + upsert-all in one transaction) so a shrinking
 * catalogue never leaves stale rows AND readers never observe a half-written intermediate
 * (guards RULE-IMPLEMENT-STORE5-001 S5-3 — the delete-then-upsert race).
 *
 * See API.md#stores — GroupTypeConfig.
 */
@Dao
interface GroupTypeConfigDao {

    /** Reactive read of the entire cached catalogue in insertion order. Backs the store SoT reader. */
    @Query("SELECT * FROM group_type_config")
    fun observeAll(): Flow<List<GroupTypeConfigEntity>>

    @Upsert
    suspend fun upsertAll(entities: List<GroupTypeConfigEntity>)

    @Query("DELETE FROM group_type_config")
    suspend fun deleteAll()

    /**
     * Atomically replace the whole cached catalogue. The store SoT writer calls this so the
     * delete + insert happen in a single transaction — no reader ever sees an empty or
     * partially-written table mid-refresh.
     */
    @Transaction
    suspend fun replaceAll(entities: List<GroupTypeConfigEntity>) {
        deleteAll()
        upsertAll(entities)
    }

    /** TTL pruning hook — removes rows written before [epochMillis]. */
    @Query("DELETE FROM group_type_config WHERE fetchedAt < :epochMillis")
    suspend fun deleteOlderThan(epochMillis: Long)
}
