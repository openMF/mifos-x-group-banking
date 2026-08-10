/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.personaldashboard

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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kpt.core.base.ui.effects.EventsEffect
import kpt.core.designsystem.theme.spacing
import kpt.core.model.GroupSummary
import kpt.core.ui.scaffold.KptScaffold
import kpt.core.ui.scaffold.rememberKptPullToRefreshState
import kpt.feature.personaldashboard.components.GroupSelectorChipRow
import kpt.feature.personaldashboard.components.LoanSummaryCard
import kpt.feature.personaldashboard.components.RecentActivityRow
import kpt.feature.personaldashboard.components.SavingsSummaryCard
import kpt.feature.personaldashboard.components.ShareoutProjectionCard
import kpt.feature.personaldashboard.generated.resources.Res
import kpt.feature.personaldashboard.generated.resources.screens_personal_dashboard_currency_kes
import kpt.feature.personaldashboard.generated.resources.screens_personal_dashboard_empty_body
import kpt.feature.personaldashboard.generated.resources.screens_personal_dashboard_empty_icon_cd
import kpt.feature.personaldashboard.generated.resources.screens_personal_dashboard_empty_title
import kpt.feature.personaldashboard.generated.resources.screens_personal_dashboard_error_icon_cd
import kpt.feature.personaldashboard.generated.resources.screens_personal_dashboard_error_subtitle
import kpt.feature.personaldashboard.generated.resources.screens_personal_dashboard_error_title
import kpt.feature.personaldashboard.generated.resources.screens_personal_dashboard_greeting_afternoon
import kpt.feature.personaldashboard.generated.resources.screens_personal_dashboard_greeting_evening
import kpt.feature.personaldashboard.generated.resources.screens_personal_dashboard_greeting_morning
import kpt.feature.personaldashboard.generated.resources.screens_personal_dashboard_menu_settings
import kpt.feature.personaldashboard.generated.resources.screens_personal_dashboard_menu_sync_status
import kpt.feature.personaldashboard.generated.resources.screens_personal_dashboard_no_group_label
import kpt.feature.personaldashboard.generated.resources.screens_personal_dashboard_notification_icon_cd
import kpt.feature.personaldashboard.generated.resources.screens_personal_dashboard_notifications_deferred
import kpt.feature.personaldashboard.generated.resources.screens_personal_dashboard_overflow_menu_cd
import kpt.feature.personaldashboard.generated.resources.screens_personal_dashboard_recent_activity_title
import kpt.feature.personaldashboard.generated.resources.screens_personal_dashboard_retry_action
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Container for `personal-dashboard-screen`. Collects [PersonalDashboardViewModel] state via
 * [collectAsStateWithLifecycle], consumes one-shot [PersonalDashboardEvent]s (navigate to
 * personal-savings / group-list) through [EventsEffect], and delegates all rendering to the
 * stateless [PersonalDashboardContent]. [onNavigateToSavings] carries the personal-savings
 * nav_params (`clientId`, `groupLinkedSavingsId`, optional `individualSavingsId`) + `poolModel` so
 * the target screen can load the member's savings ledgers and render the correct pool-model view;
 * [onNavigateToGroupList] closes
 * [PersonalDashboardEvent.NavigateToGroupList] — currently unreachable from any wired `on_click`
 * on this screen's canvas (see [PersonalDashboardViewModel] class KDoc "Idea-layer gap" note) but
 * still exposed here so the host wiring compiles and the sealed-interface `when` in [EventsEffect]
 * stays exhaustive (RULE-PROTO-COMPOSE-DEAD-CLICK-001 DC3). See API.md#screen.
 */
