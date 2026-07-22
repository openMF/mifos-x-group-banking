/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.loandetail

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import org.mifos.groupbanking.core.model.LoanDetailTab
import org.mifos.groupbanking.core.model.RepaymentRowStatus
import org.mifos.groupbanking.feature.loandetail.components.LoanActionButtonsRow
import org.mifos.groupbanking.feature.loandetail.components.LoanDetailTabs
import org.mifos.groupbanking.feature.loandetail.components.LoanHeaderCard
import org.mifos.groupbanking.feature.loandetail.components.LoanOutstandingSummaryRow
import org.mifos.groupbanking.feature.loandetail.components.RepaymentHistoryEmptyState
import org.mifos.groupbanking.feature.loandetail.components.RepaymentScheduleHeaderRow
import org.mifos.groupbanking.feature.loandetail.components.RepaymentScheduleRowItem
import org.mifos.groupbanking.feature.loandetail.components.RepaymentTransactionRow
import org.mifos.groupbanking.feature.loandetail.generated.resources.Res
import org.mifos.groupbanking.feature.loandetail.generated.resources.screens_loan_detail_action_retry
import org.mifos.groupbanking.feature.loandetail.generated.resources.screens_loan_detail_error_auth
import org.mifos.groupbanking.feature.loandetail.generated.resources.screens_loan_detail_error_icon_cd
import org.mifos.groupbanking.feature.loandetail.generated.resources.screens_loan_detail_error_network
import org.mifos.groupbanking.feature.loandetail.generated.resources.screens_loan_detail_error_not_found
import org.mifos.groupbanking.feature.loandetail.generated.resources.screens_loan_detail_error_server
import org.mifos.groupbanking.feature.loandetail.generated.resources.screens_loan_detail_error_state_title
import org.mifos.groupbanking.feature.loandetail.generated.resources.screens_loan_detail_refresh_cd
import org.mifos.groupbanking.feature.loandetail.generated.resources.screens_loan_detail_title

/**
 * Container for `loan-detail-screen`. Collects [LoanDetailViewModel] state via
 * [collectAsStateWithLifecycle], consumes one-shot [LoanDetailEvent]s through [EventsEffect], and
 * delegates all rendering to the stateless [LoanDetailContent]. [loanId] is the `ui.yaml#nav_params`
 * value forwarded from `loan-list`'s "Loan card tap" entry point — supplied to
 * [LoanDetailViewModel] via Koin `parametersOf(loanId)` (matching `LoanDetailModule`'s single
 * `loanId` declaration).
 *
 * `loan-repayment-dialog` (`ui.yaml#action_buttons_row.content[].on_click.dialog`) and
 * `loan-mark-defaulted-dialog` (the sibling irreversible-confirm modal target) are both now
 * generated feature components (`feature/loan-repayment-dialog` / `feature/loan-mark-defaulted-dialog`).
 * [LoanDetailEvent.ShowRepaymentDialog] resolves the target loan's `memberId` + next-installment
 * amount from the currently-loaded [LoanDetailState] and forwards them via [onShowRepaymentDialog];
 * [LoanDetailEvent.ShowDefaultConfirmDialog] resolves `memberName` + `totalOutstanding` the same way
 * and forwards them via [onShowDefaultDialog] (see this Container's own KDoc on both parameters for
 * why the actual dialog composables are NOT rendered from inside this module — that seam lives at
 * `cmp-navigation`, see `GroupBankingNavHost.kt`'s class KDoc). See API.md#screen.
 */
