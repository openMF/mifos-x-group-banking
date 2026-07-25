/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.meetingcalendar

import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkChangeEvent
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkInfo
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitor
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkStatus
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
import kpt.core.base.store.infra.FetchedAtRepository
import kpt.core.base.store.infra.StoreFactory
import kpt.core.base.store.screen.FetchPolicy
import kpt.core.base.store.screen.ScreenDataStream
import kpt.core.base.store.screen.asScreenStream
import org.mifos.groupbanking.core.data.repository.MeetingRepository
import org.mifos.groupbanking.core.model.MeetingListItem
import org.mifos.groupbanking.core.model.MeetingStatus
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Exercises every declared [MeetingCalendarAction] path plus the
 * [kpt.core.base.store.screen.ScreenState]->[MeetingCalendarState] mapping, per
 * RULE-TDD-METHODOLOGY-001 / RULE-IMPL-DEAD-CLICKABLE-001. Builds a REAL Store5-backed
 * [FakeMeetingRepository] (via the public `Store.asScreenStream(...)` extension + an in-memory
 * `Fetcher`/`SourceOfTruth`) rather than a hand-rolled stub — mirrors `LoanListViewModelTest`.
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
        centerId: Int = CENTER_ID,
        sessionManager: SessionManager = SessionManager(policy = SecurityPolicy()),
    ): Triple<FakeMeetingRepository, SessionManager, MeetingCalendarViewModel> {
        val repository = FakeMeetingRepository(meetings = meetings, failWith = failWith, online = online)
        val viewModel = MeetingCalendarViewModel(
            repository = repository,
            sessionManager = sessionManager,
            crashReporter = ConsoleCrashReporter(),
            analytics = KptAnalyticsTracker(NoOpAnalyticsHelper()),
            centerId = centerId,
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
    fun `initial state is loading, seeds centerId and LIST view mode`() = runTest(testDispatcher) {
        val (_, _, viewModel) = buildViewModel(centerId = CENTER_ID)

        val state = viewModel.stateFlow.value
        assertTrue(state.isLoading)
        assertNull(state.error)
        assertTrue(state.meetings.isEmpty())
        assertEquals(CENTER_ID, state.centerId)
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
            assertEquals(MeetingCalendarEvent.NavigateToReview("MTG-2", 2), awaitItem())
        }
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

private const val CENTER_ID = 1

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
 * In-memory [MeetingRepository] fake. Builds a REAL `Store<Int, List<MeetingListItem>>` per call (an
 * in-memory [Fetcher] + [SourceOfTruth]) and exposes it via the same public `.asScreenStream(...)`
 * extension the production `MeetingRepositoryImpl` uses — so the ViewModel exercises the real
 * offline-first pipeline (Loading -> Content/Empty/Error/NoNetwork/Unauthenticated, refresh, retry).
 */
private class FakeMeetingRepository(
    meetings: List<MeetingListItem> = emptyList(),
    failWith: Throwable? = null,
    online: Boolean = true,
) : MeetingRepository {

    var meetings: List<MeetingListItem> = meetings
    var failWith: Throwable? = failWith

    private val networkMonitor: NetworkMonitor = FakeNetworkMonitor(
        if (online) available() else NetworkStatus.Unavailable,
    )
    private val cache = mutableMapOf<Int, MutableStateFlow<List<MeetingListItem>?>>()

    private fun flowFor(centerId: Int): MutableStateFlow<List<MeetingListItem>?> =
        cache.getOrPut(centerId) { MutableStateFlow(null) }

    override fun meetingsStream(
        centerId: Int,
        scope: CoroutineScope,
        fetchPolicy: FetchPolicy,
    ): ScreenDataStream<List<MeetingListItem>> {
        val store = StoreFactory.createStore<Int, List<MeetingListItem>, List<MeetingListItem>>(
            fetcher = Fetcher.of { _: Int ->
                failWith?.let { throw it }
                meetings
            },
            sourceOfTruth = SourceOfTruth.of(
                reader = { key: Int -> flowFor(key) },
                writer = { key: Int, value: List<MeetingListItem> -> flowFor(key).value = value },
                delete = { key: Int -> flowFor(key).value = null },
                deleteAll = { cache.values.forEach { it.value = null } },
            ),
        )
        return store.asScreenStream(
            key = centerId,
            networkMonitor = networkMonitor,
            fetchedAtRepository = InMemoryFetchedAtRepository(),
            cacheKey = "test:meeting-calendar:$centerId",
            scope = scope,
            isEmpty = { it.isEmpty() },
            fetchPolicy = fetchPolicy,
            ttl = 5.minutes,
        )
    }
}

private fun available(): NetworkStatus.Available =
    NetworkStatus.Available(NetworkInfo(type = NetworkType.WiFi, isMetered = false))

private class FakeNetworkMonitor(initialStatus: NetworkStatus) : NetworkMonitor {
    private val _status = MutableStateFlow(initialStatus)
    override val networkStatus: StateFlow<NetworkStatus> = _status.asStateFlow()
    override val isOnline: StateFlow<Boolean> =
        MutableStateFlow(initialStatus is NetworkStatus.Available).asStateFlow()
    override val networkChanges: SharedFlow<NetworkChangeEvent> =
        MutableSharedFlow<NetworkChangeEvent>().asSharedFlow()
    override fun close() = Unit
}

@OptIn(ExperimentalTime::class)
private class InMemoryFetchedAtRepository : FetchedAtRepository {
    private val map = mutableMapOf<String, Instant>()
    override suspend fun read(storeKey: String): Instant? = map[storeKey]
    override suspend fun write(storeKey: String, instant: Instant) {
        map[storeKey] = instant
    }
}
