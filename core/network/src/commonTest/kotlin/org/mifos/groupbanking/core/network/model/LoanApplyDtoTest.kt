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
 * TDD RED-first coverage for the loan-apply wire contract (`get_group_members` +
 * `get_loan_products` + `get_loan_template` + `get_member_savings` + `get_group_corpus` +
 * `get_group_config` + `create_new_loan`). See API.md#dtos.
 */
class LoanApplyDtoTest {

    // Server-parity Json config: mirrors NetworkModule's client config (ignoreUnknownKeys +
    // coerceInputValues) so these tests prove EC30 cross-version tolerance, not just
    // happy-path round-tripping.
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    // ---------- GroupMemberDto / GroupMembersResponseDto ----------

    private val amaraMemberDto = GroupMemberDto(
        id = 5001L,
        displayName = "Amara Otieno",
        imagePresent = true,
    )

    @Test
    fun groupMemberDto_constructsWithAllFields() {
        assertEquals(5001L, amaraMemberDto.id)
        assertEquals("Amara Otieno", amaraMemberDto.displayName)
        assertTrue(amaraMemberDto.imagePresent)
    }

    @Test
    fun groupMemberDto_equality() {
        assertEquals(amaraMemberDto.copy(), amaraMemberDto.copy())
    }

    @Test
    fun groupMemberDto_carriesSchemaVersion() {
        assertEquals(1, GroupMemberDto.SCHEMA_VERSION)
    }

    @Test
    fun groupMemberDto_serializationRoundTrips_wireFieldNamesAreCamelCase() {
        val encoded = json.encodeToString(GroupMemberDto.serializer(), amaraMemberDto)
        assertTrue(encoded.contains("\"id\""))
        assertTrue(encoded.contains("\"displayName\""))
        assertTrue(encoded.contains("\"imagePresent\""))
        val decoded = json.decodeFromString(GroupMemberDto.serializer(), encoded)
        assertEquals(amaraMemberDto, decoded)
    }

    @Test
    fun groupMembersResponseDto_clientMembersDefaultsToEmptyList() {
        val payload = """{"clientMembers":[]}"""
        val decoded = json.decodeFromString(GroupMembersResponseDto.serializer(), payload)
        assertTrue(decoded.clientMembers.isEmpty())
    }

    @Test
    fun groupMembersResponseDto_decodesFromLiteralGroupMembersShape() {
        val payload = """
            {"clientMembers":[
              {"id":5001,"displayName":"Amara Otieno","imagePresent":true},
              {"id":5002,"displayName":"Juma Kamau","imagePresent":false}
            ]}
        """.trimIndent()
        val decoded = json.decodeFromString(GroupMembersResponseDto.serializer(), payload)
        assertEquals(2, decoded.clientMembers.size)
        assertEquals(5002L, decoded.clientMembers[1].id)
    }

    // ---------- LoanProductDto ----------

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
    fun loanProductDto_constructsWithAllFields() {
        assertEquals(1L, productDto.id)
        assertEquals("Group Weekly Loan", productDto.name)
        assertEquals("GWL", productDto.shortName)
        assertEquals(20000.0, productDto.principal)
        assertEquals(5000.0, productDto.minPrincipal)
        assertEquals(100000.0, productDto.maxPrincipal)
        assertEquals(12, productDto.numberOfRepayments)
        assertEquals(2.5, productDto.interestRatePerPeriod)
    }

    @Test
    fun loanProductDto_equality() {
        assertEquals(productDto.copy(), productDto.copy())
    }

    @Test
    fun loanProductDto_carriesSchemaVersion() {
        assertEquals(1, LoanProductDto.SCHEMA_VERSION)
    }

    @Test
    fun loanProductDto_serializationRoundTrips() {
        val encoded = json.encodeToString(LoanProductDto.serializer(), productDto)
        assertTrue(encoded.contains("\"principal\""))
        assertTrue(encoded.contains("\"numberOfRepayments\""))
        val decoded = json.decodeFromString(LoanProductDto.serializer(), encoded)
        assertEquals(productDto, decoded)
    }

