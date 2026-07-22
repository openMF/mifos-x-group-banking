/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.service.loanwriteoff

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.SerializationException
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.network.model.WriteoffLoanRequestDto
import org.mifos.groupbanking.core.network.model.WriteoffLoanResponseDto

private const val TAG = "LoanWriteoffApi"
private const val LOANS_PATH = "/loans"
private const val COMMAND_WRITEOFF = "writeoff"

/**
 * Plain-Ktor implementation of [LoanWriteoffApi]. This class is the ONLY layer in the
 * loan-mark-defaulted-dialog client stack allowed a try-catch — every exception (transport
 * failure, serialization failure) is caught HERE and converted into [NetworkResult.Error];
 * nothing throws past this boundary (Mandatory Rule 4 forbids try-catch one layer up, in
 * [org.mifos.groupbanking.core.data.repository.LoanWriteoffRepositoryImpl]). The HTTP-status ->
 * [NetworkError] mapping mirrors `core-base/network`'s `ResultSuspendConverterFactory` table
 * exactly, same as
 * [org.mifos.groupbanking.core.network.service.loanrepayment.LoanRepaymentApiImpl] /
 * [org.mifos.groupbanking.core.network.service.loandetail.LoanDetailApiImpl].
 *
 * Reuses the shared `HttpClient` singleton registered in `kpt.core.network.di.NetworkModule` — no
 * second engine constructed here. Auth-required requests are threaded through the shared client's
 * `bearerTokensProvider` plugin, same convention as `LoanRepaymentApi`/`LoanDetailApi`.
 *
 * See API.md#services — LoanWriteoffApi.
 */
class LoanWriteoffApiImpl(
    private val httpClient: HttpClient,
) : LoanWriteoffApi {

    override suspend fun writeoffLoan(
        loanId: Long,
        request: WriteoffLoanRequestDto,
    ): NetworkResult<WriteoffLoanResponseDto, NetworkError> {
        val path = "$LOANS_PATH/$loanId/transactions"
        Logger.d(TAG) { "writeoffLoan: POST $path?command=$COMMAND_WRITEOFF (loanId=$loanId) — irreversible" }
        return requestAsNetworkResult(op = "writeoffLoan") {
            httpClient.post(path) {
                parameter("command", COMMAND_WRITEOFF)
                contentType(ContentType.Application.Json)
                setBody(request)
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
        }
    } else {
        val error = response.status.toNetworkError()
        Logger.e(TAG) { "$op failed with HTTP ${response.status.value} -> $error" }
        NetworkResult.Error(error)
    }
}

/**
 * Mirrors `core-base/network`'s `ResultSuspendConverterFactory` status-code table exactly
 * (400/401/404/408/429/5xx/else->UNKNOWN) — an unmapped code (e.g. 403/409, which [NetworkError]
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
