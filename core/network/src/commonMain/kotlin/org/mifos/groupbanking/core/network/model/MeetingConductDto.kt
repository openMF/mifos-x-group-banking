/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire DTOs for the `meeting-conduct` feature's 11 Fineract endpoints
 * (`idea-layer/screens/meeting-conduct/api.yaml`). The shared companion `HttpClient`
 * (`NetworkModule`) is configured with `ignoreUnknownKeys = true`, so every response DTO here is a
 * PARTIAL projection — only the fields the mappers read are declared. See API.md#services.
 */

// -- Read responses ---------------------------------------------------------------------------------

/** `GET /datatables/dt_meeting_record/{groupId}` — previous meeting summary row (`get_previous_meeting_record`). */
@Serializable
data class MeetingRecordDetailDto(
    @SerialName("meetingId") val meetingId: String = "",
    @SerialName("meetingNumber") val meetingNumber: Int = 0,
    @SerialName("actualDate") val actualDate: String = "",
    @SerialName("totalSavings") val totalSavings: Long = 0L,
    @SerialName("totalRepayments") val totalRepayments: Long = 0L,
    @SerialName("totalLoansDisbursed") val totalLoansDisbursed: Long = 0L,
    @SerialName("totalFinesCollected") val totalFinesCollected: Long = 0L,
    @SerialName("closingCorpus") val closingCorpus: Long = 0L,
    @SerialName("attendanceCount") val attendanceCount: Int = 0,
)

/** `GET /groups/{groupId}?associations=clientMembers,groupRoles` — group detail incl. active client members (`get_group_members`). */
@Serializable
data class GroupMembersDetailDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("name") val name: String = "",
    // Fineract's /groups/{id}?associations=clientMembers returns the roster under `clientMembers`,
    // not `activeClientMembers` — the old name never matched, so meeting-conduct saw members=0
    // (empty attendance list). Map the real field.
    @SerialName("clientMembers") val activeClientMembers: List<ClientMemberDto> = emptyList(),
)

@Serializable
data class ClientMemberDto(
    @SerialName("id") val id: Long = 0L,
    @SerialName("displayName") val displayName: String = "",
    @SerialName("role") val role: String? = null,
    @SerialName("savingsAccountId") val savingsAccountId: String? = null,
)

/** `GET /datatables/dt_group_corpus/{groupId}` — opening corpus balance (`get_group_corpus`). */
@Serializable
data class CorpusRecordDto(
    @SerialName("groupId") val groupId: Int = 0,
    @SerialName("corpusBalance") val corpusBalance: Long = 0L,
    @SerialName("cashOnHand") val cashOnHand: Long = 0L,
    @SerialName("lastUpdatedMeeting") val lastUpdatedMeeting: Int = 0,
    @SerialName("lastUpdatedDate") val lastUpdatedDate: String = "",
)

/** `GET /loans` — active-loans list envelope (`get_active_loans`). */
@Serializable
data class LoanListResponseDto(
    @SerialName("totalFilteredRecords") val totalFilteredRecords: Int = 0,
    @SerialName("pageItems") val pageItems: List<MeetingActiveLoanDto> = emptyList(),
)

@Serializable
data class MeetingActiveLoanDto(
    @SerialName("loanId") val loanId: String = "",
    @SerialName("clientId") val clientId: String = "",
    @SerialName("clientName") val clientName: String = "",
    @SerialName("principal") val principal: Long = 0L,
    @SerialName("outstandingBalance") val outstandingBalance: Long = 0L,
    @SerialName("isOverdue") val isOverdue: Boolean = false,
    @SerialName("weekNumber") val weekNumber: Int = 0,
    @SerialName("numberOfRepayments") val numberOfRepayments: Int = 0,
    @SerialName("expectedWeeklyRepayment") val expectedWeeklyRepayment: Long = 0L,
)

/** `GET /datatables/dt_loan_vote/{loanId}` — vote tally row (`get_loan_votes`). */
@Serializable
data class LoanVoteRecordDto(
    @SerialName("loanId") val loanId: String = "",
    @SerialName("votesFor") val votesFor: Int = 0,
    @SerialName("votesAgainst") val votesAgainst: Int = 0,
    @SerialName("votesAbstain") val votesAbstain: Int = 0,
)

/** Generic Fineract datatable/command write response (`DataTableEntryResponse`). */
@Serializable
data class DataTableEntryResponseDto(
    @SerialName("resourceId") val resourceId: Long = 0L,
    @SerialName("resourceIdentifier") val resourceIdentifier: String = "",
)

// -- Write requests ---------------------------------------------------------------------------------

