/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.memberadd

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import kpt.core.base.ui.nav.composableWithRootPushTransitions

/**
 * `/groups/{groupId}/members/add` — the member-add form route (`ui.yaml#route`,
 * `ui.yaml#nav_params: { groupId }`). [groupId] is forwarded from `member-list`'s FAB tap
 * (`flow.yaml#entry_points[0]`, `MemberListEvent.NavigateToAddMember(groupId)`) and seeds
 * [MemberAddViewModel] via Koin `parametersOf(groupId)`. See API.md#route.
 */
@Serializable
data class MemberAddRoute(val groupId: String)

fun NavController.navigateToMemberAdd(groupId: String, navOptions: NavOptions? = null) =
    navigate(MemberAddRoute(groupId = groupId), navOptions)

/**
 * Registers [MemberAddScreen] on the host [NavGraphBuilder]. Every callback here closes a
 * [MemberAddEvent] navigation branch consumed by the Container (RULE-PROTO-COMPOSE-DEAD-CLICK-001
 * DC3) — matching `flow.yaml#navigates_to` (`member-list`, `member-profile`):
 * [onNavigateToMemberProfile] resolves `MemberAddEvent.NavigateToMemberProfile` and
 * [onNavigateBack] resolves `MemberAddEvent.NavigateBack` (pop back onto `member-list`, same
 * generic pass-through convention as `MemberProfileRoute.kt` / `GroupCreateRoute.kt`). No
 * callback carries a `= {}` default — DC3 count-assertion: 0 defaults / 0 overrides / 0
 * suppressed. See API.md#route.
 */
fun NavGraphBuilder.memberAddScreen(
    onNavigateToMemberProfile: (memberId: String, groupId: String) -> Unit,
    onNavigateBack: () -> Unit,
) {
    composableWithRootPushTransitions<MemberAddRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<MemberAddRoute>()
        MemberAddScreen(
            groupId = route.groupId,
            onNavigateToMemberProfile = onNavigateToMemberProfile,
            onNavigateBack = onNavigateBack,
        )
    }
}
