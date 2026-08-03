/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.loanrepaymentdialog

import androidx.compose.runtime.Composable
import kpt.core.designsystem.theme.KptTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider
import org.mifos.groupbanking.core.model.PaymentMethod

// -- ui.yaml#states.*.demo_data fixtures ---------------------------------------------------------

/** `ui.yaml#states.idle.demo_data` — KES 125.00 pre-filled from the loan's next installment, M-Pesa. */
private val demoIdleState = LoanRepaymentDialogState(
    amount = "125.00",
    paymentMethod = PaymentMethod.MPESA,
    referenceNumber = "",
    isSubmitting = false,
    amountError = null,
    submitError = null,
)

/** `ui.yaml#states.idle.demo_data` with a Cash selection + an M-Pesa receipt reference typed in. */
private val demoIdleCashWithReferenceState = demoIdleState.copy(
    paymentMethod = PaymentMethod.CASH,
    referenceNumber = "QJZ7X9A1BK",
)

/** `ui.yaml#states.submitting.description` — inputs frozen, in-flight `make_repayment` call. */
private val demoSubmittingState = demoIdleState.copy(isSubmitting = true)

/** Client-side validation failure — `LoanRepaymentDialogViewModel.validateAmount` empty-amount branch. */
private val demoValidationErrorState = demoIdleState.copy(amount = "", amountError = "error_amount_required")

/** `ui.yaml#states.error.description` — server-side rejection (`400 -> error_amount_exceeds`). */
private val demoErrorState = demoIdleState.copy(submitError = "error_amount_exceeds")

/**
 * `@Preview` gallery for `LoanRepaymentDialog.kt`. See API.md#preview. Data source:
 * `ui.yaml#states.*.demo_data` (no `demo-data.yaml` fixture file exists for this feature — flat
 * `LoanRepaymentDialogState`, same class as `MemberAddScreenPreview.kt`'s convention).
 */
private class LoanRepaymentDialogPreviewProvider : PreviewParameterProvider<LoanRepaymentDialogState> {
    override val values: Sequence<LoanRepaymentDialogState> = sequenceOf(
        demoIdleState,
        demoIdleCashWithReferenceState,
        demoSubmittingState,
        demoValidationErrorState,
        demoErrorState,
    )
}

@Preview
@Composable
private fun LoanRepaymentDialogContentPreview(
    @PreviewParameter(LoanRepaymentDialogPreviewProvider::class)
    state: LoanRepaymentDialogState,
) {
    KptTheme {
        LoanRepaymentDialogContent(state = state, onAction = {})
    }
}

@Preview
@Composable
private fun LoanRepaymentDialogFieldsPreview() {
    KptTheme {
        LoanRepaymentDialogFields(state = demoErrorState, onAction = {})
    }
}
