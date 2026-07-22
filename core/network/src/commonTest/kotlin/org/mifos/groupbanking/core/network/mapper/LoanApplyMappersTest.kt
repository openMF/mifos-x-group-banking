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

import kotlinx.datetime.TimeZone
import org.mifos.groupbanking.core.model.ApplyLoanRequest
import org.mifos.groupbanking.core.model.LoanPurpose
import org.mifos.groupbanking.core.network.model.ApplyLoanResponseDto
import org.mifos.groupbanking.core.network.model.FineractStatusDto
import org.mifos.groupbanking.core.network.model.GroupCorpusRowDto
import org.mifos.groupbanking.core.network.model.GroupLoanConfigDto
import org.mifos.groupbanking.core.network.model.GroupMemberDto
import org.mifos.groupbanking.core.network.model.GroupMembersResponseDto
import org.mifos.groupbanking.core.network.model.LoanApplyTemplateDto
import org.mifos.groupbanking.core.network.model.LoanProductDto
import org.mifos.groupbanking.core.network.model.LoanPurposeDto
import org.mifos.groupbanking.core.network.model.MemberSavingsAccountRowDto
import org.mifos.groupbanking.core.network.model.MemberSavingsResponseDto
import org.mifos.groupbanking.core.network.model.SavingsAccountStatusDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * TDD RED-first coverage for the loan-apply DTO <-> domain mappers. Every field on every DTO
 * declared in `LoanApplyDto.kt` is asserted mapped.
 */
class LoanApplyMappersTest {

    // ---------- GroupMemberDto -> GroupMember ----------

    private val amaraMemberDto = GroupMemberDto(
        id = 5001L,
        displayName = "Amara Otieno",
        imagePresent = true,
    )

    @Test
    fun groupMemberDto_mapsToDomainModel_fineractClientIdDerivedFromId() {
        val domain = amaraMemberDto.toDomainModel()
        assertEquals(5001L, domain.id)
        assertEquals("Amara Otieno", domain.displayName)
        assertEquals(true, domain.imagePresent)
        assertEquals(5001L, domain.fineractClientId)
        assertEquals(domain.id, domain.fineractClientId)
    }

    @Test
    fun groupMemberDtoList_toDomainModels_mapsEveryRowInOrder() {
        val dtos = listOf(
            amaraMemberDto,
            GroupMemberDto(id = 5002L, displayName = "Juma Kamau", imagePresent = false),
        )
        val domains = dtos.toDomainModels()
        assertEquals(2, domains.size)
        assertEquals(5001L, domains[0].id)
        assertEquals(5002L, domains[1].id)
    }

    @Test
    fun groupMembersResponseDto_toDomainModels_unwrapsClientMembers() {
        val response = GroupMembersResponseDto(clientMembers = listOf(amaraMemberDto))
        val domains = response.toDomainModels()
        assertEquals(1, domains.size)
        assertEquals("Amara Otieno", domains[0].displayName)
    }

    // ---------- LoanProductDto -> LoanProduct ----------

    private val productDto = LoanProductDto(
        id = 1L,
        name = "Group Weekly Loan",
        shortName = "GWL",
        principal = 20000.0,
        minPrincipal = 5000.0,
        maxPrincipal = 100000.0,
        numberOfRepayments = 12,
        interestRatePerPeriod = 2.5,
    )

    @Test
    fun loanProductDto_mapsToDomainModel_everyFieldMapped() {
        val domain = productDto.toDomainModel()
        assertEquals(1L, domain.id)
        assertEquals("Group Weekly Loan", domain.name)
        assertEquals("GWL", domain.shortName)
        assertEquals(20000.0, domain.principal)
        assertEquals(5000.0, domain.minPrincipal)
        assertEquals(100000.0, domain.maxPrincipal)
        assertEquals(12, domain.numberOfRepayments)
        assertEquals(2.5, domain.interestRatePerPeriod)
    }

    @Test
    fun loanProductDtoList_toDomainModels_mapsEveryRow() {
        val domains = listOf(productDto, productDto.copy(id = 2L)).toDomainModels()
        assertEquals(2, domains.size)
        assertEquals(1L, domains[0].id)
        assertEquals(2L, domains[1].id)
    }

    // ---------- composite template ----------

    private val templateDto = LoanApplyTemplateDto(
        principal = 20000.0,
        numberOfRepayments = 12,
        interestRatePerPeriod = 2.5,
        interestType = FineractStatusDto(id = 0, value = "Declining Balance"),
        amortizationType = FineractStatusDto(id = 1, value = "Equal installments"),
        repaymentEvery = 1,
    )

