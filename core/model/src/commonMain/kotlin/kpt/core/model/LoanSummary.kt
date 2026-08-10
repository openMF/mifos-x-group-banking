/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.model

/**
 * Domain model for a single loan row on the group loan-list screen (`GET
 * /groups/{groupId}/loans`) — pure business shape, no wire concerns.
 * **Canonical** — this is the SAME `LoanSummary` shape reused by loan-detail
 * + loan dialogs + personal-loans, not a loan-list-local type. Read-only;
 * loan-list never mutates it locally (mutation flows, e.g. loan-apply, are a
 * separate write contract).
 *
 * See API.md#models — LoanSummary.
 */
data class LoanSummary(
    val id: Long,
    val memberId: Long,
    val memberName: String,
    val memberPhotoUrl: String?,
    val loanProductName: String,
    val principalAmount: Double,
    val outstandingBalance: Double,
    val overdueAmount: Double,
    val status: LoanAccountStatus,
    val nextRepaymentDate: String?,
    val isOverdue: Boolean,
    val fineractLoanId: Long,
)

/**
 * Domain page envelope for the offset-paginated `GET
 * /groups/{groupId}/loans` response (`page_size=20`). Mirrors wire
 * `LoanPageDto` — see `LoanSummaryMappers.kt`.
 */
data class LoanPage(
    val totalFilteredRecords: Int,
    val loans: List<LoanSummary>,
)

/**
 * Domain enum for a loan account's lifecycle status. Mirrors wire
 * `LoanAccountStatusDto` 1:1. Named `LoanAccountStatus` (NOT `LoanStatus`) to
 * avoid a Kotlin symbol collision with the EXISTING `LoanStatus` declared in
 * `Member.kt` (member-list's member-level loan-status chip —
 * `ACTIVE`/`NONE`/`OVERDUE`/`UNKNOWN`, a different per-MEMBER concept with a
 * non-matching value-set: this enum has no `NONE`, `LoanStatus` has no
 * `CLOSED`/`PENDING`/`REJECTED`). Widening/unifying the two is a
 * cross-feature decision flagged for Station 3 rather than made unilaterally
 * here — see `LoanSummaryDto.kt` kdoc for the full rationale. [UNKNOWN]
 * absorbs any wire value this client build does not yet recognize.
 *
 * See API.md#models — LoanAccountStatus.
 */
enum class LoanAccountStatus {
    ACTIVE,
    OVERDUE,
    CLOSED,
    PENDING,
    REJECTED,
    UNKNOWN,
}

/**
 * Domain enum for the loan-list status-filter chips
 * (`ui.yaml#components.filter_chips_row` — All / Active / Overdue / Closed).
 * Pure client-side UI/filter state — never serialized to or from the wire
 * (no `@Serializable` counterpart in `core/network/model`), so it lives here
 * even though `api.yaml#dtos.LoanStatusFilter` declares its value-set.
 * `LoanListViewModel.OnFilterChange` sets this on `LoanListState.selectedFilter`
 * and recomputes `filteredLoans` from the Store5-backed in-memory
 * [LoanSummary] list; `ALL` performs no filtering.
 *
 * See API.md#models — LoanStatusFilter.
 */
enum class LoanStatusFilter {
    ALL,
    ACTIVE,
    OVERDUE,
    CLOSED,
}

/**
 * Client-side predicate for [LoanStatusFilter] — reused by both loan-list AND personal-loans to
 * filter an already-loaded [LoanSummary] list in memory, with no re-fetch. [LoanStatusFilter.ALL]
 * matches everything; every other member matches 1:1 against [LoanSummary.status]. Mirrors
 * `LoanListViewModel.filterByStatus`'s `when` branches exactly so the two features never drift on
 * filter semantics.
 *
 * `ui.yaml#components.filter_chips_row` for personal-loans only renders the `ALL`/`ACTIVE`/`CLOSED`
 * chips (no `OVERDUE` chip — overdue loans surface inline via [LoanSummary.isOverdue] on the
 * `ACTIVE` card instead), but the [LoanStatusFilter.OVERDUE] branch is kept here for parity with
 * loan-list's four-chip row, since both features share this one enum.
 *
 * See API.md#models — LoanStatusFilter.
 */
fun LoanStatusFilter.matches(loan: LoanSummary): Boolean = when (this) {
    LoanStatusFilter.ALL -> true
    LoanStatusFilter.ACTIVE -> loan.status == LoanAccountStatus.ACTIVE
    LoanStatusFilter.OVERDUE -> loan.status == LoanAccountStatus.OVERDUE
    LoanStatusFilter.CLOSED -> loan.status == LoanAccountStatus.CLOSED
}
