/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.previousmeetingreview

import app.cash.turbine.test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kpt.core.analytics.KptAnalyticsTracker
import kpt.core.base.analytics.NoOpAnalyticsHelper
import kpt.core.base.observability.ConsoleCrashReporter
import kpt.core.base.security.SecurityPolicy
import kpt.core.base.security.SessionManager
import kpt.core.base.store.screen.ScreenState
import org.mifos.groupbanking.core.data.repository.PreviousMeetingReviewRepository
import org.mifos.groupbanking.core.data.repository.PreviousMeetingReviewStream
import org.mifos.groupbanking.core.model.AttendanceRecord
import org.mifos.groupbanking.core.model.AttendanceStatus
import org.mifos.groupbanking.core.model.LoanSummaryItem
import org.mifos.groupbanking.core.model.MeetingSummaryData
import org.mifos.groupbanking.core.model.PreviousMeetingDetail
import org.mifos.groupbanking.core.model.SavingsBreakdownItem
import org.mifos.groupbanking.core.model.UnresolvedItem
import org.mifos.groupbanking.core.model.UnresolvedType
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * See API.md#viewmodel — `PreviousMeetingReviewViewModelTest` exercises the [ScreenState] ->
 * [PreviousMeetingReviewState] mapping, the NavigateBack / StartNewMeeting / Retry handlers, and the
 * Unauthenticated / NoNetwork / Error error-taxonomy branches, per RULE-TDD-METHODOLOGY-001 /
 * RULE-IMPL-DEAD-CLICKABLE-001.
 */
class PreviousMeetingReviewViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: FakePreviousMeetingReviewRepository
    private lateinit var sessionManager: SessionManager

    private fun createViewModel(launchedFrom: String = "calendar"): PreviousMeetingReviewViewModel =
        PreviousMeetingReviewViewModel(
            repository = repository,
            sessionManager = sessionManager,
            crashReporter = ConsoleCrashReporter(),
            analytics = KptAnalyticsTracker(NoOpAnalyticsHelper()),
            groupId = GROUP_ID,
            meetingNumber = MEETING_NUMBER,
            meetingId = MEETING_ID,
            launchedFrom = launchedFrom,
        )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakePreviousMeetingReviewRepository()
        sessionManager = SessionManager(policy = SecurityPolicy())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading with nav args seeded`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val state = viewModel.stateFlow.value
        assertTrue(state.isLoading)
        assertNull(state.meetingDetail)
        assertNull(state.error)
        assertEquals(GROUP_ID, state.groupId)
        assertEquals(MEETING_NUMBER, state.meetingNumber)
        assertEquals(MEETING_ID, state.meetingId)
        assertEquals("calendar", state.launchedFrom)
        assertNull(state.nextMeetingNumber)
    }

    @Test
    fun `conduct-launch seeds the next-meeting nav args`() = runTest(testDispatcher) {
        val viewModel = createViewModel(launchedFrom = "conduct")
        val state = viewModel.stateFlow.value
        assertTrue(state.isConductLaunched)
        assertEquals(MEETING_NUMBER + 1, state.nextMeetingNumber)
    }

    @Test
    fun `init requests the stream for the nav-arg keys`() = runTest(testDispatcher) {
        createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf(Triple(GROUP_ID, MEETING_NUMBER, MEETING_ID)), repository.requestedKeys)
    }

    @Test
    fun `Content maps the meeting detail plus unresolved items and clears loading`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            repository.emit(ScreenState.Content(data = previousMeetingDetail()))
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.stateFlow.value
            assertFalse(state.isLoading)
            assertEquals(15, state.meetingDetail?.meetingNumber)
            assertEquals(2000L, state.meetingDetail?.totalSavingsCollected)
            assertEquals(5, state.meetingDetail?.attendanceRecords?.size)
            assertEquals(1, state.unresolvedItems.size)
            assertEquals(UnresolvedType.UNPAID_FINE, state.unresolvedItems.first().type)
            assertEquals(PreviousMeetingReviewScreenState.Content, state.screenState)
            assertNull(state.error)
        }

    @Test
    fun `NavigateBack emits NavigateBack event`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(PreviousMeetingReviewAction.NavigateBack)
            assertEquals(PreviousMeetingReviewEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `StartNewMeeting on conduct launch emits NavigateToConduct with the next meeting`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(launchedFrom = "conduct")
            viewModel.eventFlow.test {
                viewModel.trySendAction(PreviousMeetingReviewAction.StartNewMeeting)
                val event = awaitItem()
                assertTrue(event is PreviousMeetingReviewEvent.NavigateToConduct)
                event as PreviousMeetingReviewEvent.NavigateToConduct
                assertEquals(MEETING_NUMBER + 1, event.meetingNumber)
                assertEquals(GROUP_ID, event.groupId)
            }
        }

    @Test
    fun `StartNewMeeting on calendar launch emits nothing`() = runTest(testDispatcher) {
        val viewModel = createViewModel(launchedFrom = "calendar")
        viewModel.eventFlow.test {
            viewModel.trySendAction(PreviousMeetingReviewAction.StartNewMeeting)
            testDispatcher.scheduler.advanceUntilIdle()
            expectNoEvents()
        }
    }

    @Test
    fun `Retry clears the error and re-drives both reads`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        repository.emit(ScreenState.NoNetwork())
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(PreviousMeetingReviewAction.Retry)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.stateFlow.value.error)
        assertEquals(1, repository.retryCount)
    }

    @Test
    fun `stream NoNetwork maps to Network error`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        repository.emit(ScreenState.NoNetwork())
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(PreviousMeetingReviewError.Network, state.error)
        assertTrue(state.error?.retry ?: false)
        assertEquals(PreviousMeetingReviewScreenState.Error, state.screenState)
    }

    @Test
    fun `stream generic Error maps to Server error`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        repository.emit(ScreenState.Error(error = IllegalStateException("boom"), isNetworkError = false))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(PreviousMeetingReviewError.Server, viewModel.stateFlow.value.error)
    }

    @Test
    fun `stream Unauthenticated clears the session and sets Auth error`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        sessionManager.startSession()
        assertTrue(sessionManager.isSessionActive.value)

        repository.emit(ScreenState.Unauthenticated)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(PreviousMeetingReviewError.Auth, state.error)
        assertFalse(state.error?.retry ?: true)
        assertFalse(sessionManager.isSessionActive.value)
    }

    private companion object {
        const val GROUP_ID = 7
        const val MEETING_NUMBER = 15
        const val MEETING_ID = "meeting_015"
    }
}

private fun previousMeetingDetail(): PreviousMeetingDetail = PreviousMeetingDetail(
    summary = MeetingSummaryData(
        meetingId = "meeting_015",
        meetingNumber = 15,
        actualDate = "2026-05-06",
        attendanceCount = 4,
        totalMemberCount = 5,
        groupSavingsCollected = 1800,
        individualSavingsCollected = 200,
        totalSavingsCollected = 2000,
        loansDisbursed = 5000,
        loansRepaid = 458,
        finesCollected = 100,
        openingCorpus = 41800,
        closingCorpus = 43800,
        savingsBreakdown = listOf(
            SavingsBreakdownItem("client_001", "Amina Wanjiru", 500, 0),
        ),
        loanItems = listOf(
            LoanSummaryItem("client_002", "Joseph Kamau", 5000, 0, 5000),
        ),
    ),
    attendanceRecords = listOf(
        AttendanceRecord("client_001", "Amina Wanjiru", AttendanceStatus.PRESENT, 0),
        AttendanceRecord("client_002", "Joseph Kamau", AttendanceStatus.PRESENT, 0),
        AttendanceRecord("client_003", "Grace Achieng", AttendanceStatus.PRESENT, 0),
        AttendanceRecord("client_004", "Peter Otieno", AttendanceStatus.LATE, 50),
        AttendanceRecord("client_005", "Mary Njeri", AttendanceStatus.ABSENT, 50),
    ),
    unresolvedItems = listOf(
        UnresolvedItem(UnresolvedType.UNPAID_FINE, "Mary Njeri — fine KES 50 not collected", "client_005"),
    ),
)

/**
 * In-memory [PreviousMeetingReviewRepository] fake — `previousMeetingStream` is called exactly once
 * per [PreviousMeetingReviewViewModel] instance (fixed nav-arg constructor params), so a single
 * buffered [MutableStateFlow] backing the merged stream is sufficient. [retryCount] counts calls to
 * the returned [PreviousMeetingReviewStream.retry].
 */
private class FakePreviousMeetingReviewRepository : PreviousMeetingReviewRepository {

    val requestedKeys = mutableListOf<Triple<Int, Int, String>>()
    var retryCount = 0

    private val stateFlow = MutableStateFlow<ScreenState<PreviousMeetingDetail>>(ScreenState.Loading)

    fun emit(screenState: ScreenState<PreviousMeetingDetail>) {
        stateFlow.value = screenState
    }

    override fun previousMeetingStream(
        groupId: Int,
        meetingNumber: Int,
        meetingId: String,
        scope: CoroutineScope,
    ): PreviousMeetingReviewStream {
        requestedKeys += Triple(groupId, meetingNumber, meetingId)
        return PreviousMeetingReviewStream(
            state = stateFlow,
            onRetry = { retryCount++ },
        )
    }
}
