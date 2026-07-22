/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.store.groupdashboard

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.database.groupdashboard.dao.GroupDashboardDao
import org.mifos.groupbanking.core.database.groupdashboard.entity.CachedActivityItem
import org.mifos.groupbanking.core.database.groupdashboard.entity.CachedGroupAccounts
import org.mifos.groupbanking.core.database.groupdashboard.entity.CachedGroupCorpus
import org.mifos.groupbanking.core.database.groupdashboard.entity.CachedGroupDashboard
import org.mifos.groupbanking.core.database.groupdashboard.entity.CachedGroupDetail
import org.mifos.groupbanking.core.database.groupdashboard.entity.CachedGroupInstanceConfig
import org.mifos.groupbanking.core.database.groupdashboard.entity.CachedViewerRoleInfo
import org.mifos.groupbanking.core.database.groupdashboard.entity.GroupDashboardCacheCodec
import org.mifos.groupbanking.core.database.groupdashboard.entity.GroupDashboardCacheEntity
import org.mifos.groupbanking.core.model.GroupDashboard
import org.mifos.groupbanking.core.network.model.ActivityItemDto
import org.mifos.groupbanking.core.network.model.ActivityTypeDto
import org.mifos.groupbanking.core.network.model.GroupAccountsDto
import org.mifos.groupbanking.core.network.model.GroupCorpusDto
import org.mifos.groupbanking.core.network.model.GroupDetailDto
import org.mifos.groupbanking.core.network.model.GroupInstanceConfigDto
import org.mifos.groupbanking.core.network.model.SavingsMechanismDto
import org.mifos.groupbanking.core.network.model.ViewerRoleDto
import org.mifos.groupbanking.core.network.model.ViewerRoleInfoDto
import org.mifos.groupbanking.core.network.service.groupdashboard.GroupDashboardApi
import org.mifos.groupbanking.core.store.groupdashboard.impl.GroupDashboardFetchException
import org.mifos.groupbanking.core.store.groupdashboard.impl.provideGroupDashboardStore
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD coverage for [provideGroupDashboardStore] — the COMPOSITE dynamic-key NETWORK_WITH_CACHE
 * group-dashboard store (COMP-GRP-001, `get_group` + `get_viewer_role` + `get_group_corpus` +
 * `get_group_accounts` fetched in parallel and fanned-in client-side).
 *
 * Exercises the Store5 read pipeline directly (parallel-combine fetcher + Room-shaped
 * SourceOfTruth + validator) with an in-memory [FakeGroupDashboardDao] and a scripted
 * [FakeGroupDashboardApi]; no Room runtime and no NetworkMonitor at this layer. The composite
 * axis is asserted explicitly: all-four-succeed combines into one [GroupDashboard]; ANY critical
 * read failing throws to Store5's error channel; a cache hit is served from the SoT without any
 * network fetch; and a fresh read re-drives all four parallel calls (SWR revalidation).
 */
class GroupDashboardStoreTest {

    // ─── cache-miss: all four reads succeed → parallel-combine into one dashboard, persisted ───
    @Test
    fun cache_miss_all_reads_succeed_combines_persists_and_emits() = runTest {
        val api = FakeGroupDashboardApi()
        val dao = FakeGroupDashboardDao()
        val store = provideGroupDashboardStore(api, dao)

        val data = store.awaitFreshData("group-7")

        assertEquals("group-7", data.group.id)
        assertEquals("Umoja VSLA", data.group.name)
        assertEquals(2, data.accounts.recentActivity.size, "activity feed must fan-in from get_group_accounts")
        assertEquals(1, api.getGroupCalls, "get_group runs exactly once on cache miss")
        assertEquals(1, api.getViewerRoleCalls, "get_viewer_role runs exactly once on cache miss")
        assertEquals(1, api.getGroupCorpusCalls, "get_group_corpus runs exactly once on cache miss")
        assertEquals(1, api.getGroupAccountsCalls, "get_group_accounts runs exactly once on cache miss")
        assertEquals(1, dao.currentRows().size, "combined dashboard must be written through to the SoT")
        assertEquals("group-7", dao.currentRows().single().groupId)
    }

    // ─── composite failure: one critical read fails → the whole fetch surfaces an error ────────
    @Test
    fun one_critical_read_fails_surfaces_error_response() = runTest {
        val api = FakeGroupDashboardApi(
            corpusResult = NetworkResult.Error(NetworkError.SERVER),
        )
        val dao = FakeGroupDashboardDao()
        val store = provideGroupDashboardStore(api, dao)

        val response = store.stream(StoreReadRequest.fresh("group-7"))
            .first { it is StoreReadResponse.Error }

        val error = (response as StoreReadResponse.Error.Exception).error
        assertTrue(error is GroupDashboardFetchException, "any critical read failing must throw the typed fetch exception")
        assertEquals(NetworkError.SERVER, error.networkError)
        assertEquals(0, dao.currentRows().size, "a failed composite fetch must not persist a partial snapshot")
    }

