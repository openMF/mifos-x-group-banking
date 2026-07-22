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
import org.mifos.groupbanking.core.store.groupdashboard.impl.provideGroupDashboardStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * TDD coverage for [GroupDashboardRepositoryImpl] — verifies the repository surfaces the composite
 * store as an offline-first [kpt.core.base.store.screen.ScreenDataStream] via `.asScreenStream()`,
 * mapping the combined snapshot into `ScreenState.Content<GroupDashboard>` and serving the persisted
 * composite offline (`data-flow.yaml#cache.offline: fallback_cache`).
 */
class GroupDashboardRepositoryTest {

    @Test
    fun stream_emits_Content_with_combined_dashboard_when_online() = runTest {
        val api = FakeGroupDashboardApi()
        val dao = FakeGroupDashboardDao()
        val repo = repository(api, dao, online = true)

        val state = repo.groupDashboardStream(
            groupId = "group-7",
            scope = backgroundScope,
            fetchPolicy = FetchPolicy.NETWORK_WITH_CACHE,
        ).state.first { it is ScreenState.Content }

        val data = (state as ScreenState.Content).data
        assertEquals("group-7", data.group.id)
        assertEquals("Umoja VSLA", data.group.name)
        assertEquals(ViewerRoleDto.ORGANIZER.name, data.viewerRole.role.name)
    }

    @Test
    fun stream_serves_cached_composite_offline() = runTest {
        val api = FakeGroupDashboardApi(offline = true)
        val dao = FakeGroupDashboardDao().apply { seed(cacheEntity("group-7", "Cached Group")) }
        val repo = repository(api, dao, online = false)

        val state = repo.groupDashboardStream(
            groupId = "group-7",
            scope = backgroundScope,
            fetchPolicy = FetchPolicy.CACHE_ONLY,
        ).state.first { it is ScreenState.Content }

        val data = (state as ScreenState.Content).data
        assertEquals("Cached Group", data.group.name, "offline read must serve the persisted composite (fallback_cache)")
        assertEquals(0, api.getGroupCalls, "CACHE_ONLY must not hit the network")
    }

    // ─── wiring ──────────────────────────────────────────────────────────────

    private fun repository(
        api: GroupDashboardApi,
        dao: GroupDashboardDao,
        online: Boolean,
    ): GroupDashboardRepository {
        val initial = if (online) {
            NetworkStatus.Available(NetworkInfo(type = NetworkType.WiFi, isMetered = false))
        } else {
            NetworkStatus.Unavailable
        }
        return GroupDashboardRepositoryImpl(
            groupDashboardStore = provideGroupDashboardStore(api, dao),
            networkMonitor = FakeNetworkMonitor(initial),
            fetchedAtRepository = InMemoryFetchedAtRepository(),
        )
    }
}

// ---------------------------------------------------------------------------
// Fakes
// ---------------------------------------------------------------------------

private class FakeGroupDashboardApi(offline: Boolean = false) : GroupDashboardApi {
    var getGroupCalls: Int = 0
        private set

    private val err: NetworkResult<Nothing, NetworkError> = NetworkResult.Error(NetworkError.SERVER)
    private val down = offline

    override suspend fun getGroup(groupId: String): NetworkResult<GroupDetailDto, NetworkError> {
        getGroupCalls++
        return if (down) err else NetworkResult.Success(groupDetailDto())
    }

    override suspend fun getViewerRole(groupId: String): NetworkResult<ViewerRoleInfoDto, NetworkError> =
        if (down) err else NetworkResult.Success(ViewerRoleInfoDto(role = ViewerRoleDto.ORGANIZER, memberId = 42L))

    override suspend fun getGroupCorpus(groupId: String): NetworkResult<GroupCorpusDto, NetworkError> =
        if (down) err else NetworkResult.Success(groupCorpusDto())

    override suspend fun getGroupAccounts(groupId: String): NetworkResult<GroupAccountsDto, NetworkError> =
        if (down) err else NetworkResult.Success(groupAccountsDto())
}

private class FakeGroupDashboardDao : GroupDashboardDao {
    private val rows = MutableStateFlow<List<GroupDashboardCacheEntity>>(emptyList())

    fun seed(entity: GroupDashboardCacheEntity) { rows.value = listOf(entity) }

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
