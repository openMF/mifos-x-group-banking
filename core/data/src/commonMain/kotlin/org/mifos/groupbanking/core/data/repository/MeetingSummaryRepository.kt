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
import kpt.core.base.store.screen.FetchPolicy
import kpt.core.base.store.screen.ScreenDataStream
import org.mifos.groupbanking.core.model.MeetingSummaryData

/**
 * Read surface for the meeting-summary composite view
 * (`GET /datatables/dt_meeting_record/{groupId}?meetingNumber=N`) backing the read-only
 * post-meeting summary screen.
 *
 * Wraps the single-key NETWORK_WITH_CACHE `MeetingSummaryStore` and exposes exactly one read path —
 * [meetingSummaryStream], an offline-first [ScreenDataStream] of `MeetingSummaryData` (persisted
 * totals + per-member savings breakdown + per-member loan activity) keyed by
 * (`groupId`, `meetingNumber`). The meeting-summary screen is read-only
 * (RULE-IMPLEMENT-STORE5-001 S5-1 / S5-2): no DAO-bypass read, no write path, no `try-catch`, no
 * `Result<T>` envelope — the stream surfaces `ScreenState` directly.
 *
 * See API.md#stores — MeetingSummaryData.
 */
interface MeetingSummaryRepository {

    /**
     * Offline-first stream of the composite meeting summary for (`groupId`, `meetingNumber`).
     *
     * Each meeting is cached independently, so re-opening a completed meeting serves that meeting's
     * per-key cache immediately then background-revalidates per the store's stale-while-revalidate
     * policy (`data-flow.yaml#cache.strategy`: `stale_while_revalidate`, `ttl_seconds=600`, offline
     * fallback_cache). Call [ScreenDataStream.retry] to re-drive a failed fetch.
     *
     * @param groupId The group the completed meeting belongs to (store key part 1).
     * @param meetingNumber The completed meeting's sequence number (store key part 2).
     * @param scope CoroutineScope (typically `viewModelScope`) for the auto-refresh coroutine.
     * @param fetchPolicy Read policy. Defaults to [FetchPolicy.CACHE_FIRST_SWR] —
     *   stale-while-revalidate, matching the declared cache strategy.
     */
    fun meetingSummaryStream(
        groupId: Int,
        meetingNumber: Int,
        scope: CoroutineScope,
        fetchPolicy: FetchPolicy = FetchPolicy.CACHE_FIRST_SWR,
    ): ScreenDataStream<MeetingSummaryData>

    /**
     * Offline-first cache prime, called from the meeting-conduct SUBMIT (input) side — on a
     * successful online post OR an offline enqueue — so the just-submitted [data] is written into
     * the summary Store's SourceOfTruth BEFORE the summary screen opens. This is NOT a user-facing
     * mutation of the read-only summary (there is still no domain write path, S5-1); it is the
     * documented in-memory→cache hand-off that lets the summary render the submitted totals whether
     * the device is online or offline, and overwrites any poisoned zero-row a pre-conduct 404 view
     * cached. The next successful online revalidate reconciles the full server projection.
     */
    suspend fun primeSubmittedSummary(groupId: Int, meetingNumber: Int, data: MeetingSummaryData)
}
