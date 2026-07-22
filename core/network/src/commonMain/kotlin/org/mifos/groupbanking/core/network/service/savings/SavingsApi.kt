/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.service.savings

import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.network.model.GroupSavingsSummaryDto
import org.mifos.groupbanking.core.network.model.IndividualSavingsSummaryDto
import org.mifos.groupbanking.core.network.model.MemberSavingsDetailDto
import org.mifos.groupbanking.core.network.model.SavingsLedgerEntryDto

/**
 * Ktor client for the shared savings client stack — built ONCE for the 3 consumers that share
 * Fineract savings shapes: personal-savings (raw self-service ledger), member-savings-detail
 * (companion per-member statement + sparkline), and savings-dashboard (companion group/individual
 * contribution summaries). See `idea-layer/screens/{personal-savings,member-savings-detail,
 * savings-dashboard}/api.yaml` + `core/network/API.md#services` for the endpoint contracts.
 *
 * Returns [NetworkResult] — never a raw [Result] envelope, never a thrown exception (Mandatory
 * Rule 2). `NetworkResult`/`NetworkError` are the framework's `core-base/network` sealed types
 * (consumed, never edited — Hard Rule #8). `SavingsRepositoryImpl` (`core/data`) is the sole
 * consumer — this Service is SERVICE-ONLY; no repository/store is emitted here. `savings` has no
 * `AppStoreRegistry` entry yet (SP-03 `kmp-store-gen` has not run for this feature set), so the
 * Store5-wrapping question is out of scope for this generation — see `core/data/API.md`'s
 * `SavingsRepository` row for the current Store5-free branch.
 *
 * See API.md#services — SavingsApi.
 */
interface SavingsApi {

    /**
     * `GET /self/savingsaccounts/{savingsId}/transactions`
     * (`api.yaml#api[get_group_linked_transactions,get_individual_transactions]`) — personal-savings'
     * raw Fineract self-service transaction ledger. BOTH `get_group_linked_transactions` and
     * `get_individual_transactions` share this exact endpoint template on `api.yaml`, differing
     * ONLY in which `savingsId` the caller threads in (the member's mandatory group-linked
     * account vs. their optional voluntary individual account, per `SavingsTab`) — this single
     * method is reused for both reads rather than authoring two near-identical methods, same
     * "reuse over duplicate near-identical endpoints" precedent as
     * [org.mifos.groupbanking.core.network.service.loanlist.LoanApi.getClientLoans].
     *
     * [limit]/[offset] default to `50`/`0` per `api.yaml#params` (offset-paginated). Response is
     * a top-level JSON array (`api.yaml#dtos.SavingsTransactionDto` list) — no envelope. 404 ->
     * [NetworkError.NOT_FOUND] ("account not found"); 503 -> [NetworkError.SERVER] ("server
     * unavailable").
     */
    suspend fun getSavingsTransactions(
        savingsId: Long,
        limit: Int = 50,
        offset: Int = 0,
    ): NetworkResult<List<SavingsLedgerEntryDto>, NetworkError>

    /**
     * `GET /companion/groups/{groupId}/members/{memberId}/savings`
     * (`api.yaml#api[get_member_savings_detail]`) — member-savings-detail's single companion read.
     * Replaces the old raw Fineract `GET /clients/{id}` + `GET /clients/{id}/accounts` pair.
     * Returns contribution-model-aware fields (`sharesHeld`/`shareValue` for SHARE_BASED_VARIABLE
     * groups; `savingsBalance` alone for FIXED_AMOUNT groups) plus a balance sparkline and the
     * FIRST page of the paginated transaction statement.
     *
     * [limit]/[offset] default to `20`/`0` per `api.yaml#params` (offset-paginated,
     * `pagination.page_size=20`) — the `OnLoadMore` action (`core/data`'s `SavingsRepository`)
     * threads a growing [offset] to page further. 403 -> [NetworkError.UNKNOWN] (no dedicated
     * bucket, same "else-branch" convention as
     * [org.mifos.groupbanking.core.network.service.memberprofile.MemberProfileApi]); 404 ->
     * [NetworkError.NOT_FOUND] ("member or group not found").
     */
    suspend fun getMemberSavingsDetail(
        groupId: String,
        memberId: String,
        limit: Int = 20,
        offset: Int = 0,
    ): NetworkResult<MemberSavingsDetailDto, NetworkError>

    /**
     * `GET /companion/groups/{groupId}/savings` (`api.yaml#api[get_group_savings_summary]`) —
     * savings-dashboard's group-tab read. Returns contribution-model-aware per-member rows
     * (`sharesHeld`/`shareValue` for SHARE_BASED_VARIABLE groups; `totalContributed`/
     * `meetingsContributed` for FIXED_AMOUNT groups). Replaces the old raw
     * `GET /savingsaccounts/{id}/transactions` + `GET /clients/{id}/accounts` pair. Fired in
     * PARALLEL with [getIndividualSavingsSummary] on mount/refresh/retry per
     * `data-flow.yaml#entries[0]` — the parallel-combine lives in `SavingsRepositoryImpl`
     * (`core/data`), not here. 404 -> [NetworkError.NOT_FOUND] ("group not found").
     */
    suspend fun getGroupSavingsSummary(groupId: String): NetworkResult<GroupSavingsSummaryDto, NetworkError>

    /**
     * `GET /companion/groups/{groupId}/savings/individual`
     * (`api.yaml#api[get_individual_savings_summary]`) — savings-dashboard's individual-tab read,
     * the voluntary savings balance per member. Fired in PARALLEL with [getGroupSavingsSummary] on
     * mount/refresh/retry — see that method's KDoc. `SelectTab` (GROUP/INDIVIDUAL) is a pure
     * client-side pane switch with NO re-fetch (`data-flow.yaml#entries[SelectTab]`) — this method
     * is never called standalone by a tab switch. 404 -> [NetworkError.NOT_FOUND] ("group not
     * found").
     */
    suspend fun getIndividualSavingsSummary(groupId: String): NetworkResult<IndividualSavingsSummaryDto, NetworkError>
}
