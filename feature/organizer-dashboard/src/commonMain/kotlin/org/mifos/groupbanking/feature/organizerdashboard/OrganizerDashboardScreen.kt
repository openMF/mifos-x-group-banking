/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.organizerdashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kpt.core.base.designsystem.core.TopAppBarAction
import kpt.core.base.ui.effects.EventsEffect
import kpt.core.designsystem.theme.spacing
import kpt.core.ui.scaffold.KptScaffold
import kpt.core.ui.scaffold.rememberKptPullToRefreshState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.mifos.groupbanking.core.model.OrganizerActivityItem
import org.mifos.groupbanking.core.model.ScheduledMeeting
import org.mifos.groupbanking.feature.organizerdashboard.components.ActivityRow
import org.mifos.groupbanking.feature.organizerdashboard.components.KpiGroupsIcon
import org.mifos.groupbanking.feature.organizerdashboard.components.KpiMeetingsIcon
import org.mifos.groupbanking.feature.organizerdashboard.components.KpiMembersIcon
import org.mifos.groupbanking.feature.organizerdashboard.components.KpiShareOutIcon
import org.mifos.groupbanking.feature.organizerdashboard.components.NavFieldOfficerIcon
import org.mifos.groupbanking.feature.organizerdashboard.components.NavGroupsIcon
import org.mifos.groupbanking.feature.organizerdashboard.components.OrganizerKpiCard
import org.mifos.groupbanking.feature.organizerdashboard.components.OrganizerKpiCardData
import org.mifos.groupbanking.feature.organizerdashboard.components.OrganizerQuickNavTile
import org.mifos.groupbanking.feature.organizerdashboard.components.ScheduleRow
import org.mifos.groupbanking.feature.organizerdashboard.generated.resources.Res
import org.mifos.groupbanking.feature.organizerdashboard.generated.resources.screens_organizer_dashboard_activity_label
import org.mifos.groupbanking.feature.organizerdashboard.generated.resources.screens_organizer_dashboard_activity_supporting
import org.mifos.groupbanking.feature.organizerdashboard.generated.resources.screens_organizer_dashboard_badge_optional
import org.mifos.groupbanking.feature.organizerdashboard.generated.resources.screens_organizer_dashboard_empty_body
import org.mifos.groupbanking.feature.organizerdashboard.generated.resources.screens_organizer_dashboard_empty_cta
import org.mifos.groupbanking.feature.organizerdashboard.generated.resources.screens_organizer_dashboard_empty_title
import org.mifos.groupbanking.feature.organizerdashboard.generated.resources.screens_organizer_dashboard_error_auth
import org.mifos.groupbanking.feature.organizerdashboard.generated.resources.screens_organizer_dashboard_error_cta_retry
import org.mifos.groupbanking.feature.organizerdashboard.generated.resources.screens_organizer_dashboard_error_load_title
import org.mifos.groupbanking.feature.organizerdashboard.generated.resources.screens_organizer_dashboard_error_network
import org.mifos.groupbanking.feature.organizerdashboard.generated.resources.screens_organizer_dashboard_error_server
import org.mifos.groupbanking.feature.organizerdashboard.generated.resources.screens_organizer_dashboard_kpi_meetings_today
import org.mifos.groupbanking.feature.organizerdashboard.generated.resources.screens_organizer_dashboard_kpi_members
import org.mifos.groupbanking.feature.organizerdashboard.generated.resources.screens_organizer_dashboard_kpi_my_groups
import org.mifos.groupbanking.feature.organizerdashboard.generated.resources.screens_organizer_dashboard_kpi_pending_shareout
import org.mifos.groupbanking.feature.organizerdashboard.generated.resources.screens_organizer_dashboard_loading_message
import org.mifos.groupbanking.feature.organizerdashboard.generated.resources.screens_organizer_dashboard_nav_all_groups
import org.mifos.groupbanking.feature.organizerdashboard.generated.resources.screens_organizer_dashboard_nav_all_groups_sub
import org.mifos.groupbanking.feature.organizerdashboard.generated.resources.screens_organizer_dashboard_nav_field_officers
import org.mifos.groupbanking.feature.organizerdashboard.generated.resources.screens_organizer_dashboard_nav_field_officers_sub
import org.mifos.groupbanking.feature.organizerdashboard.generated.resources.screens_organizer_dashboard_no_meetings_today
import org.mifos.groupbanking.feature.organizerdashboard.generated.resources.screens_organizer_dashboard_notifications_cd
import org.mifos.groupbanking.feature.organizerdashboard.generated.resources.screens_organizer_dashboard_notifications_deferred
import org.mifos.groupbanking.feature.organizerdashboard.generated.resources.screens_organizer_dashboard_quick_nav_label
import org.mifos.groupbanking.feature.organizerdashboard.generated.resources.screens_organizer_dashboard_schedule_count_chip
import org.mifos.groupbanking.feature.organizerdashboard.generated.resources.screens_organizer_dashboard_schedule_row_cd
import org.mifos.groupbanking.feature.organizerdashboard.generated.resources.screens_organizer_dashboard_schedule_supporting
import org.mifos.groupbanking.feature.organizerdashboard.generated.resources.screens_organizer_dashboard_todays_schedule_label
import org.mifos.groupbanking.feature.organizerdashboard.generated.resources.screens_organizer_dashboard_top_bar_title
import org.mifos.groupbanking.feature.organizerdashboard.generated.resources.screens_organizer_dashboard_welcome_greeting
import org.mifos.groupbanking.feature.organizerdashboard.generated.resources.screens_organizer_dashboard_welcome_subtitle

