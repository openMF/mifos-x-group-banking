/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.database.memberlist.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow
import kpt.core.database.memberlist.entity.MemberListEntity

/**
 * Room DAO for the paginated member-list cache (SourceOfTruth for the PAGINATED
 * NETWORK_WITH_CACHE `MembersPagingStore`).
 *
 * [observePage] is the reactive per-(group, page) read the store's SourceOfTruth reader subscribes
 * to — the store keys each page independently by (groupId carried in
 * [kpt.core.base.store.paging.PageKey.query], pageIndex), so the cache is sliced by
 * [MemberListEntity.groupId] + [MemberListEntity.pageIndex]. [replacePage] is the store's
 * SourceOfTruth writer — a single `@Transaction` that atomically swaps ONE page's rows
 * (delete-page + upsert-page in one transaction) so a re-fetched page never leaves stale rows AND
 * readers never observe a half-written intermediate (guards RULE-IMPLEMENT-STORE5-001 S5-3 /
 * S5-PAGE-ATOMIC — the delete-then-upsert pagination race).
 *
 * See API.md#stores — MemberList.
 */
@Dao
interface MemberListDao {

    /** Reactive read of one cached page of a group in server order. Backs the store SoT reader. */
    @Query(
        "SELECT * FROM member_list_cache WHERE groupId = :groupId AND pageIndex = :pageIndex " +
            "ORDER BY rowOrder ASC",
    )
    fun observePage(groupId: String, pageIndex: Int): Flow<List<MemberListEntity>>

    /** Reactive read of one group's whole cache in (page, row) order — diagnostics / whole-list reads. */
    @Query(
        "SELECT * FROM member_list_cache WHERE groupId = :groupId " +
            "ORDER BY pageIndex ASC, rowOrder ASC",
    )
    fun observeGroup(groupId: String): Flow<List<MemberListEntity>>

    @Upsert
    suspend fun upsertAll(entities: List<MemberListEntity>)

    @Query("DELETE FROM member_list_cache WHERE groupId = :groupId AND pageIndex = :pageIndex")
    suspend fun deletePage(groupId: String, pageIndex: Int)

    @Query("DELETE FROM member_list_cache WHERE groupId = :groupId")
    suspend fun deleteGroup(groupId: String)

    @Query("DELETE FROM member_list_cache")
    suspend fun deleteAll()

    /**
     * Atomically replace ONE cached page of a group. The store SoT writer calls this so the page's
     * delete + insert happen in a single transaction — no reader ever sees an empty or
     * partially-written page mid-refresh (S5-PAGE-ATOMIC).
     */
    @Transaction
    suspend fun replacePage(groupId: String, pageIndex: Int, entities: List<MemberListEntity>) {
        deletePage(groupId, pageIndex)
        upsertAll(entities)
    }

    /** TTL pruning hook — removes rows written before [epochMillis]. */
    @Query("DELETE FROM member_list_cache WHERE fetchedAt < :epochMillis")
    suspend fun deleteOlderThan(epochMillis: Long)
}
