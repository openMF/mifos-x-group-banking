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

import org.mifos.groupbanking.core.model.MemberRole
import org.mifos.groupbanking.core.model.UpdateMemberRoleRequest
import org.mifos.groupbanking.core.network.model.FineractStatusDto
import org.mifos.groupbanking.core.network.model.MemberAccountsDto
import org.mifos.groupbanking.core.network.model.MemberLoanAccountDto
import org.mifos.groupbanking.core.network.model.MemberLoanAccountSummaryDto
import org.mifos.groupbanking.core.network.model.MemberProfileDto
import org.mifos.groupbanking.core.network.model.MemberRoleDto
import org.mifos.groupbanking.core.network.model.MemberRoleInfoDto
import org.mifos.groupbanking.core.network.model.MemberSavingsAccountDto
import org.mifos.groupbanking.core.network.model.UpdateMemberRoleRequestDto
import org.mifos.groupbanking.core.network.model.UpdateMemberRoleResponseDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * TDD RED-first coverage for the member-profile DTO<->domain mappers. Every field on every DTO
 * declared in `MemberProfileDto.kt` must be exercised here (RULE: all-fields-mapped).
 */
class MemberProfileMappersTest {

    private val activeStatusDto = FineractStatusDto(id = 300, value = "Active")

    private val profileDto = MemberProfileDto(
        id = 5001L,
        displayName = "Amara Otieno",
        firstName = "Amara",
        lastName = "Otieno",
        mobileNo = "+254712345678",
        imagePresent = true,
        status = activeStatusDto,
        activationDate = "2025-03-14",
        officeId = 1L,
    )

    // ---------- MemberProfileDto -> MemberProfile (all 9 fields) ----------

    @Test
    fun memberProfileDto_toDomainModel_mapsAllFields() {
        val domain = profileDto.toDomainModel()

        assertEquals(5001L, domain.id)
        assertEquals("Amara Otieno", domain.displayName)
        assertEquals("Amara", domain.firstName)
        assertEquals("Otieno", domain.lastName)
        assertEquals("+254712345678", domain.phone)
        assertTrue(domain.hasPhoto)
        assertEquals(300, domain.status.id)
        assertEquals("Active", domain.status.value)
        assertEquals("2025-03-14", domain.joinDate)
        assertEquals(1L, domain.officeId)
    }

    @Test
    fun memberProfileDto_toDomainModel_imagePresentFalseMapsToHasPhotoFalse() {
        val domain = profileDto.copy(imagePresent = false).toDomainModel()
        assertTrue(!domain.hasPhoto)
    }

    // ---------- FineractStatusDto -> MemberStatus ----------

    @Test
    fun fineractStatusDto_toDomainModel_mapsBothFields() {
        val domain = activeStatusDto.toDomainModel()
        assertEquals(300, domain.id)
        assertEquals("Active", domain.value)
    }

    // ---------- MemberAccountsDto -> MemberAccounts (aggregated per api.yaml#dtos.MemberAccounts) ----------

    private val savingsAccountDto = MemberSavingsAccountDto(
        id = 9001L,
        productName = "VSLA Savings",
        accountNo = "SA-9001",
        balance = 1250.50,
        status = activeStatusDto,
    )

    private val secondSavingsAccountDto = MemberSavingsAccountDto(
        id = 9002L,
        productName = "Emergency Fund",
        accountNo = "SA-9002",
        balance = 200.0,
        status = activeStatusDto,
    )

    private val loanSummaryDto = MemberLoanAccountSummaryDto(
        principalDisbursed = 500.0,
        principalOutstanding = 320.0,
        totalOverdue = 40.0,
    )

    private val loanAccountDto = MemberLoanAccountDto(
        id = 7001L,
        productName = "Group Loan",
        accountNo = "LA-7001",
        status = activeStatusDto,
        summary = loanSummaryDto,
    )

    @Test
    fun memberAccountsDto_toDomainModel_savingsBalanceIsSumOfAllSavingsAccountBalances() {
        val dto = MemberAccountsDto(savingsAccounts = listOf(savingsAccountDto, secondSavingsAccountDto))
        val domain = dto.toDomainModel()
        assertEquals(1450.50, domain.savingsBalance)
    }

    @Test
    fun memberAccountsDto_toDomainModel_noSavingsAccountsMapsToZeroBalance() {
        val domain = MemberAccountsDto().toDomainModel()
        assertEquals(0.0, domain.savingsBalance)
    }

