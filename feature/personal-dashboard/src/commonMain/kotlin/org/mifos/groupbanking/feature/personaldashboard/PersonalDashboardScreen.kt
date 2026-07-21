/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.personaldashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.GroupOff
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kpt.core.base.ui.effects.EventsEffect
import kpt.core.designsystem.theme.spacing
import kpt.core.ui.scaffold.KptScaffold
import kpt.core.ui.scaffold.rememberKptPullToRefreshState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.mifos.groupbanking.core.model.GroupSummary
import org.mifos.groupbanking.feature.personaldashboard.components.GroupSelectorChipRow
import org.mifos.groupbanking.feature.personaldashboard.components.RecentActivityRow
import org.mifos.groupbanking.feature.personaldashboard.components.SavingsSummaryCard
import org.mifos.groupbanking.feature.personaldashboard.components.ShareoutProjectionCard
import org.mifos.groupbanking.feature.personaldashboard.generated.resources.Res
import org.mifos.groupbanking.feature.personaldashboard.generated.resources.screens_personal_dashboard_currency_kes
import org.mifos.groupbanking.feature.personaldashboard.generated.resources.screens_personal_dashboard_empty_body
import org.mifos.groupbanking.feature.personaldashboard.generated.resources.screens_personal_dashboard_empty_icon_cd
import org.mifos.groupbanking.feature.personaldashboard.generated.resources.screens_personal_dashboard_empty_title
import org.mifos.groupbanking.feature.personaldashboard.generated.resources.screens_personal_dashboard_error_icon_cd
import org.mifos.groupbanking.feature.personaldashboard.generated.resources.screens_personal_dashboard_error_subtitle
import org.mifos.groupbanking.feature.personaldashboard.generated.resources.screens_personal_dashboard_error_title
import org.mifos.groupbanking.feature.personaldashboard.generated.resources.screens_personal_dashboard_greeting_afternoon
import org.mifos.groupbanking.feature.personaldashboard.generated.resources.screens_personal_dashboard_greeting_evening
import org.mifos.groupbanking.feature.personaldashboard.generated.resources.screens_personal_dashboard_greeting_morning
import org.mifos.groupbanking.feature.personaldashboard.generated.resources.screens_personal_dashboard_no_group_label
import org.mifos.groupbanking.feature.personaldashboard.generated.resources.screens_personal_dashboard_notification_icon_cd
import org.mifos.groupbanking.feature.personaldashboard.generated.resources.screens_personal_dashboard_recent_activity_title
import org.mifos.groupbanking.feature.personaldashboard.generated.resources.screens_personal_dashboard_retry_action

/**
 * Container for `personal-dashboard-screen`. Collects [PersonalDashboardViewModel] state via
 * [collectAsStateWithLifecycle], consumes one-shot [PersonalDashboardEvent]s (navigate to
 * personal-savings / group-list) through [EventsEffect], and delegates all rendering to the
 * stateless [PersonalDashboardContent]. [onNavigateToSavings] carries `groupId` + `poolModel` so
 * the target screen can render the correct pool-model view; [onNavigateToGroupList] closes
 * [PersonalDashboardEvent.NavigateToGroupList] — currently unreachable from any wired `on_click`
 * on this screen's canvas (see [PersonalDashboardViewModel] class KDoc "Idea-layer gap" note) but
 * still exposed here so the host wiring compiles and the sealed-interface `when` in [EventsEffect]
 * stays exhaustive (RULE-PROTO-COMPOSE-DEAD-CLICK-001 DC3). See API.md#screen.
 */
@Composable
internal fun PersonalDashboardScreen(
    onNavigateToSavings: (groupId: String, poolModel: String) -> Unit,
    onNavigateToGroupList: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PersonalDashboardViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    EventsEffect(viewModel) { event ->
        when (event) {
            is PersonalDashboardEvent.NavigateToSavings -> onNavigateToSavings(event.groupId, event.poolModel)
            PersonalDashboardEvent.NavigateToGroupList -> onNavigateToGroupList()
        }
    }

    PersonalDashboardContent(
        state = state,
        onAction = viewModel::trySendAction,
        modifier = modifier,
    )
}

/**
 * Stateless render surface for `personal-dashboard-screen`. State-driven per
 * [PersonalDashboardState.screenState] — every [PersonalDashboardScreenState] member is handled
 * (Loading/Content/Error/Empty). Pull-to-refresh is wired at the [KptScaffold] level across every
 * screenState (mirrors `GroupListContent`'s identical convention) — [PersonalDashboardAction
 * .OnRefresh] is always reachable regardless of which section is currently rendered. No back
 * navigation affordance — `PersonalDashboardAction` declares no `OnBack` member; this is a
 * root/tab-level screen (`docs.yaml#bottom_nav: true`). See API.md#screen.
 */
