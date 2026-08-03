/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.loanapply

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import kpt.core.base.ui.nav.composableWithRootPushTransitions

/**
 * `/groups/{groupId}/loans/apply` -- the loan-apply form route (`ui.yaml#route`,
 * `ui.yaml#nav_params: { groupId }`). [groupId] is forwarded from `loan-list`'s "Apply for Loan
 * (FAB)" entry point (`flow.yaml#entry_points[0]`) and seeds [LoanApplyViewModel] via Koin
 * `parametersOf(groupId)`. See API.md#route.
 */
@Serializable
data class LoanApplyRoute(val groupId: Long)

fun NavController.navigateToLoanApply(groupId: Long, navOptions: NavOptions? = null) =
    navigate(LoanApplyRoute(groupId = groupId), navOptions)

/**
 * Registers [LoanApplyScreen] on the host [NavGraphBuilder]. Every callback here closes a
 * [LoanApplyEvent] navigation branch consumed by the Container (RULE-PROTO-COMPOSE-DEAD-CLICK-001
 * DC3) -- matching `flow.yaml#navigates_to` (`meeting-conduct`, `loan-list`):
 * [onNavigateToMeetingConduct] resolves `LoanApplyEvent.NavigateToMeetingConduct(loanId)` and
 * [onNavigateBack] resolves `LoanApplyEvent.NavigateBack` (pop back onto `loan-list`, same
 * generic pass-through convention as `MemberAddRoute.kt`). No callback carries a `= {}` default
 * -- DC3 count-assertion: 0 defaults / 0 overrides / 0 suppressed. See API.md#route.
 */
fun NavGraphBuilder.loanApplyScreen(
    onNavigateToMeetingConduct: (loanId: Long) -> Unit,
    onNavigateBack: () -> Unit,
) {
    composableWithRootPushTransitions<LoanApplyRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<LoanApplyRoute>()
        LoanApplyScreen(
            groupId = route.groupId,
            onNavigateToMeetingConduct = onNavigateToMeetingConduct,
            onNavigateBack = onNavigateBack,
        )
    }
}
