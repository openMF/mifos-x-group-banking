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

import kpt.core.model.ContributionMode
import kpt.core.model.GroupTypeSlug
import kpt.core.model.SavingsMechanism
import kpt.core.network.model.ContributionModeDto
import kpt.core.network.model.GroupTypeConfigDto
import kpt.core.network.model.GroupTypeSlugDto
import kpt.core.network.model.SavingsMechanismDto
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * TDD RED-first coverage for the group-type catalogue DTO<->domain mappers. Every field on
 * `GroupTypeConfigDto` declared in `GroupTypeConfigDto.kt` must be exercised here (RULE:
 * all-fields-mapped).
 */
class GroupTypeConfigMappersTest {

    private val vslaDto = GroupTypeConfigDto(
        typeSlug = GroupTypeSlugDto.VSLA,
        displayName = "Village Savings & Loan Association",
        tagline = "Save in shares each meeting; borrow up to 3x your shares; share-out at year end",
        savingsMechanism = SavingsMechanismDto.ACCUMULATING,
        contributionMode = ContributionModeDto.SHARE_BASED_VARIABLE,
        lendingEnabled = true,
        hasSocialFund = true,
        hasBankLinkage = false,
        welfareOnlyMode = false,
        formallyRegistered = false,
        defaultLoanMultiplier = 3.0,
        defaultInterestRatePct = 10.0,
        defaultCycleLengthMonths = 12,
        maxMembers = 30,
        minMembers = 5,
    )

    // ---------- GroupTypeConfigDto -> GroupTypeConfig (all 15 fields) ----------

    @Test
    fun groupTypeConfigDto_toDomainModel_mapsAllFields() {
        val domain = vslaDto.toDomainModel()

        assertEquals(GroupTypeSlug.VSLA, domain.typeSlug)
        assertEquals("Village Savings & Loan Association", domain.displayName)
        assertEquals("Save in shares each meeting; borrow up to 3x your shares; share-out at year end", domain.tagline)
        assertEquals(SavingsMechanism.ACCUMULATING, domain.savingsMechanism)
        assertEquals(ContributionMode.SHARE_BASED_VARIABLE, domain.contributionMode)
        assertEquals(true, domain.lendingEnabled)
        assertEquals(true, domain.hasSocialFund)
        assertEquals(false, domain.hasBankLinkage)
        assertEquals(false, domain.welfareOnlyMode)
        assertEquals(false, domain.formallyRegistered)
        assertEquals(3.0, domain.defaultLoanMultiplier)
        assertEquals(10.0, domain.defaultInterestRatePct)
        assertEquals(12, domain.defaultCycleLengthMonths)
        assertEquals(30, domain.maxMembers)
        assertEquals(5, domain.minMembers)
    }

    @Test
    fun groupTypeConfigDto_toDomainModel_jlgRow_noInternalPoolFlagsMapThrough() {
        val jlgDto = GroupTypeConfigDto(
            typeSlug = GroupTypeSlugDto.JLG,
            displayName = "Joint Liability Group",
            tagline = "4-10 members take an external MFI loan jointly",
            savingsMechanism = SavingsMechanismDto.NONE,
            contributionMode = ContributionModeDto.MINIMAL,
            lendingEnabled = false,
            hasSocialFund = false,
            hasBankLinkage = true,
            welfareOnlyMode = false,
            formallyRegistered = false,
            defaultLoanMultiplier = 0.0,
            defaultInterestRatePct = 0.0,
            defaultCycleLengthMonths = 4,
            maxMembers = 10,
            minMembers = 4,
        )

        val domain = jlgDto.toDomainModel()

        assertEquals(GroupTypeSlug.JLG, domain.typeSlug)
        assertEquals(SavingsMechanism.NONE, domain.savingsMechanism)
        assertEquals(ContributionMode.MINIMAL, domain.contributionMode)
        assertEquals(false, domain.lendingEnabled)
        assertEquals(true, domain.hasBankLinkage)
    }

    // ---------- unknown-enum fallback propagates through the whole row ----------

    @Test
    fun groupTypeConfigDto_toDomainModel_unknownTypeSlugMapsToDomainUnknown() {
        val dto = vslaDto.copy(typeSlug = GroupTypeSlugDto.UNKNOWN)
        assertEquals(GroupTypeSlug.UNKNOWN, dto.toDomainModel().typeSlug)
    }

