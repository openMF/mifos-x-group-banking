/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.grouplist

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import kotlinx.serialization.Serializable
import kpt.core.base.ui.nav.composableWithRootPushTransitions

/**
 * `/groups` — the authenticated user's group portfolio list (`SPEC.md#screens.group-list`,
 * `MOCKUP.md#GroupListScreen`). See API.md#route.
 */
@Serializable
data object GroupListRoute

fun NavController.navigateToGroupList(navOptions: NavOptions? = null) = navigate(GroupListRoute, navOptions)

/**
 * Registers [GroupListScreen] on the host [NavGraphBuilder]. Every callback here closes a
 * [GroupListEvent] navigation branch consumed by the Container (RULE-PROTO-COMPOSE-DEAD-CLICK-001
 * DC3) — [onNavigateToCreateGroup] resolves to the already-generated `group-type-picker` route
 * (`SPEC.md#navigation`: "group-list FAB → group-create", which itself routes through the
 * type-picker step first); [onNavigateToGroupDashboard] and [onNavigateToJoinGroup] are typed
 * nav-arg contracts for the caller to wire once `group-dashboard` / `join-with-code` exist as
 * generated feature modules — mirrors `GroupTypePickerRoute`'s identical
 * not-yet-generated-target convention. See API.md#route.
 */
fun NavGraphBuilder.groupListScreen(
    onNavigateToGroupDashboard: (groupId: String, viewerRole: String) -> Unit,
    onNavigateToCreateGroup: () -> Unit,
    onNavigateToJoinGroup: () -> Unit,
) {
    composableWithRootPushTransitions<GroupListRoute> {
        GroupListScreen(
            onNavigateToGroupDashboard = onNavigateToGroupDashboard,
            onNavigateToCreateGroup = onNavigateToCreateGroup,
            onNavigateToJoinGroup = onNavigateToJoinGroup,
        )
    }
}
