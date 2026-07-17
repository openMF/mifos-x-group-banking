/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.groupbanking.feature.loginsignup.model

/**
 * Companion-auth domain models for the login-signup feature.
 *
 * These mirror the contract in `server-layer/API_CONTRACT.yaml` (COMP-AUTH-001/002/003)
 * and `idea-layer/exports/login-signup/API.md`. They are the app-facing shapes; the
 * over-the-wire DTOs live in the `network` package and map into these.
 */

/** Auth entry mode — drives the visible field set on the single auth screen. */
enum class AuthMode { LOGIN, SIGNUP }

/** Role a member holds within a group (COMP-AUTH-003 groupMemberships). */
enum class MemberGroupRole {
    ORGANIZER,
    MEMBER,
    TREASURER,
    CHAIRPERSON,
    SECRETARY,
    ;

    companion object {
        fun fromWire(raw: String): MemberGroupRole =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: MEMBER
    }
}

/** One group the authenticated user belongs to. */
data class GroupMembership(
    val groupId: String,
    val groupName: String,
    val role: MemberGroupRole,
    val joinedAt: String,
)

/**
 * Authenticated session returned by COMP-AUTH-001 / COMP-AUTH-002.
 * [groupMemberships] drives post-auth routing.
 */
data class AuthSession(
    val userId: String,
    val sessionToken: String,
    val tokenExpiresAt: String,
    val groupMemberships: List<GroupMembership>,
) {
    /** True when the user belongs to zero groups → the `zero_groups` screen state. */
    val hasNoGroups: Boolean get() = groupMemberships.isEmpty()

    /** True when the user holds an organizer-tier role in any group. */
    val hasOrganizerRole: Boolean get() = groupMemberships.any {
        it.role == MemberGroupRole.ORGANIZER ||
            it.role == MemberGroupRole.CHAIRPERSON ||
            it.role == MemberGroupRole.TREASURER ||
            it.role == MemberGroupRole.SECRETARY
    }
}

/** Profile returned by COMP-AUTH-003 (GET /companion/auth/me). */
data class UserProfile(
    val userId: String,
    val displayName: String,
    val emailPhone: String,
    val groupMemberships: List<GroupMembership>,
)

/** Request body for COMP-AUTH-001 (POST /companion/auth/self-register). */
data class SelfRegisterRequest(
    val name: String,
    val emailPhone: String,
    val password: String,
)

/** Request body for COMP-AUTH-002 (POST /companion/auth/login). */
data class LoginRequest(
    val emailPhone: String,
    val password: String? = null,
    val pin: String? = null,
)
