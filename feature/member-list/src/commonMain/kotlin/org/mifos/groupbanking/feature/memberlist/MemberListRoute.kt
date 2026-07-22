/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.memberlist

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import kpt.core.base.ui.nav.composableWithRootPushTransitions

/**
 * `/groups/{groupId}/members` — the roster of members within one group (`ui.yaml#route`,
 * `ui.yaml#nav_params: { groupId }`). [groupId] is forwarded from `group-dashboard`'s
 * `view_members_button` trigger (`GroupDashboardEvent.NavigateToMemberList(groupId)`) and seeds
 * [MemberListViewModel] via Koin `parametersOf(groupId)`. See API.md#route.
 */
@Serializable
data class MemberListRoute(val groupId: String)

fun NavController.navigateToMemberList(groupId: String, navOptions: NavOptions? = null) =
    navigate(MemberListRoute(groupId = groupId), navOptions)

/**
 * Registers [MemberListScreen] on the host [NavGraphBuilder]. Every callback here closes a
 * [MemberListEvent] navigation branch consumed by the Container
 * (RULE-PROTO-COMPOSE-DEAD-CLICK-001 DC3) — [onMemberClick] resolves
 * [MemberListEvent.NavigateToMemberProfile], [onAddMember] resolves
 * [MemberListEvent.NavigateToAddMember], and [onBack] resolves [MemberListEvent.NavigateBack].
 * `member-profile` / `member-add` are not yet generated feature modules in this codebase — typed
 * nav-arg contracts for the caller to wire, mirroring `GroupListRoute.kt` /
 * `GroupDashboardRoute.kt`'s identical not-yet-generated-target convention. See API.md#route.
 */
fun NavGraphBuilder.memberListScreen(
    onMemberClick: (memberId: String, groupId: String) -> Unit,
    onAddMember: (groupId: String) -> Unit,
    onBack: () -> Unit,
) {
    composableWithRootPushTransitions<MemberListRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<MemberListRoute>()
        MemberListScreen(
            groupId = route.groupId,
            onNavigateToMemberProfile = onMemberClick,
            onNavigateToAddMember = onAddMember,
            onNavigateBack = onBack,
        )
    }
}
