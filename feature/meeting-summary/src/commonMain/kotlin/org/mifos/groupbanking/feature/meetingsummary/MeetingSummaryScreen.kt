/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.meetingsummary

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kpt.core.base.designsystem.core.TopAppBarAction
import kpt.core.base.ui.effects.EventsEffect
import kpt.core.common.formatGrouped
import kpt.core.designsystem.theme.spacing
import kpt.core.ui.scaffold.KptScaffold
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.mifos.groupbanking.core.model.MeetingSummaryData
import org.mifos.groupbanking.core.model.SavingsBreakdownItem
import org.mifos.groupbanking.feature.meetingsummary.generated.resources.Res
import org.mifos.groupbanking.feature.meetingsummary.generated.resources.screens_meeting_summary_action_retry
import org.mifos.groupbanking.feature.meetingsummary.generated.resources.screens_meeting_summary_attendance_label
import org.mifos.groupbanking.feature.meetingsummary.generated.resources.screens_meeting_summary_attendance_value_format
import org.mifos.groupbanking.feature.meetingsummary.generated.resources.screens_meeting_summary_closing_corpus_label
import org.mifos.groupbanking.feature.meetingsummary.generated.resources.screens_meeting_summary_corpus_chip_format
import org.mifos.groupbanking.feature.meetingsummary.generated.resources.screens_meeting_summary_corpus_reconciliation_header
import org.mifos.groupbanking.feature.meetingsummary.generated.resources.screens_meeting_summary_done_btn
import org.mifos.groupbanking.feature.meetingsummary.generated.resources.screens_meeting_summary_error_auth
import org.mifos.groupbanking.feature.meetingsummary.generated.resources.screens_meeting_summary_error_icon_cd
import org.mifos.groupbanking.feature.meetingsummary.generated.resources.screens_meeting_summary_error_network
import org.mifos.groupbanking.feature.meetingsummary.generated.resources.screens_meeting_summary_error_not_found
import org.mifos.groupbanking.feature.meetingsummary.generated.resources.screens_meeting_summary_error_server
import org.mifos.groupbanking.feature.meetingsummary.generated.resources.screens_meeting_summary_error_state_title
import org.mifos.groupbanking.feature.meetingsummary.generated.resources.screens_meeting_summary_fines_label
import org.mifos.groupbanking.feature.meetingsummary.generated.resources.screens_meeting_summary_group_savings_label
import org.mifos.groupbanking.feature.meetingsummary.generated.resources.screens_meeting_summary_individual_savings_label
import org.mifos.groupbanking.feature.meetingsummary.generated.resources.screens_meeting_summary_kes_amount_format
import org.mifos.groupbanking.feature.meetingsummary.generated.resources.screens_meeting_summary_loans_disbursed_label
import org.mifos.groupbanking.feature.meetingsummary.generated.resources.screens_meeting_summary_meeting_date_format
import org.mifos.groupbanking.feature.meetingsummary.generated.resources.screens_meeting_summary_member_savings_row_format
import org.mifos.groupbanking.feature.meetingsummary.generated.resources.screens_meeting_summary_net_change_label
import org.mifos.groupbanking.feature.meetingsummary.generated.resources.screens_meeting_summary_opening_corpus_label
import org.mifos.groupbanking.feature.meetingsummary.generated.resources.screens_meeting_summary_repayments_label
import org.mifos.groupbanking.feature.meetingsummary.generated.resources.screens_meeting_summary_savings_breakdown_header
import org.mifos.groupbanking.feature.meetingsummary.generated.resources.screens_meeting_summary_share_btn_description
import org.mifos.groupbanking.feature.meetingsummary.generated.resources.screens_meeting_summary_share_copied
import org.mifos.groupbanking.feature.meetingsummary.generated.resources.screens_meeting_summary_title
import org.mifos.groupbanking.feature.meetingsummary.generated.resources.screens_meeting_summary_title_format
import org.mifos.groupbanking.feature.meetingsummary.generated.resources.screens_meeting_summary_total_collected_label

