/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.mapper

import org.mifos.groupbanking.core.model.LoanAccountStatus
import org.mifos.groupbanking.core.model.LoanPage
import org.mifos.groupbanking.core.model.LoanSummary
import org.mifos.groupbanking.core.network.model.LoanAccountStatusDto
import org.mifos.groupbanking.core.network.model.LoanPageDto
import org.mifos.groupbanking.core.network.model.LoanSummaryDto
import kotlin.jvm.JvmName

/**
 * DTO -> domain mappers for the loan-list wire contract (`GET
 * /groups/{groupId}/loans`). Every field on `LoanSummaryDto` declared in
 * `LoanSummaryDto.kt` is mapped — no field left unmapped.
 */

fun LoanSummaryDto.toDomainModel(): LoanSummary = LoanSummary(
    id = id,
    memberId = memberId,
    memberName = memberName,
    memberPhotoUrl = memberPhotoUrl,
    loanProductName = loanProductName,
    principalAmount = principalAmount,
    outstandingBalance = outstandingBalance,
    overdueAmount = overdueAmount,
    status = status.toDomainModel(),
    nextRepaymentDate = nextRepaymentDate,
    isOverdue = isOverdue,
    fineractLoanId = fineractLoanId,
)

/** Batch converter — maps every loan row in declaration order. */
@JvmName("loanSummaryDtoListToDomainModels")
fun List<LoanSummaryDto>.toDomainModels(): List<LoanSummary> = map { it.toDomainModel() }

/** Page converter — maps the offset-paginated envelope, preserving `totalFilteredRecords`. */
fun LoanPageDto.toDomainModel(): LoanPage = LoanPage(
    totalFilteredRecords = totalFilteredRecords,
    loans = pageItems.toDomainModels(),
)

fun LoanAccountStatusDto.toDomainModel(): LoanAccountStatus = when (this) {
    LoanAccountStatusDto.ACTIVE -> LoanAccountStatus.ACTIVE
    LoanAccountStatusDto.OVERDUE -> LoanAccountStatus.OVERDUE
    LoanAccountStatusDto.CLOSED -> LoanAccountStatus.CLOSED
    LoanAccountStatusDto.PENDING -> LoanAccountStatus.PENDING
    LoanAccountStatusDto.REJECTED -> LoanAccountStatus.REJECTED
    LoanAccountStatusDto.UNKNOWN -> LoanAccountStatus.UNKNOWN
}
