/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.memberprofile

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import kpt.core.designsystem.theme.KptTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider
import org.mifos.groupbanking.core.model.ActiveLoanSummary
import org.mifos.groupbanking.core.model.MemberAccounts
import org.mifos.groupbanking.core.model.MemberProfile
import org.mifos.groupbanking.core.model.MemberRole
import org.mifos.groupbanking.core.model.MemberStatus
import org.mifos.groupbanking.core.model.SavingsDataPoint
import org.mifos.groupbanking.feature.memberprofile.components.ActiveLoanCard
import org.mifos.groupbanking.feature.memberprofile.components.AttendanceCard
import org.mifos.groupbanking.feature.memberprofile.components.MemberHeaderCard
import org.mifos.groupbanking.feature.memberprofile.components.RoleEditBottomSheet
import org.mifos.groupbanking.feature.memberprofile.components.SavingsHistoryCard

/**
 * `@Preview` gallery for `MemberProfileScreen.kt`. See API.md#preview. Data source:
 * `idea-layer/screens/member-profile/demo-data.yaml#entries` (Amina Wanjiru — `client_001`) —
 * `demo_data_resolved = true`, so every field below is the actual seeded value, never a generic
 * placeholder literal (RULE-PREVIEW-7).
 */
private val amina = MemberProfile(
    id = 101L,
    displayName = "Amina Wanjiru",
    firstName = "Amina",
    lastName = "Wanjiru",
    phone = "+254712345678",
    hasPhoto = false,
    status = MemberStatus(id = 300, value = "Active"),
    joinDate = "2026-01-15",
    officeId = 1L,
)

private val savingsHistory: List<SavingsDataPoint> = listOf(
    SavingsDataPoint(date = "2026-01-20", balance = 500.00),
    SavingsDataPoint(date = "2026-01-27", balance = 1000.00),
    SavingsDataPoint(date = "2026-02-03", balance = 1500.00),
    SavingsDataPoint(date = "2026-02-10", balance = 2000.00),
    SavingsDataPoint(date = "2026-02-17", balance = 2500.00),
    SavingsDataPoint(date = "2026-02-24", balance = 3000.00),
    SavingsDataPoint(date = "2026-03-03", balance = 3500.00),
)

private val accountsNoLoan = MemberAccounts(
    savingsBalance = 3500.00,
    savingsHistory = savingsHistory,
    activeLoan = null,
)

private val loanSavingsHistory: List<SavingsDataPoint> = listOf(
    SavingsDataPoint(date = "2026-01-20", balance = 4000.00),
    SavingsDataPoint(date = "2026-01-27", balance = 5000.00),
    SavingsDataPoint(date = "2026-02-03", balance = 5800.00),
    SavingsDataPoint(date = "2026-02-10", balance = 6500.00),
    SavingsDataPoint(date = "2026-02-17", balance = 7100.00),
    SavingsDataPoint(date = "2026-02-24", balance = 7700.00),
    SavingsDataPoint(date = "2026-03-03", balance = 8200.00),
)

private val activeLoan = ActiveLoanSummary(
    id = 5502L,
    productName = "Chama Emergency Loan",
    outstandingBalance = 12500.00,
    inArrears = true,
    dueDate = "2026-07-10",
)

private val accountsWithLoan = MemberAccounts(
    savingsBalance = 8200.00,
    savingsHistory = loanSavingsHistory,
    activeLoan = activeLoan,
)

private val contentState = MemberProfileState(
    isLoading = false,
    member = amina,
    accounts = accountsNoLoan,
    role = MemberRole.TREASURER,
    isCurrentUserChairperson = true,
    meetingsAttended = 14,
    totalMeetings = 15,
    attendanceRate = 0.93,
)

private val contentWithLoanState = contentState.copy(
    accounts = accountsWithLoan,
    isCurrentUserChairperson = false,
)

