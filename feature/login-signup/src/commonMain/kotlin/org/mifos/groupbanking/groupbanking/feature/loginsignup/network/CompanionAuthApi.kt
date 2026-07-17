/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.groupbanking.feature.loginsignup.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * Companion-auth API client — wraps the three companion tools from
 * `server-layer/API_CONTRACT.yaml`:
 *
 * - COMP-AUTH-001 → `POST /companion/auth/self-register`
 * - COMP-AUTH-002 → `POST /companion/auth/login`
 * - COMP-AUTH-003 → `GET  /companion/auth/me`
 *
 * The backend is not yet deployed — these calls compile against the contract and
 * will surface 5xx at runtime until the companion server is live.
 */
class CompanionAuthApi(
    private val client: HttpClient,
    private val baseUrl: String = DEFAULT_BASE_URL,
) {

    /** COMP-AUTH-001 — self-register a new companion user. */
    suspend fun selfRegister(request: SelfRegisterRequestDto): AuthResponseDto =
        client.post("${baseUrl}companion/auth/self-register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    /** COMP-AUTH-002 — login with password or PIN. */
    suspend fun login(request: LoginRequestDto): AuthResponseDto =
        client.post("${baseUrl}companion/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    /** COMP-AUTH-003 — fetch the authenticated user's profile + group memberships. */
    suspend fun me(sessionToken: String): UserProfileDto =
        client.get("${baseUrl}companion/auth/me") {
            bearerAuth(sessionToken)
        }.body()

    companion object {
        // Companion backend base URL — placeholder until the server is deployed.
        const val DEFAULT_BASE_URL: String = "https://companion.mifos.community/"
    }
}
