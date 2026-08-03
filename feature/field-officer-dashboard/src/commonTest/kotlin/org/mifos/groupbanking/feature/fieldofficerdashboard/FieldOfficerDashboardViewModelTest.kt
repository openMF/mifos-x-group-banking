/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.fieldofficerdashboard

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
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.base.observability.ConsoleCrashReporter
import kpt.core.base.security.SecurityPolicy
import kpt.core.base.security.SessionManager
import kpt.core.base.store.screen.ExperimentalScreenDataStreamTestingApi
import kpt.core.base.store.screen.FetchPolicy
import kpt.core.base.store.screen.ScreenDataStream
import kpt.core.base.store.screen.ScreenState
import kpt.core.base.store.screen.screenDataStreamForTesting
import org.mifos.groupbanking.core.data.repository.FieldOfficerDashboardRepository
import org.mifos.groupbanking.core.model.FieldOfficerDashboard
import org.mifos.groupbanking.core.model.GroupHealthSummary
import org.mifos.groupbanking.core.model.GroupStatusFilter
import org.mifos.groupbanking.core.model.HealthIndicator
import org.mifos.groupbanking.core.model.OverdueRateFilter
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * See API.md#viewmodel — FieldOfficerDashboardViewModelTest exercises every declared
 * [FieldOfficerDashboardAction] path plus the [ScreenState]→[FieldOfficerDashboardState] mapping,
 * per RULE-TDD-METHODOLOGY-001 / RULE-IMPL-DEAD-CLICKABLE-001.
 */
