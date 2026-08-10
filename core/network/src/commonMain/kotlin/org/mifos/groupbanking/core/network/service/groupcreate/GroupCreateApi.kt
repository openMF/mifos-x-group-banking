/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.service.groupcreate

import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.network.model.CreateGroupRequestDto
import org.mifos.groupbanking.core.network.model.CreateGroupResponseDto
import org.mifos.groupbanking.core.network.model.OfficeDto

/**
 * Ktor client for the group-create wizard's client stack. See
 * `idea-layer/screens/group-create/api.yaml` + API.md#services for the endpoint contract.
 *
 * Every method returns [NetworkResult] — never a raw [Result] envelope, never a thrown
 * exception (Mandatory Rule 2). `NetworkResult`/`NetworkError` are the framework's
 * `core-base/network` sealed types (consumed, never edited — Hard Rule #8).
 * [org.mifos.groupbanking.core.data.repository.GroupCreateRepositoryImpl] switches on the
 * sealed result directly, with no try-catch of its own (Mandatory Rule 4).
 *
 * See API.md#services — GroupCreateApi.
 */
interface GroupCreateApi {

    /**
     * `GET /offices` (raw Fineract passthrough — `list_all_offices`, NOT a `/companion/…`
     * endpoint; distinct host-relative-path from [createGroup]'s companion bridge, though both
     * are served by the same companion host today). Fetches every office for the wizard's
     * office dropdown. [orderBy] defaults to `"name"` per `api.yaml#get_offices.params.orderBy`.
     * Cached SWR by the caller per `data-flow.yaml` (`ttl_seconds=3600`, offline `show_cached`) —
     * this Service call itself is unconditional, always hitting the network.
     */
    suspend fun getOffices(orderBy: String = "name"): NetworkResult<List<OfficeDto>, NetworkError>

    /**
     * `POST /companion/groups` (COMP-GRP-001). Single companion-API call that orchestrates:
     * (1) createGroup in Fineract, (2) activate the group, (3) associateClients (creator becomes
     * a member), (4) assignRole ORGANIZER to the creator, and (5) provision the
     * `group_type_config` datatable row from [request]'s `typeConfig`. No auth-required header is
     * threaded explicitly — the shared client's `bearerTokensProvider` plugin handles it, same
     * convention as `GroupApi`/`InvitationApi`.
     */
    suspend fun createGroup(request: CreateGroupRequestDto): NetworkResult<CreateGroupResponseDto, NetworkError>
}
