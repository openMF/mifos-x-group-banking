/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.datastore.session

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import org.mifos.groupbanking.core.model.AuthSession

/**
 * Local encrypted persistence for the companion auth session — the `session_store` table per
 * `idea-layer/screens/login-signup/data-flow.yaml` (`persistence_contract: local_encrypted_prefs`,
 * primary key `userId`). Written on every successful login/self-register; read on screen mount
 * for the token-presence check that gates a biometric-unlock prompt (no network call on mount).
 *
 * **NOT a Store5 read-store.** `login-signup`'s `business_logic.kind` is `processor` — a
 * write+session-read mutation flow — so this is a direct multiplatform-settings SECURE-backed
 * class rather than a `core/store` `Store`/`MutableStore` (RULE-IMPLEMENT-STORE5-001 /
 * RULE-IDEA-IMPL-INTELLIGENCE-001 scope: Store5 is reserved for read-streams). This type
 * intentionally carries no `org.mobilenativefoundation.store` import.
 *
 * See API.md#dao — CompanionSessionStore (core/datastore persistence surface).
 */
interface CompanionSessionStore {

    /**
     * The persisted session, reconstructed with an EMPTY `groupMemberships` list — this store
     * only persists the `session_store` table's scope (userId/sessionToken/tokenExpiresAt).
     * Callers needing the current group memberships call
     * `AuthRepository.refreshSession` (COMP-AUTH-003) instead.
     */
    val session: Flow<AuthSession?>

    /** Persists the session token triple. Called on every successful login/self-register. */
    suspend fun save(userId: String, sessionToken: String, tokenExpiresAt: Instant)

    /** Clears the persisted session (logout / expired-token recovery). */
    suspend fun clear()
}
