/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.personalloans

import androidx.compose.runtime.Composable
import kpt.core.designsystem.theme.KptTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider
import org.mifos.groupbanking.core.model.LoanAccountStatus
import org.mifos.groupbanking.core.model.LoanStatusFilter
import org.mifos.groupbanking.core.model.LoanSummary
import org.mifos.groupbanking.feature.personalloans.components.PersonalLoansCard
import org.mifos.groupbanking.feature.personalloans.components.PersonalLoansCardSkeleton
import org.mifos.groupbanking.feature.personalloans.components.PersonalLoansFilterChips

/**
 * `@Preview` gallery for `PersonalLoansScreen.kt`. See API.md#preview. Data source:
 * `demo-data.yaml` (3 `LoanDto` rows for `clientId: 101`) — the rows below use the ACTUAL field
 * values from `entries[0].items[]` (RULE-PREVIEW-7 — no placeholder literals), mapped onto the
 * canonical [LoanSummary] domain shape (member identity from `personal-dashboard/demo-data.yaml`'s
 * `memberName: "Amina Wanjiru"`, since `personal-loans` is the signed-in member's OWN loan list).
 * Covers ACTIVE-with-overdue (5001), CLOSED (4001), and PENDING (6001) — every status bucket the
 * seed data exercises.
 */
private val previewLoans: List<LoanSummary> = listOf(
    LoanSummary(
        id = 5001,
        memberId = 101,
        memberName = "Amina Wanjiru",
        memberPhotoUrl = null,
        loanProductName = "Group Emergency Loan",
        principalAmount = 5000.0,
        outstandingBalance = 3250.0,
        overdueAmount = 150.0,
        status = LoanAccountStatus.ACTIVE,
        nextRepaymentDate = "2026-05-20",
        isOverdue = true,
        fineractLoanId = 5001,
    ),
    LoanSummary(
        id = 4001,
        memberId = 101,
        memberName = "Amina Wanjiru",
        memberPhotoUrl = null,
        loanProductName = "Seasonal Harvest Loan",
        principalAmount = 8000.0,
        outstandingBalance = 0.0,
        overdueAmount = 0.0,
        status = LoanAccountStatus.CLOSED,
        nextRepaymentDate = null,
        isOverdue = false,
        fineractLoanId = 4001,
    ),
    LoanSummary(
        id = 6001,
        memberId = 101,
        memberName = "Amina Wanjiru",
        memberPhotoUrl = null,
        loanProductName = "Group Emergency Loan",
        principalAmount = 4000.0,
        outstandingBalance = 0.0,
        overdueAmount = 0.0,
        status = LoanAccountStatus.PENDING,
        nextRepaymentDate = null,
        isOverdue = false,
        fineractLoanId = 6001,
    ),
)

private class PersonalLoansStatePreviewProvider : PreviewParameterProvider<PersonalLoansState> {
    override val values: Sequence<PersonalLoansState> = sequenceOf(
        // loading — spinner + skeleton rows, no loans yet
        PersonalLoansState(clientId = 101L, isLoading = true, loans = emptyList(), filteredLoans = emptyList(), error = null),
        // content — all 3 seeded loans, no card expanded
        PersonalLoansState(
            clientId = 101L,
            isLoading = false,
            loans = previewLoans,
            filteredLoans = previewLoans,
            filterStatus = LoanStatusFilter.ALL,
            selectedLoanId = null,
            error = null,
        ),
        // content — ACTIVE filter applied, loan 5001 expanded (repayment-schedule-gap details panel visible)
        PersonalLoansState(
            clientId = 101L,
            isLoading = false,
            loans = previewLoans,
            filteredLoans = previewLoans.filter { it.status == LoanAccountStatus.ACTIVE },
            filterStatus = LoanStatusFilter.ACTIVE,
            selectedLoanId = 5001L,
            error = null,
        ),
        // empty — genuinely zero loans (ui.yaml Empty state)
        PersonalLoansState(clientId = 101L, isLoading = false, loans = emptyList(), filteredLoans = emptyList(), error = null),
        // error — network failure (ui.yaml Error state)
        PersonalLoansState(
            clientId = 101L,
            isLoading = false,
            loans = emptyList(),
            filteredLoans = emptyList(),
            error = LoanError.Network,
        ),
    )
}

@Preview
@Composable
private fun PersonalLoansContentPreview(
    @PreviewParameter(PersonalLoansStatePreviewProvider::class)
    state: PersonalLoansState,
) {
    KptTheme {
        PersonalLoansContent(state = state, onAction = {}, onNavigateBack = {})
    }
}

@Preview
@Composable
private fun PersonalLoansFabContentPreview() {
    KptTheme {
        PersonalLoansFabContent(fabCd = "Request a new loan", fabLabel = "Request Loan")
    }
}

@Preview
@Composable
private fun PersonalLoansLoadingSectionPreview() {
    KptTheme {
        PersonalLoansLoadingSection(filterStatus = LoanStatusFilter.ALL, onAction = {})
    }
}

@Preview
@Composable
private fun PersonalLoansContentSectionPreview() {
    KptTheme {
        PersonalLoansContentSection(
            state = PersonalLoansState(
                clientId = 101L,
                isLoading = false,
                loans = previewLoans,
                filteredLoans = previewLoans,
                filterStatus = LoanStatusFilter.ALL,
                error = null,
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun PersonalLoansEmptySectionPreview() {
    KptTheme {
        PersonalLoansEmptySection(onAction = {})
    }
}

@Preview
@Composable
private fun PersonalLoansErrorSectionPreview() {
    KptTheme {
        PersonalLoansErrorSection(error = LoanError.Network, onRetry = {})
    }
}

@Preview
@Composable
private fun PersonalLoansErrorSectionUnauthorizedPreview() {
    // LoanError.Unauthorized has retry = false — Retry button is hidden.
    KptTheme {
        PersonalLoansErrorSection(error = LoanError.Unauthorized, onRetry = {})
    }
}

@Preview
@Composable
private fun PersonalLoansCardPreview() {
    KptTheme {
        PersonalLoansCard(loan = previewLoans[0], isExpanded = false, onClick = {})
    }
}

@Preview
@Composable
private fun PersonalLoansCardExpandedPreview() {
    KptTheme {
        PersonalLoansCard(loan = previewLoans[0], isExpanded = true, onClick = {})
    }
}

@Preview
@Composable
private fun PersonalLoansCardClosedPreview() {
    KptTheme {
        PersonalLoansCard(loan = previewLoans[1], isExpanded = false, onClick = {})
    }
}

@Preview
@Composable
private fun PersonalLoansCardSkeletonPreview() {
    KptTheme {
        PersonalLoansCardSkeleton()
    }
}

@Preview
@Composable
private fun PersonalLoansFilterChipsPreview() {
    KptTheme {
        PersonalLoansFilterChips(selectedFilter = LoanStatusFilter.ACTIVE, onFilterChange = {})
    }
}
