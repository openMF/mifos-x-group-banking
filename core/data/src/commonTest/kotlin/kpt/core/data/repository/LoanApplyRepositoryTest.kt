/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.data.repository

import kotlinx.coroutines.test.runTest
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.model.ApplyLoanRequest
import kpt.core.model.LoanProduct
import kpt.core.model.LoanPurpose
import kpt.core.network.model.ApplyLoanRequestDto
import kpt.core.network.model.ApplyLoanResponseDto
import kpt.core.network.model.FineractStatusDto
import kpt.core.network.model.GroupCorpusRowDto
import kpt.core.network.model.GroupLoanConfigDto
import kpt.core.network.model.GroupMemberDto
import kpt.core.network.model.GroupMembersResponseDto
import kpt.core.network.model.LoanApplyTemplateDto
import kpt.core.network.model.LoanProductDto
import kpt.core.network.model.MemberSavingsAccountRowDto
import kpt.core.network.model.MemberSavingsResponseDto
import kpt.core.network.model.SavingsAccountStatusDto
import kpt.core.network.service.loanapply.LoanApplyApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakeLoanApplyApi(
    private val groupMembersResult: NetworkResult<GroupMembersResponseDto, NetworkError>? = null,
    private val loanProductsResult: NetworkResult<List<LoanProductDto>, NetworkError>? = null,
    private val loanTemplateResult: NetworkResult<LoanApplyTemplateDto, NetworkError>? = null,
    private val memberSavingsResult: NetworkResult<MemberSavingsResponseDto, NetworkError>? = null,
    private val groupCorpusResult: NetworkResult<GroupCorpusRowDto, NetworkError>? = null,
    private val groupLoanConfigResult: NetworkResult<GroupLoanConfigDto, NetworkError>? = null,
    private val applyLoanResult: NetworkResult<ApplyLoanResponseDto, NetworkError>? = null,
) : LoanApplyApi {

    var lastApplyLoanRequest: ApplyLoanRequestDto? = null
    val callOrder = mutableListOf<String>()

    override suspend fun getGroupMembers(groupId: Long): NetworkResult<GroupMembersResponseDto, NetworkError> {
        callOrder += "getGroupMembers"
        return groupMembersResult ?: error("groupMembersResult not stubbed")
    }

    override suspend fun getLoanProducts(): NetworkResult<List<LoanProductDto>, NetworkError> {
        callOrder += "getLoanProducts"
        return loanProductsResult ?: error("loanProductsResult not stubbed")
    }

    override suspend fun getLoanTemplate(
        clientId: Long,
        productId: Long,
        templateType: String,
    ): NetworkResult<LoanApplyTemplateDto, NetworkError> {
        callOrder += "getLoanTemplate"
        return loanTemplateResult ?: error("loanTemplateResult not stubbed")
    }

    override suspend fun getMemberSavings(clientId: Long): NetworkResult<MemberSavingsResponseDto, NetworkError> {
        callOrder += "getMemberSavings"
        return memberSavingsResult ?: error("memberSavingsResult not stubbed")
    }

    override suspend fun getGroupCorpus(groupId: Long): NetworkResult<GroupCorpusRowDto, NetworkError> {
        callOrder += "getGroupCorpus"
        return groupCorpusResult ?: error("groupCorpusResult not stubbed")
    }

    override suspend fun getGroupLoanConfig(groupId: Long): NetworkResult<GroupLoanConfigDto, NetworkError> {
        callOrder += "getGroupLoanConfig"
        return groupLoanConfigResult ?: error("groupLoanConfigResult not stubbed")
    }

    override suspend fun applyLoan(
        request: ApplyLoanRequestDto,
    ): NetworkResult<ApplyLoanResponseDto, NetworkError> {
        lastApplyLoanRequest = request
        callOrder += "applyLoan"
        return applyLoanResult ?: error("applyLoanResult not stubbed")
    }
}

