/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.memberlist

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import kpt.core.base.designsystem.core.TopAppBarAction
import kpt.core.base.ui.effects.EventsEffect
import kpt.core.base.ui.paging.rememberLoadMoreTrigger
import kpt.core.designsystem.theme.spacing
import kpt.core.ui.scaffold.FloatingActionButtonContent
import kpt.core.ui.scaffold.KptScaffold
import kpt.core.ui.scaffold.rememberKptPullToRefreshState
import kpt.feature.memberlist.components.MemberListItemRow
import kpt.feature.memberlist.components.MemberListItemSkeleton
import kpt.feature.memberlist.generated.resources.Res
import kpt.feature.memberlist.generated.resources.screens_member_list_action_add_member
import kpt.feature.memberlist.generated.resources.screens_member_list_action_retry
import kpt.feature.memberlist.generated.resources.screens_member_list_empty_body
import kpt.feature.memberlist.generated.resources.screens_member_list_empty_icon_cd
import kpt.feature.memberlist.generated.resources.screens_member_list_empty_title
import kpt.feature.memberlist.generated.resources.screens_member_list_error_auth_message
import kpt.feature.memberlist.generated.resources.screens_member_list_error_icon_cd
import kpt.feature.memberlist.generated.resources.screens_member_list_error_network_message
import kpt.feature.memberlist.generated.resources.screens_member_list_error_server_message
import kpt.feature.memberlist.generated.resources.screens_member_list_error_title
import kpt.feature.memberlist.generated.resources.screens_member_list_fab_cd
import kpt.feature.memberlist.generated.resources.screens_member_list_invite_cd
import kpt.feature.memberlist.generated.resources.screens_member_list_load_more_message
import kpt.feature.memberlist.generated.resources.screens_member_list_loading_message
import kpt.feature.memberlist.generated.resources.screens_member_list_title
import kpt.feature.memberlist.generated.resources.screens_member_list_title_with_group
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Container for `member-list-screen`. Collects [MemberListViewModel] state via
 * [collectAsStateWithLifecycle], consumes one-shot [MemberListEvent]s (navigate to member-profile
 * / member-add / back, show snackbar) through [EventsEffect], and delegates all rendering to the
 * stateless [MemberListContent]. [groupId] is the `ui.yaml#nav_params.groupId` forwarded from
 * `group-dashboard`'s `view_members_button` trigger — supplied to [MemberListViewModel] via Koin
 * `parametersOf(groupId)` (mirrors `GroupDashboardScreen`'s identical nav-arg wiring).
 * [onNavigateToMemberProfile] / [onNavigateToAddMember] are typed nav-arg contracts for the caller
 * to wire — `member-profile` / `member-add` are not yet generated feature modules in this codebase,
 * mirroring `GroupListScreen`'s identical not-yet-generated-target convention
 * (RULE-PROTO-COMPOSE-DEAD-CLICK-001 DC3). [MemberListEvent.ShowSnackbar.message] carries a
 * `messageKey` (`error_auth`), NOT a display string — resolved here via [messageKeyToText] before
 * showing, since [MemberListError.messageKey] KDoc documents this is a composeResources key.
 * See API.md#screen.
 */
@Composable
internal fun MemberListScreen(
    groupId: String,
    onNavigateToMemberProfile: (memberId: String, groupId: String) -> Unit,
    onNavigateToAddMember: (groupId: String) -> Unit,
    onNavigateToMemberInvite: (groupId: String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MemberListViewModel = koinViewModel(parameters = { parametersOf(groupId) }),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val networkMessage = stringResource(Res.string.screens_member_list_error_network_message)
    val serverMessage = stringResource(Res.string.screens_member_list_error_server_message)
    val authMessage = stringResource(Res.string.screens_member_list_error_auth_message)

    EventsEffect(viewModel) { event ->
        when (event) {
            is MemberListEvent.NavigateToMemberProfile ->
                onNavigateToMemberProfile(event.memberId, event.groupId)
            is MemberListEvent.NavigateToAddMember -> onNavigateToAddMember(event.groupId)
            is MemberListEvent.NavigateToMemberInvite -> onNavigateToMemberInvite(event.groupId)
            MemberListEvent.NavigateBack -> onNavigateBack()
            is MemberListEvent.ShowSnackbar -> {
                val resolved = messageKeyToText(event.message, networkMessage, serverMessage, authMessage)
                snackbarHostState.showSnackbar(resolved)
            }
        }
    }

    MemberListContent(
        state = state,
        onAction = viewModel::trySendAction,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

/** Resolves a [MemberListError.messageKey] to display text — see [MemberListScreen] KDoc. */
private fun messageKeyToText(key: String, network: String, server: String, auth: String): String = when (key) {
    "error_network" -> network
    "error_server" -> server
    "error_auth" -> auth
    else -> key
}

/**
 * Stateless render surface for `member-list-screen`. State-driven per
 * [MemberListState.screenState] — every [MemberListScreenState] member is handled
 * (Loading/Content/Error/Empty). Back navigation is present (`ui.yaml#top_bar.on_navigation_click`
 * → [MemberListAction.OnBack]); the `+ Add Member` FAB dispatches [MemberListAction.OnAddMember].
 * See API.md#screen.
 */
@Composable
internal fun MemberListContent(
    state: MemberListState,
    onAction: (MemberListAction) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val title = if (state.groupName.isNotBlank()) {
        stringResource(Res.string.screens_member_list_title_with_group, state.groupName)
    } else {
        stringResource(Res.string.screens_member_list_title)
    }
    val fabCd = stringResource(Res.string.screens_member_list_fab_cd)
    val inviteCd = stringResource(Res.string.screens_member_list_invite_cd)

    KptScaffold(
        showNavigationIcon = true,
        onNavigationIconClick = { onAction(MemberListAction.OnBack) },
        title = title,
        actions = listOf(
            // Invite-member top-bar action — member-onboarding-flow invite path (-> member-invite).
            TopAppBarAction(
                icon = Icons.Filled.GroupAdd,
                contentDescription = inviteCd,
                onClick = { onAction(MemberListAction.OnInviteMember) },
                testTag = MemberListTestTags.INVITE_ACTION,
            ),
        ),
        floatingActionButtonContent = FloatingActionButtonContent(
            onClick = { onAction(MemberListAction.OnAddMember) },
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            content = { MemberListFabContent(fabCd = fabCd) },
        ),
        pullToRefreshState = rememberKptPullToRefreshState(
            isEnabled = true,
            isRefreshing = state.isRefreshing,
            onRefresh = { onAction(MemberListAction.OnRefresh) },
        ),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.testTag(MemberListTestTags.SCREEN),
    ) {
        when (state.screenState) {
            MemberListScreenState.Loading -> MemberListLoadingSection()

            MemberListScreenState.Content -> MemberListContentSection(state = state, onAction = onAction)

            MemberListScreenState.Empty -> MemberListEmptySection(onAction = onAction)

            MemberListScreenState.Error -> MemberListErrorSection(
                message = when (state.error) {
                    MemberListError.Network, null ->
                        stringResource(Res.string.screens_member_list_error_network_message)
                    MemberListError.Server ->
                        stringResource(Res.string.screens_member_list_error_server_message)
                    MemberListError.Auth ->
                        stringResource(Res.string.screens_member_list_error_auth_message)
                },
                onRetry = { onAction(MemberListAction.Retry) },
            )
        }
    }
}

/** FAB content — icon + label, giving the plain [kpt.core.ui.scaffold.KptScaffold] FAB slot an
 * extended-FAB look (the framework's `FloatingActionButtonContent` has no dedicated extended
 * variant). The merged [fabCd] semantics carry the accessible label; the icon is decorative. */
@Composable
internal fun MemberListFabContent(fabCd: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
        modifier = Modifier
            .testTag(MemberListTestTags.FAB_ADD_MEMBER)
            .semantics { contentDescription = fabCd },
    ) {
        Icon(imageVector = Icons.Filled.Add, contentDescription = null)
        Text(text = stringResource(Res.string.screens_member_list_action_add_member))
    }
}

/**
 * `MemberListScreenState.Loading` — 6 shimmering [MemberListItemSkeleton] rows behind a centered
 * [CircularProgressIndicator], mirroring `preview/loading.html` / `MOCKUP.md`. See API.md#screen.
 */
@Composable
internal fun MemberListLoadingSection(modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val loadingLabel = stringResource(Res.string.screens_member_list_loading_message)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = sp.md)
            .testTag(MemberListTestTags.LOADING_SECTION)
            .semantics { contentDescription = loadingLabel },
        verticalArrangement = Arrangement.spacedBy(sp.sm),
    ) {
        repeat(6) { MemberListItemSkeleton() }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.testTag(MemberListTestTags.LOADING_INDICATOR),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/**
 * `MemberListScreenState.Content` — scrollable [MemberListItemRow] list. Scroll-to-end dispatches
 * [MemberListAction.OnLoadMore] via [rememberLoadMoreTrigger] (driven by
 * [MemberListState.hasMorePages] / [MemberListState.isLoadingMore] off the underlying
 * `PagingScreenStream`), and a [LinearProgressIndicator] footer renders while
 * [MemberListState.isLoadingMore] — `ui.yaml#load_more_indicator`. See API.md#screen.
 */
@Composable
internal fun MemberListContentSection(state: MemberListState, onAction: (MemberListAction) -> Unit, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val listState = rememberLazyListState()
    val shouldLoadMore by rememberLoadMoreTrigger(
        listState = listState,
        hasMore = state.hasMorePages,
        isLoadingMore = state.isLoadingMore,
    )
    val loadMoreLabel = stringResource(Res.string.screens_member_list_load_more_message)

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onAction(MemberListAction.OnLoadMore)
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = sp.sm)
            .testTag(MemberListTestTags.MEMBER_LIST),
    ) {
        items(items = state.members, key = { it.id }) { member ->
            MemberListItemRow(
                member = member,
                onClick = { onAction(MemberListAction.OnMemberClick(member.id)) },
                testTag = MemberListTestTags.memberRowTag(member.id),
            )
        }

        if (state.isLoadingMore) {
            item(key = "member_list_load_more") {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(sp.md)
                        .testTag(MemberListTestTags.LOAD_MORE_INDICATOR)
                        .semantics { contentDescription = loadMoreLabel },
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/**
 * `MemberListScreenState.Empty` — genuinely zero members (`MemberListState.members.isEmpty()`).
 * Illustration + title + body + "Add Member" primary CTA, mirroring `MOCKUP.md`'s Empty state
 * description. See API.md#screen.
 */
@Composable
internal fun MemberListEmptySection(onAction: (MemberListAction) -> Unit, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val iconCd = stringResource(Res.string.screens_member_list_empty_icon_cd)
    val titleText = stringResource(Res.string.screens_member_list_empty_title)
    val bodyText = stringResource(Res.string.screens_member_list_empty_body)
    val addLabel = stringResource(Res.string.screens_member_list_action_add_member)

    Column(
        modifier = modifier.fillMaxSize().padding(sp.lg).testTag(MemberListTestTags.EMPTY_SECTION),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.PersonOff,
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
            onClick = { onAction(MemberListAction.OnAddMember) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = sp.touchTargetMin)
                .padding(top = sp.lg)
                .testTag(MemberListTestTags.EMPTY_ADD_BUTTON),
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = null)
            Text(text = addLabel, modifier = Modifier.padding(start = sp.xs))
        }
    }
}

/**
 * `MemberListScreenState.Error` — full-screen error surface (cloud_off icon, title, resolved
 * [message], Retry CTA), mirroring `MOCKUP.md`'s Error state description. See API.md#screen.
 */
@Composable
internal fun MemberListErrorSection(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val errorIconCd = stringResource(Res.string.screens_member_list_error_icon_cd)
    val titleText = stringResource(Res.string.screens_member_list_error_title)
    val retryLabel = stringResource(Res.string.screens_member_list_action_retry)

    Column(
        modifier = modifier.fillMaxSize().padding(sp.lg).testTag(MemberListTestTags.ERROR_SECTION),
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
                .testTag(MemberListTestTags.ERROR_RETRY_BUTTON),
        ) {
            Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
            Text(text = retryLabel, modifier = Modifier.padding(start = sp.xs))
        }
    }
}
