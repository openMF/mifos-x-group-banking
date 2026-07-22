/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.loandetail

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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
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
import org.mifos.groupbanking.core.data.repository.LoanDetailRepository
import org.mifos.groupbanking.core.model.LoanAccountStatus
import org.mifos.groupbanking.core.model.LoanDetail
import org.mifos.groupbanking.core.model.LoanDetailResponse
import org.mifos.groupbanking.core.model.LoanDetailTab
import org.mifos.groupbanking.core.model.RepaymentRowStatus
import org.mifos.groupbanking.core.model.RepaymentScheduleRow
import org.mifos.groupbanking.core.model.RepaymentTransaction

/**
 * See API.md#viewmodel — `LoanDetailViewModelTest` exercises the [ScreenState] ->
 * [LoanDetailState] mapping, the tab-switch pure transform, the dialog-opening
 * `OnRecordRepayment`/`OnMarkDefaulted` handlers, `OnBack`/`OnRefresh`/`Retry`, and the
 * `Unauthenticated`/`NoNetwork`/`Error` error-taxonomy branches, per RULE-TDD-METHODOLOGY-001 /
 * RULE-IMPL-DEAD-CLICKABLE-001.
 */
@OptIn(ExperimentalScreenDataStreamTestingApi::class)
class LoanDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: FakeLoanDetailRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: LoanDetailViewModel

    private fun createViewModel(loanId: Long = LOAN_ID) {
        viewModel = LoanDetailViewModel(
            repository = repository,
            sessionManager = sessionManager,
            crashReporter = ConsoleCrashReporter(),
            analytics = KptAnalyticsTracker(NoOpAnalyticsHelper()),
            loanId = loanId,
        )
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeLoanDetailRepository()
        sessionManager = SessionManager(policy = SecurityPolicy())
        createViewModel()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading with SCHEDULE tab selected and no permission gates open`() =
        runTest(testDispatcher) {
            val state = viewModel.stateFlow.value
            assertTrue(state.isLoading)
            assertEquals(LoanDetailTab.SCHEDULE, state.selectedTab)
            assertNull(state.loan)
            assertNull(state.error)
            assertFalse(state.canRecordRepayment)
            assertFalse(state.canMarkDefaulted)
            assertFalse(state.isRecordingRepayment)
        }

    @Test
    fun `init requests the stream for the nav-arg loanId`() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf(LOAN_ID), repository.requestedLoanIds)
    }

    @Test
    fun `Content maps loan header, schedule, and history and clears loading`() = runTest(testDispatcher) {
        repository.emit(ScreenState.Content(data = activeLoanDetail()))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertFalse(state.isLoading)
        assertEquals("Peter Otieno", state.loan?.memberName)
        assertEquals(LoanAccountStatus.ACTIVE, state.loan?.status)
        assertEquals(4, state.repaymentSchedule.size)
        assertEquals(1, state.repaymentHistory.size)
        assertNull(state.error)
    }

    @Test
    fun `Content never opens canRecordRepayment or canMarkDefaulted (documented no-role-source gap)`() =
        runTest(testDispatcher) {
            repository.emit(ScreenState.Content(data = activeLoanDetail()))
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.stateFlow.value
            assertFalse(state.canRecordRepayment)
            assertFalse(state.canMarkDefaulted)
        }

    @Test
    fun `OnTabChange switches selectedTab to HISTORY and back to SCHEDULE`() = runTest(testDispatcher) {
        viewModel.trySendAction(LoanDetailAction.OnTabChange(LoanDetailTab.HISTORY))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(LoanDetailTab.HISTORY, viewModel.stateFlow.value.selectedTab)

        viewModel.trySendAction(LoanDetailAction.OnTabChange(LoanDetailTab.SCHEDULE))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(LoanDetailTab.SCHEDULE, viewModel.stateFlow.value.selectedTab)
    }

    @Test
    fun `OnRecordRepayment emits ShowRepaymentDialog`() = runTest(testDispatcher) {
        viewModel.eventFlow.test {
            viewModel.trySendAction(LoanDetailAction.OnRecordRepayment)
            assertEquals(LoanDetailEvent.ShowRepaymentDialog, awaitItem())
        }
    }

    @Test
    fun `OnMarkDefaulted emits ShowDefaultConfirmDialog`() = runTest(testDispatcher) {
        viewModel.eventFlow.test {
            viewModel.trySendAction(LoanDetailAction.OnMarkDefaulted)
            assertEquals(LoanDetailEvent.ShowDefaultConfirmDialog, awaitItem())
        }
    }

    @Test
    fun `OnBack emits NavigateBack`() = runTest(testDispatcher) {
        viewModel.eventFlow.test {
            viewModel.trySendAction(LoanDetailAction.OnBack)
            assertEquals(LoanDetailEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `OnRefresh sets isLoading and dispatches a bypass-and-refresh fetch`() = runTest(testDispatcher) {
        repository.emit(ScreenState.Content(data = activeLoanDetail()))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(LoanDetailAction.OnRefresh)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.stateFlow.value.isLoading)
        assertEquals(1, repository.refreshTriggerCount)
    }

    @Test
    fun `Retry clears the error and re-dispatches the stream fetch`() = runTest(testDispatcher) {
        repository.emit(ScreenState.NoNetwork())
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(LoanDetailAction.Retry)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.stateFlow.value.error)
        assertEquals(1, repository.refreshTriggerCount)
    }

    @Test
    fun `stream NoNetwork maps to Network error`() = runTest(testDispatcher) {
        repository.emit(ScreenState.NoNetwork())
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(LoanDetailError.Network, state.error)
        assertTrue(state.error?.retry ?: false)
        assertFalse(state.isLoading)
    }

    @Test
    fun `stream generic Error maps to Server error`() = runTest(testDispatcher) {
        repository.emit(ScreenState.Error(error = IllegalStateException("boom"), isNetworkError = false))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(LoanDetailError.Server, viewModel.stateFlow.value.error)
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
                    LoanDetailEvent.ShowSnackbar(message = LoanDetailError.Auth.messageKey),
                    awaitItem(),
                )
            }

            val state = viewModel.stateFlow.value
            assertEquals(LoanDetailError.Auth, state.error)
            assertFalse(state.error?.retry ?: true)
            assertFalse(sessionManager.isSessionActive.value)
        }

    private companion object {
        const val LOAN_ID = 4200L
    }
}

private fun activeLoanDetail(): LoanDetailResponse = LoanDetailResponse(
    loan = LoanDetail(
        id = 4200L,
        memberId = 501L,
        memberName = "Peter Otieno",
        loanProductName = "Standard Group Loan",
        principalAmount = 1500.0,
        disbursedDate = "2026-04-14",
        interestRatePercent = 5.0,
        totalOutstanding = 1125.0,
        totalOverdue = 0.0,
        status = LoanAccountStatus.ACTIVE,
        fineractLoanId = 9001L,
    ),
    repaymentSchedule = listOf(
        RepaymentScheduleRow(1, "2026-04-21", 125.0, 125.0, 0.0, RepaymentRowStatus.PAID),
        RepaymentScheduleRow(2, "2026-04-28", 125.0, 125.0, 0.0, RepaymentRowStatus.PAID),
        RepaymentScheduleRow(3, "2026-05-05", 125.0, 125.0, 0.0, RepaymentRowStatus.PAID),
        RepaymentScheduleRow(4, "2026-05-12", 125.0, 0.0, 125.0, RepaymentRowStatus.UPCOMING),
    ),
    repaymentHistory = listOf(
        RepaymentTransaction(id = 1L, type = "REPAYMENT", date = "2026-04-21", amount = 125.0),
    ),
)

/**
 * In-memory [LoanDetailRepository] fake — `loanDetailStream` is called exactly once per
 * [LoanDetailViewModel] instance (fixed `loanId` constructor nav-arg, not a dynamic-key
 * re-fetchable stream), so a single buffered [MutableStateFlow] + a single shared `refreshTrigger`
 * is sufficient — mirrors `FakeGroupDashboardRepository`'s identical single-key convention.
 */
private class FakeLoanDetailRepository : LoanDetailRepository {

    val requestedLoanIds = mutableListOf<Long>()
    var refreshTriggerCount = 0

    private val stateFlow = MutableStateFlow<ScreenState<LoanDetailResponse>>(ScreenState.Loading)
    private val refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    fun emit(screenState: ScreenState<LoanDetailResponse>) {
        stateFlow.value = screenState
    }

    override fun loanDetailStream(
        loanId: Long,
        scope: CoroutineScope,
        fetchPolicy: FetchPolicy,
    ): ScreenDataStream<LoanDetailResponse> {
        requestedLoanIds += loanId
        scope.launch {
            refreshTrigger.collect { refreshTriggerCount++ }
        }
        return screenDataStreamForTesting(
            state = stateFlow,
            refreshTrigger = refreshTrigger,
        )
    }
}
