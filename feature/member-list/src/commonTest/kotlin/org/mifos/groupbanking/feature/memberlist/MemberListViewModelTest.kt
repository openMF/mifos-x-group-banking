/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.memberlist

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
import org.mifos.groupbanking.core.data.repository.MemberRepository
import org.mifos.groupbanking.core.model.Group
import org.mifos.groupbanking.core.model.GroupTypeSlug
import org.mifos.groupbanking.core.model.HealthIndicator
import org.mifos.groupbanking.core.model.LoanStatus
import org.mifos.groupbanking.core.model.Member
import org.mifos.groupbanking.core.model.MemberRole
import org.mifos.groupbanking.core.model.ViewerRole
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

private const val GROUP_ID = "group-1"

/**
 * See API.md#viewmodel — MemberListViewModelTest exercises every declared [MemberListAction]
 * path plus the [kpt.core.base.store.screen.ScreenState]->[MemberListState] mapping, per
 * RULE-TDD-METHODOLOGY-001 / RULE-IMPL-DEAD-CLICKABLE-001.
 *
 * [PagingScreenStream] has an `internal` constructor (core-base:store module-private), so this
 * suite builds REAL Store5-backed fakes ([FakeMemberRepository], [FakeGroupRepository]) via the
 * public `Store<PageKey, List<T>>.asPagingScreenStream(...)` extension — mirrors
 * `GroupListViewModelTest`'s identical approach, scaled to two repositories since
 * `MemberListViewModel` also does a best-effort `GroupRepository` groupName lookup (see that
 * class's KDoc).
 *
 * Every assertion that depends on a paging stream settling uses [awaitState] (a real suspending
 * `Flow.first { predicate }` hopped onto [Dispatchers.Default]) rather than
 * `advanceUntilIdle()` + a fire-and-forget `.value` read — Store5 drives its own coroutine
 * machinery independent of the `StandardTestDispatcher` virtual clock. Each created ViewModel's
 * `viewModelScope` is cancelled in [tearDown].
 */
class MemberListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val createdViewModels = mutableListOf<MemberListViewModel>()

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
        pages: Map<Int, List<Member>> = emptyMap(),
        failWith: Throwable? = null,
        online: Boolean = true,
        pageSize: Int = 2,
        groups: List<Group> = emptyList(),
        groupId: String = GROUP_ID,
    ): Triple<FakeMemberRepository, FakeGroupRepository, MemberListViewModel> {
        val memberRepository = FakeMemberRepository(
            pagesByIndex = pages,
            failWith = failWith,
            online = online,
            pageSize = pageSize,
        )
        val groupRepository = FakeGroupRepository(groups = groups)
        val viewModel = MemberListViewModel(
            memberRepository = memberRepository,
            groupRepository = groupRepository,
            crashReporter = ConsoleCrashReporter(),
            analytics = KptAnalyticsTracker(NoOpAnalyticsHelper()),
            groupId = groupId,
        )
        createdViewModels += viewModel
        return Triple(memberRepository, groupRepository, viewModel)
    }

    /** Suspends until [MemberListState] satisfies [predicate] — see class KDoc for rationale. */
    private suspend fun MemberListViewModel.awaitState(
        timeoutMs: Long = 5_000,
        predicate: (MemberListState) -> Boolean,
    ): MemberListState = withContext(Dispatchers.Default) {
        withTimeout(timeoutMs) { stateFlow.first(predicate) }
    }

    // ─── initial state + stream mapping ─────────────────────────────────────

    @Test
    fun `initial state is loading with empty members, seeded groupId, and no error`() = runTest(testDispatcher) {
        val (_, _, viewModel) = buildViewModel()

        val state = viewModel.stateFlow.value
        assertTrue(state.isLoading)
        assertNull(state.error)
        assertTrue(state.members.isEmpty())
        assertEquals(GROUP_ID, state.groupId)
        assertEquals(0, state.currentOffset)
    }

    @Test
    fun `stream Content maps to members, currentOffset, and clears loading`() = runTest(testDispatcher) {
        val (_, _, viewModel) = buildViewModel(
            pages = mapOf(0 to listOf(member("m1", "Amina Wanjiru"), member("m2", "Joseph Kamau"))),
        )

        val state = viewModel.awaitState { !it.isLoading }
        assertNull(state.error)
        assertEquals(2, state.members.size)
        assertEquals(2, state.currentOffset)
        assertEquals(MemberListScreenState.Content, state.screenState)
    }

    @Test
    fun `stream Empty maps to empty members and Empty screenState`() = runTest(testDispatcher) {
        val (_, _, viewModel) = buildViewModel(pages = mapOf(0 to emptyList()))

        val state = viewModel.awaitState { !it.isLoading }
        assertNull(state.error)
        assertTrue(state.members.isEmpty())
        assertEquals(0, state.currentOffset)
        assertEquals(MemberListScreenState.Empty, state.screenState)
    }

    @Test
    fun `stream connectivity failure maps to Network error`() = runTest(testDispatcher) {
        val (_, _, viewModel) = buildViewModel(failWith = FakeConnectException("connect failed"))

        val state = viewModel.awaitState { it.error != null }
        assertEquals(MemberListError.Network, state.error)
        assertFalse(state.isLoading)
        assertTrue(state.error?.retry == true)
        assertEquals(MemberListScreenState.Error, state.screenState)
    }

    @Test
    fun `stream 401 maps to Auth error and emits the closest declared event (ShowSnackbar)`() = runTest(testDispatcher) {
        val (_, _, viewModel) = buildViewModel(failWith = RuntimeException("HTTP 401 Unauthorized"))

        viewModel.eventFlow.test {
            val event = awaitItem()
            assertTrue(event is MemberListEvent.ShowSnackbar)
            assertEquals(MemberListError.Auth.messageKey, event.message)
        }
        val state = viewModel.awaitState { it.error != null }
        assertEquals(MemberListError.Auth, state.error)
        assertEquals(false, state.error?.retry)
    }

    @Test
    fun `stream 500 maps to Server error`() = runTest(testDispatcher) {
        val (_, _, viewModel) = buildViewModel(failWith = RuntimeException("HTTP 500 Internal Server Error"))

        val state = viewModel.awaitState { it.error != null }
        assertEquals(MemberListError.Server, state.error)
        assertTrue(state.error?.retry == true)
    }

    // ─── OnMemberClick ────────────────────────────────────────────────────────

    @Test
    fun `OnMemberClick emits NavigateToMemberProfile with memberId and groupId`() = runTest(testDispatcher) {
        val (_, _, viewModel) = buildViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(MemberListAction.OnMemberClick(memberId = "m1"))
            assertEquals(MemberListEvent.NavigateToMemberProfile("m1", GROUP_ID), awaitItem())
        }
    }

    // ─── OnAddMember ──────────────────────────────────────────────────────────

    @Test
    fun `OnAddMember emits NavigateToAddMember with groupId`() = runTest(testDispatcher) {
        val (_, _, viewModel) = buildViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(MemberListAction.OnAddMember)
            assertEquals(MemberListEvent.NavigateToAddMember(GROUP_ID), awaitItem())
        }
    }

    // ─── OnInviteMember ────────────────────────────────────────────────────────

    @Test
    fun `OnInviteMember emits NavigateToMemberInvite with groupId`() = runTest(testDispatcher) {
        val (_, _, viewModel) = buildViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(MemberListAction.OnInviteMember)
            assertEquals(MemberListEvent.NavigateToMemberInvite(GROUP_ID), awaitItem())
        }
    }

    // ─── OnBack ───────────────────────────────────────────────────────────────

    @Test
    fun `OnBack emits the flagged NavigateBack event`() = runTest(testDispatcher) {
        val (_, _, viewModel) = buildViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(MemberListAction.OnBack)
            assertEquals(MemberListEvent.NavigateBack, awaitItem())
        }
    }

    // ─── OnRefresh / Retry ────────────────────────────────────────────────────

    @Test
    fun `OnRefresh re-dispatches a real fetch and clears isRefreshing once new data returns`() = runTest(testDispatcher) {
        val (repository, _, viewModel) = buildViewModel(pages = mapOf(0 to listOf(member("m1", "Amina"))))
        viewModel.awaitState { it.members.size == 1 }

        // Widen the fixture so the post-refresh Content emission is unambiguously distinct from
        // the pre-refresh one — proves OnRefresh re-dispatched a REAL fetch via MemberRepository.
        repository.pagesByIndex = mapOf(0 to listOf(member("m1", "Amina"), member("m2", "Joseph")))
        viewModel.trySendAction(MemberListAction.OnRefresh)
        val state = viewModel.awaitState { it.members.size == 2 }

        assertEquals(false, state.isRefreshing)
    }

    @Test
    fun `Retry clears the error and re-dispatches a real fetch that recovers via MemberRepository`() = runTest(testDispatcher) {
        val (repository, _, viewModel) = buildViewModel(failWith = RuntimeException("HTTP 500 boom"))
        viewModel.awaitState { it.error != null }
        assertEquals(MemberListError.Server, viewModel.stateFlow.value.error)

        repository.failWith = null
        repository.pagesByIndex = mapOf(0 to listOf(member("m1", "Amina")))
        viewModel.trySendAction(MemberListAction.Retry)
        val state = viewModel.awaitState { it.error == null && !it.isLoading }

        assertEquals(1, state.members.size)
    }

    // ─── OnLoadMore ───────────────────────────────────────────────────────────

    @Test
    fun `OnLoadMore appends the next page and updates currentOffset`() = runTest(testDispatcher) {
        val (_, _, viewModel) = buildViewModel(
            pages = mapOf(
                0 to listOf(member("m1", "Amina"), member("m2", "Joseph")),
                1 to listOf(member("m3", "Grace")),
            ),
            pageSize = 2,
        )
        viewModel.awaitState { it.members.size == 2 }

        viewModel.trySendAction(MemberListAction.OnLoadMore)
        val state = viewModel.awaitState { it.members.size == 3 }

        assertEquals(setOf("m1", "m2", "m3"), state.members.map { it.id }.toSet())
        assertEquals(3, state.currentOffset)
    }

    @Test
    fun `hasMorePages flips false once the final page is exhausted`() = runTest(testDispatcher) {
        val (_, _, viewModel) = buildViewModel(
            pages = mapOf(0 to listOf(member("m1", "Amina"))),
            pageSize = 2,
        )

        // Predicate on BOTH fields settling — `isLoading` and `hasMorePages` are updated by two
        // independent collectors (`pagingStream.state` vs `pagingStream.hasMore`) funnelled
        // through separate Internal actions, so waiting on `isLoading` alone could observe a
        // stale default `hasMorePages=true` if that action hasn't drained yet.
        val state = viewModel.awaitState { !it.isLoading && !it.hasMorePages }
        assertEquals(false, state.hasMorePages)
    }

    // ─── groupName resolution (best-effort GroupRepository lookup) ────────────

    @Test
    fun `groupName resolves from the cached GroupRepository match`() = runTest(testDispatcher) {
        val (_, _, viewModel) = buildViewModel(
            groups = listOf(group(GROUP_ID, "Mwangaza Women's Group"), group("group-2", "Other Group")),
        )

        val state = viewModel.awaitState { it.groupName.isNotEmpty() }
        assertEquals("Mwangaza Women's Group", state.groupName)
    }

    @Test
    fun `groupName stays empty when no cached group matches (documented gap fallback)`() = runTest(testDispatcher) {
        val (_, _, viewModel) = buildViewModel(groups = listOf(group("some-other-group", "Unrelated")))
        viewModel.awaitState { !it.isLoading }

        assertEquals("", viewModel.stateFlow.value.groupName)
    }
}

