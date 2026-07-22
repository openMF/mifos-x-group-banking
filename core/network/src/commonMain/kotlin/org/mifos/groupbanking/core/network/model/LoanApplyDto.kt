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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire DTO for a single row of `get_group_members` (`GET
 * /groups/{groupId}?associations=clientMembers`) — a member row for the loan-apply member
 * selector dropdown. Carries only the 3 fields `clientMembers[]` literally returns per
 * `idea-layer/screens/loan-apply/api.yaml#api[0].response.fields`
 * (`id`/`displayName`/`imagePresent`).
 *
 * **`api.yaml#dtos.GroupMember` in-file divergence (flagged for the cross-feature repair
 * station):** the abbreviated `dtos.GroupMember` block additionally declares a
 * `fineractClientId: Long` field with NO separate wire source anywhere on this operation. This
 * endpoint is a raw Fineract `associations=clientMembers` projection (not a companion-bridge
 * response with a distinct synthetic identifier, unlike member-list's `MemberDto.id` vs
 * `.fineractClientId`) — `id` here already IS the Fineract numeric client id. The mapper derives
 * `GroupMember.fineractClientId = id` (a literal duplication of an already-present wire value,
 * not an invented field — see `LoanApplyMappers.kt`).
 *
 * See API.md#dtos — GroupMember.
 */