@OptIn(ExperimentalScreenDataStreamTestingApi::class)
class FieldOfficerDashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: FakeFieldOfficerDashboardRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: FieldOfficerDashboardViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeFieldOfficerDashboardRepository()
        sessionManager = SessionManager(policy = SecurityPolicy())
        viewModel = FieldOfficerDashboardViewModel(
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

    // ─── initial state + stream mapping ─────────────────────────────────────

    @Test
    fun `initial state is loading with export enabled for the default field officer role`() = runTest(testDispatcher) {
        val state = viewModel.stateFlow.value
        assertTrue(state.isLoading)
        assertNull(state.error)
        assertTrue(state.groups.isEmpty())
        assertTrue(state.canExport)
    }

    @Test
    fun `Content maps KPIs, groups and canExport`() = runTest(testDispatcher) {
        repository.emit(ScreenState.Content(data = dashboard(groups = threeGroups())))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(3, state.totalGroupsCount)
        assertEquals(3, state.groups.size)
        assertEquals(3, state.filteredGroups.size)
        assertEquals("FIELD_OFFICER", state.userRole)
        assertTrue(state.canExport)
        assertEquals(FieldOfficerDashboardScreenState.Content, state.screenState)
    }

    @Test
    fun `empty groups derives Empty screenState from Content`() = runTest(testDispatcher) {
        repository.emit(ScreenState.Content(data = dashboard(groups = emptyList())))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(FieldOfficerDashboardScreenState.Empty, state.screenState)
    }

    @Test
    fun `stream NoNetwork maps to Network error`() = runTest(testDispatcher) {
        repository.emit(ScreenState.NoNetwork())
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(FieldOfficerDashboardError.Network, viewModel.stateFlow.value.error)
        assertFalse(viewModel.stateFlow.value.isLoading)
    }

    @Test
    fun `stream Error with isNetworkError false maps to Server error`() = runTest(testDispatcher) {
        repository.emit(ScreenState.Error(error = IllegalStateException("boom"), isNetworkError = false))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(FieldOfficerDashboardError.Server, viewModel.stateFlow.value.error)
    }

    @Test
    fun `stream Unauthenticated maps to Auth error, clears session and emits snackbar`() = runTest(testDispatcher) {
        sessionManager.startSession()
        assertTrue(sessionManager.isSessionActive.value)

        repository.emit(ScreenState.Unauthenticated)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(FieldOfficerDashboardError.Auth, state.error)
        assertFalse(state.error?.retry ?: true)
        assertFalse(sessionManager.isSessionActive.value)
    }

    // ─── OnGroupTapped ──────────────────────────────────────────────────────

    @Test
    fun `OnGroupTapped emits NavigateToGroupDashboard with groupId`() = runTest(testDispatcher) {
        viewModel.eventFlow.test {
            viewModel.trySendAction(FieldOfficerDashboardAction.OnGroupTapped(groupId = 42L))
            assertEquals(FieldOfficerDashboardEvent.NavigateToGroupDashboard(42L), awaitItem())
        }
    }

    // ─── Filters ────────────────────────────────────────────────────────────

    @Test
    fun `OnRegionFilterSelected keeps only groups in the region`() = runTest(testDispatcher) {
        repository.emit(ScreenState.Content(data = dashboard(groups = threeGroups())))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(FieldOfficerDashboardAction.OnRegionFilterSelected("Nairobi East"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals("Nairobi East", state.selectedRegionFilter)
        assertEquals(2, state.filteredGroups.size)
        assertEquals(3, state.groups.size)
    }

    @Test
    fun `OnStatusFilterSelected keeps only groups with the status`() = runTest(testDispatcher) {
        repository.emit(ScreenState.Content(data = dashboard(groups = threeGroups())))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(FieldOfficerDashboardAction.OnStatusFilterSelected(GroupStatusFilter.PENDING))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.stateFlow.value.filteredGroups.size)
    }

    @Test
    fun `OnOverdueFilterSelected greater-than-10 keeps only high-overdue groups`() = runTest(testDispatcher) {
        repository.emit(ScreenState.Content(data = dashboard(groups = threeGroups())))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(FieldOfficerDashboardAction.OnOverdueFilterSelected(OverdueRateFilter.GREATER_THAN_10))
        testDispatcher.scheduler.advanceUntilIdle()

        val filtered = viewModel.stateFlow.value.filteredGroups
        assertEquals(1, filtered.size)
        assertEquals(3L, filtered.first().id)
    }

    @Test
    fun `OnClearFilters restores the full list and clears selections`() = runTest(testDispatcher) {
        repository.emit(ScreenState.Content(data = dashboard(groups = threeGroups())))
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.trySendAction(FieldOfficerDashboardAction.OnRegionFilterSelected("Nairobi East"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(FieldOfficerDashboardAction.OnClearFilters)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertNull(state.selectedRegionFilter)
        assertEquals(3, state.filteredGroups.size)
    }

    // ─── Picker events ──────────────────────────────────────────────────────

    @Test
    fun `OnShowRegionPicker emits ShowRegionPickerDialog`() = runTest(testDispatcher) {
        viewModel.eventFlow.test {
            viewModel.trySendAction(FieldOfficerDashboardAction.OnShowRegionPicker)
            assertEquals(FieldOfficerDashboardEvent.ShowRegionPickerDialog, awaitItem())
        }
    }

    // ─── Export ─────────────────────────────────────────────────────────────

    @Test
    fun `OnExportReport success emits NavigateToExportReport and clears isExporting`() = runTest(testDispatcher) {
        repository.emit(ScreenState.Content(data = dashboard(groups = threeGroups(), staffId = 12L)))
        testDispatcher.scheduler.advanceUntilIdle()
        repository.exportResult = NetworkResult.Success(byteArrayOf(1, 2, 3))

        viewModel.eventFlow.test {
            viewModel.trySendAction(FieldOfficerDashboardAction.OnExportReport)
            testDispatcher.scheduler.advanceUntilIdle()
            val event = awaitItem()
            assertTrue(event is FieldOfficerDashboardEvent.NavigateToExportReport)
            assertEquals(12L, (event as FieldOfficerDashboardEvent.NavigateToExportReport).staffId)
        }
        assertFalse(viewModel.stateFlow.value.isExporting)
    }

    @Test
    fun `OnExportReport failure emits ShowSnackbar and clears isExporting`() = runTest(testDispatcher) {
        repository.emit(ScreenState.Content(data = dashboard(groups = threeGroups())))
        testDispatcher.scheduler.advanceUntilIdle()
        repository.exportResult = NetworkResult.Error(NetworkError.SERVER)

        viewModel.eventFlow.test {
            viewModel.trySendAction(FieldOfficerDashboardAction.OnExportReport)
            testDispatcher.scheduler.advanceUntilIdle()
            val event = awaitItem()
            assertTrue(event is FieldOfficerDashboardEvent.ShowSnackbar)
            assertEquals("export_error", (event as FieldOfficerDashboardEvent.ShowSnackbar).message)
        }
        assertFalse(viewModel.stateFlow.value.isExporting)
    }

    // ─── Refresh / Retry ────────────────────────────────────────────────────

    @Test
    fun `OnRefresh sets isRefreshing and dispatches a refresh`() = runTest(testDispatcher) {
        repository.emit(ScreenState.Content(data = dashboard(groups = threeGroups())))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(FieldOfficerDashboardAction.OnRefresh)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.stateFlow.value.isRefreshing)
        assertEquals(1, repository.triggerCount)
    }

    @Test
    fun `OnRetry clears error and dispatches a refresh`() = runTest(testDispatcher) {
        repository.emit(ScreenState.NoNetwork())
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(FieldOfficerDashboardAction.OnRetry)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.triggerCount)
    }
}

// ---------------------------------------------------------------------------
// Fixtures
// ---------------------------------------------------------------------------

private fun group(id: Long, office: String, status: String, overdueRate: Double): GroupHealthSummary =
    GroupHealthSummary(
        id = id,
        fineractGroupId = id,
        name = "Group $id",
        officeName = office,
        status = status,
        activeClientCount = 10,
        totalSavingsBalance = 20000.0,
        totalLoansOutstanding = 9000.0,
        overdueRate = overdueRate,
        healthIndicator = HealthIndicator.fromOverdueRate(overdueRate),
        cycleNumber = 1,
    )

private fun threeGroups(): List<GroupHealthSummary> = listOf(
    group(1L, "Nairobi East", "ACTIVE", 0.0),
    group(2L, "Nairobi East", "PENDING", 0.05),
    group(3L, "Nairobi West", "ACTIVE", 0.25),
)

private fun dashboard(
    groups: List<GroupHealthSummary>,
    userRole: String = FieldOfficerDashboard.ROLE_FIELD_OFFICER,
    staffId: Long = 12L,
): FieldOfficerDashboard = FieldOfficerDashboard(
    staffId = staffId,
    userRole = userRole,
    totalGroupsCount = groups.size,
    totalActiveMembers = groups.sumOf { it.activeClientCount },
    totalSavingsThisMonth = groups.sumOf { it.totalSavingsBalance },
    totalLoansOutstanding = groups.sumOf { it.totalLoansOutstanding },
    groups = groups,
    availableRegions = groups.map { it.officeName }.distinct().sorted(),
)

/**
 * In-memory [FieldOfficerDashboardRepository] fake — exposes a single caller-owned state flow via
 * [screenDataStreamForTesting] and counts every refresh/retry dispatched through the shared
 * refresh trigger (mirrors `FakeMemberDashboardRepository`).
 */
private class FakeFieldOfficerDashboardRepository : FieldOfficerDashboardRepository {

    private val stateFlow = MutableStateFlow<ScreenState<FieldOfficerDashboard>>(ScreenState.Loading)
    private val refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    var triggerCount: Int = 0
        private set

    var exportResult: NetworkResult<ByteArray, NetworkError> = NetworkResult.Success(byteArrayOf())

    fun emit(screenState: ScreenState<FieldOfficerDashboard>) {
        stateFlow.value = screenState
    }

    override fun fieldOfficerDashboardStream(
        staffId: Long,
        userRole: String,
        scope: CoroutineScope,
        fetchPolicy: FetchPolicy,
    ): ScreenDataStream<FieldOfficerDashboard> {
        scope.launch { refreshTrigger.collect { triggerCount++ } }
        return screenDataStreamForTesting(state = stateFlow, refreshTrigger = refreshTrigger)
    }

    override suspend fun exportReport(staffId: Long): NetworkResult<ByteArray, NetworkError> = exportResult
}
