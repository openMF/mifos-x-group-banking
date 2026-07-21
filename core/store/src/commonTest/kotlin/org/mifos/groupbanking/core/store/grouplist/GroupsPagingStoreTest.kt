/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.store.grouplist

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.base.store.paging.PageKey
import kpt.core.base.store.paging.StorePageResult
import kpt.core.base.store.paging.loadPage
import org.mifos.groupbanking.core.database.grouplist.dao.GroupListDao
import org.mifos.groupbanking.core.database.grouplist.entity.GroupListEntity
import org.mifos.groupbanking.core.network.model.GroupDto
import org.mifos.groupbanking.core.network.model.GroupPageDto
import org.mifos.groupbanking.core.network.model.GroupTypeDto
import org.mifos.groupbanking.core.network.model.HealthIndicatorDto
import org.mifos.groupbanking.core.network.model.ViewerRoleDto
import org.mifos.groupbanking.core.network.service.grouplist.GroupApi
import org.mifos.groupbanking.core.store.grouplist.impl.GroupListFetchException
import org.mifos.groupbanking.core.store.grouplist.impl.provideGroupsPagingStore
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD coverage for [provideGroupsPagingStore] — the PAGINATED NETWORK_WITH_CACHE group-list store.
 *
 * Exercises the Store5 paging read pipeline directly (per-page Fetcher + Room-shaped
 * SourceOfTruth + validator) with an in-memory [FakeGroupListDao] and a scripted
 * [FakeGroupApi]; no Room runtime and no NetworkMonitor needed at this layer (paging-stream UX +
 * offline behaviour is covered by the repository test).
 *
 * Key = [PageKey] (offset paging, `pageSize=20`); Value = `List<Group>` (one page slice).
 */
class GroupsPagingStoreTest {

    // ─── first page: cache-miss → fetch(offset=0) → persisted to SoT + emitted ───
    @Test
    fun first_page_cache_miss_calls_fetcher_persists_to_sot_and_emits() = runTest {
        val api = FakeGroupApi(pages = mapOf(0 to page(0, 2)))
        val dao = FakeGroupListDao()
        val store = provideGroupsPagingStore(api, dao)

        val result = store.loadPage(PageKey.first(pageSize = PAGE_SIZE), refresh = true)

        assertTrue(result is StorePageResult.Success)
        assertEquals(2, result.items.size)
        assertEquals(listOf(0 to 20), api.calls, "fetcher runs once for page 0 with offset 0")
        assertEquals(2, dao.rowsForPage(0).size, "fetched page must be written through to the SoT")
    }

    // ─── load-more: page 1 fetch(offset=20) → appended page persisted ────────────
    @Test
    fun load_more_fetches_next_page_at_offset() = runTest {
        val api = FakeGroupApi(pages = mapOf(0 to page(0, 20), 1 to page(20, 5)))
        val dao = FakeGroupListDao()
        val store = provideGroupsPagingStore(api, dao)

        store.loadPage(PageKey.first(pageSize = PAGE_SIZE), refresh = true)
        val second = store.loadPage(PageKey(page = 1, pageSize = PAGE_SIZE), refresh = true)

        assertTrue(second is StorePageResult.Success)
        assertEquals(5, second.items.size)
        assertEquals(listOf(0 to 20, 20 to 20), api.calls, "load-more must fetch page 1 at offset 20")
        assertEquals(20, dao.rowsForPage(0).size, "page 0 rows are retained")
        assertEquals(5, dao.rowsForPage(1).size, "page 1 rows persisted under their own pageIndex")
    }

    // ─── cache-hit: seeded page served from SoT, fetcher NOT called ──────────────
    @Test
    fun cache_hit_emits_from_sot_without_fetcher_call() = runTest {
        val api = FakeGroupApi(pages = mapOf(0 to page(0, 2)))
        val dao = FakeGroupListDao().apply { seedPage(0, entities(0, 2)) }
        val store = provideGroupsPagingStore(api, dao)

        val result = store.loadPage(PageKey.first(pageSize = PAGE_SIZE), refresh = false)

        assertTrue(result is StorePageResult.Success)
        assertEquals(2, result.items.size)
        assertEquals(emptyList(), api.calls, "cache hit must be served from the SoT without a fetch")
    }

    // ─── refresh: explicit fresh() re-drives the fetcher (SWR revalidation) ──────
    @Test
    fun refresh_refetches_first_page() = runTest {
        val api = FakeGroupApi(pages = mapOf(0 to page(0, 2)))
        val dao = FakeGroupListDao()
        val store = provideGroupsPagingStore(api, dao)

        store.loadPage(PageKey.first(pageSize = PAGE_SIZE), refresh = true)
        store.loadPage(PageKey.first(pageSize = PAGE_SIZE), refresh = true)

        assertEquals(2, api.calls.size, "each fresh() read must re-drive the fetcher")
    }

    // ─── error: fetcher failure surfaces on the Store5 error channel ─────────────
    @Test
    fun fetcher_error_surfaces_as_error_response() = runTest {
        val api = FakeGroupApi(error = NetworkError.SERVER)
        val dao = FakeGroupListDao()
        val store = provideGroupsPagingStore(api, dao)

        val response = store.stream(StoreReadRequest.fresh(PageKey.first(pageSize = PAGE_SIZE)))
            .first { it is StoreReadResponse.Error }

        val error = (response as StoreReadResponse.Error.Exception).error
        assertTrue(error is GroupListFetchException, "error channel must carry the typed fetch exception")
        assertEquals(NetworkError.SERVER, error.networkError)
    }

    // ─── S5-PAGE-ATOMIC: re-fetching a page atomically replaces its prior rows ───
    @Test
    fun page_refetch_replaces_prior_rows_atomically_no_staleness() = runTest {
        val api = FakeGroupApi(pages = mapOf(0 to page(0, 5)))
        val dao = FakeGroupListDao().apply { seedPage(0, entities(0, 20)) } // 20 stale rows
        val store = provideGroupsPagingStore(api, dao)

        store.loadPage(PageKey.first(pageSize = PAGE_SIZE), refresh = true)

        assertEquals(5, dao.rowsForPage(0).size, "replacePage must drop the 20 stale rows, keep only the 5 fresh")
        assertTrue(dao.replacePageWasAtomic, "page write must go through the atomic @Transaction replacePage")
    }

    private companion object {
        const val PAGE_SIZE = 20
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
    /** Recorded (offset, limit) pairs, in call order. */
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
    var replacePageWasAtomic: Boolean = false
        private set

    fun seedPage(pageIndex: Int, entities: List<GroupListEntity>) {
        rows.value = rows.value.filterNot { it.pageIndex == pageIndex } + entities
    }

    fun rowsForPage(pageIndex: Int): List<GroupListEntity> = rows.value.filter { it.pageIndex == pageIndex }

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
        replacePageWasAtomic = true
        deletePage(pageIndex)
        upsertAll(entities)
    }

    override suspend fun deleteOlderThan(epochMillis: Long) {
        rows.value = rows.value.filter { it.fetchedAt >= epochMillis }
    }
}
