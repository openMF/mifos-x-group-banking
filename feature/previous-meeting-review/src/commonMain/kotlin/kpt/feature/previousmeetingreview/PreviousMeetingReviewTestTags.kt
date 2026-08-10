/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.previousmeetingreview

/**
 * Append-only test-tag registry for the `previous-meeting-review` feature
 * (RULE-KMP-COMPOSE-UITEST-001 CU-5 — names are stable across regenerations; only append new
 * entries, never rename or remove). Constant names are derived 1:1 from `ui.yaml#components[].id`.
 * Mirrors `MeetingSummaryTestTags`'s convention.
 *
 * The `top_app_bar` back-navigation icon carries no per-screen testTag hook — `KptScaffold`'s
 * navigation `IconButton` is not individually taggable (same documented gap as `MeetingSummaryTestTags`).
 *
 * See API.md#tags.
 */
object PreviousMeetingReviewTestTags {

    /** Root [kpt.core.ui.scaffold.KptScaffold] surface — always rendered regardless of screenState. */
    const val SCREEN: String = "previous_meeting_review_screen"

    /** Scrollable content surface — `PreviousMeetingReviewScreenState.Content`. */
    const val CONTENT_LIST: String = "previous_meeting_review_content_list"

    /** `ui.yaml#components.context_banner`. */
    const val CONTEXT_BANNER: String = "previous_meeting_review_context_banner"

    /** `ui.yaml#components.unresolved_alert_card`. */
    const val UNRESOLVED_ALERT_CARD: String = "previous_meeting_review_unresolved_alert_card"

    /** `ui.yaml#components.summary_metrics_card`. */
    const val SUMMARY_METRICS_CARD: String = "previous_meeting_review_summary_metrics_card"

    /** `ui.yaml#components.attendance_chip_row`. */
    const val ATTENDANCE_CHIP: String = "previous_meeting_review_attendance_chip"

    /** `ui.yaml#components.attendance_detail_row` section. */
    const val ATTENDANCE_SECTION: String = "previous_meeting_review_attendance_section"

    /** `ui.yaml#components.member_savings_row` section. */
    const val SAVINGS_SECTION: String = "previous_meeting_review_savings_section"

    /** `ui.yaml#components.loan_activity_row` section. */
    const val LOAN_SECTION: String = "previous_meeting_review_loan_section"

    /** `ui.yaml#components.start_meeting_cta` — [PreviousMeetingReviewAction.StartNewMeeting]. */
    const val START_MEETING_CTA: String = "previous_meeting_review_start_meeting_cta"

    /** Shimmer skeleton region — `PreviousMeetingReviewScreenState.Loading` (`ui.yaml#loading_skeleton`). */
    const val LOADING_SECTION: String = "previous_meeting_review_loading_section"

    /** Full-screen error surface — `PreviousMeetingReviewScreenState.Error`. */
    const val ERROR_SECTION: String = "previous_meeting_review_error_section"

    /** "Not conducted yet" surface — Content whose record is the blank-date not-conducted snapshot. */
    const val NOT_CONDUCTED_SECTION: String = "previous_meeting_review_not_conducted_section"

    /** Retry CTA on the error state — [PreviousMeetingReviewAction.Retry]. */
    const val ERROR_RETRY_BUTTON: String = "previous_meeting_review_error_retry_button"

    /** Per-member attendance row test tag — rows render data-driven, so this is a function. */
    fun attendanceRowTag(memberId: String): String = "previous_meeting_review_attendance_row_$memberId"

    /** Per-member savings row test tag — rows render data-driven, so this is a function. */
    fun savingsRowTag(memberId: String): String = "previous_meeting_review_savings_row_$memberId"

    /** Per-member loan-activity row test tag — rows render data-driven, so this is a function. */
    fun loanRowTag(memberId: String): String = "previous_meeting_review_loan_row_$memberId"
}
