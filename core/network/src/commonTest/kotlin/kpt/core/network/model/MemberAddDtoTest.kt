/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.network.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD RED-first coverage for the member-add create-chain wire DTOs
 * (`idea-layer/screens/member-add/api.yaml` — create_client -> assign_member_role -> optional
 * upload_photo). See API.md#dtos.
 *
 * `assign_member_role`'s request/response shapes are NOT re-declared here — they reuse the
 * SHARED `UpdateMemberRoleRequestDto`/`UpdateMemberRoleResponseDto` already covered by
 * `MemberProfileDtoTest.kt` (both features write the literal same
 * `/datatables/dt_member_role/{clientId}` endpoint with an identical body/response shape).
 */
class MemberAddDtoTest {

    // Server-parity Json config: mirrors NetworkModule's client config (ignoreUnknownKeys +
    // coerceInputValues) so these tests prove EC30 cross-version tolerance, not just happy-path
    // round-tripping.
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val createMemberRequestDto = CreateMemberRequestDto(
        firstname = "Grace",
        lastname = "Achieng",
        mobileNo = "+254723456789",
        active = true,
        activationDate = "21 July 2026",
        officeId = 1L,
        groupId = 9001L,
        locale = "en",
        dateFormat = "dd MMMM yyyy",
    )

    private val createMemberResponseDto = CreateMemberResponseDto(
        resourceId = 5010L,
        clientId = 5010L,
    )

    private val uploadMemberPhotoResponseDto = UploadMemberPhotoResponseDto(
        resourceId = 7001L,
    )

    // ---------- CreateMemberRequestDto (POST /clients) ----------

    @Test
    fun createMemberRequestDto_constructsWithAllFields() {
        assertEquals("Grace", createMemberRequestDto.firstname)
        assertEquals("Achieng", createMemberRequestDto.lastname)
        assertEquals("+254723456789", createMemberRequestDto.mobileNo)
        assertTrue(createMemberRequestDto.active)
        assertEquals("21 July 2026", createMemberRequestDto.activationDate)
        assertEquals(1L, createMemberRequestDto.officeId)
        assertEquals(9001L, createMemberRequestDto.groupId)
        assertEquals("en", createMemberRequestDto.locale)
        assertEquals("dd MMMM yyyy", createMemberRequestDto.dateFormat)
    }

    @Test
    fun createMemberRequestDto_equality() {
        assertEquals(createMemberRequestDto.copy(), createMemberRequestDto.copy())
    }

    @Test
    fun createMemberRequestDto_carriesSchemaVersion() {
        assertEquals(1, CreateMemberRequestDto.SCHEMA_VERSION)
    }

    @Test
    fun createMemberRequestDto_serializationRoundTrips_fieldNamesMatchFineractClientFields() {
        // @SerialName values are Fineract's OWN literal `/clients` resource field names
        // (mobileNo/dateFormat are camelCase per Fineract's own convention, not a project
        // snake_case datatable column) — Hard Rule 5 "match exactly" applied to the operation's
        // literal wire names.
        val encoded = json.encodeToString(CreateMemberRequestDto.serializer(), createMemberRequestDto)
        assertTrue(encoded.contains("\"firstname\""))
        assertTrue(encoded.contains("\"lastname\""))
        assertTrue(encoded.contains("\"mobileNo\""))
        assertTrue(encoded.contains("\"active\""))
        assertTrue(encoded.contains("\"activationDate\""))
        assertTrue(encoded.contains("\"officeId\""))
        assertTrue(encoded.contains("\"groupId\""))
        assertTrue(encoded.contains("\"locale\""))
        assertTrue(encoded.contains("\"dateFormat\""))

        val decoded = json.decodeFromString(CreateMemberRequestDto.serializer(), encoded)
        assertEquals(createMemberRequestDto, decoded)
    }

