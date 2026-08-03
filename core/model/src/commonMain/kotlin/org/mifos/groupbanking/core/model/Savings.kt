/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.model

import kotlinx.datetime.LocalDate

/**
 * Domain enum for the direction/kind of a savings statement transaction — used by
 * [SavingsStatementEntry] (member-savings-detail). Named `SavingsTransactionType` (distinct from
 * the EXISTING `TransactionType` in `SavingsTransaction.kt`, personal-dashboard's compact
 * DEPOSIT/WITHDRAWAL/UNKNOWN recent-activity enum) because this richer enum adds
 * INTEREST_POSTING/FEE_DEDUCTION/TRANSFER — a genuinely different, wider value-set flagged for
 * Station 3 rather than silently widening the existing enum (same "don't force reuse across
 * incompatible value-sets" precedent as `LoanAccountStatus` vs `LoanStatus`).
 *
 * See API.md#models — SavingsTransactionType.
 */
enum class SavingsTransactionType {
    DEPOSIT,
    WITHDRAWAL,
    INTEREST_POSTING,
    FEE_DEDUCTION,
    TRANSFER,
    UNKNOWN,
}

/**
 * Domain model mirroring [org.mifos.groupbanking.core.network.model.SavingsLedgerTransactionTypeDto]
 * — the raw Fineract `{value, code, description}` triple. Kept a plain data class (not collapsed
 * into [SavingsTransactionType]) — `api.yaml` declares no `code` value-set, per Hard Rule 4.
 *
 * See API.md#models — SavingsLedgerTransactionType.
 */
data class SavingsLedgerTransactionType(
    val value: Int,
    val code: String,
    val description: String,
)

/**
 * Domain model for one row of personal-savings' raw Fineract self-service ledger
 * (`GET /self/savingsaccounts/{savingsId}/transactions`) — group-linked OR individual account,
 * selected by [SavingsTab]. Named `SavingsLedgerEntry` (NOT `SavingsTransaction` — that name is
 * TAKEN by the EXISTING personal-dashboard compact shape in `SavingsTransaction.kt`) to avoid a
 * Kotlin symbol collision; see `SavingsLedgerEntryDto` kdoc for the full naming-collision note
 * flagged for Station 3. [currencyCode]/[currencyDisplaySymbol] are the flattened
 * `SavingsLedgerCurrencyDto` pair (no separate nested domain wrapper — a trivial 2-field leaf
 * value).
 *
 * See API.md#models — SavingsLedgerEntry.
 */
data class SavingsLedgerEntry(
    val id: Long,
    val type: SavingsLedgerTransactionType,
    val date: LocalDate,
    val amount: Double,
    val runningBalance: Double,
    val currencyCode: String,
    val currencyDisplaySymbol: String,
)

/**
 * Domain enum for personal-savings' own-account tab selector
 * (`ui.yaml#state_model.PersonalSavingsViewModel.state.selectedTab`) — GROUP_LINKED (the member's
 * mandatory group-linked account) vs INDIVIDUAL (their optional voluntary account). Pure
 * client-side UI state — never serialized to/from the wire.
 *
 * **Naming-collision note (flagged for the cross-feature repair station):** savings-dashboard
 * declares its OWN `SavingsTab` value-set (`GROUP`/`INDIVIDUAL`) under the SAME bare name in its
 * own `api.yaml#dtos.SavingsTab` — see [SavingsDashboardTab] below, named distinctly to avoid the
 * Kotlin symbol collision. The two are NOT unified here: personal-savings' tab flips between the
 * CALLER's own two accounts; savings-dashboard's tab flips between a GROUP-WIDE summary and a
 * PER-MEMBER-INDIVIDUAL summary — different concepts sharing an unlucky name. Resolve at
 * Station 3.
 *
 * See API.md#models — SavingsTab.
 */
enum class SavingsTab {
    GROUP_LINKED,
    INDIVIDUAL,
}

