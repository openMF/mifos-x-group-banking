/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.data.repository

import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.model.meeting.LoanVoteRecord
import kpt.core.model.meeting.MeetingConductData
import kpt.core.model.meeting.MeetingSubmissionRequest
import kpt.core.model.meeting.MeetingSubmitResult

/**
 * Repository seam for the `meeting-conduct` wizard (`idea-layer/screens/meeting-conduct`).
 *
 * **Store5-free (composite/form-submit branch).** `business_logic.kind: composite` with no
 * `AppStoreRegistry` entry — the on-mount reads surface [NetworkResult] directly (5-way parallel
 * combine into [MeetingConductData]) and the ordered submit posts through [submitMeeting], exactly
 * the same branch as `LoanApplyRepository` / `ShareOutRepository`. The offline path
 * ([enqueueMeetingOffline]) delegates to the shared [SyncQueueRepository] Room write-queue
 * (`flow.yaml#submit_meeting.offline`, key `meeting_id`) — nothing here imports
 * `org.mobilenativefoundation.store`.
 *
 * See API.md#repositories — MeetingConductRepository.
 */
interface MeetingConductRepository {

    /**
     * On-mount 5-way parallel fan-in (`flow.yaml#flow_logic.screen_init.parallel_load`):
     * `get_previous_meeting_record` (404 → null, first meeting) + `get_group_members` (required) +
     * `get_group_corpus` (404 → 0) + `get_active_loans` (404 → empty). Only a members-read failure
     * is fatal; the tolerant reads degrade to their empty/zero defaults.
     *
     * **`pendingLoanApplications` (CFF1 gap CLOSED):** now loaded as a 5th tolerant parallel read
     * via `MeetingConductApi.getPendingLoanApplications` (companion
     * `GET /companion/groups/{groupId}/loan-requests`) — the group's PENDING loan requests for the
     * step-5 review. A failure/404 degrades to empty (never blocks the wizard). Approving one and
     * submitting the meeting materialises it into a live Fineract loan via
     * `postLoanDisbursal` → companion `POST /companion/loan-applications/{clientId}/disburse`.
     */
    suspend fun loadMeetingData(
        groupId: Int,
        meetingNumber: Int,
    ): NetworkResult<MeetingConductData, NetworkError>

    /** `GET /datatables/dt_loan_vote/{loanId}` (`get_loan_votes`, on_interact). 404 → zero tally. */
    suspend fun getLoanVotes(loanId: String): NetworkResult<LoanVoteRecord, NetworkError>

    /**
     * Ordered online submit sequence (`flow.yaml#submit_meeting.online`, `data-flow.yaml` priority
     * 1..6): `post_meeting_record` → per-member `post_meeting_attendance` → per-entry
     * `post_savings_transaction` → per-loan `post_loan_repayment` → per-approved `post_loan_disbursal`
     * → `patch_corpus`. First-failure-wins; the ViewModel falls back to [enqueueMeetingOffline] on any
     * error (`flow.yaml#submit_meeting.online.on_any_5xx: fallthrough_to_offline_path`).
     */
    suspend fun submitMeeting(request: MeetingSubmissionRequest): NetworkResult<MeetingSubmitResult, NetworkError>

    /**
     * Enqueue the whole meeting payload to the `sync_queue` for later drain
     * (`flow.yaml#submit_meeting.offline.enqueue_all_payloads_to_sync_queue`). Returns the queue row id.
     */
    suspend fun enqueueMeetingOffline(request: MeetingSubmissionRequest): Long
}
