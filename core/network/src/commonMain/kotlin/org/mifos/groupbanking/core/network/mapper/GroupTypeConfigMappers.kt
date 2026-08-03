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

import org.mifos.groupbanking.core.model.ContributionMode
import org.mifos.groupbanking.core.model.GroupTypeConfig
import org.mifos.groupbanking.core.model.GroupTypeSlug
import org.mifos.groupbanking.core.model.SavingsMechanism
import org.mifos.groupbanking.core.network.model.ContributionModeDto
import org.mifos.groupbanking.core.network.model.GroupTypeConfigDto
import org.mifos.groupbanking.core.network.model.GroupTypeSlugDto
import org.mifos.groupbanking.core.network.model.SavingsMechanismDto
import kotlin.jvm.JvmName

/**
 * DTO -> domain mappers for the group-type catalogue wire contract (COMP-DT-003). Every field
 * on `GroupTypeConfigDto` declared in `GroupTypeConfigDto.kt` is mapped — no field left
 * unmapped.
 */

fun GroupTypeConfigDto.toDomainModel(): GroupTypeConfig = GroupTypeConfig(
    typeSlug = typeSlug.toDomainModel(),
    displayName = displayName,
    tagline = tagline,
    savingsMechanism = savingsMechanism.toDomainModel(),
    contributionMode = contributionMode.toDomainModel(),
    lendingEnabled = lendingEnabled,
    hasSocialFund = hasSocialFund,
    hasBankLinkage = hasBankLinkage,
    welfareOnlyMode = welfareOnlyMode,
    formallyRegistered = formallyRegistered,
    defaultLoanMultiplier = defaultLoanMultiplier,
    defaultInterestRatePct = defaultInterestRatePct,
    defaultCycleLengthMonths = defaultCycleLengthMonths,
    maxMembers = maxMembers,
    minMembers = minMembers,
)

/** Batch converter — maps every seeded catalogue row in declaration order. */
@JvmName("groupTypeConfigDtoListToDomainModels")
fun List<GroupTypeConfigDto>.toDomainModels(): List<GroupTypeConfig> = map { it.toDomainModel() }

fun GroupTypeSlugDto.toDomainModel(): GroupTypeSlug = when (this) {
    GroupTypeSlugDto.VSLA -> GroupTypeSlug.VSLA
    GroupTypeSlugDto.ROSCA -> GroupTypeSlug.ROSCA
    GroupTypeSlugDto.ASCA -> GroupTypeSlug.ASCA
    GroupTypeSlugDto.SILC -> GroupTypeSlug.SILC
    GroupTypeSlugDto.SHG -> GroupTypeSlug.SHG
    GroupTypeSlugDto.SACCO -> GroupTypeSlug.SACCO
    GroupTypeSlugDto.CBO_VILLAGE_BANK -> GroupTypeSlug.CBO_VILLAGE_BANK
    GroupTypeSlugDto.BURIAL_WELFARE -> GroupTypeSlug.BURIAL_WELFARE
    GroupTypeSlugDto.JLG -> GroupTypeSlug.JLG
    GroupTypeSlugDto.UNKNOWN -> GroupTypeSlug.UNKNOWN
}

fun SavingsMechanismDto.toDomainModel(): SavingsMechanism = when (this) {
    SavingsMechanismDto.ACCUMULATING -> SavingsMechanism.ACCUMULATING
    SavingsMechanismDto.ROTATING_PAYOUT -> SavingsMechanism.ROTATING_PAYOUT
    SavingsMechanismDto.NONE -> SavingsMechanism.NONE
    SavingsMechanismDto.UNKNOWN -> SavingsMechanism.UNKNOWN
}

fun ContributionModeDto.toDomainModel(): ContributionMode = when (this) {
    ContributionModeDto.SHARE_BASED_VARIABLE -> ContributionMode.SHARE_BASED_VARIABLE
    ContributionModeDto.FIXED -> ContributionMode.FIXED
    ContributionModeDto.MINIMAL -> ContributionMode.MINIMAL
    ContributionModeDto.UNKNOWN -> ContributionMode.UNKNOWN
}
