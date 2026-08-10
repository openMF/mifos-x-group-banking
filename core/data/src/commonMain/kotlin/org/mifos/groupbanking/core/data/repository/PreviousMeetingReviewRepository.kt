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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kpt.core.base.store.screen.ScreenState
import org.mifos.groupbanking.core.model.PreviousMeetingDetail

/**
 * Read surface for `previous-meeting-review-screen` (FR-019) — the offline-first recap of a CLOSED
 * meeting.
 *
 * This is a COMPOSITE read that reuses the existing single-key `MeetingSummaryStore` (record totals
 * + per-member savings + per-member loans, via [MeetingSummaryRepository]) and merges it with the
 * review-only per-member attendance roster (a separate single-key `MeetingAttendanceStore`,
 * `GET /datatables/dt_meeting_attendance/{meetingId}`). The two offline-first reads are combined into
 * one [PreviousMeetingDetail] `ScreenState` stream; the [PreviousMeetingDetail.unresolvedItems] are
 * derived from the attendance roster (unpaid fines). The screen is read-only
 * (`data-flow.yaml#sync_queue: []`, RULE-IMPLEMENT-STORE5-001 S5-1 / S5-2) — no write path, no
 * `try-catch`, no `Result<T>` envelope.
 *
 * See API.md#stores — PreviousMeetingDetail.
 */
interface PreviousMeetingReviewRepository {

    /**
     * Offline-first combined stream of the previous-meeting recap for (`groupId`, `meetingNumber`,
     * `meetingId`). The reused record read is scoped by (`groupId`, `meetingNumber`); the attendance
     * read is scoped by `meetingId`. Both serve their per-key cache immediately then
     * background-revalidate per each store's stale-while-revalidate policy. The returned
     * [PreviousMeetingReviewStream] exposes the merged [state] the ViewModel collects plus a [retry]
     * that re-drives BOTH underlying reads (the error-state Retry CTA).
     *
     * @param groupId The group the completed meeting belongs to (record store key part 1).
     * @param meetingNumber The completed meeting's sequence number (record store key part 2).
     * @param meetingId The completed meeting id (attendance store key + display/analytics).
     * @param scope CoroutineScope (typically `viewModelScope`) for the auto-refresh coroutines.
     */
    fun previousMeetingStream(
        groupId: Int,
        meetingNumber: Int,
        meetingId: String,
        scope: CoroutineScope,
    ): PreviousMeetingReviewStream
}

/**
 * Holder for the merged previous-meeting-review read — the combined offline-first [state] the
 * ViewModel collects plus a [retry] that re-drives both the reused record read and the attendance
 * read. Mirrors the shape a `ScreenDataStream` exposes (`state` + `retry`), but purpose-built here
 * because the review screen fans TWO independent reads into one projection.
 *
 * See API.md#stores — PreviousMeetingDetail.
 */
class PreviousMeetingReviewStream(
    val state: Flow<ScreenState<PreviousMeetingDetail>>,
    private val onRetry: () -> Unit,
) {
    /** Re-drives both underlying offline-first reads (error-state Retry CTA). */
    fun retry() = onRetry()
}
