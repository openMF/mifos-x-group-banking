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
import kotlin.test.assertTrue

/**
 * TDD RED-first coverage for the group-type catalogue wire DTO (COMP-DT-003 —
 * `GET /companion/datatables/group_type_config/{entityId}`). See API.md#dtos.
 */
class GroupTypeConfigDtoTest {

    // Server-parity Json config: mirrors NetworkModule's client config (ignoreUnknownKeys +
    // coerceInputValues) so these tests prove EC30 cross-version tolerance, not just happy-path
    // round-tripping.
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val vslaDto = GroupTypeConfigDto(
        typeSlug = GroupTypeSlugDto.VSLA,
        displayName = "Village Savings & Loan Association",
        tagline = "Save in shares each meeting; borrow up to 3x your shares; share-out at year end",
        savingsMechanism = SavingsMechanismDto.ACCUMULATING,
        contributionMode = ContributionModeDto.SHARE_BASED_VARIABLE,
        lendingEnabled = true,
        hasSocialFund = true,
        hasBankLinkage = false,
        welfareOnlyMode = false,
        formallyRegistered = false,
        defaultLoanMultiplier = 3.0,
        defaultInterestRatePct = 10.0,
        defaultCycleLengthMonths = 12,
        maxMembers = 30,
        minMembers = 5,
    )

    // ---------- GroupTypeConfigDto ----------

    @Test
    fun groupTypeConfigDto_constructsWithAllFields() {
        assertEquals(GroupTypeSlugDto.VSLA, vslaDto.typeSlug)
        assertEquals("Village Savings & Loan Association", vslaDto.displayName)
        assertEquals(SavingsMechanismDto.ACCUMULATING, vslaDto.savingsMechanism)
        assertEquals(ContributionModeDto.SHARE_BASED_VARIABLE, vslaDto.contributionMode)
        assertTrue(vslaDto.lendingEnabled)
        assertTrue(vslaDto.hasSocialFund)
        assertEquals(3.0, vslaDto.defaultLoanMultiplier)
        assertEquals(10.0, vslaDto.defaultInterestRatePct)
        assertEquals(12, vslaDto.defaultCycleLengthMonths)
        assertEquals(30, vslaDto.maxMembers)
        assertEquals(5, vslaDto.minMembers)
    }

    @Test
    fun groupTypeConfigDto_serializationRoundTrips_wireFieldNamesAreCamelCase() {
        val encoded = json.encodeToString(GroupTypeConfigDto.serializer(), vslaDto)
        assertTrue(encoded.contains("\"typeSlug\""))
        assertTrue(encoded.contains("\"displayName\""))
        assertTrue(encoded.contains("\"savingsMechanism\""))
        assertTrue(encoded.contains("\"contributionMode\""))
        assertTrue(encoded.contains("\"defaultLoanMultiplier\""))
        assertTrue(encoded.contains("\"defaultInterestRatePct\""))
        assertTrue(encoded.contains("\"defaultCycleLengthMonths\""))
        assertTrue(encoded.contains("\"maxMembers\""))
        assertTrue(encoded.contains("\"minMembers\""))

        val decoded = json.decodeFromString(GroupTypeConfigDto.serializer(), encoded)
        assertEquals(vslaDto, decoded)
    }

    @Test
    fun groupTypeConfigDto_equality() {
        val a = vslaDto.copy()
        val b = vslaDto.copy()
        assertEquals(a, b)
    }

    @Test
    fun groupTypeConfigDto_carriesSchemaVersion() {
        assertEquals(1, GroupTypeConfigDto.SCHEMA_VERSION)
    }

