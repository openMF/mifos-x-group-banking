/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire request DTO for the organizer-side generate-invite call — `POST
 * /companion/datatables/invitations/{groupId}` (COMP-DT-002,
 * `idea-layer/screens/member-invite/api.yaml#api.create_invite`). Raw companion invitations
 * datatable columns (snake_case `@SerialName`s — `invited_email_phone`, `role_to_assign`,
 * `expires_at`), matching the [InvitationRowDto] wire convention for this endpoint family.
 *
 * `role_to_assign` is a plain [String] here (NOT [GroupRoleDto]/[MemberRoleDto]): `api.yaml`
 * declares it as `type: String, enum: [member, treasurer, secretary, chairperson]` — lowercase
 * values that do NOT match either enum's UPPERCASE `@SerialName`s, and `chairperson` is absent
 * from `GroupRoleDto` entirely. The lowercase wire string is mapped to/from the shared
 * `MemberRole` domain enum at the `MemberInviteMappers.kt` boundary.
 *
 * See API.md#dtos — CreateInviteRequest.
 */
@Serializable
data class CreateInviteRequestDto(
    @SerialName("invited_email_phone") val invitedEmailPhone: String,
    @SerialName("role_to_assign") val roleToAssign: String,
    @SerialName("expires_at") val expiresAt: String,
) {
    companion object {
        /** Bumped when this DTO shape changes (registry `version:` driven). See EC30. */
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire response DTO for a successful COMP-DT-002 generate-invite call — the 6-char token, the
 * shareable deep-link URL, and the created datatable row's primary key. camelCase-ish per
 * `api.yaml#api.create_invite.response.fields` (`token`, `invite_link`, `row_id`).
 *
 * See API.md#dtos — GeneratedInvite.
 */
@Serializable
data class GeneratedInviteDto(
    @SerialName("token") val token: String,
    @SerialName("invite_link") val inviteLink: String,
    @SerialName("row_id") val rowId: Long,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire DTO for a single row of the pending-invites list — `GET
 * /companion/datatables/invitations/{groupId}` (COMP-DT-003). Raw datatable columns (snake_case
 * `@SerialName`s). [acceptedAt] is nullable — the companion API filters this list to
 * `accepted_at IS NULL` rows, so it is always `null` here, but the field is declared for total
 * mapping symmetry with [InvitationRowDto].
 *
 * See API.md#dtos — PendingInvite.
 */
@Serializable
data class PendingInviteDto(
    @SerialName("row_id") val rowId: Long,
    @SerialName("token") val token: String,
    @SerialName("invited_email_phone") val invitedEmailPhone: String,
    @SerialName("role_to_assign") val roleToAssign: String,
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("accepted_at") val acceptedAt: String? = null,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire response DTO for a successful COMP-DT-005 revoke-invite call — the standard Fineract
 * command-processing envelope (`resourceId` = the deleted invitations datatable row id).
 *
 * See API.md#dtos — RevokeInviteResponse.
 */
@Serializable
data class RevokeInviteResponseDto(
    @SerialName("resourceId") val resourceId: Long,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}
