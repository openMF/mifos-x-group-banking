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
import org.mifos.groupbanking.feature.fieldofficerdashboard.fieldOfficerDashboardScreen
import org.mifos.groupbanking.feature.fieldofficerdashboard.navigateToFieldOfficerDashboard
import org.mifos.groupbanking.feature.groupcreate.groupCreateScreen
import org.mifos.groupbanking.feature.groupcreate.navigateToGroupCreate
import org.mifos.groupbanking.feature.groupdashboard.groupDashboardScreen
import org.mifos.groupbanking.feature.groupdashboard.navigateToGroupDashboard
import org.mifos.groupbanking.feature.grouplist.groupListScreen
import org.mifos.groupbanking.feature.grouplist.navigateToGroupList
import org.mifos.groupbanking.feature.grouptypepicker.groupTypePickerScreen
import org.mifos.groupbanking.feature.grouptypepicker.navigateToGroupTypePicker
import org.mifos.groupbanking.feature.joinwithcode.joinWithCodeScreen
import org.mifos.groupbanking.feature.joinwithcode.navigateToJoinWithCode
import org.mifos.groupbanking.feature.loanapply.loanApplyScreen
import org.mifos.groupbanking.feature.loanapply.navigateToLoanApply
import org.mifos.groupbanking.feature.loandetail.loanDetailScreen
import org.mifos.groupbanking.feature.loandetail.navigateToLoanDetail
import org.mifos.groupbanking.feature.loanlist.loanListScreen
import org.mifos.groupbanking.feature.loanlist.navigateToLoanList
import org.mifos.groupbanking.feature.loanmarkdefaulteddialog.LoanMarkDefaultedDialog
import org.mifos.groupbanking.feature.loanrepaymentdialog.LoanRepaymentDialog
import org.mifos.groupbanking.feature.loanrequest.loanRequestScreen
import org.mifos.groupbanking.feature.loanrequest.navigateToLoanRequest
import org.mifos.groupbanking.feature.loginsignup.LoginSignupRoute
import org.mifos.groupbanking.feature.loginsignup.loginSignupScreen
import org.mifos.groupbanking.feature.loginsignup.navigateToLoginSignup
import org.mifos.groupbanking.feature.meetingcalendar.meetingCalendarScreen
import org.mifos.groupbanking.feature.meetingcalendar.navigateToMeetingCalendar
import org.mifos.groupbanking.feature.meetingconduct.meetingConductScreen
import org.mifos.groupbanking.feature.meetingconduct.navigateToMeetingConduct
import org.mifos.groupbanking.feature.meetingsummary.meetingSummaryScreen
import org.mifos.groupbanking.feature.meetingsummary.navigateToMeetingSummary
import org.mifos.groupbanking.feature.memberadd.memberAddScreen
import org.mifos.groupbanking.feature.memberadd.navigateToMemberAdd
import org.mifos.groupbanking.feature.memberinvite.memberInviteScreen
import org.mifos.groupbanking.feature.memberinvite.navigateToMemberInvite
import org.mifos.groupbanking.feature.memberlist.memberListScreen
import org.mifos.groupbanking.feature.memberlist.navigateToMemberList
import org.mifos.groupbanking.feature.memberprofile.memberProfileScreen
import org.mifos.groupbanking.feature.memberprofile.navigateToMemberProfile
import org.mifos.groupbanking.feature.membersavingsdetail.memberSavingsDetailScreen
import org.mifos.groupbanking.feature.membersavingsdetail.navigateToMemberSavingsDetail
import org.mifos.groupbanking.feature.organizerdashboard.navigateToOrganizerDashboard
import org.mifos.groupbanking.feature.organizerdashboard.organizerDashboardScreen
import org.mifos.groupbanking.feature.personaldashboard.PersonalDashboardRoute
import org.mifos.groupbanking.feature.personaldashboard.navigateToPersonalDashboard
import org.mifos.groupbanking.feature.personaldashboard.personalDashboardScreen
import org.mifos.groupbanking.feature.personalloans.navigateToPersonalLoans
import org.mifos.groupbanking.feature.personalloans.personalLoansScreen
import org.mifos.groupbanking.feature.personalsavings.navigateToPersonalSavings
import org.mifos.groupbanking.feature.personalsavings.personalSavingsScreen
import org.mifos.groupbanking.feature.previousmeetingreview.navigateToPreviousMeetingReview
import org.mifos.groupbanking.feature.previousmeetingreview.previousMeetingReviewScreen
import org.mifos.groupbanking.feature.savingsdashboard.navigateToSavingsDashboard
import org.mifos.groupbanking.feature.savingsdashboard.savingsDashboardScreen
import org.mifos.groupbanking.feature.settings.navigateToSettings
import org.mifos.groupbanking.feature.settings.settingsScreen
import org.mifos.groupbanking.feature.settingslogoutdialog.SettingsLogoutDialog
import org.mifos.groupbanking.feature.shareoutexecute.navigateToShareOutExecute
import org.mifos.groupbanking.feature.shareoutexecute.shareOutExecuteScreen
import org.mifos.groupbanking.feature.shareoutpreview.navigateToShareOutPreview
import org.mifos.groupbanking.feature.shareoutpreview.shareOutPreviewScreen
import org.mifos.groupbanking.feature.syncstatus.navigateToSyncStatus
import org.mifos.groupbanking.feature.syncstatus.syncStatusScreen

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
 * navigate: login`). **Resolved (was a known gap):** `settings` and `sync-status` are now reached
 * from `personal-dashboard`'s profile/overflow menu — the idea-layer declares that affordance on
 * the authenticated member home (`personal-dashboard/ui.yaml#components.top_bar.overflow_menu`),
 * replacing the never-wired `bottom_nav` trigger the settings/sync-status ui.yaml previously
 * declared. `personalDashboardScreen`'s `onNavigateToSettings`/`onNavigateToSyncStatus` callbacks
 * call `navController.navigateToSettings()` / `navController.navigateToSyncStatus()`; both
 * destinations are registered below (`settingsScreen(...)` + `syncStatusScreen()`).
 *
 * The un-deferred `personal-dashboard` loan entry card
 * (`personal-dashboard/ui.yaml#components.loan_card`) is likewise wired: `onNavigateToLoans` →
 * `navigateToPersonalLoans(clientId)`, with `personalLoansScreen(...)` + `loanRequestScreen(...)`
 * registered below (personal-dashboard → personal-loans → loan-request).
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
                startDestination = LoginSignupRoute(),
                modifier = Modifier.fillMaxSize(),
            ) {
                // 1. login-signup (start / pre-auth)
                loginSignupScreen(
                    onNavigateToPersonalDashboard = { navController.navigateToPersonalDashboard() },
                    // Organizer-in-any-group landing branch (SPEC.md#login-routing +
                    // organizer-dashboard `ui.yaml#entry_points` `app_launch` gated on
                    // `isOrganizerInAnyGroup`). This is the real role-gated login landing seam that
                    // makes `organizer-dashboard` reachable (and `field-officer-dashboard`
                    // transitively, via its optional-tier quick-nav tile). `group-list` stays
                    // reachable from the organizer hub's All-Groups quick-nav + personal-dashboard.
                    onNavigateToOrganizerDashboard = { navController.navigateToOrganizerDashboard() },
                    onNavigateToGroupList = { navController.navigateToGroupList() },
                    onNavigateToGroupTypePicker = { navController.navigateToGroupTypePicker() },
                    // Thread the optional pre-auth resume invite code onward so join-with-code
                    // can pre-fill it (null for a plain Accept-Invitation / zero-groups Join tap).
                    onNavigateToJoinWithCode = { inviteCode ->
                        navController.navigateToJoinWithCode(inviteCode = inviteCode)
                    },
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
                    // F5 — joiner lands on their own personal-dashboard (identity + joined group
                    // resolve from the auth token); replaces the prior group-dashboard landing.
                    onNavigateToPersonalDashboard = { navController.navigateToPersonalDashboard() },
                    // Pre-auth invite resume (TC-LS-010): carry the entered code back to
                    // login-signup so a subsequent login/signup success resumes the join.
                    onNavigateToLoginSignup = { pendingInviteCode ->
                        navController.navigateToLoginSignup(pendingInviteCode = pendingInviteCode)
                    },
                    onNavigateBack = { navController.popBackStack() },
                )

                // 6. personal-dashboard → group-list (+ savings placeholder)
                personalDashboardScreen(
                    // personal-dashboard `savings_summary_card.on_click` → the member's own
                    // personal-savings ledger. The event now carries the three personal-savings
                    // nav_params (clientId, groupLinkedSavingsId, optional individualSavingsId),
                    // forwarded from the get_member_dashboard response — wired straight to the real
                    // `personal-savings` feature module (the earlier savings-dashboard drift bridge
                    // is retired now that MemberDashboard carries these ids).
                    onNavigateToSavings = { clientId, groupLinkedSavingsId, individualSavingsId, _ ->
                        navController.navigateToPersonalSavings(
                            clientId = clientId,
                            groupLinkedSavingsId = groupLinkedSavingsId,
                            individualSavingsId = individualSavingsId,
                        )
                    },
                    onNavigateToGroupList = { navController.navigateToGroupList() },
                    // personal-dashboard `loan_card.on_click` → the member's own loan list. The
                    // event carries the member's clientId (personal-loans' nav_param). Un-deferred
                    // loan entry (idea-layer/screens/personal-dashboard/ui.yaml#components.loan_card).
                    onNavigateToLoans = { clientId -> navController.navigateToPersonalLoans(clientId = clientId) },
                    // personal-dashboard profile/overflow menu → shared settings + sync-status
                    // (idea-layer/screens/personal-dashboard/ui.yaml#components.top_bar.overflow_menu).
                    // This is the in-app affordance that resolves the previously-flagged "no screen
                    // calls navigateToSettings()/navigateToSyncStatus()" gap (see class KDoc).
                    onNavigateToSettings = { navController.navigateToSettings() },
                    onNavigateToSyncStatus = { navController.navigateToSyncStatus() },
                )

                // 6y. personal-loans → loan-request (FAB / empty CTA) / back. Reached from
                //     personal-dashboard's loan_card tap. `personal-loans` forwards only its own
                //     `clientId` nav-arg to loan-request; `savingsBalance` is not held at this seam
                //     (MemberDashboard/personal-loans carry no savings-balance for the member), so it
                //     is bridged as 0.0 and `loanMultiplier` defaults to 3.0 — the same documented
                //     drift-bridge convention this NavHost uses elsewhere (see PersonalLoansRoute
                //     KDoc "caller's responsibility at the nav-graph wiring site"). loan-request then
                //     re-resolves the member's real eligibility server-side on load.
                personalLoansScreen(
                    onNavigateToLoanRequest = { clientId ->
                        navController.navigateToLoanRequest(clientId = clientId, savingsBalance = 0.0)
                    },
                    onNavigateBack = { navController.popBackStack() },
                )

                // 6x. loan-request → personal-dashboard (submit success) / back to personal-loans.
                //     Reached from personal-loans' Request-Loan FAB / empty-state CTA. On success the
                //     member returns to their dashboard: pop back past personal-loans to the existing
                //     personal-dashboard entry (inclusive=false keeps the dashboard on the stack)
                //     rather than pushing a duplicate dashboard.
                loanRequestScreen(
                    onNavigateToDashboard = {
                        navController.popBackStack(PersonalDashboardRoute, inclusive = false)
                    },
                    onNavigateBack = { navController.popBackStack() },
                )

                // 6z. personal-savings → back to personal-dashboard (terminal read-only leaf,
                //     reached from personal-dashboard's savings_summary_card tap).
                personalSavingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                )

                // 6a. field-officer-dashboard (FR-009) → group-dashboard (supervisory read-only view) /
                // export share-sheet. Registered + fully reachable via
                // `navController.navigateToFieldOfficerDashboard()`; its `ui.yaml#entry_points` declare
                // an `app_launch` trigger gated on `userRole == FIELD_OFFICER || PROGRAM_MANAGER`, but
                // this app's current login shell has no staff-role landing branch, so no in-app
                // affordance calls it yet — the SAME residual-follow-up convention as `settings` above
                // (flagged for an idea-layer / login-shell update, never an invented entry point).
                fieldOfficerDashboardScreen(
                    onNavigateToGroupDashboard = { groupId ->
                        navController.navigateToGroupDashboard(groupId = groupId.toString(), viewerRole = "FIELD_OFFICER")
                    },
                    // TODO(nav): replace with the OS share-sheet handoff once the CSV export surface exists
                    onExportReport = { navController.navigateToPlaceholder("Export Report") },
                )

                // 6b. organizer-dashboard (organizer-per-group hub) → group-list (KPI cards / All-Groups
                // quick-nav / meeting rows / empty CTA) + field-officer-dashboard (optional-tier tile).
                // Registered + fully reachable via `navController.navigateToOrganizerDashboard()`; its
                // `ui.yaml#entry_points` declare an `app_launch` trigger gated on
                // `isOrganizerInAnyGroup`, but this app's current login shell has no organizer-role
                // landing branch, so no in-app affordance calls it yet — the SAME residual-follow-up
                // convention as `field-officer-dashboard` / `settings` above (flagged for an idea-layer
                // / login-shell update, never an invented entry point).
                organizerDashboardScreen(
                    onNavigateToGroupList = { navController.navigateToGroupList() },
                    onNavigateToFieldOfficerDashboard = { navController.navigateToFieldOfficerDashboard() },
                    // G7 — Today's-Schedule row + Meetings-Today KPI open the tapped group's meeting
                    // calendar. group-dashboard forwards a `groupId: String`; meeting-calendar's
                    // nav_param is `center_id: Int` — same toIntOrNull drift bridge as the
                    // group-dashboard → meeting-calendar seam below.
                    onNavigateToMeetingCalendar = { groupId ->
                        navController.navigateToMeetingCalendar(centerId = groupId.toIntOrNull() ?: 0)
                    },
                    // Top-bar overflow menu → shared settings + sync-status (both param-less,
                    // already registered) — gives the organizer a Settings entry on their landing.
                    onNavigateToSettings = { navController.navigateToSettings() },
                    onNavigateToSyncStatus = { navController.navigateToSyncStatus() },
                )

                // 7. group-dashboard → not-yet-built onward targets (placeholders) + loan-list
                groupDashboardScreen(
                    // group-dashboard `Start/View Meetings` → meeting-calendar. group-dashboard
                    // forwards a `groupId: String` but meeting-calendar's nav_param is `center_id: Int`
                    // (a flagged idea-layer nav-param drift — group-dashboard has no centerId at this
                    // seam); bridge by parsing groupId, mirroring the loan-list `toLongOrNull` drift
                    // precedent above. Reported to the caller for an idea-layer follow-up (forward
                    // fineractCenterId from group-dashboard, or key meeting-calendar by groupId).
                    onNavigateToMeetingCalendar = { groupId ->
                        navController.navigateToMeetingCalendar(centerId = groupId.toIntOrNull() ?: 0)
                    },
                    onNavigateToMemberList = { groupId -> navController.navigateToMemberList(groupId = groupId) },
                    // viewerRole is forwarded from group-dashboard (server-reconciled role) so
                    // loan-list can gate the Apply-Loan FAB (canApplyLoan); loan-list threads it
                    // onward to loan-detail for the record-repayment / mark-defaulted gates.
                    onNavigateToLoanList = { groupId, viewerRole ->
                        navController.navigateToLoanList(groupId = groupId.toLongOrNull() ?: 0L, viewerRole = viewerRole)
                    },
                    // group-dashboard `Share Out (cycle-end action)` → share-out-preview
                    // (`idea-layer/screens/share-out-preview/ui.yaml#entry_points[0]`). This seam
                    // supplies only groupId (the catalogue GroupTypeConfig is not held here — same
                    // drift ShareOutPreviewRoute's "drift bridge" KDoc documents); navigate by
                    // groupId alone, the companion preview response drives poolModel/shareoutFormula.
                    onNavigateToShareOut = { groupId, _ -> navController.navigateToShareOutPreview(groupId = groupId) },
                    // G9 — group-dashboard `OnViewSavings` → the group-level savings-dashboard
                    // (`idea-layer/screens/savings-dashboard/ui.yaml#entry_points[0]`: "OnViewSavings
                    // (all roles)"), NOT member-savings-detail. GroupDashboardViewModel emits this as
                    // `NavigateToSavingsDashboard(groupId)` carrying only groupId (the dashboard holds
                    // GroupInstanceConfig, not the catalogue GroupTypeConfig savings-dashboard's
                    // nav_param wants — flagged idea-layer drift; typeConfig degrades to default here).
                    onNavigateToSavingsDashboard = { groupId ->
                        navController.navigateToSavingsDashboard(groupId = groupId)
                    },
                    // G13 — top-bar overflow menu → shared param-less destinations.
                    onNavigateToSettings = { navController.navigateToSettings() },
                    onNavigateToSyncStatus = { navController.navigateToSyncStatus() },
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
                    onNavigateToShareOutExecute = { groupId, typeConfig, totalPool, memberPayouts, cycleNumber ->
                        navController.navigateToShareOutExecute(
                            groupId = groupId,
                            typeConfig = typeConfig,
                            totalPool = totalPool,
                            memberPayouts = memberPayouts,
                            cycleNumber = cycleNumber,
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
                    onNavigateToLoanDetail = { loanId, viewerRole ->
                        navController.navigateToLoanDetail(loanId = loanId, viewerRole = viewerRole)
                    },
                    onNavigateToLoanApply = { groupId -> navController.navigateToLoanApply(groupId = groupId) },
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

                // 9b. meeting-summary → back to meeting-calendar (flow.yaml#navigates_to:
                //     [meeting-calendar]). Read-only screen; Done/back both pop to the prior
                //     back-stack entry (the calendar / conduct wizard the summary was opened from).
                //     Reachable via `navController.navigateToMeetingSummary(...)` (deep-link entry).
                meetingSummaryScreen(
                    onNavigateDone = { navController.popBackStack() },
                )

                // 9c. meeting-calendar → meeting-conduct (Start Meeting) / previous-meeting-review
                //     (past-meeting drill-down) / back. Reached from group-dashboard's Meetings action.
                //     Both onward targets route to PlaceholderRoute until their feature modules land
                //     (mirrors loan-apply's not-yet-generated-target convention) — never a dead click.
                meetingCalendarScreen(
                    // meeting-calendar `Start Meeting` → the real meeting-conduct wizard.
                    // `centerId` is now forwarded by `meetingCalendarScreen` (from its own
                    // `MeetingCalendarRoute.centerId`), so meeting-conduct gets its full nav-arg
                    // set. `meetingNumber` comes from the tapped meeting card's event payload.
                    onNavigateToConduct = { meetingId, meetingNumber, centerId ->
                        navController.navigateToMeetingConduct(
                            meetingId = meetingId,
                            meetingNumber = meetingNumber,
                            centerId = centerId,
                        )
                    },
                    // G5 → previous-meeting-review (past-meeting drill-down, calendar-launched).
                    // `centerId` + `launchedFrom` ("calendar") are forwarded from the calendar screen's
                    // event (was previously the `centerId = 0` drift bridge + a hardcoded launchedFrom).
                    onNavigateToReview = { meetingId, meetingNumber, centerId, launchedFrom ->
                        navController.navigateToPreviousMeetingReview(
                            meetingId = meetingId,
                            meetingNumber = meetingNumber,
                            centerId = centerId,
                            launchedFrom = launchedFrom,
                        )
                    },
                    onNavigateBack = { navController.popBackStack() },
                )

                // 9d. meeting-conduct (7-step wizard) → meeting-summary (submit success) /
                //     previous-meeting-review (step-0 drill-down) / back. Reached via
                //     `navController.navigateToMeetingConduct(meetingId, meetingNumber, centerId)`
                //     (from meeting-calendar's Start-Meeting card once that callback forwards centerId).
                //     Submit success routes to the REAL meeting-summary screen; the not-yet-built
                //     previous-meeting-review target falls back to PlaceholderRoute (never a dead click).
                meetingConductScreen(
                    onNavigateToMeetingSummary = { meetingId, meetingNumber, centerId ->
                        navController.navigateToMeetingSummary(
                            meetingId = meetingId,
                            meetingNumber = meetingNumber,
                            centerId = centerId,
                        )
                    },
                    // G6 → previous-meeting-review (step-0 drill-down, conduct-launched). The callback
                    // now forwards the real meetingId (String) + meetingNumber (Int, no longer dropped)
                    // + centerId + launchedFrom ("conduct") — resolves the prior meeting_number=0 drift.
                    onNavigateToPreviousMeetingReview = { meetingId, meetingNumber, centerId, launchedFrom ->
                        navController.navigateToPreviousMeetingReview(
                            meetingId = meetingId,
                            meetingNumber = meetingNumber,
                            centerId = centerId,
                            launchedFrom = launchedFrom,
                        )
                    },
                    onNavigateBack = { navController.popBackStack() },
                )

                // 9e. previous-meeting-review (read-only FR-019 recap) → meeting-conduct (Start Meeting,
                //     conduct-launched only) / back. Reached from meeting-calendar (completed-meeting card)
                //     + meeting-conduct (step-0 drill-down), and via deep-link
                //     navigateToPreviousMeetingReview(...). The Start-Meeting onward target routes to the
                //     PlaceholderRoute until meeting-conduct navigation is wired (never a dead click).
                previousMeetingReviewScreen(
                    onNavigateBack = { navController.popBackStack() },
                    // previous-meeting-review `Start Meeting` (conduct-launched drill-down) → the
                    // real meeting-conduct wizard; its callback already forwards the full
                    // (meetingId, meetingNumber, centerId) nav-arg set.
                    onNavigateToConduct = { meetingId, meetingNumber, centerId ->
                        navController.navigateToMeetingConduct(
                            meetingId = meetingId,
                            meetingNumber = meetingNumber,
                            centerId = centerId,
                        )
                    },
                )

                // 10. settings -- migrated off the legacy `kpt.feature.settings` template shell
                // (see class KDoc "settings" note). Reached via `navController.navigateToSettings()`
                // from personal-dashboard's profile/overflow menu.
                settingsScreen(
                    onNavigateToLogin = { navController.navigateToLoginSignup() },
                    onShowLogoutDialog = { showSettingsLogoutDialog = true },
                    onNavigateBack = { navController.popBackStack() },
                )

                // 10a. sync-status -- read-only offline-sync dashboard (terminal, no outbound nav).
                // Reached via `navController.navigateToSyncStatus()` from personal-dashboard's
                // profile/overflow menu (idea-layer/screens/sync-status/ui.yaml#entry_points).
                syncStatusScreen()

                // 11. member-list → member-profile (row tap) / member-add (Add-Member FAB) / back.
                //     Reached from group-dashboard's "View Members" action.
                memberListScreen(
                    onMemberClick = { memberId, groupId ->
                        navController.navigateToMemberProfile(memberId = memberId, groupId = groupId)
                    },
                    onAddMember = { groupId -> navController.navigateToMemberAdd(groupId = groupId) },
                    // member-onboarding-flow invite path — member-list "Invite Member" top-bar action
                    // → member-invite (member-invite/ui.yaml#entry_points[0]: source=member-list,
                    // trigger=invite_fab_tap, forwarding groupId). Makes member-invite reachable.
                    onInviteMember = { groupId -> navController.navigateToMemberInvite(groupId = groupId) },
                    onBack = { navController.popBackStack() },
                )

                // 11c. member-invite → back to member-list / group-dashboard (invite-path leaf,
                //      flow.yaml#navigates_to: [member-list, group-dashboard]). Reached from
                //      member-list's "Invite Member" top-bar action.
                memberInviteScreen(
                    onNavigateBack = { navController.popBackStack() },
                )

                // 11a. member-profile → member-list (role change / removal returns) /
                //      member-savings-detail (view-savings tap) / back. Reached from member-list's
                //      row tap.
                memberProfileScreen(
                    onNavigateToMemberList = { groupId -> navController.navigateToMemberList(groupId = groupId) },
                    // member-profile `view savings` → the member's savings. The event carries only
                    // (memberId, groupId); `member-savings-detail`'s route is keyed by a
                    // GroupTypeConfig not available at this seam, so we navigate to the
                    // param-compatible group `savings-dashboard` (keyed by groupId) — the SAME
                    // drift-bridge convention group-dashboard's `onNavigateToMemberSavingsDetail`
                    // uses. TODO(nav): route straight to `member-savings-detail` once the idea-layer
                    // forwards the group's GroupTypeConfig through MemberProfileEvent.NavigateToSavingsDetail.
                    onNavigateToSavingsDetail = { _, groupId ->
                        navController.navigateToSavingsDashboard(groupId = groupId)
                    },
                )

                // 11b. member-add → member-profile (on success) / back. Reached from member-list's
                //      Add-Member FAB.
                memberAddScreen(
                    onNavigateToMemberProfile = { memberId, groupId ->
                        navController.navigateToMemberProfile(memberId = memberId, groupId = groupId)
                    },
                    onNavigateBack = { navController.popBackStack() },
                )

                // 12. loan-apply → meeting-conduct (submitted loan is discussed at the next meeting)
                //     / back to loan-list. Reached from loan-list's "Apply for Loan" FAB.
                loanApplyScreen(
                    // loan-apply's `NavigateToMeetingConduct(loanId)` forwards a loanId, but
                    // meeting-conduct is keyed by (meetingId, meetingNumber, centerId) — a flagged
                    // idea-layer nav-param drift (the loan-to-meeting linkage isn't resolved at this
                    // seam). Bridge with the loanId as the meetingId and 0 for the meeting/center
                    // ids, mirroring this NavHost's other documented drift bridges (e.g. the
                    // group-dashboard→meeting-calendar `toIntOrNull ?: 0` centerId bridge).
                    // TODO(nav): resolve the real meetingId/centerId once loan-apply forwards them.
                    onNavigateToMeetingConduct = { loanId ->
                        navController.navigateToMeetingConduct(
                            meetingId = loanId.toString(),
                            meetingNumber = 0,
                            centerId = 0,
                        )
                    },
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