@Composable
internal fun LoanDetailScreen(
    loanId: Long,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    // `feature/loan-detail` has no dependency on `feature/loan-repayment-dialog` (no feature<->
    // feature edge exists anywhere else in this codebase either — every cross-feature wire-up
    // happens at `cmp-navigation`, see `GroupBankingNavHost.kt`'s `repaymentDialogTarget` local
    // state). This callback is the seam: the Container resolves loanId/memberId/installmentAmount
    // from its own state and hands them upward; the caller (ultimately `GroupBankingNavHost.kt`)
    // owns actually rendering `LoanRepaymentDialog`. Default `= { _, _, _ -> }` is overridden by
    // both `loanDetailScreen()` (`LoanDetailRoute.kt`) and `GroupBankingNavHost.kt` — never left
    // dead (RULE-PROTO-COMPOSE-DEAD-CLICK-001 DC3 count-assertion).
    onShowRepaymentDialog: (loanId: Long, memberId: Long, installmentAmount: Double) -> Unit = { _, _, _ -> },
    // Sibling seam to onShowRepaymentDialog — see class KDoc "loan-repayment-dialog /
    // loan-mark-defaulted-dialog" note. Default `= { _, _, _ -> }` is overridden by both
    // `loanDetailScreen()` (`LoanDetailRoute.kt`) and `GroupBankingNavHost.kt` — never left dead
    // (RULE-PROTO-COMPOSE-DEAD-CLICK-001 DC3 count-assertion).
    onShowDefaultDialog: (loanId: Long, memberName: String, loanAmountKes: Double) -> Unit = { _, _, _ -> },
    viewModel: LoanDetailViewModel = koinViewModel(parameters = { parametersOf(loanId) }),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Precomputed OUTSIDE EventsEffect — stringResource() is @Composable-only and EventsEffect's
    // callback runs in a suspend (non-composable) scope, same convention as
    // GroupDashboardScreen.kt / MemberProfileScreen.kt.
    val networkMessage = stringResource(Res.string.screens_loan_detail_error_network)
    val serverMessage = stringResource(Res.string.screens_loan_detail_error_server)
    val notFoundMessage = stringResource(Res.string.screens_loan_detail_error_not_found)
    val authMessage = stringResource(Res.string.screens_loan_detail_error_auth)

    EventsEffect(viewModel) { event ->
        when (event) {
            LoanDetailEvent.NavigateBack -> onNavigateBack()
            LoanDetailEvent.ShowRepaymentDialog -> {
                val memberId = state.loan?.memberId ?: 0L
                // "Next installment" = the earliest still-owed schedule row (UPCOMING, else
                // OVERDUE); falls back to 0.0 when the schedule has nothing outstanding — the
                // dialog's own amount field is still freely editable either way.
                val installmentAmount = state.repaymentSchedule
                    .firstOrNull { it.status == RepaymentRowStatus.UPCOMING }
                    ?.dueAmount
                    ?: state.repaymentSchedule.firstOrNull { it.status == RepaymentRowStatus.OVERDUE }?.dueAmount
                    ?: 0.0
                onShowRepaymentDialog(loanId, memberId, installmentAmount)
            }
            LoanDetailEvent.ShowDefaultConfirmDialog -> {
                val memberName = state.loan?.memberName ?: ""
                val loanAmountKes = state.loan?.totalOutstanding ?: 0.0
                onShowDefaultDialog(loanId, memberName, loanAmountKes)
            }
            is LoanDetailEvent.ShowSnackbar -> snackbarHostState.showSnackbar(
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

    LoanDetailContent(
        state = state,
        onAction = viewModel::trySendAction,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

/**
 * Stateless render surface for `loan-detail-screen`. State-driven per
 * [LoanDetailState.screenState] — every [LoanDetailScreenState] member is handled
 * (Loading/Content/Error, this composite screen has no `Empty` variant). Pull-to-refresh is wired
 * at the [KptScaffold] level, dispatching [LoanDetailAction.OnRefresh]; the top-bar refresh icon
 * dispatches the same action. See API.md#screen.
 */
@Composable
internal fun LoanDetailContent(
    state: LoanDetailState,
    onAction: (LoanDetailAction) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val title = stringResource(Res.string.screens_loan_detail_title)
    val refreshCd = stringResource(Res.string.screens_loan_detail_refresh_cd)

    KptScaffold(
        onNavigationIconClick = { onAction(LoanDetailAction.OnBack) },
        title = title,
        actions = listOf(
            TopAppBarAction(
                icon = Icons.Filled.Refresh,
                contentDescription = refreshCd,
                onClick = { onAction(LoanDetailAction.OnRefresh) },
            ),
        ),
        pullToRefreshState = rememberKptPullToRefreshState(
            isEnabled = true,
            // LoanDetailState carries no separate `isRefreshing` flag — OnRefresh sets
            // `isLoading = true`, which swaps the whole screen to the Loading skeleton (matches
            // LoanDetailViewModel.handleRefresh); no distinct pull-spinner overlay signal exists
            // to bind here (flagged, not invented — mirrors GroupDashboardContent's identical gap).
            isRefreshing = false,
            onRefresh = { onAction(LoanDetailAction.OnRefresh) },
        ),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.testTag(LoanDetailTestTags.SCREEN),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (state.screenState) {
                LoanDetailScreenState.Loading -> LoanDetailLoadingSection()
                LoanDetailScreenState.Content -> LoanDetailContentSection(state = state, onAction = onAction)
                LoanDetailScreenState.Error -> LoanDetailErrorSection(state = state, onAction = onAction)
            }
        }
    }
}

/**
 * `LoanDetailScreenState.Loading` — 3 shimmer blocks mirroring `preview/loading.html`'s
 * `shimmer_detail` (`count: 3`, `height: 120dp`). See API.md#screen.
 */
@Composable
internal fun LoanDetailLoadingSection(modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    Column(
        modifier = modifier.fillMaxSize().padding(sp.lg).testTag(LoanDetailTestTags.LOADING_SECTION),
        verticalArrangement = Arrangement.spacedBy(sp.md),
    ) {
        LoanDetailSkeletonBlock(height = 120.dp)
        LoanDetailSkeletonBlock(height = 120.dp)
        LoanDetailSkeletonBlock(height = 120.dp)
    }
}

/** One shimmering placeholder block used by [LoanDetailLoadingSection] — purely decorative. */
@Composable
internal fun LoanDetailSkeletonBlock(height: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(MaterialTheme.spacing.sm))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

/**
 * `LoanDetailScreenState.Content` — [LoanHeaderCard] + [LoanOutstandingSummaryRow] +
 * [LoanDetailTabs] + tab-scoped body ([RepaymentScheduleHeaderRow] + weekly
 * [RepaymentScheduleRowItem] rows for SCHEDULE; [RepaymentTransactionRow] rows or
 * [RepaymentHistoryEmptyState] for HISTORY) + [LoanActionButtonsRow], mirroring
 * `preview/content.html`. Guards on `state.loan` being non-null — `handleStreamUpdated`'s
 * `ScreenState.Content` branch always populates it together with `isLoading = false`, so
 * `screenState == Content` implies it is set; this is a defensive no-render rather than a crash
 * if that invariant is ever violated. See API.md#screen.
 */
@Composable
internal fun LoanDetailContentSection(
    state: LoanDetailState,
    onAction: (LoanDetailAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val loan = state.loan ?: return
    val sp = MaterialTheme.spacing

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag(LoanDetailTestTags.CONTENT_LIST),
        verticalArrangement = Arrangement.spacedBy(sp.md),
        contentPadding = PaddingValues(sp.lg),
    ) {
        item { LoanHeaderCard(loan = loan) }
        item { LoanOutstandingSummaryRow(loan = loan) }
        item {
            LoanDetailTabs(
                selectedTab = state.selectedTab,
                onTabSelected = { tab -> onAction(LoanDetailAction.OnTabChange(tab)) },
            )
        }
        when (state.selectedTab) {
            LoanDetailTab.SCHEDULE -> {
                item { RepaymentScheduleHeaderRow(modifier = Modifier.testTag(LoanDetailTestTags.SCHEDULE_TABLE)) }
                items(state.repaymentSchedule, key = { it.weekNumber }) { row ->
                    RepaymentScheduleRowItem(row = row, testTag = LoanDetailTestTags.scheduleRowTag(row.weekNumber))
                }
            }
            LoanDetailTab.HISTORY -> {
                if (state.repaymentHistory.isEmpty()) {
                    item { RepaymentHistoryEmptyState() }
                } else {
                    items(state.repaymentHistory, key = { it.id }) { txn ->
                        RepaymentTransactionRow(txn = txn, testTag = LoanDetailTestTags.historyRowTag(txn.id))
                    }
                }
            }
        }
        item {
            LoanActionButtonsRow(
                loan = loan,
                canRecordRepayment = state.canRecordRepayment,
                canMarkDefaulted = state.canMarkDefaulted,
                onAction = onAction,
            )
        }
        item { Box(modifier = Modifier.height(sp.xl)) }
    }
}

/**
 * `LoanDetailScreenState.Error` — full-screen error surface (cloud_off icon, title, resolved
 * [LoanDetailError.messageKey], Retry CTA shown only when the mapped error is retryable), mirroring
 * `preview/error.html`. See API.md#screen.
 */
@Composable
internal fun LoanDetailErrorSection(
    state: LoanDetailState,
    onAction: (LoanDetailAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val errorIconCd = stringResource(Res.string.screens_loan_detail_error_icon_cd)
    val titleText = stringResource(Res.string.screens_loan_detail_error_state_title)
    val retryLabel = stringResource(Res.string.screens_loan_detail_action_retry)
    val message = when (state.error) {
        LoanDetailError.Network, null -> stringResource(Res.string.screens_loan_detail_error_network)
        LoanDetailError.Server -> stringResource(Res.string.screens_loan_detail_error_server)
        LoanDetailError.NotFound -> stringResource(Res.string.screens_loan_detail_error_not_found)
        LoanDetailError.Auth -> stringResource(Res.string.screens_loan_detail_error_auth)
    }
    val canRetry = state.error?.retry ?: true

    Column(
        modifier = modifier.fillMaxSize().padding(sp.lg).testTag(LoanDetailTestTags.ERROR_SECTION),
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
                onClick = { onAction(LoanDetailAction.Retry) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = sp.touchTargetMin)
                    .padding(top = sp.lg)
                    .testTag(LoanDetailTestTags.ERROR_RETRY_BUTTON),
            ) {
                Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
                Text(text = retryLabel, modifier = Modifier.padding(start = sp.xs))
            }
        }
    }
}
