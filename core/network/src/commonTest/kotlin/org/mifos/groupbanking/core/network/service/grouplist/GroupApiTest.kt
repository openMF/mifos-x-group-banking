/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.service.grouplist

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
import kotlin.test.assertTrue

/**
 * TDD RED-first coverage for [GroupApi] / [GroupApiImpl] (COMP-GRP-001 —
 * `GET /companion/groups/mine`). Every method returns [NetworkResult] — never a raw [Result]
 * envelope, never a thrown exception (Mandatory Rule 2 / API.md#services). Mirrors
 * [org.mifos.groupbanking.core.network.service.grouptypepicker.GroupTypeConfigApiTest]'s
 * MockEngine harness pattern.
 */
class GroupApiTest {

    private fun apiWith(
        status: HttpStatusCode,
        body: String,
        onRequestUrl: ((Url) -> Unit)? = null,
        onRequestMethod: ((HttpMethod) -> Unit)? = null,
    ): GroupApi {
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
        return GroupApiImpl(client)
    }

    private val onePageBody = """
        {
          "totalFilteredRecords": 1,
          "pageItems": [
            {
              "id": "g-1", "name": "Sunrise VSLA", "groupType": "VSLA", "viewerRole": "MEMBER",
              "cycleNumber": 2, "memberCount": 18, "lastMeetingDate": "2026-07-01",
              "healthIndicator": "GREEN", "overdueRate": 0.02, "status": "ACTIVE",
              "fineractCenterId": 100
            }
          ]
        }
    """.trimIndent()

    // ---------- getMyGroups (COMP-GRP-001) ----------

    @Test
    fun getMyGroups_success_returnsMappedPageAndUsesGetOnMinePathWithDefaultParams() = runTest {
        var capturedUrl: Url? = null
        var capturedMethod: HttpMethod? = null
        val api = apiWith(
            HttpStatusCode.OK,
            onePageBody,
            onRequestUrl = { capturedUrl = it },
            onRequestMethod = { capturedMethod = it },
        )

        val result = api.getMyGroups()

        check(result is NetworkResult.Success)
        assertEquals(1, result.data.totalFilteredRecords)
        assertEquals(1, result.data.pageItems.size)
        assertEquals("g-1", result.data.pageItems[0].id)
        assertEquals("/companion/groups/mine", capturedUrl?.encodedPath)
        assertEquals(HttpMethod.Get, capturedMethod)
        assertEquals("true", capturedUrl?.parameters?.get("paged"))
        assertEquals("20", capturedUrl?.parameters?.get("limit"))
        assertEquals("0", capturedUrl?.parameters?.get("offset"))
    }

    @Test
    fun getMyGroups_explicitParams_threadsLimitAndOffsetAndPagedIntoQuery() = runTest {
        var capturedUrl: Url? = null
        val api = apiWith(
            HttpStatusCode.OK,
            """{"totalFilteredRecords":0,"pageItems":[]}""",
            onRequestUrl = { capturedUrl = it },
        )

        val result = api.getMyGroups(paged = false, limit = 50, offset = 100)

        check(result is NetworkResult.Success)
        assertEquals("false", capturedUrl?.parameters?.get("paged"))
        assertEquals("50", capturedUrl?.parameters?.get("limit"))
        assertEquals("100", capturedUrl?.parameters?.get("offset"))
    }

    @Test
    fun getMyGroups_emptyPageItems_returnsSuccessWithZeroRecords() = runTest {
        val api = apiWith(HttpStatusCode.OK, """{"totalFilteredRecords":0,"pageItems":[]}""")

        val result = api.getMyGroups()

        check(result is NetworkResult.Success)
        assertEquals(0, result.data.totalFilteredRecords)
        assertTrue(result.data.pageItems.isEmpty())
    }

    @Test
    fun getMyGroups_unauthorized401_mapsToUnauthorizedError() = runTest {
        val api = apiWith(HttpStatusCode.Unauthorized, """{"error":"session expired"}""")

        val result = api.getMyGroups()

        assertEquals(NetworkResult.Error(NetworkError.UNAUTHORIZED), result)
    }

    @Test
    fun getMyGroups_serverError500_mapsToServerError() = runTest {
        val api = apiWith(HttpStatusCode.InternalServerError, """{"error":"boom"}""")

        val result = api.getMyGroups()

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }

    @Test
    fun getMyGroups_malformedJsonBody_mapsToSerializationError() = runTest {
        val api = apiWith(HttpStatusCode.OK, """not-json""")

        val result = api.getMyGroups()

        assertEquals(NetworkResult.Error(NetworkError.SERIALIZATION), result)
    }
}
