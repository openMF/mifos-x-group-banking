/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.loanrepaymentdialog

/**
 * Append-only test-tag registry for the `loan-repayment-dialog` feature (RULE-KMP-COMPOSE-UITEST-001
 * CU-5 — names are stable across regenerations; only append new entries, never rename or remove).
 * Consumed by Compose UI tests under `feature/loan-repayment-dialog/src/commonTest/` and by the
 * Maestro flow generator (`core/scripts/maestro-flow-gen.ts`), which emits `tapOn: { id:
 * "<test_tag>" }` selectors from these constants. Constant names are derived 1:1 from
 * `ui.yaml#components[].id`. Mirrors `LoanDetailTestTags` / `MemberAddTestTags`'s identical
 * convention.
 *
 * See API.md#tags.
 */
object LoanRepaymentDialogTestTags {

    /** Root [androidx.compose.material3.AlertDialog] surface — always rendered while the dialog is shown. */
    const val DIALOG: String = "loan_repayment_dialog"

    /** `ui.yaml#components.amount_field` — [LoanRepaymentDialogAction.OnAmountChanged]. */
    const val AMOUNT_FIELD: String = "loan_repayment_dialog_amount_field"

    /** `ui.yaml#components.payment_method_chips` — the enclosing chip row. */
    const val PAYMENT_METHOD_CHIPS: String = "loan_repayment_dialog_payment_method_chips"

    /** `ui.yaml#components.payment_method_chips.chips.chip_mpesa` — [LoanRepaymentDialogAction.OnPaymentMethodSelected]. */
    const val CHIP_MPESA: String = "loan_repayment_dialog_chip_mpesa"

    /** `ui.yaml#components.payment_method_chips.chips.chip_cash` — [LoanRepaymentDialogAction.OnPaymentMethodSelected]. */
    const val CHIP_CASH: String = "loan_repayment_dialog_chip_cash"

    /** `ui.yaml#components.reference_number_field` — [LoanRepaymentDialogAction.OnReferenceNumberChanged]. */
    const val REFERENCE_FIELD: String = "loan_repayment_dialog_reference_field"

    /** `ui.yaml#components.submit_error_text` — visible when `state.submitError != null`. */
    const val SUBMIT_ERROR_TEXT: String = "loan_repayment_dialog_submit_error_text"

    /** `ui.yaml#components.dialog_actions_row.content.cancel_button` — [LoanRepaymentDialogAction.OnDismiss]. */
    const val CANCEL_BUTTON: String = "loan_repayment_dialog_cancel_button"

    /** `ui.yaml#components.dialog_actions_row.content.record_repayment_button` — [LoanRepaymentDialogAction.OnSubmit]. */
    const val SUBMIT_BUTTON: String = "loan_repayment_dialog_submit_button"
}
