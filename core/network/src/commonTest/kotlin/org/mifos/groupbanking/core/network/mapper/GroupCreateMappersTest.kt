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

import org.mifos.groupbanking.core.model.ContributionModel
import org.mifos.groupbanking.core.model.CreateGroupRequest
import org.mifos.groupbanking.core.model.CreateGroupTypeConfig
import org.mifos.groupbanking.core.model.GroupTypeSlug
import org.mifos.groupbanking.core.model.PayoutOrderMethod
import org.mifos.groupbanking.core.model.SavingsMechanism
import org.mifos.groupbanking.core.model.ShareoutFormula
import org.mifos.groupbanking.core.network.model.ContributionModelDto
import org.mifos.groupbanking.core.network.model.CreateGroupRequestDto
import org.mifos.groupbanking.core.network.model.CreateGroupResponseDto
import org.mifos.groupbanking.core.network.model.CreateGroupTypeConfigDto
import org.mifos.groupbanking.core.network.model.GroupTypeDto
import org.mifos.groupbanking.core.network.model.OfficeDto
import org.mifos.groupbanking.core.network.model.PayoutOrderMethodDto
import org.mifos.groupbanking.core.network.model.SavingsMechanismDto
import org.mifos.groupbanking.core.network.model.ShareoutFormulaDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * TDD RED-first coverage for the group-create wizard DTO<->domain mappers. Every field on every
 * DTO declared in `GroupCreateDto.kt` must be exercised here (RULE: all-fields-mapped).
 */
class GroupCreateMappersTest {

    private val vslaTypeConfigDto = CreateGroupTypeConfigDto(
        groupType = GroupTypeDto.VSLA,
        poolModel = SavingsMechanismDto.ACCUMULATING,
        contributionModel = ContributionModelDto.SHARE_BASED_VARIABLE,
        shareoutFormula = ShareoutFormulaDto.PRORATA_SHARES,
        payoutOrderMethod = PayoutOrderMethodDto.NA,
        shareValue = 5.0,
        contributionAmount = 0.0,
        socialFundEnabled = true,
        socialFundPercent = 5.0,
        cycleLengthMonths = 12,
        loanMultiplier = 3.0,
        interestRate = 10.0,
        fineAmount = 1.0,
        maxMembers = 30,
    )

    private val requestDto = CreateGroupRequestDto(
        name = "Sunrise VSLA",
        officeId = 1L,
        userId = 42L,
        currency = "KES",
        meetingDay = "MONDAY",
        meetingTime = "14:00",
        typeConfig = vslaTypeConfigDto,
    )

    private val vslaTypeConfig = CreateGroupTypeConfig(
        groupType = GroupTypeSlug.VSLA,
        poolModel = SavingsMechanism.ACCUMULATING,
        contributionModel = ContributionModel.SHARE_BASED_VARIABLE,
        shareoutFormula = ShareoutFormula.PRORATA_SHARES,
        payoutOrderMethod = PayoutOrderMethod.NA,
        shareValue = 5.0,
        contributionAmount = 0.0,
        socialFundEnabled = true,
        socialFundPercent = 5.0,
        cycleLengthMonths = 12,
        loanMultiplier = 3.0,
        interestRate = 10.0,
        fineAmount = 1.0,
        maxMembers = 30,
    )

    private val request = CreateGroupRequest(
        name = "Sunrise VSLA",
        officeId = 1L,
        userId = 42L,
        currency = "KES",
        meetingDay = "MONDAY",
        meetingTime = "14:00",
        typeConfig = vslaTypeConfig,
    )

    // ---------- CreateGroupTypeConfigDto -> CreateGroupTypeConfig (all 14 fields) ----------

