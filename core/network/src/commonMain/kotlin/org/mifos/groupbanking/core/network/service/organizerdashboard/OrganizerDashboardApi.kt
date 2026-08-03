/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.service.organizerdashboard

import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.network.model.OrganizerDashboardSummaryDto

/**
 * Ktor client for the organizer-dashboard companion bridge — unified-identity dashboard data
 * resolved server-side from the caller's `dt_member_role` (no `staffId` param, replacing the legacy
 * raw Fineract `/staff/{id}/summary` + `/centers` + `/journal-entries` triad). See
 * `idea-layer/screens/organizer-dashboard/api.yaml#api[0]` + API.md#services.
 *
 * Returns [NetworkResult] — never a raw [Result] envelope, never a thrown exception. The downstream
 * Store5 wrapper + Repository switch on the sealed result / surface `ScreenState` directly, with no
 * try-catch of their own — this Service is SERVICE-ONLY; no repository/store is emitted here.
 */
interface OrganizerDashboardApi {

    /**
     * `GET /companion/organizer/dashboard`. Fetches the authenticated organizer's dashboard: KPIs
     * scoped to the groups where the caller holds an organizer/treasurer/chairperson role, plus
     * today's meeting schedule and the recent cross-group activity feed. No parameters — identity is
     * resolved server-side from the auth token. Response is cached stale-while-revalidate by the
     * caller per `data-flow.yaml#cache` (`ttl_seconds=300`, offline `serve_stale`) — this Service
     * call itself is unconditional, always hitting the network.
     */
    suspend fun getOrganizerDashboard(): NetworkResult<OrganizerDashboardSummaryDto, NetworkError>
}
