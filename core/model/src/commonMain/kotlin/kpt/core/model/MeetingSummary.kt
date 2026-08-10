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
 * Domain composite for the read-only meeting-summary screen
 * (`GET /datatables/dt_meeting_record/{groupId}?meetingNumber=N`). The single Store5 read result
 * bundling the meeting's persisted totals (attendance, savings, loans, fines, opening/closing
 * corpus) with the per-member savings breakdown and per-member loan activity.
 *
 * Mirrors `idea-layer/screens/meeting-summary/api.yaml#dtos.MeetingRecordDetail` /
 * `ui.yaml#state_model.MeetingSummaryViewModel.state.meetingSummary` (the state binds every
 * `{{meetingSummary.*}}` field on the hero card, metric grid, corpus-reconciliation section, and
 * savings-breakdown list). All money fields are `Long` (whole KES, matching `api.yaml#dtos`); counts
 * and the meeting number are `Int`.
 *
 * See API.md#models — MeetingSummaryData.
 */
data class MeetingSummaryData(
    val meetingId: String,
    val meetingNumber: Int,
    val actualDate: String,
    /** Wall-clock time the meeting was conducted, "HH:mm" — blank when not recorded. */
    val meetingTime: String = "",
    val attendanceCount: Int,
    val totalMemberCount: Int,
    val groupSavingsCollected: Long,
    val individualSavingsCollected: Long,
    val totalSavingsCollected: Long,
    val loansDisbursed: Long,
    val loansRepaid: Long,
    val finesCollected: Long,
    val openingCorpus: Long,
    val closingCorpus: Long,
    val savingsBreakdown: List<SavingsBreakdownItem>,
    val loanItems: List<LoanSummaryItem>,
) {
    /**
     * Corpus movement across the meeting — `closingCorpus − openingCorpus`, the value rendered by
     * `ui.yaml#corpus_reconciliation_section.net_change_value_format`. Derived (never stored) so the
     * two corpus figures stay the single source of truth.
     */
    val netCorpusChange: Long get() = closingCorpus - openingCorpus
}

/**
 * Per-member savings contribution row for one meeting (`api.yaml#dtos.SavingsBreakdownItem`).
 * Bound by `ui.yaml#savings_breakdown_section.savings_breakdown_row` — each row renders
 * [memberName] + group/individual split. See API.md#models — SavingsBreakdownItem.
 */
data class SavingsBreakdownItem(
    val memberId: String,
    val memberName: String,
    val groupSavings: Long,
    val individualSavings: Long,
) {
    /** Row total — `groupSavings + individualSavings`, the trailing value in the breakdown row. */
    val totalSavings: Long get() = groupSavings + individualSavings
}

/**
 * Per-member loan activity row for one meeting (`api.yaml#dtos.LoanSummaryItem`) — amounts
 * disbursed / repaid during the meeting plus the resulting outstanding balance. Carried in the
 * composite for the share/export report (`ShareMeetingReport`); the screen surfaces the aggregate
 * `loansDisbursed` / `loansRepaid` totals in the metric grid. See API.md#models — LoanSummaryItem.
 */
data class LoanSummaryItem(
    val memberId: String,
    val memberName: String,
    val amountDisbursed: Long,
    val amountRepaid: Long,
    val outstandingAfter: Long,
)