/**
 * Container for `organizer-dashboard-screen`. Collects [OrganizerDashboardViewModel] state, consumes
 * one-shot [OrganizerDashboardEvent]s (navigate to group-list / field-officer-dashboard, snackbar)
 * through [EventsEffect], and delegates rendering to the stateless [OrganizerDashboardContent].
 * See API.md#screen.
 */
@Composable
internal fun OrganizerDashboardScreen(
    onNavigateToGroupList: () -> Unit,
    onNavigateToFieldOfficerDashboard: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OrganizerDashboardViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val networkMsg = stringResource(Res.string.screens_organizer_dashboard_error_network)
    val serverMsg = stringResource(Res.string.screens_organizer_dashboard_error_server)
    val authMsg = stringResource(Res.string.screens_organizer_dashboard_error_auth)
    val notificationsDeferredMsg = stringResource(Res.string.screens_organizer_dashboard_notifications_deferred)

    EventsEffect(viewModel) { event ->
        when (event) {
            OrganizerDashboardEvent.NavigateToGroupList -> onNavigateToGroupList()
            OrganizerDashboardEvent.NavigateToFieldOfficerDashboard -> onNavigateToFieldOfficerDashboard()
            is OrganizerDashboardEvent.ShowSnackbar -> {
                val resolved = when (event.message) {
                    "error_network" -> networkMsg
                    "error_server" -> serverMsg
                    "error_auth" -> authMsg
                    ORGANIZER_NOTIFICATIONS_DEFERRED_KEY -> notificationsDeferredMsg
                    else -> event.message
                }
                snackbarHostState.showSnackbar(resolved)
            }
        }
    }

    OrganizerDashboardContent(
        state = state,
        onAction = viewModel::trySendAction,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

/**
 * Stateless render surface for `organizer-dashboard-screen`. State-driven per
 * `OrganizerDashboardState.screenState` — every member is handled (Loading/Content/Empty/Error).
 * See API.md#screen.
 */
@Composable
internal fun OrganizerDashboardContent(
    state: OrganizerDashboardState,
    onAction: (OrganizerDashboardAction) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val title = stringResource(Res.string.screens_organizer_dashboard_top_bar_title)
    val notificationsCd = stringResource(Res.string.screens_organizer_dashboard_notifications_cd)

    KptScaffold(
        showNavigationIcon = false,
        title = title,
        actions = listOf(
            TopAppBarAction(
                icon = Icons.Filled.Notifications,
                contentDescription = notificationsCd,
                onClick = { onAction(OrganizerDashboardAction.OnOpenNotifications) },
            ),
        ),
        pullToRefreshState = rememberKptPullToRefreshState(
            isEnabled = true,
            isRefreshing = state.isRefreshing,
            onRefresh = { onAction(OrganizerDashboardAction.OnRefresh) },
        ),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.testTag(OrganizerDashboardTestTags.SCREEN),
    ) {
        when (state.screenState) {
            OrganizerDashboardScreenState.Loading -> OrganizerLoadingSection()
            OrganizerDashboardScreenState.Content -> OrganizerContentSection(state = state, onAction = onAction)
            OrganizerDashboardScreenState.Empty -> OrganizerEmptySection(onViewGroups = { onAction(OrganizerDashboardAction.OnViewAllGroups) })
            OrganizerDashboardScreenState.Error -> OrganizerErrorSection(
                message = when (state.error) {
                    OrganizerDashboardError.Network, null -> stringResource(Res.string.screens_organizer_dashboard_error_network)
                    OrganizerDashboardError.Server -> stringResource(Res.string.screens_organizer_dashboard_error_server)
                    OrganizerDashboardError.Auth -> stringResource(Res.string.screens_organizer_dashboard_error_auth)
                },
                onRetry = { onAction(OrganizerDashboardAction.Retry) },
            )
        }
    }
}

@Composable
private fun OrganizerLoadingSection(modifier: Modifier = Modifier) {
    val loadingLabel = stringResource(Res.string.screens_organizer_dashboard_loading_message)
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.testTag(OrganizerDashboardTestTags.LOADING_INDICATOR),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = loadingLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = MaterialTheme.spacing.md),
        )
    }
}

