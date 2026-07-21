/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.grouplist

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
import kotlinx.datetime.LocalDate
import kpt.core.analytics.KptAnalyticsTracker
import kpt.core.base.analytics.NoOpAnalyticsHelper
import kpt.core.base.observability.ConsoleCrashReporter
import kpt.core.base.store.infra.FetchedAtRepository
import kpt.core.base.store.infra.StoreFactory
import kpt.core.base.store.paging.PageKey
import kpt.core.base.store.paging.PagingScreenStream
import kpt.core.base.store.paging.asPagingScreenStream
import kpt.core.base.store.screen.FetchPolicy
import org.mifos.groupbanking.core.data.repository.GroupRepository
import org.mifos.groupbanking.core.model.Group
import org.mifos.groupbanking.core.model.GroupTypeSlug
import org.mifos.groupbanking.core.model.HealthIndicator
import org.mifos.groupbanking.core.model.ViewerRole
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * See API.md#viewmodel — GroupListViewModelTest exercises every declared [GroupListAction]
 * path plus the [kpt.core.base.store.screen.ScreenState]→[GroupListState] mapping, per
 * RULE-TDD-METHODOLOGY-001 / RULE-IMPL-DEAD-CLICKABLE-001.
 *
 * [PagingScreenStream] has an `internal` constructor (core-base:store module-private), so this
 * suite builds a REAL Store5-backed [FakeGroupRepository] (via the public
 * `Store<PageKey, List<Group>>.asPagingScreenStream(...)` extension + an in-memory
 * `Fetcher`/`SourceOfTruth`) rather than a hand-rolled stub — mirrors `GroupRepositoryTest`'s
 * approach in `core/data`, scaled down (no `GroupApi`/`GroupListDao` dependency) so this feature
 * module doesn't need `core/network`/`core/database` as a test dep.
 *
 * Because the real `Store` (mobilenativefoundation/store5) drives its fetch/source-of-truth
 * pipeline on its own internal coroutine machinery — NOT solely the `StandardTestDispatcher`
 * queue `advanceUntilIdle()` drains — every assertion that depends on the paging stream settling
 * uses [awaitState] (a real suspending `Flow.first { predicate }`, exactly the technique
 * `GroupRepositoryTest` itself uses) instead of a fire-and-forget `advanceUntilIdle()` + `.value`
 * read, which proved racy in this suite's first draft. Each created ViewModel's `viewModelScope`
 * is cancelled in [tearDown] so no test's in-flight Store5 work leaks into the next test's
 * `Dispatchers.Main` binding.
 */
class GroupListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val createdViewModels = mutableListOf<GroupListViewModel>()

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
        pages: Map<Int, List<Group>> = emptyMap(),
        failWith: Throwable? = null,
        online: Boolean = true,
        pageSize: Int = 2,
    ): Pair<FakeGroupRepository, GroupListViewModel> {
        val repository = FakeGroupRepository(
            pagesByIndex = pages,
            failWith = failWith,
            online = online,
            pageSize = pageSize,
        )
        val viewModel = GroupListViewModel(
            repository = repository,
            analytics = KptAnalyticsTracker(NoOpAnalyticsHelper()),
            crashReporter = ConsoleCrashReporter(),
        )
        createdViewModels += viewModel
        return repository to viewModel
    }

    /** Suspends until [GroupListState] satisfies [predicate] — see class KDoc for rationale. */
    /**
     * The real Store5 `Store` drives its fetch/source-of-truth pipeline on its OWN internal
     * coroutine machinery (real `Dispatchers.Default`-class dispatch), independent of this
     * test's `StandardTestDispatcher` virtual clock. `withTimeout` evaluated directly inside
     * `runTest(testDispatcher)` fast-forwards the VIRTUAL clock to the deadline instantly (there
     * is nothing else queued on that scheduler), firing a spurious `TimeoutCancellationException`
     * long before the real background fetch lands. Hopping to [Dispatchers.Default] for the
     * await moves both the `first()` suspension AND the `withTimeout` deadline onto REAL wall-
     * clock time, so it genuinely waits for the cross-dispatcher Store5 emission.
     */
    private suspend fun GroupListViewModel.awaitState(
        timeoutMs: Long = 5_000,
        predicate: (GroupListState) -> Boolean,
    ): GroupListState = withContext(Dispatchers.Default) {
        withTimeout(timeoutMs) { stateFlow.first(predicate) }
    }

    // ─── initial state + stream mapping ─────────────────────────────────────

    @Test
    fun `initial state is loading with empty groups and no error`() = runTest(testDispatcher) {
        val (_, viewModel) = buildViewModel()

        val state = viewModel.stateFlow.value
        assertTrue(state.isLoading)
        assertNull(state.error)
        assertTrue(state.groups.isEmpty())
        assertTrue(state.filteredGroups.isEmpty())
    }

    @Test
    fun `stream Content maps to groups and filteredGroups and clears loading`() = runTest(testDispatcher) {
        val (_, viewModel) = buildViewModel(
            pages = mapOf(0 to listOf(group("g1", "Mwangaza Women's Group"), group("g2", "Jiunge ROSCA"))),
        )

        val state = viewModel.awaitState { !it.isLoading }
        assertNull(state.error)
        assertEquals(2, state.groups.size)
        assertEquals(2, state.filteredGroups.size, "no active search — filteredGroups mirrors groups")
    }

    @Test
    fun `stream Empty maps to empty groups with no error`() = runTest(testDispatcher) {
        val (_, viewModel) = buildViewModel(pages = mapOf(0 to emptyList()))

        val state = viewModel.awaitState { !it.isLoading }
        assertNull(state.error)
        assertTrue(state.groups.isEmpty())
        assertEquals(GroupListScreenState.Empty, state.screenState)
    }

    @Test
    fun `stream connectivity failure maps to Network error`() = runTest(testDispatcher) {
        val (_, viewModel) = buildViewModel(failWith = FakeConnectException("connect failed"))

        val state = viewModel.awaitState { it.error != null }
        assertEquals(GroupListError.Network, state.error)
        assertEquals(false, state.isLoading)
        assertTrue(state.error?.retry == true)
    }

    @Test
    fun `stream 401 maps to Auth error and emits the closest declared event (ShowSnackbar)`() = runTest(testDispatcher) {
        val (_, viewModel) = buildViewModel(failWith = RuntimeException("HTTP 401 Unauthorized"))

        viewModel.eventFlow.test {
            val event = awaitItem()
            assertTrue(event is GroupListEvent.ShowSnackbar)
            assertEquals(GroupListError.Auth.messageKey, event.message)
        }
        val state = viewModel.awaitState { it.error != null }
        assertEquals(GroupListError.Auth, state.error)
        assertEquals(false, state.error?.retry)
    }

    @Test
    fun `stream 500 maps to Server error`() = runTest(testDispatcher) {
        val (_, viewModel) = buildViewModel(failWith = RuntimeException("HTTP 500 Internal Server Error"))

        val state = viewModel.awaitState { it.error != null }
        assertEquals(GroupListError.Server, state.error)
        assertTrue(state.error?.retry == true)
    }

    // ─── OnGroupClick ─────────────────────────────────────────────────────────

    @Test
    fun `OnGroupClick emits NavigateToGroupDashboard with groupId and viewerRole`() = runTest(testDispatcher) {
        val (_, viewModel) = buildViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(GroupListAction.OnGroupClick(groupId = "g1", viewerRole = "ORGANIZER"))
            assertEquals(GroupListEvent.NavigateToGroupDashboard("g1", "ORGANIZER"), awaitItem())
        }
    }

    // ─── OnSearch / OnClearSearch ───────────────────────────────────────────

    @Test
    fun `OnSearch filters groups by name case-insensitively`() = runTest(testDispatcher) {
        val (_, viewModel) = buildViewModel(
            pages = mapOf(0 to listOf(group("g1", "Mwangaza Women's Group"), group("g2", "Jiunge ROSCA Circle"))),
        )
        viewModel.awaitState { !it.isLoading }

        viewModel.trySendAction(GroupListAction.OnSearch("mwangaza"))
        val state = viewModel.awaitState { it.searchQuery == "mwangaza" }

        assertEquals(1, state.filteredGroups.size)
        assertEquals("g1", state.filteredGroups.first().id)
        assertEquals(2, state.groups.size, "unfiltered groups list is untouched by search")
    }

    @Test
    fun `OnSearch with no match yields empty filteredGroups while groups stay populated`() = runTest(testDispatcher) {
        val (_, viewModel) = buildViewModel(pages = mapOf(0 to listOf(group("g1", "Alpha"))))
        viewModel.awaitState { !it.isLoading }

        viewModel.trySendAction(GroupListAction.OnSearch("zzz-no-match"))
        val state = viewModel.awaitState { it.searchQuery == "zzz-no-match" }

        assertTrue(state.filteredGroups.isEmpty())
        assertEquals(1, state.groups.size, "no-match search must not clear the underlying groups list")
    }

    @Test
    fun `search query survives a subsequent stream Content update`() = runTest(testDispatcher) {
        val (repository, viewModel) = buildViewModel(
            pages = mapOf(0 to listOf(group("g1", "Alpha"), group("g2", "Beta"))),
        )
        viewModel.awaitState { !it.isLoading }
        viewModel.trySendAction(GroupListAction.OnSearch("alpha"))
        viewModel.awaitState { it.searchQuery == "alpha" }

        // A refresh re-fetches page 0; this time the (deliberately widened) fixture returns a
        // THIRD group so the post-refresh Content emission is unambiguously distinguishable from
        // the pre-refresh one — proving the refresh round-trip actually completed (not just a
        // local flag flip), and that filteredGroups is recomputed against the STILL-ACTIVE
        // searchQuery rather than reset to the full list.
        repository.pagesByIndex = mapOf(
            0 to listOf(group("g1", "Alpha"), group("g2", "Beta"), group("g3", "Alphaville")),
        )
        viewModel.trySendAction(GroupListAction.OnRefresh)
        val state = viewModel.awaitState { it.groups.size == 3 }

        assertEquals("alpha", state.searchQuery)
        assertEquals(2, state.filteredGroups.size, "post-refresh filter must match against the active query")
        assertEquals(setOf("g1", "g3"), state.filteredGroups.map { it.id }.toSet())
    }

    @Test
    fun `OnClearSearch restores the full groups list and clears the query`() = runTest(testDispatcher) {
        val (_, viewModel) = buildViewModel(
            pages = mapOf(0 to listOf(group("g1", "Alpha"), group("g2", "Beta"))),
        )
        viewModel.awaitState { !it.isLoading }
        viewModel.trySendAction(GroupListAction.OnSearch("alpha"))
        viewModel.awaitState { it.searchQuery == "alpha" }

        viewModel.trySendAction(GroupListAction.OnClearSearch)
        val state = viewModel.awaitState { it.searchQuery == "" }

        assertEquals(2, state.filteredGroups.size)
    }

    // ─── OnCreateGroup / OnJoinGroup ────────────────────────────────────────

    @Test
    fun `OnCreateGroup emits NavigateToCreateGroup`() = runTest(testDispatcher) {
        val (_, viewModel) = buildViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(GroupListAction.OnCreateGroup)
            assertEquals(GroupListEvent.NavigateToCreateGroup, awaitItem())
        }
    }

    @Test
    fun `OnJoinGroup emits NavigateToJoinGroup`() = runTest(testDispatcher) {
        val (_, viewModel) = buildViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(GroupListAction.OnJoinGroup)
            assertEquals(GroupListEvent.NavigateToJoinGroup, awaitItem())
        }
    }

    // ─── OnRefresh / Retry ──────────────────────────────────────────────────

    @Test
    fun `OnRefresh re-dispatches a real fetch and clears isRefreshing once new data returns`() = runTest(testDispatcher) {
        val (repository, viewModel) = buildViewModel(pages = mapOf(0 to listOf(group("g1", "Alpha"))))
        viewModel.awaitState { it.groups.size == 1 }

        // Widen the fixture so the post-refresh Content emission is unambiguously distinct from
        // the pre-refresh one — proves OnRefresh re-dispatched a REAL fetch via GroupRepository,
        // not a local `isRefreshing` flag flip.
        repository.pagesByIndex = mapOf(0 to listOf(group("g1", "Alpha"), group("g2", "Beta")))
        viewModel.trySendAction(GroupListAction.OnRefresh)
        val state = viewModel.awaitState { it.groups.size == 2 }

        assertEquals(false, state.isRefreshing)
    }

    @Test
    fun `Retry clears the error and re-dispatches a real fetch that recovers via GroupRepository`() = runTest(testDispatcher) {
        val (repository, viewModel) = buildViewModel(failWith = RuntimeException("HTTP 500 boom"))
        viewModel.awaitState { it.error != null }
        assertEquals(GroupListError.Server, viewModel.stateFlow.value.error)

        // Clear the failure and seed content so a successful retry is unambiguously observable —
        // proves Retry re-dispatched a REAL fetch via GroupRepository, not a local flag flip.
        repository.failWith = null
        repository.pagesByIndex = mapOf(0 to listOf(group("g1", "Alpha")))
        viewModel.trySendAction(GroupListAction.Retry)
        val state = viewModel.awaitState { it.error == null && !it.isLoading }

        assertEquals(1, state.groups.size)
    }

    // ─── OnLoadMoreTap ──────────────────────────────────────────────────────

    @Test
    fun `OnLoadMoreTap appends the next page to groups`() = runTest(testDispatcher) {
        val (_, viewModel) = buildViewModel(
            pages = mapOf(
                0 to listOf(group("g1", "Alpha"), group("g2", "Beta")),
                1 to listOf(group("g3", "Gamma")),
            ),
            pageSize = 2,
        )
        viewModel.awaitState { it.groups.size == 2 }

        viewModel.trySendAction(GroupListAction.OnLoadMoreTap)
        val state = viewModel.awaitState { it.groups.size == 3 }

        assertEquals(setOf("g1", "g2", "g3"), state.groups.map { it.id }.toSet())
    }
}

