/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.grouplist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.GroupOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kpt.core.base.ui.effects.EventsEffect
import kpt.core.base.ui.paging.rememberLoadMoreTrigger
import kpt.core.designsystem.theme.spacing
import kpt.core.ui.scaffold.FloatingActionButtonContent
import kpt.core.ui.scaffold.KptScaffold
import kpt.core.ui.scaffold.rememberKptPullToRefreshState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.mifos.groupbanking.feature.grouplist.components.GroupListCard
import org.mifos.groupbanking.feature.grouplist.components.GroupListCardSkeleton
import org.mifos.groupbanking.feature.grouplist.components.GroupListSearchBar
import org.mifos.groupbanking.feature.grouplist.generated.resources.Res
import org.mifos.groupbanking.feature.grouplist.generated.resources.screens_group_list_action_create
import org.mifos.groupbanking.feature.grouplist.generated.resources.screens_group_list_action_join
import org.mifos.groupbanking.feature.grouplist.generated.resources.screens_group_list_action_retry
import org.mifos.groupbanking.feature.grouplist.generated.resources.screens_group_list_empty_body
import org.mifos.groupbanking.feature.grouplist.generated.resources.screens_group_list_empty_icon_cd
import org.mifos.groupbanking.feature.grouplist.generated.resources.screens_group_list_empty_title
import org.mifos.groupbanking.feature.grouplist.generated.resources.screens_group_list_error_auth_message
import org.mifos.groupbanking.feature.grouplist.generated.resources.screens_group_list_error_icon_cd
import org.mifos.groupbanking.feature.grouplist.generated.resources.screens_group_list_error_network_message
import org.mifos.groupbanking.feature.grouplist.generated.resources.screens_group_list_error_server_message
import org.mifos.groupbanking.feature.grouplist.generated.resources.screens_group_list_error_title
import org.mifos.groupbanking.feature.grouplist.generated.resources.screens_group_list_fab_cd
import org.mifos.groupbanking.feature.grouplist.generated.resources.screens_group_list_loading_message
import org.mifos.groupbanking.feature.grouplist.generated.resources.screens_group_list_search_empty_message
import org.mifos.groupbanking.feature.grouplist.generated.resources.screens_group_list_title

/**
 * Container for `group-list-screen`. Collects [GroupListViewModel] state via
 * [collectAsStateWithLifecycle], consumes one-shot [GroupListEvent]s (navigate to group-dashboard
 * / group-type-picker / join-with-code, show snackbar) through [EventsEffect], and delegates all
 * rendering to the stateless [GroupListContent]. [onNavigateToGroupDashboard] /
 * [onNavigateToCreateGroup] / [onNavigateToJoinGroup] are typed nav-arg contracts for the caller
 * to wire — `group-dashboard` and `join-with-code` are not yet generated feature modules in this
 * codebase, mirroring `GroupTypePickerScreen`'s identical not-yet-generated-target convention
 * (RULE-PROTO-COMPOSE-DEAD-CLICK-001 DC3). [GroupListEvent.ShowSnackbar.message] carries a
 * `messageKey` (`error_network` / `error_server` / `error_auth`), NOT a display string — resolved
 * here via [messageKeyToText] before showing, since [GroupListError.messageKey] KDoc documents
 * this is a composeResources key. See API.md#screen.
 */
