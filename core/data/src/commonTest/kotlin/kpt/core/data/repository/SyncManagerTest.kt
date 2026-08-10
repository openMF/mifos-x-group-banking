/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.model.EntityType
import kpt.core.model.SyncQueueCounts
import kpt.core.model.SyncQueueItem
import kpt.core.model.SyncStatus
import kpt.core.network.model.BatchSyncRequestDto
import kpt.core.network.model.BatchSyncResponseItemDto
import kpt.core.network.service.batchsync.BatchSyncApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** In-memory [SyncQueueRepository] fake — tracks every mark* call for behavior assertions. */
private class FakeSyncQueueRepository(initial: List<SyncQueueItem> = emptyList()) : SyncQueueRepository {
    private val rows = MutableStateFlow(initial)

    val markSyncingCalls = mutableListOf<Long>()
    val markSyncedCalls = mutableListOf<Long>()
    val markFailedCalls = mutableListOf<Pair<Long, String?>>()

    override suspend fun enqueue(operationType: String, targetTable: String, payloadJson: String): Long {
        error("not used by SyncManagerImpl")
    }

    override fun observePending(): Flow<List<SyncQueueItem>> =
        rows.asStateFlow()

    override fun observeCounts(): Flow<SyncQueueCounts> = error("not used by SyncManagerImpl")

    override suspend fun markSyncing(id: Long) {
        markSyncingCalls += id
        update(id) { it.copy(status = SyncStatus.SYNCING) }
    }

    override suspend fun markSynced(id: Long) {
        markSyncedCalls += id
        update(id) { it.copy(status = SyncStatus.SYNCED) }
    }

    override suspend fun markFailed(id: Long, error: String?) {
        markFailedCalls += id to error
        update(id) { it.copy(status = SyncStatus.FAILED, lastError = error) }
    }

    override suspend fun retryAll() {
        error("not used by SyncManagerImpl")
    }

    override fun observePendingByType(): Flow<Map<EntityType, Int>> = error("not used by SyncManagerImpl")

    override fun observeFailed(): Flow<List<SyncQueueItem>> = error("not used by SyncManagerImpl")

    override fun observeConflictCount(): Flow<Int> = error("not used by SyncManagerImpl")

    override suspend fun getItem(id: Long): SyncQueueItem? = rows.value.firstOrNull { it.id == id }

    private fun update(id: Long, transform: (SyncQueueItem) -> SyncQueueItem) {
        rows.value = rows.value.map { if (it.id == id) transform(it) else it }
    }
}

/** [BatchSyncApi] fake — captures the submitted request and returns a caller-scripted result. */
private class FakeBatchSyncApi(
    private val result: (BatchSyncRequestDto) -> NetworkResult<List<BatchSyncResponseItemDto>, NetworkError>,
) : BatchSyncApi {
    var lastRequest: BatchSyncRequestDto? = null
        private set
    var callCount: Int = 0
        private set

    override suspend fun batchSync(
        request: BatchSyncRequestDto,
    ): NetworkResult<List<BatchSyncResponseItemDto>, NetworkError> {
        lastRequest = request
        callCount++
        return result(request)
    }
}

/** In-memory [kpt.core.datastore.sync.SyncMetadataStore] fake. */
@OptIn(ExperimentalTime::class)
private class FakeSyncMetadataStore : kpt.core.datastore.sync.SyncMetadataStore {
    private val _lastSyncAt = MutableStateFlow<Instant?>(null)
    override val lastSyncAt: Flow<Instant?> = _lastSyncAt.asStateFlow()
    override suspend fun recordSyncCompleted(at: Instant) {
        _lastSyncAt.value = at
    }
}

private fun queueItem(id: Long, operationType: String = "LOAN_REQUEST", targetTable: String = "dt_loan_request") =
    SyncQueueItem(
        id = id,
        operationType = operationType,
        payloadJson = """{"id":$id}""",
        targetTable = targetTable,
        status = SyncStatus.PENDING,
        createdAtEpochMs = 1_700_000_000_000L + id,
        lastAttemptEpochMs = null,
        attemptCount = 0,
        lastError = null,
    )

