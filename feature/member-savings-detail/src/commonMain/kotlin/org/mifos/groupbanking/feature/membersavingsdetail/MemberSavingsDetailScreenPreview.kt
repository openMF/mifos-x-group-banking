/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.membersavingsdetail

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import kpt.core.designsystem.theme.KptTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider
import org.mifos.groupbanking.core.model.SavingsDataPoint
import org.mifos.groupbanking.core.model.SavingsMember
import org.mifos.groupbanking.core.model.SavingsStatementEntry
import org.mifos.groupbanking.core.model.SavingsTransactionFilter
import org.mifos.groupbanking.core.model.SavingsTransactionType
import org.mifos.groupbanking.feature.membersavingsdetail.components.MemberSavingsHeaderCard
import org.mifos.groupbanking.feature.membersavingsdetail.components.SavingsFilterChips
import org.mifos.groupbanking.feature.membersavingsdetail.components.SavingsSparklineCard
import org.mifos.groupbanking.feature.membersavingsdetail.components.SavingsStatementRow

/**
 * `@Preview` gallery for `MemberSavingsDetailScreen.kt`. See API.md#preview. Data source:
 * `idea-layer/screens/member-savings-detail/ui.yaml#states.content.demo_data` (Amina Wanjiru,
 * `grp-001`, SHARE_BASED_VARIABLE) — `demo_data_resolved = true`, so every value below is the
 * ACTUAL seeded row, never a generic placeholder literal (RULE-PREVIEW-7).
 */
private val aminaSavingsMember = SavingsMember(
    memberId = "client_101",
    displayName = "Amina Wanjiru",
    photoUri = null,
)

private val previewSparkline: List<SavingsDataPoint> = listOf(
    SavingsDataPoint(date = "2025-12-01", balance = 500.00),
    SavingsDataPoint(date = "2026-01-01", balance = 1000.00),
    SavingsDataPoint(date = "2026-02-01", balance = 1500.00),
    SavingsDataPoint(date = "2026-03-01", balance = 2500.00),
    SavingsDataPoint(date = "2026-04-01", balance = 3000.00),
    SavingsDataPoint(date = "2026-05-01", balance = 3500.00),
)

private val previewTransactions: List<SavingsStatementEntry> = listOf(
    SavingsStatementEntry(id = "1", date = LocalDate(2026, 5, 5), type = SavingsTransactionType.DEPOSIT, amount = 500.00, runningBalance = 3500.00, reversed = false),
    SavingsStatementEntry(id = "2", date = LocalDate(2026, 4, 28), type = SavingsTransactionType.DEPOSIT, amount = 500.00, runningBalance = 3000.00, reversed = false),
    SavingsStatementEntry(id = "3", date = LocalDate(2026, 4, 14), type = SavingsTransactionType.WITHDRAWAL, amount = 200.00, runningBalance = 2500.00, reversed = false),
    SavingsStatementEntry(id = "4", date = LocalDate(2026, 4, 7), type = SavingsTransactionType.DEPOSIT, amount = 700.00, runningBalance = 2700.00, reversed = false),
)

private val shareBasedContentState = MemberSavingsDetailState(
    memberId = "client_101",
    groupId = "grp-001",
    isLoading = false,
    member = aminaSavingsMember,
    contributionModel = "SHARE_BASED_VARIABLE",
    sharesHeld = 20,
    shareValue = 1000L,
    savingsBalance = 20000.00,
    savingsAccountNo = "SA-00012345",
    sparklineData = previewSparkline,
    transactions = previewTransactions,
    selectedFilter = SavingsTransactionFilter.ALL,
    filteredTransactions = previewTransactions,
    hasNextPage = true,
    currentOffset = 0,
)

private val fixedContentState = shareBasedContentState.copy(
    contributionModel = "FIXED",
    sharesHeld = null,
    shareValue = null,
    savingsBalance = 3500.00,
)

