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
 * Domain model for a single group-member row (`GET /groups/{groupId}/clients`) — pure business
 * shape, no wire concerns. **Canonical** — this is the SAME `Member` shape reused by
 * member-profile + member-add + member-invite features, not a member-list-local type. Read-only;
 * member-list never mutates it locally.
 *
 * See API.md#models — Member.
 */
data class Member(
    val id: String,
    val fineractClientId: Long,
    val displayName: String,
    val photoUri: String?,
    val role: MemberRole,
    val savingsBalance: Double,
    val loanStatus: LoanStatus,
)

/**
 * Domain page envelope for the offset-paginated `GET /groups/{groupId}/clients` response
 * (`page_size=20`). Mirrors wire `MemberPageDto` — see `MemberMappers.kt`.
 */
data class MemberPage(
    val totalFilteredRecords: Int,
    val members: List<Member>,
)

/**
 * Domain enum for a member's role badge WITHIN the group being viewed. Mirrors wire
 * `MemberRoleDto` 1:1 — deliberately NOT unified with [GroupRole] (missing `CHAIRPERSON`) or
 * [ViewerRole] (a strict superset that additionally carries `ORGANIZER`, which member-list's own
 * `api.yaml#dtos.MemberRole` does not declare); see `MemberDto.kt` kdoc for the full rationale.
 * [UNKNOWN] absorbs any wire value this client build does not yet recognize.
 *
 * See API.md#models — MemberRole.
 */
enum class MemberRole {
    CHAIRPERSON,
    TREASURER,
    SECRETARY,
    MEMBER,
    UNKNOWN,
}

/**
 * Domain enum for a member's loan-status chip. Mirrors wire `LoanStatusDto` 1:1. [UNKNOWN]
 * absorbs any wire value this client build does not yet recognize.
 *
 * See API.md#models — LoanStatus.
 */
enum class LoanStatus {
    ACTIVE,
    NONE,
    OVERDUE,
    UNKNOWN,
}