// ---------------------------------------------------------------------------
// Fixtures
// ---------------------------------------------------------------------------

private fun group(id: String, name: String): Group = Group(
    id = id,
    name = name,
    groupType = GroupTypeSlug.VSLA,
    viewerRole = ViewerRole.ORGANIZER,
    cycleNumber = 1,
    memberCount = 20,
    lastMeetingDate = LocalDate(2026, 7, 14),
    healthIndicator = HealthIndicator.GREEN,
    overdueRate = 0.01,
    status = "ACTIVE",
    fineractCenterId = 1L,
)

/** Exception whose simple class name deliberately matches `categorize()`'s network-keyword scan. */
private class FakeConnectException(message: String) : Exception(message)

// ---------------------------------------------------------------------------
// Fakes — real Store5-backed PagingScreenStream (see class KDoc above for rationale).
// ---------------------------------------------------------------------------

/**
 * In-memory [GroupRepository] fake. Builds a REAL `Store<PageKey, List<Group>>` per call (an
 * in-memory [Fetcher] + [SourceOfTruth], no TTL [kpt.core.base.store.infra.DefaultValidator])
 * and exposes it via the same public `.asPagingScreenStream(...)` extension the production
 * `GroupRepositoryImpl` uses — so the ViewModel exercises the real offline-first paging
 * pipeline (Loading → Content/Empty/Error/NoNetwork/Unauthenticated, load-more, refresh).
 *
 * [pagesByIndex] and [failWith] are `var` so a test can mutate the fixture BETWEEN two
 * dispatches (e.g. before `OnRefresh`/`Retry`) and assert on the resulting, unambiguously
 * distinct post-refetch content rather than racing a `StateFlow`'s already-current value.
 */
