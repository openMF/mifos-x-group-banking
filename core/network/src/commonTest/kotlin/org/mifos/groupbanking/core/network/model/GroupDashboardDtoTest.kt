/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * TDD RED-first coverage for the group-dashboard wire DTOs (COMP-GRP-001 — `get_group` +
 * `get_viewer_role` + `get_group_corpus` + `get_group_accounts`). See API.md#dtos.
 */
class GroupDashboardDtoTest {

    // Server-parity Json config: mirrors NetworkModule's client config (ignoreUnknownKeys +
    // coerceInputValues) so these tests prove EC30 cross-version tolerance, not just happy-path
    // round-tripping.
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val vslaInstanceConfigDto = GroupInstanceConfigDto(
        groupType = GroupTypeSlugDto.VSLA,
        poolModel = SavingsMechanismDto.ACCUMULATING,
        contributionModel = GroupContributionModelDto.SHARE_BASED_VARIABLE,
        shareoutFormula = "PRORATA_SHARES",
        payoutOrderMethod = "NA",
        shareValue = 200.0,
        contributionAmount = 200.0,
        socialFundEnabled = true,
        cycleLengthMonths = 12,
        loanMultiplier = 3.0,
        interestRate = 10.0,
        fineAmount = 50.0,
    )

    private val groupDetailDto = GroupDetailDto(
        id = "grp-1",
        fineractCenterId = 500L,
        name = "Mwangaza Women's Group",
        cycleNumber = 1,
        cycleLengthMonths = 12,
        meetingFrequency = "Weekly",
        memberCount = 20,
        overdueLoansCount = 0,
        status = "ACTIVE",
        typeConfig = vslaInstanceConfigDto,
    )

    private val viewerRoleInfoDto = ViewerRoleInfoDto(
        role = ViewerRoleDto.ORGANIZER,
        memberId = 42L,
    )

    private val corpusDto = GroupCorpusDto(
        currentBalance = 47500.0,
        openingBalance = 0.0,
        totalContributionsThisCycle = 52500.0,
        totalLoansOutstanding = 5000.0,
        lastUpdated = "2026-07-20T10:00:00Z",
        rotationPosition = null,
        nextRecipientName = null,
        nextRecipientPosition = null,
    )

    private val activityItemDto = ActivityItemDto(
        id = "act-1",
        type = ActivityTypeDto.DEPOSIT,
        description = "Weekly contribution",
        amount = 200.0,
        date = "2026-07-19",
        memberName = "Amina Hassan",
    )

    private val accountsDto = GroupAccountsDto(
        savingsBalance = 52500.0,
        loansOutstanding = 5000.0,
        activeLoanCount = 1,
        shareOutProjection = 2750.0,
        recentActivity = listOf(activityItemDto),
    )

    private val configDto = GroupConfigDto(
        shareValue = 200.0,
        shareMin = 1,
        shareMax = 5,
        contributionAmount = 200.0,
        loanMultiplier = 3.0,
        interestRate = 10.0,
        cycleLengthMonths = 12,
        fineAmount = 50.0,
        minimumDisbursementThreshold = 5000.0,
    )

    private val dashboardResponseDto = GroupDashboardResponseDto(
        group = groupDetailDto,
        viewerRole = viewerRoleInfoDto,
        corpus = corpusDto,
        accounts = accountsDto,
    )

    // ---------- GroupDashboardResponseDto ----------

    @Test
    fun groupDashboardResponseDto_constructsWithAllFields() {
        assertEquals(groupDetailDto, dashboardResponseDto.group)
        assertEquals(viewerRoleInfoDto, dashboardResponseDto.viewerRole)
        assertEquals(corpusDto, dashboardResponseDto.corpus)
        assertEquals(accountsDto, dashboardResponseDto.accounts)
    }

