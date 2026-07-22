/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.service.groupdashboard

import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.network.model.GroupAccountsDto
import org.mifos.groupbanking.core.network.model.GroupCorpusDto
import org.mifos.groupbanking.core.network.model.GroupDetailDto
import org.mifos.groupbanking.core.network.model.ViewerRoleInfoDto

/**
 * Ktor client for the group-dashboard companion bridge (COMP-GRP-001 read path) — the FOUR
 * independent reads the group-dashboard screen fires in parallel on mount/refresh/retry:
 * group identity + embedded `typeConfig` ([getGroup]), the authenticated user's role in this
 * group ([getViewerRole]), corpus/rotation financial state ([getGroupCorpus]), and
 * savings/loan account summary + recent activity feed ([getGroupAccounts]). See
 * `idea-layer/screens/group-dashboard/api.yaml#api` + `data-flow.yaml#entries[0].endpoints` (all
 * 4 fire as concurrent coroutines) + API.md#services.
 *
 * Every method returns [NetworkResult] — never a raw [Result] envelope, never a thrown
 * exception (Mandatory Rule 2). `NetworkResult`/`NetworkError` are the framework's
 * `core-base/network` sealed types (consumed, never edited — Hard Rule #8). This Service is
 * SERVICE-ONLY — the 4-way parallel-combine into `GroupDashboardResponseDto`/the domain
 * `GroupDashboard` model and the Store5 wrapper (`.asScreenStream()`) are emitted by a
 * downstream `kmp-store-gen`/`kmp-client-gen` generation step, not here; no repository/store is
 * declared in this file.
 */
interface GroupDashboardApi {

    /**
     * `GET /companion/groups/{groupId}` (COMP-GRP-001). Fetches group identity (name, cycle
     * info, member/overdue counts, status) plus the embedded `typeConfig`
     * ([GroupDetailDto.typeConfig]) that drives metric-card selection (ACCUMULATING vs
     * ROTATING_PAYOUT) downstream. 404 -> [NetworkError.NOT_FOUND] ("group not found"); 403 has
     * no dedicated [NetworkError] bucket and falls into [NetworkError.UNKNOWN], same convention
     * as every other `*ApiImpl` in this module.
     */
    suspend fun getGroup(groupId: String): NetworkResult<GroupDetailDto, NetworkError>

    /**
     * `GET /companion/groups/{groupId}/my-role` — resolves the authenticated user's role
     * (`ORGANIZER | MEMBER | TREASURER | CHAIRPERSON | SECRETARY`) in [groupId] from
     * `dt_member_role`, sourced server-side from the caller's session (no `userId` param). Drives
     * quick-actions grid visibility and Share-Out/Start-Meeting button gating downstream. 404 ->
     * [NetworkError.NOT_FOUND] ("not a member of this group").
     */
    suspend fun getViewerRole(groupId: String): NetworkResult<ViewerRoleInfoDto, NetworkError>

    /**
     * `GET /companion/groups/{groupId}/corpus`. Fetches corpus state for ACCUMULATING pool
     * models (`currentBalance`/`totalContributionsThisCycle`/`totalLoansOutstanding`) or
     * rotation state for ROTATING_PAYOUT pool models (`rotationPosition`/`nextRecipientName`/
     * `nextRecipientPosition`, all nullable on [GroupCorpusDto] — populated only for rotating
     * pool models). 404 -> [NetworkError.NOT_FOUND] ("corpus record not found").
     */
    suspend fun getGroupCorpus(groupId: String): NetworkResult<GroupCorpusDto, NetworkError>

    /**
     * `GET /companion/groups/{groupId}/accounts`. Fetches the savings/loan account summary
     * (`savingsBalance`/`loansOutstanding`/`activeLoanCount`), `shareOutProjection` (nullable —
     * ACCUMULATING pool models only), and the last-10 `recentActivity` feed
     * ([GroupAccountsDto.recentActivity]) backing the dashboard's activity-feed section. 404 ->
     * [NetworkError.NOT_FOUND] ("group not found").
     */
    suspend fun getGroupAccounts(groupId: String): NetworkResult<GroupAccountsDto, NetworkError>
}
