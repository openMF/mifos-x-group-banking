/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.loanrequest

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import kpt.core.base.ui.nav.composableWithRootPushTransitions

/**
 * `/loan-request` -- the member-side loan-application form route (`ui.yaml#route`,
 * `ui.yaml#nav_params: { clientId, savingsBalance, loanMultiplier }`). [clientId]/[savingsBalance]
 * are forwarded from `personal-dashboard`'s "Request Loan" CTA or `personal-loans`'s FAB
 * (`flow.yaml#entry_points`); [loanMultiplier] defaults to `3.0` matching
 * `ui.yaml#nav_params.loanMultiplier.default` when the caller omits it. All three seed
 * [LoanRequestViewModel] via Koin `parametersOf(clientId, savingsBalance, loanMultiplier)`. See
 * API.md#route.
 */
@Serializable
data class LoanRequestRoute(
    val clientId: Long,
    val savingsBalance: Double,
    val loanMultiplier: Double = 3.0,
)

fun NavController.navigateToLoanRequest(
    clientId: Long,
    savingsBalance: Double,
    loanMultiplier: Double = 3.0,
    navOptions: NavOptions? = null,
) = navigate(
    LoanRequestRoute(clientId = clientId, savingsBalance = savingsBalance, loanMultiplier = loanMultiplier),
    navOptions,
)

/**
 * Registers [LoanRequestScreen] on the host [NavGraphBuilder]. Every callback here closes a
 * [LoanRequestEvent] navigation branch consumed by the Container (RULE-PROTO-COMPOSE-DEAD-CLICK-001
 * DC3) -- matching `flow.yaml#navigates_to` (`personal-dashboard`, `personal-loans`):
 * [onNavigateToDashboard] resolves `LoanRequestEvent.NavigateToDashboardAfterSuccess` and
 * [onNavigateBack] resolves both the top app bar's direct back-nav (no `LoanRequestAction`
 * dispatch -- see `LoanRequestScreen.kt` KDoc) and the defensively-collected
 * `LoanRequestEvent.NavigateBack`. No callback carries a `= {}` default -- DC3 count-assertion: 0
 * defaults / 0 overrides / 0 suppressed. See API.md#route.
 */
fun NavGraphBuilder.loanRequestScreen(
    onNavigateToDashboard: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    composableWithRootPushTransitions<LoanRequestRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<LoanRequestRoute>()
        LoanRequestScreen(
            clientId = route.clientId,
            savingsBalance = route.savingsBalance,
            loanMultiplier = route.loanMultiplier,
            onNavigateToDashboard = onNavigateToDashboard,
            onNavigateBack = onNavigateBack,
        )
    }
}
