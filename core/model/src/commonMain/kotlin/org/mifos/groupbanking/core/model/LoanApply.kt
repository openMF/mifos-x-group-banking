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
 * Domain model for a single group-member row on the loan-apply member selector dropdown (`GET
 * /groups/{groupId}?associations=clientMembers`).
 *
 * **Deliberately NOT the canonical member-list `Member`** (flagged for the cross-feature repair
 * station, same "forcing reuse would require fabricating values" precedent as `MemberProfile` vs
 * `Member`): `Member` requires non-null `role`/`savingsBalance`/`loanStatus` (none of which this
 * endpoint returns) and its `id` is a companion-bridge synthetic `String`, while this endpoint's
 * `id` is the raw Fineract numeric client id (`Long`). `GroupMember` was introduced instead,
 * matching `api.yaml#dtos.GroupMember` exactly. [fineractClientId] is a literal duplicate of [id]
 * — this endpoint has no separate companion-generated identifier (see `GroupMemberDto` kdoc).
 *
 * See API.md#models — GroupMember.
 */
data class GroupMember(
    val id: Long,
    val displayName: String,
    val imagePresent: Boolean,
    val fineractClientId: Long,
)

/**
 * Domain model for a single loan-product row (`GET /loanproducts`) offered to the group.
 *
 * See API.md#models — LoanProduct.
 */
data class LoanProduct(
    val id: Long,
    val name: String,
    val shortName: String,
    val principal: Double,
    val minPrincipal: Double,
    val maxPrincipal: Double,
    val numberOfRepayments: Int,
    val interestRatePerPeriod: Double,
)

/**
 * Domain model for the loan-apply form's full template — the available [products] catalogue, the
 * selected-product defaults ([principal]/[numberOfRepayments]/[interestRatePerPeriod]/
 * [interestType]/[amortizationType]/[repaymentEvery]), and the eligibility inputs
 * ([memberSavingsBalance]/[groupCorpusBalance]/[loanMultiplier]/[maxLoanAmount]) the loan-apply
 * screen combines to compute [maxEligibleAmount].
 *
 * NOT returned by a single endpoint — assembled by `LoanRepository`/`GroupRepository`/
 * `MemberRepository` from the 5 parallel reads this feature's `api.yaml` declares
 * (`get_loan_products`, `get_loan_template`, `get_member_savings`, `get_group_corpus`,
 * `get_group_config`), same "client-side composite, not a literal envelope" precedent as
 * `GroupDashboard`. [interestType]/[amortizationType] reuse the SHARED [MemberStatus] (`{id,
 * value}`, declared in `MemberProfile.kt`) rather than introducing a new lookup-pair type.
 *
 * See API.md#models — LoanApplyTemplate.
 */
data class LoanApplyTemplate(
    val products: List<LoanProduct>,
    val principal: Double,
    val numberOfRepayments: Int,
    val interestRatePerPeriod: Double,
    val interestType: MemberStatus,
    val amortizationType: MemberStatus,
    val repaymentEvery: Int,
    val memberSavingsBalance: Double,
    val groupCorpusBalance: Double,
    val loanMultiplier: Double,
    val maxLoanAmount: Double,
) {
    /**
     * Eligibility ceiling: the smaller of the savings-linked multiplier cap
     * ([memberSavingsBalance] `*` [loanMultiplier]) and the group's configured absolute
     * [maxLoanAmount] — the "loan multiplier for eligibility" computation this feature's
     * generation brief requests. Client-side derived, no wire counterpart (same "derived
     * property" precedent as `HealthIndicator.fromOverdueRate`).
     */
    val maxEligibleAmount: Double
        get() = minOf(memberSavingsBalance * loanMultiplier, maxLoanAmount)
}

/**
 * Domain model for the loan-apply submission input — the SIMPLIFIED client-facing shape
 * `api.yaml#dtos.LoanApplicationRequest` declares, deliberately NOT the literal Fineract
 * `create_new_loan` request body (same "domain simplified input, wire literal boilerplate"
 * precedent as `RecordRepaymentRequest`). [durationWeeks] doubles as both the loan-term frequency
 * (weeks) and the repayment count — this group-banking product line is exclusively
 * weekly-cadence (`repaymentEvery=1` week, matching every group's weekly meeting cycle), so a
 * single member-entered week count resolves both wire fields; see `LoanApplyMappers.kt`.
 *
 * See API.md#models — ApplyLoanRequest.
 */
data class ApplyLoanRequest(
    val memberId: Long,
    val productId: Long,
    val amount: Double,
    val durationWeeks: Int,
    val purpose: LoanPurpose,
    val groupId: Long,
)

/**
 * Domain result of a successful `create_new_loan` submission — the literal Fineract
 * resource-create envelope, mirroring `ApplyLoanResponseDto` 1:1.
 *
 * See API.md#models — LoanApplicationResult.
 */
data class LoanApplicationResult(
    val officeId: Long,
    val clientId: Long,
    val loanId: Long,
    val resourceId: Long,
)

/**
 * Domain enum for the loan-apply form's purpose selector (`api.yaml#dtos.LoanPurpose`, originally
 * 5 known values plus [UNKNOWN] fallback, mirroring wire `LoanPurposeDto` 1:1). [fineractPurposeId]
 * is the Fineract `loanPurposeId: Int` resolution `LoanApplyMappers.kt` reads to build the wire
 * submit body — sequential assignment in `api.yaml#dtos.LoanPurpose.values` declaration order
 * (confirmed gap: `api.yaml` declares no explicit per-value id mapping). [UNKNOWN] carries a
 * sentinel `0` — never actually submitted (the loan-apply purpose dropdown never offers an
 * "unknown" choice).
 *
 * **Extended by loan-request (PP-1 — registry/screen-SoT wins over the narrower hand-written
 * shape):** `idea-layer/screens/loan-request/ui.yaml#components.purpose_dropdown.options`
 * declares 7 purpose values — `SCHOOL_FEES`/`MEDICAL`/`BUSINESS`/`FARMING`/`HOME_IMPROVEMENT`/
 * `EMERGENCY`/`OTHER` — 4 of which (`MEDICAL`/`BUSINESS`/`EMERGENCY`/`OTHER`) already existed
 * here; [SCHOOL_FEES]/[FARMING]/[HOME_IMPROVEMENT] were ADDED (never forked into a second
 * purpose enum) with `fineractPurposeId` continuing the sequential assignment at `6`/`7`/`8`
 * (loan-request's wire body never actually reads `fineractPurposeId` — it transmits the enum's
 * bare name as a literal `String` — so these 3 ids are placeholders pending a real Fineract
 * `loanPurposeId` mapping, same confirmed-gap class as the original 1-5 assignment).
 * `EDUCATION` (loan-apply-only, not in loan-request's dropdown) is UNCHANGED.
 *
 * See API.md#models — LoanPurpose.
 */
enum class LoanPurpose(val fineractPurposeId: Int) {
    MEDICAL(1),
    EDUCATION(2),
    BUSINESS(3),
    EMERGENCY(4),
    OTHER(5),
    SCHOOL_FEES(6),
    FARMING(7),
    HOME_IMPROVEMENT(8),
    UNKNOWN(0),
}
