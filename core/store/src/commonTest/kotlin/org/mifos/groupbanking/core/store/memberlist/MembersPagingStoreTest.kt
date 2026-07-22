/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.store.memberlist

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.base.store.paging.PageKey
import kpt.core.base.store.paging.StorePageResult
import kpt.core.base.store.paging.loadPage
import org.mifos.groupbanking.core.database.memberlist.dao.MemberListDao
import org.mifos.groupbanking.core.database.memberlist.entity.MemberListEntity
import org.mifos.groupbanking.core.network.model.LoanStatusDto
import org.mifos.groupbanking.core.network.model.MemberDto
import org.mifos.groupbanking.core.network.model.MemberPageDto
import org.mifos.groupbanking.core.network.model.MemberRoleDto
import org.mifos.groupbanking.core.network.service.memberlist.MemberApi
import org.mifos.groupbanking.core.store.memberlist.impl.MemberListFetchException
import org.mifos.groupbanking.core.store.memberlist.impl.provideMembersPagingStore
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD coverage for [provideMembersPagingStore] — the PAGINATED NETWORK_WITH_CACHE member-list store
 * (roster of clients belonging to a savings group, `GET /groups/{groupId}/clients`).
 *
 * Exercises the Store5 paging read pipeline directly (per-page Fetcher + Room-shaped
 * SourceOfTruth + validator) with an in-memory [FakeMemberListDao] and a scripted [FakeMemberApi];
 * no Room runtime and no NetworkMonitor needed at this layer (paging-stream UX + offline behaviour
 * is covered by the repository test).
 *
 * Key = [PageKey] with the groupId carried in [PageKey.query] (offset paging, `pageSize=20`) so the
 * cache is per-group AND per-page; Value = `List<Member>` (one page slice).
 */
class MembersPagingStoreTest {

    // ─── first page: cache-miss → fetch(groupId, offset=0) → persisted to SoT + emitted ───
    @Test
    fun first_page_cache_miss_calls_fetcher_persists_to_sot_and_emits() = runTest {
        val api = FakeMemberApi(pages = mapOf(key(GROUP_A, 0) to page(0, 2)))
        val dao = FakeMemberListDao()
        val store = provideMembersPagingStore(api, dao)

        val result = store.loadPage(pageKey(GROUP_A, 0), refresh = true)

        assertTrue(result is StorePageResult.Success)
        assertEquals(2, result.items.size)
        assertEquals(
            listOf(Triple(GROUP_A, 0, 20)),
            api.calls,
            "fetcher runs once for group A page 0 with offset 0",
        )
        assertEquals(2, dao.rowsForPage(GROUP_A, 0).size, "fetched page must be written through to the SoT")
    }

    // ─── load-more: page 1 fetch(offset=20) → appended page persisted ────────────
    @Test
    fun load_more_fetches_next_page_at_offset() = runTest {
        val api = FakeMemberApi(
            pages = mapOf(key(GROUP_A, 0) to page(0, 20), key(GROUP_A, 1) to page(20, 5)),
        )
        val dao = FakeMemberListDao()
        val store = provideMembersPagingStore(api, dao)

        store.loadPage(pageKey(GROUP_A, 0), refresh = true)
        val second = store.loadPage(pageKey(GROUP_A, 1), refresh = true)

        assertTrue(second is StorePageResult.Success)
        assertEquals(5, second.items.size)
        assertEquals(
            listOf(Triple(GROUP_A, 0, 20), Triple(GROUP_A, 20, 20)),
            api.calls,
            "load-more must fetch page 1 at offset 20",
        )
        assertEquals(20, dao.rowsForPage(GROUP_A, 0).size, "page 0 rows are retained")
        assertEquals(5, dao.rowsForPage(GROUP_A, 1).size, "page 1 rows persisted under their own pageIndex")
    }

    // ─── cache-hit: seeded page served from SoT, fetcher NOT called ──────────────
    @Test
    fun cache_hit_emits_from_sot_without_fetcher_call() = runTest {
        val api = FakeMemberApi(pages = mapOf(key(GROUP_A, 0) to page(0, 2)))
        val dao = FakeMemberListDao().apply { seedPage(GROUP_A, 0, entities(GROUP_A, 0, 2)) }
        val store = provideMembersPagingStore(api, dao)

        val result = store.loadPage(pageKey(GROUP_A, 0), refresh = false)

        assertTrue(result is StorePageResult.Success)
        assertEquals(2, result.items.size)
        assertEquals(emptyList(), api.calls, "cache hit must be served from the SoT without a fetch")
    }

