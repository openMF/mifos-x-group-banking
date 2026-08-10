/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.personalloans

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
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
import kpt.core.data.repository.LoanRepository
import kpt.core.model.LoanAccountStatus
import kpt.core.model.LoanStatusFilter
import kpt.core.model.LoanSummary
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * See API.md#viewmodel — PersonalLoansViewModelTest exercises every declared [PersonalLoansAction]
 * path plus the [NetworkResult]→[PersonalLoansState] mapping, per RULE-TDD-METHODOLOGY-001 /
 * RULE-IMPL-DEAD-CLICKABLE-001.
 *
 * `LoanRepository.getLoansForClient` is a plain `suspend fun` (NOT a Store5 stream, per its own
 * KDoc "Store5-free branch" note) so [FakePersonalLoansRepository] is a simple in-memory fake
 * (no Store5 `Fetcher`/`SourceOfTruth` scaffolding needed, unlike `loan-list`'s paged-stream
 * fixture).
 */
class PersonalLoansViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: FakePersonalLoansRepository
    private lateinit var sessionManager: SessionManager

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakePersonalLoansRepository()
        sessionManager = SessionManager(policy = SecurityPolicy())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(clientId: Long = CLIENT_ID): PersonalLoansViewModel = PersonalLoansViewModel(
        repository = repository,
        sessionManager = sessionManager,
        crashReporter = ConsoleCrashReporter(),
        analytics = KptAnalyticsTracker(NoOpAnalyticsHelper()),
        clientId = clientId,
    )

    // -- init / on_mount load -------------------------------------------------------------------

    @Test
    fun `initial state seeds clientId, ALL filter, and loading true before the fetch resolves`() =
        runTest(testDispatcher) {
            repository.result = NetworkResult.Success(listOf(loan(1)))
            val viewModel = buildViewModel(clientId = CLIENT_ID)

            val state = viewModel.stateFlow.value
            assertEquals(CLIENT_ID, state.clientId)
            assertEquals(LoanStatusFilter.ALL, state.filterStatus)
            assertTrue(state.isLoading)
            assertNull(state.selectedLoanId)
        }

    @Test
    fun `on_mount success maps loans and filteredLoans and clears loading to Content`() = runTest(testDispatcher) {
        repository.result = NetworkResult.Success(listOf(loan(1), loan(2)))
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(2, state.loans.size)
        assertEquals(2, state.filteredLoans.size)
        assertEquals(PersonalLoansScreenState.Content, state.deriveScreenState())
    }

    @Test
    fun `on_mount success with zero loans derives Empty screen state`() = runTest(testDispatcher) {
        repository.result = NetworkResult.Success(emptyList())
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertTrue(state.loans.isEmpty())
        assertEquals(PersonalLoansScreenState.Empty, state.deriveScreenState())
    }

    @Test
    fun `on_mount SERVER error maps to LoanError Server and derives Error screen state`() = runTest(testDispatcher) {
        repository.result = NetworkResult.Error(NetworkError.SERVER)
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertFalse(state.isLoading)
        assertEquals(LoanError.Server, state.error)
        assertTrue(state.error?.retry == true)
        assertEquals(PersonalLoansScreenState.Error, state.deriveScreenState())
    }

    @Test
    fun `on_mount UNAUTHORIZED maps to LoanError Unauthorized and ends the session for real`() =
        runTest(testDispatcher) {
            sessionManager.startSession()
            assertTrue(sessionManager.isSessionActive.value)
            repository.result = NetworkResult.Error(NetworkError.UNAUTHORIZED)
            val viewModel = buildViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.stateFlow.value
            assertEquals(LoanError.Unauthorized, state.error)
            assertFalse(state.error?.retry == true)
            assertFalse(sessionManager.isSessionActive.value, "Unauthorized must call sessionManager.endSession()")
        }

    // -- OnFilterChange (client-side, no re-fetch) -----------------------------------------------

    @Test
    fun `OnFilterChange ACTIVE narrows filteredLoans while loans list is untouched`() = runTest(testDispatcher) {
        repository.result = NetworkResult.Success(
            listOf(
                loan(1, status = LoanAccountStatus.ACTIVE),
                loan(2, status = LoanAccountStatus.CLOSED),
            ),
        )
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(PersonalLoansAction.OnFilterChange(LoanStatusFilter.ACTIVE))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(LoanStatusFilter.ACTIVE, state.filterStatus)
        assertEquals(listOf(1L), state.filteredLoans.map { it.id })
        assertEquals(2, state.loans.size, "unfiltered loans list is untouched by the filter")
        assertEquals(1, repository.callCount, "client-side filter must not re-fetch (only the init mount fetched)")
    }

    @Test
    fun `OnFilterChange CLOSED then ALL round-trips filteredLoans without a re-fetch`() = runTest(testDispatcher) {
        repository.result = NetworkResult.Success(
            listOf(
                loan(1, status = LoanAccountStatus.ACTIVE),
                loan(2, status = LoanAccountStatus.CLOSED),
            ),
        )
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        val fetchesAfterMount = repository.callCount

        viewModel.trySendAction(PersonalLoansAction.OnFilterChange(LoanStatusFilter.CLOSED))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf(2L), viewModel.stateFlow.value.filteredLoans.map { it.id })

        viewModel.trySendAction(PersonalLoansAction.OnFilterChange(LoanStatusFilter.ALL))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.stateFlow.value.filteredLoans.size)
        assertEquals(fetchesAfterMount, repository.callCount, "filter changes are pure in-memory transitions")
    }

    // -- OnLoanExpand (toggle) --------------------------------------------------------------------

    @Test
    fun `OnLoanExpand sets selectedLoanId and a second tap on the same loan collapses it`() = runTest(testDispatcher) {
        repository.result = NetworkResult.Success(listOf(loan(1)))
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(PersonalLoansAction.OnLoanExpand(1L))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1L, viewModel.stateFlow.value.selectedLoanId)

        viewModel.trySendAction(PersonalLoansAction.OnLoanExpand(1L))
        testDispatcher.scheduler.advanceUntilIdle()
        assertNull(viewModel.stateFlow.value.selectedLoanId)
    }

    // -- OnRefresh --------------------------------------------------------------------------------

    @Test
    fun `OnRefresh re-dispatches a real fetch and clears isRefreshing once new data returns`() =
        runTest(testDispatcher) {
            repository.result = NetworkResult.Success(listOf(loan(1)))
            val viewModel = buildViewModel()
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(1, viewModel.stateFlow.value.loans.size)

            repository.result = NetworkResult.Success(listOf(loan(1), loan(2)))
            viewModel.trySendAction(PersonalLoansAction.OnRefresh)
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.stateFlow.value
            assertEquals(2, state.loans.size)
            assertFalse(state.isRefreshing)
            assertEquals(2, repository.callCount, "on_mount + OnRefresh == 2 real fetches")
        }

    // -- OnRetry ----------------------------------------------------------------------------------

    @Test
    fun `OnRetry clears the error and re-dispatches a fetch that recovers`() = runTest(testDispatcher) {
        repository.result = NetworkResult.Error(NetworkError.SERVER)
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(LoanError.Server, viewModel.stateFlow.value.error)

        repository.result = NetworkResult.Success(listOf(loan(1)))
        viewModel.trySendAction(PersonalLoansAction.OnRetry)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertNull(state.error)
        assertFalse(state.isLoading)
        assertEquals(1, state.loans.size)
    }

    // -- OnRequestLoanClick -------------------------------------------------------------------------

    @Test
    fun `OnRequestLoanClick emits NavigateToLoanRequest`() = runTest(testDispatcher) {
        repository.result = NetworkResult.Success(listOf(loan(1)))
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(PersonalLoansAction.OnRequestLoanClick)
            assertEquals(PersonalLoansEvent.NavigateToLoanRequest, awaitItem())
        }
    }
}

