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
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * TDD RED-first coverage for [LoanDetailApi] / [LoanDetailApiImpl] — the single composite
 * `GET /loans/{loanId}` read backing the loan-detail screen
 * (`idea-layer/screens/loan-detail/api.yaml#api[get_loan_detail]`, `function: get_loan`). The
 * response bundles the loan header with its repayment schedule + transaction history in one
 * payload (`api.yaml#api[0].response.fields`) — no separate schedule/history endpoints are
 * declared, so this Service exposes exactly one method. Every method returns [NetworkResult] —
 * never a raw [Result] envelope, never a thrown exception (Mandatory Rule 2 / API.md#services).
 * Mirrors
 * [org.mifos.groupbanking.core.network.service.groupdashboard.GroupDashboardApiTest]'s MockEngine
 * harness pattern. This Service is SERVICE-ONLY — the Store5 wrapper + Repository consuming
 * [LoanDetailApi] are emitted by a downstream `kmp-store-gen`/`kmp-client-gen` generation step,
 * not covered here.
 */
class LoanDetailApiTest {

    private fun apiWith(
        status: HttpStatusCode,
        body: String,
        onRequestUrl: ((Url) -> Unit)? = null,
        onRequestMethod: ((HttpMethod) -> Unit)? = null,
    ): LoanDetailApi {
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
                json(Json { ignoreUnknownKeys = true; coerceInputValues = true })
            }
        }
        return LoanDetailApiImpl(client)
    }

    private val loanDetailBody = """
        {
          "loan": {
            "id": 501,
            "memberId": 12,
            "memberName": "Amina Yusuf",
            "loanProductName": "Business Growth Loan",
            "principalAmount": 500.0,
            "disbursedDate": "2026-04-01",
            "interestRatePercent": 12.5,
            "totalOutstanding": 320.0,
            "totalOverdue": 40.0,
            "status": "ACTIVE",
            "fineractLoanId": 9001
          },
          "repaymentSchedule": [
            {
              "weekNumber": 1,
              "dueDate": "2026-04-08",
              "dueAmount": 50.0,
              "paidAmount": 50.0,
              "balance": 0.0,
              "status": "PAID"
            }
          ],
          "transactions": [
            { "id": 1, "type": "REPAYMENT", "date": "2026-04-08", "amount": 50.0 }
          ]
        }
    """.trimIndent()

    // ---------- getLoanDetail ----------

    @Test
    fun getLoanDetail_success_returnsMappedDtoAndUsesGetOnLoanPath() = runTest {
        var capturedUrl: Url? = null
        var capturedMethod: HttpMethod? = null
        val api = apiWith(
            HttpStatusCode.OK,
            loanDetailBody,
            onRequestUrl = { capturedUrl = it },
            onRequestMethod = { capturedMethod = it },
        )

        val result = api.getLoanDetail(501L)

        check(result is NetworkResult.Success)
        assertEquals(501L, result.data.loan.id)
        assertEquals("Amina Yusuf", result.data.loan.memberName)
        assertEquals(1, result.data.repaymentSchedule.size)
        assertEquals(1, result.data.transactions.size)
        assertEquals("/loans/501", capturedUrl?.encodedPath)
        assertEquals("repaymentSchedule,transactions", capturedUrl?.parameters?.get("associations"))
        assertEquals(HttpMethod.Get, capturedMethod)
    }

    @Test
    fun getLoanDetail_unauthorized401_mapsToUnauthorizedError() = runTest {
        val api = apiWith(HttpStatusCode.Unauthorized, """{"error":"session expired"}""")

        val result = api.getLoanDetail(501L)

        assertEquals(NetworkResult.Error(NetworkError.UNAUTHORIZED), result)
    }

    @Test
    fun getLoanDetail_notFound404_mapsToNotFoundError() = runTest {
        val api = apiWith(HttpStatusCode.NotFound, """{"error":"loan not found"}""")

        val result = api.getLoanDetail(999L)

        assertEquals(NetworkResult.Error(NetworkError.NOT_FOUND), result)
    }

    @Test
    fun getLoanDetail_serverError500_mapsToServerError() = runTest {
        val api = apiWith(HttpStatusCode.InternalServerError, """{"error":"boom"}""")

        val result = api.getLoanDetail(501L)

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }

    @Test
    fun getLoanDetail_malformedJsonBody_mapsToSerializationError() = runTest {
        val api = apiWith(HttpStatusCode.OK, """not-json""")

        val result = api.getLoanDetail(501L)

        assertEquals(NetworkResult.Error(NetworkError.SERIALIZATION), result)
    }
}
