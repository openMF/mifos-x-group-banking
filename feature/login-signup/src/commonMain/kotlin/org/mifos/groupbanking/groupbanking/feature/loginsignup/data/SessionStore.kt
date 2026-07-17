/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.groupbanking.feature.loginsignup.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Encrypted session-token store (AC6).
 *
 * The contract: persist the companion `sessionToken` in **encrypted** local
 * preferences so subsequent in-session API calls can reuse it while online, and
 * clear it on logout/expiry.
 *
 * The production binding should back this with `core-base/security`'s encrypted
 * settings (multiplatform-settings secure variant). [InMemorySessionStore] is the
 * default so the feature module stays self-contained and compilable; swap the DI
 * `single<SessionStore>` binding to the secure-prefs impl when wiring the app.
 */
interface SessionStore {
    /** Reactive view of the current session token (null when signed out). */
    val tokenFlow: StateFlow<String?>

    /** Current session token, or null when signed out. */
    fun read(): String?

    /** Persist [token] as the active session token. */
    fun write(token: String)

    /** Clear the session token (logout / expiry). */
    fun clear()
}

/**
 * Default in-memory [SessionStore]. Survives for the process lifetime only — the
 * app should override the DI binding with an encrypted-prefs-backed implementation.
 */
class InMemorySessionStore : SessionStore {
    private val _tokenFlow = MutableStateFlow<String?>(null)
    override val tokenFlow: StateFlow<String?> = _tokenFlow.asStateFlow()

    override fun read(): String? = _tokenFlow.value

    override fun write(token: String) {
        _tokenFlow.value = token
    }

    override fun clear() {
        _tokenFlow.value = null
    }
}
