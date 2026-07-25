/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.service.memberinvite

import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.network.model.CreateInviteRequestDto
import org.mifos.groupbanking.core.network.model.GeneratedInviteDto
import org.mifos.groupbanking.core.network.model.PendingInviteDto
import org.mifos.groupbanking.core.network.model.RevokeInviteResponseDto

/**
 * Ktor client for the organizer-side member-invite companion bridge — invite-token generation
 * (COMP-DT-002), pending-invite listing (COMP-DT-003), and invite revocation (COMP-DT-005). See
 * `idea-layer/screens/member-invite/api.yaml` + API.md#services for the endpoint contract.
 *
 * DISTINCT from [org.mifos.groupbanking.core.network.service.joinwithcode.InvitationApi] — that
 * service models the RECIPIENT consuming an invite (validate/associate/mark-accepted); this one
 * models the ORGANIZER issuing/listing/revoking them. Both address the same
 * `/companion/datatables/invitations` datatable but with disjoint HTTP verbs + response shapes,
 * so they are separate service seams rather than one bloated interface.
 *
 * Every method returns [NetworkResult] — never a raw [Result] envelope, never a thrown exception
 * (Mandatory Rule 2). The consuming
 * [org.mifos.groupbanking.core.data.repository.MemberInviteRepositoryImpl] switches on the sealed
 * result directly with no try-catch of its own (Mandatory Rule 4) — this Service is the ONLY
 * layer allowed a try-catch (in the Impl's `requestAsNetworkResult` helper).
 */
interface MemberInviteApi {

    /**
     * `POST /companion/datatables/invitations/{groupId}` (COMP-DT-002). Creates a single-use
     * invite-token row for [request]'s contact + role and returns the 6-char token, the shareable
     * deep-link URL, and the created row id. 409 (an active invite for this contact already
     * exists) has no dedicated [NetworkError] bucket and falls into [NetworkError.UNKNOWN], same
     * convention as every other `*ApiImpl` in this module.
     */
    suspend fun createInvite(
        groupId: Long,
        request: CreateInviteRequestDto,
    ): NetworkResult<GeneratedInviteDto, NetworkError>

    /**
     * `GET /companion/datatables/invitations/{groupId}` (COMP-DT-003). Fetches all pending
     * (unaccepted) invite rows for [groupId] — the companion API filters to `accepted_at IS NULL`
     * server-side.
     */
    suspend fun listPendingInvites(groupId: Long): NetworkResult<List<PendingInviteDto>, NetworkError>

    /**
     * `DELETE /companion/datatables/invitations/{groupId}/{rowId}` (COMP-DT-005). Revokes a
     * pending invite by deleting its datatable row. 404 -> [NetworkError.NOT_FOUND] (the row does
     * not exist or was already accepted/expired), which the caller maps to the optimistic-undo
     * path.
     */
    suspend fun revokeInvite(
        groupId: Long,
        rowId: Long,
    ): NetworkResult<RevokeInviteResponseDto, NetworkError>
}
