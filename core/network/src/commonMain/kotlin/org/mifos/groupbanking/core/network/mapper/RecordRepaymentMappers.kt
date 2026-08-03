/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.mapper

import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.mifos.groupbanking.core.model.RecordRepaymentRequest
import org.mifos.groupbanking.core.model.RepaymentResult
import org.mifos.groupbanking.core.network.model.RecordRepaymentRequestDto
import org.mifos.groupbanking.core.network.model.RecordRepaymentResponseDto
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Domain <-> DTO mappers for the loan-repayment-dialog wire contract (`POST
 * /loans/{loanId}/transactions?command=repayment`). Every field on every DTO
 * declared in `RecordRepaymentDto.kt` is mapped — no field left unmapped.
 */

// ---------- domain -> request DTO ----------

/**
 * Domain -> DTO. [transactionDate] is caller-supplied (repository layer,
 * typically [fineractTransactionDate]) rather than threaded through
 * [RecordRepaymentRequest] — same "wire-only field excluded from the pure
 * domain model" precedent as `CreateMemberRequestDto.locale`/`.dateFormat`
 * (`MemberAddMappers.kt`). Blank [RecordRepaymentRequest.referenceNumber]
 * (the `LoanRepaymentDialogState.referenceNumber` field's `""` default when
 * the treasurer leaves it empty) is normalized to `null` — matches
 * `receiptNumber`'s optional `api.yaml#api[0].body.receiptNumber` contract.
 */
fun RecordRepaymentRequest.toDto(
    transactionDate: String,
    locale: String = "en",
    dateFormat: String = "dd MMMM yyyy",
): RecordRepaymentRequestDto = RecordRepaymentRequestDto(
    transactionDate = transactionDate,
    transactionAmount = amount,
    paymentTypeId = paymentMethod.paymentTypeId,
    receiptNumber = referenceNumber?.ifBlank { null },
    locale = locale,
    dateFormat = dateFormat,
)

// ---------- response DTO -> domain ----------

fun RecordRepaymentResponseDto.toDomainModel(): RepaymentResult = RepaymentResult(
    officeId = officeId,
    clientId = clientId,
    loanId = loanId,
    resourceId = resourceId,
)

// ---------- wire-date helper ----------

private val FINERACT_MONTH_NAMES = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)

/**
 * Formats [now] as Fineract's expected `dd MMMM yyyy` transaction-date string
 * (e.g. `"21 July 2026"`), matching `api.yaml#api[0].body.transactionDate`'s
 * note ("Today's date formatted as dd MMMM yyyy"). Defaults [now] to the
 * current instant via `kotlin.time.Clock` — `kotlinx.datetime.Clock` is
 * deprecated. [now]/[timeZone] are parameterized (not hardcoded to
 * `Clock.System.now()`) so callers — including this file's own tests — can
 * pin a deterministic instant.
 */
@OptIn(ExperimentalTime::class)
fun fineractTransactionDate(
    now: Instant = Clock.System.now(),
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): String {
    val dateTime = now.toLocalDateTime(timeZone)
    val day = dateTime.day.toString().padStart(2, '0')
    val month = FINERACT_MONTH_NAMES[dateTime.month.number - 1]
    return "$day $month ${dateTime.year}"
}
