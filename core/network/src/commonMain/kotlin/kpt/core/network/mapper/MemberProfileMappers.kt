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

import kpt.core.model.ActiveLoanSummary
import kpt.core.model.MemberAccounts
import kpt.core.model.MemberProfile
import kpt.core.model.MemberRole
import kpt.core.model.MemberRoleInfo
import kpt.core.model.MemberStatus
import kpt.core.model.UpdateMemberRoleRequest
import kpt.core.model.UpdateMemberRoleResult
import kpt.core.network.model.FineractStatusDto
import kpt.core.network.model.MemberAccountsDto
import kpt.core.network.model.MemberLoanAccountDto
import kpt.core.network.model.MemberProfileDto
import kpt.core.network.model.MemberRoleDto
import kpt.core.network.model.MemberRoleInfoDto
import kpt.core.network.model.UpdateMemberRoleRequestDto
import kpt.core.network.model.UpdateMemberRoleResponseDto
import kotlin.jvm.JvmName

/**
 * DTO <-> domain mappers for the member-profile wire contract (`get_client` +
 * `get_client_accounts` + `get_member_role` + `update_member_role`). Every field on every DTO
 * declared in `MemberProfileDto.kt` is mapped — no field left unmapped.
 *
 * [MemberRoleInfoDto.role] / [UpdateMemberRoleRequestDto.role] reuse the SHARED
 * `MemberRoleDto.toDomainModel()` mapper already declared in `MemberMappers.kt` (member-list) —
 * not redefined here.
 */

fun MemberProfileDto.toDomainModel(): MemberProfile = MemberProfile(
    id = id,
    displayName = displayName,
    firstName = firstName,
    lastName = lastName,
    phone = mobileNo,
    hasPhoto = imagePresent,
    status = status.toDomainModel(),
    joinDate = activationDate,
    officeId = officeId,
)

fun FineractStatusDto.toDomainModel(): MemberStatus = MemberStatus(
    id = id,
    value = value,
)

/**
 * Aggregates the LITERAL `get_client_accounts` response (`savingsAccounts[]` / `loanAccounts[]`)
 * into `api.yaml#dtos.MemberAccounts`'s declared domain shape:
 * - [MemberAccounts.savingsBalance] = sum of every `savingsAccounts[].balance`.
 * - [MemberAccounts.activeLoan] = derived from the FIRST `loanAccounts[]` row (if any) —
 *   see [toActiveLoanSummary].
 * - [MemberAccounts.savingsHistory] = always `emptyList()` — the weekly sparkline has NO wire
 *   source anywhere in `api.yaml` (confirmed registry gap, see `MemberProfile.kt` kdoc).
 */
fun MemberAccountsDto.toDomainModel(): MemberAccounts = MemberAccounts(
    savingsBalance = savingsAccounts.sumOf { it.balance },
    savingsHistory = emptyList(),
    activeLoan = loanAccounts.firstOrNull()?.toActiveLoanSummary(),
)

/**
 * Derives [ActiveLoanSummary] from a single `loanAccounts[]` row. [ActiveLoanSummary.inArrears]
 * = `summary.totalOverdue > 0.0`; [ActiveLoanSummary.dueDate] has NO wire source on
 * `MemberLoanAccountDto`/`.summary` (confirmed gap) and always maps to `null`.
 */
fun MemberLoanAccountDto.toActiveLoanSummary(): ActiveLoanSummary = ActiveLoanSummary(
    id = id,
    productName = productName,
    outstandingBalance = summary?.principalOutstanding ?: 0.0,
    inArrears = (summary?.totalOverdue ?: 0.0) > 0.0,
    dueDate = null,
)

fun MemberRoleInfoDto.toDomainModel(): MemberRoleInfo = MemberRoleInfo(
    role = role.toDomainModel(),
    groupId = groupId,
    assignedDate = assignedDate,
)

/** Batch converter — maps every `get_member_role` datatable row in declaration order. */
@JvmName("memberRoleInfoDtoListToDomainModels")
fun List<MemberRoleInfoDto>.toDomainModels(): List<MemberRoleInfo> = map { it.toDomainModel() }

/**
 * Domain -> DTO reverse mapper for [MemberRole] — NOT declared in `MemberMappers.kt` (member-list
 * only needs the DTO -> domain direction); this feature's `update_member_role` write op needs
 * domain -> DTO, so it is added here.
 */
fun MemberRole.toDto(): MemberRoleDto = when (this) {
    MemberRole.CHAIRPERSON -> MemberRoleDto.CHAIRPERSON
    MemberRole.TREASURER -> MemberRoleDto.TREASURER
    MemberRole.SECRETARY -> MemberRoleDto.SECRETARY
    MemberRole.MEMBER -> MemberRoleDto.MEMBER
    MemberRole.UNKNOWN -> MemberRoleDto.UNKNOWN
}

/** Domain -> DTO — the `update_member_role` PUT request body. */
fun UpdateMemberRoleRequest.toDto(): UpdateMemberRoleRequestDto = UpdateMemberRoleRequestDto(
    role = role.toDto(),
    groupId = groupId,
    assignedDate = assignedDate,
)

fun UpdateMemberRoleResponseDto.toDomainModel(): UpdateMemberRoleResult = UpdateMemberRoleResult(
    resourceId = resourceId,
)
