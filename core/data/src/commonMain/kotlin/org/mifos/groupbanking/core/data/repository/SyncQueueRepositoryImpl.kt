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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.mifos.groupbanking.core.database.syncqueue.dao.SyncQueueDao
import org.mifos.groupbanking.core.database.syncqueue.entity.SyncQueueEntity
import org.mifos.groupbanking.core.model.SyncQueueCounts
import org.mifos.groupbanking.core.model.SyncQueueItem
import org.mifos.groupbanking.core.model.SyncStatus
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Room-backed [SyncQueueRepository] over [SyncQueueDao] — the shared offline write-queue.
 *
 * A thin translation layer: [enqueue] stamps the create time via `kotlin.time.Clock`
 * (`kotlinx.datetime.Clock` is deprecated) and inserts a PENDING row; the read flows map the
 * persisted [SyncQueueEntity] rows to the domain [SyncQueueItem] / [SyncQueueCounts]; the mark*
 * writes delegate to the DAO's atomic status-transition `UPDATE`s. No try-catch (Mandatory
 * Rule 4) — local Room errors propagate. Not a Store5 store: no `org.mobilenativefoundation.store`
 * import anywhere in this stack.
 *
 * [nowMs] is injected (defaulting to the real clock) so tests can pin the enqueue/attempt
 * timestamps deterministically; the Koin binding uses the default.
 *
 * See API.md#repositories — SyncQueueRepository.
 */
@OptIn(ExperimentalTime::class)
class SyncQueueRepositoryImpl(
    private val dao: SyncQueueDao,
    private val nowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : SyncQueueRepository {

    override suspend fun enqueue(
        operationType: String,
        targetTable: String,
        payloadJson: String,
    ): Long = dao.insert(
        SyncQueueEntity(
            operationType = operationType,
            payloadJson = payloadJson,
            targetTable = targetTable,
            status = SyncStatus.PENDING.name,
            createdAtEpochMs = nowMs(),
        ),
    )

    override fun observePending(): Flow<List<SyncQueueItem>> =
        dao.observePending().map { rows -> rows.map(SyncQueueEntity::toDomain) }

    override fun observeCounts(): Flow<SyncQueueCounts> = combine(
        dao.countByStatus(SyncStatus.PENDING.name),
        dao.countByStatus(SyncStatus.SYNCING.name),
        dao.countByStatus(SyncStatus.FAILED.name),
        dao.countByStatus(SyncStatus.SYNCED.name),
    ) { pending, syncing, failed, synced ->
        SyncQueueCounts(pending = pending, syncing = syncing, failed = failed, synced = synced)
    }

    override suspend fun markSyncing(id: Long) = dao.markSyncing(id, nowMs())

    override suspend fun markSynced(id: Long) = dao.markSynced(id)

    override suspend fun markFailed(id: Long, error: String?) = dao.markFailed(id, nowMs(), error)

    override suspend fun retryAll() = dao.retryAllFailed()
}

/** Entity -> domain projection. [SyncStatus.valueOf] is total over the persisted enum names. */
private fun SyncQueueEntity.toDomain(): SyncQueueItem = SyncQueueItem(
    id = id,
    operationType = operationType,
    payloadJson = payloadJson,
    targetTable = targetTable,
    status = SyncStatus.valueOf(status),
    createdAtEpochMs = createdAtEpochMs,
    lastAttemptEpochMs = lastAttemptEpochMs,
    attemptCount = attemptCount,
    lastError = lastError,
)
