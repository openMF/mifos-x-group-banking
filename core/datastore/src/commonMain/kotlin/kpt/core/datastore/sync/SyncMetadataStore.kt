/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.datastore.sync

import kotlinx.coroutines.flow.Flow
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Local persistence for `SyncManager`'s `lastSyncAt` timestamp — the `app_settings.last_synced_at`
 * column per `idea-layer/screens/sync-status/api.yaml#dependencies.local_db`. Written once a
 * `batch_sync` drain completes (success or partial-failure — see `SyncManagerImpl` KDoc); read on
 * the sync-status screen mount for the "last synced X ago" caption.
 *
 * **NOT a Store5 read-store** — `sync-status`'s `data-flow.yaml` declares every entry
 * `cache.strategy: no_cache` (direct local reads, no network fetch to cache), so this is a direct
 * multiplatform-settings-backed class rather than a `core/store` `Store`/`MutableStore`
 * (RULE-IMPLEMENT-STORE5-001 scope: Store5 is reserved for network-backed read-streams). Mirrors
 * the split plain/secure pattern established by [kpt.core.datastore.session.CompanionSessionStore]
 * — this value is non-secret, so it is backed by the PLAIN `Settings` qualifier (same store as
 * `SettingsSyncStatePersister`'s change-list versions), not the secure one.
 *
 * See API.md#dao — SyncMetadataStore (core/datastore persistence surface).
 */
@OptIn(ExperimentalTime::class)
interface SyncMetadataStore {

    /** The persisted last-successful-drain timestamp, or `null` if a sync has never completed. */
    val lastSyncAt: Flow<Instant?>

    /** Persists [at] as the new `lastSyncAt`. Called by `SyncManagerImpl` after every drain. */
    suspend fun recordSyncCompleted(at: Instant)
}
