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
 * Wire request DTO for `write_off_loan` (`POST
 * /loans/{loanId}/transactions?command=writeoff`) — marks a loan as
 * defaulted by posting an irreversible Fineract write-off transaction.
 * `loanId` is a PATH parameter, not carried in this body (matches
 * `idea-layer/screens/loan-mark-defaulted-dialog/api.yaml#api[0].body`
 * verbatim). `locale`/`dateFormat` are Fineract API boilerplate parameters
 * with no domain-model counterpart — same "no domain field" precedent as
 * `RecordRepaymentRequestDto.locale`/`.dateFormat` (`RecordRepaymentDto.kt`),
 * taken here to its logical conclusion: `transactionDate` is ALSO excluded
 * from the domain model (caller-supplied, see `WriteoffLoanMappers.kt`'s
 * reuse of `fineractTransactionDate()`), leaving `WriteoffLoanRequest`
 * (`core/model/WriteoffLoan.kt`) with zero domain-meaningful fields — this
 * confirmation dialog's `api.yaml#body` declares NO free-text note/reason
 * field (confirmed against `ui.yaml` — no such input component exists).
 *
 * No dedicated `idea-layer/dtos/{Dto}.yaml` registry entry exists for this
 * feature — `api.yaml` is the sole SoT (PP-1).
 *
 * See API.md#dtos — WriteoffLoanRequest.
 */
@Serializable
data class WriteoffLoanRequestDto(
    @SerialName("transactionDate") val transactionDate: String,
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
 * Wire response DTO for `write_off_loan` — the literal Fineract
 * resource-create envelope (`officeId`/`clientId`/`loanId`/`resourceId`),
 * matching `api.yaml#api[0].response.fields` verbatim; identical shape to
 * `RecordRepaymentResponseDto` (both are Fineract loan-transaction
 * resource-create envelopes) but kept as a distinct type per-operation,
 * matching the established `RecordRepaymentResponseDto` precedent (no
 * cross-operation DTO sharing). See API.md#dtos — WriteoffLoanResponse.
 */
@Serializable
data class WriteoffLoanResponseDto(
    @SerialName("officeId") val officeId: Int,
    @SerialName("clientId") val clientId: Long,
    @SerialName("loanId") val loanId: Long,
    @SerialName("resourceId") val resourceId: Long,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}
