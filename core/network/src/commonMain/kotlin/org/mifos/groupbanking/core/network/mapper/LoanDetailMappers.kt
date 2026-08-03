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

import org.mifos.groupbanking.core.model.LoanDetail
import org.mifos.groupbanking.core.model.LoanDetailResponse
import org.mifos.groupbanking.core.model.RepaymentRowStatus
import org.mifos.groupbanking.core.model.RepaymentScheduleRow
import org.mifos.groupbanking.core.model.RepaymentTransaction
import org.mifos.groupbanking.core.network.model.LoanDetailDto
import org.mifos.groupbanking.core.network.model.LoanDetailResponseDto
import org.mifos.groupbanking.core.network.model.RepaymentRowStatusDto
import org.mifos.groupbanking.core.network.model.RepaymentScheduleRowDto
import org.mifos.groupbanking.core.network.model.RepaymentTransactionDto
import kotlin.jvm.JvmName

/**
 * DTO -> domain mappers for the loan-detail wire contract (`GET
 * /loans/{loanId}`). Every field on `LoanDetailDto` / `RepaymentScheduleRowDto`
 * / `RepaymentTransactionDto` / `LoanDetailResponseDto` declared in
 * `LoanDetailDto.kt` is mapped — no field left unmapped. Reuses
 * `LoanAccountStatusDto.toDomainModel()` (declared in `LoanSummaryMappers.kt`,
 * loan-list) for [LoanDetailDto.status] — no duplicate mapper introduced.
 */

fun LoanDetailDto.toDomainModel(): LoanDetail = LoanDetail(
    id = id,
    memberId = memberId,
    memberName = memberName,
    loanProductName = loanProductName,
    principalAmount = principalAmount,
    disbursedDate = disbursedDate,
    interestRatePercent = interestRatePercent,
    totalOutstanding = totalOutstanding,
    totalOverdue = totalOverdue,
    status = status.toDomainModel(),
    fineractLoanId = fineractLoanId,
)

fun RepaymentScheduleRowDto.toDomainModel(): RepaymentScheduleRow = RepaymentScheduleRow(
    weekNumber = weekNumber,
    dueDate = dueDate,
    dueAmount = dueAmount,
    paidAmount = paidAmount,
    balance = balance,
    status = status.toDomainModel(),
)

/** Batch converter — maps every schedule row in declaration order. */
@JvmName("repaymentScheduleRowDtoListToDomainModels")
fun List<RepaymentScheduleRowDto>.toDomainModels(): List<RepaymentScheduleRow> = map { it.toDomainModel() }

fun RepaymentTransactionDto.toDomainModel(): RepaymentTransaction = RepaymentTransaction(
    id = id,
    type = type,
    date = date,
    amount = amount,
)

/** Batch converter — maps every transaction row in declaration order. */
@JvmName("repaymentTransactionDtoListToDomainModels")
fun List<RepaymentTransactionDto>.toDomainModels(): List<RepaymentTransaction> = map { it.toDomainModel() }

/**
 * Composite converter for the single `GET /loans/{loanId}` envelope — maps
 * [LoanDetailResponseDto.loan] + [LoanDetailResponseDto.repaymentSchedule],
 * and renames the wire's `transactions` to domain [LoanDetailResponse.repaymentHistory].
 */
fun LoanDetailResponseDto.toDomainModel(): LoanDetailResponse = LoanDetailResponse(
    loan = loan.toDomainModel(),
    repaymentSchedule = repaymentSchedule.toDomainModels(),
    repaymentHistory = transactions.toDomainModels(),
)

fun RepaymentRowStatusDto.toDomainModel(): RepaymentRowStatus = when (this) {
    RepaymentRowStatusDto.PAID -> RepaymentRowStatus.PAID
    RepaymentRowStatusDto.PARTIAL -> RepaymentRowStatus.PARTIAL
    RepaymentRowStatusDto.UPCOMING -> RepaymentRowStatus.UPCOMING
    RepaymentRowStatusDto.OVERDUE -> RepaymentRowStatus.OVERDUE
    RepaymentRowStatusDto.UNKNOWN -> RepaymentRowStatus.UNKNOWN
}
