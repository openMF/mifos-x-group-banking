/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.loanmarkdefaulteddialog

import androidx.compose.runtime.Composable
import kpt.core.designsystem.theme.KptTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider

// -- ui.yaml#states.*.demo_data fixtures ---------------------------------------------------------

/** `ui.yaml#states.idle.demo_data` — Peter Otieno, KES 1500 outstanding. */
private val demoIdleState = LoanMarkDefaultedDialogState(
    isSubmitting = false,
    submitError = null,
    memberName = "Peter Otieno",
    loanAmountKes = 1500.0,
)

/** `ui.yaml#states.submitting.description` — Mark Defaulted button shows a loading indicator; both buttons disabled. */
private val demoSubmittingState = demoIdleState.copy(isSubmitting = true)

/** `ui.yaml#states.error.description` — server-side rejection rendered inline between body and action buttons. */
private val demoErrorState = demoIdleState.copy(submitError = "error_server")

/** Client-side connectivity gap — `LoanMarkDefaultedDialogViewModel`'s `OFFLINE_MESSAGE_KEY` branch. */
private val demoOfflineErrorState = demoIdleState.copy(submitError = "error_offline_no_queue")

/**
 * `@Preview` gallery for `LoanMarkDefaultedDialog.kt`. See API.md#preview. Data source:
 * `ui.yaml#states.*.demo_data` (no `demo-data.yaml` fixture file exists for this feature — flat
 * `LoanMarkDefaultedDialogState`, same class as `LoanRepaymentDialogPreview.kt`'s convention).
 */
private class LoanMarkDefaultedDialogPreviewProvider : PreviewParameterProvider<LoanMarkDefaultedDialogState> {
    override val values: Sequence<LoanMarkDefaultedDialogState> = sequenceOf(
        demoIdleState,
        demoSubmittingState,
        demoErrorState,
        demoOfflineErrorState,
    )
}

@Preview
@Composable
private fun LoanMarkDefaultedDialogContentPreview(
    @PreviewParameter(LoanMarkDefaultedDialogPreviewProvider::class)
    state: LoanMarkDefaultedDialogState,
) {
    KptTheme {
        LoanMarkDefaultedDialogContent(state = state, onAction = {})
    }
}

@Preview
@Composable
private fun LoanMarkDefaultedDialogBodyPreview() {
    KptTheme {
        LoanMarkDefaultedDialogBody(state = demoErrorState)
    }
}
