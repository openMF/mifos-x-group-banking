/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.network.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * TDD RED-first coverage for the group-create wizard wire DTOs (COMP-GRP-001 —
 * `POST /companion/groups`, plus `GET /offices`). See API.md#dtos.
 */
class GroupCreateDtoTest {

    // Server-parity Json config: mirrors NetworkModule's client config (ignoreUnknownKeys +
    // coerceInputValues) so these tests prove EC30 cross-version tolerance, not just happy-path
    // round-tripping.
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val vslaTypeConfigDto = CreateGroupTypeConfigDto(
        groupType = GroupTypeDto.VSLA,
        poolModel = SavingsMechanismDto.ACCUMULATING,
        contributionModel = ContributionModelDto.SHARE_BASED_VARIABLE,
        shareoutFormula = ShareoutFormulaDto.PRORATA_SHARES,
        payoutOrderMethod = PayoutOrderMethodDto.NA,
        shareValue = 5.0,
        contributionAmount = 0.0,
        socialFundEnabled = true,
        socialFundPercent = 5.0,
        cycleLengthMonths = 12,
        loanMultiplier = 3.0,
        interestRate = 10.0,
        fineAmount = 1.0,
        maxMembers = 30,
    )

    private val requestDto = CreateGroupRequestDto(
        name = "Sunrise VSLA",
        officeId = 1L,
        userId = 42L,
        currency = "KES",
        meetingDay = "MONDAY",
        meetingTime = "14:00",
        typeConfig = vslaTypeConfigDto,
    )

    private val responseDto = CreateGroupResponseDto(
        groupId = "grp-9001",
        fineractGroupId = 501L,
        inviteCode = "AB12CD",
    )

    private val officeDto = OfficeDto(
        id = 1L,
        name = "Head Office",
        nameDecorated = ".Head Office",
        externalId = "HO-001",
    )

    // ---------- CreateGroupRequestDto ----------

    @Test
    fun createGroupRequestDto_constructsWithAllFields() {
        assertEquals("Sunrise VSLA", requestDto.name)
        assertEquals(1L, requestDto.officeId)
        assertEquals(42L, requestDto.userId)
        assertEquals("KES", requestDto.currency)
        assertEquals("MONDAY", requestDto.meetingDay)
        assertEquals("14:00", requestDto.meetingTime)
        assertEquals(vslaTypeConfigDto, requestDto.typeConfig)
    }

    @Test
    fun createGroupRequestDto_serializationRoundTrips_topLevelFieldsAreCamelCase() {
        val encoded = json.encodeToString(CreateGroupRequestDto.serializer(), requestDto)
        assertTrue(encoded.contains("\"name\""))
        assertTrue(encoded.contains("\"officeId\""))
        assertTrue(encoded.contains("\"userId\""))
        assertTrue(encoded.contains("\"currency\""))
        assertTrue(encoded.contains("\"meetingDay\""))
        assertTrue(encoded.contains("\"meetingTime\""))
        assertTrue(encoded.contains("\"typeConfig\""))

        val decoded = json.decodeFromString(CreateGroupRequestDto.serializer(), encoded)
        assertEquals(requestDto, decoded)
    }

    @Test
    fun createGroupRequestDto_equality() {
        assertEquals(requestDto.copy(), requestDto.copy())
    }

    @Test
    fun createGroupRequestDto_carriesSchemaVersion() {
        assertEquals(1, CreateGroupRequestDto.SCHEMA_VERSION)
    }

    // ---------- CreateGroupTypeConfigDto (group_type_config datatable payload, snake_case) ----------

