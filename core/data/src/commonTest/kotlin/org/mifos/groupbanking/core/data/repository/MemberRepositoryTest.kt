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
import org.mifos.groupbanking.core.database.memberlist.dao.MemberListDao
import org.mifos.groupbanking.core.database.memberlist.entity.MemberListEntity
import org.mifos.groupbanking.core.network.model.LoanStatusDto
import org.mifos.groupbanking.core.network.model.MemberDto
import org.mifos.groupbanking.core.network.model.MemberPageDto
import org.mifos.groupbanking.core.network.model.MemberRoleDto
import org.mifos.groupbanking.core.network.service.memberlist.MemberApi
import org.mifos.groupbanking.core.store.memberlist.impl.provideMembersPagingStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * TDD coverage for [MemberRepositoryImpl] — verifies the repository surfaces the PAGINATED
 * member-list store as an offline-first [kpt.core.base.store.paging.PagingScreenStream] via
 * `.asPagingScreenStream()`, mapping cached pages into `ScreenState.Content<List<Member>>`,
 * appending on load-more, and serving cached pages offline. The groupId is threaded through the
 * store's [kpt.core.base.store.paging.PageKey] `query` so the cache is per-group.
 */
class MemberRepositoryTest {

    @Test
    fun paging_stream_emits_Content_with_first_page_when_online() = runTest {
        val api = FakeMemberApi(pages = mapOf(key(GROUP, 0) to page(0, 3)))
        val dao = FakeMemberListDao()
        val repo = repository(api, dao, online = true)

        val state = repo.membersPagingStream(groupId = GROUP, scope = backgroundScope)
            .state.first { it is ScreenState.Content }

        val data = (state as ScreenState.Content).data
        assertEquals(3, data.size)
        assertEquals("m0", data.first().id)
    }

    @Test
    fun load_next_page_appends_second_page() = runTest {
        val api = FakeMemberApi(
            pages = mapOf(key(GROUP, 0) to page(0, 20), key(GROUP, 1) to page(20, 5)),
        )
        val dao = FakeMemberListDao()
        val repo = repository(api, dao, online = true)

        val stream = repo.membersPagingStream(groupId = GROUP, scope = backgroundScope)
        stream.state.first { it is ScreenState.Content && it.data.size == 20 }

        stream.loadNextPage()
        val appended = stream.state.first { it is ScreenState.Content && (it).data.size == 25 }

        assertEquals(25, (appended as ScreenState.Content).data.size, "load-more appends the second page")
    }

    @Test
    fun paging_stream_serves_cached_first_page_offline() = runTest {
        val api = FakeMemberApi(error = NetworkError.SERVER)
        val dao = FakeMemberListDao().apply { seedPage(GROUP, 0, entities(GROUP, 0, 4)) }
        val repo = repository(api, dao, online = false)

        val state = repo.membersPagingStream(
            groupId = GROUP,
            scope = backgroundScope,
            fetchPolicy = FetchPolicy.CACHE_ONLY,
        ).state.first { it is ScreenState.Content }

        val data = (state as ScreenState.Content).data
        assertEquals(4, data.size, "offline read must serve the persisted pages (offline: show_cached)")
        assertEquals(emptyList(), api.calls, "CACHE_ONLY must not hit the network")
    }

    // ─── wiring ──────────────────────────────────────────────────────────────

    private fun repository(
        api: MemberApi,
        dao: MemberListDao,
        online: Boolean,
    ): MemberRepository {
        val initial = if (online) {
            NetworkStatus.Available(NetworkInfo(type = NetworkType.WiFi, isMetered = false))
        } else {
            NetworkStatus.Unavailable
        }
        return MemberRepositoryImpl(
            membersPagingStore = provideMembersPagingStore(api, dao),
            networkMonitor = FakeNetworkMonitor(initial),
            fetchedAtRepository = InMemoryFetchedAtRepository(),
        )
    }