    // ---------- LoanApplyTemplateDto ----------

    private val templateDto = LoanApplyTemplateDto(
        principal = 20000.0,
        numberOfRepayments = 12,
        interestRatePerPeriod = 2.5,
        interestType = FineractStatusDto(id = 0, value = "Declining Balance"),
        amortizationType = FineractStatusDto(id = 1, value = "Equal installments"),
        repaymentEvery = 1,
    )

    @Test
    fun loanApplyTemplateDto_constructsWithAllFields() {
        assertEquals(20000.0, templateDto.principal)
        assertEquals(12, templateDto.numberOfRepayments)
        assertEquals(2.5, templateDto.interestRatePerPeriod)
        assertEquals("Declining Balance", templateDto.interestType.value)
        assertEquals("Equal installments", templateDto.amortizationType.value)
        assertEquals(1, templateDto.repaymentEvery)
    }

    @Test
    fun loanApplyTemplateDto_equality() {
        assertEquals(templateDto.copy(), templateDto.copy())
    }

    @Test
    fun loanApplyTemplateDto_serializationRoundTrips_reusesSharedFineractStatusDto() {
        val encoded = json.encodeToString(LoanApplyTemplateDto.serializer(), templateDto)
        assertTrue(encoded.contains("\"interestType\""))
        assertTrue(encoded.contains("\"amortizationType\""))
        val decoded = json.decodeFromString(LoanApplyTemplateDto.serializer(), encoded)
        assertEquals(templateDto, decoded)
    }

    // ---------- MemberSavingsAccountRowDto / MemberSavingsResponseDto ----------

    private val savingsRowDto = MemberSavingsAccountRowDto(
        id = 9001L,
        accountBalance = 1250.50,
        status = SavingsAccountStatusDto(value = "Active"),
    )

    @Test
    fun memberSavingsAccountRowDto_constructsWithAllFields() {
        assertEquals(9001L, savingsRowDto.id)
        assertEquals(1250.50, savingsRowDto.accountBalance)
        assertEquals("Active", savingsRowDto.status.value)
    }

    @Test
    fun memberSavingsAccountRowDto_equality() {
        assertEquals(savingsRowDto.copy(), savingsRowDto.copy())
    }

    @Test
    fun memberSavingsResponseDto_savingsAccountsDefaultsToEmptyList() {
        val decoded = json.decodeFromString(
            MemberSavingsResponseDto.serializer(),
            """{"savingsAccounts":[]}""",
        )
        assertTrue(decoded.savingsAccounts.isEmpty())
    }

    @Test
    fun memberSavingsResponseDto_decodesMultipleAccounts() {
        val payload = """
            {"savingsAccounts":[
              {"id":9001,"accountBalance":1250.50,"status":{"value":"Active"}},
              {"id":9002,"accountBalance":300.0,"status":{"value":"Active"}}
            ]}
        """.trimIndent()
        val decoded = json.decodeFromString(MemberSavingsResponseDto.serializer(), payload)
        assertEquals(2, decoded.savingsAccounts.size)
        assertEquals(1550.50, decoded.savingsAccounts.sumOf { it.accountBalance })
    }

    // ---------- GroupCorpusRowDto ----------

    private val corpusRowDto = GroupCorpusRowDto(
        corpusBalance = 45000.0,
        lastUpdated = "2026-07-20",
    )

    @Test
    fun groupCorpusRowDto_constructsWithAllFields() {
        assertEquals(45000.0, corpusRowDto.corpusBalance)
        assertEquals("2026-07-20", corpusRowDto.lastUpdated)
    }

    @Test
    fun groupCorpusRowDto_equality() {
        assertEquals(corpusRowDto.copy(), corpusRowDto.copy())
    }

