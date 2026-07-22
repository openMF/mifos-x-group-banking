/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package cmp.navigation.groupbanking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import cmp.navigation.ui.rememberKptNavController
import kotlinx.serialization.Serializable
import kpt.core.base.ui.KptConnectivityBanner
import org.mifos.groupbanking.feature.groupcreate.groupCreateScreen
import org.mifos.groupbanking.feature.groupcreate.navigateToGroupCreate
import org.mifos.groupbanking.feature.groupdashboard.groupDashboardScreen
import org.mifos.groupbanking.feature.groupdashboard.navigateToGroupDashboard
import org.mifos.groupbanking.feature.grouplist.GroupListRoute
import org.mifos.groupbanking.feature.grouplist.groupListScreen
import org.mifos.groupbanking.feature.grouplist.navigateToGroupList
import org.mifos.groupbanking.feature.grouptypepicker.groupTypePickerScreen
import org.mifos.groupbanking.feature.grouptypepicker.navigateToGroupTypePicker
import org.mifos.groupbanking.feature.joinwithcode.joinWithCodeScreen
import org.mifos.groupbanking.feature.joinwithcode.navigateToJoinWithCode
import org.mifos.groupbanking.feature.loandetail.loanDetailScreen
import org.mifos.groupbanking.feature.loandetail.navigateToLoanDetail
import org.mifos.groupbanking.feature.loanlist.loanListScreen
import org.mifos.groupbanking.feature.loanlist.navigateToLoanList
import org.mifos.groupbanking.feature.loginsignup.LoginSignupRoute
import org.mifos.groupbanking.feature.loginsignup.loginSignupScreen
import org.mifos.groupbanking.feature.loginsignup.navigateToLoginSignup
import org.mifos.groupbanking.feature.personaldashboard.navigateToPersonalDashboard
import org.mifos.groupbanking.feature.personaldashboard.personalDashboardScreen

/**
 * App-level NavHost for the mifos-x group-banking journey.
 *
 * Start destination is [LoginSignupRoute] (`/auth`); on auth success the login screen routes to
 * the organizer path (group-list), the member path (personal-dashboard), or the group-creation /
 * join-with-code paths. Every inter-feature nav callback declared by the 8 feature `*Route.kt`
 * extensions is wired here.
 *
 * Onward targets that are not yet generated as feature modules (meeting-calendar, member-list,
 * loan-apply, share-out, member-savings-detail, savings) route to [PlaceholderRoute] — a real,
 * navigable "Coming soon" destination, never a no-op that breaks the back stack. Each is marked
 * `TODO(nav)` for replacement when its feature lands. `loan-list` and `loan-detail` are both wired
 * for real (their feature modules now exist) — `loan-detail`'s own onward targets
 * (`loan-repayment-dialog` / `loan-mark-defaulted-dialog`) are not yet generated feature
 * components and surface an in-screen "coming soon" snackbar instead (see
 * `LoanDetailScreen.kt`'s class KDoc), not a [PlaceholderRoute] destination.
 */
