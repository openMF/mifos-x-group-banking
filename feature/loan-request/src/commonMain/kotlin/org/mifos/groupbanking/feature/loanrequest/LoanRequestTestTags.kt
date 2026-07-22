/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.loanrequest

/**
 * Append-only test-tag registry for the `loan-request` feature (RULE-KMP-COMPOSE-UITEST-001
 * CU-5 -- names are stable across regenerations; only append new entries, never rename or
 * remove). Consumed by Compose UI tests under `feature/loan-request/src/commonTest/` and by the
 * Maestro flow generator (`core/scripts/maestro-flow-gen.ts`), which emits
 * `tapOn: { id: "<test_tag>" }` selectors from these constants. See API.md#tags.
 */
object LoanRequestTestTags {

    /** Root [kpt.core.ui.scaffold.KptScaffold] surface -- always rendered regardless of screenState. */
    const val SCREEN: String = "loan_request_screen"

    /** Top app bar (`ui.yaml#components.top_bar`) -- back icon navigates straight to `onNavigateBack`. */
    const val TOP_BAR: String = "loan_request_top_bar"

    /** `offline_mode_banner` -- visible only when `LoanRequestState.isOfflineMode`. */
    const val OFFLINE_BANNER: String = "loan_request_offline_banner"

    /** `savings_limit_card` -- always rendered (savings balance + max loan eligibility rows). */
    const val SAVINGS_LIMIT_CARD: String = "loan_request_savings_limit_card"

    /** `amount_field` text field. */
    const val FIELD_AMOUNT: String = "loan_request_field_amount"

    /** `purpose_dropdown`. */
    const val DROPDOWN_PURPOSE: String = "loan_request_dropdown_purpose"

    /** `duration_selector` slider card. */
    const val DURATION_SLIDER: String = "loan_request_duration_slider"

    /** `repayment_summary_card` -- visible only when `requestedAmount` is valid + non-blank. */
    const val REPAYMENT_SUMMARY_CARD: String = "loan_request_repayment_summary_card"

    /** `submit_button` -- always labelled "Submit Application"; enabled per `isFormValid`. */
    const val SUBMIT_BUTTON: String = "loan_request_submit_button"

    /** `submitting_indicator` -- centered spinner shown while `isSubmitting`. */
    const val SUBMITTING_INDICATOR: String = "loan_request_submitting_indicator"

    /** `success_dialog` -- shown for both `SubmitSuccess` and `OfflineQueued` (copy differs). */
    const val SUCCESS_DIALOG: String = "loan_request_success_dialog"

    /** `success_dialog.confirm_button` -- dispatches `OnSuccessDialogDismiss`. */
    const val SUCCESS_DIALOG_OK_BUTTON: String = "loan_request_success_dialog_ok_button"

    /** `error_snackbar` -- inline errorContainer card, visible on `SubmitError`. */
    const val ERROR_CARD: String = "loan_request_error_card"

    /** `error_snackbar`'s retry affordance -- dispatches `OnRetry`. */
    const val ERROR_CARD_RETRY_BUTTON: String = "loan_request_error_card_retry_button"
}
