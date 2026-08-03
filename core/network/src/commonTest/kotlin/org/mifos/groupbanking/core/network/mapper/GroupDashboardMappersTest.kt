/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.mapper

import org.mifos.groupbanking.core.model.ActivityType
import org.mifos.groupbanking.core.model.GroupContributionModel
import org.mifos.groupbanking.core.model.GroupTypeSlug
import org.mifos.groupbanking.core.model.SavingsMechanism
import org.mifos.groupbanking.core.model.ViewerRole
import org.mifos.groupbanking.core.network.model.ActivityItemDto
import org.mifos.groupbanking.core.network.model.ActivityTypeDto
import org.mifos.groupbanking.core.network.model.GroupAccountsDto
import org.mifos.groupbanking.core.network.model.GroupConfigDto
import org.mifos.groupbanking.core.network.model.GroupContributionModelDto
import org.mifos.groupbanking.core.network.model.GroupCorpusDto
import org.mifos.groupbanking.core.network.model.GroupDashboardResponseDto
import org.mifos.groupbanking.core.network.model.GroupDetailDto
import org.mifos.groupbanking.core.network.model.GroupInstanceConfigDto
import org.mifos.groupbanking.core.network.model.GroupTypeSlugDto
import org.mifos.groupbanking.core.network.model.SavingsMechanismDto
import org.mifos.groupbanking.core.network.model.ViewerRoleDto
import org.mifos.groupbanking.core.network.model.ViewerRoleInfoDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * TDD RED-first coverage for the group-dashboard DTO<->domain mappers. Every field on every DTO
 * declared in `GroupDashboardDto.kt` must be exercised here (RULE: all-fields-mapped).
 */
class GroupDashboardMappersTest {

    private val vslaInstanceConfigDto = GroupInstanceConfigDto(
        groupType = GroupTypeSlugDto.VSLA,
        poolModel = SavingsMechanismDto.ACCUMULATING,
        contributionModel = GroupContributionModelDto.SHARE_BASED_VARIABLE,
        shareoutFormula = "PRORATA_SHARES",
        payoutOrderMethod = "NA",
        shareValue = 200.0,
        contributionAmount = 200.0,
        socialFundEnabled = true,
        cycleLengthMonths = 12,
        loanMultiplier = 3.0,
        interestRate = 10.0,
        fineAmount = 50.0,
    )

    private val groupDetailDto = GroupDetailDto(
        id = "grp-1",
        fineractCenterId = 500L,
        name = "Mwangaza Women's Group",
        cycleNumber = 1,
        cycleLengthMonths = 12,
        meetingFrequency = "Weekly",
        memberCount = 20,
        overdueLoansCount = 0,
        status = "ACTIVE",
        typeConfig = vslaInstanceConfigDto,
    )

    private val viewerRoleInfoDto = ViewerRoleInfoDto(role = ViewerRoleDto.ORGANIZER, memberId = 42L)

    private val corpusDto = GroupCorpusDto(
        currentBalance = 47500.0,
        openingBalance = 0.0,
        totalContributionsThisCycle = 52500.0,
        totalLoansOutstanding = 5000.0,
        lastUpdated = "2026-07-20T10:00:00Z",
        rotationPosition = null,
        nextRecipientName = null,
        nextRecipientPosition = null,
    )

    private val activityItemDto = ActivityItemDto(
        id = "act-1",
        type = ActivityTypeDto.DEPOSIT,
        description = "Weekly contribution",
        amount = 200.0,
        date = "2026-07-19",
        memberName = "Amina Hassan",
    )

    private val accountsDto = GroupAccountsDto(
        savingsBalance = 52500.0,
        loansOutstanding = 5000.0,
        activeLoanCount = 1,
        shareOutProjection = 2750.0,
        recentActivity = listOf(activityItemDto),
    )

    private val configDto = GroupConfigDto(
        shareValue = 200.0,
        shareMin = 1,
        shareMax = 5,
        contributionAmount = 200.0,
        loanMultiplier = 3.0,
        interestRate = 10.0,
        cycleLengthMonths = 12,
        fineAmount = 50.0,
        minimumDisbursementThreshold = 5000.0,
    )

