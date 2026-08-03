/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.memberprofile

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import kpt.core.base.ui.nav.composableWithRootPushTransitions

/**
 * `/groups/{groupId}/members/{memberId}` — the member-profile route (`ui.yaml#route`,
 * `ui.yaml#nav_params: { memberId, groupId }`). See API.md#route.
 */
@Serializable
data class MemberProfileRoute(val memberId: String, val groupId: String)

fun NavController.navigateToMemberProfile(memberId: String, groupId: String, navOptions: NavOptions? = null) =
    navigate(MemberProfileRoute(memberId = memberId, groupId = groupId), navOptions)

/**
 * Registers [MemberProfileScreen] on the host [NavGraphBuilder]. Every callback here closes a
 * [MemberProfileEvent] navigation branch consumed by the Container (RULE-PROTO-COMPOSE-DEAD-CLICK-001
 * DC3) — matching `flow.yaml#navigates_to` (`member-list`, `member-savings-detail`); `member-edit-role`
 * is handled IN-SCREEN via the [org.mifos.groupbanking.feature.memberprofile.components.RoleEditBottomSheet]
 * overlay rather than a separate navigation destination (`ui.yaml#components.role_edit_bottom_sheet`
 * is a `bottom-sheet` component, not a route). No callback carries a `= {}` default — DC3
 * count-assertion: 2 defaults / 2 overrides / 0 suppressed. See API.md#route.
 */
fun NavGraphBuilder.memberProfileScreen(
    onNavigateToMemberList: (groupId: String) -> Unit,
    onNavigateToSavingsDetail: (memberId: String, groupId: String) -> Unit,
) {
    composableWithRootPushTransitions<MemberProfileRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<MemberProfileRoute>()
        MemberProfileScreen(
            memberId = route.memberId,
            groupId = route.groupId,
            onNavigateToMemberList = onNavigateToMemberList,
            onNavigateToSavingsDetail = onNavigateToSavingsDetail,
        )
    }
}