    @Test
    fun createGroupTypeConfigDto_toDomainModel_mapsAllFields() {
        val domain = vslaTypeConfigDto.toDomainModel()

        assertEquals(GroupTypeSlug.VSLA, domain.groupType)
        assertEquals(SavingsMechanism.ACCUMULATING, domain.poolModel)
        assertEquals(ContributionModel.SHARE_BASED_VARIABLE, domain.contributionModel)
        assertEquals(ShareoutFormula.PRORATA_SHARES, domain.shareoutFormula)
        assertEquals(PayoutOrderMethod.NA, domain.payoutOrderMethod)
        assertEquals(5.0, domain.shareValue)
        assertEquals(0.0, domain.contributionAmount)
        assertEquals(true, domain.socialFundEnabled)
        assertEquals(5.0, domain.socialFundPercent)
        assertEquals(12, domain.cycleLengthMonths)
        assertEquals(3.0, domain.loanMultiplier)
        assertEquals(10.0, domain.interestRate)
        assertEquals(1.0, domain.fineAmount)
        assertEquals(30, domain.maxMembers)
    }

    // ---------- CreateGroupTypeConfig -> CreateGroupTypeConfigDto (reverse, request-side) ----------

    @Test
    fun createGroupTypeConfig_toDto_mapsAllFieldsBack() {
        val dto = vslaTypeConfig.toDto()
        assertEquals(vslaTypeConfigDto, dto)
    }

    // ---------- CreateGroupRequestDto <-> CreateGroupRequest ----------

    @Test
    fun createGroupRequestDto_toDomainModel_mapsAllFields() {
        val domain = requestDto.toDomainModel()

        assertEquals("Sunrise VSLA", domain.name)
        assertEquals(1L, domain.officeId)
        assertEquals(42L, domain.userId)
        assertEquals("KES", domain.currency)
        assertEquals("MONDAY", domain.meetingDay)
        assertEquals("14:00", domain.meetingTime)
        assertEquals(vslaTypeConfig, domain.typeConfig)
    }

    @Test
    fun createGroupRequest_toDto_mapsAllFieldsBack() {
        val dto = request.toDto()
        assertEquals(requestDto, dto)
    }

    // ---------- CreateGroupResponseDto -> GroupCreationResult ----------

    @Test
    fun createGroupResponseDto_toDomainModel_mapsAllFields() {
        val responseDto = CreateGroupResponseDto(
            groupId = "grp-9001",
            fineractCenterId = 501L,
            inviteCode = "AB12CD",
        )
        val domain = responseDto.toDomainModel()

        assertEquals("grp-9001", domain.groupId)
        assertEquals(501L, domain.fineractCenterId)
        assertEquals("AB12CD", domain.inviteCode)
    }

    // ---------- OfficeDto -> Office ----------

    @Test
    fun officeDto_toDomainModel_mapsAllFields() {
        val dto = OfficeDto(id = 1L, name = "Head Office", nameDecorated = ".Head Office", externalId = "HO-001")
        val domain = dto.toDomainModel()

        assertEquals(1L, domain.id)
        assertEquals("Head Office", domain.name)
        assertEquals(".Head Office", domain.nameDecorated)
        assertEquals("HO-001", domain.externalId)
    }

    @Test
    fun officeDto_toDomainModel_nullExternalIdMapsThrough() {
        val dto = OfficeDto(id = 2L, name = "Branch A", nameDecorated = "..Branch A", externalId = null)
        assertNull(dto.toDomainModel().externalId)
    }

    @Test
    fun officeDtoList_toDomainModels_mapsEveryItemInOrder() {
        val offices = listOf(
            OfficeDto(id = 1L, name = "Head Office", nameDecorated = ".Head Office", externalId = "HO-001"),
            OfficeDto(id = 2L, name = "Branch A", nameDecorated = "..Branch A"),
        )
        val result = offices.toDomainModels()

        assertEquals(2, result.size)
        assertEquals("Head Office", result[0].name)
        assertEquals("Branch A", result[1].name)
        assertNull(result[1].externalId)
    }

    @Test
    fun officeDtoList_toDomainModels_emptyListStaysEmpty() {
        assertEquals(emptyList(), emptyList<OfficeDto>().toDomainModels())
    }

