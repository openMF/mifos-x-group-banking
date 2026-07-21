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
 * Wire request DTO for the companion self-registration bridge — `POST
 * /companion/auth/self-register` (COMP-AUTH-001). Creates a companion user account and a
 * linked Fineract client in one atomic request; no auth token required.
 *
 * See API.md#dtos — SelfRegisterRequest.
 */
@Serializable
data class SelfRegisterRequestDto(
    @SerialName("name") val name: String,
    @SerialName("emailPhone") val emailPhone: String,
    @SerialName("password") val password: String,
) {
    companion object {
        /** Bumped when this request shape changes (registry `version:` driven). See EC30. */
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire request DTO for the companion login bridge — `POST /companion/auth/login`
 * (COMP-AUTH-002). No auth token required.
 *
 * See API.md#dtos — LoginRequest.
 */
@Serializable
data class LoginRequestDto(
    @SerialName("emailPhone") val emailPhone: String,
    @SerialName("password") val password: String,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire response DTO shared by self-registration (COMP-AUTH-001) and login (COMP-AUTH-002) —
 * session token + expiry + the caller's current group memberships (empty for brand-new
 * self-registrations).
 *
 * See API.md#dtos — AuthResponse.
 */
@Serializable
data class AuthResponseDto(
    @SerialName("userId") val userId: String,
    @SerialName("sessionToken") val sessionToken: String,
    @SerialName("tokenExpiresAt") val tokenExpiresAt: String,
    @SerialName("groupMemberships") val groupMemberships: List<GroupMembershipDto> = emptyList(),
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire response DTO for `GET /companion/auth/me` (COMP-AUTH-003) — the authenticated user's
 * profile + group memberships, fetched after a biometric unlock to refresh the session.
 *
 * See API.md#dtos — UserProfile.
 */
@Serializable
data class UserProfileDto(
    @SerialName("userId") val userId: String,
    @SerialName("name") val name: String,
    @SerialName("emailPhone") val emailPhone: String,
    @SerialName("groupMemberships") val groupMemberships: List<GroupMembershipDto> = emptyList(),
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire DTO for a single group membership row — nested inside [AuthResponseDto] and
 * [UserProfileDto].
 *
 * See API.md#dtos — GroupMembership.
 */
@Serializable
data class GroupMembershipDto(
    @SerialName("groupId") val groupId: String,
    @SerialName("groupName") val groupName: String,
    @SerialName("role") val role: GroupRoleDto = GroupRoleDto.UNKNOWN,
    @SerialName("joinedAt") val joinedAt: String,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire enum for a companion member's role within a group. Carries an [UNKNOWN] fallback
 * (T7/EC30) so a server-added role value never crashes an old client — combined with
 * `ignoreUnknownKeys` + `coerceInputValues` on the shared `Json` instance in `NetworkModule`
 * (emitted by `kmp-client-gen`, see its NetworkModule.kt Json config), an unrecognized wire
 * value on the `role` property is coerced to this default rather than throwing.
 */
@Serializable
enum class GroupRoleDto {
    @SerialName("ORGANIZER") ORGANIZER,
    @SerialName("MEMBER") MEMBER,
    @SerialName("TREASURER") TREASURER,
    @SerialName("SECRETARY") SECRETARY,
    @SerialName("UNKNOWN") UNKNOWN,
}
