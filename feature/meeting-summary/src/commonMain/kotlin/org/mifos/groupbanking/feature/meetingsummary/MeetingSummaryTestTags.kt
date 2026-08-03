/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.meetingsummary

/**
 * Append-only test-tag registry for the `meeting-summary` feature (RULE-KMP-COMPOSE-UITEST-001 CU-5
 * — names are stable across regenerations; only append new entries, never rename or remove).
 * Constant names are derived 1:1 from `ui.yaml#components[].id`. Mirrors `LoanDetailTestTags`'s
 * identical convention.
 *
 * The `top_app_bar` back-navigation icon carries no per-screen testTag hook — `KptScaffold`'s
 * navigation `IconButton` is not individually taggable (same documented gap as `LoanDetailTestTags`).
 *
 * See API.md#tags.
 */
object MeetingSummaryTestTags {

    /** Root [kpt.core.ui.scaffold.KptScaffold] surface — always rendered regardless of screenState. */
    const val SCREEN: String = "meeting_summary_screen"

    /** `ui.yaml#components.top_app_bar.actions.share_btn` — [MeetingSummaryAction.ShareMeetingReport]. */
    const val SHARE_BUTTON: String = "meeting_summary_share_button"

    /** `ui.yaml#components.hero_card`. */
    const val HERO_CARD: String = "meeting_summary_hero_card"

    /** `ui.yaml#components.hero_card.corpus_hero_chip`. */
    const val CORPUS_CHIP: String = "meeting_summary_corpus_chip"

    /** `ui.yaml#components.metric_grid`. */
    const val METRIC_GRID: String = "meeting_summary_metric_grid"

    /** `ui.yaml#components.savings_breakdown_section`. */
    const val SAVINGS_BREAKDOWN_SECTION: String = "meeting_summary_savings_breakdown_section"

    /** `ui.yaml#components.corpus_reconciliation_section`. */
    const val CORPUS_RECONCILIATION_SECTION: String = "meeting_summary_corpus_reconciliation_section"

    /** `ui.yaml#components.done_button` — [MeetingSummaryAction.NavigateDone]. */
    const val DONE_BUTTON: String = "meeting_summary_done_button"

    /** Scrollable content surface — `MeetingSummaryScreenState.Content`. */
    const val CONTENT_LIST: String = "meeting_summary_content_list"

    /** Shimmer skeleton region — `MeetingSummaryScreenState.Loading` (`ui.yaml#loading_skeleton`). */
    const val LOADING_SECTION: String = "meeting_summary_loading_section"

    /** Full-screen error surface — `MeetingSummaryScreenState.Error`. */
    const val ERROR_SECTION: String = "meeting_summary_error_section"

    /** Retry CTA on the error state — [MeetingSummaryAction.Retry]. */
    const val ERROR_RETRY_BUTTON: String = "meeting_summary_error_retry_button"

    /**
     * Resolves the stable per-row test tag for a given savings-breakdown member. Rows render
     * data-driven from `MeetingSummaryData.savingsBreakdown` (not as N separate composables), so this
     * is a function rather than a fixed constant set — mirrors `LoanDetailTestTags.scheduleRowTag`.
     */
    fun savingsRowTag(memberId: String): String = "meeting_summary_savings_row_$memberId"
}
