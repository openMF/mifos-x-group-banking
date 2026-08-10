/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.data.repository

import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkInfo
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkStatus
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.base.store.screen.FetchPolicy
import kpt.core.base.store.screen.ScreenState
import kpt.core.database.personaldashboard.dao.MemberDashboardDao
import kpt.core.database.personaldashboard.entity.MemberDashboardCacheEntity
import kpt.core.network.model.GroupSummaryDto
import kpt.core.network.model.MemberDashboardResponseDto
import kpt.core.network.model.SavingsMechanismDto
import kpt.core.network.service.personaldashboard.MemberDashboardApi
import kpt.core.store.personaldashboard.impl.MEMBER_DASHBOARD_DEFAULT_KEY
import kpt.core.store.personaldashboard.impl.provideMemberDashboardStore
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * TDD coverage for [MemberDashboardRepositoryImpl] — verifies the repository surfaces the
 * dynamic-key store as an offline-first [kpt.core.base.store.screen.ScreenDataStream] via
 * `.asScreenStream()`, mapping the cached snapshot into `ScreenState.Content<MemberDashboard>`.
 */
class MemberDashboardRepositoryTest {

    @Test
    fun stream_emits_Content_with_mapped_dashboard_when_online() = runTest {
        val api = FakeMemberDashboardApi(NetworkResult.Success(dashboardDto("Asha")))
        val dao = FakeMemberDashboardDao()
        val repo = repository(api, dao, online = true)

        val state = repo.memberDashboardStream(
            selectedGroupId = null,
            scope = backgroundScope,
            fetchPolicy = FetchPolicy.NETWORK_WITH_CACHE,
        ).state.first { it is ScreenState.Content }

        val data = (state as ScreenState.Content).data
        assertEquals("Asha", data.memberName)
        assertEquals("group-1", data.selectedGroup.groupId)
    }

    @Test
    fun stream_serves_cached_dashboard_offline() = runTest {
        val api = FakeMemberDashboardApi(NetworkResult.Error(NetworkError.SERVER))
        val dao = FakeMemberDashboardDao().apply { seed(cacheEntity(MEMBER_DASHBOARD_DEFAULT_KEY, "cached-Asha")) }
        val repo = repository(api, dao, online = false)

        val state = repo.memberDashboardStream(
            selectedGroupId = null,
            scope = backgroundScope,
            fetchPolicy = FetchPolicy.CACHE_ONLY,
        ).state.first { it is ScreenState.Content }

        val data = (state as ScreenState.Content).data
        assertEquals("cached-Asha", data.memberName, "offline read must serve the persisted snapshot (offline: use_sqldelight)")
        assertEquals(0, api.callCount, "CACHE_ONLY must not hit the network")
    }

    // ─── wiring ──────────────────────────────────────────────────────────────

    private fun repository(
        api: MemberDashboardApi,
        dao: MemberDashboardDao,
        online: Boolean,
    ): MemberDashboardRepository {
        val initial = if (online) {
            NetworkStatus.Available(NetworkInfo(type = NetworkType.WiFi, isMetered = false))
        } else {
            NetworkStatus.Unavailable
        }
        return MemberDashboardRepositoryImpl(
            memberDashboardStore = provideMemberDashboardStore(api, dao),
            networkMonitor = FakeNetworkMonitor(initial),
            fetchedAtRepository = InMemoryFetchedAtRepository(),
        )
    }
}

// ---------------------------------------------------------------------------
// Fakes
// ---------------------------------------------------------------------------

private class FakeMemberDashboardApi(
    private val result: NetworkResult<MemberDashboardResponseDto, NetworkError>,
) : MemberDashboardApi {
    var callCount: Int = 0
        private set

    override suspend fun getMemberDashboard(
        selectedGroupId: String?,
    ): NetworkResult<MemberDashboardResponseDto, NetworkError> {
        callCount++
        return result
    }
}

private class FakeMemberDashboardDao : MemberDashboardDao {
    private val rows = MutableStateFlow<List<MemberDashboardCacheEntity>>(emptyList())

    fun seed(entity: MemberDashboardCacheEntity) {
        rows.value = listOf(entity)
    }

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

    override suspend fun deleteAll() {
        rows.value = emptyList()
    }

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
        GroupSummaryDto(groupId = "group-1", name = "Umoja VSLA", poolModel = SavingsMechanismDto.ACCUMULATING),
    ),
    selectedGroup = GroupSummaryDto(groupId = "group-1", name = "Umoja VSLA", poolModel = SavingsMechanismDto.ACCUMULATING),
    poolModel = SavingsMechanismDto.ACCUMULATING,
    groupLinkedSavingsBalance = 1200.0,
    individualSavingsBalance = 300.0,
    shareOutProjection = 1500.0,
    rotationPosition = null,
    nextRecipientEta = null,
    recentTransactions = emptyList(),
)

private fun cacheEntity(cacheKey: String, memberName: String) = MemberDashboardCacheEntity(
    cacheKey = cacheKey,
    memberName = memberName,
    clientId = 301L,
    groupLinkedSavingsId = 401L,
    individualSavingsId = 411L,
    selectedGroupId = "group-1",
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
