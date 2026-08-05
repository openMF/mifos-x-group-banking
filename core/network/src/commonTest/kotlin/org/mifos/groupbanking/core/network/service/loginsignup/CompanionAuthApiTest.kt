/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.service.loginsignup

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.network.model.LoginRequestDto
import org.mifos.groupbanking.core.network.model.SelfRegisterRequestDto
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * TDD RED-first coverage for [CompanionAuthApi] / [CompanionAuthApiImpl] (COMP-AUTH-001/002/003).
 * Every method returns [NetworkResult] — never a raw [Result] envelope, never a thrown
 * exception (Mandatory Rule 2 / API.md#services).
 */
class CompanionAuthApiTest {

    private fun apiWith(
        status: HttpStatusCode,
        body: String,
        onRequestHeader: ((String?) -> Unit)? = null,
    ): CompanionAuthApi {
        val engine = MockEngine { request ->
            onRequestHeader?.invoke(request.headers[HttpHeaders.Authorization])
            respond(
                content = body,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        coerceInputValues = true
                    },
                )
            }
        }
        return CompanionAuthApiImpl(client)
    }

    // ---------- selfRegister (Fineract self-service — not yet wired) ----------

    @Test
    fun selfRegister_notWiredToFineract_returnsUnknownError() = runTest {
        // Fineract self-service registration does not map cleanly onto the companion shape; the
        // impl short-circuits to Error(UNKNOWN) with a TODO(auth) marker (non-blocking).
        val api = apiWith(HttpStatusCode.OK, """{}""")

        val result = api.selfRegister(SelfRegisterRequestDto("Amina", "amina@example.com", "hunter22"))

        assertEquals(NetworkResult.Error(NetworkError.UNKNOWN), result)
    }

    // ---------- login (POST /fineract-provider/api/v1/authentication) ----------

    @Test
    fun login_authenticated_mapsKeyToSessionToken() = runTest {
        val api = apiWith(
            HttpStatusCode.OK,
            """{"username":"amina","userId":42,"base64EncodedAuthenticationKey":"YW1pbmE6cHc=","authenticated":true,"officeId":1,"officeName":"Head Office"}""",
        )

        val result = api.login(LoginRequestDto("amina@example.com", "hunter22"))

        check(result is NetworkResult.Success)
        assertEquals("42", result.data.userId)
        assertEquals("YW1pbmE6cHc=", result.data.sessionToken)
    }

    @Test
    fun login_authenticatedFalse_mapsToUnauthorizedError() = runTest {
        val api = apiWith(HttpStatusCode.OK, """{"authenticated":false}""")

        val result = api.login(LoginRequestDto("amina@example.com", "wrong"))

        assertEquals(NetworkResult.Error(NetworkError.UNAUTHORIZED), result)
    }

    @Test
    fun login_unauthorized401_mapsToUnauthorizedError() = runTest {
        val api = apiWith(HttpStatusCode.Unauthorized, """{"error":"bad creds"}""")

        val result = api.login(LoginRequestDto("amina@example.com", "wrong"))

        assertEquals(NetworkResult.Error(NetworkError.UNAUTHORIZED), result)
    }

    @Test
    fun login_serverError500_mapsToServerError() = runTest {
        val api = apiWith(HttpStatusCode.InternalServerError, """{"error":"boom"}""")

        val result = api.login(LoginRequestDto("amina@example.com", "hunter22"))

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }

    // ---------- me (GET /fineract-provider/api/v1/userdetails) ----------

    @Test
    fun me_success_returnsMappedUserProfileAndSendsBasicHeader() = runTest {
        var capturedAuthHeader: String? = null
        val api = apiWith(
            HttpStatusCode.OK,
            """{"username":"amina","userId":42,"officeId":1,"officeName":"Head Office"}""",
            onRequestHeader = { capturedAuthHeader = it },
        )

        val result = api.me("YW1pbmE6cHc=")

        check(result is NetworkResult.Success)
        assertEquals("amina", result.data.name)
        assertEquals("Basic YW1pbmE6cHc=", capturedAuthHeader)
    }

    @Test
    fun me_unauthorized401_mapsToUnauthorizedError() = runTest {
        val api = apiWith(HttpStatusCode.Unauthorized, """{"error":"expired"}""")

        val result = api.me("expired-token")

        assertEquals(NetworkResult.Error(NetworkError.UNAUTHORIZED), result)
    }

    @Test
    fun me_serverError500_mapsToServerError() = runTest {
        val api = apiWith(HttpStatusCode.InternalServerError, """{"error":"boom"}""")

        val result = api.me("tok")

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }
}
