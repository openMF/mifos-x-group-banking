/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.database.grouplist.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow
import org.mifos.groupbanking.core.database.grouplist.entity.GroupListEntity

/**
 * Room DAO for the paginated group-list cache (SourceOfTruth for the PAGINATED
 * NETWORK_WITH_CACHE `GroupsPagingStore`).
 *
 * [observePage] is the reactive per-page read the store's SourceOfTruth reader subscribes to —
 * the store keys each page independently ([kpt.core.base.store.paging.PageKey]), so the cache is
 * sliced by [GroupListEntity.pageIndex]. [replacePage] is the store's SourceOfTruth writer — a
 * single `@Transaction` that atomically swaps ONE page's rows (delete-page + upsert-page in one
 * transaction) so a re-fetched page never leaves stale rows AND readers never observe a
 * half-written intermediate (guards RULE-IMPLEMENT-STORE5-001 S5-3 / S5-PAGE-ATOMIC — the
 * delete-then-upsert pagination race).
 *
 * See API.md#stores — GroupList.
 */
@Dao
interface GroupListDao {

    /** Reactive read of one cached page in server order. Backs the store SoT reader. */
    @Query("SELECT * FROM group_list_cache WHERE pageIndex = :pageIndex ORDER BY rowOrder ASC")
    fun observePage(pageIndex: Int): Flow<List<GroupListEntity>>

    /** Reactive read of the whole cache in (page, row) order — diagnostics / whole-list reads. */
    @Query("SELECT * FROM group_list_cache ORDER BY pageIndex ASC, rowOrder ASC")
    fun observeAll(): Flow<List<GroupListEntity>>

    @Upsert
    suspend fun upsertAll(entities: List<GroupListEntity>)

    @Query("DELETE FROM group_list_cache WHERE pageIndex = :pageIndex")
    suspend fun deletePage(pageIndex: Int)

    @Query("DELETE FROM group_list_cache")
    suspend fun deleteAll()

    /**
     * Atomically replace ONE cached page. The store SoT writer calls this so the page's
     * delete + insert happen in a single transaction — no reader ever sees an empty or
     * partially-written page mid-refresh (S5-PAGE-ATOMIC).
     */
    @Transaction
    suspend fun replacePage(pageIndex: Int, entities: List<GroupListEntity>) {
        deletePage(pageIndex)
        upsertAll(entities)
    }

    /** TTL pruning hook — removes rows written before [epochMillis]. */
    @Query("DELETE FROM group_list_cache WHERE fetchedAt < :epochMillis")
    suspend fun deleteOlderThan(epochMillis: Long)
}
