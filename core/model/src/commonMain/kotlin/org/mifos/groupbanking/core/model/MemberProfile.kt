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
 * Domain model for the member-profile screen's identity/join-date/phone header (`get_client`) —
 * pure business shape, no wire concerns. Field naming follows `api.yaml#dtos.Member`'s
 * friendlier camelCase (`firstName`/`lastName`/`phone`/`joinDate`) rather than the raw wire
 * DTO's literal Fineract casing (`firstname`/`lastname`/`mobileNo`/`activationDate`) — see
 * `MemberProfileDto.kt` kdoc for the full three-way divergence note (this feature's own
 * `dtos.Member` block vs the `get_client` operation's literal response vs member-list's
 * canonical `Member`).
 *
 * **Deliberately NOT the canonical member-list `Member`** (flagged for the cross-feature repair
 * station, same "forcing reuse would require fabricating values" precedent as `GroupDetail` vs
 * `Group`): `Member` requires non-null `role`/`savingsBalance`/`loanStatus`, none of which
 * `get_client` returns; `get_client` returns `firstName`/`lastName`/`phone`/`hasPhoto`/`status`/
 * `joinDate`/`officeId`, none of which `Member` carries. Named `MemberProfile` after the
 * `MemberRepository.getMemberProfile(...)` method this feature's own
 * `api.yaml#dependencies.repositories` declares.
 *
 * See API.md#models — MemberProfile.
 */
data class MemberProfile(
    val id: Long,
    val displayName: String,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val hasPhoto: Boolean,
    val status: MemberStatus,
    val joinDate: String,
    val officeId: Long,
)

/**
 * Domain model for the nested Fineract `{id, value}` status pair — shared by [MemberProfile]
 * (client activation status) and by each derived [ActiveLoanSummary] status concept. Mirrors
 * wire `FineractStatusDto` 1:1.
 *
 * See API.md#models — MemberStatus.
 */
data class MemberStatus(
    val id: Int,
    val value: String,
)

/**
 * Domain model for the member-profile screen's accounts card — savings balance + weekly
 * sparkline + active-loan arrears summary (`get_client_accounts`). Matches
 * `api.yaml#dtos.MemberAccounts` (the DECLARING feature's own registry shape) rather than the
 * `get_client_accounts` operation's literal `savingsAccounts[]`/`loanAccounts[]` response — see
 * `MemberProfileDto.kt` kdoc. [savingsBalance] is derived by summing `savingsAccounts[].balance`;
 * [activeLoan] is derived from the first `loanAccounts[]` row (if any); [savingsHistory] has NO
 * wire source anywhere in `api.yaml` (confirmed gap, same class as `GroupConfig.shareMin`) and
 * is always populated as an empty list by the mapper until a real time-series endpoint exists.
 *
 * See API.md#models — MemberAccounts.
 */
data class MemberAccounts(
    val savingsBalance: Double,
    val savingsHistory: List<SavingsDataPoint>,
    val activeLoan: ActiveLoanSummary?,
)

/**
 * Domain model for a single weekly sparkline point on [MemberAccounts.savingsHistory]. **NO
 * wire source** — `api.yaml` declares this shape under `dtos.SavingsDataPoint` but neither
 * `get_client` nor `get_client_accounts` returns historical balance points; the mapper always
 * produces an empty `List<SavingsDataPoint>` until a real time-series endpoint is added
 * (confirmed registry gap, flagged for the cross-feature repair station).
 *
 * See API.md#models — SavingsDataPoint.
 */
data class SavingsDataPoint(
    val date: String,
    val balance: Double,
)

/**
 * Domain model for [MemberAccounts.activeLoan] — derived from the first row of
 * `get_client_accounts.loanAccounts[]` (if the list is non-empty). [inArrears] is derived as
 * `summary.totalOverdue > 0.0`; [dueDate] has NO wire source in `MemberLoanAccountDto`/`.summary`
 * (confirmed gap) and always maps to `null`.
 *
 * See API.md#models — ActiveLoanSummary.
 */
data class ActiveLoanSummary(
    val id: Long,
    val productName: String,
    val outstandingBalance: Double,
    val inArrears: Boolean,
    val dueDate: String?,
)

/**
 * Domain model for a single row of `get_member_role` — `GET /datatables/dt_member_role/{clientId}`
 * (response `type: array`). [role] reuses the SHARED [MemberRole] enum (declared in `Member.kt`,
 * member-list) — its value-set exactly matches THIS feature's own `api.yaml#dtos.MemberRole`.
 *
 * See API.md#models — MemberRoleInfo.
 */
data class MemberRoleInfo(
    val role: MemberRole,
    val groupId: Long,
    val assignedDate: String,
)

/**
 * Domain model for the `update_member_role` request — `PUT /datatables/dt_member_role/{clientId}`
 * (403 if the caller is not chairperson). [role] reuses the SHARED [MemberRole] enum.
 *
 * See API.md#models — UpdateMemberRoleRequest.
 */
data class UpdateMemberRoleRequest(
    val role: MemberRole,
    val groupId: Long,
    val assignedDate: String,
)

/**
 * Domain model for the `update_member_role` result.
 *
 * See API.md#models — UpdateMemberRoleResult.
 */
data class UpdateMemberRoleResult(
    val resourceId: Long,
)
