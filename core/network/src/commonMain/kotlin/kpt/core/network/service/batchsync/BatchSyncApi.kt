/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.network.service.batchsync

import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.network.model.BatchSyncRequestDto
import kpt.core.network.model.BatchSyncResponseItemDto

/**
 * Ktor client for the sync-status feature's single endpoint — the Fineract Batch API
 * (`idea-layer/screens/sync-status/api.yaml#api[batch_sync]`). Returns [NetworkResult] — never a
 * raw [Result] envelope, never a thrown exception (Mandatory Rule 2). `NetworkResult`/`NetworkError`
 * are the framework's `core-base/network` sealed types (consumed, never edited — Hard Rule #8).
 *
 * This Service is SERVICE-ONLY — the drain coordination (marking queue rows SYNCING/SYNCED/FAILED,
 * correlating each [BatchSyncResponseItemDto] back to its originating `SyncQueueItem` by
 * `requestId`, persisting `lastSyncAt`) is `SyncManagerImpl`'s job (`core/data`), never this
 * interface's.
 *
 * See API.md#services — BatchSyncApi.
 */
interface BatchSyncApi {

    /**
     * `POST /fineract-provider/api/v1/batches` (`api.yaml#api[batch_sync]`). Submits every queued
     * offline write as one atomic Fineract batch request. The response body is a **top-level JSON
     * ARRAY** of [BatchSyncResponseItemDto] (`api.yaml#dtos.BatchSyncResponse` declares
     * `type: array` at the top level, no envelope) — echoing each row's caller-assigned
     * `requestId` alongside its per-operation `statusCode` (2xx success, `409` conflict, anything
     * else a failure).
     *
     * 400 -> [NetworkError.BAD_REQUEST] ("batch validation error"); 401 ->
     * [NetworkError.UNAUTHORIZED]; 500 -> [NetworkError.SERVER]. This Service makes no
     * drain/retry decision itself — the caller (`SyncManagerImpl`) is responsible for marking
     * queue rows and surfacing a [kpt.core.model.SyncResult].
     */
    suspend fun batchSync(request: BatchSyncRequestDto): NetworkResult<List<BatchSyncResponseItemDto>, NetworkError>
}
