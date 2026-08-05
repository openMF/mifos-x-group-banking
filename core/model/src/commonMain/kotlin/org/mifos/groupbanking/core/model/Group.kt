/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.model

import kotlinx.datetime.LocalDate

/**
 * Domain model for a single group the authenticated user belongs to (COMP-GRP-001 — `GET
 * /companion/groups/mine`) — pure business shape, no wire concerns. **Canonical** — this is the
 * SAME `Group` shape reused by group-dashboard + member features, not a group-list-local type.
 * Read-only; group-list never mutates it locally.
 *
 * See API.md#models — Group.
 */
data class Group(
    val id: String,
    val name: String,
    val groupType: GroupTypeSlug,
    val viewerRole: ViewerRole,
    val cycleNumber: Int,
    val memberCount: Int,
    val lastMeetingDate: LocalDate,
    val healthIndicator: HealthIndicator,
    val overdueRate: Double,
    val status: String,
    val fineractGroupId: Long,
)

/**
 * Domain page envelope for the offset-paginated `GET /companion/groups/mine` response
 * (`page_size=20`). Mirrors wire `GroupPageDto` — see `GroupMappers.kt`.
 */
data class GroupPage(
    val totalFilteredRecords: Int,
    val groups: List<Group>,
)

/**
 * Domain enum for the authenticated user's role on a given [Group]. Distinct from [GroupRole]
 * (the login-signup / `AuthSession.groupMemberships` role, `ORGANIZER`/`MEMBER`/`TREASURER`/
 * `SECRETARY`/`UNKNOWN`) — [ViewerRole] additionally carries `CHAIRPERSON`, which [GroupRole]
 * does not. The two enums were NOT unified: `GroupRole` is a shared login-signup wire/domain
 * contract already consumed elsewhere (`AuthSession`, `UserProfile`); widening it to add
 * `CHAIRPERSON` (or dropping `ViewerRole` in favor of it) is a cross-feature decision flagged for
 * the repair station rather than made unilaterally here. [UNKNOWN] absorbs any wire role this
 * client build does not yet recognize.
 * See API.md#models — ViewerRole.
 */
enum class ViewerRole {
    ORGANIZER,
    MEMBER,
    TREASURER,
    CHAIRPERSON,
    SECRETARY,
    UNKNOWN,
}

/**
 * Domain enum for a group's traffic-light health status. The server sends a computed
 * `healthIndicator` alongside `overdueRate` (mapped 1:1 by `GroupMappers.kt`), but this enum ALSO
 * exposes [fromOverdueRate] as a standalone derivation utility so callers (ViewModel / Store
 * cache-normalization) can independently RE-derive the indicator client-side from
 * [Group.overdueRate] per `idea-layer/screens/group-list/data-flow.yaml` ("HealthIndicator is
 * derived client-side from overdueRate") — guarding against server/client rule drift, especially
 * for cached/offline rows. [UNKNOWN] absorbs any wire value this client build does not yet
 * recognize (never produced by [fromOverdueRate] itself).
 * See API.md#models — HealthIndicator.
 */
enum class HealthIndicator {
    GREEN,
    AMBER,
    RED,
    UNKNOWN,
    ;

    companion object {
        /**
         * Derives the traffic-light indicator from a raw overdue rate, per
         * `idea-layer/screens/group-list/api.yaml#dtos.HealthIndicator.rules`:
         * GREEN < 0.05, AMBER in [0.05, 0.20), RED >= 0.20.
         */
        fun fromOverdueRate(rate: Double): HealthIndicator = when {
            rate < 0.05 -> GREEN
            rate < 0.20 -> AMBER
            else -> RED
        }
    }
}
