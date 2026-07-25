/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.savingsdashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kpt.core.base.ui.effects.EventsEffect
import kpt.core.common.formatGrouped
import kpt.core.designsystem.theme.spacing
import kpt.core.ui.scaffold.KptScaffold
import kpt.core.ui.scaffold.rememberKptPullToRefreshState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.mifos.groupbanking.core.model.GroupTypeConfig
import org.mifos.groupbanking.core.model.MemberGroupSavingsRow
import org.mifos.groupbanking.core.model.MemberIndividualSavingsRow
import org.mifos.groupbanking.core.model.SavingsDashboardTab
import org.mifos.groupbanking.core.model.WeeklyContributionPoint
import org.mifos.groupbanking.feature.savingsdashboard.generated.resources.Res
import org.mifos.groupbanking.feature.savingsdashboard.generated.resources.screens_savings_dashboard_action_retry
import org.mifos.groupbanking.feature.savingsdashboard.generated.resources.screens_savings_dashboard_avatar_cd_format
import org.mifos.groupbanking.feature.savingsdashboard.generated.resources.screens_savings_dashboard_cycle_progress_amount_format
import org.mifos.groupbanking.feature.savingsdashboard.generated.resources.screens_savings_dashboard_cycle_progress_cd
import org.mifos.groupbanking.feature.savingsdashboard.generated.resources.screens_savings_dashboard_cycle_progress_label
import org.mifos.groupbanking.feature.savingsdashboard.generated.resources.screens_savings_dashboard_cycle_progress_percent_format
import org.mifos.groupbanking.feature.savingsdashboard.generated.resources.screens_savings_dashboard_cycle_progress_shares_format
import org.mifos.groupbanking.feature.savingsdashboard.generated.resources.screens_savings_dashboard_empty_icon_cd
import org.mifos.groupbanking.feature.savingsdashboard.generated.resources.screens_savings_dashboard_empty_subtitle
import org.mifos.groupbanking.feature.savingsdashboard.generated.resources.screens_savings_dashboard_empty_title
import org.mifos.groupbanking.feature.savingsdashboard.generated.resources.screens_savings_dashboard_error_auth
import org.mifos.groupbanking.feature.savingsdashboard.generated.resources.screens_savings_dashboard_error_banner
import org.mifos.groupbanking.feature.savingsdashboard.generated.resources.screens_savings_dashboard_error_group_not_found
import org.mifos.groupbanking.feature.savingsdashboard.generated.resources.screens_savings_dashboard_error_icon_cd
import org.mifos.groupbanking.feature.savingsdashboard.generated.resources.screens_savings_dashboard_error_offline
import org.mifos.groupbanking.feature.savingsdashboard.generated.resources.screens_savings_dashboard_error_server
import org.mifos.groupbanking.feature.savingsdashboard.generated.resources.screens_savings_dashboard_error_title
import org.mifos.groupbanking.feature.savingsdashboard.generated.resources.screens_savings_dashboard_group_total_chip_format
import org.mifos.groupbanking.feature.savingsdashboard.generated.resources.screens_savings_dashboard_group_trend_cd
import org.mifos.groupbanking.feature.savingsdashboard.generated.resources.screens_savings_dashboard_group_trend_title
import org.mifos.groupbanking.feature.savingsdashboard.generated.resources.screens_savings_dashboard_individual_balance_format
import org.mifos.groupbanking.feature.savingsdashboard.generated.resources.screens_savings_dashboard_individual_balance_label
import org.mifos.groupbanking.feature.savingsdashboard.generated.resources.screens_savings_dashboard_individual_last_txn_format
import org.mifos.groupbanking.feature.savingsdashboard.generated.resources.screens_savings_dashboard_individual_total_amount_format
import org.mifos.groupbanking.feature.savingsdashboard.generated.resources.screens_savings_dashboard_individual_trend_cd
import org.mifos.groupbanking.feature.savingsdashboard.generated.resources.screens_savings_dashboard_individual_trend_title
import org.mifos.groupbanking.feature.savingsdashboard.generated.resources.screens_savings_dashboard_last_synced_format
import org.mifos.groupbanking.feature.savingsdashboard.generated.resources.screens_savings_dashboard_member_amount_format
import org.mifos.groupbanking.feature.savingsdashboard.generated.resources.screens_savings_dashboard_member_label_share_value
import org.mifos.groupbanking.feature.savingsdashboard.generated.resources.screens_savings_dashboard_member_label_total
import org.mifos.groupbanking.feature.savingsdashboard.generated.resources.screens_savings_dashboard_member_meetings_format
import org.mifos.groupbanking.feature.savingsdashboard.generated.resources.screens_savings_dashboard_member_shares_format
import org.mifos.groupbanking.feature.savingsdashboard.generated.resources.screens_savings_dashboard_screen_title
import org.mifos.groupbanking.feature.savingsdashboard.generated.resources.screens_savings_dashboard_tab_group
import org.mifos.groupbanking.feature.savingsdashboard.generated.resources.screens_savings_dashboard_tab_individual
import org.mifos.groupbanking.feature.savingsdashboard.generated.resources.screens_savings_dashboard_total_individual_label
import org.mifos.groupbanking.feature.savingsdashboard.generated.resources.screens_savings_dashboard_trend_empty

