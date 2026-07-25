/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.savingsdashboard

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
import org.mifos.groupbanking.core.data.repository.SavingsRepository
import org.mifos.groupbanking.core.model.ContributionMode
import org.mifos.groupbanking.core.model.GroupSavingsSummary
import org.mifos.groupbanking.core.model.GroupTypeConfig
import org.mifos.groupbanking.core.model.GroupTypeSlug
import org.mifos.groupbanking.core.model.IndividualSavingsSummary
import org.mifos.groupbanking.core.model.MemberGroupSavingsRow
import org.mifos.groupbanking.core.model.MemberIndividualSavingsRow
import org.mifos.groupbanking.core.model.MemberSavingsBundle
import org.mifos.groupbanking.core.model.MemberSavingsDetail
import org.mifos.groupbanking.core.model.SavingsDashboardSummary
import org.mifos.groupbanking.core.model.SavingsDashboardTab
import org.mifos.groupbanking.core.model.SavingsLedgerEntry
import org.mifos.groupbanking.core.model.SavingsMechanism
import org.mifos.groupbanking.core.model.WeeklyContributionPoint
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * See API.md#viewmodel — `SavingsDashboardViewModelTest` exercises every declared
 * [SavingsDashboardAction] path plus the [NetworkResult] -> [SavingsDashboardState] mapping, per
 * RULE-TDD-METHODOLOGY-001 / RULE-IMPL-DEAD-CLICKABLE-001. Particular emphasis on the parallel
 * group+individual composite load (`SavingsRepository.loadSavingsDashboard`), contribution-model
 * derivation from the nav-param [GroupTypeConfig], the pure client-side `SelectTab` transform (no
 * re-fetch), and the connectivity-gated `RefreshDashboard` retry path.
 */
class SavingsDashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: FakeSavingsDashboardRepository
    private lateinit var networkMonitor: FakeSavingsDashboardNetworkMonitor
    private lateinit var sessionManager: SessionManager

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeSavingsDashboardRepository()
        networkMonitor = FakeSavingsDashboardNetworkMonitor(initiallyOnline = true)
        sessionManager = SessionManager(policy = SecurityPolicy())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(
        groupId: String = GROUP_ID,
        typeConfig: GroupTypeConfig = shareBasedTypeConfig(),
    ): SavingsDashboardViewModel = SavingsDashboardViewModel(
        repository = repository,
        networkMonitor = networkMonitor,
        sessionManager = sessionManager,
        crashReporter = ConsoleCrashReporter(),
        analytics = KptAnalyticsTracker(NoOpAnalyticsHelper()),
        groupId = groupId,
        typeConfig = typeConfig,
    )

    // -- initial state (nav-args seeded before any fetch resolves) ------------------------------

    @Test
    fun `initial state seeds nav args and derives SHARE_BASED_VARIABLE contribution model before fetch resolves`() =
        runTest(testDispatcher) {
            repository.dashboardResult = NetworkResult.Success(dashboardSummary())
            val viewModel = buildViewModel(typeConfig = shareBasedTypeConfig())

            val state = viewModel.stateFlow.value
            assertEquals(GROUP_ID, state.groupId)
            assertEquals("SHARE_BASED_VARIABLE", state.contributionModel)
            assertEquals(SavingsDashboardTab.GROUP, state.selectedTab)
            assertTrue(state.isLoading)
            assertEquals(0, repository.dashboardCallCount, "must not fetch until LoadDashboard is dispatched")
        }

    // -- LoadDashboard (on_mount, parallel group+individual composite fetch) --------------------

    @Test
    fun `LoadDashboard success seeds group and individual summaries, cycle progress, and weeklyTrend`() =
        runTest(testDispatcher) {
            repository.dashboardResult = NetworkResult.Success(
                dashboardSummary(
                    group = groupSummary(cycleTarget = 200L, cycleCollected = 120L),
                ),
            )
            val viewModel = buildViewModel()

            viewModel.trySendAction(SavingsDashboardAction.LoadDashboard)
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.stateFlow.value
            assertFalse(state.isLoading)
            assertNull(state.error)
            assertEquals(200L, state.cycleTarget)
            assertEquals(120L, state.cycleCollected)
            assertEquals(2, state.groupSavingsSummary?.memberRows?.size)
            assertEquals(1, state.individualSavingsSummary?.memberRows?.size)
            assertTrue(state.weeklyTrend.isNotEmpty())
            assertEquals("W3", state.weeklyTrend.last().weekLabel)
            assertEquals(1, repository.dashboardCallCount)
            assertEquals(SavingsDashboardScreenState.Content, state.deriveScreenState())
        }

    @Test
    fun `LoadDashboard success with FIXED typeConfig derives FIXED contribution model`() = runTest(testDispatcher) {
        repository.dashboardResult = NetworkResult.Success(dashboardSummary())
        val viewModel = buildViewModel(typeConfig = fixedTypeConfig())

        viewModel.trySendAction(SavingsDashboardAction.LoadDashboard)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("FIXED", viewModel.stateFlow.value.contributionModel)
    }

    @Test
    fun `LoadDashboard success with zero member rows on both tabs derives Empty screen state`() =
        runTest(testDispatcher) {
            repository.dashboardResult = NetworkResult.Success(
                dashboardSummary(
                    group = groupSummary(memberRows = emptyList()),
                    individual = individualSummary(memberRows = emptyList()),
                ),
            )
            val viewModel = buildViewModel()

            viewModel.trySendAction(SavingsDashboardAction.LoadDashboard)
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(SavingsDashboardScreenState.Empty, viewModel.stateFlow.value.deriveScreenState())
        }

    @Test
    fun `LoadDashboard NOT_FOUND with no cached summaries derives Error screen state and emits ShowError`() =
        runTest(testDispatcher) {
            repository.dashboardResult = NetworkResult.Error(NetworkError.NOT_FOUND)
            val viewModel = buildViewModel()

            viewModel.eventFlow.test {
                viewModel.trySendAction(SavingsDashboardAction.LoadDashboard)
                testDispatcher.scheduler.advanceUntilIdle()
                assertEquals(SavingsDashboardEvent.ShowError("error_group_not_found"), awaitItem())
            }

            val state = viewModel.stateFlow.value
            assertEquals("error_group_not_found", state.error)
            assertEquals(SavingsDashboardScreenState.Error, state.deriveScreenState())
        }

    @Test
    fun `LoadDashboard UNAUTHORIZED ends the session and emits ShowError`() = runTest(testDispatcher) {
        sessionManager.startSession()
        assertTrue(sessionManager.isSessionActive.value)
        repository.dashboardResult = NetworkResult.Error(NetworkError.UNAUTHORIZED)
        val viewModel = buildViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(SavingsDashboardAction.LoadDashboard)
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(SavingsDashboardEvent.ShowError("error_auth"), awaitItem())
        }

        assertFalse(sessionManager.isSessionActive.value, "401 must call sessionManager.endSession()")
    }

    // -- SelectTab (transform_state, client-side, no re-fetch) ----------------------------------

    @Test
    fun `SelectTab switches the active pane without a network call — both tabs already loaded on mount`() =
        runTest(testDispatcher) {
            repository.dashboardResult = NetworkResult.Success(dashboardSummary())
            val viewModel = buildViewModel()
            viewModel.trySendAction(SavingsDashboardAction.LoadDashboard)
            testDispatcher.scheduler.advanceUntilIdle()
            val callsAfterLoad = repository.dashboardCallCount

            viewModel.trySendAction(SavingsDashboardAction.SelectTab(SavingsDashboardTab.INDIVIDUAL))
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.stateFlow.value
            assertEquals(SavingsDashboardTab.INDIVIDUAL, state.selectedTab)
            assertEquals(callsAfterLoad, repository.dashboardCallCount, "tab switch is a pure state transform")
        }

    // -- OpenMemberDetail (navigate) -------------------------------------------------------------

    @Test
    fun `OpenMemberDetail emits NavigateToMemberDetail with memberId, groupId, and typeConfig`() =
        runTest(testDispatcher) {
            repository.dashboardResult = NetworkResult.Success(dashboardSummary())
            val typeConfig = shareBasedTypeConfig()
            val viewModel = buildViewModel(typeConfig = typeConfig)
            viewModel.trySendAction(SavingsDashboardAction.LoadDashboard)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.eventFlow.test {
                viewModel.trySendAction(SavingsDashboardAction.OpenMemberDetail("m-101"))
                assertEquals(
                    SavingsDashboardEvent.NavigateToMemberDetail(
                        memberId = "m-101",
                        groupId = GROUP_ID,
                        typeConfig = typeConfig,
                    ),
                    awaitItem(),
                )
            }
        }

    // -- RefreshDashboard (connectivity-gated bypass_and_refresh) --------------------------------

    @Test
    fun `RefreshDashboard while offline sets error_offline and emits ShowError without calling the repository`() =
        runTest(testDispatcher) {
            repository.dashboardResult = NetworkResult.Success(dashboardSummary())
            val viewModel = buildViewModel()
            viewModel.trySendAction(SavingsDashboardAction.LoadDashboard)
            testDispatcher.scheduler.advanceUntilIdle()
            val callsAfterLoad = repository.dashboardCallCount
            networkMonitor.setOnline(false)

            viewModel.eventFlow.test {
                viewModel.trySendAction(SavingsDashboardAction.RefreshDashboard)
                testDispatcher.scheduler.advanceUntilIdle()
                assertEquals(SavingsDashboardEvent.ShowError("error_offline"), awaitItem())
            }

            assertEquals("error_offline", viewModel.stateFlow.value.error)
            assertEquals(callsAfterLoad, repository.dashboardCallCount, "offline refresh must not call the repository")
        }

    @Test
    fun `RefreshDashboard while online re-fetches and clears a prior error, preserving cached summaries on retry`() =
        runTest(testDispatcher) {
            repository.dashboardResult = NetworkResult.Error(NetworkError.SERVER)
            val viewModel = buildViewModel()
            viewModel.trySendAction(SavingsDashboardAction.LoadDashboard)
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals("error_server", viewModel.stateFlow.value.error)

            repository.dashboardResult = NetworkResult.Success(
                dashboardSummary(group = groupSummary(cycleCollected = 999L)),
            )
            viewModel.trySendAction(SavingsDashboardAction.RefreshDashboard)
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.stateFlow.value
            assertNull(state.error)
            assertFalse(state.isRefreshing)
            assertEquals(999L, state.cycleCollected)
            assertEquals(2, repository.dashboardCallCount)
        }

    @Test
    fun `RefreshDashboard success stamps lastSyncAt`() = runTest(testDispatcher) {
        repository.dashboardResult = NetworkResult.Success(dashboardSummary())
        val viewModel = buildViewModel()
        viewModel.trySendAction(SavingsDashboardAction.LoadDashboard)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.stateFlow.value.lastSyncAt != null, "successful load must stamp lastSyncAt")
    }
}