    @Test
    fun groupTypeConfigDto_listOfNine_decodesFromArrayPayload() {
        // COMP-DT-003 response type is `array` of GroupTypeConfig rows — the shape actually
        // returned for entityId=0 (the seed catalogue).
        val payload = """
            [
              {"typeSlug":"VSLA","displayName":"VSLA","tagline":"t","savingsMechanism":"ACCUMULATING","contributionMode":"SHARE_BASED_VARIABLE","lendingEnabled":true,"hasSocialFund":true,"hasBankLinkage":false,"welfareOnlyMode":false,"formallyRegistered":false,"defaultLoanMultiplier":3.0,"defaultInterestRatePct":10.0,"defaultCycleLengthMonths":12,"maxMembers":30,"minMembers":5},
              {"typeSlug":"JLG","displayName":"JLG","tagline":"t","savingsMechanism":"NONE","contributionMode":"MINIMAL","lendingEnabled":false,"hasSocialFund":false,"hasBankLinkage":true,"welfareOnlyMode":false,"formallyRegistered":false,"defaultLoanMultiplier":0.0,"defaultInterestRatePct":0.0,"defaultCycleLengthMonths":4,"maxMembers":10,"minMembers":4}
            ]
        """.trimIndent()
        val decoded = json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(GroupTypeConfigDto.serializer()), payload)
        assertEquals(2, decoded.size)
        assertEquals(GroupTypeSlugDto.VSLA, decoded[0].typeSlug)
        assertEquals(GroupTypeSlugDto.JLG, decoded[1].typeSlug)
        assertEquals(SavingsMechanismDto.NONE, decoded[1].savingsMechanism)
    }

    // ---------- GroupTypeSlugDto (T7/EC30 unknown-value fallback, 9 known + UNKNOWN) ----------

    @Test
    fun groupTypeSlugDto_hasExactlyTenEntriesIncludingUnknownFallback() {
        assertEquals(10, GroupTypeSlugDto.entries.size)
        assertTrue(GroupTypeSlugDto.entries.contains(GroupTypeSlugDto.UNKNOWN))
    }

    @Test
    fun groupTypeSlugDto_decodesEachKnownWireValue() {
        val known = listOf(
            "VSLA" to GroupTypeSlugDto.VSLA,
            "ROSCA" to GroupTypeSlugDto.ROSCA,
            "ASCA" to GroupTypeSlugDto.ASCA,
            "SILC" to GroupTypeSlugDto.SILC,
            "SHG" to GroupTypeSlugDto.SHG,
            "SACCO" to GroupTypeSlugDto.SACCO,
            "CBO_VILLAGE_BANK" to GroupTypeSlugDto.CBO_VILLAGE_BANK,
            "BURIAL_WELFARE" to GroupTypeSlugDto.BURIAL_WELFARE,
            "JLG" to GroupTypeSlugDto.JLG,
        )
        known.forEach { (wire, expected) ->
            assertEquals(expected, json.decodeFromString(GroupTypeSlugDto.serializer(), "\"$wire\""))
        }
    }

    @Test
    fun groupTypeSlugDto_unknownServerValueCoercesToUnknownFallback_notCrash() {
        // A future server release adds a 10th group type — an OLD client on this schema must
        // decode the enclosing payload without throwing, per T7/EC30.
        val payload = """
            {"typeSlug":"COOP_UNION","displayName":"Coop Union","tagline":"t","savingsMechanism":"ACCUMULATING","contributionMode":"FIXED","lendingEnabled":true,"hasSocialFund":false,"hasBankLinkage":false,"welfareOnlyMode":false,"formallyRegistered":true,"defaultLoanMultiplier":2.0,"defaultInterestRatePct":8.0,"defaultCycleLengthMonths":12,"maxMembers":50,"minMembers":10}
        """.trimIndent()
        val decoded = json.decodeFromString(GroupTypeConfigDto.serializer(), payload)
        assertEquals(GroupTypeSlugDto.UNKNOWN, decoded.typeSlug)
    }

    // ---------- SavingsMechanismDto (3 known + UNKNOWN) ----------

    @Test
    fun savingsMechanismDto_hasExactlyFourEntriesIncludingUnknownFallback() {
        assertEquals(4, SavingsMechanismDto.entries.size)
        assertTrue(SavingsMechanismDto.entries.contains(SavingsMechanismDto.UNKNOWN))
    }

    @Test
    fun savingsMechanismDto_decodesEachKnownWireValue() {
        assertEquals(SavingsMechanismDto.ACCUMULATING, json.decodeFromString(SavingsMechanismDto.serializer(), "\"ACCUMULATING\""))
        assertEquals(SavingsMechanismDto.ROTATING_PAYOUT, json.decodeFromString(SavingsMechanismDto.serializer(), "\"ROTATING_PAYOUT\""))
        assertEquals(SavingsMechanismDto.NONE, json.decodeFromString(SavingsMechanismDto.serializer(), "\"NONE\""))
    }

    // ---------- ContributionModeDto (3 known + UNKNOWN) ----------

    @Test
    fun contributionModeDto_hasExactlyFourEntriesIncludingUnknownFallback() {
        assertEquals(4, ContributionModeDto.entries.size)
        assertTrue(ContributionModeDto.entries.contains(ContributionModeDto.UNKNOWN))
    }

    @Test
    fun contributionModeDto_decodesEachKnownWireValue() {
        assertEquals(ContributionModeDto.SHARE_BASED_VARIABLE, json.decodeFromString(ContributionModeDto.serializer(), "\"SHARE_BASED_VARIABLE\""))
        assertEquals(ContributionModeDto.FIXED, json.decodeFromString(ContributionModeDto.serializer(), "\"FIXED\""))
        assertEquals(ContributionModeDto.MINIMAL, json.decodeFromString(ContributionModeDto.serializer(), "\"MINIMAL\""))
    }

    // ---------- Cross-version safety fixture (T7/EC30) ----------

    @Test
    fun groupTypeConfigDto_toleratesServerAddedFieldAndUnknownEnumValues_oldClientNeverCrashes() {
        // Simulates a staggered rollout: the server has shipped a NEW top-level field
        // (`programmeCode`) plus a NEW savingsMechanism value this (old) client schema does not
        // know about. Decoding MUST succeed, never throw.
        val serverPayload = """
            {
              "typeSlug":"SILC",
              "displayName":"SILC",
              "tagline":"t",
              "savingsMechanism":"HYBRID_POOL",
              "contributionMode":"SHARE_BASED_VARIABLE",
              "lendingEnabled":true,
              "hasSocialFund":true,
              "hasBankLinkage":false,
              "welfareOnlyMode":false,
              "formallyRegistered":false,
              "defaultLoanMultiplier":3.0,
              "defaultInterestRatePct":10.0,
              "defaultCycleLengthMonths":12,
              "maxMembers":30,
              "minMembers":5,
              "programmeCode":"CRS-2026-not-in-old-schema"
            }
        """.trimIndent()
        val decoded = json.decodeFromString(GroupTypeConfigDto.serializer(), serverPayload)
        assertEquals(GroupTypeSlugDto.SILC, decoded.typeSlug)
        assertEquals(SavingsMechanismDto.UNKNOWN, decoded.savingsMechanism)
    }
}
