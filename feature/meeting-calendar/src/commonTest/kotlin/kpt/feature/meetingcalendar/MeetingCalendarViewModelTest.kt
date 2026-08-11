/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.meetingcalendar

import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kpt.core.analytics.KptAnalyticsTracker
import kpt.core.base.analytics.NoOpAnalyticsHelper
import kpt.core.base.observability.ConsoleCrashReporter
import kpt.core.base.security.SecurityPolicy
import kpt.core.base.security.SessionManager
import kpt.core.base.store.screen.FetchPolicy
import kpt.core.base.store.screen.ScreenDataStream
import kpt.core.base.store.screen.ScreenState
import kpt.core.base.store.screen.screenDataStreamForTesting
import kpt.core.data.repository.MeetingRepository
import kpt.core.model.MeetingFrequency
import kpt.core.model.MeetingListItem
import kpt.core.model.MeetingStatus
import kpt.core.model.RescheduleMeetingRequest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Exercises every declared [MeetingCalendarAction] path plus the
 * [kpt.core.base.store.screen.ScreenState]->[MeetingCalendarState] mapping, per
 * RULE-TDD-METHODOLOGY-001 / RULE-IMPL-DEAD-CLICKABLE-001. Drives the ViewModel through a
 * [FakeMeetingRepository] whose single-key stream is backed by [screenDataStreamForTesting]
 * (buffered [ScreenState] flow + refresh trigger) — the shared single-key convention used by
 * `FakeGroupDashboardRepository` / `FakeLoanDetailRepository`.
 */
class MeetingCalendarViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val createdViewModels = mutableListOf<MeetingCalendarViewModel>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        createdViewModels.forEach { it.viewModelScope.cancel() }
        createdViewModels.clear()
        Dispatchers.resetMain()
    }

    private fun buildViewModel(
        meetings: List<MeetingListItem> = emptyList(),
        failWith: Throwable? = null,
        online: Boolean = true,
        groupId: Int = GROUP_ID,
        sessionManager: SessionManager = SessionManager(policy = SecurityPolicy()),
    ): Triple<FakeMeetingRepository, SessionManager, MeetingCalendarViewModel> {
        val repository = FakeMeetingRepository(meetings = meetings, failWith = failWith, online = online)
        val viewModel = MeetingCalendarViewModel(
            repository = repository,
            sessionManager = sessionManager,
            crashReporter = ConsoleCrashReporter(),
            analytics = KptAnalyticsTracker(NoOpAnalyticsHelper()),
            groupId = groupId,
        )
        createdViewModels += viewModel
        return Triple(repository, sessionManager, viewModel)
    }

    private suspend fun MeetingCalendarViewModel.awaitState(
        timeoutMs: Long = 5_000,
        predicate: (MeetingCalendarState) -> Boolean,
    ): MeetingCalendarState = withContext(Dispatchers.Default) {
        withTimeout(timeoutMs) { stateFlow.first(predicate) }
    }

    // ─── initial state + stream mapping ─────────────────────────────────────

    @Test
    fun `initial state is loading, seeds groupId and LIST view mode`() = runTest(testDispatcher) {
        val (_, _, viewModel) = buildViewModel(groupId = GROUP_ID)

        val state = viewModel.stateFlow.value
        assertTrue(state.isLoading)
        assertNull(state.error)
        assertTrue(state.meetings.isEmpty())
        assertEquals(GROUP_ID, state.groupId)
        assertEquals(ViewMode.LIST, state.viewMode)
    }

    @Test
    fun `stream Content maps meetings and clears loading`() = runTest(testDispatcher) {
        val (_, _, viewModel) = buildViewModel(
            meetings = listOf(meeting(5, MeetingStatus.UPCOMING), meeting(4, MeetingStatus.COMPLETED, attendance = 5, collected = 2500)),
        )

        val state = viewModel.awaitState { !it.isLoading && it.meetings.isNotEmpty() }
        assertNull(state.error)
        assertEquals(2, state.meetings.size)
        assertEquals(MeetingCalendarScreenState.Content, state.screenState)
        assertEquals(5, state.upcomingMeeting?.meetingNumber)
        assertEquals(1, state.pastMeetings.size)
    }

    @Test
    fun `stream Empty maps to empty meetings with no error`() = runTest(testDispatcher) {
        val (_, _, viewModel) = buildViewModel(meetings = emptyList())

        val state = viewModel.awaitState { !it.isLoading }
        assertNull(state.error)
        assertTrue(state.meetings.isEmpty())
        assertEquals(MeetingCalendarScreenState.Empty, state.screenState)
    }

    @Test
    fun `stream connectivity failure maps to Network error`() = runTest(testDispatcher) {
        val (_, _, viewModel) = buildViewModel(failWith = FakeConnectException("connect failed"))

        val state = viewModel.awaitState { it.error != null }
        assertEquals(MeetingCalendarError.Network, state.error)
        assertFalse(state.isLoading)
        assertTrue(state.error?.retry == true)
    }

    @Test
    fun `stream 500 maps to Server error`() = runTest(testDispatcher) {
        val (_, _, viewModel) = buildViewModel(failWith = RuntimeException("HTTP 500 Internal Server Error"))

        val state = viewModel.awaitState { it.error != null }
        assertEquals(MeetingCalendarError.Server, state.error)
        assertTrue(state.error?.retry == true)
    }

    @Test
    fun `stream 401 ends the session, maps to Auth error, and emits ShowError`() = runTest(testDispatcher) {
        val sessionManager = SessionManager(policy = SecurityPolicy())
        sessionManager.startSession()
        val (_, sm, viewModel) = buildViewModel(
            failWith = RuntimeException("HTTP 401 Unauthorized"),
            sessionManager = sessionManager,
        )
        assertTrue(sm.isSessionActive.value)

        viewModel.eventFlow.test {
            val event = awaitItem()
            assertTrue(event is MeetingCalendarEvent.ShowError)
            assertEquals(MeetingCalendarError.Auth.messageKey, event.message)
        }
        val state = viewModel.awaitState { it.error != null }
        assertEquals(MeetingCalendarError.Auth, state.error)
        assertEquals(false, state.error?.retry)
        assertFalse(sm.isSessionActive.value, "Unauthenticated must call sessionManager.endSession() for real")
    }

    // ─── StartMeeting / OpenPastMeeting ─────────────────────────────────────

    @Test
    fun `StartMeeting emits NavigateToConduct with meeting params`() = runTest(testDispatcher) {
        val (_, _, viewModel) = buildViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(MeetingCalendarAction.StartMeeting("MTG-5", 5))
            assertEquals(MeetingCalendarEvent.NavigateToConduct("MTG-5", 5), awaitItem())
        }
    }

    @Test
    fun `OpenPastMeeting emits NavigateToReview with meeting params`() = runTest(testDispatcher) {
        val (_, _, viewModel) = buildViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(MeetingCalendarAction.OpenPastMeeting("MTG-2", 2))
            assertEquals(MeetingCalendarEvent.NavigateToReview("MTG-2", 2, GROUP_ID), awaitItem())
        }
    }

    // ─── G3 / F6 schedule editor (Set/Adjust Schedule + Reschedule) ──────────

    @Test
    fun `OpenScheduleEditor opens the sheet and DismissScheduleEditor closes it`() = runTest(testDispatcher) {
        val (_, _, viewModel) = buildViewModel()

        viewModel.trySendAction(MeetingCalendarAction.OpenScheduleEditor)
        assertTrue(viewModel.awaitState { it.showScheduleEditor }.showScheduleEditor)

        viewModel.trySendAction(MeetingCalendarAction.DismissScheduleEditor)
        assertFalse(viewModel.awaitState { !it.showScheduleEditor }.showScheduleEditor)
    }

    @Test
    fun `OnScheduleFieldChange updates only the changed draft field`() = runTest(testDispatcher) {
        val (_, _, viewModel) = buildViewModel()

        viewModel.trySendAction(MeetingCalendarAction.OnScheduleFieldChange(day = "WEDNESDAY"))
        viewModel.trySendAction(MeetingCalendarAction.OnScheduleFieldChange(time = "14:30"))
        viewModel.trySendAction(MeetingCalendarAction.OnScheduleFieldChange(frequency = MeetingFrequency.BIWEEKLY))

        val state = viewModel.awaitState {
            it.scheduleDay == "WEDNESDAY" &&
                it.scheduleTime == "14:30" &&
                it.scheduleFrequency == MeetingFrequency.BIWEEKLY
        }
        assertEquals("WEDNESDAY", state.scheduleDay)
        assertEquals("14:30", state.scheduleTime)
        assertEquals(MeetingFrequency.BIWEEKLY, state.scheduleFrequency)
    }

    @Test
    fun `RescheduleMeeting offline-queues the payload, closes the sheet and emits ShowScheduleUpdated`() = runTest(testDispatcher) {
        val (repository, _, viewModel) = buildViewModel(groupId = GROUP_ID)
        viewModel.trySendAction(MeetingCalendarAction.OpenScheduleEditor)
        viewModel.awaitState { it.showScheduleEditor }

        viewModel.eventFlow.test {
            viewModel.trySendAction(
                MeetingCalendarAction.RescheduleMeeting(day = "MONDAY", time = "10:00", frequency = MeetingFrequency.WEEKLY),
            )
            assertTrue(awaitItem() is MeetingCalendarEvent.ShowScheduleUpdated)
        }

        val queued = repository.lastReschedule
        assertEquals(GROUP_ID, queued?.groupId)
        assertEquals("MONDAY", queued?.day)
        assertEquals("10:00", queued?.time)
        assertEquals(MeetingFrequency.WEEKLY, queued?.frequency)

        val state = viewModel.awaitState { !it.showScheduleEditor && !it.isRescheduling }
        assertFalse(state.showScheduleEditor)
        assertFalse(state.isRescheduling)
    }

    // ─── ToggleViewMode ─────────────────────────────────────────────────────

    @Test
    fun `ToggleViewMode flips LIST to CALENDAR and back`() = runTest(testDispatcher) {
        val (_, _, viewModel) = buildViewModel()

        viewModel.trySendAction(MeetingCalendarAction.ToggleViewMode)
        val calendarState = viewModel.awaitState { it.viewMode == ViewMode.CALENDAR }
        assertEquals(ViewMode.CALENDAR, calendarState.viewMode)

        viewModel.trySendAction(MeetingCalendarAction.ToggleViewMode)
        val listState = viewModel.awaitState { it.viewMode == ViewMode.LIST }
        assertEquals(ViewMode.LIST, listState.viewMode)
    }

    // ─── RefreshMeetings / Retry ────────────────────────────────────────────

    @Test
    fun `RefreshMeetings re-dispatches a real fetch and clears isRefreshing once new data returns`() = runTest(testDispatcher) {
        val (repository, _, viewModel) = buildViewModel(meetings = listOf(meeting(1, MeetingStatus.COMPLETED)))
        viewModel.awaitState { it.meetings.size == 1 }

        repository.meetings = listOf(meeting(1, MeetingStatus.COMPLETED), meeting(2, MeetingStatus.COMPLETED))
        viewModel.trySendAction(MeetingCalendarAction.RefreshMeetings)
        val state = viewModel.awaitState { it.meetings.size == 2 }

        assertEquals(false, state.isRefreshing)
    }

    @Test
    fun `Retry clears the error and re-dispatches a real fetch that recovers`() = runTest(testDispatcher) {
        val (repository, _, viewModel) = buildViewModel(failWith = RuntimeException("HTTP 500 boom"))
        viewModel.awaitState { it.error != null }
        assertEquals(MeetingCalendarError.Server, viewModel.stateFlow.value.error)

        repository.failWith = null
        repository.meetings = listOf(meeting(1, MeetingStatus.COMPLETED))
        viewModel.trySendAction(MeetingCalendarAction.Retry)
        val state = viewModel.awaitState { it.error == null && !it.isLoading }

        assertEquals(1, state.meetings.size)
    }
}

