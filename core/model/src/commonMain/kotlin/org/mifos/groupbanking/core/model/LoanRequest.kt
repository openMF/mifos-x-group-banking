/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.model

/**
 * Domain model for the loan-request form's submission input — the member-side loan-application
 * form for `submit_loan_request` (`POST /datatables/dt_loan_request`). Reuses the SHARED
 * [LoanPurpose] (extended by this feature — see that type's kdoc in `LoanApply.kt`) rather than
 * forking a second purpose enum.
 *
 * [submittedAt]/[status] are deliberately EXCLUDED — same "wire-only field excluded from the
 * pure domain model" precedent as `RecordRepaymentRequest.transactionDate` (`RecordRepayment.kt`):
 * [submittedAt] is caller-supplied at the mapper boundary via `kotlin.time.Clock` (see
 * `LoanRequestMappers.kt#toDto`), and [status] always starts at the wire's literal `"PENDING"`
 * default (`LoanRequestPayloadDto.status`), never a member-entered value.
 *
 * See API.md#models — LoanRequestPayload.
 */
data class LoanRequestPayload(
    val clientId: Long,
    val requestedAmount: Double,
    val purpose: LoanPurpose,
    val durationWeeks: Int,
    val savingsBalanceAtRequest: Double,
)

/**
 * Domain result of a successful `submit_loan_request` submission — the literal Fineract
 * datatable resource-create envelope, mirroring `LoanRequestResponseDto` 1:1.
 *
 * See API.md#models — LoanRequestResult.
 */
data class LoanRequestResult(
    val resourceId: Long,
    val officeId: Long,
    val clientId: Long,
    val resourceExternalId: String,
)
