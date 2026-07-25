/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.organizerdashboard

/**
 * Append-only test-tag registry for the `organizer-dashboard` feature
 * (RULE-KMP-COMPOSE-UITEST-001 CU-5 — names are stable across regenerations; only append new
 * entries, never rename or remove). Consumed by Compose UI tests + the Maestro flow generator.
 * See API.md#tags.
 */
object OrganizerDashboardTestTags {

    /** Root [kpt.core.ui.scaffold.KptScaffold] surface — always rendered regardless of screenState. */
    const val SCREEN: String = "organizer_dashboard_screen"

    /** Notifications top-bar action — `ui.yaml#components.top_bar.notifications`. */
    const val NOTIFICATIONS_ACTION: String = "organizer_dashboard_notifications_action"

    /** Loading spinner surface — `OrganizerDashboardScreenState.Loading`. */
    const val LOADING_INDICATOR: String = "organizer_dashboard_loading_indicator"

    /** Scrollable content surface — `OrganizerDashboardScreenState.Content`. */
    const val CONTENT: String = "organizer_dashboard_content"

    /** Personalised welcome header — `ui.yaml#components.welcome_header`. */
    const val WELCOME_HEADER: String = "organizer_dashboard_welcome_header"

    /** 4-card KPI row — `ui.yaml#components.kpi_summary_row`. */
    const val KPI_ROW: String = "organizer_dashboard_kpi_row"

    /** My-Groups KPI card — taps navigate to group-list. */
    const val KPI_GROUPS_CARD: String = "organizer_dashboard_kpi_groups_card"

    /** Members KPI card — taps navigate to group-list. */
    const val KPI_MEMBERS_CARD: String = "organizer_dashboard_kpi_members_card"

    /** Share-Outs Due KPI card — taps navigate to group-list; error accent when count > 0. */
    const val KPI_SHAREOUT_CARD: String = "organizer_dashboard_kpi_shareout_card"

    /** Meetings-Today KPI card — taps navigate to group-list. */
    const val KPI_MEETINGS_CARD: String = "organizer_dashboard_kpi_meetings_card"

    /** Quick-navigation card section — `ui.yaml#components.quick_nav_section`. */
    const val QUICK_NAV_SECTION: String = "organizer_dashboard_quick_nav_section"

    /** All-Groups quick-nav tile — navigates to group-list. */
    const val NAV_GROUP_LIST_CARD: String = "organizer_dashboard_nav_group_list_card"

    /** Field-Officers quick-nav tile — visible only when fieldOfficerEnabled; navigates to field-officer-dashboard. */
    const val NAV_FIELD_OFFICER_CARD: String = "organizer_dashboard_nav_field_officer_card"

    /** Today's-Schedule card section — `ui.yaml#components.todays_schedule_section`. */
    const val SCHEDULE_SECTION: String = "organizer_dashboard_schedule_section"

    /** Empty label shown inside the schedule section when there are no meetings today. */
    const val SCHEDULE_EMPTY_LABEL: String = "organizer_dashboard_schedule_empty_label"

    /** Recent-Activity card section — `ui.yaml#components.recent_activity_section`. */
    const val ACTIVITY_SECTION: String = "organizer_dashboard_activity_section"

    /** Genuinely-zero-groups empty surface — `OrganizerDashboardScreenState.Empty`. */
    const val EMPTY_SECTION: String = "organizer_dashboard_empty_section"

    /** Empty-state CTA button — navigates to group-list. */
    const val EMPTY_CTA_BUTTON: String = "organizer_dashboard_empty_cta_button"

    /** Inline error surface — `OrganizerDashboardScreenState.Error`. */
    const val ERROR_SECTION: String = "organizer_dashboard_error_section"

    /** Retry CTA on the error state — [OrganizerDashboardAction.Retry]. */
    const val ERROR_RETRY_BUTTON: String = "organizer_dashboard_error_retry_button"

    /** Resolves the stable per-row test tag for a Today's-Schedule meeting row keyed by groupId. */
    fun scheduleRowTag(groupId: String): String = "organizer_dashboard_schedule_row_$groupId"

    /** Resolves the stable per-row test tag for a Recent-Activity row keyed by activity id. */
    fun activityRowTag(activityId: String): String = "organizer_dashboard_activity_row_$activityId"
}
