/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.groupdashboard

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import kpt.core.base.ui.nav.composableWithRootPushTransitions

/**
 * `/groups/{groupId}` — the single-group dashboard route (`ui.yaml#route`,
 * `ui.yaml#nav_params: { groupId, viewerRole }`). [viewerRole] seeds
 * [GroupDashboardState.viewerRole] for immediate role-gated rendering before the composite stream
 * resolves — [GroupDashboardViewModel] reconciles it against the server-confirmed
 * `GroupDashboard.viewerRole.role` once Content arrives (see that class's KDoc). See API.md#route.
 */
@Serializable
data class GroupDashboardRoute(val groupId: String, val viewerRole: String)

fun NavController.navigateToGroupDashboard(groupId: String, viewerRole: String, navOptions: NavOptions? = null) =
    navigate(GroupDashboardRoute(groupId = groupId, viewerRole = viewerRole), navOptions)

/**
 * Registers [GroupDashboardScreen] on the host [NavGraphBuilder]. Every callback here closes a
 * [GroupDashboardEvent] navigation branch consumed by the Container
 * (RULE-PROTO-COMPOSE-DEAD-CLICK-001 DC3) — matching `flow.yaml#navigates_to`
 * (`meeting-calendar`, `member-list`, `loan-list`, `share-out-preview`, `savings-dashboard` (G9),
 * `settings` + `sync-status` (G13)) plus the flagged [onNavigateBack] addition documented on
 * [GroupDashboardEvent]'s class KDoc. [onNavigateToSavingsDashboard] (G9) self-scopes the MEMBER
 * "My Savings" tap to the group's savings-dashboard (replacing the prior member-savings-detail
 * mis-route); [onNavigateToSettings] / [onNavigateToSyncStatus] (G13) close the top-bar overflow
 * menu items. No callback carries a `= {}` default — DC3 count-assertion: 8 defaults / 8 overrides
 * / 0 suppressed. See API.md#route.
 */
fun NavGraphBuilder.groupDashboardScreen(
    onNavigateToMeetingCalendar: (groupId: String) -> Unit,
    onNavigateToMemberList: (groupId: String) -> Unit,
    onNavigateToLoanList: (groupId: String, viewerRole: String) -> Unit,
    onNavigateToShareOut: (groupId: String, distributionStrategy: String) -> Unit,
    onNavigateToSavingsDashboard: (groupId: String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSyncStatus: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    composableWithRootPushTransitions<GroupDashboardRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<GroupDashboardRoute>()
        GroupDashboardScreen(
            groupId = route.groupId,
            viewerRole = route.viewerRole,
            onNavigateToMeetingCalendar = onNavigateToMeetingCalendar,
            onNavigateToMemberList = onNavigateToMemberList,
            onNavigateToLoanList = onNavigateToLoanList,
            onNavigateToShareOut = onNavigateToShareOut,
            onNavigateToSavingsDashboard = onNavigateToSavingsDashboard,
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToSyncStatus = onNavigateToSyncStatus,
            onNavigateBack = onNavigateBack,
        )
    }
}
