/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.loandetail

/**
 * Append-only test-tag registry for the `loan-detail` feature (RULE-KMP-COMPOSE-UITEST-001 CU-5 —
 * names are stable across regenerations; only append new entries, never rename or remove).
 * Consumed by Compose UI tests under `feature/loan-detail/src/commonTest/` and by the Maestro
 * flow generator (`core/scripts/maestro-flow-gen.ts`), which emits `tapOn: { id: "<test_tag>" }`
 * selectors from these constants. Constant names are derived 1:1 from `ui.yaml#components[].id`.
 * Mirrors `GroupDashboardTestTags` / `MemberProfileTestTags`'s identical convention.
 *
 * The `top_bar`/back-navigation icon carries no per-screen testTag hook — `KptTopAppBar`'s
 * navigation `IconButton` is not individually taggable (only the whole app-bar surface is, via
 * `KptTopAppBarConfiguration.testTag`), same documented gap as `GroupDashboardTestTags` /
 * `MemberProfileTestTags` (no `BACK_BUTTON` constant on either).
 *
 * See API.md#tags.
 */
object LoanDetailTestTags {

    /** Root [kpt.core.ui.scaffold.KptScaffold] surface — always rendered regardless of screenState. */
    const val SCREEN: String = "loan_detail_screen"

    /** `ui.yaml#components.member_header_card`. */
    const val HEADER_CARD: String = "loan_detail_header_card"

    /** `ui.yaml#components.member_header_card.content.loan_status_badge`. */
    const val STATUS_BADGE: String = "loan_detail_status_badge"

    /** `ui.yaml#components.outstanding_summary_row`. */
    const val OUTSTANDING_SUMMARY_ROW: String = "loan_detail_outstanding_summary_row"

    /** `ui.yaml#components.outstanding_summary_row.content.outstanding_amount_chip`. */
    const val OUTSTANDING_CHIP: String = "loan_detail_outstanding_chip"

    /** `ui.yaml#components.outstanding_summary_row.content.overdue_amount_chip` — visible only when `loan.totalOverdue > 0`. */
    const val OVERDUE_CHIP: String = "loan_detail_overdue_chip"

    /** `ui.yaml#components.detail_tabs`. */
    const val TABS: String = "loan_detail_tabs"

    /** `ui.yaml#components.detail_tabs.tabs.tab_schedule` — [LoanDetailAction.OnTabChange]. */
    const val TAB_SCHEDULE: String = "loan_detail_tab_schedule"

    /** `ui.yaml#components.detail_tabs.tabs.tab_history` — [LoanDetailAction.OnTabChange]. */
    const val TAB_HISTORY: String = "loan_detail_tab_history"

    /** `ui.yaml#components.schedule_table` header row — visible when `selectedTab == SCHEDULE`. */
    const val SCHEDULE_TABLE: String = "loan_detail_schedule_table"

    /** `ui.yaml#components.history_list` — visible when `selectedTab == HISTORY`. */
    const val HISTORY_LIST: String = "loan_detail_history_list"

    /** `ui.yaml#components.history_list.empty_text` — shown when `repaymentHistory` is empty. */
    const val HISTORY_EMPTY: String = "loan_detail_history_empty"

    /** `ui.yaml#components.action_buttons_row`. */
    const val ACTION_BUTTONS_ROW: String = "loan_detail_action_buttons_row"

    /**
     * `ui.yaml#components.action_buttons_row.content.record_repayment_button` —
     * [LoanDetailAction.OnRecordRepayment], visible when `canRecordRepayment && loan.status == ACTIVE`.
     */
    const val RECORD_REPAYMENT_BUTTON: String = "loan_detail_record_repayment_button"

    /**
     * `ui.yaml#components.action_buttons_row.content.mark_defaulted_button` —
     * [LoanDetailAction.OnMarkDefaulted], visible when `canMarkDefaulted && loan.status == OVERDUE`.
     */
    const val MARK_DEFAULTED_BUTTON: String = "loan_detail_mark_defaulted_button"

    /** Scrollable content surface (header + tabs + schedule/history + actions) — `Content` state. */
    const val CONTENT_LIST: String = "loan_detail_content_list"

    /** Shimmer skeleton region — `LoanDetailScreenState.Loading`. */
    const val LOADING_SECTION: String = "loan_detail_loading_section"

    /** Full-screen error surface — `LoanDetailScreenState.Error`. */
    const val ERROR_SECTION: String = "loan_detail_error_section"

    /** Retry CTA on the error state — [LoanDetailAction.Retry]. */
    const val ERROR_RETRY_BUTTON: String = "loan_detail_error_retry_button"

    /**
     * Resolves the stable per-row test tag for a given schedule week. Rows render data-driven
     * from `LoanDetailState.repaymentSchedule` (not as N separate composables), so this is a
     * function rather than a fixed constant set — mirrors `GroupDashboardTestTags.activityRowTag`.
     */
    fun scheduleRowTag(weekNumber: Int): String = "loan_detail_schedule_row_$weekNumber"

    /** Resolves the stable per-row test tag for a given repayment-history transaction id. */
    fun historyRowTag(transactionId: Long): String = "loan_detail_history_row_$transactionId"
}
