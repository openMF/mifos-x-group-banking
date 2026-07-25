/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.data.repository

import co.touchlab.kermit.Logger
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.model.meeting.LoanVoteRecord
import org.mifos.groupbanking.core.model.meeting.MeetingConductData
import org.mifos.groupbanking.core.model.meeting.MeetingSubmissionRequest
import org.mifos.groupbanking.core.model.meeting.MeetingSubmitResult
import org.mifos.groupbanking.core.network.mapper.toCorpusUpdateDto
import org.mifos.groupbanking.core.network.mapper.toDisbursalDto
import org.mifos.groupbanking.core.network.mapper.toDomainModel
import org.mifos.groupbanking.core.network.mapper.toDto
import org.mifos.groupbanking.core.network.mapper.toGroupMembers
import org.mifos.groupbanking.core.network.mapper.toJsonPayload
import org.mifos.groupbanking.core.network.mapper.toLoanSummaries
import org.mifos.groupbanking.core.network.mapper.toRecordDto
import org.mifos.groupbanking.core.network.service.meetingconduct.MeetingConductApi

private const val TAG = "MeetingConductRepository"
private const val SUBMIT_MEETING_OPERATION_TYPE = "SUBMIT_MEETING"
private const val SUBMIT_MEETING_TARGET_TABLE = "dt_meeting_record"

/**
 * See [MeetingConductRepository] KDoc for the Store5-free branch rationale. No try-catch here —
 * every method is a plain `when`/`coroutineScope` chain over [MeetingConductApi]'s [NetworkResult];
 * the offline enqueue delegates straight to [SyncQueueRepository.enqueue] (local Room, no network),
 * exactly as `ShareOutRepositoryImpl` / `LoanRequestRepositoryImpl` do.
 *
 * See API.md#repositories — MeetingConductRepository.
 */
