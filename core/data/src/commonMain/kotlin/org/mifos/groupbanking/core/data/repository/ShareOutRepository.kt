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

import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.model.RotationPayoutExecuteResult
import org.mifos.groupbanking.core.model.RotationPayoutRequest
import org.mifos.groupbanking.core.model.ShareOutExecuteRequest
import org.mifos.groupbanking.core.model.ShareOutExecuteResult
import org.mifos.groupbanking.core.model.ShareOutPreview

/**
 * Repository for the share-out-preview feature — wraps `ShareOutApi` (`core/network`) for the
 * single companion read `GET /companion/groups/{groupId}/shareout/preview` (COMP-DIST-001).
 *
 * **Store5 branch (SP-04):** `share-out-preview` has no `AppStoreRegistry` entry /
 * `core/store/ShareOutStore.kt` yet — SP-03 `kmp-store-gen` has not run for this feature. Per this
 * generation's explicit brief this repository therefore surfaces [NetworkResult] directly rather
 * than `.asScreenStream()` (same branch as `SavingsRepositoryImpl`/`LoanApplyRepositoryImpl`).
 * **Upgrade path**: once a future `kmp-store-gen` step emits a `ShareOutStore` and registers the
 * matching `AppStoreRegistry` entry, [getShareOutPreview] should upgrade to
 * `shareOutStore.asScreenStream(key)`.
 *
 * No try-catch anywhere in this repository (Mandatory Rule 4) — [ShareOutRepositoryImpl] is a plain
 * `when` chain over the service's sealed [NetworkResult]; `ShareOutApiImpl` is the sole layer
 * allowed to catch exceptions.
 *
 * See API.md#repositories — ShareOutRepository.
 */
interface ShareOutRepository {

    /**
     * `GET /companion/groups/{groupId}/shareout/preview` — the strategy-aware distribution preview.
     * The companion returns `totalCorpus`/`totalProfit`/`totalPool` plus the pool-model-appropriate
     * payload (`memberPayouts[]` for ACCUMULATING types, or the rotation next-recipient fields for
     * ROTATING_PAYOUT types). Called on-mount, on pull-to-refresh, and on error-retry — all three
     * go through this single read.
     */
    suspend fun getShareOutPreview(groupId: String): NetworkResult<ShareOutPreview, NetworkError>

    /**
     * `POST /companion/groups/{groupId}/shareout/execute` (COMP-DIST-001) — executes the irreversible
     * ACCUMULATING share-out. Pure network passthrough (`when` chain over `ShareOutApi`'s
     * [NetworkResult], DTO<->domain mapped via `ShareOutMappers.kt`); the ViewModel — informed by
     * `NetworkMonitor` — is responsible for the pre-flight offline decision ([enqueueShareOutExecuteOffline])
     * and for streaming the per-member `MemberExecutionStatus`. Used for both the initial execute and
     * the `OnRetryFailed` subset re-submit (the ViewModel narrows [request]'s `memberPayouts` to the
     * failed subset).
     */
    suspend fun executeShareOut(
        groupId: String,
        request: ShareOutExecuteRequest,
    ): NetworkResult<ShareOutExecuteResult, NetworkError>

    /**
     * `POST /companion/groups/{groupId}/rotation/execute` (COMP-DIST-002) — executes the irreversible
     * ROTATING_PAYOUT single-recipient rotation. Same passthrough contract as [executeShareOut].
     */
    suspend fun executeRotationPayout(
        groupId: String,
        request: RotationPayoutRequest,
    ): NetworkResult<RotationPayoutExecuteResult, NetworkError>

    /**
     * Offline pre-flight for COMP-DIST-001 — serializes [request] to the HIGH-priority `sync_queue`
     * (`entity_type: SHARE_OUT_EXECUTE`, `data-flow.yaml#offline_behavior`) and returns the queued
     * row id. Delegates to the shared `SyncQueueRepository.enqueue` (local Room, no network), exactly
     * as `LoanRequestRepository.enqueueOffline` does. Called by the ViewModel INSTEAD OF
     * [executeShareOut] when already offline; the background sync worker drains it on reconnect.
     */
    suspend fun enqueueShareOutExecuteOffline(groupId: String, request: ShareOutExecuteRequest): Long

    /**
     * Offline pre-flight for COMP-DIST-002 — serializes [request] to the HIGH-priority `sync_queue`
     * (`entity_type: ROTATION_PAYOUT_EXECUTE`). Rotation twin of [enqueueShareOutExecuteOffline].
     */
    suspend fun enqueueRotationPayoutOffline(groupId: String, request: RotationPayoutRequest): Long
}
