/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.network.service.loginsignup

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
import kpt.core.network.model.LoginRequestDto
import kpt.core.network.model.SelfRegisterRequestDto
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

    // ---------- selfRegister (COMP-AUTH-001) ----------

    @Test
    fun selfRegister_success_returnsMappedAuthResponse() = runTest {
        val api = apiWith(
            HttpStatusCode.Created,
            """{"userId":"u-1","sessionToken":"tok-abc","tokenExpiresAt":"2026-08-01T00:00:00Z","groupMemberships":[]}""",
        )

        val result = api.selfRegister(SelfRegisterRequestDto("Amina", "amina@example.com", "hunter22"))

        check(result is NetworkResult.Success)
        assertEquals("u-1", result.data.userId)
        assertEquals("tok-abc", result.data.sessionToken)
    }

    @Test
    fun selfRegister_conflict409_mapsToUnknownError() = runTest {
        // NetworkError has no CONFLICT bucket — 409 falls into the same UNKNOWN else-branch as
        // core-base/network's ResultSuspendConverterFactory (kept consistent deliberately).
        val api = apiWith(HttpStatusCode(409, "Conflict"), """{"error":"exists"}""")

        val result = api.selfRegister(SelfRegisterRequestDto("Amina", "amina@example.com", "hunter22"))

        assertEquals(NetworkResult.Error(NetworkError.UNKNOWN), result)
    }

    @Test
    fun selfRegister_serverError500_mapsToServerError() = runTest {
        val api = apiWith(HttpStatusCode.InternalServerError, """{"error":"boom"}""")

        val result = api.selfRegister(SelfRegisterRequestDto("Amina", "amina@example.com", "hunter22"))

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }

    @Test
    fun selfRegister_malformedJsonBody_mapsToSerializationError() = runTest {
        val api = apiWith(HttpStatusCode.OK, """not-json""")

        val result = api.selfRegister(SelfRegisterRequestDto("Amina", "amina@example.com", "hunter22"))

        assertEquals(NetworkResult.Error(NetworkError.SERIALIZATION), result)
    }

    // ---------- login (COMP-AUTH-002) ----------

    @Test
    fun login_success_returnsMappedAuthResponse() = runTest {
        val api = apiWith(
            HttpStatusCode.OK,
            """{"userId":"u-1","sessionToken":"tok-abc","tokenExpiresAt":"2026-08-01T00:00:00Z","groupMemberships":[]}""",
        )

        val result = api.login(LoginRequestDto("amina@example.com", "hunter22"))

        check(result is NetworkResult.Success)
        assertEquals("u-1", result.data.userId)
    }

    @Test
    fun login_unauthorized401_mapsToUnauthorizedError() = runTest {
        val api = apiWith(HttpStatusCode.Unauthorized, """{"error":"bad creds"}""")

        val result = api.login(LoginRequestDto("amina@example.com", "wrong"))

        assertEquals(NetworkResult.Error(NetworkError.UNAUTHORIZED), result)
    }

    @Test
    fun login_rateLimited429_mapsToTooManyRequestsError() = runTest {
        val api = apiWith(HttpStatusCode.TooManyRequests, """{"error":"rate limited"}""")

        val result = api.login(LoginRequestDto("amina@example.com", "hunter22"))

        assertEquals(NetworkResult.Error(NetworkError.TOO_MANY_REQUESTS), result)
    }

    @Test
    fun login_serverError500_mapsToServerError() = runTest {
        val api = apiWith(HttpStatusCode.InternalServerError, """{"error":"boom"}""")

        val result = api.login(LoginRequestDto("amina@example.com", "hunter22"))

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }

    // ---------- me (COMP-AUTH-003) ----------

    @Test
    fun me_success_returnsMappedUserProfileAndSendsBearerHeader() = runTest {
        var capturedAuthHeader: String? = null
        val api = apiWith(
            HttpStatusCode.OK,
            """{"userId":"u-1","name":"Amina","emailPhone":"amina@example.com","groupMemberships":[]}""",
            onRequestHeader = { capturedAuthHeader = it },
        )

        val result = api.me("tok-abc")

        check(result is NetworkResult.Success)
        assertEquals("Amina", result.data.name)
        assertEquals("Bearer tok-abc", capturedAuthHeader)
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
