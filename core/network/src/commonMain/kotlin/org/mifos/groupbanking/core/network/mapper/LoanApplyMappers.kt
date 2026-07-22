/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.mapper

import kotlinx.datetime.TimeZone
import org.mifos.groupbanking.core.model.ApplyLoanRequest
import org.mifos.groupbanking.core.model.GroupMember
import org.mifos.groupbanking.core.model.LoanApplicationResult
import org.mifos.groupbanking.core.model.LoanApplyTemplate
import org.mifos.groupbanking.core.model.LoanProduct
import org.mifos.groupbanking.core.model.LoanPurpose
import org.mifos.groupbanking.core.network.model.ApplyLoanRequestDto
import org.mifos.groupbanking.core.network.model.ApplyLoanResponseDto
import org.mifos.groupbanking.core.network.model.GroupCorpusRowDto
import org.mifos.groupbanking.core.network.model.GroupLoanConfigDto
import org.mifos.groupbanking.core.network.model.GroupMemberDto
import org.mifos.groupbanking.core.network.model.GroupMembersResponseDto
import org.mifos.groupbanking.core.network.model.LoanApplyTemplateDto
import org.mifos.groupbanking.core.network.model.LoanProductDto
import org.mifos.groupbanking.core.network.model.LoanPurposeDto
import org.mifos.groupbanking.core.network.model.MemberSavingsResponseDto
import kotlin.jvm.JvmName
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * DTO <-> domain mappers for the loan-apply wire contract (`get_group_members` +
 * `get_loan_products` + `get_loan_template` + `get_member_savings` + `get_group_corpus` +
 * `get_group_config` + `create_new_loan`). Every field on every DTO declared in
 * `LoanApplyDto.kt` is mapped — no field left unmapped.
 *
 * [org.mifos.groupbanking.core.network.model.FineractStatusDto.toDomainModel] is the SHARED
 * mapper already declared in `MemberProfileMappers.kt` — reused here (same package), not
 * redefined.
 */

// ---------- get_group_members ----------

fun GroupMemberDto.toDomainModel(): GroupMember = GroupMember(
    id = id,
    displayName = displayName,
    imagePresent = imagePresent,
    fineractClientId = id,
)

/** Batch converter — maps every group-member row in declaration order. */
@JvmName("groupMemberDtoListToDomainModels")
fun List<GroupMemberDto>.toDomainModels(): List<GroupMember> = map { it.toDomainModel() }

/** Envelope converter — unwraps `clientMembers` and maps every row. */
fun GroupMembersResponseDto.toDomainModels(): List<GroupMember> = clientMembers.toDomainModels()

// ---------- get_loan_products ----------

fun LoanProductDto.toDomainModel(): LoanProduct = LoanProduct(
    id = id,
    name = name,
    shortName = shortName,
    principal = principal,
    minPrincipal = minPrincipal,
    maxPrincipal = maxPrincipal,
    numberOfRepayments = numberOfRepayments,
    interestRatePerPeriod = interestRatePerPeriod,
)

/** Batch converter — maps every loan-product row in declaration order. */
@JvmName("loanProductDtoListToDomainModels")
fun List<LoanProductDto>.toDomainModels(): List<LoanProduct> = map { it.toDomainModel() }

// ---------- composite template (get_loan_template + get_member_savings + get_group_corpus + get_group_config) ----------

/**
 * Composes the loan-apply screen's full [LoanApplyTemplate] from the 5 parallel reads this
 * feature's `api.yaml` declares. [products] comes from `get_loan_products`; this receiver from
 * `get_loan_template`; [savings] from `get_member_savings` — [LoanApplyTemplate.memberSavingsBalance]
 * is the SUM of every `savingsAccounts[].accountBalance` (same "sum of balances" aggregation
 * precedent as `MemberAccountsDto.toDomainModel`); [corpus] from `get_group_corpus`; [config]
 * from `get_group_config`.
 */