    @Test
    fun groupCorpusRowDto_serializationUsesSnakeCaseWireFieldNames() {
        val encoded = json.encodeToString(GroupCorpusRowDto.serializer(), corpusRowDto)
        assertTrue(encoded.contains("\"corpus_balance\""))
        assertTrue(encoded.contains("\"last_updated\""))
        val decoded = json.decodeFromString(GroupCorpusRowDto.serializer(), encoded)
        assertEquals(corpusRowDto, decoded)
    }

    // ---------- GroupLoanConfigDto ----------

    private val loanConfigDto = GroupLoanConfigDto(
        loanMultiplier = 3.0,
        maxLoanAmount = 100000.0,
        meetingFrequency = "WEEKLY",
    )

    @Test
    fun groupLoanConfigDto_constructsWithAllFields() {
        assertEquals(3.0, loanConfigDto.loanMultiplier)
        assertEquals(100000.0, loanConfigDto.maxLoanAmount)
        assertEquals("WEEKLY", loanConfigDto.meetingFrequency)
    }

    @Test
    fun groupLoanConfigDto_equality() {
        assertEquals(loanConfigDto.copy(), loanConfigDto.copy())
    }

    @Test
    fun groupLoanConfigDto_serializationUsesSnakeCaseWireFieldNames() {
        val encoded = json.encodeToString(GroupLoanConfigDto.serializer(), loanConfigDto)
        assertTrue(encoded.contains("\"loan_multiplier\""))
        assertTrue(encoded.contains("\"max_loan_amount\""))
        assertTrue(encoded.contains("\"meeting_frequency\""))
        val decoded = json.decodeFromString(GroupLoanConfigDto.serializer(), encoded)
        assertEquals(loanConfigDto, decoded)
    }

    // ---------- ApplyLoanRequestDto ----------

    private val requestDto = ApplyLoanRequestDto(
        clientId = 5001L,
        productId = 1L,
        principal = 20000.0,
        loanTermFrequency = 12,
        numberOfRepayments = 12,
        interestRatePerPeriod = 2.5,
        expectedDisbursementDate = "21 July 2026",
        submittedOnDate = "21 July 2026",
        loanPurposeId = 3,
    )

    @Test
    fun applyLoanRequestDto_constructsWithAllFields() {
        assertEquals(5001L, requestDto.clientId)
        assertEquals(1L, requestDto.productId)
        assertEquals(20000.0, requestDto.principal)
        assertEquals(12, requestDto.loanTermFrequency)
        assertEquals(12, requestDto.numberOfRepayments)
        assertEquals(2.5, requestDto.interestRatePerPeriod)
        assertEquals("21 July 2026", requestDto.expectedDisbursementDate)
        assertEquals("21 July 2026", requestDto.submittedOnDate)
        assertEquals(3, requestDto.loanPurposeId)
    }

    @Test
    fun applyLoanRequestDto_lookupFieldsAndConstantsDefaultToLiteralApiYamlValues() {
        assertEquals(1, requestDto.loanTermFrequencyType.id)
        assertEquals("Weeks", requestDto.loanTermFrequencyType.value)
        assertEquals(1, requestDto.repaymentEvery)
        assertEquals(1, requestDto.repaymentFrequencyType.id)
        assertEquals("Weeks", requestDto.repaymentFrequencyType.value)
        assertEquals("Equal installments", requestDto.amortizationType.value)
        assertEquals("Declining Balance", requestDto.interestType.value)
        assertEquals("Same as repayment period", requestDto.interestCalculationPeriodType.value)
        assertEquals(1, requestDto.transactionProcessingStrategyId)
    }

    @Test
    fun applyLoanRequestDto_equality() {
        assertEquals(requestDto.copy(), requestDto.copy())
    }

    @Test
    fun applyLoanRequestDto_carriesSchemaVersion() {
        assertEquals(1, ApplyLoanRequestDto.SCHEMA_VERSION)
    }

