/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.service.groupcreate

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
import org.mifos.groupbanking.core.network.model.ContributionModelDto
import org.mifos.groupbanking.core.network.model.CreateGroupRequestDto
import org.mifos.groupbanking.core.network.model.CreateGroupTypeConfigDto
import org.mifos.groupbanking.core.network.model.GroupTypeDto
import org.mifos.groupbanking.core.network.model.PayoutOrderMethodDto
import org.mifos.groupbanking.core.network.model.SavingsMechanismDto
import org.mifos.groupbanking.core.network.model.ShareoutFormulaDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * TDD RED-first coverage for [GroupCreateApi] / [GroupCreateApiImpl] (COMP-GRP-001 group
 * orchestration + raw Fineract `/offices` passthrough —
 * `idea-layer/screens/group-create/api.yaml`). Every method returns [NetworkResult] — never a
 * raw [Result] envelope, never a thrown exception (Mandatory Rule 2). Mirrors
 * [org.mifos.groupbanking.core.network.service.joinwithcode.InvitationApiTest]'s MockEngine
 * harness pattern.
 */
class GroupCreateApiTest {

    private fun apiWith(
        status: HttpStatusCode,
        body: String,
        onRequestUrl: ((Url) -> Unit)? = null,
        onRequestMethod: ((HttpMethod) -> Unit)? = null,
        onRequestBody: ((String) -> Unit)? = null,
    ): GroupCreateApi {
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
                json(
                    Json {
                        ignoreUnknownKeys = true
                        coerceInputValues = true
                    },
                )
            }
        }
        return GroupCreateApiImpl(client)
    }

    // ---------- getOffices (raw Fineract GET /offices passthrough) ----------

    private val officesBody = """
        [
          { "id": 1, "name": "Head Office", "nameDecorated": ".Head Office", "externalId": "HO" },
          { "id": 2, "name": "Kampala Branch", "nameDecorated": "..Kampala Branch" }
        ]
    """.trimIndent()

    @Test
    fun getOffices_success_returnsMappedListAndUsesGetOnOfficesPathWithDefaultOrderBy() = runTest {
        var capturedUrl: Url? = null
        var capturedMethod: HttpMethod? = null
        val api = apiWith(
            HttpStatusCode.OK,
            officesBody,
            onRequestUrl = { capturedUrl = it },
            onRequestMethod = { capturedMethod = it },
        )

        val result = api.getOffices()

        check(result is NetworkResult.Success)
        assertEquals(2, result.data.size)
        assertEquals("Head Office", result.data[0].name)
        assertEquals("HO", result.data[0].externalId)
        assertEquals("/offices", capturedUrl?.encodedPath)
        assertEquals(HttpMethod.Get, capturedMethod)
        assertEquals("name", capturedUrl?.parameters?.get("orderBy"))
    }

    @Test
    fun getOffices_explicitOrderBy_threadsQueryParam() = runTest {
        var capturedUrl: Url? = null
        val api = apiWith(HttpStatusCode.OK, officesBody, onRequestUrl = { capturedUrl = it })

        api.getOffices(orderBy = "id")

        assertEquals("id", capturedUrl?.parameters?.get("orderBy"))
    }

    @Test
    fun getOffices_missingExternalId_mapsToNullExternalId() = runTest {
        val api = apiWith(HttpStatusCode.OK, officesBody)

        val result = api.getOffices()

        check(result is NetworkResult.Success)
        assertNull(result.data[1].externalId)
    }

    @Test
    fun getOffices_unauthorized401_mapsToUnauthorizedError() = runTest {
        val api = apiWith(HttpStatusCode.Unauthorized, """{"error":"session expired"}""")

        val result = api.getOffices()

        assertEquals(NetworkResult.Error(NetworkError.UNAUTHORIZED), result)
    }

    @Test
    fun getOffices_serverError500_mapsToServerError() = runTest {
        val api = apiWith(HttpStatusCode.InternalServerError, """{"error":"boom"}""")

        val result = api.getOffices()

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }

    // ---------- createGroup (COMP-GRP-001 POST /companion/groups) ----------

    private val typeConfigDto = CreateGroupTypeConfigDto(
        groupType = GroupTypeDto.VSLA,
        poolModel = SavingsMechanismDto.ACCUMULATING,
        contributionModel = ContributionModelDto.SHARE_BASED_VARIABLE,
        shareoutFormula = ShareoutFormulaDto.PRORATA_SHARES,
        payoutOrderMethod = PayoutOrderMethodDto.NA,
        shareValue = 5.0,
        contributionAmount = 0.0,
        socialFundEnabled = true,
        socialFundPercent = 10.0,
        cycleLengthMonths = 12,
        loanMultiplier = 3.0,
        interestRate = 2.0,
        fineAmount = 1.0,
        maxMembers = 30,
    )

    private val requestDto = CreateGroupRequestDto(
        name = "Sunrise VSLA",
        officeId = 1,
        userId = 42,
        currency = "UGX",
        meetingDay = "MONDAY",
        meetingTime = "10:00",
        typeConfig = typeConfigDto,
    )

    private val createResponseBody = """
        { "groupId": "grp-100", "fineractGroupId": 55, "inviteCode": "ABC123" }
    """.trimIndent()

    @Test
    fun createGroup_success_postsBodyOnCompanionGroupsPathAndReturnsMappedResponse() = runTest {
        var capturedUrl: Url? = null
        var capturedMethod: HttpMethod? = null
        var capturedBody: String? = null
        val api = apiWith(
            HttpStatusCode.OK,
            createResponseBody,
            onRequestUrl = { capturedUrl = it },
            onRequestMethod = { capturedMethod = it },
            onRequestBody = { capturedBody = it },
        )

        val result = api.createGroup(requestDto)

        check(result is NetworkResult.Success)
        assertEquals("grp-100", result.data.groupId)
        assertEquals(55L, result.data.fineractGroupId)
        assertEquals("ABC123", result.data.inviteCode)
        assertEquals("/companion/groups", capturedUrl?.encodedPath)
        assertEquals(HttpMethod.Post, capturedMethod)
        assertTrue(capturedBody!!.contains("Sunrise VSLA"))
    }

    @Test
    fun createGroup_validation400_mapsToBadRequestError() = runTest {
        val api = apiWith(HttpStatusCode.BadRequest, """{"error":"invalid typeConfig"}""")

        val result = api.createGroup(requestDto)

        assertEquals(NetworkResult.Error(NetworkError.BAD_REQUEST), result)
    }

    @Test
    fun createGroup_unauthorized401_mapsToUnauthorizedError() = runTest {
        val api = apiWith(HttpStatusCode.Unauthorized, """{"error":"session expired"}""")

        val result = api.createGroup(requestDto)

        assertEquals(NetworkResult.Error(NetworkError.UNAUTHORIZED), result)
    }

    @Test
    fun createGroup_forbidden403_mapsToUnknownError() = runTest {
        // 403 has no dedicated NetworkError bucket — mirrors ResultSuspendConverterFactory's
        // else-branch fallback (same convention as GroupApiImpl / InvitationApiImpl).
        val api = apiWith(HttpStatusCode.Forbidden, """{"error":"forbidden"}""")

        val result = api.createGroup(requestDto)

        assertEquals(NetworkResult.Error(NetworkError.UNKNOWN), result)
    }

    @Test
    fun createGroup_conflict409GroupNameTaken_mapsToUnknownError() = runTest {
        // 409 has no dedicated NetworkError bucket either — same else-branch fallback.
        val api = apiWith(HttpStatusCode.Conflict, """{"error":"group name already exists"}""")

        val result = api.createGroup(requestDto)

        assertEquals(NetworkResult.Error(NetworkError.UNKNOWN), result)
    }

    @Test
    fun createGroup_serverError500_mapsToServerError() = runTest {
        val api = apiWith(HttpStatusCode.InternalServerError, """{"error":"boom"}""")

        val result = api.createGroup(requestDto)

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }
}
