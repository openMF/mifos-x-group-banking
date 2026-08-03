/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.personalsavings

import androidx.compose.runtime.Composable
import kotlinx.datetime.LocalDate
import kpt.core.designsystem.theme.KptTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider
import org.mifos.groupbanking.core.model.SavingsLedgerEntry
import org.mifos.groupbanking.core.model.SavingsLedgerTransactionType
import org.mifos.groupbanking.core.model.SavingsTab
import org.mifos.groupbanking.feature.personalsavings.components.SavingsBalanceHeroCard
import org.mifos.groupbanking.feature.personalsavings.components.SavingsContributionProgressCard
import org.mifos.groupbanking.feature.personalsavings.components.SavingsEmptyIndividualState
import org.mifos.groupbanking.feature.personalsavings.components.SavingsTabRow
import org.mifos.groupbanking.feature.personalsavings.components.SavingsTransactionRow
import org.mifos.groupbanking.feature.personalsavings.components.SavingsTransactionSkeletonRow

/**
 * `@Preview` gallery for `PersonalSavingsScreen.kt`. See API.md#preview. Data source:
 * `demo-data.yaml` (`entries[0].items[]` `PersonalSavingsState` for `clientId: 101`,
 * `entries[1]`/`entries[2]` group-linked/individual `SavingsTransactionDto*` series) — the values
 * below are the ACTUAL rows from `demo-data.yaml` (RULE-PREVIEW-7 — no placeholder literals),
 * mapped onto the canonical [SavingsLedgerEntry] domain shape.
 */
private val depositType = SavingsLedgerTransactionType(value = 1, code = "savingsTransactionType.deposit", description = "Deposit")
private val interestType = SavingsLedgerTransactionType(
    value = 9,
    code = "savingsTransactionType.interestPosting",
    description = "Interest Posting",
)

private val previewGroupLinkedTransactions: List<SavingsLedgerEntry> = listOf(
    SavingsLedgerEntry(4001, depositType, LocalDate(2026, 5, 6), 500.00, 3500.00, "KES", "KES"),
    SavingsLedgerEntry(4002, depositType, LocalDate(2026, 4, 29), 500.00, 3000.00, "KES", "KES"),
    SavingsLedgerEntry(4003, interestType, LocalDate(2026, 4, 30), 12.50, 2512.50, "KES", "KES"),
    SavingsLedgerEntry(4004, depositType, LocalDate(2026, 4, 22), 500.00, 2500.00, "KES", "KES"),
    SavingsLedgerEntry(4005, depositType, LocalDate(2026, 4, 8), 500.00, 2000.00, "KES", "KES"),
    SavingsLedgerEntry(4006, depositType, LocalDate(2026, 3, 25), 500.00, 1500.00, "KES", "KES"),
    SavingsLedgerEntry(4007, depositType, LocalDate(2026, 3, 11), 500.00, 1000.00, "KES", "KES"),
    SavingsLedgerEntry(4008, depositType, LocalDate(2026, 2, 25), 500.00, 500.00, "KES", "KES"),
)

private val previewIndividualTransactions: List<SavingsLedgerEntry> = listOf(
    SavingsLedgerEntry(5001, depositType, LocalDate(2026, 4, 15), 400.00, 800.00, "KES", "KES"),
    SavingsLedgerEntry(5002, depositType, LocalDate(2026, 3, 1), 300.00, 400.00, "KES", "KES"),
    SavingsLedgerEntry(5003, depositType, LocalDate(2026, 2, 10), 100.00, 100.00, "KES", "KES"),
)

private val previewContentState = PersonalSavingsState(
    clientId = 101L,
    groupLinkedSavingsId = 2001L,
    individualSavingsId = 2050L,
    selectedTab = SavingsTab.GROUP_LINKED,
    groupLinkedBalance = 3500.00,
    individualBalance = 800.00,
    groupLinkedTransactions = previewGroupLinkedTransactions,
    individualTransactions = previewIndividualTransactions,
    contributionTarget = 500.00,
    meetingsAttended = 14,
    totalMeetings = 16,
    isLoading = false,
    isRefreshing = false,
    error = null,
)

