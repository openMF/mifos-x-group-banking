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
import kpt.core.network.model.ApplyLoanRequestDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * TDD RED-first coverage for [LoanApplyApi] / [LoanApplyApiImpl] (loan-apply's 7 endpoints —
 * `idea-layer/screens/loan-apply/api.yaml#api`). Every method returns [NetworkResult] — never a
 * raw [Result] envelope, never a thrown exception (Mandatory Rule 2 / API.md#services). Mirrors
 * [kpt.core.network.service.loanlist.LoanApiTest]'s MockEngine harness
 * pattern. >= 3 cases per method (success + >=2 distinct error branches).
 */
class LoanApplyApiTest {

    private fun apiWith(
        status: HttpStatusCode,
        body: String,
        onRequestUrl: ((Url) -> Unit)? = null,
        onRequestMethod: ((HttpMethod) -> Unit)? = null,
    ): LoanApplyApi {
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
        return LoanApplyApiImpl(client)
    }

    // ---------- getGroupMembers (get_group_members) ----------

    private val groupMembersBody = """
        {
          "clientMembers": [
            { "id": 7, "displayName": "Amara Okafor", "imagePresent": true }
          ]
        }
    """.trimIndent()

    @Test
    fun getGroupMembers_success_usesGetOnGroupsAssociationsPathAndReturnsMappedRows() = runTest {
        var capturedUrl: Url? = null
        var capturedMethod: HttpMethod? = null
        val api = apiWith(
            HttpStatusCode.OK,
            groupMembersBody,
            onRequestUrl = { capturedUrl = it },
            onRequestMethod = { capturedMethod = it },
        )

        val result = api.getGroupMembers(groupId = 42L)

        check(result is NetworkResult.Success)
        assertEquals(1, result.data.clientMembers.size)
        assertEquals(7L, result.data.clientMembers[0].id)
        assertEquals("/groups/42", capturedUrl?.encodedPath)
        assertEquals("clientMembers", capturedUrl?.parameters?.get("associations"))
        assertEquals(HttpMethod.Get, capturedMethod)
    }

    @Test
    fun getGroupMembers_groupNotFound404_mapsToNotFoundError() = runTest {
        val api = apiWith(HttpStatusCode.NotFound, """{"error":"group not found"}""")

        val result = api.getGroupMembers(groupId = 12345L)

        assertEquals(NetworkResult.Error(NetworkError.NOT_FOUND), result)
    }

    @Test
    fun getGroupMembers_serverError500_mapsToServerError() = runTest {
        val api = apiWith(HttpStatusCode.InternalServerError, """{"error":"boom"}""")

        val result = api.getGroupMembers(groupId = 42L)

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }

    // ---------- getLoanProducts (get_loan_products) ----------

    private val loanProductsBody = """
        [
          {
            "id": 3, "name": "Group Loan - Standard", "shortName": "GLS",
            "principal": 500.0, "minPrincipal": 100.0, "maxPrincipal": 2000.0,
            "numberOfRepayments": 12, "interestRatePerPeriod": 1.5
          }
        ]
    """.trimIndent()

    @Test
    fun getLoanProducts_success_usesGetOnLoanProductsPathAndReturnsMappedList() = runTest {
        var capturedUrl: Url? = null
        var capturedMethod: HttpMethod? = null
        val api = apiWith(
            HttpStatusCode.OK,
            loanProductsBody,
            onRequestUrl = { capturedUrl = it },
            onRequestMethod = { capturedMethod = it },
        )

        val result = api.getLoanProducts()

        check(result is NetworkResult.Success)
        assertEquals(1, result.data.size)
        assertEquals(3L, result.data[0].id)
        assertEquals("/loanproducts", capturedUrl?.encodedPath)
        assertEquals(HttpMethod.Get, capturedMethod)
    }

    @Test
    fun getLoanProducts_emptyList_returnsSuccessWithEmptyList() = runTest {
        val api = apiWith(HttpStatusCode.OK, "[]")

        val result = api.getLoanProducts()

        check(result is NetworkResult.Success)
        assertTrue(result.data.isEmpty())
    }

    @Test
    fun getLoanProducts_serverError500_mapsToServerError() = runTest {
        val api = apiWith(HttpStatusCode.InternalServerError, """{"error":"boom"}""")

        val result = api.getLoanProducts()

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }

    // ---------- getLoanTemplate (get_loan_template) ----------

    private val loanTemplateBody = """
        {
          "principal": 500.0, "numberOfRepayments": 12, "interestRatePerPeriod": 1.5,
          "interestType": { "id": 0, "value": "Declining Balance" },
          "amortizationType": { "id": 1, "value": "Equal installments" },
          "repaymentEvery": 1
        }
    """.trimIndent()

    @Test
    fun getLoanTemplate_success_threadsClientIdProductIdAndDefaultTemplateTypeQueryParams() = runTest {
        var capturedUrl: Url? = null
        var capturedMethod: HttpMethod? = null
        val api = apiWith(
            HttpStatusCode.OK,
            loanTemplateBody,
            onRequestUrl = { capturedUrl = it },
            onRequestMethod = { capturedMethod = it },
        )

        val result = api.getLoanTemplate(clientId = 7L, productId = 3L)

        check(result is NetworkResult.Success)
        assertEquals(500.0, result.data.principal)
        assertEquals("/loans/template", capturedUrl?.encodedPath)
        assertEquals("7", capturedUrl?.parameters?.get("clientId"))
        assertEquals("3", capturedUrl?.parameters?.get("productId"))
        assertEquals("individual", capturedUrl?.parameters?.get("templateType"))
        assertEquals(HttpMethod.Get, capturedMethod)
    }

    @Test
    fun getLoanTemplate_notFound404_mapsToNotFoundError() = runTest {
        val api = apiWith(HttpStatusCode.NotFound, """{"error":"client not found"}""")

        val result = api.getLoanTemplate(clientId = 999L, productId = 3L)

        assertEquals(NetworkResult.Error(NetworkError.NOT_FOUND), result)
    }

    @Test
    fun getLoanTemplate_serverError500_mapsToServerError() = runTest {
        val api = apiWith(HttpStatusCode.InternalServerError, """{"error":"boom"}""")

        val result = api.getLoanTemplate(clientId = 7L, productId = 3L)

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }

    // ---------- getMemberSavings (get_member_savings) ----------

    private val memberSavingsBody = """
        {
          "savingsAccounts": [
            { "id": 11, "accountBalance": 250.0, "status": { "value": "Active" } },
            { "id": 12, "accountBalance": 100.0, "status": { "value": "Active" } }
          ]
        }
    """.trimIndent()

    @Test
    fun getMemberSavings_success_usesGetOnClientAccountsPathAndReturnsMappedRows() = runTest {
        var capturedUrl: Url? = null
        var capturedMethod: HttpMethod? = null
        val api = apiWith(
            HttpStatusCode.OK,
            memberSavingsBody,
            onRequestUrl = { capturedUrl = it },
            onRequestMethod = { capturedMethod = it },
        )

        val result = api.getMemberSavings(clientId = 7L)

        check(result is NetworkResult.Success)
        assertEquals(2, result.data.savingsAccounts.size)
        assertEquals("/clients/7/accounts", capturedUrl?.encodedPath)
        assertEquals(HttpMethod.Get, capturedMethod)
    }

    @Test
    fun getMemberSavings_emptyAccounts_returnsSuccessWithEmptyList() = runTest {
        val api = apiWith(HttpStatusCode.OK, """{"savingsAccounts":[]}""")

        val result = api.getMemberSavings(clientId = 7L)

        check(result is NetworkResult.Success)
        assertTrue(result.data.savingsAccounts.isEmpty())
    }

    @Test
    fun getMemberSavings_serverError500_mapsToServerError() = runTest {
        val api = apiWith(HttpStatusCode.InternalServerError, """{"error":"boom"}""")

        val result = api.getMemberSavings(clientId = 7L)

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }

    // ---------- getGroupCorpus (get_group_corpus) ----------

    private val groupCorpusBody = """{ "corpus_balance": 1500.0, "last_updated": "2026-07-01" }"""

    @Test
    fun getGroupCorpus_success_usesGetOnDatatablePathAndReturnsMappedRow() = runTest {
        var capturedUrl: Url? = null
        var capturedMethod: HttpMethod? = null
        val api = apiWith(
            HttpStatusCode.OK,
            groupCorpusBody,
            onRequestUrl = { capturedUrl = it },
            onRequestMethod = { capturedMethod = it },
        )

        val result = api.getGroupCorpus(groupId = 42L)

        check(result is NetworkResult.Success)
        assertEquals(1500.0, result.data.corpusBalance)
        assertEquals("/datatables/dt_group_corpus/42", capturedUrl?.encodedPath)
        assertEquals(HttpMethod.Get, capturedMethod)
    }

    @Test
    fun getGroupCorpus_groupNotFound404_mapsToNotFoundError() = runTest {
        val api = apiWith(HttpStatusCode.NotFound, """{"error":"not found"}""")

        val result = api.getGroupCorpus(groupId = 12345L)

        assertEquals(NetworkResult.Error(NetworkError.NOT_FOUND), result)
    }

    @Test
    fun getGroupCorpus_serverError500_mapsToServerError() = runTest {
        val api = apiWith(HttpStatusCode.InternalServerError, """{"error":"boom"}""")

        val result = api.getGroupCorpus(groupId = 42L)

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }

    // ---------- getGroupLoanConfig (get_group_config) ----------

    private val groupLoanConfigBody = """
        { "loan_multiplier": 3.0, "max_loan_amount": 2000.0, "meeting_frequency": "weekly" }
    """.trimIndent()

    @Test
    fun getGroupLoanConfig_success_usesGetOnDatatablePathAndReturnsMappedRow() = runTest {
        var capturedUrl: Url? = null
        var capturedMethod: HttpMethod? = null
        val api = apiWith(
            HttpStatusCode.OK,
            groupLoanConfigBody,
            onRequestUrl = { capturedUrl = it },
            onRequestMethod = { capturedMethod = it },
        )

        val result = api.getGroupLoanConfig(groupId = 42L)

        check(result is NetworkResult.Success)
        assertEquals(3.0, result.data.loanMultiplier)
        assertEquals("/datatables/dt_group_config/42", capturedUrl?.encodedPath)
        assertEquals(HttpMethod.Get, capturedMethod)
    }

    @Test
    fun getGroupLoanConfig_groupNotFound404_mapsToNotFoundError() = runTest {
        val api = apiWith(HttpStatusCode.NotFound, """{"error":"not found"}""")

        val result = api.getGroupLoanConfig(groupId = 12345L)

        assertEquals(NetworkResult.Error(NetworkError.NOT_FOUND), result)
    }

    @Test
    fun getGroupLoanConfig_serverError500_mapsToServerError() = runTest {
        val api = apiWith(HttpStatusCode.InternalServerError, """{"error":"boom"}""")

        val result = api.getGroupLoanConfig(groupId = 42L)

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }

    // ---------- applyLoan (post_loan / create_new_loan) ----------

    private val applyLoanRequestDto = ApplyLoanRequestDto(
        clientId = 7L,
        productId = 3L,
        principal = 500.0,
        loanTermFrequency = 12,
        numberOfRepayments = 12,
        interestRatePerPeriod = 1.5,
        expectedDisbursementDate = "22 July 2026",
        submittedOnDate = "22 July 2026",
        loanPurposeId = 1,
    )

    private val applyLoanResponseBody = """
        { "officeId": 1, "clientId": 7, "loanId": 501, "resourceId": 501 }
    """.trimIndent()

    @Test
    fun applyLoan_success_postsBodyOnLoansPathAndReturnsMappedResponse() = runTest {
        var capturedUrl: Url? = null
        var capturedMethod: HttpMethod? = null
        val api = apiWith(
            HttpStatusCode.OK,
            applyLoanResponseBody,
            onRequestUrl = { capturedUrl = it },
            onRequestMethod = { capturedMethod = it },
        )

        val result = api.applyLoan(applyLoanRequestDto)

        check(result is NetworkResult.Success)
        assertEquals(501L, result.data.loanId)
        assertEquals("/loans", capturedUrl?.encodedPath)
        assertEquals(HttpMethod.Post, capturedMethod)
    }

    @Test
    fun applyLoan_validation400_mapsToBadRequestError() = runTest {
        val api = apiWith(HttpStatusCode.BadRequest, """{"error":"amount exceeds eligibility"}""")

        val result = api.applyLoan(applyLoanRequestDto)

        assertEquals(NetworkResult.Error(NetworkError.BAD_REQUEST), result)
    }

    @Test
    fun applyLoan_forbidden403_mapsToUnknownError() = runTest {
        // 403 is not in core-base's status table (400/401/404/408/429/5xx) -> falls into the
        // documented else-branch, same as the loan-list/loan-detail precedent.
        val api = apiWith(HttpStatusCode.Forbidden, """{"error":"insufficient role"}""")

        val result = api.applyLoan(applyLoanRequestDto)

        assertEquals(NetworkResult.Error(NetworkError.UNKNOWN), result)
    }

    @Test
    fun applyLoan_serverError500_mapsToServerError() = runTest {
        val api = apiWith(HttpStatusCode.InternalServerError, """{"error":"boom"}""")

        val result = api.applyLoan(applyLoanRequestDto)

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }

    @Test
    fun applyLoan_malformedJsonBody_mapsToSerializationError() = runTest {
        val api = apiWith(HttpStatusCode.OK, "not-json")

        val result = api.applyLoan(applyLoanRequestDto)

        assertEquals(NetworkResult.Error(NetworkError.SERIALIZATION), result)
    }

    // ---------- transport-level (asserted once, cross-cutting per requestAsNetworkResult) ----------

    @Test
    fun getGroupMembers_unauthorized401_mapsToUnauthorizedError() = runTest {
        val api = apiWith(HttpStatusCode.Unauthorized, """{"error":"session expired"}""")

        val result = api.getGroupMembers(groupId = 42L)

        assertEquals(NetworkResult.Error(NetworkError.UNAUTHORIZED), result)
        assertNull((result as? NetworkResult.Success)?.data)
    }
}
