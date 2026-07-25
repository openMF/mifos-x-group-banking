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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import org.mifos.groupbanking.feature.loanmarkdefaulteddialog.LoanMarkDefaultedDialog
import org.mifos.groupbanking.feature.loanrepaymentdialog.LoanRepaymentDialog
import org.mifos.groupbanking.feature.loginsignup.LoginSignupRoute
import org.mifos.groupbanking.feature.loginsignup.loginSignupScreen
import org.mifos.groupbanking.feature.loginsignup.navigateToLoginSignup
import org.mifos.groupbanking.feature.membersavingsdetail.memberSavingsDetailScreen
import org.mifos.groupbanking.feature.membersavingsdetail.navigateToMemberSavingsDetail
import org.mifos.groupbanking.feature.personaldashboard.navigateToPersonalDashboard
import org.mifos.groupbanking.feature.personaldashboard.personalDashboardScreen
import org.mifos.groupbanking.feature.savingsdashboard.navigateToSavingsDashboard
import org.mifos.groupbanking.feature.savingsdashboard.savingsDashboardScreen
import org.mifos.groupbanking.feature.settings.settingsScreen
import org.mifos.groupbanking.feature.settingslogoutdialog.SettingsLogoutDialog
import org.mifos.groupbanking.feature.shareoutexecute.navigateToShareOutExecute
import org.mifos.groupbanking.feature.shareoutexecute.shareOutExecuteScreen
import org.mifos.groupbanking.feature.shareoutpreview.navigateToShareOutPreview
import org.mifos.groupbanking.feature.shareoutpreview.shareOutPreviewScreen

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
 * for real (their feature modules now exist).
 *
 * `loan-repayment-dialog` and `loan-mark-defaulted-dialog` (both `archetype: dialog`,
 * `parent_screen: loan-detail`) are BOTH wired for real: this NavHost is the ONLY module that
 * depends on `feature/loan-detail` together with either dialog feature — every other cross-feature
 * wire-up in this codebase happens here, never as a direct feature-to-feature module dependency —
 * so `repaymentDialogTarget` / `defaultDialogTarget` (local `remember { mutableStateOf(...) }`
 * state, set by `loanDetailScreen`'s `onShowRepaymentDialog` / `onShowDefaultDialog` callbacks)
 * drive rendering [LoanRepaymentDialog] / [LoanMarkDefaultedDialog] as overlays on top of the whole
 * `NavHost` Box (a Compose `AlertDialog` renders in its own `Popup`/window regardless of where in
 * the tree it is composed, so their position here — siblings of the `NavHost` call, not nested
 * inside it — has no visual effect on the overlay).
 *
 * `settings` (migrated off the legacy `kpt.feature.settings` template shell onto
 * `org.mifos.groupbanking.feature.settings`, `idea-layer/screens/settings/ui.yaml`) is wired the
 * same way: `settingsScreen(...)`'s `onShowLogoutDialog` flips `showSettingsLogoutDialog` (local
 * `remember { mutableStateOf(false) }` state, same convention as `repaymentDialogTarget` above)
 * which renders [SettingsLogoutDialog] as an overlay; `onNavigateToLogin` routes straight to
 * [navigateToLoginSignup] (the session-expired-mid-PIN-change path, `data-flow.yaml`'s `401 ->
 * navigate: login`). **Known gap:** no screen in this NavHost currently calls
 * `navController.navigateToSettings()` — `ui.yaml#entry_points[0]` declares a `bottom_nav` trigger
 * that does not exist in this app's current live navigation shell (no bottom nav / tab bar is
 * wired here yet). The destination is registered and fully reachable via
 * `navController.navigateToSettings()`, but no in-app affordance calls it yet; flagged as a
 * residual follow-up rather than inventing an unrequested settings entry point on an unrelated
 * screen.
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

    // See class KDoc "loan-repayment-dialog / loan-mark-defaulted-dialog" note — the feature<->
    // feature seams in this NavHost.
    var repaymentDialogTarget by remember { mutableStateOf<RepaymentDialogTarget?>(null) }
    var defaultDialogTarget by remember { mutableStateOf<DefaultDialogTarget?>(null) }
    var showSettingsLogoutDialog by remember { mutableStateOf(false) }

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
                    // group-dashboard `Share Out (cycle-end action)` → share-out-preview
                    // (`idea-layer/screens/share-out-preview/ui.yaml#entry_points[0]`). This seam
                    // supplies only groupId (the catalogue GroupTypeConfig is not held here — same
                    // drift ShareOutPreviewRoute's "drift bridge" KDoc documents); navigate by
                    // groupId alone, the companion preview response drives poolModel/shareoutFormula.
                    onNavigateToShareOut = { groupId, _ -> navController.navigateToShareOutPreview(groupId = groupId) },
                    // group-dashboard `OnViewSavings` → the group-level savings-dashboard
                    // (`idea-layer/screens/savings-dashboard/ui.yaml#entry_points[0]`: "OnViewSavings
                    // (all roles)"), NOT member-savings-detail. GroupDashboardViewModel emits this as
                    // `NavigateToMemberSavingsDetail(groupId)` carrying only groupId (a flagged
                    // idea-layer drift, see that VM's KDoc + SavingsDashboardRoute KDoc "drift
                    // bridge"): the catalogue GroupTypeConfig this screen's nav_params want is not
                    // available at this seam, so we navigate by groupId alone.
                    onNavigateToMemberSavingsDetail = { groupId ->
                        navController.navigateToSavingsDashboard(groupId = groupId)
                    },
                    onNavigateBack = { navController.popBackStack() },
                )

                // 7a. savings-dashboard → member-savings-detail (member-row tap) / back.
                savingsDashboardScreen(
                    onNavigateToMemberDetail = { memberId, groupId, typeConfig ->
                        navController.navigateToMemberSavingsDetail(
                            memberId = memberId,
                            groupId = groupId,
                            typeConfig = typeConfig,
                        )
                    },
                    onNavigateBack = { navController.popBackStack() },
                )

                // 7b. member-savings-detail → back (terminal read-only leaf, reached from
                // savings-dashboard's per-member rows; its feature module already exists).
                memberSavingsDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                )

                // 7c. share-out-preview → share-out-execute (confirm) / back. Reached from
                // group-dashboard's `Share Out (cycle-end action)`. The confirm handoff now wires to
                // the REAL share-out-execute feature module (COMP-DIST-001/002 irreversible execute).
                shareOutPreviewScreen(
                    onNavigateToShareOutExecute = { groupId, typeConfig, totalPool, memberPayouts ->
                        navController.navigateToShareOutExecute(
                            groupId = groupId,
                            typeConfig = typeConfig,
                            totalPool = totalPool,
                            memberPayouts = memberPayouts,
                        )
                    },
                    onNavigateBack = { navController.popBackStack() },
                )

                // 7d. share-out-execute → group-dashboard (done) / back to share-out-preview.
                shareOutExecuteScreen(
                    // Only organizer/treasurer/chairperson roles can reach execute (entry gate on
                    // share-out-preview), so returning to the dashboard as ORGANIZER matches the
                    // existing group-create/join return convention above; the dashboard re-resolves
                    // the authoritative viewerRole server-side on load.
                    onNavigateToGroupDashboard = { groupId ->
                        navController.navigateToGroupDashboard(groupId = groupId, viewerRole = "ORGANIZER")
                    },
                    onNavigateBack = { navController.popBackStack() },
                )

                // 8. loan-list → loan-detail (wired for real, its feature module now exists) / loan-apply
                loanListScreen(
                    onNavigateToLoanDetail = { loanId -> navController.navigateToLoanDetail(loanId = loanId) },
                    // TODO(nav): replace when loan-apply feature is implemented
                    onNavigateToLoanApply = { _ -> navController.navigateToPlaceholder("Apply for Loan") },
                    onNavigateBack = { navController.popBackStack() },
                )

                // 9. loan-detail → back to loan-list (flow.yaml#navigates_to: [loan-list]) /
                //    loan-repayment-dialog + loan-mark-defaulted-dialog overlays (see class KDoc)
                loanDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onShowRepaymentDialog = { loanId, memberId, installmentAmount ->
                        repaymentDialogTarget = RepaymentDialogTarget(
                            loanId = loanId,
                            memberId = memberId,
                            installmentAmount = installmentAmount,
                        )
                    },
                    onShowDefaultDialog = { loanId, memberName, loanAmountKes ->
                        defaultDialogTarget = DefaultDialogTarget(
                            loanId = loanId,
                            memberName = memberName,
                            loanAmountKes = loanAmountKes,
                        )
                    },
                )

                // 10. settings -- migrated off the legacy `kpt.feature.settings` template shell
                // (see class KDoc "settings" note). Reachable via `navController.navigateToSettings()`.
                settingsScreen(
                    onNavigateToLogin = { navController.navigateToLoginSignup() },
                    onShowLogoutDialog = { showSettingsLogoutDialog = true },
                    onNavigateBack = { navController.popBackStack() },
                )

                // Shared "Coming soon" destination for every not-yet-built onward target.
                placeholderDestination()
            }

            // settings-logout-dialog -- overlay on top of settings, see class KDoc "settings" note.
            if (showSettingsLogoutDialog) {
                SettingsLogoutDialog(
                    onDismiss = { showSettingsLogoutDialog = false },
                    onNavigateToLogin = {
                        showSettingsLogoutDialog = false
                        navController.navigateToLoginSignup()
                    },
                )
            }

            // loan-repayment-dialog — overlay on top of loan-detail, see class KDoc.
            repaymentDialogTarget?.let { target ->
                LoanRepaymentDialog(
                    loanId = target.loanId,
                    memberId = target.memberId,
                    installmentAmount = target.installmentAmount,
                    onDismiss = { repaymentDialogTarget = null },
                    onRepaymentRecorded = { repaymentDialogTarget = null },
                )
            }

            // loan-mark-defaulted-dialog — overlay on top of loan-detail, see class KDoc.
            defaultDialogTarget?.let { target ->
                LoanMarkDefaultedDialog(
                    loanId = target.loanId,
                    memberName = target.memberName,
                    loanAmountKes = target.loanAmountKes,
                    onDismiss = { defaultDialogTarget = null },
                    onMarkedDefaulted = { defaultDialogTarget = null },
                )
            }
        }
    }
}

/**
 * Nav-arg bundle carried from `loan-detail`'s `LoanDetailEvent.ShowRepaymentDialog` (resolved
 * inside `LoanDetailScreen.kt`'s Container — see [GroupBankingNavHost]'s class KDoc) to this
 * NavHost's [LoanRepaymentDialog] overlay.
 */
private data class RepaymentDialogTarget(
    val loanId: Long,
    val memberId: Long,
    val installmentAmount: Double,
)

/**
 * Nav-arg bundle carried from `loan-detail`'s `LoanDetailEvent.ShowDefaultConfirmDialog` (resolved
 * inside `LoanDetailScreen.kt`'s Container — see [GroupBankingNavHost]'s class KDoc) to this
 * NavHost's [LoanMarkDefaultedDialog] overlay.
 */
private data class DefaultDialogTarget(
    val loanId: Long,
    val memberName: String,
    val loanAmountKes: Double,
)

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