private class PersonalSavingsStatePreviewProvider : PreviewParameterProvider<PersonalSavingsState> {
    override val values: Sequence<PersonalSavingsState> = sequenceOf(
        // loading — spinner + skeleton rows, no data rendered yet
        PersonalSavingsState(clientId = 101L, groupLinkedSavingsId = 2001L, individualSavingsId = 2050L, isLoading = true),
        // content — GROUP_LINKED tab (default), full demo-data.yaml balances/transactions/progress
        previewContentState,
        // content — INDIVIDUAL tab, real individual account + its 3 transactions
        previewContentState.copy(selectedTab = SavingsTab.INDIVIDUAL),
        // content — INDIVIDUAL tab, member has NO individual account (empty_individual_promo)
        previewContentState.copy(
            selectedTab = SavingsTab.INDIVIDUAL,
            individualSavingsId = null,
            individualBalance = 0.0,
            individualTransactions = emptyList(),
        ),
        // error — network failure (ui.yaml Error state)
        previewContentState.copy(isLoading = false, error = SavingsError.Network),
        // error — session expired (non-retryable, Retry button hidden)
        previewContentState.copy(isLoading = false, error = SavingsError.Unauthorized),
    )
}

@Preview
@Composable
private fun PersonalSavingsContentPreview(
    @PreviewParameter(PersonalSavingsStatePreviewProvider::class)
    state: PersonalSavingsState,
) {
    KptTheme {
        PersonalSavingsContent(state = state, onAction = {}, onNavigateBack = {})
    }
}

@Preview
@Composable
private fun PersonalSavingsLoadingSectionPreview() {
    KptTheme {
        PersonalSavingsLoadingSection()
    }
}

@Preview
@Composable
private fun PersonalSavingsContentSectionPreview() {
    KptTheme {
        PersonalSavingsContentSection(state = previewContentState)
    }
}

@Preview
@Composable
private fun PersonalSavingsContentSectionIndividualPreview() {
    KptTheme {
        PersonalSavingsContentSection(state = previewContentState.copy(selectedTab = SavingsTab.INDIVIDUAL))
    }
}

@Preview
@Composable
private fun PersonalSavingsErrorSectionPreview() {
    KptTheme {
        PersonalSavingsErrorSection(error = SavingsError.Network, onRetry = {})
    }
}

@Preview
@Composable
private fun PersonalSavingsErrorSectionUnauthorizedPreview() {
    // SavingsError.Unauthorized has retry = false — Retry button is hidden.
    KptTheme {
        PersonalSavingsErrorSection(error = SavingsError.Unauthorized, onRetry = {})
    }
}

@Preview
@Composable
private fun SavingsTabRowPreview() {
    KptTheme {
        SavingsTabRow(selectedTab = SavingsTab.GROUP_LINKED, hasIndividualAccount = true, onTabSelected = {})
    }
}

@Preview
@Composable
private fun SavingsTabRowNoIndividualPreview() {
    KptTheme {
        SavingsTabRow(selectedTab = SavingsTab.GROUP_LINKED, hasIndividualAccount = false, onTabSelected = {})
    }
}

@Preview
@Composable
private fun SavingsBalanceHeroCardPreview() {
    KptTheme {
        SavingsBalanceHeroCard(tab = SavingsTab.GROUP_LINKED, balance = 3500.00, accountId = 2001L)
    }
}

@Preview
@Composable
private fun SavingsContributionProgressCardPreview() {
    KptTheme {
        SavingsContributionProgressCard(meetingsAttended = 14, totalMeetings = 16, groupLinkedBalance = 3500.00)
    }
}

@Preview
@Composable
private fun SavingsTransactionRowDepositPreview() {
    KptTheme {
        SavingsTransactionRow(transaction = previewGroupLinkedTransactions[0])
    }
}

@Preview
@Composable
private fun SavingsTransactionSkeletonRowPreview() {
    KptTheme {
        SavingsTransactionSkeletonRow()
    }
}

@Preview
@Composable
private fun SavingsEmptyIndividualStatePreview() {
    KptTheme {
        SavingsEmptyIndividualState()
    }
}
