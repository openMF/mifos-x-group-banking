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

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD RED-first coverage for the login-signup wire DTOs (COMP-AUTH-001/002/003).
 * See API.md#dtos.
 */
class LoginSignupDtoTest {

    // Server-parity Json config: mirrors NetworkModule's client config (ignoreUnknownKeys +
    // coerceInputValues) so these tests prove EC30 cross-version tolerance, not just happy-path
    // round-tripping.
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    // ---------- SelfRegisterRequestDto ----------

    @Test
    fun selfRegisterRequestDto_constructsWithAllFields() {
        val dto = SelfRegisterRequestDto(
            name = "Amina Yusuf",
            emailPhone = "amina@example.com",
            password = "hunter22",
        )
        assertEquals("Amina Yusuf", dto.name)
        assertEquals("amina@example.com", dto.emailPhone)
        assertEquals("hunter22", dto.password)
    }

    @Test
    fun selfRegisterRequestDto_serializationRoundTrips() {
        val dto = SelfRegisterRequestDto(name = "Amina Yusuf", emailPhone = "+254712345678", password = "hunter22")
        val encoded = json.encodeToString(SelfRegisterRequestDto.serializer(), dto)
        assertTrue(encoded.contains("\"name\""))
        assertTrue(encoded.contains("\"emailPhone\""))
        assertTrue(encoded.contains("\"password\""))
        val decoded = json.decodeFromString(SelfRegisterRequestDto.serializer(), encoded)
        assertEquals(dto, decoded)
    }

    @Test
    fun selfRegisterRequestDto_equality() {
        val a = SelfRegisterRequestDto(name = "Amina", emailPhone = "a@b.com", password = "pw123456")
        val b = SelfRegisterRequestDto(name = "Amina", emailPhone = "a@b.com", password = "pw123456")
        assertEquals(a, b)
    }

    // ---------- LoginRequestDto ----------

    @Test
    fun loginRequestDto_constructsWithAllFields() {
        val dto = LoginRequestDto(emailPhone = "amina@example.com", password = "hunter22")
        assertEquals("amina@example.com", dto.emailPhone)
        assertEquals("hunter22", dto.password)
    }

    @Test
    fun loginRequestDto_serializationRoundTrips() {
        val dto = LoginRequestDto(emailPhone = "amina@example.com", password = "hunter22")
        val encoded = json.encodeToString(LoginRequestDto.serializer(), dto)
        val decoded = json.decodeFromString(LoginRequestDto.serializer(), encoded)
        assertEquals(dto, decoded)
    }

    @Test
    fun loginRequestDto_equality() {
        assertEquals(
            LoginRequestDto(emailPhone = "a@b.com", password = "pw123456"),
            LoginRequestDto(emailPhone = "a@b.com", password = "pw123456"),
        )
    }

    // ---------- AuthResponseDto ----------

    @Test
    fun authResponseDto_constructsWithAllFields() {
        val dto = AuthResponseDto(
            userId = "u-1",
            sessionToken = "tok-abc",
            tokenExpiresAt = "2026-08-01T00:00:00Z",
            groupMemberships = listOf(
                GroupMembershipDto(
                    groupId = "g-1",
                    groupName = "Umoja Circle",
                    role = GroupRoleDto.TREASURER,
                    joinedAt = "2026-01-01T00:00:00Z",
                ),
            ),
        )
        assertEquals("u-1", dto.userId)
        assertEquals("tok-abc", dto.sessionToken)
        assertEquals("2026-08-01T00:00:00Z", dto.tokenExpiresAt)
        assertEquals(1, dto.groupMemberships.size)
    }

    @Test
    fun authResponseDto_groupMembershipsDefaultsToEmptyList_newUserSignup() {
        // "empty for new users" per api.yaml companion_self_register.response
        val jsonPayload = """
            {"userId":"u-2","sessionToken":"tok-xyz","tokenExpiresAt":"2026-08-01T00:00:00Z"}
        """.trimIndent()
        val dto = json.decodeFromString(AuthResponseDto.serializer(), jsonPayload)
        assertEquals(emptyList(), dto.groupMemberships)
    }

    @Test
    fun authResponseDto_serializationRoundTrips() {
        val dto = AuthResponseDto(
            userId = "u-1",
            sessionToken = "tok-abc",
            tokenExpiresAt = "2026-08-01T00:00:00Z",
            groupMemberships = emptyList(),
        )
        val encoded = json.encodeToString(AuthResponseDto.serializer(), dto)
        val decoded = json.decodeFromString(AuthResponseDto.serializer(), encoded)
        assertEquals(dto, decoded)
    }

    @Test
    fun authResponseDto_equality() {
        val a = AuthResponseDto("u-1", "tok", "2026-08-01T00:00:00Z", emptyList())
        val b = AuthResponseDto("u-1", "tok", "2026-08-01T00:00:00Z", emptyList())
        assertEquals(a, b)
    }

    @Test
    fun authResponseDto_carriesSchemaVersion() {
        assertEquals(1, AuthResponseDto.SCHEMA_VERSION)
    }

    // ---------- GroupMembershipDto ----------