/**
 * Container for `meeting-summary-screen`. Collects [MeetingSummaryViewModel] state via
 * [collectAsStateWithLifecycle], consumes one-shot [MeetingSummaryEvent]s through [EventsEffect], and
 * delegates all rendering to the stateless [MeetingSummaryContent]. [centerId] / [meetingNumber] /
 * [meetingId] are the `ui.yaml#nav_params` values — supplied to [MeetingSummaryViewModel] via Koin
 * `parametersOf(...)` (matching `MeetingSummaryModule`).
 *
 * [MeetingSummaryEvent.ShareSummary] copies the composed report text to the system clipboard (the
 * shipped, cross-platform real behavior — the OS `ShareSheet` seam is a flagged follow-up, no share
 * infra exists in-tree) and confirms via a snackbar. See API.md#screen.
 */
@Composable
internal fun MeetingSummaryScreen(
    meetingId: String,
    meetingNumber: Int,
    centerId: Int,
    onNavigateDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MeetingSummaryViewModel = koinViewModel(
        parameters = { parametersOf(centerId, meetingNumber, meetingId) },
    ),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current
    val shareCopiedMessage = stringResource(Res.string.screens_meeting_summary_share_copied)

    EventsEffect(viewModel) { event ->
        when (event) {
            MeetingSummaryEvent.NavigateToCalendar -> onNavigateDone()
            is MeetingSummaryEvent.ShareSummary -> {
                clipboardManager.setText(AnnotatedString(event.reportText))
                snackbarHostState.showSnackbar(message = shareCopiedMessage)
            }
        }
    }

    MeetingSummaryContent(
        state = state,
        onAction = viewModel::trySendAction,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

/**
 * Stateless render surface for `meeting-summary-screen`. State-driven per
 * [MeetingSummaryState.screenState] — every [MeetingSummaryScreenState] member is handled
 * (Loading / Content / Error). Top-bar carries a Share action; the whole screen is read-only. See
 * API.md#screen.
 */
@Composable
internal fun MeetingSummaryContent(
    state: MeetingSummaryState,
    onAction: (MeetingSummaryAction) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val title = state.meetingSummary?.let {
        stringResource(Res.string.screens_meeting_summary_title_format, it.meetingNumber)
    } ?: stringResource(Res.string.screens_meeting_summary_title)
    val shareCd = stringResource(Res.string.screens_meeting_summary_share_btn_description)

    KptScaffold(
        onNavigationIconClick = { onAction(MeetingSummaryAction.NavigateDone) },
        title = title,
        actions = listOf(
            TopAppBarAction(
                icon = Icons.Filled.Share,
                contentDescription = shareCd,
                onClick = { onAction(MeetingSummaryAction.ShareMeetingReport) },
            ),
        ),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.testTag(MeetingSummaryTestTags.SCREEN),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (state.screenState) {
                MeetingSummaryScreenState.Loading -> MeetingSummaryLoadingSection()
                MeetingSummaryScreenState.Content -> MeetingSummaryContentSection(state = state, onAction = onAction)
                MeetingSummaryScreenState.Error -> MeetingSummaryErrorSection(state = state, onAction = onAction)
            }
        }
    }
}

/** `MeetingSummaryScreenState.Loading` — 5 shimmer blocks mirroring `ui.yaml#loading_skeleton`. */
@Composable
internal fun MeetingSummaryLoadingSection(modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    Column(
        modifier = modifier.fillMaxSize().padding(sp.lg).testTag(MeetingSummaryTestTags.LOADING_SECTION),
        verticalArrangement = Arrangement.spacedBy(sp.md),
    ) {
        repeat(5) { MeetingSummarySkeletonBlock(height = 80.dp) }
    }
}

/** One shimmering placeholder block used by [MeetingSummaryLoadingSection] — purely decorative. */
@Composable
internal fun MeetingSummarySkeletonBlock(height: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(MaterialTheme.spacing.md))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

/**
 * `MeetingSummaryScreenState.Content` — hero KES-collected card, 2-column metric grid, savings
 * breakdown list, corpus reconciliation card, and the Done CTA, mirroring `ui.yaml#states.content`.
 * Guards on `state.meetingSummary` being non-null — `handleStreamUpdated`'s `Content` branch always
 * populates it together with `isLoading = false`. See API.md#screen.
 */
@Composable
internal fun MeetingSummaryContentSection(
    state: MeetingSummaryState,
    onAction: (MeetingSummaryAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = state.meetingSummary ?: return
    val sp = MaterialTheme.spacing

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag(MeetingSummaryTestTags.CONTENT_LIST),
        verticalArrangement = Arrangement.spacedBy(sp.md),
        contentPadding = PaddingValues(sp.lg),
    ) {
        item { MeetingSummaryHeroCard(summary = summary) }
        item { MeetingSummaryMetricGrid(summary = summary) }
        item { MeetingSummarySavingsBreakdown(rows = summary.savingsBreakdown) }
        item { MeetingSummaryCorpusReconciliation(summary = summary) }
        item {
            Button(
                onClick = { onAction(MeetingSummaryAction.NavigateDone) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = sp.touchTargetMin)
                    .testTag(MeetingSummaryTestTags.DONE_BUTTON),
            ) {
                Text(text = stringResource(Res.string.screens_meeting_summary_done_btn))
            }
        }
        item { Box(modifier = Modifier.height(sp.xl)) }
    }
}

/** Hero card — total collected (large), meeting date, and the closing-corpus chip. */
@Composable
internal fun MeetingSummaryHeroCard(summary: MeetingSummaryData, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    Card(
        modifier = modifier.fillMaxWidth().testTag(MeetingSummaryTestTags.HERO_CARD),
        shape = RoundedCornerShape(sp.lg),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(sp.lg), verticalArrangement = Arrangement.spacedBy(sp.xs)) {
            Text(
                text = stringResource(Res.string.screens_meeting_summary_total_collected_label),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = stringResource(
                    Res.string.screens_meeting_summary_kes_amount_format,
                    summary.totalSavingsCollected.formatGrouped(),
                ),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(
                    Res.string.screens_meeting_summary_meeting_date_format,
                    summary.meetingNumber,
                    summary.actualDate,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(sp.md),
                modifier = Modifier.padding(top = sp.sm).testTag(MeetingSummaryTestTags.CORPUS_CHIP),
            ) {
                Text(
                    text = stringResource(
                        Res.string.screens_meeting_summary_corpus_chip_format,
                        summary.closingCorpus.formatGrouped(),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = sp.md, vertical = sp.xs),
                )
            }
        }
    }
}

/** 2-column metric grid — attendance, group/individual savings, repayments, fines, disbursed. */
@Composable
internal fun MeetingSummaryMetricGrid(summary: MeetingSummaryData, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val metrics: List<Pair<String, String>> = listOf(
        stringResource(Res.string.screens_meeting_summary_attendance_label) to
            stringResource(
                Res.string.screens_meeting_summary_attendance_value_format,
                summary.attendanceCount,
                summary.totalMemberCount,
            ),
        stringResource(Res.string.screens_meeting_summary_group_savings_label) to
            stringResource(Res.string.screens_meeting_summary_kes_amount_format, summary.groupSavingsCollected.formatGrouped()),
        stringResource(Res.string.screens_meeting_summary_individual_savings_label) to
            stringResource(Res.string.screens_meeting_summary_kes_amount_format, summary.individualSavingsCollected.formatGrouped()),
        stringResource(Res.string.screens_meeting_summary_repayments_label) to
            stringResource(Res.string.screens_meeting_summary_kes_amount_format, summary.loansRepaid.formatGrouped()),
        stringResource(Res.string.screens_meeting_summary_fines_label) to
            stringResource(Res.string.screens_meeting_summary_kes_amount_format, summary.finesCollected.formatGrouped()),
        stringResource(Res.string.screens_meeting_summary_loans_disbursed_label) to
            stringResource(Res.string.screens_meeting_summary_kes_amount_format, summary.loansDisbursed.formatGrouped()),
    )
    Column(
        modifier = modifier.fillMaxWidth().testTag(MeetingSummaryTestTags.METRIC_GRID),
        verticalArrangement = Arrangement.spacedBy(sp.sm),
    ) {
        metrics.chunked(2).forEach { rowMetrics ->
            Row(horizontalArrangement = Arrangement.spacedBy(sp.sm), modifier = Modifier.fillMaxWidth()) {
                rowMetrics.forEach { (label, value) ->
                    MeetingMetricCard(label = label, value = value, modifier = Modifier.weight(1f))
                }
                if (rowMetrics.size == 1) Box(modifier = Modifier.weight(1f))
            }
        }
    }
}

/** One metric tile — label + value on a container-tinted card. */
@Composable
internal fun MeetingMetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(sp.md),
    ) {
        Column(modifier = Modifier.padding(sp.md), verticalArrangement = Arrangement.spacedBy(sp.xs)) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Savings breakdown — header + one row per member (avatar initials + group/individual + total). */
@Composable
internal fun MeetingSummarySavingsBreakdown(
    rows: List<SavingsBreakdownItem>,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    Column(modifier = modifier.fillMaxWidth().testTag(MeetingSummaryTestTags.SAVINGS_BREAKDOWN_SECTION)) {
        Text(
            text = stringResource(Res.string.screens_meeting_summary_savings_breakdown_header),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = sp.sm),
        )
        rows.forEach { row ->
            MeetingSavingsRow(row = row)
            HorizontalDivider()
        }
    }
}

