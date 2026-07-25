/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.config

/**
 * Koin-injectable base-URL config for the previous-meeting-review feature's
 * `GET /datatables/dt_meeting_attendance/{meetingId}` endpoint. Follows the same default-param
 * config-class pattern as [MeetingRecordApiConfig] / [LoanDetailApiConfig].
 *
 * The shared `HttpClient` singleton in `kpt.core.network.di.NetworkModule` is already bound to the
 * companion server's base URL; `MeetingAttendanceApiImpl` reuses that client rather than
 * constructing a second engine. This config class exists for override-surface symmetry and for
 * forks that split the attendance datatable onto a different host.
 *
 * See API.md#services — MeetingAttendanceApi base URL.
 */
data class MeetingAttendanceApiConfig(
    val baseUrl: String = "http://localhost:8080",
)
