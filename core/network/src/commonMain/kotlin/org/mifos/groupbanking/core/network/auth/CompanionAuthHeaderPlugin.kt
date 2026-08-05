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
 * Attaches `Authorization: Basic <key>` to every request from the shared Fineract
 * [io.ktor.client.HttpClient], reading the current base64-encoded authentication key from
 * [CompanionSessionStore] (persisted as `sessionToken` on login — see `CompanionAuthApiImpl`).
 *
 * WHY: Fineract authenticates every call with the basic-auth key returned by
 * `POST /authentication` (`base64EncodedAuthenticationKey`). Sending it on every request lets
 * Fineract resolve the authenticated caller for per-user-scoped reads (dashboards, group scope).
 *
 * Contract:
 *  - Skips when no session is persisted (pre-login calls: the login POST itself needs no key) so
 *    it never sends an empty `Basic `.
 *  - Never overwrites an Authorization header a caller already set (e.g. the explicit `/userdetails`
 *    call), so existing behaviour is preserved.
 *  - Lives in core/network (fork-owned), NOT core-base (template-shared): it uses a uniquely-named
 *    custom plugin so it cannot collide with the Auth/DefaultRequest plugins core-base installs.
 */
fun companionAuthHeaderPlugin(sessionStore: CompanionSessionStore) =
    createClientPlugin("CompanionAuthHeader") {
        onRequest { request, _ ->
            if (request.headers[HttpHeaders.Authorization] != null) return@onRequest
            val key = sessionStore.session.first()?.sessionToken
            if (!key.isNullOrBlank()) {
                request.header(HttpHeaders.Authorization, "Basic $key")
            }
        }
    }
