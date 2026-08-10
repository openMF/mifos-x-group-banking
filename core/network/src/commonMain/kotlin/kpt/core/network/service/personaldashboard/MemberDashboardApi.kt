/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.network.service.personaldashboard

import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.network.model.MemberDashboardResponseDto

/**
 * Ktor client for the personal-dashboard feature's companion bridge — unified-identity dashboard
 * data resolved server-side from the caller's `dt_member_role` (no `clientId`/`selfServiceToken`
 * param, replacing the legacy `/self/clients/{id}/accounts` + `/self/savingsaccounts/{id}`
 * SelfService path). See `idea-layer/screens/personal-dashboard/api.yaml#api[0]` + API.md#services
 * (COMP-DASH-001).
 *
 * Returns [NetworkResult] — never a raw [Result] envelope, never a thrown exception (Mandatory
 * Rule 2). `NetworkResult`/`NetworkError` are the framework's `core-base/network` sealed types
 * (consumed, never edited — Hard Rule #8). The downstream `kmp-store-gen` Store5 wrapper and its
 * Repository switch on the sealed result / surface `ScreenState` directly, with no try-catch of
 * their own (Mandatory Rule 4) — this Service is SERVICE-ONLY; no repository/store is emitted
 * here.
 */
interface MemberDashboardApi {

    /**
     * `GET /companion/member/dashboard` (COMP-DASH-001). Fetches the authenticated member's
     * dashboard: every group they belong to (`myGroups`), the currently-selected group's summary
     * + savings balances, and (mutually exclusive per selected group's pool model) either
     * `shareOutProjection` (ACCUMULATING) or `rotationPosition`/`nextRecipientEta`
     * (ROTATING_PAYOUT), plus recent savings transactions. [selectedGroupId] is optional — `null`
     * (the default) resolves server-side to the member's first group. Response is cached
     * stale-while-revalidate by the caller per `data-flow.yaml#cache_strategy`
     * (`ttl_seconds=300`, offline `use_sqldelight`) — this Service call itself is unconditional,
     * always hitting the network.
     */
    suspend fun getMemberDashboard(
        selectedGroupId: String? = null,
    ): NetworkResult<MemberDashboardResponseDto, NetworkError>
}
