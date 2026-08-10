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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kpt.core.database.syncqueue.dao.SyncQueueDao
import kpt.core.database.syncqueue.entity.SyncQueueEntity
import kpt.core.model.EntityType
import kpt.core.model.SyncStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * In-memory [SyncQueueDao] fake — reimplements the atomic status-transition + FIFO-read semantics
 * over a single reactive [MutableStateFlow] of rows so the repository's mapping / delegation /
 * `combine` logic can be asserted without a Room driver (core/data commonTest has none). Mirrors
 * the `FakeMemberAddApi` convention in [MemberAddRepositoryTest].
 */
private class FakeSyncQueueDao : SyncQueueDao {
    private val rows = MutableStateFlow<List<SyncQueueEntity>>(emptyList())
    private var nextId = 1L

    override suspend fun insert(entity: SyncQueueEntity): Long {
        val id = nextId++
        rows.value = rows.value + entity.copy(id = id)
        return id
    }

    override fun observePending(): Flow<List<SyncQueueEntity>> =
        rows.map { list -> list.filter { it.status == "PENDING" }.sortedBy { it.createdAtEpochMs } }

    override fun observeByStatus(status: String): Flow<List<SyncQueueEntity>> =
        rows.map { list -> list.filter { it.status == status }.sortedBy { it.createdAtEpochMs } }

    override fun observeAll(): Flow<List<SyncQueueEntity>> =
        rows.map { list -> list.sortedBy { it.createdAtEpochMs } }

    override suspend fun getById(id: Long): SyncQueueEntity? = rows.value.firstOrNull { it.id == id }

    override fun countByStatus(status: String): Flow<Int> =
        rows.map { list -> list.count { it.status == status } }

    override suspend fun countByStatusOnce(status: String): Int =
        rows.value.count { it.status == status }

    override suspend fun markSyncing(id: Long, attemptEpochMs: Long) = update(id) {
        it.copy(status = "SYNCING", lastAttemptEpochMs = attemptEpochMs, attemptCount = it.attemptCount + 1)
    }

    override suspend fun markSynced(id: Long) = update(id) { it.copy(status = "SYNCED") }

    override suspend fun markFailed(id: Long, attemptEpochMs: Long, error: String?) = update(id) {
        it.copy(status = "FAILED", lastAttemptEpochMs = attemptEpochMs, lastError = error)
    }

    override suspend fun retryAllFailed() {
        rows.value = rows.value.map {
            if (it.status == "FAILED") it.copy(status = "PENDING", lastError = null) else it
        }
    }

    override suspend fun deleteById(id: Long) {
        rows.value = rows.value.filterNot { it.id == id }
    }

    override suspend fun deleteSynced() {
        rows.value = rows.value.filterNot { it.status == "SYNCED" }
    }

    override suspend fun deleteAll() {
        rows.value = emptyList()
    }

    private inline fun update(id: Long, transform: (SyncQueueEntity) -> SyncQueueEntity) {
        rows.value = rows.value.map { if (it.id == id) transform(it) else it }
    }
}

/**
 * TDD coverage for [SyncQueueRepository] / [SyncQueueRepositoryImpl] — the shared offline
 * write-queue seam consumed by member-add / loan-request (enqueue) and the sync-status feature
 * (observe). Verifies field mapping, the injected clock, entity->domain projection, `combine`-based
 * counts, and status-transition delegation. Not a Store5 store — no `org.mobilenativefoundation.store`.
 */
class SyncQueueRepositoryTest {

    private fun repo(dao: FakeSyncQueueDao, now: Long = 500L) =
        SyncQueueRepositoryImpl(dao = dao, nowMs = { now })

    @Test
    fun enqueueInsertsPendingRowWithClockTimestampAndReturnsId() = runTest {
        val dao = FakeSyncQueueDao()
        val repo = repo(dao, now = 12345L)

        val id = repo.enqueue(
            operationType = "LOAN_REQUEST",
            targetTable = "dt_loan_request",
            payloadJson = """{"amount":500}""",
        )

        assertEquals(1L, id)
        val item = repo.observePending().first().single()
        assertEquals("LOAN_REQUEST", item.operationType)
        assertEquals("dt_loan_request", item.targetTable)
        assertEquals("""{"amount":500}""", item.payloadJson)
        assertEquals(SyncStatus.PENDING, item.status)
        assertEquals(12345L, item.createdAtEpochMs)
        assertEquals(0, item.attemptCount)
        assertNull(item.lastError)
    }

