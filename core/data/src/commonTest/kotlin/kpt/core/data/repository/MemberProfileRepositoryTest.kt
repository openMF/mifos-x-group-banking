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
import kpt.core.database.memberprofile.dao.MemberProfileCacheDao
import kpt.core.database.memberprofile.entity.CachedMemberAccounts
import kpt.core.database.memberprofile.entity.CachedMemberIdentity
import kpt.core.database.memberprofile.entity.CachedMemberProfile
import kpt.core.database.memberprofile.entity.CachedMemberRole
import kpt.core.database.memberprofile.entity.MemberProfileCacheCodec
import kpt.core.database.memberprofile.entity.MemberProfileCacheEntity
import kpt.core.model.MemberRole
import kpt.core.model.UpdateMemberRoleRequest
import kpt.core.network.model.FineractStatusDto
import kpt.core.network.model.MemberAccountsDto
import kpt.core.network.model.MemberProfileDto
import kpt.core.network.model.MemberRoleDto
import kpt.core.network.model.MemberRoleInfoDto
import kpt.core.network.model.MemberSavingsAccountDto
import kpt.core.network.model.UpdateMemberRoleRequestDto
import kpt.core.network.model.UpdateMemberRoleResponseDto
import kpt.core.network.service.memberprofile.MemberProfileApi
import kpt.core.store.memberprofile.impl.provideMemberProfileStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD coverage for [MemberProfileRepositoryImpl] — verifies (1) the repository surfaces the
 * composite store as an offline-first [kpt.core.base.store.screen.ScreenDataStream] via
 * `.asScreenStream()`, mapping the combined snapshot into `ScreenState.Content<MemberProfileDetail>`;
 * (2) a successful `update_member_role` write INVALIDATES the per-client composite cache through the
 * store (`store.clear(clientId)`) so the profile re-fetches with the new role
 * (`data-flow.yaml#cache.strategy: invalidate`, RULE-IMPLEMENT-STORE5-001 S5-1 — not a DAO bypass);
 * and (3) a failed write leaves the cache intact and surfaces the wire error verbatim.
 */
class MemberProfileRepositoryTest {

    @Test
    fun stream_emits_Content_with_combined_profile_when_online() = runTest {
        val api = FakeMemberProfileApi()
        val dao = FakeMemberProfileDao()
        val repo = repository(api, dao, online = true)

        val state = repo.memberProfileStream(
            clientId = "client-42",
            scope = backgroundScope,
            fetchPolicy = FetchPolicy.NETWORK_WITH_CACHE,
        ).state.first { it is ScreenState.Content }

        val data = (state as ScreenState.Content).data
        assertEquals(42L, data.member.id)
        assertEquals("Asha Kumar", data.member.displayName)
        assertEquals(MemberRoleDto.TREASURER.name, data.roles.single().role.name)
    }

    @Test
    fun updateMemberRole_success_invalidates_cache_so_profile_refetches() = runTest {
        val api = FakeMemberProfileApi()
        // Seed a cached composite so we can observe the invalidation delete the SoT row.
        val dao = FakeMemberProfileDao().apply { seed(cacheEntity("client-42", "Cached Asha")) }
        val repo = repository(api, dao, online = true)

        assertEquals(1, dao.currentRows().size, "precondition: a cached composite row exists")

        val result = repo.updateMemberRole(
            clientId = "client-42",
            request = UpdateMemberRoleRequest(role = MemberRole.CHAIRPERSON, groupId = 7L, assignedDate = "2026-07-21"),
        )

        assertTrue(result is NetworkResult.Success, "a successful PUT surfaces the resource id")
        assertEquals(1, api.updateCalls, "the write must hit update_member_role exactly once")
        assertEquals(
            0,
            dao.currentRows().size,
            "a successful role update must invalidate the per-client composite cache (store.clear) so the stream re-fetches with the new role",
        )
    }

    @Test
    fun updateMemberRole_failure_leaves_cache_intact_and_surfaces_error() = runTest {
        val api = FakeMemberProfileApi(updateResult = NetworkResult.Error(NetworkError.UNKNOWN))
        val dao = FakeMemberProfileDao().apply { seed(cacheEntity("client-42", "Cached Asha")) }
        val repo = repository(api, dao, online = true)

        val result = repo.updateMemberRole(
            clientId = "client-42",
            request = UpdateMemberRoleRequest(role = MemberRole.CHAIRPERSON, groupId = 7L, assignedDate = "2026-07-21"),
        )

        assertTrue(result is NetworkResult.Error, "a 403 not-chairperson (or any error) surfaces verbatim")
        assertEquals(NetworkError.UNKNOWN, (result as NetworkResult.Error).error)
        assertEquals(1, dao.currentRows().size, "a failed write must NOT invalidate the cache")
    }

    // --- wiring ----------------------------------------------------------------------------------

    private fun repository(
        api: MemberProfileApi,
        dao: MemberProfileCacheDao,
        online: Boolean,
    ): MemberProfileRepository {
        val initial = if (online) {
            NetworkStatus.Available(NetworkInfo(type = NetworkType.WiFi, isMetered = false))
        } else {
            NetworkStatus.Unavailable
        }
        return MemberProfileRepositoryImpl(
            memberProfileStore = provideMemberProfileStore(api, dao),
            memberProfileApi = api,
            networkMonitor = FakeNetworkMonitor(initial),
            fetchedAtRepository = InMemoryFetchedAtRepository(),
        )
    }
}

// ---------------------------------------------------------------------------
// Fakes
// ---------------------------------------------------------------------------

private class FakeMemberProfileApi(
    private val updateResult: NetworkResult<UpdateMemberRoleResponseDto, NetworkError> =
        NetworkResult.Success(UpdateMemberRoleResponseDto(resourceId = 1L)),
) : MemberProfileApi {
    var updateCalls: Int = 0
        private set

    override suspend fun getClient(clientId: String): NetworkResult<MemberProfileDto, NetworkError> =
        NetworkResult.Success(
            MemberProfileDto(
                id = 42L,
                displayName = "Asha Kumar",
                firstName = "Asha",
                lastName = "Kumar",
                mobileNo = "+254700000000",
                imagePresent = true,
                status = FineractStatusDto(id = 300, value = "Active"),
                activationDate = "2025-06-01",
                officeId = 100L,
            ),
        )

    override suspend fun getClientAccounts(clientId: String): NetworkResult<MemberAccountsDto, NetworkError> =
        NetworkResult.Success(
            MemberAccountsDto(
                savingsAccounts = listOf(
                    MemberSavingsAccountDto(
                        id = 1L,
                        productName = "Voluntary Savings",
                        accountNo = "SA-1",
                        balance = 720.0,
                        status = FineractStatusDto(id = 300, value = "Active"),
                    ),
                ),
                loanAccounts = emptyList(),
            ),
        )

    override suspend fun getMemberRole(clientId: String): NetworkResult<List<MemberRoleInfoDto>, NetworkError> =
        NetworkResult.Success(listOf(MemberRoleInfoDto(role = MemberRoleDto.TREASURER, groupId = 7L, assignedDate = "2026-01-01")))

    override suspend fun updateMemberRole(
        clientId: String,
        request: UpdateMemberRoleRequestDto,
    ): NetworkResult<UpdateMemberRoleResponseDto, NetworkError> {
        updateCalls++
        return updateResult
    }
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
