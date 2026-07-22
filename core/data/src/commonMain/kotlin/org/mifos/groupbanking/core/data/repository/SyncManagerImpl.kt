/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.data.repository

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.datastore.sync.SyncMetadataStore
import org.mifos.groupbanking.core.model.BatchSyncRequest
import org.mifos.groupbanking.core.model.BatchSyncResponseItem
import org.mifos.groupbanking.core.model.SyncQueueItem
import org.mifos.groupbanking.core.model.SyncResult
import org.mifos.groupbanking.core.network.mapper.toBatchOperation
import org.mifos.groupbanking.core.network.mapper.toDomainModels
import org.mifos.groupbanking.core.network.mapper.toDto
import org.mifos.groupbanking.core.network.mapper.toSyncResult
import org.mifos.groupbanking.core.network.service.batchsync.BatchSyncApi
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private const val TAG = "SyncManager"

/**
 * See [SyncManager] KDoc for the Store5-branch rationale (`sync-status`'s `data-flow.yaml`
 * declares every entry `cache.strategy: no_cache` — no Store5 read-stream is ever cached here) and
 * the drain contract. No try-catch (Mandatory Rule 4) — [BatchSyncApi] is the sole try-catch
 * boundary; this class only branches on the [NetworkResult] it returns.
 *
 * A `409` batch-response row is recorded as [org.mifos.groupbanking.core.model.SyncStatus.FAILED]
 * (with a `"Conflict (409): ..."` note) rather than a dedicated CONFLICT status — see
 * `SyncQueueRepository.observeConflictCount` KDoc for the documented schema gap; [SyncResult]
 * still folds it into [SyncResult.conflictCount] via `toSyncResult()` so the sync-status screen's
 * conflict dialog (`data-flow.yaml#error_paths.sync.conflict`) can distinguish it from a plain
 * failure.
 *
 * [now] is injected (defaulting to the real clock, `kotlin.time.Clock` — `kotlinx.datetime.Clock`
 * is deprecated) so tests can pin the `lastSyncAt` stamp deterministically; the Koin binding uses
 * the default.
 *
 * See API.md#repositories — SyncManager.
 */
@OptIn(ExperimentalTime::class)
class SyncManagerImpl(
    private val syncQueueRepository: SyncQueueRepository,
    private val batchSyncApi: BatchSyncApi,
    private val syncMetadataStore: SyncMetadataStore,
    private val now: () -> Instant = { Clock.System.now() },
) : SyncManager {

    override fun triggerSync(): Flow<SyncResult> = flow {
        val pending = syncQueueRepository.observePending().first()
        Logger.d(TAG) { "triggerSync: snapshot has ${pending.size} pending row(s)" }

        if (pending.isEmpty()) {
            Logger.i(TAG) { "triggerSync: nothing to drain" }
            emit(SyncResult(successCount = 0, failedCount = 0, conflictCount = 0))
            return@flow
        }

        // requestId is the 1-based position of this row WITHIN this batch submission — not the
        // queue row's own generated id (see BatchSyncMappers.kt#toBatchOperation kdoc).
        val indexed: List<Pair<Int, SyncQueueItem>> = pending.mapIndexed { index, item -> (index + 1) to item }
        indexed.forEach { (_, item) -> syncQueueRepository.markSyncing(item.id) }

        val request = BatchSyncRequest(
            requests = indexed.map { (requestId, item) -> item.toBatchOperation(requestId = requestId) },
        )

        when (val result = batchSyncApi.batchSync(request.toDto())) {
            is NetworkResult.Success -> {
                val responseItems = result.data.toDomainModels()
                applyResponse(indexed, responseItems)
                val syncResult = responseItems.toSyncResult()
                syncMetadataStore.recordSyncCompleted(now())
                Logger.i(TAG) {
                    "triggerSync: drained ${pending.size} row(s) -> " +
                        "success=${syncResult.successCount} failed=${syncResult.failedCount} " +
                        "conflict=${syncResult.conflictCount}"
                }
                emit(syncResult)
            }
            is NetworkResult.Error -> {
                Logger.e(TAG) { "triggerSync: batchSync transport failure: ${result.error}" }
                pending.forEach { syncQueueRepository.markFailed(it.id, "Network error: ${result.error}") }
                emit(SyncResult(successCount = 0, failedCount = pending.size, conflictCount = 0))
            }
        }
    }

    override fun getLastSyncAt(): Flow<Instant?> = syncMetadataStore.lastSyncAt

    override suspend fun retryItem(itemId: Long): SyncResult {
        val item = syncQueueRepository.getItem(itemId)
        if (item == null) {
            Logger.i(TAG) { "retryItem: itemId=$itemId not found (already pruned?) — no-op" }
            return SyncResult(successCount = 0, failedCount = 0, conflictCount = 0)
        }

        Logger.d(TAG) { "retryItem: itemId=$itemId (${item.operationType})" }
        syncQueueRepository.markSyncing(item.id)

        val requestId = 1
        val request = BatchSyncRequest(requests = listOf(item.toBatchOperation(requestId = requestId)))

        return when (val result = batchSyncApi.batchSync(request.toDto())) {
            is NetworkResult.Success -> {
                val responseItems = result.data.toDomainModels()
                applyResponse(listOf(requestId to item), responseItems)
                val syncResult = responseItems.toSyncResult()
                Logger.i(TAG) { "retryItem: itemId=$itemId -> $syncResult" }
                syncResult
            }
            is NetworkResult.Error -> {
                Logger.e(TAG) { "retryItem: itemId=$itemId transport failure: ${result.error}" }
                syncQueueRepository.markFailed(item.id, "Network error: ${result.error}")
                SyncResult(successCount = 0, failedCount = 1, conflictCount = 0)
            }
        }
    }

    /**
     * Correlates every [responseItems] row back to its originating [SyncQueueItem] by `requestId`
     * (via [indexed]) and marks the row SYNCED (2xx) or FAILED (409 conflict / anything else) —
     * see class KDoc for why 409 has no dedicated status bucket. A response row whose `requestId`
     * has no match in [indexed] is ignored (defensive — the Batch API always echoes every
     * submitted row, so this should never happen in practice).
     */
    private suspend fun applyResponse(
        indexed: List<Pair<Int, SyncQueueItem>>,
        responseItems: List<BatchSyncResponseItem>,
    ) {
        val itemByRequestId = indexed.toMap()
        responseItems.forEach { responseItem ->
            val queueItem = itemByRequestId[responseItem.requestId] ?: return@forEach
            when {
                responseItem.statusCode in 200..299 -> syncQueueRepository.markSynced(queueItem.id)
                responseItem.statusCode == 409 -> syncQueueRepository.markFailed(
                    queueItem.id,
                    "Conflict (409): ${responseItem.body}",
                )
                else -> syncQueueRepository.markFailed(
                    queueItem.id,
                    "HTTP ${responseItem.statusCode}: ${responseItem.body}",
                )
            }
        }
    }
}