@Composable
internal fun GroupListScreen(
    onNavigateToGroupDashboard: (groupId: String, viewerRole: String) -> Unit,
    onNavigateToCreateGroup: () -> Unit,
    onNavigateToJoinGroup: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GroupListViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val networkMessage = stringResource(Res.string.screens_group_list_error_network_message)
    val serverMessage = stringResource(Res.string.screens_group_list_error_server_message)
    val authMessage = stringResource(Res.string.screens_group_list_error_auth_message)

    EventsEffect(viewModel) { event ->
        when (event) {
            is GroupListEvent.NavigateToGroupDashboard ->
                onNavigateToGroupDashboard(event.groupId, event.viewerRole)
            GroupListEvent.NavigateToCreateGroup -> onNavigateToCreateGroup()
            GroupListEvent.NavigateToJoinGroup -> onNavigateToJoinGroup()
            is GroupListEvent.ShowSnackbar -> {
                val resolved = messageKeyToText(event.message, networkMessage, serverMessage, authMessage)
                snackbarHostState.showSnackbar(resolved)
            }
        }
    }

    GroupListContent(
        state = state,
        onAction = viewModel::trySendAction,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

/** Resolves a [GroupListError.messageKey] to display text — see [GroupListScreen] KDoc. */
private fun messageKeyToText(key: String, network: String, server: String, auth: String): String = when (key) {
    "error_network" -> network
    "error_server" -> server
    "error_auth" -> auth
    else -> key
}

/**
 * Stateless render surface for `group-list-screen`. State-driven per
 * `GroupListState.screenState` — every [GroupListScreenState] member is handled
 * (Loading/Content/Error/Empty). No back navigation affordance — `GroupListAction` declares no
 * `OnBack` member; this is a root/tab-level screen. See API.md#screen.
 */
@Composable
internal fun GroupListContent(
    state: GroupListState,
    onAction: (GroupListAction) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val title = stringResource(Res.string.screens_group_list_title)
    val fabCd = stringResource(Res.string.screens_group_list_fab_cd)

    KptScaffold(
        showNavigationIcon = false,
        title = title,
        floatingActionButtonContent = FloatingActionButtonContent(
            onClick = { onAction(GroupListAction.OnCreateGroup) },
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            content = { GroupListFabContent(fabCd = fabCd) },
        ),
        pullToRefreshState = rememberKptPullToRefreshState(
            isEnabled = true,
            isRefreshing = state.isRefreshing,
            onRefresh = { onAction(GroupListAction.OnRefresh) },
        ),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.testTag(GroupListTestTags.SCREEN),
    ) {
        when (state.screenState) {
            GroupListScreenState.Loading -> GroupListLoadingSection(
                query = state.searchQuery,
                onAction = onAction,
            )

            GroupListScreenState.Content -> GroupListContentSection(state = state, onAction = onAction)

            GroupListScreenState.Empty -> GroupListEmptySection(
                query = state.searchQuery,
                onAction = onAction,
            )

            GroupListScreenState.Error -> GroupListErrorSection(
                message = when (state.error) {
                    GroupListError.Network, null ->
                        stringResource(Res.string.screens_group_list_error_network_message)
                    GroupListError.Server ->
                        stringResource(Res.string.screens_group_list_error_server_message)
                    GroupListError.Auth ->
                        stringResource(Res.string.screens_group_list_error_auth_message)
                },
                onRetry = { onAction(GroupListAction.Retry) },
            )
        }
    }
}

/** FAB content — icon + label, giving the plain [kpt.core.ui.scaffold.KptScaffold] FAB slot an
 * extended-FAB look (the framework's `FloatingActionButtonContent` has no dedicated extended
 * variant). The merged [fabCd] semantics carry the accessible label; the icon is decorative. */
@Composable
internal fun GroupListFabContent(fabCd: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
        modifier = Modifier.semantics { contentDescription = fabCd },
    ) {
        Icon(imageVector = Icons.Filled.Add, contentDescription = null)
        Text(text = stringResource(Res.string.screens_group_list_action_create))
    }
}

/**
 * `GroupListScreenState.Loading` — search bar (bound, though not yet actionable against an empty
 * list) + 5 shimmering [GroupListCardSkeleton] rows behind a centered [CircularProgressIndicator],
 * mirroring `preview/loading.html` / `MOCKUP.md`. See API.md#screen.
 */
@Composable
internal fun GroupListLoadingSection(query: String, onAction: (GroupListAction) -> Unit, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val loadingLabel = stringResource(Res.string.screens_group_list_loading_message)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(sp.lg)
            .semantics { contentDescription = loadingLabel },
        verticalArrangement = Arrangement.spacedBy(sp.md),
    ) {
        GroupListSearchBar(
            query = query,
            onQueryChange = { onAction(GroupListAction.OnSearch(it)) },
            onClear = { onAction(GroupListAction.OnClearSearch) },
        )
        repeat(5) { GroupListCardSkeleton() }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.testTag(GroupListTestTags.LOADING_INDICATOR),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/**
 * `GroupListScreenState.Content` — search bar + scrollable list of [GroupListCard]s. When
 * [GroupListState.filteredGroups] is empty while a search query is active, renders an inline
 * "no results for query" message INSTEAD of the list — still `Content`, never the full-illustration
 * [GroupListEmptySection] (see `GroupListViewModel`'s `screenState` KDoc: an empty *search result*
 * is never the same as a genuinely zero-group account). Scroll-to-end dispatches
 * [GroupListAction.OnLoadMoreTap] via [rememberLoadMoreTrigger] — `GroupListState` does not
 * surface `hasMore`/`isLoadingMore` paging-progress flags (an idea-layer gap already flagged on
 * `GroupListViewModel.handleLoadMoreTap`'s KDoc), so this uses the permissive
 * `hasMore = true, isLoadingMore = false` approximation and relies on the underlying
 * `PagingScreenStream` to no-op once exhausted. See API.md#screen.
 */
@Composable
internal fun GroupListContentSection(state: GroupListState, onAction: (GroupListAction) -> Unit, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val listState = rememberLazyListState()
    val shouldLoadMore by rememberLoadMoreTrigger(listState = listState, hasMore = true, isLoadingMore = false)

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onAction(GroupListAction.OnLoadMoreTap)
    }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = sp.lg), verticalArrangement = Arrangement.spacedBy(sp.md)) {
        GroupListSearchBar(
            query = state.searchQuery,
            onQueryChange = { onAction(GroupListAction.OnSearch(it)) },
            onClear = { onAction(GroupListAction.OnClearSearch) },
            modifier = Modifier.padding(top = sp.md),
        )

        if (state.filteredGroups.isEmpty() && state.searchQuery.isNotBlank()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(Res.string.screens_group_list_search_empty_message, state.searchQuery),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag(GroupListTestTags.SEARCH_EMPTY_MESSAGE),
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().testTag(GroupListTestTags.GROUP_LIST),
                verticalArrangement = Arrangement.spacedBy(sp.md),
            ) {
                items(items = state.filteredGroups, key = { it.id }) { group ->
                    GroupListCard(
                        group = group,
                        onClick = { onAction(GroupListAction.OnGroupClick(group.id, group.viewerRole.name)) },
                        testTag = GroupListTestTags.cardTag(group.id),
                    )
                }
            }
        }
    }
}

