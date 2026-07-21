/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.personaldashboard

import androidx.compose.runtime.Composable
import kotlinx.datetime.LocalDate
import kpt.core.designsystem.theme.KptTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider
import org.mifos.groupbanking.core.model.GroupSummary
import org.mifos.groupbanking.core.model.SavingsMechanism
import org.mifos.groupbanking.core.model.SavingsTransaction
import org.mifos.groupbanking.core.model.TransactionType
import org.mifos.groupbanking.feature.personaldashboard.components.GroupSelectorChipRow
import org.mifos.groupbanking.feature.personaldashboard.components.RecentActivityRow
import org.mifos.groupbanking.feature.personaldashboard.components.SavingsSummaryCard
import org.mifos.groupbanking.feature.personaldashboard.components.ShareoutProjectionCard

/**
 * `@Preview` gallery for `PersonalDashboardScreen.kt`. See API.md#preview. Data source:
 * `idea-layer/screens/personal-dashboard/demo-data.yaml#entries` (`MemberDashboardResponse` /
 * `GroupSummary` / `SavingsTransactionDto` — Amina Wanjiru / ACCUMULATING and Joseph Kamau /
 * ROTATING_PAYOUT) — `demo_data_resolved = true`, so every field below is the actual seeded
 * value, never a generic placeholder literal (RULE-PREVIEW-7).
 */
private val previewGroups: List<GroupSummary> = listOf(
    GroupSummary(
        groupId = "GRP-20260509-001",
        name = "Mwangaza Women's Group",
        poolModel = SavingsMechanism.ACCUMULATING,
    ),
    GroupSummary(
        groupId = "GRP-20260215-002",
        name = "Tumaini ROSCA",
        poolModel = SavingsMechanism.ROTATING_PAYOUT,
    ),
)

private val previewTransactionsAccumulating: List<SavingsTransaction> = listOf(
    SavingsTransaction(id = "TXN-3001", date = LocalDate.parse("2026-05-09"), type = TransactionType.DEPOSIT, amount = 300.00),
    SavingsTransaction(id = "TXN-3002", date = LocalDate.parse("2026-05-02"), type = TransactionType.DEPOSIT, amount = 300.00),
    SavingsTransaction(id = "TXN-3003", date = LocalDate.parse("2026-04-25"), type = TransactionType.DEPOSIT, amount = 300.00),
)

private val previewTransactionsRotating: List<SavingsTransaction> = listOf(
    SavingsTransaction(id = "TXN-4001", date = LocalDate.parse("2026-05-09"), type = TransactionType.DEPOSIT, amount = 1000.00),
    SavingsTransaction(id = "TXN-4002", date = LocalDate.parse("2026-04-25"), type = TransactionType.DEPOSIT, amount = 1000.00),
)

private val accumulatingState = PersonalDashboardState(
    memberName = "Amina Wanjiru",
    myGroups = previewGroups,
    selectedGroup = previewGroups[0],
    poolModel = "ACCUMULATING",
    groupLinkedSavingsBalance = 3500.00,
    individualSavingsBalance = 800.00,
    shareOutProjection = 9200.00,
    rotationPosition = null,
    nextRecipientEta = null,
    recentTransactions = previewTransactionsAccumulating,
    isLoading = false,
)

private val rotatingState = PersonalDashboardState(
    memberName = "Joseph Kamau",
    myGroups = listOf(previewGroups[1]),
    selectedGroup = previewGroups[1],
    poolModel = "ROTATING_PAYOUT",
    groupLinkedSavingsBalance = 5000.00,
    individualSavingsBalance = 0.00,
    shareOutProjection = 0.0,
    rotationPosition = 3,
    nextRecipientEta = "2026-06-15",
    recentTransactions = previewTransactionsRotating,
    isLoading = false,
)

