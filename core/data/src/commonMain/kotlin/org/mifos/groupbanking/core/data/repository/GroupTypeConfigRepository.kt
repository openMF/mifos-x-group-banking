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
import org.mifos.groupbanking.core.model.GroupTypeConfig

/**
 * Read surface for the seeded group-type catalogue (COMP-DT-003) backing the
 * group-type-picker screen.
 *
 * Wraps the NETWORK_WITH_CACHE `GroupTypeConfigStore` and exposes exactly one read path —
 * [groupTypeConfigsStream], an offline-first [ScreenDataStream] of `List<GroupTypeConfig>`.
 * There is no DAO-bypass read and no write path: the catalogue is a read-only seed
 * (RULE-IMPLEMENT-STORE5-001 S5-1 / S5-2). No `try-catch`, no `Result<T>` envelope — the
 * stream surfaces `ScreenState` (Loading / Content / NoNetwork / Error / Empty) directly.
 *
 * See API.md#stores — GroupTypeConfig.
 */
interface GroupTypeConfigRepository {

    /**
     * Offline-first stream of the seeded group-type catalogue.
     *
     * Emits cached rows immediately then background-revalidates per the store's
     * stale-while-revalidate policy (`data-flow.yaml`: `stale_while_revalidate`, `ttl=86400`,
     * `offline: show_cached`). Call [ScreenDataStream.retry] to re-drive a failed fetch
     * (the group-type-picker `OnRetry` action).
     *
     * @param scope CoroutineScope (typically `viewModelScope`) for the auto-refresh coroutine.
     * @param fetchPolicy Read policy. Defaults to [FetchPolicy.CACHE_FIRST_SWR] —
     *   stale-while-revalidate, matching the declared cache strategy.
     */
    fun groupTypeConfigsStream(
        scope: CoroutineScope,
        fetchPolicy: FetchPolicy = FetchPolicy.CACHE_FIRST_SWR,
    ): ScreenDataStream<List<GroupTypeConfig>>
}
