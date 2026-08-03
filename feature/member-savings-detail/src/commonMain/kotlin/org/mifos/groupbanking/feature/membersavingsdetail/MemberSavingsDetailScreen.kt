/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.membersavingsdetail

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kpt.core.base.ui.effects.EventsEffect
import kpt.core.base.ui.paging.rememberLoadMoreTrigger
import kpt.core.designsystem.theme.spacing
import kpt.core.ui.scaffold.KptScaffold
import kpt.core.ui.scaffold.rememberKptPullToRefreshState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.mifos.groupbanking.core.model.GroupTypeConfig
import org.mifos.groupbanking.feature.membersavingsdetail.components.MemberSavingsHeaderCard
import org.mifos.groupbanking.feature.membersavingsdetail.components.SavingsFilterChips
import org.mifos.groupbanking.feature.membersavingsdetail.components.SavingsSparklineCard
import org.mifos.groupbanking.feature.membersavingsdetail.components.SavingsStatementRow
import org.mifos.groupbanking.feature.membersavingsdetail.generated.resources.Res
import org.mifos.groupbanking.feature.membersavingsdetail.generated.resources.screens_member_savings_detail_action_retry
import org.mifos.groupbanking.feature.membersavingsdetail.generated.resources.screens_member_savings_detail_empty_body
import org.mifos.groupbanking.feature.membersavingsdetail.generated.resources.screens_member_savings_detail_empty_icon_cd
import org.mifos.groupbanking.feature.membersavingsdetail.generated.resources.screens_member_savings_detail_empty_title
import org.mifos.groupbanking.feature.membersavingsdetail.generated.resources.screens_member_savings_detail_error_auth
import org.mifos.groupbanking.feature.membersavingsdetail.generated.resources.screens_member_savings_detail_error_icon_cd
import org.mifos.groupbanking.feature.membersavingsdetail.generated.resources.screens_member_savings_detail_error_network
import org.mifos.groupbanking.feature.membersavingsdetail.generated.resources.screens_member_savings_detail_error_not_found
import org.mifos.groupbanking.feature.membersavingsdetail.generated.resources.screens_member_savings_detail_error_server
import org.mifos.groupbanking.feature.membersavingsdetail.generated.resources.screens_member_savings_detail_error_title
import org.mifos.groupbanking.feature.membersavingsdetail.generated.resources.screens_member_savings_detail_load_more_cd
import org.mifos.groupbanking.feature.membersavingsdetail.generated.resources.screens_member_savings_detail_screen_title

/**
 * Container for `member-savings-detail-screen`. Collects [MemberSavingsDetailViewModel] state via
 * [collectAsStateWithLifecycle], consumes one-shot [MemberSavingsDetailEvent]s (navigate back /
 * show a snackbar) through [EventsEffect], and delegates all rendering to the stateless
 * [MemberSavingsDetailContent]. [memberId]/[groupId]/[typeConfig] are the `ui.yaml#nav_params`
 * values forwarded from `savings-dashboard`'s `tap_member_row` (or `member-profile`'s
 * `view_full_history_button`) entry point — supplied to [MemberSavingsDetailViewModel] via Koin
 * `parametersOf(memberId, groupId, typeConfig)` (matching `MemberSavingsDetailModule`'s
 * `viewModel { parameters -> ... }` declaration order). See API.md#screen.
 */
