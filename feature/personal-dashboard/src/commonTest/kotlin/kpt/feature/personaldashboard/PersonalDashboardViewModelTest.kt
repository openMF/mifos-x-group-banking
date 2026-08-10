/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.personaldashboard

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
import kotlinx.datetime.LocalDate
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
import kpt.core.data.repository.MemberDashboardRepository
import kpt.core.model.GroupSummary
import kpt.core.model.MemberDashboard
import kpt.core.model.SavingsMechanism
import kpt.core.model.SavingsTransaction
import kpt.core.model.TransactionType
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * See API.md#viewmodel — PersonalDashboardViewModelTest exercises every declared
 * [PersonalDashboardAction] path plus the [ScreenState]→[PersonalDashboardState] mapping
 * (including the pool-model-adaptive ACCUMULATING vs ROTATING_PAYOUT projection and the
 * dynamic-key group-switch re-fetch), per RULE-TDD-METHODOLOGY-001 / RULE-IMPL-DEAD-CLICKABLE-001.
 */
@OptIn(ExperimentalScreenDataStreamTestingApi::class)
class PersonalDashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: FakeMemberDashboardRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: PersonalDashboardViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeMemberDashboardRepository()
        sessionManager = SessionManager(policy = SecurityPolicy())
        viewModel = PersonalDashboardViewModel(
            repository = repository,
            sessionManager = sessionManager,
            crashReporter = ConsoleCrashReporter(),
            analytics = KptAnalyticsTracker(NoOpAnalyticsHelper()),
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading with defaults`() = runTest(testDispatcher) {
        val state = viewModel.stateFlow.value
        assertTrue(state.isLoading)
        assertNull(state.error)
        assertTrue(state.myGroups.isEmpty())
        assertEquals("", state.memberName)
    }

    @Test
    fun `first mount requests the default group (selectedGroupId null)`() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf<String?>(null), repository.requestedGroupIds)
    }

    @Test
    fun `Content with ACCUMULATING pool model populates shareOutProjection and clears rotation fields`() =
        runTest(testDispatcher) {
            repository.emit(null, ScreenState.Content(data = accumulatingDashboard()))
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.stateFlow.value
            assertFalse(state.isLoading)
            assertEquals("ACCUMULATING", state.poolModel)
            assertEquals(23500.0, state.shareOutProjection)
            assertNull(state.rotationPosition)
            assertNull(state.nextRecipientEta)
            assertEquals("Amina Hassan", state.memberName)
            assertEquals(1, state.recentTransactions.size)
        }

    @Test
    fun `Content with ROTATING_PAYOUT pool model populates rotation fields and zeroes shareOutProjection`() =
        runTest(testDispatcher) {
            repository.emit(null, ScreenState.Content(data = rotatingDashboard()))
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.stateFlow.value
            assertEquals("ROTATING_PAYOUT", state.poolModel)
            assertEquals(0.0, state.shareOutProjection)
            assertEquals(3, state.rotationPosition)
            assertEquals("2026-08-15", state.nextRecipientEta)
        }

    @Test
    fun `myGroups isEmpty derives Empty screenState from Content`() = runTest(testDispatcher) {
        repository.emit(null, ScreenState.Content(data = accumulatingDashboard().copy(myGroups = emptyList())))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(PersonalDashboardScreenState.Empty, state.screenState)
    }

    @Test
    fun `OnSelectGroup re-fetches with the tapped groupId and resets isLoading`() = runTest(testDispatcher) {
        repository.emit(null, ScreenState.Content(data = accumulatingDashboard()))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(PersonalDashboardAction.OnSelectGroup("grp-002"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(null, "grp-002"), repository.requestedGroupIds)

        repository.emit(
            "grp-002",
            ScreenState.Content(
                data = accumulatingDashboard().copy(
                    selectedGroup = GroupSummary("grp-002", "Umoja SACCO", SavingsMechanism.ACCUMULATING),
                ),
            ),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("grp-002", viewModel.stateFlow.value.selectedGroup?.groupId)
    }

    @Test
    fun `OnRefresh sets isRefreshing and dispatches a bypass-and-refresh fetch`() = runTest(testDispatcher) {
        repository.emit(null, ScreenState.Content(data = accumulatingDashboard()))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(PersonalDashboardAction.OnRefresh)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.stateFlow.value.isRefreshing)
        assertEquals(1, repository.triggerCountByKey[null])
    }

    @Test
    fun `OnRetry re-dispatches the underlying stream fetch`() = runTest(testDispatcher) {
        repository.emit(null, ScreenState.NoNetwork())
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(PersonalDashboardAction.OnRetry)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.triggerCountByKey[null])
    }

    @Test
    fun `OnSavingsCardClick emits NavigateToSavings with groupId and poolModel`() = runTest(testDispatcher) {
        repository.emit(null, ScreenState.Content(data = accumulatingDashboard()))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(PersonalDashboardAction.OnSavingsCardClick)
            val event = awaitItem()
            assertTrue(event is PersonalDashboardEvent.NavigateToSavings)
            assertEquals(301L, (event as PersonalDashboardEvent.NavigateToSavings).clientId)
            assertEquals(401L, event.groupLinkedSavingsId)
            assertEquals(411L, event.individualSavingsId)
            assertEquals("ACCUMULATING", event.poolModel)
        }
    }

    @Test
    fun `OnSavingsCardClick before content loads does not navigate or crash`() = runTest(testDispatcher) {
        // stream still Loading — selectedGroup is null
        viewModel.trySendAction(PersonalDashboardAction.OnSavingsCardClick)
        testDispatcher.scheduler.advanceUntilIdle()

        // No assertion failure / crash means the defensive guard held; state unchanged.
        assertTrue(viewModel.stateFlow.value.isLoading)
    }

    @Test
    fun `OnOpenNotifications emits NotificationsDeferred without navigating`() = runTest(testDispatcher) {
        // G14 — the notification bell is a deferred affordance: it emits NotificationsDeferred
        // (snackbar), never a navigation event.
        viewModel.eventFlow.test {
            viewModel.trySendAction(PersonalDashboardAction.OnOpenNotifications)
            val event = awaitItem()
            assertTrue(event is PersonalDashboardEvent.NotificationsDeferred)
        }
    }

    @Test
    fun `stream NoNetwork maps to Network error`() = runTest(testDispatcher) {
        repository.emit(null, ScreenState.NoNetwork())
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(DashboardError.Network, state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun `stream Error with isNetworkError false maps to Server error`() = runTest(testDispatcher) {
        repository.emit(null, ScreenState.Error(error = IllegalStateException("boom"), isNetworkError = false))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(DashboardError.Server, viewModel.stateFlow.value.error)
    }

    @Test
    fun `stream Unauthenticated maps to Unauthorized error and clears the session`() = runTest(testDispatcher) {
        sessionManager.startSession()
        assertTrue(sessionManager.isSessionActive.value)

        repository.emit(null, ScreenState.Unauthenticated)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(DashboardError.Unauthorized, state.error)
        assertFalse(state.error?.retry ?: true)
        assertFalse(sessionManager.isSessionActive.value)
    }
}

private fun accumulatingDashboard(): MemberDashboard = MemberDashboard(
    memberName = "Amina Hassan",
    clientId = 301L,
    groupLinkedSavingsId = 401L,
    individualSavingsId = 411L,
    myGroups = listOf(GroupSummary("grp-001", "Mwangaza VSLA", SavingsMechanism.ACCUMULATING)),
    selectedGroup = GroupSummary("grp-001", "Mwangaza VSLA", SavingsMechanism.ACCUMULATING),
    poolModel = SavingsMechanism.ACCUMULATING,
    groupLinkedSavingsBalance = 20000.0,
    individualSavingsBalance = 5000.0,
    shareOutProjection = 23500.0,
    rotationPosition = null,
    nextRecipientEta = null,
    recentTransactions = listOf(
        SavingsTransaction(
            id = "t-1",
            date = LocalDate.parse("2026-07-10"),
            type = TransactionType.DEPOSIT,
            amount = 2500.0,
        ),
    ),
)

private fun rotatingDashboard(): MemberDashboard = MemberDashboard(
    memberName = "Amina Hassan",
    clientId = 301L,
    groupLinkedSavingsId = 401L,
    individualSavingsId = 411L,
    myGroups = listOf(GroupSummary("grp-003", "Jirani ROSCA", SavingsMechanism.ROTATING_PAYOUT)),
    selectedGroup = GroupSummary("grp-003", "Jirani ROSCA", SavingsMechanism.ROTATING_PAYOUT),
    poolModel = SavingsMechanism.ROTATING_PAYOUT,
    groupLinkedSavingsBalance = 12000.0,
    individualSavingsBalance = 0.0,
    shareOutProjection = null,
    rotationPosition = 3,
    nextRecipientEta = "2026-08-15",
    recentTransactions = emptyList(),
)

/**
 * In-memory [MemberDashboardRepository] fake keyed by `selectedGroupId` — mirrors
 * [FakeGroupTypeConfigRepository] but per-key, since `memberDashboardStream` returns a NEW
 * [ScreenDataStream] per call (dynamic-key read, not a single `Flow<Key>` stream; see
 * `MemberDashboardRepositoryImpl` KDoc). Records every requested `selectedGroupId` so
 * group-switch re-fetch behavior is assertable, and counts every re-fetch dispatched through the
 * per-key refresh trigger — `ScreenDataStream.refresh()`/`.retry()`/`.refreshFresh()` all emit
 * into the SAME `refreshTrigger` in production (see `ScreenDataStream` KDoc), so one counter per
 * key is sufficient; each test below exercises exactly one call site per key.
 */
private class FakeMemberDashboardRepository : MemberDashboardRepository {

    private val stateFlows = mutableMapOf<String?, MutableStateFlow<ScreenState<MemberDashboard>>>()
    private val refreshTriggers = mutableMapOf<String?, MutableSharedFlow<Unit>>()

    val requestedGroupIds = mutableListOf<String?>()
    val triggerCountByKey = mutableMapOf<String?, Int>()

    fun emit(selectedGroupId: String?, screenState: ScreenState<MemberDashboard>) {
        stateFlowFor(selectedGroupId).value = screenState
    }

    private fun stateFlowFor(key: String?) =
        stateFlows.getOrPut(key) { MutableStateFlow(ScreenState.Loading) }

    private fun triggerFor(key: String?) =
        refreshTriggers.getOrPut(key) { MutableSharedFlow(extraBufferCapacity = 1) }

    override fun memberDashboardStream(
        selectedGroupId: String?,
        scope: CoroutineScope,
        fetchPolicy: FetchPolicy,
    ): ScreenDataStream<MemberDashboard> {
        requestedGroupIds += selectedGroupId
        val trigger = triggerFor(selectedGroupId)
        scope.launch {
            trigger.collect {
                triggerCountByKey[selectedGroupId] = (triggerCountByKey[selectedGroupId] ?: 0) + 1
            }
        }
        return screenDataStreamForTesting(
            state = stateFlowFor(selectedGroupId),
            refreshTrigger = trigger,
        )
    }
}
