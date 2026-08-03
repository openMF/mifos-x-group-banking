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

/**
 * Domain model for the loan-detail header (`GET /loans/{loanId}`) — pure
 * business shape, no wire concerns. Reuses [LoanAccountStatus] for [status]
 * (same per-loan-lifecycle concept as loan-list; no new status enum
 * introduced — see `LoanDetailDto.kt` kdoc for the reuse rationale).
 *
 * See API.md#models — LoanDetail.
 */
data class LoanDetail(
    val id: Long,
    val memberId: Long,
    val memberName: String,
    val loanProductName: String,
    val principalAmount: Double,
    val disbursedDate: String,
    val interestRatePercent: Double,
    val totalOutstanding: Double,
    val totalOverdue: Double,
    val status: LoanAccountStatus,
    val fineractLoanId: Long,
)

/**
 * Domain model for a single repayment-schedule row (one installment period).
 * Mirrors wire `RepaymentScheduleRowDto`. See API.md#models —
 * RepaymentScheduleRow.
 */
data class RepaymentScheduleRow(
    val weekNumber: Int,
    val dueDate: String,
    val dueAmount: Double,
    val paidAmount: Double,
    val balance: Double,
    val status: RepaymentRowStatus,
)

/**
 * Domain enum for a repayment-schedule row's paid/due state. Mirrors wire
 * `RepaymentRowStatusDto` 1:1; `UNKNOWN` absorbs any wire value this client
 * build does not yet recognize.
 *
 * See API.md#models — RepaymentRowStatus.
 */
enum class RepaymentRowStatus {
    PAID,
    PARTIAL,
    UPCOMING,
    OVERDUE,
    UNKNOWN,
}

/**
 * Domain model for a single repayment-history row (a posted transaction
 * against the loan). Mirrors wire `RepaymentTransactionDto`; [type] stays a
 * raw `String` — `api.yaml` declares no value-set for it. See API.md#models
 * — RepaymentTransaction.
 */
data class RepaymentTransaction(
    val id: Long,
    val type: String,
    val date: String,
    val amount: Double,
)

/**
 * Domain composite bundling the loan-detail header with its repayment
 * schedule and transaction history — the single Store5 read result for
 * `GET /loans/{loanId}?associations=repaymentSchedule,transactions`. Mirrors
 * wire `LoanDetailResponseDto`; `transactions` is renamed to
 * [repaymentHistory] here for domain-layer readability (the wire field name
 * mirrors the raw Fineract operation's own naming — see
 * `LoanDetailResponseDto` kdoc).
 *
 * See API.md#models — LoanDetailResponse.
 */
data class LoanDetailResponse(
    val loan: LoanDetail,
    val repaymentSchedule: List<RepaymentScheduleRow>,
    val repaymentHistory: List<RepaymentTransaction>,
)

/**
 * Domain enum for the loan-detail screen's schedule/history tab selector
 * (`api.yaml#dtos.LoanDetailTab`). Pure client-side UI state — never
 * serialized to or from the wire (no `@Serializable` counterpart in
 * `core/network/model`), same pattern as loan-list's `LoanStatusFilter`.
 *
 * See API.md#models — LoanDetailTab.
 */
enum class LoanDetailTab {
    SCHEDULE,
    HISTORY,
}
