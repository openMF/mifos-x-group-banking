/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.store.memberprofile

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.database.memberprofile.dao.MemberProfileCacheDao
import kpt.core.database.memberprofile.entity.CachedMemberAccounts
import kpt.core.database.memberprofile.entity.CachedMemberIdentity
import kpt.core.database.memberprofile.entity.CachedMemberProfile
import kpt.core.database.memberprofile.entity.CachedMemberRole
import kpt.core.database.memberprofile.entity.MemberProfileCacheCodec
import kpt.core.database.memberprofile.entity.MemberProfileCacheEntity
import kpt.core.model.MemberProfileDetail
import kpt.core.network.model.FineractStatusDto
import kpt.core.network.model.MemberAccountsDto
import kpt.core.network.model.MemberLoanAccountDto
import kpt.core.network.model.MemberLoanAccountSummaryDto
import kpt.core.network.model.MemberProfileDto
import kpt.core.network.model.MemberRoleDto
import kpt.core.network.model.MemberRoleInfoDto
import kpt.core.network.model.MemberSavingsAccountDto
import kpt.core.network.model.UpdateMemberRoleRequestDto
import kpt.core.network.model.UpdateMemberRoleResponseDto
import kpt.core.network.service.memberprofile.MemberProfileApi
import kpt.core.store.memberprofile.impl.MemberProfileFetchException
import kpt.core.store.memberprofile.impl.provideMemberProfileStore
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD coverage for [provideMemberProfileStore] — the COMPOSITE dynamic-key NETWORK_WITH_CACHE
 * member-profile store (`get_client` + `get_client_accounts` + `get_member_role` fetched in
 * parallel and fanned-in client-side into one [MemberProfileDetail]).
 *
 * Exercises the Store5 read pipeline directly (parallel-combine fetcher + Room-shaped
 * SourceOfTruth + validator) with an in-memory [FakeMemberProfileDao] and a scripted
 * [FakeMemberProfileApi]; no Room runtime and no NetworkMonitor at this layer. The composite axis
 * is asserted explicitly: all-three-succeed combines into one [MemberProfileDetail]; ANY critical
 * read failing throws to Store5's error channel and persists nothing; a cache hit is served from
 * the SoT without any network fetch; and a fresh read re-drives all three parallel calls (SWR
 * revalidation).
 */
class MemberProfileStoreTest {

    // --- cache-miss: all three reads succeed -> parallel-combine into one profile, persisted -----
    @Test
    fun cache_miss_all_reads_succeed_combines_persists_and_emits() = runTest {
        val api = FakeMemberProfileApi()
        val dao = FakeMemberProfileDao()
        val store = provideMemberProfileStore(api, dao)

        val data = store.awaitFreshData("client-42")

        assertEquals(42L, data.member.id)
        assertEquals("Asha Kumar", data.member.displayName)
        assertEquals(720.0, data.accounts.savingsBalance, "savings balance must fan-in + aggregate from get_client_accounts")
        assertEquals(1, data.roles.size, "role datatable rows must fan-in from get_member_role")
        assertEquals(1, api.getClientCalls, "get_client runs exactly once on cache miss")
        assertEquals(1, api.getClientAccountsCalls, "get_client_accounts runs exactly once on cache miss")
        assertEquals(1, api.getMemberRoleCalls, "get_member_role runs exactly once on cache miss")
        assertEquals(1, dao.currentRows().size, "combined profile must be written through to the SoT")
        assertEquals("client-42", dao.currentRows().single().clientId)
    }

    // --- composite failure: one critical read fails -> the whole fetch surfaces an error ---------
    @Test
    fun one_critical_read_fails_surfaces_error_response() = runTest {
        val api = FakeMemberProfileApi(
            accountsResult = NetworkResult.Error(NetworkError.SERVER),
        )
        val dao = FakeMemberProfileDao()
        val store = provideMemberProfileStore(api, dao)

        val response = store.stream(StoreReadRequest.fresh("client-42"))
            .first { it is StoreReadResponse.Error }

        val error = (response as StoreReadResponse.Error.Exception).error
        assertTrue(error is MemberProfileFetchException, "any critical read failing must throw the typed fetch exception")
        assertEquals(NetworkError.SERVER, error.networkError)
        assertEquals(0, dao.currentRows().size, "a failed composite fetch must not persist a partial snapshot")
    }

    // --- cache-hit -> SoT emit, none of the three endpoints called -------------------------------
    @Test
    fun cache_hit_emits_from_sot_without_fetcher_call() = runTest {
        val api = FakeMemberProfileApi()
        val dao = FakeMemberProfileDao().apply { seed(cacheEntity("client-42", displayName = "Cached Asha")) }
        val store = provideMemberProfileStore(api, dao)

        val response = store.stream(StoreReadRequest.cached("client-42", refresh = false))
            .first { it is StoreReadResponse.Data<*> }

        @Suppress("UNCHECKED_CAST")
        val data = (response as StoreReadResponse.Data<MemberProfileDetail>).value
        assertEquals("Cached Asha", data.member.displayName)
        assertEquals(0, api.getClientCalls, "cache hit must be served from the SoT without a network fetch")
        assertEquals(0, api.getClientAccountsCalls)
        assertEquals(0, api.getMemberRoleCalls)
    }

    // --- validator-expiry / refresh -> re-fetch all three (SWR revalidation) ---------------------
    @Test
    fun refresh_refetches_all_three_reads_from_api() = runTest {
        val api = FakeMemberProfileApi()
        val dao = FakeMemberProfileDao()
        val store = provideMemberProfileStore(api, dao)

        store.awaitFreshData("client-42")
        store.awaitFreshData("client-42")

        assertEquals(2, api.getClientCalls, "an explicit fresh() read must re-drive get_client (SWR revalidation)")
        assertEquals(2, api.getClientAccountsCalls)
        assertEquals(2, api.getMemberRoleCalls)
    }

