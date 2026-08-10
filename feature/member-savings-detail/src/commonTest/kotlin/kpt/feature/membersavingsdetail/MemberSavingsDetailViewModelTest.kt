/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.membersavingsdetail

import app.cash.turbine.test
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkChangeEvent
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkInfo
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitor
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
import kpt.core.model.ContributionMode
import kpt.core.model.GroupSavingsSummary
import kpt.core.model.GroupTypeConfig
import kpt.core.model.GroupTypeSlug
import kpt.core.model.IndividualSavingsSummary
import kpt.core.model.MemberSavingsBundle
import kpt.core.model.MemberSavingsDetail
import kpt.core.model.SavingsDashboardSummary
import kpt.core.model.SavingsDataPoint
import kpt.core.model.SavingsLedgerEntry
import kpt.core.model.SavingsMechanism
import kpt.core.model.SavingsMember
import kpt.core.model.SavingsStatementEntry
import kpt.core.model.SavingsTransactionFilter
import kpt.core.model.SavingsTransactionType
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * See API.md#viewmodel — MemberSavingsDetailViewModelTest exercises every declared
 * [MemberSavingsDetailAction] path plus the [NetworkResult] -> [MemberSavingsDetailState] mapping,
 * per RULE-TDD-METHODOLOGY-001 / RULE-IMPL-DEAD-CLICKABLE-001. Particular emphasis on
 * contribution-model derivation from the nav-param [GroupTypeConfig] (SHARE_BASED_VARIABLE surfaces
 * shares) and offset-based pagination (`OnLoadMore` appends, `OnRefresh`/`Retry` reset to page 0).
 */
class MemberSavingsDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: FakeMemberSavingsDetailRepository
    private lateinit var networkMonitor: FakeNetworkMonitor
    private lateinit var sessionManager: SessionManager

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeMemberSavingsDetailRepository()
        networkMonitor = FakeNetworkMonitor(initiallyOnline = true)
        sessionManager = SessionManager(policy = SecurityPolicy())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(
        memberId: String = MEMBER_ID,
        groupId: String = GROUP_ID,
        typeConfig: GroupTypeConfig = shareBasedTypeConfig(),
    ): MemberSavingsDetailViewModel = MemberSavingsDetailViewModel(
        repository = repository,
        networkMonitor = networkMonitor,
        sessionManager = sessionManager,
        crashReporter = ConsoleCrashReporter(),
        analytics = KptAnalyticsTracker(NoOpAnalyticsHelper()),
        memberId = memberId,
        groupId = groupId,
        typeConfig = typeConfig,
    )

    // -- init / on_mount -----------------------------------------------------------------------

    @Test
    fun `initial state seeds nav args and derives SHARE_BASED_VARIABLE contribution model before fetch resolves`() =
        runTest(testDispatcher) {
            repository.detailResult = NetworkResult.Success(detail(sharesHeld = 20, shareValue = 1000L))
            val viewModel = buildViewModel(typeConfig = shareBasedTypeConfig())

            val state = viewModel.stateFlow.value
            assertEquals(MEMBER_ID, state.memberId)
            assertEquals(GROUP_ID, state.groupId)
            assertEquals("SHARE_BASED_VARIABLE", state.contributionModel)
            assertTrue(state.isLoading)
        }

    @Test
    fun `on_mount success with FIXED typeConfig derives FIXED contribution model, no shares`() = runTest(testDispatcher) {
        repository.detailResult = NetworkResult.Success(
            detail(sharesHeld = null, shareValue = null, savingsBalance = 4500.0),
        )
        val viewModel = buildViewModel(typeConfig = fixedTypeConfig())
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals("FIXED", state.contributionModel)
        assertNull(state.sharesHeld)
        assertNull(state.shareValue)
        assertEquals(4500.0, state.savingsBalance)
    }

    @Test
    fun `on_mount success with SHARE_BASED_VARIABLE typeConfig surfaces sharesHeld and shareValue`() =
        runTest(testDispatcher) {
            repository.detailResult = NetworkResult.Success(detail(sharesHeld = 20, shareValue = 1000L))
            val viewModel = buildViewModel(typeConfig = shareBasedTypeConfig())
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.stateFlow.value
            assertFalse(state.isLoading)
            assertNull(state.error)
            assertEquals(20, state.sharesHeld)
            assertEquals(1000L, state.shareValue)
            assertEquals(MemberSavingsDetailScreenState.Content, state.deriveScreenState())
        }

    @Test
    fun `on_mount success with zero transactions derives Empty screen state`() = runTest(testDispatcher) {
        repository.detailResult = NetworkResult.Success(detail(transactions = emptyList(), hasNextPage = false))
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertTrue(state.filteredTransactions.isEmpty())
        assertEquals(MemberSavingsDetailScreenState.Empty, state.deriveScreenState())
    }

    @Test
    fun `on_mount NOT_FOUND maps to MemberSavingsError NotFound with retry false`() = runTest(testDispatcher) {
        repository.detailResult = NetworkResult.Error(NetworkError.NOT_FOUND)
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(MemberSavingsError.NotFound, state.error)
        assertFalse(state.error?.retry == true)
        assertEquals(MemberSavingsDetailScreenState.Error, state.deriveScreenState())
    }

    @Test
    fun `on_mount UNAUTHORIZED maps to MemberSavingsError Auth, ends session, and emits ShowSnackbar`() =
        runTest(testDispatcher) {
            sessionManager.startSession()
            assertTrue(sessionManager.isSessionActive.value)
            repository.detailResult = NetworkResult.Error(NetworkError.UNAUTHORIZED)
            val viewModel = buildViewModel()

            viewModel.eventFlow.test {
                testDispatcher.scheduler.advanceUntilIdle()
                val event = awaitItem()
                assertEquals(MemberSavingsDetailEvent.ShowSnackbar("error_auth"), event)
            }

            val state = viewModel.stateFlow.value
            assertEquals(MemberSavingsError.Auth, state.error)
            assertFalse(sessionManager.isSessionActive.value, "Auth error must call sessionManager.endSession()")
        }

    // -- OnFilterSelected (transform_state, client-side, no network) -----------------------------

    @Test
    fun `OnFilterSelected DEPOSITS restricts filteredTransactions without a network call`() = runTest(testDispatcher) {
        repository.detailResult = NetworkResult.Success(
            detail(
                transactions = listOf(
                    entry(id = "1", type = SavingsTransactionType.DEPOSIT),
                    entry(id = "2", type = SavingsTransactionType.WITHDRAWAL),
                ),
            ),
        )
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        val callsAfterMount = repository.detailCallCount

        viewModel.trySendAction(MemberSavingsDetailAction.OnFilterSelected(SavingsTransactionFilter.DEPOSITS))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(SavingsTransactionFilter.DEPOSITS, state.selectedFilter)
        assertEquals(1, state.filteredTransactions.size)
        assertEquals("1", state.filteredTransactions.first().id)
        assertEquals(2, state.transactions.size, "unfiltered accumulated page cache is untouched")
        assertEquals(callsAfterMount, repository.detailCallCount, "filter is a pure state transform, no re-fetch")
    }

    // -- OnTransactionSelected (transform_state, expand/collapse toggle) -------------------------

    @Test
    fun `OnTransactionSelected toggles expandedTransactionId, second tap collapses`() = runTest(testDispatcher) {
        repository.detailResult = NetworkResult.Success(detail(transactions = listOf(entry(id = "tx-1"))))
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(MemberSavingsDetailAction.OnTransactionSelected("tx-1"))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("tx-1", viewModel.stateFlow.value.expandedTransactionId)

        viewModel.trySendAction(MemberSavingsDetailAction.OnTransactionSelected("tx-1"))
        testDispatcher.scheduler.advanceUntilIdle()
        assertNull(viewModel.stateFlow.value.expandedTransactionId)
    }

    // -- OnLoadMore (offset += PAGE_SIZE, appends) ------------------------------------------------

    @Test
    fun `OnLoadMore appends the next page and advances currentOffset by 20`() = runTest(testDispatcher) {
        repository.detailResult = NetworkResult.Success(
            detail(transactions = listOf(entry(id = "1")), hasNextPage = true),
        )
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        repository.detailResult = NetworkResult.Success(
            detail(transactions = listOf(entry(id = "2")), hasNextPage = false),
        )
        viewModel.trySendAction(MemberSavingsDetailAction.OnLoadMore)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(2, state.transactions.size)
        assertEquals(listOf("1", "2"), state.transactions.map { it.id })
        assertEquals(20, state.currentOffset)
        assertFalse(state.hasNextPage)
        assertEquals(20, repository.lastOffset)
        assertFalse(state.isLoadingNextPage)
    }

    @Test
    fun `OnLoadMore is ignored when hasNextPage is false`() = runTest(testDispatcher) {
        repository.detailResult = NetworkResult.Success(detail(transactions = listOf(entry(id = "1")), hasNextPage = false))
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        val callsAfterMount = repository.detailCallCount

        viewModel.trySendAction(MemberSavingsDetailAction.OnLoadMore)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(callsAfterMount, repository.detailCallCount, "no re-fetch once hasNextPage is false")
    }

    // -- OnRefresh (resets to offset 0, replaces page cache) ---------------------------------------

    @Test
    fun `OnRefresh resets currentOffset to 0 and replaces the transaction cache`() = runTest(testDispatcher) {
        repository.detailResult = NetworkResult.Success(detail(transactions = listOf(entry(id = "1")), hasNextPage = true))
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.trySendAction(MemberSavingsDetailAction.OnLoadMore)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(20, viewModel.stateFlow.value.currentOffset)

        repository.detailResult = NetworkResult.Success(detail(transactions = listOf(entry(id = "fresh")), hasNextPage = false))
        viewModel.trySendAction(MemberSavingsDetailAction.OnRefresh)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(0, state.currentOffset)
        assertEquals(listOf("fresh"), state.transactions.map { it.id })
        assertFalse(state.isRefreshing)
        assertEquals(0, repository.lastOffset)
    }

    // -- Retry (call_api, library_refs: [cmp-network-monitor]) -------------------------------------

    @Test
    fun `Retry while offline sets Network error and emits ShowSnackbar without calling the repository`() =
        runTest(testDispatcher) {
            repository.detailResult = NetworkResult.Error(NetworkError.SERVER)
            val viewModel = buildViewModel()
            testDispatcher.scheduler.advanceUntilIdle()
            val callsAfterMount = repository.detailCallCount
            networkMonitor.setOnline(false)

            viewModel.eventFlow.test {
                viewModel.trySendAction(MemberSavingsDetailAction.Retry)
                testDispatcher.scheduler.advanceUntilIdle()
                assertEquals(MemberSavingsDetailEvent.ShowSnackbar("error_network"), awaitItem())
            }

            assertEquals(MemberSavingsError.Network, viewModel.stateFlow.value.error)
            assertEquals(callsAfterMount, repository.detailCallCount, "offline retry must not call the repository")
        }

    @Test
    fun `Retry while online clears the error and re-dispatches a fetch that recovers`() = runTest(testDispatcher) {
        repository.detailResult = NetworkResult.Error(NetworkError.SERVER)
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(MemberSavingsError.Server, viewModel.stateFlow.value.error)

        repository.detailResult = NetworkResult.Success(detail(transactions = listOf(entry(id = "1"))))
        viewModel.trySendAction(MemberSavingsDetailAction.Retry)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertNull(state.error)
        assertFalse(state.isLoading)
        assertEquals(1, state.transactions.size)
    }

    // -- OnBack (navigate) ----------------------------------------------------------------------------

    @Test
    fun `OnBack emits NavigateBack`() = runTest(testDispatcher) {
        repository.detailResult = NetworkResult.Success(detail())
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(MemberSavingsDetailAction.OnBack)
            assertEquals(MemberSavingsDetailEvent.NavigateBack, awaitItem())
        }
    }
}

