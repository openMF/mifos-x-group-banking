/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
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
 * [relativeUrl] follows the TWO-CONVENTION [SyncQueueItem.targetTable] contract:
 *  - a bare datatable name (e.g. `"dt_loan_request"`, no leading `/`) → `"datatables/$targetTable"`
 *    (the shipped `LOAN_REQUEST` / `SUBMIT_MEETING` datatable-write call-sites), and
 *  - a full companion route beginning with `/` (e.g. `"/companion/groups/24/shareout/execute"`,
 *    the `SHARE_OUT_EXECUTE` / `ROTATION_PAYOUT_EXECUTE` orchestration call-sites) → used VERBATIM.
 * Both forms are self-dispatched by the companion's `/batches` handler, which normalises the leading
 * slash and strips any `/fineract-provider/api/v1` prefix before routing to its own mux — so a
 * command-feature replay (share-out, rotation payout, loan apply, loan repayment) lands on its
 * orchestration handler, not a bogus `datatables//companion/...` path.
 * [method] is always `"POST"` (every queued write is a create/append/execute from the client's
 * perspective — the target's OWN row/command semantics are Fineract/companion's concern, not the
 * queue's). [body] is [SyncQueueItem.payloadJson] verbatim — the queue never inspects or re-encodes
 * it (same "opaque payload" contract as `SyncQueueRepository.enqueue`'s kdoc).
 */
fun SyncQueueItem.toBatchOperation(requestId: Int): BatchOperation = BatchOperation(
    requestId = requestId,
    // Leading-slash targetTable is a full companion route (share-out/rotation/loan orchestration);
    // a bare name is a Fineract datatable the replay POSTs a row to.
    relativeUrl = if (targetTable.startsWith("/")) targetTable else "datatables/$targetTable",
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