// ---------------------------------------------------------------------------
// Fixtures
// ---------------------------------------------------------------------------

private fun member(id: String, name: String): Member = Member(
    id = id,
    fineractClientId = id.hashCode().toLong(),
    displayName = name,
    photoUri = null,
    role = MemberRole.MEMBER,
    savingsBalance = 1000.0,
    loanStatus = LoanStatus.NONE,
)

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
 * In-memory [MemberRepository] fake. Builds a REAL `Store<PageKey, List<Member>>` per call (an
 * in-memory [Fetcher] + [SourceOfTruth]) and exposes it via the same public
 * `.asPagingScreenStream(...)` extension the production `MemberRepositoryImpl` uses.
 */
private class FakeMemberRepository(
    pagesByIndex: Map<Int, List<Member>> = emptyMap(),
    failWith: Throwable? = null,
    online: Boolean = true,
    private val pageSize: Int = 2,
) : MemberRepository {

    var pagesByIndex: Map<Int, List<Member>> = pagesByIndex
    var failWith: Throwable? = failWith

    private val networkMonitor: NetworkMonitor = FakeNetworkMonitor(
        if (online) available() else NetworkStatus.Unavailable,
    )
    private val cache = mutableMapOf<Int, MutableStateFlow<List<Member>?>>()

    private fun flowFor(page: Int): MutableStateFlow<List<Member>?> =
        cache.getOrPut(page) { MutableStateFlow(null) }

    override fun membersPagingStream(
        groupId: String,
        scope: CoroutineScope,
        fetchPolicy: FetchPolicy,
    ): PagingScreenStream<Member> {
        val store = StoreFactory.createStore<PageKey, List<Member>, List<Member>>(
            fetcher = Fetcher.of { key: PageKey ->
                failWith?.let { throw it }
                pagesByIndex[key.page] ?: emptyList()
            },
            sourceOfTruth = SourceOfTruth.of(
                reader = { key: PageKey -> flowFor(key.page) },
                writer = { key: PageKey, value: List<Member> -> flowFor(key.page).value = value },
                delete = { key: PageKey -> flowFor(key.page).value = null },
                deleteAll = { cache.values.forEach { it.value = null } },
            ),
        )
        return store.asPagingScreenStream(
            networkMonitor = networkMonitor,
            fetchedAtRepository = InMemoryFetchedAtRepository(),
            cacheKey = "test:member-list:$groupId",
            scope = scope,
            pageSize = pageSize,
            fetchPolicy = fetchPolicy,
        )
    }
}

/**
 * In-memory [GroupRepository] fake backing the groupName best-effort lookup — a single-page,
 * always-online Store5-backed stream seeded with [groups].
 */
private class FakeGroupRepository(private val groups: List<Group>) : GroupRepository {
    private val networkMonitor: NetworkMonitor = FakeNetworkMonitor(available())
    private val cache = mutableMapOf<Int, MutableStateFlow<List<Group>?>>()

    private fun flowFor(page: Int): MutableStateFlow<List<Group>?> =
        cache.getOrPut(page) { MutableStateFlow(null) }

    override fun groupsPagingStream(
        scope: CoroutineScope,
        fetchPolicy: FetchPolicy,
    ): PagingScreenStream<Group> {
        val store = StoreFactory.createStore<PageKey, List<Group>, List<Group>>(
            fetcher = Fetcher.of { key: PageKey -> if (key.page == 0) groups else emptyList() },
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
            cacheKey = "test:member-list:group-lookup",
            scope = scope,
            pageSize = 20,
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
