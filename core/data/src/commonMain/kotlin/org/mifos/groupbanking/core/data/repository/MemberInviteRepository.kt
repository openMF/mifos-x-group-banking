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
import org.mifos.groupbanking.core.model.CreateInviteRequest
import org.mifos.groupbanking.core.model.GeneratedInvite
import org.mifos.groupbanking.core.model.PendingInvite

/**
 * Repository seam for the organizer-side `member-invite` screen
 * (`idea-layer/screens/member-invite`).
 *
 * **Store5-free (submit-mutation branch).** `business_logic.kind: crud` with no `AppStoreRegistry`
 * entry — every method surfaces [NetworkResult] directly (the pending-invites read is a plain
 * suspend fetch re-invoked by the ViewModel after each mutation, NOT a Store5 `.asScreenStream()`
 * read; `data-flow.yaml#sync_queue.entries: []` — invite generation/revocation require
 * connectivity and are never offline-queued). Exactly the same branch as
 * [MeetingConductRepository] / `LoanApplyRepository` / `GroupCreateRepository`. Nothing here
 * imports `org.mobilenativefoundation.store`.
 *
 * DISTINCT from [InvitationRepository] (the recipient-side join-with-code flow) — see
 * `MemberInvite.kt` domain-model KDoc for the organizer-vs-recipient bounded-context split.
 *
 * No try-catch anywhere in the Impl (Mandatory Rule 4) — every branch is a plain `when` over the
 * [MemberInviteApi][org.mifos.groupbanking.core.network.service.memberinvite.MemberInviteApi]'s
 * [NetworkResult].
 *
 * See API.md#repositories — MemberInviteRepository.
 */
interface MemberInviteRepository {

    /**
     * `POST /companion/datatables/invitations/{groupId}` (COMP-DT-002). Issues a single-use invite
     * token for [request]'s contact + role and returns the token + deep-link + row id.
     */
    suspend fun createInvite(request: CreateInviteRequest): NetworkResult<GeneratedInvite, NetworkError>

    /**
     * `GET /companion/datatables/invitations/{groupId}` (COMP-DT-003). Fetches the current pending
     * (unaccepted) invite rows for [groupId]. Re-invoked by the ViewModel on mount and after each
     * successful generate/revoke to refresh the list (`data-flow.yaml` OnGenerateInvite /
     * OnRevokeInvite reload notes).
     */
    suspend fun listPendingInvites(groupId: Long): NetworkResult<List<PendingInvite>, NetworkError>

    /**
     * `DELETE /companion/datatables/invitations/{groupId}/{rowId}` (COMP-DT-005). Revokes a pending
     * invite by row id. The ViewModel removes the row optimistically first and restores it on a
     * non-success result (`data-flow.yaml` OnRevokeInvite undo_optimistic notes).
     */
    suspend fun revokeInvite(groupId: Long, rowId: Long): NetworkResult<Unit, NetworkError>
}
