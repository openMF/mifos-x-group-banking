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

import kpt.core.model.LoanStatus
import kpt.core.model.Member
import kpt.core.model.MemberPage
import kpt.core.model.MemberRole
import kpt.core.network.model.LoanStatusDto
import kpt.core.network.model.MemberDto
import kpt.core.network.model.MemberPageDto
import kpt.core.network.model.MemberRoleDto
import kotlin.jvm.JvmName

/**
 * DTO -> domain mappers for the member-list wire contract (`GET /groups/{groupId}/clients`).
 * Every field on `MemberDto` declared in `MemberDto.kt` is mapped — no field left unmapped.
 */

fun MemberDto.toDomainModel(): Member = Member(
    id = id,
    fineractClientId = fineractClientId,
    displayName = displayName,
    photoUri = photoUri,
    role = role.toDomainModel(),
    savingsBalance = savingsBalance,
    loanStatus = loanStatus.toDomainModel(),
)

/** Batch converter — maps every member row in declaration order. */
@JvmName("memberDtoListToDomainModels")
fun List<MemberDto>.toDomainModels(): List<Member> = map { it.toDomainModel() }

/** Page converter — maps the offset-paginated envelope, preserving `totalFilteredRecords`. */
fun MemberPageDto.toDomainModel(): MemberPage = MemberPage(
    totalFilteredRecords = totalFilteredRecords,
    members = pageItems.toDomainModels(),
)

fun MemberRoleDto.toDomainModel(): MemberRole = when (this) {
    MemberRoleDto.CHAIRPERSON -> MemberRole.CHAIRPERSON
    MemberRoleDto.TREASURER -> MemberRole.TREASURER
    MemberRoleDto.SECRETARY -> MemberRole.SECRETARY
    MemberRoleDto.MEMBER -> MemberRole.MEMBER
    MemberRoleDto.UNKNOWN -> MemberRole.UNKNOWN
}

fun LoanStatusDto.toDomainModel(): LoanStatus = when (this) {
    LoanStatusDto.ACTIVE -> LoanStatus.ACTIVE
    LoanStatusDto.NONE -> LoanStatus.NONE
    LoanStatusDto.OVERDUE -> LoanStatus.OVERDUE
    LoanStatusDto.UNKNOWN -> LoanStatus.UNKNOWN
}
