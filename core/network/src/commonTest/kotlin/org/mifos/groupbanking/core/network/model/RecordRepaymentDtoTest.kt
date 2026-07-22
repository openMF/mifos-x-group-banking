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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * TDD RED-first coverage for the loan-repayment-dialog wire contract
 * (`POST /loans/{loanId}/transactions?command=repayment`). See API.md#dtos.
 */
class RecordRepaymentDtoTest {

    // Server-parity Json config: mirrors NetworkModule's client config (ignoreUnknownKeys +
    // coerceInputValues) so these tests prove EC30 cross-version tolerance, not just happy-path
    // round-tripping.
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val requestDto = RecordRepaymentRequestDto(
        transactionDate = "21 July 2026",
        transactionAmount = 125.0,
        paymentTypeId = 1,
        receiptNumber = "QJZ7X9A1BK",
        locale = "en",
        dateFormat = "dd MMMM yyyy",
    )

    // ---------- RecordRepaymentRequestDto ----------

    @Test
    fun recordRepaymentRequestDto_constructsWithAllFields() {
        assertEquals("21 July 2026", requestDto.transactionDate)
        assertEquals(125.0, requestDto.transactionAmount)
        assertEquals(1, requestDto.paymentTypeId)
        assertEquals("QJZ7X9A1BK", requestDto.receiptNumber)
        assertEquals("en", requestDto.locale)
        assertEquals("dd MMMM yyyy", requestDto.dateFormat)
    }

    @Test
    fun recordRepaymentRequestDto_receiptNumberDefaultsToNullWhenOmitted() {
        val dto = RecordRepaymentRequestDto(
            transactionDate = "21 July 2026",
            transactionAmount = 500.0,
            paymentTypeId = 2,
        )
        assertNull(dto.receiptNumber)
    }

    @Test
    fun recordRepaymentRequestDto_localeAndDateFormatDefaultWhenOmitted() {
        val dto = RecordRepaymentRequestDto(
            transactionDate = "21 July 2026",
            transactionAmount = 500.0,
            paymentTypeId = 2,
        )
        assertEquals("en", dto.locale)
        assertEquals("dd MMMM yyyy", dto.dateFormat)
    }

    @Test
    fun recordRepaymentRequestDto_equality() {
        assertEquals(requestDto.copy(), requestDto.copy())
    }

    @Test
    fun recordRepaymentRequestDto_carriesSchemaVersion() {
        assertEquals(1, RecordRepaymentRequestDto.SCHEMA_VERSION)
    }

    @Test
    fun recordRepaymentRequestDto_serializationRoundTrips_wireFieldNamesMatchApiYaml() {
        val encoded = json.encodeToString(RecordRepaymentRequestDto.serializer(), requestDto)
        assertTrue(encoded.contains("\"transactionDate\""))
        assertTrue(encoded.contains("\"transactionAmount\""))
        assertTrue(encoded.contains("\"paymentTypeId\""))
        assertTrue(encoded.contains("\"receiptNumber\""))
        assertTrue(encoded.contains("\"locale\""))
        assertTrue(encoded.contains("\"dateFormat\""))

        val decoded = json.decodeFromString(RecordRepaymentRequestDto.serializer(), encoded)
        assertEquals(requestDto, decoded)
    }

    // ---------- RecordRepaymentResponseDto ----------

    private val responseDto = RecordRepaymentResponseDto(
        officeId = 1,
        clientId = 5001L,
        loanId = 9001L,
        resourceId = 30045L,
    )

    @Test
    fun recordRepaymentResponseDto_constructsWithAllFields() {
        assertEquals(1, responseDto.officeId)
        assertEquals(5001L, responseDto.clientId)
        assertEquals(9001L, responseDto.loanId)
        assertEquals(30045L, responseDto.resourceId)
    }

    @Test
    fun recordRepaymentResponseDto_equality() {
        assertEquals(responseDto.copy(), responseDto.copy())
    }

    @Test
    fun recordRepaymentResponseDto_carriesSchemaVersion() {
        assertEquals(1, RecordRepaymentResponseDto.SCHEMA_VERSION)
    }

    @Test
    fun recordRepaymentResponseDto_serializationRoundTrips() {
        val encoded = json.encodeToString(RecordRepaymentResponseDto.serializer(), responseDto)
        assertTrue(encoded.contains("\"officeId\""))
        assertTrue(encoded.contains("\"clientId\""))
        assertTrue(encoded.contains("\"loanId\""))
        assertTrue(encoded.contains("\"resourceId\""))
        val decoded = json.decodeFromString(RecordRepaymentResponseDto.serializer(), encoded)
        assertEquals(responseDto, decoded)
    }

    @Test
    fun recordRepaymentResponseDto_decodesFromLiteralMakeRepaymentShape() {
        val payload = """
            {"officeId":1,"clientId":5001,"loanId":9001,"resourceId":30045}
        """.trimIndent()
        val decoded = json.decodeFromString(RecordRepaymentResponseDto.serializer(), payload)
        assertEquals(responseDto, decoded)
    }

    // ---------- Cross-version safety fixture (T7/EC30) ----------

    @Test
    fun recordRepaymentResponseDto_toleratesServerAddedField_oldClientNeverCrashes() {
        // Simulates a staggered rollout: the server has shipped a NEW field this (old) client
        // schema does not know about. Decoding MUST succeed, never throw.
        val serverPayload = """
            {
              "officeId":1,
              "clientId":5001,
              "loanId":9001,
              "resourceId":30045,
              "transactionAmount":125.0
            }
        """.trimIndent()
        val decoded = json.decodeFromString(RecordRepaymentResponseDto.serializer(), serverPayload)
        assertEquals(30045L, decoded.resourceId)
    }
}
