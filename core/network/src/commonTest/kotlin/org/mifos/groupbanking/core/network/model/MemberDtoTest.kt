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
 * TDD RED-first coverage for the member-list wire contract (`GET
 * /groups/{groupId}/clients`). See API.md#dtos.
 */
class MemberDtoTest {

    // Server-parity Json config: mirrors NetworkModule's client config (ignoreUnknownKeys +
    // coerceInputValues) so these tests prove EC30 cross-version tolerance, not just happy-path
    // round-tripping.
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val amaraDto = MemberDto(
        id = "MBR-20260509-001",
        fineractClientId = 5001L,
        displayName = "Amara Otieno",
        photoUri = "https://cdn.example.org/photos/5001.jpg",
        role = MemberRoleDto.CHAIRPERSON,
        savingsBalance = 1250.50,
        loanStatus = LoanStatusDto.ACTIVE,
    )

    // ---------- MemberDto ----------

    @Test
    fun memberDto_constructsWithAllFields() {
        assertEquals("MBR-20260509-001", amaraDto.id)
        assertEquals(5001L, amaraDto.fineractClientId)
        assertEquals("Amara Otieno", amaraDto.displayName)
        assertEquals("https://cdn.example.org/photos/5001.jpg", amaraDto.photoUri)
        assertEquals(MemberRoleDto.CHAIRPERSON, amaraDto.role)
        assertEquals(1250.50, amaraDto.savingsBalance)
        assertEquals(LoanStatusDto.ACTIVE, amaraDto.loanStatus)
    }

    @Test
    fun memberDto_photoUriDefaultsToNullWhenOmitted() {
        val dto = MemberDto(
            id = "MBR-2",
            fineractClientId = 5002L,
            displayName = "Juma Kamau",
            role = MemberRoleDto.MEMBER,
            savingsBalance = 0.0,
            loanStatus = LoanStatusDto.NONE,
        )
        assertNull(dto.photoUri)
    }

    @Test
    fun memberDto_roleAndLoanStatusDefaultToUnknownWhenOmitted() {
        val dto = MemberDto(
            id = "MBR-3",
            fineractClientId = 5003L,
            displayName = "New Member",
            savingsBalance = 0.0,
        )
        assertEquals(MemberRoleDto.UNKNOWN, dto.role)
        assertEquals(LoanStatusDto.UNKNOWN, dto.loanStatus)
    }

    @Test
    fun memberDto_equality() {
        val a = amaraDto.copy()
        val b = amaraDto.copy()
        assertEquals(a, b)
    }

    @Test
    fun memberDto_carriesSchemaVersion() {
        assertEquals(1, MemberDto.SCHEMA_VERSION)
    }

    @Test
    fun memberDto_serializationRoundTrips_wireFieldNamesAreCamelCase() {
        val encoded = json.encodeToString(MemberDto.serializer(), amaraDto)
        assertTrue(encoded.contains("\"id\""))
        assertTrue(encoded.contains("\"fineractClientId\""))
        assertTrue(encoded.contains("\"displayName\""))
        assertTrue(encoded.contains("\"photoUri\""))
        assertTrue(encoded.contains("\"role\""))
        assertTrue(encoded.contains("\"savingsBalance\""))
        assertTrue(encoded.contains("\"loanStatus\""))

        val decoded = json.decodeFromString(MemberDto.serializer(), encoded)
        assertEquals(amaraDto, decoded)
    }

    // ---------- MemberPageDto (offset-paginated envelope, page_size 20) ----------

    @Test
    fun memberPageDto_constructsWithTotalFilteredRecordsAndPageItems() {
        val page = MemberPageDto(totalFilteredRecords = 12, pageItems = listOf(amaraDto))
        assertEquals(12, page.totalFilteredRecords)
        assertEquals(listOf(amaraDto), page.pageItems)
    }

    @Test
    fun memberPageDto_pageItemsDefaultsToEmptyList() {
        val page = MemberPageDto(totalFilteredRecords = 0)
        assertTrue(page.pageItems.isEmpty())
    }

    @Test
    fun memberPageDto_serializationRoundTrips() {
        val page = MemberPageDto(totalFilteredRecords = 12, pageItems = listOf(amaraDto))
        val encoded = json.encodeToString(MemberPageDto.serializer(), page)
        assertTrue(encoded.contains("\"totalFilteredRecords\""))
        assertTrue(encoded.contains("\"pageItems\""))
        val decoded = json.decodeFromString(MemberPageDto.serializer(), encoded)
        assertEquals(page, decoded)
    }

