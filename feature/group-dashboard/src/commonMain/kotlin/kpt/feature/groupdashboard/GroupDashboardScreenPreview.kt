/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.groupdashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import kpt.core.designsystem.theme.KptTheme
import kpt.core.model.ActivityItem
import kpt.core.model.ActivityType
import kpt.core.model.GroupAccounts
import kpt.core.model.GroupConfig
import kpt.core.model.GroupContributionModel
import kpt.core.model.GroupCorpus
import kpt.core.model.GroupDetail
import kpt.core.model.GroupInstanceConfig
import kpt.core.model.GroupTypeSlug
import kpt.core.model.SavingsMechanism
import kpt.feature.groupdashboard.components.ActivityFeedSection
import kpt.feature.groupdashboard.components.ActivityRow
import kpt.feature.groupdashboard.components.CorpusBlockedDialog
import kpt.feature.groupdashboard.components.CorpusMetricCard
import kpt.feature.groupdashboard.components.GroupHeaderCard
import kpt.feature.groupdashboard.components.GroupSavingsSummaryCard
import kpt.feature.groupdashboard.components.QuickActionsSection
import kpt.feature.groupdashboard.components.RotationMetricCard
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider

/**
 * `@Preview` gallery for `GroupDashboardScreen.kt`. See API.md#preview. Data source:
 * `idea-layer/screens/group-dashboard/demo-data.yaml#entries` (Mwangaza Women's Group / VSLA /
 * ACCUMULATING and Jiunge ROSCA Circle / ROSCA / ROTATING_PAYOUT) — `demo_data_resolved = true`,
 * so every field below is the actual seeded value, never a generic placeholder literal
 * (RULE-PREVIEW-7). [GroupDetail.fineractGroupId] has no matching demo-data field (companion-API
 * concern, not surfaced to the dashboard canvas) — a plausible non-zero placeholder is used since
 * the field is required but never rendered.
 */
private val accumulatingGroup = GroupDetail(
    id = "GRP-20260509-001",
    fineractGroupId = 501L,
    name = "Mwangaza Women's Group",
    cycleNumber = 1,
    cycleLengthMonths = 12,
    meetingFrequency = "Weekly",
    memberCount = 20,
    overdueLoansCount = 0,
    status = "ACTIVE",
    typeConfig = GroupInstanceConfig(
        groupType = GroupTypeSlug.VSLA,
        poolModel = SavingsMechanism.ACCUMULATING,
        contributionModel = GroupContributionModel.SHARE_BASED_VARIABLE,
        shareoutFormula = "PRORATA_SHARES",
        payoutOrderMethod = "",
        shareValue = 200.0,
        contributionAmount = 0.0,
        socialFundEnabled = true,
        cycleLengthMonths = 12,
        loanMultiplier = 3.0,
        interestRate = 10.0,
        fineAmount = 50.0,
    ),
)

private val rotatingGroup = GroupDetail(
    id = "GRP-20260610-002",
    fineractGroupId = 610L,
    name = "Jiunge ROSCA Circle",
    cycleNumber = 3,
    cycleLengthMonths = 10,
    meetingFrequency = "Monthly",
    memberCount = 10,
    overdueLoansCount = 0,
    status = "ACTIVE",
    typeConfig = GroupInstanceConfig(
        groupType = GroupTypeSlug.ROSCA,
        poolModel = SavingsMechanism.ROTATING_PAYOUT,
        contributionModel = GroupContributionModel.FIXED_AMOUNT,
        shareoutFormula = "",
        payoutOrderMethod = "FIXED_ORDER",
        shareValue = 0.0,
        contributionAmount = 2000.0,
        socialFundEnabled = false,
        cycleLengthMonths = 10,
        loanMultiplier = 0.0,
        interestRate = 0.0,
        fineAmount = 100.0,
    ),
)

private val accumulatingCorpus = GroupCorpus(
    currentBalance = 47500.00,
    openingBalance = 0.00,
    totalContributionsThisCycle = 52500.00,
    totalLoansOutstanding = 5000.00,
    lastUpdated = "2026-05-09",
    isCycleEnd = false,
    rotationPosition = null,
    nextRecipientName = null,
    nextRecipientPosition = null,
)

