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
 * [LoanDetailEvent.NavigateBack] navigation branch consumed by the Container, matching
 * `flow.yaml#navigates_to: [loan-list]`. [onShowRepaymentDialog] forwards
 * [LoanDetailScreen]'s `onShowRepaymentDialog` callback up to `cmp-navigation`
 * (`GroupBankingNavHost.kt`), the module that actually depends on
 * `feature/loan-repayment-dialog` and renders `LoanRepaymentDialog` as an overlay — see
 * `LoanDetailScreen.kt`'s class KDoc "loan-repayment-dialog" note. `loan-mark-defaulted-dialog`
 * is still NOT registered here (not-yet-generated feature component; its "coming soon" snackbar
 * fallback is unchanged). DC3 count-assertion: 2 defaults (`onShowRepaymentDialog` here +
 * on [LoanDetailScreen] itself) / 2 overrides (this forwarding call + `GroupBankingNavHost.kt`'s
 * real wiring) / 0 suppressed. See API.md#route.
 */
fun NavGraphBuilder.loanDetailScreen(
    onNavigateBack: () -> Unit,
    onShowRepaymentDialog: (loanId: Long, memberId: Long, installmentAmount: Double) -> Unit = { _, _, _ -> },
) {
    composableWithRootPushTransitions<LoanDetailRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<LoanDetailRoute>()
        LoanDetailScreen(
            loanId = route.loanId,
            onNavigateBack = onNavigateBack,
            onShowRepaymentDialog = onShowRepaymentDialog,
        )
    }
}
