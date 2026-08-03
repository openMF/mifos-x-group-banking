/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.auth

import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.flow.first
import org.mifos.groupbanking.core.datastore.session.CompanionSessionStore

/**
 * Attaches `Authorization: Bearer <sessionToken>` to every companion request from the shared
 * companion [io.ktor.client.HttpClient], reading the current token from [CompanionSessionStore].
 *
 * WHY: the companion server resolves the caller's identity server-side from the bearer token
 * (COMP-AUTH-003 pattern) — e.g. the organizer dashboard greets the real user by name, and any
 * per-user-scoped endpoint can key off the authenticated caller. Before this plugin only the
 * explicit `/companion/auth/me` call sent the token, so every other companion read arrived
 * anonymous and the server fell back to a generic identity ("Organizer").
 *
 * Contract:
 *  - Skips when no session is persisted (pre-login calls: login / self-register need no token) so
 *    it never sends an empty `Bearer `.
 *  - Never overwrites an Authorization header a caller already set (e.g. the explicit `/me` call),
 *    so existing behaviour is preserved.
 *  - Lives in core/network (fork-owned), NOT core-base (template-shared): it uses a uniquely-named
 *    custom plugin so it cannot collide with the Auth/DefaultRequest plugins core-base installs.
 */
fun companionAuthHeaderPlugin(sessionStore: CompanionSessionStore) =
    createClientPlugin("CompanionAuthHeader") {
        onRequest { request, _ ->
            if (request.headers[HttpHeaders.Authorization] != null) return@onRequest
            val token = sessionStore.session.first()?.sessionToken
            if (!token.isNullOrBlank()) {
                request.header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
    }
