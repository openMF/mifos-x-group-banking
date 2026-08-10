/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.grouptypepicker

import androidx.compose.runtime.Composable
import kpt.core.designsystem.theme.KptTheme
import kpt.core.model.ContributionMode
import kpt.core.model.GroupTypeConfig
import kpt.core.model.GroupTypeSlug
import kpt.core.model.SavingsMechanism
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider

/**
 * `@Preview` gallery for `GroupTypePickerScreen.kt`. See API.md#preview. Data source:
 * `demo-data.yaml#entries[0].items` — the full 9-row seeded group-type catalogue (COMP-DT-003),
 * verbatim field values (no placeholder literals, RULE-PREVIEW-7).
 */
private val previewTypeConfigs: List<GroupTypeConfig> = listOf(
    GroupTypeConfig(
        typeSlug = GroupTypeSlug.VSLA,
        displayName = "Village Savings & Loan Association",
        tagline = "Save in shares each meeting; borrow up to 3× your shares; share-out at year end",
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
        minMembers = 15,
        maxMembers = 30,
    ),
    GroupTypeConfig(
        typeSlug = GroupTypeSlug.ROSCA,
        displayName = "Rotating Savings & Credit Association",
        tagline = "Fixed pot rotates — one member takes all each period (susu, tanda, hui, committee)",
        savingsMechanism = SavingsMechanism.ROTATING_PAYOUT,
        contributionMode = ContributionMode.FIXED,
        lendingEnabled = false,
        hasSocialFund = false,
        hasBankLinkage = false,
        welfareOnlyMode = false,
        formallyRegistered = false,
        defaultLoanMultiplier = 0.0,
        defaultInterestRatePct = 0.0,
        defaultCycleLengthMonths = 0,
        minMembers = 5,
        maxMembers = 20,
    ),
    GroupTypeConfig(
        typeSlug = GroupTypeSlug.ASCA,
        displayName = "Accumulating Savings & Credit Association",
        tagline = "Fixed contributions build a fund; members borrow and pay interest; bookkeeping required",
        savingsMechanism = SavingsMechanism.ACCUMULATING,
        contributionMode = ContributionMode.FIXED,
        lendingEnabled = true,
        hasSocialFund = false,
        hasBankLinkage = false,
        welfareOnlyMode = false,
        formallyRegistered = false,
        defaultLoanMultiplier = 2.0,
        defaultInterestRatePct = 5.0,
        defaultCycleLengthMonths = 12,
        minMembers = 10,
        maxMembers = 25,
    ),
    GroupTypeConfig(
        typeSlug = GroupTypeSlug.SILC,
        displayName = "Savings & Internal Lending Community",
        tagline = "CRS-promoted VSLA variant with share-based saving and internal lending",
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
        minMembers = 15,
        maxMembers = 30,
    ),
    GroupTypeConfig(
        typeSlug = GroupTypeSlug.SHG,
        displayName = "Self-Help Group",
        tagline = "Save regularly; borrow from internal fund; qualify for bank linkage loan (1:1–4:1)",
        savingsMechanism = SavingsMechanism.ACCUMULATING,
        contributionMode = ContributionMode.FIXED,
        lendingEnabled = true,
        hasSocialFund = false,
        hasBankLinkage = true,
        welfareOnlyMode = false,
        formallyRegistered = false,
        defaultLoanMultiplier = 2.0,
        defaultInterestRatePct = 12.0,
        defaultCycleLengthMonths = 12,
        minMembers = 10,
        maxMembers = 20,
    ),
    GroupTypeConfig(
        typeSlug = GroupTypeSlug.SACCO,
        displayName = "SACCO / Credit Union",
        tagline = "Registered co-operative; share capital; elected board; regulated by authority",
        savingsMechanism = SavingsMechanism.ACCUMULATING,
        contributionMode = ContributionMode.FIXED,
        lendingEnabled = true,
        hasSocialFund = false,
        hasBankLinkage = false,
        welfareOnlyMode = false,
        formallyRegistered = true,
        defaultLoanMultiplier = 3.0,
        defaultInterestRatePct = 12.0,
        defaultCycleLengthMonths = 12,
        minMembers = 30,
        maxMembers = 500,
    ),
    GroupTypeConfig(
        typeSlug = GroupTypeSlug.CBO_VILLAGE_BANK,
        displayName = "CBO / Village Bank",
        tagline = "External MFI loan on-lent to members alongside internal savings (FINCA model)",
        savingsMechanism = SavingsMechanism.ACCUMULATING,
        contributionMode = ContributionMode.FIXED,
        lendingEnabled = true,
        hasSocialFund = false,
        hasBankLinkage = true,
        welfareOnlyMode = false,
        formallyRegistered = false,
        defaultLoanMultiplier = 1.0,
        defaultInterestRatePct = 3.0,
        defaultCycleLengthMonths = 4,
        minMembers = 20,
        maxMembers = 50,
    ),
    GroupTypeConfig(
        typeSlug = GroupTypeSlug.BURIAL_WELFARE,
        displayName = "Burial / Welfare Society",
        tagline = "Welfare fund IS the purpose; member benefits on life events; can nest inside other groups",
        savingsMechanism = SavingsMechanism.ACCUMULATING,
        contributionMode = ContributionMode.FIXED,
        lendingEnabled = false,
        hasSocialFund = true,
        hasBankLinkage = false,
        welfareOnlyMode = true,
        formallyRegistered = false,
        defaultLoanMultiplier = 0.0,
        defaultInterestRatePct = 0.0,
        defaultCycleLengthMonths = 12,
        minMembers = 5,
        maxMembers = 100,
    ),
    GroupTypeConfig(
        typeSlug = GroupTypeSlug.JLG,
        displayName = "Joint Liability Group",
        tagline = "4–10 members take an external MFI loan jointly with mutual guarantee; no internal savings pot",
        savingsMechanism = SavingsMechanism.NONE,
        contributionMode = ContributionMode.MINIMAL,
        lendingEnabled = false,
        hasSocialFund = false,
        hasBankLinkage = true,
        welfareOnlyMode = false,
        formallyRegistered = false,
        defaultLoanMultiplier = 0.0,
        defaultInterestRatePct = 0.0,
        defaultCycleLengthMonths = 0,
        minMembers = 4,
        maxMembers = 10,
    ),
)

