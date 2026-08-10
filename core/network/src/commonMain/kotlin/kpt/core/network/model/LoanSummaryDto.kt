/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire DTO for a single row of the loan-list contract (`GET
 * /groups/{groupId}/loans`). Carries the member identity + photo, the loan
 * product name, principal/outstanding/overdue amounts, and the badge
 * [status] the loan-list card renders per row. This shape is the CANONICAL
 * wire `LoanSummary`, reused by loan-detail + loan dialogs + personal-loans
 * per `idea-layer/screens/loan-list/api.yaml#dtos.LoanSummary`.
 *
 * **Registry divergence (flagged for the cross-feature repair station, same
 * pattern as `MemberDto`'s / `GroupDto`'s documented divergences — see
 * `core/network/model/API.md`):** `idea-layer/dtos/LoanDto.yaml` (registry
 * v1.0.0) declares a DIFFERENT `LoanDto` shape (`principal: Double`,
 * `interestRate: Double`, `status: String` with LOWERCASE values
 * `pending`/`approved`/`disbursed`/`repaid`/`defaulted`/`rejected`/
 * `withdrawn`, `disbursedOn: String?`, `expectedMaturityDate: String?`,
 * `amountRepaid: Double?`, `amountOutstanding: Double?`,
 * `loanProductName: String?`) sourced from `GET /loans/{loanId}` +
 * `list_endpoint: GET /loans?groupId={groupId}`, and its `used_by` lists
 * `loan-management` + `end-user-dashboard` — NOT `loan-list`. `LoanSummaryDto`
 * here was generated from loan-list's OWN approved
 * `api.yaml#dtos.LoanSummary` instead (`GET /groups/{groupId}/loans`, a
 * DIFFERENT endpoint from the registry's), which explicitly declares the
 * flat `memberPhotoUrl`/`outstandingBalance`/`overdueAmount`/`isOverdue`/
 * `fineractLoanId` shape `ui.yaml#components.loan_card` binds to. Reconcile
 * the two `LoanDto`/`LoanSummaryDto` declarations at Station 3.
 *
 * See API.md#dtos — LoanSummary.
 */
@Serializable
data class LoanSummaryDto(
    @SerialName("id") val id: Long,
    @SerialName("memberId") val memberId: Long,
    @SerialName("memberName") val memberName: String,
    @SerialName("memberPhotoUrl") val memberPhotoUrl: String? = null,
    @SerialName("loanProductName") val loanProductName: String,
    @SerialName("principalAmount") val principalAmount: Double,
    @SerialName("outstandingBalance") val outstandingBalance: Double,
    @SerialName("overdueAmount") val overdueAmount: Double,
    @SerialName("status") val status: LoanAccountStatusDto = LoanAccountStatusDto.UNKNOWN,
    @SerialName("nextRepaymentDate") val nextRepaymentDate: String? = null,
    @SerialName("isOverdue") val isOverdue: Boolean,
    @SerialName("fineractLoanId") val fineractLoanId: Long,
) {
    companion object {
        /** Bumped when this DTO shape changes (registry `version:` driven). See EC30. */
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire envelope for the offset-paginated `GET /groups/{groupId}/loans`
 * response (`page_size=20`, stale-while-revalidate `ttl=180`, offline
 * show-cached). See API.md#dtos — LoanPage.
 */
@Serializable
data class LoanPageDto(
    @SerialName("totalFilteredRecords") val totalFilteredRecords: Int,
    @SerialName("pageItems") val pageItems: List<LoanSummaryDto> = emptyList(),
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire enum for a loan account's lifecycle status on the loan-list row
 * (loan-list's own `api.yaml#dtos.LoanStatus` — 5 known values). Named
 * `LoanAccountStatusDto` (NOT `LoanStatusDto`) to avoid a Kotlin symbol
 * collision with the EXISTING `LoanStatusDto` declared in `MemberDto.kt`
 * (member-list's member-level loan-status CHIP —
 * `ACTIVE`/`NONE`/`OVERDUE`/`UNKNOWN` — a different concept: one row per
 * MEMBER, not per LOAN). The two value-sets do not match either (this enum
 * has no `NONE`; member-list's has no `CLOSED`/`PENDING`/`REJECTED`), so
 * reuse was not possible per the "reuse only if the value-set matches"
 * constraint — flagged for Station 3 to evaluate whether the two loan-status
 * concepts should eventually be unified. [UNKNOWN] fallback per T7/EC30 so a
 * server-added status (e.g. `WRITTEN_OFF`) never crashes an old client.
 */
@Serializable
enum class LoanAccountStatusDto {
    @SerialName("ACTIVE")
    ACTIVE,

    @SerialName("OVERDUE")
    OVERDUE,

    @SerialName("CLOSED")
    CLOSED,

    @SerialName("PENDING")
    PENDING,

    @SerialName("REJECTED")
    REJECTED,

    @SerialName("UNKNOWN")
    UNKNOWN,
}