private val rotatingCorpus = GroupCorpus(
    currentBalance = 20000.00,
    openingBalance = 0.00,
    totalContributionsThisCycle = 60000.00,
    totalLoansOutstanding = 0.00,
    lastUpdated = "2026-06-10",
    isCycleEnd = false,
    rotationPosition = 7,
    nextRecipientName = "Amina Hassan",
    nextRecipientPosition = 4,
)

private val accumulatingActivity: List<ActivityItem> = listOf(
    ActivityItem(id = "ACT-201-001", type = ActivityType.MEETING, description = "Weekly meeting — week 4 recorded", amount = null, date = "2026-05-05", memberName = null),
    ActivityItem(id = "ACT-201-002", type = ActivityType.DEPOSIT, description = "Weekly contribution", amount = 300.00, date = "2026-05-05", memberName = "Amina Wanjiru"),
    ActivityItem(id = "ACT-201-003", type = ActivityType.DEPOSIT, description = "Weekly contribution", amount = 500.00, date = "2026-05-05", memberName = "Joseph Kamau"),
    ActivityItem(id = "ACT-201-004", type = ActivityType.LOAN, description = "Loan disbursed", amount = 5000.00, date = "2026-05-05", memberName = "Grace Akinyi"),
    // demo-data.yaml's "NEW_MEMBER" activity type has no matching ActivityType domain member —
    // mapped to UNKNOWN (flagged, not invented — see ActivityType KDoc "absorbs any wire value").
    ActivityItem(id = "ACT-201-005", type = ActivityType.UNKNOWN, description = "New member joined", amount = null, date = "2026-04-28", memberName = "Peter Mwangi"),
)

private val rotatingActivity: List<ActivityItem> = listOf(
    ActivityItem(
        id = "ACT-301-001",
        type = ActivityType.SHARE_OUT,
        description = "Payout disbursed to member #3",
        amount = 20000.00,
        date = "2026-05-01",
        memberName = "Fatuma Ali",
    ),
    ActivityItem(id = "ACT-301-002", type = ActivityType.DEPOSIT, description = "Monthly contribution", amount = 2000.00, date = "2026-05-01", memberName = "Amina Hassan"),
    ActivityItem(id = "ACT-301-003", type = ActivityType.MEETING, description = "Monthly meeting — cycle 3 recorded", amount = null, date = "2026-05-01", memberName = null),
)

private val accumulatingAccounts = GroupAccounts(
    savingsBalance = 52500.00,
    loansOutstanding = 5000.00,
    activeLoanCount = 1,
    shareOutProjection = 2750.00,
    recentActivity = accumulatingActivity,
)

private val rotatingAccounts = GroupAccounts(
    savingsBalance = 60000.00,
    loansOutstanding = 0.00,
    activeLoanCount = 0,
    shareOutProjection = null,
    recentActivity = rotatingActivity,
)

private val accumulatingConfig = GroupConfig(
    shareValue = 200.00,
    shareMin = null,
    shareMax = null,
    contributionAmount = null,
    loanMultiplier = 3.0,
    interestRate = 10.0,
    cycleLengthMonths = 12,
    fineAmount = 50.00,
    minimumDisbursementThreshold = null,
)

private val rotatingConfig = GroupConfig(
    shareValue = null,
    shareMin = null,
    shareMax = null,
    contributionAmount = 2000.00,
    loanMultiplier = null,
    interestRate = null,
    cycleLengthMonths = 10,
    fineAmount = 100.00,
    minimumDisbursementThreshold = null,
)

private val accumulatingState = GroupDashboardState(
    isLoading = false,
    group = accumulatingGroup,
    corpus = accumulatingCorpus,
    config = accumulatingConfig,
    typeConfig = accumulatingGroup.typeConfig,
    groupTypeName = "VSLA",
    viewerRole = "ORGANIZER",
    accounts = accumulatingAccounts,
    recentActivity = accumulatingActivity,
    isCorpusInsufficient = false,
    isCycleEnd = false,
    shareOutProjection = 2750.00,
)

private val rotatingState = GroupDashboardState(
    isLoading = false,
    group = rotatingGroup,
    corpus = rotatingCorpus,
    config = rotatingConfig,
    typeConfig = rotatingGroup.typeConfig,
    groupTypeName = "ROSCA",
    viewerRole = "MEMBER",
    accounts = rotatingAccounts,
    recentActivity = rotatingActivity,
    isCorpusInsufficient = false,
    isCycleEnd = false,
    rotationPosition = 7,
    nextRecipientName = "Amina Hassan",
    nextRecipientPosition = 4,
)