/**
 * TDD RED-first coverage for [LoanApplyRepository] / [LoanApplyRepositoryImpl]. No try-catch
 * anywhere in the repository under test (Mandatory Rule 4) — [LoanApplyRepositoryImpl.loadTemplate]
 * fans 5 reads out in parallel via `coroutineScope`/`async` and short-circuits on the first
 * [NetworkResult.Error] encountered (declaration order: products, template, savings, corpus,
 * config); every other method is a plain `when` over the fake service's [NetworkResult]. No
 * Store5 wrapping — `business_logic.kind: composite` for loan-apply has no `AppStoreRegistry`
 * entry yet (SP-03 `kmp-store-gen` has not run for this feature), so this repository surfaces
 * `NetworkResult` directly per this generation's explicit brief, same branch as
 * `MemberAddRepositoryImpl`/`GroupCreateRepositoryImpl`.
 */
class LoanApplyRepositoryTest {

    private val groupMembersDto = GroupMembersResponseDto(
        clientMembers = listOf(GroupMemberDto(id = 7L, displayName = "Amara Okafor", imagePresent = true)),
    )

    private val loanProductDto = LoanProductDto(
        id = 3L,
        name = "Group Loan - Standard",
        shortName = "GLS",
        principal = 500.0,
        minPrincipal = 100.0,
        maxPrincipal = 2000.0,
        numberOfRepayments = 12,
        interestRatePerPeriod = 1.5,
    )

    private val loanTemplateDto = LoanApplyTemplateDto(
        principal = 500.0,
        numberOfRepayments = 12,
        interestRatePerPeriod = 1.5,
        interestType = FineractStatusDto(id = 0, value = "Declining Balance"),
        amortizationType = FineractStatusDto(id = 1, value = "Equal installments"),
        repaymentEvery = 1,
    )

    private val memberSavingsDto = MemberSavingsResponseDto(
        savingsAccounts = listOf(
            MemberSavingsAccountRowDto(id = 11L, accountBalance = 250.0, status = SavingsAccountStatusDto("Active")),
            MemberSavingsAccountRowDto(id = 12L, accountBalance = 100.0, status = SavingsAccountStatusDto("Active")),
        ),
    )

    private val groupCorpusDto = GroupCorpusRowDto(corpusBalance = 1500.0, lastUpdated = "2026-07-01")

    private val groupLoanConfigDto = GroupLoanConfigDto(
        loanMultiplier = 3.0,
        maxLoanAmount = 2000.0,
        meetingFrequency = "weekly",
    )

    private val loanProduct = LoanProduct(
        id = 3L,
        name = "Group Loan - Standard",
        shortName = "GLS",
        principal = 500.0,
        minPrincipal = 100.0,
        maxPrincipal = 2000.0,
        numberOfRepayments = 12,
        interestRatePerPeriod = 1.5,
    )

    private val applyLoanRequest = ApplyLoanRequest(
        memberId = 7L,
        productId = 3L,
        amount = 500.0,
        durationWeeks = 12,
        purpose = LoanPurpose.BUSINESS,
        groupId = 42L,
    )

    private val applyLoanResponseDto = ApplyLoanResponseDto(officeId = 1L, clientId = 7L, loanId = 501L, resourceId = 501L)

    // ---------- getGroupMembers ----------

    @Test
    fun getGroupMembers_success_returnsMappedDomainList() = runTest {
        val api = FakeLoanApplyApi(groupMembersResult = NetworkResult.Success(groupMembersDto))
        val repo = LoanApplyRepositoryImpl(api)

        val result = repo.getGroupMembers(groupId = 42L)

        check(result is NetworkResult.Success)
        assertEquals(1, result.data.size)
        assertEquals("Amara Okafor", result.data[0].displayName)
        assertEquals(7L, result.data[0].fineractClientId)
    }

