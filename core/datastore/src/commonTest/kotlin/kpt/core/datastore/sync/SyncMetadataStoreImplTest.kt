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

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kpt.core.base.common.manager.DispatcherManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * TDD RED-first coverage for [SyncMetadataStoreImpl] — the `app_settings.last_synced_at` column
 * (`idea-layer/screens/sync-status/api.yaml#dependencies.local_db`) persistence surface, backed by
 * the PLAIN (non-secure) `Settings` qualifier. NOT a Store5 test — see [SyncMetadataStore] KDoc
 * for why this feature is out of Store5 scope. Mirrors
 * [kpt.core.datastore.session.CompanionSessionStoreImplTest]'s harness pattern.
 */
@OptIn(ExperimentalTime::class)
class SyncMetadataStoreImplTest {

    /** Fake with a lazy [main] so constructing it never touches the real platform Main dispatcher. */
    private object FakeDispatcherManager : DispatcherManager {
        override val default: CoroutineDispatcher = Dispatchers.Unconfined
        override val main: MainCoroutineDispatcher by lazy { Dispatchers.Main }
        override val io: CoroutineDispatcher = Dispatchers.Unconfined
        override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
        override val appScope: CoroutineScope = CoroutineScope(Dispatchers.Unconfined)
    }

    private fun newStore(settings: MapSettings = MapSettings()) =
        SyncMetadataStoreImpl(plainSettings = settings, dispatcher = FakeDispatcherManager)

    @Test
    fun lastSyncAt_isNullWhenNoSyncHasEverCompleted() = runTest {
        val store = newStore()

        assertNull(store.lastSyncAt.first())
    }

    @Test
    fun recordSyncCompleted_persistsAndEmitsTheTimestamp() = runTest {
        val store = newStore()
        val at = Instant.parse("2026-07-22T10:00:00Z")

        store.recordSyncCompleted(at)

        assertEquals(at, store.lastSyncAt.first())
    }

    @Test
    fun recordSyncCompleted_isReadableFromAFreshStoreInstanceOverTheSameSettings() = runTest {
        val settings = MapSettings()
        val writer = SyncMetadataStoreImpl(plainSettings = settings, dispatcher = FakeDispatcherManager)
        val at = Instant.parse("2026-07-22T11:30:00Z")
        writer.recordSyncCompleted(at)

        val reader = SyncMetadataStoreImpl(plainSettings = settings, dispatcher = FakeDispatcherManager)

        assertEquals(at, reader.lastSyncAt.first())
    }

    @Test
    fun recordSyncCompleted_overwritesThePreviousTimestamp() = runTest {
        val store = newStore()
        store.recordSyncCompleted(Instant.parse("2026-07-22T09:00:00Z"))

        val second = Instant.parse("2026-07-22T12:00:00Z")
        store.recordSyncCompleted(second)

        assertEquals(second, store.lastSyncAt.first())
    }
}
