/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.database.syncqueue.dao

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kpt.core.database.AppDatabase
import kpt.core.database.syncqueue.entity.SyncQueueEntity
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * In-memory Room coverage for [SyncQueueDao] — the persistence half of the shared offline
 * write-queue (`sync_queue` table). Mirrors the framework `FetchedAtDaoTest` in-memory pattern.
 *
 * Locks the queue contract the mutation features (member-add, loan-request) and the sync-status
 * feature depend on: enqueue makes a PENDING row observable, the atomic status transitions move a
 * row between buckets, and the per-status counts stay consistent for the sync-status badges. This
 * is a WRITE-QUEUE DAO — no Store5 wiring anywhere.
 */
class SyncQueueDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: SyncQueueDao

    private fun pendingRow(operationType: String, targetTable: String, createdAt: Long) =
        SyncQueueEntity(
            operationType = operationType,
            payloadJson = """{"op":"$operationType"}""",
            targetTable = targetTable,
            status = "PENDING",
            createdAtEpochMs = createdAt,
        )

    @BeforeTest
    fun setup() {
        database = Room.inMemoryDatabaseBuilder<AppDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        dao = database.syncQueueDao
    }

    @AfterTest
    fun teardown() {
        database.close()
    }

    @Test
    fun insertReturnsIdAndObservePendingEmitsRowInFifoOrder() = runTest {
        val id1 = dao.insert(pendingRow("CREATE_MEMBER", "dt_member_role", createdAt = 100L))
        val id2 = dao.insert(pendingRow("LOAN_REQUEST", "dt_loan_request", createdAt = 200L))

        assertTrue(id1 > 0)
        assertTrue(id2 > id1)

        val pending = dao.observePending().first()
        assertEquals(2, pending.size)
        assertEquals("CREATE_MEMBER", pending[0].operationType)
        assertEquals("LOAN_REQUEST", pending[1].operationType)
    }

    @Test
    fun markSyncingThenMarkSyncedTransitionsRowOutOfPendingAndCountsAttempt() = runTest {
        val id = dao.insert(pendingRow("CREATE_MEMBER", "dt_member_role", createdAt = 100L))

        dao.markSyncing(id, attemptEpochMs = 111L)
        assertTrue(dao.observePending().first().isEmpty())
        val syncing = dao.observeByStatus("SYNCING").first().single()
        assertEquals(1, syncing.attemptCount)
        assertEquals(111L, syncing.lastAttemptEpochMs)

        dao.markSynced(id)
        assertEquals(1, dao.countByStatusOnce("SYNCED"))
        assertEquals(0, dao.countByStatusOnce("SYNCING"))
    }

    @Test
    fun markFailedRecordsErrorAndRetryAllRequeuesToPending() = runTest {
        val id = dao.insert(pendingRow("LOAN_REQUEST", "dt_loan_request", createdAt = 100L))
        dao.markSyncing(id, attemptEpochMs = 111L)

        dao.markFailed(id, attemptEpochMs = 222L, error = "HTTP 500")
        val failed = dao.observeByStatus("FAILED").first().single()
        assertEquals("HTTP 500", failed.lastError)
        assertEquals(222L, failed.lastAttemptEpochMs)
        assertEquals(1, failed.attemptCount)

        dao.retryAllFailed()
        val pending = dao.observePending().first().single()
        assertEquals(id, pending.id)
        assertEquals(null, pending.lastError)
    }

    @Test
    fun countByStatusReflectsEachBucketForSyncStatusFeature() = runTest {
        val a = dao.insert(pendingRow("CREATE_MEMBER", "dt_member_role", createdAt = 100L))
        val b = dao.insert(pendingRow("LOAN_REQUEST", "dt_loan_request", createdAt = 200L))
        dao.insert(pendingRow("CREATE_MEMBER", "dt_member_role", createdAt = 300L))

        dao.markSyncing(a, attemptEpochMs = 111L)
        dao.markSynced(a)
        dao.markSyncing(b, attemptEpochMs = 222L)
        dao.markFailed(b, attemptEpochMs = 333L, error = "timeout")

        assertEquals(1, dao.countByStatus("PENDING").first())
        assertEquals(1, dao.countByStatus("FAILED").first())
        assertEquals(1, dao.countByStatus("SYNCED").first())
        assertEquals(0, dao.countByStatus("SYNCING").first())
    }

    @Test
    fun deleteByIdAndDeleteSyncedPruneRows() = runTest {
        val a = dao.insert(pendingRow("CREATE_MEMBER", "dt_member_role", createdAt = 100L))
        val b = dao.insert(pendingRow("LOAN_REQUEST", "dt_loan_request", createdAt = 200L))

        dao.deleteById(a)
        assertEquals(1, dao.observeAll().first().size)

        dao.markSyncing(b, attemptEpochMs = 222L)
        dao.markSynced(b)
        dao.deleteSynced()
        assertTrue(dao.observeAll().first().isEmpty())
    }

    @Test
    fun getByIdReturnsRowOrNullForSyncStatusRetryItem() = runTest {
        val id = dao.insert(pendingRow("LOAN_REQUEST", "dt_loan_request", createdAt = 100L))

        val found = dao.getById(id)
        val missing = dao.getById(id + 999)

        assertEquals("LOAN_REQUEST", found?.operationType)
        assertEquals(null, missing)
    }
}
