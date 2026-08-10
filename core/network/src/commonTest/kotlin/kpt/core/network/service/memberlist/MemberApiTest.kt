/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.network.service.memberlist

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
 * TDD RED-first coverage for [MemberApi] / [MemberApiImpl] (member-list's `GET
 * /groups/{groupId}/clients`, `idea-layer/screens/member-list/api.yaml#api[get_group_members]`).
 * Every method returns [NetworkResult] — never a raw [Result] envelope, never a thrown exception
 * (Mandatory Rule 2 / API.md#services). Mirrors
 * [kpt.core.network.service.grouplist.GroupApiTest]'s MockEngine harness
 * pattern.
 */
class MemberApiTest {

    private fun apiWith(
        status: HttpStatusCode,
        body: String,
        onRequestUrl: ((Url) -> Unit)? = null,
        onRequestMethod: ((HttpMethod) -> Unit)? = null,
    ): MemberApi {
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
        return MemberApiImpl(client)
    }

    private val onePageBody = """
        {
          "totalFilteredRecords": 1,
          "pageItems": [
            {
              "id": "m-1", "fineractClientId": 501, "displayName": "Amara Okafor",
              "photoUri": null, "role": "TREASURER", "savingsBalance": 125.50,
              "loanStatus": "ACTIVE"
            }
          ]
        }
    """.trimIndent()

    // ---------- getGroupMembers (get_group_members) ----------

    @Test
    fun getGroupMembers_success_returnsMappedPageAndUsesGetOnClientsPathWithDefaultParams() = runTest {
        var capturedUrl: Url? = null
        var capturedMethod: HttpMethod? = null
        val api = apiWith(
            HttpStatusCode.OK,
            onePageBody,
            onRequestUrl = { capturedUrl = it },
            onRequestMethod = { capturedMethod = it },
        )

        val result = api.getGroupMembers(groupId = "42")

        check(result is NetworkResult.Success)
        assertEquals(1, result.data.totalFilteredRecords)
        assertEquals(1, result.data.pageItems.size)
        assertEquals("m-1", result.data.pageItems[0].id)
        assertEquals("/groups/42/clients", capturedUrl?.encodedPath)
        assertEquals(HttpMethod.Get, capturedMethod)
        assertEquals("20", capturedUrl?.parameters?.get("limit"))
        assertEquals("0", capturedUrl?.parameters?.get("offset"))
    }

    @Test
    fun getGroupMembers_explicitParams_threadsGroupIdLimitAndOffsetIntoPathAndQuery() = runTest {
        var capturedUrl: Url? = null
        val api = apiWith(
            HttpStatusCode.OK,
            """{"totalFilteredRecords":0,"pageItems":[]}""",
            onRequestUrl = { capturedUrl = it },
        )

        val result = api.getGroupMembers(groupId = "99", limit = 50, offset = 100)

        check(result is NetworkResult.Success)
        assertEquals("/groups/99/clients", capturedUrl?.encodedPath)
        assertEquals("50", capturedUrl?.parameters?.get("limit"))
        assertEquals("100", capturedUrl?.parameters?.get("offset"))
    }

    @Test
    fun getGroupMembers_emptyPageItems_returnsSuccessWithZeroRecords() = runTest {
        val api = apiWith(HttpStatusCode.OK, """{"totalFilteredRecords":0,"pageItems":[]}""")

        val result = api.getGroupMembers(groupId = "42")

        check(result is NetworkResult.Success)
        assertEquals(0, result.data.totalFilteredRecords)
        assertTrue(result.data.pageItems.isEmpty())
    }

    @Test
    fun getGroupMembers_unauthorized401_mapsToUnauthorizedError() = runTest {
        val api = apiWith(HttpStatusCode.Unauthorized, """{"error":"session expired"}""")

        val result = api.getGroupMembers(groupId = "42")

        assertEquals(NetworkResult.Error(NetworkError.UNAUTHORIZED), result)
    }

    @Test
    fun getGroupMembers_groupNotFound404_mapsToNotFoundError() = runTest {
        val api = apiWith(HttpStatusCode.NotFound, """{"error":"group not found"}""")

        val result = api.getGroupMembers(groupId = "does-not-exist")

        assertEquals(NetworkResult.Error(NetworkError.NOT_FOUND), result)
    }

    @Test
    fun getGroupMembers_serverError500_mapsToServerError() = runTest {
        val api = apiWith(HttpStatusCode.InternalServerError, """{"error":"boom"}""")

        val result = api.getGroupMembers(groupId = "42")

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }

    @Test
    fun getGroupMembers_malformedJsonBody_mapsToSerializationError() = runTest {
        val api = apiWith(HttpStatusCode.OK, """not-json""")

        val result = api.getGroupMembers(groupId = "42")

        assertEquals(NetworkResult.Error(NetworkError.SERIALIZATION), result)
    }
}
