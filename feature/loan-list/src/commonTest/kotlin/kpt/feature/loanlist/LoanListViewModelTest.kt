/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.loanlist

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
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.base.observability.ConsoleCrashReporter
import kpt.core.base.security.SecurityPolicy
import kpt.core.base.security.SessionManager
import kpt.core.base.store.infra.FetchedAtRepository
import kpt.core.base.store.infra.StoreFactory
import kpt.core.base.store.paging.PageKey
import kpt.core.base.store.paging.PagingScreenStream
import kpt.core.base.store.paging.asPagingScreenStream
import kpt.core.base.store.screen.FetchPolicy
import kpt.core.data.repository.LoanRepository
import kpt.core.model.LoanAccountStatus
import kpt.core.model.LoanStatusFilter
import kpt.core.model.LoanSummary
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * See API.md#viewmodel — LoanListViewModelTest exercises every declared [LoanListAction] path
 * plus the [kpt.core.base.store.screen.ScreenState]→[LoanListState] mapping, per
 * RULE-TDD-METHODOLOGY-001 / RULE-IMPL-DEAD-CLICKABLE-001.
 *
 * [PagingScreenStream] has an `internal` constructor (core-base:store module-private), so this
 * suite builds a REAL Store5-backed [FakeLoanRepository] (via the public
 * `Store<PageKey, List<LoanSummary>>.asPagingScreenStream(...)` extension + an in-memory
 * `Fetcher`/`SourceOfTruth`) rather than a hand-rolled stub — mirrors
 * `GroupListViewModelTest`'s / `LoanRepositoryTest`'s identical approach, scaled down (no
 * `LoanApi`/`LoanListDao` dependency) so this feature module doesn't need `core/network`/
 * `core/database` as a test dep.
 *
 * Because the real `Store` (mobilenativefoundation/store5) drives its fetch/source-of-truth
 * pipeline on its own internal coroutine machinery — NOT solely the `StandardTestDispatcher`
 * queue `advanceUntilIdle()` drains — every assertion that depends on the paging stream settling
 * uses [awaitState] (a real suspending `Flow.first { predicate }`) instead of a fire-and-forget
 * `advanceUntilIdle()` + `.value` read. Each created ViewModel's `viewModelScope` is cancelled in
 * [tearDown] so no test's in-flight Store5 work leaks into the next test's `Dispatchers.Main`
 * binding.
 */
class LoanListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val createdViewModels = mutableListOf<LoanListViewModel>()

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
        pages: Map<Int, List<LoanSummary>> = emptyMap(),
        failWith: Throwable? = null,
        online: Boolean = true,
        pageSize: Int = 2,
        groupId: Long = GROUP_ID,
        sessionManager: SessionManager = SessionManager(policy = SecurityPolicy()),
    ): Triple<FakeLoanRepository, SessionManager, LoanListViewModel> {
        val repository = FakeLoanRepository(
            pagesByIndex = pages,
            failWith = failWith,
            online = online,
            pageSize = pageSize,
        )
        val viewModel = LoanListViewModel(
            repository = repository,
            sessionManager = sessionManager,
            analytics = KptAnalyticsTracker(NoOpAnalyticsHelper()),
            crashReporter = ConsoleCrashReporter(),
            groupId = groupId,
            // MEMBER is in none of the capability role-sets (LOAN_APPLY_ROLES etc.), so the
            // base-state capability flags stay false — matching these tests' assertions.
            viewerRole = "MEMBER",
        )
        createdViewModels += viewModel
        return Triple(repository, sessionManager, viewModel)
    }

    /**
     * Suspends until [LoanListState] satisfies [predicate]. The real Store5 `Store` drives its
     * fetch/source-of-truth pipeline on its OWN internal coroutine machinery, independent of this
     * test's `StandardTestDispatcher` virtual clock — hopping to [Dispatchers.Default] moves both
     * the `first()` suspension AND the `withTimeout` deadline onto real wall-clock time so it
     * genuinely waits for the cross-dispatcher Store5 emission (mirrors `GroupListViewModelTest`).
     */
    private suspend fun LoanListViewModel.awaitState(
        timeoutMs: Long = 5_000,
        predicate: (LoanListState) -> Boolean,
    ): LoanListState = withContext(Dispatchers.Default) {
        withTimeout(timeoutMs) { stateFlow.first(predicate) }
    }

    // ─── initial state + stream mapping ─────────────────────────────────────

    @Test
    fun `initial state is loading, seeds groupId, ALL filter, and canApplyLoan false`() = runTest(testDispatcher) {
        val (_, _, viewModel) = buildViewModel(groupId = GROUP_ID)

        val state = viewModel.stateFlow.value
        assertTrue(state.isLoading)
        assertNull(state.error)
        assertTrue(state.loans.isEmpty())
        assertTrue(state.filteredLoans.isEmpty())
        assertEquals(GROUP_ID, state.groupId)
        assertEquals(LoanStatusFilter.ALL, state.selectedFilter)
        assertFalse(state.canApplyLoan, "no role source reachable — see LoanListState KDoc gap note")
    }

    @Test
    fun `stream Content maps to loans and filteredLoans and clears loading`() = runTest(testDispatcher) {
        val (_, _, viewModel) = buildViewModel(
            pages = mapOf(0 to listOf(loan(1, "Peter Otieno"), loan(2, "Grace Wanjiku"))),
        )

        val state = viewModel.awaitState { !it.isLoading }
        assertNull(state.error)
        assertEquals(2, state.loans.size)
        assertEquals(2, state.filteredLoans.size, "no active filter (ALL) — filteredLoans mirrors loans")
    }

    @Test
    fun `stream Empty maps to empty loans with no error`() = runTest(testDispatcher) {
        val (_, _, viewModel) = buildViewModel(pages = mapOf(0 to emptyList()))

        val state = viewModel.awaitState { !it.isLoading }
        assertNull(state.error)
        assertTrue(state.loans.isEmpty())
        assertEquals(LoanListScreenState.Empty, state.screenState)
    }

    @Test
    fun `stream connectivity failure maps to Network error`() = runTest(testDispatcher) {
        val (_, _, viewModel) = buildViewModel(failWith = FakeConnectException("connect failed"))

        val state = viewModel.awaitState { it.error != null }
        assertEquals(LoanListError.Network, state.error)
        assertEquals(false, state.isLoading)
        assertTrue(state.error?.retry == true)
    }

    @Test
    fun `stream 500 maps to Server error`() = runTest(testDispatcher) {
        val (_, _, viewModel) = buildViewModel(failWith = RuntimeException("HTTP 500 Internal Server Error"))

        val state = viewModel.awaitState { it.error != null }
        assertEquals(LoanListError.Server, state.error)
        assertTrue(state.error?.retry == true)
    }

    @Test
    fun `stream 401 ends the session, maps to Auth error, and emits ShowSnackbar`() = runTest(testDispatcher) {
        val sessionManager = SessionManager(policy = SecurityPolicy())
        sessionManager.startSession()
        val (_, sm, viewModel) = buildViewModel(
            failWith = RuntimeException("HTTP 401 Unauthorized"),
            sessionManager = sessionManager,
        )
        assertTrue(sm.isSessionActive.value)

        viewModel.eventFlow.test {
            val event = awaitItem()
            assertTrue(event is LoanListEvent.ShowSnackbar)
            assertEquals(LoanListError.Auth.messageKey, event.message)
        }
        val state = viewModel.awaitState { it.error != null }
        assertEquals(LoanListError.Auth, state.error)
        assertEquals(false, state.error?.retry)
        assertFalse(sm.isSessionActive.value, "Unauthenticated must call sessionManager.endSession() for real")
    }

    // ─── OnLoanClick / OnApplyLoan ───────────────────────────────────────────

    @Test
    fun `OnLoanClick emits NavigateToLoanDetail with loanId`() = runTest(testDispatcher) {
        val (_, _, viewModel) = buildViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(LoanListAction.OnLoanClick(loanId = 42L))
            assertEquals(LoanListEvent.NavigateToLoanDetail(42L), awaitItem())
        }
    }

    @Test
    fun `OnApplyLoan emits NavigateToLoanApply with groupId`() = runTest(testDispatcher) {
        val (_, _, viewModel) = buildViewModel(groupId = GROUP_ID)

        viewModel.eventFlow.test {
            viewModel.trySendAction(LoanListAction.OnApplyLoan)
            assertEquals(LoanListEvent.NavigateToLoanApply(GROUP_ID), awaitItem())
        }
    }

    // ─── OnFilterChange ───────────────────────────────────────────────────────

    @Test
    fun `OnFilterChange ACTIVE narrows filteredLoans while loans stays full`() = runTest(testDispatcher) {
        val (_, _, viewModel) = buildViewModel(
            pages = mapOf(
                0 to listOf(
                    loan(1, "Peter Otieno", status = LoanAccountStatus.ACTIVE),
                    loan(2, "Grace Wanjiku", status = LoanAccountStatus.OVERDUE, isOverdue = true),
                ),
            ),
        )
        viewModel.awaitState { !it.isLoading }

        viewModel.trySendAction(LoanListAction.OnFilterChange(LoanStatusFilter.ACTIVE))
        val state = viewModel.awaitState { it.selectedFilter == LoanStatusFilter.ACTIVE }

        assertEquals(1, state.filteredLoans.size)
        assertEquals(1L, state.filteredLoans.first().id)
        assertEquals(2, state.loans.size, "unfiltered loans list is untouched by the filter")
    }

    @Test
    fun `OnFilterChange OVERDUE and CLOSED select only matching statuses`() = runTest(testDispatcher) {
        val (_, _, viewModel) = buildViewModel(
            pages = mapOf(
                0 to listOf(
                    loan(1, "Active", status = LoanAccountStatus.ACTIVE),
                    loan(2, "Overdue", status = LoanAccountStatus.OVERDUE, isOverdue = true),
                    loan(3, "Closed", status = LoanAccountStatus.CLOSED),
                ),
            ),
        )
        viewModel.awaitState { !it.isLoading }

        viewModel.trySendAction(LoanListAction.OnFilterChange(LoanStatusFilter.OVERDUE))
        val overdueState = viewModel.awaitState { it.selectedFilter == LoanStatusFilter.OVERDUE }
        assertEquals(listOf(2L), overdueState.filteredLoans.map { it.id })

        viewModel.trySendAction(LoanListAction.OnFilterChange(LoanStatusFilter.CLOSED))
        val closedState = viewModel.awaitState { it.selectedFilter == LoanStatusFilter.CLOSED }
        assertEquals(listOf(3L), closedState.filteredLoans.map { it.id })
    }

    @Test
    fun `OnFilterChange ALL restores the full loans list after a narrower filter`() = runTest(testDispatcher) {
        val (_, _, viewModel) = buildViewModel(
            pages = mapOf(
                0 to listOf(
                    loan(1, "Active", status = LoanAccountStatus.ACTIVE),
                    loan(2, "Closed", status = LoanAccountStatus.CLOSED),
                ),
            ),
        )
        viewModel.awaitState { !it.isLoading }
        viewModel.trySendAction(LoanListAction.OnFilterChange(LoanStatusFilter.CLOSED))
        viewModel.awaitState { it.selectedFilter == LoanStatusFilter.CLOSED }

        viewModel.trySendAction(LoanListAction.OnFilterChange(LoanStatusFilter.ALL))
        val state = viewModel.awaitState { it.selectedFilter == LoanStatusFilter.ALL }

        assertEquals(2, state.filteredLoans.size)
    }

    @Test
    fun `active filter survives a subsequent stream Content update`() = runTest(testDispatcher) {
        val (repository, _, viewModel) = buildViewModel(
            pages = mapOf(
                0 to listOf(
                    loan(1, "Active", status = LoanAccountStatus.ACTIVE),
                    loan(2, "Closed", status = LoanAccountStatus.CLOSED),
                ),
            ),
        )
        viewModel.awaitState { !it.isLoading }
        viewModel.trySendAction(LoanListAction.OnFilterChange(LoanStatusFilter.ACTIVE))
        viewModel.awaitState { it.selectedFilter == LoanStatusFilter.ACTIVE }

        // Widen the fixture so the post-refresh Content emission is unambiguously distinguishable
        // from the pre-refresh one — proves the refresh round-trip actually completed AND
        // filteredLoans is recomputed against the still-active selectedFilter (not reset).
        repository.pagesByIndex = mapOf(
            0 to listOf(
                loan(1, "Active", status = LoanAccountStatus.ACTIVE),
                loan(2, "Closed", status = LoanAccountStatus.CLOSED),
                loan(3, "Active 2", status = LoanAccountStatus.ACTIVE),
            ),
        )
        viewModel.trySendAction(LoanListAction.OnRefresh)
        val state = viewModel.awaitState { it.loans.size == 3 }

        assertEquals(LoanStatusFilter.ACTIVE, state.selectedFilter)
        assertEquals(setOf(1L, 3L), state.filteredLoans.map { it.id }.toSet())
    }

    // ─── OnRefresh / Retry ──────────────────────────────────────────────────

    @Test
    fun `OnRefresh re-dispatches a real fetch and clears isRefreshing once new data returns`() = runTest(testDispatcher) {
        val (repository, _, viewModel) = buildViewModel(pages = mapOf(0 to listOf(loan(1, "Alpha"))))
        viewModel.awaitState { it.loans.size == 1 }

        repository.pagesByIndex = mapOf(0 to listOf(loan(1, "Alpha"), loan(2, "Beta")))
        viewModel.trySendAction(LoanListAction.OnRefresh)
        val state = viewModel.awaitState { it.loans.size == 2 }

        assertEquals(false, state.isRefreshing)
    }

    @Test
    fun `Retry clears the error and re-dispatches a real fetch that recovers via LoanRepository`() = runTest(testDispatcher) {
        val (repository, _, viewModel) = buildViewModel(failWith = RuntimeException("HTTP 500 boom"))
        viewModel.awaitState { it.error != null }
        assertEquals(LoanListError.Server, viewModel.stateFlow.value.error)

        repository.failWith = null
        repository.pagesByIndex = mapOf(0 to listOf(loan(1, "Alpha")))
        viewModel.trySendAction(LoanListAction.Retry)
        val state = viewModel.awaitState { it.error == null && !it.isLoading }

        assertEquals(1, state.loans.size)
    }

    // ─── OnLoadNextPage ──────────────────────────────────────────────────────

    @Test
    fun `OnLoadNextPage appends the next page to loans and filteredLoans`() = runTest(testDispatcher) {
        val (_, _, viewModel) = buildViewModel(
            pages = mapOf(
                0 to listOf(loan(1, "Alpha"), loan(2, "Beta")),
                1 to listOf(loan(3, "Gamma")),
            ),
            pageSize = 2,
        )
        viewModel.awaitState { it.loans.size == 2 }

        viewModel.trySendAction(LoanListAction.OnLoadNextPage)
        val state = viewModel.awaitState { it.loans.size == 3 }

        assertEquals(setOf(1L, 2L, 3L), state.loans.map { it.id }.toSet())
        assertEquals(setOf(1L, 2L, 3L), state.filteredLoans.map { it.id }.toSet())
    }
}

