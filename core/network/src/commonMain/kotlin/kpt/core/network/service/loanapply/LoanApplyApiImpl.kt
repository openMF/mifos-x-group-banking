/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.network.service.loanapply

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
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
import kpt.core.network.model.ApplyLoanRequestDto
import kpt.core.network.model.ApplyLoanResponseDto
import kpt.core.network.model.GroupCorpusRowDto
import kpt.core.network.model.GroupLoanConfigDto
import kpt.core.network.model.GroupMembersResponseDto
import kpt.core.network.model.LoanApplyTemplateDto
import kpt.core.network.model.LoanProductDto
import kpt.core.network.model.MemberSavingsResponseDto

private const val TAG = "LoanApplyApi"
private const val GROUPS_PATH = "/groups"
private const val LOAN_PRODUCTS_PATH = "/loanproducts"
private const val LOAN_TEMPLATE_PATH = "/loans/template"
private const val CLIENTS_PATH = "/clients"
private const val GROUP_CORPUS_DATATABLE_PATH = "/datatables/dt_group_corpus"
private const val GROUP_CONFIG_DATATABLE_PATH = "/datatables/dt_group_config"
private const val LOANS_PATH = "/loans"

/**
 * Plain-Ktor implementation of [LoanApplyApi]. This class is the ONLY layer in the loan-apply
 * client stack allowed a try-catch — every exception (transport failure, serialization failure)
 * is caught HERE and converted into [NetworkResult.Error]; nothing throws past this boundary
 * (Mandatory Rule 4 forbids try-catch one layer up, in `LoanApplyRepositoryImpl`). The
 * HTTP-status -> [NetworkError] mapping mirrors `core-base/network`'s
 * `ResultSuspendConverterFactory` table exactly, same as
 * [kpt.core.network.service.loanlist.LoanApiImpl] and
 * [kpt.core.network.service.memberadd.MemberAddApiImpl].
 *
 * Reuses the shared `HttpClient` singleton registered in `kpt.core.network.di.NetworkModule` — no
 * second engine constructed here. Auth-required requests are threaded through the shared client's
 * optional `bearerTokensProvider` plugin (see `core-base/network`'s `setupDefaultHttpClient`)
 * rather than a per-call header.
 *
 * See API.md#services — LoanApplyApi.
 */
class LoanApplyApiImpl(
    private val httpClient: HttpClient,
) : LoanApplyApi {

    override suspend fun getGroupMembers(groupId: Long): NetworkResult<GroupMembersResponseDto, NetworkError> {
        val path = "$GROUPS_PATH/$groupId"
        Logger.d(TAG) { "getGroupMembers: GET $path (associations=clientMembers)" }
        return requestAsNetworkResult(op = "getGroupMembers") {
            httpClient.get(path) {
                parameter("associations", "clientMembers")
            }
        }
    }

    override suspend fun getLoanProducts(): NetworkResult<List<LoanProductDto>, NetworkError> {
        Logger.d(TAG) { "getLoanProducts: GET $LOAN_PRODUCTS_PATH" }
        return requestAsNetworkResult(op = "getLoanProducts") {
            httpClient.get(LOAN_PRODUCTS_PATH)
        }
    }

    override suspend fun getLoanTemplate(
        clientId: Long,
        productId: Long,
        templateType: String,
    ): NetworkResult<LoanApplyTemplateDto, NetworkError> {
        Logger.d(TAG) {
            "getLoanTemplate: GET $LOAN_TEMPLATE_PATH (clientId=$clientId, productId=$productId, " +
                "templateType=$templateType)"
        }
        return requestAsNetworkResult(op = "getLoanTemplate") {
            httpClient.get(LOAN_TEMPLATE_PATH) {
                parameter("clientId", clientId)
                parameter("productId", productId)
                parameter("templateType", templateType)
            }
        }
    }

    override suspend fun getMemberSavings(clientId: Long): NetworkResult<MemberSavingsResponseDto, NetworkError> {
        val path = "$CLIENTS_PATH/$clientId/accounts"
        Logger.d(TAG) { "getMemberSavings: GET $path" }
        return requestAsNetworkResult(op = "getMemberSavings") {
            httpClient.get(path)
        }
    }

    override suspend fun getGroupCorpus(groupId: Long): NetworkResult<GroupCorpusRowDto, NetworkError> {
        val path = "$GROUP_CORPUS_DATATABLE_PATH/$groupId"
        Logger.d(TAG) { "getGroupCorpus: GET $path" }
        return requestAsNetworkResult(op = "getGroupCorpus") {
            httpClient.get(path)
        }
    }

    override suspend fun getGroupLoanConfig(groupId: Long): NetworkResult<GroupLoanConfigDto, NetworkError> {
        val path = "$GROUP_CONFIG_DATATABLE_PATH/$groupId"
        Logger.d(TAG) { "getGroupLoanConfig: GET $path" }
        return requestAsNetworkResult(op = "getGroupLoanConfig") {
            httpClient.get(path)
        }
    }

    override suspend fun applyLoan(
        request: ApplyLoanRequestDto,
    ): NetworkResult<ApplyLoanResponseDto, NetworkError> {
        Logger.d(TAG) {
            "applyLoan: POST $LOANS_PATH (clientId=${request.clientId}, productId=${request.productId}, " +
                "principal=${request.principal})"
        }
        return requestAsNetworkResult(op = "applyLoan") {
            httpClient.post(LOANS_PATH) {
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
            // Covers io.ktor.serialization.JsonConvertException (malformed JSON body, e.g.
            // "not-json") — a ContentConvertException subtype the shared
            // loanlist/memberadd precedent's requestAsNetworkResult helper does NOT catch
            // (confirmed pre-existing gap, flagged for a future upstream fix); caught here
            // explicitly so a malformed 2xx body maps to NetworkError.SERIALIZATION rather than
            // propagating as an uncaught exception past this service boundary (Mandatory Rule 4).
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
 * (400/401/404/408/429/5xx/else->UNKNOWN) — an unmapped code (e.g. 403 Forbidden) intentionally
 * falls into [NetworkError.UNKNOWN], same as that converter's `else` branch.
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
