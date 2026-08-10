/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.personalloans

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import kpt.core.base.ui.nav.composableWithPushTransitions

/**
 * `/loans` — the signed-in member's own filterable loan list (`ui.yaml#route`,
 * `ui.yaml#nav_params: { clientId }`), entered from `personal-dashboard`'s
 * `user_taps_loan_card` entry point. See API.md#route.
 */
@Serializable
data class PersonalLoansRoute(val clientId: Long)

fun NavController.navigateToPersonalLoans(clientId: Long, navOptions: NavOptions? = null) =
    navigate(PersonalLoansRoute(clientId = clientId), navOptions)

/**
 * Registers [PersonalLoansScreen] on the host [NavGraphBuilder]. Every callback here closes a
 * [PersonalLoansEvent] navigation branch (RULE-PROTO-COMPOSE-DEAD-CLICK-001 DC3) — matching
 * `flow.yaml#navigates_to` (`loan-request`, `personal-dashboard`).
 *
 * [onNavigateToLoanRequest] forwards [PersonalLoansRoute.clientId] per
 * `flow.yaml#flow_logic.on_request_loan_click.params` (`clientId: "{clientId}"`) —
 * `loan-request`'s other constructor args (`savingsBalance`, `loanMultiplier`) are the caller's
 * responsibility at the nav-graph wiring site, same documented gap as
 * [PersonalLoansEvent.NavigateToLoanRequest]'s KDoc (RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1).
 *
 * No `onNavigateToLoanDetail` callback is declared here — `ui.yaml#components.loan_card.on_click`
 * is `OnLoanExpand` (in-place expand via [PersonalLoansAction.OnLoanExpand], not a navigation
 * event) and `flow.yaml#navigates_to` does NOT list a `loan-detail` target, so no navigation
 * affordance exists to wire; inventing one would fail the generated-nav-vs-flow completion gate
 * (every `navigate*()` target here must appear in `flow.yaml#navigates_to[]`).
 *
 * [composableWithPushTransitions] (not `...RootPushTransitions`) is used because `personal-loans`
 * is a non-root, back-stack-bearing destination pushed from `personal-dashboard`. No callback
 * carries a `= {}` default — DC3 count-assertion: 2 defaults / 2 overrides / 0 suppressed. See
 * API.md#route.
 */
fun NavGraphBuilder.personalLoansScreen(
    onNavigateToLoanRequest: (clientId: Long) -> Unit,
    onNavigateBack: () -> Unit,
) {
    composableWithPushTransitions<PersonalLoansRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<PersonalLoansRoute>()
        PersonalLoansScreen(
            clientId = route.clientId,
            onNavigateToLoanRequest = onNavigateToLoanRequest,
            onNavigateBack = onNavigateBack,
        )
    }
}