    @Test
    fun createGroupTypeConfigDto_constructsWithAllFields() {
        assertEquals(GroupTypeDto.VSLA, vslaTypeConfigDto.groupType)
        assertEquals(SavingsMechanismDto.ACCUMULATING, vslaTypeConfigDto.poolModel)
        assertEquals(ContributionModelDto.SHARE_BASED_VARIABLE, vslaTypeConfigDto.contributionModel)
        assertEquals(ShareoutFormulaDto.PRORATA_SHARES, vslaTypeConfigDto.shareoutFormula)
        assertEquals(PayoutOrderMethodDto.NA, vslaTypeConfigDto.payoutOrderMethod)
        assertEquals(5.0, vslaTypeConfigDto.shareValue)
        assertEquals(0.0, vslaTypeConfigDto.contributionAmount)
        assertTrue(vslaTypeConfigDto.socialFundEnabled)
        assertEquals(5.0, vslaTypeConfigDto.socialFundPercent)
        assertEquals(12, vslaTypeConfigDto.cycleLengthMonths)
        assertEquals(3.0, vslaTypeConfigDto.loanMultiplier)
        assertEquals(10.0, vslaTypeConfigDto.interestRate)
        assertEquals(1.0, vslaTypeConfigDto.fineAmount)
        assertEquals(30, vslaTypeConfigDto.maxMembers)
    }

    @Test
    fun createGroupTypeConfigDto_serializationRoundTrips_fieldNamesAreSnakeCase_matchingDatatableColumns() {
        // group_type_config is provisioned as a raw Fineract datatable row (COMP-GRP-001 step 5)
        // — @SerialName values MUST match the datatable column names exactly (Hard Rule 5).
        val encoded = json.encodeToString(CreateGroupTypeConfigDto.serializer(), vslaTypeConfigDto)
        assertTrue(encoded.contains("\"group_type\""))
        assertTrue(encoded.contains("\"pool_model\""))
        assertTrue(encoded.contains("\"contribution_model\""))
        assertTrue(encoded.contains("\"shareout_formula\""))
        assertTrue(encoded.contains("\"payout_order_method\""))
        assertTrue(encoded.contains("\"share_value\""))
        assertTrue(encoded.contains("\"contribution_amount\""))
        assertTrue(encoded.contains("\"social_fund_enabled\""))
        assertTrue(encoded.contains("\"social_fund_percent\""))
        assertTrue(encoded.contains("\"cycle_length_months\""))
        assertTrue(encoded.contains("\"loan_multiplier\""))
        assertTrue(encoded.contains("\"interest_rate\""))
        assertTrue(encoded.contains("\"fine_amount\""))
        assertTrue(encoded.contains("\"max_members\""))

        val decoded = json.decodeFromString(CreateGroupTypeConfigDto.serializer(), encoded)
        assertEquals(vslaTypeConfigDto, decoded)
    }

    @Test
    fun createGroupTypeConfigDto_equality() {
        assertEquals(vslaTypeConfigDto.copy(), vslaTypeConfigDto.copy())
    }

    @Test
    fun createGroupTypeConfigDto_carriesSchemaVersion() {
        assertEquals(1, CreateGroupTypeConfigDto.SCHEMA_VERSION)
    }

    // ---------- CreateGroupResponseDto ----------

    @Test
    fun createGroupResponseDto_constructsWithAllFields() {
        assertEquals("grp-9001", responseDto.groupId)
        assertEquals(501L, responseDto.fineractGroupId)
        assertEquals("AB12CD", responseDto.inviteCode)
    }

    @Test
    fun createGroupResponseDto_serializationRoundTrips() {
        val encoded = json.encodeToString(CreateGroupResponseDto.serializer(), responseDto)
        assertTrue(encoded.contains("\"groupId\""))
        assertTrue(encoded.contains("\"fineractGroupId\""))
        assertTrue(encoded.contains("\"inviteCode\""))

        val decoded = json.decodeFromString(CreateGroupResponseDto.serializer(), encoded)
        assertEquals(responseDto, decoded)
    }

    @Test
    fun createGroupResponseDto_equality() {
        assertEquals(responseDto.copy(), responseDto.copy())
    }

    @Test
    fun createGroupResponseDto_carriesSchemaVersion() {
        assertEquals(1, CreateGroupResponseDto.SCHEMA_VERSION)
    }

    // ---------- OfficeDto ----------

    @Test
    fun officeDto_constructsWithAllFields() {
        assertEquals(1L, officeDto.id)
        assertEquals("Head Office", officeDto.name)
        assertEquals(".Head Office", officeDto.nameDecorated)
        assertEquals("HO-001", officeDto.externalId)
    }

