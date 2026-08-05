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

import org.mifos.groupbanking.core.model.ActivityItem
import org.mifos.groupbanking.core.model.ActivityType
import org.mifos.groupbanking.core.model.GroupAccounts
import org.mifos.groupbanking.core.model.GroupConfig
import org.mifos.groupbanking.core.model.GroupContributionModel
import org.mifos.groupbanking.core.model.GroupCorpus
import org.mifos.groupbanking.core.model.GroupDashboard
import org.mifos.groupbanking.core.model.GroupDetail
import org.mifos.groupbanking.core.model.GroupInstanceConfig
import org.mifos.groupbanking.core.model.ViewerRoleInfo
import org.mifos.groupbanking.core.network.model.ActivityItemDto
import org.mifos.groupbanking.core.network.model.ActivityTypeDto
import org.mifos.groupbanking.core.network.model.GroupAccountsDto
import org.mifos.groupbanking.core.network.model.GroupConfigDto
import org.mifos.groupbanking.core.network.model.GroupContributionModelDto
import org.mifos.groupbanking.core.network.model.GroupCorpusDto
import org.mifos.groupbanking.core.network.model.GroupDashboardResponseDto
import org.mifos.groupbanking.core.network.model.GroupDetailDto
import org.mifos.groupbanking.core.network.model.GroupInstanceConfigDto
import org.mifos.groupbanking.core.network.model.ViewerRoleInfoDto
import kotlin.jvm.JvmName

/**
 * DTO -> domain mappers for the group-dashboard wire contract (COMP-GRP-001). Every field on
 * every DTO declared in `GroupDashboardDto.kt` is mapped — no field left unmapped.
 *
 * [GroupInstanceConfigDto.groupType]/[GroupInstanceConfigDto.poolModel] reuse the SHARED
 * `GroupTypeSlugDto.toDomainModel()` / `SavingsMechanismDto.toDomainModel()` mappers already
 * declared in `GroupTypeConfigMappers.kt` — not redefined here. [ViewerRoleInfoDto.role] reuses
 * the SHARED `ViewerRoleDto.toDomainModel()` mapper already declared in `GroupMappers.kt`.
 */

/**
 * Composite mapper — assembles [GroupDashboard] from the 4-way COMP-GRP-001 parallel-fetch
 * envelope. Deliberately does NOT populate [GroupConfig] (see `GroupDashboard.kt` kdoc: that
 * derivation additionally needs the separately-fetched catalogue `GroupTypeConfig`, a
 * repository-layer concern out of scope here).
 */
fun GroupDashboardResponseDto.toDomainModel(): GroupDashboard = GroupDashboard(
    group = group.toDomainModel(),
    viewerRole = viewerRole.toDomainModel(),
    corpus = corpus.toDomainModel(),
    accounts = accounts.toDomainModel(),
)

fun GroupDetailDto.toDomainModel(): GroupDetail = GroupDetail(
    id = id,
    fineractGroupId = fineractGroupId,
    name = name,
    cycleNumber = cycleNumber,
    cycleLengthMonths = cycleLengthMonths,
    meetingFrequency = meetingFrequency,
    memberCount = memberCount,
    overdueLoansCount = overdueLoansCount,
    status = status,
    typeConfig = typeConfig.toDomainModel(),
)

fun GroupInstanceConfigDto.toDomainModel(): GroupInstanceConfig = GroupInstanceConfig(
    groupType = groupType.toDomainModel(),
    poolModel = poolModel.toDomainModel(),
    contributionModel = contributionModel.toDomainModel(),
    shareoutFormula = shareoutFormula,
    payoutOrderMethod = payoutOrderMethod,
    shareValue = shareValue,
    contributionAmount = contributionAmount,
    socialFundEnabled = socialFundEnabled,
    cycleLengthMonths = cycleLengthMonths,
    loanMultiplier = loanMultiplier,
    interestRate = interestRate,
    fineAmount = fineAmount,
)

fun GroupContributionModelDto.toDomainModel(): GroupContributionModel = when (this) {
    GroupContributionModelDto.FIXED_AMOUNT -> GroupContributionModel.FIXED_AMOUNT
    GroupContributionModelDto.SHARE_BASED_VARIABLE -> GroupContributionModel.SHARE_BASED_VARIABLE
    GroupContributionModelDto.FIXED_NEGOTIATED -> GroupContributionModel.FIXED_NEGOTIATED
    GroupContributionModelDto.UNKNOWN -> GroupContributionModel.UNKNOWN
}

