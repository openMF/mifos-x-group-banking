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
import kotlinx.coroutines.flow.flowOf
import org.mifos.groupbanking.core.model.EntityType
import org.mifos.groupbanking.core.model.SyncQueueCounts
import org.mifos.groupbanking.core.model.SyncQueueItem

/**
 * Shared minimal [SyncQueueRepository] test double for the ONLINE-path repository tests
 * (group-create / member-invite / member-add) that inject a queue only for their offline branch.
 * [enqueue] records the last call so a test may assert it; every read stream emits empty. The
 * dedicated offline-behaviour tests use the feature ViewModels' own richer fakes.
 */
internal class NoOpSyncQueueRepository : SyncQueueRepository {
    var enqueueCallCount = 0
    var lastOperationType: String? = null
    var lastTargetTable: String? = null
    var lastPayloadJson: String? = null

    override suspend fun enqueue(operationType: String, targetTable: String, payloadJson: String): Long {
        enqueueCallCount++
        lastOperationType = operationType
        lastTargetTable = targetTable
        lastPayloadJson = payloadJson
        return enqueueCallCount.toLong()
    }

    override fun observePending(): Flow<List<SyncQueueItem>> = flowOf(emptyList())
    override fun observeCounts(): Flow<SyncQueueCounts> = flowOf(SyncQueueCounts(0, 0, 0, 0))
    override suspend fun markSyncing(id: Long) = Unit
    override suspend fun markSynced(id: Long) = Unit
    override suspend fun markFailed(id: Long, error: String?) = Unit
    override suspend fun retryAll() = Unit
    override fun observePendingByType(): Flow<Map<EntityType, Int>> = flowOf(emptyMap())
    override fun observeFailed(): Flow<List<SyncQueueItem>> = flowOf(emptyList())
    override fun observeConflictCount(): Flow<Int> = flowOf(0)
    override suspend fun getItem(id: Long): SyncQueueItem? = null
}
