/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.network.service.meetingconduct

import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.network.model.CorpusRecordDto
import kpt.core.network.model.CreateAttendanceRequestDto
import kpt.core.network.model.CreateMeetingRecordRequestDto
import kpt.core.network.model.DataTableEntryResponseDto
import kpt.core.network.model.GroupMembersDetailDto
import kpt.core.network.model.LoanDisbursalRequestDto
import kpt.core.network.model.LoanListResponseDto
import kpt.core.network.model.LoanRepaymentRequestDto
import kpt.core.network.model.LoanVoteRecordDto
import kpt.core.network.model.MeetingLoanApplicationDto
import kpt.core.network.model.MeetingRecordDetailDto
import kpt.core.network.model.SavingsTransactionRequestDto
import kpt.core.network.model.UpdateCorpusRequestDto

/**
 * Ktor client for the `meeting-conduct` wizard — the 11 Fineract collection-sheet endpoints
 * (`idea-layer/screens/meeting-conduct/api.yaml#api`). Returns [NetworkResult] — never a raw
 * [Result] envelope, never a thrown exception (Mandatory Rule 2). `NetworkResult`/`NetworkError` are
 * the framework's `core-base/network` sealed types (consumed, never edited — Hard Rule #8). This
 * Service is SERVICE-ONLY; the 5-way on-mount parallel combine + the ordered submit orchestration
 * live in `MeetingConductRepository` (`core/data`). See API.md#services.
 */
interface MeetingConductApi {

    /** `GET /datatables/dt_meeting_record/{groupId}` (`get_previous_meeting_record`). 404 → first meeting. */
    suspend fun getPreviousMeetingRecord(groupId: Int): NetworkResult<MeetingRecordDetailDto, NetworkError>

    /** `GET /groups/{groupId}?associations=clientMembers,groupRoles` (`get_group_members`). 404 → members required (blocks advance). */
    suspend fun getGroupMembers(groupId: Int): NetworkResult<GroupMembersDetailDto, NetworkError>

    /** `GET /datatables/dt_group_corpus/{groupId}` (`get_group_corpus`). 404 → default to 0. */
    suspend fun getGroupCorpus(groupId: Int): NetworkResult<CorpusRecordDto, NetworkError>

    /** `GET /loans?groupId={groupId}&loanStatus=active` (`get_active_loans`). 404 → empty step 4. */
    suspend fun getActiveLoans(groupId: Int): NetworkResult<LoanListResponseDto, NetworkError>

    /** `GET /datatables/dt_loan_vote/{loanId}` (`get_loan_votes`). 404 → no votes yet. */
    suspend fun getLoanVotes(loanId: String): NetworkResult<LoanVoteRecordDto, NetworkError>

    /**
     * `GET /companion/groups/{groupId}/loan-requests` (`get_pending_loan_applications`). The group's
     * PENDING loan requests for the meeting-conduct review step (step 5). 404/empty → no applications.
     * Closes the CFF1 gap: this list was previously hard-coded empty in the repository.
     */
    suspend fun getPendingLoanApplications(groupId: Int): NetworkResult<List<MeetingLoanApplicationDto>, NetworkError>

    /** `POST /datatables/dt_meeting_record` (`post_meeting_record`, priority 1). */
    suspend fun postMeetingRecord(request: CreateMeetingRecordRequestDto): NetworkResult<DataTableEntryResponseDto, NetworkError>

    /** `POST /datatables/dt_meeting_attendance` (`post_meeting_attendance`, priority 2). */
    suspend fun postMeetingAttendance(request: CreateAttendanceRequestDto): NetworkResult<DataTableEntryResponseDto, NetworkError>

    /** `POST /savingsaccounts/{savingsId}/transactions` (`post_savings_transaction`, priority 3). */
    suspend fun postSavingsTransaction(
        savingsId: String,
        request: SavingsTransactionRequestDto,
    ): NetworkResult<DataTableEntryResponseDto, NetworkError>

    /** `POST /loans/{loanId}/transactions?command=repayment` (`post_loan_repayment`, priority 4). */
    suspend fun postLoanRepayment(
        loanId: String,
        request: LoanRepaymentRequestDto,
    ): NetworkResult<DataTableEntryResponseDto, NetworkError>

    /**
     * `POST /companion/loan-applications/{clientId}/disburse` (`post_loan_disbursal`, priority 5).
     * [loanId] is the application id (== the borrower's clientId from [getPendingLoanApplications]).
     * The companion materialises the approved request into a live Fineract loan (create → approve →
     * disburse), so this is NOT a raw Fineract loan-transaction disburse (an application has no
     * Fineract loan yet). See `HandleDisburseLoanApplication`.
     */
    suspend fun postLoanDisbursal(
        loanId: String,
        request: LoanDisbursalRequestDto,
    ): NetworkResult<DataTableEntryResponseDto, NetworkError>

    /** `PUT /datatables/dt_group_corpus/{groupId}` (`patch_corpus`, priority 6). */
    suspend fun updateCorpus(
        groupId: Int,
        request: UpdateCorpusRequestDto,
    ): NetworkResult<DataTableEntryResponseDto, NetworkError>
}
