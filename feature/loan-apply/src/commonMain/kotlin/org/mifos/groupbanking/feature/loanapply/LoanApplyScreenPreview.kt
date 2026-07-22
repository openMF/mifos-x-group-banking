/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.loanapply

import androidx.compose.runtime.Composable
import kpt.core.designsystem.theme.KptTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider
import org.mifos.groupbanking.core.model.GroupMember
import org.mifos.groupbanking.core.model.LoanProduct
import org.mifos.groupbanking.core.model.LoanPurpose

// -- ui.yaml#states.content.demo_data fixtures -------------------------------------------------

/** `ui.yaml#states.content.demo_data.selectedMember` -- Peter Otieno. */
private val demoMember = GroupMember(id = 1, displayName = "Peter Otieno", imagePresent = false, fineractClientId = 1001)

private val demoMembers = listOf(
    demoMember,
    GroupMember(id = 2, displayName = "Grace Achieng", imagePresent = false, fineractClientId = 1002),
    GroupMember(id = 3, displayName = "Faith Mutua", imagePresent = false, fineractClientId = 1003),
)

private val demoProduct = LoanProduct(
    id = 1,
    name = "Group Working Capital Loan",
    shortName = "GWC",
    principal = 15000.0,
    minPrincipal = 1000.0,
    maxPrincipal = 50000.0,
    numberOfRepayments = 12,
    interestRatePerPeriod = 2.0,
)

private val demoProducts = listOf(
    demoProduct,
    LoanProduct(
        id = 2,
        name = "Emergency Relief Loan",
        shortName = "EMR",
        principal = 5000.0,
        minPrincipal = 500.0,
        maxPrincipal = 10000.0,
        numberOfRepayments = 8,
        interestRatePerPeriod = 1.5,
    ),
)

/** `ui.yaml#states.content.demo_data` verbatim -- member + product selected, form fully populated. */
private val demoContentState = LoanApplyState(
    selectedMember = demoMember,
    requestedAmount = "1500",
    durationWeeks = 12,
    purpose = LoanPurpose.BUSINESS,
    selectedProduct = demoProduct,
    members = demoMembers,
    loanProducts = demoProducts,
    memberSavingsBalance = 5000.0,
    loanMultiplier = 3.0,
    eligibleAmount = 15000.0,
    corpusBalance = 24000.0,
)

/** Fresh form -- no member selected yet, eligibility banner hidden. */
private val demoContentEmptyState = LoanApplyState(
    members = demoMembers,
    loanProducts = demoProducts,
)

/** `ui.yaml#components.corpus_warning_banner.visible_when: corpusWarning` -- amber banner shown. */
private val demoCorpusWarningState = demoContentState.copy(
    requestedAmount = "26000",
    corpusWarning = true,
)

/** `ui.yaml#states.submitting.description` -- form frozen, in-flight `post_loan` call. */
private val demoSubmittingState = demoContentState.copy(isSubmitting = true)

/** `ui.yaml#states.loading.description` -- initial mount, also reused for template reload. */
private val demoLoadingState = LoanApplyState(isLoadingTemplate = true)

/** `ui.yaml#states.error` -- `LoanApplyError.AmountExceedsEligibility` (non-retryable). */
private val demoErrorAmountState = demoContentState.copy(isSubmitting = false, error = LoanApplyError.AmountExceedsEligibility)

/** `LoanApplyError.Network` (retryable) -- offline submit attempt. */
private val demoErrorNetworkState = demoContentState.copy(isSubmitting = false, error = LoanApplyError.Network)

/** Transient `ui.yaml#states.success`-equivalent frame -- `NavigateToMeetingConduct` fires the same frame. */
private val demoSuccessState = demoContentState.copy(isSubmitting = false, submitSuccess = true)

/**
 * `@Preview` gallery for `LoanApplyScreen.kt`. See API.md#preview. Data source: `ui.yaml#states.*.demo_data`
 * (`content` block verbatim; `loading`/`submitting`/`error` derived per each state's own description).
 */
private class LoanApplyScreenPreviewProvider : PreviewParameterProvider<LoanApplyState> {
    override val values: Sequence<LoanApplyState> = sequenceOf(
        demoLoadingState,
        demoContentEmptyState,
        demoContentState,
        demoCorpusWarningState,
        demoSubmittingState,
        demoErrorAmountState,
        demoErrorNetworkState,
        demoSuccessState,
    )
}

@Preview
@Composable
private fun LoanApplyContentPreview(
    @PreviewParameter(LoanApplyScreenPreviewProvider::class)
    state: LoanApplyState,
) {
    KptTheme {
        LoanApplyContent(state = state, onAction = {})
    }
}

@Preview
@Composable
private fun LoanApplyLoadingSectionPreview() {
    KptTheme {
        LoanApplyLoadingSection()
    }
}

@Preview
@Composable
private fun LoanApplyFormSectionContentPreview() {
    KptTheme {
        LoanApplyFormSection(state = demoContentState, onAction = {}, enabled = true)
    }
}

@Preview
@Composable
private fun LoanApplyFormSectionCorpusWarningPreview() {
    KptTheme {
        LoanApplyFormSection(state = demoCorpusWarningState, onAction = {}, enabled = true)
    }
}

@Preview
@Composable
private fun LoanApplyFormSectionSubmittingPreview() {
    KptTheme {
        LoanApplyFormSection(state = demoSubmittingState, onAction = {}, enabled = false)
    }
}

@Preview
@Composable
private fun LoanApplyErrorSectionPreview() {
    KptTheme {
        LoanApplyErrorSection(state = demoErrorAmountState, onAction = {})
    }
}

@Preview
@Composable
private fun LoanApplySuccessSectionPreview() {
    KptTheme {
        LoanApplySuccessSection()
    }
}
