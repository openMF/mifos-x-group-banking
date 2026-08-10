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
 * TDD RED-first coverage for the CANONICAL `SavingsTransactionDto` wire contract, sourced from
 * `idea-layer/screens/personal-dashboard/api.yaml#dtos.SavingsTransactionDto` (nested in
 * `MemberDashboardResponse.recentTransactions`, COMP companion `GET /companion/member/dashboard`).
 * See API.md#dtos.
 */
class SavingsTransactionDtoTest {

    // Server-parity Json config: mirrors NetworkModule's client config (ignoreUnknownKeys +
    // coerceInputValues) so these tests prove EC30 cross-version tolerance, not just happy-path
    // round-tripping.
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val depositDto = SavingsTransactionDto(
        id = "TXN-20260714-001",
        date = "2026-07-14",
        type = TransactionTypeDto.DEPOSIT,
        amount = 500.0,
    )

    // ---------- SavingsTransactionDto ----------

    @Test
    fun savingsTransactionDto_constructsWithAllFields() {
        assertEquals("TXN-20260714-001", depositDto.id)
        assertEquals("2026-07-14", depositDto.date)
        assertEquals(TransactionTypeDto.DEPOSIT, depositDto.type)
        assertEquals(500.0, depositDto.amount)
    }

    @Test
    fun savingsTransactionDto_serializationRoundTrips_wireFieldNamesMatch() {
        val encoded = json.encodeToString(SavingsTransactionDto.serializer(), depositDto)
        assertTrue(encoded.contains("\"id\""))
        assertTrue(encoded.contains("\"date\""))
        assertTrue(encoded.contains("\"type\""))
        assertTrue(encoded.contains("\"amount\""))

        val decoded = json.decodeFromString(SavingsTransactionDto.serializer(), encoded)
        assertEquals(depositDto, decoded)
    }

    @Test
    fun savingsTransactionDto_equality() {
        val a = depositDto.copy()
        val b = depositDto.copy()
        assertEquals(a, b)
    }

    @Test
    fun savingsTransactionDto_carriesSchemaVersion() {
        assertEquals(1, SavingsTransactionDto.SCHEMA_VERSION)
    }

    @Test
    fun savingsTransactionDto_withdrawalType_constructsAndRoundTrips() {
        val withdrawalDto = depositDto.copy(id = "TXN-20260710-002", type = TransactionTypeDto.WITHDRAWAL, amount = 150.0)
        val encoded = json.encodeToString(SavingsTransactionDto.serializer(), withdrawalDto)
        val decoded = json.decodeFromString(SavingsTransactionDto.serializer(), encoded)
        assertEquals(withdrawalDto, decoded)
        assertEquals(TransactionTypeDto.WITHDRAWAL, decoded.type)
    }

    // ---------- TransactionTypeDto (2 known + UNKNOWN, T7/EC30 fallback) ----------

    @Test
    fun transactionTypeDto_hasExactlyThreeEntriesIncludingUnknownFallback() {
        assertEquals(3, TransactionTypeDto.entries.size)
        assertTrue(TransactionTypeDto.entries.contains(TransactionTypeDto.UNKNOWN))
    }

    @Test
    fun transactionTypeDto_decodesEachKnownWireValue() {
        assertEquals(TransactionTypeDto.DEPOSIT, json.decodeFromString(TransactionTypeDto.serializer(), "\"DEPOSIT\""))
        assertEquals(TransactionTypeDto.WITHDRAWAL, json.decodeFromString(TransactionTypeDto.serializer(), "\"WITHDRAWAL\""))
    }

    @Test
    fun transactionTypeDto_unknownServerValueCoercesToUnknownFallback_notCrash() {
        val payload = """
            {"id":"TXN-9","date":"2026-07-01","type":"INTEREST_POSTING","amount":10.0}
        """.trimIndent()
        val decoded = json.decodeFromString(SavingsTransactionDto.serializer(), payload)
        assertEquals(TransactionTypeDto.UNKNOWN, decoded.type)
    }

    // ---------- Cross-version safety fixture (T7/EC30) ----------

    @Test
    fun savingsTransactionDto_toleratesServerAddedFieldAndUnknownEnumValues_oldClientNeverCrashes() {
        // Simulates a staggered rollout: the server has shipped a NEW top-level field
        // (`feeCode`) plus a NEW type value this (old) client schema does not know about.
        // Decoding MUST succeed, never throw.
        val serverPayload = """
            {
              "id":"TXN-20260714-001",
              "date":"2026-07-14",
              "type":"FEE_DEDUCTION",
              "amount":500.0,
              "feeCode":"KE-2026-not-in-old-schema"
            }
        """.trimIndent()
        val decoded = json.decodeFromString(SavingsTransactionDto.serializer(), serverPayload)
        assertEquals("TXN-20260714-001", decoded.id)
        assertEquals(TransactionTypeDto.UNKNOWN, decoded.type)
    }
}
