/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.database.syncqueue.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * Room persisted representation of one queued offline write (`sync_queue` table).
 *
 * Backs the shared offline write-queue infra ([SyncQueueDao][org.mifos.groupbanking.core.database.syncqueue.dao.SyncQueueDao]
 * / `SyncQueueRepository`) that every mutation feature enqueues into when the device is offline
 * (member-add's `CREATE_MEMBER`, loan-request's `LOAN_REQUEST`) and the sync-status feature reads.
 *
 * This is a LOCAL WRITE-QUEUE, deliberately NOT a Store5 read-cache — there is no `Fetcher` /
 * `SourceOfTruth` / offline-first read-stream here, and nothing in this file imports
 * `org.mobilenativefoundation.store`. Rows survive process death so an un-synced mutation is
 * replayed on the next connectivity window.
 *
 * [id] is an auto-generated surrogate key. [status] is persisted as the `SyncStatus` enum `.name`
 * string (mapped back on read by the repository) so a value added later never breaks the schema; a
 * secondary index on [status] keeps the sync worker's `WHERE status = 'PENDING'` read and the
 * sync-status per-status COUNT queries cheap. [payloadJson] is the opaque serialized operation
 * payload (the enqueuing feature owns encode/decode). [createdAtEpochMs] / [lastAttemptEpochMs] are
 * epoch-millis wall times written via `kotlin.time.Clock` at the repository boundary.
 *
 * See API.md#stores — SyncQueue (write-queue, non-Store5).
 */
@Entity(
    tableName = "sync_queue",
    indices = [Index(value = ["status"])],
)
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val operationType: String,
    val payloadJson: String,
    val targetTable: String,
    val status: String,
    val createdAtEpochMs: Long,
    val lastAttemptEpochMs: Long? = null,
    val attemptCount: Int = 0,
    val lastError: String? = null,
)
