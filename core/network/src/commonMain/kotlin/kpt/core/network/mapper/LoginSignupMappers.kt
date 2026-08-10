/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.network.mapper

import kotlinx.datetime.Instant
import kpt.core.model.AuthSession
import kpt.core.model.GroupMembership
import kpt.core.model.GroupRole
import kpt.core.model.LoginCredentials
import kpt.core.model.SelfRegistration
import kpt.core.model.UserProfile
import kpt.core.network.model.AuthResponseDto
import kpt.core.network.model.GroupMembershipDto
import kpt.core.network.model.GroupRoleDto
import kpt.core.network.model.LoginRequestDto
import kpt.core.network.model.SelfRegisterRequestDto
import kpt.core.network.model.UserProfileDto
import kotlin.jvm.JvmName

/**
 * DTO -> domain mappers for the login-signup wire contracts (COMP-AUTH-001/002/003). Every
 * field on every DTO declared in `LoginSignupDto.kt` is mapped — no field left unmapped.
 */

// ---------- response DTOs -> domain ----------

fun AuthResponseDto.toDomainModel(): AuthSession = AuthSession(
    userId = userId,
    sessionToken = sessionToken,
    tokenExpiresAt = Instant.parse(tokenExpiresAt),
    groupMemberships = groupMemberships.toDomainModels(),
)

fun UserProfileDto.toDomainModel(): UserProfile = UserProfile(
    userId = userId,
    name = name,
    emailPhone = emailPhone,
    groupMemberships = groupMemberships.toDomainModels(),
)

fun GroupMembershipDto.toDomainModel(): GroupMembership = GroupMembership(
    groupId = groupId,
    groupName = groupName,
    role = role.toDomainModel(),
    joinedAt = Instant.parse(joinedAt),
)

fun GroupRoleDto.toDomainModel(): GroupRole = when (this) {
    GroupRoleDto.ORGANIZER -> GroupRole.ORGANIZER
    GroupRoleDto.MEMBER -> GroupRole.MEMBER
    GroupRoleDto.TREASURER -> GroupRole.TREASURER
    GroupRoleDto.SECRETARY -> GroupRole.SECRETARY
    GroupRoleDto.UNKNOWN -> GroupRole.UNKNOWN
}

/** Batch converter — maps every membership row in declaration order. */
@JvmName("groupMembershipDtoListToDomainModels")
fun List<GroupMembershipDto>.toDomainModels(): List<GroupMembership> = map { it.toDomainModel() }

// ---------- domain -> request DTOs ----------

fun LoginCredentials.toDto(): LoginRequestDto = LoginRequestDto(
    emailPhone = emailPhone,
    password = password,
)

fun SelfRegistration.toDto(): SelfRegisterRequestDto = SelfRegisterRequestDto(
    name = name,
    emailPhone = emailPhone,
    password = password,
)
