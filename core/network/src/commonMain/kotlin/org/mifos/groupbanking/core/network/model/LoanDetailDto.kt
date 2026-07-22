/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire DTO for the loan-detail header (`GET /loans/{loanId}`) — product,
 * principal, interest, disbursement, outstanding/overdue balances, status,
 * and member identity for a single loan. Generated from loan-detail's own
 * approved `api.yaml#dtos.LoanDetail` (the flat, companion-normalized shape),
 * NOT the raw literal Fineract `api[0].response.fields` block on the same
 * `api.yaml` (which nests `summary`, `interestType`/`status` as `{id,value}`
 * pairs, and `disbursementDate` as a raw `List<Int>`) — same precedent as
 * `LoanSummaryDto` (loan-list): the companion bridge normalizes Fineract's
 * raw response before this client's Ktor service ever sees it.
 *
 * **Enum reuse:** [status] reuses the EXISTING `LoanAccountStatusDto`
 * (declared in `LoanSummaryDto.kt`, loan-list) rather than introducing a new
 * `LoanStatusDto`-adjacent enum — `api.yaml#dtos.LoanDetail.status` declares
 * the bare type `LoanStatus` with NO local value-set override, and this is
 * the SAME per-loan-lifecycle concept loan-list's `LoanStatus`
 * (`ACTIVE`/`OVERDUE`/`CLOSED`/`PENDING`/`REJECTED`) already models — no new
 * enum needed, no divergence to flag.
 *
 * **Registry divergence (flagged for the cross-feature repair station, same
 * class as the `LoanSummaryDto`/`idea-layer/dtos/LoanDto.yaml` divergence):**
 * `idea-layer/dtos/LoanDto.yaml` (registry v1.0.0, `source.endpoint: GET
 * /loans/{loanId}` — the SAME endpoint) declares a DIFFERENT `LoanDto` shape
 * (`principal: Double`, `interestRate: Double`, `status: String` lowercase
 * lifecycle values `pending`/`approved`/`disbursed`/`repaid`/`defaulted`/
 * `rejected`/`withdrawn`, `disbursedOn: String?`, `expectedMaturityDate:
 * String?`, `amountRepaid: Double?`, `amountOutstanding: Double?`), and lists
 * `loan-management` (`get_loan`) as its consumer, NOT `loan-detail`.
 * `LoanDetailDto` here was generated from loan-detail's OWN approved
 * `api.yaml#dtos.LoanDetail` instead. Reconcile at Station 3.
 *
 * See API.md#dtos — LoanDetail.
 */
@Serializable
data class LoanDetailDto(
    @SerialName("id") val id: Long,
    @SerialName("memberId") val memberId: Long,
    @SerialName("memberName") val memberName: String,
    @SerialName("loanProductName") val loanProductName: String,
    @SerialName("principalAmount") val principalAmount: Double,
    @SerialName("disbursedDate") val disbursedDate: String,
    @SerialName("interestRatePercent") val interestRatePercent: Double,
    @SerialName("totalOutstanding") val totalOutstanding: Double,
    @SerialName("totalOverdue") val totalOverdue: Double,
    @SerialName("status") val status: LoanAccountStatusDto = LoanAccountStatusDto.UNKNOWN,
    @SerialName("fineractLoanId") val fineractLoanId: Long,
) {
    companion object {
        /** Bumped when this DTO shape changes (registry `version:` driven). See EC30. */
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire DTO for a single row of the loan-detail repayment schedule
 * (`api.yaml#dtos.RepaymentScheduleRow`) — one installment period. See
 * API.md#dtos — RepaymentScheduleRow.
 */
@Serializable
data class RepaymentScheduleRowDto(
    @SerialName("weekNumber") val weekNumber: Int,
    @SerialName("dueDate") val dueDate: String,
    @SerialName("dueAmount") val dueAmount: Double,
    @SerialName("paidAmount") val paidAmount: Double,
    @SerialName("balance") val balance: Double,
    @SerialName("status") val status: RepaymentRowStatusDto = RepaymentRowStatusDto.UNKNOWN,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire enum for a single repayment-schedule row's paid/due state
 * (`api.yaml#dtos.RepaymentRowStatus` — 4 known values). `UNKNOWN` fallback
 * per T7/EC30 so a server-added value never crashes an old client.
 */
@Serializable(with = RepaymentRowStatusDto.Serializer::class)
enum class RepaymentRowStatusDto {
    @SerialName("PAID") PAID,
    @SerialName("PARTIAL") PARTIAL,
    @SerialName("UPCOMING") UPCOMING,
    @SerialName("OVERDUE") OVERDUE,
    @SerialName("UNKNOWN") UNKNOWN,
    ;

    internal object Serializer : KSerializer<RepaymentRowStatusDto> by unknownFallbackEnumSerializer(
        "RepaymentRowStatusDto", entries, UNKNOWN,
    )
}

/**
 * Wire DTO for a single repayment-history row (`api.yaml#dtos.RepaymentTransaction`)
 * — a posted transaction against the loan. [type] is left `String` (not an
 * enum) because `api.yaml` declares no local value-set for this field (unlike
 * `RepaymentScheduleRow.status`, which does) — Hard Rule 4 forbids inventing
 * an undeclared value-set. See API.md#dtos — RepaymentTransaction.
 */
@Serializable
data class RepaymentTransactionDto(
    @SerialName("id") val id: Long,
    @SerialName("type") val type: String,
    @SerialName("date") val date: String,
    @SerialName("amount") val amount: Double,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire envelope for the single `GET /loans/{loanId}?associations=repaymentSchedule,transactions`
 * response — bundles the loan header ([loan]) with its repayment schedule and
 * transaction history, matching the literal operation's single-payload shape
 * (`api.yaml#api[0].response.fields` returns `repaymentSchedule` +
 * `transactions` inline on the SAME object as the header fields). Not
 * literally named under `api.yaml#dtos` (same "inferred envelope" precedent
 * as loan-list's `LoanPageDto`, which also has no dedicated `dtos:` entry).
 * `transactions` keeps the raw operation's own field name (no
 * `dtos:`-declared alternative to prefer); the domain mapper renames it to
 * `repaymentHistory` for readability — see `LoanDetailMappers.kt`.
 *
 * See API.md#dtos — LoanDetailResponse.
 */
@Serializable
data class LoanDetailResponseDto(
    @SerialName("loan") val loan: LoanDetailDto,
    @SerialName("repaymentSchedule") val repaymentSchedule: List<RepaymentScheduleRowDto> = emptyList(),
    @SerialName("transactions") val transactions: List<RepaymentTransactionDto> = emptyList(),
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}
