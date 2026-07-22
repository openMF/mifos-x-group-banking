/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.groupdashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kpt.core.base.designsystem.core.TopAppBarAction
import kpt.core.base.ui.effects.EventsEffect
import kpt.core.designsystem.theme.spacing
import kpt.core.ui.scaffold.KptScaffold
import kpt.core.ui.scaffold.rememberKptPullToRefreshState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.mifos.groupbanking.core.model.SavingsMechanism
import org.mifos.groupbanking.feature.groupdashboard.components.ActivityFeedSection
import org.mifos.groupbanking.feature.groupdashboard.components.CorpusBlockedDialog
import org.mifos.groupbanking.feature.groupdashboard.components.CorpusMetricCard
import org.mifos.groupbanking.feature.groupdashboard.components.GroupHeaderCard
import org.mifos.groupbanking.feature.groupdashboard.components.GroupSavingsSummaryCard
import org.mifos.groupbanking.feature.groupdashboard.components.QuickActionsSection
import org.mifos.groupbanking.feature.groupdashboard.components.RotationMetricCard
import org.mifos.groupbanking.feature.groupdashboard.generated.resources.Res
import org.mifos.groupbanking.feature.groupdashboard.generated.resources.screens_group_dashboard_btn_retry
import org.mifos.groupbanking.feature.groupdashboard.generated.resources.screens_group_dashboard_error_auth
import org.mifos.groupbanking.feature.groupdashboard.generated.resources.screens_group_dashboard_error_icon_cd
import org.mifos.groupbanking.feature.groupdashboard.generated.resources.screens_group_dashboard_error_network
import org.mifos.groupbanking.feature.groupdashboard.generated.resources.screens_group_dashboard_error_not_found
import org.mifos.groupbanking.feature.groupdashboard.generated.resources.screens_group_dashboard_error_server
import org.mifos.groupbanking.feature.groupdashboard.generated.resources.screens_group_dashboard_error_state_title
import org.mifos.groupbanking.feature.groupdashboard.generated.resources.screens_group_dashboard_more_options_cd
import org.mifos.groupbanking.feature.groupdashboard.generated.resources.screens_group_dashboard_share_out_not_available

/**
 * Container for `group-dashboard-screen`. Collects [GroupDashboardViewModel] state via
 * [collectAsStateWithLifecycle], consumes one-shot [GroupDashboardEvent]s (navigate to
 * meeting-calendar / member-list / loan-list / share-out-preview / member-savings-detail / back,
 * show the corpus-blocked dialog, show a snackbar) through [EventsEffect], and delegates all
 * rendering to the stateless [GroupDashboardContent]. [groupId] and [viewerRole] are the
 * `ui.yaml#nav_params` forwarded from `group-list` / `group-create` — supplied to
 * [GroupDashboardViewModel] via Koin `parametersOf(groupId, viewerRole)` (`groupId` FIRST,
 * `viewerRole` SECOND, matching `GroupDashboardModule`'s declaration order). See API.md#screen.
 */