    @Test
    fun createMemberRequestDto_toleratesServerAddedField_oldClientNeverCrashes() {
        // Simulates a staggered rollout: the server has shipped a NEW field (`middlename`) this
        // (old) client schema does not know about. Decoding MUST succeed, never throw.
        val serverPayload = """
            {
              "firstname":"Grace",
              "lastname":"Achieng",
              "middlename":"Wanjiru",
              "mobileNo":"+254723456789",
              "active":true,
              "activationDate":"21 July 2026",
              "officeId":1,
              "groupId":9001,
              "locale":"en",
              "dateFormat":"dd MMMM yyyy"
            }
        """.trimIndent()
        val decoded = json.decodeFromString(CreateMemberRequestDto.serializer(), serverPayload)
        assertEquals("Grace", decoded.firstname)
        assertEquals(9001L, decoded.groupId)
    }

    // ---------- CreateMemberResponseDto (POST /clients response) ----------

    @Test
    fun createMemberResponseDto_constructsWithAllFields() {
        assertEquals(5010L, createMemberResponseDto.resourceId)
        assertEquals(5010L, createMemberResponseDto.clientId)
    }

    @Test
    fun createMemberResponseDto_equality() {
        assertEquals(createMemberResponseDto.copy(), createMemberResponseDto.copy())
    }

    @Test
    fun createMemberResponseDto_carriesSchemaVersion() {
        assertEquals(1, CreateMemberResponseDto.SCHEMA_VERSION)
    }

    @Test
    fun createMemberResponseDto_serializationRoundTrips() {
        val encoded = json.encodeToString(CreateMemberResponseDto.serializer(), createMemberResponseDto)
        assertTrue(encoded.contains("\"resourceId\""))
        assertTrue(encoded.contains("\"clientId\""))

        val decoded = json.decodeFromString(CreateMemberResponseDto.serializer(), encoded)
        assertEquals(createMemberResponseDto, decoded)
    }

    // ---------- UploadMemberPhotoResponseDto (POST /clients/{clientId}/images response) ----------
    // NOTE: no `UploadMemberPhotoRequestDto` is generated — `body_type: multipart/form-data` with
    // a raw `file: File` field is NOT expressible as a kotlinx.serialization JSON DTO; the
    // multipart body is constructed directly in the API service implementation
    // (MultiPartFormDataContent over the platform file bytes), never round-tripped as JSON. See
    // API.md#dtos for the flagged gap.

    @Test
    fun uploadMemberPhotoResponseDto_constructsWithAllFields() {
        assertEquals(7001L, uploadMemberPhotoResponseDto.resourceId)
    }

    @Test
    fun uploadMemberPhotoResponseDto_equality() {
        assertEquals(uploadMemberPhotoResponseDto.copy(), uploadMemberPhotoResponseDto.copy())
    }

    @Test
    fun uploadMemberPhotoResponseDto_carriesSchemaVersion() {
        assertEquals(1, UploadMemberPhotoResponseDto.SCHEMA_VERSION)
    }

    @Test
    fun uploadMemberPhotoResponseDto_serializationRoundTrips() {
        val encoded = json.encodeToString(UploadMemberPhotoResponseDto.serializer(), uploadMemberPhotoResponseDto)
        assertTrue(encoded.contains("\"resourceId\""))

        val decoded = json.decodeFromString(UploadMemberPhotoResponseDto.serializer(), encoded)
        assertEquals(uploadMemberPhotoResponseDto, decoded)
    }

    // ---------- reuse: assign_member_role reuses UpdateMemberRoleRequestDto/ResponseDto ----------

    @Test
    fun assignMemberRoleStep_reusesUpdateMemberRoleRequestDto_sameDatatableEndpointShape() {
        // member-add's `assign_member_role` (POST) and member-profile's `update_member_role`
        // (PUT) both target `/datatables/dt_member_role/{clientId}` with an identical
        // role/groupId/assignedDate body — no duplicate DTO pair introduced.
        val dto = UpdateMemberRoleRequestDto(
            role = MemberRoleDto.SECRETARY,
            groupId = 9001L,
            assignedDate = "21 July 2026",
        )
        val encoded = json.encodeToString(UpdateMemberRoleRequestDto.serializer(), dto)
        assertTrue(encoded.contains("\"role\""))
        assertTrue(encoded.contains("\"groupId\""))
        assertTrue(encoded.contains("\"assignedDate\""))
        assertEquals(dto, json.decodeFromString(UpdateMemberRoleRequestDto.serializer(), encoded))
    }
}
