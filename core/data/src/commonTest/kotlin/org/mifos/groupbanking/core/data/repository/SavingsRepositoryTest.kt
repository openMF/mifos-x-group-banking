/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.data.repository

import kotlinx.coroutines.test.runTest
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.network.model.GroupSavingsSummaryDto
import org.mifos.groupbanking.core.network.model.IndividualSavingsSummaryDto
import org.mifos.groupbanking.core.network.model.MemberGroupSavingsRowDto
import org.mifos.groupbanking.core.network.model.MemberIndividualSavingsRowDto
import org.mifos.groupbanking.core.network.model.MemberSavingsDetailDto
import org.mifos.groupbanking.core.network.model.SavingsDataPointDto
import org.mifos.groupbanking.core.network.model.SavingsLedgerCurrencyDto
import org.mifos.groupbanking.core.network.model.SavingsLedgerEntryDto
import org.mifos.groupbanking.core.network.model.SavingsLedgerTransactionTypeDto
import org.mifos.groupbanking.core.network.model.SavingsMemberDto
import org.mifos.groupbanking.core.network.model.SavingsStatementEntryDto
import org.mifos.groupbanking.core.network.model.SavingsStatementTypeDto
import org.mifos.groupbanking.core.network.model.WeeklyContributionPointDto
import org.mifos.groupbanking.core.network.service.savings.SavingsApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Uniquely-named fake, file-private — avoids the K2 same-package private-declaration collision
 * `RepositoryTestFakes.kt` documents (same convention as `FakeChangePinApi`).
 * [getSavingsTransactions] is stubbed per-[savingsId] since [SavingsRepository.loadMemberSavings]
 * calls the SAME [SavingsApi.getSavingsTransactions] method twice with two different ids
 * (group-linked + individual).
 */
private class FakeSavingsApi(
    private val savingsTransactionsResults: Map<Long, NetworkResult<List<SavingsLedgerEntryDto>, NetworkError>> = emptyMap(),
    private val memberSavingsDetailResult: NetworkResult<MemberSavingsDetailDto, NetworkError>? = null,
    private val groupSavingsSummaryResult: NetworkResult<GroupSavingsSummaryDto, NetworkError>? = null,
    private val individualSavingsSummaryResult: NetworkResult<IndividualSavingsSummaryDto, NetworkError>? = null,
) : SavingsApi {

    val requestedSavingsIds = mutableListOf<Long>()
    val callOrder = mutableListOf<String>()

    override suspend fun getSavingsTransactions(
        savingsId: Long,
        limit: Int,
        offset: Int,
    ): NetworkResult<List<SavingsLedgerEntryDto>, NetworkError> {
        requestedSavingsIds += savingsId
        callOrder += "getSavingsTransactions:$savingsId"
        return savingsTransactionsResults[savingsId] ?: error("no stub for savingsId=$savingsId")
    }

    override suspend fun getMemberSavingsDetail(
        groupId: String,
        memberId: String,
        limit: Int,
        offset: Int,
    ): NetworkResult<MemberSavingsDetailDto, NetworkError> {
        callOrder += "getMemberSavingsDetail"
        return memberSavingsDetailResult ?: error("memberSavingsDetailResult not stubbed")
    }

    override suspend fun getGroupSavingsSummary(groupId: String): NetworkResult<GroupSavingsSummaryDto, NetworkError> {
        callOrder += "getGroupSavingsSummary"
        return groupSavingsSummaryResult ?: error("groupSavingsSummaryResult not stubbed")
    }

    override suspend fun getIndividualSavingsSummary(groupId: String): NetworkResult<IndividualSavingsSummaryDto, NetworkError> {
        callOrder += "getIndividualSavingsSummary"
        return individualSavingsSummaryResult ?: error("individualSavingsSummaryResult not stubbed")
    }
}

/**
 * TDD RED-first coverage for [SavingsRepository] / [SavingsRepositoryImpl]. No try-catch anywhere
 * in the repository under test (Mandatory Rule 4) — [SavingsRepositoryImpl.loadMemberSavings] and
 * [SavingsRepositoryImpl.loadSavingsDashboard] fan reads out in parallel via
 * `kotlinx.coroutines.coroutineScope`/`async` and short-circuit on the first
 * [NetworkResult.Error] encountered (fixed declaration order); every other method is a plain
 * `when` over the fake service's [NetworkResult]. No Store5 wrapping — `savings` has no
 * `AppStoreRegistry` entry yet (SP-03 `kmp-store-gen` has not run for this feature set), so this
 * repository surfaces `NetworkResult` directly per this generation's explicit brief, same branch
 * as `LoanApplyRepositoryImpl`.
 */
