/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.loanrepaymentdialog

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
import org.mifos.groupbanking.core.data.repository.LoanRepaymentRepository
import org.mifos.groupbanking.core.model.PaymentMethod
import org.mifos.groupbanking.core.model.RecordRepaymentRequest
import org.mifos.groupbanking.core.model.RepaymentResult
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * See API.md#viewmodel — LoanRepaymentDialogViewModelTest exercises every declared
 * [LoanRepaymentDialogAction] path (nav-arg pre-fill, field edits + validation-error clearing,
 * payment-method chip selection, submit validation-block/offline-block/success/transport-error,
 * and dismiss), per RULE-TDD-METHODOLOGY-001 / RULE-IMPL-DEAD-CLICKABLE-001. Written FIRST,
 * ahead of [LoanRepaymentDialogViewModel] itself, per RULE-TDD-GLOBAL-001.
 */
class LoanRepaymentDialogViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: FakeLoanRepaymentRepository
    private lateinit var networkMonitor: FakeNetworkMonitor

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeLoanRepaymentRepository()
        networkMonitor = FakeNetworkMonitor(initiallyOnline = true)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(
        loanId: Long = 42L,
        memberId: Long = 7L,
        installmentAmount: Double = 125.0,
    ): LoanRepaymentDialogViewModel = LoanRepaymentDialogViewModel(
        repository = repository,
        networkMonitor = networkMonitor,
        analytics = KptAnalyticsTracker(NoOpAnalyticsHelper()),
        crashReporter = ConsoleCrashReporter(),
        loanId = loanId,
        memberId = memberId,
        installmentAmount = installmentAmount,
    )

    // -- Initial state / nav-arg seeding ---------------------------------------------------------

    @Test
    fun `initial state is pre-filled from the installmentAmount nav-arg and defaults to MPESA`() = runTest(testDispatcher) {
        val viewModel = buildViewModel(installmentAmount = 125.5)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals("125.50", state.amount)
        assertEquals(PaymentMethod.MPESA, state.paymentMethod)
        assertEquals("", state.referenceNumber)
        assertEquals(false, state.isSubmitting)
        assertNull(state.amountError)
        assertNull(state.submitError)
    }

    // -- Field-change actions ---------------------------------------------------------------------

    @Test
    fun `OnAmountChanged updates amount and clears amountError`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.trySendAction(LoanRepaymentDialogAction.OnAmountChanged(""))
        viewModel.trySendAction(LoanRepaymentDialogAction.OnSubmit) // trigger amountError (blank)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("error_amount_required", viewModel.stateFlow.value.amountError)

        viewModel.trySendAction(LoanRepaymentDialogAction.OnAmountChanged("200.00"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals("200.00", state.amount)
        assertNull(state.amountError)
    }

    @Test
    fun `OnPaymentMethodSelected switches between MPESA and CASH`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(LoanRepaymentDialogAction.OnPaymentMethodSelected(PaymentMethod.CASH))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(PaymentMethod.CASH, viewModel.stateFlow.value.paymentMethod)

        viewModel.trySendAction(LoanRepaymentDialogAction.OnPaymentMethodSelected(PaymentMethod.MPESA))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(PaymentMethod.MPESA, viewModel.stateFlow.value.paymentMethod)
    }

    @Test
    fun `OnReferenceNumberChanged updates referenceNumber`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(LoanRepaymentDialogAction.OnReferenceNumberChanged("QJZ7X9A1BK"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("QJZ7X9A1BK", viewModel.stateFlow.value.referenceNumber)
    }

    // -- Dismiss ------------------------------------------------------------------------------------

    @Test
    fun `OnDismiss always emits Dismiss`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(LoanRepaymentDialogAction.OnDismiss)
            assertEquals(LoanRepaymentDialogEvent.Dismiss, awaitItem())
        }
    }

    // -- Submit validation-block ---------------------------------------------------------------------

    @Test
    fun `OnSubmit with a blank amount sets amountError and does not call the repository`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.trySendAction(LoanRepaymentDialogAction.OnAmountChanged(""))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(LoanRepaymentDialogAction.OnSubmit)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("error_amount_required", viewModel.stateFlow.value.amountError)
        assertEquals(0, repository.recordRepaymentCallCount)
    }

    @Test
    fun `OnSubmit with a non-positive amount sets amountError to invalid and does not call the repository`() =
        runTest(testDispatcher) {
            val viewModel = buildViewModel()
            testDispatcher.scheduler.advanceUntilIdle()
            viewModel.trySendAction(LoanRepaymentDialogAction.OnAmountChanged("0"))
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.trySendAction(LoanRepaymentDialogAction.OnSubmit)
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals("error_amount_invalid", viewModel.stateFlow.value.amountError)
            assertEquals(0, repository.recordRepaymentCallCount)
        }

    @Test
    fun `OnSubmit with a non-numeric amount sets amountError to invalid`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.trySendAction(LoanRepaymentDialogAction.OnAmountChanged("abc"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(LoanRepaymentDialogAction.OnSubmit)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("error_amount_invalid", viewModel.stateFlow.value.amountError)
        assertEquals(0, repository.recordRepaymentCallCount)
    }

    // -- Submit while offline --------------------------------------------------------------------------

    @Test
    fun `OnSubmit while offline sets submitError and shows an error, without calling the repository`() =
        runTest(testDispatcher) {
            networkMonitor.setOnline(false)
            val viewModel = buildViewModel(installmentAmount = 125.0)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.eventFlow.test {
                viewModel.trySendAction(LoanRepaymentDialogAction.OnSubmit)
                assertEquals(LoanRepaymentDialogEvent.ShowError(message = "error_offline_no_queue"), awaitItem())
            }

            val state = viewModel.stateFlow.value
            assertEquals("error_offline_no_queue", state.submitError)
            assertEquals(false, state.isSubmitting)
            assertEquals(0, repository.recordRepaymentCallCount)
        }

    // -- Submit success ------------------------------------------------------------------------------------

    @Test
    fun `OnSubmit success emits RepaymentRecorded then Dismiss and resets isSubmitting`() = runTest(testDispatcher) {
        repository.recordRepaymentResult = NetworkResult.Success(
            RepaymentResult(officeId = 1, clientId = 501L, loanId = 42L, resourceId = 9001L),
        )
        val viewModel = buildViewModel(loanId = 42L, installmentAmount = 125.0)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.trySendAction(LoanRepaymentDialogAction.OnPaymentMethodSelected(PaymentMethod.CASH))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(LoanRepaymentDialogAction.OnSubmit)
            assertEquals(LoanRepaymentDialogEvent.RepaymentRecorded(loanId = 42L), awaitItem())
            assertEquals(LoanRepaymentDialogEvent.Dismiss, awaitItem())
        }

        val state = viewModel.stateFlow.value
        assertEquals(false, state.isSubmitting)
        assertNull(state.submitError)
        assertEquals(1, repository.recordRepaymentCallCount)
        assertEquals(42L, repository.lastLoanId)
        assertEquals(
            RecordRepaymentRequest(amount = 125.0, paymentMethod = PaymentMethod.CASH, referenceNumber = null),
            repository.lastRequest,
        )
    }

    // -- Submit transport error -----------------------------------------------------------------------------

    @Test
    fun `OnSubmit BAD_REQUEST error sets submitError to amount-exceeds`() = runTest(testDispatcher) {
        repository.recordRepaymentResult = NetworkResult.Error(NetworkError.BAD_REQUEST)
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(LoanRepaymentDialogAction.OnSubmit)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals("error_amount_exceeds", state.submitError)
        assertEquals(false, state.isSubmitting)
    }

    @Test
    fun `OnSubmit NOT_FOUND error sets submitError to loan-not-found`() = runTest(testDispatcher) {
        repository.recordRepaymentResult = NetworkResult.Error(NetworkError.NOT_FOUND)
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(LoanRepaymentDialogAction.OnSubmit)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("error_loan_not_found", viewModel.stateFlow.value.submitError)
    }

    @Test
    fun `OnSubmit SERVER error sets submitError to server and emits ShowError`() = runTest(testDispatcher) {
        repository.recordRepaymentResult = NetworkResult.Error(NetworkError.SERVER)
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(LoanRepaymentDialogAction.OnSubmit)
            assertEquals(LoanRepaymentDialogEvent.ShowError(message = "error_server"), awaitItem())
        }
        assertEquals("error_server", viewModel.stateFlow.value.submitError)
    }

    @Test
    fun `retry after a failed submit re-sends the corrected amount`() = runTest(testDispatcher) {
        repository.recordRepaymentResult = NetworkResult.Error(NetworkError.SERVER)
        val viewModel = buildViewModel(loanId = 99L, installmentAmount = 50.0)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.trySendAction(LoanRepaymentDialogAction.OnSubmit)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("error_server", viewModel.stateFlow.value.submitError)

        repository.recordRepaymentResult = NetworkResult.Success(
            RepaymentResult(officeId = 1, clientId = 1L, loanId = 99L, resourceId = 1L),
        )
        viewModel.trySendAction(LoanRepaymentDialogAction.OnSubmit)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.stateFlow.value.submitError)
        assertEquals(2, repository.recordRepaymentCallCount)
    }
}

private class FakeLoanRepaymentRepository : LoanRepaymentRepository {
    var recordRepaymentResult: NetworkResult<RepaymentResult, NetworkError> = NetworkResult.Success(
        RepaymentResult(officeId = 1, clientId = 1L, loanId = 1L, resourceId = 1L),
    )

    var recordRepaymentCallCount: Int = 0
        private set
    var lastLoanId: Long? = null
        private set
    var lastRequest: RecordRepaymentRequest? = null
        private set

    override suspend fun recordRepayment(
        loanId: Long,
        request: RecordRepaymentRequest,
    ): NetworkResult<RepaymentResult, NetworkError> {
        recordRepaymentCallCount++
        lastLoanId = loanId
        lastRequest = request
        return recordRepaymentResult
    }
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
