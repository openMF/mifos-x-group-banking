/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.store.grouptypepicker

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.database.grouptypepicker.dao.GroupTypeConfigDao
import org.mifos.groupbanking.core.database.grouptypepicker.entity.GroupTypeConfigEntity
import org.mifos.groupbanking.core.model.GroupTypeConfig
import org.mifos.groupbanking.core.network.model.ContributionModeDto
import org.mifos.groupbanking.core.network.model.GroupTypeConfigDto
import org.mifos.groupbanking.core.network.model.GroupTypeSlugDto
import org.mifos.groupbanking.core.network.model.SavingsMechanismDto
import org.mifos.groupbanking.core.network.service.grouptypepicker.GroupTypeConfigApi
import org.mifos.groupbanking.core.store.grouptypepicker.impl.GROUP_TYPE_CONFIG_CATALOGUE_KEY
import org.mifos.groupbanking.core.store.grouptypepicker.impl.GroupTypeConfigFetchException
import org.mifos.groupbanking.core.store.grouptypepicker.impl.provideGroupTypeConfigStore
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD coverage for [provideGroupTypeConfigStore] — the NETWORK_WITH_CACHE seed-catalogue store.
 *
 * Exercises the Store5 read pipeline directly (fetcher + Room-shaped SourceOfTruth + validator)
 * with an in-memory [FakeGroupTypeConfigDao] and a scripted [FakeGroupTypeConfigApi]; no Room
 * runtime and no NetworkMonitor needed at this layer (that is covered by the repository test).
 */
class GroupTypeConfigStoreTest {

    // ─── cache-miss → fetch → data + persisted to SoT ────────────────────────
    @Test
    fun cache_miss_calls_fetcher_persists_to_sot_and_emits() = runTest {
        val api = FakeGroupTypeConfigApi(NetworkResult.Success(SEED_DTOS))
        val dao = FakeGroupTypeConfigDao()
        val store = provideGroupTypeConfigStore(api, dao)

        val data = store.awaitFreshData()

        assertEquals(2, data.size)
        assertEquals(1, api.callCount, "fetcher must run exactly once on cache miss")
        assertEquals(2, dao.currentRows().size, "fetched catalogue must be written through to the SoT")
    }

    // ─── cache-hit → SoT emit, fetcher NOT called ────────────────────────────
    @Test
    fun cache_hit_emits_from_sot_without_fetcher_call() = runTest {
        val api = FakeGroupTypeConfigApi(NetworkResult.Success(SEED_DTOS))
        val dao = FakeGroupTypeConfigDao().apply { seed(SEED_ENTITIES) }
        val store = provideGroupTypeConfigStore(api, dao)

        val response = store.stream(StoreReadRequest.cached(GROUP_TYPE_CONFIG_CATALOGUE_KEY, refresh = false))
            .first { it is StoreReadResponse.Data<*> }

        val data = (response as StoreReadResponse.Data<List<GroupTypeConfig>>).value
        assertEquals(2, data.size)
        assertEquals(0, api.callCount, "cache hit must be served from the SoT without a network fetch")
    }

    // ─── validator-expiry / refresh → re-fetch ───────────────────────────────
    @Test
    fun refresh_refetches_from_api() = runTest {
        val api = FakeGroupTypeConfigApi(NetworkResult.Success(SEED_DTOS))
        val dao = FakeGroupTypeConfigDao()
        val store = provideGroupTypeConfigStore(api, dao)

        store.awaitFreshData()
        store.awaitFreshData()

        assertEquals(2, api.callCount, "an explicit fresh() read must re-drive the fetcher (SWR revalidation)")
    }

    // ─── fetcher error surfaces on the Store5 error channel ──────────────────
    @Test
    fun fetcher_error_surfaces_as_error_response() = runTest {
        val api = FakeGroupTypeConfigApi(NetworkResult.Error(NetworkError.SERVER))
        val dao = FakeGroupTypeConfigDao()
        val store = provideGroupTypeConfigStore(api, dao)

        val response = store.stream(StoreReadRequest.fresh(GROUP_TYPE_CONFIG_CATALOGUE_KEY))
            .first { it is StoreReadResponse.Error }

        val error = (response as StoreReadResponse.Error.Exception).error
        assertTrue(error is GroupTypeConfigFetchException, "error channel must carry the typed fetch exception")
        assertEquals(NetworkError.SERVER, error.networkError)
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private suspend fun Store<String, List<GroupTypeConfig>>.awaitFreshData(): List<GroupTypeConfig> {
        val response = stream(StoreReadRequest.fresh(GROUP_TYPE_CONFIG_CATALOGUE_KEY))
            .first { it is StoreReadResponse.Data<*> }
        @Suppress("UNCHECKED_CAST")
        return (response as StoreReadResponse.Data<List<GroupTypeConfig>>).value
    }
}

// ---------------------------------------------------------------------------
// Fakes + fixtures
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
    fun currentRows(): List<GroupTypeConfigEntity> = rows.value

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
