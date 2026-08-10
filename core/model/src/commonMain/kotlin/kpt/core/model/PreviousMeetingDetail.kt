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
 * Attendance status for one member at a completed meeting
 * (`previous-meeting-review/api.yaml#dtos.AttendanceRecord.status`). Drives the
 * `attendance_detail_row` avatar/chip colour mapping in `ui.yaml` (PRESENT → primaryContainer,
 * LATE → warningContainer, ABSENT → errorContainer). See API.md#models — AttendanceStatus.
 */
enum class AttendanceStatus {
    PRESENT,
    LATE,
    ABSENT,
    ;

    companion object {
        /** Lenient parse for the wire `status` string — unknown values fall back to [ABSENT]. */
        fun fromWire(raw: String): AttendanceStatus = when (raw.trim().uppercase()) {
            "PRESENT" -> PRESENT
            "LATE" -> LATE
            else -> ABSENT
        }
    }
}

/**
 * Per-member attendance row for one completed meeting
 * (`previous-meeting-review/api.yaml#dtos.AttendanceRecord`) — resolved from the separate
 * `GET /datatables/dt_meeting_attendance/{meetingId}` datatable (data-flow.yaml
 * `get_meeting_attendance`). Bound by `ui.yaml#attendance_detail_row` — [memberName] +
 * [status] chip + the optional fine supporting line (rendered only when [fineAmount] > 0).
 * [fineAmount] is `Long` whole KES, matching every other money field on the screen.
 *
 * See API.md#models — AttendanceRecord.
 */
data class AttendanceRecord(
    val memberId: String,
    val memberName: String,
    val status: AttendanceStatus,
    val fineAmount: Long,
)

/**
 * Category of an unresolved carry-over item surfaced in the `unresolved_alert_card`
 * (`previous-meeting-review/api.yaml#dtos.UnresolvedItem.type`). Drives the leading-icon selection
 * in `ui.yaml#unresolved_item_row`. See API.md#models — UnresolvedType.
 */
enum class UnresolvedType {
    UNPAID_FINE,
    PENDING_LOAN_VOTE,
    MISSED_ATTENDANCE,
}

/**
 * One unresolved carry-over item from the previous meeting
 * (`previous-meeting-review/api.yaml#dtos.UnresolvedItem`). Rendered as a warning row in
 * `ui.yaml#unresolved_alert_card` (the whole card is hidden by `visible_when:
 * unresolvedItems.isNotEmpty()`). [description] is a server- or rule-derived human sentence;
 * [memberId] links the item back to the member it concerns.
 *
 * See API.md#models — UnresolvedItem.
 */
data class UnresolvedItem(
    val type: UnresolvedType,
    val description: String,
    val memberId: String,
)

/**
 * Read-only projection backing `previous-meeting-review-screen` (FR-019). Bundles the reused
 * meeting record composite ([summary] — persisted totals + per-member savings breakdown + per-member
 * loan activity, sourced from the SAME single-key `MeetingSummaryStore` that backs the
 * meeting-summary screen) with the two review-only additions: the per-member [attendanceRecords]
 * (from `get_meeting_attendance`) and the derived [unresolvedItems] (carry-over fines / votes).
 *
 * Deliberately COMPOSES [MeetingSummaryData] rather than duplicating its fields — the record
 * pipeline (DTO / store / DAO / mapper) is reused wholesale (no second `@Serializable` MeetingRecord
 * shape, per the batch-1 collision precedent). The passthrough accessors below expose the summary
 * fields the `ui.yaml` `{{meetingDetail.*}}` tokens bind, so the Screen never reaches through
 * `.summary.` for the common metrics.
 *
 * Previous-meeting data is immutable once the meeting is closed, so this projection is strictly
 * read (`data-flow.yaml#sync_queue: []`, RULE-IMPLEMENT-STORE5-001 S5-1 / S5-2).
 *
 * See API.md#models — PreviousMeetingDetail.
 */
data class PreviousMeetingDetail(
    val summary: MeetingSummaryData,
    val attendanceRecords: List<AttendanceRecord>,
    val unresolvedItems: List<UnresolvedItem>,
) {
    // -- Passthrough accessors for the ui.yaml {{meetingDetail.*}} binding tokens ------------------
    val meetingId: String get() = summary.meetingId
    val meetingNumber: Int get() = summary.meetingNumber
    val actualDate: String get() = summary.actualDate
    val meetingTime: String get() = summary.meetingTime
    val attendanceCount: Int get() = summary.attendanceCount
    val totalMemberCount: Int get() = summary.totalMemberCount
    val totalSavingsCollected: Long get() = summary.totalSavingsCollected
    val closingCorpus: Long get() = summary.closingCorpus
    val finesCollected: Long get() = summary.finesCollected
    val loansDisbursed: Long get() = summary.loansDisbursed
    val savingsBreakdown: List<SavingsBreakdownItem> get() = summary.savingsBreakdown
    val loanItems: List<LoanSummaryItem> get() = summary.loanItems
}
