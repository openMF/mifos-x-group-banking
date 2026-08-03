/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * TDD RED-first coverage for the loan-list wire contract (`GET
 * /groups/{groupId}/loans`). See API.md#dtos.
 */
class LoanSummaryDtoTest {

    // Server-parity Json config: mirrors NetworkModule's client config (ignoreUnknownKeys +
    // coerceInputValues) so these tests prove EC30 cross-version tolerance, not just happy-path
    // round-tripping.
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val peterDto = LoanSummaryDto(
        id = 9001L,
        memberId = 5001L,
        memberName = "Peter Otieno",
        memberPhotoUrl = null,
        loanProductName = "Standard Group Loan",
        principalAmount = 1500.0,
        outstandingBalance = 1125.0,
        overdueAmount = 0.0,
        status = LoanAccountStatusDto.ACTIVE,
        nextRepaymentDate = "2026-05-12",
        isOverdue = false,
        fineractLoanId = 9001L,
    )

    // ---------- LoanSummaryDto ----------

    @Test
    fun loanSummaryDto_constructsWithAllFields() {
        assertEquals(9001L, peterDto.id)
        assertEquals(5001L, peterDto.memberId)
        assertEquals("Peter Otieno", peterDto.memberName)
        assertNull(peterDto.memberPhotoUrl)
        assertEquals("Standard Group Loan", peterDto.loanProductName)
        assertEquals(1500.0, peterDto.principalAmount)
        assertEquals(1125.0, peterDto.outstandingBalance)
        assertEquals(0.0, peterDto.overdueAmount)
        assertEquals(LoanAccountStatusDto.ACTIVE, peterDto.status)
        assertEquals("2026-05-12", peterDto.nextRepaymentDate)
        assertEquals(false, peterDto.isOverdue)
        assertEquals(9001L, peterDto.fineractLoanId)
    }

    @Test
    fun loanSummaryDto_memberPhotoUrlAndNextRepaymentDateDefaultToNullWhenOmitted() {
        val dto = LoanSummaryDto(
            id = 9002L,
            memberId = 5002L,
            memberName = "Grace Wanjiku",
            loanProductName = "Standard Group Loan",
            principalAmount = 3000.0,
            outstandingBalance = 2625.0,
            overdueAmount = 375.0,
            status = LoanAccountStatusDto.OVERDUE,
            isOverdue = true,
            fineractLoanId = 9002L,
        )
        assertNull(dto.memberPhotoUrl)
        assertNull(dto.nextRepaymentDate)
    }

    @Test
    fun loanSummaryDto_statusDefaultsToUnknownWhenOmitted() {
        val dto = LoanSummaryDto(
            id = 9003L,
            memberId = 5003L,
            memberName = "New Borrower",
            loanProductName = "Standard Group Loan",
            principalAmount = 0.0,
            outstandingBalance = 0.0,
            overdueAmount = 0.0,
            isOverdue = false,
            fineractLoanId = 9003L,
        )
        assertEquals(LoanAccountStatusDto.UNKNOWN, dto.status)
    }

    @Test
    fun loanSummaryDto_equality() {
        val a = peterDto.copy()
        val b = peterDto.copy()
        assertEquals(a, b)
    }

    @Test
    fun loanSummaryDto_carriesSchemaVersion() {
        assertEquals(1, LoanSummaryDto.SCHEMA_VERSION)
    }

    @Test
    fun loanSummaryDto_serializationRoundTrips_wireFieldNamesAreCamelCase() {
        val encoded = json.encodeToString(LoanSummaryDto.serializer(), peterDto)
        assertTrue(encoded.contains("\"id\""))
        assertTrue(encoded.contains("\"memberId\""))
        assertTrue(encoded.contains("\"memberName\""))
        assertTrue(encoded.contains("\"loanProductName\""))
        assertTrue(encoded.contains("\"principalAmount\""))
        assertTrue(encoded.contains("\"outstandingBalance\""))
        assertTrue(encoded.contains("\"overdueAmount\""))
        assertTrue(encoded.contains("\"status\""))
        assertTrue(encoded.contains("\"nextRepaymentDate\""))
        assertTrue(encoded.contains("\"isOverdue\""))
        assertTrue(encoded.contains("\"fineractLoanId\""))

        val decoded = json.decodeFromString(LoanSummaryDto.serializer(), encoded)
        assertEquals(peterDto, decoded)
    }

    // ---------- LoanPageDto (offset-paginated envelope, page_size 20) ----------

    @Test
    fun loanPageDto_constructsWithTotalFilteredRecordsAndPageItems() {
        val page = LoanPageDto(totalFilteredRecords = 12, pageItems = listOf(peterDto))
        assertEquals(12, page.totalFilteredRecords)
        assertEquals(listOf(peterDto), page.pageItems)
    }

