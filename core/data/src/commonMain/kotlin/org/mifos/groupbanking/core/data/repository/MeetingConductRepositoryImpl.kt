/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.data.repository

import co.touchlab.kermit.Logger
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.model.MeetingSummaryData
import org.mifos.groupbanking.core.model.meeting.LoanVoteRecord
import org.mifos.groupbanking.core.model.meeting.MeetingConductData
import org.mifos.groupbanking.core.model.meeting.MeetingSubmissionRequest
import org.mifos.groupbanking.core.model.meeting.MeetingSubmitResult
import org.mifos.groupbanking.core.model.meeting.SavingsType
import org.mifos.groupbanking.core.network.mapper.toCorpusUpdateDto
import org.mifos.groupbanking.core.network.mapper.toDisbursalDto
import org.mifos.groupbanking.core.network.mapper.toDomainModel
import org.mifos.groupbanking.core.network.mapper.toDto
import org.mifos.groupbanking.core.network.mapper.toGroupMembers
import org.mifos.groupbanking.core.network.mapper.toJsonPayload
import org.mifos.groupbanking.core.network.mapper.toLoanApplications
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
    private val meetingSummaryRepository: MeetingSummaryRepository,
) : MeetingConductRepository {

    override suspend fun loadMeetingData(
        groupId: Int,
        meetingNumber: Int,
    ): NetworkResult<MeetingConductData, NetworkError> = coroutineScope {
        Logger.d(TAG) { "loadMeetingData: groupId=$groupId meetingNumber=$meetingNumber (5-way parallel)" }

        val previousDeferred = async { api.getPreviousMeetingRecord(groupId) }
        val membersDeferred = async { api.getGroupMembers(groupId) }
        val corpusDeferred = async { api.getGroupCorpus(groupId) }
        val loansDeferred = async { api.getActiveLoans(groupId) }
        val pendingAppsDeferred = async { api.getPendingLoanApplications(groupId) }

        val previousResult = previousDeferred.await()
        val membersResult = membersDeferred.await()
        val corpusResult = corpusDeferred.await()
        val loansResult = loansDeferred.await()
        val pendingAppsResult = pendingAppsDeferred.await()

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
        // Tolerant read (companion GET /companion/groups/{groupId}/loan-requests) — a failure/404
        // degrades to no pending applications rather than blocking the wizard.
        val pendingApplications = (pendingAppsResult as? NetworkResult.Success)?.data?.toLoanApplications().orEmpty()

        val data = MeetingConductData(
            previousMeetingSummary = previousSummary,
            groupMembers = members,
            openingCorpus = corpus?.corpusBalance ?: 0L,
            cashOnHand = corpus?.cashOnHand ?: 0L,
            activeLoans = activeLoans,
            // CFF1 gap closed — loaded from the companion group loan-requests facade.
            pendingLoanApplications = pendingApplications,
        )
        Logger.i(TAG) {
            "loadMeetingData: succeeded groupId=$groupId members=${members.size} " +
                "openingCorpus=${data.openingCorpus} activeLoans=${activeLoans.size} " +
                "pendingApplications=${pendingApplications.size} hasPrevious=${previousSummary != null}"
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

        // Ordered, lazily-evaluated write steps (Priority 1 record → 2 attendance → 3 savings →
        // 4 repayments → 5 disbursals → 6 corpus PATCH). `firstNotNullOfOrNull` runs them in order
        // and stops at the FIRST failure — the same sequential short-circuit as before, now with a
        // single Error/Success return pair. Each step logs its own failure before surfacing it.
        val steps: List<suspend () -> NetworkError?> = buildList {
            add {
                api.postMeetingRecord(request.toRecordDto()).errorOrNull()
                    ?.also { Logger.e(TAG) { "submitMeeting: postMeetingRecord failed: $it" } }
            }
            request.attendance.forEach { attendance ->
                add {
                    api.postMeetingAttendance(attendance.toDto(request.meetingId)).errorOrNull()
                        ?.also { Logger.e(TAG) { "submitMeeting: postMeetingAttendance failed memberId=${attendance.memberId}: $it" } }
                }
            }
            request.savings.forEach { savings ->
                add {
                    api.postSavingsTransaction(savings.savingsAccountId, savings.toDto(request.actualDate)).errorOrNull()
                        ?.also { Logger.e(TAG) { "submitMeeting: postSavingsTransaction failed savingsId=${savings.savingsAccountId}: $it" } }
                }
            }
            request.repayments.forEach { repayment ->
                add {
                    api.postLoanRepayment(repayment.loanId, repayment.toDto(request.actualDate)).errorOrNull()
                        ?.also { Logger.e(TAG) { "submitMeeting: postLoanRepayment failed loanId=${repayment.loanId}: $it" } }
                }
            }
            request.disbursals.forEach { disbursal ->
                add {
                    api.postLoanDisbursal(disbursal.loanId, disbursal.toDisbursalDto(request.actualDate)).errorOrNull()
                        ?.also { Logger.e(TAG) { "submitMeeting: postLoanDisbursal failed loanId=${disbursal.loanId}: $it" } }
                }
            }
            add {
                api.updateCorpus(request.groupId, request.toCorpusUpdateDto()).errorOrNull()
                    ?.also { Logger.e(TAG) { "submitMeeting: updateCorpus failed: $it" } }
            }
        }

        steps.firstNotNullOfOrNull { it() }?.let { return NetworkResult.Error(it) }

        Logger.i(TAG) { "submitMeeting: succeeded meetingId=${request.meetingId} (all 6 priorities posted)" }
        // Write-through the just-submitted record into the summary Store's local cache so the summary
        // screen is offline-first (renders the submitted totals instantly, no dependence on a fresh
        // fetch, and overwrites any poisoned pre-conduct 404 zero-row). Best-effort: a local cache
        // write must never turn a fully-posted submit into a failure.
        primeSummaryCache(request)
        return NetworkResult.Success(MeetingSubmitResult(meetingId = request.meetingId, isOffline = false))
    }

    override suspend fun enqueueMeetingOffline(request: MeetingSubmissionRequest): Long {
        val payloadJson = request.toJsonPayload()
        Logger.i(TAG) { "enqueueMeetingOffline: queuing $SUBMIT_MEETING_OPERATION_TYPE meetingId=${request.meetingId}" }
        val queuedId = syncQueueRepository.enqueue(
            operationType = SUBMIT_MEETING_OPERATION_TYPE,
            targetTable = SUBMIT_MEETING_TARGET_TABLE,
            payloadJson = payloadJson,
        )
        // Same optimistic write-through as the online path — the summary must render the submitted
        // totals offline while the payload waits in the sync queue.
        primeSummaryCache(request)
        return queuedId
    }

    /**
     * Best-effort offline-first cache prime: build the summary snapshot from the submitted request
     * and write it into the summary Store's SourceOfTruth (keyed by `groupId:meetingNumber`). Wrapped
     * so a local persistence hiccup never propagates out of a successful submit/enqueue.
     */
    private suspend fun primeSummaryCache(request: MeetingSubmissionRequest) {
        runCatching {
            meetingSummaryRepository.primeSubmittedSummary(
                groupId = request.groupId,
                meetingNumber = request.meetingNumber,
                data = request.toSummaryData(),
            )
        }.onFailure { Logger.w(TAG) { "primeSummaryCache: cache prime failed meetingId=${request.meetingId}: ${it.message}" } }
    }
}

/**
 * Builds the offline-first [MeetingSummaryData] snapshot from the just-submitted request. Scalar
 * totals come straight from the request; the group/individual split is summed from the per-member
 * savings lines. The per-member `savingsBreakdown` / `loanItems` are left empty here (the request
 * carries member ids but not display names) — they reconcile from the server on the next successful
 * online revalidate, which is exactly the offline-first stale-while-revalidate contract.
 */
private fun MeetingSubmissionRequest.toSummaryData(): MeetingSummaryData {
    // The per-member savings LINES only exist for members with a Fineract savings account, so in a
    // group with no member savings accounts `savings` is empty even though the running
    // `totalSavingsCollected` (all members) is non-zero. When the split is unavailable, attribute
    // the whole running total to GROUP savings — mirroring the companion handleMeetingSummary
    // fallback so the primed cache and the server-revalidated projection agree.
    val groupFromLines = savings.filter { it.type == SavingsType.GROUP_LINKED }.sumOf { it.amount }
    val individualFromLines = savings.filter { it.type == SavingsType.INDIVIDUAL }.sumOf { it.amount }
    val hasLineSplit = groupFromLines > 0L || individualFromLines > 0L
    return MeetingSummaryData(
        meetingId = meetingId,
        meetingNumber = meetingNumber,
        actualDate = actualDate,
        meetingTime = completedTime,
        attendanceCount = attendanceCount,
        totalMemberCount = attendance.size,
        groupSavingsCollected = if (hasLineSplit) groupFromLines else totalSavingsCollected,
        individualSavingsCollected = individualFromLines,
        totalSavingsCollected = totalSavingsCollected,
        loansDisbursed = totalLoansDisbursed,
        loansRepaid = totalRepaymentsReceived,
        finesCollected = totalFinesCollected,
        openingCorpus = openingCorpus,
        closingCorpus = closingCorpus,
        savingsBreakdown = emptyList(),
        loanItems = emptyList(),
    )
}

/** Returns the [NetworkError] when this result is an error, or `null` on success — the ordered-sequence short-circuit helper. */
private fun <T> NetworkResult<T, NetworkError>.errorOrNull(): NetworkError? =
    (this as? NetworkResult.Error)?.error
