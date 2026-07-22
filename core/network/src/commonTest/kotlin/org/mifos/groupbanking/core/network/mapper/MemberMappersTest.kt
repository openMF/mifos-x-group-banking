/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.mapper

import org.mifos.groupbanking.core.model.LoanStatus
import org.mifos.groupbanking.core.model.MemberRole
import org.mifos.groupbanking.core.network.model.LoanStatusDto
import org.mifos.groupbanking.core.network.model.MemberDto
import org.mifos.groupbanking.core.network.model.MemberPageDto
import org.mifos.groupbanking.core.network.model.MemberRoleDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * TDD RED-first coverage for the member-list DTO<->domain mappers (`GET
 * /groups/{groupId}/clients`). Every field on `MemberDto` declared in `MemberDto.kt` must be
 * exercised here (RULE: all-fields-mapped).
 */
class MemberMappersTest {

    private val amaraDto = MemberDto(
        id = "MBR-20260509-001",
        fineractClientId = 5001L,
        displayName = "Amara Otieno",
        photoUri = "https://cdn.example.org/photos/5001.jpg",
        role = MemberRoleDto.CHAIRPERSON,
        savingsBalance = 1250.50,
        loanStatus = LoanStatusDto.ACTIVE,
    )

    // ---------- MemberDto -> Member (all 7 fields) ----------

    @Test
    fun memberDto_toDomainModel_mapsAllFields() {
        val domain = amaraDto.toDomainModel()

        assertEquals("MBR-20260509-001", domain.id)
        assertEquals(5001L, domain.fineractClientId)
        assertEquals("Amara Otieno", domain.displayName)
        assertEquals("https://cdn.example.org/photos/5001.jpg", domain.photoUri)
        assertEquals(MemberRole.CHAIRPERSON, domain.role)
        assertEquals(1250.50, domain.savingsBalance)
        assertEquals(LoanStatus.ACTIVE, domain.loanStatus)
    }

    @Test
    fun memberDto_toDomainModel_nullPhotoUriMapsToNull() {
        val dto = amaraDto.copy(photoUri = null)
        assertNull(dto.toDomainModel().photoUri)
    }

    // ---------- unknown-enum fallback propagates through the whole row ----------

    @Test
    fun memberDto_toDomainModel_unknownRoleMapsToDomainUnknown() {
        val dto = amaraDto.copy(role = MemberRoleDto.UNKNOWN)
        assertEquals(MemberRole.UNKNOWN, dto.toDomainModel().role)
    }

    @Test
    fun memberDto_toDomainModel_unknownLoanStatusMapsToDomainUnknown() {
        val dto = amaraDto.copy(loanStatus = LoanStatusDto.UNKNOWN)
        assertEquals(LoanStatus.UNKNOWN, dto.toDomainModel().loanStatus)
    }

    // ---------- enum mappers (every entry) ----------

    @Test
    fun memberRoleDto_toDomainModel_mapsEveryEntry() {
        assertEquals(MemberRole.CHAIRPERSON, MemberRoleDto.CHAIRPERSON.toDomainModel())
        assertEquals(MemberRole.TREASURER, MemberRoleDto.TREASURER.toDomainModel())
        assertEquals(MemberRole.SECRETARY, MemberRoleDto.SECRETARY.toDomainModel())
        assertEquals(MemberRole.MEMBER, MemberRoleDto.MEMBER.toDomainModel())
        assertEquals(MemberRole.UNKNOWN, MemberRoleDto.UNKNOWN.toDomainModel())
    }

    @Test
    fun loanStatusDto_toDomainModel_mapsEveryEntry() {
        assertEquals(LoanStatus.ACTIVE, LoanStatusDto.ACTIVE.toDomainModel())
        assertEquals(LoanStatus.NONE, LoanStatusDto.NONE.toDomainModel())
        assertEquals(LoanStatus.OVERDUE, LoanStatusDto.OVERDUE.toDomainModel())
        assertEquals(LoanStatus.UNKNOWN, LoanStatusDto.UNKNOWN.toDomainModel())
    }

    // ---------- List<MemberDto>.toDomainModels() batch converter ----------

    @Test
    fun memberDtoList_toDomainModels_mapsEveryItemInOrder() {
        val jumaDto = amaraDto.copy(id = "MBR-2", displayName = "Juma Kamau", role = MemberRoleDto.MEMBER)
        val result = listOf(amaraDto, jumaDto).toDomainModels()

        assertEquals(2, result.size)
        assertEquals("MBR-20260509-001", result[0].id)
        assertEquals("MBR-2", result[1].id)
        assertEquals(MemberRole.MEMBER, result[1].role)
    }

    @Test
    fun memberDtoList_toDomainModels_emptyListStaysEmpty() {
        assertEquals(emptyList(), emptyList<MemberDto>().toDomainModels())
    }

    // ---------- MemberPageDto -> MemberPage ----------

    @Test
    fun memberPageDto_toDomainModel_mapsTotalFilteredRecordsAndMembers() {
        val page = MemberPageDto(totalFilteredRecords = 12, pageItems = listOf(amaraDto))
        val domain = page.toDomainModel()

        assertEquals(12, domain.totalFilteredRecords)
        assertEquals(1, domain.members.size)
        assertEquals("MBR-20260509-001", domain.members[0].id)
    }

    @Test
    fun memberPageDto_toDomainModel_emptyPageItemsMapsToEmptyMembers() {
        val page = MemberPageDto(totalFilteredRecords = 0, pageItems = emptyList())
        assertEquals(emptyList(), page.toDomainModel().members)
    }
}
