/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.database.loandetail.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow
import kpt.core.database.loandetail.entity.LoanDetailCacheEntity

/**
 * Room DAO for the loan-detail cache (SourceOfTruth for the single-key NETWORK_WITH_CACHE
 * [kpt.core.store.loandetail.impl.provideLoanDetailStore]).
 *
 * The store keys each loan-detail snapshot by [LoanDetailCacheEntity.loanId], so [observeByKey] is
 * the reactive per-loan read the store's SourceOfTruth reader subscribes to and [replaceForKey] is
 * the store's SourceOfTruth writer. Because a loan's whole detail snapshot is exactly ONE row, a
 * keyed `@Upsert` is inherently atomic — no reader ever observes a half-written snapshot (guards
 * RULE-IMPLEMENT-STORE5-001 S5-3, the delete-then-upsert race, by construction). [replaceForKey] is
 * a single `@Transaction` for symmetry with the member-dashboard / paging DAOs.
 *
 * See API.md#stores — LoanDetailResponse.
 */
@Dao
interface LoanDetailDao {

    /** Reactive read of one cached loan's detail snapshot. Backs the store SoT reader. */
    @Query("SELECT * FROM loan_detail_cache WHERE loanId = :loanId")
    fun observeByKey(loanId: Long): Flow<LoanDetailCacheEntity?>

    @Upsert
    suspend fun upsert(entity: LoanDetailCacheEntity)

    @Query("DELETE FROM loan_detail_cache WHERE loanId = :loanId")
    suspend fun deleteByKey(loanId: Long)

    @Query("DELETE FROM loan_detail_cache")
    suspend fun deleteAll()

    /**
     * Atomically replace ONE loan's cached detail snapshot. The store SoT writer calls this so the
     * write for a [LoanDetailCacheEntity.loanId] lands in a single transaction — no reader ever sees
     * a partially-written row mid-refresh (S5-3).
     */
    @Transaction
    suspend fun replaceForKey(entity: LoanDetailCacheEntity) {
        upsert(entity)
    }

    /** TTL pruning hook — removes snapshots written before [epochMillis]. */
    @Query("DELETE FROM loan_detail_cache WHERE fetchedAt < :epochMillis")
    suspend fun deleteOlderThan(epochMillis: Long)
}
