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

import kotlinx.datetime.LocalDate
import org.mifos.groupbanking.core.model.GroupSavingsSummary
import org.mifos.groupbanking.core.model.IndividualSavingsSummary
import org.mifos.groupbanking.core.model.MemberGroupSavingsRow
import org.mifos.groupbanking.core.model.MemberIndividualSavingsRow
import org.mifos.groupbanking.core.model.MemberSavingsDetail
import org.mifos.groupbanking.core.model.SavingsDataPoint
import org.mifos.groupbanking.core.model.SavingsLedgerEntry
import org.mifos.groupbanking.core.model.SavingsLedgerTransactionType
import org.mifos.groupbanking.core.model.SavingsMember
import org.mifos.groupbanking.core.model.SavingsStatementEntry
import org.mifos.groupbanking.core.model.SavingsTransactionType
import org.mifos.groupbanking.core.model.WeeklyContributionPoint
import org.mifos.groupbanking.core.network.model.GroupSavingsSummaryDto
import org.mifos.groupbanking.core.network.model.IndividualSavingsSummaryDto
import org.mifos.groupbanking.core.network.model.MemberGroupSavingsRowDto
import org.mifos.groupbanking.core.network.model.MemberIndividualSavingsRowDto
import org.mifos.groupbanking.core.network.model.MemberSavingsDetailDto
import org.mifos.groupbanking.core.network.model.SavingsDataPointDto
import org.mifos.groupbanking.core.network.model.SavingsLedgerEntryDto
import org.mifos.groupbanking.core.network.model.SavingsLedgerTransactionTypeDto
import org.mifos.groupbanking.core.network.model.SavingsMemberDto
import org.mifos.groupbanking.core.network.model.SavingsStatementEntryDto
import org.mifos.groupbanking.core.network.model.SavingsStatementTypeDto
import org.mifos.groupbanking.core.network.model.WeeklyContributionPointDto
import kotlin.jvm.JvmName

/**
 * DTO -> domain mappers for the shared Savings domain layer (`SavingsDto.kt`), built once for the
 * 3 consumers that share Fineract savings shapes: personal-savings (raw self-service ledger),
 * member-savings-detail (companion per-member statement + sparkline), and savings-dashboard
 * (companion group/individual contribution summaries). Every field on every DTO declared in
 * `SavingsDto.kt` is mapped — no field left unmapped.
 */

// ==================== wire-date helper (personal-savings raw ledger) ====================

/**
 * Parses a raw Fineract `List<Int>` date-component array (`[year, month, day]`, e.g.
 * `[2026, 7, 21]`) into a [LocalDate]. No existing helper in this codebase covers this shape
 * (`fineractTransactionDate` in `RecordRepaymentMappers.kt` goes the OPPOSITE direction — formats
 * a `kotlin.time.Instant` INTO a Fineract `dd MMMM yyyy` string, it does not parse a component
 * array) — this is a NEW, reusable helper for any future feature consuming a raw Fineract
 * self-service endpoint that returns dates this way.
 */
fun fineractDateComponents(components: List<Int>): LocalDate {
    require(components.size >= 3) { "Fineract date-component array must have at least [year, month, day], got: $components" }
    return LocalDate(components[0], components[1], components[2])
}

// ==================== personal-savings — raw Fineract self-service ledger ====================

fun SavingsLedgerEntryDto.toDomainModel(): SavingsLedgerEntry = SavingsLedgerEntry(
    id = id,
    type = transactionType.toDomainModel(),
    date = fineractDateComponents(date),
    amount = amount,
    runningBalance = runningBalance,
    currencyCode = currency.code,
    currencyDisplaySymbol = currency.displaySymbol,
)

/** Batch converter — maps every raw ledger row in declaration order. */
@JvmName("savingsLedgerEntryDtoListToDomainModels")
fun List<SavingsLedgerEntryDto>.toDomainModels(): List<SavingsLedgerEntry> = map { it.toDomainModel() }

fun SavingsLedgerTransactionTypeDto.toDomainModel(): SavingsLedgerTransactionType = SavingsLedgerTransactionType(
    value = value,
    code = code,
    description = description,
)

// ==================== member-savings-detail — companion per-member statement ====================

fun SavingsMemberDto.toDomainModel(): SavingsMember = SavingsMember(
    memberId = memberId,
    displayName = displayName,
    photoUri = photoUri,
)

/**
 * Maps to the EXISTING [SavingsDataPoint] domain model (`core/model/MemberProfile.kt`) — REUSED
 * outright, not redeclared. See `SavingsDataPointDto` kdoc.
 */
