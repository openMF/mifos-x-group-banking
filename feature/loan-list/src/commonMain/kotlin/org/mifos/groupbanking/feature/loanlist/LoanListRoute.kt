/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.loanlist

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import kpt.core.base.ui.nav.composableWithPushTransitions

/**
 * `/groups/{groupId}/loans` — the filterable, paginated list of a savings group's loan accounts
 * (`ui.yaml#route`, `ui.yaml#nav_params: { groupId }`), entered from `group-dashboard`'s "Loans"
 * quick action or `bottom_nav`. See API.md#route.
 */
@Serializable
data class LoanListRoute(val groupId: Long, val viewerRole: String)

fun NavController.navigateToLoanList(groupId: Long, viewerRole: String, navOptions: NavOptions? = null) =
    navigate(LoanListRoute(groupId = groupId, viewerRole = viewerRole), navOptions)

/**
 * Registers [LoanListScreen] on the host [NavGraphBuilder]. Every callback here closes a
 * [LoanListEvent] navigation branch consumed by the Container (RULE-PROTO-COMPOSE-DEAD-CLICK-001
 * DC3) — matching `flow.yaml#navigates_to` (`loan-apply`, `loan-detail`) — plus [onNavigateBack],
 * a plain nav-pop callback for `top_bar`'s back affordance (NOT a [LoanListEvent] — no
 * `NavigateBack` member is declared; see [LoanListScreen] KDoc). Neither `loan-apply` nor
 * `loan-detail` is yet a generated feature module in this codebase — typed nav-arg contracts for
 * the caller to wire, mirroring `GroupListRoute.kt` / `GroupDashboardRoute.kt`'s identical
 * not-yet-generated-target convention. [composableWithPushTransitions] (not
 * `...RootPushTransitions`) is used because `loan-list` is a non-root, back-stack-bearing
 * destination pushed from `group-dashboard`. No callback carries a `= {}` default — DC3
 * count-assertion: 3 defaults / 3 overrides / 0 suppressed. See API.md#route.
 */
fun NavGraphBuilder.loanListScreen(
    onNavigateToLoanDetail: (loanId: Long, viewerRole: String) -> Unit,
    onNavigateToLoanApply: (groupId: Long) -> Unit,
    onNavigateBack: () -> Unit,
) {
    composableWithPushTransitions<LoanListRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<LoanListRoute>()
        LoanListScreen(
            groupId = route.groupId,
            viewerRole = route.viewerRole,
            onNavigateToLoanDetail = onNavigateToLoanDetail,
            onNavigateToLoanApply = onNavigateToLoanApply,
            onNavigateBack = onNavigateBack,
        )
    }
}