fun ViewerRoleInfoDto.toDomainModel(): ViewerRoleInfo = ViewerRoleInfo(
    role = role.toDomainModel(),
    memberId = memberId,
)

fun GroupCorpusDto.toDomainModel(): GroupCorpus = GroupCorpus(
    currentBalance = currentBalance,
    openingBalance = openingBalance,
    totalContributionsThisCycle = totalContributionsThisCycle,
    totalLoansOutstanding = totalLoansOutstanding,
    lastUpdated = lastUpdated,
    isCycleEnd = isCycleEnd,
    rotationPosition = rotationPosition,
    nextRecipientName = nextRecipientName,
    nextRecipientPosition = nextRecipientPosition,
)

fun ActivityItemDto.toDomainModel(): ActivityItem = ActivityItem(
    id = id,
    type = type.toDomainModel(),
    description = description,
    amount = amount,
    date = date,
    memberName = memberName,
)

/** Batch converter — maps every recent-activity row in declaration order. */
@JvmName("activityItemDtoListToDomainModels")
fun List<ActivityItemDto>.toDomainModels(): List<ActivityItem> = map { it.toDomainModel() }

fun ActivityTypeDto.toDomainModel(): ActivityType = when (this) {
    ActivityTypeDto.MEETING -> ActivityType.MEETING
    ActivityTypeDto.DEPOSIT -> ActivityType.DEPOSIT
    ActivityTypeDto.LOAN -> ActivityType.LOAN
    ActivityTypeDto.PENALTY -> ActivityType.PENALTY
    ActivityTypeDto.SHARE_OUT -> ActivityType.SHARE_OUT
    ActivityTypeDto.UNKNOWN -> ActivityType.UNKNOWN
}

fun GroupAccountsDto.toDomainModel(): GroupAccounts = GroupAccounts(
    savingsBalance = savingsBalance,
    loansOutstanding = loansOutstanding,
    activeLoanCount = activeLoanCount,
    shareOutProjection = shareOutProjection,
    recentActivity = recentActivity.toDomainModels(),
)

fun GroupConfigDto.toDomainModel(): GroupConfig = GroupConfig(
    shareValue = shareValue,
    shareMin = shareMin,
    shareMax = shareMax,
    contributionAmount = contributionAmount,
    loanMultiplier = loanMultiplier,
    interestRate = interestRate,
    cycleLengthMonths = cycleLengthMonths,
    fineAmount = fineAmount,
    minimumDisbursementThreshold = minimumDisbursementThreshold,
)

/**
 * PARTIAL single-source derivation helper — covers only the [GroupConfigDto] fields directly
 * available on [GroupInstanceConfigDto] ([GroupConfigDto.shareValue],
 * [GroupConfigDto.contributionAmount], [GroupConfigDto.fineAmount],
 * [GroupConfigDto.cycleLengthMonths]). [GroupConfigDto.loanMultiplier] /
 * [GroupConfigDto.interestRate] additionally require the separately-fetched catalogue
 * `GroupTypeConfigDto.defaultLoanMultiplier` / `defaultInterestRatePct` (COMP-DT-003) — NOT
 * available on this DTO, so they map to `null` here; [GroupConfigDto.shareMin] /
 * [GroupConfigDto.shareMax] / [GroupConfigDto.minimumDisbursementThreshold] have no wire source
 * anywhere in `api.yaml` (confirmed gap) and always map to `null`. The full merge (this DTO +
 * the catalogue row) is a repository-layer concern — see `GroupConfig` kdoc.
 */
fun GroupInstanceConfigDto.toPartialGroupConfigDto(): GroupConfigDto = GroupConfigDto(
    shareValue = shareValue,
    shareMin = null,
    shareMax = null,
    contributionAmount = contributionAmount,
    loanMultiplier = null,
    interestRate = null,
    cycleLengthMonths = cycleLengthMonths,
    fineAmount = fineAmount,
    minimumDisbursementThreshold = null,
)
