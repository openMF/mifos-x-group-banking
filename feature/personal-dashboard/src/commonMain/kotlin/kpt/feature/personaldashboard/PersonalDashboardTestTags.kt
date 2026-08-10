/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.personaldashboard

/**
 * Append-only test-tag registry for the `personal-dashboard` feature
 * (RULE-KMP-COMPOSE-UITEST-001 CU-5 — names are stable across regenerations; only append new
 * entries, never rename or remove). Consumed by Compose UI tests under
 * `feature/personal-dashboard/src/commonTest/` and by the Maestro flow generator
 * (`core/scripts/maestro-flow-gen.ts`), which emits `tapOn: { id: "<test_tag>" }` selectors from
 * these constants. Mirrors `GroupListTestTags`'s identical convention. See API.md#tags.
 */
object PersonalDashboardTestTags {

    /** Root [kpt.core.ui.scaffold.KptScaffold] surface — always rendered regardless of screenState. */
    const val SCREEN: String = "personal_dashboard_screen"

    /** Notifications bell icon-button — [PersonalDashboardAction.OnOpenNotifications] (G14, deferred-notifications snackbar). */
    const val NOTIFICATION_ICON: String = "personal_dashboard_notification_icon"

    /** Primary-colored group banner (name + currency chip + optional selector row). */
    const val GROUP_BANNER: String = "personal_dashboard_group_banner"

    /** Horizontal group-selector chip row — visible only when `myGroups.size > 1`. */
    const val GROUP_SELECTOR_ROW: String = "personal_dashboard_group_selector_row"

    /** Savings summary card — [PersonalDashboardAction.OnSavingsCardClick]. */
    const val SAVINGS_CARD: String = "personal_dashboard_savings_card"

    /** Loan entry card — [PersonalDashboardAction.OnLoansCardClick] (navigates to personal-loans). */
    const val LOAN_CARD: String = "personal_dashboard_loan_card"

    /** Profile / overflow menu icon-button — opens the Settings / Sync Status dropdown. */
    const val OVERFLOW_MENU: String = "personal_dashboard_overflow_menu"

    /** "Settings" item in the profile overflow menu — [PersonalDashboardAction.OnSettingsClick]. */
    const val MENU_SETTINGS_ITEM: String = "personal_dashboard_menu_settings"

    /** "Sync Status" item in the profile overflow menu — [PersonalDashboardAction.OnSyncStatusClick]. */
    const val MENU_SYNC_STATUS_ITEM: String = "personal_dashboard_menu_sync_status"

    /** Pool-model-adaptive share-out / rotation-position projection card. */
    const val SHAREOUT_CARD: String = "personal_dashboard_shareout_card"

    /** "Recent Activity" section heading. */
    const val RECENT_ACTIVITY_HEADER: String = "personal_dashboard_recent_activity_header"

    /** Scrollable content surface (banner + cards + recent-activity rows) — `Content` state. */
    const val RECENT_ACTIVITY_LIST: String = "personal_dashboard_recent_activity_list"

    /** Shimmer skeleton region — `PersonalDashboardScreenState.Loading`. */
    const val LOADING_SECTION: String = "personal_dashboard_loading_section"

    /** Full-illustration empty surface — `PersonalDashboardScreenState.Empty` (zero groups). */
    const val EMPTY_SECTION: String = "personal_dashboard_empty_section"

    /** Full-screen error surface — `PersonalDashboardScreenState.Error`. */
    const val ERROR_SECTION: String = "personal_dashboard_error_section"

    /** Retry CTA on the error state — [PersonalDashboardAction.OnRetry]. */
    const val ERROR_RETRY_BUTTON: String = "personal_dashboard_error_retry_button"

    /**
     * Resolves the stable per-chip test tag for a given group id. Chips render data-driven from
     * `PersonalDashboardState.myGroups` (not as N separate composables), so this is a function
     * rather than a fixed constant set — mirrors `GroupListTestTags.cardTag`.
     */
    fun groupChipTag(groupId: String): String = "personal_dashboard_group_chip_$groupId"

    /** Resolves the stable per-row test tag for a given transaction id. */
    fun transactionTag(transactionId: String): String = "personal_dashboard_transaction_$transactionId"
}