    // ─── cache-hit → SoT emit, none of the four endpoints called ───────────────────────────────
    @Test
    fun cache_hit_emits_from_sot_without_fetcher_call() = runTest {
        val api = FakeGroupDashboardApi()
        val dao = FakeGroupDashboardDao().apply { seed(cacheEntity("group-7", name = "Cached Group")) }
        val store = provideGroupDashboardStore(api, dao)

        val response = store.stream(StoreReadRequest.cached("group-7", refresh = false))
            .first { it is StoreReadResponse.Data<*> }

        @Suppress("UNCHECKED_CAST")
        val data = (response as StoreReadResponse.Data<GroupDashboard>).value
        assertEquals("Cached Group", data.group.name)
        assertEquals(0, api.getGroupCalls, "cache hit must be served from the SoT without a network fetch")
        assertEquals(0, api.getGroupAccountsCalls)
    }

    // ─── validator-expiry / refresh → re-fetch all four (SWR revalidation) ─────────────────────
    @Test
    fun refresh_refetches_all_four_reads_from_api() = runTest {
        val api = FakeGroupDashboardApi()
        val dao = FakeGroupDashboardDao()
        val store = provideGroupDashboardStore(api, dao)

        store.awaitFreshData("group-7")
        store.awaitFreshData("group-7")

        assertEquals(2, api.getGroupCalls, "an explicit fresh() read must re-drive get_group (SWR revalidation)")
        assertEquals(2, api.getViewerRoleCalls)
        assertEquals(2, api.getGroupCorpusCalls)
        assertEquals(2, api.getGroupAccountsCalls)
    }

    // ─── round-trip: the whole composite (incl. nested activity feed) survives the JSON SoT ────
    @Test
    fun combined_dashboard_round_trips_composite_through_sot() = runTest {
        val api = FakeGroupDashboardApi()
        val dao = FakeGroupDashboardDao()
        val store = provideGroupDashboardStore(api, dao)

        val data = store.awaitFreshData("group-7")

        assertEquals(SavingsMechanismDto.ACCUMULATING.name, data.group.typeConfig.poolModel.name)
        assertEquals(720.0, data.corpus.currentBalance, "corpus must round-trip through the JSON SoT column")
        assertEquals("act-1", data.accounts.recentActivity.first().id, "activity feed must round-trip through the JSON SoT column")
    }

    // ─── helpers ─────────────────────────────────────────────────────────────────────────────

    private suspend fun Store<String, GroupDashboard>.awaitFreshData(key: String): GroupDashboard {
        val response = stream(StoreReadRequest.fresh(key))
            .first { it is StoreReadResponse.Data<*> }
        @Suppress("UNCHECKED_CAST")
        return (response as StoreReadResponse.Data<GroupDashboard>).value
    }
}

// ---------------------------------------------------------------------------
// Fakes + fixtures
// ---------------------------------------------------------------------------

private class FakeGroupDashboardApi(
    private val groupResult: NetworkResult<GroupDetailDto, NetworkError> =
        NetworkResult.Success(groupDetailDto()),
    private val viewerRoleResult: NetworkResult<ViewerRoleInfoDto, NetworkError> =
        NetworkResult.Success(ViewerRoleInfoDto(role = ViewerRoleDto.ORGANIZER, memberId = 42L)),
    private val corpusResult: NetworkResult<GroupCorpusDto, NetworkError> =
        NetworkResult.Success(groupCorpusDto()),
    private val accountsResult: NetworkResult<GroupAccountsDto, NetworkError> =
        NetworkResult.Success(groupAccountsDto()),
) : GroupDashboardApi {
    var getGroupCalls: Int = 0
        private set
    var getViewerRoleCalls: Int = 0
        private set
    var getGroupCorpusCalls: Int = 0
        private set
    var getGroupAccountsCalls: Int = 0
        private set

    override suspend fun getGroup(groupId: String): NetworkResult<GroupDetailDto, NetworkError> {
        getGroupCalls++
        return groupResult
    }

    override suspend fun getViewerRole(groupId: String): NetworkResult<ViewerRoleInfoDto, NetworkError> {
        getViewerRoleCalls++
        return viewerRoleResult
    }

    override suspend fun getGroupCorpus(groupId: String): NetworkResult<GroupCorpusDto, NetworkError> {
        getGroupCorpusCalls++
        return corpusResult
    }

    override suspend fun getGroupAccounts(groupId: String): NetworkResult<GroupAccountsDto, NetworkError> {
        getGroupAccountsCalls++
        return accountsResult
    }
}

private class FakeGroupDashboardDao : GroupDashboardDao {
    private val rows = MutableStateFlow<List<GroupDashboardCacheEntity>>(emptyList())