    @Test
    fun groupDashboardResponseDto_serializationRoundTrips() {
        val encoded = json.encodeToString(GroupDashboardResponseDto.serializer(), dashboardResponseDto)
        val decoded = json.decodeFromString(GroupDashboardResponseDto.serializer(), encoded)
        assertEquals(dashboardResponseDto, decoded)
    }

    @Test
    fun groupDashboardResponseDto_equality() {
        assertEquals(dashboardResponseDto.copy(), dashboardResponseDto.copy())
    }

    @Test
    fun groupDashboardResponseDto_carriesSchemaVersion() {
        assertEquals(1, GroupDashboardResponseDto.SCHEMA_VERSION)
    }

    // ---------- GroupDetailDto ----------

    @Test
    fun groupDetailDto_constructsWithAllFields() {
        assertEquals("grp-1", groupDetailDto.id)
        assertEquals(500L, groupDetailDto.fineractCenterId)
        assertEquals("Mwangaza Women's Group", groupDetailDto.name)
        assertEquals(1, groupDetailDto.cycleNumber)
        assertEquals(12, groupDetailDto.cycleLengthMonths)
        assertEquals("Weekly", groupDetailDto.meetingFrequency)
        assertEquals(20, groupDetailDto.memberCount)
        assertEquals(0, groupDetailDto.overdueLoansCount)
        assertEquals("ACTIVE", groupDetailDto.status)
        assertEquals(vslaInstanceConfigDto, groupDetailDto.typeConfig)
    }

    @Test
    fun groupDetailDto_serializationRoundTrips_wireFieldNamesAreCamelCase() {
        val encoded = json.encodeToString(GroupDetailDto.serializer(), groupDetailDto)
        assertTrue(encoded.contains("\"fineractCenterId\""))
        assertTrue(encoded.contains("\"cycleLengthMonths\""))
        assertTrue(encoded.contains("\"meetingFrequency\""))
        assertTrue(encoded.contains("\"overdueLoansCount\""))
        assertTrue(encoded.contains("\"typeConfig\""))
        assertEquals(groupDetailDto, json.decodeFromString(GroupDetailDto.serializer(), encoded))
    }

    @Test
    fun groupDetailDto_equality() {
        assertEquals(groupDetailDto.copy(), groupDetailDto.copy())
    }

    @Test
    fun groupDetailDto_carriesSchemaVersion() {
        assertEquals(1, GroupDetailDto.SCHEMA_VERSION)
    }

    // ---------- GroupInstanceConfigDto (snake_case @SerialName per api.yaml#dtos.GroupTypeConfig) ----------

    @Test
    fun groupInstanceConfigDto_constructsWithAllFields() {
        assertEquals(GroupTypeSlugDto.VSLA, vslaInstanceConfigDto.groupType)
        assertEquals(SavingsMechanismDto.ACCUMULATING, vslaInstanceConfigDto.poolModel)
        assertEquals(GroupContributionModelDto.SHARE_BASED_VARIABLE, vslaInstanceConfigDto.contributionModel)
        assertEquals("PRORATA_SHARES", vslaInstanceConfigDto.shareoutFormula)
        assertEquals(200.0, vslaInstanceConfigDto.shareValue)
        assertTrue(vslaInstanceConfigDto.socialFundEnabled)
        assertEquals(3.0, vslaInstanceConfigDto.loanMultiplier)
        assertEquals(10.0, vslaInstanceConfigDto.interestRate)
        assertEquals(50.0, vslaInstanceConfigDto.fineAmount)
    }