    private val savingsResponse = MemberSavingsResponseDto(
        savingsAccounts = listOf(
            MemberSavingsAccountRowDto(id = 9001L, accountBalance = 1250.50, status = SavingsAccountStatusDto("Active")),
            MemberSavingsAccountRowDto(id = 9002L, accountBalance = 300.0, status = SavingsAccountStatusDto("Active")),
        ),
    )

    private val corpusRowDto = GroupCorpusRowDto(corpusBalance = 45000.0, lastUpdated = "2026-07-20")
    private val loanConfigDto = GroupLoanConfigDto(loanMultiplier = 3.0, maxLoanAmount = 100000.0, meetingFrequency = "WEEKLY")

    @Test
    fun loanApplyTemplateDto_composesFullDomainTemplate_everyFieldMapped() {
        val domain = templateDto.toDomainModel(
            products = listOf(productDto),
            savings = savingsResponse,
            corpus = corpusRowDto,
            config = loanConfigDto,
        )
        assertEquals(1, domain.products.size)
        assertEquals(20000.0, domain.principal)
        assertEquals(12, domain.numberOfRepayments)
        assertEquals(2.5, domain.interestRatePerPeriod)
        assertEquals(0, domain.interestType.id)
        assertEquals("Declining Balance", domain.interestType.value)
        assertEquals(1, domain.amortizationType.id)
        assertEquals("Equal installments", domain.amortizationType.value)
        assertEquals(1, domain.repaymentEvery)
        assertEquals(1550.50, domain.memberSavingsBalance)
        assertEquals(45000.0, domain.groupCorpusBalance)
        assertEquals(3.0, domain.loanMultiplier)
        assertEquals(100000.0, domain.maxLoanAmount)
    }

    @Test
    fun loanApplyTemplate_maxEligibleAmount_capsAtMultiplierWhenBelowMaxLoanAmount() {
        val domain = templateDto.toDomainModel(
            products = listOf(productDto),
            savings = savingsResponse,
            corpus = corpusRowDto,
            config = loanConfigDto,
        )
        // savings (1550.50) * multiplier (3.0) = 4651.50, well below maxLoanAmount (100000.0)
        assertEquals(4651.50, domain.maxEligibleAmount)
    }

    @Test
    fun loanApplyTemplate_maxEligibleAmount_capsAtMaxLoanAmountWhenMultiplierExceedsIt() {
        val richSavings = MemberSavingsResponseDto(
            savingsAccounts = listOf(
                MemberSavingsAccountRowDto(id = 9003L, accountBalance = 1_000_000.0, status = SavingsAccountStatusDto("Active")),
            ),
        )
        val domain = templateDto.toDomainModel(
            products = listOf(productDto),
            savings = richSavings,
            corpus = corpusRowDto,
            config = loanConfigDto,
        )
        // savings (1_000_000.0) * multiplier (3.0) = 3_000_000.0, capped at maxLoanAmount (100000.0)
        assertEquals(100000.0, domain.maxEligibleAmount)
    }

    // ---------- ApplyLoanRequest -> ApplyLoanRequestDto ----------

    @OptIn(ExperimentalTime::class)
    private val pinnedNow = Instant.parse("2026-07-21T09:00:00Z")

    private val applyRequest = ApplyLoanRequest(
        memberId = 5001L,
        productId = 1L,
        amount = 20000.0,
        durationWeeks = 12,
        purpose = LoanPurpose.BUSINESS,
        groupId = 777L,
    )

