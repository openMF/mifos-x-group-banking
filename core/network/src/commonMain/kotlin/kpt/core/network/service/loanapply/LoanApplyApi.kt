/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.network.service.loanapply

import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.network.model.ApplyLoanRequestDto
import kpt.core.network.model.ApplyLoanResponseDto
import kpt.core.network.model.GroupCorpusRowDto
import kpt.core.network.model.GroupLoanConfigDto
import kpt.core.network.model.GroupMembersResponseDto
import kpt.core.network.model.LoanApplyTemplateDto
import kpt.core.network.model.LoanProductDto
import kpt.core.network.model.MemberSavingsResponseDto

/**
 * Ktor client for the loan-apply feature — the loan-application form's 7 endpoints
 * (`idea-layer/screens/loan-apply/api.yaml#api`). Returns [NetworkResult] — never a raw [Result]
 * envelope, never a thrown exception (Mandatory Rule 2). `NetworkResult`/`NetworkError` are the
 * framework's `core-base/network` sealed types (consumed, never edited — Hard Rule #8). This
 * Service is SERVICE-ONLY; the parallel-combine into the composite [LoanApplyTemplateDto] domain
 * model and the submit orchestration live in `LoanApplyRepository` (`core/data`).
 *
 * See API.md#services — LoanApplyApi.
 */
interface LoanApplyApi {

    /**
     * `GET /groups/{groupId}?associations=clientMembers` (`api.yaml#api[get_group_members]`).
     * Fetches every group member for the loan-apply member selector dropdown. Cached
     * `cache-first`, `ttl=3600` by the caller (`api.yaml#api[get_group_members].cache`) — this
     * Service call itself is unconditional, always hitting the network. 404 ->
     * [NetworkError.NOT_FOUND] ("group not found").
     */
    suspend fun getGroupMembers(groupId: Long): NetworkResult<GroupMembersResponseDto, NetworkError>

    /**
     * `GET /loanproducts` (`api.yaml#api[get_loan_products]`). Lists every loan product available
     * to the group, for the product dropdown. Cached `cache-first`, `ttl=3600` by the caller.
     */
    suspend fun getLoanProducts(): NetworkResult<List<LoanProductDto>, NetworkError>

    /**
     * `GET /loans/template` (`api.yaml#api[get_loan_template]`). Loads the pre-filled loan
     * defaults (`principal`/`numberOfRepayments`/`interestRatePerPeriod`/`interestType`/
     * `amortizationType`/`repaymentEvery`) for the selected [clientId] + [productId] pair — fired
     * once a member and product are both selected (`data-flow.yaml#entries[1,2]`, `on_member_selected`
     * / `on_product_selected`). [templateType] defaults to `"individual"` per `api.yaml`'s declared
     * param default. 404 -> [NetworkError.NOT_FOUND] ("client/product not found").
     */
    suspend fun getLoanTemplate(
        clientId: Long,
        productId: Long,
        templateType: String = "individual",
    ): NetworkResult<LoanApplyTemplateDto, NetworkError>

    /**
     * `GET /clients/{clientId}/accounts` (`api.yaml#api[get_member_savings]`). Fetches the
     * selected member's savings accounts — the eligibility input for the loan-multiplier
     * computation (`LoanApplyTemplate.memberSavingsBalance` = sum of `accountBalance` across every
     * row).
     */
    suspend fun getMemberSavings(clientId: Long): NetworkResult<MemberSavingsResponseDto, NetworkError>

    /**
     * `GET /datatables/dt_group_corpus/{groupId}` (`api.yaml#api[get_group_corpus]`). Fetches the
     * group's current fund balance — the eligibility input for the 10% corpus buffer check. 404 ->
     * [NetworkError.NOT_FOUND] ("group not found").
     */
    suspend fun getGroupCorpus(groupId: Long): NetworkResult<GroupCorpusRowDto, NetworkError>

    /**
     * `GET /datatables/dt_group_config/{groupId}` (`api.yaml#api[get_group_config]`). Fetches the
     * group's `loan_multiplier`/`max_loan_amount`/`meeting_frequency` policy row — the eligibility
     * ceiling inputs `LoanApplyTemplate.maxEligibleAmount` derives from. 404 ->
     * [NetworkError.NOT_FOUND] ("group not found").
     */
    suspend fun getGroupLoanConfig(groupId: Long): NetworkResult<GroupLoanConfigDto, NetworkError>

    /**
     * `POST /loans` (`api.yaml#api[post_loan]`, `create_new_loan`). Submits the loan application
     * [request] — the literal Fineract `create_new_loan` body (see `LoanApplyMappers.kt`'s
     * `ApplyLoanRequest.toDto` for how the domain request resolves into this wire shape). Fineract
     * creates the loan with status "submitted and pending approval". 400 ->
     * [NetworkError.BAD_REQUEST] (validation — e.g. amount exceeds eligibility); 403 falls into
     * the shared status-table's `else` branch -> [NetworkError.UNKNOWN] (insufficient role — no
     * dedicated `FORBIDDEN` entry in `core-base/network`'s `NetworkError`, same as every other
     * Service in this module).
     */
    suspend fun applyLoan(request: ApplyLoanRequestDto): NetworkResult<ApplyLoanResponseDto, NetworkError>
}
