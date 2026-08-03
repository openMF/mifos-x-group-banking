/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.personalsavings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import kpt.core.base.ui.nav.composableWithPushTransitions

/**
 * `/savings` — the signed-in member's own tabbed savings ledger (`ui.yaml#route`,
 * `ui.yaml#nav_params: { clientId, groupLinkedSavingsId, individualSavingsId }`), entered from
 * `personal-dashboard`'s `user_taps_savings_card` entry point. [individualSavingsId] defaults to
 * `null` — the member may have no voluntary individual account. See API.md#route.
 */
@Serializable
data class PersonalSavingsRoute(
    val clientId: Long,
    val groupLinkedSavingsId: Long,
    val individualSavingsId: Long? = null,
)

fun NavController.navigateToPersonalSavings(
    clientId: Long,
    groupLinkedSavingsId: Long,
    individualSavingsId: Long?,
    navOptions: NavOptions? = null,
) = navigate(
    PersonalSavingsRoute(
        clientId = clientId,
        groupLinkedSavingsId = groupLinkedSavingsId,
        individualSavingsId = individualSavingsId,
    ),
    navOptions,
)

/**
 * Registers [PersonalSavingsScreen] on the host [NavGraphBuilder]. [onNavigateBack] closes the
 * lone [PersonalSavingsEvent] navigation branch (RULE-PROTO-COMPOSE-DEAD-CLICK-001 DC3) —
 * matching `flow.yaml#navigates_to` (`personal-dashboard`, `condition: user_taps_back`). No
 * other navigation target is declared for this screen. [composableWithPushTransitions] (not
 * `...RootPushTransitions`) is used because `personal-savings` is a non-root, back-stack-bearing
 * destination pushed from `personal-dashboard`. No callback carries a `= {}` default — DC3
 * count-assertion: 1 default / 1 override / 0 suppressed. See API.md#route.
 */
fun NavGraphBuilder.personalSavingsScreen(onNavigateBack: () -> Unit) {
    composableWithPushTransitions<PersonalSavingsRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<PersonalSavingsRoute>()
        PersonalSavingsScreen(
            clientId = route.clientId,
            groupLinkedSavingsId = route.groupLinkedSavingsId,
            individualSavingsId = route.individualSavingsId,
            onNavigateBack = onNavigateBack,
        )
    }
}