    private val dashboardResponseDto = GroupDashboardResponseDto(
        group = groupDetailDto,
        viewerRole = viewerRoleInfoDto,
        corpus = corpusDto,
        accounts = accountsDto,
    )

    // ---------- GroupDashboardResponseDto -> GroupDashboard (composite) ----------

    @Test
    fun groupDashboardResponseDto_toDomainModel_mapsAllFourSections() {
        val domain = dashboardResponseDto.toDomainModel()

        assertEquals("grp-1", domain.group.id)
        assertEquals(ViewerRole.ORGANIZER, domain.viewerRole.role)
        assertEquals(42L, domain.viewerRole.memberId)
        assertEquals(47500.0, domain.corpus.currentBalance)
        assertEquals(52500.0, domain.accounts.savingsBalance)
        assertEquals(1, domain.accounts.recentActivity.size)
    }

    // ---------- GroupDetailDto -> GroupDetail (all 10 fields) ----------

    @Test
    fun groupDetailDto_toDomainModel_mapsAllFields() {
        val domain = groupDetailDto.toDomainModel()

        assertEquals("grp-1", domain.id)
        assertEquals(500L, domain.fineractCenterId)
        assertEquals("Mwangaza Women's Group", domain.name)
        assertEquals(1, domain.cycleNumber)
        assertEquals(12, domain.cycleLengthMonths)
        assertEquals("Weekly", domain.meetingFrequency)
        assertEquals(20, domain.memberCount)
        assertEquals(0, domain.overdueLoansCount)
        assertEquals("ACTIVE", domain.status)
        assertEquals(GroupTypeSlug.VSLA, domain.typeConfig.groupType)
    }

    // ---------- GroupInstanceConfigDto -> GroupInstanceConfig (all 12 fields) ----------

    @Test
    fun groupInstanceConfigDto_toDomainModel_mapsAllFields() {
        val domain = vslaInstanceConfigDto.toDomainModel()

        assertEquals(GroupTypeSlug.VSLA, domain.groupType)
        assertEquals(SavingsMechanism.ACCUMULATING, domain.poolModel)
        assertEquals(GroupContributionModel.SHARE_BASED_VARIABLE, domain.contributionModel)
        assertEquals("PRORATA_SHARES", domain.shareoutFormula)
        assertEquals("NA", domain.payoutOrderMethod)
        assertEquals(200.0, domain.shareValue)
        assertEquals(200.0, domain.contributionAmount)
        assertEquals(true, domain.socialFundEnabled)
        assertEquals(12, domain.cycleLengthMonths)
        assertEquals(3.0, domain.loanMultiplier)
        assertEquals(10.0, domain.interestRate)
        assertEquals(50.0, domain.fineAmount)
    }

    @Test
    fun groupInstanceConfigDto_toDomainModel_rotatingRow_poolModelAndContributionModelMapCorrectly() {
        val roscaDto = vslaInstanceConfigDto.copy(
            groupType = GroupTypeSlugDto.ROSCA,
            poolModel = SavingsMechanismDto.ROTATING_PAYOUT,
            contributionModel = GroupContributionModelDto.FIXED_AMOUNT,
        )
        val domain = roscaDto.toDomainModel()
        assertEquals(GroupTypeSlug.ROSCA, domain.groupType)
        assertEquals(SavingsMechanism.ROTATING_PAYOUT, domain.poolModel)
        assertEquals(GroupContributionModel.FIXED_AMOUNT, domain.contributionModel)
    }

    // ---------- unknown-enum fallback propagates through the whole row ----------

    @Test
    fun groupInstanceConfigDto_toDomainModel_unknownGroupTypeMapsToDomainUnknown() {
        val dto = vslaInstanceConfigDto.copy(groupType = GroupTypeSlugDto.UNKNOWN)
        assertEquals(GroupTypeSlug.UNKNOWN, dto.toDomainModel().groupType)
    }

    @Test
    fun groupInstanceConfigDto_toDomainModel_unknownPoolModelMapsToDomainUnknown() {
        val dto = vslaInstanceConfigDto.copy(poolModel = SavingsMechanismDto.UNKNOWN)
        assertEquals(SavingsMechanism.UNKNOWN, dto.toDomainModel().poolModel)
    }