// ---------------------------------------------------------------------------
// Fixtures
// ---------------------------------------------------------------------------

private const val MEMBER_ID = "member-42"
private const val GROUP_ID = "grp-001"

private fun shareBasedTypeConfig(): GroupTypeConfig = groupTypeConfig(ContributionMode.SHARE_BASED_VARIABLE)

private fun fixedTypeConfig(): GroupTypeConfig = groupTypeConfig(ContributionMode.FIXED)

private fun groupTypeConfig(contributionMode: ContributionMode): GroupTypeConfig = GroupTypeConfig(
    typeSlug = GroupTypeSlug.VSLA,
    displayName = "VSLA",
    tagline = "Village Savings and Loan Association",
    savingsMechanism = SavingsMechanism.ACCUMULATING,
    contributionMode = contributionMode,
    lendingEnabled = true,
    hasSocialFund = false,
    hasBankLinkage = false,
    welfareOnlyMode = false,
    formallyRegistered = false,
    defaultLoanMultiplier = 3.0,
    defaultInterestRatePct = 2.0,
    defaultCycleLengthMonths = 12,
    maxMembers = 30,
    minMembers = 5,
)

private fun entry(
    id: String,
    type: SavingsTransactionType = SavingsTransactionType.DEPOSIT,
    date: LocalDate = LocalDate(2026, 5, 5),
    amount: Double = 500.0,
    runningBalance: Double = 3500.0,
    reversed: Boolean = false,
): SavingsStatementEntry = SavingsStatementEntry(
    id = id,
    date = date,
    type = type,
    amount = amount,
    runningBalance = runningBalance,
    reversed = reversed,
)

