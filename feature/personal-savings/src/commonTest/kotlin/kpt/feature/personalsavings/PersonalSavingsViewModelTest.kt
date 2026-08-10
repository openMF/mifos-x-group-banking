/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.personalsavings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kpt.core.analytics.KptAnalyticsTracker
import kpt.core.base.analytics.NoOpAnalyticsHelper
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.base.observability.ConsoleCrashReporter
import kpt.core.base.security.SecurityPolicy
import kpt.core.base.security.SessionManager
import kpt.core.data.repository.SavingsRepository
import kpt.core.model.GroupSavingsSummary
import kpt.core.model.IndividualSavingsSummary
import kpt.core.model.MemberSavingsBundle
import kpt.core.model.MemberSavingsDetail
import kpt.core.model.SavingsDashboardSummary
import kpt.core.model.SavingsLedgerEntry
import kpt.core.model.SavingsLedgerTransactionType
import kpt.core.model.SavingsTab
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * See API.md#viewmodel — PersonalSavingsViewModelTest exercises every declared
 * [PersonalSavingsAction] path plus the [NetworkResult] -> [PersonalSavingsState] mapping, per
 * RULE-TDD-METHODOLOGY-001 / RULE-IMPL-DEAD-CLICKABLE-001. Particular emphasis on the
 * balance-from-newest-runningBalance derivation ([currentBalanceEntry]) since `SavingsRepository`
 * exposes no dedicated account-summary endpoint (`Savings.kt` `MemberSavingsBundle` KDoc).
 */
class PersonalSavingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: FakeSavingsRepository
    private lateinit var sessionManager: SessionManager

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeSavingsRepository()
        sessionManager = SessionManager(policy = SecurityPolicy())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(
        clientId: Long = CLIENT_ID,
        groupLinkedSavingsId: Long = GROUP_LINKED_ID,
        individualSavingsId: Long? = INDIVIDUAL_ID,
    ): PersonalSavingsViewModel = PersonalSavingsViewModel(
        repository = repository,
        sessionManager = sessionManager,
        crashReporter = ConsoleCrashReporter(),
        analytics = KptAnalyticsTracker(NoOpAnalyticsHelper()),
        clientId = clientId,
        groupLinkedSavingsId = groupLinkedSavingsId,
        individualSavingsId = individualSavingsId,
    )

    // -- init / on_mount -----------------------------------------------------------------------

    @Test
    fun `initial state seeds nav args, GROUP_LINKED tab, and loading true before the fetch resolves`() =
        runTest(testDispatcher) {
            repository.bundleResult = NetworkResult.Success(
                MemberSavingsBundle(groupLinkedTransactions = listOf(entry(1, 100.0)), individualTransactions = null),
            )
            val viewModel = buildViewModel()

            val state = viewModel.stateFlow.value
            assertEquals(CLIENT_ID, state.clientId)
            assertEquals(GROUP_LINKED_ID, state.groupLinkedSavingsId)
            assertEquals(INDIVIDUAL_ID, state.individualSavingsId)
            assertEquals(SavingsTab.GROUP_LINKED, state.selectedTab)
            assertTrue(state.isLoading)
        }

    @Test
    fun `on_mount success derives balances from the NEWEST entry runningBalance, not list order`() =
        runTest(testDispatcher) {
            // Deliberately out-of-order (oldest first) with a distinct runningBalance per entry —
            // proves the reducer sorts by date rather than trusting firstOrNull().
            repository.bundleResult = NetworkResult.Success(
                MemberSavingsBundle(
                    groupLinkedTransactions = listOf(
                        entry(1, runningBalance = 500.0, date = LocalDate(2026, 1, 1)),
                        entry(2, runningBalance = 900.0, date = LocalDate(2026, 6, 1)),
                        entry(3, runningBalance = 700.0, date = LocalDate(2026, 3, 1)),
                    ),
                    individualTransactions = listOf(
                        entry(4, runningBalance = 50.0, date = LocalDate(2026, 2, 1)),
                        entry(5, runningBalance = 120.0, date = LocalDate(2026, 5, 1)),
                    ),
                ),
            )
            val viewModel = buildViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.stateFlow.value
            assertFalse(state.isLoading)
            assertNull(state.error)
            assertEquals(900.0, state.groupLinkedBalance, "must be the 2026-06-01 entry's runningBalance, not the first list element")
            assertEquals(120.0, state.individualBalance)
            assertEquals(3, state.groupLinkedTransactions.size)
            assertEquals(2, state.individualTransactions.size)
            assertEquals(PersonalSavingsScreenState.Content, state.deriveScreenState())
        }

    @Test
    fun `on_mount with null individualSavingsId leaves individual transactions empty and balance zero`() =
        runTest(testDispatcher) {
            repository.bundleResult = NetworkResult.Success(
                MemberSavingsBundle(groupLinkedTransactions = listOf(entry(1, 250.0)), individualTransactions = null),
            )
            val viewModel = buildViewModel(individualSavingsId = null)
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.stateFlow.value
            assertNull(state.individualSavingsId)
            assertTrue(state.individualTransactions.isEmpty())
            assertEquals(0.0, state.individualBalance)
            assertEquals(250.0, state.groupLinkedBalance)
        }

    @Test
    fun `on_mount success with zero group-linked transactions derives balance zero`() = runTest(testDispatcher) {
        repository.bundleResult = NetworkResult.Success(
            MemberSavingsBundle(groupLinkedTransactions = emptyList(), individualTransactions = null),
        )
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(0.0, state.groupLinkedBalance)
        assertTrue(state.groupLinkedTransactions.isEmpty())
        assertEquals(PersonalSavingsScreenState.Content, state.deriveScreenState())
    }

    @Test
    fun `on_mount SERVER error maps to SavingsError Server and derives Error screen state`() = runTest(testDispatcher) {
        repository.bundleResult = NetworkResult.Error(NetworkError.SERVER)
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertFalse(state.isLoading)
        assertEquals(SavingsError.Server, state.error)
        assertTrue(state.error?.retry == true)
        assertEquals(PersonalSavingsScreenState.Error, state.deriveScreenState())
    }

    @Test
    fun `on_mount UNAUTHORIZED maps to SavingsError Unauthorized and ends the session for real`() =
        runTest(testDispatcher) {
            sessionManager.startSession()
            assertTrue(sessionManager.isSessionActive.value)
            repository.bundleResult = NetworkResult.Error(NetworkError.UNAUTHORIZED)
            val viewModel = buildViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.stateFlow.value
            assertEquals(SavingsError.Unauthorized, state.error)
            assertFalse(state.error?.retry == true)
            assertFalse(sessionManager.isSessionActive.value, "Unauthorized must call sessionManager.endSession()")
        }

    // -- OnTabSelected (pure transform_state, no re-fetch) --------------------------------------

    @Test
    fun `OnTabSelected switches selectedTab without re-fetching (already eagerly loaded on mount)`() =
        runTest(testDispatcher) {
            repository.bundleResult = NetworkResult.Success(
                MemberSavingsBundle(
                    groupLinkedTransactions = listOf(entry(1, 300.0)),
                    individualTransactions = listOf(entry(2, 80.0)),
                ),
            )
            val viewModel = buildViewModel()
            testDispatcher.scheduler.advanceUntilIdle()
            val fetchesAfterMount = repository.bundleCallCount

            viewModel.trySendAction(PersonalSavingsAction.OnTabSelected(SavingsTab.INDIVIDUAL))
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.stateFlow.value
            assertEquals(SavingsTab.INDIVIDUAL, state.selectedTab)
            assertEquals(fetchesAfterMount, repository.bundleCallCount, "tab switch is a pure state transform")
            assertEquals(0, repository.transactionsCallCount)
        }

    // -- OnRefresh -------------------------------------------------------------------------------

    @Test
    fun `OnRefresh on GROUP_LINKED tab re-fetches only the group-linked account and updates its balance`() =
        runTest(testDispatcher) {
            repository.bundleResult = NetworkResult.Success(
                MemberSavingsBundle(groupLinkedTransactions = listOf(entry(1, 100.0)), individualTransactions = null),
            )
            val viewModel = buildViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            repository.transactionsResult = NetworkResult.Success(listOf(entry(1, 100.0), entry(9, 400.0, date = LocalDate(2026, 7, 1))))
            viewModel.trySendAction(PersonalSavingsAction.OnRefresh)
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.stateFlow.value
            assertFalse(state.isRefreshing)
            assertEquals(400.0, state.groupLinkedBalance)
            assertEquals(GROUP_LINKED_ID, repository.lastTransactionsSavingsId)
            assertEquals(1, repository.transactionsCallCount)
        }

    @Test
    fun `OnRefresh on INDIVIDUAL tab re-fetches only the individual account`() = runTest(testDispatcher) {
        repository.bundleResult = NetworkResult.Success(
            MemberSavingsBundle(
                groupLinkedTransactions = listOf(entry(1, 100.0)),
                individualTransactions = listOf(entry(2, 50.0)),
            ),
        )
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.trySendAction(PersonalSavingsAction.OnTabSelected(SavingsTab.INDIVIDUAL))
        testDispatcher.scheduler.advanceUntilIdle()

        repository.transactionsResult = NetworkResult.Success(listOf(entry(2, 220.0)))
        viewModel.trySendAction(PersonalSavingsAction.OnRefresh)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(220.0, state.individualBalance)
        assertEquals(INDIVIDUAL_ID, repository.lastTransactionsSavingsId)
    }

    // -- OnRetry -----------------------------------------------------------------------------------

    @Test
    fun `OnRetry clears the error and re-dispatches a fetch that recovers`() = runTest(testDispatcher) {
        repository.bundleResult = NetworkResult.Error(NetworkError.SERVER)
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(SavingsError.Server, viewModel.stateFlow.value.error)

        repository.transactionsResult = NetworkResult.Success(listOf(entry(1, 60.0)))
        viewModel.trySendAction(PersonalSavingsAction.OnRetry)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertNull(state.error)
        assertFalse(state.isLoading)
        assertEquals(60.0, state.groupLinkedBalance)
    }

    @Test
    fun `OnRetry NETWORK error keeps SavingsError Network with retry true`() = runTest(testDispatcher) {
        repository.bundleResult = NetworkResult.Error(NetworkError.SERVER)
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        repository.transactionsResult = NetworkResult.Error(NetworkError.REQUEST_TIMEOUT)
        viewModel.trySendAction(PersonalSavingsAction.OnRetry)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(SavingsError.Network, state.error)
        assertTrue(state.error?.retry == true)
    }
}