/**
 * `GroupListScreenState.Empty` — genuinely zero groups (`GroupListState.groups.isEmpty()`, no
 * search filter). Illustration + title + body + "Create Group" (primary) and "Join with Code"
 * (secondary) CTAs, mirroring `MOCKUP.md`'s Empty state description. See API.md#screen.
 */
@Composable
internal fun GroupListEmptySection(query: String, onAction: (GroupListAction) -> Unit, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val iconCd = stringResource(Res.string.screens_group_list_empty_icon_cd)
    val titleText = stringResource(Res.string.screens_group_list_empty_title)
    val bodyText = stringResource(Res.string.screens_group_list_empty_body)
    val createLabel = stringResource(Res.string.screens_group_list_action_create)
    val joinLabel = stringResource(Res.string.screens_group_list_action_join)

    Column(
        modifier = modifier.fillMaxSize().padding(sp.lg).testTag(GroupListTestTags.EMPTY_SECTION),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        GroupListSearchBar(
            query = query,
            onQueryChange = { onAction(GroupListAction.OnSearch(it)) },
            onClear = { onAction(GroupListAction.OnClearSearch) },
            modifier = Modifier.padding(bottom = sp.xl),
        )

        Icon(
            imageVector = Icons.Filled.GroupOff,
            contentDescription = iconCd,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(80.dp),
        )
        Text(
            text = titleText,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = sp.lg),
        )
        Text(
            text = bodyText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = sp.sm),
        )

        OutlinedButton(
            onClick = { onAction(GroupListAction.OnCreateGroup) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = sp.touchTargetMin)
                .padding(top = sp.lg)
                .testTag(GroupListTestTags.EMPTY_CREATE_BUTTON),
        ) {
            Text(text = createLabel)
        }

        TextButton(
            onClick = { onAction(GroupListAction.OnJoinGroup) },
            modifier = Modifier
                .heightIn(min = sp.touchTargetMin)
                .padding(top = sp.sm)
                .testTag(GroupListTestTags.EMPTY_JOIN_BUTTON),
        ) {
            Text(text = joinLabel)
        }
    }
}

/**
 * `GroupListScreenState.Error` — full-screen error surface (cloud_off icon, title, resolved
 * [message], Retry CTA), mirroring `MOCKUP.md`'s Error state description. See API.md#screen.
 */
@Composable
internal fun GroupListErrorSection(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val errorIconCd = stringResource(Res.string.screens_group_list_error_icon_cd)
    val titleText = stringResource(Res.string.screens_group_list_error_title)
    val retryLabel = stringResource(Res.string.screens_group_list_action_retry)

    Column(
        modifier = modifier.fillMaxSize().padding(sp.lg).testTag(GroupListTestTags.ERROR_SECTION),
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
            text = titleText,
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
            colors = ButtonDefaults.buttonColors(),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = sp.touchTargetMin)
                .padding(top = sp.lg)
                .testTag(GroupListTestTags.ERROR_RETRY_BUTTON),
        ) {
            Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
            Text(text = retryLabel, modifier = Modifier.padding(start = sp.xs))
        }
    }
}
