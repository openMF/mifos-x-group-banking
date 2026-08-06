/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.service.fieldofficerdashboard

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.ContentConvertException
import kotlinx.serialization.SerializationException
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.network.model.PagedGroupsResponseDto

private const val TAG = "FieldOfficerApi"
private const val GROUPS_PATH = "/companion/field-officer/groups"
private const val REPORT_PATH = "/companion/field-officer/report"

/**
 * Plain-Ktor implementation of [FieldOfficerApi]. This class is the ONLY layer in the
 * field-officer-dashboard client stack allowed a try-catch — every exception (transport /
 * serialization failure) is caught HERE and converted into [NetworkResult.Error]; nothing throws
 * past this boundary (Mandatory Rule 4 forbids try-catch one layer up, in the Store5 wrapper). The
 * HTTP-status -> [NetworkError] mapping mirrors `core-base/network`'s `ResultSuspendConverterFactory`
 * table exactly, same as [org.mifos.groupbanking.core.network.service.grouplist.GroupApiImpl].
 *
 * Reuses the shared `HttpClient` singleton registered in `kpt.core.network.di.NetworkModule` — no
 * second engine constructed here. See API.md#services — FieldOfficerApi.
 */
class FieldOfficerApiImpl(
    private val httpClient: HttpClient,
) : FieldOfficerApi {

    override suspend fun getGroupsForStaff(
        staffId: Long,
        paged: Boolean,
        limit: Int,
        offset: Int,
    ): NetworkResult<PagedGroupsResponseDto, NetworkError> {
        Logger.d(TAG) { "getGroupsForStaff: GET $GROUPS_PATH (staffId=$staffId)" }
        return requestAsNetworkResult(op = "getGroupsForStaff") {
            httpClient.get(GROUPS_PATH) {
                parameter("staffId", staffId)
                parameter("paged", paged)
                parameter("limit", limit)
                parameter("offset", offset)
            }
        }
    }

    override suspend fun runReport(
        staffId: Long,
        outputType: String,
    ): NetworkResult<ByteArray, NetworkError> {
        Logger.d(TAG) { "runReport: GET $REPORT_PATH (R_staffId=$staffId, output-type=$outputType)" }
        return requestAsNetworkResult(op = "runReport") {
            httpClient.get(REPORT_PATH) {
                parameter("R_staffId", staffId)
                parameter("output-type", outputType)
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
 * (400/401/404/408/429/5xx/else->UNKNOWN).
 */
private fun HttpStatusCode.toNetworkError(): NetworkError = when (value) {
    HttpStatusCode.BadRequest.value -> NetworkError.BAD_REQUEST
    HttpStatusCode.Unauthorized.value -> NetworkError.UNAUTHORIZED
    HttpStatusCode.Forbidden.value -> NetworkError.UNAUTHORIZED
    HttpStatusCode.NotFound.value -> NetworkError.NOT_FOUND
    HttpStatusCode.RequestTimeout.value -> NetworkError.REQUEST_TIMEOUT
    HttpStatusCode.TooManyRequests.value -> NetworkError.TOO_MANY_REQUESTS
    in 500..599 -> NetworkError.SERVER
    else -> NetworkError.UNKNOWN
}