private const val SHARE_BASED = "SHARE_BASED_VARIABLE"
private val CHART_HEIGHT = 180.dp
private val SKELETON_HEIGHT = 72.dp
private const val BAR_CORNER_RADIUS_PX = 6f

/**
 * Container for `savings-dashboard-screen`. Collects [SavingsDashboardViewModel] state via
 * [collectAsStateWithLifecycle], consumes one-shot [SavingsDashboardEvent]s (navigate to member
 * detail / show a snackbar) through [EventsEffect], dispatches the on_mount
 * [SavingsDashboardAction.LoadDashboard] from a [LaunchedEffect] (per
 * `ui.yaml#state_model.actions.members[0]` `trigger: "Screen enters composition"` — the ViewModel
 * does NOT auto-fire it in `init`, see its KDoc), and delegates all rendering to the stateless
 * [SavingsDashboardContent]. [groupId]/[typeConfig] are the `ui.yaml#nav_params` supplied to the
 * ViewModel via Koin `parametersOf(groupId, typeConfig)` (matching `SavingsDashboardModule`'s
 * declaration order). See API.md#screen.
 */
@Composable
internal fun SavingsDashboardScreen(
    groupId: String,
    typeConfig: GroupTypeConfig,
    onNavigateToMemberDetail: (memberId: String, groupId: String, typeConfig: GroupTypeConfig) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SavingsDashboardViewModel = koinViewModel(
        parameters = { parametersOf(groupId, typeConfig) },
    ),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Precomputed OUTSIDE EventsEffect — stringResource() is @Composable-only and the EventsEffect
    // callback runs in a suspend (non-composable) scope, same convention as MemberSavingsDetailScreen.
    val notFoundMessage = stringResource(Res.string.screens_savings_dashboard_error_group_not_found)
    val authMessage = stringResource(Res.string.screens_savings_dashboard_error_auth)
    val offlineMessage = stringResource(Res.string.screens_savings_dashboard_error_offline)
    val serverMessage = stringResource(Res.string.screens_savings_dashboard_error_server)

    EventsEffect(viewModel) { event ->
        when (event) {
            is SavingsDashboardEvent.NavigateToMemberDetail ->
                onNavigateToMemberDetail(event.memberId, event.groupId, event.typeConfig)

            is SavingsDashboardEvent.ShowError -> snackbarHostState.showSnackbar(
                message = when (event.message) {
                    "error_group_not_found" -> notFoundMessage
                    "error_auth" -> authMessage
                    "error_offline" -> offlineMessage
                    "error_server" -> serverMessage
                    else -> event.message
                },
            )
        }
    }

    LaunchedEffect(Unit) { viewModel.trySendAction(SavingsDashboardAction.LoadDashboard) }

    SavingsDashboardContent(
        state = state,
        onAction = viewModel::trySendAction,
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

/**
 * Stateless render surface for `savings-dashboard-screen`. State-driven per
 * [SavingsDashboardState.deriveScreenState] — every [SavingsDashboardScreenState] member is handled
 * (Loading/Content/Empty/Error). Pull-to-refresh and the error-banner/error-state retry all dispatch
 * the real declared [SavingsDashboardAction.RefreshDashboard]; tab taps dispatch
 * [SavingsDashboardAction.SelectTab]; member-row taps dispatch [SavingsDashboardAction.OpenMemberDetail]
 * — no free callbacks threaded through (RULE-IMPL-DEAD-CLICKABLE-001 Rule 3). See API.md#screen.
 */
@Composable
internal fun SavingsDashboardContent(
    state: SavingsDashboardState,
    onAction: (SavingsDashboardAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val title = stringResource(Res.string.screens_savings_dashboard_screen_title)
    val screenState = state.deriveScreenState()

    KptScaffold(
        showNavigationIcon = true,
        onNavigationIconClick = onNavigateBack,
        title = title,
        pullToRefreshState = rememberKptPullToRefreshState(
            isEnabled = screenState == SavingsDashboardScreenState.Content,
            isRefreshing = state.isRefreshing,
            onRefresh = { onAction(SavingsDashboardAction.RefreshDashboard) },
        ),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.testTag(SavingsDashboardTestTags.SCREEN),
    ) {
        when (screenState) {
            SavingsDashboardScreenState.Loading -> SavingsDashboardLoadingSection()
            SavingsDashboardScreenState.Content -> SavingsDashboardContentSection(state = state, onAction = onAction)
            SavingsDashboardScreenState.Empty -> SavingsDashboardEmptySection()
            SavingsDashboardScreenState.Error -> SavingsDashboardErrorSection(state = state, onAction = onAction)
        }
    }
}

// -- Loading (ui.yaml#states.loading: loading_skeleton, 6 shimmer blocks) ------------------------

@Composable
private fun SavingsDashboardLoadingSection(modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    Column(
        modifier = modifier.fillMaxSize().padding(sp.lg).testTag(SavingsDashboardTestTags.LOADING_SECTION),
        verticalArrangement = Arrangement.spacedBy(sp.sm),
    ) {
        repeat(6) { SkeletonBlock(height = SKELETON_HEIGHT) }
    }
}

@Composable
private fun SkeletonBlock(height: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(MaterialTheme.spacing.sm))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

// -- Content (sync band + inline error banner + tab row + selected pane) -------------------------

@Composable
private fun SavingsDashboardContentSection(
    state: SavingsDashboardState,
    onAction: (SavingsDashboardAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (state.lastSyncAt != null) {
            SyncBand(lastSyncAt = state.lastSyncAt)
        }
        if (state.error != null) {
            InlineErrorBanner(onRetry = { onAction(SavingsDashboardAction.RefreshDashboard) })
        }
        SavingsTabRow(
            selectedTab = state.selectedTab,
            onTabSelected = { onAction(SavingsDashboardAction.SelectTab(it)) },
        )
        when (state.selectedTab) {
            SavingsDashboardTab.GROUP -> GroupSavingsPane(state = state, onAction = onAction)
            SavingsDashboardTab.INDIVIDUAL -> IndividualSavingsPane(state = state, onAction = onAction)
        }
    }
}

@Composable
private fun SyncBand(lastSyncAt: String, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    Text(
        text = stringResource(Res.string.screens_savings_dashboard_last_synced_format, lastSyncAt),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = sp.lg, vertical = sp.xs)
            .testTag(SavingsDashboardTestTags.SYNC_BAND),
    )
}

@Composable
private fun InlineErrorBanner(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(sp.md)
            .testTag(SavingsDashboardTestTags.ERROR_BANNER),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.screens_savings_dashboard_error_banner),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.weight(1f).padding(end = sp.sm),
        )
        Text(
            text = stringResource(Res.string.screens_savings_dashboard_action_retry),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onErrorContainer,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(sp.sm))
                .clickable { onRetry() }
                .padding(horizontal = sp.sm, vertical = sp.xs),
        )
    }
}

