/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.mapper

import org.mifos.groupbanking.core.model.BatchOperation
import org.mifos.groupbanking.core.model.BatchSyncRequest
import org.mifos.groupbanking.core.model.BatchSyncResponseItem
import org.mifos.groupbanking.core.model.SyncQueueItem
import org.mifos.groupbanking.core.model.SyncResult
import org.mifos.groupbanking.core.network.model.BatchOperationDto
import org.mifos.groupbanking.core.network.model.BatchSyncRequestDto
import org.mifos.groupbanking.core.network.model.BatchSyncResponseItemDto
import kotlin.jvm.JvmName

/**
 * DTO <-> domain mappers for the sync-status batch-drain wire contract (`batch_sync`,
 * `POST /fineract-provider/api/v1/batches`). Every field on every DTO declared in
 * `BatchSyncDto.kt` is mapped — no field left unmapped.
 */

// ---------- SyncQueueItem -> BatchOperation ----------

/**
 * Resolves one queued offline write ([SyncQueueItem]) into a Fineract batch request row.
 *
 * @param requestId the CALLER-ASSIGNED position of this operation within the batch submission
 *   being built (`1..N`, matching the order [SyncQueueRepository.observePending] emits) — this
 *   is NOT [SyncQueueItem.id]; Fineract echoes [requestId] back on [BatchSyncResponseItem] so the
 *   caller can correlate the response row back to the originating queue row via the SAME index
 *   the caller used to build the request list.
 *
 * [relativeUrl] is derived as `"datatables/${targetTable}"` — every shipped enqueue call-site
 * (`CREATE_MEMBER`, `ASSIGN_MEMBER_ROLE`, `UPLOAD_MEMBER_PHOTO`, `LOAN_REQUEST`) targets a
 * Fineract datatable row (`SyncQueueItem.targetTable` kdoc: "the Fineract datatable the replayed
 * write lands on, e.g. `dt_loan_request`"), so every batched replay is a datatable `POST`.
 * [method] is always `"POST"` for the same reason (queued writes are always creates/appends to a
 * datatable row from the client's perspective — the datatable's OWN row-level semantics, e.g. an
 * upsert-by-entityId, are Fineract's concern, not the queue's). [body] is
 * [SyncQueueItem.payloadJson] verbatim — the queue never inspects or re-encodes it (same
 * "opaque payload" contract as `SyncQueueRepository.enqueue`'s kdoc).
 */
fun SyncQueueItem.toBatchOperation(requestId: Int): BatchOperation = BatchOperation(
    requestId = requestId,
    relativeUrl = "datatables/$targetTable",
    method = "POST",
    body = payloadJson,
)

// ---------- BatchOperation <-> BatchOperationDto ----------

fun BatchOperation.toDto(): BatchOperationDto = BatchOperationDto(
    requestId = requestId,
    relativeUrl = relativeUrl,
    method = method,
    body = body,
)

fun BatchOperationDto.toDomainModel(): BatchOperation = BatchOperation(
    requestId = requestId,
    relativeUrl = relativeUrl,
    method = method,
    body = body,
)

// ---------- BatchSyncRequest <-> BatchSyncRequestDto ----------

fun BatchSyncRequest.toDto(): BatchSyncRequestDto = BatchSyncRequestDto(
    requests = requests.map { it.toDto() },
)

fun BatchSyncRequestDto.toDomainModel(): BatchSyncRequest = BatchSyncRequest(
    requests = requests.map { it.toDomainModel() },
)

// ---------- BatchSyncResponseItemDto -> BatchSyncResponseItem ----------

fun BatchSyncResponseItemDto.toDomainModel(): BatchSyncResponseItem = BatchSyncResponseItem(
    requestId = requestId,
    statusCode = statusCode,
    body = body,
)

/** Batch converter — maps every `/batches` response row in declaration order. */
@JvmName("batchSyncResponseItemDtoListToDomainModels")
fun List<BatchSyncResponseItemDto>.toDomainModels(): List<BatchSyncResponseItem> = map { it.toDomainModel() }

// ---------- List<BatchSyncResponseItem> -> SyncResult ----------

/**
 * Folds a `/batches` response into the domain [SyncResult] rollup: any `2xx` [statusCode]
 * increments [SyncResult.successCount]; exactly `409` (Fineract's conflict status for a
 * datatable uniqueness/version clash) increments [SyncResult.conflictCount]; every other status
 * increments [SyncResult.failedCount].
 */
fun List<BatchSyncResponseItem>.toSyncResult(): SyncResult {
    var successCount = 0
    var failedCount = 0
    var conflictCount = 0
    for (item in this) {
        when {
            item.statusCode in 200..299 -> successCount++
            item.statusCode == 409 -> conflictCount++
            else -> failedCount++
        }
    }
    return SyncResult(
        successCount = successCount,
        failedCount = failedCount,
        conflictCount = conflictCount,
    )
}