    private companion object {
        const val GROUP = "grp-a"

        fun key(groupId: String, pageIndex: Int): Pair<String, Int> = groupId to pageIndex
    }
}

// ---------------------------------------------------------------------------
// Fakes + fixtures
// ---------------------------------------------------------------------------

private fun page(offset: Int, count: Int): MemberPageDto = MemberPageDto(
    totalFilteredRecords = 100,
    pageItems = (0 until count).map { i -> memberDto(offset + i) },
)

private fun memberDto(n: Int): MemberDto = MemberDto(
    id = "m$n",
    fineractClientId = n.toLong(),
    displayName = "Member $n",
    photoUri = null,
    role = MemberRoleDto.MEMBER,
    savingsBalance = 100.0 + n,
    loanStatus = LoanStatusDto.NONE,
)

private fun entities(groupId: String, pageIndex: Int, count: Int): List<MemberListEntity> =
    (0 until count).map { i ->
        MemberListEntity(
            groupId = groupId,
            memberId = "seed-$groupId-$pageIndex-$i",
            pageIndex = pageIndex,
            rowOrder = i,
            fineractClientId = i.toLong(),
            displayName = "Seed $i",
            photoUri = null,
            role = "MEMBER",
            savingsBalance = 100.0,
            loanStatus = "NONE",
            fetchedAt = 1L,
        )
    }

private class FakeMemberApi(
    private val pages: Map<Pair<String, Int>, MemberPageDto> = emptyMap(),
    private val error: NetworkError? = null,
) : MemberApi {
    val calls = mutableListOf<Triple<String, Int, Int>>()

    override suspend fun getGroupMembers(
        groupId: String,
        limit: Int,
        offset: Int,
    ): NetworkResult<MemberPageDto, NetworkError> {
        calls += Triple(groupId, offset, limit)
        error?.let { return NetworkResult.Error(it) }
        val pageIndex = offset / limit
        return NetworkResult.Success(pages[groupId to pageIndex] ?: MemberPageDto(totalFilteredRecords = 0))
    }
}

private class FakeMemberListDao : MemberListDao {
    private val rows = MutableStateFlow<List<MemberListEntity>>(emptyList())

    fun seedPage(groupId: String, pageIndex: Int, entities: List<MemberListEntity>) {
        rows.value = rows.value.filterNot { it.groupId == groupId && it.pageIndex == pageIndex } + entities
    }

    override fun observePage(groupId: String, pageIndex: Int): Flow<List<MemberListEntity>> =
        rows.map { all ->
            all.filter { it.groupId == groupId && it.pageIndex == pageIndex }.sortedBy { it.rowOrder }
        }

    override fun observeGroup(groupId: String): Flow<List<MemberListEntity>> =
        rows.map { all ->
            all.filter { it.groupId == groupId }.sortedWith(compareBy({ it.pageIndex }, { it.rowOrder }))
        }

    override suspend fun upsertAll(entities: List<MemberListEntity>) {
        val byKey = rows.value.associateBy { it.groupId to it.memberId }.toMutableMap()
        entities.forEach { byKey[it.groupId to it.memberId] = it }
        rows.value = byKey.values.toList()
    }

    override suspend fun deletePage(groupId: String, pageIndex: Int) {
        rows.value = rows.value.filterNot { it.groupId == groupId && it.pageIndex == pageIndex }
    }

    override suspend fun deleteGroup(groupId: String) {
        rows.value = rows.value.filterNot { it.groupId == groupId }
    }

    override suspend fun deleteAll() { rows.value = emptyList() }

    override suspend fun replacePage(groupId: String, pageIndex: Int, entities: List<MemberListEntity>) {
        deletePage(groupId, pageIndex)
        upsertAll(entities)
    }

    override suspend fun deleteOlderThan(epochMillis: Long) {
        rows.value = rows.value.filter { it.fetchedAt >= epochMillis }
    }
}
