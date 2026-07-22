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

import kotlinx.datetime.TimeZone
import org.mifos.groupbanking.core.model.PaymentMethod
import org.mifos.groupbanking.core.model.RecordRepaymentRequest
import org.mifos.groupbanking.core.network.model.RecordRepaymentResponseDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * TDD RED-first coverage for the loan-repayment-dialog DTO <-> domain
 * mappers. Asserts every field on `RecordRepaymentRequestDto` /
 * `RecordRepaymentResponseDto` is mapped — no field left unmapped.
 */
class RecordRepaymentMappersTest {

    // ---------- RecordRepaymentRequest -> RecordRepaymentRequestDto ----------

    @Test
    fun toDto_mapsEveryField() {
        val domain = RecordRepaymentRequest(
            amount = 125.0,
            paymentMethod = PaymentMethod.MPESA,
            referenceNumber = "QJZ7X9A1BK",
        )
        val dto = domain.toDto(transactionDate = "21 July 2026")
        assertEquals("21 July 2026", dto.transactionDate)
        assertEquals(125.0, dto.transactionAmount)
        assertEquals(1, dto.paymentTypeId)
        assertEquals("QJZ7X9A1BK", dto.receiptNumber)
        assertEquals("en", dto.locale)
        assertEquals("dd MMMM yyyy", dto.dateFormat)
    }

    @Test
    fun toDto_resolvesCashPaymentTypeId() {
        val domain = RecordRepaymentRequest(
            amount = 500.0,
            paymentMethod = PaymentMethod.CASH,
            referenceNumber = null,
        )
        val dto = domain.toDto(transactionDate = "21 July 2026")
        assertEquals(2, dto.paymentTypeId)
    }

    @Test
    fun toDto_blankReferenceNumberNormalizesToNullReceiptNumber() {
        val domain = RecordRepaymentRequest(
            amount = 500.0,
            paymentMethod = PaymentMethod.MPESA,
            referenceNumber = "",
        )
        val dto = domain.toDto(transactionDate = "21 July 2026")
        assertNull(dto.receiptNumber)
    }

    @Test
    fun toDto_allowsOverridingLocaleAndDateFormat() {
        val domain = RecordRepaymentRequest(
            amount = 500.0,
            paymentMethod = PaymentMethod.MPESA,
            referenceNumber = null,
        )
        val dto = domain.toDto(transactionDate = "21 July 2026", locale = "sw", dateFormat = "yyyy-MM-dd")
        assertEquals("sw", dto.locale)
        assertEquals("yyyy-MM-dd", dto.dateFormat)
    }

    // ---------- RecordRepaymentResponseDto -> RepaymentResult ----------

    @Test
    fun toDomainModel_mapsEveryField() {
        val dto = RecordRepaymentResponseDto(officeId = 1, clientId = 5001L, loanId = 9001L, resourceId = 30045L)
        val domain = dto.toDomainModel()
        assertEquals(1, domain.officeId)
        assertEquals(5001L, domain.clientId)
        assertEquals(9001L, domain.loanId)
        assertEquals(30045L, domain.resourceId)
    }

    // ---------- fineractTransactionDate wire-date helper (kotlin.time.Clock, not deprecated kotlinx.datetime.Clock) ----------

    @OptIn(ExperimentalTime::class)
    @Test
    fun fineractTransactionDate_formatsPinnedInstantAsDdMonthNameYyyy() {
        val pinned = Instant.parse("2026-07-21T10:00:00Z")
        val formatted = fineractTransactionDate(now = pinned, timeZone = TimeZone.UTC)
        assertEquals("21 July 2026", formatted)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun fineractTransactionDate_padsSingleDigitDay() {
        val pinned = Instant.parse("2026-03-05T00:00:00Z")
        val formatted = fineractTransactionDate(now = pinned, timeZone = TimeZone.UTC)
        assertEquals("05 March 2026", formatted)
    }
}
