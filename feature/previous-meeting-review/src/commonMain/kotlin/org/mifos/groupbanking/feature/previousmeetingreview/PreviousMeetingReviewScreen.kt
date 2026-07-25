/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.previousmeetingreview

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kpt.core.base.ui.effects.EventsEffect
import kpt.core.common.formatGrouped
import kpt.core.designsystem.theme.spacing
import kpt.core.ui.scaffold.KptScaffold
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.mifos.groupbanking.core.model.AttendanceRecord
import org.mifos.groupbanking.core.model.AttendanceStatus
import org.mifos.groupbanking.core.model.LoanSummaryItem
import org.mifos.groupbanking.core.model.PreviousMeetingDetail
import org.mifos.groupbanking.core.model.SavingsBreakdownItem
import org.mifos.groupbanking.core.model.UnresolvedItem
import org.mifos.groupbanking.feature.previousmeetingreview.generated.resources.Res
import org.mifos.groupbanking.feature.previousmeetingreview.generated.resources.screens_previous_meeting_review_action_retry
import org.mifos.groupbanking.feature.previousmeetingreview.generated.resources.screens_previous_meeting_review_attendance_chip_a11y
import org.mifos.groupbanking.feature.previousmeetingreview.generated.resources.screens_previous_meeting_review_attendance_chip_format
import org.mifos.groupbanking.feature.previousmeetingreview.generated.resources.screens_previous_meeting_review_attendance_fine_format
import org.mifos.groupbanking.feature.previousmeetingreview.generated.resources.screens_previous_meeting_review_attendance_section_header
import org.mifos.groupbanking.feature.previousmeetingreview.generated.resources.screens_previous_meeting_review_closing_corpus_label
import org.mifos.groupbanking.feature.previousmeetingreview.generated.resources.screens_previous_meeting_review_context_calendar_format
import org.mifos.groupbanking.feature.previousmeetingreview.generated.resources.screens_previous_meeting_review_context_conduct_format
import org.mifos.groupbanking.feature.previousmeetingreview.generated.resources.screens_previous_meeting_review_error_auth
import org.mifos.groupbanking.feature.previousmeetingreview.generated.resources.screens_previous_meeting_review_error_icon_cd
import org.mifos.groupbanking.feature.previousmeetingreview.generated.resources.screens_previous_meeting_review_error_network
import org.mifos.groupbanking.feature.previousmeetingreview.generated.resources.screens_previous_meeting_review_error_not_found
import org.mifos.groupbanking.feature.previousmeetingreview.generated.resources.screens_previous_meeting_review_error_server
import org.mifos.groupbanking.feature.previousmeetingreview.generated.resources.screens_previous_meeting_review_error_state_title
import org.mifos.groupbanking.feature.previousmeetingreview.generated.resources.screens_previous_meeting_review_fines_label
import org.mifos.groupbanking.feature.previousmeetingreview.generated.resources.screens_previous_meeting_review_kes_amount_format
import org.mifos.groupbanking.feature.previousmeetingreview.generated.resources.screens_previous_meeting_review_loan_disbursed_chip_format
import org.mifos.groupbanking.feature.previousmeetingreview.generated.resources.screens_previous_meeting_review_loan_section_header
import org.mifos.groupbanking.feature.previousmeetingreview.generated.resources.screens_previous_meeting_review_loan_supporting_format
import org.mifos.groupbanking.feature.previousmeetingreview.generated.resources.screens_previous_meeting_review_loans_disbursed_label
import org.mifos.groupbanking.feature.previousmeetingreview.generated.resources.screens_previous_meeting_review_savings_section_header
import org.mifos.groupbanking.feature.previousmeetingreview.generated.resources.screens_previous_meeting_review_savings_supporting_format
import org.mifos.groupbanking.feature.previousmeetingreview.generated.resources.screens_previous_meeting_review_start_meeting_btn_format
import org.mifos.groupbanking.feature.previousmeetingreview.generated.resources.screens_previous_meeting_review_start_meeting_cta_a11y
import org.mifos.groupbanking.feature.previousmeetingreview.generated.resources.screens_previous_meeting_review_status_absent
import org.mifos.groupbanking.feature.previousmeetingreview.generated.resources.screens_previous_meeting_review_status_late
import org.mifos.groupbanking.feature.previousmeetingreview.generated.resources.screens_previous_meeting_review_status_present
import org.mifos.groupbanking.feature.previousmeetingreview.generated.resources.screens_previous_meeting_review_title_format
import org.mifos.groupbanking.feature.previousmeetingreview.generated.resources.screens_previous_meeting_review_total_collected_label
import org.mifos.groupbanking.feature.previousmeetingreview.generated.resources.screens_previous_meeting_review_unresolved_header
import org.mifos.groupbanking.feature.previousmeetingreview.generated.resources.screens_previous_meeting_review_warning_icon_a11y

