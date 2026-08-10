/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.network.service.memberinvite

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.request.delete
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
import kpt.core.network.model.CreateInviteRequestDto
import kpt.core.network.model.GeneratedInviteDto
import kpt.core.network.model.PendingInviteDto
import kpt.core.network.model.RevokeInviteResponseDto

private const val TAG = "MemberInviteApi"
private const val INVITATIONS_PATH = "/companion/datatables/invitations"

/**
 * Plain-Ktor implementation of [MemberInviteApi]. This class is the ONLY layer in the
 * member-invite client stack allowed a try-catch — every transport/serialization exception is
 * caught HERE and converted into [NetworkResult.Error]; nothing throws past this boundary
 * (Mandatory Rule 4 forbids try-catch one layer up, in
 * [kpt.core.data.repository.MemberInviteRepositoryImpl]). The HTTP-status ->
 * [NetworkError] mapping mirrors `core-base/network`'s `ResultSuspendConverterFactory` table
 * exactly, same as
 * [kpt.core.network.service.joinwithcode.InvitationApiImpl].
 *
 * Reuses the shared `HttpClient` singleton registered in `kpt.core.network.di.NetworkModule`
 * (bound to the companion server base URL) — no second engine constructed here.
 *
 * See API.md#services — MemberInviteApi.
 */
class MemberInviteApiImpl(
    private val httpClient: HttpClient,
) : MemberInviteApi {

    override suspend fun createInvite(
        groupId: Long,
        request: CreateInviteRequestDto,
    ): NetworkResult<GeneratedInviteDto, NetworkError> {
        val path = "$INVITATIONS_PATH/$groupId"
        Logger.d(TAG) { "createInvite: POST $path" }
        return requestAsNetworkResult(op = "createInvite") {
            httpClient.post(path) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    override suspend fun listPendingInvites(groupId: Long): NetworkResult<List<PendingInviteDto>, NetworkError> {
        val path = "$INVITATIONS_PATH/$groupId"
        Logger.d(TAG) { "listPendingInvites: GET $path" }
        return requestAsNetworkResult(op = "listPendingInvites") {
            httpClient.get(path)
        }
    }

    override suspend fun revokeInvite(
        groupId: Long,
        rowId: Long,
    ): NetworkResult<RevokeInviteResponseDto, NetworkError> {
        val path = "$INVITATIONS_PATH/$groupId/$rowId"
        Logger.d(TAG) { "revokeInvite: DELETE $path" }
        return requestAsNetworkResult(op = "revokeInvite") {
            httpClient.delete(path)
        }
    }
}

/**
 * Executes [block], catches transport/serialization exceptions, and maps the resulting
 * [HttpResponse] status code into a [NetworkResult]. Mirrors the join-with-code stack's identical
 * helper (`InvitationApiImpl.requestAsNetworkResult`).
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
 * has no dedicated bucket for) intentionally falls into [NetworkError.UNKNOWN], same as the
 * join-with-code service's identical mapper.
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
