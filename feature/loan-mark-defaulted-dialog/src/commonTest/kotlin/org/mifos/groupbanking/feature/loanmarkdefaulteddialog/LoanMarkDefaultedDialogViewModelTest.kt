/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.loanmarkdefaulteddialog

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
import org.mifos.groupbanking.core.data.repository.LoanWriteoffRepository
import org.mifos.groupbanking.core.model.WriteoffResult
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * See API.md#viewmodel — LoanMarkDefaultedDialogViewModelTest exercises every declared
 * [LoanMarkDefaultedDialogAction] path (nav-arg pre-fill, confirm success/offline-block/transport
 * error, and dismiss), per RULE-TDD-METHODOLOGY-001 / RULE-IMPL-DEAD-CLICKABLE-001. Written FIRST,
 * ahead of [LoanMarkDefaultedDialogViewModel] itself, per RULE-TDD-GLOBAL-001. Modeled directly on
 * `LoanRepaymentDialogViewModelTest` (this feature is the simpler "confirm only, no input fields"
 * sibling — see the caller's brief).
 */
class LoanMarkDefaultedDialogViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: FakeLoanWriteoffRepository
    private lateinit var networkMonitor: FakeNetworkMonitor

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeLoanWriteoffRepository()
        networkMonitor = FakeNetworkMonitor(initiallyOnline = true)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(
        loanId: Long = 42L,
        memberName: String = "Peter Otieno",
        loanAmountKes: Double = 1500.0,
    ): LoanMarkDefaultedDialogViewModel = LoanMarkDefaultedDialogViewModel(
        repository = repository,
        networkMonitor = networkMonitor,
        analytics = KptAnalyticsTracker(NoOpAnalyticsHelper()),
        crashReporter = ConsoleCrashReporter(),
        loanId = loanId,
        memberName = memberName,
        loanAmountKes = loanAmountKes,
    )

    // -- Initial state / nav-arg seeding ---------------------------------------------------------

    @Test
    fun `initial state is pre-filled from the memberName and loanAmountKes nav-args`() = runTest(testDispatcher) {
        val viewModel = buildViewModel(memberName = "Grace Wanjiru", loanAmountKes = 2500.0)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals("Grace Wanjiru", state.memberName)
        assertEquals(2500.0, state.loanAmountKes)
        assertEquals(false, state.isSubmitting)
        assertNull(state.submitError)
    }

    // -- Dismiss ------------------------------------------------------------------------------------

    @Test
    fun `OnDismiss always emits Dismiss without calling the repository`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(LoanMarkDefaultedDialogAction.OnDismiss)
            assertEquals(LoanMarkDefaultedDialogEvent.Dismiss, awaitItem())
        }
        assertEquals(0, repository.writeoffLoanCallCount)
    }

    // -- Confirm while offline -----------------------------------------------------------------------

    @Test
    fun `OnConfirm while offline sets submitError and shows an error, without calling the repository`() =
        runTest(testDispatcher) {
            networkMonitor.setOnline(false)
            val viewModel = buildViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.eventFlow.test {
                viewModel.trySendAction(LoanMarkDefaultedDialogAction.OnConfirm)
                assertEquals(LoanMarkDefaultedDialogEvent.ShowError(message = "error_offline_no_queue"), awaitItem())
            }

            val state = viewModel.stateFlow.value
            assertEquals("error_offline_no_queue", state.submitError)
            assertEquals(false, state.isSubmitting)
            assertEquals(0, repository.writeoffLoanCallCount)
        }

    // -- Confirm success --------------------------------------------------------------------------

    @Test
    fun `OnConfirm success emits LoanMarkedDefaulted then Dismiss and resets isSubmitting`() = runTest(testDispatcher) {
        repository.writeoffLoanResult = NetworkResult.Success(
            WriteoffResult(officeId = 1, clientId = 501L, loanId = 42L, resourceId = 9001L),
        )
        val viewModel = buildViewModel(loanId = 42L)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(LoanMarkDefaultedDialogAction.OnConfirm)
            assertEquals(LoanMarkDefaultedDialogEvent.LoanMarkedDefaulted(loanId = 42L), awaitItem())
            assertEquals(LoanMarkDefaultedDialogEvent.Dismiss, awaitItem())
        }

        val state = viewModel.stateFlow.value
        assertEquals(false, state.isSubmitting)
        assertNull(state.submitError)
        assertEquals(1, repository.writeoffLoanCallCount)
        assertEquals(42L, repository.lastLoanId)
    }

    // -- Confirm transport error ---------------------------------------------------------------------

    @Test
    fun `OnConfirm NOT_FOUND error sets submitError to loan-not-found`() = runTest(testDispatcher) {
        repository.writeoffLoanResult = NetworkResult.Error(NetworkError.NOT_FOUND)
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(LoanMarkDefaultedDialogAction.OnConfirm)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("error_loan_not_found", viewModel.stateFlow.value.submitError)
        assertEquals(false, viewModel.stateFlow.value.isSubmitting)
    }

    @Test
    fun `OnConfirm SERVER error sets submitError to server and emits ShowError`() = runTest(testDispatcher) {
        repository.writeoffLoanResult = NetworkResult.Error(NetworkError.SERVER)
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(LoanMarkDefaultedDialogAction.OnConfirm)
            assertEquals(LoanMarkDefaultedDialogEvent.ShowError(message = "error_server"), awaitItem())
        }
        assertEquals("error_server", viewModel.stateFlow.value.submitError)
    }

    @Test
    fun `OnConfirm UNKNOWN (folded 403 Forbidden or 409 conflict) error falls back to error_server`() =
        runTest(testDispatcher) {
            repository.writeoffLoanResult = NetworkResult.Error(NetworkError.UNKNOWN)
            val viewModel = buildViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.trySendAction(LoanMarkDefaultedDialogAction.OnConfirm)
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals("error_server", viewModel.stateFlow.value.submitError)
        }

    @Test
    fun `retry after a failed confirm re-attempts the write-off`() = runTest(testDispatcher) {
        repository.writeoffLoanResult = NetworkResult.Error(NetworkError.SERVER)
        val viewModel = buildViewModel(loanId = 99L)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.trySendAction(LoanMarkDefaultedDialogAction.OnConfirm)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("error_server", viewModel.stateFlow.value.submitError)

        repository.writeoffLoanResult = NetworkResult.Success(
            WriteoffResult(officeId = 1, clientId = 1L, loanId = 99L, resourceId = 1L),
        )
        viewModel.trySendAction(LoanMarkDefaultedDialogAction.OnConfirm)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.stateFlow.value.submitError)
        assertEquals(2, repository.writeoffLoanCallCount)
    }
}

private class FakeLoanWriteoffRepository : LoanWriteoffRepository {
    var writeoffLoanResult: NetworkResult<WriteoffResult, NetworkError> = NetworkResult.Success(
        WriteoffResult(officeId = 1, clientId = 1L, loanId = 1L, resourceId = 1L),
    )

    var writeoffLoanCallCount: Int = 0
        private set
    var lastLoanId: Long? = null
        private set

    override suspend fun writeoffLoan(loanId: Long): NetworkResult<WriteoffResult, NetworkError> {
        writeoffLoanCallCount++
        lastLoanId = loanId
        return writeoffLoanResult
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
