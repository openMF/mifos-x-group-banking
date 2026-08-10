/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.savingsdashboard

/**
 * Append-only test-tag registry for the `savings-dashboard` feature (RULE-KMP-COMPOSE-UITEST-001
 * CU-5 — names are stable across regenerations; only append new entries, never rename or remove).
 * Consumed by Compose UI tests under `feature/savings-dashboard/src/commonTest/` and by the
 * Maestro flow generator (`core/scripts/maestro-flow-gen.ts`), which emits `tapOn: { id:
 * "<test_tag>" }` selectors from these constants. Constant names are derived 1:1 from
 * `ui.yaml#components[].id`. Mirrors `MemberSavingsDetailTestTags`'s identical convention. See
 * API.md#tags.
 */
object SavingsDashboardTestTags {

    /** Root [kpt.core.ui.scaffold.KptScaffold] surface — always rendered regardless of screenState. */
    const val SCREEN: String = "savings_dashboard_screen"

    /** `ui.yaml#components.sync_band` — visible only when `state.lastSyncAt != null`, `Content` screenState only. */
    const val SYNC_BAND: String = "savings_dashboard_sync_band"

    /** `ui.yaml#components.error_banner` — inline cached-data banner, `Content` screenState with `state.error != null`. */
    const val ERROR_BANNER: String = "savings_dashboard_error_banner"

    /** `ui.yaml#components.tab_row`. */
    const val TAB_ROW: String = "savings_dashboard_tab_row"

    /** `ui.yaml#components.tab_row.style.tabs[GROUP]`. */
    const val TAB_GROUP: String = "savings_dashboard_tab_group"

    /** `ui.yaml#components.tab_row.style.tabs[INDIVIDUAL]`. */
    const val TAB_INDIVIDUAL: String = "savings_dashboard_tab_individual"

    /** `ui.yaml#components.group_savings_content.children.weekly_trend_chart_group`. */
    const val WEEKLY_TREND_CHART_GROUP: String = "savings_dashboard_weekly_trend_chart_group"

    /** `ui.yaml#components.individual_savings_content.children.weekly_trend_chart_individual`. */
    const val WEEKLY_TREND_CHART_INDIVIDUAL: String = "savings_dashboard_weekly_trend_chart_individual"

    /** `ui.yaml#components.group_savings_content.children.cycle_progress_card`. */
    const val CYCLE_PROGRESS_CARD: String = "savings_dashboard_cycle_progress_card"

    /** `ui.yaml#components.group_savings_content.children.cycle_progress_card.content.cycle_progress_bar`. */
    const val CYCLE_PROGRESS_BAR: String = "savings_dashboard_cycle_progress_bar"

    /** `ui.yaml#components.group_savings_content.children.group_savings_total_chip`. */
    const val GROUP_TOTAL_CHIP: String = "savings_dashboard_group_total_chip"

    /** `ui.yaml#components.individual_savings_content.children.individual_total_card`. */
    const val INDIVIDUAL_TOTAL_CARD: String = "savings_dashboard_individual_total_card"

    /** Scrollable surface for the GROUP tab's trend chart + cycle progress + member rows. */
    const val GROUP_MEMBER_LIST: String = "savings_dashboard_group_member_list"

    /** Scrollable surface for the INDIVIDUAL tab's trend chart + total card + member rows. */
    const val INDIVIDUAL_MEMBER_LIST: String = "savings_dashboard_individual_member_list"

    /** `ui.yaml#components.loading_skeleton` — `SavingsDashboardScreenState.Loading`. */
    const val LOADING_SECTION: String = "savings_dashboard_loading_section"

    /** `SavingsDashboardScreenState.Empty` full-screen surface. */
    const val EMPTY_SECTION: String = "savings_dashboard_empty_section"

    /** `SavingsDashboardScreenState.Error` full-screen surface. */
    const val ERROR_SECTION: String = "savings_dashboard_error_section"

    /** Retry CTA on the full-screen error state — dispatches `SavingsDashboardAction.RefreshDashboard`. */
    const val ERROR_RETRY_BUTTON: String = "savings_dashboard_error_retry_button"

    /**
     * Resolves the stable per-row test tag for a GROUP-tab member row.
     * `ui.yaml#components.group_member_savings_row` renders data-driven from
     * `SavingsDashboardState.groupSavingsSummary.memberRows` (not N separate composables), so this
     * is a function rather than a fixed constant set — mirrors `MemberSavingsDetailTestTags
     * .transactionRowTag`.
     */
    fun groupMemberRowTag(memberId: String): String = "savings_dashboard_group_member_row_$memberId"

    /** Resolves the stable per-row test tag for an INDIVIDUAL-tab member row. */
    fun individualMemberRowTag(memberId: String): String = "savings_dashboard_individual_member_row_$memberId"
}
