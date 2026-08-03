/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.service.meetingcalendar

import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.network.model.MeetingListItemDto
import org.mifos.groupbanking.core.network.model.MeetingRecordListDto

/**
 * Ktor client for the meeting-calendar read path — the TWO independent Fineract reads the
 * meeting-calendar screen fires in parallel on mount/refresh
 * (`idea-layer/screens/meeting-calendar/api.yaml#api` + `data-flow.yaml#entries`):
 * the scheduled meetings list for the group center ([getCenterMeetings]) and the completed-meeting
 * financial records datatable ([getMeetingRecords]) that enriches past-meeting rows.
 *
 * Every method returns [NetworkResult] — never a raw [Result] envelope, never a thrown exception
 * (Mandatory Rule 2). `NetworkResult`/`NetworkError` are the framework's `core-base/network` sealed
 * types (consumed, never edited). This Service is SERVICE-ONLY — the 2-way merge into the domain
 * `List<MeetingListItem>` and the Store5 wrapper (`.asScreenStream()`) live downstream in
 * `core/store` + `core/data`, not here.
 */
interface MeetingApi {

    /**
     * `GET /fineract-provider/api/v1/centers/{centerId}/meetings`. Fetches the scheduled meetings
     * (upcoming + past) for the group center. 404 -> [NetworkError.NOT_FOUND] (surfaced as an empty
     * state by the store), 401 -> [NetworkError.UNAUTHORIZED], 5xx -> [NetworkError.SERVER]
     * (cache-fallback in the store).
     */
    suspend fun getCenterMeetings(centerId: Int): NetworkResult<List<MeetingListItemDto>, NetworkError>

    /**
     * `GET /fineract-provider/api/v1/datatables/dt_meeting_record/{centerId}`. Fetches the
     * completed-meeting financial records (attendance + collected amounts) that enrich the
     * past-meeting rows. 404 -> [NetworkError.NOT_FOUND] ("no records yet" — the store degrades to
     * the bare meetings list).
     */
    suspend fun getMeetingRecords(centerId: Int): NetworkResult<MeetingRecordListDto, NetworkError>
}
