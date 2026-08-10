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
import org.mifos.groupbanking.core.model.MeetingListItem
import org.mifos.groupbanking.core.model.RescheduleMeetingRequest

/**
 * Read surface for the meeting-calendar screen — the single-key merge of `get_meeting_schedule` +
 * `get_meeting_records_datatable`.
 *
 * Wraps the single-key NETWORK_WITH_CACHE `MeetingCalendarStore` and exposes exactly one read
 * path — [meetingsStream], an offline-first [ScreenDataStream] of `List<MeetingListItem>` keyed by
 * `groupId`. There is no DAO-bypass read and no write path: the list is read-only
 * (`data-flow.yaml#sync_queue: []`, RULE-IMPLEMENT-STORE5-001 S5-1 / S5-2). No `try-catch`, no
 * `Result<T>` envelope — the stream surfaces `ScreenState` (Loading / Content / NoNetwork / Error /
 * Empty) directly.
 *
 * See API.md#stores — MeetingCalendar.
 */
interface MeetingRepository {

    /**
     * Offline-first stream of the merged scheduled-meetings list for [groupId].
     *
     * The store fires the two reads in parallel and merges them into one `List<MeetingListItem>`; a
     * cached list is served immediately then background-revalidated per the store's
     * stale-while-revalidate policy (`data-flow.yaml`: `stale_while_revalidate`, `ttl=300`,
     * `offline: fallback_cache`). An empty result (no meetings scheduled) surfaces as
     * `ScreenState.Empty`. Call [ScreenDataStream.retry] to re-drive a failed fetch (the Retry CTA);
     * pull-to-refresh maps to a fresh network re-collection.
     *
     * @param groupId The group whose meetings to stream (nav param).
     * @param scope CoroutineScope (typically `viewModelScope`) for the auto-refresh coroutine.
     * @param fetchPolicy Read policy. Defaults to [FetchPolicy.CACHE_FIRST_SWR] —
     *   stale-while-revalidate, matching the declared cache strategy.
     */
    fun meetingsStream(
        groupId: Int,
        scope: CoroutineScope,
        fetchPolicy: FetchPolicy = FetchPolicy.CACHE_FIRST_SWR,
    ): ScreenDataStream<List<MeetingListItem>>

    /**
     * Adjust the group's recurring meeting schedule (G3 / F6, `ui.yaml#schedule_confirm_btn`, action
     * `RescheduleMeeting`). Enqueues the [request] to the shared offline `sync_queue` for later drain
     * and returns the generated queue row id.
     *
     * **Server-gated — the live network PUT is pending-device-verify.** The mapped companion tool
     * `companion_update_calendar` (`PUT /groups/{groupId}/calendars/{calendarId}?command=updateCalendar`,
     * mirrored to `dt_group_config`) is not deployed, so this method does NOT attempt a network call.
     * It offline-queues the payload (operation `UPDATE_MEETING_CALENDAR`, table `dt_group_config`) so the
     * write is durable and replays when the companion API + connectivity are available. Offline-first by
     * construction — matches the `MeetingConductRepository.enqueueMeetingOffline` sync-queue seam.
     */
    suspend fun rescheduleMeeting(request: RescheduleMeetingRequest): Long
}
