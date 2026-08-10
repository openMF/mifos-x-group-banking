/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.meetingsummary

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
import org.mifos.groupbanking.core.data.repository.MeetingSummaryRepository
import org.mifos.groupbanking.core.model.LoanSummaryItem
import org.mifos.groupbanking.core.model.MeetingSummaryData
import org.mifos.groupbanking.core.model.SavingsBreakdownItem
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * See API.md#viewmodel — `MeetingSummaryViewModelTest` exercises the [ScreenState] ->
 * [MeetingSummaryState] mapping, the ShareMeetingReport / NavigateDone / Retry handlers, and the
 * Unauthenticated / NoNetwork / Error error-taxonomy branches, per RULE-TDD-METHODOLOGY-001 /
 * RULE-IMPL-DEAD-CLICKABLE-001.
 */
@OptIn(ExperimentalScreenDataStreamTestingApi::class)
class MeetingSummaryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: FakeMeetingSummaryRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: MeetingSummaryViewModel

    private fun createViewModel() {
        viewModel = MeetingSummaryViewModel(
            repository = repository,
            sessionManager = sessionManager,
            crashReporter = ConsoleCrashReporter(),
            analytics = KptAnalyticsTracker(NoOpAnalyticsHelper()),
            groupId = GROUP_ID,
            meetingNumber = MEETING_NUMBER,
            meetingId = MEETING_ID,
        )
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeMeetingSummaryRepository()
        sessionManager = SessionManager(policy = SecurityPolicy())
        createViewModel()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading with nav args seeded and nothing sharing`() = runTest(testDispatcher) {
        val state = viewModel.stateFlow.value
        assertTrue(state.isLoading)
        assertNull(state.meetingSummary)
        assertNull(state.error)
        assertFalse(state.isSharing)
        assertEquals(GROUP_ID, state.groupId)
        assertEquals(MEETING_NUMBER, state.meetingNumber)
        assertEquals(MEETING_ID, state.meetingId)
    }

    @Test
    fun `init requests the stream for the nav-arg group and meeting`() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf(GROUP_ID to MEETING_NUMBER), repository.requestedKeys)
    }

    @Test
    fun `Content maps the meeting summary and clears loading`() = runTest(testDispatcher) {
        repository.emit(ScreenState.Content(data = meetingSummary()))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertFalse(state.isLoading)
        assertEquals(5, state.meetingSummary?.meetingNumber)
        assertEquals(1750L, state.meetingSummary?.totalSavingsCollected)
        assertEquals(5, state.meetingSummary?.savingsBreakdown?.size)
        assertEquals(MeetingSummaryScreenState.Content, state.screenState)
        assertNull(state.error)
    }

    @Test
    fun `ShareMeetingReport emits ShareSummary with a report carrying the meeting figures`() =
        runTest(testDispatcher) {
            repository.emit(ScreenState.Content(data = meetingSummary()))
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.eventFlow.test {
                viewModel.trySendAction(MeetingSummaryAction.ShareMeetingReport)
                val event = awaitItem()
                assertTrue(event is MeetingSummaryEvent.ShareSummary)
                val text = (event as MeetingSummaryEvent.ShareSummary).reportText
                assertTrue(text.contains("Meeting #5"))
                assertTrue(text.contains("1750"))
                assertTrue(text.contains("Amina Wangari"))
            }
            // isSharing is toggled true then false within the handler — settles back to false.
            assertFalse(viewModel.stateFlow.value.isSharing)
        }

    @Test
    fun `ShareMeetingReport with no loaded summary emits nothing`() = runTest(testDispatcher) {
        viewModel.eventFlow.test {
            viewModel.trySendAction(MeetingSummaryAction.ShareMeetingReport)
            testDispatcher.scheduler.advanceUntilIdle()
            expectNoEvents()
        }
    }

    @Test
    fun `NavigateDone emits NavigateToCalendar`() = runTest(testDispatcher) {
        viewModel.eventFlow.test {
            viewModel.trySendAction(MeetingSummaryAction.NavigateDone)
            assertEquals(MeetingSummaryEvent.NavigateToCalendar, awaitItem())
        }
    }

    @Test
    fun `Retry clears the error and re-dispatches the stream fetch`() = runTest(testDispatcher) {
        repository.emit(ScreenState.NoNetwork())
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(MeetingSummaryAction.Retry)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.stateFlow.value.error)
        assertEquals(1, repository.refreshTriggerCount)
    }

    @Test
    fun `stream NoNetwork maps to Network error`() = runTest(testDispatcher) {
        repository.emit(ScreenState.NoNetwork())
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(MeetingSummaryError.Network, state.error)
        assertTrue(state.error?.retry ?: false)
        assertFalse(state.isLoading)
        assertEquals(MeetingSummaryScreenState.Error, state.screenState)
    }

    @Test
    fun `stream generic Error maps to Server error`() = runTest(testDispatcher) {
        repository.emit(ScreenState.Error(error = IllegalStateException("boom"), isNetworkError = false))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(MeetingSummaryError.Server, viewModel.stateFlow.value.error)
    }

    @Test
    fun `stream Unauthenticated clears the session and sets Auth error`() = runTest(testDispatcher) {
        sessionManager.startSession()
        assertTrue(sessionManager.isSessionActive.value)

        repository.emit(ScreenState.Unauthenticated)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(MeetingSummaryError.Auth, state.error)
        assertFalse(state.error?.retry ?: true)
        assertFalse(sessionManager.isSessionActive.value)
    }

    private companion object {
        const val GROUP_ID = 42
        const val MEETING_NUMBER = 5
        const val MEETING_ID = "MTG-2026-05-12"
    }
}

private fun meetingSummary(): MeetingSummaryData = MeetingSummaryData(
    meetingId = "MTG-2026-05-12",
    meetingNumber = 5,
    actualDate = "12 May 2026",
    attendanceCount = 5,
    totalMemberCount = 5,
    groupSavingsCollected = 1500,
    individualSavingsCollected = 250,
    totalSavingsCollected = 1750,
    loansDisbursed = 8000,
    loansRepaid = 2500,
    finesCollected = 100,
    openingCorpus = 51750,
    closingCorpus = 64100,
    savingsBreakdown = listOf(
        SavingsBreakdownItem("1001", "Amina Wangari", 300, 50),
        SavingsBreakdownItem("1002", "Joseph Otieno", 300, 0),
        SavingsBreakdownItem("1003", "Grace Wanjiku", 300, 100),
        SavingsBreakdownItem("1004", "Peter Kamau", 300, 50),
        SavingsBreakdownItem("1005", "Mary Achieng", 300, 50),
    ),
    loanItems = listOf(
        LoanSummaryItem("1004", "Peter Kamau", 8000, 0, 8000),
    ),
)

/**
 * In-memory [MeetingSummaryRepository] fake — `meetingSummaryStream` is called exactly once per
 * [MeetingSummaryViewModel] instance (fixed groupId/meetingNumber constructor nav-args), so a
 * single buffered [MutableStateFlow] + a single shared `refreshTrigger` is sufficient — mirrors
 * `FakeLoanDetailRepository`'s identical single-key convention.
 */
private class FakeMeetingSummaryRepository : MeetingSummaryRepository {

    val requestedKeys = mutableListOf<Pair<Int, Int>>()
    var refreshTriggerCount = 0

    private val stateFlow = MutableStateFlow<ScreenState<MeetingSummaryData>>(ScreenState.Loading)
    private val refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    fun emit(screenState: ScreenState<MeetingSummaryData>) {
        stateFlow.value = screenState
    }

    override fun meetingSummaryStream(
        groupId: Int,
        meetingNumber: Int,
        scope: CoroutineScope,
        fetchPolicy: FetchPolicy,
    ): ScreenDataStream<MeetingSummaryData> {
        requestedKeys += groupId to meetingNumber
        scope.launch {
            refreshTrigger.collect { refreshTriggerCount++ }
        }
        return screenDataStreamForTesting(
            state = stateFlow,
            refreshTrigger = refreshTrigger,
        )
    }

    val primedSummaries = mutableListOf<Triple<Int, Int, MeetingSummaryData>>()

    override suspend fun primeSubmittedSummary(groupId: Int, meetingNumber: Int, data: MeetingSummaryData) {
        primedSummaries += Triple(groupId, meetingNumber, data)
    }
}
