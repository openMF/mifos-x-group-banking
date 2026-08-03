/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.loanlist

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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import org.koin.core.parameter.parametersOf
import org.mifos.groupbanking.core.model.LoanStatusFilter
import org.mifos.groupbanking.feature.loanlist.components.LoanListCard
import org.mifos.groupbanking.feature.loanlist.components.LoanListCardSkeleton
import org.mifos.groupbanking.feature.loanlist.components.LoanListFilterChips
import org.mifos.groupbanking.feature.loanlist.generated.resources.Res
import org.mifos.groupbanking.feature.loanlist.generated.resources.screens_loan_list_action_apply
import org.mifos.groupbanking.feature.loanlist.generated.resources.screens_loan_list_action_retry
import org.mifos.groupbanking.feature.loanlist.generated.resources.screens_loan_list_empty_body
import org.mifos.groupbanking.feature.loanlist.generated.resources.screens_loan_list_empty_icon_cd
import org.mifos.groupbanking.feature.loanlist.generated.resources.screens_loan_list_empty_title
import org.mifos.groupbanking.feature.loanlist.generated.resources.screens_loan_list_error_auth_message
import org.mifos.groupbanking.feature.loanlist.generated.resources.screens_loan_list_error_icon_cd
import org.mifos.groupbanking.feature.loanlist.generated.resources.screens_loan_list_error_network_message
import org.mifos.groupbanking.feature.loanlist.generated.resources.screens_loan_list_error_server_message
import org.mifos.groupbanking.feature.loanlist.generated.resources.screens_loan_list_error_title
import org.mifos.groupbanking.feature.loanlist.generated.resources.screens_loan_list_fab_cd
import org.mifos.groupbanking.feature.loanlist.generated.resources.screens_loan_list_loading_message
import org.mifos.groupbanking.feature.loanlist.generated.resources.screens_loan_list_title

/**
 * Container for `loan-list-screen`. Collects [LoanListViewModel] state via
 * [collectAsStateWithLifecycle], consumes one-shot [LoanListEvent]s (navigate to loan-detail /
 * loan-apply, show snackbar) through [EventsEffect], and delegates all rendering to the stateless
 * [LoanListContent]. [groupId] is the `ui.yaml#nav_params` value forwarded from
 * `group-dashboard`'s "Loans" entry point (or `bottom_nav`) — supplied to [LoanListViewModel] via
 * Koin `parametersOf(groupId)` (matching `LoanListModule`'s `viewModel { parameters -> ... }`
 * declaration). [onNavigateBack] is a PLAIN nav callback, NOT a [LoanListAction] dispatch —
 * `ui.yaml#components.top_bar.on_nav_click` wires an `OnBack` action, but
 * `state_model.actions.members` does NOT declare it (see [LoanListAction] class KDoc "Idea-layer
 * gap"); mirrors `GroupDashboardContent`'s identical "not a ViewModel action" precedent for its
 * top-bar overflow menu — the back icon pops the NavController stack directly rather than
 * inventing an unlisted action member (RULE-IMPL-DEAD-CLICKABLE-001 Rule 1). See API.md#screen.
 */