    @Test
    fun groupMembershipDto_constructsWithAllFields() {
        val dto = GroupMembershipDto(
            groupId = "g-1",
            groupName = "Umoja Circle",
            role = GroupRoleDto.ORGANIZER,
            joinedAt = "2026-01-01T00:00:00Z",
        )
        assertEquals("g-1", dto.groupId)
        assertEquals("Umoja Circle", dto.groupName)
        assertEquals(GroupRoleDto.ORGANIZER, dto.role)
        assertEquals("2026-01-01T00:00:00Z", dto.joinedAt)
    }

    @Test
    fun groupMembershipDto_serialNamesMatchWireFieldNames() {
        val jsonPayload = """
            {"groupId":"g-1","groupName":"Umoja Circle","role":"SECRETARY","joinedAt":"2026-01-01T00:00:00Z"}
        """.trimIndent()
        val dto = json.decodeFromString(GroupMembershipDto.serializer(), jsonPayload)
        assertEquals("g-1", dto.groupId)
        assertEquals(GroupRoleDto.SECRETARY, dto.role)
    }

    @Test
    fun groupMembershipDto_equality() {
        val a = GroupMembershipDto("g-1", "Umoja Circle", GroupRoleDto.MEMBER, "2026-01-01T00:00:00Z")
        val b = GroupMembershipDto("g-1", "Umoja Circle", GroupRoleDto.MEMBER, "2026-01-01T00:00:00Z")
        assertEquals(a, b)
    }

    // ---------- UserProfileDto ----------

    @Test
    fun userProfileDto_constructsWithAllFields() {
        val dto = UserProfileDto(
            userId = "u-1",
            name = "Amina Yusuf",
            emailPhone = "amina@example.com",
            groupMemberships = emptyList(),
        )
        assertEquals("u-1", dto.userId)
        assertEquals("Amina Yusuf", dto.name)
        assertEquals("amina@example.com", dto.emailPhone)
        assertEquals(emptyList(), dto.groupMemberships)
    }

    @Test
    fun userProfileDto_serializationRoundTrips() {
        val dto = UserProfileDto(
            userId = "u-1",
            name = "Amina Yusuf",
            emailPhone = "amina@example.com",
            groupMemberships = listOf(
                GroupMembershipDto("g-1", "Umoja Circle", GroupRoleDto.ORGANIZER, "2026-01-01T00:00:00Z"),
            ),
        )
        val encoded = json.encodeToString(UserProfileDto.serializer(), dto)
        val decoded = json.decodeFromString(UserProfileDto.serializer(), encoded)
        assertEquals(dto, decoded)
    }

    @Test
    fun userProfileDto_equality() {
        val a = UserProfileDto("u-1", "Amina", "a@b.com", emptyList())
        val b = UserProfileDto("u-1", "Amina", "a@b.com", emptyList())
        assertEquals(a, b)
    }

    // ---------- GroupRoleDto (T7/EC30 unknown-value fallback) ----------

    @Test
    fun groupRoleDto_hasExactlyFiveEntriesIncludingUnknownFallback() {
        assertEquals(5, GroupRoleDto.entries.size)
        assertTrue(GroupRoleDto.entries.contains(GroupRoleDto.UNKNOWN))
    }

    @Test
    fun groupRoleDto_decodesEachKnownWireValue() {
        assertEquals(GroupRoleDto.ORGANIZER, json.decodeFromString(GroupRoleDto.serializer(), "\"ORGANIZER\""))
        assertEquals(GroupRoleDto.MEMBER, json.decodeFromString(GroupRoleDto.serializer(), "\"MEMBER\""))
        assertEquals(GroupRoleDto.TREASURER, json.decodeFromString(GroupRoleDto.serializer(), "\"TREASURER\""))
        assertEquals(GroupRoleDto.SECRETARY, json.decodeFromString(GroupRoleDto.serializer(), "\"SECRETARY\""))
    }

    @Test
    fun groupRoleDto_unknownServerValueCoercesToUnknownFallback_notCrash() {
        // A future server release adds a 5th role (e.g. "COORDINATOR") — an OLD client on this
        // schema must decode the enclosing payload without throwing, per T7/EC30.
        val jsonPayload = """
            {"groupId":"g-9","groupName":"New Circle","role":"COORDINATOR","joinedAt":"2026-01-01T00:00:00Z"}
        """.trimIndent()
        val dto = json.decodeFromString(GroupMembershipDto.serializer(), jsonPayload)
        assertEquals(GroupRoleDto.UNKNOWN, dto.role)
    }

    // ---------- Cross-version safety fixture (T7/EC30) ----------

    @Test
    fun authResponseDto_toleratesServerAddedFieldAndUnknownNestedEnum_oldClientNeverCrashes() {
        // Simulates a staggered rollout: the server has shipped a NEW top-level field
        // (`refreshToken`) and a NEW group role (`COORDINATOR`) that this (old) client schema
        // does not know about. Decoding MUST succeed, never throw.
        val serverPayload = """
            {
              "userId":"u-1",
              "sessionToken":"tok-abc",
              "tokenExpiresAt":"2026-08-01T00:00:00Z",
              "refreshToken":"refresh-not-in-old-schema",
              "groupMemberships":[
                {"groupId":"g-1","groupName":"Umoja Circle","role":"COORDINATOR","joinedAt":"2026-01-01T00:00:00Z"}
              ]
            }
        """.trimIndent()
        val dto = json.decodeFromString(AuthResponseDto.serializer(), serverPayload)
        assertEquals("u-1", dto.userId)
        assertEquals(GroupRoleDto.UNKNOWN, dto.groupMemberships.single().role)
    }
}
