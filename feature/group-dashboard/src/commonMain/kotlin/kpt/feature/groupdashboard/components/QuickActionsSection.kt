/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.groupdashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kpt.core.base.designsystem.component.AppCard
import kpt.core.designsystem.theme.spacing
import kpt.feature.groupdashboard.GroupDashboardAction
import kpt.feature.groupdashboard.GroupDashboardTestTags
import kpt.feature.groupdashboard.generated.resources.Res
import kpt.feature.groupdashboard.generated.resources.screens_group_dashboard_action_loans
import kpt.feature.groupdashboard.generated.resources.screens_group_dashboard_action_meetings
import kpt.feature.groupdashboard.generated.resources.screens_group_dashboard_action_members
import kpt.feature.groupdashboard.generated.resources.screens_group_dashboard_action_my_loans
import kpt.feature.groupdashboard.generated.resources.screens_group_dashboard_action_my_savings
import kpt.feature.groupdashboard.generated.resources.screens_group_dashboard_action_share_out
import kpt.feature.groupdashboard.generated.resources.screens_group_dashboard_action_start_meeting
import kpt.feature.groupdashboard.generated.resources.screens_group_dashboard_quick_actions_label
import kpt.feature.groupdashboard.generated.resources.screens_group_dashboard_share_out_disabled_hint
import org.jetbrains.compose.resources.stringResource

/** ORGANIZER / CHAIRPERSON / TREASURER — mirrors `ui.yaml#management_actions_grid.visible`. */
private val MANAGEMENT_ROLES = setOf("ORGANIZER", "CHAIRPERSON", "TREASURER")

/** ORGANIZER / TREASURER only — mirrors `ui.yaml#share_out_button.enabled`. */
private val SHARE_OUT_ROLES = setOf("ORGANIZER", "TREASURER")

/**
 * Role-gated quick-action grid — `ui.yaml#components.quick_actions_section`. Management roles
 * (ORGANIZER/CHAIRPERSON/TREASURER) see `management_actions_grid` (Start Meeting / Members /
 * Loans / Share-Out); MEMBER sees the read-only `member_actions_grid` (My Savings / My Loans /
 * Meetings / Members). Every button dispatches a typed [GroupDashboardAction] member — no bare
 * `onClick = {}` (RULE-IMPL-DEAD-CLICKABLE-001 Rule 1). See API.md#screen.
 */
