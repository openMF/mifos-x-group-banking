/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.network.mapper

import kpt.core.model.GroupSummary
import kpt.core.model.MemberDashboard
import kpt.core.network.model.GroupSummaryDto
import kpt.core.network.model.MemberDashboardResponseDto

/**
 * DTO -> domain mappers for the personal-dashboard wire contract (`GET
 * /companion/member/dashboard`). Every field on `MemberDashboardResponseDto` / `GroupSummaryDto`
 * declared in `MemberDashboardDto.kt` is mapped — no field left unmapped.
 *
 * `poolModel` on both DTOs reuses the SHARED `SavingsMechanismDto.toDomainModel()` mapper already
 * declared in `GroupTypeConfigMappers.kt` — not redefined here.
 */

fun MemberDashboardResponseDto.toDomainModel(): MemberDashboard = MemberDashboard(
    memberName = memberName,
    clientId = clientId,
    groupLinkedSavingsId = groupLinkedSavingsId,
    individualSavingsId = individualSavingsId,
    myGroups = myGroups.map { it.toDomainModel() },
    selectedGroup = selectedGroup.toDomainModel(),
    poolModel = poolModel.toDomainModel(),
    groupLinkedSavingsBalance = groupLinkedSavingsBalance,
    individualSavingsBalance = individualSavingsBalance,
    shareOutProjection = shareOutProjection,
    rotationPosition = rotationPosition,
    nextRecipientEta = nextRecipientEta,
    recentTransactions = recentTransactions.toDomainModels(),
)

fun GroupSummaryDto.toDomainModel(): GroupSummary = GroupSummary(
    groupId = groupId,
    name = name,
    poolModel = poolModel.toDomainModel(),
)
