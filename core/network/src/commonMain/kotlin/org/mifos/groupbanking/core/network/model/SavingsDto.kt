/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ==================== personal-savings — raw Fineract self-service ledger ====================

/**
 * Wire DTO for one row of personal-savings' raw Fineract self-service transaction ledger
 * (`GET /self/savingsaccounts/{savingsId}/transactions`, group-linked AND individual accounts
 * share this shape — `idea-layer/screens/personal-savings/api.yaml#dtos.SavingsTransactionDto`).
 *
 * **Naming-collision note (flagged for the cross-feature repair station):** the bare name
 * `SavingsTransactionDto` is already TAKEN by the EXISTING compact companion shape
 * (`SavingsTransactionDto.kt`, personal-dashboard's `recentTransactions` row — `id: String`,
 * `date: String`, `type: TransactionTypeDto` DEPOSIT/WITHDRAWAL/UNKNOWN, `amount: Double`) whose
 * own kdoc already documents a three-way collision against this exact raw-Fineract shape plus the
 * `idea-layer/dtos/SavingsTransactionDto.yaml` registry entry. Named `SavingsLedgerEntryDto` here
 * instead of forcing the clash — this is the FOURTH declared shape under the conceptual "savings
 * transaction" name (compact companion / registry / this raw-Fineract shape / and
 * member-savings-detail's own richer companion statement row, [SavingsStatementEntryDto] below).
 * Resolve all four at Station 3.
 *
 * [transactionType] and [date] are kept as the LITERAL raw Fineract shapes (a `{value, code,
 * description}` object and a `List<Int>` `[year, month, day]` date-component array respectively)
 * per Hard Rule 4 — `api.yaml` declares no value-set for `transactionType.code`, so it is NOT
 * collapsed into a DEPOSIT/WITHDRAWAL enum here (that would require inventing an undeclared
 * mapping); same "literal operation response, no invented value-set" precedent as
 * `RepaymentTransactionDto.type` (`LoanDetailDto.kt`).
 *
 * See API.md#dtos — SavingsLedgerEntry.
 */
