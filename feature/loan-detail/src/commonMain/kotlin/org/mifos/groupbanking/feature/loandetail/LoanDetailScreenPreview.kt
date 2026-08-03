/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.loandetail

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import kpt.core.designsystem.theme.KptTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider
import org.mifos.groupbanking.core.model.LoanAccountStatus
import org.mifos.groupbanking.core.model.LoanDetail
import org.mifos.groupbanking.core.model.LoanDetailTab
import org.mifos.groupbanking.core.model.RepaymentRowStatus
import org.mifos.groupbanking.core.model.RepaymentScheduleRow
import org.mifos.groupbanking.core.model.RepaymentTransaction
import org.mifos.groupbanking.feature.loandetail.components.LoanActionButtonsRow
import org.mifos.groupbanking.feature.loandetail.components.LoanDetailTabs
import org.mifos.groupbanking.feature.loandetail.components.LoanHeaderCard
import org.mifos.groupbanking.feature.loandetail.components.LoanOutstandingSummaryRow
import org.mifos.groupbanking.feature.loandetail.components.LoanStatusBadge
import org.mifos.groupbanking.feature.loandetail.components.RepaymentHistoryEmptyState
import org.mifos.groupbanking.feature.loandetail.components.RepaymentScheduleHeaderRow
import org.mifos.groupbanking.feature.loandetail.components.RepaymentScheduleRowItem
import org.mifos.groupbanking.feature.loandetail.components.RepaymentTransactionRow

/**
 * `@Preview` gallery for `LoanDetailScreen.kt`. See API.md#preview. Data source:
 * `idea-layer/screens/loan-detail/demo-data.yaml#entries` (Grace Akinyi's KES 8,000 Group
 * Solidarity Loan, `fineractLoanId 401`) — `demo_data_resolved = true`, so every field below is
 * the actual seeded value, never a generic placeholder literal (RULE-PREVIEW-7).
 *
 * [overdueLoan] is a synthesized `.copy(status = OVERDUE, ...)` variant, NOT a second demo-data
 * entry (`demo-data.yaml` seeds only one ACTIVE loan) — needed so the OVERDUE colour-coded badge
 * and the Mark-Defaulted action row (`canMarkDefaulted`) are also design-covered, mirrors
 * `GroupDashboardScreenPreview`'s `isCorpusInsufficient = true` / `MemberProfileScreenPreview`'s
 * `isCurrentUserChairperson = true` identical "flag a role/state variant that has no dedicated
 * demo-data row" convention.
 */
private val loan = LoanDetail(
    id = 401L,
    memberId = 303L,
    memberName = "Grace Akinyi",
    loanProductName = "Group Solidarity Loan",
    principalAmount = 8000.00,
    disbursedDate = "2026-05-05",
    interestRatePercent = 0.83,
    totalOutstanding = 7034.00,
    totalOverdue = 0.00,
    status = LoanAccountStatus.ACTIVE,
    fineractLoanId = 401L,
)

private val overdueLoan = loan.copy(
    status = LoanAccountStatus.OVERDUE,
    totalOverdue = 1058.65,
)

private val repaymentSchedule: List<RepaymentScheduleRow> = listOf(
    RepaymentScheduleRow(weekNumber = 1, dueDate = "2026-05-12", dueAmount = 1066.40, paidAmount = 1066.40, balance = 6933.60, status = RepaymentRowStatus.PAID),
    RepaymentScheduleRow(weekNumber = 2, dueDate = "2026-05-19", dueAmount = 1058.65, paidAmount = 0.00, balance = 5874.95, status = RepaymentRowStatus.UPCOMING),
    RepaymentScheduleRow(weekNumber = 3, dueDate = "2026-05-26", dueAmount = 1050.84, paidAmount = 0.00, balance = 4824.11, status = RepaymentRowStatus.UPCOMING),
    RepaymentScheduleRow(weekNumber = 4, dueDate = "2026-06-02", dueAmount = 1042.98, paidAmount = 0.00, balance = 3781.13, status = RepaymentRowStatus.UPCOMING),
    RepaymentScheduleRow(weekNumber = 5, dueDate = "2026-06-09", dueAmount = 1035.06, paidAmount = 0.00, balance = 2746.07, status = RepaymentRowStatus.UPCOMING),
    RepaymentScheduleRow(weekNumber = 6, dueDate = "2026-06-16", dueAmount = 1027.09, paidAmount = 0.00, balance = 1718.98, status = RepaymentRowStatus.UPCOMING),
    RepaymentScheduleRow(weekNumber = 7, dueDate = "2026-06-23", dueAmount = 1019.07, paidAmount = 0.00, balance = 699.91, status = RepaymentRowStatus.UPCOMING),
    RepaymentScheduleRow(weekNumber = 8, dueDate = "2026-06-30", dueAmount = 705.73, paidAmount = 0.00, balance = 0.00, status = RepaymentRowStatus.UPCOMING),
)