class SavingsRepositoryTest {

    private val groupLinkedEntryDto = SavingsLedgerEntryDto(
        id = 9001L,
        transactionType = SavingsLedgerTransactionTypeDto(1, "savingsAccountTransactionType.deposit", "Deposit"),
        date = listOf(2026, 7, 15),
        amount = 500.0,
        runningBalance = 1500.0,
        currency = SavingsLedgerCurrencyDto("KES", "KSh"),
    )

    private val individualEntryDto = SavingsLedgerEntryDto(
        id = 9002L,
        transactionType = SavingsLedgerTransactionTypeDto(1, "savingsAccountTransactionType.deposit", "Deposit"),
        date = listOf(2026, 7, 10),
        amount = 200.0,
        runningBalance = 800.0,
        currency = SavingsLedgerCurrencyDto("KES", "KSh"),
    )

    private val memberSavingsDetailDto = MemberSavingsDetailDto(
        member = SavingsMemberDto(memberId = "m-7", displayName = "Amara Okafor", photoUri = null),
        savingsAccountNo = "SA-0007",
        savingsBalance = 1500.0,
        sharesHeld = 10,
        shareValue = 1000L,
        sparklineData = listOf(SavingsDataPointDto(date = "2026-07-01", balance = 1000.0)),
        transactions = listOf(
            SavingsStatementEntryDto(
                id = "t-1",
                date = "2026-07-15",
                type = SavingsStatementTypeDto.DEPOSIT,
                amount = 500.0,
                runningBalance = 1500.0,
                reversed = false,
            ),
        ),
        totalTransactions = 1,
        hasNextPage = false,
    )

    private val groupSavingsSummaryDto = GroupSavingsSummaryDto(
        cycleTarget = 50000L,
        cycleCollected = 32000L,
        totalCollected = 320000L,
        weeklyTrend = listOf(WeeklyContributionPointDto("W1", 8000L, 2000L)),
        memberRows = listOf(
            MemberGroupSavingsRowDto(
                memberId = "m-7",
                name = "Amara Okafor",
                totalContributed = 5000L,
                lastContribution = 500L,
                meetingsContributed = 10,
                sharesHeld = null,
                shareValue = null,
            ),
        ),
    )

    private val individualSavingsSummaryDto = IndividualSavingsSummaryDto(
        totalBalance = 15000L,
        weeklyTrend = listOf(WeeklyContributionPointDto("W1", 8000L, 2000L)),
        memberRows = listOf(
            MemberIndividualSavingsRowDto(
                memberId = "m-7",
                name = "Amara Okafor",
                currentBalance = 1500L,
                lastTransaction = 500L,
                lastTransactionDate = "2026-07-15",
            ),
        ),
    )

    // ---------- getSavingsTransactions ----------

    @Test
    fun getSavingsTransactions_success_returnsMappedDomainList() = runTest {
        val api = FakeSavingsApi(savingsTransactionsResults = mapOf(501L to NetworkResult.Success(listOf(groupLinkedEntryDto))))
        val repo = SavingsRepositoryImpl(api)

        val result = repo.getSavingsTransactions(savingsId = 501L)

        check(result is NetworkResult.Success)
        assertEquals(1, result.data.size)
        assertEquals(9001L, result.data[0].id)
        assertEquals(1500.0, result.data[0].runningBalance)
    }

    @Test
    fun getSavingsTransactions_emptyList_returnsSuccessWithEmptyList() = runTest {
        val api = FakeSavingsApi(savingsTransactionsResults = mapOf(501L to NetworkResult.Success(emptyList())))
        val repo = SavingsRepositoryImpl(api)

        val result = repo.getSavingsTransactions(savingsId = 501L)

        check(result is NetworkResult.Success)
        assertTrue(result.data.isEmpty())
    }

    @Test
    fun getSavingsTransactions_accountNotFound404_propagatesError() = runTest {
        val api = FakeSavingsApi(savingsTransactionsResults = mapOf(999L to NetworkResult.Error(NetworkError.NOT_FOUND)))
        val repo = SavingsRepositoryImpl(api)

        val result = repo.getSavingsTransactions(savingsId = 999L)

        assertEquals(NetworkResult.Error(NetworkError.NOT_FOUND), result)
    }

    // ---------- loadMemberSavings (personal-savings composite) ----------

