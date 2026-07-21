/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire DTO for the personal-dashboard companion call (`GET /companion/member/dashboard`) —
 * `idea-layer/screens/personal-dashboard/api.yaml#api[0]` (unified-identity companion API,
 * resolves the caller's groups via `dt_member_role`; no `clientId`/`selfServiceToken` param,
 * replacing the old `/self/clients/{id}/accounts` + `/self/savingsaccounts/{id}` SelfService
 * path per `docs.yaml`). Group-type-aware: [shareOutProjection] is populated for ACCUMULATING
 * pool models (VSLA/ASCA/SHG/SILC); [rotationPosition] + [nextRecipientEta] are populated for
 * ROTATING_PAYOUT pool models (ROSCA-style rotation) — the two projection axes are mutually
 * exclusive per-selected-group, both nullable.
 *
 * See API.md#dtos — MemberDashboardResponse.
 */
@Serializable
data class MemberDashboardResponseDto(
    @SerialName("memberName") val memberName: String,
    @SerialName("myGroups") val myGroups: List<GroupSummaryDto> = emptyList(),
    @SerialName("selectedGroup") val selectedGroup: GroupSummaryDto,
    @SerialName("poolModel") val poolModel: SavingsMechanismDto = SavingsMechanismDto.UNKNOWN,
    @SerialName("groupLinkedSavingsBalance") val groupLinkedSavingsBalance: Double,
    @SerialName("individualSavingsBalance") val individualSavingsBalance: Double,
    @SerialName("shareOutProjection") val shareOutProjection: Double? = null,
    @SerialName("rotationPosition") val rotationPosition: Int? = null,
    @SerialName("nextRecipientEta") val nextRecipientEta: String? = null,
    @SerialName("recentTransactions") val recentTransactions: List<SavingsTransactionDto> = emptyList(),
) {
    companion object {
        /** Bumped when this DTO shape changes (registry `version:` driven). See EC30. */
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire DTO for a single row of `MemberDashboardResponseDto.myGroups` / `.selectedGroup` — a
 * lightweight per-group summary (id, display name, pool model) distinct from the full
 * `GroupDto` (COMP-GRP-001 group-list row); the dashboard's group-selector chip row only needs
 * these three fields. [poolModel] reuses the SHARED `SavingsMechanismDto` enum (declared in
 * `GroupTypeConfigDto.kt`) rather than introducing a new pool-model wire enum — its value-set
 * (`ACCUMULATING`/`ROTATING_PAYOUT`/`NONE`/`UNKNOWN`) exactly matches
 * `api.yaml#dtos.GroupSummary.poolModel`'s declared `ACCUMULATING | ROTATING_PAYOUT | NONE`.
 *
 * See API.md#dtos — GroupSummary.
 */
@Serializable
data class GroupSummaryDto(
    @SerialName("groupId") val groupId: String,
    @SerialName("name") val name: String,
    @SerialName("poolModel") val poolModel: SavingsMechanismDto = SavingsMechanismDto.UNKNOWN,
)
