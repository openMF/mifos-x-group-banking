/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD RED-first coverage for the loan-request wire contract (`submit_loan_request` — `POST
 * /datatables/dt_loan_request`). See API.md#dtos.
 */
class LoanRequestDtoTest {

    // Server-parity Json config: mirrors NetworkModule's client config (ignoreUnknownKeys +
    // coerceInputValues) so these tests prove EC30 cross-version tolerance, not just happy-path
    // round-tripping.
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val payloadDto = LoanRequestPayloadDto(
        clientId = 5001L,
        requestedAmount = 15000.0,
        purpose = LoanPurposeDto.SCHOOL_FEES,
        durationWeeks = 16,
        savingsBalanceAtRequest = 6000.0,
        submittedAt = "2026-07-21T09:00:00Z",
    )

    private val responseDto = LoanRequestResponseDto(
        resourceId = 7101L,
        officeId = 1L,
        clientId = 5001L,
        resourceExternalId = "LR-7101",
    )

    // ---------- LoanRequestPayloadDto (POST /datatables/dt_loan_request) ----------

    @Test
    fun loanRequestPayloadDto_constructsWithAllFields() {
        assertEquals(5001L, payloadDto.clientId)
        assertEquals(15000.0, payloadDto.requestedAmount)
        assertEquals(LoanPurposeDto.SCHOOL_FEES, payloadDto.purpose)
        assertEquals(16, payloadDto.durationWeeks)
        assertEquals(6000.0, payloadDto.savingsBalanceAtRequest)
        assertEquals("2026-07-21T09:00:00Z", payloadDto.submittedAt)
        assertEquals("PENDING", payloadDto.status)
    }

    @Test
    fun loanRequestPayloadDto_statusDefaultsToPendingWhenOmitted() {
        val dto = payloadDto.copy()
        assertEquals("PENDING", dto.status)
    }

    @Test
    fun loanRequestPayloadDto_acceptsOverriddenStatus() {
        val dto = payloadDto.copy(status = "APPROVED")
        assertEquals("APPROVED", dto.status)
    }

    @Test
    fun loanRequestPayloadDto_equality() {
        assertEquals(payloadDto.copy(), payloadDto.copy())
    }

    @Test
    fun loanRequestPayloadDto_carriesSchemaVersion() {
        assertEquals(1, LoanRequestPayloadDto.SCHEMA_VERSION)
    }

    @Test
    fun loanRequestPayloadDto_serializationRoundTrips_fieldNamesMatchDatatableColumns() {
        // clientId is camelCase (the standard Fineract client identifier, not a datatable
        // column); the remaining fields are the literal snake_case dt_loan_request columns
        // (Hard Rule 5).
        val encoded = json.encodeToString(LoanRequestPayloadDto.serializer(), payloadDto)
        assertTrue(encoded.contains("\"clientId\""))
        assertTrue(encoded.contains("\"requested_amount\""))
        assertTrue(encoded.contains("\"purpose\""))
        assertTrue(encoded.contains("\"duration_weeks\""))
        assertTrue(encoded.contains("\"savings_balance_at_request\""))
        assertTrue(encoded.contains("\"submitted_at\""))
        assertTrue(encoded.contains("\"status\""))
        // purpose is transmitted as the bare enum @SerialName string, not a nested object.
        assertTrue(encoded.contains("\"purpose\":\"SCHOOL_FEES\""))

        val decoded = json.decodeFromString(LoanRequestPayloadDto.serializer(), encoded)
        assertEquals(payloadDto, decoded)
    }

    @Test
    fun loanRequestPayloadDto_toleratesServerAddedField_oldClientNeverCrashes() {
        // Simulates a staggered rollout: the server has shipped a NEW field (`priority`) this
        // (old) client schema does not know about. Decoding MUST succeed, never throw.
        val serverPayload = """
            {
              "clientId":5001,
              "requested_amount":15000.0,
              "purpose":"SCHOOL_FEES",
              "duration_weeks":16,
              "savings_balance_at_request":6000.0,
              "submitted_at":"2026-07-21T09:00:00Z",
              "status":"PENDING",
              "priority":"HIGH"
            }
        """.trimIndent()
        val decoded = json.decodeFromString(LoanRequestPayloadDto.serializer(), serverPayload)
        assertEquals(5001L, decoded.clientId)
        assertEquals(16, decoded.durationWeeks)
    }

    @Test
    fun loanRequestPayloadDto_unknownServerPurposeCoercesToUnknownFallback_notCrash() {
        val serverPayload = """
            {
              "clientId":5001,
              "requested_amount":15000.0,
              "purpose":"DEBT_CONSOLIDATION",
              "duration_weeks":16,
              "savings_balance_at_request":6000.0,
              "submitted_at":"2026-07-21T09:00:00Z",
              "status":"PENDING"
            }
        """.trimIndent()
        val decoded = json.decodeFromString(LoanRequestPayloadDto.serializer(), serverPayload)
        assertEquals(LoanPurposeDto.UNKNOWN, decoded.purpose)
    }

    // ---------- LoanRequestResponseDto (submit_loan_request response) ----------

    @Test
    fun loanRequestResponseDto_constructsWithAllFields() {
        assertEquals(7101L, responseDto.resourceId)
        assertEquals(1L, responseDto.officeId)
        assertEquals(5001L, responseDto.clientId)
        assertEquals("LR-7101", responseDto.resourceExternalId)
    }

    @Test
    fun loanRequestResponseDto_equality() {
        assertEquals(responseDto.copy(), responseDto.copy())
    }

    @Test
    fun loanRequestResponseDto_carriesSchemaVersion() {
        assertEquals(1, LoanRequestResponseDto.SCHEMA_VERSION)
    }

    @Test
    fun loanRequestResponseDto_serializationRoundTrips() {
        val encoded = json.encodeToString(LoanRequestResponseDto.serializer(), responseDto)
        assertTrue(encoded.contains("\"resourceId\""))
        assertTrue(encoded.contains("\"officeId\""))
        assertTrue(encoded.contains("\"clientId\""))
        assertTrue(encoded.contains("\"resourceExternalId\""))

        val decoded = json.decodeFromString(LoanRequestResponseDto.serializer(), encoded)
        assertEquals(responseDto, decoded)
    }

    @Test
    fun loanRequestResponseDto_toleratesServerAddedField_oldClientNeverCrashes() {
        val serverPayload = """
            {
              "resourceId":7101,
              "officeId":1,
              "clientId":5001,
              "resourceExternalId":"LR-7101",
              "groupId":9001
            }
        """.trimIndent()
        val decoded = json.decodeFromString(LoanRequestResponseDto.serializer(), serverPayload)
        assertEquals(7101L, decoded.resourceId)
        assertEquals("LR-7101", decoded.resourceExternalId)
    }
}
