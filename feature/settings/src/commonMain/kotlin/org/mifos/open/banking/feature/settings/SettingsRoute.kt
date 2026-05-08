/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
<<<<<<<< HEAD:feature/settings/src/commonMain/kotlin/org/mifos/open/banking/feature/settings/SettingsRoute.kt
package org.mifos.open.banking.feature.settings
========
package org.mifos.groupbanking.groupbanking.feature.settings
>>>>>>>> c3e419f (feat(scaffold): customise kmp-project-template for group-banking (CommonPurse)):feature/settings/src/commonMain/kotlin/org/mifos/groupbanking/feature/settings/SettingsRoute.kt

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import kotlinx.serialization.Serializable
import template.core.base.ui.composableWithPushTransitions

@Serializable
data object SettingsRoute

@Serializable
data object NotificationRoute

fun NavController.navigateToSettings(navOptions: NavOptions? = null) =
    navigate(SettingsRoute, navOptions)

fun NavController.navigateToNotification(navOptions: NavOptions? = null) =
    navigate(NotificationRoute, navOptions)

fun NavGraphBuilder.settingsDestination(
    onBackClick: () -> Unit,
) {
    composableWithPushTransitions<SettingsRoute> {
        SettingsScreen(
            onBackClick = onBackClick,
        )
    }
}

fun NavGraphBuilder.notificationDestination(
    onBackClick: () -> Unit,
) {
    composableWithPushTransitions<NotificationRoute> {
        NotificationScreen(
            onBackClick = onBackClick,
        )
    }
}
