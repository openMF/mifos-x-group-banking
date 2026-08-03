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
import kotlin.test.assertTrue

/**
 * TDD RED-first coverage for the loan-detail wire contract (`GET
 * /loans/{loanId}`). See API.md#dtos.
 */
class LoanDetailDtoTest {

    // Server-parity Json config: mirrors NetworkModule's client config (ignoreUnknownKeys +
    // coerceInputValues) so these tests prove EC30 cross-version tolerance, not just happy-path
    // round-tripping.
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val loanDetailDto = LoanDetailDto(
        id = 9001L,
        memberId = 5001L,
        memberName = "Peter Otieno",
        loanProductName = "Standard Group Loan",
        principalAmount = 1500.0,
        disbursedDate = "2026-03-01",
        interestRatePercent = 15.0,
        totalOutstanding = 1125.0,
        totalOverdue = 0.0,
        status = LoanAccountStatusDto.ACTIVE,
        fineractLoanId = 9001L,
    )

    private val scheduleRowDto = RepaymentScheduleRowDto(
        weekNumber = 1,
        dueDate = "2026-03-08",
        dueAmount = 187.5,
        paidAmount = 187.5,
        balance = 1312.5,
        status = RepaymentRowStatusDto.PAID,
    )

    private val transactionDto = RepaymentTransactionDto(
        id = 7001L,
        type = "repayment",
        date = "2026-03-08",
        amount = 187.5,
    )

    // ---------- LoanDetailDto ----------

    @Test
    fun loanDetailDto_constructsWithAllFields() {
        assertEquals(9001L, loanDetailDto.id)
        assertEquals(5001L, loanDetailDto.memberId)
        assertEquals("Peter Otieno", loanDetailDto.memberName)
        assertEquals("Standard Group Loan", loanDetailDto.loanProductName)
        assertEquals(1500.0, loanDetailDto.principalAmount)
        assertEquals("2026-03-01", loanDetailDto.disbursedDate)
        assertEquals(15.0, loanDetailDto.interestRatePercent)
        assertEquals(1125.0, loanDetailDto.totalOutstanding)
        assertEquals(0.0, loanDetailDto.totalOverdue)
        assertEquals(LoanAccountStatusDto.ACTIVE, loanDetailDto.status)
        assertEquals(9001L, loanDetailDto.fineractLoanId)
    }

    @Test
    fun loanDetailDto_statusDefaultsToUnknownWhenOmitted() {
        val dto = LoanDetailDto(
            id = 9099L,
            memberId = 5099L,
            memberName = "New Borrower",
            loanProductName = "Standard Group Loan",
            principalAmount = 0.0,
            disbursedDate = "2026-03-01",
            interestRatePercent = 0.0,
            totalOutstanding = 0.0,
            totalOverdue = 0.0,
            fineractLoanId = 9099L,
        )
        assertEquals(LoanAccountStatusDto.UNKNOWN, dto.status)
    }

    @Test
    fun loanDetailDto_equality() {
        assertEquals(loanDetailDto.copy(), loanDetailDto.copy())
    }

    @Test
    fun loanDetailDto_carriesSchemaVersion() {
        assertEquals(1, LoanDetailDto.SCHEMA_VERSION)
    }

    @Test
    fun loanDetailDto_serializationRoundTrips() {
        val encoded = json.encodeToString(LoanDetailDto.serializer(), loanDetailDto)
        assertTrue(encoded.contains("\"id\""))
        assertTrue(encoded.contains("\"memberId\""))
        assertTrue(encoded.contains("\"memberName\""))
        assertTrue(encoded.contains("\"loanProductName\""))
        assertTrue(encoded.contains("\"principalAmount\""))
        assertTrue(encoded.contains("\"disbursedDate\""))
        assertTrue(encoded.contains("\"interestRatePercent\""))
        assertTrue(encoded.contains("\"totalOutstanding\""))
        assertTrue(encoded.contains("\"totalOverdue\""))
        assertTrue(encoded.contains("\"status\""))
        assertTrue(encoded.contains("\"fineractLoanId\""))

        val decoded = json.decodeFromString(LoanDetailDto.serializer(), encoded)
        assertEquals(loanDetailDto, decoded)
    }

    @Test
    fun loanDetailDto_toleratesServerAddedFieldAndUnknownStatus_oldClientNeverCrashes() {
        val serverPayload = """
            {
              "id":9001,
              "memberId":5001,
              "memberName":"Peter Otieno",
              "loanProductName":"Standard Group Loan",
              "principalAmount":1500.0,
              "disbursedDate":"2026-03-01",
              "interestRatePercent":15.0,
              "totalOutstanding":1125.0,
              "totalOverdue":0.0,
              "status":"WRITTEN_OFF",
              "fineractLoanId":9001,
              "approvedPrincipal":1500.0
            }
        """.trimIndent()
        val decoded = json.decodeFromString(LoanDetailDto.serializer(), serverPayload)
        assertEquals(9001L, decoded.id)
        assertEquals(LoanAccountStatusDto.UNKNOWN, decoded.status)
    }

    // ---------- RepaymentScheduleRowDto ----------

