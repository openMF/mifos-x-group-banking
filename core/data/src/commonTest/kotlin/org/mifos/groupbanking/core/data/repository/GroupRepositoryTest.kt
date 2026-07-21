/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.data.repository

import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkChangeEvent
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkInfo
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitor
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkStatus
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.base.store.infra.FetchedAtRepository
import kpt.core.base.store.screen.FetchPolicy
import kpt.core.base.store.screen.ScreenState
import org.mifos.groupbanking.core.database.grouplist.dao.GroupListDao
import org.mifos.groupbanking.core.database.grouplist.entity.GroupListEntity
import org.mifos.groupbanking.core.network.model.GroupDto
import org.mifos.groupbanking.core.network.model.GroupPageDto
import org.mifos.groupbanking.core.network.model.GroupTypeDto
import org.mifos.groupbanking.core.network.model.HealthIndicatorDto
import org.mifos.groupbanking.core.network.model.ViewerRoleDto
import org.mifos.groupbanking.core.network.service.grouplist.GroupApi
import org.mifos.groupbanking.core.store.grouplist.impl.provideGroupsPagingStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * TDD coverage for [GroupRepositoryImpl] — verifies the repository surfaces the PAGINATED store as
 * an offline-first [kpt.core.base.store.paging.PagingScreenStream] via `.asPagingScreenStream()`,
 * mapping cached pages into `ScreenState.Content<List<Group>>`, appending on load-more, and
 * serving cached pages offline.
 */
class GroupRepositoryTest {

    @Test
    fun paging_stream_emits_Content_with_first_page_when_online() = runTest {
        val api = FakeGroupApi(pages = mapOf(0 to page(0, 3)))
        val dao = FakeGroupListDao()
        val repo = repository(api, dao, online = true)

        val state = repo.groupsPagingStream(scope = backgroundScope)
            .state.first { it is ScreenState.Content }

        val data = (state as ScreenState.Content).data
        assertEquals(3, data.size)
        assertEquals("g0", data.first().id)
    }

    @Test
    fun load_next_page_appends_second_page() = runTest {
        val api = FakeGroupApi(pages = mapOf(0 to page(0, 20), 1 to page(20, 5)))
        val dao = FakeGroupListDao()
        val repo = repository(api, dao, online = true)

        val stream = repo.groupsPagingStream(scope = backgroundScope)
        stream.state.first { it is ScreenState.Content && it.data.size == 20 }

        stream.loadNextPage()
        val appended = stream.state.first { it is ScreenState.Content && (it).data.size == 25 }

        assertEquals(25, (appended as ScreenState.Content).data.size, "load-more appends the second page")
    }

    @Test
    fun paging_stream_serves_cached_first_page_offline() = runTest {
        val api = FakeGroupApi(error = NetworkError.SERVER)
        val dao = FakeGroupListDao().apply { seedPage(0, entities(0, 4)) }
        val repo = repository(api, dao, online = false)

        val state = repo.groupsPagingStream(
            scope = backgroundScope,
            fetchPolicy = FetchPolicy.CACHE_ONLY,
        ).state.first { it is ScreenState.Content }

        val data = (state as ScreenState.Content).data
        assertEquals(4, data.size, "offline read must serve the persisted pages (offline: show_cached)")
        assertEquals(emptyList(), api.calls, "CACHE_ONLY must not hit the network")
    }

    // ─── wiring ──────────────────────────────────────────────────────────────

    private fun repository(
        api: GroupApi,
        dao: GroupListDao,
        online: Boolean,
    ): GroupRepository {
        val initial = if (online) {
            NetworkStatus.Available(NetworkInfo(type = NetworkType.WiFi, isMetered = false))
        } else {
            NetworkStatus.Unavailable
        }
        return GroupRepositoryImpl(
            groupsPagingStore = provideGroupsPagingStore(api, dao),
            networkMonitor = FakeNetworkMonitor(initial),
            fetchedAtRepository = InMemoryFetchedAtRepository(),
        )
    }
}

// ---------------------------------------------------------------------------
// Fakes + fixtures
// ---------------------------------------------------------------------------

private fun page(offset: Int, count: Int): GroupPageDto = GroupPageDto(
    totalFilteredRecords = 100,
    pageItems = (0 until count).map { i -> groupDto(offset + i) },
)

private fun groupDto(n: Int): GroupDto = GroupDto(
    id = "g$n",
    name = "Group $n",
    groupType = GroupTypeDto.VSLA,
    viewerRole = ViewerRoleDto.ORGANIZER,
    cycleNumber = 1,
    memberCount = 20,
    lastMeetingDate = "2026-07-14",
    healthIndicator = HealthIndicatorDto.GREEN,
    overdueRate = 0.01,
    status = "ACTIVE",
    fineractCenterId = n.toLong(),
)

private fun entities(pageIndex: Int, count: Int): List<GroupListEntity> = (0 until count).map { i ->
    GroupListEntity(
        groupId = "seed-$pageIndex-$i",
        pageIndex = pageIndex,
        rowOrder = i,
        name = "Seed $i",
        groupType = "VSLA",
        viewerRole = "ORGANIZER",
        cycleNumber = 1,
        memberCount = 20,
        lastMeetingDate = "2026-07-14",
        overdueRate = 0.01,
        status = "ACTIVE",
        fineractCenterId = i.toLong(),
        fetchedAt = 1L,
    )
}

private class FakeGroupApi(
    private val pages: Map<Int, GroupPageDto> = emptyMap(),
    private val error: NetworkError? = null,
) : GroupApi {
    val calls = mutableListOf<Pair<Int, Int>>()

    override suspend fun getMyGroups(
        paged: Boolean,
        limit: Int,
        offset: Int,
    ): NetworkResult<GroupPageDto, NetworkError> {
        calls += offset to limit
        error?.let { return NetworkResult.Error(it) }
        val pageIndex = offset / limit
        return NetworkResult.Success(pages[pageIndex] ?: GroupPageDto(totalFilteredRecords = 0))
    }
}

private class FakeGroupListDao : GroupListDao {
    private val rows = MutableStateFlow<List<GroupListEntity>>(emptyList())

    fun seedPage(pageIndex: Int, entities: List<GroupListEntity>) {
        rows.value = rows.value.filterNot { it.pageIndex == pageIndex } + entities
    }

    override fun observePage(pageIndex: Int): Flow<List<GroupListEntity>> =
        rows.map { all -> all.filter { it.pageIndex == pageIndex }.sortedBy { it.rowOrder } }

    override fun observeAll(): Flow<List<GroupListEntity>> =
        rows.map { all -> all.sortedWith(compareBy({ it.pageIndex }, { it.rowOrder })) }

    override suspend fun upsertAll(entities: List<GroupListEntity>) {
        val byKey = rows.value.associateBy { it.groupId }.toMutableMap()
        entities.forEach { byKey[it.groupId] = it }
        rows.value = byKey.values.toList()
    }

    override suspend fun deletePage(pageIndex: Int) {
        rows.value = rows.value.filterNot { it.pageIndex == pageIndex }
    }

    override suspend fun deleteAll() { rows.value = emptyList() }

    override suspend fun replacePage(pageIndex: Int, entities: List<GroupListEntity>) {
        deletePage(pageIndex)
        upsertAll(entities)
    }

    override suspend fun deleteOlderThan(epochMillis: Long) {
        rows.value = rows.value.filter { it.fetchedAt >= epochMillis }
    }
}

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
    override suspend fun write(storeKey: String, instant: Instant) { map[storeKey] = instant }
}