@Composable
internal fun PersonalDashboardScreen(
    onNavigateToSavings: (clientId: Long, groupLinkedSavingsId: Long, individualSavingsId: Long?, poolModel: String) -> Unit,
    onNavigateToGroupList: () -> Unit,
    onNavigateToLoans: (clientId: Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSyncStatus: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PersonalDashboardViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    // Precomputed OUTSIDE EventsEffect — stringResource() is @Composable-only and the EventsEffect
    // callback runs in a suspend (non-composable) scope (same convention as SavingsDashboardScreen).
    val notificationsDeferredMessage = stringResource(Res.string.screens_personal_dashboard_notifications_deferred)

    EventsEffect(viewModel) { event ->
        when (event) {
            is PersonalDashboardEvent.NavigateToSavings -> onNavigateToSavings(
                event.clientId,
                event.groupLinkedSavingsId,
                event.individualSavingsId,
                event.poolModel,
            )
            PersonalDashboardEvent.NavigateToGroupList -> onNavigateToGroupList()
            is PersonalDashboardEvent.NavigateToLoans -> onNavigateToLoans(event.clientId)
            PersonalDashboardEvent.NavigateToSettings -> onNavigateToSettings()
            PersonalDashboardEvent.NavigateToSyncStatus -> onNavigateToSyncStatus()
            // G14 — deferred notifications centre: no navigation, just a snackbar.
            PersonalDashboardEvent.NotificationsDeferred -> snackbarHostState.showSnackbar(notificationsDeferredMessage)
        }
    }

    PersonalDashboardContent(
        state = state,
        onAction = viewModel::trySendAction,
        snackbarHostState = snackbarHostState,
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
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val hour = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour }
    val greetingKey = when {
        hour < 12 -> Res.string.screens_personal_dashboard_greeting_morning
        hour < 17 -> Res.string.screens_personal_dashboard_greeting_afternoon
        else -> Res.string.screens_personal_dashboard_greeting_evening
    }
    val greeting = stringResource(greetingKey, state.memberName)

    // Profile / overflow menu callbacks (`ui.yaml#components.top_bar.overflow_menu`) — reachable
    // from every screenState's top section so Settings / Sync Status are always one tap away.
    val onOpenSettings = { onAction(PersonalDashboardAction.OnSettingsClick) }
    val onOpenSyncStatus = { onAction(PersonalDashboardAction.OnSyncStatusClick) }
    // G14 — notification bell tap → deferred-notifications snackbar (reachable from every state).
    val onOpenNotifications = { onAction(PersonalDashboardAction.OnOpenNotifications) }

    KptScaffold(
        showNavigationIcon = false,
        pullToRefreshState = rememberKptPullToRefreshState(
            isEnabled = true,
            isRefreshing = state.isRefreshing,
            onRefresh = { onAction(PersonalDashboardAction.OnRefresh) },
        ),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.testTag(PersonalDashboardTestTags.SCREEN),
    ) {
        when (state.screenState) {
            PersonalDashboardScreenState.Loading -> PersonalDashboardLoadingSection(
                greeting = greeting,
                onOpenSettings = onOpenSettings,
                onOpenSyncStatus = onOpenSyncStatus,
                onOpenNotifications = onOpenNotifications,
            )

            PersonalDashboardScreenState.Content -> PersonalDashboardContentSection(
                state = state,
                greeting = greeting,
                onAction = onAction,
                onOpenSettings = onOpenSettings,
                onOpenSyncStatus = onOpenSyncStatus,
                onOpenNotifications = onOpenNotifications,
            )

            PersonalDashboardScreenState.Empty -> PersonalDashboardEmptySection(
                greeting = greeting,
                onOpenSettings = onOpenSettings,
                onOpenSyncStatus = onOpenSyncStatus,
                onOpenNotifications = onOpenNotifications,
            )

            PersonalDashboardScreenState.Error -> PersonalDashboardErrorSection(
                greeting = greeting,
                groupName = state.selectedGroup?.name.orEmpty(),
                onRetry = { onAction(PersonalDashboardAction.OnRetry) },
                onOpenSettings = onOpenSettings,
                onOpenSyncStatus = onOpenSyncStatus,
                onOpenNotifications = onOpenNotifications,
            )
        }
    }
}

/**
 * Shared primary-colored header — `ui.yaml#components.top_bar` + `group_banner`. Renders the
 * time-of-day [greeting], an interactive notifications bell ([onOpenNotifications] →
 * [PersonalDashboardAction.OnOpenNotifications], G14 — was a dead badge icon with no `on_click`),
 * the active [groupName] + currency chip, and — only when [showChips] is true (`myGroups.size > 1`)
 * — the [GroupSelectorChipRow]. See API.md#screen.
 */
@Composable
internal fun PersonalDashboardTopSection(
    greeting: String,
    groupName: String,
    showChips: Boolean,
    groups: List<GroupSummary>,
    selectedGroupId: String?,
    onGroupSelected: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSyncStatus: () -> Unit,
    onOpenNotifications: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val notificationCd = stringResource(Res.string.screens_personal_dashboard_notification_icon_cd)
    val currencyLabel = stringResource(Res.string.screens_personal_dashboard_currency_kes)

    Surface(color = MaterialTheme.colorScheme.primary, modifier = modifier.fillMaxWidth()) {
        // Bottom apron of green below the group banner: the Savings card floats up over the header
        // via `offset(y = -sp.xl)` — without this green landing zone that offset lands the card ON
        // the group banner (covering it). The apron equals the card's lift so the card overlaps only
        // green, keeping the banner fully visible above it.
        Column(modifier = Modifier.padding(bottom = sp.xl)) {
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
                IconButton(
                    onClick = onOpenNotifications,
                    modifier = Modifier.testTag(PersonalDashboardTestTags.NOTIFICATION_ICON),
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsNone,
                        contentDescription = notificationCd,
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                PersonalDashboardOverflowMenu(
                    onOpenSettings = onOpenSettings,
                    onOpenSyncStatus = onOpenSyncStatus,
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
 * Profile / overflow menu — `ui.yaml#components.top_bar.overflow_menu`. A [MoreVert] [IconButton]
 * that opens a [DropdownMenu] with `Settings` ([onOpenSettings] →
 * [PersonalDashboardAction.OnSettingsClick]) and `Sync Status` ([onOpenSyncStatus] →
 * [PersonalDashboardAction.OnSyncStatusClick]) items — the in-app affordance that makes the shared
 * `settings` + `sync-status` screens reachable from the authenticated member home. See API.md#screen.
 */
@Composable
internal fun PersonalDashboardOverflowMenu(
    onOpenSettings: () -> Unit,
    onOpenSyncStatus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val menuCd = stringResource(Res.string.screens_personal_dashboard_overflow_menu_cd)
    val settingsLabel = stringResource(Res.string.screens_personal_dashboard_menu_settings)
    val syncStatusLabel = stringResource(Res.string.screens_personal_dashboard_menu_sync_status)

    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.testTag(PersonalDashboardTestTags.OVERFLOW_MENU),
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = menuCd,
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(settingsLabel) },
                leadingIcon = { Icon(imageVector = Icons.Filled.Settings, contentDescription = null) },
                onClick = {
                    expanded = false
                    onOpenSettings()
                },
                modifier = Modifier.testTag(PersonalDashboardTestTags.MENU_SETTINGS_ITEM),
            )
            DropdownMenuItem(
                text = { Text(syncStatusLabel) },
                leadingIcon = { Icon(imageVector = Icons.Filled.Sync, contentDescription = null) },
                onClick = {
                    expanded = false
                    onOpenSyncStatus()
                },
                modifier = Modifier.testTag(PersonalDashboardTestTags.MENU_SYNC_STATUS_ITEM),
            )
        }
    }
}

/**
 * `PersonalDashboardScreenState.Loading` — top section (group name blank on cold start, per
 * [PersonalDashboardState]'s declared `""`/`null` defaults) + 4 shimmering skeleton blocks,
 * mirroring `preview/loading.html`'s `shimmer_loading` (count: 4). See API.md#screen.
 */
@Composable
internal fun PersonalDashboardLoadingSection(
    greeting: String,
    onOpenSettings: () -> Unit,
    onOpenSyncStatus: () -> Unit,
    onOpenNotifications: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    Column(modifier = modifier.fillMaxSize().testTag(PersonalDashboardTestTags.LOADING_SECTION)) {
        PersonalDashboardTopSection(
            greeting = greeting,
            groupName = "",
            showChips = false,
            groups = emptyList(),
            selectedGroupId = null,
            onGroupSelected = {},
            onOpenSettings = onOpenSettings,
            onOpenSyncStatus = onOpenSyncStatus,
            onOpenNotifications = onOpenNotifications,
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
    onOpenSettings: () -> Unit,
    onOpenSyncStatus: () -> Unit,
    onOpenNotifications: () -> Unit,
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
                onOpenSettings = onOpenSettings,
                onOpenSyncStatus = onOpenSyncStatus,
                onOpenNotifications = onOpenNotifications,
            )
        }
        item {
            // All three summary cards in ONE column with a single, consistent `spacedBy(sp.md)` gap
            // (was three separately-`offset` items whose chained lifts left UNEVEN gaps — 12dp between
            // savings→loans but ~0dp between loans→share-out). The column floats up by `-sp.xl` so only
            // the FIRST card (savings) overlaps the header's green apron; the rest flow evenly below.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = -sp.xl)
                    .padding(horizontal = sp.lg),
                verticalArrangement = Arrangement.spacedBy(sp.md),
            ) {
                SavingsSummaryCard(
                    groupLinkedBalance = state.groupLinkedSavingsBalance,
                    individualBalance = state.individualSavingsBalance,
                    onClick = { onAction(PersonalDashboardAction.OnSavingsCardClick) },
                )
                LoanSummaryCard(
                    onClick = { onAction(PersonalDashboardAction.OnLoansCardClick) },
                )
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
internal fun PersonalDashboardEmptySection(
    greeting: String,
    onOpenSettings: () -> Unit,
    onOpenSyncStatus: () -> Unit,
    onOpenNotifications: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            onOpenSettings = onOpenSettings,
            onOpenSyncStatus = onOpenSyncStatus,
            onOpenNotifications = onOpenNotifications,
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
    onOpenSettings: () -> Unit,
    onOpenSyncStatus: () -> Unit,
    onOpenNotifications: () -> Unit,
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
            onOpenSettings = onOpenSettings,
            onOpenSyncStatus = onOpenSyncStatus,
            onOpenNotifications = onOpenNotifications,
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
