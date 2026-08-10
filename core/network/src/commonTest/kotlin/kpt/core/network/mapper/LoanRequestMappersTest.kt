/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.network.mapper

import kpt.core.model.LoanPurpose
import kpt.core.model.LoanRequestPayload
import kpt.core.network.model.LoanPurposeDto
import kpt.core.network.model.LoanRequestResponseDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * TDD RED-first coverage for the loan-request domain<->DTO mappers. Every field on every DTO
 * declared in `LoanRequestDto.kt` is exercised here (RULE: all-fields-mapped).
 */
class LoanRequestMappersTest {

    @OptIn(ExperimentalTime::class)
    private val pinnedNow = Instant.parse("2026-07-21T09:00:00Z")

    private val payload = LoanRequestPayload(
        clientId = 5001L,
        requestedAmount = 15000.0,
        purpose = LoanPurpose.SCHOOL_FEES,
        durationWeeks = 16,
        savingsBalanceAtRequest = 6000.0,
    )

    // ---------- LoanRequestPayload -> LoanRequestPayloadDto ----------

    @OptIn(ExperimentalTime::class)
    @Test
    fun loanRequestPayload_toDto_mapsAllFields() {
        val dto = payload.toDto(now = pinnedNow)

        assertEquals(5001L, dto.clientId)
        assertEquals(15000.0, dto.requestedAmount)
        assertEquals(LoanPurposeDto.SCHOOL_FEES, dto.purpose)
        assertEquals(16, dto.durationWeeks)
        assertEquals(6000.0, dto.savingsBalanceAtRequest)
        assertEquals("2026-07-21T09:00:00Z", dto.submittedAt)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun loanRequestPayload_toDto_statusDefaultsToPending() {
        val dto = payload.toDto(now = pinnedNow)
        assertEquals("PENDING", dto.status)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun loanRequestPayload_toDto_defaultsNowViaKotlinTimeClock() {
        // No explicit `now` supplied — resolves via kotlin.time.Clock.System.now() and still
        // produces a valid ISO-8601 string (format, not value, is asserted — value is
        // non-deterministic without a pinned clock).
        val dto = payload.toDto()
        assertTrue(dto.submittedAt.contains("T"))
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun loanRequestPayload_toDto_reusesLoanPurposeToDtoMapper() {
        // LoanPurpose.toDto() is already declared in LoanApplyMappers.kt — not redefined here.
        val dto = payload.copy(purpose = LoanPurpose.EMERGENCY).toDto(now = pinnedNow)
        assertEquals(LoanPurposeDto.EMERGENCY, dto.purpose)
    }

    // ---------- LoanRequestResponseDto -> LoanRequestResult ----------

    @Test
    fun loanRequestResponseDto_toDomainModel_mapsAllFields() {
        val dto = LoanRequestResponseDto(
            resourceId = 7101L,
            officeId = 1L,
            clientId = 5001L,
            resourceExternalId = "LR-7101",
        )
        val domain = dto.toDomainModel()

        assertEquals(7101L, domain.resourceId)
        assertEquals(1L, domain.officeId)
        assertEquals(5001L, domain.clientId)
        assertEquals("LR-7101", domain.resourceExternalId)
    }

    // ---------- offline SyncQueue serialization helper ----------

    @OptIn(ExperimentalTime::class)
    @Test
    fun loanRequestPayloadDto_toJsonPayload_producesDecodableJsonString() {
        val dto = payload.toDto(now = pinnedNow)
        val json = dto.toJsonPayload()

        assertTrue(json.contains("\"clientId\""))
        assertTrue(json.contains("\"requested_amount\""))
        assertTrue(json.contains("\"purpose\":\"SCHOOL_FEES\""))

        val decoded = loanRequestPayloadDtoFromJson(json)
        assertEquals(dto, decoded)
    }
}
