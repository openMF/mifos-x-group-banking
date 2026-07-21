/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.service.joinwithcode

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
import org.mifos.groupbanking.core.network.model.AssociateClientsRequestDto
import org.mifos.groupbanking.core.network.model.GroupRoleDto
import org.mifos.groupbanking.core.network.model.MarkAcceptedRequestDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * TDD RED-first coverage for [InvitationApi] / [InvitationApiImpl] (COMP-DT-004 + COMP-GRP-003
 * — `idea-layer/screens/join-with-code/api.yaml`). Every method returns [NetworkResult] — never
 * a raw [Result] envelope, never a thrown exception (Mandatory Rule 2). Mirrors
 * [org.mifos.groupbanking.core.network.service.grouplist.GroupApiTest]'s MockEngine harness
 * pattern.
 */
class InvitationApiTest {

    private fun apiWith(
        status: HttpStatusCode,
        body: String,
        onRequestUrl: ((Url) -> Unit)? = null,
        onRequestMethod: ((HttpMethod) -> Unit)? = null,
        onRequestBody: ((String) -> Unit)? = null,
    ): InvitationApi {
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
        return InvitationApiImpl(client)
    }

    // ---------- validateInviteToken (COMP-DT-004 GET) ----------

    private val unusedRowBody = """
        {
          "token": "ABC123", "group_id": 5, "inviter_client_id": 10,
          "invited_email_phone": "amina@example.com", "role_to_assign": "MEMBER",
          "expires_at": "2026-08-01T00:00:00Z", "accepted_at": null
        }
    """.trimIndent()

    @Test
    fun validateInviteToken_success_returnsMappedRowAndUsesGetOnInvitationsPathWithCode() = runTest {
        var capturedUrl: Url? = null
        var capturedMethod: HttpMethod? = null
        val api = apiWith(
            HttpStatusCode.OK,
            unusedRowBody,
            onRequestUrl = { capturedUrl = it },
            onRequestMethod = { capturedMethod = it },
        )

        val result = api.validateInviteToken("ABC123")

        check(result is NetworkResult.Success)
        assertEquals("ABC123", result.data.token)
        assertEquals(5L, result.data.groupId)
        assertNull(result.data.acceptedAt)
        assertEquals("/companion/datatables/invitations/ABC123", capturedUrl?.encodedPath)
        assertEquals(HttpMethod.Get, capturedMethod)
    }

    @Test
    fun validateInviteToken_alreadyUsedRow_returnsSuccessWithNonNullAcceptedAt() = runTest {
        val body = """
            {
              "token": "USED01", "group_id": 5, "inviter_client_id": 10,
              "invited_email_phone": "amina@example.com", "role_to_assign": "MEMBER",
              "expires_at": "2026-08-01T00:00:00Z", "accepted_at": "2026-07-01T00:00:00Z"
            }
        """.trimIndent()
        val api = apiWith(HttpStatusCode.OK, body)

        val result = api.validateInviteToken("USED01")

        check(result is NetworkResult.Success)
        assertEquals("2026-07-01T00:00:00Z", result.data.acceptedAt)
    }

    @Test
    fun validateInviteToken_notFound404_mapsToNotFoundError() = runTest {
        val api = apiWith(HttpStatusCode.NotFound, """{"error":"no such code"}""")

        val result = api.validateInviteToken("BOGUS0")

        assertEquals(NetworkResult.Error(NetworkError.NOT_FOUND), result)
    }

    @Test
    fun validateInviteToken_serverError500_mapsToServerError() = runTest {
        val api = apiWith(HttpStatusCode.InternalServerError, """{"error":"boom"}""")

        val result = api.validateInviteToken("ABC123")

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }

    // ---------- getGroupPreview (companion bridge GET) ----------

    private val previewBody = """
        {
          "groupId": 5, "groupName": "Sunrise VSLA", "groupType": "VSLA",
          "organizerName": "Amina", "memberCount": 18, "officeId": 100,
          "roleToAssign": "MEMBER"
        }
    """.trimIndent()

    @Test
    fun getGroupPreview_success_returnsMappedPreviewAndUsesGetOnGroupsPathWithGroupId() = runTest {
        var capturedUrl: Url? = null
        var capturedMethod: HttpMethod? = null
        val api = apiWith(
            HttpStatusCode.OK,
            previewBody,
            onRequestUrl = { capturedUrl = it },
            onRequestMethod = { capturedMethod = it },
        )

        val result = api.getGroupPreview(5)

        check(result is NetworkResult.Success)
        assertEquals("Sunrise VSLA", result.data.groupName)
        assertEquals(18, result.data.memberCount)
        assertEquals("/companion/groups/5", capturedUrl?.encodedPath)
        assertEquals(HttpMethod.Get, capturedMethod)
    }

    @Test
    fun getGroupPreview_notFound404_mapsToNotFoundError() = runTest {
        val api = apiWith(HttpStatusCode.NotFound, """{"error":"no such group"}""")

        val result = api.getGroupPreview(999)

        assertEquals(NetworkResult.Error(NetworkError.NOT_FOUND), result)
    }

