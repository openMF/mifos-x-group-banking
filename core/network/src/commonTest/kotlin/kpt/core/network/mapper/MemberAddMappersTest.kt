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

import kpt.core.model.CreateMemberRequest
import kpt.core.model.MemberRole
import kpt.core.network.model.CreateMemberResponseDto
import kpt.core.network.model.MemberRoleDto
import kpt.core.network.model.UploadMemberPhotoResponseDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TDD RED-first coverage for the member-add create-chain domain<->DTO mappers. Every field on
 * every DTO declared in `MemberAddDto.kt` (plus the reused `UpdateMemberRoleRequestDto`) is
 * exercised here (RULE: all-fields-mapped).
 */
class MemberAddMappersTest {

    private val request = CreateMemberRequest(
        firstName = "Grace",
        lastName = "Achieng",
        phone = "+254723456789",
        photoUri = "file:///cache/photo.jpg",
        role = MemberRole.SECRETARY,
        groupId = "9001",
        officeId = 1L,
        activationDate = "21 July 2026",
    )

    private val requestNoPhoto = request.copy(photoUri = null)

    // ---------- CreateMemberRequest -> CreateMemberRequestDto (step 1, /clients) ----------

    @Test
    fun createMemberRequest_toCreateMemberRequestDto_mapsAllFields() {
        val dto = request.toCreateMemberRequestDto()

        assertEquals("Grace", dto.firstname)
        assertEquals("Achieng", dto.lastname)
        assertEquals("+254723456789", dto.mobileNo)
        assertTrue(dto.active)
        assertEquals("21 July 2026", dto.activationDate)
        assertEquals(1L, dto.officeId)
        assertEquals(9001L, dto.groupId)
    }

    @Test
    fun createMemberRequest_toCreateMemberRequestDto_defaultsLocaleAndDateFormat() {
        val dto = request.toCreateMemberRequestDto()
        assertEquals("en", dto.locale)
        assertEquals("dd MMMM yyyy", dto.dateFormat)
    }

    @Test
    fun createMemberRequest_toCreateMemberRequestDto_acceptsOverriddenLocaleAndDateFormat() {
        val dto = request.toCreateMemberRequestDto(locale = "fr", dateFormat = "yyyy-MM-dd")
        assertEquals("fr", dto.locale)
        assertEquals("yyyy-MM-dd", dto.dateFormat)
    }

    @Test
    fun createMemberRequest_toCreateMemberRequestDto_photoUriIsNotCarried() {
        // photoUri is NOT part of the /clients create body — it is consumed separately by the
        // (DTO-less) upload_photo multipart step. Both with and without a photo, the request DTO
        // shape is identical.
        assertEquals(request.toCreateMemberRequestDto(), requestNoPhoto.toCreateMemberRequestDto())
    }

    // ---------- CreateMemberRequest -> UpdateMemberRoleRequestDto (step 2, reused) ----------

    @Test
    fun createMemberRequest_toAssignMemberRoleRequestDto_mapsRoleGroupIdAndAssignedDate() {
        val dto = request.toAssignMemberRoleRequestDto()

        assertEquals(MemberRoleDto.SECRETARY, dto.role)
        assertEquals(9001L, dto.groupId)
        assertEquals("21 July 2026", dto.assignedDate)
    }

    @Test
    fun createMemberRequest_toAssignMemberRoleRequestDto_reusesMemberRoleToDtoMapper() {
        // MemberRole.toDto() is already declared in MemberProfileMappers.kt — not redefined here.
        val dto = CreateMemberRequest(
            firstName = "Amara",
            lastName = "Otieno",
            phone = "+254700000000",
            photoUri = null,
            role = MemberRole.CHAIRPERSON,
            groupId = "42",
            officeId = 2L,
            activationDate = "1 January 2026",
        ).toAssignMemberRoleRequestDto()
        assertEquals(MemberRoleDto.CHAIRPERSON, dto.role)
    }

    // ---------- CreateMemberResponseDto -> MemberCreationResult (composite, all 3 steps) ----------

    @Test
    fun createMemberResponseDto_toDomainModel_mapsAllFieldsWithPhotoUploaded() {
        val responseDto = CreateMemberResponseDto(resourceId = 5010L, clientId = 5010L)
        val result = responseDto.toDomainModel(request = request, photoUploaded = true)

        assertEquals("5010", result.memberId)
        assertEquals(5010L, result.fineractClientId)
        assertEquals("9001", result.groupId)
        assertEquals(MemberRole.SECRETARY, result.role)
        assertTrue(result.photoUploaded)
    }

    @Test
    fun createMemberResponseDto_toDomainModel_photoUploadedFalseWhenNoPhotoStepRan() {
        val responseDto = CreateMemberResponseDto(resourceId = 5011L, clientId = 5011L)
        val result = responseDto.toDomainModel(request = requestNoPhoto, photoUploaded = false)

        assertFalse(result.photoUploaded)
    }

    @Test
    fun uploadMemberPhotoResponseDto_signalsPhotoUploadedTrueWhenPresent() {
        // Repository-layer usage pattern: `uploadPhotoResponse != null` becomes the
        // `photoUploaded` flag passed into `toDomainModel`.
        val uploadResponse: UploadMemberPhotoResponseDto? = UploadMemberPhotoResponseDto(resourceId = 7001L)
        val responseDto = CreateMemberResponseDto(resourceId = 5012L, clientId = 5012L)
        val result = responseDto.toDomainModel(request = request, photoUploaded = uploadResponse != null)

        assertTrue(result.photoUploaded)
    }
}