    @Test
    fun officeDto_externalId_defaultsToNullWhenAbsentFromPayload() {
        // `GET /offices` operation schema declares `externalId`, but the abbreviated
        // `dtos.Office` registry block omits it — treated as optional/nullable per Hard Rule 4
        // (never invent a required field the registry doesn't declare as such).
        val payload = """{"id":2,"name":"Branch A","nameDecorated":"..Branch A"}"""
        val decoded = json.decodeFromString(OfficeDto.serializer(), payload)
        assertEquals(2L, decoded.id)
        assertEquals("Branch A", decoded.name)
        assertNull(decoded.externalId)
    }

    @Test
    fun officeDto_serializationRoundTrips() {
        val encoded = json.encodeToString(OfficeDto.serializer(), officeDto)
        assertTrue(encoded.contains("\"id\""))
        assertTrue(encoded.contains("\"name\""))
        assertTrue(encoded.contains("\"nameDecorated\""))
        assertTrue(encoded.contains("\"externalId\""))

        val decoded = json.decodeFromString(OfficeDto.serializer(), encoded)
        assertEquals(officeDto, decoded)
    }

    @Test
    fun officeDto_equality() {
        assertEquals(officeDto.copy(), officeDto.copy())
    }

    @Test
    fun officeDto_carriesSchemaVersion() {
        assertEquals(1, OfficeDto.SCHEMA_VERSION)
    }

