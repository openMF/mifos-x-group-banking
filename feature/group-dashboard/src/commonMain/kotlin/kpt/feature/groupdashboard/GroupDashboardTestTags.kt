/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.groupdashboard

/**
 * Append-only test-tag registry for the `group-dashboard` feature (RULE-KMP-COMPOSE-UITEST-001
 * CU-5 — names are stable across regenerations; only append new entries, never rename or remove).
 * Consumed by Compose UI tests under `feature/group-dashboard/src/commonTest/` and by the Maestro
 * flow generator (`core/scripts/maestro-flow-gen.ts`), which emits `tapOn: { id: "<test_tag>" }`
 * selectors from these constants. Constant names are derived 1:1 from `ui.yaml#components[].id`.
 * Mirrors `PersonalDashboardTestTags` / `GroupListTestTags`'s identical convention. See API.md#tags.
 */
object GroupDashboardTestTags {

    /** Root [kpt.core.ui.scaffold.KptScaffold] surface — always rendered regardless of screenState. */
    const val SCREEN: String = "group_dashboard_screen"

    /** Top-bar overflow icon — `ui.yaml#components.top_bar.actions[0]` (`OnMoreOptions` toggles `isMoreMenuExpanded`). */
    const val MORE_OPTIONS_BUTTON: String = "group_dashboard_more_options_button"

    /** G13 — top-bar overflow dropdown surface (`ui.yaml#components.top_bar.actions[0].menu`). */
    const val MORE_MENU: String = "group_dashboard_more_menu"

    /** Overflow menu "Meetings" item — [GroupDashboardAction.OnViewMeetings] (→ meeting-calendar list). */
    const val MENU_MEETINGS_ITEM: String = "group_dashboard_menu_meetings_item"

    /** G13 — overflow menu "Settings" item — [GroupDashboardAction.OnGroupSettings] (→ settings). */
    const val MENU_SETTINGS_ITEM: String = "group_dashboard_menu_settings_item"

    /** G13 — overflow menu "Sync Status" item — [GroupDashboardAction.OnSyncStatus] (→ sync-status). */
    const val MENU_SYNC_STATUS_ITEM: String = "group_dashboard_menu_sync_status_item"

    /** `ui.yaml#components.group_header_card`. */
    const val HEADER_CARD: String = "group_dashboard_header_card"

    /** `ui.yaml#components.group_header_card.content.header_badges_row.group_type_chip`. */
    const val GROUP_TYPE_CHIP: String = "group_dashboard_group_type_chip"

    /** `ui.yaml#components.group_header_card.content.header_badges_row.viewer_role_chip`. */
    const val VIEWER_ROLE_CHIP: String = "group_dashboard_viewer_role_chip"

    /** `ui.yaml#components.group_header_card.content.header_badges_row.member_count_chip`. */
    const val MEMBER_COUNT_CHIP: String = "group_dashboard_member_count_chip"

    /** `ui.yaml#components.group_header_card.content.header_badges_row.overdue_loans_chip` — visible only when `group.overdueLoansCount > 0`. */
    const val OVERDUE_LOANS_CHIP: String = "group_dashboard_overdue_loans_chip"

    /** ACCUMULATING metric card — `ui.yaml#components.corpus_card`. */
    const val CORPUS_CARD: String = "group_dashboard_corpus_card"

    /** `ui.yaml#components.corpus_card.content.corpus_blocked_banner` — visible only when `isCorpusInsufficient`. */
    const val CORPUS_BLOCKED_BANNER: String = "group_dashboard_corpus_blocked_banner"

    /** ROTATING_PAYOUT metric card — `ui.yaml#components.rotation_card`. */
    const val ROTATION_CARD: String = "group_dashboard_rotation_card"

    /** `ui.yaml#components.quick_actions_section`. */
    const val QUICK_ACTIONS_SECTION: String = "group_dashboard_quick_actions_section"

