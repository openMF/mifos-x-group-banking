/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.data.repository

import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.model.GroupSavingsSummary
import kpt.core.model.IndividualSavingsSummary
import kpt.core.model.MemberSavingsBundle
import kpt.core.model.MemberSavingsDetail
import kpt.core.model.SavingsDashboardSummary
import kpt.core.model.SavingsLedgerEntry

/**
 * Shared savings repository — built ONCE for the 3 consumers that share Fineract savings shapes:
 * personal-savings (raw self-service ledger), member-savings-detail (companion per-member
 * statement + sparkline), and savings-dashboard (companion group/individual contribution
 * summaries). Wraps `SavingsApi` (`core/network`).
 *
 * **Store5 branch (SP-04):** `savings` has no `AppStoreRegistry` entry / `core/store/SavingsStore.kt`
 * yet — SP-03 `kmp-store-gen` has not run for these 3 features. Per this generation's explicit
 * brief this repository therefore surfaces [NetworkResult] directly rather than
 * `.asScreenStream()` (same branch as `LoanApplyRepositoryImpl`/`GroupCreateRepositoryImpl` today)
 * even though all 3 consumers' `data-flow.yaml#cache_strategy` declares `stale_while_revalidate`.
 * **Upgrade path**: once a future `kmp-store-gen` step emits a `SavingsStore` (or per-feature
 * stores) and registers the matching `AppStoreRegistry` entries, the read methods below should
 * upgrade to `savingsStore.asScreenStream(key)` — see `core/data/API.md`'s Store5 note.
 *
 * No try-catch anywhere in this repository (Mandatory Rule 4) — [SavingsRepositoryImpl] is a
 * plain `when`/`coroutineScope` chain over the service's sealed [NetworkResult]; `SavingsApiImpl`
 * is the sole layer allowed to catch exceptions.
 *
 * See API.md#repositories — SavingsRepository.
 */
interface SavingsRepository {

    /**
     * `GET /self/savingsaccounts/{savingsId}/transactions` — a single account's raw Fineract
     * self-service transaction ledger. Used STANDALONE by personal-savings for tab-switch
     * lazy-load (`OnTabSelected(INDIVIDUAL)`), pull-to-refresh, and error-retry — each of those
     * triggers re-fetches only the ACTIVE tab's `savingsId`, never both accounts (see
     * `data-flow.yaml#entries[on_refresh,OnRetry]`). For the initial-mount combined read of both
     * accounts, see [loadMemberSavings].
     */
    suspend fun getSavingsTransactions(
        savingsId: Long,
        limit: Int = 50,
        offset: Int = 0,
    ): NetworkResult<List<SavingsLedgerEntry>, NetworkError>

    /**
     * Composite convenience for personal-savings — fetches BOTH the mandatory group-linked
     * account's transactions and (when [individualSavingsId] is non-null) the optional voluntary
     * individual account's transactions, concurrently
     * (`kotlinx.coroutines.coroutineScope`+`async`, same parallel-combine shape as
     * `LoanApplyRepositoryImpl.loadTemplate`). [individualSavingsId] `== null` skips that second
     * read entirely — no network call is made for it, and
     * [MemberSavingsBundle.individualTransactions] is `null` (distinct from an empty
     * fetched-but-zero-rows list). The FIRST [NetworkResult.Error] encountered (declaration order:
     * group-linked, then individual) short-circuits the whole call; both in-flight reads are
     * still awaited (structured concurrency) before the function returns.
     *
     * @param groupLinkedSavingsId The member's mandatory group-linked savings account id.
     * @param individualSavingsId The member's optional voluntary individual savings account id,
     *   or `null` if they have none.
     */
    suspend fun loadMemberSavings(
        groupLinkedSavingsId: Long,
        individualSavingsId: Long? = null,
    ): NetworkResult<MemberSavingsBundle, NetworkError>

    /**
     * `GET /companion/groups/{groupId}/members/{memberId}/savings` — member-savings-detail's
     * single companion read (member identity + contribution-model-aware balance fields +
     * sparkline + paginated statement page). [limit]/[offset] page through the transaction
     * statement (`OnLoadMore` increments [offset] by `page_size=20`); [offset] `== 0` for the
     * initial mount / pull-to-refresh / retry (resets pagination per
     * `data-flow.yaml#entries[on_refresh,Retry]`).
     */
    suspend fun getMemberSavingsDetail(
        groupId: String,
        memberId: String,
        limit: Int = 20,
        offset: Int = 0,
    ): NetworkResult<MemberSavingsDetail, NetworkError>

    /**
     * `GET /companion/groups/{groupId}/savings` — savings-dashboard's group-tab summary,
     * standalone. Exposed for callers that only need the group tab; the screen's actual
     * on-mount/refresh/retry triggers always fetch both tabs together via [loadSavingsDashboard].
     */
    suspend fun getGroupSavingsSummary(groupId: String): NetworkResult<GroupSavingsSummary, NetworkError>

    /**
     * `GET /companion/groups/{groupId}/savings/individual` — savings-dashboard's individual-tab
     * summary, standalone. Exposed for callers that only need the individual tab; the screen's
     * actual on-mount/refresh/retry triggers always fetch both tabs together via
     * [loadSavingsDashboard]. `SelectTab` never calls this directly — both tabs' data is already
     * present from the initial parallel load (`data-flow.yaml#entries[SelectTab]`).
     */
    suspend fun getIndividualSavingsSummary(groupId: String): NetworkResult<IndividualSavingsSummary, NetworkError>

    /**
     * Composite for savings-dashboard's on-mount/refresh/retry triggers — fetches
     * [getGroupSavingsSummary] and [getIndividualSavingsSummary] concurrently
     * (`kotlinx.coroutines.coroutineScope`+`async`) per `data-flow.yaml#entries[0]`: "Both calls
     * run in parallel on mount." The FIRST [NetworkResult.Error] encountered (declaration order:
     * group, then individual) short-circuits the whole call; both in-flight reads are still
     * awaited (structured concurrency) before the function returns.
     */
    suspend fun loadSavingsDashboard(groupId: String): NetworkResult<SavingsDashboardSummary, NetworkError>
}