private class FakeGroupRepository(
    pagesByIndex: Map<Int, List<Group>> = emptyMap(),
    failWith: Throwable? = null,
    online: Boolean = true,
    private val pageSize: Int = 2,
) : GroupRepository {

    var pagesByIndex: Map<Int, List<Group>> = pagesByIndex
    var failWith: Throwable? = failWith

    private val networkMonitor: NetworkMonitor = FakeNetworkMonitor(
        if (online) available() else NetworkStatus.Unavailable,
    )
    private val cache = mutableMapOf<Int, MutableStateFlow<List<Group>?>>()

    var fetchCallCount: Int = 0
        private set

    private fun flowFor(page: Int): MutableStateFlow<List<Group>?> =
        cache.getOrPut(page) { MutableStateFlow(null) }

    override fun groupsPagingStream(
        scope: CoroutineScope,
        fetchPolicy: FetchPolicy,
    ): PagingScreenStream<Group> {
        val store = StoreFactory.createStore<PageKey, List<Group>, List<Group>>(
            fetcher = Fetcher.of { key: PageKey ->
                fetchCallCount++
                failWith?.let { throw it }
                pagesByIndex[key.page] ?: emptyList()
            },
            sourceOfTruth = SourceOfTruth.of(
                reader = { key: PageKey -> flowFor(key.page) },
                writer = { key: PageKey, value: List<Group> -> flowFor(key.page).value = value },
                delete = { key: PageKey -> flowFor(key.page).value = null },
                deleteAll = { cache.values.forEach { it.value = null } },
            ),
        )
        return store.asPagingScreenStream(
            networkMonitor = networkMonitor,
            fetchedAtRepository = InMemoryFetchedAtRepository(),
            cacheKey = "test:group-list",
            scope = scope,
            pageSize = pageSize,
            fetchPolicy = fetchPolicy,
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