    @Test
    fun groupInstanceConfigDto_serializationRoundTrips_wireFieldNamesAreSnakeCase() {
        val encoded = json.encodeToString(GroupInstanceConfigDto.serializer(), vslaInstanceConfigDto)
        assertTrue(encoded.contains("\"group_type\""))
        assertTrue(encoded.contains("\"pool_model\""))
        assertTrue(encoded.contains("\"contribution_model\""))
        assertTrue(encoded.contains("\"shareout_formula\""))
        assertTrue(encoded.contains("\"payout_order_method\""))
        assertTrue(encoded.contains("\"share_value\""))
        assertTrue(encoded.contains("\"contribution_amount\""))
        assertTrue(encoded.contains("\"social_fund_enabled\""))
        assertTrue(encoded.contains("\"cycle_length_months\""))
        assertTrue(encoded.contains("\"loan_multiplier\""))
        assertTrue(encoded.contains("\"interest_rate\""))
        assertTrue(encoded.contains("\"fine_amount\""))
        assertEquals(vslaInstanceConfigDto, json.decodeFromString(GroupInstanceConfigDto.serializer(), encoded))
    }

    @Test
    fun groupInstanceConfigDto_equality() {
        assertEquals(vslaInstanceConfigDto.copy(), vslaInstanceConfigDto.copy())
    }

    @Test
    fun groupInstanceConfigDto_carriesSchemaVersion() {
        assertEquals(1, GroupInstanceConfigDto.SCHEMA_VERSION)
    }

    @Test
    fun groupInstanceConfigDto_unknownGroupTypeAndPoolModelCoerceToUnknownFallback_notCrash() {
        // A future server adds a new group_type / pool_model value — an OLD client must not crash.
        val payload = """
            {
              "group_type":"COOP_UNION",
              "pool_model":"HYBRID_POOL",
              "contribution_model":"TIERED",
              "shareout_formula":"t",
              "payout_order_method":"t",
              "share_value":1.0,
              "contribution_amount":1.0,
              "social_fund_enabled":false,
              "cycle_length_months":6,
              "loan_multiplier":1.0,
              "interest_rate":1.0,
              "fine_amount":1.0,
              "programmeCode":"new-field-not-in-old-schema"
            }
        """.trimIndent()
        val decoded = json.decodeFromString(GroupInstanceConfigDto.serializer(), payload)
        assertEquals(GroupTypeSlugDto.UNKNOWN, decoded.groupType)
        assertEquals(SavingsMechanismDto.UNKNOWN, decoded.poolModel)
        assertEquals(GroupContributionModelDto.UNKNOWN, decoded.contributionModel)
    }

    // ---------- GroupContributionModelDto (3 known + UNKNOWN) ----------

    @Test
    fun groupContributionModelDto_hasExactlyFourEntriesIncludingUnknownFallback() {
        assertEquals(4, GroupContributionModelDto.entries.size)
        assertTrue(GroupContributionModelDto.entries.contains(GroupContributionModelDto.UNKNOWN))
    }

    @Test
    fun groupContributionModelDto_decodesEachKnownWireValue() {
        assertEquals(GroupContributionModelDto.FIXED_AMOUNT, json.decodeFromString(GroupContributionModelDto.serializer(), "\"FIXED_AMOUNT\""))
        assertEquals(GroupContributionModelDto.SHARE_BASED_VARIABLE, json.decodeFromString(GroupContributionModelDto.serializer(), "\"SHARE_BASED_VARIABLE\""))
        assertEquals(GroupContributionModelDto.FIXED_NEGOTIATED, json.decodeFromString(GroupContributionModelDto.serializer(), "\"FIXED_NEGOTIATED\""))
    }

    @Test
    fun groupContributionModelDto_unknownWireValueCoercesToUnknownFallback() {
        assertEquals(GroupContributionModelDto.UNKNOWN, json.decodeFromString(GroupContributionModelDto.serializer(), "\"TIERED\""))
    }

    // ---------- ViewerRoleInfoDto ----------

    @Test
    fun viewerRoleInfoDto_constructsWithAllFields() {
        assertEquals(ViewerRoleDto.ORGANIZER, viewerRoleInfoDto.role)
        assertEquals(42L, viewerRoleInfoDto.memberId)
    }