private class MemberSavingsDetailStatePreviewProvider : PreviewParameterProvider<MemberSavingsDetailState> {
    override val values: Sequence<MemberSavingsDetailState> = sequenceOf(
        // loading — companion fetch in-flight
        MemberSavingsDetailState(memberId = "client_101", groupId = "grp-001", isLoading = true),
        // content — SHARE_BASED_VARIABLE (VSLA/SILC): shares held header
        shareBasedContentState,
        // content — FIXED contribution model: plain savings-balance header
        fixedContentState,
        // content — next page currently loading (scroll-to-end in flight)
        shareBasedContentState.copy(isLoadingNextPage = true),
        // empty — filter (Withdrawals) matches nothing
        shareBasedContentState.copy(
            selectedFilter = SavingsTransactionFilter.WITHDRAWALS,
            filteredTransactions = emptyList(),
        ),
        // error — network failure, retryable
        MemberSavingsDetailState(memberId = "client_101", groupId = "grp-001", isLoading = false, error = MemberSavingsError.Network),
        // error — savings account not found, non-retryable (Retry button hidden)
        MemberSavingsDetailState(memberId = "client_101", groupId = "grp-001", isLoading = false, error = MemberSavingsError.NotFound),
    )
}

@Preview
@Composable
private fun MemberSavingsDetailContentPreview(
    @PreviewParameter(MemberSavingsDetailStatePreviewProvider::class)
    state: MemberSavingsDetailState,
) {
    KptTheme {
        MemberSavingsDetailContent(state = state, onAction = {})
    }
}

@Preview
@Composable
private fun MemberSavingsDetailLoadingSectionPreview() {
    KptTheme {
        MemberSavingsDetailLoadingSection()
    }
}

@Preview
@Composable
private fun MemberSavingsDetailSkeletonBlockPreview() {
    KptTheme {
        MemberSavingsDetailSkeletonBlock(height = 72.dp)
    }
}

@Preview
@Composable
private fun MemberSavingsDetailHeaderSectionPreview() {
    KptTheme {
        MemberSavingsDetailHeaderSection(state = shareBasedContentState, onAction = {})
    }
}

@Preview
@Composable
private fun MemberSavingsDetailContentSectionPreview() {
    KptTheme {
        MemberSavingsDetailContentSection(state = shareBasedContentState, onAction = {})
    }
}

@Preview
@Composable
private fun MemberSavingsDetailEmptySectionPreview() {
    KptTheme {
        MemberSavingsDetailEmptySection(
            state = shareBasedContentState.copy(selectedFilter = SavingsTransactionFilter.WITHDRAWALS, filteredTransactions = emptyList()),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun MemberSavingsDetailErrorSectionPreview() {
    KptTheme {
        MemberSavingsDetailErrorSection(
            state = MemberSavingsDetailState(memberId = "client_101", groupId = "grp-001", isLoading = false, error = MemberSavingsError.Network),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun MemberSavingsHeaderCardShareBasedPreview() {
    KptTheme {
        MemberSavingsHeaderCard(
            member = aminaSavingsMember,
            contributionModel = "SHARE_BASED_VARIABLE",
            sharesHeld = 20,
            shareValue = 1000L,
            savingsBalance = 20000.00,
            savingsAccountNo = "SA-00012345",
        )
    }
}

@Preview
@Composable
private fun MemberSavingsHeaderCardFixedPreview() {
    KptTheme {
        MemberSavingsHeaderCard(
            member = aminaSavingsMember,
            contributionModel = "FIXED",
            sharesHeld = null,
            shareValue = null,
            savingsBalance = 3500.00,
            savingsAccountNo = "SA-00012345",
        )
    }
}

@Preview
@Composable
private fun SavingsSparklineCardPreview() {
    KptTheme {
        SavingsSparklineCard(points = previewSparkline)
    }
}

@Preview
@Composable
private fun SavingsFilterChipsPreview() {
    KptTheme {
        SavingsFilterChips(selectedFilter = SavingsTransactionFilter.ALL, onFilterSelected = {})
    }
}

@Preview
@Composable
private fun SavingsStatementRowDepositPreview() {
    KptTheme {
        SavingsStatementRow(entry = previewTransactions[0], isExpanded = false, onClick = {})
    }
}

@Preview
@Composable
private fun SavingsStatementRowExpandedPreview() {
    KptTheme {
        SavingsStatementRow(entry = previewTransactions[2], isExpanded = true, onClick = {})
    }
}