@OptIn(ExperimentalTime::class)
@Composable
internal fun PersonalDashboardContent(
    state: PersonalDashboardState,
    onAction: (PersonalDashboardAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hour = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour }
    val greetingKey = when {
        hour < 12 -> Res.string.screens_personal_dashboard_greeting_morning
        hour < 17 -> Res.string.screens_personal_dashboard_greeting_afternoon
        else -> Res.string.screens_personal_dashboard_greeting_evening
    }
    val greeting = stringResource(greetingKey, state.memberName)

    KptScaffold(
        showNavigationIcon = false,
        pullToRefreshState = rememberKptPullToRefreshState(
            isEnabled = true,
            isRefreshing = state.isRefreshing,
            onRefresh = { onAction(PersonalDashboardAction.OnRefresh) },
        ),
        modifier = modifier.testTag(PersonalDashboardTestTags.SCREEN),
    ) {
        when (state.screenState) {
            PersonalDashboardScreenState.Loading -> PersonalDashboardLoadingSection(greeting = greeting)

            PersonalDashboardScreenState.Content -> PersonalDashboardContentSection(
                state = state,
                greeting = greeting,
                onAction = onAction,
            )

            PersonalDashboardScreenState.Empty -> PersonalDashboardEmptySection(greeting = greeting)

            PersonalDashboardScreenState.Error -> PersonalDashboardErrorSection(
                greeting = greeting,
                groupName = state.selectedGroup?.name.orEmpty(),
                onRetry = { onAction(PersonalDashboardAction.OnRetry) },
            )
        }
    }
}

/**
 * Shared primary-colored header — `ui.yaml#components.top_bar` + `group_banner`. Renders the
 * time-of-day [greeting], a decorative (non-interactive — ui.yaml declares no `on_click`)
 * notifications bell, the active [groupName] + currency chip, and — only when [showChips] is
 * true (`myGroups.size > 1`) — the [GroupSelectorChipRow]. See API.md#screen.
 */
@Composable
internal fun PersonalDashboardTopSection(
    greeting: String,
    groupName: String,
    showChips: Boolean,
    groups: List<GroupSummary>,
    selectedGroupId: String?,
    onGroupSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val notificationCd = stringResource(Res.string.screens_personal_dashboard_notification_icon_cd)
    val currencyLabel = stringResource(Res.string.screens_personal_dashboard_currency_kes)

    Surface(color = MaterialTheme.colorScheme.primary, modifier = modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(sp.lg),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Filled.NotificationsNone,
                    contentDescription = notificationCd,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag(PersonalDashboardTestTags.NOTIFICATION_ICON),
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = sp.lg, vertical = sp.sm)
                    .testTag(PersonalDashboardTestTags.GROUP_BANNER),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(sp.sm),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = groupName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(50),
                    ) {
                        Text(
                            text = currencyLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = sp.sm, vertical = sp.xs),
                        )
                    }
                }
                if (showChips) {
                    GroupSelectorChipRow(
                        groups = groups,
                        selectedGroupId = selectedGroupId,
                        onGroupSelected = onGroupSelected,
                        modifier = Modifier.padding(top = sp.sm),
                    )
                }
            }
        }
    }
}

/**
 * `PersonalDashboardScreenState.Loading` — top section (group name blank on cold start, per
 * [PersonalDashboardState]'s declared `""`/`null` defaults) + 4 shimmering skeleton blocks,
 * mirroring `preview/loading.html`'s `shimmer_loading` (count: 4). See API.md#screen.
 */
@Composable
internal fun PersonalDashboardLoadingSection(greeting: String, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    Column(modifier = modifier.fillMaxSize().testTag(PersonalDashboardTestTags.LOADING_SECTION)) {
        PersonalDashboardTopSection(
            greeting = greeting,
            groupName = "",
            showChips = false,
            groups = emptyList(),
            selectedGroupId = null,
            onGroupSelected = {},
        )
        Column(
            modifier = Modifier.fillMaxWidth().padding(sp.lg),
            verticalArrangement = Arrangement.spacedBy(sp.md),
        ) {
            repeat(4) { PersonalDashboardSkeletonBlock() }
        }
    }
}

/** One shimmering placeholder block used by [PersonalDashboardLoadingSection]. */
@Composable
internal fun PersonalDashboardSkeletonBlock(modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(sp.sm))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

/**
 * `PersonalDashboardScreenState.Content` — top section (with group-selector chips when
 * `myGroups.size > 1`) + overlapping [SavingsSummaryCard] + [ShareoutProjectionCard] + recent
 * activity header + rows, mirroring `preview/content.html`. See API.md#screen.
 */
