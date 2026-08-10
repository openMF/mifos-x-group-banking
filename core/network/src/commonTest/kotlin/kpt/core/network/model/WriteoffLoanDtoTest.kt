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

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD RED-first coverage for the loan-mark-defaulted-dialog wire contract
 * (`POST /loans/{loanId}/transactions?command=writeoff`). See API.md#dtos.
 */
class WriteoffLoanDtoTest {

    // Server-parity Json config: mirrors NetworkModule's client config (ignoreUnknownKeys +
    // coerceInputValues) so these tests prove EC30 cross-version tolerance, not just
    // happy-path round-tripping.
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val requestDto = WriteoffLoanRequestDto(
        transactionDate = "21 July 2026",
        locale = "en",
        dateFormat = "dd MMMM yyyy",
    )

    // ---------- WriteoffLoanRequestDto ----------

    @Test
    fun writeoffLoanRequestDto_constructsWithAllFields() {
        assertEquals("21 July 2026", requestDto.transactionDate)
        assertEquals("en", requestDto.locale)
        assertEquals("dd MMMM yyyy", requestDto.dateFormat)
    }

    @Test
    fun writeoffLoanRequestDto_localeAndDateFormatDefaultWhenOmitted() {
        val dto = WriteoffLoanRequestDto(transactionDate = "21 July 2026")
        assertEquals("en", dto.locale)
        assertEquals("dd MMMM yyyy", dto.dateFormat)
    }

    @Test
    fun writeoffLoanRequestDto_equality() {
        assertEquals(requestDto.copy(), requestDto.copy())
    }

    @Test
    fun writeoffLoanRequestDto_carriesSchemaVersion() {
        assertEquals(1, WriteoffLoanRequestDto.SCHEMA_VERSION)
    }

    @Test
    fun writeoffLoanRequestDto_serializationRoundTrips_wireFieldNamesMatchApiYaml() {
        val encoded = json.encodeToString(WriteoffLoanRequestDto.serializer(), requestDto)
        assertTrue(encoded.contains("\"transactionDate\""))
        assertTrue(encoded.contains("\"locale\""))
        assertTrue(encoded.contains("\"dateFormat\""))

        val decoded = json.decodeFromString(WriteoffLoanRequestDto.serializer(), encoded)
        assertEquals(requestDto, decoded)
    }

    // ---------- WriteoffLoanResponseDto ----------

    private val responseDto = WriteoffLoanResponseDto(
        officeId = 1,
        clientId = 5001L,
        loanId = 9001L,
        resourceId = 40017L,
    )

    @Test
    fun writeoffLoanResponseDto_constructsWithAllFields() {
        assertEquals(1, responseDto.officeId)
        assertEquals(5001L, responseDto.clientId)
        assertEquals(9001L, responseDto.loanId)
        assertEquals(40017L, responseDto.resourceId)
    }

    @Test
    fun writeoffLoanResponseDto_equality() {
        assertEquals(responseDto.copy(), responseDto.copy())
    }

    @Test
    fun writeoffLoanResponseDto_carriesSchemaVersion() {
        assertEquals(1, WriteoffLoanResponseDto.SCHEMA_VERSION)
    }

    @Test
    fun writeoffLoanResponseDto_serializationRoundTrips() {
        val encoded = json.encodeToString(WriteoffLoanResponseDto.serializer(), responseDto)
        assertTrue(encoded.contains("\"officeId\""))
        assertTrue(encoded.contains("\"clientId\""))
        assertTrue(encoded.contains("\"loanId\""))
        assertTrue(encoded.contains("\"resourceId\""))
        val decoded = json.decodeFromString(WriteoffLoanResponseDto.serializer(), encoded)
        assertEquals(responseDto, decoded)
    }

    @Test
    fun writeoffLoanResponseDto_decodesFromLiteralWriteoffShape() {
        val payload = """
            {"officeId":1,"clientId":5001,"loanId":9001,"resourceId":40017}
        """.trimIndent()
        val decoded = json.decodeFromString(WriteoffLoanResponseDto.serializer(), payload)
        assertEquals(responseDto, decoded)
    }

    // ---------- Cross-version safety fixture (T7/EC30) ----------

    @Test
    fun writeoffLoanResponseDto_toleratesServerAddedField_oldClientNeverCrashes() {
        // Simulates a staggered rollout: the server has shipped a NEW field this (old) client
        // schema does not know about. Decoding MUST succeed, never throw.
        val serverPayload = """
            {
              "officeId":1,
              "clientId":5001,
              "loanId":9001,
              "resourceId":40017,
              "transactionDate":"21 July 2026"
            }
        """.trimIndent()
        val decoded = json.decodeFromString(WriteoffLoanResponseDto.serializer(), serverPayload)
        assertEquals(40017L, decoded.resourceId)
    }
}
