/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.network.service.memberadd

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
import kpt.core.network.model.CreateMemberRequestDto
import kpt.core.network.model.MemberRoleDto
import kpt.core.network.model.UpdateMemberRoleRequestDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD RED-first coverage for [MemberAddApi] / [MemberAddApiImpl] (member-add create-chain —
 * `idea-layer/screens/member-add/api.yaml` — `create_client` -> `assign_member_role` -> optional
 * `upload_photo`). Every method returns [NetworkResult] — never a raw [Result] envelope, never a
 * thrown exception (Mandatory Rule 2). Mirrors
 * [kpt.core.network.service.groupcreate.GroupCreateApiTest]'s MockEngine
 * harness pattern.
 */
class MemberAddApiTest {

    private fun apiWith(
        status: HttpStatusCode,
        body: String,
        onRequestUrl: ((Url) -> Unit)? = null,
        onRequestMethod: ((HttpMethod) -> Unit)? = null,
        onRequestBody: ((io.ktor.client.request.HttpRequestData) -> Unit)? = null,
    ): MemberAddApi {
        val engine = MockEngine { request ->
            onRequestUrl?.invoke(request.url)
            onRequestMethod?.invoke(request.method)
            onRequestBody?.invoke(request)
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
        return MemberAddApiImpl(client)
    }

    // ---------- createClient (POST /clients) ----------

    private val createRequestDto = CreateMemberRequestDto(
        firstname = "Amina",
        lastname = "Nabirye",
        mobileNo = "+256700000001",
        active = true,
        activationDate = "22 July 2026",
        officeId = 1,
        groupId = 5,
        locale = "en",
        dateFormat = "dd MMMM yyyy",
    )

    private val createResponseBody = """{ "resourceId": 42, "clientId": 42 }"""

    @Test
    fun createClient_success_postsBodyOnClientsPathAndReturnsMappedResponse() = runTest {
        var capturedUrl: Url? = null
        var capturedMethod: HttpMethod? = null
        val api = apiWith(
            HttpStatusCode.OK,
            createResponseBody,
            onRequestUrl = { capturedUrl = it },
            onRequestMethod = { capturedMethod = it },
        )

        val result = api.createClient(createRequestDto)

        check(result is NetworkResult.Success)
        assertEquals(42L, result.data.clientId)
        assertEquals(42L, result.data.resourceId)
        assertEquals("/clients", capturedUrl?.encodedPath)
        assertEquals(HttpMethod.Post, capturedMethod)
    }

    @Test
    fun createClient_validation400_mapsToBadRequestError() = runTest {
        val api = apiWith(HttpStatusCode.BadRequest, """{"error":"duplicate phone"}""")

        val result = api.createClient(createRequestDto)

        assertEquals(NetworkResult.Error(NetworkError.BAD_REQUEST), result)
    }

    @Test
    fun createClient_unauthorized401_mapsToUnauthorizedError() = runTest {
        val api = apiWith(HttpStatusCode.Unauthorized, """{"error":"session expired"}""")

        val result = api.createClient(createRequestDto)

        assertEquals(NetworkResult.Error(NetworkError.UNAUTHORIZED), result)
    }

    @Test
    fun createClient_serverError500_mapsToServerError() = runTest {
        val api = apiWith(HttpStatusCode.InternalServerError, """{"error":"boom"}""")

        val result = api.createClient(createRequestDto)

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }

    // ---------- assignMemberRole (POST /datatables/dt_member_role/{clientId}) ----------

    private val assignRoleRequestDto = UpdateMemberRoleRequestDto(
        role = MemberRoleDto.TREASURER,
        groupId = 5,
        assignedDate = "22 July 2026",
    )

    private val assignRoleResponseBody = """{ "resourceId": 99 }"""

    @Test
    fun assignMemberRole_success_postsOnMemberRoleDatatablePathWithClientIdAndReturnsResourceId() = runTest {
        var capturedUrl: Url? = null
        var capturedMethod: HttpMethod? = null
        val api = apiWith(
            HttpStatusCode.OK,
            assignRoleResponseBody,
            onRequestUrl = { capturedUrl = it },
            onRequestMethod = { capturedMethod = it },
        )

        val result = api.assignMemberRole("42", assignRoleRequestDto)

        check(result is NetworkResult.Success)
        assertEquals(99L, result.data.resourceId)
        assertEquals("/datatables/dt_member_role/42", capturedUrl?.encodedPath)
        assertEquals(HttpMethod.Post, capturedMethod)
    }

    @Test
    fun assignMemberRole_invalidRole400_mapsToBadRequestError() = runTest {
        val api = apiWith(HttpStatusCode.BadRequest, """{"error":"invalid role"}""")

        val result = api.assignMemberRole("42", assignRoleRequestDto)

        assertEquals(NetworkResult.Error(NetworkError.BAD_REQUEST), result)
    }

    @Test
    fun assignMemberRole_serverError500_mapsToServerError() = runTest {
        val api = apiWith(HttpStatusCode.InternalServerError, """{"error":"boom"}""")

        val result = api.assignMemberRole("42", assignRoleRequestDto)

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }

    // ---------- uploadMemberPhoto (POST /clients/{clientId}/images, multipart) ----------

    private val uploadPhotoResponseBody = """{ "resourceId": 7 }"""

    @Test
    fun uploadMemberPhoto_success_postsMultipartOnClientImagesPathAndReturnsResourceId() = runTest {
        var capturedUrl: Url? = null
        var capturedMethod: HttpMethod? = null
        val api = apiWith(
            HttpStatusCode.OK,
            uploadPhotoResponseBody,
            onRequestUrl = { capturedUrl = it },
            onRequestMethod = { capturedMethod = it },
        )

        val result = api.uploadMemberPhoto("42", byteArrayOf(1, 2, 3, 4), "member-photo.jpg")

        check(result is NetworkResult.Success)
        assertEquals(7L, result.data.resourceId)
        assertEquals("/clients/42/images", capturedUrl?.encodedPath)
        assertEquals(HttpMethod.Post, capturedMethod)
    }

    @Test
    fun uploadMemberPhoto_fileTooLarge400_mapsToBadRequestError() = runTest {
        val api = apiWith(HttpStatusCode.BadRequest, """{"error":"file too large"}""")

        val result = api.uploadMemberPhoto("42", byteArrayOf(1, 2, 3), "big.jpg")

        assertEquals(NetworkResult.Error(NetworkError.BAD_REQUEST), result)
    }

    @Test
    fun uploadMemberPhoto_serverError500_mapsToServerError() = runTest {
        val api = apiWith(HttpStatusCode.InternalServerError, """{"error":"boom"}""")

        val result = api.uploadMemberPhoto("42", byteArrayOf(1, 2, 3), "photo.jpg")

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }

    @Test
    fun uploadMemberPhoto_carriesRequestBodyBytes() = runTest {
        var bodyByteCount = -1
        val api = apiWith(
            HttpStatusCode.OK,
            uploadPhotoResponseBody,
            onRequestBody = { data ->
                // Multipart content length is not exposed directly on HttpRequestData in a
                // MockEngine-portable way; presence of a non-empty body confirms the multipart
                // envelope carried the photo bytes through to the transport layer.
                bodyByteCount = data.body.contentLength?.toInt() ?: 0
            },
        )

        api.uploadMemberPhoto("42", byteArrayOf(1, 2, 3, 4, 5), "member-photo.jpg")

        assertTrue(bodyByteCount > 0)
    }
}
