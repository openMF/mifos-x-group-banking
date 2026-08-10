/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.model

/**
 * Fineract's per-entity classification for a queued offline write, per
 * `idea-layer/screens/sync-status/api.yaml#dtos.EntityType`.
 *
 * **Registry divergence (flagged for the cross-feature repair station):** `api.yaml`'s own
 * `dtos.SyncQueueItem` models `entityType: EntityType` + `operation: SyncOperation` as first-class
 * columns, but the SHIPPED [SyncQueueItem] (this file's sibling, `SyncQueue.kt`) instead stores a
 * single generic `operationType: String` (+ `targetTable: String`) — more extensible, one column
 * covers every mutation feature's enqueue call without a schema migration per new entity/operation
 * pair. [EntityType]/[SyncOperation] are declared here as their OWN types (matching the registry's
 * value-set exactly) rather than added as new [SyncQueueItem] columns — [SyncClassifier.kt]'s
 * `operationTypeToEntityType`/`operationTypeToSyncOperation` bridge the two representations
 * without a DB migration. See `SyncClassifier.kt` kdoc for the full mapping table.
 *
 * See API.md#models — BatchSync.
 */
enum class EntityType {
    MEETING,
    LOAN,
    SAVINGS,
    ATTENDANCE,
    SHARE_OUT,
    MEMBER,
}

/**
 * The write kind a queued offline operation represents, per
 * `idea-layer/screens/sync-status/api.yaml#dtos.SyncOperation`. See [EntityType] kdoc for the
 * registry-divergence rationale (`SyncClassifier.kt` resolves this from
 * [SyncQueueItem.operationType], never stored as its own column).
 *
 * See API.md#models — BatchSync.
 */
enum class SyncOperation {
    CREATE,
    UPDATE,
    DELETE,
}

/**
 * Screen-level rollup badge for the sync-status feature, per
 * `idea-layer/screens/sync-status/api.yaml#dtos.SyncOverallStatus`. Distinct from the
 * per-row [SyncStatus] (`SyncQueue.kt`) — this is the aggregate across the whole queue
 * (e.g. ANY row FAILED -> FAILED; ANY row PENDING/SYNCING and none FAILED -> PENDING; every row
 * SYNCED -> SYNCED), derivation left to the repository/ViewModel layer (out of this generation's
 * scope — no wire source produces this value directly, it is always client-computed from
 * [SyncQueueCounts]).
 *
 * See API.md#models — BatchSync.
 */
enum class SyncOverallStatus {
    SYNCED,
    PENDING,
    FAILED,
}

/**
 * One request row inside a Fineract `/batches` submission — the domain projection of
 * [SyncQueueItem] resolved for the wire (see [kpt.core.network.mapper.toBatchOperation]
 * in `BatchSyncMappers.kt`).
 *
 * See API.md#models — BatchSync.
 */
data class BatchOperation(
    val requestId: Int,
    val relativeUrl: String,
    val method: String,
    val body: String,
)

/**
 * The full `/batches` submission — every pending [SyncQueueItem] resolved to a [BatchOperation],
 * sent as one atomic Fineract batch call (`sync-status` screen's "Sync Now" action /
 * `SyncManager`'s background job).
 *
 * See API.md#models — BatchSync.
 */
data class BatchSyncRequest(
    val requests: List<BatchOperation>,
)

/**
 * One response row of the `/batches` call — Fineract echoes `requestId` back so the caller can
 * correlate [BatchSyncResponseItem.statusCode] to the originating [SyncQueueItem] (by `requestId`
 * == the queue row's own generated batch position, NOT the queue row's own `id` — see
 * `BatchSyncMappers.kt#toBatchOperation`'s `requestId` parameter kdoc).
 *
 * See API.md#models — BatchSync.
 */
data class BatchSyncResponseItem(
    val requestId: Int,
    val statusCode: Int,
    val body: String,
)

/**
 * Client-computed rollup of a `/batches` response — folds every [BatchSyncResponseItem] by
 * `statusCode` (2xx -> [successCount], `409` -> [conflictCount], anything else -> [failedCount]).
 * See `BatchSyncMappers.kt#toSyncResult`.
 *
 * See API.md#models — BatchSync.
 */
data class SyncResult(
    val successCount: Int,
    val failedCount: Int,
    val conflictCount: Int,
)