/**
 * Domain enum for savings-dashboard's own tab selector (`api.yaml#dtos.SavingsTab` —
 * `[GROUP, INDIVIDUAL]`). Named `SavingsDashboardTab` (NOT `SavingsTab` — TAKEN by
 * personal-savings' own account-selector enum above) to avoid a Kotlin symbol collision — see the
 * naming-collision note on [SavingsTab].
 *
 * See API.md#models — SavingsDashboardTab.
 */
enum class SavingsDashboardTab {
    GROUP,
    INDIVIDUAL,
}

/**
 * Domain model for member-savings-detail's member identity subset (`member` field of the
 * single-read companion response).
 *
 * See API.md#models — SavingsMember.
 */
data class SavingsMember(
    val memberId: String,
    val displayName: String,
    val photoUri: String?,
)

/**
 * Domain model for one statement row on member-savings-detail's paginated transaction history —
 * richer than [SavingsLedgerEntry] (adds [reversed]) and companion-normalized (`String` id/date,
 * no raw Fineract component-array/currency shapes). Named `SavingsStatementEntry` (NOT
 * `SavingsTransaction`, TAKEN by the existing personal-dashboard shape) — see the naming-collision
 * note on [SavingsLedgerEntry].
 *
 * See API.md#models — SavingsStatementEntry.
 */
data class SavingsStatementEntry(
    val id: String,
    val date: LocalDate,
    val type: SavingsTransactionType,
    val amount: Double,
    val runningBalance: Double,
    val reversed: Boolean,
)

/**
 * Domain composite for the member-savings-detail screen's single read
 * (`GET /companion/groups/{groupId}/members/{memberId}/savings`) — member identity +
 * contribution-model-aware balance fields + sparkline + paginated statement.
 * [sharesHeld]/[shareValue] are populated only for SHARE_BASED_VARIABLE (VSLA/SILC) groups;
 * `null` for FIXED_AMOUNT (ROSCA/SHG) groups, where [savingsBalance] alone is meaningful.
 * [sparklineData] REUSES the EXISTING [SavingsDataPoint] domain model (`MemberProfile.kt`)
 * outright — same `{date, balance}` shape, now with a real wire source (unlike member-profile's
 * own "confirmed gap, always empty" usage).
 *
 * See API.md#models — MemberSavingsDetail.
 */
data class MemberSavingsDetail(
    val member: SavingsMember,
    val savingsAccountNo: String,
    val savingsBalance: Double,
    val sharesHeld: Int?,
    val shareValue: Long?,
    val sparklineData: List<SavingsDataPoint>,
    val transactions: List<SavingsStatementEntry>,
    val totalTransactions: Int,
    val hasNextPage: Boolean,
)

/**
 * Client-side transaction-list filter for member-savings-detail
 * (`api.yaml#dtos.TransactionFilter`). Pure UI/filter state — never serialized to or from the
 * wire, same "local filter enum" precedent as `LoanStatusFilter`.
 *
 * See API.md#models — SavingsTransactionFilter.
 */
enum class SavingsTransactionFilter {
    ALL,
    DEPOSITS,
    WITHDRAWALS,
}

/**
 * Client-side predicate for [SavingsTransactionFilter] — filters an already-loaded
 * [SavingsStatementEntry] list in memory, no re-fetch. [SavingsTransactionFilter.ALL] matches
 * everything.
 *
 * See API.md#models — SavingsTransactionFilter.
 */
fun SavingsTransactionFilter.matches(entry: SavingsStatementEntry): Boolean = when (this) {
    SavingsTransactionFilter.ALL -> true
    SavingsTransactionFilter.DEPOSITS -> entry.type == SavingsTransactionType.DEPOSIT
    SavingsTransactionFilter.WITHDRAWALS -> entry.type == SavingsTransactionType.WITHDRAWAL
}

/**
 * Domain model for one weekly point on savings-dashboard's group/individual contribution trend
 * chart (`api.yaml#dtos.WeeklyContributionPoint`) — shared by [GroupSavingsSummary] and
 * [IndividualSavingsSummary].
 *
 * See API.md#models — WeeklyContributionPoint.
 */