    // ---------- unknown-enum fallback propagates through the whole typeConfig ----------

    @Test
    fun createGroupTypeConfigDto_toDomainModel_unknownGroupTypeMapsToDomainUnknown() {
        val dto = vslaTypeConfigDto.copy(groupType = GroupTypeDto.UNKNOWN)
        assertEquals(GroupTypeSlug.UNKNOWN, dto.toDomainModel().groupType)
    }

    @Test
    fun createGroupTypeConfigDto_toDomainModel_unknownPoolModelMapsToDomainUnknown() {
        val dto = vslaTypeConfigDto.copy(poolModel = SavingsMechanismDto.UNKNOWN)
        assertEquals(SavingsMechanism.UNKNOWN, dto.toDomainModel().poolModel)
    }

    @Test
    fun createGroupTypeConfigDto_toDomainModel_unknownContributionModelMapsToDomainUnknown() {
        val dto = vslaTypeConfigDto.copy(contributionModel = ContributionModelDto.UNKNOWN)
        assertEquals(ContributionModel.UNKNOWN, dto.toDomainModel().contributionModel)
    }

    @Test
    fun createGroupTypeConfigDto_toDomainModel_unknownShareoutFormulaMapsToDomainUnknown() {
        val dto = vslaTypeConfigDto.copy(shareoutFormula = ShareoutFormulaDto.UNKNOWN)
        assertEquals(ShareoutFormula.UNKNOWN, dto.toDomainModel().shareoutFormula)
    }

    @Test
    fun createGroupTypeConfigDto_toDomainModel_unknownPayoutOrderMethodMapsToDomainUnknown() {
        val dto = vslaTypeConfigDto.copy(payoutOrderMethod = PayoutOrderMethodDto.UNKNOWN)
        assertEquals(PayoutOrderMethod.UNKNOWN, dto.toDomainModel().payoutOrderMethod)
    }

    // ---------- enum mappers (every entry, both directions) ----------

    @Test
    fun contributionModelDto_toDomainModel_mapsEveryEntry() {
        assertEquals(ContributionModel.FIXED_AMOUNT, ContributionModelDto.FIXED_AMOUNT.toDomainModel())
        assertEquals(ContributionModel.SHARE_BASED_VARIABLE, ContributionModelDto.SHARE_BASED_VARIABLE.toDomainModel())
        assertEquals(ContributionModel.FIXED_NEGOTIATED, ContributionModelDto.FIXED_NEGOTIATED.toDomainModel())
        assertEquals(ContributionModel.UNKNOWN, ContributionModelDto.UNKNOWN.toDomainModel())
    }

    @Test
    fun contributionModel_toDto_mapsEveryEntry() {
        assertEquals(ContributionModelDto.FIXED_AMOUNT, ContributionModel.FIXED_AMOUNT.toDto())
        assertEquals(ContributionModelDto.SHARE_BASED_VARIABLE, ContributionModel.SHARE_BASED_VARIABLE.toDto())
        assertEquals(ContributionModelDto.FIXED_NEGOTIATED, ContributionModel.FIXED_NEGOTIATED.toDto())
        assertEquals(ContributionModelDto.UNKNOWN, ContributionModel.UNKNOWN.toDto())
    }

    @Test
    fun shareoutFormulaDto_toDomainModel_mapsEveryEntry() {
        assertEquals(ShareoutFormula.NONE, ShareoutFormulaDto.NONE.toDomainModel())
        assertEquals(ShareoutFormula.PRORATA_SHARES, ShareoutFormulaDto.PRORATA_SHARES.toDomainModel())
        assertEquals(ShareoutFormula.PRORATA_SAVINGS, ShareoutFormulaDto.PRORATA_SAVINGS.toDomainModel())
        assertEquals(ShareoutFormula.EQUAL, ShareoutFormulaDto.EQUAL.toDomainModel())
        assertEquals(ShareoutFormula.INVESTMENT_PROPORTIONAL, ShareoutFormulaDto.INVESTMENT_PROPORTIONAL.toDomainModel())
        assertEquals(ShareoutFormula.UNKNOWN, ShareoutFormulaDto.UNKNOWN.toDomainModel())
    }

