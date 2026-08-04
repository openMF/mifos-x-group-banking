/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.model

/**
 * Lifecycle state of one queued offline write.
 *
 * - [PENDING]  enqueued, not yet attempted (the state the sync worker picks up).
 * - [SYNCING]  a sync attempt is in flight for this row.
 * - [FAILED]   the last sync attempt failed; carries a [SyncQueueItem.lastError] and is
 *              eligible for [SyncQueueRepository.retryAll].
 * - [SYNCED]   the write reached the server successfully; safe to prune.
 *
 * Persisted as the enum `.name` string in the `sync_queue` table so a value added later never
 * breaks the schema. This is a WRITE-QUEUE domain type — NOT a Store5 read-cache; nothing in this
 * stack imports `org.mobilenativefoundation.store`.
 *
 * See API.md#models — SyncQueue.
 */
enum class SyncStatus {
    PENDING,
    SYNCING,
    FAILED,
    SYNCED,
}

/**
 * Domain projection of one row in the offline write-queue (`sync_queue` table).
 *
 * Backs the shared offline-sync infra consumed by the mutation features (member-add's
 * `CREATE_MEMBER` create-chain, loan-request's `LOAN_REQUEST`) which enqueue a serialized payload
 * when offline, and read by the sync-status feature (via [SyncQueueRepository.observePending] /
 * [SyncQueueRepository.observeCounts]).
 *
 * [payloadJson] is the opaque serialized operation payload — the enqueuing feature owns its
 * encode/decode; the queue never inspects it. [targetTable] carries the replay route in one of TWO
 * conventions read by `BatchSyncMappers.toBatchOperation`: a bare Fineract datatable name (e.g.
 * `dt_loan_request`, replayed as `POST datatables/<name>`) OR a full companion route starting with
 * `/` (e.g. `/companion/groups/24/shareout/execute`, replayed verbatim) for orchestration writes
 * that are not a plain datatable row-append. [attemptCount] increments once per sync attempt
 * ([SyncQueueRepository.markSyncing]); [lastError] holds the most recent failure reason for the
 * sync-status UI.
 *
 * See API.md#models — SyncQueue.
 */
data class SyncQueueItem(
    val id: Long,
    val operationType: String,
    val payloadJson: String,
    val targetTable: String,
    val status: SyncStatus,
    val createdAtEpochMs: Long,
    val lastAttemptEpochMs: Long?,
    val attemptCount: Int,
    val lastError: String?,
)

/**
 * Aggregate per-status row counts of the offline write-queue, surfaced to the sync-status feature
 * as a single reactive snapshot (`SyncQueueRepository.observeCounts()`).
 *
 * See API.md#models — SyncQueue.
 */
data class SyncQueueCounts(
    val pending: Int,
    val syncing: Int,
    val failed: Int,
    val synced: Int,
)
