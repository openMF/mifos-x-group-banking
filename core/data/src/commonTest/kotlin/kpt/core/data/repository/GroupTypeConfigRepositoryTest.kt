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
import kotlinx.coroutines.test.runTest
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.base.store.screen.FetchPolicy
import kpt.core.base.store.screen.ScreenState
import kpt.core.database.grouptypepicker.dao.GroupTypeConfigDao
import kpt.core.database.grouptypepicker.entity.GroupTypeConfigEntity
import kpt.core.model.GroupTypeSlug
import kpt.core.network.model.ContributionModeDto
import kpt.core.network.model.GroupTypeConfigDto
import kpt.core.network.model.GroupTypeSlugDto
import kpt.core.network.model.SavingsMechanismDto
import kpt.core.network.service.grouptypepicker.GroupTypeConfigApi
import kpt.core.store.grouptypepicker.impl.provideGroupTypeConfigStore
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * TDD coverage for [GroupTypeConfigRepositoryImpl] — verifies the repository surfaces the store
 * as an offline-first [kpt.core.base.store.screen.ScreenDataStream] via `.asScreenStream()`,
 * mapping the cached catalogue into `ScreenState.Content<List<GroupTypeConfig>>`.
 */
class GroupTypeConfigRepositoryTest {

    @Test
    fun stream_emits_Content_with_mapped_catalogue_when_online() = runTest {
        val api = FakeGroupTypeConfigApi(NetworkResult.Success(SEED_DTOS))
        val dao = FakeGroupTypeConfigDao()
        val repo = repository(api, dao, online = true)

        val state = repo.groupTypeConfigsStream(
            scope = backgroundScope,
            fetchPolicy = FetchPolicy.NETWORK_WITH_CACHE,
        ).state.first { it is ScreenState.Content }

        val data = (state as ScreenState.Content).data
        assertEquals(2, data.size)
        assertEquals(GroupTypeSlug.VSLA, data.first().typeSlug)
    }

    @Test
    fun stream_serves_cached_catalogue_offline() = runTest {
        val api = FakeGroupTypeConfigApi(NetworkResult.Error(NetworkError.SERVER))
        val dao = FakeGroupTypeConfigDao().apply { seed(SEED_ENTITIES) }
        val repo = repository(api, dao, online = false)

        val state = repo.groupTypeConfigsStream(
            scope = backgroundScope,
            fetchPolicy = FetchPolicy.CACHE_ONLY,
        ).state.first { it is ScreenState.Content }

        val data = (state as ScreenState.Content).data
        assertEquals(2, data.size, "offline read must serve the persisted catalogue (offline: show_cached)")
        assertEquals(0, api.callCount, "CACHE_ONLY must not hit the network")
    }

    // ─── wiring ──────────────────────────────────────────────────────────────

    private fun repository(
        api: GroupTypeConfigApi,
        dao: GroupTypeConfigDao,
        online: Boolean,
    ): GroupTypeConfigRepository {
        val initial = if (online) {
            NetworkStatus.Available(NetworkInfo(type = NetworkType.WiFi, isMetered = false))
        } else {
            NetworkStatus.Unavailable
        }
        return GroupTypeConfigRepositoryImpl(
            groupTypeConfigStore = provideGroupTypeConfigStore(api, dao),
            networkMonitor = FakeNetworkMonitor(initial),
            fetchedAtRepository = InMemoryFetchedAtRepository(),
        )
    }
}

// ---------------------------------------------------------------------------
// Fakes
// ---------------------------------------------------------------------------

private class FakeGroupTypeConfigApi(
    private val result: NetworkResult<List<GroupTypeConfigDto>, NetworkError>,
) : GroupTypeConfigApi {
    var callCount: Int = 0
        private set

    override suspend fun getGroupTypeConfigs(entityId: Long): NetworkResult<List<GroupTypeConfigDto>, NetworkError> {
        callCount++
        return result
    }
}

