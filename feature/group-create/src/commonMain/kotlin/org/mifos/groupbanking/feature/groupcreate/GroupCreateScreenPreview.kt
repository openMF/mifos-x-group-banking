/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.groupcreate

import androidx.compose.runtime.Composable
import kpt.core.designsystem.theme.KptTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider
import org.mifos.groupbanking.core.model.ContributionMode
import org.mifos.groupbanking.core.model.GroupTypeConfig
import org.mifos.groupbanking.core.model.GroupTypeSlug
import org.mifos.groupbanking.core.model.Office
import org.mifos.groupbanking.core.model.SavingsMechanism

// -- demo-data.yaml#entries fixtures (Office / GroupTypeConfig) -------------------------------

private val demoOffices = listOf(
    Office(id = 10L, name = "Kisumu West Branch", nameDecorated = "  Kisumu West Branch", externalId = "KWB-001"),
    Office(id = 11L, name = "Kisumu East Branch", nameDecorated = "  Kisumu East Branch", externalId = "KEB-001"),
    Office(id = 12L, name = "Kisumu Central Branch", nameDecorated = "  Kisumu Central Branch", externalId = "KCB-001"),
)

private val demoVslaTypeConfig = GroupTypeConfig(
    typeSlug = GroupTypeSlug.VSLA,
    displayName = "VSLA",
    tagline = "Village Savings and Loan Association",
    savingsMechanism = SavingsMechanism.ACCUMULATING,
    contributionMode = ContributionMode.SHARE_BASED_VARIABLE,
    lendingEnabled = true,
    hasSocialFund = true,
    hasBankLinkage = false,
    welfareOnlyMode = false,
    formallyRegistered = false,
    defaultLoanMultiplier = 3.0,
    defaultInterestRatePct = 10.0,
    defaultCycleLengthMonths = 12,
    maxMembers = 20,
    minMembers = 5,
)

private val demoRoscaTypeConfig = GroupTypeConfig(
    typeSlug = GroupTypeSlug.ROSCA,
    displayName = "ROSCA",
    tagline = "Rotating Savings and Credit Association",
    savingsMechanism = SavingsMechanism.ROTATING_PAYOUT,
    contributionMode = ContributionMode.FIXED,
    lendingEnabled = false,
    hasSocialFund = false,
    hasBankLinkage = false,
    welfareOnlyMode = false,
    formallyRegistered = false,
    defaultLoanMultiplier = 2.0,
    defaultInterestRatePct = 0.0,
    defaultCycleLengthMonths = 10,
    maxMembers = 10,
    minMembers = 5,
)

/** Fully-filled VSLA wizard state (demo-data.yaml `CreateGroupOrchestrationRequest` #1 — Mwangaza Women's Group). */
private val demoVslaReviewState = GroupCreateState(
    currentStep = 4,
    typeConfig = demoVslaTypeConfig,
    groupTypeName = "VSLA",
    groupName = "Mwangaza Women's Group",
    officeId = 10L,
    officeName = "Kisumu West Branch",
    currency = "KES",
    meetingDay = "Monday",
    meetingTime = "09:00",
    shareValue = "200",
    shareMin = "1",
    shareMax = "5",
    loanMultiplier = "3",
    interestRate = "10",
    cycleLengthMonths = "12",
    fineAmount = "50",
    socialFundEnabled = true,
    socialFundPercent = "5",
    maxMembers = "20",
    officeList = demoOffices,
)

/**
 * `@Preview` gallery for `GroupCreateScreen.kt`. See API.md#preview. Data source: `demo-data.yaml`
 * (`entries[dto=Office]`, `entries[dto=GroupTypeConfig]` — VSLA/ROSCA — and `entries[dto=
 * CreateGroupOrchestrationRequest]` — Mwangaza Women's Group / Kilimani Merry-Go-Round) plus
 * `ui.yaml#states.*.demo_data`.
 */
