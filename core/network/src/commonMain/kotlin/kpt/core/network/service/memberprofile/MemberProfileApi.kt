/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.network.service.memberprofile

import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.network.model.MemberAccountsDto
import kpt.core.network.model.MemberProfileDto
import kpt.core.network.model.MemberRoleInfoDto
import kpt.core.network.model.UpdateMemberRoleRequestDto
import kpt.core.network.model.UpdateMemberRoleResponseDto

/**
 * Ktor client for the member-profile screen's client stack — three raw-Fineract reads
 * (identity, savings/loan accounts, member role datatable) and one raw-Fineract write (role
 * update, chairperson-gated). See `idea-layer/screens/member-profile/api.yaml#api` +
 * API.md#services for the endpoint contract.
 *
 * Every method returns [NetworkResult] — never a raw [Result] envelope, never a thrown
 * exception (Mandatory Rule 2). `NetworkResult`/`NetworkError` are the framework's
 * `core-base/network` sealed types (consumed, never edited — Hard Rule #8). This Service is
 * SERVICE-ONLY — the composite Store5 read-store wrapping [getClient]/[getClientAccounts]/
 * [getMemberRole] plus the write-invalidation repository wrapping [updateMemberRole] are emitted
 * by a downstream `kmp-store-gen`/`kmp-client-gen` generation step, not declared here.
 */
interface MemberProfileApi {

    /**
     * `GET /clients/{clientId}` (raw Fineract passthrough, `api.yaml#api[get_client]`). Fetches
     * the member's identity header (`displayName`/`firstname`/`lastname`/`mobileNo`/
     * `imagePresent`/`status`/`activationDate`/`officeId`). Cached stale-while-revalidate by the
     * caller per `data-flow.yaml` (`ttl=300`, offline `show_cached`) — this Service call itself
     * is unconditional, always hitting the network. 404 -> [NetworkError.NOT_FOUND] ("client not
     * found").
     */
    suspend fun getClient(clientId: String): NetworkResult<MemberProfileDto, NetworkError>

    /**
     * `GET /clients/{clientId}/accounts` (raw Fineract passthrough,
     * `api.yaml#api[get_client_accounts]`). Fetches the client's savings and loan account arrays.
     * Cached stale-while-revalidate by the caller per `data-flow.yaml` (`ttl=120`, offline
     * `show_cached`) — this Service call itself is unconditional, always hitting the network.
     * 404 -> [NetworkError.NOT_FOUND] ("client not found").
     */
    suspend fun getClientAccounts(clientId: String): NetworkResult<MemberAccountsDto, NetworkError>

    /**
     * `GET /datatables/dt_member_role/{clientId}` (raw Fineract custom-datatable passthrough,
     * `api.yaml#api[get_member_role]`). Fetches the member's role row(s) (`role`/`groupId`/
     * `assignedDate`) — the response is a literal array per `api.yaml#api[get_member_role]
     * .response.type`. Cached stale-while-revalidate by the caller per `data-flow.yaml`
     * (`ttl=300`, offline `show_cached`). 404 -> [NetworkError.NOT_FOUND] ("role record not
     * found").
     */
    suspend fun getMemberRole(clientId: String): NetworkResult<List<MemberRoleInfoDto>, NetworkError>

    /**
     * `PUT /datatables/dt_member_role/{clientId}` (raw Fineract custom-datatable passthrough,
     * `api.yaml#api[update_member_role]`) — the feature's single write signal. Server-side
     * authorization requires the caller be the group's chairperson; a non-chairperson caller
     * gets 403 -> [NetworkError.UNKNOWN] (no dedicated bucket, same convention as every other
     * `*ApiImpl` in this module). 400 -> [NetworkError.BAD_REQUEST] ("invalid role value"). No
     * repository/Store5 invalidation is wired here — that composition is the downstream
     * `kmp-store-gen`/`kmp-client-gen` step's responsibility.
     */
    suspend fun updateMemberRole(
        clientId: String,
        request: UpdateMemberRoleRequestDto,
    ): NetworkResult<UpdateMemberRoleResponseDto, NetworkError>
}
