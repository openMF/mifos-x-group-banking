/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.loandetail

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
data class LoanDetailRoute(val loanId: Long, val viewerRole: String)

fun NavController.navigateToLoanDetail(loanId: Long, viewerRole: String, navOptions: NavOptions? = null) =
    navigate(LoanDetailRoute(loanId = loanId, viewerRole = viewerRole), navOptions)

/**
 * Registers [LoanDetailScreen] on the host [NavGraphBuilder]. [onNavigateBack] closes the single
 * [LoanDetailEvent.NavigateBack] navigation branch consumed by the Container, matching
 * `flow.yaml#navigates_to: [loan-list]`. [onShowRepaymentDialog] / [onShowDefaultDialog] forward
 * [LoanDetailScreen]'s identically-named callbacks up to `cmp-navigation`
 * (`GroupBankingNavHost.kt`), the module that actually depends on `feature/loan-repayment-dialog` /
 * `feature/loan-mark-defaulted-dialog` and renders both dialogs as overlays — see
 * `LoanDetailScreen.kt`'s class KDoc "loan-repayment-dialog / loan-mark-defaulted-dialog" note.
 * DC3 count-assertion: 4 defaults (`onShowRepaymentDialog` + `onShowDefaultDialog` here, and again
 * on [LoanDetailScreen] itself) / 4 overrides (both forwarding calls here + `GroupBankingNavHost.kt`'s
 * real wiring for both) / 0 suppressed. See API.md#route.
 */
fun NavGraphBuilder.loanDetailScreen(
    onNavigateBack: () -> Unit,
    onShowRepaymentDialog: (loanId: Long, memberId: Long, installmentAmount: Double) -> Unit = { _, _, _ -> },
    onShowDefaultDialog: (loanId: Long, memberName: String, loanAmountKes: Double) -> Unit = { _, _, _ -> },
) {
    composableWithRootPushTransitions<LoanDetailRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<LoanDetailRoute>()
        LoanDetailScreen(
            loanId = route.loanId,
            viewerRole = route.viewerRole,
            onNavigateBack = onNavigateBack,
            onShowRepaymentDialog = onShowRepaymentDialog,
            onShowDefaultDialog = onShowDefaultDialog,
        )
    }
}
