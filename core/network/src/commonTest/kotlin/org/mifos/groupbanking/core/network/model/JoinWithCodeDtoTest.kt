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

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * TDD RED-first coverage for the join-with-code wire contracts (COMP-DT-004 invitations
 * datatable + COMP-GRP-003 associate-clients). See API.md#dtos.
 */
class JoinWithCodeDtoTest {

    // Server-parity Json config: mirrors NetworkModule's client config (ignoreUnknownKeys +
    // coerceInputValues) so these tests prove EC30 cross-version tolerance, not just happy-path
    // round-tripping.
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    // ---------- InvitationRowDto (GET /companion/datatables/invitations/{entityId}) ----------

    private val unusedInviteDto = InvitationRowDto(
        token = "AB12CD",
        groupId = 1001L,
        inviterClientId = 55L,
        invitedEmailPhone = "invitee@example.com",
        roleToAssign = GroupRoleDto.MEMBER,
        expiresAt = "2026-08-01T00:00:00Z",
        acceptedAt = null,
    )

    @Test
    fun invitationRowDto_constructsWithAllFields() {
        assertEquals("AB12CD", unusedInviteDto.token)
        assertEquals(1001L, unusedInviteDto.groupId)
        assertEquals(55L, unusedInviteDto.inviterClientId)
        assertEquals("invitee@example.com", unusedInviteDto.invitedEmailPhone)
        assertEquals(GroupRoleDto.MEMBER, unusedInviteDto.roleToAssign)
        assertEquals("2026-08-01T00:00:00Z", unusedInviteDto.expiresAt)
        assertNull(unusedInviteDto.acceptedAt)
    }

    @Test
    fun invitationRowDto_acceptedAtDefaultsToNull_whenOmittedFromWire() {
        val payload = """
            {"token":"AB12CD","group_id":1001,"inviter_client_id":55,"invited_email_phone":"invitee@example.com","role_to_assign":"MEMBER","expires_at":"2026-08-01T00:00:00Z"}
        """.trimIndent()
        val decoded = json.decodeFromString(InvitationRowDto.serializer(), payload)
        assertNull(decoded.acceptedAt)
    }

    @Test
    fun invitationRowDto_acceptedAtCarriesTimestamp_whenCodeAlreadyUsed() {
        val used = unusedInviteDto.copy(acceptedAt = "2026-07-20T09:15:00Z")
        assertEquals("2026-07-20T09:15:00Z", used.acceptedAt)
    }

    @Test
    fun invitationRowDto_serializationRoundTrips_wireFieldNamesAreSnakeCase() {
        val encoded = json.encodeToString(InvitationRowDto.serializer(), unusedInviteDto)
        assertTrue(encoded.contains("\"token\""))
        assertTrue(encoded.contains("\"group_id\""))
        assertTrue(encoded.contains("\"inviter_client_id\""))
        assertTrue(encoded.contains("\"invited_email_phone\""))
        assertTrue(encoded.contains("\"role_to_assign\""))
        assertTrue(encoded.contains("\"expires_at\""))

        val decoded = json.decodeFromString(InvitationRowDto.serializer(), encoded)
        assertEquals(unusedInviteDto, decoded)
    }

    @Test
    fun invitationRowDto_equality() {
        assertEquals(unusedInviteDto.copy(), unusedInviteDto.copy())
    }

    @Test
    fun invitationRowDto_carriesSchemaVersion() {
        assertEquals(1, InvitationRowDto.SCHEMA_VERSION)
    }

    @Test
    fun invitationRowDto_roleToAssignUnknownServerValueCoercesToUnknownFallback_notCrash() {
        val payload = """
            {"token":"AB12CD","group_id":1001,"inviter_client_id":55,"invited_email_phone":"invitee@example.com","role_to_assign":"AUDITOR","expires_at":"2026-08-01T00:00:00Z","accepted_at":null}
        """.trimIndent()
        val decoded = json.decodeFromString(InvitationRowDto.serializer(), payload)
        assertEquals(GroupRoleDto.UNKNOWN, decoded.roleToAssign)
    }

    // ---------- GroupPreviewDto (GET /companion/groups/{groupId}) ----------

