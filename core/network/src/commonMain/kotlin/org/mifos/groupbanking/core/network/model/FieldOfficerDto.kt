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
 * Wire envelope for the paginated `GET /fineract-provider/api/v1/groups?staffId=…` response
 * (field-officer-dashboard `get_groups_for_staff`, FR-009). See API.md#dtos — PagedGroupsResponse.
 */
@Serializable
data class PagedGroupsResponseDto(
    @SerialName("totalFilteredRecords") val totalFilteredRecords: Int = 0,
    @SerialName("pageItems") val pageItems: List<GroupItemDto> = emptyList(),
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire DTO for a single group row (`get_groups_for_staff`). Note this is the Fineract
 * staff-scoped group shape — distinct from the companion `GroupDto` (COMP-GRP-001) used by
 * group-list. `overdueRate` / savings / loan balances are NOT returned by this endpoint; the
 * domain [org.mifos.groupbanking.core.model.GroupHealthSummary] defaults them to `0.0` (see
 * `FieldOfficerMappers.kt`). See API.md#dtos — GroupItem.
 */
@Serializable
data class GroupItemDto(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String = "",
    @SerialName("status") val status: StatusInfoDto = StatusInfoDto(),
    @SerialName("officeName") val officeName: String = "",
    @SerialName("staffName") val staffName: String = "",
    @SerialName("activeClientCount") val activeClientCount: Int = 0,
)

/**
 * Wire DTO for the nested Fineract `status` object (`{ id, value }`) carried by both center and
 * group rows. [value] is the human status string (e.g. `"active"`, `"pending"`, `"closed"`).
 * See API.md#dtos — StatusInfo.
 */
@Serializable
data class StatusInfoDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("value") val value: String = "",
)
