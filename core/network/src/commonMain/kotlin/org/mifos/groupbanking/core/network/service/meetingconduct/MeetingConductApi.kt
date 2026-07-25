/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.service.meetingconduct

import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.network.model.CenterDetailDto
import org.mifos.groupbanking.core.network.model.CorpusRecordDto
import org.mifos.groupbanking.core.network.model.CreateAttendanceRequestDto
import org.mifos.groupbanking.core.network.model.CreateMeetingRecordRequestDto
import org.mifos.groupbanking.core.network.model.DataTableEntryResponseDto
import org.mifos.groupbanking.core.network.model.LoanDisbursalRequestDto
import org.mifos.groupbanking.core.network.model.LoanListResponseDto
import org.mifos.groupbanking.core.network.model.LoanRepaymentRequestDto
import org.mifos.groupbanking.core.network.model.LoanVoteRecordDto
import org.mifos.groupbanking.core.network.model.MeetingRecordDetailDto
import org.mifos.groupbanking.core.network.model.SavingsTransactionRequestDto
import org.mifos.groupbanking.core.network.model.UpdateCorpusRequestDto

/**
 * Ktor client for the `meeting-conduct` wizard — the 11 Fineract collection-sheet endpoints
 * (`idea-layer/screens/meeting-conduct/api.yaml#api`). Returns [NetworkResult] — never a raw
 * [Result] envelope, never a thrown exception (Mandatory Rule 2). `NetworkResult`/`NetworkError` are
 * the framework's `core-base/network` sealed types (consumed, never edited — Hard Rule #8). This
 * Service is SERVICE-ONLY; the 5-way on-mount parallel combine + the ordered submit orchestration
 * live in `MeetingConductRepository` (`core/data`). See API.md#services.
 */
interface MeetingConductApi {

    /** `GET /datatables/dt_meeting_record/{centerId}` (`get_previous_meeting_record`). 404 → first meeting. */
    suspend fun getPreviousMeetingRecord(centerId: Int): NetworkResult<MeetingRecordDetailDto, NetworkError>

    /** `GET /centers/{centerId}` (`get_group_members`). 404 → members required (blocks advance). */
    suspend fun getGroupMembers(centerId: Int): NetworkResult<CenterDetailDto, NetworkError>

    /** `GET /datatables/dt_group_corpus/{centerId}` (`get_group_corpus`). 404 → default to 0. */
    suspend fun getGroupCorpus(centerId: Int): NetworkResult<CorpusRecordDto, NetworkError>

    /** `GET /loans?groupId={groupId}&loanStatus=active` (`get_active_loans`). 404 → empty step 4. */
    suspend fun getActiveLoans(groupId: Int): NetworkResult<LoanListResponseDto, NetworkError>

    /** `GET /datatables/dt_loan_vote/{loanId}` (`get_loan_votes`). 404 → no votes yet. */
    suspend fun getLoanVotes(loanId: String): NetworkResult<LoanVoteRecordDto, NetworkError>

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

    /** `POST /loans/{loanId}/transactions?command=disburse` (`post_loan_disbursal`, priority 5). */
    suspend fun postLoanDisbursal(
        loanId: String,
        request: LoanDisbursalRequestDto,
    ): NetworkResult<DataTableEntryResponseDto, NetworkError>

    /** `PUT /datatables/dt_group_corpus/{centerId}` (`patch_corpus`, priority 6). */
    suspend fun updateCorpus(
        centerId: Int,
        request: UpdateCorpusRequestDto,
    ): NetworkResult<DataTableEntryResponseDto, NetworkError>
}
