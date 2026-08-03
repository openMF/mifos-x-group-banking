/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.memberinvite

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import kpt.core.base.ui.nav.composableWithRootPushTransitions

/**
 * `/groups/{groupId}/invite` — the member-invite route (`ui.yaml#route`,
 * `ui.yaml#nav_params: { groupId }`). [groupId] is forwarded from `member-list`'s invite FAB tap
 * (`flow.yaml#entry_points[0]`) or `group-dashboard`'s invite action (`entry_points[1]`) and seeds
 * [MemberInviteViewModel] via Koin `parametersOf(groupId)`. See API.md#route.
 */
@Serializable
data class MemberInviteRoute(val groupId: String)

fun NavController.navigateToMemberInvite(groupId: String, navOptions: NavOptions? = null) =
    navigate(MemberInviteRoute(groupId = groupId), navOptions)

/**
 * Registers [MemberInviteScreen] on the host [NavGraphBuilder]. [onNavigateBack] resolves
 * `MemberInviteEvent.NavigateBack` (pop back onto `member-list`/`group-dashboard`, matching
 * `flow.yaml#navigates_to`). No callback carries a `= {}` default (RULE-PROTO-COMPOSE-DEAD-CLICK-001
 * DC3). See API.md#route.
 */
fun NavGraphBuilder.memberInviteScreen(
    onNavigateBack: () -> Unit,
) {
    composableWithRootPushTransitions<MemberInviteRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<MemberInviteRoute>()
        MemberInviteScreen(
            groupId = route.groupId,
            onNavigateBack = onNavigateBack,
        )
    }
}
