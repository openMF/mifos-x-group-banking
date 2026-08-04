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

import kotlinx.serialization.json.Json
import org.mifos.groupbanking.core.model.ContributionModel
import org.mifos.groupbanking.core.model.CreateGroupRequest
import org.mifos.groupbanking.core.model.CreateGroupTypeConfig
import org.mifos.groupbanking.core.model.GroupCreationResult
import org.mifos.groupbanking.core.model.GroupTypeSlug
import org.mifos.groupbanking.core.model.Office
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
import kotlin.jvm.JvmName

/**
 * DTO <-> domain mappers for the group-create wizard wire contract (COMP-GRP-001, `GET
 * /offices`). Every field on every DTO declared in `GroupCreateDto.kt` is mapped — no field left
 * unmapped. [CreateGroupRequestDto.typeConfig.groupType] and `.poolModel` reuse the EXISTING
 * `GroupTypeDto.toDomainModel()` (`GroupMappers.kt`) and `SavingsMechanismDto.toDomainModel()`
 * (`GroupTypeConfigMappers.kt`) extension functions already defined in this same mapper package
 * — no duplicate mapper is introduced for either. The REVERSE direction
 * (`GroupTypeSlug.toDto(): GroupTypeDto` / `SavingsMechanism.toDto(): SavingsMechanismDto`) did
 * not exist anywhere in the module before this feature (both prior mapper files were read-only,
 * DTO -> domain only), so it is added HERE — still against the shared enums, not a duplicate
 * enum, just the missing reverse arm needed because `typeConfig` is submitted (domain -> wire) on
 * this screen.
 */

// ---------- response/read DTOs -> domain ----------

fun CreateGroupTypeConfigDto.toDomainModel(): CreateGroupTypeConfig = CreateGroupTypeConfig(
    groupType = groupType.toDomainModel(),
    poolModel = poolModel.toDomainModel(),
    contributionModel = contributionModel.toDomainModel(),
    shareoutFormula = shareoutFormula.toDomainModel(),
    payoutOrderMethod = payoutOrderMethod.toDomainModel(),
    shareValue = shareValue,
    contributionAmount = contributionAmount,
    socialFundEnabled = socialFundEnabled,
    socialFundPercent = socialFundPercent,
    cycleLengthMonths = cycleLengthMonths,
    loanMultiplier = loanMultiplier,
    interestRate = interestRate,
    fineAmount = fineAmount,
    maxMembers = maxMembers,
)

fun CreateGroupRequestDto.toDomainModel(): CreateGroupRequest = CreateGroupRequest(
    name = name,
    officeId = officeId,
    userId = userId,
    currency = currency,
    meetingDay = meetingDay,
    meetingTime = meetingTime,
    typeConfig = typeConfig.toDomainModel(),
)

fun CreateGroupResponseDto.toDomainModel(): GroupCreationResult = GroupCreationResult(
    groupId = groupId,
    fineractCenterId = fineractCenterId,
    inviteCode = inviteCode,
)

fun OfficeDto.toDomainModel(): Office = Office(
    id = id,
    name = name,
    nameDecorated = nameDecorated,
    externalId = externalId,
)

/** Batch converter — maps every office row in declaration order. */
@JvmName("officeDtoListToDomainModels")
fun List<OfficeDto>.toDomainModels(): List<Office> = map { it.toDomainModel() }

fun ContributionModelDto.toDomainModel(): ContributionModel = when (this) {
    ContributionModelDto.FIXED_AMOUNT -> ContributionModel.FIXED_AMOUNT
    ContributionModelDto.SHARE_BASED_VARIABLE -> ContributionModel.SHARE_BASED_VARIABLE
    ContributionModelDto.FIXED_NEGOTIATED -> ContributionModel.FIXED_NEGOTIATED
    ContributionModelDto.UNKNOWN -> ContributionModel.UNKNOWN
}

fun ShareoutFormulaDto.toDomainModel(): ShareoutFormula = when (this) {
    ShareoutFormulaDto.NONE -> ShareoutFormula.NONE
    ShareoutFormulaDto.PRORATA_SHARES -> ShareoutFormula.PRORATA_SHARES
    ShareoutFormulaDto.PRORATA_SAVINGS -> ShareoutFormula.PRORATA_SAVINGS
    ShareoutFormulaDto.EQUAL -> ShareoutFormula.EQUAL
    ShareoutFormulaDto.INVESTMENT_PROPORTIONAL -> ShareoutFormula.INVESTMENT_PROPORTIONAL
    ShareoutFormulaDto.UNKNOWN -> ShareoutFormula.UNKNOWN
}

fun PayoutOrderMethodDto.toDomainModel(): PayoutOrderMethod = when (this) {
    PayoutOrderMethodDto.FIXED_ORDER -> PayoutOrderMethod.FIXED_ORDER
    PayoutOrderMethodDto.LOTTERY -> PayoutOrderMethod.LOTTERY
    PayoutOrderMethodDto.AUCTION -> PayoutOrderMethod.AUCTION
    PayoutOrderMethodDto.NEED_BASED -> PayoutOrderMethod.NEED_BASED
    PayoutOrderMethodDto.NA -> PayoutOrderMethod.NA
    PayoutOrderMethodDto.UNKNOWN -> PayoutOrderMethod.UNKNOWN
}

// ---------- domain -> request DTOs ----------

/**
 * Reverse mapper for the shared [GroupTypeSlug] domain enum, onto the short-form [GroupTypeDto]
 * wire enum group-create's `typeConfig.group_type` uses. `CBO_VILLAGE_BANK`/`BURIAL_WELFARE`
 * (long-form domain names) map back to the short `CBO`/`BURIAL` wire values — the inverse of
 * `GroupTypeDto.toDomainModel()` in `GroupMappers.kt`.
 */