    // --- helpers ---------------------------------------------------------------------------------

    private suspend fun Store<String, MemberProfileDetail>.awaitFreshData(key: String): MemberProfileDetail {
        val response = stream(StoreReadRequest.fresh(key))
            .first { it is StoreReadResponse.Data<*> }
        @Suppress("UNCHECKED_CAST")
        return (response as StoreReadResponse.Data<MemberProfileDetail>).value
    }
}

// ---------------------------------------------------------------------------
// Fakes + fixtures
// ---------------------------------------------------------------------------

private class FakeMemberProfileApi(
    private val clientResult: NetworkResult<MemberProfileDto, NetworkError> =
        NetworkResult.Success(memberProfileDto()),
    private val accountsResult: NetworkResult<MemberAccountsDto, NetworkError> =
        NetworkResult.Success(memberAccountsDto()),
    private val roleResult: NetworkResult<List<MemberRoleInfoDto>, NetworkError> =
        NetworkResult.Success(listOf(MemberRoleInfoDto(role = MemberRoleDto.TREASURER, groupId = 7L, assignedDate = "2026-01-01"))),
) : MemberProfileApi {
    var getClientCalls: Int = 0
        private set
    var getClientAccountsCalls: Int = 0
        private set
    var getMemberRoleCalls: Int = 0
        private set

    override suspend fun getClient(clientId: String): NetworkResult<MemberProfileDto, NetworkError> {
        getClientCalls++
        return clientResult
    }

    override suspend fun getClientAccounts(clientId: String): NetworkResult<MemberAccountsDto, NetworkError> {
        getClientAccountsCalls++
        return accountsResult
    }

    override suspend fun getMemberRole(clientId: String): NetworkResult<List<MemberRoleInfoDto>, NetworkError> {
        getMemberRoleCalls++
        return roleResult
    }

    override suspend fun updateMemberRole(
        clientId: String,
        request: UpdateMemberRoleRequestDto,
    ): NetworkResult<UpdateMemberRoleResponseDto, NetworkError> =
        NetworkResult.Success(UpdateMemberRoleResponseDto(resourceId = 1L))
}

private class FakeMemberProfileDao : MemberProfileCacheDao {
    private val rows = MutableStateFlow<List<MemberProfileCacheEntity>>(emptyList())

    fun seed(entity: MemberProfileCacheEntity) {
        rows.value = listOf(entity)
    }
    fun currentRows(): List<MemberProfileCacheEntity> = rows.value

    override fun observeByKey(clientId: String): Flow<MemberProfileCacheEntity?> =
        rows.map { list -> list.firstOrNull { it.clientId == clientId } }

    override suspend fun upsert(entity: MemberProfileCacheEntity) {
        val byKey = rows.value.associateBy { it.clientId }.toMutableMap()
        byKey[entity.clientId] = entity
        rows.value = byKey.values.toList()
    }

    override suspend fun deleteByKey(clientId: String) {
        rows.value = rows.value.filterNot { it.clientId == clientId }
    }

    override suspend fun deleteAll() {
        rows.value = emptyList()
    }

    override suspend fun replaceForKey(entity: MemberProfileCacheEntity) {
        upsert(entity)
    }

    override suspend fun deleteOlderThan(epochMillis: Long) {
        rows.value = rows.value.filter { it.fetchedAt >= epochMillis }
    }
}

private fun memberProfileDto(displayName: String = "Asha Kumar") = MemberProfileDto(
    id = 42L,
    displayName = displayName,
    firstName = "Asha",
    lastName = "Kumar",
    mobileNo = "+254700000000",
    imagePresent = true,
    status = FineractStatusDto(id = 300, value = "Active"),
    activationDate = "2025-06-01",
    officeId = 100L,
)

private fun memberAccountsDto() = MemberAccountsDto(
    savingsAccounts = listOf(
        MemberSavingsAccountDto(id = 1L, productName = "Voluntary Savings", accountNo = "SA-1", balance = 500.0, status = FineractStatusDto(id = 300, value = "Active")),
        MemberSavingsAccountDto(id = 2L, productName = "Cycle Savings", accountNo = "SA-2", balance = 220.0, status = FineractStatusDto(id = 300, value = "Active")),
    ),
    loanAccounts = listOf(
        MemberLoanAccountDto(
            id = 9L,
            productName = "Group Loan",
            accountNo = "LN-9",
            status = FineractStatusDto(id = 300, value = "Active"),
            summary = MemberLoanAccountSummaryDto(principalDisbursed = 1000.0, principalOutstanding = 400.0, totalOverdue = 50.0),
        ),
    ),
)

private fun cacheEntity(clientId: String, displayName: String): MemberProfileCacheEntity {
    val payload = CachedMemberProfile(
        member = CachedMemberIdentity(
            id = 42L,
            displayName = displayName,
            firstName = "Asha",
            lastName = "Kumar",
            phone = "+254700000000",
            hasPhoto = true,
            statusId = 300,
            statusValue = "Active",
            joinDate = "2025-06-01",
            officeId = 100L,
        ),
        accounts = CachedMemberAccounts(
            savingsBalance = 720.0,
            savingsHistory = emptyList(),
            activeLoan = null,
        ),
        roles = listOf(CachedMemberRole(role = "TREASURER", groupId = 7L, assignedDate = "2026-01-01")),
    )
    return MemberProfileCacheEntity(
        clientId = clientId,
        profileJson = MemberProfileCacheCodec.encode(payload),
        fetchedAt = 1L,
    )
}