/** One savings-breakdown row for a member. */
@Composable
internal fun MeetingSavingsRow(row: SavingsBreakdownItem, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(vertical = sp.sm)
            .testTag(MeetingSummaryTestTags.savingsRowTag(row.memberId)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = row.memberName, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(
                    Res.string.screens_meeting_summary_member_savings_row_format,
                    row.groupSavings.formatGrouped(),
                    row.individualSavings.formatGrouped(),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(Res.string.screens_meeting_summary_kes_amount_format, row.totalSavings.formatGrouped()),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/** Corpus reconciliation — opening, closing, and net-change info rows on a tertiary card. */
@Composable
internal fun MeetingSummaryCorpusReconciliation(summary: MeetingSummaryData, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    Card(
        modifier = modifier.fillMaxWidth().testTag(MeetingSummaryTestTags.CORPUS_RECONCILIATION_SECTION),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
        shape = RoundedCornerShape(sp.md),
    ) {
        Column(modifier = Modifier.padding(sp.md), verticalArrangement = Arrangement.spacedBy(sp.xs)) {
            Text(
                text = stringResource(Res.string.screens_meeting_summary_corpus_reconciliation_header),
                style = MaterialTheme.typography.titleSmall,
            )
            CorpusInfoRow(
                label = stringResource(Res.string.screens_meeting_summary_opening_corpus_label),
                value = stringResource(Res.string.screens_meeting_summary_kes_amount_format, summary.openingCorpus.formatGrouped()),
            )
            CorpusInfoRow(
                label = stringResource(Res.string.screens_meeting_summary_closing_corpus_label),
                value = stringResource(Res.string.screens_meeting_summary_kes_amount_format, summary.closingCorpus.formatGrouped()),
                emphasize = true,
            )
            CorpusInfoRow(
                label = stringResource(Res.string.screens_meeting_summary_net_change_label),
                value = stringResource(Res.string.screens_meeting_summary_kes_amount_format, summary.netCorpusChange.formatGrouped()),
                valueColor = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun CorpusInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    emphasize: Boolean = false,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onTertiaryContainer,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = valueColor,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

/**
 * `MeetingSummaryScreenState.Error` — full-screen error surface (cloud_off icon, title, resolved
 * [MeetingSummaryError.messageKey], Retry CTA shown only when the mapped error is retryable),
 * mirroring `ui.yaml#states.error`. See API.md#screen.
 */
@Composable
internal fun MeetingSummaryErrorSection(
    state: MeetingSummaryState,
    onAction: (MeetingSummaryAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val errorIconCd = stringResource(Res.string.screens_meeting_summary_error_icon_cd)
    val titleText = stringResource(Res.string.screens_meeting_summary_error_state_title)
    val retryLabel = stringResource(Res.string.screens_meeting_summary_action_retry)
    val message = when (state.error) {
        MeetingSummaryError.Network, null -> stringResource(Res.string.screens_meeting_summary_error_network)
        MeetingSummaryError.Server -> stringResource(Res.string.screens_meeting_summary_error_server)
        MeetingSummaryError.NotFound -> stringResource(Res.string.screens_meeting_summary_error_not_found)
        MeetingSummaryError.Auth -> stringResource(Res.string.screens_meeting_summary_error_auth)
    }
    val canRetry = state.error?.retry ?: true

    Column(
        modifier = modifier.fillMaxSize().padding(sp.lg).testTag(MeetingSummaryTestTags.ERROR_SECTION),
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
                onClick = { onAction(MeetingSummaryAction.Retry) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = sp.touchTargetMin)
                    .padding(top = sp.lg)
                    .testTag(MeetingSummaryTestTags.ERROR_RETRY_BUTTON),
            ) {
                Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
                Text(text = retryLabel, modifier = Modifier.padding(start = sp.xs))
            }
        }
    }
}