fun GroupTypeSlug.toDto(): GroupTypeDto = when (this) {
    GroupTypeSlug.VSLA -> GroupTypeDto.VSLA
    GroupTypeSlug.ROSCA -> GroupTypeDto.ROSCA
    GroupTypeSlug.ASCA -> GroupTypeDto.ASCA
    GroupTypeSlug.SILC -> GroupTypeDto.SILC
    GroupTypeSlug.SHG -> GroupTypeDto.SHG
    GroupTypeSlug.SACCO -> GroupTypeDto.SACCO
    GroupTypeSlug.CBO_VILLAGE_BANK -> GroupTypeDto.CBO
    GroupTypeSlug.BURIAL_WELFARE -> GroupTypeDto.BURIAL
    GroupTypeSlug.JLG -> GroupTypeDto.JLG
    GroupTypeSlug.UNKNOWN -> GroupTypeDto.UNKNOWN
}

/**
 * Reverse mapper for the shared [SavingsMechanism] domain enum, onto [SavingsMechanismDto] — the
 * inverse of `SavingsMechanismDto.toDomainModel()` in `GroupTypeConfigMappers.kt`.
 */
fun SavingsMechanism.toDto(): SavingsMechanismDto = when (this) {
    SavingsMechanism.ACCUMULATING -> SavingsMechanismDto.ACCUMULATING
    SavingsMechanism.ROTATING_PAYOUT -> SavingsMechanismDto.ROTATING_PAYOUT
    SavingsMechanism.NONE -> SavingsMechanismDto.NONE
    SavingsMechanism.UNKNOWN -> SavingsMechanismDto.UNKNOWN
}

fun CreateGroupTypeConfig.toDto(): CreateGroupTypeConfigDto = CreateGroupTypeConfigDto(
    groupType = groupType.toDto(),
    poolModel = poolModel.toDto(),
    contributionModel = contributionModel.toDto(),
    shareoutFormula = shareoutFormula.toDto(),
    payoutOrderMethod = payoutOrderMethod.toDto(),
    shareValue = shareValue,
    contributionAmount = contributionAmount,
    socialFundEnabled = socialFundEnabled,
    socialFundPercent = socialFundPercent,
    cycleLengthMonths = cycleLengthMonths,
    loanMultiplier = loanMultiplier,
    interestRate = interestRate,
    fineAmount = fineAmount,
    maxMembers = maxMembers,
)

fun CreateGroupRequest.toDto(): CreateGroupRequestDto = CreateGroupRequestDto(
    name = name,
    officeId = officeId,
    userId = userId,
    currency = currency,
    meetingDay = meetingDay,
    meetingTime = meetingTime,
    typeConfig = typeConfig.toDto(),
)

fun ContributionModel.toDto(): ContributionModelDto = when (this) {
    ContributionModel.FIXED_AMOUNT -> ContributionModelDto.FIXED_AMOUNT
    ContributionModel.SHARE_BASED_VARIABLE -> ContributionModelDto.SHARE_BASED_VARIABLE
    ContributionModel.FIXED_NEGOTIATED -> ContributionModelDto.FIXED_NEGOTIATED
    ContributionModel.UNKNOWN -> ContributionModelDto.UNKNOWN
}

fun ShareoutFormula.toDto(): ShareoutFormulaDto = when (this) {
    ShareoutFormula.NONE -> ShareoutFormulaDto.NONE
    ShareoutFormula.PRORATA_SHARES -> ShareoutFormulaDto.PRORATA_SHARES
    ShareoutFormula.PRORATA_SAVINGS -> ShareoutFormulaDto.PRORATA_SAVINGS
    ShareoutFormula.EQUAL -> ShareoutFormulaDto.EQUAL
    ShareoutFormula.INVESTMENT_PROPORTIONAL -> ShareoutFormulaDto.INVESTMENT_PROPORTIONAL
    ShareoutFormula.UNKNOWN -> ShareoutFormulaDto.UNKNOWN
}

fun PayoutOrderMethod.toDto(): PayoutOrderMethodDto = when (this) {
    PayoutOrderMethod.FIXED_ORDER -> PayoutOrderMethodDto.FIXED_ORDER
    PayoutOrderMethod.LOTTERY -> PayoutOrderMethodDto.LOTTERY
    PayoutOrderMethod.AUCTION -> PayoutOrderMethodDto.AUCTION
    PayoutOrderMethod.NEED_BASED -> PayoutOrderMethodDto.NEED_BASED
    PayoutOrderMethod.NA -> PayoutOrderMethodDto.NA
    PayoutOrderMethod.UNKNOWN -> PayoutOrderMethodDto.UNKNOWN
}

// ---------- offline SyncQueue serialization ----------

/**
 * Server-parity Json config (mirrors `NetworkModule`'s client config — `ignoreUnknownKeys` +
 * `coerceInputValues`) reused for the group-create SyncQueue payload round-trip. Kept private to
 * this file — the offline-queue payload is always this project's own wire shape (same precedent as
 * `LoanRequestMappers.syncQueueJson`).
 */
private val groupCreateSyncQueueJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

/**
 * Serializes this [CreateGroupRequestDto] to the exact JSON body the ONLINE `GroupCreateApi.createGroup`
 * POSTs to `/companion/groups`, for `SyncQueueRepository.enqueue(targetTable = "/companion/groups")`
 * to persist when `cmp-network-monitor` reports offline. The queued row replays through the
 * companion's `/batches` self-dispatch back to `HandleCreateGroup` — so an offline group-create is
 * durably queued and drained, not silently dropped.
 */
fun CreateGroupRequestDto.toJsonPayload(): String =
    groupCreateSyncQueueJson.encodeToString(CreateGroupRequestDto.serializer(), this)
