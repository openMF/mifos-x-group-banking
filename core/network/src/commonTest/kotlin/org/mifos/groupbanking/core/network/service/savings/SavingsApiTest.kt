/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.service.savings

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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * TDD RED-first coverage for [SavingsApi] / [SavingsApiImpl] — the shared savings client stack
 * consumed by personal-savings (`get_group_linked_transactions`/`get_individual_transactions`,
 * both `GET /self/savingsaccounts/{savingsId}/transactions`), member-savings-detail
 * (`get_member_savings_detail`, `GET /companion/groups/{groupId}/members/{memberId}/savings`),
 * and savings-dashboard (`get_group_savings_summary`/`get_individual_savings_summary`, `GET
 * /companion/groups/{groupId}/savings[/individual]`). Every method returns [NetworkResult] —
 * never a raw [Result] envelope, never a thrown exception (Mandatory Rule 2 / API.md#services).
 * Mirrors [org.mifos.groupbanking.core.network.service.loanlist.LoanApiTest]'s MockEngine harness
 * pattern.
 */
class SavingsApiTest {

    private fun apiWith(
        status: HttpStatusCode,
        body: String,
        onRequestUrl: ((Url) -> Unit)? = null,
        onRequestMethod: ((HttpMethod) -> Unit)? = null,
    ): SavingsApi {
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
        return SavingsApiImpl(client)
    }

    // ---------- getSavingsTransactions (personal-savings: group-linked AND individual, same shape) ----------

    private val oneLedgerEntryBody = """
        [
          {
            "id": 9001,
            "transactionType": {"value": 1, "code": "savingsAccountTransactionType.deposit", "description": "Deposit"},
            "date": [2026, 7, 15],
            "amount": 500.0,
            "runningBalance": 1500.0,
            "currency": {"code": "KES", "displaySymbol": "KSh"}
          }
        ]
    """.trimIndent()

    @Test
    fun getSavingsTransactions_success_returnsMappedListAndUsesGetOnSelfSavingsAccountsPathWithDefaultParams() = runTest {
        var capturedUrl: Url? = null
        var capturedMethod: HttpMethod? = null
        val api = apiWith(
            HttpStatusCode.OK,
            oneLedgerEntryBody,
            onRequestUrl = { capturedUrl = it },
            onRequestMethod = { capturedMethod = it },
        )

        val result = api.getSavingsTransactions(savingsId = 501L)

        check(result is NetworkResult.Success)
        assertEquals(1, result.data.size)
        assertEquals(9001L, result.data[0].id)
        assertEquals("/self/savingsaccounts/501/transactions", capturedUrl?.encodedPath)
        assertEquals(HttpMethod.Get, capturedMethod)
        assertEquals("50", capturedUrl?.parameters?.get("limit"))
        assertEquals("0", capturedUrl?.parameters?.get("offset"))
    }

    @Test
    fun getSavingsTransactions_explicitParams_threadsSavingsIdLimitAndOffsetIntoPathAndQuery() = runTest {
        var capturedUrl: Url? = null
        val api = apiWith(HttpStatusCode.OK, "[]", onRequestUrl = { capturedUrl = it })

        val result = api.getSavingsTransactions(savingsId = 777L, limit = 20, offset = 40)

        check(result is NetworkResult.Success)
        assertEquals("/self/savingsaccounts/777/transactions", capturedUrl?.encodedPath)
        assertEquals("20", capturedUrl?.parameters?.get("limit"))
        assertEquals("40", capturedUrl?.parameters?.get("offset"))
    }

    @Test
    fun getSavingsTransactions_individualAccountId_usesSamePathShapeAsGroupLinked() = runTest {
        // personal-savings reuses this single method for BOTH the group-linked and individual
        // accounts (api.yaml#api[get_group_linked_transactions,get_individual_transactions] share
        // the same endpoint template, differing only in the savingsId passed by the caller).
        var capturedUrl: Url? = null
        val api = apiWith(HttpStatusCode.OK, "[]", onRequestUrl = { capturedUrl = it })

        val result = api.getSavingsTransactions(savingsId = 888L)

        check(result is NetworkResult.Success)
        assertEquals("/self/savingsaccounts/888/transactions", capturedUrl?.encodedPath)
    }

    @Test
    fun getSavingsTransactions_emptyList_returnsSuccessWithEmptyList() = runTest {
        val api = apiWith(HttpStatusCode.OK, "[]")

        val result = api.getSavingsTransactions(savingsId = 501L)

        check(result is NetworkResult.Success)
        assertTrue(result.data.isEmpty())
    }

    @Test
    fun getSavingsTransactions_unauthorized401_mapsToUnauthorizedError() = runTest {
        val api = apiWith(HttpStatusCode.Unauthorized, """{"error":"session expired"}""")

        val result = api.getSavingsTransactions(savingsId = 501L)

        assertEquals(NetworkResult.Error(NetworkError.UNAUTHORIZED), result)
    }

    @Test
    fun getSavingsTransactions_accountNotFound404_mapsToNotFoundError() = runTest {
        val api = apiWith(HttpStatusCode.NotFound, """{"error":"account not found"}""")

        val result = api.getSavingsTransactions(savingsId = 999999L)

        assertEquals(NetworkResult.Error(NetworkError.NOT_FOUND), result)
    }

    @Test
    fun getSavingsTransactions_serverUnavailable503_mapsToServerError() = runTest {
        val api = apiWith(HttpStatusCode.ServiceUnavailable, """{"error":"unavailable"}""")

        val result = api.getSavingsTransactions(savingsId = 501L)

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }

    @Test
    fun getSavingsTransactions_malformedJsonBody_mapsToSerializationError() = runTest {
        val api = apiWith(HttpStatusCode.OK, """not-json""")

        val result = api.getSavingsTransactions(savingsId = 501L)

        assertEquals(NetworkResult.Error(NetworkError.SERIALIZATION), result)
    }

    // ---------- getMemberSavingsDetail (member-savings-detail companion single-read) ----------

    private val memberSavingsDetailBody = """
        {
          "member": {"memberId": "m-7", "displayName": "Amara Okafor", "photoUri": null},
          "savingsAccountNo": "SA-0007",
          "savingsBalance": 1500.0,
          "sharesHeld": 10,
          "shareValue": 1000,
          "sparklineData": [{"date": "2026-07-01", "balance": 1000.0}],
          "transactions": [
            {"id": "t-1", "date": "2026-07-15", "type": "DEPOSIT", "amount": 500.0, "runningBalance": 1500.0, "reversed": false}
          ],
          "totalTransactions": 1,
          "hasNextPage": false
        }
    """.trimIndent()

    @Test
    fun getMemberSavingsDetail_success_returnsMappedDetailAndUsesGetOnCompanionMemberSavingsPathWithDefaultParams() = runTest {
        var capturedUrl: Url? = null
        var capturedMethod: HttpMethod? = null
        val api = apiWith(
            HttpStatusCode.OK,
            memberSavingsDetailBody,
            onRequestUrl = { capturedUrl = it },
            onRequestMethod = { capturedMethod = it },
        )

        val result = api.getMemberSavingsDetail(groupId = "g-1", memberId = "m-7")

        check(result is NetworkResult.Success)
        assertEquals("SA-0007", result.data.savingsAccountNo)
        assertEquals(1, result.data.transactions.size)
        assertEquals("/companion/groups/g-1/members/m-7/savings", capturedUrl?.encodedPath)
        assertEquals(HttpMethod.Get, capturedMethod)
        assertEquals("20", capturedUrl?.parameters?.get("limit"))
        assertEquals("0", capturedUrl?.parameters?.get("offset"))
    }

    @Test
    fun getMemberSavingsDetail_explicitPaginationParams_threadsLimitAndOffsetForLoadMore() = runTest {
        var capturedUrl: Url? = null
        val api = apiWith(HttpStatusCode.OK, memberSavingsDetailBody, onRequestUrl = { capturedUrl = it })

        val result = api.getMemberSavingsDetail(groupId = "g-1", memberId = "m-7", limit = 20, offset = 20)

        check(result is NetworkResult.Success)
        assertEquals("20", capturedUrl?.parameters?.get("limit"))
        assertEquals("20", capturedUrl?.parameters?.get("offset"))
    }

    @Test
    fun getMemberSavingsDetail_forbidden403_mapsToUnknownError() = runTest {
        val api = apiWith(HttpStatusCode.Forbidden, """{"error":"forbidden"}""")

        val result = api.getMemberSavingsDetail(groupId = "g-1", memberId = "m-7")

        assertEquals(NetworkResult.Error(NetworkError.UNKNOWN), result)
    }

    @Test
    fun getMemberSavingsDetail_memberNotFound404_mapsToNotFoundError() = runTest {
        val api = apiWith(HttpStatusCode.NotFound, """{"error":"member or group not found"}""")

        val result = api.getMemberSavingsDetail(groupId = "g-1", memberId = "m-nope")

        assertEquals(NetworkResult.Error(NetworkError.NOT_FOUND), result)
    }

    @Test
    fun getMemberSavingsDetail_serverError500_mapsToServerError() = runTest {
        val api = apiWith(HttpStatusCode.InternalServerError, """{"error":"boom"}""")

        val result = api.getMemberSavingsDetail(groupId = "g-1", memberId = "m-7")

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }

    @Test
    fun getMemberSavingsDetail_malformedJsonBody_mapsToSerializationError() = runTest {
        val api = apiWith(HttpStatusCode.OK, """not-json""")

        val result = api.getMemberSavingsDetail(groupId = "g-1", memberId = "m-7")

        assertEquals(NetworkResult.Error(NetworkError.SERIALIZATION), result)
    }

    // ---------- getGroupSavingsSummary (savings-dashboard group tab) ----------

    private val groupSavingsSummaryBody = """
        {
          "cycleTarget": 50000,
          "cycleCollected": 32000,
          "totalCollected": 320000,
          "weeklyTrend": [{"weekLabel": "W1", "groupAmount": 8000, "individualAmount": 2000}],
          "memberRows": [
            {"memberId": "m-7", "name": "Amara Okafor", "totalContributed": 5000, "lastContribution": 500, "meetingsContributed": 10, "sharesHeld": null, "shareValue": null}
          ]
        }
    """.trimIndent()

    @Test
    fun getGroupSavingsSummary_success_returnsMappedSummaryAndUsesGetOnCompanionGroupSavingsPath() = runTest {
        var capturedUrl: Url? = null
        var capturedMethod: HttpMethod? = null
        val api = apiWith(
            HttpStatusCode.OK,
            groupSavingsSummaryBody,
            onRequestUrl = { capturedUrl = it },
            onRequestMethod = { capturedMethod = it },
        )

        val result = api.getGroupSavingsSummary(groupId = "g-1")

        check(result is NetworkResult.Success)
        assertEquals(50000L, result.data.cycleTarget)
        assertEquals(1, result.data.memberRows.size)
        assertEquals("/companion/groups/g-1/savings", capturedUrl?.encodedPath)
        assertEquals(HttpMethod.Get, capturedMethod)
    }

    @Test
    fun getGroupSavingsSummary_emptyMemberRowsAndTrend_returnsSuccessWithEmptyLists() = runTest {
        val api = apiWith(
            HttpStatusCode.OK,
            """{"cycleTarget":0,"cycleCollected":0,"totalCollected":0,"weeklyTrend":[],"memberRows":[]}""",
        )

        val result = api.getGroupSavingsSummary(groupId = "g-empty")

        check(result is NetworkResult.Success)
        assertTrue(result.data.memberRows.isEmpty())
        assertTrue(result.data.weeklyTrend.isEmpty())
    }

    @Test
    fun getGroupSavingsSummary_groupNotFound404_mapsToNotFoundError() = runTest {
        val api = apiWith(HttpStatusCode.NotFound, """{"error":"group not found"}""")

        val result = api.getGroupSavingsSummary(groupId = "g-missing")

        assertEquals(NetworkResult.Error(NetworkError.NOT_FOUND), result)
    }

    @Test
    fun getGroupSavingsSummary_unauthorized401_mapsToUnauthorizedError() = runTest {
        val api = apiWith(HttpStatusCode.Unauthorized, """{"error":"session expired"}""")

        val result = api.getGroupSavingsSummary(groupId = "g-1")

        assertEquals(NetworkResult.Error(NetworkError.UNAUTHORIZED), result)
    }

    @Test
    fun getGroupSavingsSummary_serverError500_mapsToServerError() = runTest {
        val api = apiWith(HttpStatusCode.InternalServerError, """{"error":"boom"}""")

        val result = api.getGroupSavingsSummary(groupId = "g-1")

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }

    @Test
    fun getGroupSavingsSummary_malformedJsonBody_mapsToSerializationError() = runTest {
        val api = apiWith(HttpStatusCode.OK, """not-json""")

        val result = api.getGroupSavingsSummary(groupId = "g-1")

        assertEquals(NetworkResult.Error(NetworkError.SERIALIZATION), result)
    }

    // ---------- getIndividualSavingsSummary (savings-dashboard individual tab) ----------

    private val individualSavingsSummaryBody = """
        {
          "totalBalance": 15000,
          "weeklyTrend": [{"weekLabel": "W1", "groupAmount": 8000, "individualAmount": 2000}],
          "memberRows": [
            {"memberId": "m-7", "name": "Amara Okafor", "currentBalance": 1500, "lastTransaction": 500, "lastTransactionDate": "2026-07-15"}
          ]
        }
    """.trimIndent()

    @Test
    fun getIndividualSavingsSummary_success_returnsMappedSummaryAndUsesGetOnCompanionIndividualPath() = runTest {
        var capturedUrl: Url? = null
        var capturedMethod: HttpMethod? = null
        val api = apiWith(
            HttpStatusCode.OK,
            individualSavingsSummaryBody,
            onRequestUrl = { capturedUrl = it },
            onRequestMethod = { capturedMethod = it },
        )

        val result = api.getIndividualSavingsSummary(groupId = "g-1")

        check(result is NetworkResult.Success)
        assertEquals(15000L, result.data.totalBalance)
        assertEquals(1, result.data.memberRows.size)
        assertEquals("/companion/groups/g-1/savings/individual", capturedUrl?.encodedPath)
        assertEquals(HttpMethod.Get, capturedMethod)
    }

    @Test
    fun getIndividualSavingsSummary_memberRowWithNullLastTransaction_mapsSuccessfully() = runTest {
        val api = apiWith(
            HttpStatusCode.OK,
            """{"totalBalance":0,"weeklyTrend":[],"memberRows":[{"memberId":"m-8","name":"Kofi Mensah","currentBalance":0,"lastTransaction":null,"lastTransactionDate":null}]}""",
        )

        val result = api.getIndividualSavingsSummary(groupId = "g-1")

        check(result is NetworkResult.Success)
        assertNull(result.data.memberRows[0].lastTransaction)
        assertNull(result.data.memberRows[0].lastTransactionDate)
    }

    @Test
    fun getIndividualSavingsSummary_groupNotFound404_mapsToNotFoundError() = runTest {
        val api = apiWith(HttpStatusCode.NotFound, """{"error":"group not found"}""")

        val result = api.getIndividualSavingsSummary(groupId = "g-missing")

        assertEquals(NetworkResult.Error(NetworkError.NOT_FOUND), result)
    }

    @Test
    fun getIndividualSavingsSummary_unauthorized401_mapsToUnauthorizedError() = runTest {
        val api = apiWith(HttpStatusCode.Unauthorized, """{"error":"session expired"}""")

        val result = api.getIndividualSavingsSummary(groupId = "g-1")

        assertEquals(NetworkResult.Error(NetworkError.UNAUTHORIZED), result)
    }

    @Test
    fun getIndividualSavingsSummary_serverError500_mapsToServerError() = runTest {
        val api = apiWith(HttpStatusCode.InternalServerError, """{"error":"boom"}""")

        val result = api.getIndividualSavingsSummary(groupId = "g-1")

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }

    @Test
    fun getIndividualSavingsSummary_malformedJsonBody_mapsToSerializationError() = runTest {
        val api = apiWith(HttpStatusCode.OK, """not-json""")

        val result = api.getIndividualSavingsSummary(groupId = "g-1")

        assertEquals(NetworkResult.Error(NetworkError.SERIALIZATION), result)
    }
}