/**
 * Container for `previous-meeting-review-screen` (FR-019). Collects
 * [PreviousMeetingReviewViewModel] state via [collectAsStateWithLifecycle], consumes one-shot
 * [PreviousMeetingReviewEvent]s through [EventsEffect], and delegates all rendering to the stateless
 * [PreviousMeetingReviewContent]. [centerId] / [meetingNumber] / [meetingId] / [launchedFrom] are the
 * `ui.yaml#nav_params` supplied to the ViewModel via Koin `parametersOf(...)`. See API.md#screen.
 */
@Composable
internal fun PreviousMeetingReviewScreen(
    meetingId: String,
    meetingNumber: Int,
    centerId: Int,
    launchedFrom: String,
    onNavigateBack: () -> Unit,
    onNavigateToConduct: (meetingId: String, meetingNumber: Int, centerId: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PreviousMeetingReviewViewModel = koinViewModel(
        parameters = { parametersOf(centerId, meetingNumber, meetingId, launchedFrom) },
    ),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    EventsEffect(viewModel) { event ->
        when (event) {
            PreviousMeetingReviewEvent.NavigateBack -> onNavigateBack()
            is PreviousMeetingReviewEvent.NavigateToConduct ->
                onNavigateToConduct(event.meetingId, event.meetingNumber, event.centerId)
        }
    }

    PreviousMeetingReviewContent(
        state = state,
        onAction = viewModel::trySendAction,
        modifier = modifier,
    )
}

/**
 * Stateless render surface for `previous-meeting-review-screen`. State-driven per
 * [PreviousMeetingReviewState.screenState] — every [PreviousMeetingReviewScreenState] member is
 * handled (Loading / Content / Error). Read-only screen. See API.md#screen.
 */
@Composable
internal fun PreviousMeetingReviewContent(
    state: PreviousMeetingReviewState,
    onAction: (PreviousMeetingReviewAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(Res.string.screens_previous_meeting_review_title_format, state.meetingNumber)

    KptScaffold(
        onNavigationIconClick = { onAction(PreviousMeetingReviewAction.NavigateBack) },
        title = title,
        modifier = modifier.testTag(PreviousMeetingReviewTestTags.SCREEN),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (state.screenState) {
                PreviousMeetingReviewScreenState.Loading -> PreviousMeetingReviewLoadingSection()
                PreviousMeetingReviewScreenState.Content -> PreviousMeetingReviewContentSection(state = state, onAction = onAction)
                PreviousMeetingReviewScreenState.Error -> PreviousMeetingReviewErrorSection(state = state, onAction = onAction)
            }
        }
    }
}

/** `PreviousMeetingReviewScreenState.Loading` — 5 shimmer blocks mirroring `ui.yaml#loading_skeleton`. */
@Composable
internal fun PreviousMeetingReviewLoadingSection(modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    Column(
        modifier = modifier.fillMaxSize().padding(sp.lg).testTag(PreviousMeetingReviewTestTags.LOADING_SECTION),
        verticalArrangement = Arrangement.spacedBy(sp.md),
    ) {
        repeat(5) { SkeletonBlock(height = 72.dp) }
    }
}

/** One shimmering placeholder block — purely decorative. */
@Composable
private fun SkeletonBlock(height: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(MaterialTheme.spacing.md))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

/**
 * `PreviousMeetingReviewScreenState.Content` — context banner, unresolved-items alert (hidden when
 * empty), summary metrics card, attendance chip + per-member attendance rows, per-member savings
 * rows, per-member loan-activity rows, and the conduct-only Start-Meeting CTA. Mirrors
 * `ui.yaml#states.content`. Guards on `state.meetingDetail` being non-null. See API.md#screen.
 */
@Composable
internal fun PreviousMeetingReviewContentSection(
    state: PreviousMeetingReviewState,
    onAction: (PreviousMeetingReviewAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val detail = state.meetingDetail ?: return
    val sp = MaterialTheme.spacing

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag(PreviousMeetingReviewTestTags.CONTENT_LIST),
        verticalArrangement = Arrangement.spacedBy(sp.md),
        contentPadding = PaddingValues(sp.lg),
    ) {
        item { ContextBanner(state = state, actualDate = detail.actualDate) }
        if (state.unresolvedItems.isNotEmpty()) {
            item { UnresolvedAlertCard(items = state.unresolvedItems) }
        }
        item { SummaryMetricsCard(detail = detail) }
        item { AttendanceSection(detail = detail) }
        item { SavingsSection(rows = detail.savingsBreakdown) }
        item { LoanSection(rows = detail.loanItems) }
        if (state.isConductLaunched) {
            item { StartMeetingCta(nextMeetingNumber = state.nextMeetingNumber ?: (state.meetingNumber + 1), onAction = onAction) }
        }
        item { Box(modifier = Modifier.height(sp.xl)) }
    }
}

/** Context banner — conduct vs calendar variant per `ui.yaml#context_banner`. */
@Composable
private fun ContextBanner(state: PreviousMeetingReviewState, actualDate: String, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val text = if (state.isConductLaunched) {
        stringResource(
            Res.string.screens_previous_meeting_review_context_conduct_format,
            state.nextMeetingNumber ?: (state.meetingNumber + 1),
            actualDate,
        )
    } else {
        stringResource(Res.string.screens_previous_meeting_review_context_calendar_format, actualDate)
    }
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(sp.md),
        modifier = modifier.fillMaxWidth().testTag(PreviousMeetingReviewTestTags.CONTEXT_BANNER),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = sp.md, vertical = sp.sm),
        )
    }
}