    /** `ui.yaml#components.quick_actions_section.content.management_actions_grid.start_meeting_button` — [GroupDashboardAction.OnStartMeeting]. */
    const val START_MEETING_BUTTON: String = "group_dashboard_start_meeting_button"

    /** Management grid "Meetings" core action — [GroupDashboardAction.OnViewMeetings] (→ meeting-calendar list). */
    const val MEETINGS_BUTTON: String = "group_dashboard_meetings_button"

    /** `ui.yaml#components.quick_actions_section.content.management_actions_grid.view_members_button` — [GroupDashboardAction.OnViewMembers]. */
    const val VIEW_MEMBERS_BUTTON: String = "group_dashboard_view_members_button"

    /** `ui.yaml#components.quick_actions_section.content.management_actions_grid.view_loans_button` — [GroupDashboardAction.OnViewLoans]. */
    const val VIEW_LOANS_BUTTON: String = "group_dashboard_view_loans_button"

    /** `ui.yaml#components.quick_actions_section.content.management_actions_grid.share_out_button` — [GroupDashboardAction.OnShareOut]. */
    const val SHARE_OUT_BUTTON: String = "group_dashboard_share_out_button"

    /** `ui.yaml#components.quick_actions_section.content.member_actions_grid.view_savings_button` — [GroupDashboardAction.OnViewSavings]. */
    const val VIEW_SAVINGS_BUTTON: String = "group_dashboard_view_savings_button"

    /** `ui.yaml#components.quick_actions_section.content.member_actions_grid.view_loans_member_button` — [GroupDashboardAction.OnViewLoans]. */
    const val VIEW_LOANS_MEMBER_BUTTON: String = "group_dashboard_view_loans_member_button"

    /** `ui.yaml#components.quick_actions_section.content.member_actions_grid.view_meetings_button` — [GroupDashboardAction.OnStartMeeting]. */
    const val VIEW_MEETINGS_BUTTON: String = "group_dashboard_view_meetings_button"

    /** `ui.yaml#components.quick_actions_section.content.member_actions_grid.view_members_member_button` — [GroupDashboardAction.OnViewMembers]. */
    const val VIEW_MEMBERS_MEMBER_BUTTON: String = "group_dashboard_view_members_member_button"

    /** `ui.yaml#components.savings_summary_card`. */
    const val SAVINGS_SUMMARY_CARD: String = "group_dashboard_savings_summary_card"

    /** `ui.yaml#components.activity_feed_section`. */
    const val ACTIVITY_FEED_SECTION: String = "group_dashboard_activity_feed_section"

    /** Scrollable content surface (header + metric card + quick actions + savings + activity) — `Content` state. */
    const val CONTENT_LIST: String = "group_dashboard_content_list"

    /** Shimmer skeleton region — `GroupDashboardScreenState.Loading`. */
    const val LOADING_SECTION: String = "group_dashboard_loading_section"

    /** Full-screen error surface — `GroupDashboardScreenState.Error`. */
    const val ERROR_SECTION: String = "group_dashboard_error_section"

    /** Retry CTA on the error state — [GroupDashboardAction.Retry]. */
    const val ERROR_RETRY_BUTTON: String = "group_dashboard_error_retry_button"

    /** `GroupDashboardEvent.ShowCorpusBlockedDialog` modal. */
    const val CORPUS_BLOCKED_DIALOG: String = "group_dashboard_corpus_blocked_dialog"

    /** Dismiss/confirm button on the corpus-blocked dialog. */
    const val CORPUS_BLOCKED_DIALOG_CONFIRM: String = "group_dashboard_corpus_blocked_dialog_confirm"

    /**
     * Resolves the stable per-row test tag for a given activity id. Rows render data-driven from
     * `GroupDashboardState.recentActivity` (not as N separate composables), so this is a function
     * rather than a fixed constant set — mirrors `PersonalDashboardTestTags.transactionTag`.
     */
    fun activityRowTag(activityId: String): String = "group_dashboard_activity_row_$activityId"
}
