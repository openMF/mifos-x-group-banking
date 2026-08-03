/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
@file:OptIn(ExperimentalMaterial3Api::class)

package org.mifos.groupbanking.feature.syncstatus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kpt.core.base.designsystem.component.KptTopAppBar
import kpt.core.base.designsystem.core.KptTopAppBarConfiguration
import kpt.core.base.ui.effects.EventsEffect
import kpt.core.common.formatTimeAgo
import kpt.core.designsystem.theme.spacing
import kpt.core.ui.scaffold.KptScaffold
import kpt.core.ui.scaffold.rememberKptPullToRefreshState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.mifos.groupbanking.feature.syncstatus.components.SyncNowButton
import org.mifos.groupbanking.feature.syncstatus.components.SyncStatusConflictChip
import org.mifos.groupbanking.feature.syncstatus.components.SyncStatusEmptyState
import org.mifos.groupbanking.feature.syncstatus.components.SyncStatusFailedOpsSection
import org.mifos.groupbanking.feature.syncstatus.components.SyncStatusOverallCard
import org.mifos.groupbanking.feature.syncstatus.components.SyncStatusPendingBreakdownCard
import org.mifos.groupbanking.feature.syncstatus.components.SyncStatusShimmer
import org.mifos.groupbanking.feature.syncstatus.generated.resources.Res
import org.mifos.groupbanking.feature.syncstatus.generated.resources.screens_sync_status_all_synced_body
import org.mifos.groupbanking.feature.syncstatus.generated.resources.screens_sync_status_all_synced_icon_cd
import org.mifos.groupbanking.feature.syncstatus.generated.resources.screens_sync_status_all_synced_title
import org.mifos.groupbanking.feature.syncstatus.generated.resources.screens_sync_status_conflict_chip_cd
import org.mifos.groupbanking.feature.syncstatus.generated.resources.screens_sync_status_conflict_label
import org.mifos.groupbanking.feature.syncstatus.generated.resources.screens_sync_status_entity_count_cd
import org.mifos.groupbanking.feature.syncstatus.generated.resources.screens_sync_status_error_db
import org.mifos.groupbanking.feature.syncstatus.generated.resources.screens_sync_status_error_sync_failed
import org.mifos.groupbanking.feature.syncstatus.generated.resources.screens_sync_status_failed_title
import org.mifos.groupbanking.feature.syncstatus.generated.resources.screens_sync_status_last_sync_label
import org.mifos.groupbanking.feature.syncstatus.generated.resources.screens_sync_status_never_synced
import org.mifos.groupbanking.feature.syncstatus.generated.resources.screens_sync_status_pending_title
import org.mifos.groupbanking.feature.syncstatus.generated.resources.screens_sync_status_retry
import org.mifos.groupbanking.feature.syncstatus.generated.resources.screens_sync_status_retry_cd
import org.mifos.groupbanking.feature.syncstatus.generated.resources.screens_sync_status_shimmer_cd
import org.mifos.groupbanking.feature.syncstatus.generated.resources.screens_sync_status_status_failed
import org.mifos.groupbanking.feature.syncstatus.generated.resources.screens_sync_status_status_icon_cd
import org.mifos.groupbanking.feature.syncstatus.generated.resources.screens_sync_status_status_pending
import org.mifos.groupbanking.feature.syncstatus.generated.resources.screens_sync_status_status_synced
import org.mifos.groupbanking.feature.syncstatus.generated.resources.screens_sync_status_sync_now
import org.mifos.groupbanking.feature.syncstatus.generated.resources.screens_sync_status_sync_now_cd
import org.mifos.groupbanking.feature.syncstatus.generated.resources.screens_sync_status_sync_now_offline
import org.mifos.groupbanking.feature.syncstatus.generated.resources.screens_sync_status_title
import org.mifos.groupbanking.feature.syncstatus.generated.resources.screens_sync_status_top_bar_cd

/**
 * Container for `sync-status-screen` (`ui.yaml#route`: `/sync-status`, `ui.yaml#nav_params: {}`).
 * Collects [SyncStatusViewModel] state via [collectAsStateWithLifecycle], consumes the one-shot
 * [SyncStatusEvent]s through [EventsEffect] (`ShowSnackbar` resolves the `messageKey` to a
 * localized string; `SyncCompleted` is a documented no-op — see [SyncStatusViewModel] class KDoc
 * "Sync-success snackbar"), and delegates all rendering to the stateless [SyncStatusContent]. This
 * is a **terminal screen with no outbound navigation** (`ui.yaml#screens[0].description`) — no
 * back icon in `ui.yaml#components.top_bar`, no `NavigateBack` member in
 * `ui.yaml#state_model.events.members` — so no `onNavigateBack` callback is declared here (adding
 * one with a bare `= {}` default and no wired UI element would itself be a dead-clickable-adjacent
 * violation per RULE-IMPL-DEAD-CLICKABLE-001). See API.md#screen.
 */
