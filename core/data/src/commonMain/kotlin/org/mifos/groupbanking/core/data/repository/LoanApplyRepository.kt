/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.data.repository

import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.model.ApplyLoanRequest
import org.mifos.groupbanking.core.model.GroupMember
import org.mifos.groupbanking.core.model.LoanApplicationResult
import org.mifos.groupbanking.core.model.LoanApplyTemplate
import org.mifos.groupbanking.core.model.LoanProduct

/**
 * Loan-apply form repository (`idea-layer/screens/loan-apply`). Wraps `LoanApplyApi`
 * (core/network).
 *
 * **Store5 branch (SP-04):** loan-apply's `business_logic.kind` is `composite`
 * (`ui.yaml#business_logic.kind`), but no `AppStoreRegistry` entry / `core/store/LoanApplyStore.kt`
 * exists yet for this feature — SP-03 `kmp-store-gen` has not run for loan-apply. Per this
 * generation's explicit brief this repository therefore surfaces [NetworkResult] directly rather
 * than `.asScreenStream()` (same branch as `MemberAddRepositoryImpl`/`GroupCreateRepositoryImpl`
 * today) — [loadTemplate] is a one-shot form-prefill combine, not a persistently cached
 * single-entity read. **Upgrade path**: once a future `kmp-store-gen` step emits a composite
 * `LoanApplyStore` (mirroring `GroupDashboardStore`/`MemberProfileStore`'s dynamic-key
 * `NETWORK_WITH_CACHE` pattern) and registers `AppStoreRegistry.LoanApply`, [loadTemplate]'s body
 * should upgrade to `loanApplyStore.asScreenStream(key)` — see `core/data/API.md`'s Store5 note.
 *
 * No try-catch anywhere in this repository (Mandatory Rule 4) — [LoanApplyRepositoryImpl] is a
 * plain `when`/`coroutineScope` chain over the service's sealed [NetworkResult]; `LoanApplyApiImpl`
 * is the sole layer allowed to catch exceptions.
 *
 * **Offline note**: `data-flow.yaml#entries[on_submit].error_paths` blocks the submit while
 * offline (risk policy forbids queued offline loan submissions) — this repository makes no
 * offline decision itself; the ViewModel, informed by `NetworkMonitor`, is responsible for
 * gating the [applyLoan] call.
 *
 * See API.md#repositories — LoanApplyRepository.
 */
interface LoanApplyRepository {

    /**
     * `GET /groups/{groupId}?associations=clientMembers` (`get_group_members`) — every group
     * member for the member selector dropdown. Independent of [loadTemplate]: `LoanApplyTemplate`
     * carries no member list (see `LoanApplyMappers.kt`'s composition KDoc — the 5-way combine is
     * `get_loan_products` + `get_loan_template` + `get_member_savings` + `get_group_corpus` +
     * `get_group_config`, NOT `get_group_members`), so this is a standalone read fired on-mount
     * alongside [loadTemplate]'s group-scoped inputs, per `data-flow.yaml#entries[0]`.
     */
    suspend fun getGroupMembers(groupId: Long): NetworkResult<List<GroupMember>, NetworkError>

    /**
     * Composes the loan-apply screen's full [LoanApplyTemplate] from the 5 parallel reads
     * `LoanApplyMappers.kt#LoanApplyTemplateDto.toDomainModel` requires: `get_loan_products`
     * ([groupId]-scoped product catalogue), `get_loan_template` ([clientId] + [productId]-scoped
     * pre-filled defaults, fired once BOTH a member and a product are selected per
     * `data-flow.yaml#entries[1,2]`), `get_member_savings` ([clientId]-scoped eligibility input),
     * `get_group_corpus` ([groupId]-scoped eligibility input), and `get_group_config`
     * ([groupId]-scoped eligibility policy). All 5 fire concurrently
     * (`kotlinx.coroutines.coroutineScope` + `async`); the FIRST [NetworkResult.Error] encountered
     * (declaration order: products, template, savings, corpus, config) short-circuits the whole
     * call — the remaining in-flight reads are still awaited (structured concurrency via
     * `coroutineScope`) but their results are discarded.
     */
    suspend fun loadTemplate(
        groupId: Long,
        clientId: Long,
        productId: Long,
    ): NetworkResult<LoanApplyTemplate, NetworkError>

    /**
     * `POST /loans` (`create_new_loan`) — submits [request], resolving the wire body's
     * `interestRatePerPeriod` from the caller-supplied [product] (the selected [LoanProduct] the
     * loan-apply screen already holds from [loadTemplate]'s `products` list — no redundant
     * `getLoanProducts` re-fetch at submit time; see `LoanApplyMappers.kt#ApplyLoanRequest.toDto`
     * for the full domain -> wire resolution, incl. `loanTermFrequency`/`numberOfRepayments`
     * both sourcing [ApplyLoanRequest.durationWeeks] and `submittedOnDate`/
     * `expectedDisbursementDate` both resolving to "now" via `kotlin.time.Clock`).
     */
    suspend fun applyLoan(
        request: ApplyLoanRequest,
        product: LoanProduct,
    ): NetworkResult<LoanApplicationResult, NetworkError>
}
