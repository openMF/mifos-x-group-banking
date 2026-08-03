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
 * Domain marker for the confirm-to-writeoff mutation submitted via
 * `write_off_loan` (`POST /loans/{loanId}/transactions?command=writeoff`).
 * Carries zero domain-meaningful fields: `loanId` is a path parameter passed
 * separately to `LoanRepository.markDefaulted(loanId)`
 * (`idea-layer/screens/loan-mark-defaulted-dialog/ui.yaml#dependencies.repositories`),
 * and `transactionDate`/`locale`/`dateFormat` are Fineract API boilerplate
 * with no domain-model counterpart — the same "no domain field" precedent as
 * `RecordRepaymentRequest`'s excluded `transactionDate`/`locale`/`dateFormat`
 * (`RecordRepayment.kt`), taken here to its logical conclusion since this
 * confirmation dialog's `ui.yaml` declares no free-text note/reason input
 * component. Modeled as a `data object` (a documented, equatable singleton)
 * rather than an empty data class — Kotlin data classes require at least one
 * constructor parameter. See `WriteoffLoanMappers.kt` for the caller-side
 * wire-date helper reuse.
 *
 * See API.md#models — WriteoffLoanRequest.
 */
data object WriteoffLoanRequest

/**
 * Domain result of a successful `write_off_loan` submission — the literal
 * Fineract resource-create envelope, mirroring `WriteoffLoanResponseDto` 1:1.
 * Structurally identical to `RepaymentResult` (both are Fineract
 * loan-transaction resource-create envelopes) but kept distinct per the
 * per-operation naming precedent already established by `RecordRepayment.kt`.
 *
 * See API.md#models — WriteoffResult.
 */
data class WriteoffResult(
    val officeId: Int,
    val clientId: Long,
    val loanId: Long,
    val resourceId: Long,
)