    @Test
    fun groupTypeConfigDto_toDomainModel_unknownSavingsMechanismMapsToDomainUnknown() {
        val dto = vslaDto.copy(savingsMechanism = SavingsMechanismDto.UNKNOWN)
        assertEquals(SavingsMechanism.UNKNOWN, dto.toDomainModel().savingsMechanism)
    }

    @Test
    fun groupTypeConfigDto_toDomainModel_unknownContributionModeMapsToDomainUnknown() {
        val dto = vslaDto.copy(contributionMode = ContributionModeDto.UNKNOWN)
        assertEquals(ContributionMode.UNKNOWN, dto.toDomainModel().contributionMode)
    }

    // ---------- enum mappers (every entry) ----------

    @Test
    fun groupTypeSlugDto_toDomainModel_mapsEveryEntry() {
        assertEquals(GroupTypeSlug.VSLA, GroupTypeSlugDto.VSLA.toDomainModel())
        assertEquals(GroupTypeSlug.ROSCA, GroupTypeSlugDto.ROSCA.toDomainModel())
        assertEquals(GroupTypeSlug.ASCA, GroupTypeSlugDto.ASCA.toDomainModel())
        assertEquals(GroupTypeSlug.SILC, GroupTypeSlugDto.SILC.toDomainModel())
        assertEquals(GroupTypeSlug.SHG, GroupTypeSlugDto.SHG.toDomainModel())
        assertEquals(GroupTypeSlug.SACCO, GroupTypeSlugDto.SACCO.toDomainModel())
        assertEquals(GroupTypeSlug.CBO_VILLAGE_BANK, GroupTypeSlugDto.CBO_VILLAGE_BANK.toDomainModel())
        assertEquals(GroupTypeSlug.BURIAL_WELFARE, GroupTypeSlugDto.BURIAL_WELFARE.toDomainModel())
        assertEquals(GroupTypeSlug.JLG, GroupTypeSlugDto.JLG.toDomainModel())
        assertEquals(GroupTypeSlug.UNKNOWN, GroupTypeSlugDto.UNKNOWN.toDomainModel())
    }

    @Test
    fun savingsMechanismDto_toDomainModel_mapsEveryEntry() {
        assertEquals(SavingsMechanism.ACCUMULATING, SavingsMechanismDto.ACCUMULATING.toDomainModel())
        assertEquals(SavingsMechanism.ROTATING_PAYOUT, SavingsMechanismDto.ROTATING_PAYOUT.toDomainModel())
        assertEquals(SavingsMechanism.NONE, SavingsMechanismDto.NONE.toDomainModel())
        assertEquals(SavingsMechanism.UNKNOWN, SavingsMechanismDto.UNKNOWN.toDomainModel())
    }

    @Test
    fun contributionModeDto_toDomainModel_mapsEveryEntry() {
        assertEquals(ContributionMode.SHARE_BASED_VARIABLE, ContributionModeDto.SHARE_BASED_VARIABLE.toDomainModel())
        assertEquals(ContributionMode.FIXED, ContributionModeDto.FIXED.toDomainModel())
        assertEquals(ContributionMode.MINIMAL, ContributionModeDto.MINIMAL.toDomainModel())
        assertEquals(ContributionMode.UNKNOWN, ContributionModeDto.UNKNOWN.toDomainModel())
    }

    // ---------- List<GroupTypeConfigDto>.toDomainModels() batch converter ----------

    @Test
    fun groupTypeConfigDtoList_toDomainModels_mapsEveryItemInOrder() {
        val jlgDto = vslaDto.copy(typeSlug = GroupTypeSlugDto.JLG, displayName = "Joint Liability Group")
        val result = listOf(vslaDto, jlgDto).toDomainModels()

        assertEquals(2, result.size)
        assertEquals(GroupTypeSlug.VSLA, result[0].typeSlug)
        assertEquals(GroupTypeSlug.JLG, result[1].typeSlug)
        assertEquals("Joint Liability Group", result[1].displayName)
    }

    @Test
    fun groupTypeConfigDtoList_toDomainModels_emptyListStaysEmpty() {
        assertEquals(emptyList(), emptyList<GroupTypeConfigDto>().toDomainModels())
    }
}
