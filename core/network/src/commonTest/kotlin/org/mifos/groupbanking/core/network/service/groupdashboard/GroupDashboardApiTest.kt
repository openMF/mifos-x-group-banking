/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.service.groupdashboard

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
 * TDD RED-first coverage for [GroupDashboardApi] / [GroupDashboardApiImpl] — the FOUR
 * COMP-GRP-001 companion reads driving the group-dashboard screen (`get_group`,
 * `get_viewer_role`, `get_group_corpus`, `get_group_accounts` —
 * `idea-layer/screens/group-dashboard/api.yaml#api`). Every method returns [NetworkResult] —
 * never a raw [Result] envelope, never a thrown exception (Mandatory Rule 2 / API.md#services).
 * Mirrors [org.mifos.groupbanking.core.network.service.personaldashboard.MemberDashboardApiTest]'s
 * MockEngine harness pattern. This Service is SERVICE-ONLY — the 4-way parallel-combine +
 * Store5 wrapping is a downstream `kmp-store-gen` generation step, not covered here.
 */
class GroupDashboardApiTest {

    private fun apiWith(
        status: HttpStatusCode,
        body: String,
        onRequestUrl: ((Url) -> Unit)? = null,
        onRequestMethod: ((HttpMethod) -> Unit)? = null,
    ): GroupDashboardApi {
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
        return GroupDashboardApiImpl(client)
    }

    private val groupBody = """
        {
          "id": "g-1",
          "fineractGroupId": 42,
          "name": "Sunrise VSLA",
          "cycleNumber": 3,
          "cycleLengthMonths": 12,
          "meetingFrequency": "WEEKLY",
          "memberCount": 18,
          "overdueLoansCount": 2,
          "status": "ACTIVE",
          "typeConfig": {
            "group_type": "VSLA",
            "pool_model": "ACCUMULATING",
            "contribution_model": "FIXED_AMOUNT",
            "shareout_formula": "PRO_RATA",
            "payout_order_method": "SEQUENTIAL",
            "share_value": 10.0,
            "contribution_amount": 50.0,
            "social_fund_enabled": true,
            "cycle_length_months": 12,
            "loan_multiplier": 3.0,
            "interest_rate": 0.02,
            "fine_amount": 5.0
          }
        }
    """.trimIndent()

    private val viewerRoleBody = """{ "role": "TREASURER", "memberId": 501 }"""

    private val corpusBody = """
        {
          "currentBalance": 1820.5,
          "openingBalance": 1000.0,
          "totalContributionsThisCycle": 900.0,
          "totalLoansOutstanding": 250.0,
          "lastUpdated": "2026-07-20T10:00:00Z",
          "rotationPosition": null,
          "nextRecipientName": null,
          "nextRecipientPosition": null
        }
    """.trimIndent()

    private val accountsBody = """
        {
          "savingsBalance": 1500.0,
          "loansOutstanding": 250.0,
          "activeLoanCount": 3,
          "shareOutProjection": 1820.5,
          "recentActivity": [
            { "id": "a-1", "type": "DEPOSIT", "description": "Weekly contribution", "amount": 50.0, "date": "2026-07-20", "memberName": "Amina Yusuf" }
          ]
        }
    """.trimIndent()

    // ---------- getGroup ----------

    @Test
    fun getGroup_success_returnsMappedDtoAndUsesGetOnGroupPath() = runTest {
        var capturedUrl: Url? = null
        var capturedMethod: HttpMethod? = null
        val api = apiWith(
            HttpStatusCode.OK,
            groupBody,
            onRequestUrl = { capturedUrl = it },
            onRequestMethod = { capturedMethod = it },
        )

        val result = api.getGroup("g-1")

        check(result is NetworkResult.Success)
        assertEquals("g-1", result.data.id)
        assertEquals("Sunrise VSLA", result.data.name)
        assertEquals("/companion/groups/g-1", capturedUrl?.encodedPath)
        assertEquals(HttpMethod.Get, capturedMethod)
    }

    @Test
    fun getGroup_unauthorized401_mapsToUnauthorizedError() = runTest {
        val api = apiWith(HttpStatusCode.Unauthorized, """{"error":"session expired"}""")

        val result = api.getGroup("g-1")

        assertEquals(NetworkResult.Error(NetworkError.UNAUTHORIZED), result)
    }

    @Test
    fun getGroup_notFound404_mapsToNotFoundError() = runTest {
        val api = apiWith(HttpStatusCode.NotFound, """{"error":"group not found"}""")

        val result = api.getGroup("g-missing")

        assertEquals(NetworkResult.Error(NetworkError.NOT_FOUND), result)
    }

    @Test
    fun getGroup_serverError500_mapsToServerError() = runTest {
        val api = apiWith(HttpStatusCode.InternalServerError, """{"error":"boom"}""")

        val result = api.getGroup("g-1")

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }

    @Test
    fun getGroup_malformedJsonBody_mapsToSerializationError() = runTest {
        val api = apiWith(HttpStatusCode.OK, """not-json""")

        val result = api.getGroup("g-1")

        assertEquals(NetworkResult.Error(NetworkError.SERIALIZATION), result)
    }

    // ---------- getViewerRole ----------

    @Test
    fun getViewerRole_success_returnsMappedDtoAndUsesGetOnMyRolePath() = runTest {
        var capturedUrl: Url? = null
        var capturedMethod: HttpMethod? = null
        val api = apiWith(
            HttpStatusCode.OK,
            viewerRoleBody,
            onRequestUrl = { capturedUrl = it },
            onRequestMethod = { capturedMethod = it },
        )

        val result = api.getViewerRole("g-1")

        check(result is NetworkResult.Success)
        assertEquals(501L, result.data.memberId)
        assertEquals("/companion/groups/g-1/my-role", capturedUrl?.encodedPath)
        assertEquals(HttpMethod.Get, capturedMethod)
    }

    @Test
    fun getViewerRole_unauthorized401_mapsToUnauthorizedError() = runTest {
        val api = apiWith(HttpStatusCode.Unauthorized, """{"error":"session expired"}""")

        val result = api.getViewerRole("g-1")

        assertEquals(NetworkResult.Error(NetworkError.UNAUTHORIZED), result)
    }

    @Test
    fun getViewerRole_notFound404_mapsToNotFoundError() = runTest {
        val api = apiWith(HttpStatusCode.NotFound, """{"error":"not a member of this group"}""")

        val result = api.getViewerRole("g-1")

        assertEquals(NetworkResult.Error(NetworkError.NOT_FOUND), result)
    }

    @Test
    fun getViewerRole_serverError500_mapsToServerError() = runTest {
        val api = apiWith(HttpStatusCode.InternalServerError, """{"error":"boom"}""")

        val result = api.getViewerRole("g-1")

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }

    // ---------- getGroupCorpus ----------

    @Test
    fun getGroupCorpus_success_returnsMappedDtoAndUsesGetOnCorpusPath() = runTest {
        var capturedUrl: Url? = null
        var capturedMethod: HttpMethod? = null
        val api = apiWith(
            HttpStatusCode.OK,
            corpusBody,
            onRequestUrl = { capturedUrl = it },
            onRequestMethod = { capturedMethod = it },
        )

        val result = api.getGroupCorpus("g-1")

        check(result is NetworkResult.Success)
        assertEquals(1820.5, result.data.currentBalance)
        assertEquals("/companion/groups/g-1/corpus", capturedUrl?.encodedPath)
        assertEquals(HttpMethod.Get, capturedMethod)
    }

    @Test
    fun getGroupCorpus_notFound404_mapsToNotFoundError() = runTest {
        val api = apiWith(HttpStatusCode.NotFound, """{"error":"corpus record not found"}""")

        val result = api.getGroupCorpus("g-1")

        assertEquals(NetworkResult.Error(NetworkError.NOT_FOUND), result)
    }

    @Test
    fun getGroupCorpus_serverError500_mapsToServerError() = runTest {
        val api = apiWith(HttpStatusCode.InternalServerError, """{"error":"boom"}""")

        val result = api.getGroupCorpus("g-1")

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }

    @Test
    fun getGroupCorpus_malformedJsonBody_mapsToSerializationError() = runTest {
        val api = apiWith(HttpStatusCode.OK, """not-json""")

        val result = api.getGroupCorpus("g-1")

        assertEquals(NetworkResult.Error(NetworkError.SERIALIZATION), result)
    }

    // ---------- getGroupAccounts ----------

    @Test
    fun getGroupAccounts_success_returnsMappedDtoWithRecentActivityAndUsesGetOnAccountsPath() = runTest {
        var capturedUrl: Url? = null
        var capturedMethod: HttpMethod? = null
        val api = apiWith(
            HttpStatusCode.OK,
            accountsBody,
            onRequestUrl = { capturedUrl = it },
            onRequestMethod = { capturedMethod = it },
        )

        val result = api.getGroupAccounts("g-1")

        check(result is NetworkResult.Success)
        assertEquals(1, result.data.recentActivity.size)
        assertEquals("a-1", result.data.recentActivity.first().id)
        assertEquals("/companion/groups/g-1/accounts", capturedUrl?.encodedPath)
        assertEquals(HttpMethod.Get, capturedMethod)
    }

    @Test
    fun getGroupAccounts_unauthorized401_mapsToUnauthorizedError() = runTest {
        val api = apiWith(HttpStatusCode.Unauthorized, """{"error":"session expired"}""")

        val result = api.getGroupAccounts("g-1")

        assertEquals(NetworkResult.Error(NetworkError.UNAUTHORIZED), result)
    }

    @Test
    fun getGroupAccounts_notFound404_mapsToNotFoundError() = runTest {
        val api = apiWith(HttpStatusCode.NotFound, """{"error":"group not found"}""")

        val result = api.getGroupAccounts("g-1")

        assertEquals(NetworkResult.Error(NetworkError.NOT_FOUND), result)
    }

    @Test
    fun getGroupAccounts_serverError500_mapsToServerError() = runTest {
        val api = apiWith(HttpStatusCode.InternalServerError, """{"error":"boom"}""")

        val result = api.getGroupAccounts("g-1")

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }
}