private fun detail(
    member: SavingsMember = SavingsMember(memberId = MEMBER_ID, displayName = "Amina Wanjiru", photoUri = null),
    savingsAccountNo: String = "SA-00012345",
    savingsBalance: Double = 20000.0,
    sharesHeld: Int? = 20,
    shareValue: Long? = 1000L,
    sparklineData: List<SavingsDataPoint> = listOf(SavingsDataPoint(date = "2026-05-01", balance = 3500.0)),
    transactions: List<SavingsStatementEntry> = listOf(entry(id = "1")),
    totalTransactions: Int = transactions.size,
    hasNextPage: Boolean = false,
): MemberSavingsDetail = MemberSavingsDetail(
    member = member,
    savingsAccountNo = savingsAccountNo,
    savingsBalance = savingsBalance,
    sharesHeld = sharesHeld,
    shareValue = shareValue,
    sparklineData = sparklineData,
    transactions = transactions,
    totalTransactions = totalTransactions,
    hasNextPage = hasNextPage,
)

/**
 * In-memory [SavingsRepository] fake, uniquely named to avoid collision with
 * `PersonalSavingsViewModelTest`'s own `FakeSavingsRepository` (distinct file/package, but kept
 * distinct by convention per the generation brief). Only [getMemberSavingsDetail] is exercised by
 * [MemberSavingsDetailViewModel]; every other method throws if invoked, so an accidental call
 * surfaces loudly instead of silently returning fixture data.
 */
private class FakeMemberSavingsDetailRepository : SavingsRepository {
    var detailResult: NetworkResult<MemberSavingsDetail, NetworkError> = NetworkResult.Success(detail())

    var detailCallCount: Int = 0
        private set
    var lastOffset: Int = -1
        private set

    override suspend fun getSavingsTransactions(
        savingsId: Long,
        limit: Int,
        offset: Int,
    ): NetworkResult<List<SavingsLedgerEntry>, NetworkError> {
        error("getSavingsTransactions is not used by MemberSavingsDetailViewModel")
    }

    override suspend fun loadMemberSavings(
        groupLinkedSavingsId: Long,
        individualSavingsId: Long?,
    ): NetworkResult<MemberSavingsBundle, NetworkError> {
        error("loadMemberSavings is not used by MemberSavingsDetailViewModel")
    }

    override suspend fun getMemberSavingsDetail(
        groupId: String,
        memberId: String,
        limit: Int,
        offset: Int,
    ): NetworkResult<MemberSavingsDetail, NetworkError> {
        detailCallCount++
        lastOffset = offset
        return detailResult
    }

    override suspend fun getGroupSavingsSummary(groupId: String): NetworkResult<GroupSavingsSummary, NetworkError> {
        error("getGroupSavingsSummary is not used by MemberSavingsDetailViewModel")
    }

    override suspend fun getIndividualSavingsSummary(groupId: String): NetworkResult<IndividualSavingsSummary, NetworkError> {
        error("getIndividualSavingsSummary is not used by MemberSavingsDetailViewModel")
    }

    override suspend fun loadSavingsDashboard(groupId: String): NetworkResult<SavingsDashboardSummary, NetworkError> {
        error("loadSavingsDashboard is not used by MemberSavingsDetailViewModel")
    }
}

private class FakeNetworkMonitor(initiallyOnline: Boolean) : NetworkMonitor {
    private val _isOnline = MutableStateFlow(initiallyOnline)
    override val isOnline: StateFlow<Boolean> = _isOnline
    override val networkStatus: StateFlow<NetworkStatus> = MutableStateFlow(
        if (initiallyOnline) NetworkStatus.Available(NetworkInfo()) else NetworkStatus.Unavailable,
    )
    override val networkChanges: SharedFlow<NetworkChangeEvent> = MutableSharedFlow<NetworkChangeEvent>().asSharedFlow()

    fun setOnline(online: Boolean) {
        _isOnline.value = online
    }

    override fun close() = Unit
}
