/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.mapper

import org.mifos.groupbanking.core.model.LoanStatus
import org.mifos.groupbanking.core.model.Member
import org.mifos.groupbanking.core.model.MemberPage
import org.mifos.groupbanking.core.model.MemberRole
import org.mifos.groupbanking.core.network.model.LoanStatusDto
import org.mifos.groupbanking.core.network.model.MemberDto
import org.mifos.groupbanking.core.network.model.MemberPageDto
import org.mifos.groupbanking.core.network.model.MemberRoleDto
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