    @Test
    fun memberPageDto_decodesFromGroupClientsShape() {
        // The literal `GET /groups/{groupId}/clients` response envelope (companion Member DTO
        // shape per this feature's own approved api.yaml#dtos.Member).
        val payload = """
            {
              "totalFilteredRecords": 2,
              "pageItems": [
                {"id":"MBR-1","fineractClientId":5001,"displayName":"Amara Otieno","photoUri":"https://cdn.example.org/photos/5001.jpg","role":"CHAIRPERSON","savingsBalance":1250.50,"loanStatus":"ACTIVE"},
                {"id":"MBR-2","fineractClientId":5002,"displayName":"Juma Kamau","role":"MEMBER","savingsBalance":0.0,"loanStatus":"NONE"}
              ]
            }
        """.trimIndent()
        val decoded = json.decodeFromString(MemberPageDto.serializer(), payload)
        assertEquals(2, decoded.totalFilteredRecords)
        assertEquals(2, decoded.pageItems.size)
        assertNull(decoded.pageItems[1].photoUri)
        assertEquals(MemberRoleDto.MEMBER, decoded.pageItems[1].role)
    }

    // ---------- MemberRoleDto (4 known + UNKNOWN, T7/EC30 fallback) ----------

    @Test
    fun memberRoleDto_hasExactlyFiveEntriesIncludingUnknownFallback() {
        assertEquals(5, MemberRoleDto.entries.size)
        assertTrue(MemberRoleDto.entries.contains(MemberRoleDto.UNKNOWN))
    }

    @Test
    fun memberRoleDto_decodesEachKnownWireValue() {
        val known = listOf(
            "CHAIRPERSON" to MemberRoleDto.CHAIRPERSON,
            "TREASURER" to MemberRoleDto.TREASURER,
            "SECRETARY" to MemberRoleDto.SECRETARY,
            "MEMBER" to MemberRoleDto.MEMBER,
        )
        known.forEach { (wire, expected) ->
            assertEquals(expected, json.decodeFromString(MemberRoleDto.serializer(), "\"$wire\""))
        }
    }

    @Test
    fun memberRoleDto_unknownServerValueCoercesToUnknownFallback_notCrash() {
        val payload = """
            {"id":"MBR-9","fineractClientId":9999,"displayName":"New","role":"ORGANIZER","savingsBalance":0.0,"loanStatus":"NONE"}
        """.trimIndent()
        val decoded = json.decodeFromString(MemberDto.serializer(), payload)
        assertEquals(MemberRoleDto.UNKNOWN, decoded.role)
    }

    // ---------- LoanStatusDto (3 known + UNKNOWN, T7/EC30 fallback) ----------

    @Test
    fun loanStatusDto_hasExactlyFourEntriesIncludingUnknownFallback() {
        assertEquals(4, LoanStatusDto.entries.size)
        assertTrue(LoanStatusDto.entries.contains(LoanStatusDto.UNKNOWN))
    }

    @Test
    fun loanStatusDto_decodesEachKnownWireValue() {
        assertEquals(LoanStatusDto.ACTIVE, json.decodeFromString(LoanStatusDto.serializer(), "\"ACTIVE\""))
        assertEquals(LoanStatusDto.NONE, json.decodeFromString(LoanStatusDto.serializer(), "\"NONE\""))
        assertEquals(LoanStatusDto.OVERDUE, json.decodeFromString(LoanStatusDto.serializer(), "\"OVERDUE\""))
    }

    @Test
    fun loanStatusDto_unknownServerValueCoercesToUnknownFallback_notCrash() {
        val payload = """
            {"id":"MBR-9","fineractClientId":9999,"displayName":"New","role":"MEMBER","savingsBalance":0.0,"loanStatus":"WRITTEN_OFF"}
        """.trimIndent()
        val decoded = json.decodeFromString(MemberDto.serializer(), payload)
        assertEquals(LoanStatusDto.UNKNOWN, decoded.loanStatus)
    }

    // ---------- Cross-version safety fixture (T7/EC30) ----------

    @Test
    fun memberDto_toleratesServerAddedFieldAndUnknownEnumValues_oldClientNeverCrashes() {
        // Simulates a staggered rollout: the server has shipped a NEW top-level field
        // (`kycVerified`) plus a NEW role value this (old) client schema does not know about.
        // Decoding MUST succeed, never throw.
        val serverPayload = """
            {
              "id":"MBR-20260509-001",
              "fineractClientId":5001,
              "displayName":"Amara Otieno",
              "photoUri":"https://cdn.example.org/photos/5001.jpg",
              "role":"AUDITOR",
              "savingsBalance":1250.50,
              "loanStatus":"ACTIVE",
              "kycVerified":true
            }
        """.trimIndent()
        val decoded = json.decodeFromString(MemberDto.serializer(), serverPayload)
        assertEquals("MBR-20260509-001", decoded.id)
        assertEquals(MemberRoleDto.UNKNOWN, decoded.role)
    }
}