@Composable
private fun OrganizerContentSection(
    state: OrganizerDashboardState,
    onAction: (OrganizerDashboardAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing

    val greeting = stringResource(Res.string.screens_organizer_dashboard_welcome_greeting, state.organizerName)
    val subtitle = stringResource(Res.string.screens_organizer_dashboard_welcome_subtitle)

    val kpiGroups = OrganizerKpiCardData(
        value = state.myGroupCount.toString(),
        label = stringResource(Res.string.screens_organizer_dashboard_kpi_my_groups),
        icon = KpiGroupsIcon,
        testTag = OrganizerDashboardTestTags.KPI_GROUPS_CARD,
    )
    val kpiMembers = OrganizerKpiCardData(
        value = state.totalMembers.toString(),
        label = stringResource(Res.string.screens_organizer_dashboard_kpi_members),
        icon = KpiMembersIcon,
        testTag = OrganizerDashboardTestTags.KPI_MEMBERS_CARD,
    )
    val kpiShareOut = OrganizerKpiCardData(
        value = state.pendingShareOutCount.toString(),
        label = stringResource(Res.string.screens_organizer_dashboard_kpi_pending_shareout),
        icon = KpiShareOutIcon,
        testTag = OrganizerDashboardTestTags.KPI_SHAREOUT_CARD,
        emphasizeNonZero = true,
        nonZero = state.pendingShareOutCount > 0,
    )
    val kpiMeetings = OrganizerKpiCardData(
        value = state.meetingsTodayCount.toString(),
        label = stringResource(Res.string.screens_organizer_dashboard_kpi_meetings_today),
        icon = KpiMeetingsIcon,
        testTag = OrganizerDashboardTestTags.KPI_MEETINGS_CARD,
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag(OrganizerDashboardTestTags.CONTENT),
        verticalArrangement = Arrangement.spacedBy(sp.md),
        contentPadding = PaddingValues(bottom = sp.lg),
    ) {
        // Welcome header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(OrganizerDashboardTestTags.WELCOME_HEADER)
                    .padding(horizontal = sp.lg, vertical = sp.md),
                verticalArrangement = Arrangement.spacedBy(sp.xs),
            ) {
                Text(text = greeting, style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // KPI summary row (2x2 grid rendered as two Rows so it fits narrow screens)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(OrganizerDashboardTestTags.KPI_ROW)
                    .padding(horizontal = sp.lg),
                verticalArrangement = Arrangement.spacedBy(sp.sm),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(sp.sm)) {
                    OrganizerKpiCard(kpiGroups, onClick = { onAction(OrganizerDashboardAction.OnViewAllGroups) }, modifier = Modifier.weight(1f))
                    OrganizerKpiCard(kpiMembers, onClick = { onAction(OrganizerDashboardAction.OnViewAllGroups) }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(sp.sm)) {
                    OrganizerKpiCard(kpiShareOut, onClick = { onAction(OrganizerDashboardAction.OnViewAllGroups) }, modifier = Modifier.weight(1f))
                    OrganizerKpiCard(kpiMeetings, onClick = { onAction(OrganizerDashboardAction.OnViewAllGroups) }, modifier = Modifier.weight(1f))
                }
            }
        }

        // Quick navigation section
        item { QuickNavSection(state = state, onAction = onAction) }

        // Today's Schedule section
        item { TodaysScheduleSection(state = state, onAction = onAction) }

        // Recent Activity section
        item { RecentActivitySection(state = state) }
    }
}