    @Test
    fun getGroupMembers_notFound404_propagatesError() = runTest {
        val api = FakeLoanApplyApi(groupMembersResult = NetworkResult.Error(NetworkError.NOT_FOUND))
        val repo = LoanApplyRepositoryImpl(api)

        val result = repo.getGroupMembers(groupId = 12345L)

        assertEquals(NetworkResult.Error(NetworkError.NOT_FOUND), result)
    }

    @Test
    fun getGroupMembers_emptyMembers_returnsSuccessWithEmptyList() = runTest {
        val api = FakeLoanApplyApi(groupMembersResult = NetworkResult.Success(GroupMembersResponseDto()))
        val repo = LoanApplyRepositoryImpl(api)

        val result = repo.getGroupMembers(groupId = 42L)

        check(result is NetworkResult.Success)
        assertTrue(result.data.isEmpty())
    }

    // ---------- loadTemplate (5-way parallel combine) ----------

    @Test
    fun loadTemplate_allFiveReadsSucceed_combinesIntoLoanApplyTemplateWithDerivedEligibility() = runTest {
        val api = FakeLoanApplyApi(
            loanProductsResult = NetworkResult.Success(listOf(loanProductDto)),
            loanTemplateResult = NetworkResult.Success(loanTemplateDto),
            memberSavingsResult = NetworkResult.Success(memberSavingsDto),
            groupCorpusResult = NetworkResult.Success(groupCorpusDto),
            groupLoanConfigResult = NetworkResult.Success(groupLoanConfigDto),
        )
        val repo = LoanApplyRepositoryImpl(api)

        val result = repo.loadTemplate(groupId = 42L, clientId = 7L, productId = 3L)

        check(result is NetworkResult.Success)
        assertEquals(1, result.data.products.size)
        assertEquals(500.0, result.data.principal)
        // memberSavingsBalance = sum(250.0 + 100.0) = 350.0; loanMultiplier = 3.0 -> 1050.0,
        // capped by maxLoanAmount = 2000.0 -> maxEligibleAmount = 1050.0.
        assertEquals(350.0, result.data.memberSavingsBalance)
        assertEquals(1050.0, result.data.maxEligibleAmount)
        assertTrue(
            setOf("getLoanProducts", "getLoanTemplate", "getMemberSavings", "getGroupCorpus", "getGroupLoanConfig")
                .containsAll(api.callOrder.toSet()) && api.callOrder.size == 5,
        )
    }

    @Test
    fun loadTemplate_loanProductsReadFails_propagatesErrorWithoutAssemblingTemplate() = runTest {
        val api = FakeLoanApplyApi(
            loanProductsResult = NetworkResult.Error(NetworkError.SERVER),
            loanTemplateResult = NetworkResult.Success(loanTemplateDto),
            memberSavingsResult = NetworkResult.Success(memberSavingsDto),
            groupCorpusResult = NetworkResult.Success(groupCorpusDto),
            groupLoanConfigResult = NetworkResult.Success(groupLoanConfigDto),
        )
        val repo = LoanApplyRepositoryImpl(api)

        val result = repo.loadTemplate(groupId = 42L, clientId = 7L, productId = 3L)

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }

    @Test
    fun loadTemplate_memberSavingsReadFails_propagatesError() = runTest {
        val api = FakeLoanApplyApi(
            loanProductsResult = NetworkResult.Success(listOf(loanProductDto)),
            loanTemplateResult = NetworkResult.Success(loanTemplateDto),
            memberSavingsResult = NetworkResult.Error(NetworkError.NOT_FOUND),
            groupCorpusResult = NetworkResult.Success(groupCorpusDto),
            groupLoanConfigResult = NetworkResult.Success(groupLoanConfigDto),
        )
        val repo = LoanApplyRepositoryImpl(api)

        val result = repo.loadTemplate(groupId = 42L, clientId = 7L, productId = 3L)

        assertEquals(NetworkResult.Error(NetworkError.NOT_FOUND), result)
    }