class MeetingConductRepositoryImpl(
    private val api: MeetingConductApi,
    private val syncQueueRepository: SyncQueueRepository,
) : MeetingConductRepository {

    override suspend fun loadMeetingData(
        centerId: Int,
        groupId: Int,
        meetingNumber: Int,
    ): NetworkResult<MeetingConductData, NetworkError> = coroutineScope {
        Logger.d(TAG) { "loadMeetingData: centerId=$centerId groupId=$groupId meetingNumber=$meetingNumber (4-way parallel)" }

        val previousDeferred = async { api.getPreviousMeetingRecord(centerId) }
        val membersDeferred = async { api.getGroupMembers(centerId) }
        val corpusDeferred = async { api.getGroupCorpus(centerId) }
        val loansDeferred = async { api.getActiveLoans(groupId) }

        val previousResult = previousDeferred.await()
        val membersResult = membersDeferred.await()
        val corpusResult = corpusDeferred.await()
        val loansResult = loansDeferred.await()

        // Members are the only hard-required read — a members failure blocks the wizard
        // (api.yaml#get_group_members: 404 → members required).
        if (membersResult is NetworkResult.Error) {
            Logger.e(TAG) { "loadMeetingData: getGroupMembers failed: ${membersResult.error}" }
            return@coroutineScope membersResult
        }

        // Tolerant reads degrade to their declared empty/zero defaults (api.yaml error branches):
        //   previous 404 → null (first meeting); corpus 404 → 0; active-loans 404 → empty.
        val previousSummary = (previousResult as? NetworkResult.Success)?.data?.toDomainModel()
        val members = (membersResult as NetworkResult.Success).data.toGroupMembers()
        val corpus = (corpusResult as? NetworkResult.Success)?.data
        val activeLoans = (loansResult as? NetworkResult.Success)?.data?.toLoanSummaries().orEmpty()

        val data = MeetingConductData(
            previousMeetingSummary = previousSummary,
            groupMembers = members,
            openingCorpus = corpus?.corpusBalance ?: 0L,
            cashOnHand = corpus?.cashOnHand ?: 0L,
            activeLoans = activeLoans,
            // pendingLoanApplications gap (CFF1) — no api.yaml endpoint loads it; see interface KDoc.
            pendingLoanApplications = emptyList(),
        )
        Logger.i(TAG) {
            "loadMeetingData: succeeded centerId=$centerId members=${members.size} " +
                "openingCorpus=${data.openingCorpus} activeLoans=${activeLoans.size} " +
                "hasPrevious=${previousSummary != null}"
        }
        NetworkResult.Success(data)
    }

    override suspend fun getLoanVotes(loanId: String): NetworkResult<LoanVoteRecord, NetworkError> {
        Logger.d(TAG) { "getLoanVotes: loanId=$loanId" }
        return when (val result = api.getLoanVotes(loanId)) {
            is NetworkResult.Error -> {
                // api.yaml#get_loan_votes: 404 → no votes cast yet (zero tally, not a hard error).
                if (result.error == NetworkError.NOT_FOUND) {
                    Logger.i(TAG) { "getLoanVotes: no votes yet for loanId=$loanId (404 → zero tally)" }
                    NetworkResult.Success(LoanVoteRecord(loanId = loanId, votesFor = 0, votesAgainst = 0, votesAbstain = 0))
                } else {
                    Logger.e(TAG) { "getLoanVotes: failed loanId=$loanId: ${result.error}" }
                    result
                }
            }
            is NetworkResult.Success -> NetworkResult.Success(result.data.toDomainModel())
        }
    }

    override suspend fun submitMeeting(
        request: MeetingSubmissionRequest,
    ): NetworkResult<MeetingSubmitResult, NetworkError> {
        Logger.d(TAG) {
            "submitMeeting: meetingId=${request.meetingId} attendance=${request.attendance.size} " +
                "savings=${request.savings.size} repayments=${request.repayments.size} disbursals=${request.disbursals.size}"
        }

        // Priority 1 — meeting record.
        api.postMeetingRecord(request.toRecordDto()).errorOrNull()?.let {
            Logger.e(TAG) { "submitMeeting: postMeetingRecord failed: $it" }
            return NetworkResult.Error(it)
        }

        // Priority 2 — per-member attendance.
        for (attendance in request.attendance) {
            api.postMeetingAttendance(attendance.toDto(request.meetingId)).errorOrNull()?.let {
                Logger.e(TAG) { "submitMeeting: postMeetingAttendance failed memberId=${attendance.memberId}: $it" }
                return NetworkResult.Error(it)
            }
        }

        // Priority 3 — per-entry savings deposits.
        for (savings in request.savings) {
            api.postSavingsTransaction(savings.savingsAccountId, savings.toDto(request.actualDate)).errorOrNull()?.let {
                Logger.e(TAG) { "submitMeeting: postSavingsTransaction failed savingsId=${savings.savingsAccountId}: $it" }
                return NetworkResult.Error(it)
            }
        }

        // Priority 4 — per-loan repayments.
        for (repayment in request.repayments) {
            api.postLoanRepayment(repayment.loanId, repayment.toDto(request.actualDate)).errorOrNull()?.let {
                Logger.e(TAG) { "submitMeeting: postLoanRepayment failed loanId=${repayment.loanId}: $it" }
                return NetworkResult.Error(it)
            }
        }

        // Priority 5 — per-approved disbursals (corpus-gated in the ViewModel before this call).
        for (disbursal in request.disbursals) {
            api.postLoanDisbursal(disbursal.loanId, disbursal.toDisbursalDto(request.actualDate)).errorOrNull()?.let {
                Logger.e(TAG) { "submitMeeting: postLoanDisbursal failed loanId=${disbursal.loanId}: $it" }
                return NetworkResult.Error(it)
            }
        }

        // Priority 6 — terminal corpus PATCH.
        api.updateCorpus(request.centerId, request.toCorpusUpdateDto()).errorOrNull()?.let {
            Logger.e(TAG) { "submitMeeting: updateCorpus failed: $it" }
            return NetworkResult.Error(it)
        }

        Logger.i(TAG) { "submitMeeting: succeeded meetingId=${request.meetingId} (all 6 priorities posted)" }
        return NetworkResult.Success(MeetingSubmitResult(meetingId = request.meetingId, isOffline = false))
    }

    override suspend fun enqueueMeetingOffline(request: MeetingSubmissionRequest): Long {
        val payloadJson = request.toJsonPayload()
        Logger.i(TAG) { "enqueueMeetingOffline: queuing $SUBMIT_MEETING_OPERATION_TYPE meetingId=${request.meetingId}" }
        return syncQueueRepository.enqueue(
            operationType = SUBMIT_MEETING_OPERATION_TYPE,
            targetTable = SUBMIT_MEETING_TARGET_TABLE,
            payloadJson = payloadJson,
        )
    }
}

/** Returns the [NetworkError] when this result is an error, or `null` on success — the ordered-sequence short-circuit helper. */
private fun <T> NetworkResult<T, NetworkError>.errorOrNull(): NetworkError? =
    (this as? NetworkResult.Error)?.error
