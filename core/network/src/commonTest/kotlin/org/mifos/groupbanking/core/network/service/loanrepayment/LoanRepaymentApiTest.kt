/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.service.loanrepayment

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.network.model.RecordRepaymentRequestDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD RED-first coverage for [LoanRepaymentApi] / [LoanRepaymentApiImpl] (`make_repayment`,
 * `POST /loans/{loanId}/transactions?command=repayment` —
 * `idea-layer/screens/loan-repayment-dialog/api.yaml`). Every method returns [NetworkResult] —
 * never a raw [Result] envelope, never a thrown exception (Mandatory Rule 2). Mirrors
 * [org.mifos.groupbanking.core.network.service.groupcreate.GroupCreateApiTest]'s MockEngine
 * harness pattern.
 */
class LoanRepaymentApiTest {

    private fun apiWith(
        status: HttpStatusCode,
        body: String,
        onRequestUrl: ((Url) -> Unit)? = null,
        onRequestMethod: ((HttpMethod) -> Unit)? = null,
        onRequestBody: ((String) -> Unit)? = null,
    ): LoanRepaymentApi {
        val engine = MockEngine { request ->
            onRequestUrl?.invoke(request.url)
            onRequestMethod?.invoke(request.method)
            onRequestBody?.invoke((request.body as? OutgoingContent.ByteArrayContent)?.bytes()?.decodeToString() ?: "")
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
        return LoanRepaymentApiImpl(client)
    }

    private val requestDto = RecordRepaymentRequestDto(
        transactionDate = "21 July 2026",
        transactionAmount = 250.0,
        paymentTypeId = 2,
        receiptNumber = "RCPT-9001",
    )

    private val responseBody = """
        { "officeId": 1, "clientId": 9, "loanId": 500, "resourceId": 777 }
    """.trimIndent()

    @Test
    fun recordRepayment_success_postsCommandRepaymentQueryParamOnLoansTransactionsPathAndReturnsMappedResponse() = runTest {
        var capturedUrl: Url? = null
        var capturedMethod: HttpMethod? = null
        var capturedBody: String? = null
        val api = apiWith(
            HttpStatusCode.OK,
            responseBody,
            onRequestUrl = { capturedUrl = it },
            onRequestMethod = { capturedMethod = it },
            onRequestBody = { capturedBody = it },
        )

        val result = api.recordRepayment(loanId = 500L, request = requestDto)

        check(result is NetworkResult.Success)
        assertEquals(1, result.data.officeId)
        assertEquals(9L, result.data.clientId)
        assertEquals(500L, result.data.loanId)
        assertEquals(777L, result.data.resourceId)
        assertEquals("/loans/500/transactions", capturedUrl?.encodedPath)
        assertEquals("repayment", capturedUrl?.parameters?.get("command"))
        assertEquals(HttpMethod.Post, capturedMethod)
        assertTrue(capturedBody!!.contains("250.0"))
        assertTrue(capturedBody!!.contains("RCPT-9001"))
    }

    @Test
    fun recordRepayment_validation400_mapsToBadRequestError() = runTest {
        val api = apiWith(HttpStatusCode.BadRequest, """{"error":"amount exceeds outstanding"}""")

        val result = api.recordRepayment(loanId = 500L, request = requestDto)

        assertEquals(NetworkResult.Error(NetworkError.BAD_REQUEST), result)
    }

    @Test
    fun recordRepayment_unauthorized401_mapsToUnauthorizedError() = runTest {
        val api = apiWith(HttpStatusCode.Unauthorized, """{"error":"session expired"}""")

        val result = api.recordRepayment(loanId = 500L, request = requestDto)

        assertEquals(NetworkResult.Error(NetworkError.UNAUTHORIZED), result)
    }

    @Test
    fun recordRepayment_notFound404_mapsToNotFoundError() = runTest {
        val api = apiWith(HttpStatusCode.NotFound, """{"error":"loan not found"}""")

        val result = api.recordRepayment(loanId = 999999L, request = requestDto)

        assertEquals(NetworkResult.Error(NetworkError.NOT_FOUND), result)
    }

    @Test
    fun recordRepayment_forbidden403_mapsToUnknownError() = runTest {
        // 403 has no dedicated NetworkError bucket — mirrors ResultSuspendConverterFactory's
        // else-branch fallback (same convention as GroupCreateApiImpl / InvitationApiImpl).
        val api = apiWith(HttpStatusCode.Forbidden, """{"error":"role check failed"}""")

        val result = api.recordRepayment(loanId = 500L, request = requestDto)

        assertEquals(NetworkResult.Error(NetworkError.UNKNOWN), result)
    }

    @Test
    fun recordRepayment_serverError500_mapsToServerError() = runTest {
        val api = apiWith(HttpStatusCode.InternalServerError, """{"error":"boom"}""")

        val result = api.recordRepayment(loanId = 500L, request = requestDto)

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }
}
