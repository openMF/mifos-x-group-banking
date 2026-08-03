/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.database.syncqueue.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow
import org.mifos.groupbanking.core.database.syncqueue.entity.SyncQueueEntity

/**
 * Room DAO for the offline write-queue (`sync_queue` table) — the persistence half of the shared
 * `SyncQueueRepository` infra consumed by the mutation features (member-add, loan-request) and read
 * by the sync-status feature.
 *
 * This is a plain Room reactive DAO: [observePending] / [observeByStatus] / [countByStatus] return
 * Room `Flow`s that re-emit on every write (Room 3's own invalidation tracker), so callers never
 * poll. This is a WRITE-QUEUE DAO — no `SourceOfTruth` / Store5 wiring; nothing here imports
 * `org.mobilenativefoundation.store`.
 *
 * The status-transition writes ([markSyncing] / [markSynced] / [markFailed] / [retryAllFailed])
 * are single atomic `UPDATE` statements — the mutation state machine lives in SQL so a transition
 * can never observe a half-applied row. [markSyncing] increments [SyncQueueEntity.attemptCount]
 * once per attempt; [markFailed] records the reason without a second increment.
 *
 * See API.md#stores — SyncQueue (write-queue, non-Store5).
 */
@Dao
interface SyncQueueDao {

    /** Enqueue one write. Returns the auto-generated row id (the enqueuing feature's handle). */
    @Insert
    suspend fun insert(entity: SyncQueueEntity): Long

    /** Reactive read of the pending backlog in FIFO order — the sync worker subscribes to this. */
    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY createdAtEpochMs ASC")
    fun observePending(): Flow<List<SyncQueueEntity>>

    /** Reactive read of one status bucket in FIFO order (diagnostics / sync-status detail lists). */
    @Query("SELECT * FROM sync_queue WHERE status = :status ORDER BY createdAtEpochMs ASC")
    fun observeByStatus(status: String): Flow<List<SyncQueueEntity>>

    /** Reactive read of the whole queue in FIFO order. */
    @Query("SELECT * FROM sync_queue ORDER BY createdAtEpochMs ASC")
    fun observeAll(): Flow<List<SyncQueueEntity>>

    /** One-shot lookup of a single row by id — backs the sync-status feature's per-item retry. */
    @Query("SELECT * FROM sync_queue WHERE id = :id")
    suspend fun getById(id: Long): SyncQueueEntity?

    /** Reactive per-status row count — backs the sync-status feature's badge counters. */
    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = :status")
    fun countByStatus(status: String): Flow<Int>

    /** One-shot per-status count (non-reactive callers / assertions). */
    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = :status")
    suspend fun countByStatusOnce(status: String): Int

    /** Mark a row as in-flight: SYNCING + stamp attempt time + increment the attempt counter. */
    @Query(
        "UPDATE sync_queue SET status = 'SYNCING', lastAttemptEpochMs = :attemptEpochMs, " +
            "attemptCount = attemptCount + 1 WHERE id = :id",
    )
    suspend fun markSyncing(id: Long, attemptEpochMs: Long)

    /** Mark a row as durably synced (safe to prune via [deleteSynced]). */
    @Query("UPDATE sync_queue SET status = 'SYNCED' WHERE id = :id")
    suspend fun markSynced(id: Long)

    /** Mark a row as failed: FAILED + record the reason + stamp the attempt time. */
    @Query(
        "UPDATE sync_queue SET status = 'FAILED', lastAttemptEpochMs = :attemptEpochMs, " +
            "lastError = :error WHERE id = :id",
    )
    suspend fun markFailed(id: Long, attemptEpochMs: Long, error: String?)

    /** Requeue every FAILED row back to PENDING (clearing its last error) — the retry-all action. */
    @Query("UPDATE sync_queue SET status = 'PENDING', lastError = NULL WHERE status = 'FAILED'")
    suspend fun retryAllFailed()

    /** Remove one row by id (e.g. a synced write the caller chose to drop immediately). */
    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Prune all durably-synced rows. */
    @Query("DELETE FROM sync_queue WHERE status = 'SYNCED'")
    suspend fun deleteSynced()

    /** Wipe the whole queue (logout cache-clear). */
    @Query("DELETE FROM sync_queue")
    suspend fun deleteAll()
}
