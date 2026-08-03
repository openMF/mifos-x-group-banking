/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.model

/**
 * Domain input for the organizer-side "generate invite" action (COMP-DT-002) — pure business
 * shape, no wire concerns. Assembled by `MemberInviteViewModel` from the validated form fields
 * (`invitedEmailPhone` + `roleToAssign`) plus the `groupId` nav-arg and the client-computed
 * `expiresAt` (`now() + 7 days`, ISO-8601). [roleToAssign] reuses the SHARED [MemberRole] enum
 * (`Member.kt`, already used by member-add/member-list/member-profile) — the idea-layer role
 * value-set (`member`/`treasurer`/`secretary`/`chairperson`) is an exact match, so no new enum is
 * introduced.
 *
 * Distinct from the recipient-side `Invitation` / `JoinGroupRequest` (`Invitation.kt`, the
 * join-with-code flow): those model the invitee CONSUMING an invite, this models the organizer
 * ISSUING one.
 *
 * See API.md#models — CreateInviteRequest.
 */
data class CreateInviteRequest(
    val groupId: Long,
    val invitedEmailPhone: String,
    val roleToAssign: MemberRole,
    val expiresAt: String,
)

/**
 * Domain result of a successful COMP-DT-002 generate-invite call — the newly issued single-use
 * token, the shareable deep-link URL, and the created datatable row's primary key ([rowId], used
 * as the delete target for a later revoke).
 *
 * See API.md#models — GeneratedInvite.
 */
data class GeneratedInvite(
    val token: String,
    val inviteLink: String,
    val rowId: Long,
)

/**
 * Domain model for a single pending (unaccepted) invite row (COMP-DT-003) — pure business shape,
 * no wire concerns. [acceptedAt] is always `null` for rows surfaced by the pending-invites list
 * (the companion API filters to `accepted_at IS NULL`); the field is retained so the mapper is a
 * total function over the datatable row. [roleToAssign] reuses the SHARED [MemberRole] enum.
 *
 * See API.md#models — PendingInvite.
 */
data class PendingInvite(
    val rowId: Long,
    val token: String,
    val invitedEmailPhone: String,
    val roleToAssign: MemberRole,
    val expiresAt: String,
    val acceptedAt: String? = null,
)