    @Test
    fun viewerRoleInfoDto_serializationRoundTrips() {
        val encoded = json.encodeToString(ViewerRoleInfoDto.serializer(), viewerRoleInfoDto)
        assertTrue(encoded.contains("\"role\""))
        assertTrue(encoded.contains("\"memberId\""))
        assertEquals(viewerRoleInfoDto, json.decodeFromString(ViewerRoleInfoDto.serializer(), encoded))
    }

    @Test
    fun viewerRoleInfoDto_equality() {
        assertEquals(viewerRoleInfoDto.copy(), viewerRoleInfoDto.copy())
    }

    @Test
    fun viewerRoleInfoDto_carriesSchemaVersion() {
        assertEquals(1, ViewerRoleInfoDto.SCHEMA_VERSION)
    }

    @Test
    fun viewerRoleInfoDto_unknownRoleCoercesToUnknownFallback_notCrash() {
        val payload = """{"role":"AUDITOR","memberId":9}"""
        val decoded = json.decodeFromString(ViewerRoleInfoDto.serializer(), payload)
        assertEquals(ViewerRoleDto.UNKNOWN, decoded.role)
    }

    // ---------- GroupCorpusDto ----------

    @Test
    fun groupCorpusDto_constructsWithAllFields_accumulatingRow() {
        assertEquals(47500.0, corpusDto.currentBalance)
        assertEquals(0.0, corpusDto.openingBalance)
        assertEquals(52500.0, corpusDto.totalContributionsThisCycle)
        assertEquals(5000.0, corpusDto.totalLoansOutstanding)
        assertEquals("2026-07-20T10:00:00Z", corpusDto.lastUpdated)
        assertNull(corpusDto.rotationPosition)
        assertNull(corpusDto.nextRecipientName)
        assertNull(corpusDto.nextRecipientPosition)
    }

    @Test
    fun groupCorpusDto_nullableFieldsDefaultToNull_rotatingRow() {
        val rotating = GroupCorpusDto(
            currentBalance = 20000.0,
            openingBalance = 0.0,
            totalContributionsThisCycle = 0.0,
            totalLoansOutstanding = 0.0,
            lastUpdated = "2026-07-20T10:00:00Z",
            rotationPosition = 7,
            nextRecipientName = "Amina Hassan",
            nextRecipientPosition = 4,
        )
        assertEquals(7, rotating.rotationPosition)
        assertEquals("Amina Hassan", rotating.nextRecipientName)
        assertEquals(4, rotating.nextRecipientPosition)
    }

    @Test
    fun groupCorpusDto_serializationRoundTrips() {
        val encoded = json.encodeToString(GroupCorpusDto.serializer(), corpusDto)
        assertEquals(corpusDto, json.decodeFromString(GroupCorpusDto.serializer(), encoded))
    }

    @Test
    fun groupCorpusDto_equality() {
        assertEquals(corpusDto.copy(), corpusDto.copy())
    }

    @Test
    fun groupCorpusDto_carriesSchemaVersion() {
        assertEquals(1, GroupCorpusDto.SCHEMA_VERSION)
    }

    @Test
    fun groupCorpusDto_missingRotationFieldsCoerceToNull_notCrash() {
        val payload = """
            {"currentBalance":100.0,"openingBalance":0.0,"totalContributionsThisCycle":100.0,"totalLoansOutstanding":0.0,"lastUpdated":"2026-07-20"}
        """.trimIndent()
        val decoded = json.decodeFromString(GroupCorpusDto.serializer(), payload)
        assertNull(decoded.rotationPosition)
        assertNull(decoded.nextRecipientName)
        assertNull(decoded.nextRecipientPosition)
    }

    // ---------- ActivityItemDto ----------

    @Test
    fun activityItemDto_constructsWithAllFields() {
        assertEquals("act-1", activityItemDto.id)
        assertEquals(ActivityTypeDto.DEPOSIT, activityItemDto.type)
        assertEquals("Weekly contribution", activityItemDto.description)
        assertEquals(200.0, activityItemDto.amount)
        assertEquals("2026-07-19", activityItemDto.date)
        assertEquals("Amina Hassan", activityItemDto.memberName)
    }