private class PersonalDashboardStatePreviewProvider : PreviewParameterProvider<PersonalDashboardState> {
    override val values: Sequence<PersonalDashboardState> = sequenceOf(
        // loading — cold start, memberName/selectedGroup still at their declared defaults
        PersonalDashboardState(isLoading = true),
        // content — ACCUMULATING pool model (Mwangaza Women's Group), 2-group selector visible
        accumulatingState,
        // content — ROTATING_PAYOUT pool model (Tumaini ROSCA), single-group (no selector)
        rotatingState,
        // empty — zero groups joined yet
        PersonalDashboardState(isLoading = false, memberName = "Amina Wanjiru", myGroups = emptyList()),
        // error — network failure with a previously-cached selected group still in state
        PersonalDashboardState(
            isLoading = false,
            memberName = "Amina Wanjiru",
            myGroups = previewGroups,
            selectedGroup = previewGroups[0],
            error = DashboardError.Network,
        ),
    )
}

@Preview
@Composable
private fun PersonalDashboardContentPreview(
    @PreviewParameter(PersonalDashboardStatePreviewProvider::class)
    state: PersonalDashboardState,
) {
    KptTheme {
        PersonalDashboardContent(state = state, onAction = {})
    }
}

@Preview
@Composable
private fun PersonalDashboardTopSectionPreview() {
    KptTheme {
        PersonalDashboardTopSection(
            greeting = "Good morning, Amina Wanjiru",
            groupName = "Mwangaza Women's Group",
            showChips = true,
            groups = previewGroups,
            selectedGroupId = previewGroups[0].groupId,
            onGroupSelected = {},
        )
    }
}

@Preview
@Composable
private fun PersonalDashboardLoadingSectionPreview() {
    KptTheme {
        PersonalDashboardLoadingSection(greeting = "Good morning, Amina Wanjiru")
    }
}

@Preview
@Composable
private fun PersonalDashboardSkeletonBlockPreview() {
    KptTheme {
        PersonalDashboardSkeletonBlock()
    }
}

@Preview
@Composable
private fun PersonalDashboardContentSectionAccumulatingPreview() {
    KptTheme {
        PersonalDashboardContentSection(
            state = accumulatingState,
            greeting = "Good morning, Amina Wanjiru",
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun PersonalDashboardContentSectionRotatingPreview() {
    KptTheme {
        PersonalDashboardContentSection(
            state = rotatingState,
            greeting = "Good afternoon, Joseph Kamau",
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun PersonalDashboardEmptySectionPreview() {
    KptTheme {
        PersonalDashboardEmptySection(greeting = "Good morning, Amina Wanjiru")
    }
}

@Preview
@Composable
private fun PersonalDashboardErrorSectionPreview() {
    KptTheme {
        PersonalDashboardErrorSection(
            greeting = "Good morning, Amina Wanjiru",
            groupName = "Mwangaza Women's Group",
            onRetry = {},
        )
    }
}

@Preview
@Composable
private fun GroupSelectorChipRowPreview() {
    KptTheme {
        GroupSelectorChipRow(
            groups = previewGroups,
            selectedGroupId = previewGroups[0].groupId,
            onGroupSelected = {},
        )
    }
}

@Preview
@Composable
private fun SavingsSummaryCardPreview() {
    KptTheme {
        SavingsSummaryCard(groupLinkedBalance = 3500.00, individualBalance = 800.00, onClick = {})
    }
}

@Preview
@Composable
private fun ShareoutProjectionCardAccumulatingPreview() {
    KptTheme {
        ShareoutProjectionCard(
            poolModel = "ACCUMULATING",
            shareOutProjection = 9200.00,
            rotationPosition = null,
            nextRecipientEta = null,
        )
    }
}

@Preview
@Composable
private fun ShareoutProjectionCardRotatingPreview() {
    KptTheme {
        ShareoutProjectionCard(
            poolModel = "ROTATING_PAYOUT",
            shareOutProjection = 0.0,
            rotationPosition = 3,
            nextRecipientEta = "2026-06-15",
        )
    }
}

@Preview
@Composable
private fun RecentActivityRowDepositPreview() {
    KptTheme {
        RecentActivityRow(transaction = previewTransactionsAccumulating[0])
    }
}
