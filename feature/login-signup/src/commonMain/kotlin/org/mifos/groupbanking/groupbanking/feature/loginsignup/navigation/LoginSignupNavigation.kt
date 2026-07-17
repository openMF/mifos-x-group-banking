/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.groupbanking.feature.loginsignup.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.navigation
import kotlinx.serialization.Serializable
import org.mifos.groupbanking.groupbanking.feature.loginsignup.ui.LoginSignupScreen
import template.core.base.ui.composableWithRootPushTransitions

@Serializable
data object LoginSignupGraphRoute

@Serializable
data object LoginSignupRoute

fun NavController.navigateToLoginSignup(navOptions: NavOptions? = null) {
    navigate(route = LoginSignupGraphRoute, navOptions = navOptions)
}

/**
 * Registers the login-signup graph. The post-auth navigation callbacks are supplied
 * by the app-level navigation host so the feature stays decoupled from concrete
 * destinations (group-list / personal-dashboard / group-type-picker / join-with-code).
 */
fun NavGraphBuilder.loginSignupGraph(
    onNavigateToPersonalDashboard: () -> Unit,
    onNavigateToGroupList: () -> Unit,
    onNavigateToGroupTypePicker: () -> Unit,
    onNavigateToJoinWithCode: () -> Unit,
    onPromptBiometric: () -> Unit,
    onShowSnackbar: (String) -> Unit,
) {
    navigation<LoginSignupGraphRoute>(startDestination = LoginSignupRoute) {
        composableWithRootPushTransitions<LoginSignupRoute> {
            LoginSignupScreen(
                onNavigateToPersonalDashboard = onNavigateToPersonalDashboard,
                onNavigateToGroupList = onNavigateToGroupList,
                onNavigateToGroupTypePicker = onNavigateToGroupTypePicker,
                onNavigateToJoinWithCode = onNavigateToJoinWithCode,
                onPromptBiometric = onPromptBiometric,
                onShowSnackbar = onShowSnackbar,
            )
        }
    }
}
