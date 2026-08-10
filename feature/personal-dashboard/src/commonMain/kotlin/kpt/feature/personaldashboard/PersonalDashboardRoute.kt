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

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import kotlinx.serialization.Serializable
import kpt.core.base.ui.nav.composableWithRootPushTransitions

/**
 * `/dashboard/member` — the authenticated member's home screen (`ui.yaml#route`,
 * `docs.yaml#description`). Unified identity — no `clientId`/`selfServiceToken` nav-param;
 * identity resolves from the auth token inside `PersonalDashboardViewModel`. See API.md#route.
 */
@Serializable
data object PersonalDashboardRoute

fun NavController.navigateToPersonalDashboard(navOptions: NavOptions? = null) =
    navigate(PersonalDashboardRoute, navOptions)

/**
 * Registers [PersonalDashboardScreen] on the host [NavGraphBuilder]. Every callback here closes a
 * [PersonalDashboardEvent] navigation branch consumed by the Container
 * (RULE-PROTO-COMPOSE-DEAD-CLICK-001 DC3):
 *
 * - [onNavigateToSavings] closes [PersonalDashboardEvent.NavigateToSavings]
 *   (`ui.yaml#components.savings_summary_card.on_click.target: personal-savings`) — a typed nav-arg
 *   contract carrying the three `personal-savings` nav_params (`clientId`, `groupLinkedSavingsId`,
 *   optional `individualSavingsId`) + `poolModel`, wired to the real `personal-savings` feature
 *   module in `GroupBankingNavHost`.
 * - [onNavigateToGroupList] closes [PersonalDashboardEvent.NavigateToGroupList]
 *   (`flow.yaml#navigates_to: group-list`) — currently unreachable from any wired `on_click` on
 *   this screen's canvas (see [PersonalDashboardViewModel] class KDoc "Idea-layer gap" note); the
 *   callback is still exposed here so the host wiring compiles and is ready the moment an
 *   idea-layer update adds the missing affordance.
 * - [onNavigateToLoans] closes [PersonalDashboardEvent.NavigateToLoans]
 *   (`ui.yaml#components.loan_card.on_click.target: personal-loans`) — carries the member's
 *   `clientId` (the `personal-loans` nav_param) to the loans list. The un-deferred loan entry card.
 * - [onNavigateToSettings] / [onNavigateToSyncStatus] close
 *   [PersonalDashboardEvent.NavigateToSettings] / [PersonalDashboardEvent.NavigateToSyncStatus]
 *   (`ui.yaml#components.top_bar.overflow_menu`) — the profile/overflow-menu entry points to the
 *   shared `settings` + `sync-status` screens.
 *
 * See API.md#route.
 */
fun NavGraphBuilder.personalDashboardScreen(
    onNavigateToSavings: (clientId: Long, groupLinkedSavingsId: Long, individualSavingsId: Long?, poolModel: String) -> Unit,
    onNavigateToGroupList: () -> Unit,
    onNavigateToLoans: (clientId: Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSyncStatus: () -> Unit,
) {
    composableWithRootPushTransitions<PersonalDashboardRoute> {
        PersonalDashboardScreen(
            onNavigateToSavings = onNavigateToSavings,
            onNavigateToGroupList = onNavigateToGroupList,
            onNavigateToLoans = onNavigateToLoans,
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToSyncStatus = onNavigateToSyncStatus,
        )
    }
}
