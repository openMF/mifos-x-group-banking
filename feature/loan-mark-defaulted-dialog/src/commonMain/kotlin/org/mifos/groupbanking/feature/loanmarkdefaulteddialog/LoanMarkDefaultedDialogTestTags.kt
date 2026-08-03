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

/**
 * Append-only test-tag registry for the `loan-mark-defaulted-dialog` feature
 * (RULE-KMP-COMPOSE-UITEST-001 CU-5 — names are stable across regenerations; only append new
 * entries, never rename or remove). Consumed by Compose UI tests under
 * `feature/loan-mark-defaulted-dialog/src/commonTest/` and by the Maestro flow generator
 * (`core/scripts/maestro-flow-gen.ts`), which emits `tapOn: { id: "<test_tag>" }` selectors from
 * these constants. Constant names are derived 1:1 from `ui.yaml#components[].id`. Mirrors
 * `LoanRepaymentDialogTestTags`'s identical convention.
 *
 * See API.md#tags.
 */
object LoanMarkDefaultedDialogTestTags {

    /** Root [androidx.compose.material3.AlertDialog] surface — always rendered while the dialog is shown. */
    const val DIALOG: String = "loan_mark_defaulted_dialog"

    /** `ui.yaml#components.warning_icon` — decorative warning glyph, always rendered. */
    const val WARNING_ICON: String = "loan_mark_defaulted_dialog_warning_icon"

    /** `ui.yaml#components.warning_body_text` — always rendered, interpolates memberName + loanAmountKes. */
    const val WARNING_BODY_TEXT: String = "loan_mark_defaulted_dialog_warning_body_text"

    /** `ui.yaml#components.submit_error_text` — visible when `state.submitError != null`. */
    const val SUBMIT_ERROR_TEXT: String = "loan_mark_defaulted_dialog_submit_error_text"

    /** `ui.yaml#components.dialog_actions_row.content.cancel_button` — [LoanMarkDefaultedDialogAction.OnDismiss]. */
    const val CANCEL_BUTTON: String = "loan_mark_defaulted_dialog_cancel_button"

    /** `ui.yaml#components.dialog_actions_row.content.confirm_defaulted_button` — [LoanMarkDefaultedDialogAction.OnConfirm]. */
    const val CONFIRM_BUTTON: String = "loan_mark_defaulted_dialog_confirm_button"
}
