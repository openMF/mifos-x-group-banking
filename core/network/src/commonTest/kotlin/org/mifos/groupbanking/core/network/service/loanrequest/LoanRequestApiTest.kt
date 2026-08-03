/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.service.loanrequest

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
import org.mifos.groupbanking.core.network.model.LoanPurposeDto
import org.mifos.groupbanking.core.network.model.LoanRequestPayloadDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * TDD RED-first coverage for [LoanRequestApi] / [LoanRequestApiImpl] — the loan-request form's
 * single endpoint (`idea-layer/screens/loan-request/api.yaml#api[submit_loan_request]`). Returns
 * [NetworkResult] — never a raw [Result] envelope, never a thrown exception (Mandatory Rule 2 /
 * API.md#services). Mirrors
 * [org.mifos.groupbanking.core.network.service.loanapply.LoanApplyApiTest]'s MockEngine harness
 * pattern, including its `ContentConvertException` malformed-body coverage.
 */
class LoanRequestApiTest {

    private fun apiWith(
        status: HttpStatusCode,
        body: String,
        onRequestUrl: ((Url) -> Unit)? = null,
        onRequestMethod: ((HttpMethod) -> Unit)? = null,
    ): LoanRequestApi {
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
        return LoanRequestApiImpl(client)
    }

    private val requestDto = LoanRequestPayloadDto(
        clientId = 7L,
        requestedAmount = 1500.0,
        purpose = LoanPurposeDto.BUSINESS,
        durationWeeks = 12,
        savingsBalanceAtRequest = 5000.0,
        submittedAt = "2026-07-22T09:00:00Z",
    )

    private val responseBody = """
        { "resourceId": 501, "officeId": 1, "clientId": 7, "resourceExternalId": "LR-501" }
    """.trimIndent()

    // ---------- submitLoanRequest (submit_loan_request) ----------

    @Test
    fun submitLoanRequest_success_postsBodyOnDatatablePathAndReturnsMappedResponse() = runTest {
        var capturedUrl: Url? = null
        var capturedMethod: HttpMethod? = null
        val api = apiWith(
            HttpStatusCode.OK,
            responseBody,
            onRequestUrl = { capturedUrl = it },
            onRequestMethod = { capturedMethod = it },
        )

        val result = api.submitLoanRequest(requestDto)

        check(result is NetworkResult.Success)
        assertEquals(501L, result.data.resourceId)
        assertEquals(7L, result.data.clientId)
        assertEquals("LR-501", result.data.resourceExternalId)
        assertEquals("/datatables/dt_loan_request", capturedUrl?.encodedPath)
        assertEquals(HttpMethod.Post, capturedMethod)
    }

    @Test
    fun submitLoanRequest_validation400_mapsToBadRequestError() = runTest {
        val api = apiWith(HttpStatusCode.BadRequest, """{"error":"requestedAmount below floor"}""")

        val result = api.submitLoanRequest(requestDto)

        assertEquals(NetworkResult.Error(NetworkError.BAD_REQUEST), result)
    }

    @Test
    fun submitLoanRequest_sessionExpired401_mapsToUnauthorizedError() = runTest {
        val api = apiWith(HttpStatusCode.Unauthorized, """{"error":"session expired"}""")

        val result = api.submitLoanRequest(requestDto)

        assertEquals(NetworkResult.Error(NetworkError.UNAUTHORIZED), result)
        assertNull((result as? NetworkResult.Success)?.data)
    }

    @Test
    fun submitLoanRequest_duplicatePending409_mapsToUnknownError() = runTest {
        // 409 is not in core-base's status table (400/401/404/408/429/5xx) -> falls into the
        // documented else-branch, same as the loan-apply/loan-list 403 precedent.
        val api = apiWith(HttpStatusCode.Conflict, """{"error":"duplicate loan request pending"}""")

        val result = api.submitLoanRequest(requestDto)

        assertEquals(NetworkResult.Error(NetworkError.UNKNOWN), result)
    }

    @Test
    fun submitLoanRequest_serviceUnavailable503_mapsToServerErrorForCallerToEnqueueSync() = runTest {
        // api.yaml#errors.503: "Server unavailable — queue for sync" — this Service makes no
        // offline decision itself (Mandatory Rule 4 seam); it surfaces the raw NetworkError.SERVER
        // so LoanRequestRepositoryImpl's caller can enqueue to SyncQueueRepository.
        val api = apiWith(HttpStatusCode.ServiceUnavailable, """{"error":"unavailable"}""")

        val result = api.submitLoanRequest(requestDto)

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }

    @Test
    fun submitLoanRequest_serverError500_mapsToServerError() = runTest {
        val api = apiWith(HttpStatusCode.InternalServerError, """{"error":"boom"}""")

        val result = api.submitLoanRequest(requestDto)

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }

    @Test
    fun submitLoanRequest_malformedJsonBody_mapsToSerializationError() = runTest {
        // Covers the ContentConvertException gap LoanApplyApiTest first flagged (a malformed 2xx
        // body must map to NetworkError.SERIALIZATION, never propagate as an uncaught exception).
        val api = apiWith(HttpStatusCode.OK, "not-json")

        val result = api.submitLoanRequest(requestDto)

        assertEquals(NetworkResult.Error(NetworkError.SERIALIZATION), result)
    }
}