// ---------------------------------------------------------------------------
// Fixtures
// ---------------------------------------------------------------------------

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

private fun weeklyTrend(): List<WeeklyContributionPoint> = listOf(
    WeeklyContributionPoint(weekLabel = "W48", groupAmount = 1000L, individualAmount = 300L),
    WeeklyContributionPoint(weekLabel = "W3", groupAmount = 1850L, individualAmount = 650L),
)

private fun groupSummary(
    cycleTarget: Long = 200L,
    cycleCollected: Long = 120L,
    totalCollected: Long = 120000L,
    weeklyTrend: List<WeeklyContributionPoint> = weeklyTrend(),
    memberRows: List<MemberGroupSavingsRow> = listOf(
        MemberGroupSavingsRow(
            memberId = "m-101",
            name = "Amina Hassan",
            totalContributed = 2500L,
            lastContribution = 2500L,
            meetingsContributed = 8,
            sharesHeld = 20,
            shareValue = 20000L,
        ),
        MemberGroupSavingsRow(
            memberId = "m-102",
            name = "Peter Otieno",
            totalContributed = 1875L,
            lastContribution = 1875L,
            meetingsContributed = 8,
            sharesHeld = 15,
            shareValue = 15000L,
        ),
    ),
): GroupSavingsSummary = GroupSavingsSummary(
    cycleTarget = cycleTarget,
    cycleCollected = cycleCollected,
    totalCollected = totalCollected,
    weeklyTrend = weeklyTrend,
    memberRows = memberRows,
)