    @Test
    fun loadTemplate_groupCorpusReadFails_propagatesError() = runTest {
        val api = FakeLoanApplyApi(
            loanProductsResult = NetworkResult.Success(listOf(loanProductDto)),
            loanTemplateResult = NetworkResult.Success(loanTemplateDto),
            memberSavingsResult = NetworkResult.Success(memberSavingsDto),
            groupCorpusResult = NetworkResult.Error(NetworkError.SERVER),
            groupLoanConfigResult = NetworkResult.Success(groupLoanConfigDto),
        )
        val repo = LoanApplyRepositoryImpl(api)

        val result = repo.loadTemplate(groupId = 42L, clientId = 7L, productId = 3L)

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }

    @Test
    fun loadTemplate_groupLoanConfigReadFails_propagatesError() = runTest {
        val api = FakeLoanApplyApi(
            loanProductsResult = NetworkResult.Success(listOf(loanProductDto)),
            loanTemplateResult = NetworkResult.Success(loanTemplateDto),
            memberSavingsResult = NetworkResult.Success(memberSavingsDto),
            groupCorpusResult = NetworkResult.Success(groupCorpusDto),
            groupLoanConfigResult = NetworkResult.Error(NetworkError.UNAUTHORIZED),
        )
        val repo = LoanApplyRepositoryImpl(api)

        val result = repo.loadTemplate(groupId = 42L, clientId = 7L, productId = 3L)

        assertEquals(NetworkResult.Error(NetworkError.UNAUTHORIZED), result)
    }

    @Test
    fun loadTemplate_loanTemplateReadFails_propagatesError() = runTest {
        val api = FakeLoanApplyApi(
            loanProductsResult = NetworkResult.Success(listOf(loanProductDto)),
            loanTemplateResult = NetworkResult.Error(NetworkError.NOT_FOUND),
            memberSavingsResult = NetworkResult.Success(memberSavingsDto),
            groupCorpusResult = NetworkResult.Success(groupCorpusDto),
            groupLoanConfigResult = NetworkResult.Success(groupLoanConfigDto),
        )
        val repo = LoanApplyRepositoryImpl(api)

        val result = repo.loadTemplate(groupId = 42L, clientId = 7L, productId = 3L)

        assertEquals(NetworkResult.Error(NetworkError.NOT_FOUND), result)
    }

    // ---------- applyLoan ----------

    @Test
    fun applyLoan_success_postsMappedRequestAndReturnsMappedResult() = runTest {
        val api = FakeLoanApplyApi(applyLoanResult = NetworkResult.Success(applyLoanResponseDto))
        val repo = LoanApplyRepositoryImpl(api)

        val result = repo.applyLoan(applyLoanRequest, loanProduct)

        check(result is NetworkResult.Success)
        assertEquals(501L, result.data.loanId)
        assertEquals(7L, api.lastApplyLoanRequest?.clientId)
        assertEquals(3L, api.lastApplyLoanRequest?.productId)
        assertEquals(1.5, api.lastApplyLoanRequest?.interestRatePerPeriod)
        // purpose = LoanPurpose.BUSINESS -> fineractPurposeId = 3 (see LoanPurpose domain enum).
        assertEquals(3, api.lastApplyLoanRequest?.loanPurposeId)
    }

    @Test
    fun applyLoan_validationError400_propagatesError() = runTest {
        val api = FakeLoanApplyApi(applyLoanResult = NetworkResult.Error(NetworkError.BAD_REQUEST))
        val repo = LoanApplyRepositoryImpl(api)

        val result = repo.applyLoan(applyLoanRequest, loanProduct)

        assertEquals(NetworkResult.Error(NetworkError.BAD_REQUEST), result)
    }

    @Test
    fun applyLoan_serverError500_propagatesError() = runTest {
        val api = FakeLoanApplyApi(applyLoanResult = NetworkResult.Error(NetworkError.SERVER))
        val repo = LoanApplyRepositoryImpl(api)

        val result = repo.applyLoan(applyLoanRequest, loanProduct)

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }
}
