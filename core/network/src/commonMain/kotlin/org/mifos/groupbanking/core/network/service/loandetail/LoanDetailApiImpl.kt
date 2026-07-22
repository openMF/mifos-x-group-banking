/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.service.loandetail

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.SerializationException
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.network.model.LoanDetailResponseDto

private const val TAG = "LoanDetailApi"
private const val LOANS_PATH = "/loans"

/**
 * Plain-Ktor implementation of [LoanDetailApi]. This class is the ONLY layer in the loan-detail
 * client stack allowed a try-catch — every exception (transport failure, serialization failure)
 * is caught HERE and converted into [NetworkResult.Error]; nothing throws past this boundary
 * (Mandatory Rule 4 forbids try-catch one layer up, in the Repository / Store5 wrapper). The
 * HTTP-status -> [NetworkError] mapping mirrors `core-base/network`'s
 * `ResultSuspendConverterFactory` table exactly, same as
 * [org.mifos.groupbanking.core.network.service.loanlist.LoanApiImpl] and
 * [org.mifos.groupbanking.core.network.service.groupdashboard.GroupDashboardApiImpl].
 *
 * Reuses the shared `HttpClient` singleton registered in `kpt.core.network.di.NetworkModule` — no
 * second engine constructed here. Auth-required requests are threaded through the shared client's
 * optional `bearerTokensProvider` plugin (see `core-base/network`'s `setupDefaultHttpClient`)
 * rather than a per-call header — no `sessionToken` parameter is threaded through
 * [LoanDetailApi.getLoanDetail].
 *
 * See API.md#services — LoanDetailApi.
 */
class LoanDetailApiImpl(
    private val httpClient: HttpClient,
) : LoanDetailApi {

    override suspend fun getLoanDetail(
        loanId: Long,
        associations: String,
    ): NetworkResult<LoanDetailResponseDto, NetworkError> {
        val path = "$LOANS_PATH/$loanId"
        Logger.d(TAG) { "getLoanDetail: GET $path (associations=$associations)" }
        return requestAsNetworkResult(op = "getLoanDetail") {
            httpClient.get(path) {
                parameter("associations", associations)
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
        }
    } else {
        val error = response.status.toNetworkError()
        Logger.e(TAG) { "$op failed with HTTP ${response.status.value} -> $error" }
        NetworkResult.Error(error)
    }
}

/**
 * Mirrors `core-base/network`'s `ResultSuspendConverterFactory` status-code table exactly
 * (400/401/404/408/429/5xx/else->UNKNOWN) — an unmapped code intentionally falls into
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
