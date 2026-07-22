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
import org.mifos.groupbanking.core.model.GroupDashboard

/**
 * Read surface for the group-dashboard screen (COMP-GRP-001) — the composite fan-in of `get_group`
 * + `get_viewer_role` + `get_group_corpus` + `get_group_accounts`.
 *
 * Wraps the composite dynamic-key NETWORK_WITH_CACHE `GroupDashboardStore` and exposes exactly one
 * read path — [groupDashboardStream], an offline-first [ScreenDataStream] of `GroupDashboard` keyed
 * by `groupId`. There is no DAO-bypass read and no write path: the dashboard is read-only
 * (`data-flow.yaml#sync_queue: []`, RULE-IMPLEMENT-STORE5-001 S5-1 / S5-2). No `try-catch`, no
 * `Result<T>` envelope — the stream surfaces `ScreenState` (Loading / Content / NoNetwork / Error /
 * Empty) directly.
 *
 * See API.md#stores — GroupDashboard.
 */
interface GroupDashboardRepository {

    /**
     * Offline-first stream of the combined group-dashboard composite for [groupId].
     *
     * The store fires the four companion reads in parallel and fans them into one [GroupDashboard];
     * a cached composite is served immediately then background-revalidated per the store's
     * stale-while-revalidate policy (`data-flow.yaml`: `stale_while_revalidate`, `ttl=300`,
     * `offline: fallback_cache`). Call [ScreenDataStream.retry] to re-drive a failed fetch (the
     * Retry CTA); pull-to-refresh maps to a [FetchPolicy.NETWORK_ONLY] re-collection
     * (`bypass_and_refresh`).
     *
     * @param groupId The group whose dashboard to stream (nav param).
     * @param scope CoroutineScope (typically `viewModelScope`) for the auto-refresh coroutine.
     * @param fetchPolicy Read policy. Defaults to [FetchPolicy.CACHE_FIRST_SWR] —
     *   stale-while-revalidate, matching the declared cache strategy.
     */
    fun groupDashboardStream(
        groupId: String,
        scope: CoroutineScope,
        fetchPolicy: FetchPolicy = FetchPolicy.CACHE_FIRST_SWR,
    ): ScreenDataStream<GroupDashboard>
}
