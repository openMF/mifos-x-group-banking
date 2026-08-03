/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.data.repository

import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.base.store.infra.FetchedAtRepository
import kpt.core.base.store.screen.FetchPolicy
import kpt.core.base.store.screen.ScreenDataStream
import kpt.core.base.store.screen.asScreenStream
import kpt.core.store.AppStoreRegistry
import org.mifos.groupbanking.core.model.MemberProfileDetail
import org.mifos.groupbanking.core.model.UpdateMemberRoleRequest
import org.mifos.groupbanking.core.model.UpdateMemberRoleResult
import org.mifos.groupbanking.core.network.mapper.toDomainModel
import org.mifos.groupbanking.core.network.mapper.toDto
import org.mifos.groupbanking.core.network.service.memberprofile.MemberProfileApi
import org.mobilenativefoundation.store.store5.Store

/**
 * Store5-backed implementation of [MemberProfileRepository].
 *
 * The composite dynamic-key read maps `clientId` to the store key and goes exclusively through
 * [Store.asScreenStream] so the whole offline-first pipeline (cached emit -> background revalidate
 * -> DecisionEngine -> ScreenState) is inherited from `core-base`. The freshness [cacheKey] is
 * per-client (`memberprofile:{clientId}`) so each member's TTL window is tracked independently. No
 * DAO-bypass read, no `try-catch`, no `Result` envelope on the read (RULE-IMPLEMENT-STORE5-001 S5-2).
 *
 * The single write ([updateMemberRole]) is a fire-and-invalidate mutation: it calls
 * [MemberProfileApi.updateMemberRole] and, on success, INVALIDATES the composite cache for that
 * client through the store ([Store.clear] deletes the in-memory cache AND the SoT row) rather than
 * writing to the DAO directly (S5-1). The still-active [memberProfileStream] then re-fetches the
 * profile with the new role — the declared `data-flow.yaml#cache.strategy: invalidate`. A plain
 * `when` over the wire [NetworkResult] — no try-catch, no `Result<T>` envelope.
 *
 * See API.md#stores — MemberProfile.
 */
class MemberProfileRepositoryImpl(
    private val memberProfileStore: Store<String, MemberProfileDetail>,
    private val memberProfileApi: MemberProfileApi,
    private val networkMonitor: NetworkMonitor,
    private val fetchedAtRepository: FetchedAtRepository,
) : MemberProfileRepository {

    override fun memberProfileStream(
        clientId: String,
        scope: CoroutineScope,
        fetchPolicy: FetchPolicy,
    ): ScreenDataStream<MemberProfileDetail> {
        return memberProfileStore.asScreenStream(
            key = clientId,
            networkMonitor = networkMonitor,
            fetchedAtRepository = fetchedAtRepository,
            cacheKey = "$CACHE_KEY_PREFIX$clientId",
            scope = scope,
            // A single MemberProfileDetail composite is never "empty" once present — Content always.
            isEmpty = { false },
            fetchPolicy = fetchPolicy,
            ttl = AppStoreRegistry.Ttl.MEMBER_PROFILE,
        )
    }

    override suspend fun updateMemberRole(
        clientId: String,
        request: UpdateMemberRoleRequest,
    ): NetworkResult<UpdateMemberRoleResult, NetworkError> {
        return when (val result = memberProfileApi.updateMemberRole(clientId, request.toDto())) {
            is NetworkResult.Success -> {
                // data-flow.yaml#cache.strategy: invalidate — route the mutation through the store
                // (deletes the in-memory cache + the SoT row for this client) so the active
                // memberProfileStream re-fetches with the new role. NOT a silent DAO write (S5-1).
                memberProfileStore.clear(clientId)
                NetworkResult.Success(result.data.toDomainModel())
            }
            is NetworkResult.Error -> result
        }
    }

    private companion object {
        /** FetchedAtRepository key prefix — one freshness timestamp per client. */
        const val CACHE_KEY_PREFIX = "memberprofile:"
    }
}
