/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.data.repository

import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.model.LoanRequestPayload
import kpt.core.model.LoanRequestResult

/**
 * Loan-request submission repository (`idea-layer/screens/loan-request`). Wraps `LoanRequestApi`
 * (core/network) + `SyncQueueRepository` (shared offline write-queue).
 *
 * **Store5 branch (SP-04):** loan-request's `business_logic.kind` is `crud`
 * (`ui.yaml#business_logic.kind`) — the legacy template path
 * (RULE-IDEA-IMPL-INTELLIGENCE-001 AC-03i zero-regression). There is no read-stream to back with a
 * Store5 cache (the screen's only reads — `savingsBalance`/`clientId`/`loanMultiplier` — arrive via
 * `nav_params`/`SessionManager`, never through this repository). This repository surfaces
 * [NetworkResult] directly rather than `.asScreenStream()` / `.asPagingScreenStream()` /
 * `MutableStore.write(...)` — same branch as [MemberAddRepositoryImpl] / [InvitationRepositoryImpl]
 * / [GroupCreateRepositoryImpl]. No `org.mobilenativefoundation.store` import anywhere in this
 * stack.
 *
 * No try-catch anywhere in this repository (Mandatory Rule 4) — [LoanRequestRepositoryImpl] is a
 * plain `when` chain over the service's sealed [NetworkResult]; `LoanRequestApiImpl` is the sole
 * layer allowed to catch exceptions.
 *
 * **Offline-queue seam (VM-layer, per project convention — same pattern as
 * `MemberAddRepository` / `InvitationRepository` / `GroupCreateRepository`):**
 * `api.yaml#api[submit_loan_request].cache.offline: queue_to_syncqueue` and
 * `data-flow.yaml#entries[on_submit_click].offline_behavior` (`strategy: enqueue_to_sync_queue`,
 * `entity_type: LOAN_REQUEST`) both declare the offline contract. This repository makes NO offline
 * decision itself — it exposes two explicit, independently-testable methods so the ViewModel (
 * informed by `NetworkMonitor`/`ConnectivityManager`, per `ui.yaml#state_model.di`) stays the
 * single decision point:
 *
 * 1. [submit] — the online path. Called only when the caller has already confirmed connectivity.
 * 2. [enqueueOffline] — called BOTH as a pre-flight when already offline (never calling [submit]
 *    at all) AND as a retry-enqueue when a transport-level [NetworkResult.Error] surfaces from a
 *    [submit] call that started online and lost connectivity mid-flight, OR when [submit] returns
 *    the 503 ("server unavailable — queue for sync") branch of [NetworkError.SERVER].
 *
 * See API.md#repositories — LoanRequestRepository.
 */
interface LoanRequestRepository {

    /**
     * Submits [payload] to `POST /datatables/dt_loan_request` via the wrapped `LoanRequestApi` —
     * the pure online path. On success the datatable's `PENDING` resource-create envelope is
     * mapped to [LoanRequestResult]. Never enqueues to SyncQueue itself — see [enqueueOffline].
     */
    suspend fun submit(payload: LoanRequestPayload): NetworkResult<LoanRequestResult, NetworkError>

    /**
     * Serializes [payload] to JSON and enqueues it to the shared `SyncQueueRepository` with
     * `operationType = "LOAN_REQUEST"` and `targetTable = "dt_loan_request"` — the offline
     * fallback for [submit] (`data-flow.yaml#offline_behavior.strategy: enqueue_to_sync_queue`).
     * Returns the generated `sync_queue` row id. Never calls the network.
     */
    suspend fun enqueueOffline(payload: LoanRequestPayload): Long

    /**
     * Resolves the member's current savings balance (loan-eligibility input) via the wrapped
     * `LoanRequestApi.getMemberSavingsBalance`. The loan-request screen fetches this at mount rather
     * than trusting the upstream nav-param `savingsBalance`, which `personal-dashboard`/
     * `personal-loans` do not reliably populate (documented threading gap).
     */
    suspend fun memberSavingsBalance(clientId: Long): NetworkResult<Double, NetworkError>
}
