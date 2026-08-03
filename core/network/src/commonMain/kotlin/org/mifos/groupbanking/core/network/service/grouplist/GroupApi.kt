/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.service.grouplist

import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.network.model.GroupPageDto

/**
 * Ktor client for the group-list feature's companion bridge — every group the authenticated
 * user belongs to across any role, offset-paginated. See
 * `idea-layer/screens/group-list/api.yaml` + API.md#services for the endpoint contract
 * (COMP-GRP-001).
 *
 * Returns [NetworkResult] — never a raw [Result] envelope, never a thrown exception (Mandatory
 * Rule 2). `NetworkResult`/`NetworkError` are the framework's `core-base/network` sealed types
 * (consumed, never edited — Hard Rule #8). The downstream `kmp-store-gen` Store5 wrapper and its
 * Repository switch on the sealed result / surface `ScreenState`/`PagingData` directly, with no
 * try-catch of their own (Mandatory Rule 4) — this Service is SERVICE-ONLY; no repository/store
 * is emitted here.
 */
interface GroupApi {

    /**
     * `GET /companion/groups/mine` (COMP-GRP-001). Fetches all groups the authenticated user
     * belongs to, with `groupType` and `viewerRole` resolved server-side per group (from
     * `dt_member_role`). Offset-paginated: [paged] toggles pagination framing, [limit] is the
     * page size (`page_size=20` default per `api.yaml#pagination`), [offset] is the zero-based
     * row offset for the next page. Response is cached stale-while-revalidate by the caller per
     * `data-flow.yaml#cache` (`ttl_seconds=300`, offline `show_cached`) — this Service call
     * itself is unconditional, always hitting the network.
     */
    suspend fun getMyGroups(
        paged: Boolean = true,
        limit: Int = 20,
        offset: Int = 0,
    ): NetworkResult<GroupPageDto, NetworkError>
}
