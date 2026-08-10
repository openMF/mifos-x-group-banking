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

import kpt.core.model.WriteoffLoanRequest
import kpt.core.network.model.WriteoffLoanResponseDto
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * TDD RED-first coverage for the loan-mark-defaulted-dialog DTO <-> domain
 * mappers. Asserts every field on `WriteoffLoanRequestDto` /
 * `WriteoffLoanResponseDto` is mapped — no field left unmapped.
 */
class WriteoffLoanMappersTest {

    // ---------- WriteoffLoanRequest -> WriteoffLoanRequestDto ----------

    @Test
    fun toDto_mapsTransactionDate() {
        val dto = WriteoffLoanRequest.toDto(transactionDate = "21 July 2026")
        assertEquals("21 July 2026", dto.transactionDate)
    }

    @Test
    fun toDto_localeAndDateFormatDefaultToFineractBoilerplate() {
        val dto = WriteoffLoanRequest.toDto(transactionDate = "21 July 2026")
        assertEquals("en", dto.locale)
        assertEquals("dd MMMM yyyy", dto.dateFormat)
    }

    @Test
    fun toDto_allowsOverridingLocaleAndDateFormat() {
        val dto = WriteoffLoanRequest.toDto(
            transactionDate = "21 July 2026",
            locale = "sw",
            dateFormat = "yyyy-MM-dd",
        )
        assertEquals("sw", dto.locale)
        assertEquals("yyyy-MM-dd", dto.dateFormat)
    }

    // ---------- WriteoffLoanResponseDto -> WriteoffResult ----------

    @Test
    fun toDomainModel_mapsEveryField() {
        val dto = WriteoffLoanResponseDto(officeId = 1, clientId = 5001L, loanId = 9001L, resourceId = 40017L)
        val domain = dto.toDomainModel()
        assertEquals(1, domain.officeId)
        assertEquals(5001L, domain.clientId)
        assertEquals(9001L, domain.loanId)
        assertEquals(40017L, domain.resourceId)
    }
}