    @Test
    fun activityItemDto_nullableFieldsDefaultToNull() {
        val payload = """{"id":"act-2","type":"MEETING","description":"Weekly meeting","date":"2026-07-19"}"""
        val decoded = json.decodeFromString(ActivityItemDto.serializer(), payload)
        assertNull(decoded.amount)
        assertNull(decoded.memberName)
        assertEquals(ActivityTypeDto.MEETING, decoded.type)
    }

    @Test
    fun activityItemDto_serializationRoundTrips() {
        val encoded = json.encodeToString(ActivityItemDto.serializer(), activityItemDto)
        assertEquals(activityItemDto, json.decodeFromString(ActivityItemDto.serializer(), encoded))
    }

    @Test
    fun activityItemDto_equality() {
        assertEquals(activityItemDto.copy(), activityItemDto.copy())
    }

    @Test
    fun activityItemDto_carriesSchemaVersion() {
        assertEquals(1, ActivityItemDto.SCHEMA_VERSION)
    }

    // ---------- ActivityTypeDto (5 known + UNKNOWN) ----------

    @Test
    fun activityTypeDto_hasExactlySixEntriesIncludingUnknownFallback() {
        assertEquals(6, ActivityTypeDto.entries.size)
        assertTrue(ActivityTypeDto.entries.contains(ActivityTypeDto.UNKNOWN))
    }

    @Test
    fun activityTypeDto_decodesEachKnownWireValue() {
        val known = listOf(
            "MEETING" to ActivityTypeDto.MEETING,
            "DEPOSIT" to ActivityTypeDto.DEPOSIT,
            "LOAN" to ActivityTypeDto.LOAN,
            "PENALTY" to ActivityTypeDto.PENALTY,
            "SHARE_OUT" to ActivityTypeDto.SHARE_OUT,
        )
        known.forEach { (wire, expected) ->
            assertEquals(expected, json.decodeFromString(ActivityTypeDto.serializer(), "\"$wire\""))
        }
    }

    @Test
    fun activityTypeDto_unknownWireValueCoercesToUnknownFallback_notCrash() {
        val payload = """{"id":"act-9","type":"WELFARE_PAYOUT","description":"d","date":"2026-07-20"}"""
        val decoded = json.decodeFromString(ActivityItemDto.serializer(), payload)
        assertEquals(ActivityTypeDto.UNKNOWN, decoded.type)
    }

    // ---------- GroupAccountsDto ----------

    @Test
    fun groupAccountsDto_constructsWithAllFields() {
        assertEquals(52500.0, accountsDto.savingsBalance)
        assertEquals(5000.0, accountsDto.loansOutstanding)
        assertEquals(1, accountsDto.activeLoanCount)
        assertEquals(2750.0, accountsDto.shareOutProjection)
        assertEquals(1, accountsDto.recentActivity.size)
    }

    @Test
    fun groupAccountsDto_nullableAndDefaultFields_rotatingTypeHasNoShareOutProjection() {
        val rotating = GroupAccountsDto(
            savingsBalance = 20000.0,
            loansOutstanding = 0.0,
            activeLoanCount = 0,
        )
        assertNull(rotating.shareOutProjection)
        assertEquals(emptyList(), rotating.recentActivity)
    }

    @Test
    fun groupAccountsDto_serializationRoundTrips() {
        val encoded = json.encodeToString(GroupAccountsDto.serializer(), accountsDto)
        assertEquals(accountsDto, json.decodeFromString(GroupAccountsDto.serializer(), encoded))
    }

    @Test
    fun groupAccountsDto_equality() {
        assertEquals(accountsDto.copy(), accountsDto.copy())
    }

