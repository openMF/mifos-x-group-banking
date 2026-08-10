/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.model.meeting

import kotlinx.serialization.Serializable

/**
 * Domain models for the `meeting-conduct` 7-step wizard feature
 * (`idea-layer/screens/meeting-conduct/api.yaml#dtos`, `ui.yaml#state_model`).
 *
 * Namespaced under `org.mifos.groupbanking.core.model.meeting` deliberately: the meeting projection
 * of a group member ([GroupMember] here — `memberId`/`initials`/`role`) is a DISTINCT shape from the
 * existing `org.mifos.groupbanking.core.model.GroupMember` (`fineractClientId`/`displayName`), and
 * likewise [LoanSummary] here differs from `org.mifos.groupbanking.core.model.LoanSummary`. Keeping
 * them in their own package avoids a clobbering simple-name collision while matching the idea-layer
 * DTO names verbatim. See API.md#models.
 */

/** `api.yaml#dtos.AttendanceStatus` — a member's attendance verdict for one meeting. */
enum class AttendanceStatus { PRESENT, LATE, ABSENT }

/** `api.yaml#dtos.SavingsType` — the two savings buckets collected per member per meeting. */
enum class SavingsType { GROUP_LINKED, INDIVIDUAL }

/** `api.yaml#dtos.LoanVote` — a chairperson/member vote on a pending loan application. */
enum class LoanVote { FOR, AGAINST, ABSTAIN }

/**
 * `api.yaml#dtos.GroupMember` — the attendance/savings repeater-row projection of one group member.
 * [savingsAccountId] is the Fineract savings account the step-3 deposits post against
 * (`api.yaml#api[post_savings_transaction].params.savingsId`); nullable because a member without a
 * linked savings account cannot receive a group deposit (that member's row disables the input).
 */
@Serializable
data class GroupMember(
    val memberId: String,
    val name: String,
    val initials: String,
    val role: String,
    val savingsAccountId: String? = null,
)

/** `api.yaml#dtos.SavingsEntry` — the per-member group + individual savings amounts entered in step 3. */
@Serializable
data class SavingsEntry(
    val memberId: String,
    val groupAmount: Long = 0L,
    val individualAmount: Long = 0L,
) {
    val total: Long get() = groupAmount + individualAmount
}

/**
 * `api.yaml#dtos.LoanSummary` — the step-4 loan-review-row projection of one active loan (projected
 * from `LoanDetail` by `MeetingConductMappers`).
 */
@Serializable
data class LoanSummary(
    val loanId: String,
    val memberId: String,
    val memberName: String,
    val memberInitials: String,
    val principal: Long,
    val outstandingBalance: Long,
    val isOverdue: Boolean,
    val weekNumber: Int,
    val numberOfRepayments: Int,
    val expectedWeeklyRepayment: Long,
)

/** `api.yaml#dtos.LoanApplication` — a pending loan application reviewed + voted on in step 5. */
@Serializable
data class LoanApplication(
    val id: String,
    val memberId: String,
    val memberName: String,
    val requestedAmount: Long,
    val purpose: String,
)

/** `api.yaml#dtos.PreviousMeetingSummary` — the read-only step-0 review card projection. */
@Serializable
data class PreviousMeetingSummary(
    val meetingId: String = "",
    val meetingNumber: Int,
    val date: String,
    val totalCollected: Long,
    val corpusAtClose: Long,
    val attendanceCount: Int,
)

/** `api.yaml#dtos.LoanVoteRecord` — the persisted vote tally for one pending loan application (step 5). */
@Serializable
data class LoanVoteRecord(
    val loanId: String,
    val votesFor: Int,
    val votesAgainst: Int,
    val votesAbstain: Int,
)

/** `api.yaml#dtos.CorpusRecord` — the group's opening corpus + cash-on-hand balance (step 2, corpus gate). */
@Serializable
data class CorpusRecord(
    val groupId: Int,
    val corpusBalance: Long,
    val cashOnHand: Long,
    val lastUpdatedMeeting: Int,
    val lastUpdatedDate: String,
)

/**
 * Aggregate on-mount load result — the 5-way parallel fan-in of `get_previous_meeting_record` +
 * `get_group_members` + `get_group_corpus` + `get_active_loans` + `get_loan_votes`
 * (`flow.yaml#flow_logic.screen_init.parallel_load`). Surfaced by
 * [org.mifos.groupbanking.core.data.repository.MeetingConductRepository.loadMeetingData] and applied
 * whole to `MeetingConductState`.
 */
data class MeetingConductData(
    val previousMeetingSummary: PreviousMeetingSummary?,
    val groupMembers: List<GroupMember>,
    val openingCorpus: Long,
    val cashOnHand: Long,
    val activeLoans: List<LoanSummary>,
    val pendingLoanApplications: List<LoanApplication>,
)

/** One member's attendance record in the ordered submit sequence (`post_meeting_attendance`, priority 2). */
@Serializable
data class AttendanceSubmission(
    val memberId: String,
    val status: AttendanceStatus,
    val fineAmount: Long,
)

/** One savings deposit in the ordered submit sequence (`post_savings_transaction`, priority 3). */
@Serializable
data class SavingsSubmission(
    val savingsAccountId: String,
    val memberId: String,
    val amount: Long,
    val type: SavingsType,
)

/** One loan repayment in the ordered submit sequence (`post_loan_repayment`, priority 4). */
@Serializable
data class RepaymentSubmission(
    val loanId: String,
    val amount: Long,
)

/** One approved-loan disbursal in the ordered submit sequence (`post_loan_disbursal`, priority 5, corpus-gated). */
@Serializable
data class DisbursalSubmission(
    val loanId: String,
    val amount: Long,
)

/**
 * The full meeting submission payload — everything the ordered submit sequence
 * (`flow.yaml#submit_meeting`, `data-flow.yaml` priority-1..6 entries) posts, plus the closing-corpus
 * PATCH. Serialized whole to the `sync_queue` when offline
 * (`flow.yaml#submit_meeting.offline.enqueue_all_payloads_to_sync_queue`, key `meeting_id`).
 */
@Serializable
data class MeetingSubmissionRequest(
    val meetingId: String,
    val meetingNumber: Int,
    val groupId: Int,
    val actualDate: String,
    /** Local wall-clock time the meeting was conducted/submitted, "HH:mm" (24h). Empty when unknown. */
    val completedTime: String = "",
    val openingCorpus: Long,
    val closingCorpus: Long,
    val totalSavingsCollected: Long,
    val totalRepaymentsReceived: Long,
    val totalLoansDisbursed: Long,
    val totalFinesCollected: Long,
    val attendanceCount: Int,
    val attendance: List<AttendanceSubmission>,
    val savings: List<SavingsSubmission>,
    val repayments: List<RepaymentSubmission>,
    val disbursals: List<DisbursalSubmission>,
)

/** Terminal result of a meeting submission — [isOffline] `true` when the payload was enqueued rather than posted live. */
data class MeetingSubmitResult(
    val meetingId: String,
    val isOffline: Boolean,
)
