/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.network.service.grouptypepicker

import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.network.model.GroupTypeConfigDto

/**
 * Ktor client for the seeded group-type catalogue — the card list backing the
 * group-type-picker screen. See `idea-layer/screens/group-type-picker/api.yaml` +
 * API.md#services for the endpoint contract (COMP-DT-003).
 *
 * Returns [NetworkResult] — never a raw [Result] envelope, never a thrown exception (Mandatory
 * Rule 2). `NetworkResult`/`NetworkError` are the framework's `core-base/network` sealed types
 * (consumed, never edited — Hard Rule #8). The next generator's Store5 wrapper switches on the
 * sealed result directly, with no try-catch of its own (Mandatory Rule 4).
 */
interface GroupTypeConfigApi {

    /**
     * `GET /companion/datatables/group_type_config/{entityId}` (COMP-DT-003) — companion read of
     * the seeded group-type archetype catalogue. No auth token required. [entityId] defaults to
     * `0`, the global seeded catalogue query the companion serves, not a per-group lookup. Response
     * is cached SWR by the caller per `data-flow.yaml#cache_strategy` (`ttl=86400`,
     * `offline=show_cached`) — this Service call itself is unconditional.
     */
    suspend fun getGroupTypeConfigs(entityId: Long = 0): NetworkResult<List<GroupTypeConfigDto>, NetworkError>
}
