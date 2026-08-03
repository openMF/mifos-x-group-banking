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
 * TDD RED-first coverage for the group-list wire contract (COMP-GRP-001 —
 * `GET /companion/groups/mine`). See API.md#dtos.
 */
class GroupDtoTest {

    // Server-parity Json config: mirrors NetworkModule's client config (ignoreUnknownKeys +
    // coerceInputValues) so these tests prove EC30 cross-version tolerance, not just happy-path
    // round-tripping.
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val mwangazaDto = GroupDto(
        id = "GRP-20260509-001",
        name = "Mwangaza Women's Group",
        groupType = GroupTypeDto.VSLA,
        viewerRole = ViewerRoleDto.ORGANIZER,
        cycleNumber = 1,
        memberCount = 20,
        lastMeetingDate = "2026-07-14",
        healthIndicator = HealthIndicatorDto.GREEN,
        overdueRate = 0.00,
        status = "ACTIVE",
        fineractCenterId = 1001L,
    )

    // ---------- GroupDto ----------

    @Test
    fun groupDto_constructsWithAllFields() {
        assertEquals("GRP-20260509-001", mwangazaDto.id)
        assertEquals("Mwangaza Women's Group", mwangazaDto.name)
        assertEquals(GroupTypeDto.VSLA, mwangazaDto.groupType)
        assertEquals(ViewerRoleDto.ORGANIZER, mwangazaDto.viewerRole)
        assertEquals(1, mwangazaDto.cycleNumber)
        assertEquals(20, mwangazaDto.memberCount)
        assertEquals("2026-07-14", mwangazaDto.lastMeetingDate)
        assertEquals(HealthIndicatorDto.GREEN, mwangazaDto.healthIndicator)
        assertEquals(0.00, mwangazaDto.overdueRate)
        assertEquals("ACTIVE", mwangazaDto.status)
        assertEquals(1001L, mwangazaDto.fineractCenterId)
    }

    @Test
    fun groupDto_serializationRoundTrips_wireFieldNamesAreCamelCase() {
        val encoded = json.encodeToString(GroupDto.serializer(), mwangazaDto)
        assertTrue(encoded.contains("\"id\""))
        assertTrue(encoded.contains("\"name\""))
        assertTrue(encoded.contains("\"groupType\""))
        assertTrue(encoded.contains("\"viewerRole\""))
        assertTrue(encoded.contains("\"cycleNumber\""))
        assertTrue(encoded.contains("\"memberCount\""))
        assertTrue(encoded.contains("\"lastMeetingDate\""))
        assertTrue(encoded.contains("\"healthIndicator\""))
        assertTrue(encoded.contains("\"overdueRate\""))
        assertTrue(encoded.contains("\"status\""))
        assertTrue(encoded.contains("\"fineractCenterId\""))

        val decoded = json.decodeFromString(GroupDto.serializer(), encoded)
        assertEquals(mwangazaDto, decoded)
    }

    @Test
    fun groupDto_equality() {
        val a = mwangazaDto.copy()
        val b = mwangazaDto.copy()
        assertEquals(a, b)
    }

    @Test
    fun groupDto_carriesSchemaVersion() {
        assertEquals(1, GroupDto.SCHEMA_VERSION)
    }

    // ---------- GroupPageDto (offset-paginated envelope, page_size 20) ----------

    @Test
    fun groupPageDto_constructsWithTotalFilteredRecordsAndPageItems() {
        val page = GroupPageDto(totalFilteredRecords = 5, pageItems = listOf(mwangazaDto))
        assertEquals(5, page.totalFilteredRecords)
        assertEquals(listOf(mwangazaDto), page.pageItems)
    }

    @Test
    fun groupPageDto_pageItemsDefaultsToEmptyList() {
        val page = GroupPageDto(totalFilteredRecords = 0)
        assertTrue(page.pageItems.isEmpty())
    }

    @Test
    fun groupPageDto_serializationRoundTrips() {
        val page = GroupPageDto(totalFilteredRecords = 5, pageItems = listOf(mwangazaDto))
        val encoded = json.encodeToString(GroupPageDto.serializer(), page)
        assertTrue(encoded.contains("\"totalFilteredRecords\""))
        assertTrue(encoded.contains("\"pageItems\""))
        val decoded = json.decodeFromString(GroupPageDto.serializer(), encoded)
        assertEquals(page, decoded)
    }

    @Test
    fun groupPageDto_decodesFromCompanionGroupsMineShape() {
        // The literal COMP-GRP-001 GET /companion/groups/mine response envelope.
        val payload = """
            {
              "totalFilteredRecords": 2,
              "pageItems": [
                {"id":"GRP-1","name":"A","groupType":"VSLA","viewerRole":"ORGANIZER","cycleNumber":1,"memberCount":20,"lastMeetingDate":"2026-07-14","healthIndicator":"GREEN","overdueRate":0.0,"status":"ACTIVE","fineractCenterId":1001},
                {"id":"GRP-2","name":"B","groupType":"ROSCA","viewerRole":"MEMBER","cycleNumber":3,"memberCount":10,"lastMeetingDate":"2026-07-10","healthIndicator":"AMBER","overdueRate":0.12,"status":"ACTIVE","fineractCenterId":1002}
              ]
            }
        """.trimIndent()
        val decoded = json.decodeFromString(GroupPageDto.serializer(), payload)
        assertEquals(2, decoded.totalFilteredRecords)
        assertEquals(2, decoded.pageItems.size)
        assertEquals(GroupTypeDto.ROSCA, decoded.pageItems[1].groupType)
    }

    // ---------- GroupTypeDto (9 known + UNKNOWN, T7/EC30 fallback) ----------

