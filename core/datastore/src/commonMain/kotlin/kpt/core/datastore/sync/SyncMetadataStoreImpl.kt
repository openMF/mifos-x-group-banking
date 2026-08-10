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

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kpt.core.base.common.manager.DispatcherManager
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private const val LAST_SYNC_AT_EPOCH_MS_KEY = "last_synced_at_epoch_ms"

/**
 * [SyncMetadataStore] backed directly by the PLAIN (non-secure) [Settings] instance —
 * `named("plain")` from `core-base/datastore`'s `DatastoreBaseModule` — mirroring
 * `SettingsSyncStatePersister` / `UserPreferencesRepositoryImpl`'s plain-store convention in this
 * module. Persists as a raw epoch-millis `Long` (`putLong`/`getLongOrNull`, both part of the
 * core, non-experimental `Settings` surface) rather than a serialized wrapper — no
 * `kotlinx.serialization` needed for a single scalar. No try-catch here: `Settings` reads/writes
 * are synchronous and non-throwing for an absent key.
 *
 * See API.md#dao — SyncMetadataStore.
 */
@OptIn(ExperimentalTime::class)
class SyncMetadataStoreImpl(
    private val plainSettings: Settings,
    private val dispatcher: DispatcherManager,
) : SyncMetadataStore {

    private fun loadPersisted(): Instant? =
        plainSettings.getLongOrNull(LAST_SYNC_AT_EPOCH_MS_KEY)?.let(Instant::fromEpochMilliseconds)

    private val _lastSyncAt = MutableStateFlow(loadPersisted())

    override val lastSyncAt: Flow<Instant?> = _lastSyncAt.asStateFlow()

    override suspend fun recordSyncCompleted(at: Instant) {
        withContext(dispatcher.io) {
            plainSettings.putLong(LAST_SYNC_AT_EPOCH_MS_KEY, at.toEpochMilliseconds())
            _lastSyncAt.value = at
        }
    }
}