// ---------------------------------------------------------------------------
// Fixtures
// ---------------------------------------------------------------------------

private const val CLIENT_ID = 55L

private fun loan(
    id: Long,
    status: LoanAccountStatus = LoanAccountStatus.ACTIVE,
): LoanSummary = LoanSummary(
    id = id,
    memberId = CLIENT_ID,
    memberName = "Member $id",
    memberPhotoUrl = null,
    loanProductName = "Personal Loan",
    principalAmount = 2000.0,
    outstandingBalance = 1500.0,
    overdueAmount = 0.0,
    status = status,
    nextRepaymentDate = "2026-08-01",
    isOverdue = status == LoanAccountStatus.OVERDUE,
    fineractLoanId = id,
)

/**
 * In-memory [LoanRepository] fake — [getLoansForClient] is a plain `suspend fun` (no Store5
 * wrap per its own KDoc), so [result] is a simple `var` swapped between dispatches, mirroring
 * `LoanRequestViewModelTest`'s `FakeLoanRequestRepository.submitResult` convention.
 * [loansPagingStream] is unused by `PersonalLoansViewModel` but must still be implemented to
 * satisfy the shared [LoanRepository] contract — it throws if ever invoked, so an accidental
 * call surfaces loudly instead of silently returning empty pages.
 */
private class FakePersonalLoansRepository : LoanRepository {
    var result: NetworkResult<List<LoanSummary>, NetworkError> = NetworkResult.Success(emptyList())

    var callCount: Int = 0
        private set

    override suspend fun getLoansForClient(clientId: Long): NetworkResult<List<LoanSummary>, NetworkError> {
        callCount++
        return result
    }

    override fun loansPagingStream(
        groupId: Long,
        scope: kotlinx.coroutines.CoroutineScope,
        fetchPolicy: kpt.core.base.store.screen.FetchPolicy,
    ): kpt.core.base.store.paging.PagingScreenStream<LoanSummary> {
        error("loansPagingStream is not used by PersonalLoansViewModel — personal-loans reads via getLoansForClient")
    }
}
