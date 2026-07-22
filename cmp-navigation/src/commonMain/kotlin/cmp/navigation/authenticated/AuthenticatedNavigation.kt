/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
@file:Suppress("MatchingDeclarationName")

package cmp.navigation.authenticated

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.navigation
import cmp.navigation.authenticatednavbar.AuthenticatedNavbarRoute
import cmp.navigation.authenticatednavbar.authenticatedNavbarGraph
import kotlinx.serialization.Serializable
import kpt.core.base.ui.nav.popBackStackSafely
import org.mifos.groupbanking.feature.settings.navigateToSettings
import org.mifos.groupbanking.feature.settings.settingsScreen

@Serializable
internal data object AuthenticatedGraphRoute

internal fun NavController.navigateToAuthenticatedGraph(navOptions: NavOptions? = null) {
    navigate(route = AuthenticatedGraphRoute, navOptions = navOptions)
}

/**
 * **This graph is orphaned scaffolding** — the leftover generic "Money Toolkit" template shell
 * (`AuthenticatedNavbarRoute` -> Home/Profile tabs, `kpt.feature.home`/`kpt.feature.profile`).
 * `cmp.navigation.ComposeApp` (the app's real entry point) composes
 * `cmp.navigation.groupbanking.GroupBankingNavHost` exclusively — this file is never reached at
 * runtime, but it still lives in the `cmp-navigation` module and must compile. The legacy
 * `kpt.feature.settings.settingsDestination`/`navigateToSettings`/`notificationDestination` this
 * graph used to wire have been migrated away with the rest of `kpt.feature.settings`; this graph
 * is repointed at the new `org.mifos.groupbanking.feature.settings.settingsScreen(...)`. Because
 * this legacy demo shell has no login/logout-confirmation concept of its own (unlike
 * `GroupBankingNavHost`, which wires the real `onNavigateToLogin`/`onShowLogoutDialog` targets --
 * see that file), [onNavigateToLogin] / [onShowLogoutDialog] both fall back to a real, honest
 * `popBackStackSafely()` (return to the navbar) rather than a fabricated no-op.
 */
internal fun NavGraphBuilder.authenticatedGraph(navController: NavController) {
    navigation<AuthenticatedGraphRoute>(
        startDestination = AuthenticatedNavbarRoute,
    ) {
        authenticatedNavbarGraph(
            navigateToSettingsScreen = navController::navigateToSettings,
        )

        settingsScreen(
            onNavigateToLogin = { navController.popBackStackSafely() },
            onShowLogoutDialog = { navController.popBackStackSafely() },
            onNavigateBack = { navController.popBackStackSafely() },
        )
    }
}