    @Test
    fun groupContributionModelDto_toDomainModel_mapsEveryEntry() {
        assertEquals(GroupContributionModel.FIXED_AMOUNT, GroupContributionModelDto.FIXED_AMOUNT.toDomainModel())
        assertEquals(GroupContributionModel.SHARE_BASED_VARIABLE, GroupContributionModelDto.SHARE_BASED_VARIABLE.toDomainModel())
        assertEquals(GroupContributionModel.FIXED_NEGOTIATED, GroupContributionModelDto.FIXED_NEGOTIATED.toDomainModel())
        assertEquals(GroupContributionModel.UNKNOWN, GroupContributionModelDto.UNKNOWN.toDomainModel())
    }

    // ---------- ViewerRoleInfoDto -> ViewerRoleInfo (reuses shared ViewerRoleDto.toDomainModel()) ----------

    @Test
    fun viewerRoleInfoDto_toDomainModel_mapsAllFields() {
        val domain = viewerRoleInfoDto.toDomainModel()
        assertEquals(ViewerRole.ORGANIZER, domain.role)
        assertEquals(42L, domain.memberId)
    }

    @Test
    fun viewerRoleInfoDto_toDomainModel_unknownRoleMapsToDomainUnknown() {
        val dto = viewerRoleInfoDto.copy(role = ViewerRoleDto.UNKNOWN)
        assertEquals(ViewerRole.UNKNOWN, dto.toDomainModel().role)
    }

    // ---------- GroupCorpusDto -> GroupCorpus (all 8 fields) ----------

    @Test
    fun groupCorpusDto_toDomainModel_mapsAllFields_accumulatingRow() {
        val domain = corpusDto.toDomainModel()
        assertEquals(47500.0, domain.currentBalance)
        assertEquals(0.0, domain.openingBalance)
        assertEquals(52500.0, domain.totalContributionsThisCycle)
        assertEquals(5000.0, domain.totalLoansOutstanding)
        assertEquals("2026-07-20T10:00:00Z", domain.lastUpdated)
        assertNull(domain.rotationPosition)
        assertNull(domain.nextRecipientName)
        assertNull(domain.nextRecipientPosition)
    }

    @Test
    fun groupCorpusDto_toDomainModel_mapsAllFields_rotatingRow() {
        val rotatingDto = corpusDto.copy(rotationPosition = 7, nextRecipientName = "Amina Hassan", nextRecipientPosition = 4)
        val domain = rotatingDto.toDomainModel()
        assertEquals(7, domain.rotationPosition)
        assertEquals("Amina Hassan", domain.nextRecipientName)
        assertEquals(4, domain.nextRecipientPosition)
    }

    // ---------- ActivityItemDto -> ActivityItem (all 6 fields) ----------

    @Test
    fun activityItemDto_toDomainModel_mapsAllFields() {
        val domain = activityItemDto.toDomainModel()
        assertEquals("act-1", domain.id)
        assertEquals(ActivityType.DEPOSIT, domain.type)
        assertEquals("Weekly contribution", domain.description)
        assertEquals(200.0, domain.amount)
        assertEquals("2026-07-19", domain.date)
        assertEquals("Amina Hassan", domain.memberName)
    }

    @Test
    fun activityItemDto_toDomainModel_nullableFieldsMapToNull() {
        val dto = activityItemDto.copy(amount = null, memberName = null)
        val domain = dto.toDomainModel()
        assertNull(domain.amount)
        assertNull(domain.memberName)
    }

    @Test
    fun activityTypeDto_toDomainModel_mapsEveryEntry() {
        assertEquals(ActivityType.MEETING, ActivityTypeDto.MEETING.toDomainModel())
        assertEquals(ActivityType.DEPOSIT, ActivityTypeDto.DEPOSIT.toDomainModel())
        assertEquals(ActivityType.LOAN, ActivityTypeDto.LOAN.toDomainModel())
        assertEquals(ActivityType.PENALTY, ActivityTypeDto.PENALTY.toDomainModel())
        assertEquals(ActivityType.SHARE_OUT, ActivityTypeDto.SHARE_OUT.toDomainModel())
        assertEquals(ActivityType.UNKNOWN, ActivityTypeDto.UNKNOWN.toDomainModel())
    }