    // ─── refresh: explicit fresh() re-drives the fetcher (SWR revalidation) ──────
    @Test
    fun refresh_refetches_first_page() = runTest {
        val api = FakeMemberApi(pages = mapOf(key(GROUP_A, 0) to page(0, 2)))
        val dao = FakeMemberListDao()
        val store = provideMembersPagingStore(api, dao)

        store.loadPage(pageKey(GROUP_A, 0), refresh = true)
        store.loadPage(pageKey(GROUP_A, 0), refresh = true)

        assertEquals(2, api.calls.size, "each fresh() read must re-drive the fetcher")
    }

    // ─── error: fetcher failure surfaces on the Store5 error channel ─────────────
    @Test
    fun fetcher_error_surfaces_as_error_response() = runTest {
        val api = FakeMemberApi(error = NetworkError.SERVER)
        val dao = FakeMemberListDao()
        val store = provideMembersPagingStore(api, dao)

        val response = store.stream(StoreReadRequest.fresh(pageKey(GROUP_A, 0)))
            .first { it is StoreReadResponse.Error }

        val error = (response as StoreReadResponse.Error.Exception).error
        assertTrue(error is MemberListFetchException, "error channel must carry the typed fetch exception")
        assertEquals(NetworkError.SERVER, error.networkError)
    }

    // ─── S5-PAGE-ATOMIC: re-fetching a page atomically replaces its prior rows ───
    @Test
    fun page_refetch_replaces_prior_rows_atomically_no_staleness() = runTest {
        val api = FakeMemberApi(pages = mapOf(key(GROUP_A, 0) to page(0, 5)))
        val dao = FakeMemberListDao().apply { seedPage(GROUP_A, 0, entities(GROUP_A, 0, 20)) } // 20 stale rows
        val store = provideMembersPagingStore(api, dao)

        store.loadPage(pageKey(GROUP_A, 0), refresh = true)

        assertEquals(5, dao.rowsForPage(GROUP_A, 0).size, "replacePage must drop the 20 stale rows, keep only the 5 fresh")
        assertTrue(dao.replacePageWasAtomic, "page write must go through the atomic @Transaction replacePage")
    }

    // ─── per-group isolation: group B's cache never collides with group A's ──────
    @Test
    fun distinct_groups_cache_pages_independently() = runTest {
        val api = FakeMemberApi(
            pages = mapOf(key(GROUP_A, 0) to page(0, 3), key(GROUP_B, 0) to page(0, 7)),
        )
        val dao = FakeMemberListDao()
        val store = provideMembersPagingStore(api, dao)

        store.loadPage(pageKey(GROUP_A, 0), refresh = true)
        store.loadPage(pageKey(GROUP_B, 0), refresh = true)

        assertEquals(3, dao.rowsForPage(GROUP_A, 0).size, "group A page 0 cached under its own groupId")
        assertEquals(7, dao.rowsForPage(GROUP_B, 0).size, "group B page 0 cached under its own groupId")
    }

    private companion object {
        const val PAGE_SIZE = 20
        const val GROUP_A = "grp-a"
        const val GROUP_B = "grp-b"

        fun pageKey(groupId: String, page: Int): PageKey =
            PageKey(page = page, pageSize = PAGE_SIZE, query = groupId)

        /** Fake-API map key: (groupId, pageIndex). */
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
    /** Recorded (groupId, offset, limit) triples, in call order. */
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
    var replacePageWasAtomic: Boolean = false
        private set

    fun seedPage(groupId: String, pageIndex: Int, entities: List<MemberListEntity>) {
        rows.value = rows.value.filterNot { it.groupId == groupId && it.pageIndex == pageIndex } + entities
    }

    fun rowsForPage(groupId: String, pageIndex: Int): List<MemberListEntity> =
        rows.value.filter { it.groupId == groupId && it.pageIndex == pageIndex }

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
        replacePageWasAtomic = true
        deletePage(groupId, pageIndex)
        upsertAll(entities)
    }

    override suspend fun deleteOlderThan(epochMillis: Long) {
        rows.value = rows.value.filter { it.fetchedAt >= epochMillis }
    }
}
