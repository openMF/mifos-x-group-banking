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

import org.mifos.groupbanking.core.model.LoanSummaryItem
import org.mifos.groupbanking.core.model.MeetingSummaryData
import org.mifos.groupbanking.core.model.SavingsBreakdownItem
import org.mifos.groupbanking.core.network.model.LoanSummaryItemDto
import org.mifos.groupbanking.core.network.model.MeetingSummaryRecordDto
import org.mifos.groupbanking.core.network.model.SavingsBreakdownItemDto
import kotlin.jvm.JvmName

/**
 * DTO -> domain mappers for the meeting-record wire contract
 * (`GET /datatables/dt_meeting_record/{centerId}`). Every field on `MeetingRecordDetailDto` /
 * `SavingsBreakdownItemDto` / `LoanSummaryItemDto` is mapped — no field left unmapped.
 */

fun SavingsBreakdownItemDto.toDomainModel(): SavingsBreakdownItem = SavingsBreakdownItem(
    memberId = memberId,
    memberName = memberName,
    groupSavings = groupSavings,
    individualSavings = individualSavings,
)

/** Batch converter — maps every breakdown row in declaration order. */
@JvmName("savingsBreakdownItemDtoListToDomainModels")
fun List<SavingsBreakdownItemDto>.toDomainModels(): List<SavingsBreakdownItem> = map { it.toDomainModel() }

fun LoanSummaryItemDto.toDomainModel(): LoanSummaryItem = LoanSummaryItem(
    memberId = memberId,
    memberName = memberName,
    amountDisbursed = amountDisbursed,
    amountRepaid = amountRepaid,
    outstandingAfter = outstandingAfter,
)

/** Batch converter — maps every loan-activity row in declaration order. */
@JvmName("loanSummaryItemDtoListToDomainModels")
fun List<LoanSummaryItemDto>.toDomainModels(): List<LoanSummaryItem> = map { it.toDomainModel() }

/**
 * Composite converter for the single `GET /datatables/dt_meeting_record/{centerId}` payload — maps
 * the scalar meeting totals plus both nested per-member collections.
 */
fun MeetingSummaryRecordDto.toDomainModel(): MeetingSummaryData = MeetingSummaryData(
    meetingId = meetingId,
    meetingNumber = meetingNumber,
    actualDate = actualDate,
    attendanceCount = attendanceCount,
    totalMemberCount = totalMemberCount,
    groupSavingsCollected = groupSavingsCollected,
    individualSavingsCollected = individualSavingsCollected,
    totalSavingsCollected = totalSavingsCollected,
    loansDisbursed = loansDisbursed,
    loansRepaid = loansRepaid,
    finesCollected = finesCollected,
    openingCorpus = openingCorpus,
    closingCorpus = closingCorpus,
    savingsBreakdown = savingsBreakdown.toDomainModels(),
    loanItems = loanItems.toDomainModels(),
)
