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
}