    @Test
    fun repaymentScheduleRowDto_constructsWithAllFields() {
        assertEquals(1, scheduleRowDto.weekNumber)
        assertEquals("2026-03-08", scheduleRowDto.dueDate)
        assertEquals(187.5, scheduleRowDto.dueAmount)
        assertEquals(187.5, scheduleRowDto.paidAmount)
        assertEquals(1312.5, scheduleRowDto.balance)
        assertEquals(RepaymentRowStatusDto.PAID, scheduleRowDto.status)
    }

    @Test
    fun repaymentScheduleRowDto_statusDefaultsToUnknownWhenOmitted() {
        val dto = RepaymentScheduleRowDto(
            weekNumber = 2,
            dueDate = "2026-03-15",
            dueAmount = 187.5,
            paidAmount = 0.0,
            balance = 1500.0,
        )
        assertEquals(RepaymentRowStatusDto.UNKNOWN, dto.status)
    }

    @Test
    fun repaymentScheduleRowDto_equality() {
        assertEquals(scheduleRowDto.copy(), scheduleRowDto.copy())
    }

    @Test
    fun repaymentScheduleRowDto_serializationRoundTrips() {
        val encoded = json.encodeToString(RepaymentScheduleRowDto.serializer(), scheduleRowDto)
        val decoded = json.decodeFromString(RepaymentScheduleRowDto.serializer(), encoded)
        assertEquals(scheduleRowDto, decoded)
    }

    // ---------- RepaymentRowStatusDto (4 known + UNKNOWN, T7/EC30 fallback) ----------

    @Test
    fun repaymentRowStatusDto_hasExactlyFiveEntriesIncludingUnknownFallback() {
        assertEquals(5, RepaymentRowStatusDto.entries.size)
        assertTrue(RepaymentRowStatusDto.entries.contains(RepaymentRowStatusDto.UNKNOWN))
    }

    @Test
    fun repaymentRowStatusDto_decodesEachKnownWireValue() {
        val known = listOf(
            "PAID" to RepaymentRowStatusDto.PAID,
            "PARTIAL" to RepaymentRowStatusDto.PARTIAL,
            "UPCOMING" to RepaymentRowStatusDto.UPCOMING,
            "OVERDUE" to RepaymentRowStatusDto.OVERDUE,
        )
        known.forEach { (wire, expected) ->
            assertEquals(expected, json.decodeFromString(RepaymentRowStatusDto.serializer(), "\"$wire\""))
        }
    }

    @Test
    fun repaymentRowStatusDto_unknownServerValueCoercesToUnknownFallback_notCrash() {
        val decoded = json.decodeFromString(RepaymentRowStatusDto.serializer(), "\"WAIVED\"")
        assertEquals(RepaymentRowStatusDto.UNKNOWN, decoded)
    }

    // ---------- RepaymentTransactionDto ----------

    @Test
    fun repaymentTransactionDto_constructsWithAllFields() {
        assertEquals(7001L, transactionDto.id)
        assertEquals("repayment", transactionDto.type)
        assertEquals("2026-03-08", transactionDto.date)
        assertEquals(187.5, transactionDto.amount)
    }

    @Test
    fun repaymentTransactionDto_equality() {
        assertEquals(transactionDto.copy(), transactionDto.copy())
    }

    @Test
    fun repaymentTransactionDto_serializationRoundTrips() {
        val encoded = json.encodeToString(RepaymentTransactionDto.serializer(), transactionDto)
        assertTrue(encoded.contains("\"type\""))
        val decoded = json.decodeFromString(RepaymentTransactionDto.serializer(), encoded)
        assertEquals(transactionDto, decoded)
    }

    // ---------- LoanDetailResponseDto (composite envelope, `GET /loans/{loanId}`) ----------

    @Test
    fun loanDetailResponseDto_constructsWithLoanScheduleAndTransactions() {
        val response = LoanDetailResponseDto(
            loan = loanDetailDto,
            repaymentSchedule = listOf(scheduleRowDto),
            transactions = listOf(transactionDto),
        )
        assertEquals(loanDetailDto, response.loan)
        assertEquals(listOf(scheduleRowDto), response.repaymentSchedule)
        assertEquals(listOf(transactionDto), response.transactions)
    }

    @Test
    fun loanDetailResponseDto_scheduleAndTransactionsDefaultToEmptyListWhenOmitted() {
        val response = LoanDetailResponseDto(loan = loanDetailDto)
        assertTrue(response.repaymentSchedule.isEmpty())
        assertTrue(response.transactions.isEmpty())
    }

    @Test
    fun loanDetailResponseDto_serializationRoundTrips() {
        val response = LoanDetailResponseDto(
            loan = loanDetailDto,
            repaymentSchedule = listOf(scheduleRowDto),
            transactions = listOf(transactionDto),
        )
        val encoded = json.encodeToString(LoanDetailResponseDto.serializer(), response)
        assertTrue(encoded.contains("\"loan\""))
        assertTrue(encoded.contains("\"repaymentSchedule\""))
        assertTrue(encoded.contains("\"transactions\""))
        val decoded = json.decodeFromString(LoanDetailResponseDto.serializer(), encoded)
        assertEquals(response, decoded)
    }

    @Test
    fun loanDetailResponseDto_carriesSchemaVersion() {
        assertEquals(1, LoanDetailResponseDto.SCHEMA_VERSION)
    }
}
