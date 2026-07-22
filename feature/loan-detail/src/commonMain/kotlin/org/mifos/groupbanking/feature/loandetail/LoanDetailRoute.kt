/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.loandetail

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import kpt.core.base.ui.nav.composableWithRootPushTransitions

/**
 * `/loans/{loanId}` — the loan-detail route (`ui.yaml#route`, `ui.yaml#nav_params: { loanId }`).
 * See API.md#route.
 */
@Serializable
data class LoanDetailRoute(val loanId: Long)

fun NavController.navigateToLoanDetail(loanId: Long, navOptions: NavOptions? = null) =
    navigate(LoanDetailRoute(loanId = loanId), navOptions)

/**
 * Registers [LoanDetailScreen] on the host [NavGraphBuilder]. [onNavigateBack] closes the single
 * [LoanDetailEvent.NavigateBack] navigation branch consumed by the Container
 * (RULE-PROTO-COMPOSE-DEAD-CLICK-001 DC3) — matching `flow.yaml#navigates_to: [loan-list]`. No
 * callback carries a `= {}` default — DC3 count-assertion: 1 default / 1 override / 0 suppressed.
 *
 * `loan-repayment-dialog` / `loan-mark-defaulted-dialog` are NOT registered here — see
 * `LoanDetailScreen.kt`'s class KDoc for the "coming soon" snackbar fallback (both dialog targets
 * are not-yet-generated feature components; `flow.yaml#navigates_to` declares only `loan-list`,
 * so this is not an under-wiring gap against the flow graph). See API.md#route.
 */
fun NavGraphBuilder.loanDetailScreen(onNavigateBack: () -> Unit) {
    composableWithRootPushTransitions<LoanDetailRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<LoanDetailRoute>()
        LoanDetailScreen(
            loanId = route.loanId,
            onNavigateBack = onNavigateBack,
        )
    }
}