// ---------------------------------------------------------------------------
// Fixtures
// ---------------------------------------------------------------------------

private const val GROUP_ID = 101L

private fun loan(
    id: Long,
    memberName: String,
    status: LoanAccountStatus = LoanAccountStatus.ACTIVE,
    isOverdue: Boolean = false,
): LoanSummary = LoanSummary(
    id = id,
    memberId = id,
    memberName = memberName,
    memberPhotoUrl = null,
    loanProductName = "Group Loan",
    principalAmount = 1500.0,
    outstandingBalance = 1125.0,
    overdueAmount = if (isOverdue) 375.0 else 0.0,
    status = status,
    nextRepaymentDate = "2026-05-12",
    isOverdue = isOverdue,
    fineractLoanId = id,
)

/** Exception whose simple class name deliberately matches `categorize()`'s network-keyword scan. */
private class FakeConnectException(message: String) : Exception(message)

// ---------------------------------------------------------------------------
// Fakes — real Store5-backed PagingScreenStream (see class KDoc above for rationale).
// ---------------------------------------------------------------------------

/**
 * In-memory [LoanRepository] fake. Builds a REAL `Store<PageKey, List<LoanSummary>>` per call (an
 * in-memory [Fetcher] + [SourceOfTruth], no TTL [kpt.core.base.store.infra.DefaultValidator]) and
 * exposes it via the same public `.asPagingScreenStream(...)` extension the production
 * `LoanRepositoryImpl` uses — so the ViewModel exercises the real offline-first paging pipeline
 * (Loading → Content/Empty/Error/NoNetwork/Unauthenticated, load-more, refresh).
 *
 * [pagesByIndex] and [failWith] are `var` so a test can mutate the fixture BETWEEN two dispatches
 * (e.g. before `OnRefresh`/`Retry`) and assert on the resulting, unambiguously distinct
 * post-refetch content rather than racing a `StateFlow`'s already-current value.
 */
