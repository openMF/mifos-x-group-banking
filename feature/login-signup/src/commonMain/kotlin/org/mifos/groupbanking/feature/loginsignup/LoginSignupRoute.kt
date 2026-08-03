/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.loginsignup

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import kpt.core.base.ui.nav.composableWithRootPushTransitions

/**
 * `/auth` — the unified login/signup entry route (`ui.yaml#route`). [pendingInviteCode] carries the
 * 6-char invite code back from join-with-code for a pre-auth accept-invitation resume
 * (`ui.yaml#nav_params.pendingInviteCode`, TC-LS-010); null for the default cold-launch start. See
 * API.md#route.
 */
@Serializable
data class LoginSignupRoute(val pendingInviteCode: String? = null)

fun NavController.navigateToLoginSignup(pendingInviteCode: String? = null, navOptions: NavOptions? = null) =
    navigate(LoginSignupRoute(pendingInviteCode = pendingInviteCode), navOptions)

/**
 * Registers [LoginSignupScreen] on the host [NavGraphBuilder]. Every callback here closes a
 * [LoginSignupEvent] navigation branch consumed by the Container (RULE-PROTO-COMPOSE-DEAD-CLICK-001
 * DC3) — `flow.yaml#navigates_to` declares all targets. [onNavigateToJoinWithCode] receives the
 * optional resume invite code so join-with-code can pre-fill it. See API.md#route.
 */
fun NavGraphBuilder.loginSignupScreen(
    onNavigateToPersonalDashboard: () -> Unit,
    onNavigateToOrganizerDashboard: () -> Unit,
    onNavigateToGroupList: () -> Unit,
    onNavigateToGroupTypePicker: () -> Unit,
    onNavigateToJoinWithCode: (inviteCode: String?) -> Unit,
) {
    composableWithRootPushTransitions<LoginSignupRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<LoginSignupRoute>()
        LoginSignupScreen(
            onNavigateToPersonalDashboard = onNavigateToPersonalDashboard,
            onNavigateToOrganizerDashboard = onNavigateToOrganizerDashboard,
            onNavigateToGroupList = onNavigateToGroupList,
            onNavigateToGroupTypePicker = onNavigateToGroupTypePicker,
            onNavigateToJoinWithCode = onNavigateToJoinWithCode,
            pendingInviteCode = route.pendingInviteCode,
        )
    }
}
