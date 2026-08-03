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
import org.mifos.groupbanking.core.network.model.LoginRequestDto
import org.mifos.groupbanking.core.network.model.SelfRegisterRequestDto
import org.mifos.groupbanking.core.network.model.UserProfileDto

private const val TAG = "CompanionAuthApi"
private const val SELF_REGISTER_PATH = "/companion/auth/self-register"
private const val LOGIN_PATH = "/companion/auth/login"
private const val ME_PATH = "/companion/auth/me"

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
        Logger.d(TAG) { "selfRegister: POST $SELF_REGISTER_PATH" }
        return requestAsNetworkResult(op = "selfRegister") {
            httpClient.post(SELF_REGISTER_PATH) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    override suspend fun login(request: LoginRequestDto): NetworkResult<AuthResponseDto, NetworkError> {
        Logger.d(TAG) { "login: POST $LOGIN_PATH" }
        return requestAsNetworkResult(op = "login") {
            httpClient.post(LOGIN_PATH) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    override suspend fun me(sessionToken: String): NetworkResult<UserProfileDto, NetworkError> {
        Logger.d(TAG) { "me: GET $ME_PATH" }
        return requestAsNetworkResult(op = "me") {
            httpClient.get(ME_PATH) {
                header(HttpHeaders.Authorization, "Bearer $sessionToken")
            }
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
