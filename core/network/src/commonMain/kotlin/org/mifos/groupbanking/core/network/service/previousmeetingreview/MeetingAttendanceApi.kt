/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.service.previousmeetingreview

import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.network.model.MeetingAttendanceRowDto

/**
 * Ktor client for the previous-meeting-review feature's per-member attendance read — the separate
 * `dt_meeting_attendance` datatable that the review screen merges with the reused meeting record
 * (`previous-meeting-review/api.yaml#api[get_meeting_attendance]`, `data-flow.yaml`
 * `stale_while_revalidate`, ttl=300). See API.md#services.
 *
 * Returns [NetworkResult] — never a raw [Result] envelope, never a thrown exception.
 * `NetworkResult`/`NetworkError` are the framework's `core-base/network` sealed types (consumed,
 * never edited). The downstream Store5 wrapper + its Repository switch on the sealed result / surface
 * `ScreenState` directly, with no try-catch of their own — this Service is SERVICE-ONLY (mirrors
 * `MeetingRecordApi`).
 */
interface MeetingAttendanceApi {

    /**
     * `GET /fineract-provider/api/v1/datatables/dt_meeting_attendance/{meetingId}`
     * (`api.yaml#api[get_meeting_attendance]`). Fetches every per-member attendance row for the
     * completed meeting [meetingId]. 404 -> [NetworkError.NOT_FOUND] (the review screen treats a
     * missing attendance table as an empty list — `data-flow.yaml` `404: Empty attendance list`);
     * 401 -> [NetworkError.UNAUTHORIZED]; 5xx -> [NetworkError.SERVER].
     */
    suspend fun getMeetingAttendance(
        meetingId: String,
    ): NetworkResult<List<MeetingAttendanceRowDto>, NetworkError>
}