    @Test
    fun memberAccountsDto_toDomainModel_activeLoanDerivedFromFirstLoanAccount() {
        val dto = MemberAccountsDto(loanAccounts = listOf(loanAccountDto))
        val domain = dto.toDomainModel()

        assertEquals(7001L, domain.activeLoan?.id)
        assertEquals("Group Loan", domain.activeLoan?.productName)
        assertEquals(320.0, domain.activeLoan?.outstandingBalance)
        assertTrue(domain.activeLoan?.inArrears == true)
        assertNull(domain.activeLoan?.dueDate)
    }

    @Test
    fun memberAccountsDto_toDomainModel_notInArrearsWhenTotalOverdueIsZero() {
        val dto = MemberAccountsDto(
            loanAccounts = listOf(loanAccountDto.copy(summary = loanSummaryDto.copy(totalOverdue = 0.0))),
        )
        val domain = dto.toDomainModel()
        assertTrue(domain.activeLoan?.inArrears == false)
    }

    @Test
    fun memberAccountsDto_toDomainModel_noLoanAccountsMapsToNullActiveLoan() {
        val domain = MemberAccountsDto().toDomainModel()
        assertNull(domain.activeLoan)
    }

    @Test
    fun memberAccountsDto_toDomainModel_savingsHistoryHasNoWireSourceAndIsAlwaysEmpty() {
        val dto = MemberAccountsDto(savingsAccounts = listOf(savingsAccountDto))
        val domain = dto.toDomainModel()
        assertTrue(domain.savingsHistory.isEmpty())
    }

    // ---------- MemberRoleInfoDto -> MemberRoleInfo (reuses MemberRoleDto.toDomainModel()) ----------

    @Test
    fun memberRoleInfoDto_toDomainModel_mapsAllFields() {
        val dto = MemberRoleInfoDto(role = MemberRoleDto.CHAIRPERSON, groupId = 42L, assignedDate = "2025-01-05")
        val domain = dto.toDomainModel()

        assertEquals(MemberRole.CHAIRPERSON, domain.role)
        assertEquals(42L, domain.groupId)
        assertEquals("2025-01-05", domain.assignedDate)
    }

    @Test
    fun memberRoleInfoDto_toDomainModel_unknownRoleMapsToDomainUnknown() {
        val dto = MemberRoleInfoDto(role = MemberRoleDto.UNKNOWN, groupId = 42L, assignedDate = "2025-01-05")
        assertEquals(MemberRole.UNKNOWN, dto.toDomainModel().role)
    }

    @Test
    fun memberRoleInfoDtoList_toDomainModels_mapsEveryItemInOrder() {
        val chairDto = MemberRoleInfoDto(role = MemberRoleDto.CHAIRPERSON, groupId = 42L, assignedDate = "2025-01-05")
        val treasurerDto = MemberRoleInfoDto(role = MemberRoleDto.TREASURER, groupId = 42L, assignedDate = "2025-02-11")
        val result = listOf(chairDto, treasurerDto).toDomainModels()

        assertEquals(2, result.size)
        assertEquals(MemberRole.CHAIRPERSON, result[0].role)
        assertEquals(MemberRole.TREASURER, result[1].role)
    }

    @Test
    fun memberRoleInfoDtoList_toDomainModels_emptyListStaysEmpty() {
        assertEquals(emptyList(), emptyList<MemberRoleInfoDto>().toDomainModels())
    }

    // ---------- UpdateMemberRoleRequest (domain) -> UpdateMemberRoleRequestDto (reverse mapper, write op) ----------

    @Test
    fun updateMemberRoleRequest_toDto_mapsAllFields() {
        val request = UpdateMemberRoleRequest(
            role = MemberRole.TREASURER,
            groupId = 42L,
            assignedDate = "2025-03-01",
        )
        val dto = request.toDto()

        assertEquals(MemberRoleDto.TREASURER, dto.role)
        assertEquals(42L, dto.groupId)
        assertEquals("2025-03-01", dto.assignedDate)
    }

    @Test
    fun updateMemberRoleRequest_toDto_unknownRoleMapsToDtoUnknown() {
        val request = UpdateMemberRoleRequest(
            role = MemberRole.UNKNOWN,
            groupId = 42L,
            assignedDate = "2025-03-01",
        )
        assertEquals(MemberRoleDto.UNKNOWN, request.toDto().role)
    }

    // ---------- UpdateMemberRoleResponseDto -> UpdateMemberRoleResult ----------

    @Test
    fun updateMemberRoleResponseDto_toDomainModel_mapsResourceId() {
        val dto = UpdateMemberRoleResponseDto(resourceId = 99L)
        assertEquals(99L, dto.toDomainModel().resourceId)
    }
}
