/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.service.memberlist

import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.network.model.MemberPageDto

/**
 * Ktor client for the member-list feature — the offset-paginated roster of clients (members)
 * belonging to a savings group, with role badge and loan-status resolved server-side. See
 * `idea-layer/screens/member-list/api.yaml#api[get_group_members]` + API.md#services for the
 * endpoint contract.
 *
 * Returns [NetworkResult] — never a raw [Result] envelope, never a thrown exception (Mandatory
 * Rule 2). `NetworkResult`/`NetworkError` are the framework's `core-base/network` sealed types
 * (consumed, never edited — Hard Rule #8). The downstream `kmp-store-gen` Store5 wrapper and its
 * Repository switch on the sealed result / surface `ScreenState`/`PagingData` directly, with no
 * try-catch of their own (Mandatory Rule 4) — this Service is SERVICE-ONLY; no repository/store
 * is emitted here.
 */
interface MemberApi {

    /**
     * `GET /groups/{groupId}/clients` (`api.yaml#api[get_group_members]`). Fetches a page of
     * clients (members) belonging to [groupId], offset-paginated ([limit] page size defaults to
     * `page_size=20` per `api.yaml#pagination`, [offset] the zero-based row offset for the next
     * page). Response is cached stale-while-revalidate by the caller per
     * `data-flow.yaml#cache` (`ttl_seconds=120`, offline `show_cached`) — this Service call
     * itself is unconditional, always hitting the network. 404 -> [NetworkError.NOT_FOUND]
     * ("group not found").
     */
    suspend fun getGroupMembers(
        groupId: String,
        limit: Int = 20,
        offset: Int = 0,
    ): NetworkResult<MemberPageDto, NetworkError>
}