@Composable
fun QuickActionsSection(
    viewerRole: String,
    isCycleEnd: Boolean,
    onAction: (GroupDashboardAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val label = stringResource(Res.string.screens_group_dashboard_quick_actions_label)

    AppCard(modifier = modifier.fillMaxWidth().testTag(GroupDashboardTestTags.QUICK_ACTIONS_SECTION)) {
        Column {
            Text(text = label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (viewerRole in MANAGEMENT_ROLES) {
                val shareOutEnabled = isCycleEnd && viewerRole in SHARE_OUT_ROLES
                Column(modifier = Modifier.padding(top = sp.md), verticalArrangement = Arrangement.spacedBy(sp.sm)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(sp.sm)) {
                        QuickActionButton(
                            label = stringResource(Res.string.screens_group_dashboard_action_start_meeting),
                            icon = Icons.Filled.MeetingRoom,
                            filled = true,
                            onClick = { onAction(GroupDashboardAction.OnStartMeeting) },
                            testTag = GroupDashboardTestTags.START_MEETING_BUTTON,
                            modifier = Modifier.weight(1f),
                        )
                        // Meetings is a CORE quick action (read-only meeting calendar/list), peer to
                        // Members / Loans / Share-Out — mirrors the member grid's Meetings action.
                        QuickActionButton(
                            label = stringResource(Res.string.screens_group_dashboard_action_meetings),
                            icon = Icons.Filled.CalendarMonth,
                            onClick = { onAction(GroupDashboardAction.OnViewMeetings) },
                            testTag = GroupDashboardTestTags.MEETINGS_BUTTON,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(sp.sm)) {
                        QuickActionButton(
                            label = stringResource(Res.string.screens_group_dashboard_action_members),
                            icon = Icons.Filled.Group,
                            onClick = { onAction(GroupDashboardAction.OnViewMembers) },
                            testTag = GroupDashboardTestTags.VIEW_MEMBERS_BUTTON,
                            modifier = Modifier.weight(1f),
                        )
                        QuickActionButton(
                            label = stringResource(Res.string.screens_group_dashboard_action_loans),
                            icon = Icons.Filled.AccountBalance,
                            onClick = { onAction(GroupDashboardAction.OnViewLoans) },
                            testTag = GroupDashboardTestTags.VIEW_LOANS_BUTTON,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(sp.sm)) {
                        QuickActionButton(
                            label = stringResource(Res.string.screens_group_dashboard_action_share_out),
                            icon = Icons.Filled.Share,
                            enabled = shareOutEnabled,
                            onClick = { onAction(GroupDashboardAction.OnShareOut) },
                            testTag = GroupDashboardTestTags.SHARE_OUT_BUTTON,
                            modifier = Modifier.weight(1f),
                        )
                        // Keep the 2-column grid aligned — Share-Out sits in a full row with an empty
                        // peer slot (its disabled hint renders below).
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    if (!shareOutEnabled) {
                        Text(
                            text = stringResource(Res.string.screens_group_dashboard_share_out_disabled_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.padding(top = sp.md), verticalArrangement = Arrangement.spacedBy(sp.sm)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(sp.sm)) {
                        QuickActionButton(
                            label = stringResource(Res.string.screens_group_dashboard_action_my_savings),
                            icon = Icons.Filled.Savings,
                            onClick = { onAction(GroupDashboardAction.OnViewSavings) },
                            testTag = GroupDashboardTestTags.VIEW_SAVINGS_BUTTON,
                            modifier = Modifier.weight(1f),
                        )
                        QuickActionButton(
                            label = stringResource(Res.string.screens_group_dashboard_action_my_loans),
                            icon = Icons.Filled.AccountBalance,
                            onClick = { onAction(GroupDashboardAction.OnViewLoans) },
                            testTag = GroupDashboardTestTags.VIEW_LOANS_MEMBER_BUTTON,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(sp.sm)) {
                        QuickActionButton(
                            label = stringResource(Res.string.screens_group_dashboard_action_meetings),
                            icon = Icons.Filled.CalendarMonth,
                            // Member role: read-only meeting history/schedule — same OnStartMeeting
                            // action member, no corpus-sufficiency check (see
                            // GroupDashboardViewModel.handleStartMeeting KDoc).
                            onClick = { onAction(GroupDashboardAction.OnStartMeeting) },
                            testTag = GroupDashboardTestTags.VIEW_MEETINGS_BUTTON,
                            modifier = Modifier.weight(1f),
                        )
                        QuickActionButton(
                            label = stringResource(Res.string.screens_group_dashboard_action_members),
                            icon = Icons.Filled.Group,
                            onClick = { onAction(GroupDashboardAction.OnViewMembers) },
                            testTag = GroupDashboardTestTags.VIEW_MEMBERS_MEMBER_BUTTON,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/** One quick-action tile — ≥56dp touch target per `ui.yaml#style.min_touch_target`. Icon is
 * decorative (`contentDescription = null`) — the button's own [Text] label carries the accessible
 * name (M3 merges descendant [Text] into the button's semantics automatically), mirrors
 * `GroupListErrorSection`'s identical icon+label Button precedent. */
@Composable
private fun QuickActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    enabled: Boolean = true,
) {
    val sp = MaterialTheme.spacing
    val buttonModifier = modifier.heightIn(min = 56.dp).testTag(testTag)
    if (filled) {
        Button(onClick = onClick, enabled = enabled, colors = ButtonDefaults.buttonColors(), modifier = buttonModifier) {
            Icon(imageVector = icon, contentDescription = null)
            Text(text = label, modifier = Modifier.padding(start = sp.xs))
        }
    } else {
        OutlinedButton(onClick = onClick, enabled = enabled, modifier = buttonModifier) {
            Icon(imageVector = icon, contentDescription = null)
            Text(text = label, modifier = Modifier.padding(start = sp.xs))
        }
    }
}
