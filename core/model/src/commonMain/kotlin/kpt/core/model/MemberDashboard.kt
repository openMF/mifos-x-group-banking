/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.model

/**
 * Domain model for the personal-dashboard member home screen — pure business shape, no wire
 * concerns. Group-type-aware: [shareOutProjection] is populated for ACCUMULATING pool models,
 * [rotationPosition] + [nextRecipientEta] are populated for ROTATING_PAYOUT pool models
 * (mutually exclusive per [selectedGroup]).
 *
 * See API.md#models — MemberDashboard.
 */
data class MemberDashboard(
    val memberName: String,
    /**
     * Member-identity + savings-account ids forwarded to the `personal-savings` nav_params
     * (`clientId`, `groupLinkedSavingsId`, `individualSavingsId`) when the member taps the savings
     * summary card. [individualSavingsId] is nullable — the member may have no voluntary individual
     * account (`personal-savings` route's `individualSavingsId: Long? = null`).
     */
    val clientId: Long,
    val groupLinkedSavingsId: Long,
    val individualSavingsId: Long?,
    val myGroups: List<GroupSummary>,
    val selectedGroup: GroupSummary,
    val poolModel: SavingsMechanism,
    val groupLinkedSavingsBalance: Double,
    val individualSavingsBalance: Double,
    val shareOutProjection: Double?,
    val rotationPosition: Int?,
    val nextRecipientEta: String?,
    val recentTransactions: List<SavingsTransaction>,
)

/**
 * Domain model for a single per-group summary row (group-selector chip). [poolModel] reuses the
 * SHARED `SavingsMechanism` domain enum (declared in `GroupTypeConfig.kt`) — the group-type-picker
 * catalogue's internal-pool axis — rather than introducing a second pool-model domain enum.
 *
 * See API.md#models — GroupSummary.
 */
data class GroupSummary(
    val groupId: String,
    val name: String,
    val poolModel: SavingsMechanism,
)
