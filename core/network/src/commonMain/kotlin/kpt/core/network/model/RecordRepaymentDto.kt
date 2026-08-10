/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package kpt.core.network.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire request DTO for `make_repayment` (`POST
 * /loans/{loanId}/transactions?command=repayment`) — records a treasurer-entered
 * repayment against a loan. `loanId` is a PATH parameter, not carried in this
 * body (matches `idea-layer/screens/loan-repayment-dialog/api.yaml#api[0].body`
 * verbatim). `locale`/`dateFormat` are Fineract API boilerplate parameters
 * with no domain-model counterpart, same "no domain field" precedent as
 * `CreateMemberRequestDto.locale`/`.dateFormat` (`MemberAddDto.kt`) — defaulted
 * here rather than threaded through the domain `RecordRepaymentRequest`.
 *
 * Registry note (flagged for the cross-feature repair station, per PP-1):
 * `idea-layer/dtos/LoanRepaymentDto.yaml` (registry v1.0.0, SAME endpoint
 * `POST /loans/{loanId}/transactions?command=repayment`, `used_by` explicitly
 * names "loan-repayment-dialog submits this") declares a DIFFERENT shape — a
 * post-hoc transaction-record view (`id`, `loanId`, `amount`, `date`, `type`,
 * `currency`, `principalPortion?`, `interestPortion?`, `outstandingAfter?`),
 * not the literal request/response body this feature's OWN `api.yaml#api[0]`
 * declares. This DTO instead mirrors the LITERAL `make_repayment` request body
 * per Hard Rule 5 (wire truth over the registry summary) — the same precedent
 * already established for `RepaymentTransactionDto` (`LoanDetailDto.kt`),
 * which faced an identical registry-shape mismatch against this SAME registry
 * entry. Reconcile at Station 3 — the registry's richer transaction-record
 * shape may fit `get_loan_hist` (loan-detail's repayment-history tab, already
 * modeled by `RepaymentTransactionDto`) better than the request/response pair
 * here.
 *
 * See API.md#dtos — RecordRepaymentRequest.
 */
@Serializable
data class RecordRepaymentRequestDto(
    @SerialName("transactionDate") val transactionDate: String,
    @SerialName("transactionAmount") val transactionAmount: Double,
    @SerialName("paymentTypeId") val paymentTypeId: Int,
    @SerialName("receiptNumber") val receiptNumber: String? = null,
    // Fineract requires locale + dateFormat on every date-bearing transaction body to parse
    // transactionDate; force-encode the defaults so they always reach the wire (kotlinx omits
    // default-valued fields otherwise — encodeDefaults is off in the production client config).
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    @SerialName("locale") val locale: String = "en",
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    @SerialName("dateFormat") val dateFormat: String = "dd MMMM yyyy",
) {
    companion object {
        /** Bumped when this DTO shape changes (registry `version:` driven). See EC30. */
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire response DTO for `make_repayment` — the literal Fineract
 * resource-create envelope (`officeId`/`clientId`/`loanId`/`resourceId`),
 * matching `api.yaml#api[0].response.fields` verbatim. See API.md#dtos —
 * RecordRepaymentResponse.
 */
@Serializable
data class RecordRepaymentResponseDto(
    @SerialName("officeId") val officeId: Int,
    @SerialName("clientId") val clientId: Long,
    @SerialName("loanId") val loanId: Long,
    @SerialName("resourceId") val resourceId: Long,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}