@Composable
internal fun GroupDashboardScreen(
    groupId: String,
    viewerRole: String,
    onNavigateToMeetingCalendar: (groupId: String) -> Unit,
    onNavigateToMemberList: (groupId: String) -> Unit,
    onNavigateToLoanList: (groupId: String) -> Unit,
    onNavigateToShareOut: (groupId: String, distributionStrategy: String) -> Unit,
    onNavigateToMemberSavingsDetail: (groupId: String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GroupDashboardViewModel = koinViewModel(parameters = { parametersOf(groupId, viewerRole) }),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCorpusBlockedDialog by remember { mutableStateOf(false) }

    // Precomputed OUTSIDE EventsEffect — stringResource() is @Composable-only and EventsEffect's
    // callback runs in a suspend (non-composable) scope, same convention as GroupCreateScreen.kt.
    val networkMessage = stringResource(Res.string.screens_group_dashboard_error_network)
    val serverMessage = stringResource(Res.string.screens_group_dashboard_error_server)
    val notFoundMessage = stringResource(Res.string.screens_group_dashboard_error_not_found)
    val authMessage = stringResource(Res.string.screens_group_dashboard_error_auth)
    val shareOutNotAvailableMessage = stringResource(Res.string.screens_group_dashboard_share_out_not_available)

    EventsEffect(viewModel) { event ->
        when (event) {
            is GroupDashboardEvent.NavigateToMeetingCalendar -> onNavigateToMeetingCalendar(event.groupId)
            is GroupDashboardEvent.NavigateToMemberList -> onNavigateToMemberList(event.groupId)
            is GroupDashboardEvent.NavigateToLoanList -> onNavigateToLoanList(event.groupId)
            is GroupDashboardEvent.NavigateToShareOut -> onNavigateToShareOut(event.groupId, event.distributionStrategy)
            is GroupDashboardEvent.NavigateToMemberSavingsDetail -> onNavigateToMemberSavingsDetail(event.groupId)
            GroupDashboardEvent.NavigateBack -> onNavigateBack()
            GroupDashboardEvent.ShowCorpusBlockedDialog -> showCorpusBlockedDialog = true
            is GroupDashboardEvent.ShowSnackbar -> snackbarHostState.showSnackbar(
                message = when (event.message) {
                    "error_network" -> networkMessage
                    "error_server" -> serverMessage
                    "error_not_found" -> notFoundMessage
                    "error_auth" -> authMessage
                    "share_out_not_available" -> shareOutNotAvailableMessage
                    else -> event.message
                },
            )
        }
    }

    GroupDashboardContent(
        state = state,
        onAction = viewModel::trySendAction,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )

    if (showCorpusBlockedDialog) {
        CorpusBlockedDialog(onDismiss = { showCorpusBlockedDialog = false })
    }
}

/**
 * Stateless render surface for `group-dashboard-screen`. State-driven per
 * [GroupDashboardState.screenState] — every [GroupDashboardScreenState] member is handled
 * (Loading/Content/Error — this composite screen has no `Empty` variant, see
 * [GroupDashboardScreenState] KDoc). Pull-to-refresh is wired at the [KptScaffold] level across
 * every screenState, dispatching [GroupDashboardAction.OnRefresh]. The top-bar overflow icon
 * ([GroupDashboardTestTags.MORE_OPTIONS_BUTTON]) toggles a local (pure Compose UI state, no
 * ViewModel action — see [GroupDashboardAction] class KDoc "Idea-layer gap") [DropdownMenu]; no
 * items are declared anywhere in `ui.yaml` for `group-dashboard-more-menu`, so none are invented
 * here (RULE-IMPL-DEAD-CLICKABLE-001 Rule 1). See API.md#screen.
 */
@Composable
internal fun GroupDashboardContent(
    state: GroupDashboardState,
    onAction: (GroupDashboardAction) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var showMoreMenu by remember { mutableStateOf(false) }
    val moreOptionsCd = stringResource(Res.string.screens_group_dashboard_more_options_cd)
    val title = state.group?.name.orEmpty()

    KptScaffold(
        onNavigationIconClick = { onAction(GroupDashboardAction.OnBack) },
        title = title,
        actions = listOf(
            TopAppBarAction(
                icon = Icons.Filled.MoreVert,
                contentDescription = moreOptionsCd,
                onClick = { showMoreMenu = true },
            ),
        ),
        pullToRefreshState = rememberKptPullToRefreshState(
            isEnabled = true,
            // GroupDashboardState carries no separate `isRefreshing` flag — OnRefresh sets
            // `isLoading = true`, which swaps the whole screen to the Loading skeleton (matches
            // GroupDashboardViewModel.handleRefresh); no distinct pull-spinner overlay signal
            // exists to bind here (flagged, not invented).
            isRefreshing = false,
            onRefresh = { onAction(GroupDashboardAction.OnRefresh) },
        ),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.testTag(GroupDashboardTestTags.SCREEN),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (state.screenState) {
                GroupDashboardScreenState.Loading -> GroupDashboardLoadingSection()
                GroupDashboardScreenState.Content -> GroupDashboardContentSection(state = state, onAction = onAction)
                GroupDashboardScreenState.Error -> GroupDashboardErrorSection(state = state, onAction = onAction)
            }

            DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                // Intentionally empty — see class KDoc.
            }
        }
    }
}

/**
 * `GroupDashboardScreenState.Loading` — 4 shimmer blocks mirroring `preview/loading.html`'s
 * `shimmer_dashboard` (header / metric-hero / actions-grid / savings-activity block heights).
 * See API.md#screen.
 */
@Composable
internal fun GroupDashboardLoadingSection(modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    Column(
        modifier = modifier.fillMaxSize().padding(sp.lg).testTag(GroupDashboardTestTags.LOADING_SECTION),
        verticalArrangement = Arrangement.spacedBy(sp.md),
    ) {
        GroupDashboardSkeletonBlock(height = 84.dp)
        GroupDashboardSkeletonBlock(height = 128.dp)
        GroupDashboardSkeletonBlock(height = 132.dp)
        GroupDashboardSkeletonBlock(height = 120.dp)
    }
}

