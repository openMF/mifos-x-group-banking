/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.service.personaldashboard

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
 * TDD RED-first coverage for [MemberDashboardApi] / [MemberDashboardApiImpl]
 * (`GET /companion/member/dashboard` — `idea-layer/screens/personal-dashboard/api.yaml#api[0]`).
 * Every method returns [NetworkResult] — never a raw [Result] envelope, never a thrown exception
 * (Mandatory Rule 2 / API.md#services). Mirrors
 * [org.mifos.groupbanking.core.network.service.grouplist.GroupApiTest]'s MockEngine harness
 * pattern.
 */
class MemberDashboardApiTest {

    private fun apiWith(
        status: HttpStatusCode,
        body: String,
        onRequestUrl: ((Url) -> Unit)? = null,
        onRequestMethod: ((HttpMethod) -> Unit)? = null,
    ): MemberDashboardApi {
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
        return MemberDashboardApiImpl(client)
    }

    private val dashboardBody = """
        {
          "memberName": "Amina Yusuf",
          "myGroups": [
            { "groupId": "g-1", "name": "Sunrise VSLA", "poolModel": "ACCUMULATING" },
            { "groupId": "g-2", "name": "Baraka ROSCA", "poolModel": "ROTATING_PAYOUT" }
          ],
          "selectedGroup": { "groupId": "g-1", "name": "Sunrise VSLA", "poolModel": "ACCUMULATING" },
          "poolModel": "ACCUMULATING",
          "groupLinkedSavingsBalance": 1500.0,
          "individualSavingsBalance": 320.5,
          "shareOutProjection": 1820.5,
          "rotationPosition": null,
          "nextRecipientEta": null,
          "recentTransactions": [
            { "id": "t-1", "date": "2026-07-01", "type": "DEPOSIT", "amount": 50.0 }
          ]
        }
    """.trimIndent()

    // ---------- getMemberDashboard ----------

    @Test
    fun getMemberDashboard_success_returnsMappedDtoAndUsesGetOnDashboardPath() = runTest {
        var capturedUrl: Url? = null
        var capturedMethod: HttpMethod? = null
        val api = apiWith(
            HttpStatusCode.OK,
            dashboardBody,
            onRequestUrl = { capturedUrl = it },
            onRequestMethod = { capturedMethod = it },
        )

        val result = api.getMemberDashboard()

        check(result is NetworkResult.Success)
        assertEquals("Amina Yusuf", result.data.memberName)
        assertEquals(2, result.data.myGroups.size)
        assertEquals("g-1", result.data.selectedGroup.groupId)
        assertEquals(1, result.data.recentTransactions.size)
        assertEquals("/companion/member/dashboard", capturedUrl?.encodedPath)
        assertEquals(HttpMethod.Get, capturedMethod)
        assertNull(capturedUrl?.parameters?.get("selectedGroupId"))
    }

    @Test
    fun getMemberDashboard_withSelectedGroupId_threadsItIntoQuery() = runTest {
        var capturedUrl: Url? = null
        val api = apiWith(
            HttpStatusCode.OK,
            dashboardBody,
            onRequestUrl = { capturedUrl = it },
        )

        val result = api.getMemberDashboard(selectedGroupId = "g-2")

        check(result is NetworkResult.Success)
        assertEquals("g-2", capturedUrl?.parameters?.get("selectedGroupId"))
    }

    @Test
    fun getMemberDashboard_nullSelectedGroupId_omitsQueryParam() = runTest {
        var capturedUrl: Url? = null
        val api = apiWith(
            HttpStatusCode.OK,
            dashboardBody,
            onRequestUrl = { capturedUrl = it },
        )

        val result = api.getMemberDashboard(selectedGroupId = null)

        check(result is NetworkResult.Success)
        assertTrue(capturedUrl?.parameters?.names()?.contains("selectedGroupId") != true)
    }

    @Test
    fun getMemberDashboard_unauthorized401_mapsToUnauthorizedError() = runTest {
        val api = apiWith(HttpStatusCode.Unauthorized, """{"error":"session expired"}""")

        val result = api.getMemberDashboard()

        assertEquals(NetworkResult.Error(NetworkError.UNAUTHORIZED), result)
    }

    @Test
    fun getMemberDashboard_notFound404_mapsToNotFoundError() = runTest {
        val api = apiWith(HttpStatusCode.NotFound, """{"error":"member not found in any group"}""")

        val result = api.getMemberDashboard()

        assertEquals(NetworkResult.Error(NetworkError.NOT_FOUND), result)
    }

    @Test
    fun getMemberDashboard_serverError500_mapsToServerError() = runTest {
        val api = apiWith(HttpStatusCode.InternalServerError, """{"error":"boom"}""")

        val result = api.getMemberDashboard()

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }

    @Test
    fun getMemberDashboard_malformedJsonBody_mapsToSerializationError() = runTest {
        val api = apiWith(HttpStatusCode.OK, """not-json""")

        val result = api.getMemberDashboard()

        assertEquals(NetworkResult.Error(NetworkError.SERIALIZATION), result)
    }
}