data class WeeklyContributionPoint(
    val weekLabel: String,
    val groupAmount: Long,
    val individualAmount: Long,
)

/**
 * Domain model for one member row on the group savings-dashboard summary
 * (`GET /companion/groups/{groupId}/savings`). [sharesHeld]/[shareValue] populated only for
 * SHARE_BASED_VARIABLE (VSLA/SILC) groups.
 *
 * See API.md#models — MemberGroupSavingsRow.
 */
data class MemberGroupSavingsRow(
    val memberId: String,
    val name: String,
    val totalContributed: Long,
    val lastContribution: Long,
    val meetingsContributed: Int,
    val sharesHeld: Int?,
    val shareValue: Long?,
)

/**
 * Domain model for savings-dashboard's group-tab summary
 * (`GET /companion/groups/{groupId}/savings`).
 *
 * See API.md#models — GroupSavingsSummary.
 */
data class GroupSavingsSummary(
    val cycleTarget: Long,
    val cycleCollected: Long,
    val totalCollected: Long,
    val weeklyTrend: List<WeeklyContributionPoint>,
    val memberRows: List<MemberGroupSavingsRow>,
)

/**
 * Domain model for one member row on savings-dashboard's individual-tab summary
 * (`GET /companion/groups/{groupId}/savings/individual`).
 *
 * See API.md#models — MemberIndividualSavingsRow.
 */
data class MemberIndividualSavingsRow(
    val memberId: String,
    val name: String,
    val currentBalance: Long,
    val lastTransaction: Long?,
    val lastTransactionDate: LocalDate?,
)

/**
 * Domain model for savings-dashboard's individual-tab summary
 * (`GET /companion/groups/{groupId}/savings/individual`).
 *
 * See API.md#models — IndividualSavingsSummary.
 */
data class IndividualSavingsSummary(
    val totalBalance: Long,
    val weeklyTrend: List<WeeklyContributionPoint>,
    val memberRows: List<MemberIndividualSavingsRow>,
)

/**
 * Domain composite for personal-savings' combined-tab read (`SavingsRepository.loadMemberSavings`,
 * `core/data`) — bundles the mandatory group-linked account's transaction ledger with the
 * optional voluntary individual account's ledger. Neither
 * `api.yaml#api[get_group_linked_transactions,get_individual_transactions]` exposes a separate
 * account-summary endpoint — the "account" itself is represented purely by its transaction
 * ledger; the newest entry's [SavingsLedgerEntry.runningBalance] IS the current balance (Fineract
 * returns ledger rows newest-first). [individualTransactions] is `null` when the caller passed no
 * `individualSavingsId` (the member has no voluntary account — the Individual tab does not exist
 * for them), distinct from an empty (fetched-but-zero-rows) list.
 *
 * See API.md#models — MemberSavingsBundle.
 */
data class MemberSavingsBundle(
    val groupLinkedTransactions: List<SavingsLedgerEntry>,
    val individualTransactions: List<SavingsLedgerEntry>?,
)

/**
 * Domain composite for savings-dashboard's parallel on-mount/refresh/retry read
 * (`SavingsRepository.loadSavingsDashboard`, `core/data`) — bundles the group-tab and
 * individual-tab summaries fetched concurrently from the two independent companion endpoints
 * (`get_group_savings_summary`/`get_individual_savings_summary`, `data-flow.yaml#entries[0]`:
 * "Both calls run in parallel on mount"). Both tabs' data is always present after a successful
 * load — `SelectTab` (`api.yaml#dtos.SavingsTab` GROUP/INDIVIDUAL) is a pure client-side pane
 * switch with no re-fetch (`data-flow.yaml#entries[SelectTab]`).
 *
 * See API.md#models — SavingsDashboardSummary.
 */
data class SavingsDashboardSummary(
    val group: GroupSavingsSummary,
    val individual: IndividualSavingsSummary,
)
