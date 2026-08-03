/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.config

/**
 * Koin-injectable base-URL config for the meeting-calendar read path — the two Fineract reads
 * (`get_center_meetings` + `get_meeting_records_datatable`) the meeting-calendar screen fires on
 * mount/refresh. Follows the same default-param config-class pattern as [GroupDashboardApiConfig] /
 * [GroupApiConfig] / [CompanionAuthApiConfig].
 *
 * The shared `HttpClient` singleton in `kpt.core.network.di.NetworkModule` is already bound to the
 * server base URL — [org.mifos.groupbanking.core.network.service.meetingcalendar.MeetingApiImpl]
 * reuses that client rather than constructing a second engine (Hard Rule "don't duplicate the
 * HttpClient"). This config class exists for override-surface symmetry / documentation and for
 * forks that split the meeting endpoints onto a different host.
 *
 * See API.md#services — MeetingApi base URL.
 */
data class MeetingApiConfig(
    val baseUrl: String = "http://localhost:8080",
)