    private val previewDto = GroupPreviewDto(
        groupId = 1001L,
        groupName = "Mwangaza Women's Group",
        groupType = GroupTypeSlugDto.VSLA,
        organizerName = "Amina Yusuf",
        memberCount = 18,
        officeId = 1L,
        roleToAssign = GroupRoleDto.MEMBER,
    )

    @Test
    fun groupPreviewDto_constructsWithAllFields() {
        assertEquals(1001L, previewDto.groupId)
        assertEquals("Mwangaza Women's Group", previewDto.groupName)
        assertEquals(GroupTypeSlugDto.VSLA, previewDto.groupType)
        assertEquals("Amina Yusuf", previewDto.organizerName)
        assertEquals(18, previewDto.memberCount)
        assertEquals(1L, previewDto.officeId)
        assertEquals(GroupRoleDto.MEMBER, previewDto.roleToAssign)
    }

    @Test
    fun groupPreviewDto_serializationRoundTrips_wireFieldNamesAreCamelCase() {
        val encoded = json.encodeToString(GroupPreviewDto.serializer(), previewDto)
        assertTrue(encoded.contains("\"groupId\""))
        assertTrue(encoded.contains("\"groupName\""))
        assertTrue(encoded.contains("\"groupType\""))
        assertTrue(encoded.contains("\"organizerName\""))
        assertTrue(encoded.contains("\"memberCount\""))
        assertTrue(encoded.contains("\"officeId\""))
        assertTrue(encoded.contains("\"roleToAssign\""))

        val decoded = json.decodeFromString(GroupPreviewDto.serializer(), encoded)
        assertEquals(previewDto, decoded)
    }

    @Test
    fun groupPreviewDto_equality() {
        assertEquals(previewDto.copy(), previewDto.copy())
    }

    @Test
    fun groupPreviewDto_carriesSchemaVersion() {
        assertEquals(1, GroupPreviewDto.SCHEMA_VERSION)
    }

    @Test
    fun groupPreviewDto_groupTypeUnknownServerValueCoercesToUnknownFallback_notCrash() {
        val payload = """
            {"groupId":1001,"groupName":"New","groupType":"COOP_UNION","organizerName":"X","memberCount":5,"officeId":1,"roleToAssign":"MEMBER"}
        """.trimIndent()
        val decoded = json.decodeFromString(GroupPreviewDto.serializer(), payload)
        assertEquals(GroupTypeSlugDto.UNKNOWN, decoded.groupType)
    }

    // ---------- AssociateClientsRequestDto / ResponseDto (POST .../associate-clients) ----------

    private val associateRequestDto = AssociateClientsRequestDto(
        clientIds = listOf(789L),
        roleToAssign = GroupRoleDto.MEMBER,
    )

    private val associateResponseDto = AssociateClientsResponseDto(
        resourceId = 1001L,
        groupId = 1001L,
        clientIds = listOf(789L),
    )

    @Test
    fun associateClientsRequestDto_constructsWithAllFields() {
        assertEquals(listOf(789L), associateRequestDto.clientIds)
        assertEquals(GroupRoleDto.MEMBER, associateRequestDto.roleToAssign)
    }

    @Test
    fun associateClientsRequestDto_serializationRoundTrips_wireFieldNamesAreCamelCase() {
        val encoded = json.encodeToString(AssociateClientsRequestDto.serializer(), associateRequestDto)
        assertTrue(encoded.contains("\"clientIds\""))
        assertTrue(encoded.contains("\"roleToAssign\""))
        val decoded = json.decodeFromString(AssociateClientsRequestDto.serializer(), encoded)
        assertEquals(associateRequestDto, decoded)
    }

    @Test
    fun associateClientsRequestDto_equality() {
        assertEquals(associateRequestDto.copy(), associateRequestDto.copy())
    }

    @Test
    fun associateClientsResponseDto_constructsWithAllFields() {
        assertEquals(1001L, associateResponseDto.resourceId)
        assertEquals(1001L, associateResponseDto.groupId)
        assertEquals(listOf(789L), associateResponseDto.clientIds)
    }

    @Test
    fun associateClientsResponseDto_serializationRoundTrips() {
        val encoded = json.encodeToString(AssociateClientsResponseDto.serializer(), associateResponseDto)
        assertTrue(encoded.contains("\"resourceId\""))
        assertTrue(encoded.contains("\"groupId\""))
        assertTrue(encoded.contains("\"clientIds\""))
        val decoded = json.decodeFromString(AssociateClientsResponseDto.serializer(), encoded)
        assertEquals(associateResponseDto, decoded)
    }

