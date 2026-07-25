/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.meetingsummary

import androidx.compose.runtime.Composable
import kpt.core.designsystem.theme.KptTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider
import org.mifos.groupbanking.core.model.LoanSummaryItem
import org.mifos.groupbanking.core.model.MeetingSummaryData
import org.mifos.groupbanking.core.model.SavingsBreakdownItem

/**
 * `@Preview` gallery for `MeetingSummaryScreen.kt`. See API.md#preview. Data source:
 * `idea-layer/screens/meeting-summary/demo-data.yaml#entries` (Meeting #5, 12 May 2026 — all 5
 * members present, KES 1,750 total savings, KES 2,500 repaid, Peter's KES 8,000 loan disbursed) —
 * every field below is the actual seeded value, never a generic placeholder (RULE-PREVIEW-7).
 */
private val savingsBreakdown: List<SavingsBreakdownItem> = listOf(
    SavingsBreakdownItem(memberId = "1001", memberName = "Amina Wangari", groupSavings = 300, individualSavings = 50),
    SavingsBreakdownItem(memberId = "1002", memberName = "Joseph Otieno", groupSavings = 300, individualSavings = 0),
    SavingsBreakdownItem(memberId = "1003", memberName = "Grace Wanjiku", groupSavings = 300, individualSavings = 100),
    SavingsBreakdownItem(memberId = "1004", memberName = "Peter Kamau", groupSavings = 300, individualSavings = 50),
    SavingsBreakdownItem(memberId = "1005", memberName = "Mary Achieng", groupSavings = 300, individualSavings = 50),
)

private val loanItems: List<LoanSummaryItem> = listOf(
    LoanSummaryItem(memberId = "1001", memberName = "Amina Wangari", amountDisbursed = 0, amountRepaid = 700, outstandingAfter = 4900),
    LoanSummaryItem(memberId = "1004", memberName = "Peter Kamau", amountDisbursed = 8000, amountRepaid = 0, outstandingAfter = 8000),
)

private val meetingSummary = MeetingSummaryData(
    meetingId = "MTG-2026-05-12",
    meetingNumber = 5,
    actualDate = "12 May 2026",
    attendanceCount = 5,
    totalMemberCount = 5,
    groupSavingsCollected = 1500,
    individualSavingsCollected = 250,
    totalSavingsCollected = 1750,
    loansDisbursed = 8000,
    loansRepaid = 2500,
    finesCollected = 100,
    openingCorpus = 51750,
    closingCorpus = 64100,
    savingsBreakdown = savingsBreakdown,
    loanItems = loanItems,
)

private val contentState = MeetingSummaryState(
    isLoading = false,
    meetingSummary = meetingSummary,
    meetingId = meetingSummary.meetingId,
    meetingNumber = meetingSummary.meetingNumber,
    centerId = 42,
)

private val networkErrorState = MeetingSummaryState(isLoading = false, error = MeetingSummaryError.Network)

private class MeetingSummaryStatePreviewProvider : PreviewParameterProvider<MeetingSummaryState> {
    override val values: Sequence<MeetingSummaryState> = sequenceOf(
        // loading — record fetch in-flight
        MeetingSummaryState(isLoading = true),
        // content — full summary rendered
        contentState,
        // error — network failure
        networkErrorState,
    )
}

@Preview
@Composable
private fun MeetingSummaryContentPreview(
    @PreviewParameter(MeetingSummaryStatePreviewProvider::class)
    state: MeetingSummaryState,
) {
    KptTheme {
        MeetingSummaryContent(state = state, onAction = {})
    }
}

@Preview
@Composable
private fun MeetingSummaryLoadingSectionPreview() {
    KptTheme {
        MeetingSummaryLoadingSection()
    }
}

@Preview
@Composable
private fun MeetingSummaryContentSectionPreview() {
    KptTheme {
        MeetingSummaryContentSection(state = contentState, onAction = {})
    }
}

@Preview
@Composable
private fun MeetingSummaryHeroCardPreview() {
    KptTheme {
        MeetingSummaryHeroCard(summary = meetingSummary)
    }
}

@Preview
@Composable
private fun MeetingSummaryMetricGridPreview() {
    KptTheme {
        MeetingSummaryMetricGrid(summary = meetingSummary)
    }
}

@Preview
@Composable
private fun MeetingSummarySavingsBreakdownPreview() {
    KptTheme {
        MeetingSummarySavingsBreakdown(rows = savingsBreakdown)
    }
}

@Preview
@Composable
private fun MeetingSummaryCorpusReconciliationPreview() {
    KptTheme {
        MeetingSummaryCorpusReconciliation(summary = meetingSummary)
    }
}

@Preview
@Composable
private fun MeetingSummaryErrorSectionPreview() {
    KptTheme {
        MeetingSummaryErrorSection(state = networkErrorState, onAction = {})
    }
}