private class FakeGroupTypeConfigDao : GroupTypeConfigDao {
    private val rows = MutableStateFlow<List<GroupTypeConfigEntity>>(emptyList())

    fun seed(entities: List<GroupTypeConfigEntity>) {
        rows.value = entities
    }

    override fun observeAll(): Flow<List<GroupTypeConfigEntity>> = rows

    override suspend fun upsertAll(entities: List<GroupTypeConfigEntity>) {
        val byKey = rows.value.associateBy { it.typeSlug }.toMutableMap()
        entities.forEach { byKey[it.typeSlug] = it }
        rows.value = byKey.values.toList()
    }

    override suspend fun deleteAll() {
        rows.value = emptyList()
    }

    override suspend fun replaceAll(entities: List<GroupTypeConfigEntity>) {
        deleteAll()
        upsertAll(entities)
    }

    override suspend fun deleteOlderThan(epochMillis: Long) {
        rows.value = rows.value.filter { it.fetchedAt >= epochMillis }
    }
}

private val SEED_DTOS = listOf(
    GroupTypeConfigDto(
        typeSlug = GroupTypeSlugDto.VSLA,
        displayName = "Village Savings & Loan Association",
        tagline = "Save in shares; borrow up to 3x; share-out at year end",
        savingsMechanism = SavingsMechanismDto.ACCUMULATING,
        contributionMode = ContributionModeDto.SHARE_BASED_VARIABLE,
        lendingEnabled = true,
        hasSocialFund = true,
        hasBankLinkage = false,
        welfareOnlyMode = false,
        formallyRegistered = false,
        defaultLoanMultiplier = 3.0,
        defaultInterestRatePct = 10.0,
        defaultCycleLengthMonths = 12,
        maxMembers = 30,
        minMembers = 5,
    ),
    GroupTypeConfigDto(
        typeSlug = GroupTypeSlugDto.JLG,
        displayName = "Joint Liability Group",
        tagline = "4-10 members take an external MFI loan jointly",
        savingsMechanism = SavingsMechanismDto.NONE,
        contributionMode = ContributionModeDto.MINIMAL,
        lendingEnabled = false,
        hasSocialFund = false,
        hasBankLinkage = true,
        welfareOnlyMode = false,
        formallyRegistered = false,
        defaultLoanMultiplier = 0.0,
        defaultInterestRatePct = 0.0,
        defaultCycleLengthMonths = 12,
        maxMembers = 10,
        minMembers = 4,
    ),
)

private val SEED_ENTITIES = listOf(
    GroupTypeConfigEntity(
        typeSlug = "VSLA",
        displayName = "Village Savings & Loan Association",
        tagline = "Save in shares; borrow up to 3x; share-out at year end",
        savingsMechanism = "ACCUMULATING",
        contributionMode = "SHARE_BASED_VARIABLE",
        lendingEnabled = true,
        hasSocialFund = true,
        hasBankLinkage = false,
        welfareOnlyMode = false,
        formallyRegistered = false,
        defaultLoanMultiplier = 3.0,
        defaultInterestRatePct = 10.0,
        defaultCycleLengthMonths = 12,
        maxMembers = 30,
        minMembers = 5,
        fetchedAt = 1L,
    ),
    GroupTypeConfigEntity(
        typeSlug = "JLG",
        displayName = "Joint Liability Group",
        tagline = "4-10 members take an external MFI loan jointly",
        savingsMechanism = "NONE",
        contributionMode = "MINIMAL",
        lendingEnabled = false,
        hasSocialFund = false,
        hasBankLinkage = true,
        welfareOnlyMode = false,
        formallyRegistered = false,
        defaultLoanMultiplier = 0.0,
        defaultInterestRatePct = 0.0,
        defaultCycleLengthMonths = 12,
        maxMembers = 10,
        minMembers = 4,
        fetchedAt = 1L,
    ),
)
