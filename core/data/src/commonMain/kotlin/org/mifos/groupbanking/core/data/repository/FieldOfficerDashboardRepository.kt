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
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.base.store.screen.FetchPolicy
import kpt.core.base.store.screen.ScreenDataStream
import org.mifos.groupbanking.core.model.FieldOfficerDashboard

/**
 * Read surface for the field-officer-dashboard screen (FR-009) — the composite fan-in of
 * `get_centers_for_staff` + `get_groups_for_staff` aggregated client-side into cross-group KPIs +
 * a per-group health list, plus the one-shot CSV export.
 *
 * The two-way parallel read is exposed through [fieldOfficerDashboardStream] — an offline-first
 * [ScreenDataStream] of `FieldOfficerDashboard` keyed by the field officer's staffId (+ session
 * role). There is no Store5 write path: the dashboard is read-only (RULE-IMPLEMENT-STORE5-001 S5-1
 * / S5-2). The export ([exportReport]) is a Store5-FREE direct read of the Fineract
 * `runreports` endpoint — its CSV bytes are handed to the OS share sheet by the ViewModel, never
 * cached — so it surfaces a raw [NetworkResult] rather than a `ScreenState`.
 *
 * See API.md#stores — FieldOfficerDashboard.
 */
interface FieldOfficerDashboardRepository {

    /**
     * Offline-first stream of the aggregated field-officer dashboard for [staffId].
     *
     * The store fires `get_groups_for_staff` (critical) + `get_centers_for_staff` (best-effort) in
     * parallel and fans them into one [FieldOfficerDashboard]; a cached aggregate is served
     * immediately then background-revalidated per the store's stale-while-revalidate policy
     * (`data-flow.yaml`: `stale_while_revalidate`, `ttl=300`, `offline: show_cached_with_banner`).
     * Call [ScreenDataStream.retry] to re-drive a failed fetch (the Retry CTA); pull-to-refresh
     * maps to a [ScreenDataStream.refreshFresh].
     *
     * @param staffId The field officer whose dashboard to stream (session-derived).
     * @param userRole The field officer's session role — threaded into the aggregate so the Export
     *   Report gate ([FieldOfficerDashboard.canExport]) can resolve.
     * @param scope CoroutineScope (typically `viewModelScope`) for the auto-refresh coroutine.
     * @param fetchPolicy Read policy. Defaults to [FetchPolicy.CACHE_FIRST_SWR].
     */
    fun fieldOfficerDashboardStream(
        staffId: Long,
        userRole: String,
        scope: CoroutineScope,
        fetchPolicy: FetchPolicy = FetchPolicy.CACHE_FIRST_SWR,
    ): ScreenDataStream<FieldOfficerDashboard>

    /**
     * Generates the field-officer group CSV report for [staffId]
     * (`GET /runreports/FieldOfficerGroupReport?R_staffId=…&output-type=CSV`). Returns the raw CSV
     * bytes on success; the ViewModel hands them to the OS share sheet. Store5-free (no read-stream
     * to cache) — surfaces the sealed [NetworkResult] directly.
     */
    suspend fun exportReport(staffId: Long): NetworkResult<ByteArray, NetworkError>
}
