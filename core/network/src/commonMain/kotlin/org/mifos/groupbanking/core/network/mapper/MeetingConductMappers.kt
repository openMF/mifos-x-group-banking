/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.mapper

import kotlinx.serialization.json.Json
import org.mifos.groupbanking.core.model.meeting.AttendanceSubmission
import org.mifos.groupbanking.core.model.meeting.CorpusRecord
import org.mifos.groupbanking.core.model.meeting.DisbursalSubmission
import org.mifos.groupbanking.core.model.meeting.GroupMember
import org.mifos.groupbanking.core.model.meeting.LoanSummary
import org.mifos.groupbanking.core.model.meeting.LoanVoteRecord
import org.mifos.groupbanking.core.model.meeting.MeetingSubmissionRequest
import org.mifos.groupbanking.core.model.meeting.PreviousMeetingSummary
import org.mifos.groupbanking.core.model.meeting.RepaymentSubmission
import org.mifos.groupbanking.core.model.meeting.SavingsSubmission
import org.mifos.groupbanking.core.network.model.CenterDetailDto
import org.mifos.groupbanking.core.network.model.CorpusRecordDto
import org.mifos.groupbanking.core.network.model.CreateAttendanceRequestDto
import org.mifos.groupbanking.core.network.model.CreateMeetingRecordRequestDto
import org.mifos.groupbanking.core.network.model.MeetingActiveLoanDto
import org.mifos.groupbanking.core.network.model.LoanDisbursalRequestDto
import org.mifos.groupbanking.core.network.model.LoanListResponseDto
import org.mifos.groupbanking.core.network.model.LoanRepaymentRequestDto
import org.mifos.groupbanking.core.network.model.LoanVoteRecordDto
import org.mifos.groupbanking.core.network.model.MeetingDisbursalPayloadDto
import org.mifos.groupbanking.core.network.model.MeetingRecordDetailDto
import org.mifos.groupbanking.core.network.model.MeetingRepaymentPayloadDto
import org.mifos.groupbanking.core.network.model.MeetingSavingsPayloadDto
import org.mifos.groupbanking.core.network.model.MeetingSubmissionPayloadDto
import org.mifos.groupbanking.core.network.model.SavingsTransactionRequestDto
import org.mifos.groupbanking.core.network.model.UpdateCorpusRequestDto

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
    meetingNumber = meetingNumber,
    date = actualDate,
    totalCollected = totalSavings + totalRepayments + totalFinesCollected,
    corpusAtClose = closingCorpus,
    attendanceCount = attendanceCount,
)

fun CenterDetailDto.toGroupMembers(): List<GroupMember> = activeClientMembers.map { member ->
    GroupMember(
        memberId = member.id.toString(),
        name = member.displayName,
        initials = initialsOf(member.displayName),
        role = member.role ?: "Member",
        savingsAccountId = member.savingsAccountId,
    )
}

fun CorpusRecordDto.toDomainModel(): CorpusRecord = CorpusRecord(
    centerId = centerId,
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
    centerId = centerId,
    meetingNumber = meetingNumber,
    actualDate = actualDate,
    openingCorpus = openingCorpus,
    closingCorpus = closingCorpus,
    totalSavingsCollected = totalSavingsCollected,
    totalRepaymentsReceived = totalRepaymentsReceived,
    totalLoansDisbursed = totalLoansDisbursed,
    totalFinesCollected = totalFinesCollected,
    attendanceCount = attendanceCount,
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
)

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
        centerId = centerId,
        record = toRecordDto(),
        attendance = attendance.map { it.toDto(meetingId) },
        savings = savings.map { MeetingSavingsPayloadDto(it.savingsAccountId, it.toDto(actualDate)) },
        repayments = repayments.map { MeetingRepaymentPayloadDto(it.loanId, it.toDto(actualDate)) },
        disbursals = disbursals.map { MeetingDisbursalPayloadDto(it.loanId, it.toDisbursalDto(actualDate)) },
        corpusUpdate = toCorpusUpdateDto(),
    )
    return meetingJson.encodeToString(MeetingSubmissionPayloadDto.serializer(), payload)
}
