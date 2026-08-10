/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.network.mapper

import kotlinx.serialization.json.Json
import kpt.core.model.meeting.AttendanceSubmission
import kpt.core.model.meeting.CorpusRecord
import kpt.core.model.meeting.DisbursalSubmission
import kpt.core.model.meeting.GroupMember
import kpt.core.model.meeting.LoanApplication
import kpt.core.model.meeting.LoanSummary
import kpt.core.model.meeting.LoanVoteRecord
import kpt.core.model.meeting.MeetingSubmissionRequest
import kpt.core.model.meeting.PreviousMeetingSummary
import kpt.core.model.meeting.RepaymentSubmission
import kpt.core.model.meeting.SavingsSubmission
import kpt.core.network.model.CorpusRecordDto
import kpt.core.network.model.CreateAttendanceRequestDto
import kpt.core.network.model.CreateMeetingRecordRequestDto
import kpt.core.network.model.GroupMembersDetailDto
import kpt.core.network.model.LoanDisbursalRequestDto
import kpt.core.network.model.LoanListResponseDto
import kpt.core.network.model.LoanRepaymentRequestDto
import kpt.core.network.model.LoanVoteRecordDto
import kpt.core.network.model.MeetingActiveLoanDto
import kpt.core.network.model.MeetingDisbursalPayloadDto
import kpt.core.network.model.MeetingLoanApplicationDto
import kpt.core.network.model.MeetingRecordDetailDto
import kpt.core.network.model.MeetingRepaymentPayloadDto
import kpt.core.network.model.MeetingSavingsPayloadDto
import kpt.core.network.model.MeetingSubmissionPayloadDto
import kpt.core.network.model.SavingsTransactionRequestDto
import kpt.core.network.model.UpdateCorpusRequestDto

/**
 * DTO ⇄ domain mappers for the `meeting-conduct` feature. Pure functions — no I/O, no try-catch.
 * Consumed by `MeetingConductRepositoryImpl` (`core/data`). See API.md#mappers.
 */

private val meetingJson = Json { encodeDefaults = true }

private fun initialsOf(displayName: String): String =
    displayName.trim().split(" ").filter { it.isNotBlank() }.take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "?" }

fun MeetingRecordDetailDto.toDomainModel(): PreviousMeetingSummary = PreviousMeetingSummary(
    meetingId = meetingId,
    meetingNumber = meetingNumber,
    date = actualDate,
    totalCollected = totalSavings + totalRepayments + totalFinesCollected,
    corpusAtClose = closingCorpus,
    attendanceCount = attendanceCount,
)

fun GroupMembersDetailDto.toGroupMembers(): List<GroupMember> = activeClientMembers.map { member ->
    GroupMember(
        memberId = member.id.toString(),
        name = member.displayName,
        initials = initialsOf(member.displayName),
        role = member.role ?: "Member",
        savingsAccountId = member.savingsAccountId,
    )
}

fun CorpusRecordDto.toDomainModel(): CorpusRecord = CorpusRecord(
    groupId = groupId,
    corpusBalance = corpusBalance,
    cashOnHand = cashOnHand,
    lastUpdatedMeeting = lastUpdatedMeeting,
    lastUpdatedDate = lastUpdatedDate,
)

fun MeetingActiveLoanDto.toLoanSummary(): LoanSummary = LoanSummary(
    loanId = loanId,
    memberId = clientId,
    memberName = clientName,
    memberInitials = initialsOf(clientName),
    principal = principal,
    outstandingBalance = outstandingBalance,
    isOverdue = isOverdue,
    weekNumber = weekNumber,
    numberOfRepayments = numberOfRepayments,
    expectedWeeklyRepayment = expectedWeeklyRepayment,
)

fun LoanListResponseDto.toLoanSummaries(): List<LoanSummary> = pageItems.map { it.toLoanSummary() }

fun LoanVoteRecordDto.toDomainModel(): LoanVoteRecord = LoanVoteRecord(
    loanId = loanId,
    votesFor = votesFor,
    votesAgainst = votesAgainst,
    votesAbstain = votesAbstain,
)

// -- Domain request -> submission sub-payloads ------------------------------------------------------

fun MeetingSubmissionRequest.toRecordDto(): CreateMeetingRecordRequestDto = CreateMeetingRecordRequestDto(
    groupId = groupId,
    meetingNumber = meetingNumber,
    actualDate = actualDate,
    openingCorpus = openingCorpus,
    closingCorpus = closingCorpus,
    totalSavingsCollected = totalSavingsCollected,
    totalRepaymentsReceived = totalRepaymentsReceived,
    totalLoansDisbursed = totalLoansDisbursed,
    totalFinesCollected = totalFinesCollected,
    attendanceCount = attendanceCount,
    completedTime = completedTime,
)

fun AttendanceSubmission.toDto(meetingId: String): CreateAttendanceRequestDto = CreateAttendanceRequestDto(
    meetingId = meetingId,
    memberId = memberId,
    status = status.name,
    fineAmount = fineAmount,
)

fun SavingsSubmission.toDto(date: String): SavingsTransactionRequestDto = SavingsTransactionRequestDto(
    transactionDate = date,
    transactionAmount = amount,
)

fun RepaymentSubmission.toDto(date: String): LoanRepaymentRequestDto = LoanRepaymentRequestDto(
    transactionDate = date,
    transactionAmount = amount,
)

fun DisbursalSubmission.toDisbursalDto(date: String): LoanDisbursalRequestDto = LoanDisbursalRequestDto(
    actualDisbursementDate = date,
    amount = amount,
)

/**
 * `GET /companion/groups/{groupId}/loan-requests` row -> domain [LoanApplication] for the
 * meeting-conduct review step. [MeetingLoanApplicationDto.id] carries the borrower's clientId so the
 * approve→disburse action (`postLoanDisbursal`) resolves the client from the application id.
 */
fun MeetingLoanApplicationDto.toDomainModel(): LoanApplication = LoanApplication(
    id = id,
    memberId = memberId,
    memberName = memberName,
    requestedAmount = requestedAmount,
    purpose = purpose,
)

/** Batch converter for the pending loan-application list. */
fun List<MeetingLoanApplicationDto>.toLoanApplications(): List<LoanApplication> = map { it.toDomainModel() }

fun MeetingSubmissionRequest.toCorpusUpdateDto(): UpdateCorpusRequestDto = UpdateCorpusRequestDto(
    corpusBalance = closingCorpus,
    lastUpdatedMeeting = meetingNumber,
    lastUpdatedDate = actualDate,
)

/**
 * Serializes the whole submission to the single opaque `sync_queue` payload string
 * (`flow.yaml#submit_meeting.offline`). The sync worker replays the priority-1..6 sequence from it.
 */
fun MeetingSubmissionRequest.toJsonPayload(): String {
    val payload = MeetingSubmissionPayloadDto(
        meetingId = meetingId,
        meetingNumber = meetingNumber,
        groupId = groupId,
        record = toRecordDto(),
        attendance = attendance.map { it.toDto(meetingId) },
        savings = savings.map { MeetingSavingsPayloadDto(it.savingsAccountId, it.toDto(actualDate)) },
        repayments = repayments.map { MeetingRepaymentPayloadDto(it.loanId, it.toDto(actualDate)) },
        disbursals = disbursals.map { MeetingDisbursalPayloadDto(it.loanId, it.toDisbursalDto(actualDate)) },
        corpusUpdate = toCorpusUpdateDto(),
    )
    return meetingJson.encodeToString(MeetingSubmissionPayloadDto.serializer(), payload)
}
