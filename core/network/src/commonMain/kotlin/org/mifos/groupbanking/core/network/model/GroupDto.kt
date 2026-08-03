/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire DTO for a single row of the group-list contract (COMP-GRP-001 —
 * `GET /companion/groups/mine`). Lists every group the authenticated user belongs to across any
 * role, with [groupType] and [viewerRole] resolved server-side from `dt_member_role` (no
 * `staffId` param — replaces the old staff-only `/centers?staffId=` Fineract endpoint). This
 * shape is the CANONICAL wire `Group` for group-list, reused by group-dashboard + member
 * features per `idea-layer/screens/group-list/api.yaml#dtos.Group`.
 *
 * See API.md#dtos — Group.
 */
@Serializable
data class GroupDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("groupType") val groupType: GroupTypeDto = GroupTypeDto.UNKNOWN,
    @SerialName("viewerRole") val viewerRole: ViewerRoleDto = ViewerRoleDto.UNKNOWN,
    @SerialName("cycleNumber") val cycleNumber: Int,
    @SerialName("memberCount") val memberCount: Int,
    @SerialName("lastMeetingDate") val lastMeetingDate: String,
    @SerialName("healthIndicator") val healthIndicator: HealthIndicatorDto = HealthIndicatorDto.UNKNOWN,
    @SerialName("overdueRate") val overdueRate: Double,
    @SerialName("status") val status: String,
    @SerialName("fineractCenterId") val fineractCenterId: Long,
) {
    companion object {
        /** Bumped when this DTO shape changes (registry `version:` driven). See EC30. */
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire envelope for the offset-paginated `GET /companion/groups/mine` response
 * (`page_size=20`). See API.md#dtos — GroupPage.
 */
@Serializable
data class GroupPageDto(
    @SerialName("totalFilteredRecords") val totalFilteredRecords: Int,
    @SerialName("pageItems") val pageItems: List<GroupDto> = emptyList(),
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire enum for a group's self-funded type as returned by COMP-GRP-001 (literal wire values —
 * note `CBO` / `BURIAL` are the SHORT forms used on THIS endpoint, distinct from the
 * `CBO_VILLAGE_BANK` / `BURIAL_WELFARE` `@SerialName`s on `GroupTypeSlugDto` in
 * `GroupTypeConfigDto.kt` (COMP-DT-003). The two wire enums are NOT reused 1:1 because their
 * `@SerialName` values differ; the domain mapper adapts both onto the SAME shared
 * `core.model.GroupTypeSlug` domain enum — see `GroupMappers.kt`.) [UNKNOWN] fallback per
 * T7/EC30 so a server-added 10th group type never crashes an old client.
 */
@Serializable
enum class GroupTypeDto {
    @SerialName("VSLA")
    VSLA,

    @SerialName("ROSCA")
    ROSCA,

    @SerialName("ASCA")
    ASCA,

    @SerialName("SILC")
    SILC,

    @SerialName("SHG")
    SHG,

    @SerialName("SACCO")
    SACCO,

    @SerialName("CBO")
    CBO,

    @SerialName("BURIAL")
    BURIAL,

    @SerialName("JLG")
    JLG,

    @SerialName("UNKNOWN")
    UNKNOWN,
}

/**
 * Wire enum for the authenticated user's role on a given group (resolved server-side from
 * `dt_member_role`). [UNKNOWN] fallback per T7/EC30 so a server-added role (e.g. AUDITOR) never
 * crashes an old client.
 */
@Serializable
enum class ViewerRoleDto {
    @SerialName("ORGANIZER")
    ORGANIZER,

    @SerialName("MEMBER")
    MEMBER,

    @SerialName("TREASURER")
    TREASURER,

    @SerialName("CHAIRPERSON")
    CHAIRPERSON,

    @SerialName("SECRETARY")
    SECRETARY,

    @SerialName("UNKNOWN")
    UNKNOWN,
}

/**
 * Wire enum for the server-computed traffic-light health status of a group, derived from
 * [GroupDto.overdueRate] (GREEN < 5%, AMBER 5-20%, RED >= 20% — see
 * `idea-layer/screens/group-list/api.yaml#dtos.HealthIndicator.rules`). The domain layer
 * re-derives this independently client-side via `HealthIndicator.fromOverdueRate` (see
 * `Group.kt`) per `idea-layer/screens/group-list/data-flow.yaml`, so cached/offline rows stay
 * correct even if the server-sent enum drifts from the rate. [UNKNOWN] fallback per T7/EC30.
 */
@Serializable
enum class HealthIndicatorDto {
    @SerialName("GREEN")
    GREEN,

    @SerialName("AMBER")
    AMBER,

    @SerialName("RED")
    RED,

    @SerialName("UNKNOWN")
    UNKNOWN,
}
