/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
@file:OptIn(ExperimentalSettingsApi::class, ExperimentalSerializationApi::class)

package org.mifos.groupbanking.core.datastore.session

import co.touchlab.kermit.Logger
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.Settings
import com.russhwolf.settings.serialization.decodeValueOrNull
import com.russhwolf.settings.serialization.encodeValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kpt.core.base.common.manager.DispatcherManager
import org.mifos.groupbanking.core.model.AuthSession

private const val SESSION_KEY = "companion_session_key"
private const val TAG = "CompanionSessionStore"

/** Wire shape actually persisted — mirrors the `session_store` table's declared scope exactly. */
@Serializable
internal data class PersistedCompanionSession(
    val userId: String,
    val sessionToken: String,
    val tokenExpiresAt: String, // ISO-8601 — avoids requiring a custom kotlinx.datetime.Instant serializer.
)

/**
 * [CompanionSessionStore] backed directly by a SECURE (encrypted) [Settings] instance —
 * `named("secure")` from `core-base/datastore`'s `DatastoreBaseModule` — mirroring the
 * split plain/secure pattern already established by `UserPreferencesRepositoryImpl` in this
 * module. No try-catch here: `Settings` reads/writes are synchronous and non-throwing for
 * absent keys (`decodeValueOrNull` returns null), so there is nothing to catch.
 *
 * See API.md#dao — CompanionSessionStore.
 */
class CompanionSessionStoreImpl(
    private val secureSettings: Settings,
    private val dispatcher: DispatcherManager,
) : CompanionSessionStore {

    private fun loadPersisted(): PersistedCompanionSession? = secureSettings.decodeValueOrNull(
        key = SESSION_KEY,
        serializer = PersistedCompanionSession.serializer(),
    )

    private fun PersistedCompanionSession.toDomain(): AuthSession = AuthSession(
        userId = userId,
        sessionToken = sessionToken,
        tokenExpiresAt = Instant.parse(tokenExpiresAt),
        groupMemberships = emptyList(),
    )

    private val _session = MutableStateFlow(loadPersisted()?.toDomain())

    override val session: Flow<AuthSession?> = _session.asStateFlow()

    override suspend fun save(userId: String, sessionToken: String, tokenExpiresAt: Instant) {
        withContext(dispatcher.io) {
            val record = PersistedCompanionSession(
                userId = userId,
                sessionToken = sessionToken,
                tokenExpiresAt = tokenExpiresAt.toString(),
            )
            secureSettings.encodeValue(
                key = SESSION_KEY,
                serializer = PersistedCompanionSession.serializer(),
                value = record,
            )
            _session.value = record.toDomain()
            Logger.i(TAG) { "Session saved for userId=$userId" }
        }
    }

    override suspend fun clear() {
        withContext(dispatcher.io) {
            secureSettings.remove(SESSION_KEY)
            _session.value = null
            Logger.i(TAG) { "Session cleared" }
        }
    }
}
