/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.organizerdashboard

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
import org.mifos.groupbanking.core.data.repository.OrganizerDashboardRepository
import org.mifos.groupbanking.core.model.OrganizerActivityItem
import org.mifos.groupbanking.core.model.OrganizerActivityType
import org.mifos.groupbanking.core.model.OrganizerDashboardSummary
import org.mifos.groupbanking.core.model.ScheduledMeeting
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Exercises every declared [OrganizerDashboardAction] path plus the [ScreenState] ->
 * [OrganizerDashboardState] mapping, per RULE-TDD-METHODOLOGY-001 / RULE-IMPL-DEAD-CLICKABLE-001.
 * See API.md#viewmodel.
 */
@OptIn(ExperimentalScreenDataStreamTestingApi::class)
class OrganizerDashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: FakeOrganizerDashboardRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: OrganizerDashboardViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeOrganizerDashboardRepository()
        sessionManager = SessionManager(policy = SecurityPolicy())
        viewModel = OrganizerDashboardViewModel(
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
        assertEquals(0, state.myGroupCount)
        assertEquals("", state.organizerName)
    }

    @Test
    fun `first mount subscribes to the organizer dashboard stream`() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, repository.streamRequests)
    }

    @Test
    fun `Content maps every KPI plus schedule and activity into state`() = runTest(testDispatcher) {
        repository.emit(ScreenState.Content(data = sampleDashboard()))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertFalse(state.isLoading)
        assertEquals("David Otieno", state.organizerName)
        assertEquals(5, state.myGroupCount)
        assertEquals(42, state.totalMembers)
        assertEquals(1, state.pendingShareOutCount)
        assertEquals(2, state.meetingsTodayCount)
        assertFalse(state.fieldOfficerEnabled)
        assertEquals(2, state.todaySchedule.size)
        assertEquals(1, state.recentActivity.size)
        assertEquals(OrganizerDashboardScreenState.Content, state.screenState)
    }

    @Test
    fun `zero groups derives Empty screenState from Content`() = runTest(testDispatcher) {
        repository.emit(ScreenState.Content(data = sampleDashboard().copy(myGroupCount = 0)))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(OrganizerDashboardScreenState.Empty, state.screenState)
    }

    @Test
    fun `OnViewAllGroups emits NavigateToGroupList`() = runTest(testDispatcher) {
        repository.emit(ScreenState.Content(data = sampleDashboard()))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(OrganizerDashboardAction.OnViewAllGroups)
            assertEquals(OrganizerDashboardEvent.NavigateToGroupList, awaitItem())
        }
    }

    @Test
    fun `OnMeetingGroupClick navigates to that group's meeting calendar`() = runTest(testDispatcher) {
        repository.emit(ScreenState.Content(data = sampleDashboard()))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(OrganizerDashboardAction.OnMeetingGroupClick("ctr-001"))
            assertEquals(OrganizerDashboardEvent.NavigateToMeetingCalendar("ctr-001"), awaitItem())
        }
    }

    @Test
    fun `OnViewMeetingsToday opens the earliest today meeting's calendar`() = runTest(testDispatcher) {
        repository.emit(ScreenState.Content(data = sampleDashboard()))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(OrganizerDashboardAction.OnViewMeetingsToday)
            assertEquals(OrganizerDashboardEvent.NavigateToMeetingCalendar("ctr-001"), awaitItem())
        }
    }

    @Test
    fun `OnViewMeetingsToday is ignored when no meetings are scheduled today`() = runTest(testDispatcher) {
        repository.emit(
            ScreenState.Content(
                data = sampleDashboard().copy(meetingsTodayCount = 0, todaySchedule = emptyList()),
            ),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(OrganizerDashboardAction.OnViewMeetingsToday)
            expectNoEvents()
        }
    }

    @Test
    fun `OnViewFieldOfficer emits NavigateToFieldOfficerDashboard when enabled`() = runTest(testDispatcher) {
        repository.emit(ScreenState.Content(data = sampleDashboard().copy(fieldOfficerEnabled = true)))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(OrganizerDashboardAction.OnViewFieldOfficer)
            assertEquals(OrganizerDashboardEvent.NavigateToFieldOfficerDashboard, awaitItem())
        }
    }

    @Test
    fun `OnViewFieldOfficer is ignored when the optional tier is disabled`() = runTest(testDispatcher) {
        repository.emit(ScreenState.Content(data = sampleDashboard().copy(fieldOfficerEnabled = false)))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(OrganizerDashboardAction.OnViewFieldOfficer)
            expectNoEvents()
        }
    }

    @Test
    fun `OnOpenNotifications emits the deferred-notifications event`() = runTest(testDispatcher) {
        viewModel.eventFlow.test {
            viewModel.trySendAction(OrganizerDashboardAction.OnOpenNotifications)
            assertEquals(OrganizerDashboardEvent.NotificationsDeferred, awaitItem())
        }
    }

    @Test
    fun `OnRefresh sets isRefreshing and dispatches a fresh fetch`() = runTest(testDispatcher) {
        repository.emit(ScreenState.Content(data = sampleDashboard()))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(OrganizerDashboardAction.OnRefresh)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.stateFlow.value.isRefreshing)
        assertEquals(1, repository.triggerCount)
    }

    @Test
    fun `Retry re-dispatches the underlying stream fetch`() = runTest(testDispatcher) {
        repository.emit(ScreenState.NoNetwork())
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(OrganizerDashboardAction.Retry)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.triggerCount)
    }

    @Test
    fun `stream NoNetwork maps to Network error`() = runTest(testDispatcher) {
        repository.emit(ScreenState.NoNetwork())
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(OrganizerDashboardError.Network, state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun `stream Error with isNetworkError false maps to Server error`() = runTest(testDispatcher) {
        repository.emit(ScreenState.Error(error = IllegalStateException("boom"), isNetworkError = false))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(OrganizerDashboardError.Server, viewModel.stateFlow.value.error)
    }

    @Test
    fun `stream Unauthenticated maps to Auth error and clears the session`() = runTest(testDispatcher) {
        sessionManager.startSession()
        assertTrue(sessionManager.isSessionActive.value)

        repository.emit(ScreenState.Unauthenticated)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(OrganizerDashboardError.Auth, state.error)
        assertFalse(state.error?.retry ?: true)
        assertFalse(sessionManager.isSessionActive.value)
    }
}

private fun sampleDashboard(): OrganizerDashboardSummary = OrganizerDashboardSummary(
    organizerName = "David Otieno",
    myGroupCount = 5,
    totalMembers = 42,
    pendingShareOutCount = 1,
    meetingsTodayCount = 2,
    fieldOfficerEnabled = false,
    todaySchedule = listOf(
        ScheduledMeeting("ctr-001", "Mwangaza Women's Group", "09:00", 12, "Community Hall"),
        ScheduledMeeting("ctr-002", "Tumaini Savings Group", "14:00", 8, null),
    ),
    recentActivity = listOf(
        OrganizerActivityItem("act-001", OrganizerActivityType.DEPOSIT, "Weekly contribution", 300.0, "2026-05-09", "Amina Wanjiru", "Mwangaza Women's Group"),
    ),
)

/**
 * In-memory [OrganizerDashboardRepository] fake — the single-key `organizerDashboardStream` returns
 * a NEW [ScreenDataStream] per call, backed by one shared state flow + one refresh trigger. Records
 * how many streams were requested and how many refreshes/retries were dispatched.
 */
private class FakeOrganizerDashboardRepository : OrganizerDashboardRepository {

    private val stateFlow = MutableStateFlow<ScreenState<OrganizerDashboardSummary>>(ScreenState.Loading)
    private val refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    var streamRequests = 0
        private set
    var triggerCount = 0
        private set

    fun emit(screenState: ScreenState<OrganizerDashboardSummary>) {
        stateFlow.value = screenState
    }

    override fun organizerDashboardStream(
        scope: CoroutineScope,
        fetchPolicy: FetchPolicy,
    ): ScreenDataStream<OrganizerDashboardSummary> {
        streamRequests += 1
        scope.launch {
            refreshTrigger.collect { triggerCount += 1 }
        }
        return screenDataStreamForTesting(
            state = stateFlow,
            refreshTrigger = refreshTrigger,
        )
    }
}
