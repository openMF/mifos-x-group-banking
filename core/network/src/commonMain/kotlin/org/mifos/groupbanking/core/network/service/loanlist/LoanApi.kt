/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.service.loanlist

import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.network.model.LoanPageDto
import org.mifos.groupbanking.core.network.model.LoanSummaryDto

/**
 * Ktor client for the loan-list feature — the offset-paginated list of loan accounts belonging
 * to a savings group, with member identity + product name + principal/outstanding/overdue
 * amounts resolved server-side per row. See
 * `idea-layer/screens/loan-list/api.yaml#api[get_group_loans]` + API.md#services for the
 * endpoint contract.
 *
 * Returns [NetworkResult] — never a raw [Result] envelope, never a thrown exception (Mandatory
 * Rule 2). `NetworkResult`/`NetworkError` are the framework's `core-base/network` sealed types
 * (consumed, never edited — Hard Rule #8). The downstream `kmp-store-gen` Store5 wrapper and its
 * Repository switch on the sealed result / surface `ScreenState`/`PagingData` directly, with no
 * try-catch of their own (Mandatory Rule 4) — this Service is SERVICE-ONLY; no repository/store
 * is emitted here.
 */
interface LoanApi {

    /**
     * `GET /groups/{groupId}/loans` (`api.yaml#api[get_group_loans]`). Fetches a page of loan
     * accounts belonging to [groupId], offset-paginated ([limit] page size defaults to
     * `page_size=20` per `api.yaml#pagination`, [offset] the zero-based row offset for the next
     * page). [loanStatus] is an optional server-side filter (`active|overdue|closed|all` per
     * `api.yaml#params.loanStatus`) — omitted from the query entirely when null, so the server
     * applies its own unfiltered default. Response is cached stale-while-revalidate by the
     * caller per `data-flow.yaml#cache` (`ttl=180`, offline `show_cached`) — this Service call
     * itself is unconditional, always hitting the network. 404 -> [NetworkError.NOT_FOUND]
     * ("group not found").
     */
    suspend fun getGroupLoans(
        groupId: Long,
        limit: Int = 20,
        offset: Int = 0,
        loanStatus: String? = null,
    ): NetworkResult<LoanPageDto, NetworkError>

    /**
     * `GET /clients/{clientId}/loans` — member-loans read for the personal-loans feature, reusing
     * the loan-list contract's flat [org.mifos.groupbanking.core.network.model.LoanSummaryDto] row
     * shape (the same wire shape [getGroupLoans] returns for `GET /groups/{groupId}/loans`) instead
     * of authoring a second DTO family. Fetches the FULL (non-paginated) list of loan accounts
     * belonging to a single client/member.
     *
     * **Registry divergence flagged for Station 3** (same pattern as the documented divergence on
     * [org.mifos.groupbanking.core.network.model.LoanSummaryDto]): `idea-layer/screens/personal-loans/api.yaml#api[get_self_loans]`
     * declares a DIFFERENT self-scoped `GET /self/loans` endpoint (no explicit `clientId` path
     * param — the client is inferred from the authenticated self-service session) with a DIFFERENT
     * nested Fineract-native response shape (`LoanDto` with `status`/`repaymentSchedule`/`summary`
     * sub-objects). This method deliberately reuses the ALREADY-SHIPPED loan-list client stack
     * (flat `LoanSummaryDto`, [org.mifos.groupbanking.core.network.mapper.toDomainModels]) per an
     * explicit client-layer reuse directive rather than authoring that second nested DTO family;
     * reconcile the two `personal-loans` endpoint/DTO declarations at Station 3.
     *
     * 404 -> [NetworkError.NOT_FOUND] ("client not found").
     */
    suspend fun getClientLoans(clientId: Long): NetworkResult<List<LoanSummaryDto>, NetworkError>
}