private class FakeLoanRepository(
    pagesByIndex: Map<Int, List<LoanSummary>> = emptyMap(),
    failWith: Throwable? = null,
    online: Boolean = true,
    private val pageSize: Int = 2,
) : LoanRepository {

    var pagesByIndex: Map<Int, List<LoanSummary>> = pagesByIndex
    var failWith: Throwable? = failWith

    private val networkMonitor: NetworkMonitor = FakeNetworkMonitor(
        if (online) available() else NetworkStatus.Unavailable,
    )
    private val cache = mutableMapOf<Int, MutableStateFlow<List<LoanSummary>?>>()

    private fun flowFor(page: Int): MutableStateFlow<List<LoanSummary>?> =
        cache.getOrPut(page) { MutableStateFlow(null) }

    override fun loansPagingStream(
        groupId: Long,
        scope: CoroutineScope,
        fetchPolicy: FetchPolicy,
    ): PagingScreenStream<LoanSummary> {
        val store = StoreFactory.createStore<PageKey, List<LoanSummary>, List<LoanSummary>>(
            fetcher = Fetcher.of { key: PageKey ->
                failWith?.let { throw it }
                pagesByIndex[key.page] ?: emptyList()
            },
            sourceOfTruth = SourceOfTruth.of(
                reader = { key: PageKey -> flowFor(key.page) },
                writer = { key: PageKey, value: List<LoanSummary> -> flowFor(key.page).value = value },
                delete = { key: PageKey -> flowFor(key.page).value = null },
                deleteAll = { cache.values.forEach { it.value = null } },
            ),
        )
        return store.asPagingScreenStream(
            networkMonitor = networkMonitor,
            fetchedAtRepository = InMemoryFetchedAtRepository(),
            cacheKey = "test:loan-list:$groupId",
            scope = scope,
            pageSize = pageSize,
            fetchPolicy = fetchPolicy,
        )
    }

    override suspend fun getLoansForClient(clientId: Long): NetworkResult<List<LoanSummary>, NetworkError> {
        failWith?.let { return NetworkResult.Error(NetworkError.UNKNOWN) }
        return NetworkResult.Success(pagesByIndex.values.flatten())
    }
}

private fun available(): NetworkStatus.Available =
    NetworkStatus.Available(NetworkInfo(type = NetworkType.WiFi, isMetered = false))

private class FakeNetworkMonitor(initialStatus: NetworkStatus) : NetworkMonitor {
    private val _networkStatus = MutableStateFlow(initialStatus)
    override val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()
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
