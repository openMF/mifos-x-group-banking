/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
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
 * (`meeting-calendar`, `member-list`, `loan-list`, `share-out-preview`) plus the two flagged
 * additions documented on [GroupDashboardEvent]'s class KDoc ([onNavigateToMemberSavingsDetail],
 * [onNavigateBack]). None of `meeting-calendar` / `member-list` / `loan-list` /
 * `share-out-preview` / `member-savings-detail` are yet-generated feature modules in this
 * codebase — typed nav-arg contracts for the caller to wire, mirroring `GroupListRoute.kt`'s
 * identical not-yet-generated-target convention. No callback carries a `= {}` default — DC3
 * count-assertion: 6 defaults / 6 overrides / 0 suppressed. See API.md#route.
 */
fun NavGraphBuilder.groupDashboardScreen(
    onNavigateToMeetingCalendar: (groupId: String) -> Unit,
    onNavigateToMemberList: (groupId: String) -> Unit,
    onNavigateToLoanList: (groupId: String) -> Unit,
    onNavigateToShareOut: (groupId: String, distributionStrategy: String) -> Unit,
    onNavigateToMemberSavingsDetail: (groupId: String) -> Unit,
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
            onNavigateToMemberSavingsDetail = onNavigateToMemberSavingsDetail,
            onNavigateBack = onNavigateBack,
        )
    }
}