/**
 * TDD RED-first coverage for [SyncManager] / [SyncManagerImpl] — the sync-status feature's drain
 * coordinator. Verifies the empty-backlog short-circuit, the full success/mixed/network-error
 * drain paths (mark-before-submit, correlate-by-requestId, lastSyncAt stamping), and the
 * single-item retry path. See API.md#repositories.
 */
@OptIn(ExperimentalTime::class)
class SyncManagerTest {

    private fun manager(
        queueRepo: FakeSyncQueueRepository,
        api: FakeBatchSyncApi,
        metadataStore: FakeSyncMetadataStore = FakeSyncMetadataStore(),
        now: () -> Instant = { Instant.parse("2026-07-22T10:00:00Z") },
    ) = SyncManagerImpl(
        syncQueueRepository = queueRepo,
        batchSyncApi = api,
        syncMetadataStore = metadataStore,
        now = now,
    ) to metadataStore

    // ---------- triggerSync ----------

    @Test
    fun triggerSync_emptyBacklog_emitsZeroResultWithNoNetworkCall() = runTest {
        val queueRepo = FakeSyncQueueRepository(emptyList())
        val api = FakeBatchSyncApi { error("must not be called for an empty backlog") }
        val (manager, _) = manager(queueRepo, api)

        val result = manager.triggerSync().first()

        assertEquals(0, result.successCount)
        assertEquals(0, result.failedCount)
        assertEquals(0, result.conflictCount)
        assertEquals(0, api.callCount)
    }

    @Test
    fun triggerSync_marksEveryPendingRowSyncingBeforeSubmitting() = runTest {
        val queueRepo = FakeSyncQueueRepository(listOf(queueItem(1), queueItem(2)))
        val api = FakeBatchSyncApi {
            NetworkResult.Success(
                listOf(
                    BatchSyncResponseItemDto(1, 200, "{}"),
                    BatchSyncResponseItemDto(2, 200, "{}"),
                ),
            )
        }
        val (manager, _) = manager(queueRepo, api)

        manager.triggerSync().first()

        assertEquals(listOf(1L, 2L), queueRepo.markSyncingCalls)
    }

    @Test
    fun triggerSync_allSuccess_marksEverySyncedAndStampsLastSyncAt() = runTest {
        val queueRepo = FakeSyncQueueRepository(listOf(queueItem(1), queueItem(2)))
        val api = FakeBatchSyncApi {
            NetworkResult.Success(
                listOf(
                    BatchSyncResponseItemDto(1, 200, "{}"),
                    BatchSyncResponseItemDto(2, 201, "{}"),
                ),
            )
        }
        val at = Instant.parse("2026-07-22T10:00:00Z")
        val (manager, metadataStore) = manager(queueRepo, api, now = { at })

        val result = manager.triggerSync().first()

        assertEquals(2, result.successCount)
        assertEquals(0, result.failedCount)
        assertEquals(0, result.conflictCount)
        assertEquals(listOf(1L, 2L), queueRepo.markSyncedCalls.sorted())
        assertTrue(queueRepo.markFailedCalls.isEmpty())
        assertEquals(at, metadataStore.lastSyncAt.first())
    }

    @Test
    fun triggerSync_mixedStatuses_marksSuccessConflictAndFailureAccordingly() = runTest {
        val queueRepo = FakeSyncQueueRepository(listOf(queueItem(1), queueItem(2), queueItem(3)))
        val api = FakeBatchSyncApi {
            NetworkResult.Success(
                listOf(
                    BatchSyncResponseItemDto(1, 200, "{}"),
                    BatchSyncResponseItemDto(2, 409, """{"errors":[{"developerMessage":"duplicate"}]}"""),
                    BatchSyncResponseItemDto(3, 500, """{"error":"boom"}"""),
                ),
            )
        }
        val (manager, _) = manager(queueRepo, api)

        val result = manager.triggerSync().first()

        assertEquals(1, result.successCount)
        assertEquals(1, result.conflictCount)
        assertEquals(1, result.failedCount)
        assertEquals(listOf(1L), queueRepo.markSyncedCalls)
        val failedIds = queueRepo.markFailedCalls.map { it.first }.sorted()
        assertEquals(listOf(2L, 3L), failedIds)
        // 409 row is recorded as FAILED (no dedicated CONFLICT SyncStatus) with a note.
        val conflictNote = queueRepo.markFailedCalls.first { it.first == 2L }.second
        assertTrue(conflictNote?.contains("409") == true)
    }