private val roleEditState = contentState.copy(
    isEditingRole = true,
    selectedRole = MemberRole.SECRETARY,
)

private class MemberProfileStatePreviewProvider : PreviewParameterProvider<MemberProfileState> {
    override val values: Sequence<MemberProfileState> = sequenceOf(
        // loading — companion fetch in-flight
        MemberProfileState(isLoading = true),
        // content — no active loan, chairperson viewer (Edit Role visible)
        contentState,
        // content — active loan in arrears, non-chairperson viewer (Edit Role hidden)
        contentWithLoanState,
        // content — role-edit bottom sheet open, SECRETARY selected
        roleEditState,
        // error — network failure
        MemberProfileState(isLoading = false, error = MemberProfileError.Network),
    )
}

@Preview
@Composable
private fun MemberProfileContentPreview(
    @PreviewParameter(MemberProfileStatePreviewProvider::class)
    state: MemberProfileState,
) {
    KptTheme {
        MemberProfileContent(state = state, onAction = {})
    }
}

@Preview
@Composable
private fun MemberProfileLoadingSectionPreview() {
    KptTheme {
        MemberProfileLoadingSection()
    }
}

@Preview
@Composable
private fun MemberProfileSkeletonBlockPreview() {
    KptTheme {
        MemberProfileSkeletonBlock(height = 180.dp)
    }
}

@Preview
@Composable
private fun MemberProfileContentSectionPreview() {
    KptTheme {
        MemberProfileContentSection(state = contentState, onAction = {})
    }
}

@Preview
@Composable
private fun MemberProfileContentSectionWithLoanPreview() {
    KptTheme {
        MemberProfileContentSection(state = contentWithLoanState, onAction = {})
    }
}

@Preview
@Composable
private fun MemberProfileErrorSectionPreview() {
    KptTheme {
        MemberProfileErrorSection(
            state = MemberProfileState(isLoading = false, error = MemberProfileError.Network),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun MemberHeaderCardChairpersonPreview() {
    KptTheme {
        MemberHeaderCard(
            member = amina,
            role = MemberRole.TREASURER,
            isCurrentUserChairperson = true,
            onEditRoleClick = {},
        )
    }
}

@Preview
@Composable
private fun MemberHeaderCardMemberPreview() {
    KptTheme {
        MemberHeaderCard(
            member = amina,
            role = MemberRole.MEMBER,
            isCurrentUserChairperson = false,
            onEditRoleClick = {},
        )
    }
}

@Preview
@Composable
private fun SavingsHistoryCardPreview() {
    KptTheme {
        SavingsHistoryCard(
            savingsBalance = accountsNoLoan.savingsBalance,
            savingsHistory = accountsNoLoan.savingsHistory,
            onViewFullHistoryClick = {},
        )
    }
}

@Preview
@Composable
private fun ActiveLoanCardPreview() {
    KptTheme {
        ActiveLoanCard(loan = activeLoan)
    }
}

@Preview
@Composable
private fun AttendanceCardPreview() {
    KptTheme {
        AttendanceCard(meetingsAttended = 14, totalMeetings = 15, attendanceRate = 0.93)
    }
}

@Preview
@Composable
private fun AttendanceCardLowRatePreview() {
    KptTheme {
        AttendanceCard(meetingsAttended = 5, totalMeetings = 15, attendanceRate = 0.33)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun RoleEditBottomSheetPreview() {
    KptTheme {
        RoleEditBottomSheet(
            selectedRole = MemberRole.SECRETARY,
            isUpdatingRole = false,
            onRoleSelected = {},
            onConfirmClick = {},
            onDismiss = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun RoleEditBottomSheetUpdatingPreview() {
    KptTheme {
        RoleEditBottomSheet(
            selectedRole = MemberRole.SECRETARY,
            isUpdatingRole = true,
            onRoleSelected = {},
            onConfirmClick = {},
            onDismiss = {},
        )
    }
}
