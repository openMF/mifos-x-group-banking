/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.service.memberprofile

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
import org.mifos.groupbanking.core.network.model.MemberRoleDto
import org.mifos.groupbanking.core.network.model.UpdateMemberRoleRequestDto
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * TDD RED-first coverage for [MemberProfileApi] / [MemberProfileApiImpl] — the FOUR raw-Fineract
 * reads/write driving the member-profile screen (`get_client`, `get_client_accounts`,
 * `get_member_role`, `update_member_role` — `idea-layer/screens/member-profile/api.yaml#api`).
 * Every method returns [NetworkResult] — never a raw [Result] envelope, never a thrown exception
 * (Mandatory Rule 2 / API.md#services). Mirrors
 * [org.mifos.groupbanking.core.network.service.groupcreate.GroupCreateApiTest]'s MockEngine
 * harness pattern (raw Fineract passthrough alongside companion-bridge calls, same shared
 * client). This Service is SERVICE-ONLY — the composite Store5 read-store + write-invalidation
 * repository is a downstream `kmp-store-gen`/`kmp-client-gen` generation step, not covered here.
 */
class MemberProfileApiTest {

    private fun apiWith(
        status: HttpStatusCode,
        body: String,
        onRequestUrl: ((Url) -> Unit)? = null,
        onRequestMethod: ((HttpMethod) -> Unit)? = null,
    ): MemberProfileApi {
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
        return MemberProfileApiImpl(client)
    }

    private val clientBody = """
        {
          "id": 501,
          "displayName": "Amina Yusuf",
          "firstname": "Amina",
          "lastname": "Yusuf",
          "mobileNo": "+254700000000",
          "imagePresent": true,
          "status": { "id": 300, "value": "Active" },
          "activationDate": "2026-01-15",
          "officeId": 1
        }
    """.trimIndent()

    private val accountsBody = """
        {
          "savingsAccounts": [
            { "id": 1, "productName": "VSLA Savings", "accountNo": "SA001", "balance": 1500.0, "status": { "id": 300, "value": "Active" } }
          ],
          "loanAccounts": [
            { "id": 2, "productName": "VSLA Loan", "accountNo": "LA001", "status": { "id": 300, "value": "Active" }, "summary": { "principalDisbursed": 500.0, "principalOutstanding": 250.0, "totalOverdue": 0.0 } }
          ]
        }
    """.trimIndent()

    private val roleArrayBody = """
        [
          { "role": "TREASURER", "groupId": 42, "assignedDate": "2026-01-15" }
        ]
    """.trimIndent()

    private val updateRoleResponseBody = """{ "resourceId": 501 }"""

    // ---------- getClient ----------

    @Test
    fun getClient_success_returnsMappedDtoAndUsesGetOnClientPath() = runTest {
        var capturedUrl: Url? = null
        var capturedMethod: HttpMethod? = null
        val api = apiWith(
            HttpStatusCode.OK,
            clientBody,
            onRequestUrl = { capturedUrl = it },
            onRequestMethod = { capturedMethod = it },
        )

        val result = api.getClient("501")

        check(result is NetworkResult.Success)
        assertEquals(501L, result.data.id)
        assertEquals("Amina Yusuf", result.data.displayName)
        assertEquals("/clients/501", capturedUrl?.encodedPath)
        assertEquals(HttpMethod.Get, capturedMethod)
    }

    @Test
    fun getClient_unauthorized401_mapsToUnauthorizedError() = runTest {
        val api = apiWith(HttpStatusCode.Unauthorized, """{"error":"session expired"}""")

        val result = api.getClient("501")

        assertEquals(NetworkResult.Error(NetworkError.UNAUTHORIZED), result)
    }

    @Test
    fun getClient_notFound404_mapsToNotFoundError() = runTest {
        val api = apiWith(HttpStatusCode.NotFound, """{"error":"client not found"}""")

        val result = api.getClient("999")

        assertEquals(NetworkResult.Error(NetworkError.NOT_FOUND), result)
    }

    @Test
    fun getClient_serverError500_mapsToServerError() = runTest {
        val api = apiWith(HttpStatusCode.InternalServerError, """{"error":"boom"}""")

        val result = api.getClient("501")

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }

    // ---------- getClientAccounts ----------

    @Test
    fun getClientAccounts_success_returnsMappedDtoAndUsesGetOnAccountsPath() = runTest {
        var capturedUrl: Url? = null
        var capturedMethod: HttpMethod? = null
        val api = apiWith(
            HttpStatusCode.OK,
            accountsBody,
            onRequestUrl = { capturedUrl = it },
            onRequestMethod = { capturedMethod = it },
        )

        val result = api.getClientAccounts("501")

        check(result is NetworkResult.Success)
        assertEquals(1, result.data.savingsAccounts.size)
        assertEquals(1, result.data.loanAccounts.size)
        assertEquals("/clients/501/accounts", capturedUrl?.encodedPath)
        assertEquals(HttpMethod.Get, capturedMethod)
    }

    @Test
    fun getClientAccounts_notFound404_mapsToNotFoundError() = runTest {
        val api = apiWith(HttpStatusCode.NotFound, """{"error":"client not found"}""")

        val result = api.getClientAccounts("999")

        assertEquals(NetworkResult.Error(NetworkError.NOT_FOUND), result)
    }

    @Test
    fun getClientAccounts_serverError500_mapsToServerError() = runTest {
        val api = apiWith(HttpStatusCode.InternalServerError, """{"error":"boom"}""")

        val result = api.getClientAccounts("501")

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }

    // ---------- getMemberRole ----------

    @Test
    fun getMemberRole_success_returnsMappedArrayAndUsesGetOnDatatablePath() = runTest {
        var capturedUrl: Url? = null
        var capturedMethod: HttpMethod? = null
        val api = apiWith(
            HttpStatusCode.OK,
            roleArrayBody,
            onRequestUrl = { capturedUrl = it },
            onRequestMethod = { capturedMethod = it },
        )

        val result = api.getMemberRole("501")

        check(result is NetworkResult.Success)
        assertEquals(1, result.data.size)
        assertEquals(42L, result.data.first().groupId)
        assertEquals("/datatables/dt_member_role/501", capturedUrl?.encodedPath)
        assertEquals(HttpMethod.Get, capturedMethod)
    }

    @Test
    fun getMemberRole_notFound404_mapsToNotFoundError() = runTest {
        val api = apiWith(HttpStatusCode.NotFound, """{"error":"role record not found"}""")

        val result = api.getMemberRole("501")

        assertEquals(NetworkResult.Error(NetworkError.NOT_FOUND), result)
    }

    @Test
    fun getMemberRole_serverError500_mapsToServerError() = runTest {
        val api = apiWith(HttpStatusCode.InternalServerError, """{"error":"boom"}""")

        val result = api.getMemberRole("501")

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }

    // ---------- updateMemberRole ----------

    @Test
    fun updateMemberRole_success_putsBodyOnDatatablePathAndReturnsMappedResponse() = runTest {
        var capturedUrl: Url? = null
        var capturedMethod: HttpMethod? = null
        val api = apiWith(
            HttpStatusCode.OK,
            updateRoleResponseBody,
            onRequestUrl = { capturedUrl = it },
            onRequestMethod = { capturedMethod = it },
        )
        val request = UpdateMemberRoleRequestDto(
            role = MemberRoleDto.TREASURER,
            groupId = 42,
            assignedDate = "2026-01-15",
        )

        val result = api.updateMemberRole("501", request)

        check(result is NetworkResult.Success)
        assertEquals(501L, result.data.resourceId)
        assertEquals("/datatables/dt_member_role/501", capturedUrl?.encodedPath)
        assertEquals(HttpMethod.Put, capturedMethod)
    }

    @Test
    fun updateMemberRole_invalidRole400_mapsToBadRequestError() = runTest {
        val api = apiWith(HttpStatusCode.BadRequest, """{"error":"invalid role value"}""")
        val request = UpdateMemberRoleRequestDto(role = MemberRoleDto.MEMBER, groupId = 42, assignedDate = "2026-01-15")

        val result = api.updateMemberRole("501", request)

        assertEquals(NetworkResult.Error(NetworkError.BAD_REQUEST), result)
    }

    @Test
    fun updateMemberRole_forbidden403NotChairperson_mapsToUnknownError() = runTest {
        val api = apiWith(HttpStatusCode.Forbidden, """{"error":"caller is not chairperson"}""")
        val request = UpdateMemberRoleRequestDto(role = MemberRoleDto.SECRETARY, groupId = 42, assignedDate = "2026-01-15")

        val result = api.updateMemberRole("501", request)

        assertEquals(NetworkResult.Error(NetworkError.UNKNOWN), result)
    }

    @Test
    fun updateMemberRole_serverError500_mapsToServerError() = runTest {
        val api = apiWith(HttpStatusCode.InternalServerError, """{"error":"boom"}""")
        val request = UpdateMemberRoleRequestDto(role = MemberRoleDto.CHAIRPERSON, groupId = 42, assignedDate = "2026-01-15")

        val result = api.updateMemberRole("501", request)

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }
}
