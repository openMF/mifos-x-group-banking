/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.loanrequest

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
import kpt.core.analytics.KptAnalyticsTracker
import kpt.core.base.analytics.NoOpAnalyticsHelper
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.base.observability.ConsoleCrashReporter
import kpt.core.data.repository.LoanRequestRepository
import kpt.core.model.LoanPurpose
import kpt.core.model.LoanRequestPayload
import kpt.core.model.LoanRequestResult
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * See API.md#viewmodel — LoanRequestViewModelTest exercises every declared [LoanRequestAction]
 * path (amount/purpose/duration transforms + their local validation and derivation, submit
 * online-success / offline-enqueue / transport-retry-enqueue / unauthorized, retry, and success
 * dialog dismiss), per RULE-TDD-METHODOLOGY-001 / RULE-IMPL-DEAD-CLICKABLE-001.
 */
class LoanRequestViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: FakeLoanRequestRepository
    private lateinit var networkMonitor: FakeNetworkMonitor

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeLoanRequestRepository()
        networkMonitor = FakeNetworkMonitor(initiallyOnline = true)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(
        clientId: Long = 10L,
        savingsBalance: Double = 5000.0,
        loanMultiplier: Double = 3.0,
    ): LoanRequestViewModel {
        // The VM re-resolves the real savings balance at mount via repository.memberSavingsBalance
        // (the nav-param is a seed, not authoritative). Seed the fake to the SAME value so the
        // resolve confirms — not overrides — the nav-param and maxLoanAmount stays consistent.
        repository.memberSavingsBalanceResult = NetworkResult.Success(savingsBalance)
        return LoanRequestViewModel(
            repository = repository,
            networkMonitor = networkMonitor,
            analytics = KptAnalyticsTracker(NoOpAnalyticsHelper()),
            crashReporter = ConsoleCrashReporter(),
            clientId = clientId,
            savingsBalance = savingsBalance,
            loanMultiplier = loanMultiplier,
        )
    }

    private fun fillValidForm(viewModel: LoanRequestViewModel) {
        viewModel.trySendAction(LoanRequestAction.OnAmountChange("3000"))
        viewModel.trySendAction(LoanRequestAction.OnPurposeSelected(LoanPurpose.BUSINESS))
    }

    // -- Initial state / nav-arg seeding ---------------------------------------------------------

    @Test
    fun `initial state seeds nav-args and computes maxLoanAmount from savingsBalance times loanMultiplier`() =
        runTest(testDispatcher) {
            val viewModel = buildViewModel(clientId = 10L, savingsBalance = 5000.0, loanMultiplier = 3.0)
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.stateFlow.value
            assertEquals(10L, state.clientId)
            assertEquals(5000.0, state.savingsBalance)
            assertEquals(3.0, state.loanMultiplier)
            assertEquals(15000.0, state.maxLoanAmount)
            assertEquals(12, state.durationWeeks)
            assertTrue(!state.isFormValid)
            assertEquals(LoanRequestScreenState.Content, state.deriveScreenState())
        }

    @Test
    fun `going offline after init flips isOfflineMode via the reactive connectivity collector`() =
        runTest(testDispatcher) {
            val viewModel = buildViewModel()
            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(!viewModel.stateFlow.value.isOfflineMode)

            networkMonitor.setOnline(false)
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(viewModel.stateFlow.value.isOfflineMode)
        }

    // -- OnAmountChange validation + derivation --------------------------------------------------

    @Test
    fun `OnAmountChange with a non-numeric value sets requestedAmountError and keeps isFormValid false`() =
        runTest(testDispatcher) {
            val viewModel = buildViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.trySendAction(LoanRequestAction.OnAmountChange("abc"))
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.stateFlow.value
            assertEquals("error_validation", state.requestedAmountError)
            assertTrue(!state.isFormValid)
        }

    @Test
    fun `OnAmountChange below the KES 500 floor sets requestedAmountError`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(LoanRequestAction.OnAmountChange("100"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("error_validation", viewModel.stateFlow.value.requestedAmountError)
    }

    @Test
    fun `OnAmountChange above maxLoanAmount sets requestedAmountError`() = runTest(testDispatcher) {
        val viewModel = buildViewModel(savingsBalance = 5000.0, loanMultiplier = 3.0) // max = 15000
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(LoanRequestAction.OnAmountChange("20000"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("error_validation", viewModel.stateFlow.value.requestedAmountError)
    }

    @Test
    fun `OnAmountChange with a valid amount clears the error and recomputes repaymentEstimate`() =
        runTest(testDispatcher) {
            val viewModel = buildViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.trySendAction(LoanRequestAction.OnAmountChange("3000"))
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.stateFlow.value
            assertNull(state.requestedAmountError)
            // repaymentEstimate = 3000 * 1.10 / 12 weeks = 275.0 (documented GROUP_INTEREST_RATE assumption)
            assertEquals(275.0, state.repaymentEstimate, 0.001)
        }

    // -- OnPurposeSelected --------------------------------------------------------------------------

    @Test
    fun `OnPurposeSelected sets purpose and combined with a valid amount flips isFormValid to true`() =
        runTest(testDispatcher) {
            val viewModel = buildViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.trySendAction(LoanRequestAction.OnAmountChange("3000"))
            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(!viewModel.stateFlow.value.isFormValid)

            viewModel.trySendAction(LoanRequestAction.OnPurposeSelected(LoanPurpose.SCHOOL_FEES))
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.stateFlow.value
            assertEquals(LoanPurpose.SCHOOL_FEES, state.purpose)
            assertNull(state.purposeError)
            assertTrue(state.isFormValid)
        }

    // -- OnDurationChanged ----------------------------------------------------------------------------

    @Test
    fun `OnDurationChanged updates durationWeeks and recomputes repaymentEstimate`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.trySendAction(LoanRequestAction.OnAmountChange("3000"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(LoanRequestAction.OnDurationChanged(6))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(6, state.durationWeeks)
        // 3000 * 1.10 / 6 weeks = 550.0
        assertEquals(550.0, state.repaymentEstimate, 0.001)
    }

    // -- OnSubmitClick validation-block ------------------------------------------------------------

    @Test
    fun `OnSubmitClick with a blank form sets requestedAmountError and purposeError and does not call submit`() =
        runTest(testDispatcher) {
            val viewModel = buildViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.trySendAction(LoanRequestAction.OnSubmitClick)
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.stateFlow.value
            assertEquals("error_validation", state.requestedAmountError)
            assertEquals("error_validation", state.purposeError)
            assertEquals(0, repository.submitCallCount)
            assertEquals(0, repository.enqueueCallCount)
        }

    // -- OnSubmitClick success (online) --------------------------------------------------------------

    @Test
    fun `OnSubmitClick success while online transitions to SubmitSuccess and shows the success dialog`() =
        runTest(testDispatcher) {
            repository.submitResult = NetworkResult.Success(
                LoanRequestResult(resourceId = 501L, officeId = 1L, clientId = 10L, resourceExternalId = "ext-501"),
            )
            val viewModel = buildViewModel(clientId = 10L)
            testDispatcher.scheduler.advanceUntilIdle()
            fillValidForm(viewModel)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.trySendAction(LoanRequestAction.OnSubmitClick)
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.stateFlow.value
            assertEquals(false, state.isSubmitting)
            assertNull(state.submitError)
            assertTrue(state.successDialogVisible)
            assertEquals(LoanRequestScreenState.SubmitSuccess, state.deriveScreenState())
            assertEquals(1, repository.submitCallCount)
            assertEquals(0, repository.enqueueCallCount)
            assertEquals(10L, repository.lastSubmitPayload?.clientId)
            assertEquals(LoanPurpose.BUSINESS, repository.lastSubmitPayload?.purpose)
        }

    // -- OnSubmitClick while offline ------------------------------------------------------------------

    @Test
    fun `OnSubmitClick while offline enqueues instead of calling submit and emits ShowOfflineQueuedConfirmation`() =
        runTest(testDispatcher) {
            networkMonitor.setOnline(false)
            val viewModel = buildViewModel()
            testDispatcher.scheduler.advanceUntilIdle()
            fillValidForm(viewModel)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.eventFlow.test {
                viewModel.trySendAction(LoanRequestAction.OnSubmitClick)
                assertEquals(LoanRequestEvent.ShowOfflineQueuedConfirmation, awaitItem())
            }

            val state = viewModel.stateFlow.value
            assertTrue(state.isOfflineMode)
            assertTrue(state.successDialogVisible)
            assertEquals(LoanRequestScreenState.OfflineQueued, state.deriveScreenState())
            assertEquals(0, repository.submitCallCount)
            assertEquals(1, repository.enqueueCallCount)
        }

    // -- OnSubmitClick transport error (retry-enqueue) -------------------------------------------------

    @Test
    fun `OnSubmitClick with a SERVER transport error retry-enqueues rather than surfacing an inline error`() =
        runTest(testDispatcher) {
            repository.submitResult = NetworkResult.Error(NetworkError.SERVER)
            val viewModel = buildViewModel()
            testDispatcher.scheduler.advanceUntilIdle()
            fillValidForm(viewModel)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.trySendAction(LoanRequestAction.OnSubmitClick)
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.stateFlow.value
            assertNull(state.submitError)
            assertEquals(LoanRequestScreenState.OfflineQueued, state.deriveScreenState())
            assertEquals(1, repository.submitCallCount)
            assertEquals(1, repository.enqueueCallCount)
        }

    @Test
    fun `OnSubmitClick with a REQUEST_TIMEOUT transport error also retry-enqueues`() = runTest(testDispatcher) {
        repository.submitResult = NetworkResult.Error(NetworkError.REQUEST_TIMEOUT)
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        fillValidForm(viewModel)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(LoanRequestAction.OnSubmitClick)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.enqueueCallCount)
        assertEquals(LoanRequestScreenState.OfflineQueued, viewModel.stateFlow.value.deriveScreenState())
    }

    // -- OnSubmitClick UNAUTHORIZED ---------------------------------------------------------------------

    @Test
    fun `OnSubmitClick with UNAUTHORIZED sets SubmitError Unauthorized without enqueueing`() = runTest(testDispatcher) {
        repository.submitResult = NetworkResult.Error(NetworkError.UNAUTHORIZED)
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        fillValidForm(viewModel)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(LoanRequestAction.OnSubmitClick)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(SubmitError.Unauthorized, state.submitError)
        assertEquals(LoanRequestScreenState.SubmitError, state.deriveScreenState())
        assertEquals(0, repository.enqueueCallCount)
    }

    // -- OnRetry -------------------------------------------------------------------------------------------

    @Test
    fun `OnRetry re-submits the same payload and can transition to success after a prior failure`() =
        runTest(testDispatcher) {
            repository.submitResult = NetworkResult.Error(NetworkError.BAD_REQUEST)
            val viewModel = buildViewModel()
            testDispatcher.scheduler.advanceUntilIdle()
            fillValidForm(viewModel)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.trySendAction(LoanRequestAction.OnSubmitClick)
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(SubmitError.Validation, viewModel.stateFlow.value.submitError)
            assertEquals(1, repository.submitCallCount)

            repository.submitResult = NetworkResult.Success(
                LoanRequestResult(resourceId = 9L, officeId = 1L, clientId = 10L, resourceExternalId = "ext-9"),
            )
            viewModel.trySendAction(LoanRequestAction.OnRetry)
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.stateFlow.value
            assertEquals(2, repository.submitCallCount)
            assertNull(state.submitError)
            assertTrue(state.successDialogVisible)
            assertEquals(LoanRequestScreenState.SubmitSuccess, state.deriveScreenState())
        }

    // -- OnSuccessDialogDismiss ---------------------------------------------------------------------------

    @Test
    fun `OnSuccessDialogDismiss clears successDialogVisible and emits NavigateToDashboardAfterSuccess`() =
        runTest(testDispatcher) {
            repository.submitResult = NetworkResult.Success(
                LoanRequestResult(resourceId = 1L, officeId = 1L, clientId = 10L, resourceExternalId = "ext-1"),
            )
            val viewModel = buildViewModel()
            testDispatcher.scheduler.advanceUntilIdle()
            fillValidForm(viewModel)
            testDispatcher.scheduler.advanceUntilIdle()
            viewModel.trySendAction(LoanRequestAction.OnSubmitClick)
            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(viewModel.stateFlow.value.successDialogVisible)

            viewModel.eventFlow.test {
                viewModel.trySendAction(LoanRequestAction.OnSuccessDialogDismiss)
                assertEquals(LoanRequestEvent.NavigateToDashboardAfterSuccess, awaitItem())
            }

            assertTrue(!viewModel.stateFlow.value.successDialogVisible)
        }
}

private class FakeLoanRequestRepository : LoanRequestRepository {
    var submitResult: NetworkResult<LoanRequestResult, NetworkError> = NetworkResult.Success(
        LoanRequestResult(resourceId = 1L, officeId = 1L, clientId = 1L, resourceExternalId = "ext-1"),
    )

    var submitCallCount: Int = 0
        private set
    var lastSubmitPayload: LoanRequestPayload? = null
        private set

    var enqueueCallCount: Int = 0
        private set
    var lastEnqueuePayload: LoanRequestPayload? = null
        private set
    var enqueueReturnId: Long = 42L

    override suspend fun submit(payload: LoanRequestPayload): NetworkResult<LoanRequestResult, NetworkError> {
        submitCallCount++
        lastSubmitPayload = payload
        return submitResult
    }

    override suspend fun enqueueOffline(payload: LoanRequestPayload): Long {
        enqueueCallCount++
        lastEnqueuePayload = payload
        return enqueueReturnId
    }

    var memberSavingsBalanceResult: NetworkResult<Double, NetworkError> = NetworkResult.Success(0.0)

    override suspend fun memberSavingsBalance(clientId: Long): NetworkResult<Double, NetworkError> =
        memberSavingsBalanceResult
}

private class FakeNetworkMonitor(initiallyOnline: Boolean) : NetworkMonitor {
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