    @Test
    fun loanPageDto_pageItemsDefaultsToEmptyList() {
        val page = LoanPageDto(totalFilteredRecords = 0)
        assertTrue(page.pageItems.isEmpty())
    }

    @Test
    fun loanPageDto_serializationRoundTrips() {
        val page = LoanPageDto(totalFilteredRecords = 12, pageItems = listOf(peterDto))
        val encoded = json.encodeToString(LoanPageDto.serializer(), page)
        assertTrue(encoded.contains("\"totalFilteredRecords\""))
        assertTrue(encoded.contains("\"pageItems\""))
        val decoded = json.decodeFromString(LoanPageDto.serializer(), encoded)
        assertEquals(page, decoded)
    }

    @Test
    fun loanPageDto_decodesFromGroupLoansShape() {
        // The literal `GET /groups/{groupId}/loans` response envelope (loan-list's own approved
        // api.yaml#dtos.LoanSummary shape).
        val payload = """
            {
              "totalFilteredRecords": 2,
              "pageItems": [
                {"id":9001,"memberId":5001,"memberName":"Peter Otieno","loanProductName":"Standard Group Loan","principalAmount":1500.0,"outstandingBalance":1125.0,"overdueAmount":0.0,"status":"ACTIVE","nextRepaymentDate":"2026-05-12","isOverdue":false,"fineractLoanId":9001},
                {"id":9002,"memberId":5002,"memberName":"Grace Wanjiku","loanProductName":"Standard Group Loan","principalAmount":3000.0,"outstandingBalance":2625.0,"overdueAmount":375.0,"status":"OVERDUE","isOverdue":true,"fineractLoanId":9002}
              ]
            }
        """.trimIndent()
        val decoded = json.decodeFromString(LoanPageDto.serializer(), payload)
        assertEquals(2, decoded.totalFilteredRecords)
        assertEquals(2, decoded.pageItems.size)
        assertNull(decoded.pageItems[1].memberPhotoUrl)
        assertNull(decoded.pageItems[1].nextRepaymentDate)
        assertEquals(LoanAccountStatusDto.OVERDUE, decoded.pageItems[1].status)
    }

    // ---------- LoanAccountStatusDto (5 known + UNKNOWN, T7/EC30 fallback) ----------

    @Test
    fun loanAccountStatusDto_hasExactlySixEntriesIncludingUnknownFallback() {
        assertEquals(6, LoanAccountStatusDto.entries.size)
        assertTrue(LoanAccountStatusDto.entries.contains(LoanAccountStatusDto.UNKNOWN))
    }

    @Test
    fun loanAccountStatusDto_decodesEachKnownWireValue() {
        val known = listOf(
            "ACTIVE" to LoanAccountStatusDto.ACTIVE,
            "OVERDUE" to LoanAccountStatusDto.OVERDUE,
            "CLOSED" to LoanAccountStatusDto.CLOSED,
            "PENDING" to LoanAccountStatusDto.PENDING,
            "REJECTED" to LoanAccountStatusDto.REJECTED,
        )
        known.forEach { (wire, expected) ->
            assertEquals(expected, json.decodeFromString(LoanAccountStatusDto.serializer(), "\"$wire\""))
        }
    }

    @Test
    fun loanAccountStatusDto_unknownServerValueCoercesToUnknownFallback_notCrash() {
        val payload = """
            {"id":9099,"memberId":5099,"memberName":"New","loanProductName":"Standard Group Loan","principalAmount":0.0,"outstandingBalance":0.0,"overdueAmount":0.0,"status":"WRITTEN_OFF","isOverdue":false,"fineractLoanId":9099}
        """.trimIndent()
        val decoded = json.decodeFromString(LoanSummaryDto.serializer(), payload)
        assertEquals(LoanAccountStatusDto.UNKNOWN, decoded.status)
    }

    // ---------- Cross-version safety fixture (T7/EC30) ----------

    @Test
    fun loanSummaryDto_toleratesServerAddedFieldAndUnknownEnumValues_oldClientNeverCrashes() {
        // Simulates a staggered rollout: the server has shipped a NEW top-level field
        // (`interestRate`) plus a NEW status value this (old) client schema does not know about.
        // Decoding MUST succeed, never throw.
        val serverPayload = """
            {
              "id":9001,
              "memberId":5001,
              "memberName":"Peter Otieno",
              "loanProductName":"Standard Group Loan",
              "principalAmount":1500.0,
              "outstandingBalance":1125.0,
              "overdueAmount":0.0,
              "status":"WRITTEN_OFF",
              "nextRepaymentDate":"2026-05-12",
              "isOverdue":false,
              "fineractLoanId":9001,
              "interestRate":15.0
            }
        """.trimIndent()
        val decoded = json.decodeFromString(LoanSummaryDto.serializer(), serverPayload)
        assertEquals(9001L, decoded.id)
        assertEquals(LoanAccountStatusDto.UNKNOWN, decoded.status)
    }
}
