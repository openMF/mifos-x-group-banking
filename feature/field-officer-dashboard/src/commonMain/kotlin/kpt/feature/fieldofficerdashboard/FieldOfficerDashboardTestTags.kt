/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.fieldofficerdashboard

/**
 * Append-only test-tag registry for the `field-officer-dashboard` feature
 * (RULE-KMP-COMPOSE-UITEST-001 CU-5 — names are stable across regenerations; only append new
 * entries, never rename or remove). Consumed by Compose UI tests + the Maestro flow generator.
 * See API.md#tags.
 */
object FieldOfficerDashboardTestTags {

    /** Root [kpt.core.ui.scaffold.KptScaffold] surface — always rendered regardless of screenState. */
    const val SCREEN: String = "field_officer_dashboard_screen"

    /** Export Report top-bar action — `ui.yaml#components.top_bar.export_action`. */
    const val EXPORT_ACTION: String = "field_officer_dashboard_export_action"

    /** Loading shimmer + spinner surface — `FieldOfficerDashboardScreenState.Loading`. */
    const val LOADING_INDICATOR: String = "field_officer_dashboard_loading_indicator"

    /** Horizontally-scrollable 4-card KPI row — `ui.yaml#components.kpi_cards_row`. */
    const val KPI_ROW: String = "field_officer_dashboard_kpi_row"

    /** Filter chip row — `ui.yaml#components.filter_row`. */
    const val FILTER_ROW: String = "field_officer_dashboard_filter_row"

    /** Region filter chip — opens the region picker dialog. */
    const val REGION_FILTER_CHIP: String = "field_officer_dashboard_region_chip"

    /** Status filter chip — opens the status picker dialog. */
    const val STATUS_FILTER_CHIP: String = "field_officer_dashboard_status_chip"

    /** Overdue-rate filter chip — opens the overdue picker dialog. */
    const val OVERDUE_FILTER_CHIP: String = "field_officer_dashboard_overdue_chip"

    /** Clear-filters chip — `ui.yaml#components.filter_row.clear_filters_chip`. */
    const val CLEAR_FILTERS_CHIP: String = "field_officer_dashboard_clear_filters_chip"

    /** Scrollable list surface wrapping the group-health cards — `FieldOfficerDashboardScreenState.Content`. */
    const val GROUP_LIST: String = "field_officer_dashboard_group_list"

    /** Genuinely-zero-groups empty surface — `FieldOfficerDashboardScreenState.Empty`. */
    const val EMPTY_SECTION: String = "field_officer_dashboard_empty_section"

    /** No-filter-results empty surface — filters active + zero matches, still `Content`. */
    const val EMPTY_FILTER_SECTION: String = "field_officer_dashboard_empty_filter_section"

    /** Inline error surface — `FieldOfficerDashboardScreenState.Error`. */
    const val ERROR_SECTION: String = "field_officer_dashboard_error_section"

    /** Retry CTA on the error state — [FieldOfficerDashboardAction.OnRetry]. */
    const val ERROR_RETRY_BUTTON: String = "field_officer_dashboard_error_retry_button"

    /**
     * Resolves the stable per-card test tag for a given group id. Cards render data-driven from
     * `FieldOfficerDashboardState.filteredGroups` (not as N separate composables), so this is a
     * function rather than a fixed constant set — mirrors `GroupListTestTags.cardTag`.
     */
    fun cardTag(groupId: Long): String = "field_officer_dashboard_card_$groupId"
}
