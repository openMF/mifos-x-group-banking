/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.loginsignup

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import kotlinx.serialization.Serializable
import kpt.core.base.ui.nav.composableWithRootPushTransitions

/**
 * `/auth` — the unified login/signup entry route (`ui.yaml#route`). See API.md#route.
 */
@Serializable
data object LoginSignupRoute

fun NavController.navigateToLoginSignup(navOptions: NavOptions? = null) = navigate(LoginSignupRoute, navOptions)

/**
 * Registers [LoginSignupScreen] on the host [NavGraphBuilder]. Every callback here closes a
 * [LoginSignupEvent] navigation branch consumed by the Container (RULE-PROTO-COMPOSE-DEAD-CLICK-001
 * DC3) — `flow.yaml#navigates_to` declares all 4 targets. See API.md#route.
 */
fun NavGraphBuilder.loginSignupScreen(
    onNavigateToPersonalDashboard: () -> Unit,
    onNavigateToGroupList: () -> Unit,
    onNavigateToGroupTypePicker: () -> Unit,
    onNavigateToJoinWithCode: () -> Unit,
) {
    composableWithRootPushTransitions<LoginSignupRoute> {
        LoginSignupScreen(
            onNavigateToPersonalDashboard = onNavigateToPersonalDashboard,
            onNavigateToGroupList = onNavigateToGroupList,
            onNavigateToGroupTypePicker = onNavigateToGroupTypePicker,
            onNavigateToJoinWithCode = onNavigateToJoinWithCode,
        )
    }
}
