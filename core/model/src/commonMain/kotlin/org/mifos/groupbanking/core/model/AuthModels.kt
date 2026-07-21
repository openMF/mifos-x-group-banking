/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.model

import kotlinx.datetime.Instant

/**
 * Domain input for the companion login bridge (COMP-AUTH-002) — pure business shape, no wire
 * concerns. See API.md#models — LoginCredentials.
 */
data class LoginCredentials(
    val emailPhone: String,
    val password: String,
)

/**
 * Domain input for the companion self-registration bridge (COMP-AUTH-001).
 * See API.md#models — SelfRegistration.
 */
data class SelfRegistration(
    val name: String,
    val emailPhone: String,
    val password: String,
)

/**
 * Domain result of a successful companion authentication event (login or self-registration) —
 * the auth-result value object consumed by ViewModels to branch navigation (personal-dashboard
 * vs. zero-groups onboarding, per `flow.yaml#on_login`/`on_signup`).
 *
 * See API.md#models — AuthSession.
 */
data class AuthSession(
    val userId: String,
    val sessionToken: String,
    val tokenExpiresAt: Instant,
    val groupMemberships: List<GroupMembership>,
) {
    /** True when the authenticated user already belongs to at least one group. */
    val hasGroups: Boolean get() = groupMemberships.isNotEmpty()
}

/**
 * Domain model for the authenticated user's profile, returned by `companion_me`
 * (COMP-AUTH-003) after a biometric unlock refresh.
 *
 * See API.md#models — UserProfile.
 */
data class UserProfile(
    val userId: String,
    val name: String,
    val emailPhone: String,
    val groupMemberships: List<GroupMembership>,
) {
    /** True when the user already belongs to at least one group. */
    val hasGroups: Boolean get() = groupMemberships.isNotEmpty()
}

/**
 * Domain model for a single group the user belongs to, plus their role within it.
 * See API.md#models — GroupMembership.
 */
data class GroupMembership(
    val groupId: String,
    val groupName: String,
    val role: GroupRole,
    val joinedAt: Instant,
)

/**
 * Domain enum mirroring the wire `GroupRoleDto` one-to-one (pure Kotlin — no `@Serializable`).
 * [UNKNOWN] absorbs any wire role value this client build does not yet recognize.
 * See API.md#models — GroupRole.
 */
enum class GroupRole {
    ORGANIZER,
    MEMBER,
    TREASURER,
    SECRETARY,
    UNKNOWN,
}