/** `POST /datatables/dt_meeting_record` (`post_meeting_record`, priority 1). */
@Serializable
data class CreateMeetingRecordRequestDto(
    val groupId: Int,
    val meetingNumber: Int,
    val actualDate: String,
    val openingCorpus: Long,
    val closingCorpus: Long,
    val totalSavingsCollected: Long,
    val totalRepaymentsReceived: Long,
    val totalLoansDisbursed: Long,
    val totalFinesCollected: Long,
    val attendanceCount: Int,
    /** Wall-clock time the meeting was conducted, "HH:mm" — persisted to dt_meeting_record.completed_time. */
    val completedTime: String = "",
    val locale: String = "en",
    val dateFormat: String = "dd MMMM yyyy",
)

/** `POST /datatables/dt_meeting_attendance` (`post_meeting_attendance`, priority 2). */
@Serializable
data class CreateAttendanceRequestDto(
    val meetingId: String,
    val memberId: String,
    val status: String,
    val fineAmount: Long,
    val locale: String = "en",
)

/** `POST /savingsaccounts/{savingsId}/transactions` (`post_savings_transaction`, priority 3). */
@Serializable
data class SavingsTransactionRequestDto(
    val transactionDate: String,
    val transactionAmount: Long,
    val paymentTypeId: Int = 1,
    val locale: String = "en",
    val dateFormat: String = "dd MMMM yyyy",
)

/** `POST /loans/{loanId}/transactions?command=repayment` (`post_loan_repayment`, priority 4). */
@Serializable
data class LoanRepaymentRequestDto(
    val transactionDate: String,
    val transactionAmount: Long,
    val paymentTypeId: Int = 1,
    val locale: String = "en",
    val dateFormat: String = "dd MMMM yyyy",
)

/**
 * `POST /companion/loan-applications/{clientId}/disburse` (`post_loan_disbursal`, priority 5).
 * The approve-then-materialise action: the companion reads the client's PENDING dt_loan_request,
 * creates the Fineract loan from the KES VSLA product, approves + disburses it, and marks the
 * request APPROVED. [amount] is the group-approved principal (defaults to the requested amount on
 * the companion side when omitted/zero). Not a raw Fineract disburse — see the companion's
 * `HandleDisburseLoanApplication` (a bare loan-transaction disburse can't materialise an
 * application that has no Fineract loan yet).
 */
@Serializable
data class LoanDisbursalRequestDto(
    val actualDisbursementDate: String,
    val amount: Long = 0L,
    val note: String = "",
    val locale: String = "en",
    val dateFormat: String = "dd MMMM yyyy",
)

/**
 * `GET /companion/groups/{groupId}/loan-requests` (`get_pending_loan_applications`) — one PENDING
 * loan request projected to the meeting-conduct review step's [org.mifos.groupbanking.core.model.meeting.LoanApplication].
 * [id] == the borrower's clientId (string), so the approve→disburse action resolves the client from
 * the application id. Parsed with `ignoreUnknownKeys`.
 */
@Serializable
data class MeetingLoanApplicationDto(
    val id: String,
    val memberId: String,
    val memberName: String,
    val requestedAmount: Long,
    val purpose: String = "",
)

/** `PUT /datatables/dt_group_corpus/{groupId}` (`patch_corpus`, priority 6). */
@Serializable
data class UpdateCorpusRequestDto(
    val corpusBalance: Long,
    val lastUpdatedMeeting: Int,
    val lastUpdatedDate: String,
    val locale: String = "en",
    val dateFormat: String = "dd MMMM yyyy",
)

/**
 * The offline `sync_queue` payload — the whole meeting submission serialized as ONE row
 * (`flow.yaml#submit_meeting.offline`, key `meeting_id`). Carries the ordered sub-payloads so the
 * sync worker can replay the priority-1..6 sequence on reconnect.
 */
@Serializable
data class MeetingSubmissionPayloadDto(
    val meetingId: String,
    val meetingNumber: Int,
    val groupId: Int,
    val record: CreateMeetingRecordRequestDto,
    val attendance: List<CreateAttendanceRequestDto>,
    val savings: List<MeetingSavingsPayloadDto>,
    val repayments: List<MeetingRepaymentPayloadDto>,
    val disbursals: List<MeetingDisbursalPayloadDto>,
    val corpusUpdate: UpdateCorpusRequestDto,
)

@Serializable
data class MeetingSavingsPayloadDto(val savingsAccountId: String, val body: SavingsTransactionRequestDto)

@Serializable
data class MeetingRepaymentPayloadDto(val loanId: String, val body: LoanRepaymentRequestDto)

@Serializable
data class MeetingDisbursalPayloadDto(val loanId: String, val body: LoanDisbursalRequestDto)