    // ---------- List<ActivityItemDto>.toDomainModels() batch converter ----------

    @Test
    fun activityItemDtoList_toDomainModels_mapsEveryItemInOrder() {
        val second = activityItemDto.copy(id = "act-2", type = ActivityTypeDto.MEETING, description = "Meeting")
        val result = listOf(activityItemDto, second).toDomainModels()

        assertEquals(2, result.size)
        assertEquals("act-1", result[0].id)
        assertEquals("act-2", result[1].id)
        assertEquals(ActivityType.MEETING, result[1].type)
    }

    @Test
    fun activityItemDtoList_toDomainModels_emptyListStaysEmpty() {
        assertEquals(emptyList(), emptyList<ActivityItemDto>().toDomainModels())
    }

    // ---------- GroupAccountsDto -> GroupAccounts (all 5 fields) ----------

    @Test
    fun groupAccountsDto_toDomainModel_mapsAllFields() {
        val domain = accountsDto.toDomainModel()
        assertEquals(52500.0, domain.savingsBalance)
        assertEquals(5000.0, domain.loansOutstanding)
        assertEquals(1, domain.activeLoanCount)
        assertEquals(2750.0, domain.shareOutProjection)
        assertEquals(1, domain.recentActivity.size)
        assertEquals("act-1", domain.recentActivity[0].id)
    }

    @Test
    fun groupAccountsDto_toDomainModel_rotatingType_nullShareOutProjectionAndEmptyActivity() {
        val rotatingDto = GroupAccountsDto(savingsBalance = 20000.0, loansOutstanding = 0.0, activeLoanCount = 0)
        val domain = rotatingDto.toDomainModel()
        assertNull(domain.shareOutProjection)
        assertEquals(emptyList(), domain.recentActivity)
    }

    // ---------- GroupConfigDto -> GroupConfig (all 9 fields) ----------

    @Test
    fun groupConfigDto_toDomainModel_mapsAllFields() {
        val domain = configDto.toDomainModel()
        assertEquals(200.0, domain.shareValue)
        assertEquals(1, domain.shareMin)
        assertEquals(5, domain.shareMax)
        assertEquals(200.0, domain.contributionAmount)
        assertEquals(3.0, domain.loanMultiplier)
        assertEquals(10.0, domain.interestRate)
        assertEquals(12, domain.cycleLengthMonths)
        assertEquals(50.0, domain.fineAmount)
        assertEquals(5000.0, domain.minimumDisbursementThreshold)
    }

    @Test
    fun groupConfigDto_toDomainModel_allNullableFieldsMapToNull() {
        val minimal = GroupConfigDto(cycleLengthMonths = 10)
        val domain = minimal.toDomainModel()
        assertNull(domain.shareValue)
        assertNull(domain.shareMin)
        assertNull(domain.shareMax)
        assertNull(domain.contributionAmount)
        assertNull(domain.loanMultiplier)
        assertNull(domain.interestRate)
        assertNull(domain.fineAmount)
        assertNull(domain.minimumDisbursementThreshold)
        assertEquals(10, domain.cycleLengthMonths)
    }

    // ---------- GroupInstanceConfigDto.toPartialGroupConfigDto() derivation helper ----------

    @Test
    fun groupInstanceConfigDto_toPartialGroupConfigDto_mapsDirectlyAvailableFieldsOnly() {
        val partial = vslaInstanceConfigDto.toPartialGroupConfigDto()

        assertEquals(200.0, partial.shareValue)
        assertEquals(200.0, partial.contributionAmount)
        assertEquals(50.0, partial.fineAmount)
        assertEquals(12, partial.cycleLengthMonths)
        // Not derivable from a single GroupInstanceConfigDto — needs the catalogue GroupTypeConfig
        // (defaultLoanMultiplier/defaultInterestRatePct) merged in by the repository.
        assertNull(partial.shareMin)
        assertNull(partial.shareMax)
        assertNull(partial.loanMultiplier)
        assertNull(partial.interestRate)
        assertNull(partial.minimumDisbursementThreshold)
    }
}
