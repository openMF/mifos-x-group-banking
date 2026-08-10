/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.store.savingsdashboard.impl

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.base.store.infra.StoreFactory
import kpt.core.model.SavingsDashboardSummary
import kpt.core.network.mapper.toDomainModel
import kpt.core.network.service.savings.SavingsApi
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.Store

/**
 * Builds the **single-key composite** read-only NETWORK_WITH_CACHE [Store] for the savings-dashboard
 * screen (COMP-DIST / `savings-dashboard/data-flow.yaml`) — the client-side fan-in of the TWO
 * independent companion reads the screen fires in parallel on mount / refresh / retry
 * ([SavingsApi.getGroupSavingsSummary] + [SavingsApi.getIndividualSavingsSummary]).
 *
 * The key is the `String` `groupId` and the value is one combined [SavingsDashboardSummary] snapshot
 * (group-tab summary + individual-tab summary); Store5 caches each group independently, so re-opening
 * a group re-uses that group's per-key cache or re-fetches if stale. The read side is exposed to the
 * UI exclusively through `SavingsDashboardRepository.savingsDashboardStream(...)` →
 * `.asScreenStream(...)` — the feature ViewModel therefore consumes `ScreenState<SavingsDashboardSummary>`
 * and never `NetworkResult` (RULE-IMPLEMENT-STORE5-001 S5-2 inconsistent-read-path fix). The dashboard
 * is read-only (`data-flow.yaml#entries[SelectTab]` is a pure client transform, no write) so there is
 * no write path (S5-1).
 *
 * **In-memory SourceOfTruth (honest deviation from loan-detail's Room SoT):** unlike
 * `provideLoanDetailStore` (single-endpoint composite persisted to a JSON-column Room table), this
 * store is memory-backed ([StoreFactory.createMemoryStore]). The savings-dashboard read is a
 * two-endpoint parallel COMPOSITE whose two nested member-row lists (group + individual) would each
 * need a bespoke Room entity + codec + a DB migration; the primary user-flagged defect being fixed
 * here is the S5-2 read-path leak (feature consuming `NetworkResult`), which the ScreenState-surfacing
 * `.asScreenStream()` seam already resolves. Cross-process-death offline persistence (a Room SoT) is a
 * follow-up upgrade that can slot in later WITHOUT changing this store's public shape or the
 * repository/ViewModel — `createMemoryStore` → `createStore(sourceOfTruth = …)`. The `NetworkResult`
 * from each service is consumed INSIDE the fetcher and never surfaced up.
 *
 * - **Fetcher** — a PARALLEL-COMBINE: inside a [coroutineScope] the two reads fire as concurrent
 *   [async] coroutines, then `await` fans them into a [SavingsDashboardSummary]. Each read returns a
 *   sealed [NetworkResult]; either read failing ([NetworkResult.Error]) throws
 *   [SavingsDashboardFetchException] so Store5 routes it to an error response (no try-catch, no
 *   `Result` envelope, no partial persist — Store5 owns the error channel), matching
 *   `provideGroupDashboardStore`'s identical composite-fetcher precedent.
 *
 * See API.md#stores — SavingsDashboardSummary.
 */
fun provideSavingsDashboardStore(
    api: SavingsApi,
): Store<String, SavingsDashboardSummary> = StoreFactory.createMemoryStore(
    fetcher = Fetcher.of { groupId: String ->
        coroutineScope {
            // data-flow.yaml#entries[0]: "Both calls run in parallel on mount."
            val groupDeferred = async { api.getGroupSavingsSummary(groupId) }
            val individualDeferred = async { api.getIndividualSavingsSummary(groupId) }

            SavingsDashboardSummary(
                group = groupDeferred.await().dataOrThrow().toDomainModel(),
                individual = individualDeferred.await().dataOrThrow().toDomainModel(),
            )
        }
    },
)

/**
 * Signals a failed savings-dashboard composite fetch to Store5's error channel. Carries the sealed
 * [NetworkError] of the FIRST critical read that failed so downstream error mapping (feature-layer)
 * can branch on the exact cause; the message is `categorize()`-friendly for state routing.
 */
class SavingsDashboardFetchException(
    val networkError: NetworkError,
) : Exception("Savings dashboard fetch failed: $networkError")

/**
 * Unwraps a critical composite read: returns the DTO on success, or throws
 * [SavingsDashboardFetchException] on failure so a single failed leg aborts the whole parallel fetch
 * (no partial composite is ever cached).
 */
private fun <T> NetworkResult<T, NetworkError>.dataOrThrow(): T = when (this) {
    is NetworkResult.Success -> data
    is NetworkResult.Error -> throw SavingsDashboardFetchException(error)
}