    @Test
    fun observePendingMapsEntitiesToDomainAndExcludesNonPending() = runTest {
        val dao = FakeSyncQueueDao()
        val repo = repo(dao)
        val a = repo.enqueue("CREATE_MEMBER", "dt_member_role", "{}")
        repo.enqueue("LOAN_REQUEST", "dt_loan_request", "{}")

        repo.markSyncing(a)
        repo.markSynced(a)

        val pending = repo.observePending().first()
        assertEquals(1, pending.size)
        assertEquals("LOAN_REQUEST", pending.single().operationType)
    }

    @Test
    fun observeCountsCombinesEachStatusBucketForSyncStatusFeature() = runTest {
        val dao = FakeSyncQueueDao()
        val repo = repo(dao)
        val a = repo.enqueue("CREATE_MEMBER", "dt_member_role", "{}")
        val b = repo.enqueue("LOAN_REQUEST", "dt_loan_request", "{}")
        repo.enqueue("CREATE_MEMBER", "dt_member_role", "{}")

        repo.markSyncing(a)
        repo.markSynced(a)
        repo.markFailed(b, error = "timeout")

        val counts = repo.observeCounts().first()
        assertEquals(1, counts.pending)
        assertEquals(0, counts.syncing)
        assertEquals(1, counts.failed)
        assertEquals(1, counts.synced)
    }

    @Test
    fun markSyncingStampsAttemptTimeFromInjectedClockAndIncrementsAttemptCount() = runTest {
        val dao = FakeSyncQueueDao()
        val repo = repo(dao, now = 999L)
        val id = repo.enqueue("LOAN_REQUEST", "dt_loan_request", "{}")

        repo.markSyncing(id)

        val syncing = dao.observeByStatus("SYNCING").first().single()
        assertEquals(999L, syncing.lastAttemptEpochMs)
        assertEquals(1, syncing.attemptCount)
    }

    @Test
    fun markFailedRecordsErrorAndRetryAllRequeuesFailedToPending() = runTest {
        val dao = FakeSyncQueueDao()
        val repo = repo(dao)
        val id = repo.enqueue("LOAN_REQUEST", "dt_loan_request", "{}")
        repo.markSyncing(id)

        repo.markFailed(id, error = "HTTP 500")
        assertEquals(1, repo.observeCounts().first().failed)

        repo.retryAll()
        val pending = repo.observePending().first().single()
        assertEquals(SyncStatus.PENDING, pending.status)
        assertNull(pending.lastError)
    }

    // ---------- observePendingByType / observeFailed / observeConflictCount / getItem ----------

    @Test
    fun observePendingByTypeGroupsPendingRowsByClassifiedEntityType() = runTest {
        val dao = FakeSyncQueueDao()
        val repo = repo(dao)
        repo.enqueue("LOAN_REQUEST", "dt_loan_request", "{}")
        repo.enqueue("CREATE_MEMBER", "dt_member_role", "{}")
        val synced = repo.enqueue("CREATE_MEMBER", "dt_member_role", "{}")
        repo.markSyncing(synced)
        repo.markSynced(synced)

        val byType = repo.observePendingByType().first()

        assertEquals(1, byType[EntityType.LOAN])
        assertEquals(1, byType[EntityType.MEMBER])
    }

    @Test
    fun observeFailedReturnsOnlyFailedRowsInFifoOrder() = runTest {
        val dao = FakeSyncQueueDao()
        val repo = repo(dao)
        val a = repo.enqueue("LOAN_REQUEST", "dt_loan_request", "{}")
        repo.enqueue("CREATE_MEMBER", "dt_member_role", "{}")
        repo.markSyncing(a)
        repo.markFailed(a, error = "HTTP 500")

        val failed = repo.observeFailed().first()

        assertEquals(1, failed.size)
        assertEquals(a, failed.single().id)
        assertEquals("HTTP 500", failed.single().lastError)
    }

    @Test
    fun observeConflictCountIsAlwaysZeroDocumentedGap() = runTest {
        val dao = FakeSyncQueueDao()
        val repo = repo(dao)
        val a = repo.enqueue("LOAN_REQUEST", "dt_loan_request", "{}")
        repo.markSyncing(a)
        repo.markFailed(a, error = "409 conflict")

        // No CONFLICT SyncStatus bucket exists yet — see interface KDoc for the documented gap.
        assertEquals(0, repo.observeConflictCount().first())
    }

    @Test
    fun getItemReturnsMappedRowByIdOrNullWhenAbsent() = runTest {
        val dao = FakeSyncQueueDao()
        val repo = repo(dao)
        val id = repo.enqueue("LOAN_REQUEST", "dt_loan_request", "{}")

        val found = repo.getItem(id)
        val missing = repo.getItem(id + 999)

        assertEquals("LOAN_REQUEST", found?.operationType)
        assertNull(missing)
    }
}
