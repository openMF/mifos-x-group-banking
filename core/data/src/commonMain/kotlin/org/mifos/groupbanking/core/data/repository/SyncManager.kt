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

import kotlinx.coroutines.flow.Flow
import org.mifos.groupbanking.core.model.SyncResult
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * The sync-status feature's drain coordinator — submits the pending `SyncQueueRepository` backlog
 * to the Fineract Batch API (`batch_sync`, `POST /fineract-provider/api/v1/batches`) via
 * [org.mifos.groupbanking.core.network.service.batchsync.BatchSyncApi] and applies the result back
 * onto each queue row, per `idea-layer/screens/sync-status/api.yaml#dependencies.repositories.SyncManager`
 * + `data-flow.yaml`'s `OnSyncNow` / `OnRetryOperation` triggers.
 *
 * **Not a Store5 read-store** — `sync-status`'s `data-flow.yaml` declares every entry
 * `cache.strategy: no_cache`; every read this feature needs is a direct local read
 * (`SyncQueueRepository`) or a direct network submission (this interface), never a cached
 * network-backed projection. No `org.mobilenativefoundation.store` import anywhere in this
 * stack. No try-catch here (Mandatory Rule 4) — [org.mifos.groupbanking.core.network.service.batchsync.BatchSyncApi]
 * is the sole try-catch boundary; this coordinator only branches on the [kpt.core.base.network.NetworkResult]
 * it returns.
 *
 * See API.md#repositories — SyncManager.
 */
@OptIn(ExperimentalTime::class)
interface SyncManager {

    /**
     * Drains the ENTIRE pending backlog (a snapshot taken at call time — rows enqueued mid-drain
     * are picked up by the NEXT `triggerSync`, never mid-flight) as one atomic `batch_sync`
     * submission, marks each row SYNCED/FAILED per its response row's `statusCode`, persists
     * `lastSyncAt` on completion, and emits the folded [SyncResult]. `api.yaml#api[batch_sync]`
     * (`OnSyncNow` trigger, `data-flow.yaml`).
     *
     * An empty backlog emits `SyncResult(0, 0, 0)` immediately, with NO network call — the
     * `Store5 Bookkeeper coordinates the drain` framing in `data-flow.yaml` refers to this
     * queue-then-submit shape, not a `core/store` Store5 binding (see interface KDoc above).
     */
    fun triggerSync(): Flow<SyncResult>

    /** The persisted timestamp of the last completed drain, or `null` if none has run yet. */
    fun getLastSyncAt(): Flow<Instant?>

    /**
     * Re-submits ONE failed queue row (by [itemId]) via the same `batch_sync` endpoint —
     * `OnRetryOperation` (`data-flow.yaml`, params `[itemId]`). Returns the (single-row)
     * [SyncResult] directly rather than a `Flow` — this is a targeted, one-shot retry, not a
     * long-running drain the caller subscribes to. **Documented `api.yaml` gap**: `api.yaml`'s
     * `dependencies.repositories.SyncManager.methods` list declares only `triggerSync` /
     * `getLastSyncAt`; `retryItem` is present in `data-flow.yaml`'s `OnRetryOperation` local_write
     * but was never added to `api.yaml`'s method list — added here to close that gap, not a
     * speculative addition.
     *
     * A missing [itemId] (already pruned / never existed) is a no-op that returns
     * `SyncResult(0, 0, 0)` — never throws.
     */
    suspend fun retryItem(itemId: Long): SyncResult
}