    @OptIn(ExperimentalTime::class)
    @Test
    fun applyLoanRequest_toDto_resolvesLiteralFineractBody() {
        val dto = applyRequest.toDto(product = productDto.toDomainModel(), now = pinnedNow, timeZone = TimeZone.UTC)
        assertEquals(5001L, dto.clientId)
        assertEquals(1L, dto.productId)
        assertEquals(20000.0, dto.principal)
        assertEquals(12, dto.loanTermFrequency)
        assertEquals(12, dto.numberOfRepayments)
        assertEquals(2.5, dto.interestRatePerPeriod)
        assertEquals(3, dto.loanPurposeId)
        assertEquals("21 July 2026", dto.expectedDisbursementDate)
        assertEquals("21 July 2026", dto.submittedOnDate)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun applyLoanRequest_toDto_lookupConstantsMatchApiYamlLiterals() {
        val dto = applyRequest.toDto(product = productDto.toDomainModel(), now = pinnedNow, timeZone = TimeZone.UTC)
        assertEquals(1, dto.repaymentEvery)
        assertEquals(1, dto.transactionProcessingStrategyId)
        assertEquals("Weeks", dto.loanTermFrequencyType.value)
        assertEquals("Weeks", dto.repaymentFrequencyType.value)
        assertEquals("Equal installments", dto.amortizationType.value)
        assertEquals("Declining Balance", dto.interestType.value)
        assertEquals("Same as repayment period", dto.interestCalculationPeriodType.value)
    }

    // ---------- ApplyLoanResponseDto -> LoanApplicationResult ----------

    @Test
    fun applyLoanResponseDto_mapsToDomainModel_everyFieldMapped() {
        val dto = ApplyLoanResponseDto(officeId = 1L, clientId = 5001L, loanId = 9101L, resourceId = 9101L)
        val domain = dto.toDomainModel()
        assertEquals(1L, domain.officeId)
        assertEquals(5001L, domain.clientId)
        assertEquals(9101L, domain.loanId)
        assertEquals(9101L, domain.resourceId)
    }

    // ---------- LoanPurpose <-> LoanPurposeDto ----------
    // (extended by loan-request to 8 known values + UNKNOWN — see LoanApply.kt / LoanApplyDto.kt
    // kdoc, PP-1 registry-wins precedent)

    @Test
    fun loanPurposeDto_toDomainModel_everyKnownValueMapped() {
        assertEquals(LoanPurpose.MEDICAL, LoanPurposeDto.MEDICAL.toDomainModel())
        assertEquals(LoanPurpose.EDUCATION, LoanPurposeDto.EDUCATION.toDomainModel())
        assertEquals(LoanPurpose.BUSINESS, LoanPurposeDto.BUSINESS.toDomainModel())
        assertEquals(LoanPurpose.EMERGENCY, LoanPurposeDto.EMERGENCY.toDomainModel())
        assertEquals(LoanPurpose.OTHER, LoanPurposeDto.OTHER.toDomainModel())
        assertEquals(LoanPurpose.SCHOOL_FEES, LoanPurposeDto.SCHOOL_FEES.toDomainModel())
        assertEquals(LoanPurpose.FARMING, LoanPurposeDto.FARMING.toDomainModel())
        assertEquals(LoanPurpose.HOME_IMPROVEMENT, LoanPurposeDto.HOME_IMPROVEMENT.toDomainModel())
        assertEquals(LoanPurpose.UNKNOWN, LoanPurposeDto.UNKNOWN.toDomainModel())
    }

    @Test
    fun loanPurpose_toDto_everyKnownValueMapped() {
        assertEquals(LoanPurposeDto.MEDICAL, LoanPurpose.MEDICAL.toDto())
        assertEquals(LoanPurposeDto.EDUCATION, LoanPurpose.EDUCATION.toDto())
        assertEquals(LoanPurposeDto.BUSINESS, LoanPurpose.BUSINESS.toDto())
        assertEquals(LoanPurposeDto.EMERGENCY, LoanPurpose.EMERGENCY.toDto())
        assertEquals(LoanPurposeDto.OTHER, LoanPurpose.OTHER.toDto())
        assertEquals(LoanPurposeDto.SCHOOL_FEES, LoanPurpose.SCHOOL_FEES.toDto())
        assertEquals(LoanPurposeDto.FARMING, LoanPurpose.FARMING.toDto())
        assertEquals(LoanPurposeDto.HOME_IMPROVEMENT, LoanPurpose.HOME_IMPROVEMENT.toDto())
        assertEquals(LoanPurposeDto.UNKNOWN, LoanPurpose.UNKNOWN.toDto())
    }

    @Test
    fun loanPurpose_fineractPurposeId_sequentialAssignmentMatchesDeclarationOrder() {
        assertEquals(1, LoanPurpose.MEDICAL.fineractPurposeId)
        assertEquals(2, LoanPurpose.EDUCATION.fineractPurposeId)
        assertEquals(3, LoanPurpose.BUSINESS.fineractPurposeId)
        assertEquals(4, LoanPurpose.EMERGENCY.fineractPurposeId)
        assertEquals(5, LoanPurpose.OTHER.fineractPurposeId)
        assertEquals(6, LoanPurpose.SCHOOL_FEES.fineractPurposeId)
        assertEquals(7, LoanPurpose.FARMING.fineractPurposeId)
        assertEquals(8, LoanPurpose.HOME_IMPROVEMENT.fineractPurposeId)
        assertEquals(0, LoanPurpose.UNKNOWN.fineractPurposeId)
    }
}
