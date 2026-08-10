/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.model

/**
 * Domain model for the treasurer-entered repayment submitted via
 * `make_repayment` (`POST /loans/{loanId}/transactions?command=repayment`) —
 * pure business shape, no wire concerns. `loanId` is NOT carried here — it is
 * a path parameter passed separately to `LoanRepository.recordRepayment`
 * (`idea-layer/screens/loan-repayment-dialog/ui.yaml#dependencies.repositories`).
 * `transactionDate`/`locale`/`dateFormat` are likewise excluded — Fineract API
 * boilerplate with no domain-model counterpart, same "no domain field"
 * precedent as `CreateMemberRequest`'s excluded `locale`/`dateFormat`
 * (`MemberAddMappers.kt`); see `RecordRepaymentMappers.kt` for the caller-side
 * wire-date helper.
 *
 * See API.md#models — RecordRepaymentRequest.
 */
data class RecordRepaymentRequest(
    val amount: Double,
    val paymentMethod: PaymentMethod,
    val referenceNumber: String?,
)

/**
 * Domain result of a successful `make_repayment` submission — the literal
 * Fineract resource-create envelope, mirroring `RecordRepaymentResponseDto`
 * 1:1.
 *
 * See API.md#models — RepaymentResult.
 */
data class RepaymentResult(
    val officeId: Int,
    val clientId: Long,
    val loanId: Long,
    val resourceId: Long,
)

/**
 * Domain enum for the payment-method chip selector
 * (`ui.yaml#components.payment_method_chips` — M-Pesa / Cash). Pure
 * client-side selection state — NEVER serialized to or from the wire (no
 * `@Serializable` counterpart in `core/network/model`; `api.yaml#api[0].body`
 * only ever transmits the already-resolved `paymentTypeId: Int`), so it lives
 * here even though `ui.yaml#dtos.PaymentMethod` declares its value-set. Same
 * "pure client-side state, no wire counterpart" precedent as `LoanStatusFilter`
 * (`LoanSummary.kt`) — no `UNKNOWN` fallback needed for the same reason
 * `LoanStatusFilter` has none: this is a fixed local chip set the treasurer
 * picks from, never a value decoded from a server payload. [paymentTypeId] is
 * the Fineract payment-type resolution `api.yaml#api[0].body.paymentTypeId`
 * documents (`MPESA=1, CASH=2`) — read by `RecordRepaymentMappers.kt` to build
 * the wire request body.
 *
 * See API.md#models — PaymentMethod.
 */
enum class PaymentMethod(val paymentTypeId: Int) {
    MPESA(1),
    CASH(2),
}