@Composable
private fun QuickNavSection(
    state: OrganizerDashboardState,
    onAction: (OrganizerDashboardAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = sp.lg)
            .testTag(OrganizerDashboardTestTags.QUICK_NAV_SECTION),
    ) {
        Column(modifier = Modifier.padding(sp.md), verticalArrangement = Arrangement.spacedBy(sp.sm)) {
            Text(
                text = stringResource(Res.string.screens_organizer_dashboard_quick_nav_label),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(sp.sm)) {
                OrganizerQuickNavTile(
                    icon = NavGroupsIcon,
                    label = stringResource(Res.string.screens_organizer_dashboard_nav_all_groups),
                    sublabel = stringResource(Res.string.screens_organizer_dashboard_nav_all_groups_sub, state.myGroupCount),
                    container = MaterialTheme.colorScheme.primaryContainer,
                    onContainer = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = { onAction(OrganizerDashboardAction.OnViewAllGroups) },
                    testTag = OrganizerDashboardTestTags.NAV_GROUP_LIST_CARD,
                    modifier = Modifier.weight(1f),
                )
                if (state.fieldOfficerEnabled) {
                    OrganizerQuickNavTile(
                        icon = NavFieldOfficerIcon,
                        label = stringResource(Res.string.screens_organizer_dashboard_nav_field_officers),
                        sublabel = stringResource(Res.string.screens_organizer_dashboard_nav_field_officers_sub),
                        container = MaterialTheme.colorScheme.secondaryContainer,
                        onContainer = MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = { onAction(OrganizerDashboardAction.OnViewFieldOfficer) },
                        testTag = OrganizerDashboardTestTags.NAV_FIELD_OFFICER_CARD,
                        badgeLabel = stringResource(Res.string.screens_organizer_dashboard_badge_optional),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun TodaysScheduleSection(
    state: OrganizerDashboardState,
    onAction: (OrganizerDashboardAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = sp.lg)
            .testTag(OrganizerDashboardTestTags.SCHEDULE_SECTION),
    ) {
        Column(modifier = Modifier.padding(sp.md), verticalArrangement = Arrangement.spacedBy(sp.xs)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.screens_organizer_dashboard_todays_schedule_label),
                    style = MaterialTheme.typography.titleMedium,
                )
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                ) {
                    Text(
                        text = stringResource(Res.string.screens_organizer_dashboard_schedule_count_chip, state.meetingsTodayCount),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = sp.sm, vertical = sp.xs),
                    )
                }
            }
            if (state.todaySchedule.isEmpty()) {
                Text(
                    text = stringResource(Res.string.screens_organizer_dashboard_no_meetings_today),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(OrganizerDashboardTestTags.SCHEDULE_EMPTY_LABEL)
                        .padding(vertical = sp.md),
                )
            } else {
                state.todaySchedule.forEach { meeting ->
                    ScheduleRowItem(meeting = meeting, onAction = onAction)
                }
            }
        }
    }
}

