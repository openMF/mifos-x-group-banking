/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.loanlist

import androidx.compose.runtime.Composable
import kpt.core.designsystem.theme.KptTheme
import kpt.core.model.LoanAccountStatus
import kpt.core.model.LoanStatusFilter
import kpt.core.model.LoanSummary
import kpt.feature.loanlist.components.LoanListCard
import kpt.feature.loanlist.components.LoanListCardSkeleton
import kpt.feature.loanlist.components.LoanListFilterChips
import kpt.feature.loanlist.generated.resources.Res
import kpt.feature.loanlist.generated.resources.screens_loan_list_error_network_message
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider

/**
 * `@Preview` gallery for `LoanListScreen.kt`. See API.md#preview. Data source: `demo-data.yaml`
 * (12 `LoanSummary` rows for Mwangaza Women's Group, Kisumu West Branch) — the rows below use the
 * ACTUAL field values from `entries[0].items[]` (RULE-PREVIEW-7 — no placeholder literals),
 * covering all four `LoanStatusFilter` buckets.
 */
private val previewLoans: List<LoanSummary> = listOf(
    LoanSummary(
        id = 5001,
        memberId = 1001,
        memberName = "Amina Wangari",
        memberPhotoUrl = null,
        loanProductName = "Group Solidarity Loan",
        principalAmount = 8000.0,
        outstandingBalance = 5600.0,
        overdueAmount = 0.0,
        status = LoanAccountStatus.ACTIVE,
        nextRepaymentDate = "2026-07-25",
        isOverdue = false,
        fineractLoanId = 5001,
    ),
    LoanSummary(
        id = 5002,
        memberId = 1002,
        memberName = "Joseph Otieno",
        memberPhotoUrl = null,
        loanProductName = "Group Solidarity Loan",
        principalAmount = 12000.0,
        outstandingBalance = 9600.0,
        overdueAmount = 600.0,
        status = LoanAccountStatus.OVERDUE,
        nextRepaymentDate = "2026-07-18",
        isOverdue = true,
        fineractLoanId = 5002,
    ),
    LoanSummary(
        id = 5004,
        memberId = 1004,
        memberName = "Peter Kamau",
        memberPhotoUrl = null,
        loanProductName = "Group Solidarity Loan",
        principalAmount = 6000.0,
        outstandingBalance = 0.0,
        overdueAmount = 0.0,
        status = LoanAccountStatus.CLOSED,
        nextRepaymentDate = null,
        isOverdue = false,
        fineractLoanId = 5004,
    ),
    LoanSummary(
        id = 5011,
        memberId = 1011,
        memberName = "John Mwangi",
        memberPhotoUrl = null,
        loanProductName = "Group Solidarity Loan",
        principalAmount = 10000.0,
        outstandingBalance = 10000.0,
        overdueAmount = 0.0,
        status = LoanAccountStatus.PENDING,
        nextRepaymentDate = null,
        isOverdue = false,
        fineractLoanId = 5011,
    ),
)

private class LoanListStatePreviewProvider : PreviewParameterProvider<LoanListState> {
    override val values: Sequence<LoanListState> = sequenceOf(
        // loading — spinner + skeleton rows, no loans yet
        LoanListState(isLoading = true, loans = emptyList(), filteredLoans = emptyList(), error = null, groupId = 1L),
        // content — the 4 seeded loans from demo-data.yaml (all four status buckets)
        LoanListState(
            isLoading = false,
            loans = previewLoans,
            filteredLoans = previewLoans,
            selectedFilter = LoanStatusFilter.ALL,
            error = null,
            groupId = 1L,
            canApplyLoan = true,
        ),
        // empty — genuinely zero loans for the selected filter (ui.yaml Empty state)
        LoanListState(isLoading = false, loans = emptyList(), filteredLoans = emptyList(), error = null, groupId = 1L),
        // error — network failure (ui.yaml Error state)
        LoanListState(
            isLoading = false,
            loans = emptyList(),
            filteredLoans = emptyList(),
            error = LoanListError.Network,
            groupId = 1L,
        ),
    )
}

@Preview
@Composable
private fun LoanListContentPreview(
    @PreviewParameter(LoanListStatePreviewProvider::class)
    state: LoanListState,
) {
    KptTheme {
        LoanListContent(state = state, onAction = {}, onNavigateBack = {})
    }
}

@Preview
@Composable
private fun LoanListFabContentPreview() {
    KptTheme {
        LoanListFabContent(fabCd = "Apply for a new group loan")
    }
}

@Preview
@Composable
private fun LoanListLoadingSectionPreview() {
    KptTheme {
        LoanListLoadingSection(selectedFilter = LoanStatusFilter.ALL, onAction = {})
    }
}

@Preview
@Composable
private fun LoanListContentSectionPreview() {
    KptTheme {
        LoanListContentSection(
            state = LoanListState(
                isLoading = false,
                loans = previewLoans,
                filteredLoans = previewLoans,
                selectedFilter = LoanStatusFilter.ALL,
                error = null,
                groupId = 1L,
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun LoanListEmptySectionPreview() {
    KptTheme {
        LoanListEmptySection(selectedFilter = LoanStatusFilter.OVERDUE, onAction = {})
    }
}

@Preview
@Composable
private fun LoanListErrorSectionPreview() {
    KptTheme {
        LoanListErrorSection(
            message = stringResource(Res.string.screens_loan_list_error_network_message),
            onRetry = {},
        )
    }
}

@Preview
@Composable
private fun LoanListCardPreview() {
    KptTheme {
        LoanListCard(loan = previewLoans[0], onClick = {})
    }
}

@Preview
@Composable
private fun LoanListCardOverduePreview() {
    KptTheme {
        LoanListCard(loan = previewLoans[1], onClick = {})
    }
}

@Preview
@Composable
private fun LoanListCardSkeletonPreview() {
    KptTheme {
        LoanListCardSkeleton()
    }
}

@Preview
@Composable
private fun LoanListFilterChipsPreview() {
    KptTheme {
        LoanListFilterChips(selectedFilter = LoanStatusFilter.ACTIVE, onFilterChange = {})
    }
}
