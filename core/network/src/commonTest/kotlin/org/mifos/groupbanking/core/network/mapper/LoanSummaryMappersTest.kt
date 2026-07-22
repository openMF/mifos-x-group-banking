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

import org.mifos.groupbanking.core.model.LoanAccountStatus
import org.mifos.groupbanking.core.network.model.LoanAccountStatusDto
import org.mifos.groupbanking.core.network.model.LoanPageDto
import org.mifos.groupbanking.core.network.model.LoanSummaryDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * TDD RED-first coverage for the loan-list DTO -> domain mappers. Asserts
 * every field on `LoanSummaryDto` is mapped — no field left unmapped.
 */
class LoanSummaryMappersTest {

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

    private val graceDto = LoanSummaryDto(
        id = 9002L,
        memberId = 5002L,
        memberName = "Grace Wanjiku",
        memberPhotoUrl = "https://cdn.example.org/photos/5002.jpg",
        loanProductName = "Standard Group Loan",
        principalAmount = 3000.0,
        outstandingBalance = 2625.0,
        overdueAmount = 375.0,
        status = LoanAccountStatusDto.OVERDUE,
        nextRepaymentDate = null,
        isOverdue = true,
        fineractLoanId = 9002L,
    )

    // ---------- LoanSummaryDto -> LoanSummary ----------

    @Test
    fun toDomainModel_mapsEveryField() {
        val domain = peterDto.toDomainModel()
        assertEquals(9001L, domain.id)
        assertEquals(5001L, domain.memberId)
        assertEquals("Peter Otieno", domain.memberName)
        assertNull(domain.memberPhotoUrl)
        assertEquals("Standard Group Loan", domain.loanProductName)
        assertEquals(1500.0, domain.principalAmount)
        assertEquals(1125.0, domain.outstandingBalance)
        assertEquals(0.0, domain.overdueAmount)
        assertEquals(LoanAccountStatus.ACTIVE, domain.status)
        assertEquals("2026-05-12", domain.nextRepaymentDate)
        assertEquals(false, domain.isOverdue)
        assertEquals(9001L, domain.fineractLoanId)
    }

    @Test
    fun toDomainModel_mapsNullablePhotoAndRepaymentDateThrough() {
        val domain = graceDto.toDomainModel()
        assertEquals("https://cdn.example.org/photos/5002.jpg", domain.memberPhotoUrl)
        assertNull(domain.nextRepaymentDate)
        assertEquals(LoanAccountStatus.OVERDUE, domain.status)
        assertEquals(true, domain.isOverdue)
    }

    // ---------- List<LoanSummaryDto> -> List<LoanSummary> (batch) ----------

    @Test
    fun listToDomainModels_mapsEveryRowInDeclarationOrder() {
        val domainList = listOf(peterDto, graceDto).toDomainModels()
        assertEquals(2, domainList.size)
        assertEquals(9001L, domainList[0].id)
        assertEquals(9002L, domainList[1].id)
    }

    @Test
    fun listToDomainModels_emptyListMapsToEmptyList() {
        assertTrue(emptyList<LoanSummaryDto>().toDomainModels().isEmpty())
    }

    // ---------- LoanPageDto -> LoanPage ----------

    @Test
    fun pageToDomainModel_preservesTotalFilteredRecordsAndMapsLoans() {
        val pageDto = LoanPageDto(totalFilteredRecords = 12, pageItems = listOf(peterDto, graceDto))
        val page = pageDto.toDomainModel()
        assertEquals(12, page.totalFilteredRecords)
        assertEquals(2, page.loans.size)
        assertEquals(9001L, page.loans[0].id)
        assertEquals(9002L, page.loans[1].id)
    }

    @Test
    fun pageToDomainModel_emptyPageItemsMapsToEmptyLoans() {
        val pageDto = LoanPageDto(totalFilteredRecords = 0)
        val page = pageDto.toDomainModel()
        assertTrue(page.loans.isEmpty())
    }

    // ---------- LoanAccountStatusDto -> LoanAccountStatus (every value) ----------

    @Test
    fun statusToDomainModel_mapsEveryKnownValueAndUnknownFallback() {
        assertEquals(LoanAccountStatus.ACTIVE, LoanAccountStatusDto.ACTIVE.toDomainModel())
        assertEquals(LoanAccountStatus.OVERDUE, LoanAccountStatusDto.OVERDUE.toDomainModel())
        assertEquals(LoanAccountStatus.CLOSED, LoanAccountStatusDto.CLOSED.toDomainModel())
        assertEquals(LoanAccountStatus.PENDING, LoanAccountStatusDto.PENDING.toDomainModel())
        assertEquals(LoanAccountStatus.REJECTED, LoanAccountStatusDto.REJECTED.toDomainModel())
        assertEquals(LoanAccountStatus.UNKNOWN, LoanAccountStatusDto.UNKNOWN.toDomainModel())
    }
}