    @Test
    fun getGroupPreview_unauthorized401_mapsToUnauthorizedError() = runTest {
        val api = apiWith(HttpStatusCode.Unauthorized, """{"error":"session expired"}""")

        val result = api.getGroupPreview(5)

        assertEquals(NetworkResult.Error(NetworkError.UNAUTHORIZED), result)
    }

    // ---------- associateClientToGroup (COMP-GRP-003 POST) ----------

    private val associateResponseBody = """
        { "resourceId": 200, "groupId": 5, "clientIds": [42] }
    """.trimIndent()

    @Test
    fun associateClientToGroup_success_postsBodyOnAssociateClientsPathAndReturnsMappedResponse() = runTest {
        var capturedUrl: Url? = null
        var capturedMethod: HttpMethod? = null
        var capturedBody: String? = null
        val api = apiWith(
            HttpStatusCode.OK,
            associateResponseBody,
            onRequestUrl = { capturedUrl = it },
            onRequestMethod = { capturedMethod = it },
            onRequestBody = { capturedBody = it },
        )

        val result = api.associateClientToGroup(
            groupId = 5,
            request = AssociateClientsRequestDto(clientIds = listOf(42), roleToAssign = GroupRoleDto.MEMBER),
        )

        check(result is NetworkResult.Success)
        assertEquals(200L, result.data.resourceId)
        assertEquals(listOf(42L), result.data.clientIds)
        assertEquals("/companion/groups/5/associate-clients", capturedUrl?.encodedPath)
        assertEquals(HttpMethod.Post, capturedMethod)
        assertTrue(capturedBody!!.contains("42"))
    }

    @Test
    fun associateClientToGroup_alreadyMember400_mapsToBadRequestError() = runTest {
        val api = apiWith(HttpStatusCode.BadRequest, """{"error":"already a member"}""")

        val result = api.associateClientToGroup(5, AssociateClientsRequestDto(listOf(42), GroupRoleDto.MEMBER))

        assertEquals(NetworkResult.Error(NetworkError.BAD_REQUEST), result)
    }

    @Test
    fun associateClientToGroup_forbidden403GroupClosed_mapsToUnknownError() = runTest {
        // 403 has no dedicated NetworkError bucket — mirrors ResultSuspendConverterFactory's
        // else-branch fallback (same convention as GroupApiImpl / CompanionAuthApiImpl).
        val api = apiWith(HttpStatusCode.Forbidden, """{"error":"group closed"}""")

        val result = api.associateClientToGroup(5, AssociateClientsRequestDto(listOf(42), GroupRoleDto.MEMBER))

        assertEquals(NetworkResult.Error(NetworkError.UNKNOWN), result)
    }

    // ---------- markInvitationAccepted (COMP-DT-004 PUT) ----------

    private val markAcceptedResponseBody = """
        { "resourceId": 300, "changes": { "accepted_at": "2026-07-22T10:00:00Z" } }
    """.trimIndent()

    @Test
    fun markInvitationAccepted_success_putsBodyOnCodeAndRowIdPathAndReturnsMappedResponse() = runTest {
        var capturedUrl: Url? = null
        var capturedMethod: HttpMethod? = null
        var capturedBody: String? = null
        val api = apiWith(
            HttpStatusCode.OK,
            markAcceptedResponseBody,
            onRequestUrl = { capturedUrl = it },
            onRequestMethod = { capturedMethod = it },
            onRequestBody = { capturedBody = it },
        )

        val result = api.markInvitationAccepted(
            code = "ABC123",
            rowId = 77,
            request = MarkAcceptedRequestDto(acceptedAt = "2026-07-22T10:00:00Z"),
        )

        check(result is NetworkResult.Success)
        assertEquals(300L, result.data.resourceId)
        assertEquals("2026-07-22T10:00:00Z", result.data.changes.acceptedAt)
        assertEquals("/companion/datatables/invitations/ABC123/77", capturedUrl?.encodedPath)
        assertEquals(HttpMethod.Put, capturedMethod)
        assertTrue(capturedBody!!.contains("2026-07-22T10:00:00Z"))
    }

    @Test
    fun markInvitationAccepted_notFound404_mapsToNotFoundError() = runTest {
        val api = apiWith(HttpStatusCode.NotFound, """{"error":"row not found"}""")

        val result = api.markInvitationAccepted("ABC123", 77, MarkAcceptedRequestDto("2026-07-22T10:00:00Z"))

        assertEquals(NetworkResult.Error(NetworkError.NOT_FOUND), result)
    }

    @Test
    fun markInvitationAccepted_serverError500_mapsToServerError() = runTest {
        val api = apiWith(HttpStatusCode.InternalServerError, """{"error":"boom"}""")

        val result = api.markInvitationAccepted("ABC123", 77, MarkAcceptedRequestDto("2026-07-22T10:00:00Z"))

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }
}
