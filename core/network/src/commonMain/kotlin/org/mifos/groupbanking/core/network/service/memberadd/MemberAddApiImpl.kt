/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.service.memberadd

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.append
import io.ktor.http.contentType
import io.ktor.serialization.ContentConvertException
import kotlinx.serialization.SerializationException
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.network.model.CreateMemberRequestDto
import org.mifos.groupbanking.core.network.model.CreateMemberResponseDto
import org.mifos.groupbanking.core.network.model.UpdateMemberRoleRequestDto
import org.mifos.groupbanking.core.network.model.UpdateMemberRoleResponseDto
import org.mifos.groupbanking.core.network.model.UploadMemberPhotoResponseDto

private const val TAG = "MemberAddApi"
private const val CLIENTS_PATH = "/clients"
private const val MEMBER_ROLE_DATATABLE_PATH = "/datatables/dt_member_role"

/**
 * Plain-Ktor implementation of [MemberAddApi]. This class is the ONLY layer in the member-add
 * client stack allowed a try-catch — every exception (transport failure, serialization failure)
 * is caught HERE and converted into [NetworkResult.Error]; nothing throws past this boundary
 * (Mandatory Rule 4 forbids try-catch one layer up, in
 * [org.mifos.groupbanking.core.data.repository.MemberAddRepositoryImpl]). The HTTP-status ->
 * [NetworkError] mapping mirrors `core-base/network`'s `ResultSuspendConverterFactory` table
 * exactly, same as
 * [org.mifos.groupbanking.core.network.service.groupcreate.GroupCreateApiImpl] /
 * [org.mifos.groupbanking.core.network.service.memberprofile.MemberProfileApiImpl].
 *
 * Reuses the shared `HttpClient` singleton registered in `kpt.core.network.di.NetworkModule`
 * (bound to the companion server base URL) — no second engine constructed here, even though
 * every path here is a raw Fineract passthrough (`/clients`, `/datatables/…`) rather than a
 * `/companion/…` one; both are served by the same host today, same convention as
 * [org.mifos.groupbanking.core.network.service.groupcreate.GroupCreateApiImpl.getOffices].
 *
 * See API.md#services — MemberAddApi.
 */
class MemberAddApiImpl(
    private val httpClient: HttpClient,
) : MemberAddApi {

    override suspend fun createClient(
        request: CreateMemberRequestDto,
    ): NetworkResult<CreateMemberResponseDto, NetworkError> {
        Logger.d(TAG) { "createClient: POST $CLIENTS_PATH" }
        return requestAsNetworkResult(op = "createClient") {
            httpClient.post(CLIENTS_PATH) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    override suspend fun assignMemberRole(
        clientId: String,
        request: UpdateMemberRoleRequestDto,
    ): NetworkResult<UpdateMemberRoleResponseDto, NetworkError> {
        val path = "$MEMBER_ROLE_DATATABLE_PATH/$clientId"
        Logger.d(TAG) { "assignMemberRole: POST $path" }
        return requestAsNetworkResult(op = "assignMemberRole") {
            httpClient.post(path) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    override suspend fun uploadMemberPhoto(
        clientId: String,
        photoBytes: ByteArray,
        fileName: String,
    ): NetworkResult<UploadMemberPhotoResponseDto, NetworkError> {
        val path = "$CLIENTS_PATH/$clientId/images"
        Logger.d(TAG) { "uploadMemberPhoto: POST $path (fileName=$fileName, bytes=${photoBytes.size})" }
        return requestAsNetworkResult(op = "uploadMemberPhoto") {
            httpClient.post(path) {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append(
                                key = "file",
                                value = photoBytes,
                                headers = Headers.build {
                                    append(HttpHeaders.ContentType, ContentType.Image.Any.toString())
                                    append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                                },
                            )
                        },
                    ),
                )
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