fun LoanApplyTemplateDto.toDomainModel(
    products: List<LoanProductDto>,
    savings: MemberSavingsResponseDto,
    corpus: GroupCorpusRowDto,
    config: GroupLoanConfigDto,
): LoanApplyTemplate = LoanApplyTemplate(
    products = products.toDomainModels(),
    principal = principal,
    numberOfRepayments = numberOfRepayments,
    interestRatePerPeriod = interestRatePerPeriod,
    interestType = interestType.toDomainModel(),
    amortizationType = amortizationType.toDomainModel(),
    repaymentEvery = repaymentEvery,
    memberSavingsBalance = savings.savingsAccounts.sumOf { it.accountBalance },
    groupCorpusBalance = corpus.corpusBalance,
    loanMultiplier = config.loanMultiplier,
    maxLoanAmount = config.maxLoanAmount,
)

// ---------- create_new_loan ----------

/**
 * Domain -> wire request. Resolves the literal Fineract `create_new_loan` body from the
 * simplified domain [ApplyLoanRequest] plus the caller-supplied [product] (the
 * `interestRatePerPeriod` source — the domain request carries no rate field of its own).
 * [now] defaults via `kotlin.time.Clock` (NOT the deprecated `kotlinx.datetime.Clock`) and is
 * formatted through the SHARED `fineractTransactionDate` helper (`RecordRepaymentMappers.kt`),
 * reused rather than duplicated (same reuse precedent already established by
 * `WriteoffLoanMappers.kt`). `submittedOnDate`/`expectedDisbursementDate` both resolve to [now] —
 * the loan-apply `ui.yaml` collects no separate disbursement-date input, so same-day
 * disbursement is assumed (confirmed gap, same class as `RepaymentTransaction`'s no-wire-source
 * fields). `loanTermFrequency`/`numberOfRepayments` both resolve to
 * [ApplyLoanRequest.durationWeeks] — see `ApplyLoanRequest` kdoc for the weekly-cadence
 * rationale. The 5 lookup-pair fields + `repaymentEvery`/`transactionProcessingStrategyId` are
 * left at [ApplyLoanRequestDto]'s own declared defaults (the literal `api.yaml` constants).
 */
@OptIn(ExperimentalTime::class)
fun ApplyLoanRequest.toDto(
    product: LoanProduct,
    now: Instant = Clock.System.now(),
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): ApplyLoanRequestDto {
    val fineractDate = fineractTransactionDate(now, timeZone)
    return ApplyLoanRequestDto(
        clientId = memberId,
        productId = productId,
        principal = amount,
        loanTermFrequency = durationWeeks,
        numberOfRepayments = durationWeeks,
        interestRatePerPeriod = product.interestRatePerPeriod,
        expectedDisbursementDate = fineractDate,
        submittedOnDate = fineractDate,
        loanPurposeId = purpose.fineractPurposeId,
    )
}

fun ApplyLoanResponseDto.toDomainModel(): LoanApplicationResult = LoanApplicationResult(
    officeId = officeId,
    clientId = clientId,
    loanId = loanId,
    resourceId = resourceId,
)

// ---------- LoanPurpose <-> LoanPurposeDto ----------

fun LoanPurposeDto.toDomainModel(): LoanPurpose = when (this) {
    LoanPurposeDto.MEDICAL -> LoanPurpose.MEDICAL
    LoanPurposeDto.EDUCATION -> LoanPurpose.EDUCATION
    LoanPurposeDto.BUSINESS -> LoanPurpose.BUSINESS
    LoanPurposeDto.EMERGENCY -> LoanPurpose.EMERGENCY
    LoanPurposeDto.OTHER -> LoanPurpose.OTHER
    LoanPurposeDto.UNKNOWN -> LoanPurpose.UNKNOWN
}

fun LoanPurpose.toDto(): LoanPurposeDto = when (this) {
    LoanPurpose.MEDICAL -> LoanPurposeDto.MEDICAL
    LoanPurpose.EDUCATION -> LoanPurposeDto.EDUCATION
    LoanPurpose.BUSINESS -> LoanPurposeDto.BUSINESS
    LoanPurpose.EMERGENCY -> LoanPurposeDto.EMERGENCY
    LoanPurpose.OTHER -> LoanPurposeDto.OTHER
    LoanPurpose.UNKNOWN -> LoanPurposeDto.UNKNOWN
}