// ---------------------------------------------------------------------------
// Fixtures
// ---------------------------------------------------------------------------

private const val CLIENT_ID = 42L
private const val GROUP_LINKED_ID = 101L
private const val INDIVIDUAL_ID = 202L

private fun entry(
    id: Long,
    runningBalance: Double,
    date: LocalDate = LocalDate(2026, 1, 1),
): SavingsLedgerEntry = SavingsLedgerEntry(
    id = id,
    type = SavingsLedgerTransactionType(value = 1, code = "deposit", description = "Deposit"),
    date = date,
    amount = 50.0,
    runningBalance = runningBalance,
    currencyCode = "KES",
    currencyDisplaySymbol = "KES",
)

/**
 * In-memory [SavingsRepository] fake. Only [loadMemberSavings] (initial mount) and
 * [getSavingsTransactions] (tab-switch/refresh/retry single-account bypass) are exercised by
 * [PersonalSavingsViewModel]; the companion-read methods throw if invoked, so an accidental call
 * surfaces loudly instead of silently returning fixture data.
 */
private class FakeSavingsRepository : SavingsRepository {
    var bundleResult: NetworkResult<MemberSavingsBundle, NetworkError> =
        NetworkResult.Success(MemberSavingsBundle(groupLinkedTransactions = emptyList(), individualTransactions = null))
    var transactionsResult: NetworkResult<List<SavingsLedgerEntry>, NetworkError> = NetworkResult.Success(emptyList())

    var bundleCallCount: Int = 0
        private set
    var transactionsCallCount: Int = 0
        private set
    var lastTransactionsSavingsId: Long? = null
        private set

    override suspend fun getSavingsTransactions(
        savingsId: Long,
        limit: Int,
        offset: Int,
    ): NetworkResult<List<SavingsLedgerEntry>, NetworkError> {
        transactionsCallCount++
        lastTransactionsSavingsId = savingsId
        return transactionsResult
    }

    override suspend fun loadMemberSavings(
        groupLinkedSavingsId: Long,
        individualSavingsId: Long?,
    ): NetworkResult<MemberSavingsBundle, NetworkError> {
        bundleCallCount++
        return bundleResult
    }

    override suspend fun getMemberSavingsDetail(
        groupId: String,
        memberId: String,
        limit: Int,
        offset: Int,
    ): NetworkResult<MemberSavingsDetail, NetworkError> {
        error("getMemberSavingsDetail is not used by PersonalSavingsViewModel")
    }

    override suspend fun getGroupSavingsSummary(groupId: String): NetworkResult<GroupSavingsSummary, NetworkError> {
        error("getGroupSavingsSummary is not used by PersonalSavingsViewModel")
    }

    override suspend fun getIndividualSavingsSummary(groupId: String): NetworkResult<IndividualSavingsSummary, NetworkError> {
        error("getIndividualSavingsSummary is not used by PersonalSavingsViewModel")
    }

    override suspend fun loadSavingsDashboard(groupId: String): NetworkResult<SavingsDashboardSummary, NetworkError> {
        error("loadSavingsDashboard is not used by PersonalSavingsViewModel")
    }
}
