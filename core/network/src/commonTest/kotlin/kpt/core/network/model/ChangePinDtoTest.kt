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
import kotlin.test.assertTrue

/**
 * TDD RED-first coverage for the change-pin wire contract — `PUT
 * /fineract-provider/api/v1/self/user/updatePassword` (`idea-layer/screens/settings/api.yaml#api[0]`,
 * `change_pin`). See API.md#dtos.
 */
class ChangePinDtoTest {

    // Server-parity Json config: mirrors NetworkModule's client config (ignoreUnknownKeys +
    // coerceInputValues) so these tests prove EC30 cross-version tolerance, not just happy-path
    // round-tripping.
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    // ---------- ChangePinRequestDto ----------

    private val requestDto = ChangePinRequestDto(
        password = "4821",
        repeatPassword = "4821",
    )

    @Test
    fun changePinRequestDto_constructsWithAllFields() {
        assertEquals("4821", requestDto.password)
        assertEquals("4821", requestDto.repeatPassword)
    }

    @Test
    fun changePinRequestDto_serializationRoundTrips_wireFieldNamesMatchApiYamlVerbatim() {
        val encoded = json.encodeToString(ChangePinRequestDto.serializer(), requestDto)
        assertTrue(encoded.contains("\"password\""))
        assertTrue(encoded.contains("\"repeatPassword\""))

        val decoded = json.decodeFromString(ChangePinRequestDto.serializer(), encoded)
        assertEquals(requestDto, decoded)
    }

    @Test
    fun changePinRequestDto_equality() {
        assertEquals(requestDto.copy(), requestDto.copy())
    }

    @Test
    fun changePinRequestDto_carriesSchemaVersion() {
        assertEquals(1, ChangePinRequestDto.SCHEMA_VERSION)
    }

    // ---------- ChangePinResponseDto ----------

    private val responseDto = ChangePinResponseDto(resourceId = 42L)

    @Test
    fun changePinResponseDto_constructsWithAllFields() {
        assertEquals(42L, responseDto.resourceId)
    }

    @Test
    fun changePinResponseDto_serializationRoundTrips() {
        val encoded = json.encodeToString(ChangePinResponseDto.serializer(), responseDto)
        assertTrue(encoded.contains("\"resourceId\""))

        val decoded = json.decodeFromString(ChangePinResponseDto.serializer(), encoded)
        assertEquals(responseDto, decoded)
    }

    @Test
    fun changePinResponseDto_equality() {
        assertEquals(responseDto.copy(), responseDto.copy())
    }

    @Test
    fun changePinResponseDto_carriesSchemaVersion() {
        assertEquals(1, ChangePinResponseDto.SCHEMA_VERSION)
    }

    // ---------- Cross-version safety fixture (T7/EC30) ----------

    @Test
    fun changePinResponseDto_toleratesServerAddedField_oldClientNeverCrashes() {
        // Simulates a staggered rollout: the server has shipped a NEW top-level column
        // (`changes`) this (old) client schema does not know about. Decoding MUST succeed,
        // never throw.
        val serverPayload = """
            {"resourceId":42,"changes":{"password":"encoded-elsewhere"}}
        """.trimIndent()
        val decoded = json.decodeFromString(ChangePinResponseDto.serializer(), serverPayload)
        assertEquals(42L, decoded.resourceId)
    }
}
