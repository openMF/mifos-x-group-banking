/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import kotlinx.serialization.Serializable
import kpt.core.base.ui.nav.composableWithRootPushTransitions

/**
 * `/settings` (`ui.yaml#route`) -- reached from `entry_points[0]`: `trigger:
 * settings_tab_selected`. See API.md#route.
 */
@Serializable
data object SettingsRoute

fun NavController.navigateToSettings(navOptions: NavOptions? = null) = navigate(SettingsRoute, navOptions)

/**
 * Registers [SettingsScreen] on the host [NavGraphBuilder]. Every callback here closes a
 * [SettingsEvent] navigation branch consumed by the Container (RULE-PROTO-COMPOSE-DEAD-CLICK-001
 * DC3): [onNavigateToLogin] resolves `SettingsEvent.NavigateToLogin` (session-expired mid-PIN-
 * change, `data-flow.yaml`'s `401 -> navigate: login` error path), [onShowLogoutDialog] resolves
 * `SettingsEvent.ShowLogoutDialog` (presents the sibling `settings-logout-dialog` feature's
 * overlay -- the caller renders it, mirroring `loan-detail`'s `onShowRepaymentDialog`/
 * `onShowDefaultDialog` overlay-target convention), and [onNavigateBack] pops back to the
 * screen's `entry_points[0]` caller. No callback carries a `= {}` default -- DC3 count-assertion:
 * 0 defaults / 0 overrides / 0 suppressed. See API.md#route.
 */
fun NavGraphBuilder.settingsScreen(
    onNavigateToLogin: () -> Unit,
    onShowLogoutDialog: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    composableWithRootPushTransitions<SettingsRoute> {
        SettingsScreen(
            onNavigateToLogin = onNavigateToLogin,
            onShowLogoutDialog = onShowLogoutDialog,
            onNavigateBack = onNavigateBack,
        )
    }
}