    @Test
    fun loadMemberSavings_bothAccountsSucceed_returnsBundleWithBothTransactionLists() = runTest {
        val api = FakeSavingsApi(
            savingsTransactionsResults = mapOf(
                501L to NetworkResult.Success(listOf(groupLinkedEntryDto)),
                777L to NetworkResult.Success(listOf(individualEntryDto)),
            ),
        )
        val repo = SavingsRepositoryImpl(api)

        val result = repo.loadMemberSavings(groupLinkedSavingsId = 501L, individualSavingsId = 777L)

        check(result is NetworkResult.Success)
        assertEquals(1, result.data.groupLinkedTransactions.size)
        assertEquals(9001L, result.data.groupLinkedTransactions[0].id)
        assertEquals(1, result.data.individualTransactions?.size)
        assertEquals(9002L, result.data.individualTransactions?.get(0)?.id)
        assertTrue(api.requestedSavingsIds.containsAll(listOf(501L, 777L)))
    }

    @Test
    fun loadMemberSavings_individualSavingsIdNull_skipsIndividualReadAndReturnsNullIndividualTransactions() = runTest {
        val api = FakeSavingsApi(savingsTransactionsResults = mapOf(501L to NetworkResult.Success(listOf(groupLinkedEntryDto))))
        val repo = SavingsRepositoryImpl(api)

        val result = repo.loadMemberSavings(groupLinkedSavingsId = 501L, individualSavingsId = null)

        check(result is NetworkResult.Success)
        assertEquals(1, result.data.groupLinkedTransactions.size)
        assertNull(result.data.individualTransactions)
        assertEquals(listOf(501L), api.requestedSavingsIds)
    }

    @Test
    fun loadMemberSavings_groupLinkedReadFails_propagatesErrorEvenWhenIndividualSucceeds() = runTest {
        val api = FakeSavingsApi(
            savingsTransactionsResults = mapOf(
                501L to NetworkResult.Error(NetworkError.SERVER),
                777L to NetworkResult.Success(listOf(individualEntryDto)),
            ),
        )
        val repo = SavingsRepositoryImpl(api)

        val result = repo.loadMemberSavings(groupLinkedSavingsId = 501L, individualSavingsId = 777L)

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
        // Both reads still fire concurrently (structured concurrency) even though group-linked's
        // error wins the declaration-order check.
        assertTrue(api.requestedSavingsIds.containsAll(listOf(501L, 777L)))
    }

    @Test
    fun loadMemberSavings_individualReadFails_propagatesErrorWhenGroupLinkedSucceeds() = runTest {
        val api = FakeSavingsApi(
            savingsTransactionsResults = mapOf(
                501L to NetworkResult.Success(listOf(groupLinkedEntryDto)),
                777L to NetworkResult.Error(NetworkError.NOT_FOUND),
            ),
        )
        val repo = SavingsRepositoryImpl(api)

        val result = repo.loadMemberSavings(groupLinkedSavingsId = 501L, individualSavingsId = 777L)

        assertEquals(NetworkResult.Error(NetworkError.NOT_FOUND), result)
    }

    // ---------- getMemberSavingsDetail ----------

    @Test
    fun getMemberSavingsDetail_success_returnsMappedDomain() = runTest {
        val api = FakeSavingsApi(memberSavingsDetailResult = NetworkResult.Success(memberSavingsDetailDto))
        val repo = SavingsRepositoryImpl(api)

        val result = repo.getMemberSavingsDetail(groupId = "g-1", memberId = "m-7")

        check(result is NetworkResult.Success)
        assertEquals("SA-0007", result.data.savingsAccountNo)
        assertEquals("Amara Okafor", result.data.member.displayName)
        assertEquals(1, result.data.transactions.size)
    }

    @Test
    fun getMemberSavingsDetail_memberNotFound404_propagatesError() = runTest {
        val api = FakeSavingsApi(memberSavingsDetailResult = NetworkResult.Error(NetworkError.NOT_FOUND))
        val repo = SavingsRepositoryImpl(api)

        val result = repo.getMemberSavingsDetail(groupId = "g-1", memberId = "m-nope")

        assertEquals(NetworkResult.Error(NetworkError.NOT_FOUND), result)
    }

    @Test
    fun getMemberSavingsDetail_forbidden_propagatesUnknownError() = runTest {
        val api = FakeSavingsApi(memberSavingsDetailResult = NetworkResult.Error(NetworkError.UNKNOWN))
        val repo = SavingsRepositoryImpl(api)

        val result = repo.getMemberSavingsDetail(groupId = "g-1", memberId = "m-7")

        assertEquals(NetworkResult.Error(NetworkError.UNKNOWN), result)
    }

    // ---------- getGroupSavingsSummary ----------