    @Test
    fun applyLoanRequestDto_serializationRoundTrips() {
        val encoded = json.encodeToString(ApplyLoanRequestDto.serializer(), requestDto)
        assertTrue(encoded.contains("\"clientId\""))
        assertTrue(encoded.contains("\"loanPurposeId\""))
        val decoded = json.decodeFromString(ApplyLoanRequestDto.serializer(), encoded)
        assertEquals(requestDto, decoded)
    }

    // ---------- ApplyLoanResponseDto ----------

    private val responseDto = ApplyLoanResponseDto(
        officeId = 1L,
        clientId = 5001L,
        loanId = 9101L,
        resourceId = 9101L,
    )

    @Test
    fun applyLoanResponseDto_constructsWithAllFields() {
        assertEquals(1L, responseDto.officeId)
        assertEquals(5001L, responseDto.clientId)
        assertEquals(9101L, responseDto.loanId)
        assertEquals(9101L, responseDto.resourceId)
    }

    @Test
    fun applyLoanResponseDto_equality() {
        assertEquals(responseDto.copy(), responseDto.copy())
    }

    @Test
    fun applyLoanResponseDto_carriesSchemaVersion() {
        assertEquals(1, ApplyLoanResponseDto.SCHEMA_VERSION)
    }

    @Test
    fun applyLoanResponseDto_decodesFromLiteralCreateNewLoanShape() {
        val payload = """{"officeId":1,"clientId":5001,"loanId":9101,"resourceId":9101}"""
        val decoded = json.decodeFromString(ApplyLoanResponseDto.serializer(), payload)
        assertEquals(responseDto, decoded)
    }

    // ---------- LoanPurposeDto (originally 5 known + UNKNOWN; extended to 8 known + UNKNOWN by
    // loan-request per PP-1 — see LoanApplyDto.kt kdoc) ----------

    @Test
    fun loanPurposeDto_hasExactlyNineEntriesIncludingUnknownFallback() {
        assertEquals(9, LoanPurposeDto.entries.size)
        assertTrue(LoanPurposeDto.entries.contains(LoanPurposeDto.UNKNOWN))
    }

    @Test
    fun loanPurposeDto_decodesEachKnownWireValue() {
        val known = listOf(
            "MEDICAL" to LoanPurposeDto.MEDICAL,
            "EDUCATION" to LoanPurposeDto.EDUCATION,
            "BUSINESS" to LoanPurposeDto.BUSINESS,
            "EMERGENCY" to LoanPurposeDto.EMERGENCY,
            "OTHER" to LoanPurposeDto.OTHER,
            "SCHOOL_FEES" to LoanPurposeDto.SCHOOL_FEES,
            "FARMING" to LoanPurposeDto.FARMING,
            "HOME_IMPROVEMENT" to LoanPurposeDto.HOME_IMPROVEMENT,
        )
        known.forEach { (wire, expected) ->
            assertEquals(expected, json.decodeFromString(LoanPurposeDto.serializer(), "\"$wire\""))
        }
    }

    @Test
    fun loanPurposeDto_unknownServerValueCoercesToUnknownFallback_notCrash() {
        // HOME_IMPROVEMENT is now a REAL known value (loan-request extension) — probe a
        // genuinely-unmodeled future value instead.
        val decoded = json.decodeFromString(LoanPurposeDto.serializer(), "\"DEBT_CONSOLIDATION\"")
        assertEquals(LoanPurposeDto.UNKNOWN, decoded)
    }

    // ---------- Cross-version safety fixture (T7/EC30) ----------

    @Test
    fun applyLoanResponseDto_toleratesServerAddedField_oldClientNeverCrashes() {
        // Simulates a staggered rollout: the server has shipped a NEW field this (old) client
        // schema does not know about. Decoding MUST succeed, never throw.
        val serverPayload = """
            {
              "officeId":1,
              "clientId":5001,
              "loanId":9101,
              "resourceId":9101,
              "loanStatus":"submitted_and_pending_approval"
            }
        """.trimIndent()
        val decoded = json.decodeFromString(ApplyLoanResponseDto.serializer(), serverPayload)
        assertEquals(9101L, decoded.loanId)
    }
}