@Composable
fun GroupBankingNavHost(
    modifier: Modifier = Modifier,
    onSplashScreenRemoved: () -> Unit = {},
    navController: NavHostController = rememberKptNavController(name = "GroupBankingNavHost"),
) {
    // No auth/passcode gate in this journey build — the login screen IS the start, so the Android
    // splash can be removed as soon as the NavHost composes.
    LaunchedEffect(Unit) { onSplashScreenRemoved() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        KptConnectivityBanner()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .consumeWindowInsets(WindowInsets.statusBars),
        ) {
            NavHost(
                navController = navController,
                startDestination = LoginSignupRoute,
                modifier = Modifier.fillMaxSize(),
            ) {
                // 1. login-signup (start / pre-auth)
                loginSignupScreen(
                    onNavigateToPersonalDashboard = { navController.navigateToPersonalDashboard() },
                    onNavigateToGroupList = { navController.navigateToGroupList() },
                    onNavigateToGroupTypePicker = { navController.navigateToGroupTypePicker() },
                    onNavigateToJoinWithCode = { navController.navigateToJoinWithCode() },
                )

                // 2. group-type-picker → group-create (carries the resolved GroupTypeConfig)
                groupTypePickerScreen(
                    onNavigateToGroupCreate = { typeConfig ->
                        navController.navigateToGroupCreate(typeConfig)
                    },
                    onNavigateBack = { navController.popBackStack() },
                )

                // 3. group-create → group-dashboard (creator is always the ORGANIZER)
                groupCreateScreen(
                    onNavigateToGroupDashboard = { groupId ->
                        navController.navigateToGroupDashboard(groupId = groupId, viewerRole = "ORGANIZER")
                    },
                    onNavigateBack = { navController.popBackStack() },
                )

                // 4. group-list → dashboard / create (via type-picker) / join
                groupListScreen(
                    onNavigateToGroupDashboard = { groupId, viewerRole ->
                        navController.navigateToGroupDashboard(groupId = groupId, viewerRole = viewerRole)
                    },
                    onNavigateToCreateGroup = { navController.navigateToGroupTypePicker() },
                    onNavigateToJoinGroup = { navController.navigateToJoinWithCode() },
                )

                // 5. join-with-code → dashboard (joiner is a MEMBER) / re-auth
                joinWithCodeScreen(
                    onNavigateToGroupDashboard = { groupId ->
                        navController.navigateToGroupDashboard(groupId = groupId, viewerRole = "MEMBER")
                    },
                    onNavigateToLoginSignup = { _ -> navController.navigateToLoginSignup() },
                    onNavigateBack = { navController.popBackStack() },
                )

                // 6. personal-dashboard → group-list (+ savings placeholder)
                personalDashboardScreen(
                    // TODO(nav): replace when savings feature is implemented
                    onNavigateToSavings = { _, _ -> navController.navigateToPlaceholder("Savings") },
                    onNavigateToGroupList = { navController.navigateToGroupList() },
                )

                // 7. group-dashboard → not-yet-built onward targets (placeholders) + loan-list
                groupDashboardScreen(
                    // TODO(nav): replace when meeting-calendar feature is implemented
                    onNavigateToMeetingCalendar = { navController.navigateToPlaceholder("Meetings") },
                    // TODO(nav): replace when member-list feature is implemented
                    onNavigateToMemberList = { navController.navigateToPlaceholder("Members") },
                    onNavigateToLoanList = { groupId ->
                        navController.navigateToLoanList(groupId = groupId.toLongOrNull() ?: 0L)
                    },
                    // TODO(nav): replace when share-out feature is implemented
                    onNavigateToShareOut = { _, _ -> navController.navigateToPlaceholder("Share-out") },
                    // TODO(nav): replace when member-savings-detail feature is implemented
                    onNavigateToMemberSavingsDetail = { navController.navigateToPlaceholder("Member savings") },
                    onNavigateBack = { navController.popBackStack() },
                )

                // 8. loan-list → loan-detail (wired for real, its feature module now exists) / loan-apply
                loanListScreen(
                    onNavigateToLoanDetail = { loanId -> navController.navigateToLoanDetail(loanId = loanId) },
                    // TODO(nav): replace when loan-apply feature is implemented
                    onNavigateToLoanApply = { _ -> navController.navigateToPlaceholder("Apply for Loan") },
                    onNavigateBack = { navController.popBackStack() },
                )

                // 9. loan-detail → back to loan-list (flow.yaml#navigates_to: [loan-list])
                loanDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                )

                // Shared "Coming soon" destination for every not-yet-built onward target.
                placeholderDestination()
            }
        }
    }
}

/**
 * A real, navigable placeholder destination for onward targets whose feature module has not yet
 * been generated. Renders a centered "Coming soon: <name>" message — never a crash or a no-op that
 * would leave a dead click / broken back stack.
 */
@Serializable
data class PlaceholderRoute(val title: String)

fun NavController.navigateToPlaceholder(title: String) = navigate(PlaceholderRoute(title = title))

private fun NavGraphBuilder.placeholderDestination() {
    composable<PlaceholderRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<PlaceholderRoute>()
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Coming soon: ${route.title}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
        }
    }
}
