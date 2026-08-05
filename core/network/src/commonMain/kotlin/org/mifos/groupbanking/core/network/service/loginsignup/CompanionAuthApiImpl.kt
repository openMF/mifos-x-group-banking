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

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.ContentConvertException
import kotlinx.serialization.SerializationException
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.network.model.AuthResponseDto
import org.mifos.groupbanking.core.network.model.FineractAuthRequestDto
import org.mifos.groupbanking.core.network.model.FineractAuthResponseDto
import org.mifos.groupbanking.core.network.model.FineractUserDetailsDto
import org.mifos.groupbanking.core.network.model.LoginRequestDto
import org.mifos.groupbanking.core.network.model.SelfRegisterRequestDto
import org.mifos.groupbanking.core.network.model.UserProfileDto

private const val TAG = "CompanionAuthApi"

// Fineract-native auth endpoints (single-instance SoT: BuildKonfig.FINERACT_BASE_URL, resolved
// via the shared HttpClient's base URL from CompanionAuthApiConfig). Replaces the retired
// `/companion/auth/*` bridge.
private const val AUTH_PATH = "/fineract-provider/api/v1/authentication"
private const val USER_DETAILS_PATH = "/fineract-provider/api/v1/userdetails"

// Fineract basic-auth keys do not carry an expiry; the app's AuthSession/session store still
// require an Instant, so we synthesize a far-future stamp for the token-presence gate.
private const val NON_EXPIRING_TOKEN_STAMP = "2099-12-31T23:59:59Z"

/**
 * Plain-Ktor implementation of [CompanionAuthApi]. This class is the ONLY layer in the
 * login-signup client stack allowed a try-catch — every exception (transport failure,
 * serialization failure) is caught HERE and converted into [NetworkResult.Error]; nothing
 * throws past this boundary (Mandatory Rule 4 forbids try-catch one layer up, in the
 * Repository). The HTTP-status -> [NetworkError] mapping mirrors
 * `core-base/network`'s `ResultSuspendConverterFactory` table exactly, so behaviour is
 * identical regardless of which client stack (this one, or a future Ktorfit-annotated
 * service) a feature uses.
 *
 * See API.md#services — CompanionAuthApi.
 */
class CompanionAuthApiImpl(
    private val httpClient: HttpClient,
) : CompanionAuthApi {

    override suspend fun selfRegister(request: SelfRegisterRequestDto): NetworkResult<AuthResponseDto, NetworkError> {
        // TODO(auth): /self/registration — Fineract self-service registration
        // (POST /fineract-provider/api/v1/self/registration) does not map cleanly onto the
        // companion self-register shape (it requires an authenticationMode + a separate activation
        // step and returns no auth key). Non-blocking: signup is not a core group-banking flow.
        Logger.d(TAG) { "selfRegister: not wired to Fineract self-service yet" }
        return NetworkResult.Error(NetworkError.UNKNOWN)
    }

    override suspend fun login(request: LoginRequestDto): NetworkResult<AuthResponseDto, NetworkError> {
        Logger.d(TAG) { "login: POST $AUTH_PATH" }
        val result: NetworkResult<FineractAuthResponseDto, NetworkError> = requestAsNetworkResult(op = "login") {
            httpClient.post(AUTH_PATH) {
                contentType(ContentType.Application.Json)
                setBody(FineractAuthRequestDto(username = request.emailPhone, password = request.password))
            }
        }
        return when (result) {
            is NetworkResult.Success -> {
                val fineract = result.data
                val key = fineract.base64EncodedAuthenticationKey
                if (fineract.authenticated && !key.isNullOrBlank()) {
                    NetworkResult.Success(
                        AuthResponseDto(
                            userId = fineract.userId?.toString() ?: (fineract.username ?: ""),
                            // The base64 basic-auth key becomes the stored credential the
                            // CompanionAuthHeaderPlugin attaches as `Authorization: Basic <key>`.
                            sessionToken = key,
                            tokenExpiresAt = NON_EXPIRING_TOKEN_STAMP,
                            groupMemberships = emptyList(),
                        ),
                    )
                } else {
                    Logger.e(TAG) { "login: authenticated=false or missing key" }
                    NetworkResult.Error(NetworkError.UNAUTHORIZED)
                }
            }
            is NetworkResult.Error -> result
        }
    }

    override suspend fun me(sessionToken: String): NetworkResult<UserProfileDto, NetworkError> {
        Logger.d(TAG) { "me: GET $USER_DETAILS_PATH" }
        val result: NetworkResult<FineractUserDetailsDto, NetworkError> = requestAsNetworkResult(op = "me") {
            httpClient.get(USER_DETAILS_PATH) {
                header(HttpHeaders.Authorization, "Basic $sessionToken")
            }
        }
        return when (result) {
            is NetworkResult.Success -> {
                val details = result.data
                val name = details.username ?: ""
                NetworkResult.Success(
                    UserProfileDto(
                        userId = details.userId?.toString() ?: name,
                        name = name,
                        // Fineract /userdetails exposes no email/phone — fall back to username.
                        emailPhone = name,
                        groupMemberships = emptyList(),
                    ),
                )
            }
            is NetworkResult.Error -> result
        }
    }
}

