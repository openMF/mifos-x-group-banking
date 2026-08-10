/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.network.service.meetingsummary

import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.network.model.MeetingSummaryRecordDto

/**
 * Ktor client for the meeting-summary feature — the single composite read that resolves one
 * completed meeting record (persisted totals + per-member savings breakdown + per-member loan
 * activity) for a group/meeting pair in one round trip. See
 * `idea-layer/screens/meeting-summary/api.yaml#api[get_meeting_record]` +
 * `data-flow.yaml#cache.strategy` (`stale_while_revalidate`, ttl=600) + API.md#services for the
 * endpoint contract.
 *
 * Returns [NetworkResult] — never a raw [Result] envelope, never a thrown exception.
 * `NetworkResult`/`NetworkError` are the framework's `core-base/network` sealed types (consumed,
 * never edited). The downstream Store5 wrapper and its Repository switch on the sealed result /
 * surface `ScreenState` directly, with no try-catch of their own — this Service is SERVICE-ONLY.
 */
interface MeetingRecordApi {

    /**
     * `GET /fineract-provider/api/v1/datatables/dt_meeting_record/{groupId}?meetingNumber=N`
     * (`api.yaml#api[get_meeting_record]`). Fetches the completed meeting record for [meetingNumber]
     * in group [groupId]. 401 -> [NetworkError.UNAUTHORIZED]; 404 -> [NetworkError.NOT_FOUND]
     * ("meeting record not found" — the screen falls back to cached data); 5xx ->
     * [NetworkError.SERVER].
     */
    suspend fun getMeetingRecord(
        groupId: Int,
        meetingNumber: Int,
    ): NetworkResult<MeetingSummaryRecordDto, NetworkError>
}
