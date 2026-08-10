/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.loanapply

/**
 * Append-only test-tag registry for the `loan-apply` feature (RULE-KMP-COMPOSE-UITEST-001
 * CU-5 -- names are stable across regenerations; only append new entries, never rename or
 * remove). Consumed by Compose UI tests under `feature/loan-apply/src/commonTest/` and by the
 * Maestro flow generator (`core/scripts/maestro-flow-gen.ts`), which emits
 * `tapOn: { id: "<test_tag>" }` selectors from these constants. See API.md#tags.
 */
object LoanApplyTestTags {

    /** Root [kpt.core.ui.scaffold.KptScaffold] surface -- always rendered regardless of screenState. */
    const val SCREEN: String = "loan_apply_screen"

    /** Top app bar (`ui.yaml#components.top_bar`) -- back icon dispatches `OnBack`. */
    const val TOP_BAR: String = "loan_apply_top_bar"

    /** Centered spinner shown during `LoanApplyScreenState.Loading`. */
    const val LOADING_INDICATOR: String = "loan_apply_loading_indicator"

    // -- Form fields -------------------------------------------------------------------------------

    /** `member_selector` dropdown. */
    const val DROPDOWN_MEMBER: String = "loan_apply_dropdown_member"

    /** `eligibility_banner` -- visible only when a member is selected. */
    const val ELIGIBILITY_BANNER: String = "loan_apply_eligibility_banner"

    /** `amount_input` text field. */
    const val FIELD_AMOUNT: String = "loan_apply_field_amount"

    /** `corpus_warning_banner` -- visible only when `LoanApplyState.corpusWarning`. */
    const val CORPUS_WARNING_BANNER: String = "loan_apply_corpus_warning_banner"

    /** `duration_dropdown`. */
    const val DROPDOWN_DURATION: String = "loan_apply_dropdown_duration"

    /** `purpose_dropdown`. */
    const val DROPDOWN_PURPOSE: String = "loan_apply_dropdown_purpose"

    /** `product_dropdown`. */
    const val DROPDOWN_PRODUCT: String = "loan_apply_dropdown_product"

    // -- Submit / error / success --------------------------------------------------------------------

    /** `submit_button` -- always labelled "Submit Application"; shows a spinner while `isSubmitting`. */
    const val SUBMIT_BUTTON: String = "loan_apply_submit_button"

    /** Full-screen `error_state` surface -- `LoanApplyScreenState.Error`. */
    const val ERROR_SECTION: String = "loan_apply_error_section"

    /** `error_state.cta` retry affordance embedded in [ERROR_SECTION]. */
    const val ERROR_RETRY_BUTTON: String = "loan_apply_error_retry_button"

    /** Transient success surface -- `LoanApplyScreenState.Success` (nav fires the same frame). */
    const val SUCCESS_SECTION: String = "loan_apply_success_section"
}
