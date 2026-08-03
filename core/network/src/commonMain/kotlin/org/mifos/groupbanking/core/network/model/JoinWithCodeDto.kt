/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire DTO for a single row of the companion invitations datatable — `GET
 * /companion/datatables/invitations/{entityId}` (COMP-DT-004; entityId is the 6-char invite
 * code). This endpoint surfaces RAW Fineract datatable columns (snake_case `@SerialName`s —
 * `group_id`, `inviter_client_id`, `invited_email_phone`, `role_to_assign`, `expires_at`,
 * `accepted_at`), distinct from the companion bridge's normalized camelCase convention used by
 * [GroupPreviewDto] / [AssociateClientsRequestDto] below. [acceptedAt] is nullable — `null` means
 * the code has not been used yet; a non-null value means it was already consumed and the join
 * must be rejected client-side (`JoinError.AlreadyUsed`). [roleToAssign] reuses [GroupRoleDto]
 * (the login-signup / `GroupMembership.role` wire enum) rather than introducing a new one — its
 * value-set (`ORGANIZER`/`MEMBER`/`TREASURER`/`SECRETARY`/`UNKNOWN`) exactly matches every role an
 * invite can assign (invites never assign `CHAIRPERSON`, so [ViewerRoleDto] was not the fit).
 *
 * NOTE (flagged for the cross-feature repair station): `mark_invitation_accepted`'s `rowId` param
 * is declared as sourced from `validate_invite_token_response.id`, but neither
 * `idea-layer/screens/join-with-code/api.yaml#api[0].response.fields` nor `#dtos.InvitationRow`
 * declare an `id` field on this response — this DTO therefore does not carry one either (Hard
 * Rule 4 forbids inventing an undeclared field). The repository/use-case layer wiring
 * `mark_invitation_accepted`'s `rowId` will need this contract gap resolved upstream.
 *
 * See API.md#dtos — InvitationRow.
 */
@Serializable
data class InvitationRowDto(
    @SerialName("token") val token: String,
    @SerialName("group_id") val groupId: Long,
    @SerialName("inviter_client_id") val inviterClientId: Long,
    @SerialName("invited_email_phone") val invitedEmailPhone: String,
    @SerialName("role_to_assign") val roleToAssign: GroupRoleDto = GroupRoleDto.UNKNOWN,
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("accepted_at") val acceptedAt: String? = null,
) {
    companion object {
        /** Bumped when this DTO shape changes (registry `version:` driven). See EC30. */
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire DTO for the group preview card shown to the invitee before they confirm joining — `GET
 * /companion/groups/{groupId}` (companion bridge; camelCase, unlike [InvitationRowDto]'s raw
 * datatable columns). [groupType] reuses [GroupTypeSlugDto] (the same long-form group-type wire
 * enum already backing `GroupTypeConfigDto.typeSlug`) rather than the short-form [GroupTypeDto]
 * used by `GroupDto` — this endpoint is a companion-bridge group lookup, not the group-list row
 * contract, so it is expected to emit the same long-form slugs as COMP-DT-003. [roleToAssign]
 * reuses [GroupRoleDto] (see [InvitationRowDto] kdoc for the reuse rationale).
 *
 * See API.md#dtos — GroupPreview.
 */
@Serializable
data class GroupPreviewDto(
    @SerialName("groupId") val groupId: Long,
    @SerialName("groupName") val groupName: String,
    @SerialName("groupType") val groupType: GroupTypeSlugDto = GroupTypeSlugDto.UNKNOWN,
    @SerialName("organizerName") val organizerName: String,
    @SerialName("memberCount") val memberCount: Int,
    @SerialName("officeId") val officeId: Long,
    @SerialName("roleToAssign") val roleToAssign: GroupRoleDto = GroupRoleDto.UNKNOWN,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire request DTO to auto-associate the authenticated invitee to the group as a member — `POST
 * /companion/groups/{groupId}/associate-clients` (COMP-GRP-003). [roleToAssign] reuses
 * [GroupRoleDto] (see [InvitationRowDto] kdoc).
 *
 * See API.md#dtos — AssociateClientsRequest.
 */
@Serializable
data class AssociateClientsRequestDto(
    @SerialName("clientIds") val clientIds: List<Long>,
    @SerialName("roleToAssign") val roleToAssign: GroupRoleDto = GroupRoleDto.UNKNOWN,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire response DTO for COMP-GRP-003 — the standard Fineract command-processing envelope
 * (`resourceId`) plus the echoed `groupId` + `clientIds`.
 *
 * See API.md#dtos — AssociateClientsResponse.
 */
@Serializable
data class AssociateClientsResponseDto(
    @SerialName("resourceId") val resourceId: Long,
    @SerialName("groupId") val groupId: Long,
    @SerialName("clientIds") val clientIds: List<Long>,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire request DTO to mark the invitations datatable row as consumed — `PUT
 * /companion/datatables/invitations/{entityId}/{rowId}` (COMP-DT-004). Raw datatable column
 * (`accepted_at`, snake_case), matching [InvitationRowDto]'s wire convention for this endpoint
 * family.
 *
 * See API.md#dtos — MarkAcceptedRequest.
 */
@Serializable
data class MarkAcceptedRequestDto(
    @SerialName("accepted_at") val acceptedAt: String,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire response DTO for the mark-accepted datatable update — the standard Fineract
 * command-processing envelope (`resourceId`) plus a nested `changes` object echoing the updated
 * column. See API.md#dtos — MarkAcceptedResponse.
 */
@Serializable
data class MarkAcceptedResponseDto(
    @SerialName("resourceId") val resourceId: Long,
    @SerialName("changes") val changes: MarkAcceptedChangesDto,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Nested `changes` object of [MarkAcceptedResponseDto] — echoes the updated datatable column
 * (raw snake_case, matching [MarkAcceptedRequestDto]).
 *
 * See API.md#dtos — MarkAcceptedResponse.changes.
 */
@Serializable
data class MarkAcceptedChangesDto(
    @SerialName("accepted_at") val acceptedAt: String,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}
