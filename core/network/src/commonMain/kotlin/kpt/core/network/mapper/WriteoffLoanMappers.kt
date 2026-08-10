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
import kpt.core.model.WriteoffResult
import kpt.core.network.model.WriteoffLoanRequestDto
import kpt.core.network.model.WriteoffLoanResponseDto

/**
 * Domain <-> DTO mappers for the loan-mark-defaulted-dialog wire contract
 * (`POST /loans/{loanId}/transactions?command=writeoff`). Every field on
 * every DTO declared in `WriteoffLoanDto.kt` is mapped — no field left
 * unmapped. Reuses [fineractTransactionDate] (`RecordRepaymentMappers.kt`)
 * for the caller-side wire-date string — no second `kotlin.time.Clock`
 * helper is defined here.
 */

// ---------- domain -> request DTO ----------

/**
 * Domain -> DTO. [WriteoffLoanRequest] carries zero domain-meaningful
 * fields (see its kdoc), so [transactionDate] is caller-supplied (repository
 * layer, typically [fineractTransactionDate]) same as
 * `RecordRepaymentRequest.toDto`'s [transactionDate] parameter
 * (`RecordRepaymentMappers.kt`).
 */
fun WriteoffLoanRequest.toDto(
    transactionDate: String,
    locale: String = "en",
    dateFormat: String = "dd MMMM yyyy",
): WriteoffLoanRequestDto = WriteoffLoanRequestDto(
    transactionDate = transactionDate,
    locale = locale,
    dateFormat = dateFormat,
)

// ---------- response DTO -> domain ----------

fun WriteoffLoanResponseDto.toDomainModel(): WriteoffResult = WriteoffResult(
    officeId = officeId,
    clientId = clientId,
    loanId = loanId,
    resourceId = resourceId,
)
