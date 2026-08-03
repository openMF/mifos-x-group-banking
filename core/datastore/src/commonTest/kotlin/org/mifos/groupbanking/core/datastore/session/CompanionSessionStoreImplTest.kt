/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.datastore.session

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kpt.core.base.common.manager.DispatcherManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * TDD RED-first coverage for [CompanionSessionStoreImpl] — the `session_store` table
 * (data-flow.yaml) persistence surface: userId/sessionToken/tokenExpiresAt only, backed by the
 * SECURE (encrypted) `Settings` qualifier. NOT a Store5 test — see [CompanionSessionStore]
 * KDoc for why this feature is out of Store5 scope.
 */
class CompanionSessionStoreImplTest {

    /** Fake with a lazy [main] so constructing it never touches the real platform Main dispatcher. */
    private object FakeDispatcherManager : DispatcherManager {
        override val default: CoroutineDispatcher = Dispatchers.Unconfined
        override val main: MainCoroutineDispatcher by lazy { Dispatchers.Main }
        override val io: CoroutineDispatcher = Dispatchers.Unconfined
        override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
        override val appScope: CoroutineScope = CoroutineScope(Dispatchers.Unconfined)
    }

    private fun newStore(settings: MapSettings = MapSettings()) =
        CompanionSessionStoreImpl(secureSettings = settings, dispatcher = FakeDispatcherManager)

    @Test
    fun session_isNullWhenNothingPersistedYet() = runTest {
        val store = newStore()

        assertNull(store.session.first())
    }

    @Test
    fun save_persistsAndEmitsReconstructedSessionWithEmptyGroupMemberships() = runTest {
        val store = newStore()

        store.save(userId = "u-1", sessionToken = "tok-abc", tokenExpiresAt = Instant.parse("2026-08-01T00:00:00Z"))

        val session = store.session.first()
        assertEquals("u-1", session?.userId)
        assertEquals("tok-abc", session?.sessionToken)
        assertEquals(Instant.parse("2026-08-01T00:00:00Z"), session?.tokenExpiresAt)
        assertEquals(emptyList(), session?.groupMemberships)
    }

    @Test
    fun save_isReadableFromAFreshStoreInstanceOverTheSameSettings() = runTest {
        val settings = MapSettings()
        val writer = CompanionSessionStoreImpl(secureSettings = settings, dispatcher = FakeDispatcherManager)
        writer.save(userId = "u-2", sessionToken = "tok-xyz", tokenExpiresAt = Instant.parse("2026-09-01T00:00:00Z"))

        val reader = CompanionSessionStoreImpl(secureSettings = settings, dispatcher = FakeDispatcherManager)

        assertEquals("u-2", reader.session.first()?.userId)
    }

    @Test
    fun clear_removesThePersistedSession() = runTest {
        val store = newStore()
        store.save(userId = "u-1", sessionToken = "tok-abc", tokenExpiresAt = Instant.parse("2026-08-01T00:00:00Z"))

        store.clear()

        assertNull(store.session.first())
    }

    @Test
    fun clear_isReflectedFromAFreshStoreInstanceOverTheSameSettings() = runTest {
        val settings = MapSettings()
        val writer = CompanionSessionStoreImpl(secureSettings = settings, dispatcher = FakeDispatcherManager)
        writer.save(userId = "u-3", sessionToken = "tok", tokenExpiresAt = Instant.parse("2026-08-01T00:00:00Z"))
        writer.clear()

        val reader = CompanionSessionStoreImpl(secureSettings = settings, dispatcher = FakeDispatcherManager)

        assertNull(reader.session.first())
    }
}