@Serializable
data class GroupMemberDto(
    @SerialName("id") val id: Long,
    @SerialName("displayName") val displayName: String,
    @SerialName("imagePresent") val imagePresent: Boolean,
) {
    companion object {
        /** Bumped when this DTO shape changes (registry `version:` driven). See EC30. */
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire envelope for `get_group_members` (`GET /groups/{groupId}?associations=clientMembers`,
 * cache `ttl=3600`, `cache-first`).
 *
 * See API.md#dtos — GroupMembersResponse.
 */
@Serializable
data class GroupMembersResponseDto(
    @SerialName("clientMembers") val clientMembers: List<GroupMemberDto> = emptyList(),
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire DTO for a single row of `get_loan_products` (`GET /loanproducts`, cache `ttl=3600`,
 * `cache-first`) — carries the FULL literal operation response (`id`/`name`/`shortName`/
 * `principal`/`minPrincipal`/`maxPrincipal`/`numberOfRepayments`/`interestRatePerPeriod`), not the
 * narrower abbreviated `api.yaml#dtos.LoanProduct` block (which omits `principal`/
 * `numberOfRepayments`) — same "literal operation response wins over the abbreviated dtos
 * summary" precedent as `OfficeDto`/`CreateGroupTypeConfigDto`.
 *
 * See API.md#dtos — LoanProduct.
 */
@Serializable
data class LoanProductDto(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("shortName") val shortName: String,
    @SerialName("principal") val principal: Double,
    @SerialName("minPrincipal") val minPrincipal: Double,
    @SerialName("maxPrincipal") val maxPrincipal: Double,
    @SerialName("numberOfRepayments") val numberOfRepayments: Int,
    @SerialName("interestRatePerPeriod") val interestRatePerPeriod: Double,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire DTO for `get_loan_template` (`GET /loans/template`) — pre-filled defaults for the
 * loan-apply form once a member + product are selected. [interestType]/[amortizationType] reuse
 * the SHARED `FineractStatusDto` (`{id, value}`, declared in `MemberProfileDto.kt`) rather than
 * introducing two new nested types — the exact same `{id: Int, value: String}` shape `api.yaml`
 * declares for both.
 *
 * See API.md#dtos — LoanApplyTemplate.
 */
@Serializable
data class LoanApplyTemplateDto(
    @SerialName("principal") val principal: Double,
    @SerialName("numberOfRepayments") val numberOfRepayments: Int,
    @SerialName("interestRatePerPeriod") val interestRatePerPeriod: Double,
    @SerialName("interestType") val interestType: FineractStatusDto,
    @SerialName("amortizationType") val amortizationType: FineractStatusDto,
    @SerialName("repaymentEvery") val repaymentEvery: Int,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire DTO for a single savings-account row of `get_member_savings` (`GET
 * /clients/{clientId}/accounts`) — an eligibility input for the loan-multiplier computation.
 * [status] carries only `value` (no `id`) per `api.yaml`'s declared
 * `savingsAccounts[].status: { value: String }` shape — distinct from the richer
 * `FineractStatusDto {id, value}` reused elsewhere in this module, so a value-only
 * [SavingsAccountStatusDto] was introduced instead of forcing `FineractStatusDto` reuse with a
 * fabricated `id` (Hard Rule 4).
 *
 * See API.md#dtos — MemberSavingsAccount (loan-apply).
 */
@Serializable
data class MemberSavingsAccountRowDto(
    @SerialName("id") val id: Long,
    @SerialName("accountBalance") val accountBalance: Double,
    @SerialName("status") val status: SavingsAccountStatusDto,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire DTO for the value-only status pair on [MemberSavingsAccountRowDto.status]. Distinct from
 * the shared `FineractStatusDto` (`{id, value}`) — this operation's declared shape carries no
 * `id`.
 *
 * See API.md#dtos — SavingsAccountStatus.
 */
@Serializable
data class SavingsAccountStatusDto(
    @SerialName("value") val value: String,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire envelope for `get_member_savings` (`GET /clients/{clientId}/accounts`) — loan-apply's
 * eligibility-input read (member savings balance for the loan-multiplier computation).
 *
 * See API.md#dtos — MemberSavingsResponse (loan-apply).
 */
@Serializable
data class MemberSavingsResponseDto(
    @SerialName("savingsAccounts") val savingsAccounts: List<MemberSavingsAccountRowDto> = emptyList(),
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire DTO for `get_group_corpus` (`GET /datatables/dt_group_corpus/{groupId}`) — the group's
 * current fund balance, an eligibility input. Raw datatable row, snake_case `@SerialName`s (Hard
 * Rule 5), matching `InvitationRowDto`/`CreateGroupTypeConfigDto`'s established precedent.
 *
 * **Distinct from the existing `GroupCorpusDto`** (`GroupDashboardDto.kt`, companion `GET
 * /companion/groups/{groupId}/corpus`, a much richer camelCase shape) — flagged for the
 * cross-feature repair station: both concepts are "this group's corpus balance", sourced from two
 * different endpoints with two different literal shapes. Named `GroupCorpusRowDto` here to avoid
 * the Kotlin class-name clash, same "avoid the clash, flag the collision" precedent as
 * `GroupInstanceConfigDto` vs `GroupTypeConfigDto`.
 *
 * See API.md#dtos — GroupCorpus (loan-apply).
 */
@Serializable
data class GroupCorpusRowDto(
    @SerialName("corpus_balance") val corpusBalance: Double,
    @SerialName("last_updated") val lastUpdated: String,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire DTO for `get_group_config` (`GET /datatables/dt_group_config/{groupId}`) — the group's
 * loan-eligibility policy (`loan_multiplier`, `max_loan_amount`) plus meeting cadence. Raw
 * datatable row, snake_case `@SerialName`s.
 *
 * **Distinct from the existing `GroupConfigDto`** (`GroupDashboardDto.kt`, a client-side
 * -constructed savings/loan rule set NOT returned by any endpoint, with `shareValue`/`shareMin`/
 * `fineAmount` etc.) — same bare-name-collision-avoidance precedent as [GroupCorpusRowDto] above;
 * named `GroupLoanConfigDto` here to avoid the clash, flagged for Station 3.
 *
 * See API.md#dtos — GroupConfig (loan-apply).
 */
@Serializable
data class GroupLoanConfigDto(
    @SerialName("loan_multiplier") val loanMultiplier: Double,
    @SerialName("max_loan_amount") val maxLoanAmount: Double,
    @SerialName("meeting_frequency") val meetingFrequency: String,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire request DTO for `create_new_loan` (`POST /loans`) — the LITERAL Fineract loan-application
 * body, matching `api.yaml#post_loan.body` verbatim. The 5 nested `{id, value}` lookup fields
 * reuse the SHARED `FineractStatusDto`, each defaulted to the literal constant `api.yaml`
 * declares (`loanTermFrequencyType`/`repaymentFrequencyType` = `Weeks`, `amortizationType` =
 * `Equal installments`, `interestType` = `Declining Balance`, `interestCalculationPeriodType` =
 * `Same as repayment period`); `repaymentEvery`/`transactionProcessingStrategyId` default to the
 * literal constants `1`/`1` `api.yaml` declares.
 *
 * Deliberately NOT the same shape as the domain `ApplyLoanRequest`
 * (`memberId`/`productId`/`amount`/`durationWeeks`/`purpose`/`groupId`, per
 * `api.yaml#dtos.LoanApplicationRequest`) — same "domain simplified input, wire literal Fineract
 * boilerplate" precedent as `RecordRepaymentRequest`/`RecordRepaymentRequestDto`. See
 * `LoanApplyMappers.kt` for the resolution logic (`interestRatePerPeriod` sourced from the
 * selected `LoanProduct`, `submittedOnDate`/`expectedDisbursementDate` via the SHARED
 * `fineractTransactionDate` helper reused from `RecordRepaymentMappers.kt`, `loanPurposeId`
 * sourced from `LoanPurpose.fineractPurposeId`).
 *
 * See API.md#dtos — ApplyLoanRequest.
 */
@Serializable
data class ApplyLoanRequestDto(
    @SerialName("clientId") val clientId: Long,
    @SerialName("productId") val productId: Long,
    @SerialName("principal") val principal: Double,
    @SerialName("loanTermFrequency") val loanTermFrequency: Int,
    @SerialName("loanTermFrequencyType")
    val loanTermFrequencyType: FineractStatusDto = FineractStatusDto(id = 1, value = "Weeks"),
    @SerialName("numberOfRepayments") val numberOfRepayments: Int,
    @SerialName("repaymentEvery") val repaymentEvery: Int = 1,
    @SerialName("repaymentFrequencyType")
    val repaymentFrequencyType: FineractStatusDto = FineractStatusDto(id = 1, value = "Weeks"),
    @SerialName("interestRatePerPeriod") val interestRatePerPeriod: Double,
    @SerialName("amortizationType")
    val amortizationType: FineractStatusDto = FineractStatusDto(id = 1, value = "Equal installments"),
    @SerialName("interestType")
    val interestType: FineractStatusDto = FineractStatusDto(id = 0, value = "Declining Balance"),
    @SerialName("interestCalculationPeriodType")
    val interestCalculationPeriodType: FineractStatusDto =
        FineractStatusDto(id = 1, value = "Same as repayment period"),
    @SerialName("transactionProcessingStrategyId") val transactionProcessingStrategyId: Int = 1,
    @SerialName("expectedDisbursementDate") val expectedDisbursementDate: String,
    @SerialName("submittedOnDate") val submittedOnDate: String,
    @SerialName("loanPurposeId") val loanPurposeId: Int,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire response DTO for `create_new_loan` — the literal Fineract resource-create envelope,
 * matching `api.yaml#post_loan.response.fields` verbatim.
 *
 * See API.md#dtos — ApplyLoanResponse.
 */
@Serializable
data class ApplyLoanResponseDto(
    @SerialName("officeId") val officeId: Long,
    @SerialName("clientId") val clientId: Long,
    @SerialName("loanId") val loanId: Long,
    @SerialName("resourceId") val resourceId: Long,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire enum for `api.yaml#dtos.LoanPurpose` (originally 5 known values + [UNKNOWN] fallback).
 *
 * **NOT literally transmitted on the wire for `create_new_loan`** — that body only ever carries
 * the already-resolved `loanPurposeId: Int` (see [ApplyLoanRequestDto.loanPurposeId]), same
 * "chip-selector resolved to an Int before it hits the wire" precedent as
 * `PaymentMethod`/`paymentTypeId`. Declared `@Serializable` with a full [UNKNOWN] fallback anyway
 * (per this generation brief's explicit request, and for forward-compatibility — a future
 * `GET`-loan-detail response echoing the purpose back would decode safely through this same
 * type). `LoanApplyMappers.kt` bridges this to/from the domain `LoanPurpose`.
 *
 * **IS literally transmitted for loan-request** — `idea-layer/screens/loan-request/api.yaml`'s
 * `LoanRequestPayloadDto.purpose` field carries this enum directly (kotlinx.serialization
 * encodes an enum by its `@SerialName` string, matching the wire `"purpose": String` contract
 * with no wrapper object needed). **Extended (PP-1 — screen-SoT wins):** loan-request's
 * `ui.yaml#components.purpose_dropdown.options` adds [SCHOOL_FEES]/[FARMING]/[HOME_IMPROVEMENT]
 * to the existing value-set — see `LoanRequestDto.kt` kdoc and the domain `LoanPurpose` kdoc
 * (`LoanApply.kt`) for the full rationale. `LoanApplyDtoTest.kt`'s unknown-fallback fixture was
 * updated to probe a genuinely-unmodeled value (no longer `HOME_IMPROVEMENT`, now real).
 *
 * See API.md#dtos — LoanPurpose.
 */
@Serializable
enum class LoanPurposeDto {
    @SerialName("MEDICAL") MEDICAL,
    @SerialName("EDUCATION") EDUCATION,
    @SerialName("BUSINESS") BUSINESS,
    @SerialName("EMERGENCY") EMERGENCY,
    @SerialName("OTHER") OTHER,
    @SerialName("SCHOOL_FEES") SCHOOL_FEES,
    @SerialName("FARMING") FARMING,
    @SerialName("HOME_IMPROVEMENT") HOME_IMPROVEMENT,
    @SerialName("UNKNOWN") UNKNOWN,
}