private class GroupCreateScreenPreviewProvider : PreviewParameterProvider<GroupCreateState> {
    override val values: Sequence<GroupCreateState> = sequenceOf(
        // Step 1 — Identity (ui.yaml#states.content.demo_data)
        GroupCreateState(
            currentStep = 1,
            typeConfig = demoVslaTypeConfig,
            groupTypeName = "VSLA",
            groupName = "Mwangaza Women's Group",
            officeName = "",
            meetingDay = "",
            meetingTime = "",
            officeList = demoOffices,
        ),
        // Step 2 — Rules, VSLA share-based + social fund
        GroupCreateState(
            currentStep = 2,
            typeConfig = demoVslaTypeConfig,
            groupTypeName = "VSLA",
            groupName = "Mwangaza Women's Group",
            officeId = 10L,
            officeName = "Kisumu West Branch",
            meetingDay = "Monday",
            meetingTime = "09:00",
            shareValue = "200",
            shareMin = "1",
            shareMax = "5",
            socialFundEnabled = true,
            officeList = demoOffices,
        ),
        // Step 2 — Rules, ROSCA fixed + rotating payout (demo-data.yaml Kilimani Merry-Go-Round)
        GroupCreateState(
            currentStep = 2,
            typeConfig = demoRoscaTypeConfig,
            groupTypeName = "ROSCA",
            groupName = "Kilimani Merry-Go-Round",
            officeId = 11L,
            officeName = "Kisumu East Branch",
            meetingDay = "Friday",
            meetingTime = "18:00",
            contributionAmount = "500",
            payoutOrderMethod = "LOTTERY",
            loanMultiplier = "2",
            interestRate = "0",
            cycleLengthMonths = "10",
            fineAmount = "100",
            socialFundEnabled = false,
            officeList = demoOffices,
        ),
        // Step 3 — Members
        GroupCreateState(
            currentStep = 3,
            typeConfig = demoVslaTypeConfig,
            groupTypeName = "VSLA",
            groupName = "Mwangaza Women's Group",
            maxMembers = "20",
            officeList = demoOffices,
        ),
        // Step 4 — Review (Content)
        demoVslaReviewState,
        // Submitting
        demoVslaReviewState.copy(isSubmitting = true),
        // Error — offline, retry queued (ui.yaml#states.error.demo_data)
        demoVslaReviewState.copy(error = GroupCreateError.Network, isOffline = true),
        // Success — inviteCode from demo-data.yaml CreateGroupOrchestrationResponse #1
        demoVslaReviewState.copy(isSubmitting = false, isSubmitSuccess = true, inviteCode = "MWG001"),
    )
}

@Preview
@Composable
private fun GroupCreateContentPreview(
    @PreviewParameter(GroupCreateScreenPreviewProvider::class)
    state: GroupCreateState,
) {
    KptTheme {
        GroupCreateContent(state = state, onAction = {})
    }
}

@Preview
@Composable
private fun GroupCreateWizardSectionPreview() {
    KptTheme {
        GroupCreateWizardSection(
            state = GroupCreateState(
                currentStep = 1,
                typeConfig = demoVslaTypeConfig,
                groupTypeName = "VSLA",
                officeList = demoOffices,
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun GroupIdentityStepSectionPreview() {
    KptTheme {
        GroupIdentityStepSection(
            state = GroupCreateState(
                currentStep = 1,
                typeConfig = demoVslaTypeConfig,
                groupTypeName = "VSLA",
                groupName = "Mwangaza Women's Group",
                officeList = demoOffices,
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun GroupRulesStepSectionSharePreview() {
    KptTheme {
        GroupRulesStepSection(
            state = GroupCreateState(
                currentStep = 2,
                typeConfig = demoVslaTypeConfig,
                groupTypeName = "VSLA",
                shareValue = "200",
                shareMin = "1",
                shareMax = "5",
                socialFundEnabled = true,
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun GroupRulesStepSectionFixedRotatingPreview() {
    KptTheme {
        GroupRulesStepSection(
            state = GroupCreateState(
                currentStep = 2,
                typeConfig = demoRoscaTypeConfig,
                groupTypeName = "ROSCA",
                contributionAmount = "500",
                payoutOrderMethod = "LOTTERY",
                socialFundEnabled = false,
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun GroupMembersStepSectionPreview() {
    KptTheme {
        GroupMembersStepSection(
            state = GroupCreateState(currentStep = 3, typeConfig = demoVslaTypeConfig, maxMembers = "20"),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun GroupReviewStepSectionPreview() {
    KptTheme {
        GroupReviewStepSection(state = demoVslaReviewState, onAction = {}, isSubmitting = false)
    }
}

@Preview
@Composable
private fun GroupCreateSubmittingSectionPreview() {
    KptTheme {
        GroupCreateSubmittingSection(state = demoVslaReviewState.copy(isSubmitting = true), onAction = {})
    }
}

@Preview
@Composable
private fun GroupCreateErrorSectionPreview() {
    KptTheme {
        GroupCreateErrorSection(
            state = demoVslaReviewState.copy(error = GroupCreateError.Network, isOffline = true),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun GroupCreateSuccessSectionPreview() {
    KptTheme {
        GroupCreateSuccessSection(
            state = demoVslaReviewState.copy(isSubmitting = false, isSubmitSuccess = true, inviteCode = "MWG001"),
        )
    }
}

@Preview
@Composable
private fun OfflineSyncDialogPreview() {
    KptTheme {
        OfflineSyncDialog(onDismiss = {})
    }
}

@Preview
@Composable
private fun GroupCreateReviewCardBodyPreview() {
    KptTheme {
        GroupCreateReviewCardBody(state = demoVslaReviewState)
    }
}