// ---------------------------------------------------------------------------
// Fixtures
// ---------------------------------------------------------------------------

private const val GROUP_ID = 1

private fun meeting(
    number: Int,
    status: MeetingStatus,
    attendance: Int? = null,
    collected: Long? = null,
): MeetingListItem = MeetingListItem(
    meetingId = "MTG-$number",
    meetingNumber = number,
    meetingDate = "2026-07-0$number",
    status = status,
    attendanceCount = attendance,
    totalCollectedKES = collected,
)

/** Exception whose simple class name deliberately matches `categorize()`'s network-keyword scan. */
private class FakeConnectException(message: String) : Exception(message)

/**
 * Single-key stream fake — mirrors `FakeGroupDashboardRepository` / `FakeLoanDetailRepository`
 * (the shared single-key convention): a buffered [ScreenState] flow + a `refreshTrigger`, wired
 * through [screenDataStreamForTesting], rather than a live Store5. The screen state is re-derived
 * from (`failWith`, `online`, `meetings`) on subscription and on every refresh/retry, so tests
 * drive it purely through the constructor fixture + `trySendAction(RefreshMeetings)` — no live
 * Store5 fetch/source-of-truth machinery, so empty/refresh settle deterministically.
 */
private class FakeMeetingRepository(
    meetings: List<MeetingListItem> = emptyList(),
    failWith: Throwable? = null,
    online: Boolean = true,
) : MeetingRepository {

    var meetings: List<MeetingListItem> = meetings
    var failWith: Throwable? = failWith
    var online: Boolean = online

    private val stateFlow = MutableStateFlow<ScreenState<List<MeetingListItem>>>(ScreenState.Loading)
    private val refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private fun resolve(): ScreenState<List<MeetingListItem>> = when {
        failWith is FakeConnectException -> ScreenState.Error(failWith!!, isNetworkError = true)
        failWith?.message?.contains("401") == true -> ScreenState.Unauthenticated
        failWith != null -> ScreenState.Error(failWith!!, isNetworkError = false)
        !online -> ScreenState.NoNetwork()
        meetings.isEmpty() -> ScreenState.Empty
        else -> ScreenState.Content(meetings)
    }

    override fun meetingsStream(
        groupId: Int,
        scope: CoroutineScope,
        fetchPolicy: FetchPolicy,
    ): ScreenDataStream<List<MeetingListItem>> {
        stateFlow.value = resolve()
        scope.launch { refreshTrigger.collect { stateFlow.value = resolve() } }
        return screenDataStreamForTesting(state = stateFlow, refreshTrigger = refreshTrigger)
    }

    /** Records the last queued reschedule payload — G3 / F6 server-gated offline-queue write. */
    var lastReschedule: RescheduleMeetingRequest? = null
    override suspend fun rescheduleMeeting(request: RescheduleMeetingRequest): Long {
        lastReschedule = request
        return 1L
    }
}
