/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
<<<<<<<< HEAD:feature/profile/src/commonMain/kotlin/org/mifos/open/banking/feature/profile/ProfileRoute.kt
package org.mifos.open.banking.feature.profile
========
package org.mifos.groupbanking.groupbanking.feature.profile
>>>>>>>> c3e419f (feat(scaffold): customise kmp-project-template for group-banking (CommonPurse)):feature/profile/src/commonMain/kotlin/org/mifos/groupbanking/feature/profile/ProfileRoute.kt

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import kotlinx.serialization.Serializable
import template.core.base.ui.composableWithStayTransitions

@Serializable
data object ProfileRoute

fun NavController.navigateToProfile(navOptions: NavOptions? = null) = navigate(ProfileRoute, navOptions)

fun NavGraphBuilder.profileDestination() {
    composableWithStayTransitions<ProfileRoute> {
        ProfileScreen()
    }
}