/**
 * Executes [block], catches transport/serialization exceptions, and maps the resulting
 * [HttpResponse] status code into a [NetworkResult]. Debug-logs on start (callers already log
 * that), info-logs on success, error-logs on every failure branch — Kermit-tagged with
 * [TAG]/[op].
 */
private suspend inline fun <reified T> requestAsNetworkResult(
    op: String,
    block: suspend () -> HttpResponse,
): NetworkResult<T, NetworkError> {
    val response = try {
        block()
    } catch (e: SerializationException) {
        Logger.e(TAG) { "$op request-body serialization failure: ${e.message}" }
        return NetworkResult.Error(NetworkError.SERIALIZATION)
    } catch (e: Exception) {
        Logger.e(TAG) { "$op transport failure: ${e.message}" }
        return NetworkResult.Error(NetworkError.UNKNOWN)
    }

    return if (response.status.value in 200..299) {
        try {
            val data = response.body<T>()
            Logger.i(TAG) { "$op succeeded (${response.status.value})" }
            NetworkResult.Success(data)
        } catch (e: NoTransformationFoundException) {
            Logger.e(TAG) { "$op response deserialization failure: ${e.message}" }
            NetworkResult.Error(NetworkError.SERIALIZATION)
        } catch (e: SerializationException) {
            Logger.e(TAG) { "$op response deserialization failure: ${e.message}" }
            NetworkResult.Error(NetworkError.SERIALIZATION)
        } catch (e: ContentConvertException) {
            // Covers io.ktor.serialization.JsonConvertException (malformed JSON body, e.g.
            // "not-json") — a ContentConvertException subtype NOT caught by the
            // SerializationException clauses above; caught here so a malformed 2xx body maps to
            // NetworkError.SERIALIZATION rather than propagating as an uncaught exception past this
            // service boundary (Mandatory Rule 4).
            Logger.e(TAG) { "$op response deserialization failure: ${e.message}" }
            NetworkResult.Error(NetworkError.SERIALIZATION)
        }
    } else {
        val error = response.status.toNetworkError()
        Logger.e(TAG) { "$op failed with HTTP ${response.status.value} -> $error" }
        NetworkResult.Error(error)
    }
}

/**
 * Mirrors `core-base/network`'s `ResultSuspendConverterFactory` status-code table exactly
 * (400/401/404/408/429/5xx/else->UNKNOWN) — an unmapped code (e.g. 409, which [NetworkError]
 * has no dedicated bucket for) intentionally falls into [NetworkError.UNKNOWN], same as that
 * converter's `else` branch.
 */
private fun HttpStatusCode.toNetworkError(): NetworkError = when (value) {
    HttpStatusCode.BadRequest.value -> NetworkError.BAD_REQUEST
    HttpStatusCode.Unauthorized.value -> NetworkError.UNAUTHORIZED
    HttpStatusCode.NotFound.value -> NetworkError.NOT_FOUND
    HttpStatusCode.RequestTimeout.value -> NetworkError.REQUEST_TIMEOUT
    HttpStatusCode.TooManyRequests.value -> NetworkError.TOO_MANY_REQUESTS
    in 500..599 -> NetworkError.SERVER
    else -> NetworkError.UNKNOWN
}