    @Test
    fun triggerSync_networkError_marksEveryRowFailedAndEmitsAllFailedResult() = runTest {
        val queueRepo = FakeSyncQueueRepository(listOf(queueItem(1), queueItem(2)))
        val api = FakeBatchSyncApi { NetworkResult.Error(NetworkError.SERVER) }
        val (manager, metadataStore) = manager(queueRepo, api)

        val result = manager.triggerSync().first()

        assertEquals(0, result.successCount)
        assertEquals(2, result.failedCount)
        assertEquals(0, result.conflictCount)
        assertEquals(listOf(1L, 2L), queueRepo.markFailedCalls.map { it.first }.sorted())
        // No successful drain -> lastSyncAt is untouched.
        assertNull(metadataStore.lastSyncAt.first())
    }

    // ---------- getLastSyncAt ----------

    @Test
    fun getLastSyncAt_delegatesToTheMetadataStore() = runTest {
        val queueRepo = FakeSyncQueueRepository(emptyList())
        val api = FakeBatchSyncApi { NetworkResult.Success(emptyList()) }
        val metadataStore = FakeSyncMetadataStore()
        val at = Instant.parse("2026-07-20T08:00:00Z")
        metadataStore.recordSyncCompleted(at)
        val manager = SyncManagerImpl(
            syncQueueRepository = queueRepo,
            batchSyncApi = api,
            syncMetadataStore = metadataStore,
        )

        assertEquals(at, manager.getLastSyncAt().first())
    }

    // ---------- retryItem ----------

    @Test
    fun retryItem_success_marksSyncedAndReturnsSingleRowResult() = runTest {
        val queueRepo = FakeSyncQueueRepository(listOf(queueItem(7)))
        val api = FakeBatchSyncApi { NetworkResult.Success(listOf(BatchSyncResponseItemDto(1, 200, "{}"))) }
        val (manager, _) = manager(queueRepo, api)

        val result = manager.retryItem(7L)

        assertEquals(1, result.successCount)
        assertEquals(0, result.failedCount)
        assertEquals(listOf(7L), queueRepo.markSyncingCalls)
        assertEquals(listOf(7L), queueRepo.markSyncedCalls)
        assertEquals(1, api.lastRequest?.requests?.size)
    }

    @Test
    fun retryItem_failureResponse_marksFailedAndReturnsResult() = runTest {
        val queueRepo = FakeSyncQueueRepository(listOf(queueItem(7)))
        val api = FakeBatchSyncApi { NetworkResult.Success(listOf(BatchSyncResponseItemDto(1, 500, "{}"))) }
        val (manager, _) = manager(queueRepo, api)

        val result = manager.retryItem(7L)

        assertEquals(0, result.successCount)
        assertEquals(1, result.failedCount)
        assertEquals(listOf(7L), queueRepo.markFailedCalls.map { it.first })
    }

    @Test
    fun retryItem_networkError_marksFailedAndReturnsResult() = runTest {
        val queueRepo = FakeSyncQueueRepository(listOf(queueItem(7)))
        val api = FakeBatchSyncApi { NetworkResult.Error(NetworkError.SERVER) }
        val (manager, _) = manager(queueRepo, api)

        val result = manager.retryItem(7L)

        assertEquals(0, result.successCount)
        assertEquals(1, result.failedCount)
        assertEquals(listOf(7L), queueRepo.markFailedCalls.map { it.first })
    }

    @Test
    fun retryItem_itemNotFound_returnsZeroResultAndNeverCallsTheApi() = runTest {
        val queueRepo = FakeSyncQueueRepository(emptyList())
        val api = FakeBatchSyncApi { error("must not be called when the item is absent") }
        val (manager, _) = manager(queueRepo, api)

        val result = manager.retryItem(999L)

        assertEquals(0, result.successCount)
        assertEquals(0, result.failedCount)
        assertEquals(0, result.conflictCount)
        assertEquals(0, api.callCount)
    }
}
