/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.groupdashboard

import app.cash.turbine.test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kpt.core.analytics.KptAnalyticsTracker
import kpt.core.base.analytics.NoOpAnalyticsHelper
import kpt.core.base.observability.ConsoleCrashReporter
import kpt.core.base.security.SecurityPolicy
import kpt.core.base.security.SessionManager
import kpt.core.base.store.screen.ExperimentalScreenDataStreamTestingApi
import kpt.core.base.store.screen.FetchPolicy
import kpt.core.base.store.screen.ScreenDataStream
import kpt.core.base.store.screen.ScreenState
import kpt.core.base.store.screen.screenDataStreamForTesting
import org.mifos.groupbanking.core.data.repository.GroupDashboardRepository
import org.mifos.groupbanking.core.model.ActivityItem
import org.mifos.groupbanking.core.model.ActivityType
import org.mifos.groupbanking.core.model.GroupAccounts
import org.mifos.groupbanking.core.model.GroupConfig
import org.mifos.groupbanking.core.model.GroupContributionModel
import org.mifos.groupbanking.core.model.GroupCorpus
import org.mifos.groupbanking.core.model.GroupDashboard
import org.mifos.groupbanking.core.model.GroupDetail
import org.mifos.groupbanking.core.model.GroupInstanceConfig
import org.mifos.groupbanking.core.model.GroupTypeSlug
import org.mifos.groupbanking.core.model.SavingsMechanism
import org.mifos.groupbanking.core.model.ViewerRole
import org.mifos.groupbanking.core.model.ViewerRoleInfo
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * See API.md#viewmodel — `GroupDashboardViewModelTest` exercises the [ScreenState] ->
 * [GroupDashboardState] mapping (both pool-model-adaptive branches, plus [viewerRole]
 * reconciliation from the resolved [ViewerRoleInfo]), the role-gated quick-action handlers
 * (management vs read-only), the [isCorpusInsufficient] pure formula, and every declared
 * navigation / refresh / retry action, per RULE-TDD-METHODOLOGY-001 / RULE-IMPL-DEAD-CLICKABLE-001.
 *
 * `config.minimumDisbursementThreshold` has **no wire source** anywhere in `api.yaml`
 * (documented gap on `GroupConfig`'s KDoc in `core/model/GroupDashboard.kt`) — so in the real
 * Content-mapping pipeline `isCorpusInsufficient` always evaluates `false` today. The formula
 * itself is still fully covered via the top-level [isCorpusInsufficient] pure function (tested
 * directly below with a non-null threshold), decoupled from that wire-gap.
 */
@OptIn(ExperimentalScreenDataStreamTestingApi::class)
class GroupDashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: FakeGroupDashboardRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: GroupDashboardViewModel

    private fun createViewModel(viewerRole: String = "ORGANIZER") {
        viewModel = GroupDashboardViewModel(
            repository = repository,
            sessionManager = sessionManager,
            crashReporter = ConsoleCrashReporter(),
            analytics = KptAnalyticsTracker(NoOpAnalyticsHelper()),
            groupId = GROUP_ID,
            viewerRole = viewerRole,
        )
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeGroupDashboardRepository()
        sessionManager = SessionManager(policy = SecurityPolicy())
        createViewModel()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state seeds viewerRole from the nav arg and starts loading`() = runTest(testDispatcher) {
        val state = viewModel.stateFlow.value
        assertTrue(state.isLoading)
        assertEquals("ORGANIZER", state.viewerRole)
        assertNull(state.error)
        assertNull(state.group)
    }

    @Test
    fun `init requests the stream for the nav-arg groupId`() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf(GROUP_ID), repository.requestedGroupIds)
    }

    @Test
    fun `Content with ACCUMULATING pool model maps corpus fields and clears rotation fields`() =
        runTest(testDispatcher) {
            repository.emit(ScreenState.Content(data = accumulatingDashboard()))
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.stateFlow.value
            assertFalse(state.isLoading)
            assertEquals("VSLA", state.groupTypeName)
            assertEquals(47500.0, state.corpus?.currentBalance)
            assertEquals(2750.0, state.shareOutProjection)
            assertNull(state.rotationPosition)
            assertNull(state.nextRecipientName)
            assertNull(state.nextRecipientPosition)
            assertEquals(1, state.recentActivity.size)
        }

    @Test
    fun `Content with ROTATING_PAYOUT pool model maps rotation fields and nulls shareOutProjection`() =
        runTest(testDispatcher) {
            repository.emit(ScreenState.Content(data = rotatingDashboard()))
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.stateFlow.value
            assertEquals("ROSCA", state.groupTypeName)
            assertNull(state.shareOutProjection)
            assertEquals(7, state.rotationPosition)
            assertEquals("Amina Hassan", state.nextRecipientName)
            assertEquals(4, state.nextRecipientPosition)
        }

    @Test
    fun `Content reconciles viewerRole from the resolved ViewerRoleInfo`() = runTest(testDispatcher) {
        // Nav-arg seeded "ORGANIZER" (see setUp), server resolves the real role as MEMBER.
        repository.emit(ScreenState.Content(data = accumulatingDashboard(role = ViewerRole.MEMBER)))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("MEMBER", viewModel.stateFlow.value.viewerRole)
    }

    @Test
    fun `isCorpusInsufficient formula returns true when balance is below the configured threshold`() {
        val corpus = testCorpus(currentBalance = 1000.0)
        val config = testConfig(minimumDisbursementThreshold = 5000.0)
        assertTrue(isCorpusInsufficient(corpus, config))
    }

    @Test
    fun `isCorpusInsufficient formula returns false when threshold is null (current wire-gap default)`() {
        val corpus = testCorpus(currentBalance = 1000.0)
        val config = testConfig(minimumDisbursementThreshold = null)
        assertFalse(isCorpusInsufficient(corpus, config))
    }

    @Test
    fun `Content always yields isCorpusInsufficient false in production today (documented wire gap)`() =
        runTest(testDispatcher) {
            repository.emit(ScreenState.Content(data = accumulatingDashboard()))
            testDispatcher.scheduler.advanceUntilIdle()

            assertFalse(viewModel.stateFlow.value.isCorpusInsufficient)
        }

    @Test
    fun `management role OnStartMeeting navigates to meeting-calendar without a corpus dialog`() =
        runTest(testDispatcher) {
            repository.emit(ScreenState.Content(data = accumulatingDashboard()))
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.eventFlow.test {
                viewModel.trySendAction(GroupDashboardAction.OnStartMeeting)
                assertEquals(GroupDashboardEvent.NavigateToMeetingCalendar(GROUP_ID), awaitItem())
            }
        }

    @Test
    fun `member role OnStartMeeting (view-meetings) navigates without any corpus check`() = runTest(testDispatcher) {
        createViewModel(viewerRole = "MEMBER")
        repository.emit(ScreenState.Content(data = accumulatingDashboard(role = ViewerRole.MEMBER)))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(GroupDashboardAction.OnStartMeeting)
            assertEquals(GroupDashboardEvent.NavigateToMeetingCalendar(GROUP_ID), awaitItem())
        }
    }

    @Test
    fun `OnViewMembers navigates to member-list regardless of role`() = runTest(testDispatcher) {
        viewModel.eventFlow.test {
            viewModel.trySendAction(GroupDashboardAction.OnViewMembers)
            assertEquals(GroupDashboardEvent.NavigateToMemberList(GROUP_ID), awaitItem())
        }
    }

    @Test
    fun `OnViewLoans navigates to loan-list regardless of role`() = runTest(testDispatcher) {
        viewModel.eventFlow.test {
            viewModel.trySendAction(GroupDashboardAction.OnViewLoans)
            assertEquals(GroupDashboardEvent.NavigateToLoanList(GROUP_ID), awaitItem())
        }
    }

    @Test
    fun `OnShareOut with isCycleEnd false emits ShowSnackbar instead of navigating`() = runTest(testDispatcher) {
        repository.emit(ScreenState.Content(data = accumulatingDashboard()))
        testDispatcher.scheduler.advanceUntilIdle()
        // isCycleEnd defaults false (no wire source today — same documented class of gap as
        // isCorpusInsufficient), so this is the currently-reachable branch.
        assertFalse(viewModel.stateFlow.value.isCycleEnd)

        viewModel.eventFlow.test {
            viewModel.trySendAction(GroupDashboardAction.OnShareOut)
            assertEquals(
                GroupDashboardEvent.ShowSnackbar(message = "share_out_not_available"),
                awaitItem(),
            )
        }
    }

    @Test
    fun `OnShareOut dispatched by an unauthorized role is defensively ignored`() = runTest(testDispatcher) {
        createViewModel(viewerRole = "MEMBER")
        repository.emit(ScreenState.Content(data = accumulatingDashboard(role = ViewerRole.MEMBER)))
        testDispatcher.scheduler.advanceUntilIdle()

        var eventEmitted = false
        val job = launch { viewModel.eventFlow.collect { eventEmitted = true } }
        viewModel.trySendAction(GroupDashboardAction.OnShareOut)
        testDispatcher.scheduler.advanceUntilIdle()
        job.cancel()

        assertFalse(eventEmitted)
    }

    @Test
    fun `OnViewSavings emits NavigateToSavingsDashboard`() = runTest(testDispatcher) {
        createViewModel(viewerRole = "MEMBER")
        viewModel.eventFlow.test {
            viewModel.trySendAction(GroupDashboardAction.OnViewSavings)
            assertEquals(GroupDashboardEvent.NavigateToSavingsDashboard(GROUP_ID), awaitItem())
        }
    }

    @Test
    fun `OnMoreOptions toggles isMoreMenuExpanded`() = runTest(testDispatcher) {
        assertFalse(viewModel.stateFlow.value.isMoreMenuExpanded)

        viewModel.trySendAction(GroupDashboardAction.OnMoreOptions)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.stateFlow.value.isMoreMenuExpanded)

        viewModel.trySendAction(GroupDashboardAction.OnMoreOptions)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.stateFlow.value.isMoreMenuExpanded)
    }

    @Test
    fun `OnGroupSettings closes the menu and emits NavigateToSettings`() = runTest(testDispatcher) {
        viewModel.trySendAction(GroupDashboardAction.OnMoreOptions)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(GroupDashboardAction.OnGroupSettings)
            assertEquals(GroupDashboardEvent.NavigateToSettings, awaitItem())
        }
        assertFalse(viewModel.stateFlow.value.isMoreMenuExpanded)
    }

    @Test
    fun `OnSyncStatus closes the menu and emits NavigateToSyncStatus`() = runTest(testDispatcher) {
        viewModel.trySendAction(GroupDashboardAction.OnMoreOptions)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(GroupDashboardAction.OnSyncStatus)
            assertEquals(GroupDashboardEvent.NavigateToSyncStatus, awaitItem())
        }
        assertFalse(viewModel.stateFlow.value.isMoreMenuExpanded)
    }

    @Test
    fun `OnRefresh sets isLoading and dispatches a bypass-and-refresh fetch`() = runTest(testDispatcher) {
        repository.emit(ScreenState.Content(data = accumulatingDashboard()))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(GroupDashboardAction.OnRefresh)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.stateFlow.value.isLoading)
        assertEquals(1, repository.refreshTriggerCount)
    }

    @Test
    fun `Retry clears the error and re-dispatches the stream fetch`() = runTest(testDispatcher) {
        repository.emit(ScreenState.NoNetwork())
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(GroupDashboardAction.Retry)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.stateFlow.value.error)
        assertEquals(1, repository.refreshTriggerCount)
    }

    @Test
    fun `OnBack emits NavigateBack`() = runTest(testDispatcher) {
        viewModel.eventFlow.test {
            viewModel.trySendAction(GroupDashboardAction.OnBack)
            assertEquals(GroupDashboardEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `stream NoNetwork maps to Network error`() = runTest(testDispatcher) {
        repository.emit(ScreenState.NoNetwork())
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(GroupDashboardError.Network, state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun `stream generic Error maps to Server error`() = runTest(testDispatcher) {
        repository.emit(ScreenState.Error(error = IllegalStateException("boom"), isNetworkError = false))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(GroupDashboardError.Server, viewModel.stateFlow.value.error)
    }

    @Test
    fun `stream Unauthenticated clears the session, sets Auth error, and shows a snackbar`() =
        runTest(testDispatcher) {
            sessionManager.startSession()
            assertTrue(sessionManager.isSessionActive.value)

            viewModel.eventFlow.test {
                repository.emit(ScreenState.Unauthenticated)
                testDispatcher.scheduler.advanceUntilIdle()

                assertEquals(
                    GroupDashboardEvent.ShowSnackbar(message = GroupDashboardError.Auth.messageKey),
                    awaitItem(),
                )
            }

            val state = viewModel.stateFlow.value
            assertEquals(GroupDashboardError.Auth, state.error)
            assertFalse(state.error?.retry ?: true)
            assertFalse(sessionManager.isSessionActive.value)
        }

    private companion object {
        const val GROUP_ID = "grp-100"
    }
}

private fun testCorpus(currentBalance: Double) = GroupCorpus(
    currentBalance = currentBalance,
    openingBalance = 0.0,
    totalContributionsThisCycle = 0.0,
    totalLoansOutstanding = 0.0,
    isCycleEnd = false,
    lastUpdated = "2026-07-20T00:00:00Z",
    rotationPosition = null,
    nextRecipientName = null,
    nextRecipientPosition = null,
)

private fun testConfig(minimumDisbursementThreshold: Double?) = GroupConfig(
    shareValue = 200.0,
    shareMin = null,
    shareMax = null,
    contributionAmount = null,
    loanMultiplier = 3.0,
    interestRate = 10.0,
    cycleLengthMonths = 12,
    fineAmount = 50.0,
    minimumDisbursementThreshold = minimumDisbursementThreshold,
)

private fun accumulatingDashboard(role: ViewerRole = ViewerRole.ORGANIZER): GroupDashboard = GroupDashboard(
    group = GroupDetail(
        id = "grp-100",
        fineractGroupId = 1L,
        name = "Mwangaza Women's Group",
        cycleNumber = 1,
        cycleLengthMonths = 12,
        meetingFrequency = "Weekly",
        memberCount = 20,
        overdueLoansCount = 0,
        status = "ACTIVE",
        typeConfig = GroupInstanceConfig(
            groupType = GroupTypeSlug.VSLA,
            poolModel = SavingsMechanism.ACCUMULATING,
            contributionModel = GroupContributionModel.SHARE_BASED_VARIABLE,
            shareoutFormula = "PRORATA_SHARES",
            payoutOrderMethod = "",
            shareValue = 200.0,
            contributionAmount = 0.0,
            socialFundEnabled = false,
            cycleLengthMonths = 12,
            loanMultiplier = 3.0,
            interestRate = 10.0,
            fineAmount = 50.0,
        ),
    ),
    viewerRole = ViewerRoleInfo(role = role, memberId = 501L),
    corpus = GroupCorpus(
        currentBalance = 47500.0,
        openingBalance = 0.0,
        totalContributionsThisCycle = 52500.0,
        totalLoansOutstanding = 5000.0,
        isCycleEnd = false,
        lastUpdated = "2026-07-20T00:00:00Z",
        rotationPosition = null,
        nextRecipientName = null,
        nextRecipientPosition = null,
    ),
    accounts = GroupAccounts(
        savingsBalance = 52500.0,
        loansOutstanding = 5000.0,
        activeLoanCount = 1,
        shareOutProjection = 2750.0,
        recentActivity = listOf(
            ActivityItem(
                id = "act-1",
                type = ActivityType.DEPOSIT,
                description = "Weekly contribution",
                amount = 1200.0,
                date = "2026-07-19",
                memberName = "Amina Hassan",
            ),
        ),
    ),
)

private fun rotatingDashboard(): GroupDashboard = GroupDashboard(
    group = GroupDetail(
        id = "grp-100",
        fineractGroupId = 1L,
        name = "Jiunge ROSCA Circle",
        cycleNumber = 3,
        cycleLengthMonths = 10,
        meetingFrequency = "Monthly",
        memberCount = 10,
        overdueLoansCount = 0,
        status = "ACTIVE",
        typeConfig = GroupInstanceConfig(
            groupType = GroupTypeSlug.ROSCA,
            poolModel = SavingsMechanism.ROTATING_PAYOUT,
            contributionModel = GroupContributionModel.FIXED_AMOUNT,
            shareoutFormula = "",
            payoutOrderMethod = "FIXED_ORDER",
            shareValue = 0.0,
            contributionAmount = 2000.0,
            socialFundEnabled = false,
            cycleLengthMonths = 10,
            loanMultiplier = 0.0,
            interestRate = 0.0,
            fineAmount = 0.0,
        ),
    ),
    viewerRole = ViewerRoleInfo(role = ViewerRole.MEMBER, memberId = 777L),
    corpus = GroupCorpus(
        currentBalance = 20000.0,
        openingBalance = 0.0,
        totalContributionsThisCycle = 0.0,
        totalLoansOutstanding = 0.0,
        isCycleEnd = false,
        lastUpdated = "2026-07-20T00:00:00Z",
        rotationPosition = 7,
        nextRecipientName = "Amina Hassan",
        nextRecipientPosition = 4,
    ),
    accounts = GroupAccounts(
        savingsBalance = 20000.0,
        loansOutstanding = 0.0,
        activeLoanCount = 0,
        shareOutProjection = null,
        recentActivity = emptyList(),
    ),
)

/**
 * In-memory [GroupDashboardRepository] fake — `groupDashboardStream` is called exactly once per
 * [GroupDashboardViewModel] instance (fixed `groupId` constructor nav-arg, not a dynamic-key
 * re-fetchable stream like `MemberDashboardRepository`), so a single buffered [MutableStateFlow]
 * + a single shared `refreshTrigger` is sufficient — mirrors `FakeMemberDashboardRepository`'s
 * per-key convention, simplified for the single-key case.
 */
private class FakeGroupDashboardRepository : GroupDashboardRepository {

    val requestedGroupIds = mutableListOf<String>()
    var refreshTriggerCount = 0

    private val stateFlow = MutableStateFlow<ScreenState<GroupDashboard>>(ScreenState.Loading)
    private val refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    fun emit(screenState: ScreenState<GroupDashboard>) {
        stateFlow.value = screenState
    }

    override fun groupDashboardStream(
        groupId: String,
        scope: CoroutineScope,
        fetchPolicy: FetchPolicy,
    ): ScreenDataStream<GroupDashboard> {
        requestedGroupIds += groupId
        scope.launch {
            refreshTrigger.collect { refreshTriggerCount++ }
        }
        return screenDataStreamForTesting(
            state = stateFlow,
            refreshTrigger = refreshTrigger,
        )
    }
}
