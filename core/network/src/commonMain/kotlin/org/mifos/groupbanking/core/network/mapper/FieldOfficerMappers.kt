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

import org.mifos.groupbanking.core.model.FieldOfficerDashboard
import org.mifos.groupbanking.core.model.GroupHealthSummary
import org.mifos.groupbanking.core.model.HealthIndicator
import org.mifos.groupbanking.core.network.model.CenterItemDto
import org.mifos.groupbanking.core.network.model.GroupItemDto

/**
 * DTO -> domain mappers for the field-officer-dashboard Fineract reads (FR-009).
 *
 * [aggregateFieldOfficerDashboard] is the client-side fan-in described by
 * `idea-layer/screens/field-officer-dashboard/data-flow.yaml`: it maps each [GroupItemDto] to a
 * [GroupHealthSummary], sums the per-group counts/balances into the four KPI totals, and derives
 * [FieldOfficerDashboard.availableRegions] from the distinct union of group + center `officeName`
 * values.
 *
 * **Idea-layer gap (flagged):** the `GET /groups` contract returns no `overdueRate` and no per-group
 * savings/loan balances, so [GroupItemDto.toGroupHealthSummary] defaults those to `0.0` and derives
 * [GroupHealthSummary.healthIndicator] from that (`0.0` -> GREEN) until a companion roll-up endpoint
 * supplies them — see `FieldOfficerDashboard` KDoc.
 */

fun GroupItemDto.toGroupHealthSummary(): GroupHealthSummary {
    val rate = 0.0 // no wire source on GET /groups — see file KDoc gap note
    return GroupHealthSummary(
        id = id,
        fineractGroupId = id,
        name = name,
        officeName = officeName,
        status = status.value,
        activeClientCount = activeClientCount,
        totalSavingsBalance = 0.0,
        totalLoansOutstanding = 0.0,
        overdueRate = rate,
        healthIndicator = HealthIndicator.fromOverdueRate(rate),
        cycleNumber = 0,
    )
}

/**
 * Fans the two parallel reads into the [FieldOfficerDashboard] composite, aggregating KPIs
 * client-side per `data-flow.yaml`. [staffId] and [userRole] are threaded through from the caller
 * (session-derived; see [FieldOfficerDashboard] KDoc gap note).
 */
fun aggregateFieldOfficerDashboard(
    staffId: Long,
    userRole: String,
    groups: List<GroupItemDto>,
    centers: List<CenterItemDto>,
): FieldOfficerDashboard {
    val summaries = groups.map { it.toGroupHealthSummary() }
    val regions = (summaries.map { it.officeName } + centers.map { it.officeName })
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()
    return FieldOfficerDashboard(
        staffId = staffId,
        userRole = userRole,
        totalGroupsCount = summaries.size,
        totalActiveMembers = summaries.sumOf { it.activeClientCount },
        totalSavingsThisMonth = summaries.sumOf { it.totalSavingsBalance },
        totalLoansOutstanding = summaries.sumOf { it.totalLoansOutstanding },
        groups = summaries,
        availableRegions = regions,
    )
}