    fun seed(entity: GroupDashboardCacheEntity) { rows.value = listOf(entity) }
    fun currentRows(): List<GroupDashboardCacheEntity> = rows.value

    override fun observeByKey(groupId: String): Flow<GroupDashboardCacheEntity?> =
        rows.map { list -> list.firstOrNull { it.groupId == groupId } }

    override suspend fun upsert(entity: GroupDashboardCacheEntity) {
        val byKey = rows.value.associateBy { it.groupId }.toMutableMap()
        byKey[entity.groupId] = entity
        rows.value = byKey.values.toList()
    }

    override suspend fun deleteByKey(groupId: String) {
        rows.value = rows.value.filterNot { it.groupId == groupId }
    }

    override suspend fun deleteAll() { rows.value = emptyList() }

    override suspend fun replaceForKey(entity: GroupDashboardCacheEntity) {
        upsert(entity)
    }

    override suspend fun deleteOlderThan(epochMillis: Long) {
        rows.value = rows.value.filter { it.fetchedAt >= epochMillis }
    }
}

private fun groupDetailDto(name: String = "Umoja VSLA") = GroupDetailDto(
    id = "group-7",
    fineractCenterId = 100L,
    name = name,
    cycleNumber = 2,
    cycleLengthMonths = 12,
    meetingFrequency = "WEEKLY",
    memberCount = 25,
    overdueLoansCount = 1,
    status = "ACTIVE",
    typeConfig = GroupInstanceConfigDto(
        poolModel = SavingsMechanismDto.ACCUMULATING,
        shareoutFormula = "PROPORTIONAL",
        payoutOrderMethod = "FIXED",
        shareValue = 100.0,
        contributionAmount = 200.0,
        socialFundEnabled = true,
        cycleLengthMonths = 12,
        loanMultiplier = 3.0,
        interestRate = 10.0,
        fineAmount = 5.0,
    ),
)

private fun groupCorpusDto() = GroupCorpusDto(
    currentBalance = 720.0,
    openingBalance = 500.0,
    totalContributionsThisCycle = 300.0,
    totalLoansOutstanding = 80.0,
    lastUpdated = "2026-07-18T12:00:00Z",
    rotationPosition = null,
    nextRecipientName = null,
    nextRecipientPosition = null,
)

private fun groupAccountsDto() = GroupAccountsDto(
    savingsBalance = 720.0,
    loansOutstanding = 80.0,
    activeLoanCount = 1,
    shareOutProjection = 1500.0,
    recentActivity = listOf(
        ActivityItemDto(id = "act-1", type = ActivityTypeDto.DEPOSIT, description = "Deposit", amount = 50.0, date = "2026-07-01", memberName = "Asha"),
        ActivityItemDto(id = "act-2", type = ActivityTypeDto.MEETING, description = "Meeting", amount = null, date = "2026-07-08", memberName = null),
    ),
)

private fun cacheEntity(groupId: String, name: String): GroupDashboardCacheEntity {
    val payload = CachedGroupDashboard(
        group = CachedGroupDetail(
            id = groupId,
            fineractCenterId = 100L,
            name = name,
            cycleNumber = 2,
            cycleLengthMonths = 12,
            meetingFrequency = "WEEKLY",
            memberCount = 25,
            overdueLoansCount = 1,
            status = "ACTIVE",
            typeConfig = CachedGroupInstanceConfig(
                groupType = "UNKNOWN",
                poolModel = "ACCUMULATING",
                contributionModel = "UNKNOWN",
                shareoutFormula = "PROPORTIONAL",
                payoutOrderMethod = "FIXED",
                shareValue = 100.0,
                contributionAmount = 200.0,
                socialFundEnabled = true,
                cycleLengthMonths = 12,
                loanMultiplier = 3.0,
                interestRate = 10.0,
                fineAmount = 5.0,
            ),
        ),
        viewerRole = CachedViewerRoleInfo(role = "ORGANIZER", memberId = 42L),
        corpus = CachedGroupCorpus(
            currentBalance = 720.0,
            openingBalance = 500.0,
            totalContributionsThisCycle = 300.0,
            totalLoansOutstanding = 80.0,
            lastUpdated = "2026-07-18T12:00:00Z",
            rotationPosition = null,
            nextRecipientName = null,
            nextRecipientPosition = null,
        ),
        accounts = CachedGroupAccounts(
            savingsBalance = 720.0,
            loansOutstanding = 80.0,
            activeLoanCount = 1,
            shareOutProjection = 1500.0,
            recentActivity = listOf(
                CachedActivityItem(id = "act-1", type = "DEPOSIT", description = "Deposit", amount = 50.0, date = "2026-07-01", memberName = "Asha"),
            ),
        ),
    )
    return GroupDashboardCacheEntity(
        groupId = groupId,
        dashboardJson = GroupDashboardCacheCodec.encode(payload),
        fetchedAt = 1L,
    )
}
