/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.mapper

import org.mifos.groupbanking.core.model.ChangePinRequest
import org.mifos.groupbanking.core.network.model.ChangePinRequestDto
import org.mifos.groupbanking.core.network.model.ChangePinResponseDto
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * TDD RED-first coverage for the change-pin domain<->DTO mappers. Every field on every DTO
 * declared in `ChangePinDto.kt` is exercised here (RULE: all-fields-mapped) — `ChangePinRequestDto`
 * has exactly 2 fields (`password`, `repeatPassword`), both mapped from [ChangePinRequest.newPin];
 * `ChangePinResponseDto` has exactly 1 field (`resourceId`), mapped to `ChangePinResult`.
 */
class ChangePinMappersTest {

    private val request = ChangePinRequest(
        currentPin = "1234",
        newPin = "4821",
    )

    // ---------- ChangePinRequest -> ChangePinRequestDto ----------

    @Test
    fun changePinRequest_toDto_mapsNewPinToPasswordField() {
        val dto = request.toDto()
        assertEquals("4821", dto.password)
    }

    @Test
    fun changePinRequest_toDto_mapsNewPinToRepeatPasswordField() {
        val dto = request.toDto()
        assertEquals("4821", dto.repeatPassword)
    }

    @Test
    fun changePinRequest_toDto_passwordAndRepeatPasswordAlwaysMatch() {
        // Fineract requires the confirmation pair to be identical — the member enters newPin
        // once, so both wire fields always resolve to the SAME value by construction.
        val dto = request.toDto()
        assertEquals(dto.password, dto.repeatPassword)
    }

    @Test
    fun changePinRequest_toDto_currentPinDoesNotLeakIntoDtoFields() {
        // currentPin authenticates via BasicAuth at the service boundary (api.yaml `auth:
        // BasicAuth`), never the JSON body — assert neither wire field ever carries it.
        val dto = request.toDto()
        assertEquals(false, dto.password == request.currentPin)
        assertEquals(false, dto.repeatPassword == request.currentPin)
    }

    // ---------- ChangePinResponseDto -> ChangePinResult ----------

    @Test
    fun changePinResponseDto_toDomainModel_mapsAllFields() {
        val dto = ChangePinResponseDto(resourceId = 42L)
        val domain = dto.toDomainModel()

        assertEquals(42L, domain.resourceId)
    }

    @Test
    fun changePinRequestDto_directConstruction_stillEqualsMapperOutput() {
        // Cross-checks the mapper's resolved DTO against a directly-constructed one — proves
        // toDto() doesn't silently diverge from the literal wire shape.
        val expected = ChangePinRequestDto(password = "4821", repeatPassword = "4821")
        assertEquals(expected, request.toDto())
    }
}
