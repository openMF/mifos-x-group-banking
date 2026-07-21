/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.data.repository

import kotlinx.coroutines.flow.Flow
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.model.AuthSession
import org.mifos.groupbanking.core.model.LoginCredentials
import org.mifos.groupbanking.core.model.SelfRegistration
import org.mifos.groupbanking.core.model.UserProfile

/**
 * Companion auth mutation + session-read repository (COMP-AUTH-001/002/003). Wraps
 * `CompanionAuthApi` (core/network) and persists the resulting session token to
 * `CompanionSessionStore` (core/datastore) on every successful login/self-register.
 *
 * **Store5 branch (SP-04):** `login-signup`'s `business_logic.kind` is `processor` — a
 * write+session-read mutation flow, not a read-stream — so per RULE-IMPLEMENT-STORE5-001 /
 * RULE-IDEA-IMPL-INTELLIGENCE-001 this repository surfaces [NetworkResult] directly rather
 * than `.asScreenStream()`/`.asPagingScreenStream()`. No `org.mobilenativefoundation.store`
 * import anywhere in this stack.
 *
 * No try-catch anywhere in this repository (Mandatory Rule 4) — every implementation method is
 * a plain `when` over the service's sealed [NetworkResult]; `CompanionAuthApiImpl` is the sole
 * layer allowed to catch exceptions.
 *
 * See API.md#repositories — AuthRepository.
 */
interface AuthRepository {

    /**
     * Reactive read of the persisted session — the `on_mount` token-presence check
     * (data-flow.yaml). Emits `null` when no session is persisted, or an [AuthSession] with an
     * EMPTY `groupMemberships` (see `CompanionSessionStore` KDoc) otherwise.
     */
    val currentSession: Flow<AuthSession?>

    /**
     * COMP-AUTH-001 — creates the companion account (+ linked Fineract client) and persists the
     * returned session on success. Does NOT persist anything on error.
     */
    suspend fun selfRegister(registration: SelfRegistration): NetworkResult<AuthSession, NetworkError>

    /**
     * COMP-AUTH-002 — authenticates and persists the returned session on success. Does NOT
     * persist anything on error.
     */
    suspend fun login(credentials: LoginCredentials): NetworkResult<AuthSession, NetworkError>

    /**
     * COMP-AUTH-003 — biometric-unlock profile refresh using an existing [sessionToken]. This
     * endpoint issues no new token, so nothing is (re-)persisted here; callers already hold a
     * valid [sessionToken] (from [currentSession]) before invoking this.
     */
    suspend fun refreshSession(sessionToken: String): NetworkResult<UserProfile, NetworkError>

    /** Clears the persisted session (logout / expired-token recovery). */
    suspend fun clearSession()
}
