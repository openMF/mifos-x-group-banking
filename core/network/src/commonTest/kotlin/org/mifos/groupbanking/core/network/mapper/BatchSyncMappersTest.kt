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
import org.mifos.groupbanking.core.model.SyncStatus
import org.mifos.groupbanking.core.network.model.BatchOperationDto
import org.mifos.groupbanking.core.network.model.BatchSyncRequestDto
import org.mifos.groupbanking.core.network.model.BatchSyncResponseItemDto
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * TDD RED-first coverage for the sync-status batch-drain mappers (`batch_sync`,
 * `POST /fineract-provider/api/v1/batches`). Every field on every DTO declared in
 * `BatchSyncDto.kt` is mapped — no field left unmapped. See API.md#dtos.
 */
class BatchSyncMappersTest {

    // ---------- SyncQueueItem.toBatchOperation ----------

    private val loanRequestQueueItem = SyncQueueItem(
        id = 42L,
        operationType = "LOAN_REQUEST",
        payloadJson = """{"clientId":5001,"requested_amount":20000.0}""",
        targetTable = "dt_loan_request",
        status = SyncStatus.PENDING,
        createdAtEpochMs = 1_700_000_000_000L,
        lastAttemptEpochMs = null,
        attemptCount = 0,
        lastError = null,
    )

    @Test
    fun toBatchOperation_derivesRelativeUrlFromTargetTable() {
        val op = loanRequestQueueItem.toBatchOperation(requestId = 1)
        assertEquals("datatables/dt_loan_request", op.relativeUrl)
    }

    @Test
    fun toBatchOperation_methodIsAlwaysPost() {
        val op = loanRequestQueueItem.toBatchOperation(requestId = 1)
        assertEquals("POST", op.method)
    }

    @Test
    fun toBatchOperation_bodyIsThePayloadJsonVerbatim() {
        val op = loanRequestQueueItem.toBatchOperation(requestId = 1)
        assertEquals(loanRequestQueueItem.payloadJson, op.body)
    }

    @Test
    fun toBatchOperation_requestIdIsTheCallerSuppliedBatchPosition_notTheQueueRowId() {
        // requestId is the caller-assigned position within THIS batch submission, distinct from
        // the queue row's own generated `id` (42L here) — see BatchOperation.kt kdoc.
        val op = loanRequestQueueItem.toBatchOperation(requestId = 7)
        assertEquals(7, op.requestId)
    }

    // ---------- BatchOperation <-> BatchOperationDto ----------

    private val operation = BatchOperation(
        requestId = 1,
        relativeUrl = "datatables/dt_loan_request",
        method = "POST",
        body = """{"clientId":5001}""",
    )

    @Test
    fun batchOperation_toDto_mapsEveryField() {
        val dto = operation.toDto()
        assertEquals(1, dto.requestId)
        assertEquals("datatables/dt_loan_request", dto.relativeUrl)
        assertEquals("POST", dto.method)
        assertEquals("""{"clientId":5001}""", dto.body)
    }

    @Test
    fun batchOperationDto_toDomainModel_mapsEveryField() {
        val dto = BatchOperationDto(
            requestId = 2,
            relativeUrl = "datatables/dt_member_role",
            method = "POST",
            body = """{"groupId":9001}""",
        )
        val domain = dto.toDomainModel()
        assertEquals(2, domain.requestId)
        assertEquals("datatables/dt_member_role", domain.relativeUrl)
        assertEquals("POST", domain.method)
        assertEquals("""{"groupId":9001}""", domain.body)
    }

    // ---------- BatchSyncRequest <-> BatchSyncRequestDto ----------

    @Test
    fun batchSyncRequest_toDto_mapsEveryOperationInOrder() {
        val request = BatchSyncRequest(requests = listOf(operation, operation.copy(requestId = 2)))
        val dto = request.toDto()
        assertEquals(2, dto.requests.size)
        assertEquals(1, dto.requests[0].requestId)
        assertEquals(2, dto.requests[1].requestId)
    }

    @Test
    fun batchSyncRequestDto_toDomainModel_mapsEveryOperationInOrder() {
        val dto = BatchSyncRequestDto(
            requests = listOf(
                BatchOperationDto(1, "datatables/dt_loan_request", "POST", "{}"),
                BatchOperationDto(2, "datatables/dt_member_role", "POST", "{}"),
            ),
        )
        val domain = dto.toDomainModel()
        assertEquals(2, domain.requests.size)
        assertEquals("datatables/dt_loan_request", domain.requests[0].relativeUrl)
    }

    // ---------- BatchSyncResponseItemDto -> BatchSyncResponseItem ----------

    @Test
    fun batchSyncResponseItemDto_toDomainModel_mapsEveryField() {
        val dto = BatchSyncResponseItemDto(requestId = 1, statusCode = 200, body = """{"resourceId":9101}""")
        val domain = dto.toDomainModel()
        assertEquals(1, domain.requestId)
        assertEquals(200, domain.statusCode)
        assertEquals("""{"resourceId":9101}""", domain.body)
    }

    @Test
    fun batchSyncResponseItemDtoList_toDomainModels_mapsEveryRowInOrder() {
        val dtos = listOf(
            BatchSyncResponseItemDto(1, 200, "{}"),
            BatchSyncResponseItemDto(2, 409, "{}"),
        )
        val domains = dtos.toDomainModels()
        assertEquals(2, domains.size)
        assertEquals(409, domains[1].statusCode)
    }

    // ---------- List<BatchSyncResponseItem>.toSyncResult — mixed 200/409/500 fold ----------

    @Test
    fun toSyncResult_foldsMixedStatusCodesIntoCounts() {
        val items = listOf(
            BatchSyncResponseItem(1, 200, "{}"),
            BatchSyncResponseItem(2, 201, "{}"),
            BatchSyncResponseItem(3, 409, "{}"),
            BatchSyncResponseItem(4, 500, "{}"),
            BatchSyncResponseItem(5, 400, "{}"),
        )
        val result = items.toSyncResult()
        assertEquals(2, result.successCount)
        assertEquals(1, result.conflictCount)
        assertEquals(2, result.failedCount)
    }

    @Test
    fun toSyncResult_allSuccess_zeroFailedAndConflict() {
        val items = listOf(BatchSyncResponseItem(1, 200, "{}"), BatchSyncResponseItem(2, 204, "{}"))
        val result = items.toSyncResult()
        assertEquals(2, result.successCount)
        assertEquals(0, result.failedCount)
        assertEquals(0, result.conflictCount)
    }

    @Test
    fun toSyncResult_emptyList_allZero() {
        val result = emptyList<BatchSyncResponseItem>().toSyncResult()
        assertEquals(0, result.successCount)
        assertEquals(0, result.failedCount)
        assertEquals(0, result.conflictCount)
    }
}