@Serializable
data class SavingsLedgerEntryDto(
    @SerialName("id") val id: Long,
    @SerialName("transactionType") val transactionType: SavingsLedgerTransactionTypeDto,
    @SerialName("date") val date: List<Int>,
    @SerialName("amount") val amount: Double,
    @SerialName("runningBalance") val runningBalance: Double,
    @SerialName("currency") val currency: SavingsLedgerCurrencyDto,
) {
    companion object {
        /** Bumped when this DTO shape changes (registry `version:` driven). See EC30. */
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire DTO for [SavingsLedgerEntryDto.transactionType] — the raw Fineract `{value, code,
 * description}` triple. Kept a raw object (not an enum) — see [SavingsLedgerEntryDto] kdoc.
 *
 * See API.md#dtos — SavingsLedgerTransactionType.
 */
@Serializable
data class SavingsLedgerTransactionTypeDto(
    @SerialName("value") val value: Int,
    @SerialName("code") val code: String,
    @SerialName("description") val description: String,
)

/**
 * Wire DTO for [SavingsLedgerEntryDto.currency] — the raw Fineract currency pair.
 *
 * See API.md#dtos — SavingsLedgerCurrency.
 */
@Serializable
data class SavingsLedgerCurrencyDto(
    @SerialName("code") val code: String,
    @SerialName("displaySymbol") val displaySymbol: String,
)

// ==================== member-savings-detail — companion per-member statement ====================

/**
 * Wire DTO for the member-savings-detail companion member identity subset (`member` field of the
 * single-read `GET /companion/groups/{groupId}/members/{memberId}/savings`).
 *
 * See API.md#dtos — SavingsMember.
 */
@Serializable
data class SavingsMemberDto(
    @SerialName("memberId") val memberId: String,
    @SerialName("displayName") val displayName: String,
    @SerialName("photoUri") val photoUri: String? = null,
)

/**
 * Wire DTO for one weekly balance sparkline point on `MemberSavingsDetailDto.sparklineData`. Same
 * `{date, balance}` shape as the EXISTING `core/model/SavingsDataPoint` domain model
 * (`MemberProfile.kt`) — that domain model is REUSED outright by
 * `SavingsMappers.kt#toDomainModel()` below (no new domain type introduced), even though it was
 * originally declared as a "no wire source, confirmed gap" model for member-profile — this feature
 * finally gives it a real wire source.
 *
 * See API.md#dtos — SavingsDataPoint.
 */
@Serializable
data class SavingsDataPointDto(
    @SerialName("date") val date: String,
    @SerialName("balance") val balance: Double,
)

/**
 * Wire enum for [SavingsStatementEntryDto.type]
 * (`idea-layer/screens/member-savings-detail/api.yaml#dtos.TransactionType` — 5 known values).
 * `UNKNOWN` fallback per T7/EC30 so a server-added value never crashes an old client. Wider
 * value-set than the compact companion `TransactionTypeDto` (`SavingsTransactionDto.kt`,
 * DEPOSIT/WITHDRAWAL/UNKNOWN only) — NOT reused/widened, same "don't force reuse across
 * incompatible value-sets" precedent as `LoanAccountStatusDto` vs `LoanStatusDto`; flagged for
 * Station 3 alongside the [SavingsLedgerEntryDto] naming-collision note.
 */
@Serializable(with = SavingsStatementTypeDto.Serializer::class)
enum class SavingsStatementTypeDto {
    @SerialName("DEPOSIT") DEPOSIT,
    @SerialName("WITHDRAWAL") WITHDRAWAL,
    @SerialName("INTEREST_POSTING") INTEREST_POSTING,
    @SerialName("FEE_DEDUCTION") FEE_DEDUCTION,
    @SerialName("TRANSFER") TRANSFER,
    @SerialName("UNKNOWN") UNKNOWN,
    ;

    internal object Serializer : KSerializer<SavingsStatementTypeDto> by unknownFallbackEnumSerializer(
        "SavingsStatementTypeDto", entries, UNKNOWN,
    )
}

/**
 * Wire DTO for one row of member-savings-detail's paginated statement
 * (`MemberSavingsDetailDto.transactions`). Named `SavingsStatementEntryDto` (NOT
 * `SavingsTransactionDto` — TAKEN, see [SavingsLedgerEntryDto] kdoc). Companion-normalized
 * (`String` id/date, no raw Fineract component-array/currency shapes) — adds [reversed] over the
 * compact `SavingsTransactionDto`.
 *
 * See API.md#dtos — SavingsStatementEntry.
 */
@Serializable
data class SavingsStatementEntryDto(
    @SerialName("id") val id: String,
    @SerialName("date") val date: String,
    @SerialName("type") val type: SavingsStatementTypeDto = SavingsStatementTypeDto.UNKNOWN,
    @SerialName("amount") val amount: Double,
    @SerialName("runningBalance") val runningBalance: Double,
    @SerialName("reversed") val reversed: Boolean,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire DTO for the single-read `GET /companion/groups/{groupId}/members/{memberId}/savings`
 * response — member identity + contribution-model-aware balance fields + sparkline + paginated
 * statement. [sharesHeld]/[shareValue] are populated only for SHARE_BASED_VARIABLE (VSLA/SILC)
 * groups; `null` for FIXED_AMOUNT (ROSCA/SHG) groups, where [savingsBalance] alone is meaningful.
 *
 * See API.md#dtos — MemberSavingsDetail.
 */
@Serializable
data class MemberSavingsDetailDto(
    @SerialName("member") val member: SavingsMemberDto,
    @SerialName("savingsAccountNo") val savingsAccountNo: String,
    @SerialName("savingsBalance") val savingsBalance: Double,
    @SerialName("sharesHeld") val sharesHeld: Int? = null,
    @SerialName("shareValue") val shareValue: Long? = null,
    @SerialName("sparklineData") val sparklineData: List<SavingsDataPointDto> = emptyList(),
    @SerialName("transactions") val transactions: List<SavingsStatementEntryDto> = emptyList(),
    @SerialName("totalTransactions") val totalTransactions: Int,
    @SerialName("hasNextPage") val hasNextPage: Boolean,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

// ==================== savings-dashboard — companion group/individual summaries ====================

/**
 * Wire DTO for one weekly point on savings-dashboard's group/individual contribution trend chart
 * (`api.yaml#dtos.WeeklyContributionPoint`) — shared by both [GroupSavingsSummaryDto] and
 * [IndividualSavingsSummaryDto].
 *
 * See API.md#dtos — WeeklyContributionPoint.
 */
@Serializable
data class WeeklyContributionPointDto(
    @SerialName("weekLabel") val weekLabel: String,
    @SerialName("groupAmount") val groupAmount: Long,
    @SerialName("individualAmount") val individualAmount: Long,
)

/**
 * Wire DTO for one member row on the group savings-dashboard summary
 * (`GET /companion/groups/{groupId}/savings`). [sharesHeld]/[shareValue] populated only for
 * SHARE_BASED_VARIABLE (VSLA/SILC) groups; `null` for FIXED_AMOUNT (ROSCA/SHG) groups, same
 * mutually-exclusive nullable-field-group pattern as [MemberSavingsDetailDto].
 *
 * See API.md#dtos — MemberGroupSavingsRow.
 */
@Serializable
data class MemberGroupSavingsRowDto(
    @SerialName("memberId") val memberId: String,
    @SerialName("name") val name: String,
    @SerialName("totalContributed") val totalContributed: Long,
    @SerialName("lastContribution") val lastContribution: Long,
    @SerialName("meetingsContributed") val meetingsContributed: Int,
    @SerialName("sharesHeld") val sharesHeld: Int? = null,
    @SerialName("shareValue") val shareValue: Long? = null,
)

/**
 * Wire DTO for savings-dashboard's group-tab summary (`GET /companion/groups/{groupId}/savings`).
 *
 * See API.md#dtos — GroupSavingsSummary.
 */
@Serializable
data class GroupSavingsSummaryDto(
    @SerialName("cycleTarget") val cycleTarget: Long,
    @SerialName("cycleCollected") val cycleCollected: Long,
    @SerialName("totalCollected") val totalCollected: Long,
    @SerialName("weeklyTrend") val weeklyTrend: List<WeeklyContributionPointDto> = emptyList(),
    @SerialName("memberRows") val memberRows: List<MemberGroupSavingsRowDto> = emptyList(),
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire DTO for one member row on savings-dashboard's individual-tab summary
 * (`GET /companion/groups/{groupId}/savings/individual`).
 *
 * See API.md#dtos — MemberIndividualSavingsRow.
 */
@Serializable
data class MemberIndividualSavingsRowDto(
    @SerialName("memberId") val memberId: String,
    @SerialName("name") val name: String,
    @SerialName("currentBalance") val currentBalance: Long,
    @SerialName("lastTransaction") val lastTransaction: Long? = null,
    @SerialName("lastTransactionDate") val lastTransactionDate: String? = null,
)

/**
 * Wire DTO for savings-dashboard's individual-tab summary
 * (`GET /companion/groups/{groupId}/savings/individual`).
 *
 * See API.md#dtos — IndividualSavingsSummary.
 */
@Serializable
data class IndividualSavingsSummaryDto(
    @SerialName("totalBalance") val totalBalance: Long,
    @SerialName("weeklyTrend") val weeklyTrend: List<WeeklyContributionPointDto> = emptyList(),
    @SerialName("memberRows") val memberRows: List<MemberIndividualSavingsRowDto> = emptyList(),
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}