private class GroupTypePickerScreenPreviewProvider : PreviewParameterProvider<GroupTypePickerState> {
    override val values: Sequence<GroupTypePickerState> = sequenceOf(
        // loading — spinner + skeleton rows, no catalogue yet
        GroupTypePickerState(isLoading = true, typeConfigs = emptyList(), error = null),
        // content — all 9 seeded types (ui.yaml#states.content.demo_data + demo-data.yaml)
        GroupTypePickerState(isLoading = false, typeConfigs = previewTypeConfigs, error = null),
        // error — COMP-DT-003 network failure (ui.yaml#state_model.errors.types[0])
        GroupTypePickerState(isLoading = false, typeConfigs = emptyList(), error = GroupTypePickerError.Network),
    )
}

@Preview
@Composable
private fun GroupTypePickerScreenPreview(
    @PreviewParameter(GroupTypePickerScreenPreviewProvider::class)
    state: GroupTypePickerState,
) {
    KptTheme {
        GroupTypePickerContent(state = state, onAction = {})
    }
}

@Preview
@Composable
private fun GroupTypeLoadingSectionPreview() {
    KptTheme {
        GroupTypeLoadingSection()
    }
}

@Preview
@Composable
private fun GroupTypeListSectionPreview() {
    KptTheme {
        GroupTypeListSection(typeConfigs = previewTypeConfigs, onAction = {})
    }
}
