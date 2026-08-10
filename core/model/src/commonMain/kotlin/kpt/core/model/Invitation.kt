/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.model

import kotlinx.datetime.Instant
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Domain model for a single invitations-datatable row (COMP-DT-004) — pure business shape, no
 * wire concerns. [roleToAssign] reuses the SHARED [GroupRole] domain enum (see
 * `AuthModels.kt`) rather than introducing a second role enum — every role an invite can assign
 * (`ORGANIZER`/`MEMBER`/`TREASURER`/`SECRETARY`) is already covered by [GroupRole]. [acceptedAt]
 * is `null` while the code is unused; [isAlreadyUsed] and [isExpired] are derived helpers so
 * ViewModels never re-implement the same null/date-comparison checks inline.
 *
 * See API.md#models — Invitation.
 */
data class Invitation(
    val token: String,
    val groupId: Long,
    val inviterClientId: Long,
    val invitedEmailPhone: String,
    val roleToAssign: GroupRole,
    val expiresAt: Instant,
    val acceptedAt: Instant?,
) {
    /** True once `accepted_at` has been set server-side — the code was already consumed. */
    val isAlreadyUsed: Boolean get() = acceptedAt != null

    /**
     * True when [expiresAt] is at or before [now] (defaults to the current instant). Exposed as
     * a function rather than a stored/derived property because "now" is inherently a moving
     * target — callers may also want to check expiry against a fixed instant in tests.
     */
    @OptIn(ExperimentalTime::class)
    fun isExpired(now: Instant = Clock.System.now()): Boolean = now >= expiresAt
}

/**
 * Domain model for the group preview card shown to the invitee before they confirm joining
 * (companion bridge `GET /companion/groups/{groupId}`) — pure business shape, no wire concerns.
 * [groupType] reuses the SHARED [GroupTypeSlug] domain enum (see `GroupTypeConfig.kt`);
 * [roleToAssign] reuses the SHARED [GroupRole] domain enum (see `AuthModels.kt`) — neither is
 * duplicated here.
 *
 * See API.md#models — GroupPreview.
 */
data class GroupPreview(
    val groupId: Long,
    val groupName: String,
    val groupType: GroupTypeSlug,
    val organizerName: String,
    val memberCount: Int,
    val officeId: Long,
    val roleToAssign: GroupRole,
)

/**
 * Domain input to auto-associate the authenticated invitee to a group as a member
 * (COMP-GRP-003). See API.md#models — JoinGroupRequest.
 */
data class JoinGroupRequest(
    val clientIds: List<Long>,
    val roleToAssign: GroupRole,
)

/**
 * Domain result of a successful COMP-GRP-003 association call.
 * See API.md#models — JoinGroupResult.
 */
data class JoinGroupResult(
    val resourceId: Long,
    val groupId: Long,
    val clientIds: List<Long>,
)

/**
 * Domain input to mark an invitation as accepted/consumed (COMP-DT-004 `PUT`).
 * See API.md#models — InvitationAcceptance.
 */
data class InvitationAcceptance(
    val acceptedAt: Instant,
)

/**
 * Domain result of a successful invitation-acceptance update — flattens the wire's nested
 * `changes.accepted_at` into a single top-level field.
 * See API.md#models — InvitationAcceptanceResult.
 */
data class InvitationAcceptanceResult(
    val resourceId: Long,
    val acceptedAt: Instant,
)