    @Test
    fun groupTypeDto_hasExactlyTenEntriesIncludingUnknownFallback() {
        assertEquals(10, GroupTypeDto.entries.size)
        assertTrue(GroupTypeDto.entries.contains(GroupTypeDto.UNKNOWN))
    }

    @Test
    fun groupTypeDto_decodesEachKnownWireValue() {
        val known = listOf(
            "VSLA" to GroupTypeDto.VSLA,
            "ROSCA" to GroupTypeDto.ROSCA,
            "ASCA" to GroupTypeDto.ASCA,
            "SILC" to GroupTypeDto.SILC,
            "SHG" to GroupTypeDto.SHG,
            "SACCO" to GroupTypeDto.SACCO,
            "CBO" to GroupTypeDto.CBO,
            "BURIAL" to GroupTypeDto.BURIAL,
            "JLG" to GroupTypeDto.JLG,
        )
        known.forEach { (wire, expected) ->
            assertEquals(expected, json.decodeFromString(GroupTypeDto.serializer(), "\"$wire\""))
        }
    }

    @Test
    fun groupTypeDto_unknownServerValueCoercesToUnknownFallback_notCrash() {
        val payload = """
            {"id":"GRP-9","name":"New","groupType":"COOP_UNION","viewerRole":"MEMBER","cycleNumber":1,"memberCount":5,"lastMeetingDate":"2026-07-01","healthIndicator":"GREEN","overdueRate":0.0,"status":"ACTIVE","fineractCenterId":9999}
        """.trimIndent()
        val decoded = json.decodeFromString(GroupDto.serializer(), payload)
        assertEquals(GroupTypeDto.UNKNOWN, decoded.groupType)
    }

    // ---------- ViewerRoleDto (5 known + UNKNOWN) ----------

    @Test
    fun viewerRoleDto_hasExactlySixEntriesIncludingUnknownFallback() {
        assertEquals(6, ViewerRoleDto.entries.size)
        assertTrue(ViewerRoleDto.entries.contains(ViewerRoleDto.UNKNOWN))
    }

    @Test
    fun viewerRoleDto_decodesEachKnownWireValue() {
        assertEquals(ViewerRoleDto.ORGANIZER, json.decodeFromString(ViewerRoleDto.serializer(), "\"ORGANIZER\""))
        assertEquals(ViewerRoleDto.MEMBER, json.decodeFromString(ViewerRoleDto.serializer(), "\"MEMBER\""))
        assertEquals(ViewerRoleDto.TREASURER, json.decodeFromString(ViewerRoleDto.serializer(), "\"TREASURER\""))
        assertEquals(ViewerRoleDto.CHAIRPERSON, json.decodeFromString(ViewerRoleDto.serializer(), "\"CHAIRPERSON\""))
        assertEquals(ViewerRoleDto.SECRETARY, json.decodeFromString(ViewerRoleDto.serializer(), "\"SECRETARY\""))
    }

    @Test
    fun viewerRoleDto_unknownServerValueCoercesToUnknownFallback_notCrash() {
        val payload = """
            {"id":"GRP-9","name":"New","groupType":"VSLA","viewerRole":"AUDITOR","cycleNumber":1,"memberCount":5,"lastMeetingDate":"2026-07-01","healthIndicator":"GREEN","overdueRate":0.0,"status":"ACTIVE","fineractCenterId":9999}
        """.trimIndent()
        val decoded = json.decodeFromString(GroupDto.serializer(), payload)
        assertEquals(ViewerRoleDto.UNKNOWN, decoded.viewerRole)
    }

    // ---------- HealthIndicatorDto (3 known + UNKNOWN) ----------

    @Test
    fun healthIndicatorDto_hasExactlyFourEntriesIncludingUnknownFallback() {
        assertEquals(4, HealthIndicatorDto.entries.size)
        assertTrue(HealthIndicatorDto.entries.contains(HealthIndicatorDto.UNKNOWN))
    }

    @Test
    fun healthIndicatorDto_decodesEachKnownWireValue() {
        assertEquals(HealthIndicatorDto.GREEN, json.decodeFromString(HealthIndicatorDto.serializer(), "\"GREEN\""))
        assertEquals(HealthIndicatorDto.AMBER, json.decodeFromString(HealthIndicatorDto.serializer(), "\"AMBER\""))
        assertEquals(HealthIndicatorDto.RED, json.decodeFromString(HealthIndicatorDto.serializer(), "\"RED\""))
    }

    // ---------- Cross-version safety fixture (T7/EC30) ----------

    @Test
    fun groupDto_toleratesServerAddedFieldAndUnknownEnumValues_oldClientNeverCrashes() {
        // Simulates a staggered rollout: the server has shipped a NEW top-level field
        // (`programCode`) plus a NEW viewerRole value this (old) client schema does not know
        // about. Decoding MUST succeed, never throw.
        val serverPayload = """
            {
              "id":"GRP-20260509-001",
              "name":"Mwangaza Women's Group",
              "groupType":"VSLA",
              "viewerRole":"AUDITOR",
              "cycleNumber":1,
              "memberCount":20,
              "lastMeetingDate":"2026-07-14",
              "healthIndicator":"GREEN",
              "overdueRate":0.00,
              "status":"ACTIVE",
              "fineractCenterId":1001,
              "programCode":"KE-2026-not-in-old-schema"
            }
        """.trimIndent()
        val decoded = json.decodeFromString(GroupDto.serializer(), serverPayload)
        assertEquals("GRP-20260509-001", decoded.id)
        assertEquals(ViewerRoleDto.UNKNOWN, decoded.viewerRole)
    }
}