private class GroupDashboardStatePreviewProvider : PreviewParameterProvider<GroupDashboardState> {
    override val values: Sequence<GroupDashboardState> = sequenceOf(
        // loading — cold start
        GroupDashboardState(isLoading = true),
        // content — ACCUMULATING pool model (Mwangaza Women's Group), ORGANIZER viewer
        accumulatingState,
        // content — ROTATING_PAYOUT pool model (Jiunge ROSCA Circle), MEMBER viewer
        rotatingState,
        // error — network failure
        GroupDashboardState(isLoading = false, viewerRole = "ORGANIZER", error = GroupDashboardError.Network),
    )
}

@Preview
@Composable
private fun GroupDashboardContentPreview(
    @PreviewParameter(GroupDashboardStatePreviewProvider::class)
    state: GroupDashboardState,
) {
    KptTheme {
        GroupDashboardContent(state = state, onAction = {})
    }
}

@Preview
@Composable
private fun GroupDashboardLoadingSectionPreview() {
    KptTheme {
        GroupDashboardLoadingSection()
    }
}

@Preview
@Composable
private fun GroupDashboardSkeletonBlockPreview() {
    KptTheme {
        GroupDashboardSkeletonBlock(height = 128.dp)
    }
}

@Preview
@Composable
private fun GroupDashboardContentSectionAccumulatingPreview() {
    KptTheme {
        GroupDashboardContentSection(state = accumulatingState, onAction = {})
    }
}

@Preview
@Composable
private fun GroupDashboardContentSectionRotatingPreview() {
    KptTheme {
        GroupDashboardContentSection(state = rotatingState, onAction = {})
    }
}

@Preview
@Composable
private fun GroupDashboardErrorSectionPreview() {
    KptTheme {
        GroupDashboardErrorSection(
            state = GroupDashboardState(isLoading = false, error = GroupDashboardError.Network),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun GroupHeaderCardPreview() {
    KptTheme {
        GroupHeaderCard(group = accumulatingGroup, groupTypeName = "VSLA", viewerRole = "ORGANIZER")
    }
}

@Preview
@Composable
private fun CorpusMetricCardPreview() {
    KptTheme {
        CorpusMetricCard(corpus = accumulatingCorpus, shareOutProjection = 2750.00, isCorpusInsufficient = false)
    }
}

@Preview
@Composable
private fun CorpusMetricCardInsufficientPreview() {
    KptTheme {
        CorpusMetricCard(corpus = accumulatingCorpus, shareOutProjection = null, isCorpusInsufficient = true)
    }
}

@Preview
@Composable
private fun RotationMetricCardPreview() {
    KptTheme {
        RotationMetricCard(
            corpus = rotatingCorpus,
            rotationPosition = 7,
            nextRecipientName = "Amina Hassan",
            nextRecipientPosition = 4,
        )
    }
}

@Preview
@Composable
private fun QuickActionsSectionManagementPreview() {
    KptTheme {
        QuickActionsSection(viewerRole = "ORGANIZER", isCycleEnd = false, onAction = {})
    }
}

@Preview
@Composable
private fun QuickActionsSectionMemberPreview() {
    KptTheme {
        QuickActionsSection(viewerRole = "MEMBER", isCycleEnd = false, onAction = {})
    }
}

@Preview
@Composable
private fun GroupSavingsSummaryCardPreview() {
    KptTheme {
        GroupSavingsSummaryCard(
            contributionModel = GroupContributionModel.SHARE_BASED_VARIABLE,
            config = accumulatingConfig,
            totalSavings = 52500.00,
        )
    }
}

@Preview
@Composable
private fun ActivityFeedSectionPreview() {
    KptTheme {
        ActivityFeedSection(activities = accumulatingActivity)
    }
}

@Preview
@Composable
private fun ActivityRowPreview() {
    KptTheme {
        ActivityRow(activity = accumulatingActivity[1])
    }
}

@Preview
@Composable
private fun CorpusBlockedDialogPreview() {
    KptTheme {
        CorpusBlockedDialog(onDismiss = {})
    }
}