@Composable
internal fun SyncStatusScreen(
    modifier: Modifier = Modifier,
    viewModel: SyncStatusViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Precomputed OUTSIDE EventsEffect — stringResource() is @Composable-only and EventsEffect's
    // callback runs in a suspend (non-composable) scope, same convention as GroupDashboardScreen.kt.
    val errorDbMessage = stringResource(Res.string.screens_sync_status_error_db)
    val errorSyncFailedMessage = stringResource(Res.string.screens_sync_status_error_sync_failed)

    EventsEffect(viewModel) { event ->
        when (event) {
            is SyncStatusEvent.ShowSnackbar -> snackbarHostState.showSnackbar(
                message = when (event.message) {
                    "error_db" -> errorDbMessage
                    "error_sync_failed" -> errorSyncFailedMessage
                    else -> event.message
                },
            )
            // No dedicated success copy key is declared in `ui.yaml#i18n` — see
            // SyncStatusViewModel class KDoc "Sync-success snackbar" documented gap. The
            // overall-status card already flips to SYNCED from the live combine() re-emission,
            // so this is an honest no-op rather than a fabricated English literal.
            SyncStatusEvent.SyncCompleted -> Unit
        }
    }

    SyncStatusContent(
        state = state,
        onAction = viewModel::trySendAction,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

/**
 * Stateless render surface for `sync-status-screen`. State-driven per
 * [SyncStatusState.deriveScreenState] — every [SyncStatusScreenState] member is handled
 * (Loading/Content/Error). Pull-to-refresh is wired at the [KptScaffold] level across every
 * screenState, dispatching [SyncStatusAction.OnRefresh]. See API.md#screen.
 */
@Composable
fun SyncStatusContent(
    state: SyncStatusState,
    onAction: (SyncStatusAction) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val screenState = state.deriveScreenState()
    val title = stringResource(Res.string.screens_sync_status_title)
    val topBarCd = stringResource(Res.string.screens_sync_status_top_bar_cd)

    KptScaffold(
        modifier = modifier.testTag(SyncStatusTestTags.SCREEN),
        topBar = {
            KptTopAppBar(
                KptTopAppBarConfiguration(
                    title = title,
                    navigationIcon = null,
                    testTag = SyncStatusTestTags.TOP_BAR,
                    contentDescription = topBarCd,
                ),
            )
        },
        snackbarHostState = snackbarHostState,
        pullToRefreshState = rememberKptPullToRefreshState(
            isEnabled = true,
            // SyncStatusState carries no separate `isRefreshing` flag — OnRefresh pulses
            // `isLoading` true→false, swapping the whole screen to the Loading shimmer; no
            // distinct pull-spinner overlay signal exists to bind here (flagged, not invented —
            // mirrors GroupDashboardContent's identical documented gap).
            isRefreshing = false,
            onRefresh = { onAction(SyncStatusAction.OnRefresh) },
        ),
    ) {
        when (screenState) {
            SyncStatusScreenState.Loading -> {
                val shimmerCd = stringResource(Res.string.screens_sync_status_shimmer_cd)
                SyncStatusShimmer(accessibilityLabel = shimmerCd)
            }
            SyncStatusScreenState.Content -> SyncStatusContentBody(state = state, onAction = onAction)
            SyncStatusScreenState.Error -> SyncStatusErrorBody(state = state)
        }
    }
}

/**
 * `SyncStatusScreenState.Content` — `ui.yaml#states.content.components`: overall-status card,
 * optional conflict chip, optional pending-breakdown card, the Sync Now button, optional
 * failed-operations section, optional all-synced empty state. See API.md#screen.
 */
@Composable
internal fun SyncStatusContentBody(
    state: SyncStatusState,
    onAction: (SyncStatusAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing

    val statusIconCd = stringResource(Res.string.screens_sync_status_status_icon_cd)
    val syncedLabel = stringResource(Res.string.screens_sync_status_status_synced)
    val pendingLabel = stringResource(Res.string.screens_sync_status_status_pending)
    val failedLabel = stringResource(Res.string.screens_sync_status_status_failed)
    val lastSyncText = formatTimeAgo(state.lastSyncAt)?.let {
        stringResource(Res.string.screens_sync_status_last_sync_label, it)
    } ?: stringResource(Res.string.screens_sync_status_never_synced)

    val conflictLabel = stringResource(Res.string.screens_sync_status_conflict_label, state.conflictCount)
    val conflictChipCd = stringResource(Res.string.screens_sync_status_conflict_chip_cd)

    val pendingTitle = stringResource(Res.string.screens_sync_status_pending_title, state.pendingCount)
    val entityCountCd = stringResource(Res.string.screens_sync_status_entity_count_cd)

    val syncNowLabel = if (state.isOnline) {
        stringResource(Res.string.screens_sync_status_sync_now)
    } else {
        stringResource(Res.string.screens_sync_status_sync_now_offline)
    }
    val syncNowCd = stringResource(Res.string.screens_sync_status_sync_now_cd)

    val failedTitle = stringResource(Res.string.screens_sync_status_failed_title, state.failedCount)
    val retryLabel = stringResource(Res.string.screens_sync_status_retry)
    val retryCd = stringResource(Res.string.screens_sync_status_retry_cd)

    val allSyncedIconCd = stringResource(Res.string.screens_sync_status_all_synced_icon_cd)
    val allSyncedTitle = stringResource(Res.string.screens_sync_status_all_synced_title)
    val allSyncedBody = stringResource(Res.string.screens_sync_status_all_synced_body)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = sp.lg, vertical = sp.md),
        verticalArrangement = Arrangement.spacedBy(sp.md),
    ) {
        SyncStatusOverallCard(
            overallStatus = state.overallStatus,
            statusIconContentDescription = statusIconCd,
            syncedLabel = syncedLabel,
            pendingLabel = pendingLabel,
            failedLabel = failedLabel,
            lastSyncText = lastSyncText,
        )

        if (state.conflictCount > 0) {
            SyncStatusConflictChip(label = conflictLabel, accessibilityLabel = conflictChipCd)
        }

        if (state.pendingCount > 0) {
            SyncStatusPendingBreakdownCard(
                titleText = pendingTitle,
                pendingByType = state.pendingByType,
                entityCountAccessibilityLabel = entityCountCd,
            )
        }

        SyncNowButton(
            label = syncNowLabel,
            contentDescription = syncNowCd,
            enabled = state.isOnline && !state.isSyncing,
            isSyncing = state.isSyncing,
            onClick = { onAction(SyncStatusAction.OnSyncNow) },
        )

        if (state.failedCount > 0) {
            SyncStatusFailedOpsSection(
                titleText = failedTitle,
                failedOperations = state.failedOperations,
                retryLabel = retryLabel,
                retryAccessibilityLabel = retryCd,
                onRetryClick = { itemId -> onAction(SyncStatusAction.OnRetryOperation(itemId)) },
            )
        }

        if (state.pendingCount == 0 && state.failedCount == 0) {
            SyncStatusEmptyState(
                iconContentDescription = allSyncedIconCd,
                title = allSyncedTitle,
                body = allSyncedBody,
            )
        }
    }
}

/**
 * `SyncStatusScreenState.Error` — `ui.yaml#states.error.components: [top_bar, overall_status_card]`.
 * A local `sync_queue` DB read failed; [SyncStatusOverallCard]'s `dbErrorMessage` param forces
 * FAILED-red styling with the real [SyncStatusError.DbRead] message rather than a stale/misleading
 * status card. See API.md#screen.
 */
@Composable
internal fun SyncStatusErrorBody(state: SyncStatusState, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val statusIconCd = stringResource(Res.string.screens_sync_status_status_icon_cd)
    val syncedLabel = stringResource(Res.string.screens_sync_status_status_synced)
    val pendingLabel = stringResource(Res.string.screens_sync_status_status_pending)
    val failedLabel = stringResource(Res.string.screens_sync_status_status_failed)
    val errorDbMessage = stringResource(Res.string.screens_sync_status_error_db)

    Column(
        modifier = modifier.fillMaxSize().padding(sp.lg),
    ) {
        SyncStatusOverallCard(
            overallStatus = state.overallStatus,
            statusIconContentDescription = statusIconCd,
            syncedLabel = syncedLabel,
            pendingLabel = pendingLabel,
            failedLabel = failedLabel,
            lastSyncText = "",
            dbErrorMessage = errorDbMessage,
        )
    }
}