    @Test
    fun associateClientsResponseDto_equality() {
        assertEquals(associateResponseDto.copy(), associateResponseDto.copy())
    }

    @Test
    fun associateClientsDtos_carrySchemaVersion() {
        assertEquals(1, AssociateClientsRequestDto.SCHEMA_VERSION)
        assertEquals(1, AssociateClientsResponseDto.SCHEMA_VERSION)
    }

    // ---------- MarkAcceptedRequestDto / ResponseDto (PUT .../invitations/{entityId}/{rowId}) ----------

    private val markAcceptedRequestDto = MarkAcceptedRequestDto(acceptedAt = "2026-07-20T09:15:00Z")

    private val markAcceptedResponseDto = MarkAcceptedResponseDto(
        resourceId = 1001L,
        changes = MarkAcceptedChangesDto(acceptedAt = "2026-07-20T09:15:00Z"),
    )

    @Test
    fun markAcceptedRequestDto_constructsWithAcceptedAt() {
        assertEquals("2026-07-20T09:15:00Z", markAcceptedRequestDto.acceptedAt)
    }

    @Test
    fun markAcceptedRequestDto_serializationRoundTrips_wireFieldNameIsSnakeCase() {
        val encoded = json.encodeToString(MarkAcceptedRequestDto.serializer(), markAcceptedRequestDto)
        assertTrue(encoded.contains("\"accepted_at\""))
        val decoded = json.decodeFromString(MarkAcceptedRequestDto.serializer(), encoded)
        assertEquals(markAcceptedRequestDto, decoded)
    }

    @Test
    fun markAcceptedRequestDto_equality() {
        assertEquals(markAcceptedRequestDto.copy(), markAcceptedRequestDto.copy())
    }

    @Test
    fun markAcceptedResponseDto_constructsWithResourceIdAndNestedChanges() {
        assertEquals(1001L, markAcceptedResponseDto.resourceId)
        assertEquals("2026-07-20T09:15:00Z", markAcceptedResponseDto.changes.acceptedAt)
    }

    @Test
    fun markAcceptedResponseDto_serializationRoundTrips_nestedChangesObject() {
        val encoded = json.encodeToString(MarkAcceptedResponseDto.serializer(), markAcceptedResponseDto)
        assertTrue(encoded.contains("\"resourceId\""))
        assertTrue(encoded.contains("\"changes\""))
        assertTrue(encoded.contains("\"accepted_at\""))
        val decoded = json.decodeFromString(MarkAcceptedResponseDto.serializer(), encoded)
        assertEquals(markAcceptedResponseDto, decoded)
    }

    @Test
    fun markAcceptedResponseDto_equality() {
        assertEquals(markAcceptedResponseDto.copy(), markAcceptedResponseDto.copy())
    }

    @Test
    fun markAcceptedDtos_carrySchemaVersion() {
        assertEquals(1, MarkAcceptedRequestDto.SCHEMA_VERSION)
        assertEquals(1, MarkAcceptedResponseDto.SCHEMA_VERSION)
    }

    // ---------- Cross-version safety fixture (T7/EC30) ----------

    @Test
    fun invitationRowDto_toleratesServerAddedFieldAndUnknownEnumValue_oldClientNeverCrashes() {
        // Simulates a staggered rollout: the server has shipped a NEW top-level column
        // (`campaign_code`) plus a NEW role_to_assign value this (old) client schema does not
        // know about. Decoding MUST succeed, never throw.
        val serverPayload = """
            {
              "token":"AB12CD",
              "group_id":1001,
              "inviter_client_id":55,
              "invited_email_phone":"invitee@example.com",
              "role_to_assign":"AUDITOR",
              "expires_at":"2026-08-01T00:00:00Z",
              "accepted_at":null,
              "campaign_code":"KE-2026-not-in-old-schema"
            }
        """.trimIndent()
        val decoded = json.decodeFromString(InvitationRowDto.serializer(), serverPayload)
        assertEquals("AB12CD", decoded.token)
        assertEquals(GroupRoleDto.UNKNOWN, decoded.roleToAssign)
        assertNull(decoded.acceptedAt)
    }
}
