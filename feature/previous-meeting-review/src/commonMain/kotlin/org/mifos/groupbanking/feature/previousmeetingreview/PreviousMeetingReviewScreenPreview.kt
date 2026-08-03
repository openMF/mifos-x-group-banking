/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.previousmeetingreview

import androidx.compose.runtime.Composable
import kpt.core.designsystem.theme.KptTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider
import org.mifos.groupbanking.core.model.AttendanceRecord
import org.mifos.groupbanking.core.model.AttendanceStatus
import org.mifos.groupbanking.core.model.LoanSummaryItem
import org.mifos.groupbanking.core.model.MeetingSummaryData
import org.mifos.groupbanking.core.model.PreviousMeetingDetail
import org.mifos.groupbanking.core.model.SavingsBreakdownItem
import org.mifos.groupbanking.core.model.UnresolvedItem
import org.mifos.groupbanking.core.model.UnresolvedType

/**
 * `@Preview` gallery for `PreviousMeetingReviewScreen.kt`. See API.md#preview. Data source:
 * `idea-layer/screens/previous-meeting-review/demo-data.yaml#entries.meetingDetail` (Meeting #15,
 * closed 2026-05-06 — 4/5 present, KES 2,000 total collected, Mary Njeri ABSENT with a KES 50 fine →
 * one derived UNPAID_FINE unresolved item) — every field below is the actual seeded value, never a
 * generic placeholder (RULE-PREVIEW-7).
 */
private val savingsBreakdown: List<SavingsBreakdownItem> = listOf(
    SavingsBreakdownItem(memberId = "client_001", memberName = "Amina Wanjiru", groupSavings = 500, individualSavings = 0),
    SavingsBreakdownItem(memberId = "client_002", memberName = "Joseph Kamau", groupSavings = 500, individualSavings = 100),
    SavingsBreakdownItem(memberId = "client_003", memberName = "Grace Achieng", groupSavings = 400, individualSavings = 0),
    SavingsBreakdownItem(memberId = "client_004", memberName = "Peter Otieno", groupSavings = 400, individualSavings = 100),
    SavingsBreakdownItem(memberId = "client_005", memberName = "Mary Njeri", groupSavings = 0, individualSavings = 0),
)

private val loanItems: List<LoanSummaryItem> = listOf(
    LoanSummaryItem(memberId = "client_001", memberName = "Amina Wanjiru", amountDisbursed = 0, amountRepaid = 458, outstandingAfter = 2792),
    LoanSummaryItem(memberId = "client_002", memberName = "Joseph Kamau", amountDisbursed = 5000, amountRepaid = 0, outstandingAfter = 5000),
)

private val attendanceRecords: List<AttendanceRecord> = listOf(
    AttendanceRecord(memberId = "client_001", memberName = "Amina Wanjiru", status = AttendanceStatus.PRESENT, fineAmount = 0),
    AttendanceRecord(memberId = "client_002", memberName = "Joseph Kamau", status = AttendanceStatus.PRESENT, fineAmount = 0),
    AttendanceRecord(memberId = "client_003", memberName = "Grace Achieng", status = AttendanceStatus.PRESENT, fineAmount = 0),
    AttendanceRecord(memberId = "client_004", memberName = "Peter Otieno", status = AttendanceStatus.LATE, fineAmount = 50),
    AttendanceRecord(memberId = "client_005", memberName = "Mary Njeri", status = AttendanceStatus.ABSENT, fineAmount = 50),
)

private val summary = MeetingSummaryData(
    meetingId = "meeting_015",
    meetingNumber = 15,
    actualDate = "2026-05-06",
    attendanceCount = 4,
    totalMemberCount = 5,
    groupSavingsCollected = 1800,
    individualSavingsCollected = 200,
    totalSavingsCollected = 2000,
    loansDisbursed = 5000,
    loansRepaid = 458,
    finesCollected = 100,
    openingCorpus = 41800,
    closingCorpus = 43800,
    savingsBreakdown = savingsBreakdown,
    loanItems = loanItems,
)

private val unresolvedItems: List<UnresolvedItem> = listOf(
    UnresolvedItem(
        type = UnresolvedType.UNPAID_FINE,
        description = "Mary Njeri — fine KES 50 not collected",
        memberId = "client_005",
    ),
)

private val meetingDetail = PreviousMeetingDetail(
    summary = summary,
    attendanceRecords = attendanceRecords,
    unresolvedItems = unresolvedItems,
)

private val calendarContentState = PreviousMeetingReviewState(
    isLoading = false,
    meetingDetail = meetingDetail,
    unresolvedItems = unresolvedItems,
    launchedFrom = "calendar",
    meetingId = summary.meetingId,
    meetingNumber = summary.meetingNumber,
    centerId = 7,
)

private val conductContentState = PreviousMeetingReviewState(
    isLoading = false,
    meetingDetail = meetingDetail,
    unresolvedItems = emptyList(),
    launchedFrom = "conduct",
    meetingId = summary.meetingId,
    meetingNumber = summary.meetingNumber,
    centerId = 7,
    nextMeetingId = "meeting_016",
    nextMeetingNumber = 16,
)

private val networkErrorState = PreviousMeetingReviewState(isLoading = false, error = PreviousMeetingReviewError.Network)

private class PreviousMeetingReviewStatePreviewProvider : PreviewParameterProvider<PreviousMeetingReviewState> {
    override val values: Sequence<PreviousMeetingReviewState> = sequenceOf(
        // loading — combined read in-flight
        PreviousMeetingReviewState(isLoading = true),
        // content — calendar-launched (no Start-Meeting CTA), unresolved item shown
        calendarContentState,
        // content — conduct-launched (Start-Meeting CTA visible), no unresolved items
        conductContentState,
        // error — network failure
        networkErrorState,
    )
}

@Preview
@Composable
private fun PreviousMeetingReviewContentPreview(
    @PreviewParameter(PreviousMeetingReviewStatePreviewProvider::class)
    state: PreviousMeetingReviewState,
) {
    KptTheme {
        PreviousMeetingReviewContent(state = state, onAction = {})
    }
}

@Preview
@Composable
private fun PreviousMeetingReviewContentSectionPreview() {
    KptTheme {
        PreviousMeetingReviewContentSection(state = calendarContentState, onAction = {})
    }
}

@Preview
@Composable
private fun PreviousMeetingReviewConductContentPreview() {
    KptTheme {
        PreviousMeetingReviewContentSection(state = conductContentState, onAction = {})
    }
}

@Preview
@Composable
private fun PreviousMeetingReviewLoadingSectionPreview() {
    KptTheme {
        PreviousMeetingReviewLoadingSection()
    }
}

@Preview
@Composable
private fun PreviousMeetingReviewErrorSectionPreview() {
    KptTheme {
        PreviousMeetingReviewErrorSection(state = networkErrorState, onAction = {})
    }
}