private val repaymentHistory: List<RepaymentTransaction> = listOf(
    RepaymentTransaction(id = 5001L, type = "DISBURSEMENT", date = "2026-05-05", amount = 8000.00),
    RepaymentTransaction(id = 5002L, type = "REPAYMENT", date = "2026-05-12", amount = 1066.40),
)

private val contentScheduleState = LoanDetailState(
    isLoading = false,
    loan = loan,
    repaymentSchedule = repaymentSchedule,
    repaymentHistory = repaymentHistory,
    selectedTab = LoanDetailTab.SCHEDULE,
    canRecordRepayment = true,
    canMarkDefaulted = false,
)

private val contentHistoryState = contentScheduleState.copy(selectedTab = LoanDetailTab.HISTORY)

private val overdueContentState = LoanDetailState(
    isLoading = false,
    loan = overdueLoan,
    repaymentSchedule = repaymentSchedule,
    repaymentHistory = repaymentHistory,
    selectedTab = LoanDetailTab.SCHEDULE,
    canRecordRepayment = false,
    canMarkDefaulted = true,
)

private val networkErrorState = LoanDetailState(isLoading = false, error = LoanDetailError.Network)

private class LoanDetailStatePreviewProvider : PreviewParameterProvider<LoanDetailState> {
    override val values: Sequence<LoanDetailState> = sequenceOf(
        // loading — companion fetch in-flight
        LoanDetailState(isLoading = true),
        // content — Schedule tab, ACTIVE loan, Record Repayment visible
        contentScheduleState,
        // content — Repayment History tab
        contentHistoryState,
        // content — OVERDUE loan, Mark Defaulted visible
        overdueContentState,
        // error — network failure
        networkErrorState,
    )
}

@Preview
@Composable
private fun LoanDetailContentPreview(
    @PreviewParameter(LoanDetailStatePreviewProvider::class)
    state: LoanDetailState,
) {
    KptTheme {
        LoanDetailContent(state = state, onAction = {})
    }
}

@Preview
@Composable
private fun LoanDetailLoadingSectionPreview() {
    KptTheme {
        LoanDetailLoadingSection()
    }
}

@Preview
@Composable
private fun LoanDetailSkeletonBlockPreview() {
    KptTheme {
        LoanDetailSkeletonBlock(height = 120.dp)
    }
}

@Preview
@Composable
private fun LoanDetailContentSectionSchedulePreview() {
    KptTheme {
        LoanDetailContentSection(state = contentScheduleState, onAction = {})
    }
}

@Preview
@Composable
private fun LoanDetailContentSectionHistoryPreview() {
    KptTheme {
        LoanDetailContentSection(state = contentHistoryState, onAction = {})
    }
}

@Preview
@Composable
private fun LoanDetailContentSectionOverduePreview() {
    KptTheme {
        LoanDetailContentSection(state = overdueContentState, onAction = {})
    }
}

@Preview
@Composable
private fun LoanDetailErrorSectionPreview() {
    KptTheme {
        LoanDetailErrorSection(state = networkErrorState, onAction = {})
    }
}

@Preview
@Composable
private fun LoanHeaderCardPreview() {
    KptTheme {
        LoanHeaderCard(loan = loan)
    }
}

@Preview
@Composable
private fun LoanHeaderCardOverduePreview() {
    KptTheme {
        LoanHeaderCard(loan = overdueLoan)
    }
}

@Preview
@Composable
private fun LoanStatusBadgePreview() {
    KptTheme {
        LoanStatusBadge(status = LoanAccountStatus.ACTIVE)
    }
}

@Preview
@Composable
private fun LoanOutstandingSummaryRowPreview() {
    KptTheme {
        LoanOutstandingSummaryRow(loan = overdueLoan)
    }
}

@Preview
@Composable
private fun LoanDetailTabsPreview() {
    KptTheme {
        LoanDetailTabs(selectedTab = LoanDetailTab.SCHEDULE, onTabSelected = {})
    }
}

@Preview
@Composable
private fun RepaymentScheduleHeaderRowPreview() {
    KptTheme {
        RepaymentScheduleHeaderRow()
    }
}

@Preview
@Composable
private fun RepaymentScheduleRowItemPreview() {
    KptTheme {
        RepaymentScheduleRowItem(row = repaymentSchedule.first())
    }
}

@Preview
@Composable
private fun RepaymentTransactionRowPreview() {
    KptTheme {
        RepaymentTransactionRow(txn = repaymentHistory[1])
    }
}

@Preview
@Composable
private fun RepaymentHistoryEmptyStatePreview() {
    KptTheme {
        RepaymentHistoryEmptyState()
    }
}

@Preview
@Composable
private fun LoanActionButtonsRowRecordPreview() {
    KptTheme {
        LoanActionButtonsRow(loan = loan, canRecordRepayment = true, canMarkDefaulted = false, onAction = {})
    }
}

@Preview
@Composable
private fun LoanActionButtonsRowDefaultPreview() {
    KptTheme {
        LoanActionButtonsRow(loan = overdueLoan, canRecordRepayment = false, canMarkDefaulted = true, onAction = {})
    }
}
