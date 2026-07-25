/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.model

/**
 * Domain composite for the field-officer-dashboard screen (FR-009) — the client-side fan-in of
 * `get_centers_for_staff` + `get_groups_for_staff` (fired in parallel on mount, keyed by the
 * session-derived staffId) aggregated into cross-group KPIs plus a per-group health list. Pure
 * business shape, no wire concerns.
 *
 * Read-only monitoring surface: there is no mutation path. Per
 * `idea-layer/screens/field-officer-dashboard/data-flow.yaml`, the ViewModel/store aggregate the
 * KPIs client-side (`totalGroupsCount = groups.size`, `totalActiveMembers = sum(activeClientCount)`,
 * `totalSavingsThisMonth = sum(totalSavingsBalance)`, `totalLoansOutstanding =
 * sum(totalLoansOutstanding)`), derive each group's [GroupHealthSummary.healthIndicator] from its
 * `overdueRate` (GREEN < 0.05, AMBER 0.05–0.20, RED >= 0.20 — reusing the shared
 * [HealthIndicator.fromOverdueRate] rule already used by group-list), and extract [availableRegions]
 * from the distinct set of group + center `officeName` values.
 *
 * **Idea-layer gap (flagged, not invented here):** `api.yaml` declares [GroupHealthSummary] as
 * "client-side derived from GroupItem + computed overdueRate; not a network DTO", but the two
 * source endpoints (`GET /centers`, `GET /groups`) return neither `overdueRate` nor per-group
 * savings/loan balances — the mapper therefore defaults those to `0.0` (see `FieldOfficerMappers.kt`)
 * until a companion roll-up endpoint supplies them. Similarly [staffId]/[userRole] are declared
 * `source: session` on `ui.yaml#nav_params`, but no session-staff accessor exists in this build
 * (`SessionManager` carries only inactivity-timeout state and `AuthSession` carries `userId`, not a
 * Fineract staffId/role) — they are threaded through as-supplied defaults pending that accessor.
 * Reported to the caller for an idea-layer / infra follow-up rather than fabricated with invented
 * values. See API.md#models — FieldOfficerDashboard.
 */
data class FieldOfficerDashboard(
    val staffId: Long,
    val userRole: String,
    val totalGroupsCount: Int,
    val totalActiveMembers: Int,
    val totalSavingsThisMonth: Double,
    val totalLoansOutstanding: Double,
    val groups: List<GroupHealthSummary>,
    val availableRegions: List<String>,
) {
    /**
     * Role gate for the Export Report action (`ui.yaml#components.top_bar.export_action.visible_when:
     * canExport == true`). True only for `FIELD_OFFICER` and `PROGRAM_MANAGER` — the same two roles
     * `ui.yaml#entry_points` already require to reach this screen at all.
     */
    val canExport: Boolean
        get() = userRole == ROLE_FIELD_OFFICER || userRole == ROLE_PROGRAM_MANAGER

    companion object {
        const val ROLE_FIELD_OFFICER: String = "FIELD_OFFICER"
        const val ROLE_PROGRAM_MANAGER: String = "PROGRAM_MANAGER"
    }
}

/**
 * Domain model for one row of the field-officer group-health list
 * (`ui.yaml#components.group_health_card`, repeats over `filteredGroups`). [healthIndicator] reuses
 * the shared [HealthIndicator] enum (GREEN/AMBER/RED/UNKNOWN) already declared on `Group.kt`, and is
 * re-derived client-side from [overdueRate] via [HealthIndicator.fromOverdueRate] so cached/offline
 * rows stay correct even if a future server-sent indicator drifts from the rate.
 *
 * [id] is the client-side stable list key (mirrors [fineractGroupId] today, kept distinct for
 * forward-compat with a companion roll-up id). [totalSavingsBalance]/[totalLoansOutstanding]/
 * [overdueRate] have no wire source on the current `GET /groups` contract (see
 * [FieldOfficerDashboard] KDoc gap note) — the mapper defaults them to `0.0`.
 *
 * See API.md#models — GroupHealthSummary.
 */
data class GroupHealthSummary(
    val id: Long,
    val fineractGroupId: Long,
    val name: String,
    val officeName: String,
    val status: String,
    val activeClientCount: Int,
    val totalSavingsBalance: Double,
    val totalLoansOutstanding: Double,
    val overdueRate: Double,
    val healthIndicator: HealthIndicator,
    val cycleNumber: Int,
)

/**
 * Domain enum for the group-status filter chip (`ui.yaml#components.filter_row.status_filter_chip`)
 * — verbatim mirror of `api.yaml#dtos.GroupStatusFilter.values`. Matched case-insensitively against
 * [GroupHealthSummary.status] in the ViewModel's client-side filter.
 * See API.md#models — GroupStatusFilter.
 */
enum class GroupStatusFilter {
    ACTIVE,
    PENDING,
    CLOSED,
}

/**
 * Domain enum for the overdue-rate threshold filter chip
 * (`ui.yaml#components.filter_row.overdue_filter_chip`) — verbatim mirror of
 * `api.yaml#dtos.OverdueRateFilter.values`. [NONE] keeps only groups with no overdue
 * (`overdueRate <= 0.0`), [LESS_THAN_10] keeps `overdueRate < 0.10`, [GREATER_THAN_10] keeps
 * `overdueRate > 0.10`.
 * See API.md#models — OverdueRateFilter.
 */
enum class OverdueRateFilter {
    NONE,
    LESS_THAN_10,
    GREATER_THAN_10,
    ;

    /** Applies this threshold to a raw overdue [rate] (0.0–1.0). */
    fun matches(rate: Double): Boolean = when (this) {
        NONE -> rate <= 0.0
        LESS_THAN_10 -> rate < 0.10
        GREATER_THAN_10 -> rate > 0.10
    }
}