fun SavingsDataPointDto.toDomainModel(): SavingsDataPoint = SavingsDataPoint(
    date = date,
    balance = balance,
)

/** Batch converter — maps every sparkline point in declaration order. */
@JvmName("savingsDataPointDtoListToDomainModels")
fun List<SavingsDataPointDto>.toDomainModels(): List<SavingsDataPoint> = map { it.toDomainModel() }

fun SavingsStatementTypeDto.toDomainModel(): SavingsTransactionType = when (this) {
    SavingsStatementTypeDto.DEPOSIT -> SavingsTransactionType.DEPOSIT
    SavingsStatementTypeDto.WITHDRAWAL -> SavingsTransactionType.WITHDRAWAL
    SavingsStatementTypeDto.INTEREST_POSTING -> SavingsTransactionType.INTEREST_POSTING
    SavingsStatementTypeDto.FEE_DEDUCTION -> SavingsTransactionType.FEE_DEDUCTION
    SavingsStatementTypeDto.TRANSFER -> SavingsTransactionType.TRANSFER
    SavingsStatementTypeDto.UNKNOWN -> SavingsTransactionType.UNKNOWN
}

fun SavingsStatementEntryDto.toDomainModel(): SavingsStatementEntry = SavingsStatementEntry(
    id = id,
    date = LocalDate.parse(date),
    type = type.toDomainModel(),
    amount = amount,
    runningBalance = runningBalance,
    reversed = reversed,
)

/** Batch converter — maps every statement row in declaration order. */
@JvmName("savingsStatementEntryDtoListToDomainModels")
fun List<SavingsStatementEntryDto>.toDomainModels(): List<SavingsStatementEntry> = map { it.toDomainModel() }

fun MemberSavingsDetailDto.toDomainModel(): MemberSavingsDetail = MemberSavingsDetail(
    member = member.toDomainModel(),
    savingsAccountNo = savingsAccountNo,
    savingsBalance = savingsBalance,
    sharesHeld = sharesHeld,
    shareValue = shareValue,
    sparklineData = sparklineData.toDomainModels(),
    transactions = transactions.toDomainModels(),
    totalTransactions = totalTransactions,
    hasNextPage = hasNextPage,
)

// ==================== savings-dashboard — companion group/individual summaries ====================

fun WeeklyContributionPointDto.toDomainModel(): WeeklyContributionPoint = WeeklyContributionPoint(
    weekLabel = weekLabel,
    groupAmount = groupAmount,
    individualAmount = individualAmount,
)

/** Batch converter — maps every weekly trend point in declaration order. */
@JvmName("weeklyContributionPointDtoListToDomainModels")
fun List<WeeklyContributionPointDto>.toDomainModels(): List<WeeklyContributionPoint> = map { it.toDomainModel() }

fun MemberGroupSavingsRowDto.toDomainModel(): MemberGroupSavingsRow = MemberGroupSavingsRow(
    memberId = memberId,
    name = name,
    totalContributed = totalContributed,
    lastContribution = lastContribution,
    meetingsContributed = meetingsContributed,
    sharesHeld = sharesHeld,
    shareValue = shareValue,
)

/** Batch converter — maps every group-savings member row in declaration order. */
@JvmName("memberGroupSavingsRowDtoListToDomainModels")
fun List<MemberGroupSavingsRowDto>.toDomainModels(): List<MemberGroupSavingsRow> = map { it.toDomainModel() }

fun GroupSavingsSummaryDto.toDomainModel(): GroupSavingsSummary = GroupSavingsSummary(
    cycleTarget = cycleTarget,
    cycleCollected = cycleCollected,
    totalCollected = totalCollected,
    weeklyTrend = weeklyTrend.toDomainModels(),
    memberRows = memberRows.toDomainModels(),
)

fun MemberIndividualSavingsRowDto.toDomainModel(): MemberIndividualSavingsRow = MemberIndividualSavingsRow(
    memberId = memberId,
    name = name,
    currentBalance = currentBalance,
    lastTransaction = lastTransaction,
    lastTransactionDate = lastTransactionDate?.let { LocalDate.parse(it) },
)

/** Batch converter — maps every individual-savings member row in declaration order. */
@JvmName("memberIndividualSavingsRowDtoListToDomainModels")
fun List<MemberIndividualSavingsRowDto>.toDomainModels(): List<MemberIndividualSavingsRow> = map { it.toDomainModel() }

fun IndividualSavingsSummaryDto.toDomainModel(): IndividualSavingsSummary = IndividualSavingsSummary(
    totalBalance = totalBalance,
    weeklyTrend = weeklyTrend.toDomainModels(),
    memberRows = memberRows.toDomainModels(),
)
