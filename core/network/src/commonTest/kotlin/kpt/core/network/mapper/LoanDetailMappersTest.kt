/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.network.mapper

import kpt.core.model.LoanAccountStatus
import kpt.core.model.RepaymentRowStatus
import kpt.core.network.model.LoanAccountStatusDto
import kpt.core.network.model.LoanDetailDto
import kpt.core.network.model.LoanDetailResponseDto
import kpt.core.network.model.RepaymentRowStatusDto
import kpt.core.network.model.RepaymentScheduleRowDto
import kpt.core.network.model.RepaymentTransactionDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD RED-first coverage for the loan-detail DTO -> domain mappers. Asserts
 * every field on `LoanDetailDto` / `RepaymentScheduleRowDto` /
 * `RepaymentTransactionDto` / `LoanDetailResponseDto` is mapped — no field
 * left unmapped.
 */
class LoanDetailMappersTest {

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

    private val upcomingRowDto = RepaymentScheduleRowDto(
        weekNumber = 2,
        dueDate = "2026-03-15",
        dueAmount = 187.5,
        paidAmount = 0.0,
        balance = 1500.0,
        status = RepaymentRowStatusDto.UPCOMING,
    )

    private val transactionDto = RepaymentTransactionDto(
        id = 7001L,
        type = "repayment",
        date = "2026-03-08",
        amount = 187.5,
    )

    // ---------- LoanDetailDto -> LoanDetail ----------

    @Test
    fun toDomainModel_mapsEveryField() {
        val domain = loanDetailDto.toDomainModel()
        assertEquals(9001L, domain.id)
        assertEquals(5001L, domain.memberId)
        assertEquals("Peter Otieno", domain.memberName)
        assertEquals("Standard Group Loan", domain.loanProductName)
        assertEquals(1500.0, domain.principalAmount)
        assertEquals("2026-03-01", domain.disbursedDate)
        assertEquals(15.0, domain.interestRatePercent)
        assertEquals(1125.0, domain.totalOutstanding)
        assertEquals(0.0, domain.totalOverdue)
        assertEquals(LoanAccountStatus.ACTIVE, domain.status)
        assertEquals(9001L, domain.fineractLoanId)
    }

    // ---------- RepaymentScheduleRowDto -> RepaymentScheduleRow ----------

    @Test
    fun scheduleRowToDomainModel_mapsEveryField() {
        val domain = scheduleRowDto.toDomainModel()
        assertEquals(1, domain.weekNumber)
        assertEquals("2026-03-08", domain.dueDate)
        assertEquals(187.5, domain.dueAmount)
        assertEquals(187.5, domain.paidAmount)
        assertEquals(1312.5, domain.balance)
        assertEquals(RepaymentRowStatus.PAID, domain.status)
    }

    @Test
    fun scheduleRowListToDomainModels_mapsEveryRowInDeclarationOrder() {
        val domainList = listOf(scheduleRowDto, upcomingRowDto).toDomainModels()
        assertEquals(2, domainList.size)
        assertEquals(1, domainList[0].weekNumber)
        assertEquals(2, domainList[1].weekNumber)
        assertEquals(RepaymentRowStatus.UPCOMING, domainList[1].status)
    }

    @Test
    fun scheduleRowListToDomainModels_emptyListMapsToEmptyList() {
        assertTrue(emptyList<RepaymentScheduleRowDto>().toDomainModels().isEmpty())
    }

    // ---------- RepaymentTransactionDto -> RepaymentTransaction ----------

    @Test
    fun transactionToDomainModel_mapsEveryField() {
        val domain = transactionDto.toDomainModel()
        assertEquals(7001L, domain.id)
        assertEquals("repayment", domain.type)
        assertEquals("2026-03-08", domain.date)
        assertEquals(187.5, domain.amount)
    }

    @Test
    fun transactionListToDomainModels_emptyListMapsToEmptyList() {
        assertTrue(emptyList<RepaymentTransactionDto>().toDomainModels().isEmpty())
    }

    // ---------- LoanDetailResponseDto -> LoanDetailResponse ----------

    @Test
    fun responseToDomainModel_mapsLoanScheduleAndRenamesTransactionsToRepaymentHistory() {
        val responseDto = LoanDetailResponseDto(
            loan = loanDetailDto,
            repaymentSchedule = listOf(scheduleRowDto, upcomingRowDto),
            transactions = listOf(transactionDto),
        )
        val domain = responseDto.toDomainModel()
        assertEquals(9001L, domain.loan.id)
        assertEquals(2, domain.repaymentSchedule.size)
        assertEquals(1, domain.repaymentHistory.size)
        assertEquals(7001L, domain.repaymentHistory[0].id)
    }

    @Test
    fun responseToDomainModel_emptyScheduleAndTransactionsMapToEmptyLists() {
        val responseDto = LoanDetailResponseDto(loan = loanDetailDto)
        val domain = responseDto.toDomainModel()
        assertTrue(domain.repaymentSchedule.isEmpty())
        assertTrue(domain.repaymentHistory.isEmpty())
    }

    // ---------- RepaymentRowStatusDto -> RepaymentRowStatus (every value) ----------

    @Test
    fun rowStatusToDomainModel_mapsEveryKnownValueAndUnknownFallback() {
        assertEquals(RepaymentRowStatus.PAID, RepaymentRowStatusDto.PAID.toDomainModel())
        assertEquals(RepaymentRowStatus.PARTIAL, RepaymentRowStatusDto.PARTIAL.toDomainModel())
        assertEquals(RepaymentRowStatus.UPCOMING, RepaymentRowStatusDto.UPCOMING.toDomainModel())
        assertEquals(RepaymentRowStatus.OVERDUE, RepaymentRowStatusDto.OVERDUE.toDomainModel())
        assertEquals(RepaymentRowStatus.UNKNOWN, RepaymentRowStatusDto.UNKNOWN.toDomainModel())
    }
}
