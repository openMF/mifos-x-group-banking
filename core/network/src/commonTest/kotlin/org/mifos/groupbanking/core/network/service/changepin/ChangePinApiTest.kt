/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.service.changepin

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.network.model.ChangePinRequestDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * TDD RED-first coverage for [ChangePinApi] / [ChangePinApiImpl] — the settings screen's
 * change-PIN submission (`idea-layer/screens/settings/api.yaml#api[change_pin]`,
 * `PUT /fineract-provider/api/v1/self/user/updatePassword`). Returns [NetworkResult] — never a
 * raw [Result] envelope, never a thrown exception (Mandatory Rule 2 / API.md#services). Mirrors
 * [org.mifos.groupbanking.core.network.service.memberprofile.MemberProfileApiTest]'s (PUT
 * variant of) the shared loan-request/member-add MockEngine harness pattern, including its
 * `ContentConvertException` malformed-body coverage.
 */
class ChangePinApiTest {

    private fun apiWith(
        status: HttpStatusCode,
        body: String,
        onRequestUrl: ((Url) -> Unit)? = null,
        onRequestMethod: ((HttpMethod) -> Unit)? = null,
    ): ChangePinApi {
        val engine = MockEngine { request ->
            onRequestUrl?.invoke(request.url)
            onRequestMethod?.invoke(request.method)
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
        return ChangePinApiImpl(client)
    }

    private val requestDto = ChangePinRequestDto(password = "5678", repeatPassword = "5678")

    private val responseBody = """{ "resourceId": 42 }"""

    // ---------- changePin (change_pin) ----------

    @Test
    fun changePin_success_putsBodyOnUpdatePasswordPathAndReturnsMappedResponse() = runTest {
        var capturedUrl: Url? = null
        var capturedMethod: HttpMethod? = null
        val api = apiWith(
            HttpStatusCode.OK,
            responseBody,
            onRequestUrl = { capturedUrl = it },
            onRequestMethod = { capturedMethod = it },
        )

        val result = api.changePin(requestDto)

        check(result is NetworkResult.Success)
        assertEquals(42L, result.data.resourceId)
        assertEquals(
            "/fineract-provider/api/v1/self/user/updatePassword",
            capturedUrl?.encodedPath,
        )
        assertEquals(HttpMethod.Put, capturedMethod)
    }

    @Test
    fun changePin_validation400_mapsToBadRequestError() = runTest {
        // api.yaml#error_handling.400: "invalid current PIN or weak new PIN"
        val api = apiWith(HttpStatusCode.BadRequest, """{"error":"invalid current PIN or weak new PIN"}""")

        val result = api.changePin(requestDto)

        assertEquals(NetworkResult.Error(NetworkError.BAD_REQUEST), result)
    }

    @Test
    fun changePin_sessionExpired401_mapsToUnauthorizedError() = runTest {
        // api.yaml#error_handling.401: "Session expired — navigate to login"
        val api = apiWith(HttpStatusCode.Unauthorized, """{"error":"session expired"}""")

        val result = api.changePin(requestDto)

        assertEquals(NetworkResult.Error(NetworkError.UNAUTHORIZED), result)
        assertNull((result as? NetworkResult.Success)?.data)
    }

    @Test
    fun changePin_serverError500_mapsToServerError() = runTest {
        // api.yaml#error_handling.500: "Show generic error snackbar"
        val api = apiWith(HttpStatusCode.InternalServerError, """{"error":"boom"}""")

        val result = api.changePin(requestDto)

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }

    @Test
    fun changePin_malformedJsonBody_mapsToSerializationError() = runTest {
        // Covers the ContentConvertException gap flagged by the loan-apply/loan-request/member-add
        // precedent — a malformed 2xx body must map to NetworkError.SERIALIZATION, never propagate
        // as an uncaught exception past this service boundary.
        val api = apiWith(HttpStatusCode.OK, "not-json")

        val result = api.changePin(requestDto)

        assertEquals(NetworkResult.Error(NetworkError.SERIALIZATION), result)
    }

    @Test
    fun changePin_sendsBothPasswordFieldsAsTheSameNewPinValue() = runTest {
        // The wire body carries ONLY password/repeatPassword — currentPin never appears in the
        // JSON body (see ChangePinMappers.kt / ChangePinRepository KDoc for the BasicAuth seam).
        var capturedUrl: Url? = null
        val api = apiWith(HttpStatusCode.OK, responseBody, onRequestUrl = { capturedUrl = it })

        val result = api.changePin(requestDto)

        check(result is NetworkResult.Success)
        assertEquals(requestDto.password, requestDto.repeatPassword)
        assertEquals("/fineract-provider/api/v1/self/user/updatePassword", capturedUrl?.encodedPath)
    }
}
