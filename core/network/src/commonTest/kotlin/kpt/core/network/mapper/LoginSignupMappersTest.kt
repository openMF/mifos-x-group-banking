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
import kpt.core.model.GroupRole
import kpt.core.model.LoginCredentials
import kpt.core.model.SelfRegistration
import kpt.core.network.model.AuthResponseDto
import kpt.core.network.model.GroupMembershipDto
import kpt.core.network.model.GroupRoleDto
import kpt.core.network.model.LoginRequestDto
import kpt.core.network.model.SelfRegisterRequestDto
import kpt.core.network.model.UserProfileDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TDD RED-first coverage for the login-signup DTO<->domain mappers. Every field on every
 * DTO declared in `LoginSignupDto.kt` must be exercised here (RULE: all-fields-mapped).
 */
class LoginSignupMappersTest {

    private val membershipDto = GroupMembershipDto(
        groupId = "g-1",
        groupName = "Umoja Circle",
        role = GroupRoleDto.TREASURER,
        joinedAt = "2026-01-01T00:00:00Z",
    )

    // ---------- AuthResponseDto -> AuthSession ----------

    @Test
    fun authResponseDto_toDomainModel_mapsAllFields() {
        val dto = AuthResponseDto(
            userId = "u-1",
            sessionToken = "tok-abc",
            tokenExpiresAt = "2026-08-01T00:00:00Z",
            groupMemberships = listOf(membershipDto),
        )

        val session = dto.toDomainModel()

        assertEquals("u-1", session.userId)
        assertEquals("tok-abc", session.sessionToken)
        assertEquals(Instant.parse("2026-08-01T00:00:00Z"), session.tokenExpiresAt)
        assertEquals(1, session.groupMemberships.size)
        assertEquals("g-1", session.groupMemberships.first().groupId)
    }

    @Test
    fun authResponseDto_toDomainModel_hasGroupsFalseForNewSignup() {
        val dto = AuthResponseDto(
            userId = "u-2",
            sessionToken = "tok-xyz",
            tokenExpiresAt = "2026-08-01T00:00:00Z",
            groupMemberships = emptyList(),
        )
        assertFalse(dto.toDomainModel().hasGroups)
    }

    @Test
    fun authResponseDto_toDomainModel_hasGroupsTrueWhenMembershipsPresent() {
        val dto = AuthResponseDto("u-1", "tok", "2026-08-01T00:00:00Z", listOf(membershipDto))
        assertTrue(dto.toDomainModel().hasGroups)
    }

    // ---------- UserProfileDto -> UserProfile ----------

    @Test
    fun userProfileDto_toDomainModel_mapsAllFields() {
        val dto = UserProfileDto(
            userId = "u-1",
            name = "Amina Yusuf",
            emailPhone = "amina@example.com",
            groupMemberships = listOf(membershipDto),
        )

        val profile = dto.toDomainModel()

        assertEquals("u-1", profile.userId)
        assertEquals("Amina Yusuf", profile.name)
        assertEquals("amina@example.com", profile.emailPhone)
        assertEquals(1, profile.groupMemberships.size)
        assertTrue(profile.hasGroups)
    }

    @Test
    fun userProfileDto_toDomainModel_emptyMembershipsMeansNoGroups() {
        val dto = UserProfileDto("u-1", "Amina", "a@b.com", emptyList())
        assertFalse(dto.toDomainModel().hasGroups)
    }

    // ---------- GroupMembershipDto -> GroupMembership ----------

    @Test
    fun groupMembershipDto_toDomainModel_mapsAllFields() {
        val membership = membershipDto.toDomainModel()

        assertEquals("g-1", membership.groupId)
        assertEquals("Umoja Circle", membership.groupName)
        assertEquals(GroupRole.TREASURER, membership.role)
        assertEquals(Instant.parse("2026-01-01T00:00:00Z"), membership.joinedAt)
    }

    @Test
    fun groupMembershipDto_toDomainModel_unknownRoleMapsToDomainUnknown() {
        val dto = membershipDto.copy(role = GroupRoleDto.UNKNOWN)
        assertEquals(GroupRole.UNKNOWN, dto.toDomainModel().role)
    }

    // ---------- GroupRoleDto -> GroupRole (every entry) ----------

    @Test
    fun groupRoleDto_toDomainModel_mapsEveryEntry() {
        assertEquals(GroupRole.ORGANIZER, GroupRoleDto.ORGANIZER.toDomainModel())
        assertEquals(GroupRole.MEMBER, GroupRoleDto.MEMBER.toDomainModel())
        assertEquals(GroupRole.TREASURER, GroupRoleDto.TREASURER.toDomainModel())
        assertEquals(GroupRole.SECRETARY, GroupRoleDto.SECRETARY.toDomainModel())
        assertEquals(GroupRole.UNKNOWN, GroupRoleDto.UNKNOWN.toDomainModel())
    }

    // ---------- List<GroupMembershipDto>.toDomainModels() batch converter ----------

    @Test
    fun groupMembershipDtoList_toDomainModels_mapsEveryItemInOrder() {
        val second = membershipDto.copy(groupId = "g-2", role = GroupRoleDto.MEMBER)
        val result = listOf(membershipDto, second).toDomainModels()

        assertEquals(2, result.size)
        assertEquals("g-1", result[0].groupId)
        assertEquals("g-2", result[1].groupId)
        assertEquals(GroupRole.MEMBER, result[1].role)
    }

    @Test
    fun groupMembershipDtoList_toDomainModels_emptyListStaysEmpty() {
        assertEquals(emptyList(), emptyList<GroupMembershipDto>().toDomainModels())
    }

    // ---------- domain -> DTO (request side) ----------

    @Test
    fun loginCredentials_toDto_mapsAllFields() {
        val credentials = LoginCredentials(emailPhone = "amina@example.com", password = "hunter22")
        val dto = credentials.toDto()

        assertEquals(LoginRequestDto(emailPhone = "amina@example.com", password = "hunter22"), dto)
    }

    @Test
    fun selfRegistration_toDto_mapsAllFields() {
        val registration = SelfRegistration(name = "Amina Yusuf", emailPhone = "amina@example.com", password = "hunter22")
        val dto = registration.toDto()

        assertEquals(
            SelfRegisterRequestDto(name = "Amina Yusuf", emailPhone = "amina@example.com", password = "hunter22"),
            dto,
        )
    }
}
