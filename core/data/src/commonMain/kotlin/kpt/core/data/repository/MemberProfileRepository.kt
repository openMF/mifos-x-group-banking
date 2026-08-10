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

import kotlinx.coroutines.CoroutineScope
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.base.store.screen.FetchPolicy
import kpt.core.base.store.screen.ScreenDataStream
import kpt.core.model.MemberProfileDetail
import kpt.core.model.UpdateMemberRoleRequest
import kpt.core.model.UpdateMemberRoleResult

/**
 * Read + write surface for the member-profile screen — the composite fan-in of `get_client` +
 * `get_client_accounts` + `get_member_role`, plus the chairperson-gated `update_member_role` write.
 *
 * Wraps the composite dynamic-key NETWORK_WITH_CACHE `MemberProfileStore`. Exposes exactly one read
 * path — [memberProfileStream], an offline-first [ScreenDataStream] of [MemberProfileDetail] keyed
 * by `clientId` — and one write path — [updateMemberRole], which routes the mutation through the
 * store (`store.clear(clientId)`) rather than a silent DAO write (RULE-IMPLEMENT-STORE5-001 S5-1),
 * so the still-active stream re-fetches the profile with the new role
 * (`data-flow.yaml#cache.strategy: invalidate`). No DAO-bypass read (S5-2), no `try-catch` on the
 * read, no `Result<T>` envelope — the read stream surfaces `ScreenState` directly; the write
 * surfaces the wire [NetworkResult] directly.
 *
 * See API.md#stores — MemberProfile.
 */
interface MemberProfileRepository {

    /**
     * Offline-first stream of the combined member-profile composite for [clientId].
     *
     * The store fires the three companion reads in parallel and fans them into one
     * [MemberProfileDetail]; a cached composite is served immediately then background-revalidated
     * per the store's stale-while-revalidate policy (`data-flow.yaml`: `stale_while_revalidate`,
     * `ttl=300`, `offline: show_cached`). Call [ScreenDataStream.retry] to re-drive a failed fetch
     * (the Retry CTA); pull-to-refresh maps to a [FetchPolicy.NETWORK_ONLY] re-collection.
     *
     * @param clientId The client (member) whose profile to stream (nav param).
     * @param scope CoroutineScope (typically `viewModelScope`) for the auto-refresh coroutine.
     * @param fetchPolicy Read policy. Defaults to [FetchPolicy.CACHE_FIRST_SWR] —
     *   stale-while-revalidate, matching the declared cache strategy.
     */
    fun memberProfileStream(
        clientId: String,
        scope: CoroutineScope,
        fetchPolicy: FetchPolicy = FetchPolicy.CACHE_FIRST_SWR,
    ): ScreenDataStream<MemberProfileDetail>

    /**
     * Chairperson-gated role update for [clientId] (`PUT /datatables/dt_member_role/{clientId}`,
     * `update_member_role`). On success the composite cache for [clientId] is INVALIDATED via the
     * store (`store.clear(clientId)`) — the declared `data-flow.yaml#cache.strategy: invalidate` —
     * so the next stream collection re-fetches the profile with the new role rather than serving a
     * stale one. On failure (400 invalid role / 403 not-chairperson / 401 / 500 / offline) the
     * cache is left intact and the wire [NetworkResult.Error] is surfaced verbatim for the snackbar
     * error path (`data-flow.yaml#error_paths`).
     *
     * @param clientId The client (member) whose role to update.
     * @param request The new role plus its `groupId` + `assignedDate` (the wire PUT body requires
     *   all three — carried by the domain [UpdateMemberRoleRequest] the prior domain layer declared).
     */
    suspend fun updateMemberRole(
        clientId: String,
        request: UpdateMemberRoleRequest,
    ): NetworkResult<UpdateMemberRoleResult, NetworkError>
}
