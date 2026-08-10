/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.network.model

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD RED-first coverage for the sync-status batch-drain wire contract (`POST
 * /fineract-provider/api/v1/batches`, `batch_sync`). See API.md#dtos.
 */
class BatchSyncDtoTest {

    // Server-parity Json config: mirrors NetworkModule's client config (ignoreUnknownKeys +
    // coerceInputValues) so these tests prove EC30 cross-version tolerance, not just
    // happy-path round-tripping.
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    // ---------- BatchOperationDto / BatchSyncRequestDto ----------

    private val loanRequestOperationDto = BatchOperationDto(
        requestId = 1,
        relativeUrl = "datatables/dt_loan_request",
        method = "POST",
        body = """{"clientId":5001,"requested_amount":20000.0}""",
    )

    @Test
    fun batchOperationDto_constructsWithAllFields() {
        assertEquals(1, loanRequestOperationDto.requestId)
        assertEquals("datatables/dt_loan_request", loanRequestOperationDto.relativeUrl)
        assertEquals("POST", loanRequestOperationDto.method)
        assertTrue(loanRequestOperationDto.body.contains("clientId"))
    }

    @Test
    fun batchOperationDto_equality() {
        assertEquals(loanRequestOperationDto.copy(), loanRequestOperationDto.copy())
    }

    @Test
    fun batchOperationDto_carriesSchemaVersion() {
        assertEquals(1, BatchOperationDto.SCHEMA_VERSION)
    }

    @Test
    fun batchOperationDto_serializationRoundTrips_wireFieldNamesAreCamelCase() {
        val encoded = json.encodeToString(BatchOperationDto.serializer(), loanRequestOperationDto)
        assertTrue(encoded.contains("\"requestId\""))
        assertTrue(encoded.contains("\"relativeUrl\""))
        assertTrue(encoded.contains("\"method\""))
        assertTrue(encoded.contains("\"body\""))
        val decoded = json.decodeFromString(BatchOperationDto.serializer(), encoded)
        assertEquals(loanRequestOperationDto, decoded)
    }

    private val requestDto = BatchSyncRequestDto(
        requests = listOf(
            loanRequestOperationDto,
            BatchOperationDto(
                requestId = 2,
                relativeUrl = "datatables/dt_member_role",
                method = "POST",
                body = """{"groupId":9001}""",
            ),
        ),
    )

    @Test
    fun batchSyncRequestDto_constructsWithAllFields() {
        assertEquals(2, requestDto.requests.size)
        assertEquals(1, requestDto.requests[0].requestId)
        assertEquals(2, requestDto.requests[1].requestId)
    }

    @Test
    fun batchSyncRequestDto_requestsDefaultsToEmptyList() {
        val decoded = json.decodeFromString(BatchSyncRequestDto.serializer(), """{"requests":[]}""")
        assertTrue(decoded.requests.isEmpty())
    }

    @Test
    fun batchSyncRequestDto_carriesSchemaVersion() {
        assertEquals(1, BatchSyncRequestDto.SCHEMA_VERSION)
    }

    @Test
    fun batchSyncRequestDto_serializationRoundTrips() {
        val encoded = json.encodeToString(BatchSyncRequestDto.serializer(), requestDto)
        assertTrue(encoded.contains("\"requests\""))
        val decoded = json.decodeFromString(BatchSyncRequestDto.serializer(), encoded)
        assertEquals(requestDto, decoded)
    }

    // ---------- BatchSyncResponseItemDto — top-level JSON ARRAY, no wrapper ----------

    private val successItemDto = BatchSyncResponseItemDto(
        requestId = 1,
        statusCode = 200,
        body = """{"resourceId":9101}""",
    )

    private val conflictItemDto = BatchSyncResponseItemDto(
        requestId = 2,
        statusCode = 409,
        body = """{"errors":[{"developerMessage":"duplicate"}]}""",
    )

    @Test
    fun batchSyncResponseItemDto_constructsWithAllFields() {
        assertEquals(1, successItemDto.requestId)
        assertEquals(200, successItemDto.statusCode)
        assertTrue(successItemDto.body.contains("resourceId"))
    }

    @Test
    fun batchSyncResponseItemDto_equality() {
        assertEquals(successItemDto.copy(), successItemDto.copy())
    }

    @Test
    fun batchSyncResponseItemDto_carriesSchemaVersion() {
        assertEquals(1, BatchSyncResponseItemDto.SCHEMA_VERSION)
    }

    @Test
    fun batchSyncResponseItemDto_decodesTopLevelJsonArray_noWrapperObject() {
        // The /batches response IS a bare JSON array — api.yaml#dtos.BatchSyncResponse
        // declares `type: array` at the top level, no envelope.
        val payload = """
            [
              {"requestId":1,"statusCode":200,"body":"{\"resourceId\":9101}"},
              {"requestId":2,"statusCode":409,"body":"{\"errors\":[{\"developerMessage\":\"duplicate\"}]}"}
            ]
        """.trimIndent()
        val decoded = json.decodeFromString(
            ListSerializer(BatchSyncResponseItemDto.serializer()),
            payload,
        )
        assertEquals(2, decoded.size)
        assertEquals(successItemDto, decoded[0])
        assertEquals(conflictItemDto, decoded[1])
    }

    @Test
    fun batchSyncResponseItemDto_serializationRoundTrips() {
        val encoded = json.encodeToString(BatchSyncResponseItemDto.serializer(), successItemDto)
        assertTrue(encoded.contains("\"statusCode\""))
        val decoded = json.decodeFromString(BatchSyncResponseItemDto.serializer(), encoded)
        assertEquals(successItemDto, decoded)
    }

    // ---------- Cross-version safety fixture (T7/EC30) ----------

    @Test
    fun batchSyncResponseItemDto_toleratesServerAddedField_oldClientNeverCrashes() {
        // Simulates a staggered rollout: the server has shipped a NEW field this (old) client
        // schema does not know about. Decoding MUST succeed, never throw.
        val serverPayload = """
            {"requestId":1,"statusCode":200,"body":"{}","headers":{"Content-Type":"application/json"}}
        """.trimIndent()
        val decoded = json.decodeFromString(BatchSyncResponseItemDto.serializer(), serverPayload)
        assertEquals(200, decoded.statusCode)
    }

    @Test
    fun batchSyncResponseItemDto_arrayWithServerAddedElementFields_toleratesWithoutCrash() {
        val serverPayload = """
            [
              {"requestId":1,"statusCode":200,"body":"{}","traceId":"abc-123"}
            ]
        """.trimIndent()
        val decoded = json.decodeFromString(
            ListSerializer(BatchSyncResponseItemDto.serializer()),
            serverPayload,
        )
        assertEquals(1, decoded.size)
        assertEquals(1, decoded[0].requestId)
    }
}