    @Test
    fun getGroupSavingsSummary_success_returnsMappedDomain() = runTest {
        val api = FakeSavingsApi(groupSavingsSummaryResult = NetworkResult.Success(groupSavingsSummaryDto))
        val repo = SavingsRepositoryImpl(api)

        val result = repo.getGroupSavingsSummary(groupId = "g-1")

        check(result is NetworkResult.Success)
        assertEquals(50000L, result.data.cycleTarget)
        assertEquals(1, result.data.memberRows.size)
    }

    @Test
    fun getGroupSavingsSummary_groupNotFound404_propagatesError() = runTest {
        val api = FakeSavingsApi(groupSavingsSummaryResult = NetworkResult.Error(NetworkError.NOT_FOUND))
        val repo = SavingsRepositoryImpl(api)

        val result = repo.getGroupSavingsSummary(groupId = "g-missing")

        assertEquals(NetworkResult.Error(NetworkError.NOT_FOUND), result)
    }

    @Test
    fun getGroupSavingsSummary_emptyMemberRows_returnsSuccessWithEmptyList() = runTest {
        val api = FakeSavingsApi(
            groupSavingsSummaryResult = NetworkResult.Success(
                groupSavingsSummaryDto.copy(memberRows = emptyList(), weeklyTrend = emptyList()),
            ),
        )
        val repo = SavingsRepositoryImpl(api)

        val result = repo.getGroupSavingsSummary(groupId = "g-1")

        check(result is NetworkResult.Success)
        assertTrue(result.data.memberRows.isEmpty())
    }

    // ---------- getIndividualSavingsSummary ----------

    @Test
    fun getIndividualSavingsSummary_success_returnsMappedDomain() = runTest {
        val api = FakeSavingsApi(individualSavingsSummaryResult = NetworkResult.Success(individualSavingsSummaryDto))
        val repo = SavingsRepositoryImpl(api)

        val result = repo.getIndividualSavingsSummary(groupId = "g-1")

        check(result is NetworkResult.Success)
        assertEquals(15000L, result.data.totalBalance)
        assertEquals(1, result.data.memberRows.size)
    }

    @Test
    fun getIndividualSavingsSummary_groupNotFound404_propagatesError() = runTest {
        val api = FakeSavingsApi(individualSavingsSummaryResult = NetworkResult.Error(NetworkError.NOT_FOUND))
        val repo = SavingsRepositoryImpl(api)

        val result = repo.getIndividualSavingsSummary(groupId = "g-missing")

        assertEquals(NetworkResult.Error(NetworkError.NOT_FOUND), result)
    }

    // ---------- loadSavingsDashboard (savings-dashboard composite, 2-way parallel) ----------

    @Test
    fun loadSavingsDashboard_bothReadsSucceed_returnsCombinedSummaryAndFiresBothInParallel() = runTest {
        val api = FakeSavingsApi(
            groupSavingsSummaryResult = NetworkResult.Success(groupSavingsSummaryDto),
            individualSavingsSummaryResult = NetworkResult.Success(individualSavingsSummaryDto),
        )
        val repo = SavingsRepositoryImpl(api)

        val result = repo.loadSavingsDashboard(groupId = "g-1")

        check(result is NetworkResult.Success)
        assertEquals(50000L, result.data.group.cycleTarget)
        assertEquals(15000L, result.data.individual.totalBalance)
        assertTrue(
            setOf("getGroupSavingsSummary", "getIndividualSavingsSummary").containsAll(api.callOrder.toSet()) &&
                api.callOrder.size == 2,
        )
    }

    @Test
    fun loadSavingsDashboard_groupReadFails_propagatesErrorWithoutAssemblingSummary() = runTest {
        val api = FakeSavingsApi(
            groupSavingsSummaryResult = NetworkResult.Error(NetworkError.SERVER),
            individualSavingsSummaryResult = NetworkResult.Success(individualSavingsSummaryDto),
        )
        val repo = SavingsRepositoryImpl(api)

        val result = repo.loadSavingsDashboard(groupId = "g-1")

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }

    @Test
    fun loadSavingsDashboard_individualReadFails_propagatesError() = runTest {
        val api = FakeSavingsApi(
            groupSavingsSummaryResult = NetworkResult.Success(groupSavingsSummaryDto),
            individualSavingsSummaryResult = NetworkResult.Error(NetworkError.UNAUTHORIZED),
        )
        val repo = SavingsRepositoryImpl(api)

        val result = repo.loadSavingsDashboard(groupId = "g-1")

        assertEquals(NetworkResult.Error(NetworkError.UNAUTHORIZED), result)
    }
}
