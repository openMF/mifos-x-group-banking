/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.network.service.shareout

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.ContentConvertException
import kotlinx.serialization.SerializationException
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.network.model.RotationPayoutExecuteRequestDto
import kpt.core.network.model.RotationPayoutExecuteResponseDto
import kpt.core.network.model.ShareOutExecuteRequestDto
import kpt.core.network.model.ShareOutExecuteResponseDto
import kpt.core.network.model.ShareOutPreviewDto

private const val TAG = "ShareOutApi"
private const val COMPANION_GROUPS_PATH = "/companion/groups"

/**
 * Plain-Ktor implementation of [ShareOutApi]. This class is the ONLY layer in the share-out client
 * stack allowed a try-catch — every exception (transport failure, serialization failure) is caught
 * HERE and converted into [NetworkResult.Error]; nothing throws past this boundary (Mandatory Rule
 * 4 forbids try-catch one layer up, in `ShareOutRepositoryImpl`). The HTTP-status -> [NetworkError]
 * mapping mirrors `core-base/network`'s `ResultSuspendConverterFactory` table exactly, same as
 * [kpt.core.network.service.savings.SavingsApiImpl].
 *
 * Reuses the shared `HttpClient` singleton registered in `kpt.core.network.di.NetworkModule` — no
 * second engine constructed here (same companion host as `CompanionAuthApiConfig`). Auth-required
 * requests are threaded through the shared client's optional `bearerTokensProvider` plugin (see
 * `core-base/network`'s `setupDefaultHttpClient`) rather than a per-call header.
 *
 * See API.md#services — ShareOutApi.
 */
class ShareOutApiImpl(
    private val httpClient: HttpClient,
) : ShareOutApi {

    override suspend fun getShareOutPreview(groupId: String): NetworkResult<ShareOutPreviewDto, NetworkError> {
        val path = "$COMPANION_GROUPS_PATH/$groupId/shareout/preview"
        Logger.d(TAG) { "getShareOutPreview: GET $path" }
        return requestAsNetworkResult(op = "getShareOutPreview") {
            httpClient.get(path)
        }
    }

    override suspend fun executeShareOut(
        groupId: String,
        request: ShareOutExecuteRequestDto,
    ): NetworkResult<ShareOutExecuteResponseDto, NetworkError> {
        val path = "$COMPANION_GROUPS_PATH/$groupId/shareout/execute"
        Logger.d(TAG) {
            "executeShareOut: POST $path (cycle=${request.cycleNumber}, payouts=${request.memberPayouts.size})"
        }
        return requestAsNetworkResult(op = "executeShareOut") {
            httpClient.post(path) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    override suspend fun executeRotationPayout(
        groupId: String,
        request: RotationPayoutExecuteRequestDto,
    ): NetworkResult<RotationPayoutExecuteResponseDto, NetworkError> {
        val path = "$COMPANION_GROUPS_PATH/$groupId/rotation/execute"
        Logger.d(TAG) {
            "executeRotationPayout: POST $path (recipient=${request.recipientMemberId}, amount=${request.amount})"
        }
        return requestAsNetworkResult(op = "executeRotationPayout") {
            httpClient.post(path) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }
}

/**
 * Executes [block], catches transport/serialization exceptions, and maps the resulting
 * [HttpResponse] status code into a [NetworkResult]. Info-logs on success, error-logs on every
 * failure branch — Kermit-tagged with [TAG]/[op].
 */
private suspend inline fun <reified T> requestAsNetworkResult(
    op: String,
    block: suspend () -> HttpResponse,
): NetworkResult<T, NetworkError> {
    val response = try {
        block()
    } catch (e: SerializationException) {
        Logger.e(TAG) { "$op request failure: ${e.message}" }
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
 * (400/401/404/408/429/5xx/else->UNKNOWN) — an unmapped code (e.g. 403 role-forbidden or 409
 * already-executed, which [NetworkError] has no dedicated bucket for) intentionally falls into
 * [NetworkError.UNKNOWN], same as that converter's `else` branch.
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