@Composable
internal fun LoanListScreen(
    groupId: Long,
    viewerRole: String,
    onNavigateToLoanDetail: (loanId: Long, viewerRole: String) -> Unit,
    onNavigateToLoanApply: (groupId: Long) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoanListViewModel = koinViewModel(parameters = { parametersOf(groupId, viewerRole) }),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val networkMessage = stringResource(Res.string.screens_loan_list_error_network_message)
    val serverMessage = stringResource(Res.string.screens_loan_list_error_server_message)
    val authMessage = stringResource(Res.string.screens_loan_list_error_auth_message)

    EventsEffect(viewModel) { event ->
        when (event) {
            is LoanListEvent.NavigateToLoanDetail -> onNavigateToLoanDetail(event.loanId, viewerRole)
            is LoanListEvent.NavigateToLoanApply -> onNavigateToLoanApply(event.groupId)
            is LoanListEvent.ShowSnackbar -> {
                val resolved = messageKeyToText(event.message, networkMessage, serverMessage, authMessage)
                snackbarHostState.showSnackbar(resolved)
            }
        }
    }

    LoanListContent(
        state = state,
        onAction = viewModel::trySendAction,
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

/** Resolves a [LoanListError.messageKey] to display text — see [LoanListScreen] KDoc. */
private fun messageKeyToText(key: String, network: String, server: String, auth: String): String = when (key) {
    "error_network" -> network
    "error_server" -> server
    "error_auth" -> auth
    else -> key
}

/**
 * Stateless render surface for `loan-list-screen`. State-driven per [LoanListState.screenState] —
 * every [LoanListScreenState] member is handled (Loading/Content/Error/Empty). [onNavigateBack] is
 * threaded through as a plain callback (see [LoanListScreen] KDoc) rather than folded into
 * [onAction] — every OTHER interactive element on this screen dispatches a real declared
 * [LoanListAction] member. See API.md#screen.
 */
@Composable
internal fun LoanListContent(
    state: LoanListState,
    onAction: (LoanListAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val title = stringResource(Res.string.screens_loan_list_title)
    val fabCd = stringResource(Res.string.screens_loan_list_fab_cd)

    KptScaffold(
        showNavigationIcon = true,
        onNavigationIconClick = onNavigateBack,
        title = title,
        floatingActionButtonContent = if (state.canApplyLoan) {
            FloatingActionButtonContent(
                onClick = { onAction(LoanListAction.OnApplyLoan) },
                contentColor = MaterialTheme.colorScheme.onPrimary,
                content = { LoanListFabContent(fabCd = fabCd) },
            )
        } else {
            null
        },
        pullToRefreshState = rememberKptPullToRefreshState(
            isEnabled = true,
            isRefreshing = state.isRefreshing,
            onRefresh = { onAction(LoanListAction.OnRefresh) },
        ),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.testTag(LoanListTestTags.SCREEN),
    ) {
        when (state.screenState) {
            LoanListScreenState.Loading -> LoanListLoadingSection(
                selectedFilter = state.selectedFilter,
                onAction = onAction,
            )

            LoanListScreenState.Content -> LoanListContentSection(state = state, onAction = onAction)

            LoanListScreenState.Empty -> LoanListEmptySection(
                selectedFilter = state.selectedFilter,
                onAction = onAction,
            )

            LoanListScreenState.Error -> LoanListErrorSection(
                message = when (state.error) {
                    LoanListError.Network, null -> stringResource(Res.string.screens_loan_list_error_network_message)
                    LoanListError.Server -> stringResource(Res.string.screens_loan_list_error_server_message)
                    LoanListError.Auth -> stringResource(Res.string.screens_loan_list_error_auth_message)
                },
                onRetry = { onAction(LoanListAction.Retry) },
            )
        }
    }
}

/** FAB content — icon + label, giving the plain FAB slot an extended-FAB look (the framework's
 * `FloatingActionButtonContent` has no dedicated extended variant, mirrors `GroupListFabContent`).
 * The merged [fabCd] semantics carry the accessible label; the icon is decorative. */
@Composable
internal fun LoanListFabContent(fabCd: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
        modifier = Modifier
            .testTag(LoanListTestTags.FAB_APPLY)
            .semantics { contentDescription = fabCd },
    ) {
        Icon(imageVector = Icons.Filled.Add, contentDescription = null)
        Text(text = stringResource(Res.string.screens_loan_list_action_apply))
    }
}

/**
 * `LoanListScreenState.Loading` — functional filter-chip row (bound, though the underlying list
 * is still empty) + 5 shimmering [LoanListCardSkeleton] rows behind a centered
 * [CircularProgressIndicator], mirroring `preview/loading.html` / `ui.yaml#states.loading`. See
 * API.md#screen.
 */
@Composable
internal fun LoanListLoadingSection(
    selectedFilter: LoanStatusFilter,
    onAction: (LoanListAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val loadingLabel = stringResource(Res.string.screens_loan_list_loading_message)

    Column(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = loadingLabel },
    ) {
        LoanListFilterChips(
            selectedFilter = selectedFilter,
            onFilterChange = { onAction(LoanListAction.OnFilterChange(it)) },
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = sp.lg),
            verticalArrangement = Arrangement.spacedBy(sp.md),
        ) {
            repeat(5) { LoanListCardSkeleton() }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.testTag(LoanListTestTags.LOADING_INDICATOR),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/**
 * `LoanListScreenState.Content` — filter-chip row + scrollable, paginated list of [LoanListCard]s.
 * Scroll-to-end dispatches [LoanListAction.OnLoadNextPage] via [rememberLoadMoreTrigger] —
 * `LoanListState` does not surface `hasMore`/`isLoadingMore` paging-progress flags (an idea-layer
 * gap already flagged on `LoanListViewModel.handleLoadNextPage`'s KDoc precedent from
 * `GroupListViewModel`), so this uses the permissive `hasMore = true, isLoadingMore = false`
 * approximation and relies on the underlying `PagingScreenStream` to no-op once exhausted. See
 * API.md#screen.
 */
@Composable
internal fun LoanListContentSection(state: LoanListState, onAction: (LoanListAction) -> Unit, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val listState = rememberLazyListState()
    val shouldLoadMore by rememberLoadMoreTrigger(listState = listState, hasMore = true, isLoadingMore = false)

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onAction(LoanListAction.OnLoadNextPage)
    }

    Column(modifier = modifier.fillMaxSize()) {
        LoanListFilterChips(
            selectedFilter = state.selectedFilter,
            onFilterChange = { onAction(LoanListAction.OnFilterChange(it)) },
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(horizontal = sp.lg).testTag(LoanListTestTags.LOAN_LIST),
            verticalArrangement = Arrangement.spacedBy(sp.md),
        ) {
            items(items = state.filteredLoans, key = { it.id }) { loan ->
                LoanListCard(
                    loan = loan,
                    onClick = { onAction(LoanListAction.OnLoanClick(loan.id)) },
                    testTag = LoanListTestTags.cardTag(loan.id),
                )
            }
        }
    }
}

/**
 * `LoanListScreenState.Empty` — genuinely zero loans for the selected filter
 * (`LoanListState.loans.isEmpty()`). Illustration + title + body; the "Apply for Loan" CTA is the
 * scaffold-level FAB (`ui.yaml#states.empty.components` includes `apply_loan_fab`, not a
 * duplicate in-body button), mirroring `ui.yaml`'s design (unlike `group-list`'s Empty state,
 * which has no equivalent FAB and so puts its CTAs in-body). See API.md#screen.
 */
@Composable
internal fun LoanListEmptySection(
    selectedFilter: LoanStatusFilter,
    onAction: (LoanListAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val iconCd = stringResource(Res.string.screens_loan_list_empty_icon_cd)
    val titleText = stringResource(Res.string.screens_loan_list_empty_title)
    val bodyText = stringResource(Res.string.screens_loan_list_empty_body)

    Column(modifier = modifier.fillMaxSize()) {
        LoanListFilterChips(
            selectedFilter = selectedFilter,
            onFilterChange = { onAction(LoanListAction.OnFilterChange(it)) },
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(sp.lg).testTag(LoanListTestTags.EMPTY_SECTION),
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
 * `LoanListScreenState.Error` — full-screen error surface (cloud_off icon, title, resolved
 * [message], Retry CTA), mirroring `preview/error.html` / `ui.yaml#states.error` (no filter chips
 * during Error — `ui.yaml#states.error.components` is `[top_bar, error_state]` only). See
 * API.md#screen.
 */
@Composable
internal fun LoanListErrorSection(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val errorIconCd = stringResource(Res.string.screens_loan_list_error_icon_cd)
    val titleText = stringResource(Res.string.screens_loan_list_error_title)
    val retryLabel = stringResource(Res.string.screens_loan_list_action_retry)

    Column(
        modifier = modifier.fillMaxSize().padding(sp.lg).testTag(LoanListTestTags.ERROR_SECTION),
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

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = sp.touchTargetMin)
                .padding(top = sp.lg)
                .testTag(LoanListTestTags.ERROR_RETRY_BUTTON),
        ) {
            Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
            Text(text = retryLabel, modifier = Modifier.padding(start = sp.xs))
        }
    }
}