private fun individualSummary(
    totalBalance: Long = 45000L,
    weeklyTrend: List<WeeklyContributionPoint> = weeklyTrend(),
    memberRows: List<MemberIndividualSavingsRow> = listOf(
        MemberIndividualSavingsRow(
            memberId = "m-101",
            name = "Amina Hassan",
            currentBalance = 8000L,
            lastTransaction = 500L,
            lastTransactionDate = LocalDate(2026, 7, 10),
        ),
    ),
): IndividualSavingsSummary = IndividualSavingsSummary(
    totalBalance = totalBalance,
    weeklyTrend = weeklyTrend,
    memberRows = memberRows,
)

private fun dashboardSummary(
    group: GroupSavingsSummary = groupSummary(),
    individual: IndividualSavingsSummary = individualSummary(),
): SavingsDashboardSummary = SavingsDashboardSummary(group = group, individual = individual)

/**
 * In-memory [SavingsRepository] fake, uniquely named to avoid collision with
 * `MemberSavingsDetailViewModelTest`'s `FakeMemberSavingsDetailRepository` /
 * `PersonalSavingsViewModelTest`'s `FakeSavingsRepository`. Only [loadSavingsDashboard] is
 * exercised by [SavingsDashboardViewModel]; every other method throws if invoked, so an
 * accidental call surfaces loudly instead of silently returning fixture data.
 */
private class FakeSavingsDashboardRepository : SavingsRepository {
    var dashboardResult: NetworkResult<SavingsDashboardSummary, NetworkError> = NetworkResult.Success(dashboardSummary())

    var dashboardCallCount: Int = 0
        private set

    override suspend fun getSavingsTransactions(
        savingsId: Long,
        limit: Int,
        offset: Int,
    ): NetworkResult<List<SavingsLedgerEntry>, NetworkError> {
        error("getSavingsTransactions is not used by SavingsDashboardViewModel")
    }

    override suspend fun loadMemberSavings(
        groupLinkedSavingsId: Long,
        individualSavingsId: Long?,
    ): NetworkResult<MemberSavingsBundle, NetworkError> {
        error("loadMemberSavings is not used by SavingsDashboardViewModel")
    }

    override suspend fun getMemberSavingsDetail(
        groupId: String,
        memberId: String,
        limit: Int,
        offset: Int,
    ): NetworkResult<MemberSavingsDetail, NetworkError> {
        error("getMemberSavingsDetail is not used by SavingsDashboardViewModel")
    }

    override suspend fun getGroupSavingsSummary(groupId: String): NetworkResult<GroupSavingsSummary, NetworkError> {
        error("getGroupSavingsSummary is not used by SavingsDashboardViewModel — loadSavingsDashboard covers both tabs")
    }

    override suspend fun getIndividualSavingsSummary(groupId: String): NetworkResult<IndividualSavingsSummary, NetworkError> {
        error(
            "getIndividualSavingsSummary is not used by SavingsDashboardViewModel — " +
                "loadSavingsDashboard covers both tabs",
        )
    }

    override suspend fun loadSavingsDashboard(groupId: String): NetworkResult<SavingsDashboardSummary, NetworkError> {
        dashboardCallCount++
        return dashboardResult
    }
}

private class FakeSavingsDashboardNetworkMonitor(initiallyOnline: Boolean) : NetworkMonitor {
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
