/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.network.service.batchsync

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
import kpt.core.network.model.BatchOperationDto
import kpt.core.network.model.BatchSyncRequestDto
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * TDD RED-first coverage for [BatchSyncApi] / [BatchSyncApiImpl] — the sync-status feature's
 * single endpoint (`idea-layer/screens/sync-status/api.yaml#api[batch_sync]`). Returns
 * [NetworkResult] — never a raw [Result] envelope, never a thrown exception (Mandatory Rule 2 /
 * API.md#services). Mirrors
 * [kpt.core.network.service.loanrequest.LoanRequestApiTest]'s MockEngine
 * harness pattern, including its `ContentConvertException` malformed-body coverage — extended
 * here with the top-level-JSON-ARRAY response shape (`api.yaml#dtos.BatchSyncResponse`).
 */
class BatchSyncApiTest {

    private fun apiWith(
        status: HttpStatusCode,
        body: String,
        onRequestUrl: ((Url) -> Unit)? = null,
        onRequestMethod: ((HttpMethod) -> Unit)? = null,
    ): BatchSyncApi {
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
        return BatchSyncApiImpl(client)
    }

    private val requestDto = BatchSyncRequestDto(
        requests = listOf(
            BatchOperationDto(
                requestId = 1,
                relativeUrl = "datatables/dt_loan_request",
                method = "POST",
                body = """{"clientId":5001}""",
            ),
            BatchOperationDto(
                requestId = 2,
                relativeUrl = "datatables/dt_member_role",
                method = "POST",
                body = """{"groupId":9001}""",
            ),
        ),
    )

    // ---------- batchSync (batch_sync) ----------

    @Test
    fun batchSync_success_postsBodyOnBatchesPathAndReturnsMappedTopLevelArray() = runTest {
        var capturedUrl: Url? = null
        var capturedMethod: HttpMethod? = null
        val responseBody = """
            [
              {"requestId":1,"statusCode":200,"body":"{\"resourceId\":9101}"},
              {"requestId":2,"statusCode":201,"body":"{\"resourceId\":9102}"}
            ]
        """.trimIndent()
        val api = apiWith(
            HttpStatusCode.OK,
            responseBody,
            onRequestUrl = { capturedUrl = it },
            onRequestMethod = { capturedMethod = it },
        )

        val result = api.batchSync(requestDto)

        check(result is NetworkResult.Success)
        assertEquals(2, result.data.size)
        assertEquals(1, result.data[0].requestId)
        assertEquals(200, result.data[0].statusCode)
        assertEquals(2, result.data[1].requestId)
        assertEquals(201, result.data[1].statusCode)
        assertEquals("/fineract-provider/api/v1/batches", capturedUrl?.encodedPath)
        assertEquals(HttpMethod.Post, capturedMethod)
    }

    @Test
    fun batchSync_mixedStatusRows_decodesEveryRowIncludingConflictAndFailure() = runTest {
        val responseBody = """
            [
              {"requestId":1,"statusCode":200,"body":"{}"},
              {"requestId":2,"statusCode":409,"body":"{\"errors\":[{\"developerMessage\":\"duplicate\"}]}"},
              {"requestId":3,"statusCode":500,"body":"{\"error\":\"boom\"}"}
            ]
        """.trimIndent()
        val api = apiWith(HttpStatusCode.OK, responseBody)

        val result = api.batchSync(requestDto)

        check(result is NetworkResult.Success)
        assertEquals(3, result.data.size)
        assertEquals(409, result.data[1].statusCode)
        assertEquals(500, result.data[2].statusCode)
    }

    @Test
    fun batchSync_emptyArrayResponse_decodesToEmptyList() = runTest {
        val api = apiWith(HttpStatusCode.OK, "[]")

        val result = api.batchSync(BatchSyncRequestDto(requests = emptyList()))

        check(result is NetworkResult.Success)
        assertEquals(emptyList(), result.data)
    }

    @Test
    fun batchSync_validationError400_mapsToBadRequestError() = runTest {
        val api = apiWith(HttpStatusCode.BadRequest, """{"error":"batch validation error"}""")

        val result = api.batchSync(requestDto)

        assertEquals(NetworkResult.Error(NetworkError.BAD_REQUEST), result)
    }

    @Test
    fun batchSync_sessionExpired401_mapsToUnauthorizedError() = runTest {
        val api = apiWith(HttpStatusCode.Unauthorized, """{"error":"session expired"}""")

        val result = api.batchSync(requestDto)

        assertEquals(NetworkResult.Error(NetworkError.UNAUTHORIZED), result)
    }

    @Test
    fun batchSync_serverError500_mapsToServerError() = runTest {
        val api = apiWith(HttpStatusCode.InternalServerError, """{"error":"boom"}""")

        val result = api.batchSync(requestDto)

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }

    @Test
    fun batchSync_malformedJsonBody_mapsToSerializationError() = runTest {
        // Covers the ContentConvertException gap LoanApplyApiTest/LoanRequestApiTest first
        // flagged (a malformed 2xx body must map to NetworkError.SERIALIZATION, never propagate
        // as an uncaught exception) — including a non-array 2xx body for THIS array-shaped DTO.
        val api = apiWith(HttpStatusCode.OK, "not-json")

        val result = api.batchSync(requestDto)

        assertEquals(NetworkResult.Error(NetworkError.SERIALIZATION), result)
    }

    @Test
    fun batchSync_nonArrayJsonObjectBody_mapsToSerializationError() = runTest {
        // The /batches contract is a bare array — a wrapped-object 2xx body is malformed for
        // this endpoint and must map to SERIALIZATION, never throw past the ApiImpl boundary.
        val api = apiWith(HttpStatusCode.OK, """{"unexpected":"wrapper"}""")

        val result = api.batchSync(requestDto)

        assertEquals(NetworkResult.Error(NetworkError.SERIALIZATION), result)
    }
}