@Composable
private fun SavingsTabRow(
    selectedTab: SavingsDashboardTab,
    onTabSelected: (SavingsDashboardTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val groupLabel = stringResource(Res.string.screens_savings_dashboard_tab_group)
    val individualLabel = stringResource(Res.string.screens_savings_dashboard_tab_individual)
    val selectedIndex = if (selectedTab == SavingsDashboardTab.GROUP) 0 else 1

    TabRow(
        selectedTabIndex = selectedIndex,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        modifier = modifier.fillMaxWidth().testTag(SavingsDashboardTestTags.TAB_ROW),
    ) {
        Tab(
            selected = selectedTab == SavingsDashboardTab.GROUP,
            onClick = { onTabSelected(SavingsDashboardTab.GROUP) },
            text = { Text(groupLabel) },
            icon = { Icon(imageVector = Icons.Filled.Groups, contentDescription = null) },
            selectedContentColor = MaterialTheme.colorScheme.primary,
            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(SavingsDashboardTestTags.TAB_GROUP),
        )
        Tab(
            selected = selectedTab == SavingsDashboardTab.INDIVIDUAL,
            onClick = { onTabSelected(SavingsDashboardTab.INDIVIDUAL) },
            text = { Text(individualLabel) },
            icon = { Icon(imageVector = Icons.Filled.Person, contentDescription = null) },
            selectedContentColor = MaterialTheme.colorScheme.primary,
            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(SavingsDashboardTestTags.TAB_INDIVIDUAL),
        )
    }
}

// -- GROUP pane (bar trend + cycle progress + group total chip + per-member share/meeting rows) --

@Composable
private fun GroupSavingsPane(
    state: SavingsDashboardState,
    onAction: (SavingsDashboardAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val memberRows = state.groupSavingsSummary?.memberRows.orEmpty()
    val totalCollected = state.groupSavingsSummary?.totalCollected ?: 0L

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag(SavingsDashboardTestTags.GROUP_MEMBER_LIST),
        contentPadding = PaddingValues(vertical = sp.md),
        verticalArrangement = Arrangement.spacedBy(sp.md),
    ) {
        item(key = "group_trend") {
            WeeklyTrendChart(
                points = state.weeklyTrend,
                title = stringResource(Res.string.screens_savings_dashboard_group_trend_title),
                contentDescription = stringResource(Res.string.screens_savings_dashboard_group_trend_cd),
                isBar = true,
                accentColor = MaterialTheme.colorScheme.primary,
                valueSelector = { it.groupAmount },
                testTag = SavingsDashboardTestTags.WEEKLY_TREND_CHART_GROUP,
                modifier = Modifier.padding(horizontal = sp.lg),
            )
        }
        item(key = "cycle_progress") {
            CycleProgressCard(state = state, modifier = Modifier.padding(horizontal = sp.lg))
        }
        item(key = "group_total") {
            GroupTotalChip(totalCollected = totalCollected, modifier = Modifier.padding(horizontal = sp.lg))
        }
        items(items = memberRows, key = { it.memberId }) { member ->
            GroupMemberRow(
                member = member,
                contributionModel = state.contributionModel,
                onClick = { onAction(SavingsDashboardAction.OpenMemberDetail(member.memberId)) },
            )
        }
    }
}

@Composable
private fun CycleProgressCard(state: SavingsDashboardState, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val fraction = if (state.cycleTarget > 0L) {
        (state.cycleCollected.toFloat() / state.cycleTarget.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val percent = if (state.cycleTarget > 0L) (state.cycleCollected * 100 / state.cycleTarget) else 0L
    val progressText = if (state.contributionModel == SHARE_BASED) {
        stringResource(
            Res.string.screens_savings_dashboard_cycle_progress_shares_format,
            state.cycleCollected.formatGrouped(),
            state.cycleTarget.formatGrouped(),
        )
    } else {
        stringResource(
            Res.string.screens_savings_dashboard_cycle_progress_amount_format,
            state.cycleCollected.formatGrouped(),
            state.cycleTarget.formatGrouped(),
        )
    }
    val percentText = stringResource(Res.string.screens_savings_dashboard_cycle_progress_percent_format, percent.toString())
    val progressCd = stringResource(Res.string.screens_savings_dashboard_cycle_progress_cd, percent.toString())

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(sp.md),
        modifier = modifier.fillMaxWidth().testTag(SavingsDashboardTestTags.CYCLE_PROGRESS_CARD),
    ) {
        Column(modifier = Modifier.padding(sp.lg), verticalArrangement = Arrangement.spacedBy(sp.sm)) {
            Text(
                text = stringResource(Res.string.screens_savings_dashboard_cycle_progress_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            LinearProgressIndicator(
                progress = { fraction },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .semantics { contentDescription = progressCd }
                    .testTag(SavingsDashboardTestTags.CYCLE_PROGRESS_BAR),
            )
            Text(text = progressText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(text = percentText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun GroupTotalChip(totalCollected: Long, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    Text(
        text = stringResource(Res.string.screens_savings_dashboard_group_total_chip_format, totalCollected.formatGrouped()),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = sp.md, vertical = sp.sm)
            .testTag(SavingsDashboardTestTags.GROUP_TOTAL_CHIP),
    )
}

@Composable
private fun GroupMemberRow(
    member: MemberGroupSavingsRow,
    contributionModel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val isShareBased = contributionModel == SHARE_BASED
    val supporting = if (isShareBased) {
        stringResource(
            Res.string.screens_savings_dashboard_member_shares_format,
            member.sharesHeld ?: 0,
            (member.shareValue ?: 0L).formatGrouped(),
        )
    } else {
        stringResource(
            Res.string.screens_savings_dashboard_member_meetings_format,
            member.meetingsContributed,
            member.lastContribution.formatGrouped(),
        )
    }
    val trailingAmount = stringResource(
        Res.string.screens_savings_dashboard_member_amount_format,
        (if (isShareBased) (member.shareValue ?: 0L) else member.totalContributed).formatGrouped(),
    )
    val trailingLabel = if (isShareBased) {
        stringResource(Res.string.screens_savings_dashboard_member_label_share_value)
    } else {
        stringResource(Res.string.screens_savings_dashboard_member_label_total)
    }

    MemberRow(
        name = member.name,
        supporting = supporting,
        trailingAmount = trailingAmount,
        trailingLabel = trailingLabel,
        avatarColor = MaterialTheme.colorScheme.secondaryContainer,
        onAvatarColorContent = MaterialTheme.colorScheme.onSecondaryContainer,
        trailingColor = MaterialTheme.colorScheme.primary,
        onClick = onClick,
        modifier = modifier.testTag(SavingsDashboardTestTags.groupMemberRowTag(member.memberId)),
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = sp.lg))
}

// -- INDIVIDUAL pane (line trend + total balances card + per-member balance rows) ----------------

@Composable
private fun IndividualSavingsPane(
    state: SavingsDashboardState,
    onAction: (SavingsDashboardAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val memberRows = state.individualSavingsSummary?.memberRows.orEmpty()
    val totalBalance = state.individualSavingsSummary?.totalBalance ?: 0L

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag(SavingsDashboardTestTags.INDIVIDUAL_MEMBER_LIST),
        contentPadding = PaddingValues(vertical = sp.md),
        verticalArrangement = Arrangement.spacedBy(sp.md),
    ) {
        item(key = "individual_trend") {
            WeeklyTrendChart(
                points = state.weeklyTrend,
                title = stringResource(Res.string.screens_savings_dashboard_individual_trend_title),
                contentDescription = stringResource(Res.string.screens_savings_dashboard_individual_trend_cd),
                isBar = false,
                accentColor = MaterialTheme.colorScheme.secondary,
                valueSelector = { it.individualAmount },
                testTag = SavingsDashboardTestTags.WEEKLY_TREND_CHART_INDIVIDUAL,
                modifier = Modifier.padding(horizontal = sp.lg),
            )
        }
        item(key = "individual_total") {
            IndividualTotalCard(totalBalance = totalBalance, modifier = Modifier.padding(horizontal = sp.lg))
        }
        items(items = memberRows, key = { it.memberId }) { member ->
            IndividualMemberRow(
                member = member,
                onClick = { onAction(SavingsDashboardAction.OpenMemberDetail(member.memberId)) },
            )
        }
    }
}

@Composable
private fun IndividualTotalCard(totalBalance: Long, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(sp.md),
        modifier = modifier.fillMaxWidth().testTag(SavingsDashboardTestTags.INDIVIDUAL_TOTAL_CARD),
    ) {
        Column(modifier = Modifier.padding(sp.lg), verticalArrangement = Arrangement.spacedBy(sp.xs)) {
            Text(
                text = stringResource(Res.string.screens_savings_dashboard_total_individual_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = stringResource(Res.string.screens_savings_dashboard_individual_total_amount_format, totalBalance.formatGrouped()),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun IndividualMemberRow(
    member: MemberIndividualSavingsRow,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val supporting = member.lastTransaction?.let { last ->
        stringResource(
            Res.string.screens_savings_dashboard_individual_last_txn_format,
            last.formatGrouped(),
            member.lastTransactionDate?.toString().orEmpty(),
        )
    }
    val trailingAmount = stringResource(Res.string.screens_savings_dashboard_individual_balance_format, member.currentBalance.formatGrouped())
    val trailingLabel = stringResource(Res.string.screens_savings_dashboard_individual_balance_label)

    MemberRow(
        name = member.name,
        supporting = supporting,
        trailingAmount = trailingAmount,
        trailingLabel = trailingLabel,
        avatarColor = MaterialTheme.colorScheme.tertiaryContainer,
        onAvatarColorContent = MaterialTheme.colorScheme.onTertiaryContainer,
        trailingColor = MaterialTheme.colorScheme.secondary,
        onClick = onClick,
        modifier = modifier.testTag(SavingsDashboardTestTags.individualMemberRowTag(member.memberId)),
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = sp.lg))
}

// -- Shared leaf composables ---------------------------------------------------------------------

@Composable
private fun MemberRow(
    name: String,
    supporting: String?,
    trailingAmount: String,
    trailingLabel: String,
    avatarColor: Color,
    onAvatarColorContent: Color,
    trailingColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val avatarCd = stringResource(Res.string.screens_savings_dashboard_avatar_cd_format, name)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = sp.lg, vertical = sp.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(sp.md),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(avatarColor)
                .semantics { contentDescription = avatarCd },
            contentAlignment = Alignment.Center,
        ) {
            Text(text = initialsOf(name), style = MaterialTheme.typography.labelLarge, color = onAvatarColorContent)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(sp.xs)) {
            Text(text = name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            if (supporting != null) {
                Text(text = supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = trailingAmount, style = MaterialTheme.typography.labelLarge, color = trailingColor, fontWeight = FontWeight.Bold)
            Text(text = trailingLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * `ui.yaml#components.{weekly_trend_chart_group,weekly_trend_chart_individual}` — a lightweight
 * `Canvas` chart over [points] (bar for the group tab, filled line for the individual tab; no
 * external chart library is wired in this codebase — mirrors `SavingsSparklineCard`'s identical
 * bar-Canvas precedent). Heights are normalized by [BarGeometry.normalizedHeights] (shared pure math,
 * degenerate-safe). Renders an empty-state label when [points] is empty rather than a blank canvas.
 */
@Composable
private fun WeeklyTrendChart(
    points: List<WeeklyContributionPoint>,
    title: String,
    contentDescription: String,
    isBar: Boolean,
    accentColor: Color,
    valueSelector: (WeeklyContributionPoint) -> Long,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val emptyLabel = stringResource(Res.string.screens_savings_dashboard_trend_empty)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier.fillMaxWidth().testTag(testTag), verticalArrangement = Arrangement.spacedBy(sp.sm)) {
        Text(text = title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        if (points.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(CHART_HEIGHT),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = emptyLabel, style = MaterialTheme.typography.bodySmall, color = labelColor)
            }
            return@Column
        }

        val values = points.map { valueSelector(it).toFloat() }
        val normalized = normalizedHeights(values)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(CHART_HEIGHT)
                .semantics { this.contentDescription = contentDescription },
        ) {
            if (isBar) {
                drawTrendBars(normalized = normalized, barColor = accentColor)
            } else {
                drawTrendLine(normalized = normalized, lineColor = accentColor)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            points.forEach { point ->
                Text(text = point.weekLabel, style = MaterialTheme.typography.labelSmall, color = labelColor)
            }
        }
    }
}

/**
 * Per-bar/point height as a fraction `[0f, 1f]` of the canvas height, normalized against the
 * series max. Degenerate-safe (empty → empty; all-zero/negative max → all `0f`) — same pure math as
 * `kpt.core.base.designsystem.chart.BarGeometry.normalizedHeights`, inlined here to keep this feature
 * module's dependency surface identical to `member-savings-detail`'s `SavingsSparklineCard`.
 */
private fun normalizedHeights(values: List<Float>): List<Float> {
    val max = values.maxOrNull() ?: 0f
    return when {
        values.isEmpty() -> emptyList()
        max <= 0f -> List(values.size) { 0f }
        else -> values.map { (it / max).coerceIn(0f, 1f) }
    }
}

private fun DrawScope.drawTrendBars(normalized: List<Float>, barColor: Color) {
    if (normalized.isEmpty()) return
    val gapPx = 8f
    val totalGap = gapPx * (normalized.size - 1).coerceAtLeast(0)
    val barWidth = ((size.width - totalGap) / normalized.size).coerceAtLeast(1f)
    normalized.forEachIndexed { index, fraction ->
        val barHeight = size.height * fraction
        val left = index * (barWidth + gapPx)
        drawRoundRect(
            color = barColor,
            topLeft = Offset(x = left, y = size.height - barHeight),
            size = Size(width = barWidth, height = barHeight),
            cornerRadius = CornerRadius(BAR_CORNER_RADIUS_PX, BAR_CORNER_RADIUS_PX),
        )
    }
}

private fun DrawScope.drawTrendLine(normalized: List<Float>, lineColor: Color) {
    if (normalized.isEmpty()) return
    val stepX = if (normalized.size > 1) size.width / (normalized.size - 1) else size.width
    fun pointX(index: Int): Float = if (normalized.size > 1) index * stepX else size.width / 2f
    fun pointY(fraction: Float): Float = size.height - (size.height * fraction)

    val fill = Path().apply {
        moveTo(pointX(0), size.height)
        normalized.forEachIndexed { index, fraction -> lineTo(pointX(index), pointY(fraction)) }
        lineTo(pointX(normalized.lastIndex), size.height)
        close()
    }
    drawPath(path = fill, color = lineColor.copy(alpha = 0.12f))

    val line = Path().apply {
        normalized.forEachIndexed { index, fraction ->
            if (index == 0) moveTo(pointX(index), pointY(fraction)) else lineTo(pointX(index), pointY(fraction))
        }
    }
    drawPath(path = line, color = lineColor, style = Stroke(width = 4f))
    normalized.forEachIndexed { index, fraction ->
        drawCircle(color = lineColor, radius = 6f, center = Offset(pointX(index), pointY(fraction)))
    }
}

@Composable
private fun HorizontalDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

// -- Empty (ui.yaml#states.empty: "No savings data") ---------------------------------------------

@Composable
private fun SavingsDashboardEmptySection(modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val iconCd = stringResource(Res.string.screens_savings_dashboard_empty_icon_cd)
    Column(
        modifier = modifier.fillMaxSize().padding(sp.lg).testTag(SavingsDashboardTestTags.EMPTY_SECTION),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Savings,
            contentDescription = iconCd,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(80.dp).padding(bottom = sp.lg),
        )
        Text(
            text = stringResource(Res.string.screens_savings_dashboard_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(Res.string.screens_savings_dashboard_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = sp.sm),
        )
    }
}

// -- Error (ui.yaml#states.error: "No cached data — full error") ---------------------------------

@Composable
private fun SavingsDashboardErrorSection(
    state: SavingsDashboardState,
    onAction: (SavingsDashboardAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val errorIconCd = stringResource(Res.string.screens_savings_dashboard_error_icon_cd)
    val message = when (state.error) {
        "error_group_not_found" -> stringResource(Res.string.screens_savings_dashboard_error_group_not_found)
        "error_auth" -> stringResource(Res.string.screens_savings_dashboard_error_auth)
        "error_offline" -> stringResource(Res.string.screens_savings_dashboard_error_offline)
        else -> stringResource(Res.string.screens_savings_dashboard_error_server)
    }

    Column(
        modifier = modifier.fillMaxSize().padding(sp.lg).testTag(SavingsDashboardTestTags.ERROR_SECTION),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.CloudOff,
            contentDescription = errorIconCd,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp),
        )
        Text(
            text = stringResource(Res.string.screens_savings_dashboard_error_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = sp.lg),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = sp.sm),
        )
        Button(
            onClick = { onAction(SavingsDashboardAction.RefreshDashboard) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = sp.touchTargetMin)
                .padding(top = sp.lg)
                .testTag(SavingsDashboardTestTags.ERROR_RETRY_BUTTON),
        ) {
            Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
            Text(
                text = stringResource(Res.string.screens_savings_dashboard_action_retry),
                modifier = Modifier.padding(start = sp.xs),
            )
        }
    }
}

private fun initialsOf(name: String): String = name
    .trim()
    .split(" ")
    .filter { it.isNotBlank() }
    .take(2)
    .joinToString("") { it.first().uppercase() }
    .ifBlank { "?" }