    @Test
    fun officeDto_listOfOffices_decodesFromArrayPayload() {
        val payload = """
            [
              {"id":1,"name":"Head Office","nameDecorated":".Head Office","externalId":"HO-001"},
              {"id":2,"name":"Branch A","nameDecorated":"..Branch A"}
            ]
        """.trimIndent()
        val decoded = json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(OfficeDto.serializer()), payload)
        assertEquals(2, decoded.size)
        assertEquals("Head Office", decoded[0].name)
        assertNull(decoded[1].externalId)
    }

    // ---------- ContributionModelDto (3 known + UNKNOWN) ----------
    // Distinct from the catalogue-axis `ContributionModeDto` (SHARE_BASED_VARIABLE/FIXED/MINIMAL,
    // see `GroupTypeConfigDto.kt`) — this enum backs the per-group create-time payload and its
    // wire value-set differs (FIXED_AMOUNT/FIXED_NEGOTIATED are not on `ContributionModeDto`).

    @Test
    fun contributionModelDto_hasExactlyFourEntriesIncludingUnknownFallback() {
        assertEquals(4, ContributionModelDto.entries.size)
        assertTrue(ContributionModelDto.entries.contains(ContributionModelDto.UNKNOWN))
    }

    @Test
    fun contributionModelDto_decodesEachKnownWireValue() {
        assertEquals(ContributionModelDto.FIXED_AMOUNT, json.decodeFromString(ContributionModelDto.serializer(), "\"FIXED_AMOUNT\""))
        assertEquals(ContributionModelDto.SHARE_BASED_VARIABLE, json.decodeFromString(ContributionModelDto.serializer(), "\"SHARE_BASED_VARIABLE\""))
        assertEquals(ContributionModelDto.FIXED_NEGOTIATED, json.decodeFromString(ContributionModelDto.serializer(), "\"FIXED_NEGOTIATED\""))
    }

    @Test
    fun contributionModelDto_unknownServerValueCoercesToUnknownFallback_notCrash() {
        val decoded = json.decodeFromString(ContributionModelDto.serializer(), "\"TIERED_DYNAMIC\"")
        assertEquals(ContributionModelDto.UNKNOWN, decoded)
    }

    // ---------- ShareoutFormulaDto (5 known + UNKNOWN) ----------

    @Test
    fun shareoutFormulaDto_hasExactlySixEntriesIncludingUnknownFallback() {
        assertEquals(6, ShareoutFormulaDto.entries.size)
        assertTrue(ShareoutFormulaDto.entries.contains(ShareoutFormulaDto.UNKNOWN))
    }

    @Test
    fun shareoutFormulaDto_decodesEachKnownWireValue() {
        assertEquals(ShareoutFormulaDto.NONE, json.decodeFromString(ShareoutFormulaDto.serializer(), "\"NONE\""))
        assertEquals(ShareoutFormulaDto.PRORATA_SHARES, json.decodeFromString(ShareoutFormulaDto.serializer(), "\"PRORATA_SHARES\""))
        assertEquals(ShareoutFormulaDto.PRORATA_SAVINGS, json.decodeFromString(ShareoutFormulaDto.serializer(), "\"PRORATA_SAVINGS\""))
        assertEquals(ShareoutFormulaDto.EQUAL, json.decodeFromString(ShareoutFormulaDto.serializer(), "\"EQUAL\""))
        assertEquals(ShareoutFormulaDto.INVESTMENT_PROPORTIONAL, json.decodeFromString(ShareoutFormulaDto.serializer(), "\"INVESTMENT_PROPORTIONAL\""))
    }

    // ---------- PayoutOrderMethodDto (5 known + UNKNOWN) ----------

    @Test
    fun payoutOrderMethodDto_hasExactlySixEntriesIncludingUnknownFallback() {
        assertEquals(6, PayoutOrderMethodDto.entries.size)
        assertTrue(PayoutOrderMethodDto.entries.contains(PayoutOrderMethodDto.UNKNOWN))
    }

    @Test
    fun payoutOrderMethodDto_decodesEachKnownWireValue() {
        assertEquals(PayoutOrderMethodDto.FIXED_ORDER, json.decodeFromString(PayoutOrderMethodDto.serializer(), "\"FIXED_ORDER\""))
        assertEquals(PayoutOrderMethodDto.LOTTERY, json.decodeFromString(PayoutOrderMethodDto.serializer(), "\"LOTTERY\""))
        assertEquals(PayoutOrderMethodDto.AUCTION, json.decodeFromString(PayoutOrderMethodDto.serializer(), "\"AUCTION\""))
        assertEquals(PayoutOrderMethodDto.NEED_BASED, json.decodeFromString(PayoutOrderMethodDto.serializer(), "\"NEED_BASED\""))
        assertEquals(PayoutOrderMethodDto.NA, json.decodeFromString(PayoutOrderMethodDto.serializer(), "\"NA\""))
    }

    @Test
    fun payoutOrderMethodDto_unknownServerValueCoercesToUnknownFallback_notCrash() {
        val decoded = json.decodeFromString(PayoutOrderMethodDto.serializer(), "\"WEIGHTED_NEED\"")
        assertEquals(PayoutOrderMethodDto.UNKNOWN, decoded)
    }

    // ---------- Cross-version safety fixture (T7/EC30) ----------

    @Test
    fun createGroupRequestDto_toleratesServerAddedFieldAndUnknownEnumValues_oldClientNeverCrashes() {
        // Simulates a staggered rollout: the server has shipped a NEW top-level field
        // (`branchId`) plus a NEW payout_order_method value this (old) client schema does not
        // know about. Decoding MUST succeed, never throw.
        val serverPayload = """
            {
              "name":"Sunrise VSLA",
              "officeId":1,
              "userId":42,
              "currency":"KES",
              "meetingDay":"MONDAY",
              "meetingTime":"14:00",
              "branchId":"BR-9",
              "typeConfig":{
                "group_type":"VSLA",
                "pool_model":"ACCUMULATING",
                "contribution_model":"SHARE_BASED_VARIABLE",
                "shareout_formula":"PRORATA_SHARES",
                "payout_order_method":"WEIGHTED_NEED",
                "share_value":5.0,
                "contribution_amount":0.0,
                "social_fund_enabled":true,
                "social_fund_percent":5.0,
                "cycle_length_months":12,
                "loan_multiplier":3.0,
                "interest_rate":10.0,
                "fine_amount":1.0,
                "max_members":30
              }
            }
        """.trimIndent()
        val decoded = json.decodeFromString(CreateGroupRequestDto.serializer(), serverPayload)
        assertEquals("Sunrise VSLA", decoded.name)
        assertEquals(PayoutOrderMethodDto.UNKNOWN, decoded.typeConfig.payoutOrderMethod)
    }
}