@Composable
internal fun PersonalDashboardContentSection(
    state: PersonalDashboardState,
    greeting: String,
    onAction: (PersonalDashboardAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val recentActivityTitle = stringResource(Res.string.screens_personal_dashboard_recent_activity_title)

    LazyColumn(modifier = modifier.fillMaxSize().testTag(PersonalDashboardTestTags.RECENT_ACTIVITY_LIST)) {
        item {
            PersonalDashboardTopSection(
                greeting = greeting,
                groupName = state.selectedGroup?.name.orEmpty(),
                showChips = state.myGroups.size > 1,
                groups = state.myGroups,
                selectedGroupId = state.selectedGroup?.groupId,
                onGroupSelected = { onAction(PersonalDashboardAction.OnSelectGroup(it)) },
            )
        }
        item {
            Box(modifier = Modifier.fillMaxWidth().offset(y = -sp.xl).padding(horizontal = sp.lg)) {
                SavingsSummaryCard(
                    groupLinkedBalance = state.groupLinkedSavingsBalance,
                    individualBalance = state.individualSavingsBalance,
                    onClick = { onAction(PersonalDashboardAction.OnSavingsCardClick) },
                )
            }
        }
        item {
            Box(modifier = Modifier.fillMaxWidth().offset(y = -sp.md).padding(horizontal = sp.lg)) {
                ShareoutProjectionCard(
                    poolModel = state.poolModel,
                    shareOutProjection = state.shareOutProjection,
                    rotationPosition = state.rotationPosition,
                    nextRecipientEta = state.nextRecipientEta,
                )
            }
        }
        item {
            Text(
                text = recentActivityTitle,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .padding(horizontal = sp.lg, vertical = sp.md)
                    .testTag(PersonalDashboardTestTags.RECENT_ACTIVITY_HEADER),
            )
        }
        items(items = state.recentTransactions, key = { it.id }) { transaction ->
            RecentActivityRow(
                transaction = transaction,
                modifier = Modifier.padding(horizontal = sp.lg, vertical = sp.xs),
            )
        }
        item { Box(modifier = Modifier.height(sp.xl)) }
    }
}

/**
 * `PersonalDashboardScreenState.Empty` — genuinely zero groups (`myGroups.isEmpty()`, derived by
 * [PersonalDashboardState.screenState]). Illustration + title + body, mirroring
 * `preview/empty.html`. **Idea-layer gap (flagged, not invented here):** `preview/empty.html`
 * shows "Browse Groups" / "Create a Group" CTA buttons, but `PersonalDashboardAction` declares no
 * member to dispatch them and ui.yaml's `states.empty.components` lists only `[top_bar,
 * group_banner]` — no CTA component. Per RULE-IMPL-DEAD-CLICKABLE-001 Rule 1 this screen does NOT
 * invent an unlisted action; pull-to-refresh ([PersonalDashboardAction.OnRefresh], wired at the
 * [KptScaffold] level) remains the one real affordance available here. Reported to the caller for
 * an idea-layer `ui.yaml#components`/`PersonalDashboardAction` update (e.g. `OnBrowseGroups` /
 * `OnCreateGroup` members + matching events) if the CTA buttons are actually wanted — mirrors
 * [PersonalDashboardViewModel]'s own documented `NavigateToGroupList` gap. See API.md#screen.
 */
@Composable
internal fun PersonalDashboardEmptySection(greeting: String, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val noGroupLabel = stringResource(Res.string.screens_personal_dashboard_no_group_label)
    val iconCd = stringResource(Res.string.screens_personal_dashboard_empty_icon_cd)
    val title = stringResource(Res.string.screens_personal_dashboard_empty_title)
    val body = stringResource(Res.string.screens_personal_dashboard_empty_body)

    Column(modifier = modifier.fillMaxSize()) {
        PersonalDashboardTopSection(
            greeting = greeting,
            groupName = noGroupLabel,
            showChips = false,
            groups = emptyList(),
            selectedGroupId = null,
            onGroupSelected = {},
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(sp.xl)
                .testTag(PersonalDashboardTestTags.EMPTY_SECTION),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.GroupOff,
                contentDescription = iconCd,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(80.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = sp.lg),
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = sp.sm),
            )
        }
    }
}

/**
 * `PersonalDashboardScreenState.Error` — full-screen error surface (wifi_off icon, title, body,
 * Retry CTA dispatching [PersonalDashboardAction.OnRetry] via [onRetry]), mirroring
 * `preview/error.html`. See API.md#screen.
 */
@Composable
internal fun PersonalDashboardErrorSection(
    greeting: String,
    groupName: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val iconCd = stringResource(Res.string.screens_personal_dashboard_error_icon_cd)
    val title = stringResource(Res.string.screens_personal_dashboard_error_title)
    val subtitle = stringResource(Res.string.screens_personal_dashboard_error_subtitle)
    val retryLabel = stringResource(Res.string.screens_personal_dashboard_retry_action)

    Column(modifier = modifier.fillMaxSize()) {
        PersonalDashboardTopSection(
            greeting = greeting,
            groupName = groupName,
            showChips = false,
            groups = emptyList(),
            selectedGroupId = null,
            onGroupSelected = {},
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(sp.xl)
                .testTag(PersonalDashboardTestTags.ERROR_SECTION),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.CloudOff,
                contentDescription = iconCd,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = sp.lg),
            )
            Text(
                text = subtitle,
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
                    .testTag(PersonalDashboardTestTags.ERROR_RETRY_BUTTON),
            ) {
                Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
                Text(text = retryLabel, modifier = Modifier.padding(start = sp.xs))
            }
        }
    }
}
