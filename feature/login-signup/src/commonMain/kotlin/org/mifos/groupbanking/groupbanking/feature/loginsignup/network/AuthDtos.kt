/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.groupbanking.feature.loginsignup.network

import kotlinx.serialization.Serializable
import org.mifos.groupbanking.groupbanking.feature.loginsignup.model.AuthSession
import org.mifos.groupbanking.groupbanking.feature.loginsignup.model.GroupMembership
import org.mifos.groupbanking.groupbanking.feature.loginsignup.model.MemberGroupRole
import org.mifos.groupbanking.groupbanking.feature.loginsignup.model.UserProfile

/** Over-the-wire request for COMP-AUTH-001 (POST /companion/auth/self-register). */
@Serializable
data class SelfRegisterRequestDto(
    val name: String,
    val emailPhone: String,
    val password: String,
)

/** Over-the-wire request for COMP-AUTH-002 (POST /companion/auth/login). */
@Serializable
data class LoginRequestDto(
    val emailPhone: String,
    val password: String? = null,
    val pin: String? = null,
)

/** Group membership as returned by the companion backend. */
@Serializable
data class GroupMembershipDto(
    val groupId: String,
    val groupName: String,
    val role: String,
    val joinedAt: String,
) {
    fun toDomain(): GroupMembership = GroupMembership(
        groupId = groupId,
        groupName = groupName,
        role = MemberGroupRole.fromWire(role),
        joinedAt = joinedAt,
    )
}

/** Response for COMP-AUTH-001 / COMP-AUTH-002. */
@Serializable
data class AuthResponseDto(
    val userId: String,
    val sessionToken: String,
    val tokenExpiresAt: String,
    val groupMemberships: List<GroupMembershipDto> = emptyList(),
) {
    fun toDomain(): AuthSession = AuthSession(
        userId = userId,
        sessionToken = sessionToken,
        tokenExpiresAt = tokenExpiresAt,
        groupMemberships = groupMemberships.map { it.toDomain() },
    )
}

/** Response for COMP-AUTH-003 (GET /companion/auth/me). */
@Serializable
data class UserProfileDto(
    val userId: String,
    val displayName: String,
    val emailPhone: String,
    val groupMemberships: List<GroupMembershipDto> = emptyList(),
) {
    fun toDomain(): UserProfile = UserProfile(
        userId = userId,
        displayName = displayName,
        emailPhone = emailPhone,
        groupMemberships = groupMemberships.map { it.toDomain() },
    )
}
