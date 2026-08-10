/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.loanrequest

import androidx.compose.runtime.Composable
import kpt.core.designsystem.theme.KptTheme
import kpt.core.model.LoanPurpose
import kpt.feature.loanrequest.generated.resources.Res
import kpt.feature.loanrequest.generated.resources.screens_loan_request_submit_button
import kpt.feature.loanrequest.generated.resources.screens_loan_request_submit_cd
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider

// -- ui.yaml#states.*.demo_data-shaped fixtures --------------------------------------------------

/** `ui.yaml#states.content` -- fresh form, no amount typed yet, no purpose selected. */
private val demoContentEmptyState = LoanRequestState(
    clientId = 1L,
    savingsBalance = 5000.0,
    loanMultiplier = 3.0,
    maxLoanAmount = 15000.0,
)

/** `ui.yaml#states.content` -- amount + purpose populated, `repayment_summary_card` visible. */
private val demoContentFilledState = demoContentEmptyState.copy(
    requestedAmount = "3000",
    purpose = LoanPurpose.BUSINESS,
    durationWeeks = 12,
    repaymentEstimate = 275.0,
    isFormValid = true,
)

/** `ui.yaml#components.offline_mode_banner.visible_when: "isOfflineMode == true"`. */
private val demoOfflineModeState = demoContentEmptyState.copy(isOfflineMode = true)

/** `ui.yaml#states.submitting` -- form frozen, `submitting_indicator` shown. */
private val demoSubmittingState = demoContentFilledState.copy(isSubmitting = true)

/** `ui.yaml#states.submit_error` -- `SubmitError.Validation` (non-retryable per taxonomy). */
private val demoSubmitErrorState = demoContentFilledState.copy(
    isSubmitting = false,
    submitError = SubmitError.Validation,
)

/** `ui.yaml#states.submit_success` -- online submission, `success_dialog` w/ online copy. */
private val demoSubmitSuccessState = demoContentFilledState.copy(
    isSubmitting = false,
    successDialogVisible = true,
)

/** `ui.yaml#states.offline_queued` -- offline submission, `success_dialog` w/ offline copy. */
private val demoOfflineQueuedState = demoContentFilledState.copy(
    isSubmitting = false,
    isOfflineMode = true,
    successDialogVisible = true,
)

/**
 * `@Preview` gallery for `LoanRequestScreen.kt`. See API.md#preview. Data source: hand-authored
 * stubs shaped after `ui.yaml#states.*` (no `demo-data.yaml` was resolved for this generation
 * pass).
 */
private class LoanRequestScreenPreviewProvider : PreviewParameterProvider<LoanRequestState> {
    override val values: Sequence<LoanRequestState> = sequenceOf(
        demoContentEmptyState,
        demoContentFilledState,
        demoOfflineModeState,
        demoSubmittingState,
        demoSubmitErrorState,
        demoSubmitSuccessState,
        demoOfflineQueuedState,
    )
}

@Preview
@Composable
private fun LoanRequestContentPreview(
    @PreviewParameter(LoanRequestScreenPreviewProvider::class)
    state: LoanRequestState,
) {
    KptTheme {
        LoanRequestContent(state = state, onAction = {}, onNavigateBack = {})
    }
}

@Preview
@Composable
private fun LoanRequestFormFieldsContentEmptyPreview() {
    KptTheme {
        LoanRequestFormFields(
            state = demoContentEmptyState,
            onAction = {},
            screenState = demoContentEmptyState.deriveScreenState(),
        )
    }
}

@Preview
@Composable
private fun LoanRequestFormFieldsContentFilledPreview() {
    KptTheme {
        LoanRequestFormFields(
            state = demoContentFilledState,
            onAction = {},
            screenState = demoContentFilledState.deriveScreenState(),
        )
    }
}

@Preview
@Composable
private fun LoanRequestFormFieldsOfflinePreview() {
    KptTheme {
        LoanRequestFormFields(
            state = demoOfflineModeState,
            onAction = {},
            screenState = demoOfflineModeState.deriveScreenState(),
        )
    }
}

@Preview
@Composable
private fun LoanRequestFormFieldsSubmittingPreview() {
    KptTheme {
        LoanRequestFormFields(
            state = demoSubmittingState,
            onAction = {},
            screenState = demoSubmittingState.deriveScreenState(),
        )
    }
}

@Preview
@Composable
private fun LoanRequestFormFieldsSubmitErrorPreview() {
    KptTheme {
        LoanRequestFormFields(
            state = demoSubmitErrorState,
            onAction = {},
            screenState = demoSubmitErrorState.deriveScreenState(),
        )
    }
}

@Preview
@Composable
private fun LoanRequestSubmitButtonEnabledPreview() {
    KptTheme {
        LoanRequestSubmitButton(
            label = stringResource(Res.string.screens_loan_request_submit_button),
            contentDescription = stringResource(Res.string.screens_loan_request_submit_cd),
            enabled = true,
            onClick = {},
        )
    }
}

@Preview
@Composable
private fun LoanRequestSubmitButtonDisabledPreview() {
    KptTheme {
        LoanRequestSubmitButton(
            label = stringResource(Res.string.screens_loan_request_submit_button),
            contentDescription = stringResource(Res.string.screens_loan_request_submit_cd),
            enabled = false,
            onClick = {},
        )
    }
}
