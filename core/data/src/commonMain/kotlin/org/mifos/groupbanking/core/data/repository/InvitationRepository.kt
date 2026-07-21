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

import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.model.GroupPreview
import org.mifos.groupbanking.core.model.GroupRole
import org.mifos.groupbanking.core.model.Invitation
import org.mifos.groupbanking.core.model.JoinGroupResult

/**
 * Join-with-code mutation-orchestration repository (COMP-DT-004 + COMP-GRP-003). Wraps
 * `InvitationApi` (core/network).
 *
 * **Store5 branch (SP-04):** `join-with-code`'s `business_logic.kind` is a mutation
 * orchestration flow (`processor`) — every endpoint is `writable: false` no-cache or
 * `writable: true` (`api.yaml#cache_strategy.default: no-cache`); there is no read-stream to
 * back with a Store5 cache. Per RULE-IMPLEMENT-STORE5-001 / RULE-IDEA-IMPL-INTELLIGENCE-001
 * this repository surfaces [NetworkResult] directly rather than `.asScreenStream()` /
 * `.asPagingScreenStream()` / `MutableStore.write(...)` — same branch as [AuthRepositoryImpl].
 * No `org.mobilenativefoundation.store` import anywhere in this stack.
 *
 * No try-catch anywhere in this repository (Mandatory Rule 4) — every implementation method is
 * a plain `when` over the service's sealed [NetworkResult]; `InvitationApiImpl` is the sole
 * layer allowed to catch exceptions.
 *
 * See API.md#repositories — InvitationRepository.
 */
interface InvitationRepository {

    /**
     * Validates [code] (the 6-char invite token) against the companion invitations datatable
     * and returns the mapped domain [Invitation] on success. A `404` surfaces as
     * `NetworkResult.Error(NetworkError.NOT_FOUND)` — callers (ViewModel / feature layer) map
     * this to `JoinError.InvalidCode` per `flow.yaml#on_validate_code.on_error.classify_error`.
     * On success, callers are expected to call [Invitation.isExpired] / [Invitation.isAlreadyUsed]
     * themselves (per `flow.yaml#on_validate_code.check_expiry` / `check_already_accepted`) —
     * this repository does not pre-empt that screen-level branching, it only surfaces the raw
     * validated row.
     */
    suspend fun validateCode(code: String): NetworkResult<Invitation, NetworkError>

    /**
     * Fetches the group-preview card (name, type, organizer, member count, role-to-assign) for
     * [groupId] — sourced from a prior [validateCode] result's `groupId`.
     */
    suspend fun fetchGroupPreview(groupId: Long): NetworkResult<GroupPreview, NetworkError>

    /**
     * Orchestrates the join confirmation (`flow.yaml#on_confirm_join`): associates [clientId] to
     * [groupId] with [role] (COMP-GRP-003), then — ONLY if that succeeds — marks the invitations
     * datatable row (keyed by [code] + [rowId]) as accepted (COMP-DT-004 `PUT`). The
     * mark-accepted call is BEST-EFFORT / non-fatal: per `flow.yaml#on_confirm_join` and
     * `data-flow.yaml` OnConfirmJoin notes ("association succeeded is still a completed join even
     * if the accepted_at bookkeeping call fails"), a mark-accepted failure is logged but does
     * NOT fail the overall [joinGroup] call — the returned [NetworkResult.Success] still carries
     * the [JoinGroupResult] from the association step. An association failure, by contrast, DOES
     * short-circuit the whole call (mark-accepted is never attempted) and its [NetworkResult.Error]
     * is returned as-is.
     *
     * KNOWN CONTRACT GAP — [rowId]: `api.yaml#mark_invitation_accepted.params.rowId.source`
     * declares `validate_invite_token_response.id`, but neither `api.yaml`'s
     * `validate_invite_token` response fields nor `dtos.InvitationRow` declare an `id` field (see
     * [org.mifos.groupbanking.core.network.model.InvitationRowDto] KDoc for the same gap on the
     * wire side). [rowId] is therefore NOT derivable from [validateCode]'s result inside this
     * client stack — it is exposed here as an explicit caller-supplied parameter rather than
     * fabricated. Resolving this requires either (a) the companion server adding an `id`/`rowId`
     * field to the `validate_invite_token` response, or (b) a documented alternate datatable-row
     * lookup not currently modeled in `api.yaml`.
     */
    suspend fun joinGroup(
        groupId: Long,
        clientId: Long,
        role: GroupRole,
        code: String,
        rowId: Long,
    ): NetworkResult<JoinGroupResult, NetworkError>
}
