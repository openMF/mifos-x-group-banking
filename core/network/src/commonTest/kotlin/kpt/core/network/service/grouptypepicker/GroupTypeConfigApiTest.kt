/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.network.service.grouptypepicker

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.network.model.GroupTypeSlugDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD RED-first coverage for [GroupTypeConfigApi] / [GroupTypeConfigApiImpl] (COMP-DT-003).
 * Every method returns [NetworkResult] — never a raw [Result] envelope, never a thrown
 * exception (Mandatory Rule 2 / API.md#services). Mirrors [CompanionAuthApiTest]'s MockEngine
 * harness pattern.
 */
class GroupTypeConfigApiTest {

    private fun apiWith(
        status: HttpStatusCode,
        body: String,
        onRequestUrl: ((String) -> Unit)? = null,
        onRequestMethod: ((HttpMethod) -> Unit)? = null,
    ): GroupTypeConfigApi {
        val engine = MockEngine { request ->
            onRequestUrl?.invoke(request.url.encodedPath)
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
                        isLenient = true
                    },
                )
            }
        }
        return GroupTypeConfigApiImpl(client)
    }

    // ---------- getGroupTypeConfigs (COMP-DT-003) ----------

    @Test
    fun getGroupTypeConfigs_success_returnsMappedDtoListAndUsesGetOnEntityIdPath() = runTest {
        var capturedPath = ""
        var capturedMethod: HttpMethod? = null
        val api = apiWith(
            HttpStatusCode.OK,
            """
            [
              {
                "typeSlug":"VSLA","displayName":"Village Savings & Loan Association","tagline":"Community-led savings",
                "savingsMechanism":"ACCUMULATING","contributionMode":"SHARE_BASED_VARIABLE",
                "lendingEnabled":true,"hasSocialFund":true,"hasBankLinkage":false,"welfareOnlyMode":false,
                "formallyRegistered":false,"defaultLoanMultiplier":3.0,"defaultInterestRatePct":5.0,
                "defaultCycleLengthMonths":12,"maxMembers":30,"minMembers":15
              }
            ]
            """.trimIndent(),
            onRequestUrl = { capturedPath = it },
            onRequestMethod = { capturedMethod = it },
        )

        val result = api.getGroupTypeConfigs()

        check(result is NetworkResult.Success)
        assertEquals(1, result.data.size)
        assertEquals(GroupTypeSlugDto.VSLA, result.data[0].typeSlug)
        // Default entityId 0 = the global seeded catalogue the companion serves.
        assertEquals("/companion/datatables/group_type_config/0", capturedPath)
        assertEquals(HttpMethod.Get, capturedMethod)
    }

    @Test
    fun getGroupTypeConfigs_explicitEntityId_threadsIntoPath() = runTest {
        var capturedPath = ""
        val api = apiWith(HttpStatusCode.OK, "[]", onRequestUrl = { capturedPath = it })

        val result = api.getGroupTypeConfigs(entityId = 42L)

        check(result is NetworkResult.Success)
        assertTrue(result.data.isEmpty())
        assertEquals("/companion/datatables/group_type_config/42", capturedPath)
    }

    @Test
    fun getGroupTypeConfigs_unauthorized401_mapsToUnauthorizedError() = runTest {
        val api = apiWith(HttpStatusCode.Unauthorized, """{"error":"expired"}""")

        val result = api.getGroupTypeConfigs()

        assertEquals(NetworkResult.Error(NetworkError.UNAUTHORIZED), result)
    }

    @Test
    fun getGroupTypeConfigs_notFound404_mapsToNotFoundError() = runTest {
        val api = apiWith(HttpStatusCode.NotFound, """{"error":"no seed catalogue"}""")

        val result = api.getGroupTypeConfigs()

        assertEquals(NetworkResult.Error(NetworkError.NOT_FOUND), result)
    }

    @Test
    fun getGroupTypeConfigs_serverError500_mapsToServerError() = runTest {
        val api = apiWith(HttpStatusCode.InternalServerError, """{"error":"boom"}""")

        val result = api.getGroupTypeConfigs()

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }

    @Test
    fun getGroupTypeConfigs_malformedJsonBody_mapsToSerializationError() = runTest {
        val api = apiWith(HttpStatusCode.OK, """not-json""")

        val result = api.getGroupTypeConfigs()

        assertEquals(NetworkResult.Error(NetworkError.SERIALIZATION), result)
    }
}
