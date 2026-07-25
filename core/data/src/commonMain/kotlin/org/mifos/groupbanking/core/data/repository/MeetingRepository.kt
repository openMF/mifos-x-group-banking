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

import kotlinx.coroutines.CoroutineScope
import kpt.core.base.store.screen.FetchPolicy
import kpt.core.base.store.screen.ScreenDataStream
import org.mifos.groupbanking.core.model.MeetingListItem

/**
 * Read surface for the meeting-calendar screen — the single-key merge of `get_center_meetings` +
 * `get_meeting_records_datatable`.
 *
 * Wraps the single-key NETWORK_WITH_CACHE `MeetingCalendarStore` and exposes exactly one read
 * path — [meetingsStream], an offline-first [ScreenDataStream] of `List<MeetingListItem>` keyed by
 * `centerId`. There is no DAO-bypass read and no write path: the list is read-only
 * (`data-flow.yaml#sync_queue: []`, RULE-IMPLEMENT-STORE5-001 S5-1 / S5-2). No `try-catch`, no
 * `Result<T>` envelope — the stream surfaces `ScreenState` (Loading / Content / NoNetwork / Error /
 * Empty) directly.
 *
 * See API.md#stores — MeetingCalendar.
 */
interface MeetingRepository {

    /**
     * Offline-first stream of the merged scheduled-meetings list for [centerId].
     *
     * The store fires the two reads in parallel and merges them into one `List<MeetingListItem>`; a
     * cached list is served immediately then background-revalidated per the store's
     * stale-while-revalidate policy (`data-flow.yaml`: `stale_while_revalidate`, `ttl=300`,
     * `offline: fallback_cache`). An empty result (no meetings scheduled) surfaces as
     * `ScreenState.Empty`. Call [ScreenDataStream.retry] to re-drive a failed fetch (the Retry CTA);
     * pull-to-refresh maps to a fresh network re-collection.
     *
     * @param centerId The group center whose meetings to stream (nav param).
     * @param scope CoroutineScope (typically `viewModelScope`) for the auto-refresh coroutine.
     * @param fetchPolicy Read policy. Defaults to [FetchPolicy.CACHE_FIRST_SWR] —
     *   stale-while-revalidate, matching the declared cache strategy.
     */
    fun meetingsStream(
        centerId: Int,
        scope: CoroutineScope,
        fetchPolicy: FetchPolicy = FetchPolicy.CACHE_FIRST_SWR,
    ): ScreenDataStream<List<MeetingListItem>>
}
