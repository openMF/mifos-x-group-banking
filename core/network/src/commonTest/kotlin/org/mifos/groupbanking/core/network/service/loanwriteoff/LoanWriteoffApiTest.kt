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
import org.mifos.groupbanking.core.network.model.WriteoffLoanRequestDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD RED-first coverage for [LoanWriteoffApi] / [LoanWriteoffApiImpl] (`write_off_loan`,
 * `POST /loans/{loanId}/transactions?command=writeoff` —
 * `idea-layer/screens/loan-mark-defaulted-dialog/api.yaml`). Every method returns [NetworkResult]
 * — never a raw [Result] envelope, never a thrown exception (Mandatory Rule 2). Mirrors
 * [org.mifos.groupbanking.core.network.service.loanrepayment.LoanRepaymentApiTest]'s MockEngine
 * harness pattern.
 */
class LoanWriteoffApiTest {

    private fun apiWith(
        status: HttpStatusCode,
        body: String,
        onRequestUrl: ((Url) -> Unit)? = null,
        onRequestMethod: ((HttpMethod) -> Unit)? = null,
        onRequestBody: ((String) -> Unit)? = null,
    ): LoanWriteoffApi {
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
                json(Json { ignoreUnknownKeys = true; coerceInputValues = true })
            }
        }
        return LoanWriteoffApiImpl(client)
    }

    private val requestDto = WriteoffLoanRequestDto(
        transactionDate = "21 July 2026",
    )

    private val responseBody = """
        { "officeId": 1, "clientId": 9, "loanId": 500, "resourceId": 888 }
    """.trimIndent()

    @Test
    fun writeoffLoan_success_postsCommandWriteoffQueryParamOnLoansTransactionsPathAndReturnsMappedResponse() = runTest {
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

        val result = api.writeoffLoan(loanId = 500L, request = requestDto)

        check(result is NetworkResult.Success)
        assertEquals(1, result.data.officeId)
        assertEquals(9L, result.data.clientId)
        assertEquals(500L, result.data.loanId)
        assertEquals(888L, result.data.resourceId)
        assertEquals("/loans/500/transactions", capturedUrl?.encodedPath)
        assertEquals("writeoff", capturedUrl?.parameters?.get("command"))
        assertEquals(HttpMethod.Post, capturedMethod)
        assertTrue(capturedBody!!.contains("21 July 2026"))
    }

    @Test
    fun writeoffLoan_unauthorized401_mapsToUnauthorizedError() = runTest {
        val api = apiWith(HttpStatusCode.Unauthorized, """{"error":"session expired"}""")

        val result = api.writeoffLoan(loanId = 500L, request = requestDto)

        assertEquals(NetworkResult.Error(NetworkError.UNAUTHORIZED), result)
    }

    @Test
    fun writeoffLoan_forbidden403_mapsToUnknownError() = runTest {
        // 403 has no dedicated NetworkError bucket (chairperson-role check) — mirrors
        // ResultSuspendConverterFactory's else-branch fallback, same convention as
        // LoanRepaymentApiImpl.recordRepayment_forbidden403.
        val api = apiWith(HttpStatusCode.Forbidden, """{"error":"chairperson role required"}""")

        val result = api.writeoffLoan(loanId = 500L, request = requestDto)

        assertEquals(NetworkResult.Error(NetworkError.UNKNOWN), result)
    }

    @Test
    fun writeoffLoan_notFound404_mapsToNotFoundError() = runTest {
        val api = apiWith(HttpStatusCode.NotFound, """{"error":"loan not found"}""")

        val result = api.writeoffLoan(loanId = 999999L, request = requestDto)

        assertEquals(NetworkResult.Error(NetworkError.NOT_FOUND), result)
    }

    @Test
    fun writeoffLoan_conflict409_mapsToUnknownError() = runTest {
        // 409 (loan not in an eligible state for write-off) has no dedicated NetworkError bucket
        // either — same else-branch fallback as 403 above.
        val api = apiWith(HttpStatusCode.Conflict, """{"error":"loan is not in an eligible state"}""")

        val result = api.writeoffLoan(loanId = 500L, request = requestDto)

        assertEquals(NetworkResult.Error(NetworkError.UNKNOWN), result)
    }

    @Test
    fun writeoffLoan_serverError500_mapsToServerError() = runTest {
        val api = apiWith(HttpStatusCode.InternalServerError, """{"error":"boom"}""")

        val result = api.writeoffLoan(loanId = 500L, request = requestDto)

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }
}
