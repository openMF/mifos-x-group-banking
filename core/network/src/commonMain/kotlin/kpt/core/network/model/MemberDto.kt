/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire DTO for a single row of the member-list contract (`GET
 * /groups/{groupId}/clients`). Carries the role badge ([role]) and loan-status chip
 * ([loanStatus]) the member-list screen renders per row. This shape is the CANONICAL wire
 * `Member` for member-list, reused by member-profile + member-add + member-invite per
 * `idea-layer/screens/member-list/api.yaml#dtos.Member`.
 *
 * **Registry divergence (flagged for the cross-feature repair station, same pattern as
 * `GroupDto`'s / `SavingsTransactionDto`'s documented divergences — see
 * `core/network/model/API.md`):** `idea-layer/dtos/MemberDto.yaml` (registry v2.0.0) declares a
 * DIFFERENT `MemberDto` shape (`id: Long`, `roleInGroup: String` with lowercase values
 * `organizer`/`treasurer`/`chairperson`/`secretary`/`member`, `status: String` Fineract client
 * status, `imageId: Long?`, `savingsAccountId: Long?`, `joinedDate: String?`) sourced from `GET
 * /clients/{clientId}` + the SAME `list_endpoint: GET /groups/{groupId}/clients`, and does NOT
 * list `member-list` in its `used_by` (only `member-onboarding` + `meeting-lifecycle`). It also
 * carries no `loanStatus`/`savingsBalance`/role-badge concept at all. This DTO was generated
 * from member-list's OWN approved `api.yaml#dtos.Member` block instead (which explicitly
 * declares `role: MemberRole` + `loanStatus: LoanStatus` — the exact role-badge + loan-status
 * shape this generation brief requested) — reconcile the two `MemberDto` declarations at
 * Station 3.
 *
 * See API.md#dtos — Member.
 */
@Serializable
data class MemberDto(
    @SerialName("id") val id: String,
    @SerialName("fineractClientId") val fineractClientId: Long,
    @SerialName("displayName") val displayName: String,
    @SerialName("photoUri") val photoUri: String? = null,
    @SerialName("role") val role: MemberRoleDto = MemberRoleDto.UNKNOWN,
    @SerialName("savingsBalance") val savingsBalance: Double,
    @SerialName("loanStatus") val loanStatus: LoanStatusDto = LoanStatusDto.UNKNOWN,
) {
    companion object {
        /** Bumped when this DTO shape changes (registry `version:` driven). See EC30. */
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire envelope for the offset-paginated `GET /groups/{groupId}/clients` response
 * (`page_size=20`, stale-while-revalidate, `ttl=120`). See API.md#dtos — MemberPage.
 */
@Serializable
data class MemberPageDto(
    @SerialName("totalFilteredRecords") val totalFilteredRecords: Int,
    @SerialName("pageItems") val pageItems: List<MemberDto> = emptyList(),
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire enum for a member's role badge WITHIN the group being viewed (member-list's own
 * `api.yaml#dtos.MemberRole` — 4 known values, no `ORGANIZER`). [UNKNOWN] fallback per T7/EC30
 * so a server-added role never crashes an old client.
 *
 * **NOT reused from an existing role enum (flagged for Station 3, same "near-miss, don't force
 * it" precedent as `ViewerRole`'s own kdoc vs `GroupRole`):** neither `GroupRoleDto`
 * (`ORGANIZER`/`MEMBER`/`TREASURER`/`SECRETARY` — missing `CHAIRPERSON`) nor `ViewerRoleDto`
 * (`ORGANIZER`/`MEMBER`/`TREASURER`/`CHAIRPERSON`/`SECRETARY` — a strict superset that ALSO
 * carries `ORGANIZER`, which this feature's `api.yaml#dtos.MemberRole` does not declare) is an
 * exact value-set match for this DTO's declared 4 values. Widening `GroupRole` to add
 * `CHAIRPERSON`, or narrowing `ViewerRole` by dropping `ORGANIZER`, are both cross-feature
 * decisions left to the repair station rather than made unilaterally here.
 */
@Serializable
enum class MemberRoleDto {
    @SerialName("CHAIRPERSON")
    CHAIRPERSON,

    @SerialName("TREASURER")
    TREASURER,

    @SerialName("SECRETARY")
    SECRETARY,

    @SerialName("MEMBER")
    MEMBER,

    @SerialName("UNKNOWN")
    UNKNOWN,
}

/**
 * Wire enum for a member's loan-status chip on the member-list row (`api.yaml#dtos.LoanStatus`
 * — 3 known values). [UNKNOWN] fallback per T7/EC30 so a server-added status (e.g. `WRITTEN_OFF`)
 * never crashes an old client. No pre-existing loan-status enum was found in `core/network/model`
 * to reuse.
 */
@Serializable
enum class LoanStatusDto {
    @SerialName("ACTIVE")
    ACTIVE,

    @SerialName("NONE")
    NONE,

    @SerialName("OVERDUE")
    OVERDUE,

    @SerialName("UNKNOWN")
    UNKNOWN,
}
