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
import org.mifos.groupbanking.core.model.MemberDashboard

/**
 * Read surface for the personal-dashboard member home (COMP-DASH-001) backing the
 * personal-dashboard screen.
 *
 * Wraps the dynamic-key NETWORK_WITH_CACHE `MemberDashboardStore` and exposes exactly one read
 * path — [memberDashboardStream], an offline-first [ScreenDataStream] of `MemberDashboard` keyed
 * by the selected group. There is no DAO-bypass read and no write path: the dashboard is read-only
 * (`data-flow.yaml#sync_queue: []`, RULE-IMPLEMENT-STORE5-001 S5-1 / S5-2). No `try-catch`, no
 * `Result<T>` envelope — the stream surfaces `ScreenState` (Loading / Content / NoNetwork / Error /
 * Empty) directly.
 *
 * See API.md#stores — MemberDashboard.
 */
interface MemberDashboardRepository {

    /**
     * Offline-first stream of the authenticated member's dashboard for [selectedGroupId].
     *
     * [selectedGroupId] `= null` resolves server-side to the member's first group (first mount);
     * a non-null value re-drives the read for the group whose selector chip the user tapped
     * (`OnSelectGroup`) — each group is cached independently, so switching groups serves that
     * group's per-key cache immediately then background-revalidates per the store's
     * stale-while-revalidate policy (`data-flow.yaml`: `stale_while_revalidate`, `ttl=300`,
     * `offline: use_sqldelight`). Call [ScreenDataStream.retry] to re-drive a failed fetch;
     * pull-to-refresh maps to a [FetchPolicy.NETWORK_ONLY] re-collection (`bypass_and_refresh`).
     *
     * @param selectedGroupId The tapped group id, or `null` for the companion-resolved first group.
     * @param scope CoroutineScope (typically `viewModelScope`) for the auto-refresh coroutine.
     * @param fetchPolicy Read policy. Defaults to [FetchPolicy.CACHE_FIRST_SWR] —
     *   stale-while-revalidate, matching the declared cache strategy.
     */
    fun memberDashboardStream(
        selectedGroupId: String?,
        scope: CoroutineScope,
        fetchPolicy: FetchPolicy = FetchPolicy.CACHE_FIRST_SWR,
    ): ScreenDataStream<MemberDashboard>
}
