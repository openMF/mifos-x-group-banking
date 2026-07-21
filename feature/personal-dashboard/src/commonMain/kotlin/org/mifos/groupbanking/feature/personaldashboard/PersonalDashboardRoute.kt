/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.personaldashboard

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
 *   (`flow.yaml#navigates_to: savings-dashboard`, `ui.yaml#components.savings_summary_card
 *   .on_click.target: personal-savings`) — a typed nav-arg contract (`groupId` + `poolModel`) for
 *   the caller to wire once that feature module is generated, mirroring `GroupListRoute`'s
 *   not-yet-generated-target convention.
 * - [onNavigateToGroupList] closes [PersonalDashboardEvent.NavigateToGroupList]
 *   (`flow.yaml#navigates_to: group-list`) — currently unreachable from any wired `on_click` on
 *   this screen's canvas (see [PersonalDashboardViewModel] class KDoc "Idea-layer gap" note); the
 *   callback is still exposed here so the host wiring compiles and is ready the moment an
 *   idea-layer update adds the missing affordance.
 *
 * See API.md#route.
 */
fun NavGraphBuilder.personalDashboardScreen(
    onNavigateToSavings: (groupId: String, poolModel: String) -> Unit,
    onNavigateToGroupList: () -> Unit,
) {
    composableWithRootPushTransitions<PersonalDashboardRoute> {
        PersonalDashboardScreen(
            onNavigateToSavings = onNavigateToSavings,
            onNavigateToGroupList = onNavigateToGroupList,
        )
    }
}