@Composable
private fun ScheduleRowItem(
    meeting: ScheduledMeeting,
    onAction: (OrganizerDashboardAction) -> Unit,
) {
    val supporting = stringResource(
        Res.string.screens_organizer_dashboard_schedule_supporting,
        meeting.meetingTime,
        meeting.memberCount,
    )
    val rowCd = stringResource(
        Res.string.screens_organizer_dashboard_schedule_row_cd,
        meeting.groupName,
        meeting.meetingTime,
    )
    ScheduleRow(
        meeting = meeting,
        supporting = supporting,
        rowCd = rowCd,
        onClick = { onAction(OrganizerDashboardAction.OnMeetingGroupClick(meeting.groupId)) },
        testTag = OrganizerDashboardTestTags.scheduleRowTag(meeting.groupId),
    )
}

@Composable
private fun RecentActivitySection(
    state: OrganizerDashboardState,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = sp.lg)
            .testTag(OrganizerDashboardTestTags.ACTIVITY_SECTION),
    ) {
        Column(modifier = Modifier.padding(sp.md), verticalArrangement = Arrangement.spacedBy(sp.xs)) {
            Text(
                text = stringResource(Res.string.screens_organizer_dashboard_activity_label),
                style = MaterialTheme.typography.titleMedium,
            )
            state.recentActivity.forEach { activity ->
                ActivityRowItem(activity = activity)
            }
        }
    }
}

@Composable
private fun ActivityRowItem(activity: OrganizerActivityItem) {
    val supporting = stringResource(
        Res.string.screens_organizer_dashboard_activity_supporting,
        activity.date,
        activity.groupName ?: "",
    )
    ActivityRow(
        activity = activity,
        supporting = supporting,
        testTag = OrganizerDashboardTestTags.activityRowTag(activity.id),
    )
}

@Composable
private fun OrganizerEmptySection(onViewGroups: () -> Unit, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(sp.lg)
            .testTag(OrganizerDashboardTestTags.EMPTY_SECTION),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.DashboardCustomize,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(80.dp),
        )
        Text(
            text = stringResource(Res.string.screens_organizer_dashboard_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = sp.lg),
        )
        Text(
            text = stringResource(Res.string.screens_organizer_dashboard_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = sp.sm),
        )
        Button(
            onClick = onViewGroups,
            modifier = Modifier
                .heightIn(min = sp.touchTargetMin)
                .padding(top = sp.lg)
                .testTag(OrganizerDashboardTestTags.EMPTY_CTA_BUTTON),
        ) {
            Text(text = stringResource(Res.string.screens_organizer_dashboard_empty_cta))
        }
    }
}

@Composable
private fun OrganizerErrorSection(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(sp.lg)
            .testTag(OrganizerDashboardTestTags.ERROR_SECTION),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.WifiOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp),
        )
        Text(
            text = stringResource(Res.string.screens_organizer_dashboard_error_load_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = sp.lg),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = sp.sm),
        )
        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = sp.touchTargetMin)
                .padding(top = sp.lg)
                .testTag(OrganizerDashboardTestTags.ERROR_RETRY_BUTTON),
        ) {
            Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
            Text(
                text = stringResource(Res.string.screens_organizer_dashboard_error_cta_retry),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = sp.xs),
            )
        }
    }
}