    @Test
    fun shareoutFormula_toDto_mapsEveryEntry() {
        assertEquals(ShareoutFormulaDto.NONE, ShareoutFormula.NONE.toDto())
        assertEquals(ShareoutFormulaDto.PRORATA_SHARES, ShareoutFormula.PRORATA_SHARES.toDto())
        assertEquals(ShareoutFormulaDto.PRORATA_SAVINGS, ShareoutFormula.PRORATA_SAVINGS.toDto())
        assertEquals(ShareoutFormulaDto.EQUAL, ShareoutFormula.EQUAL.toDto())
        assertEquals(ShareoutFormulaDto.INVESTMENT_PROPORTIONAL, ShareoutFormula.INVESTMENT_PROPORTIONAL.toDto())
        assertEquals(ShareoutFormulaDto.UNKNOWN, ShareoutFormula.UNKNOWN.toDto())
    }

    @Test
    fun payoutOrderMethodDto_toDomainModel_mapsEveryEntry() {
        assertEquals(PayoutOrderMethod.FIXED_ORDER, PayoutOrderMethodDto.FIXED_ORDER.toDomainModel())
        assertEquals(PayoutOrderMethod.LOTTERY, PayoutOrderMethodDto.LOTTERY.toDomainModel())
        assertEquals(PayoutOrderMethod.AUCTION, PayoutOrderMethodDto.AUCTION.toDomainModel())
        assertEquals(PayoutOrderMethod.NEED_BASED, PayoutOrderMethodDto.NEED_BASED.toDomainModel())
        assertEquals(PayoutOrderMethod.NA, PayoutOrderMethodDto.NA.toDomainModel())
        assertEquals(PayoutOrderMethod.UNKNOWN, PayoutOrderMethodDto.UNKNOWN.toDomainModel())
    }

    @Test
    fun payoutOrderMethod_toDto_mapsEveryEntry() {
        assertEquals(PayoutOrderMethodDto.FIXED_ORDER, PayoutOrderMethod.FIXED_ORDER.toDto())
        assertEquals(PayoutOrderMethodDto.LOTTERY, PayoutOrderMethod.LOTTERY.toDto())
        assertEquals(PayoutOrderMethodDto.AUCTION, PayoutOrderMethod.AUCTION.toDto())
        assertEquals(PayoutOrderMethodDto.NEED_BASED, PayoutOrderMethod.NEED_BASED.toDto())
        assertEquals(PayoutOrderMethodDto.NA, PayoutOrderMethod.NA.toDto())
        assertEquals(PayoutOrderMethodDto.UNKNOWN, PayoutOrderMethod.UNKNOWN.toDto())
    }

    // ---------- reuse: GroupTypeDto/SavingsMechanismDto mappers already defined elsewhere ----------

    @Test
    fun groupType_reusesExistingGroupTypeDtoMapper_noNewEnumIntroduced() {
        // GroupTypeDto.toDomainModel() is defined in GroupMappers.kt (group-list feature) —
        // this test proves group-create's typeConfig.group_type reuses it directly.
        assertEquals(GroupTypeSlug.CBO_VILLAGE_BANK, GroupTypeDto.CBO.toDomainModel())
        assertEquals(GroupTypeSlug.BURIAL_WELFARE, GroupTypeDto.BURIAL.toDomainModel())
    }

    @Test
    fun poolModel_reusesExistingSavingsMechanismDtoMapper_noNewEnumIntroduced() {
        // SavingsMechanismDto.toDomainModel() is defined in GroupTypeConfigMappers.kt
        // (group-type-picker feature) — this test proves group-create's typeConfig.pool_model
        // reuses it directly.
        assertEquals(SavingsMechanism.ROTATING_PAYOUT, SavingsMechanismDto.ROTATING_PAYOUT.toDomainModel())
    }
}
