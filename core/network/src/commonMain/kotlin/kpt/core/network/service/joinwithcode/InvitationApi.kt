/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.network.service.joinwithcode

import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.network.model.AssociateClientsRequestDto
import kpt.core.network.model.AssociateClientsResponseDto
import kpt.core.network.model.GroupPreviewDto
import kpt.core.network.model.InvitationRowDto
import kpt.core.network.model.MarkAcceptedRequestDto
import kpt.core.network.model.MarkAcceptedResponseDto

/**
 * Ktor client for the join-with-code companion bridge — invite-token validation (COMP-DT-004),
 * group preview (companion bridge), member association (COMP-GRP-003), and invite-consumption
 * bookkeeping (COMP-DT-004 `PUT`). See `idea-layer/screens/join-with-code/api.yaml` +
 * API.md#services for the endpoint contract.
 *
 * Every method returns [NetworkResult] — never a raw [Result] envelope, never a thrown
 * exception (Mandatory Rule 2). `NetworkResult`/`NetworkError` are the framework's
 * `core-base/network` sealed types (consumed, never edited — Hard Rule #8).
 * [kpt.core.data.repository.InvitationRepositoryImpl] switches on the
 * sealed result directly, with no try-catch of its own (Mandatory Rule 4) — this Service is
 * SERVICE-ONLY; the join orchestration (associate-then-mark-accepted ordering) lives in the
 * Repository, not here.
 */
interface InvitationApi {

    /**
     * `GET /companion/datatables/invitations/{entityId}` (COMP-DT-004). Validates [code] (the
     * 6-char invite token) against the companion invitations datatable and returns the full
     * row — `group_id`, `inviter_client_id`, `invited_email_phone`, `role_to_assign`,
     * `expires_at`, and `accepted_at` (non-null means the code was already consumed). No-cache
     * per `api.yaml#validate_invite_token.cache` — token freshness is checked on every call.
     * 404 -> [NetworkError.NOT_FOUND] (caller maps this to "invalid code").
     */
    suspend fun validateInviteToken(code: String): NetworkResult<InvitationRowDto, NetworkError>

    /**
     * `GET /companion/groups/{groupId}` (companion bridge). Fetches the minimal group-preview
     * card (name, type, organizer, member count, role-to-assign) shown to the invitee before
     * they confirm joining. [groupId] is sourced from a prior [validateInviteToken] row's
     * `group_id`. Stale-while-revalidate cache per `api.yaml#get_group_preview.cache` (ttl=60s)
     * — caching itself is the caller's concern; this call always hits the network.
     */
    suspend fun getGroupPreview(groupId: Long): NetworkResult<GroupPreviewDto, NetworkError>

    /**
     * `POST /companion/groups/{groupId}/associate-clients` (COMP-GRP-003). Auto-associates the
     * authenticated invitee (`request.clientIds`) to [groupId] as a member with the role from
     * the invitation row (`request.roleToAssign`). 400 -> [NetworkError.BAD_REQUEST] (caller
     * maps this to "already a member" / invalid role); 403 (group closed to new members) has no
     * dedicated [NetworkError] bucket and falls into [NetworkError.UNKNOWN], same convention as
     * every other `*ApiImpl` in this module.
     */
    suspend fun associateClientToGroup(
        groupId: Long,
        request: AssociateClientsRequestDto,
    ): NetworkResult<AssociateClientsResponseDto, NetworkError>

    /**
     * `PUT /companion/datatables/invitations/{entityId}/{rowId}` (COMP-DT-004). Marks the
     * invitations datatable row for [code] as consumed (`accepted_at` = [request]'s timestamp)
     * so it cannot be reused. Called by the Repository AFTER [associateClientToGroup] succeeds
     * — this call itself is a plain network operation; the "best-effort, non-fatal on failure"
     * orchestration semantics (`flow.yaml#on_confirm_join` / `data-flow.yaml` OnConfirmJoin
     * notes) live in `InvitationRepositoryImpl.joinGroup`, not here.
     *
     * KNOWN CONTRACT GAP: `api.yaml#mark_invitation_accepted.params.rowId.source` declares
     * `validate_invite_token_response.id`, but neither `api.yaml#api[0].response.fields` nor
     * `#dtos.InvitationRow` declare an `id` field on that response (mirrors the same gap flagged
     * on [InvitationRowDto]). [rowId] is therefore NOT threaded from [validateInviteToken]'s
     * result inside this client stack — it is a caller-supplied parameter. Resolving this
     * requires either (a) the companion server adding `id`/`rowId` to the
     * `validate_invite_token` response, or (b) a documented alternate row-key lookup. Until
     * then, callers (ViewModel / feature layer) must obtain `rowId` some other way (e.g. a
     * dedicated datatable-row lookup, not currently modeled in `api.yaml`) — DO NOT fabricate a
     * value here.
     */
    suspend fun markInvitationAccepted(
        code: String,
        rowId: Long,
        request: MarkAcceptedRequestDto,
    ): NetworkResult<MarkAcceptedResponseDto, NetworkError>
}