@Composable
internal fun MemberSavingsDetailScreen(
    memberId: String,
    groupId: String,
    typeConfig: GroupTypeConfig,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MemberSavingsDetailViewModel = koinViewModel(
        parameters = { parametersOf(memberId, groupId, typeConfig) },
    ),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Precomputed OUTSIDE EventsEffect — stringResource() is @Composable-only and EventsEffect's
    // callback runs in a suspend (non-composable) scope, same convention as
    // MemberProfileScreen.kt / GroupDashboardScreen.kt.
    val networkMessage = stringResource(Res.string.screens_member_savings_detail_error_network)
    val serverMessage = stringResource(Res.string.screens_member_savings_detail_error_server)
    val notFoundMessage = stringResource(Res.string.screens_member_savings_detail_error_not_found)
    val authMessage = stringResource(Res.string.screens_member_savings_detail_error_auth)

    EventsEffect(viewModel) { event ->
        when (event) {
            MemberSavingsDetailEvent.NavigateBack -> onNavigateBack()
            is MemberSavingsDetailEvent.ShowSnackbar -> snackbarHostState.showSnackbar(
                message = when (event.message) {
                    "error_network" -> networkMessage
                    "error_server" -> serverMessage
                    "error_not_found" -> notFoundMessage
                    "error_auth" -> authMessage
                    else -> event.message
                },
            )
        }
    }

    MemberSavingsDetailContent(
        state = state,
        onAction = viewModel::trySendAction,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

/**
 * Stateless render surface for `member-savings-detail-screen`. State-driven per
 * [MemberSavingsDetailState.deriveScreenState] — every [MemberSavingsDetailScreenState] member is
 * handled (Loading/Content/Empty/Error). Back navigation and pull-to-refresh both dispatch real
 * declared [MemberSavingsDetailAction] members ([MemberSavingsDetailAction.OnBack] /
 * [MemberSavingsDetailAction.OnRefresh]) — no free callback params threaded through (RULE-IMPL-DEAD-CLICKABLE-001
 * Rule 3). See API.md#screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MemberSavingsDetailContent(
    state: MemberSavingsDetailState,
    onAction: (MemberSavingsDetailAction) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val title = stringResource(Res.string.screens_member_savings_detail_screen_title)
    val screenState = state.deriveScreenState()

    KptScaffold(
        showNavigationIcon = true,
        onNavigationIconClick = { onAction(MemberSavingsDetailAction.OnBack) },
        title = title,
        pullToRefreshState = rememberKptPullToRefreshState(
            isEnabled = screenState == MemberSavingsDetailScreenState.Content,
            isRefreshing = state.isRefreshing,
            onRefresh = { onAction(MemberSavingsDetailAction.OnRefresh) },
        ),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.testTag(MemberSavingsDetailTestTags.SCREEN),
    ) {
        when (screenState) {
            MemberSavingsDetailScreenState.Loading -> MemberSavingsDetailLoadingSection()
            MemberSavingsDetailScreenState.Content -> MemberSavingsDetailContentSection(state = state, onAction = onAction)
            MemberSavingsDetailScreenState.Empty -> MemberSavingsDetailEmptySection(state = state, onAction = onAction)
            MemberSavingsDetailScreenState.Error -> MemberSavingsDetailErrorSection(state = state, onAction = onAction)
        }
    }
}

/**
 * Header block shared by [MemberSavingsDetailScreenState.Content] and
 * [MemberSavingsDetailScreenState.Empty] — `ui.yaml#states.content.components` and
 * `ui.yaml#states.empty.components` both list `[member_header_card, savings_sparkline_card,
 * filter_chips_row]` verbatim. Defensively returns without rendering when `state.member == null`
 * (guaranteed non-null once `screenState` is `Content`/`Empty`, per
 * [MemberSavingsDetailState.deriveScreenState] — this is a no-render guard, not a reachable branch).
 * See API.md#screen.
 */
@Composable
internal fun MemberSavingsDetailHeaderSection(
    state: MemberSavingsDetailState,
    onAction: (MemberSavingsDetailAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val member = state.member ?: return
    val sp = MaterialTheme.spacing

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(sp.md)) {
        MemberSavingsHeaderCard(
            member = member,
            contributionModel = state.contributionModel,
            sharesHeld = state.sharesHeld,
            shareValue = state.shareValue,
            savingsBalance = state.savingsBalance,
            savingsAccountNo = state.savingsAccountNo,
        )
        if (state.sparklineData.isNotEmpty()) {
            SavingsSparklineCard(points = state.sparklineData)
        }
        SavingsFilterChips(
            selectedFilter = state.selectedFilter,
            onFilterSelected = { filter -> onAction(MemberSavingsDetailAction.OnFilterSelected(filter)) },
        )
    }
}

/**
 * `MemberSavingsDetailScreenState.Loading` — 6 shimmer blocks mirroring
 * `preview/loading.html`'s `shimmer_list` (`ui.yaml#states.loading.components: [top_bar,
 * shimmer_list]` — no header/sparkline/filters yet, matching `ui.yaml`'s state-component list). See
 * API.md#screen.
 */
@Composable
internal fun MemberSavingsDetailLoadingSection(modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    Column(
        modifier = modifier.fillMaxSize().padding(sp.lg).testTag(MemberSavingsDetailTestTags.LOADING_SECTION),
        verticalArrangement = Arrangement.spacedBy(sp.sm),
    ) {
        repeat(6) { MemberSavingsDetailSkeletonBlock(height = 72.dp) }
    }
}

/** One shimmering placeholder block used by [MemberSavingsDetailLoadingSection] — purely decorative. */
@Composable
internal fun MemberSavingsDetailSkeletonBlock(height: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(MaterialTheme.spacing.sm))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

/**
 * `MemberSavingsDetailScreenState.Content` — [MemberSavingsDetailHeaderSection] followed by the
 * paginated [SavingsStatementRow] list. Scroll-to-end dispatches
 * [MemberSavingsDetailAction.OnLoadMore] via [rememberLoadMoreTrigger], driven by the REAL
 * `state.hasNextPage`/`state.isLoadingNextPage` paging-progress flags (unlike `loan-list`'s
 * permissive `hasMore = true` approximation — this feature's ViewModel surfaces both flags for
 * real). Tapping a row dispatches [MemberSavingsDetailAction.OnTransactionSelected], toggling
 * `state.expandedTransactionId` for the in-place expand. See API.md#screen.
 */
@Composable
internal fun MemberSavingsDetailContentSection(
    state: MemberSavingsDetailState,
    onAction: (MemberSavingsDetailAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val listState = rememberLazyListState()
    val shouldLoadMore by rememberLoadMoreTrigger(
        listState = listState,
        hasMore = state.hasNextPage,
        isLoadingMore = state.isLoadingNextPage,
    )
    val loadMoreCd = stringResource(Res.string.screens_member_savings_detail_load_more_cd)

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onAction(MemberSavingsDetailAction.OnLoadMore)
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().testTag(MemberSavingsDetailTestTags.TRANSACTION_LIST),
        contentPadding = PaddingValues(horizontal = sp.lg, vertical = sp.md),
        verticalArrangement = Arrangement.spacedBy(sp.md),
    ) {
        item(key = "header") {
            MemberSavingsDetailHeaderSection(state = state, onAction = onAction)
        }

        items(items = state.filteredTransactions, key = { it.id }) { entry ->
            SavingsStatementRow(
                entry = entry,
                isExpanded = state.expandedTransactionId == entry.id,
                onClick = { onAction(MemberSavingsDetailAction.OnTransactionSelected(entry.id)) },
                testTag = MemberSavingsDetailTestTags.transactionRowTag(entry.id),
                expandedTestTag = MemberSavingsDetailTestTags.transactionExpandedTag(entry.id),
            )
        }

        if (state.isLoadingNextPage) {
            item(key = "load_more") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(sp.md)
                        .semantics { contentDescription = loadMoreCd }
                        .testTag(MemberSavingsDetailTestTags.LOAD_MORE_INDICATOR),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

/**
 * `MemberSavingsDetailScreenState.Empty` — genuinely zero transactions for the active filter
 * (`ui.yaml#states.empty.description`: "No transactions match the active filter" —
 * [MemberSavingsDetailState.deriveScreenState] derives this from `filteredTransactions`, not the
 * unfiltered `transactions`). Header/sparkline/filter chips stay visible so the member can switch
 * filters without navigating away. See API.md#screen.
 */
@Composable
internal fun MemberSavingsDetailEmptySection(
    state: MemberSavingsDetailState,
    onAction: (MemberSavingsDetailAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val iconCd = stringResource(Res.string.screens_member_savings_detail_empty_icon_cd)
    val titleText = stringResource(Res.string.screens_member_savings_detail_empty_title)
    val bodyText = stringResource(Res.string.screens_member_savings_detail_empty_body)

    Column(modifier = modifier.fillMaxSize()) {
        MemberSavingsDetailHeaderSection(state = state, onAction = onAction, modifier = Modifier.padding(sp.lg))

        Column(
            modifier = Modifier.fillMaxSize().padding(sp.lg).testTag(MemberSavingsDetailTestTags.EMPTY_SECTION),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.AccountBalanceWallet,
                contentDescription = iconCd,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(80.dp).padding(bottom = sp.lg),
            )
            Text(text = titleText, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = bodyText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = sp.sm),
            )
        }
    }
}

/**
 * `MemberSavingsDetailScreenState.Error` — full-screen error surface (cloud_off icon, title,
 * resolved [MemberSavingsError.messageKey], Retry CTA shown only when the mapped error is
 * retryable), mirroring `preview/error.html` / `ui.yaml#states.error` (`components: [top_bar,
 * error_state]` only — no header/filters during Error). See API.md#screen.
 */
@Composable
internal fun MemberSavingsDetailErrorSection(
    state: MemberSavingsDetailState,
    onAction: (MemberSavingsDetailAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val errorIconCd = stringResource(Res.string.screens_member_savings_detail_error_icon_cd)
    val titleText = stringResource(Res.string.screens_member_savings_detail_error_title)
    val retryLabel = stringResource(Res.string.screens_member_savings_detail_action_retry)
    val message = when (state.error) {
        MemberSavingsError.Network, null -> stringResource(Res.string.screens_member_savings_detail_error_network)
        MemberSavingsError.Server -> stringResource(Res.string.screens_member_savings_detail_error_server)
        MemberSavingsError.NotFound -> stringResource(Res.string.screens_member_savings_detail_error_not_found)
        MemberSavingsError.Auth -> stringResource(Res.string.screens_member_savings_detail_error_auth)
    }
    val canRetry = state.error?.retry ?: true

    Column(
        modifier = modifier.fillMaxSize().padding(sp.lg).testTag(MemberSavingsDetailTestTags.ERROR_SECTION),
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
                onClick = { onAction(MemberSavingsDetailAction.Retry) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = sp.touchTargetMin)
                    .padding(top = sp.lg)
                    .testTag(MemberSavingsDetailTestTags.ERROR_RETRY_BUTTON),
            ) {
                Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
                Text(text = retryLabel, modifier = Modifier.padding(start = sp.xs))
            }
        }
    }
}
