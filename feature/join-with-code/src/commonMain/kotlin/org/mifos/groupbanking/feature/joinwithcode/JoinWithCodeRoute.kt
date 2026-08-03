/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.joinwithcode

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import kpt.core.base.ui.nav.composableWithRootPushTransitions

/**
 * `/groups/join` — the invite-code entry + group-preview confirmation route
 * (`ui.yaml#route`). [inviteCode] is the optional deep-link nav-arg
 * (`ui.yaml#nav_params.inviteCode` — `app://groups/join?code={inviteCode}`); a non-null 6-char
 * value skips manual entry and auto-validates
 * (`flow.yaml#on_mount.parse_deep_link_code`). See API.md#route.
 */
@Serializable
data class JoinWithCodeRoute(val inviteCode: String? = null)

fun NavController.navigateToJoinWithCode(inviteCode: String? = null, navOptions: NavOptions? = null) =
    navigate(JoinWithCodeRoute(inviteCode = inviteCode), navOptions)

/**
 * Registers [JoinWithCodeScreen] on the host [NavGraphBuilder]. Every callback here closes a
 * [JoinWithCodeEvent] navigation branch consumed by the Container
 * (RULE-PROTO-COMPOSE-DEAD-CLICK-001 DC3) — `flow.yaml#navigates_to` declares both targets:
 * `personal-dashboard` ([onNavigateToPersonalDashboard] — F5 post-join landing per member role,
 * reconciled from `group-dashboard`) and `login-signup` ([onNavigateToLoginSignup] — pre-auth
 * invite resume). [onNavigateBack] is the `OnBack`/pop target (same pass-through convention as
 * `LoginSignupRoute.kt` / `GroupTypePickerRoute.kt`). No callback here carries a `= {}` default —
 * every parameter is required, so the nav host wiring this destination must supply a real
 * implementation for all 3 (DC3 count-assertion: 0 defaults / 0 overrides / 0 suppressed — a pure
 * pass-through, identical shape to the two sibling Route.kt files). See API.md#route.
 */
fun NavGraphBuilder.joinWithCodeScreen(
    onNavigateToPersonalDashboard: () -> Unit,
    onNavigateToLoginSignup: (pendingInviteCode: String) -> Unit,
    onNavigateBack: () -> Unit,
) {
    composableWithRootPushTransitions<JoinWithCodeRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<JoinWithCodeRoute>()
        JoinWithCodeScreen(
            inviteCode = route.inviteCode,
            onNavigateToPersonalDashboard = onNavigateToPersonalDashboard,
            onNavigateToLoginSignup = onNavigateToLoginSignup,
            onNavigateBack = onNavigateBack,
        )
    }
}