/** Unresolved-items alert card — warning-toned, one row per derived item (`ui.yaml#unresolved_alert_card`). */
@Composable
private fun UnresolvedAlertCard(items: List<UnresolvedItem>, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val warningIconCd = stringResource(Res.string.screens_previous_meeting_review_warning_icon_a11y)
    Card(
        modifier = modifier.fillMaxWidth().testTag(PreviousMeetingReviewTestTags.UNRESOLVED_ALERT_CARD),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        shape = RoundedCornerShape(sp.md),
    ) {
        Column(modifier = Modifier.padding(sp.md), verticalArrangement = Arrangement.spacedBy(sp.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = warningIconCd,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(Res.string.screens_previous_meeting_review_unresolved_header),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = sp.sm),
                )
            }
            items.forEach { item ->
                Text(text = item.description, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** Summary metrics card — total collected (hero) + closing corpus / fines / loans disbursed rows. */
@Composable
private fun SummaryMetricsCard(detail: PreviousMeetingDetail, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    Card(
        modifier = modifier.fillMaxWidth().testTag(PreviousMeetingReviewTestTags.SUMMARY_METRICS_CARD),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        shape = RoundedCornerShape(sp.lg),
    ) {
        Column(modifier = Modifier.padding(sp.lg), verticalArrangement = Arrangement.spacedBy(sp.xs)) {
            Text(
                text = stringResource(Res.string.screens_previous_meeting_review_total_collected_label),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = stringResource(
                    Res.string.screens_previous_meeting_review_kes_amount_format,
                    detail.totalSavingsCollected.formatGrouped(),
                ),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = sp.sm))
            InfoRow(
                label = stringResource(Res.string.screens_previous_meeting_review_closing_corpus_label),
                value = stringResource(Res.string.screens_previous_meeting_review_kes_amount_format, detail.closingCorpus.formatGrouped()),
            )
            InfoRow(
                label = stringResource(Res.string.screens_previous_meeting_review_fines_label),
                value = stringResource(Res.string.screens_previous_meeting_review_kes_amount_format, detail.finesCollected.formatGrouped()),
            )
            InfoRow(
                label = stringResource(Res.string.screens_previous_meeting_review_loans_disbursed_label),
                value = stringResource(Res.string.screens_previous_meeting_review_kes_amount_format, detail.loansDisbursed.formatGrouped()),
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

/** Attendance chip + per-member attendance rows (`ui.yaml#attendance_chip_row` + `attendance_detail_row`). */
@Composable
private fun AttendanceSection(detail: PreviousMeetingDetail, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val chipA11y = stringResource(
        Res.string.screens_previous_meeting_review_attendance_chip_a11y,
        detail.attendanceCount,
        detail.totalMemberCount,
    )
    Column(modifier = modifier.fillMaxWidth().testTag(PreviousMeetingReviewTestTags.ATTENDANCE_SECTION)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = sp.sm)) {
            Text(
                text = stringResource(Res.string.screens_previous_meeting_review_attendance_section_header),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(sp.sm),
                modifier = Modifier.testTag(PreviousMeetingReviewTestTags.ATTENDANCE_CHIP).semanticsLabel(chipA11y),
            ) {
                Text(
                    text = stringResource(
                        Res.string.screens_previous_meeting_review_attendance_chip_format,
                        detail.attendanceCount,
                        detail.totalMemberCount,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = sp.md, vertical = sp.xs),
                )
            }
        }
        detail.attendanceRecords.forEach { record ->
            AttendanceRow(record = record)
            HorizontalDivider()
        }
    }
}

/** One attendance row — status-colored avatar + name + optional fine line + status chip. */
@Composable
private fun AttendanceRow(record: AttendanceRecord, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val (avatarBg, avatarFg) = statusColors(record.status)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(vertical = sp.sm)
            .testTag(PreviousMeetingReviewTestTags.attendanceRowTag(record.memberId)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(name = record.memberName, background = avatarBg, foreground = avatarFg)
        Column(modifier = Modifier.weight(1f).padding(start = sp.md)) {
            Text(text = record.memberName, style = MaterialTheme.typography.bodyLarge)
            if (record.fineAmount > 0L) {
                Text(
                    text = stringResource(
                        Res.string.screens_previous_meeting_review_attendance_fine_format,
                        record.fineAmount.formatGrouped(),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        StatusChip(status = record.status)
    }
}

/** Status chip — colored per `ui.yaml#attendance_detail_row.trailing.chip.style`. */
@Composable
private fun StatusChip(status: AttendanceStatus, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val (bg, fg) = statusColors(status)
    val label = when (status) {
        AttendanceStatus.PRESENT -> stringResource(Res.string.screens_previous_meeting_review_status_present)
        AttendanceStatus.LATE -> stringResource(Res.string.screens_previous_meeting_review_status_late)
        AttendanceStatus.ABSENT -> stringResource(Res.string.screens_previous_meeting_review_status_absent)
    }
    Surface(color = bg, contentColor = fg, shape = RoundedCornerShape(sp.sm), modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = sp.sm, vertical = sp.xs),
        )
    }
}

@Composable
private fun statusColors(status: AttendanceStatus): Pair<Color, Color> = when (status) {
    AttendanceStatus.PRESENT -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    AttendanceStatus.LATE -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    AttendanceStatus.ABSENT -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
}

/** Per-member savings rows (`ui.yaml#member_savings_row`). */
@Composable
private fun SavingsSection(rows: List<SavingsBreakdownItem>, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    Column(modifier = modifier.fillMaxWidth().testTag(PreviousMeetingReviewTestTags.SAVINGS_SECTION)) {
        Text(
            text = stringResource(Res.string.screens_previous_meeting_review_savings_section_header),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = sp.sm),
        )
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(vertical = sp.sm)
                    .testTag(PreviousMeetingReviewTestTags.savingsRowTag(row.memberId)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(
                    name = row.memberName,
                    background = MaterialTheme.colorScheme.secondaryContainer,
                    foreground = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Column(modifier = Modifier.weight(1f).padding(start = sp.md)) {
                    Text(text = row.memberName, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = stringResource(
                            Res.string.screens_previous_meeting_review_savings_supporting_format,
                            row.groupSavings.formatGrouped(),
                            row.individualSavings.formatGrouped(),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = stringResource(Res.string.screens_previous_meeting_review_kes_amount_format, row.totalSavings.formatGrouped()),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            HorizontalDivider()
        }
    }
}

/** Per-member loan-activity rows (`ui.yaml#loan_activity_row`). */
@Composable
private fun LoanSection(rows: List<LoanSummaryItem>, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    Column(modifier = modifier.fillMaxWidth().testTag(PreviousMeetingReviewTestTags.LOAN_SECTION)) {
        Text(
            text = stringResource(Res.string.screens_previous_meeting_review_loan_section_header),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = sp.sm),
        )
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(vertical = sp.sm)
                    .testTag(PreviousMeetingReviewTestTags.loanRowTag(row.memberId)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = row.memberName, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = stringResource(
                            Res.string.screens_previous_meeting_review_loan_supporting_format,
                            row.amountRepaid.formatGrouped(),
                            row.outstandingAfter.formatGrouped(),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (row.amountDisbursed > 0L) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = RoundedCornerShape(sp.sm),
                    ) {
                        Text(
                            text = stringResource(
                                Res.string.screens_previous_meeting_review_loan_disbursed_chip_format,
                                row.amountDisbursed.formatGrouped(),
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = sp.sm, vertical = sp.xs),
                        )
                    }
                }
            }
            HorizontalDivider()
        }
    }
}

/** Start-Meeting CTA — conduct-launched only (`ui.yaml#start_meeting_cta`). */
@Composable
private fun StartMeetingCta(
    nextMeetingNumber: Int,
    onAction: (PreviousMeetingReviewAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val ctaA11y = stringResource(Res.string.screens_previous_meeting_review_start_meeting_cta_a11y, nextMeetingNumber)
    Button(
        onClick = { onAction(PreviousMeetingReviewAction.StartNewMeeting) },
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = sp.touchTargetMin)
            .testTag(PreviousMeetingReviewTestTags.START_MEETING_CTA)
            .semanticsLabel(ctaA11y),
    ) {
        Text(text = stringResource(Res.string.screens_previous_meeting_review_start_meeting_btn_format, nextMeetingNumber))
    }
}

/** Circular avatar with the member's initials — status/role-tinted background. */
@Composable
private fun Avatar(name: String, background: Color, foreground: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(36.dp).clip(CircleShape).background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = initialsOf(name), style = MaterialTheme.typography.labelMedium, color = foreground, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * `PreviousMeetingReviewScreenState.Error` — full-screen error surface (cloud_off icon, title,
 * resolved message, Retry CTA shown only when retryable), mirroring `ui.yaml#states.error`.
 */
@Composable
internal fun PreviousMeetingReviewErrorSection(
    state: PreviousMeetingReviewState,
    onAction: (PreviousMeetingReviewAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val errorIconCd = stringResource(Res.string.screens_previous_meeting_review_error_icon_cd)
    val titleText = stringResource(Res.string.screens_previous_meeting_review_error_state_title)
    val retryLabel = stringResource(Res.string.screens_previous_meeting_review_action_retry)
    val message = when (state.error) {
        PreviousMeetingReviewError.Network, null -> stringResource(Res.string.screens_previous_meeting_review_error_network)
        PreviousMeetingReviewError.Server -> stringResource(Res.string.screens_previous_meeting_review_error_server)
        PreviousMeetingReviewError.NotFound -> stringResource(Res.string.screens_previous_meeting_review_error_not_found)
        PreviousMeetingReviewError.Auth -> stringResource(Res.string.screens_previous_meeting_review_error_auth)
    }
    val canRetry = state.error?.retry ?: true

    Column(
        modifier = modifier.fillMaxSize().padding(sp.lg).testTag(PreviousMeetingReviewTestTags.ERROR_SECTION),
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
                onClick = { onAction(PreviousMeetingReviewAction.Retry) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = sp.touchTargetMin)
                    .padding(top = sp.lg)
                    .testTag(PreviousMeetingReviewTestTags.ERROR_RETRY_BUTTON),
            ) {
                Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
                Text(text = retryLabel, modifier = Modifier.padding(start = sp.xs))
            }
        }
    }
}

/** Two-letter initials from a member name (first + last word), for the avatar. Pure UI helper. */
private fun initialsOf(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(1).uppercase()
        else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
    }
}

/** Small a11y helper — attaches a content description as a semantics label. */
private fun Modifier.semanticsLabel(label: String): Modifier =
    this.semantics { contentDescription = label }