    @Test
    fun groupAccountsDto_carriesSchemaVersion() {
        assertEquals(1, GroupAccountsDto.SCHEMA_VERSION)
    }

    // ---------- GroupConfigDto ----------

    @Test
    fun groupConfigDto_constructsWithAllFields() {
        assertEquals(200.0, configDto.shareValue)
        assertEquals(1, configDto.shareMin)
        assertEquals(5, configDto.shareMax)
        assertEquals(200.0, configDto.contributionAmount)
        assertEquals(3.0, configDto.loanMultiplier)
        assertEquals(10.0, configDto.interestRate)
        assertEquals(12, configDto.cycleLengthMonths)
        assertEquals(50.0, configDto.fineAmount)
        assertEquals(5000.0, configDto.minimumDisbursementThreshold)
    }

    @Test
    fun groupConfigDto_nullableFieldsDefaultToNull_onlyCycleLengthMonthsRequired() {
        val minimal = GroupConfigDto(cycleLengthMonths = 10)
        assertNull(minimal.shareValue)
        assertNull(minimal.shareMin)
        assertNull(minimal.shareMax)
        assertNull(minimal.contributionAmount)
        assertNull(minimal.loanMultiplier)
        assertNull(minimal.interestRate)
        assertNull(minimal.fineAmount)
        assertNull(minimal.minimumDisbursementThreshold)
        assertEquals(10, minimal.cycleLengthMonths)
    }

    @Test
    fun groupConfigDto_serializationRoundTrips() {
        val encoded = json.encodeToString(GroupConfigDto.serializer(), configDto)
        assertEquals(configDto, json.decodeFromString(GroupConfigDto.serializer(), encoded))
    }

    @Test
    fun groupConfigDto_equality() {
        assertEquals(configDto.copy(), configDto.copy())
    }

    @Test
    fun groupConfigDto_carriesSchemaVersion() {
        assertEquals(1, GroupConfigDto.SCHEMA_VERSION)
    }

    // ---------- Cross-version safety fixture (T7/EC30) — full composite tolerates a server-added field ----------

    @Test
    fun groupDashboardResponseDto_toleratesServerAddedTopLevelField_oldClientNeverCrashes() {
        val serverPayload = """
            {
              "group": {
                "id":"grp-2","fineractCenterId":9,"name":"Jiunge ROSCA Circle","cycleNumber":3,
                "cycleLengthMonths":10,"meetingFrequency":"Monthly","memberCount":10,
                "overdueLoansCount":0,"status":"ACTIVE",
                "typeConfig": {
                  "group_type":"ROSCA","pool_model":"ROTATING_PAYOUT","contribution_model":"FIXED_AMOUNT",
                  "shareout_formula":"NA","payout_order_method":"FIXED_ORDER","share_value":0.0,
                  "contribution_amount":2000.0,"social_fund_enabled":false,"cycle_length_months":10,
                  "loan_multiplier":0.0,"interest_rate":0.0,"fine_amount":0.0
                }
              },
              "viewerRole": {"role":"MEMBER","memberId":11},
              "corpus": {
                "currentBalance":20000.0,"openingBalance":0.0,"totalContributionsThisCycle":0.0,
                "totalLoansOutstanding":0.0,"lastUpdated":"2026-07-20","rotationPosition":7,
                "nextRecipientName":"Amina Hassan","nextRecipientPosition":4
              },
              "accounts": {"savingsBalance":20000.0,"loansOutstanding":0.0,"activeLoanCount":0,"recentActivity":[]},
              "syncedAtServerVersion":"2026.7.20-not-in-old-schema"
            }
        """.trimIndent()
        val decoded = json.decodeFromString(GroupDashboardResponseDto.serializer(), serverPayload)
        assertEquals("grp-2", decoded.group.id)
        assertEquals(7, decoded.corpus.rotationPosition)
        assertEquals(ViewerRoleDto.MEMBER, decoded.viewerRole.role)
    }
}
