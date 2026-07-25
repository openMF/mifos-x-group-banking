/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.meetingconduct

/**
 * Append-only test-tag registry for the `meeting-conduct` feature (RULE-KMP-COMPOSE-UITEST-001 CU-5
 * — names are stable across regenerations; only append new entries, never rename or remove).
 * Constant names derived 1:1 from `ui.yaml#components[].id`. See API.md#tags.
 */
object MeetingConductTestTags {

    /** Root [kpt.core.ui.scaffold.KptScaffold] surface. */
    const val SCREEN: String = "meeting_conduct_screen"

    /** `ui.yaml#components.top_app_bar` close/back navigation icon. */
    const val CLOSE_ACTION: String = "meeting_conduct_close_action"

    /** `ui.yaml#components.offline_badge`. */
    const val OFFLINE_BADGE: String = "meeting_conduct_offline_badge"

    /** `ui.yaml#components.step_stepper`. */
    const val STEPPER: String = "meeting_conduct_stepper"

    /** `ui.yaml#components.corpus_band` (steps 2-6). */
    const val CORPUS_BAND: String = "meeting_conduct_corpus_band"

    /** `ui.yaml#components.step0_previous_review`. */
    const val STEP0_PREVIOUS_REVIEW: String = "meeting_conduct_step0_previous_review"

    /** `ui.yaml#components.view_full_previous_btn`. */
    const val VIEW_FULL_PREVIOUS_BUTTON: String = "meeting_conduct_view_full_previous_button"

    /** `ui.yaml#components.step1_attendance`. */
    const val STEP1_ATTENDANCE: String = "meeting_conduct_step1_attendance"

    /** `ui.yaml#components.step2_opening_balance`. */
    const val STEP2_OPENING_BALANCE: String = "meeting_conduct_step2_opening_balance"

    /** `ui.yaml#components.step3_savings_collection`. */
    const val STEP3_SAVINGS: String = "meeting_conduct_step3_savings"

    /** `ui.yaml#components.step4_loan_review`. */
    const val STEP4_LOAN_REVIEW: String = "meeting_conduct_step4_loan_review"

    /** `ui.yaml#components.step5_loan_applications`. */
    const val STEP5_LOAN_APPLICATIONS: String = "meeting_conduct_step5_loan_applications"

    /** `ui.yaml#components.step6_closing_balance`. */
    const val STEP6_CLOSING_BALANCE: String = "meeting_conduct_step6_closing_balance"

    /** `ui.yaml#components.reconciliation_card`. */
    const val RECONCILIATION_CARD: String = "meeting_conduct_reconciliation_card"

    /** `ui.yaml#components.submit_meeting_btn`. */
    const val SUBMIT_BUTTON: String = "meeting_conduct_submit_button"

    /** `ui.yaml#components.wizard_footer.back_footer_btn`. */
    const val BACK_FOOTER_BUTTON: String = "meeting_conduct_back_footer_button"

    /** `ui.yaml#components.wizard_footer.next_footer_btn`. */
    const val NEXT_FOOTER_BUTTON: String = "meeting_conduct_next_footer_button"

    /** `ui.yaml#states.loading` centered progress indicator. */
    const val LOADING_INDICATOR: String = "meeting_conduct_loading_indicator"

    /** `ui.yaml#components.step_validation_error_snackbar` host anchor. */
    const val VALIDATION_ERROR: String = "meeting_conduct_validation_error"

    /** Per-member attendance segmented row. Data-driven from `groupMembers`, hence a function. */
    fun attendanceRowTag(memberId: String): String = "meeting_conduct_attendance_row_$memberId"

    /** Per-member savings input row. */
    fun savingsRowTag(memberId: String): String = "meeting_conduct_savings_row_$memberId"

    /** Per-loan review row. */
    fun loanReviewRowTag(loanId: String): String = "meeting_conduct_loan_review_row_$loanId"

    /** Per-application loan card. */
    fun loanApplicationCardTag(applicationId: String): String = "meeting_conduct_loan_app_card_$applicationId"
}
