/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.data.repository

import kotlinx.coroutines.flow.Flow
import org.mifos.groupbanking.core.model.EntityType
import org.mifos.groupbanking.core.model.SyncQueueCounts
import org.mifos.groupbanking.core.model.SyncQueueItem

/**
 * Shared offline write-queue repository — the seam every mutation feature enqueues into when the
 * device is offline, and the sync-status feature reads.
 *
 * **Not a Store5 read-store.** This is a Room-backed WRITE-QUEUE (`sync_queue` table via
 * `SyncQueueDao`); there is no `Fetcher` / `SourceOfTruth` / `.asScreenStream()` and nothing in
 * this stack imports `org.mobilenativefoundation.store`. It is the concrete backing for the seam
 * declared (but until now un-built) in `MemberAddRepository` / `MemberAddViewModel` /
 * `GroupCreateViewModel` KDocs: the ViewModel — informed by `NetworkMonitor` — calls [enqueue] as a
 * pre-flight when already offline (never calling the network repository at all), or enqueues a
 * retry when a transport-level error surfaces mid-chain (`data-flow.yaml#offline_behavior`
 * `strategy: queue_for_sync`).
 *
 * No try-catch envelopes (Mandatory Rule 4) — this is local Room, not network. Errors propagate;
 * the mapping helpers surface typed domain values ([SyncQueueItem] / [SyncQueueCounts]).
 *
 * See API.md#repositories — SyncQueueRepository.
 */
interface SyncQueueRepository {

    /**
     * Enqueue one offline write and return its generated row id.
     *
     * @param operationType the operation discriminator the sync worker keys replay off
     *                      (e.g. `"CREATE_MEMBER"`, `"LOAN_REQUEST"`).
     * @param targetTable   the Fineract datatable the replayed write lands on (e.g. `"dt_loan_request"`).
     * @param payloadJson   opaque serialized operation payload — the enqueuing feature owns
     *                      encode/decode; the queue never inspects it.
     */
    suspend fun enqueue(operationType: String, targetTable: String, payloadJson: String): Long

    /** Reactive pending backlog in FIFO order (the sync worker's input stream). */
    fun observePending(): Flow<List<SyncQueueItem>>

    /** Reactive per-status counts snapshot — backs the sync-status feature's badges. */
    fun observeCounts(): Flow<SyncQueueCounts>

    /** Mark a row as in-flight (SYNCING + attempt-count increment). */
    suspend fun markSyncing(id: Long)

    /** Mark a row as durably synced. */
    suspend fun markSynced(id: Long)

    /** Mark a row as failed, recording [error] for the sync-status UI. */
    suspend fun markFailed(id: Long, error: String?)

    /** Requeue every FAILED row back to PENDING — the retry-all action. */
    suspend fun retryAll()

    /**
     * Reactive pending backlog grouped by [EntityType] — backs the sync-status screen's
     * per-entity pending-count badges (`api.yaml#dependencies.repositories.SyncQueueRepository.getPendingByType`).
     * Derived from [observePending] via `SyncClassifier.kt#pendingByType` — no second DAO query.
     */
    fun observePendingByType(): Flow<Map<EntityType, Int>>

    /**
     * Reactive FAILED backlog in FIFO order — backs the sync-status screen's failed-operations
     * list (`api.yaml#dependencies.repositories.SyncQueueRepository.getFailedOperations`).
     */
    fun observeFailed(): Flow<List<SyncQueueItem>>

    /**
     * Reactive conflict-row count — backs the sync-status screen's conflict badge
     * (`api.yaml#dependencies.repositories.SyncQueueRepository.getConflictCount`). **Documented
     * gap**: [org.mifos.groupbanking.core.model.SyncStatus] has no dedicated `CONFLICT` bucket
     * today (a Fineract `409` batch-response row is recorded as [SyncStatus.FAILED] — see
     * `SyncManagerImpl` KDoc), so this always emits `0` until a `CONFLICT` status is added to the
     * schema. Kept as its own reactive stream (rather than removed) so the sync-status screen's
     * per-source read list matches `data-flow.yaml#local_sources` exactly today, and the future
     * schema change is a pure implementation swap with no interface break.
     */
    fun observeConflictCount(): Flow<Int>

    /**
     * One-shot lookup of a single queue row by id — backs the sync-status screen's per-item retry
     * (`SyncManagerImpl.retryItem`).
     */
    suspend fun getItem(id: Long): SyncQueueItem?
}
