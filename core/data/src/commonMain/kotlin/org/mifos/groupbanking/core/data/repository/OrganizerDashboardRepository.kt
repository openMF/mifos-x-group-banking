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
import org.mifos.groupbanking.core.model.OrganizerDashboardSummary

/**
 * Read surface for the organizer-dashboard hub backing the organizer-dashboard screen.
 *
 * Wraps the single-key NETWORK_WITH_CACHE `OrganizerDashboardStore` and exposes exactly one read
 * path — [organizerDashboardStream], an offline-first [ScreenDataStream] of
 * `OrganizerDashboardSummary`. There is no DAO-bypass read and no write path: the dashboard is
 * read-only (`data-flow.yaml#sync_queue: []`, RULE-IMPLEMENT-STORE5-001 S5-1 / S5-2). No `try-catch`,
 * no `Result<T>` envelope — the stream surfaces `ScreenState` (Loading / Content / NoNetwork / Error
 * / Empty) directly.
 *
 * See API.md#stores — OrganizerDashboard.
 */
interface OrganizerDashboardRepository {

    /**
     * Offline-first stream of the authenticated organizer's dashboard.
     *
     * A cached snapshot is served immediately then background-revalidated per the store's
     * stale-while-revalidate policy (`data-flow.yaml`: `stale_while_revalidate`, `ttl=300`,
     * `offline: serve_stale`). Call [ScreenDataStream.retry] to re-drive a failed fetch (the Retry
     * CTA); pull-to-refresh maps to a [ScreenDataStream.refreshFresh] (`bypass_and_refresh`).
     *
     * @param scope CoroutineScope (typically `viewModelScope`) for the auto-refresh coroutine.
     * @param fetchPolicy Read policy. Defaults to [FetchPolicy.CACHE_FIRST_SWR] —
     *   stale-while-revalidate, matching the declared cache strategy.
     */
    fun organizerDashboardStream(
        scope: CoroutineScope,
        fetchPolicy: FetchPolicy = FetchPolicy.CACHE_FIRST_SWR,
    ): ScreenDataStream<OrganizerDashboardSummary>
}
