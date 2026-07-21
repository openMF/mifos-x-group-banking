/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.store.personaldashboard

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.database.personaldashboard.dao.MemberDashboardDao
import org.mifos.groupbanking.core.database.personaldashboard.entity.MemberDashboardCacheEntity
import org.mifos.groupbanking.core.model.MemberDashboard
import org.mifos.groupbanking.core.network.model.GroupSummaryDto
import org.mifos.groupbanking.core.network.model.MemberDashboardResponseDto
import org.mifos.groupbanking.core.network.model.SavingsMechanismDto
import org.mifos.groupbanking.core.network.model.SavingsTransactionDto
import org.mifos.groupbanking.core.network.model.TransactionTypeDto
import org.mifos.groupbanking.core.network.service.personaldashboard.MemberDashboardApi
import org.mifos.groupbanking.core.store.personaldashboard.impl.MEMBER_DASHBOARD_DEFAULT_KEY
import org.mifos.groupbanking.core.store.personaldashboard.impl.MemberDashboardFetchException
import org.mifos.groupbanking.core.store.personaldashboard.impl.provideMemberDashboardStore
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * TDD coverage for [provideMemberDashboardStore] — the dynamic-key NETWORK_WITH_CACHE
 * member-dashboard store.
 *
 * Exercises the Store5 read pipeline directly (fetcher + Room-shaped SourceOfTruth + validator)
 * with an in-memory [FakeMemberDashboardDao] and a scripted [FakeMemberDashboardApi]; no Room
 * runtime and no NetworkMonitor needed at this layer (that is covered by the repository test).
 * The dynamic-key axis is asserted explicitly: `null` selectedGroupId → `__default__` key, a
 * specific group id → its own key, each cached in its own row.
 */
class MemberDashboardStoreTest {

    // ─── dynamic key: null selectedGroupId → "__default__" key, fetched + persisted ──────────
    @Test
    fun default_key_cache_miss_calls_fetcher_with_null_group_persists_and_emits() = runTest {
        val api = FakeMemberDashboardApi(NetworkResult.Success(dashboardDto(memberName = "Asha")))
        val dao = FakeMemberDashboardDao()
        val store = provideMemberDashboardStore(api, dao)

        val data = store.awaitFreshData(MEMBER_DASHBOARD_DEFAULT_KEY)

        assertEquals("Asha", data.memberName)
        assertEquals(1, api.callCount, "fetcher must run exactly once on cache miss")
        assertNull(api.lastSelectedGroupId, "default key must decode to a null selectedGroupId (first group)")
        assertEquals(1, dao.currentRows().size, "fetched dashboard must be written through to the SoT")
        assertEquals(MEMBER_DASHBOARD_DEFAULT_KEY, dao.currentRows().first().cacheKey)
    }

    // ─── dynamic key: specific group id caches under its own key + forwards the id ───────────
    @Test
    fun specific_group_key_fetches_that_group_and_caches_per_key() = runTest {
        val api = FakeMemberDashboardApi(NetworkResult.Success(dashboardDto(memberName = "Bea")))
        val dao = FakeMemberDashboardDao()
        val store = provideMemberDashboardStore(api, dao)

        store.awaitFreshData("group-42")

        assertEquals("group-42", api.lastSelectedGroupId, "non-default key must be forwarded as the selectedGroupId")
        assertEquals("group-42", dao.currentRows().single().cacheKey, "the group must be cached under its own key")
    }

    // ─── cache-hit → SoT emit, fetcher NOT called ────────────────────────────────────────────
    @Test
    fun cache_hit_emits_from_sot_without_fetcher_call() = runTest {
        val api = FakeMemberDashboardApi(NetworkResult.Success(dashboardDto(memberName = "net")))
        val dao = FakeMemberDashboardDao().apply { seed(cacheEntity(MEMBER_DASHBOARD_DEFAULT_KEY, memberName = "cached")) }
        val store = provideMemberDashboardStore(api, dao)

        val response = store.stream(StoreReadRequest.cached(MEMBER_DASHBOARD_DEFAULT_KEY, refresh = false))
            .first { it is StoreReadResponse.Data<*> }

        @Suppress("UNCHECKED_CAST")
        val data = (response as StoreReadResponse.Data<MemberDashboard>).value
        assertEquals("cached", data.memberName)
        assertEquals(0, api.callCount, "cache hit must be served from the SoT without a network fetch")
    }

    // ─── validator-expiry / refresh → re-fetch (SWR revalidation) ────────────────────────────
    @Test
    fun refresh_refetches_from_api() = runTest {
        val api = FakeMemberDashboardApi(NetworkResult.Success(dashboardDto(memberName = "Asha")))
        val dao = FakeMemberDashboardDao()
        val store = provideMemberDashboardStore(api, dao)

        store.awaitFreshData(MEMBER_DASHBOARD_DEFAULT_KEY)
        store.awaitFreshData(MEMBER_DASHBOARD_DEFAULT_KEY)

        assertEquals(2, api.callCount, "an explicit fresh() read must re-drive the fetcher (SWR revalidation)")
    }

