/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.mapper

import kotlinx.datetime.LocalDate
import org.mifos.groupbanking.core.model.Group
import org.mifos.groupbanking.core.model.GroupPage
import org.mifos.groupbanking.core.model.GroupTypeSlug
import org.mifos.groupbanking.core.model.HealthIndicator
import org.mifos.groupbanking.core.model.ViewerRole
import org.mifos.groupbanking.core.network.model.GroupDto
import org.mifos.groupbanking.core.network.model.GroupPageDto
import org.mifos.groupbanking.core.network.model.GroupTypeDto
import org.mifos.groupbanking.core.network.model.HealthIndicatorDto
import org.mifos.groupbanking.core.network.model.ViewerRoleDto
import kotlin.jvm.JvmName

/**
 * DTO -> domain mappers for the group-list wire contract (COMP-GRP-001). Every field on
 * `GroupDto` declared in `GroupDto.kt` is mapped — no field left unmapped.
 */

fun GroupDto.toDomainModel(): Group = Group(
    id = id,
    name = name,
    groupType = groupType.toDomainModel(),
    viewerRole = viewerRole.toDomainModel(),
    cycleNumber = cycleNumber,
    memberCount = memberCount,
    lastMeetingDate = LocalDate.parse(lastMeetingDate),
    healthIndicator = healthIndicator.toDomainModel(),
    overdueRate = overdueRate,
    status = status,
    fineractCenterId = fineractCenterId,
)

/** Batch converter — maps every group row in declaration order. */
@JvmName("groupDtoListToDomainModels")
fun List<GroupDto>.toDomainModels(): List<Group> = map { it.toDomainModel() }

/** Page converter — maps the offset-paginated envelope, preserving `totalFilteredRecords`. */
fun GroupPageDto.toDomainModel(): GroupPage = GroupPage(
    totalFilteredRecords = totalFilteredRecords,
    groups = pageItems.toDomainModels(),
)

/**
 * Adapts the group-list wire enum (short-form `CBO` / `BURIAL`) onto the SHARED
 * `core.model.GroupTypeSlug` domain enum (long-form `CBO_VILLAGE_BANK` / `BURIAL_WELFARE`,
 * already used by group-type-picker's `GroupTypeConfig`) rather than introducing a second
 * group-type domain enum — see `GroupDto.kt` kdoc for why the two WIRE enums (`GroupTypeDto` vs
 * `GroupTypeSlugDto`) are not literally reused.
 */
fun GroupTypeDto.toDomainModel(): GroupTypeSlug = when (this) {
    GroupTypeDto.VSLA -> GroupTypeSlug.VSLA
    GroupTypeDto.ROSCA -> GroupTypeSlug.ROSCA
    GroupTypeDto.ASCA -> GroupTypeSlug.ASCA
    GroupTypeDto.SILC -> GroupTypeSlug.SILC
    GroupTypeDto.SHG -> GroupTypeSlug.SHG
    GroupTypeDto.SACCO -> GroupTypeSlug.SACCO
    GroupTypeDto.CBO -> GroupTypeSlug.CBO_VILLAGE_BANK
    GroupTypeDto.BURIAL -> GroupTypeSlug.BURIAL_WELFARE
    GroupTypeDto.JLG -> GroupTypeSlug.JLG
    GroupTypeDto.UNKNOWN -> GroupTypeSlug.UNKNOWN
}

fun ViewerRoleDto.toDomainModel(): ViewerRole = when (this) {
    ViewerRoleDto.ORGANIZER -> ViewerRole.ORGANIZER
    ViewerRoleDto.MEMBER -> ViewerRole.MEMBER
    ViewerRoleDto.TREASURER -> ViewerRole.TREASURER
    ViewerRoleDto.CHAIRPERSON -> ViewerRole.CHAIRPERSON
    ViewerRoleDto.SECRETARY -> ViewerRole.SECRETARY
    ViewerRoleDto.UNKNOWN -> ViewerRole.UNKNOWN
}

fun HealthIndicatorDto.toDomainModel(): HealthIndicator = when (this) {
    HealthIndicatorDto.GREEN -> HealthIndicator.GREEN
    HealthIndicatorDto.AMBER -> HealthIndicator.AMBER
    HealthIndicatorDto.RED -> HealthIndicator.RED
    HealthIndicatorDto.UNKNOWN -> HealthIndicator.UNKNOWN
}