/** One shimmering placeholder block used by [GroupDashboardLoadingSection] — purely decorative. */
@Composable
internal fun GroupDashboardSkeletonBlock(height: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(MaterialTheme.spacing.sm))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

/**
 * `GroupDashboardScreenState.Content` — header + pool-model-adaptive metric card
 * ([CorpusMetricCard] for ACCUMULATING/NONE, [RotationMetricCard] for ROTATING_PAYOUT) +
 * role-gated [QuickActionsSection] + [GroupSavingsSummaryCard] + [ActivityFeedSection], mirroring
 * `preview/content_accumulating.html` / `preview/content_rotating.html`. Guards on `state.group`
 * being non-null — `handleStreamUpdated`'s `ScreenState.Content` branch always populates it
 * together with `isLoading = false`, so `screenState == Content` implies it is set; this is a
 * defensive no-render rather than a crash if that invariant is ever violated. See API.md#screen.
 */
@Composable
internal fun GroupDashboardContentSection(
    state: GroupDashboardState,
    onAction: (GroupDashboardAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val group = state.group ?: return
    val corpus = state.corpus ?: return
    val sp = MaterialTheme.spacing

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag(GroupDashboardTestTags.CONTENT_LIST),
        verticalArrangement = Arrangement.spacedBy(sp.md),
        contentPadding = PaddingValues(sp.lg),
    ) {
        item {
            GroupHeaderCard(group = group, groupTypeName = state.groupTypeName, viewerRole = state.viewerRole)
        }
        item {
            if (state.typeConfig?.poolModel == SavingsMechanism.ROTATING_PAYOUT) {
                RotationMetricCard(
                    corpus = corpus,
                    rotationPosition = state.rotationPosition,
                    nextRecipientName = state.nextRecipientName,
                    nextRecipientPosition = state.nextRecipientPosition,
                )
            } else {
                CorpusMetricCard(
                    corpus = corpus,
                    shareOutProjection = state.shareOutProjection,
                    isCorpusInsufficient = state.isCorpusInsufficient,
                )
            }
        }
        item {
            QuickActionsSection(viewerRole = state.viewerRole, isCycleEnd = state.isCycleEnd, onAction = onAction)
        }
        item {
            GroupSavingsSummaryCard(
                contributionModel = state.typeConfig?.contributionModel,
                config = state.config,
                totalSavings = state.accounts?.savingsBalance ?: 0.0,
            )
        }
        item {
            ActivityFeedSection(activities = state.recentActivity)
        }
        item { Box(modifier = Modifier.height(sp.xl)) }
    }
}

/**
 * `GroupDashboardScreenState.Error` — full-screen error surface (cloud_off icon, title, resolved
 * [GroupDashboardError.messageKey], Retry CTA shown only when the mapped error is retryable),
 * mirroring `preview/error.html`. See API.md#screen.
 */
@Composable
internal fun GroupDashboardErrorSection(
    state: GroupDashboardState,
    onAction: (GroupDashboardAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val errorIconCd = stringResource(Res.string.screens_group_dashboard_error_icon_cd)
    val titleText = stringResource(Res.string.screens_group_dashboard_error_state_title)
    val retryLabel = stringResource(Res.string.screens_group_dashboard_btn_retry)
    val message = when (state.error) {
        GroupDashboardError.Network, null -> stringResource(Res.string.screens_group_dashboard_error_network)
        GroupDashboardError.Server -> stringResource(Res.string.screens_group_dashboard_error_server)
        GroupDashboardError.NotFound -> stringResource(Res.string.screens_group_dashboard_error_not_found)
        GroupDashboardError.Auth -> stringResource(Res.string.screens_group_dashboard_error_auth)
    }
    val canRetry = state.error?.retry ?: true

    Column(
        modifier = modifier.fillMaxSize().padding(sp.lg).testTag(GroupDashboardTestTags.ERROR_SECTION),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.CloudOff,
            contentDescription = errorIconCd,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp),
        )
        Text(text = titleText, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = sp.lg))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = sp.sm),
        )
        if (canRetry) {
            Button(
                onClick = { onAction(GroupDashboardAction.Retry) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = sp.touchTargetMin)
                    .padding(top = sp.lg)
                    .testTag(GroupDashboardTestTags.ERROR_RETRY_BUTTON),
            ) {
                Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
                Text(text = retryLabel, modifier = Modifier.padding(start = sp.xs))
            }
        }
    }
}