    // ─── fetcher error surfaces on the Store5 error channel ──────────────────────────────────
    @Test
    fun fetcher_error_surfaces_as_error_response() = runTest {
        val api = FakeMemberDashboardApi(NetworkResult.Error(NetworkError.SERVER))
        val dao = FakeMemberDashboardDao()
        val store = provideMemberDashboardStore(api, dao)

        val response = store.stream(StoreReadRequest.fresh(MEMBER_DASHBOARD_DEFAULT_KEY))
            .first { it is StoreReadResponse.Error }

        val error = (response as StoreReadResponse.Error.Exception).error
        assertTrue(error is MemberDashboardFetchException, "error channel must carry the typed fetch exception")
        assertEquals(NetworkError.SERVER, error.networkError)
    }

    // ─── round-trip: nested lists survive the JSON-column SoT ─────────────────────────────────
    @Test
    fun fetched_dashboard_round_trips_nested_lists_through_sot() = runTest {
        val api = FakeMemberDashboardApi(NetworkResult.Success(dashboardDto(memberName = "Asha")))
        val dao = FakeMemberDashboardDao()
        val store = provideMemberDashboardStore(api, dao)

        val data = store.awaitFreshData(MEMBER_DASHBOARD_DEFAULT_KEY)

        assertEquals(2, data.myGroups.size, "myGroups must round-trip through the JSON SoT column")
        assertEquals(1, data.recentTransactions.size, "recentTransactions must round-trip through the JSON SoT column")
        assertEquals("txn-1", data.recentTransactions.first().id)
    }

    // ─── helpers ─────────────────────────────────────────────────────────────────────────────

    private suspend fun Store<String, MemberDashboard>.awaitFreshData(key: String): MemberDashboard {
        val response = stream(StoreReadRequest.fresh(key))
            .first { it is StoreReadResponse.Data<*> }
        @Suppress("UNCHECKED_CAST")
        return (response as StoreReadResponse.Data<MemberDashboard>).value
    }
}

// ---------------------------------------------------------------------------
// Fakes + fixtures
// ---------------------------------------------------------------------------

private class FakeMemberDashboardApi(
    private val result: NetworkResult<MemberDashboardResponseDto, NetworkError>,
) : MemberDashboardApi {
    var callCount: Int = 0
        private set
    var lastSelectedGroupId: String? = null
        private set

    override suspend fun getMemberDashboard(
        selectedGroupId: String?,
    ): NetworkResult<MemberDashboardResponseDto, NetworkError> {
        callCount++
        lastSelectedGroupId = selectedGroupId
        return result
    }
}

private class FakeMemberDashboardDao : MemberDashboardDao {
    private val rows = MutableStateFlow<List<MemberDashboardCacheEntity>>(emptyList())

    fun seed(entity: MemberDashboardCacheEntity) { rows.value = listOf(entity) }
    fun currentRows(): List<MemberDashboardCacheEntity> = rows.value

    override fun observeByKey(cacheKey: String): Flow<MemberDashboardCacheEntity?> =
        rows.map { list -> list.firstOrNull { it.cacheKey == cacheKey } }

    override suspend fun upsert(entity: MemberDashboardCacheEntity) {
        val byKey = rows.value.associateBy { it.cacheKey }.toMutableMap()
        byKey[entity.cacheKey] = entity
        rows.value = byKey.values.toList()
    }

    override suspend fun deleteByKey(cacheKey: String) {
        rows.value = rows.value.filterNot { it.cacheKey == cacheKey }
    }

    override suspend fun deleteAll() { rows.value = emptyList() }

    override suspend fun replaceForKey(entity: MemberDashboardCacheEntity) {
        upsert(entity)
    }

    override suspend fun deleteOlderThan(epochMillis: Long) {
        rows.value = rows.value.filter { it.fetchedAt >= epochMillis }
    }
}

private fun dashboardDto(memberName: String) = MemberDashboardResponseDto(
    memberName = memberName,
    myGroups = listOf(
        GroupSummaryDto(groupId = "g1", name = "Umoja VSLA", poolModel = SavingsMechanismDto.ACCUMULATING),
        GroupSummaryDto(groupId = "g2", name = "Tumaini ROSCA", poolModel = SavingsMechanismDto.ROTATING_PAYOUT),
    ),
    selectedGroup = GroupSummaryDto(groupId = "g1", name = "Umoja VSLA", poolModel = SavingsMechanismDto.ACCUMULATING),
    poolModel = SavingsMechanismDto.ACCUMULATING,
    groupLinkedSavingsBalance = 1200.0,
    individualSavingsBalance = 300.0,
    shareOutProjection = 1500.0,
    rotationPosition = null,
    nextRecipientEta = null,
    recentTransactions = listOf(
        SavingsTransactionDto(id = "txn-1", date = "2026-07-01", type = TransactionTypeDto.DEPOSIT, amount = 50.0),
    ),
)

private fun cacheEntity(cacheKey: String, memberName: String) = MemberDashboardCacheEntity(
    cacheKey = cacheKey,
    memberName = memberName,
    selectedGroupId = "g1",
    selectedGroupName = "Umoja VSLA",
    selectedGroupPoolModel = "ACCUMULATING",
    poolModel = "ACCUMULATING",
    groupLinkedSavingsBalance = 1200.0,
    individualSavingsBalance = 300.0,
    shareOutProjection = 1500.0,
    rotationPosition = null,
    nextRecipientEta = null,
    myGroupsJson = "[]",
    recentTransactionsJson = "[]",
    fetchedAt = 1L,
)
