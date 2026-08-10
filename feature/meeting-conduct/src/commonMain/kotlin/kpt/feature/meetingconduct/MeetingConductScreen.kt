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

package kpt.feature.meetingconduct

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kpt.core.base.designsystem.component.KptButton
import kpt.core.base.designsystem.component.KptOutlinedButton
import kpt.core.base.designsystem.component.KptTopAppBar
import kpt.core.base.designsystem.core.KptTopAppBarConfiguration
import kpt.core.base.ui.effects.EventsEffect
import kpt.core.common.formatGrouped
import kpt.core.designsystem.theme.spacing
import kpt.core.model.meeting.AttendanceStatus
import kpt.core.model.meeting.GroupMember
import kpt.core.model.meeting.LoanApplication
import kpt.core.model.meeting.LoanSummary
import kpt.core.model.meeting.LoanVote
import kpt.core.model.meeting.SavingsType
import kpt.core.ui.scaffold.KptScaffold
import kpt.feature.meetingconduct.generated.resources.Res
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_attendance_absent
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_attendance_fine_chip
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_attendance_fine_info
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_attendance_late
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_attendance_present
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_attendance_progress
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_attendance_row_cd
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_back
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_back_cd
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_cash_on_hand_label
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_close_cd
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_corpus_at_start
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_corpus_band_cash
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_corpus_band_cd
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_corpus_band_corpus
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_corpus_gate_chip
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_corpus_group_fund
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_currency_kes
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_error_attendance_incomplete
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_error_auth
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_error_corpus_insufficient
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_error_min_contribution
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_error_offline
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_error_repayment_exceeds
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_error_server
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_first_meeting_body
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_first_meeting_title
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_info_attendance
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_info_closing_corpus
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_info_date
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_info_meeting
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_info_total_collected
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_loading_cd
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_loan_app_purpose
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_loan_app_requests
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_loan_approve
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_loan_approved
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_loan_fine_label
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_loan_overdue
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_loan_repayment_cd
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_loan_repayment_label
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_loan_summary
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_loan_vote_against
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_loan_vote_for
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_next
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_next_cd
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_no_active_loans_body
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_no_active_loans_title
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_no_pending_loans_body
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_no_pending_loans_title
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_offline_badge
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_offline_submit_note
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_prev_summary_cd
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_recon_closing
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_recon_disbursed
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_recon_fines
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_recon_group_savings
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_recon_opening
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_recon_repayments
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_reconciliation_cd
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_savings_group_cd
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_savings_group_label
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_savings_individual_cd
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_savings_individual_label
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_savings_min_chip
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_savings_running_total_label
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_step0_title
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_step1_title
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_step2_title
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_step3_title
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_step4_title
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_step5_title
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_step6_title
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_stepper_apply
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_stepper_attendance
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_stepper_balance
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_stepper_cd
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_stepper_close
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_stepper_loans
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_stepper_review
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_stepper_savings
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_submit_cd
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_submit_meeting
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_submitting
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_success_message
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_topbar_subtitle
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_topbar_title
import kpt.feature.meetingconduct.generated.resources.screens_meeting_conduct_view_full_report
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Container for `meeting-conduct-screen` (`ui.yaml#route`: `/meetings/{meetingId}/conduct`). Collects
 * [MeetingConductViewModel] state via [collectAsStateWithLifecycle], forwards the required nav-args to
 * Koin via `parametersOf(meetingId, meetingNumber, groupId)`, consumes one-shot
 * [MeetingConductEvent]s (navigation + snackbar) through [EventsEffect], and delegates rendering to
 * the stateless [MeetingConductContent]. See API.md#screen.
 */
@Composable
internal fun MeetingConductScreen(
    meetingId: String,
    meetingNumber: Int,
    groupId: Int,
    onNavigateToMeetingSummary: (meetingId: String, meetingNumber: Int, groupId: Int) -> Unit,
    onNavigateToPreviousMeetingReview: (meetingId: String, meetingNumber: Int, groupId: Int, launchedFrom: String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MeetingConductViewModel = koinViewModel(
        parameters = { parametersOf(meetingId, meetingNumber, groupId) },
    ),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Precomputed OUTSIDE EventsEffect — stringResource() is @Composable-only and EventsEffect's
    // callback runs in a suspend (non-composable) scope (same convention as LoanApplyScreen). The
    // ViewModel emits message-KEYS; the Screen owns the key → localized-string resolution.
    val errorMessages: Map<String, String> = mapOf(
        "error_attendance_incomplete" to stringResource(Res.string.screens_meeting_conduct_error_attendance_incomplete),
        "error_min_contribution" to stringResource(Res.string.screens_meeting_conduct_error_min_contribution),
        "error_repayment_exceeds" to stringResource(Res.string.screens_meeting_conduct_error_repayment_exceeds),
        "error_corpus_insufficient" to stringResource(Res.string.screens_meeting_conduct_error_corpus_insufficient),
        "error_server" to stringResource(Res.string.screens_meeting_conduct_error_server),
        "error_auth" to stringResource(Res.string.screens_meeting_conduct_error_auth),
        "error_offline" to stringResource(Res.string.screens_meeting_conduct_error_offline),
    )
    fun resolve(key: String): String = errorMessages[key] ?: key

    EventsEffect(viewModel) { event ->
        when (event) {
            is MeetingConductEvent.NavigateToMeetingSummary ->
                onNavigateToMeetingSummary(event.meetingId, event.meetingNumber, event.groupId)
            is MeetingConductEvent.NavigateToPreviousMeetingReview ->
                onNavigateToPreviousMeetingReview(event.meetingId, event.meetingNumber, event.groupId, event.launchedFrom)
            MeetingConductEvent.NavigateBack -> onNavigateBack()
            is MeetingConductEvent.ShowStepError -> snackbarHostState.showSnackbar(resolve(event.message))
            MeetingConductEvent.ShowSubmitSuccess -> Unit // navigation follows immediately
            is MeetingConductEvent.ShowSubmitError -> snackbarHostState.showSnackbar(resolve(event.message))
        }
    }

    MeetingConductContent(
        state = state,
        onAction = viewModel::trySendAction,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

/** Stateless render surface — every [MeetingConductScreenState] member is handled. See API.md#screen. */
@Composable
internal fun MeetingConductContent(
    state: MeetingConductState,
    onAction: (MeetingConductAction) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val screenState = state.deriveScreenState()
    val stepTitle = stepTitle(state.currentStep)
    val title = stringResource(Res.string.screens_meeting_conduct_topbar_title, state.meetingNumber)
    val subtitle = stringResource(
        Res.string.screens_meeting_conduct_topbar_subtitle,
        stepTitle,
        state.currentStep + 1,
        state.totalSteps,
    )
    val closeCd = stringResource(Res.string.screens_meeting_conduct_close_cd)

    KptScaffold(
        modifier = modifier.testTag(MeetingConductTestTags.SCREEN),
        topBar = {
            KptTopAppBar(
                KptTopAppBarConfiguration(
                    title = title,
                    subtitle = subtitle,
                    navigationIcon = Icons.Filled.Close,
                    onNavigationIonClick = { onAction(MeetingConductAction.OnBack) },
                    testTag = MeetingConductTestTags.CLOSE_ACTION,
                    contentDescription = closeCd,
                ),
            )
        },
        snackbarHostState = snackbarHostState,
    ) {
        when (screenState) {
            MeetingConductScreenState.Loading -> MeetingConductLoadingSection()
            MeetingConductScreenState.Content -> MeetingConductWizard(state = state, onAction = onAction, enabled = true)
            MeetingConductScreenState.Submitting -> MeetingConductWizard(state = state, onAction = onAction, enabled = false)
            MeetingConductScreenState.SubmitSuccess -> MeetingConductSuccessSection(state = state)
            MeetingConductScreenState.SubmitError -> MeetingConductWizard(state = state, onAction = onAction, enabled = true)
        }
    }
}

@Composable
private fun MeetingConductLoadingSection(modifier: Modifier = Modifier) {
    val loadingCd = stringResource(Res.string.screens_meeting_conduct_loading_cd)
    Box(
        modifier = modifier.fillMaxSize().semantics { contentDescription = loadingCd },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.testTag(MeetingConductTestTags.LOADING_INDICATOR))
    }
}

/** The wizard shell: stepper header + conditional corpus band + per-step body + navigation footer. */
@Composable
private fun MeetingConductWizard(
    state: MeetingConductState,
    onAction: (MeetingConductAction) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    Column(modifier = modifier.fillMaxSize()) {
        StepperHeader(currentStep = state.currentStep, modifier = Modifier.padding(horizontal = sp.md, vertical = sp.sm))
        if (state.isOffline) OfflineBadge()
        if (state.currentStep >= 2) CorpusBand(state = state)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = sp.lg),
        ) {
            VSpace(sp.md)
            when (state.currentStep) {
                0 -> Step0PreviousReview(state = state, onAction = onAction)
                1 -> Step1Attendance(state = state, onAction = onAction, enabled = enabled)
                2 -> Step2OpeningBalance(state = state)
                3 -> Step3Savings(state = state, onAction = onAction, enabled = enabled)
                4 -> Step4LoanReview(state = state, onAction = onAction, enabled = enabled)
                5 -> Step5LoanApplications(state = state, onAction = onAction, enabled = enabled)
                6 -> Step6ClosingBalance(state = state, onAction = onAction, enabled = enabled)
            }
            VSpace(sp.xxl)
        }
        WizardFooter(state = state, onAction = onAction, enabled = enabled)
    }
}

// -- Persistent header components -------------------------------------------------------------------

@Composable
private fun StepperHeader(currentStep: Int, modifier: Modifier = Modifier) {
    val labels = listOf(
        stringResource(Res.string.screens_meeting_conduct_stepper_review),
        stringResource(Res.string.screens_meeting_conduct_stepper_attendance),
        stringResource(Res.string.screens_meeting_conduct_stepper_balance),
        stringResource(Res.string.screens_meeting_conduct_stepper_savings),
        stringResource(Res.string.screens_meeting_conduct_stepper_loans),
        stringResource(Res.string.screens_meeting_conduct_stepper_apply),
        stringResource(Res.string.screens_meeting_conduct_stepper_close),
    )
    val stepperCd = stringResource(Res.string.screens_meeting_conduct_stepper_cd)
    Column(modifier = modifier.testTag(MeetingConductTestTags.STEPPER).semantics { contentDescription = stepperCd }) {
        LinearProgressIndicator(
            progress = { (currentStep + 1).toFloat() / labels.size.toFloat() },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = labels.getOrElse(currentStep) { "" },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun OfflineBadge(modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    Row(modifier = modifier.padding(horizontal = sp.lg, vertical = sp.xs)) {
        AssistChip(
            onClick = {},
            enabled = false,
            label = { Text(stringResource(Res.string.screens_meeting_conduct_offline_badge)) },
            leadingIcon = { Icon(Icons.Filled.CloudOff, contentDescription = null, modifier = Modifier.size(16.dp)) },
            modifier = Modifier.testTag(MeetingConductTestTags.OFFLINE_BADGE),
        )
    }
}

@Composable
private fun CorpusBand(state: MeetingConductState, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val cd = stringResource(Res.string.screens_meeting_conduct_corpus_band_cd)
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = modifier
            .fillMaxWidth()
            .testTag(MeetingConductTestTags.CORPUS_BAND)
            .semantics { contentDescription = cd },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = sp.lg, vertical = sp.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(Res.string.screens_meeting_conduct_corpus_band_corpus, state.closingCorpus.formatGrouped()),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = stringResource(Res.string.screens_meeting_conduct_corpus_band_cash, state.cashOnHand.formatGrouped()),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

// -- Step 0: previous meeting review ---------------------------------------------------------------

@Composable
private fun Step0PreviousReview(
    state: MeetingConductState,
    onAction: (MeetingConductAction) -> Unit,
) {
    val sp = MaterialTheme.spacing
    Column(modifier = Modifier.fillMaxWidth().testTag(MeetingConductTestTags.STEP0_PREVIOUS_REVIEW)) {
        StepHeading(stringResource(Res.string.screens_meeting_conduct_step0_title))
        VSpace(sp.md)
        val summary = state.previousMeetingSummary
        if (summary == null) {
            EmptyState(
                title = stringResource(Res.string.screens_meeting_conduct_first_meeting_title),
                body = stringResource(Res.string.screens_meeting_conduct_first_meeting_body),
            )
        } else {
            val cd = stringResource(Res.string.screens_meeting_conduct_prev_summary_cd)
            Card(
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = cd },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(Modifier.padding(sp.lg)) {
                    InfoRow(stringResource(Res.string.screens_meeting_conduct_info_meeting), "#${summary.meetingNumber}")
                    InfoRow(stringResource(Res.string.screens_meeting_conduct_info_date), summary.date)
                    InfoRow(stringResource(Res.string.screens_meeting_conduct_info_total_collected), kes(summary.totalCollected))
                    InfoRow(stringResource(Res.string.screens_meeting_conduct_info_closing_corpus), kes(summary.corpusAtClose))
                    InfoRow(
                        stringResource(Res.string.screens_meeting_conduct_info_attendance),
                        "${summary.attendanceCount}/${state.groupMembers.size}",
                    )
                }
            }
            VSpace(sp.md)
            KptOutlinedButton(
                onClick = { onAction(MeetingConductAction.ViewFullPreviousMeeting) },
                modifier = Modifier.fillMaxWidth().testTag(MeetingConductTestTags.VIEW_FULL_PREVIOUS_BUTTON),
            ) {
                Text(stringResource(Res.string.screens_meeting_conduct_view_full_report))
            }
        }
    }
}

// -- Step 1: attendance -----------------------------------------------------------------------------

@Composable
private fun Step1Attendance(
    state: MeetingConductState,
    onAction: (MeetingConductAction) -> Unit,
    enabled: Boolean,
) {
    val sp = MaterialTheme.spacing
    Column(modifier = Modifier.fillMaxWidth().testTag(MeetingConductTestTags.STEP1_ATTENDANCE)) {
        StepHeading(stringResource(Res.string.screens_meeting_conduct_step1_title))
        VSpace(sp.sm)
        WarningChip(stringResource(Res.string.screens_meeting_conduct_attendance_fine_info))
        VSpace(sp.md)
        state.groupMembers.forEach { member ->
            AttendanceRow(
                member = member,
                selected = state.attendanceMap[member.memberId],
                enabled = enabled,
                onSelected = { status -> onAction(MeetingConductAction.SetAttendance(member.memberId, status)) },
            )
            HorizontalDivider()
        }
        VSpace(sp.sm)
        val recorded = state.attendanceMap.size
        AssistChip(
            onClick = {},
            enabled = false,
            label = { Text(stringResource(Res.string.screens_meeting_conduct_attendance_progress, recorded, state.groupMembers.size)) },
        )
    }
}

@Composable
private fun AttendanceRow(
    member: GroupMember,
    selected: AttendanceStatus?,
    enabled: Boolean,
    onSelected: (AttendanceStatus) -> Unit,
) {
    val sp = MaterialTheme.spacing
    val rowCd = stringResource(Res.string.screens_meeting_conduct_attendance_row_cd, member.name)
    val statuses = listOf(
        AttendanceStatus.PRESENT to stringResource(Res.string.screens_meeting_conduct_attendance_present),
        AttendanceStatus.LATE to stringResource(Res.string.screens_meeting_conduct_attendance_late),
        AttendanceStatus.ABSENT to stringResource(Res.string.screens_meeting_conduct_attendance_absent),
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = sp.sm)
            .testTag(MeetingConductTestTags.attendanceRowTag(member.memberId))
            .semantics { contentDescription = rowCd },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(member.initials)
            Spacer(Modifier.width(sp.md))
            Column {
                Text(member.name, style = MaterialTheme.typography.bodyLarge)
                Text(member.role, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
        VSpace(sp.xs)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            statuses.forEachIndexed { index, (status, label) ->
                SegmentedButton(
                    selected = selected == status,
                    onClick = { onSelected(status) },
                    enabled = enabled,
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = statuses.size),
                ) { Text(label) }
            }
        }
        val fine = when (selected) {
            AttendanceStatus.LATE -> LATE_FINE
            AttendanceStatus.ABSENT -> ABSENT_FINE
            else -> 0L
        }
        if (fine > 0L) {
            VSpace(sp.xs)
            Text(
                text = stringResource(Res.string.screens_meeting_conduct_attendance_fine_chip, fine.formatGrouped()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

// -- Step 2: opening balance ------------------------------------------------------------------------

@Composable
private fun Step2OpeningBalance(state: MeetingConductState) {
    val sp = MaterialTheme.spacing
    Column(modifier = Modifier.fillMaxWidth().testTag(MeetingConductTestTags.STEP2_OPENING_BALANCE)) {
        StepHeading(stringResource(Res.string.screens_meeting_conduct_step2_title))
        VSpace(sp.md)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        ) {
            Column(Modifier.padding(sp.xl)) {
                Text(stringResource(Res.string.screens_meeting_conduct_corpus_group_fund), style = MaterialTheme.typography.labelLarge)
                Text(kes(state.openingCorpus), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(Res.string.screens_meeting_conduct_corpus_at_start, state.meetingNumber),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        VSpace(sp.md)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(Modifier.padding(sp.lg)) {
                Text(stringResource(Res.string.screens_meeting_conduct_cash_on_hand_label), style = MaterialTheme.typography.labelLarge)
                Text(kes(state.cashOnHand), style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}

// -- Step 3: savings collection ---------------------------------------------------------------------

@Composable
private fun Step3Savings(
    state: MeetingConductState,
    onAction: (MeetingConductAction) -> Unit,
    enabled: Boolean,
) {
    val sp = MaterialTheme.spacing
    Column(modifier = Modifier.fillMaxWidth().testTag(MeetingConductTestTags.STEP3_SAVINGS)) {
        StepHeading(stringResource(Res.string.screens_meeting_conduct_step3_title))
        VSpace(sp.sm)
        InfoChip(stringResource(Res.string.screens_meeting_conduct_savings_min_chip))
        VSpace(sp.md)
        state.groupMembers.forEach { member ->
            val entry = state.savingsMap[member.memberId]
            val groupCd = stringResource(Res.string.screens_meeting_conduct_savings_group_cd, member.name)
            val individualCd = stringResource(Res.string.screens_meeting_conduct_savings_individual_cd, member.name)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = sp.sm)
                    .testTag(MeetingConductTestTags.savingsRowTag(member.memberId)),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(member.initials)
                    Spacer(Modifier.width(sp.md))
                    Text(member.name, style = MaterialTheme.typography.bodyLarge)
                }
                VSpace(sp.xs)
                AmountField(
                    label = stringResource(Res.string.screens_meeting_conduct_savings_group_label),
                    value = entry?.groupAmount ?: 0L,
                    contentDescription = groupCd,
                    enabled = enabled,
                    onValueChange = { onAction(MeetingConductAction.SetSavingsAmount(member.memberId, it, SavingsType.GROUP_LINKED)) },
                )
                VSpace(sp.xs)
                AmountField(
                    label = stringResource(Res.string.screens_meeting_conduct_savings_individual_label),
                    value = entry?.individualAmount ?: 0L,
                    contentDescription = individualCd,
                    enabled = enabled,
                    onValueChange = { onAction(MeetingConductAction.SetSavingsAmount(member.memberId, it, SavingsType.INDIVIDUAL)) },
                )
            }
            HorizontalDivider()
        }
        VSpace(sp.md)
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(sp.lg)) {
                Text(stringResource(Res.string.screens_meeting_conduct_savings_running_total_label), style = MaterialTheme.typography.labelMedium)
                Text(kes(state.runningSavingsTotal), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -- Step 4: loan review ----------------------------------------------------------------------------

@Composable
private fun Step4LoanReview(
    state: MeetingConductState,
    onAction: (MeetingConductAction) -> Unit,
    enabled: Boolean,
) {
    val sp = MaterialTheme.spacing
    Column(modifier = Modifier.fillMaxWidth().testTag(MeetingConductTestTags.STEP4_LOAN_REVIEW)) {
        StepHeading(stringResource(Res.string.screens_meeting_conduct_step4_title))
        VSpace(sp.md)
        if (state.activeLoans.isEmpty()) {
            EmptyState(
                title = stringResource(Res.string.screens_meeting_conduct_no_active_loans_title),
                body = stringResource(Res.string.screens_meeting_conduct_no_active_loans_body),
            )
        } else {
            state.activeLoans.forEach { loan ->
                LoanReviewRow(
                    loan = loan,
                    repayment = state.loanRepayments[loan.loanId] ?: 0L,
                    fine = state.loanFines[loan.loanId] ?: 0L,
                    enabled = enabled,
                    onRepaymentChange = { onAction(MeetingConductAction.SetLoanRepayment(loan.loanId, it)) },
                    onFineChange = { onAction(MeetingConductAction.SetLoanFine(loan.loanId, it)) },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun LoanReviewRow(
    loan: LoanSummary,
    repayment: Long,
    fine: Long,
    enabled: Boolean,
    onRepaymentChange: (Long) -> Unit,
    onFineChange: (Long) -> Unit,
) {
    val sp = MaterialTheme.spacing
    val repaymentCd = stringResource(Res.string.screens_meeting_conduct_loan_repayment_cd, loan.memberName)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = sp.sm)
            .testTag(MeetingConductTestTags.loanReviewRowTag(loan.loanId)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(loan.memberInitials)
            Spacer(Modifier.width(sp.md))
            Column(Modifier.weight(1f)) {
                Text(loan.memberName, style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(
                        Res.string.screens_meeting_conduct_loan_summary,
                        loan.principal.formatGrouped(),
                        loan.outstandingBalance.formatGrouped(),
                        loan.weekNumber,
                        loan.numberOfRepayments,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (loan.isOverdue) {
                Spacer(Modifier.width(sp.sm))
                WarningChip(stringResource(Res.string.screens_meeting_conduct_loan_overdue))
            }
        }
        VSpace(sp.xs)
        AmountField(
            label = stringResource(Res.string.screens_meeting_conduct_loan_repayment_label),
            value = repayment,
            contentDescription = repaymentCd,
            enabled = enabled,
            onValueChange = onRepaymentChange,
        )
        if (loan.isOverdue) {
            VSpace(sp.xs)
            AmountField(
                label = stringResource(Res.string.screens_meeting_conduct_loan_fine_label),
                value = fine,
                contentDescription = stringResource(Res.string.screens_meeting_conduct_loan_fine_label),
                enabled = enabled,
                onValueChange = onFineChange,
            )
        }
    }
}

// -- Step 5: loan applications ----------------------------------------------------------------------

@Composable
private fun Step5LoanApplications(
    state: MeetingConductState,
    onAction: (MeetingConductAction) -> Unit,
    enabled: Boolean,
) {
    val sp = MaterialTheme.spacing
    Column(modifier = Modifier.fillMaxWidth().testTag(MeetingConductTestTags.STEP5_LOAN_APPLICATIONS)) {
        StepHeading(stringResource(Res.string.screens_meeting_conduct_step5_title))
        VSpace(sp.sm)
        if (state.totalLoansDisbursed > 0L) {
            InfoChip(stringResource(Res.string.screens_meeting_conduct_corpus_gate_chip, state.closingCorpus.formatGrouped()))
            VSpace(sp.md)
        }
        if (state.pendingLoanApplications.isEmpty()) {
            EmptyState(
                title = stringResource(Res.string.screens_meeting_conduct_no_pending_loans_title),
                body = stringResource(Res.string.screens_meeting_conduct_no_pending_loans_body),
            )
        } else {
            state.pendingLoanApplications.forEach { application ->
                LoanApplicationCard(
                    application = application,
                    vote = state.loanVotes[application.id],
                    approved = application.id in state.approvedApplicationIds,
                    enabled = enabled,
                    onVote = { onAction(MeetingConductAction.CastLoanVote(application.id, it)) },
                    onApprove = { onAction(MeetingConductAction.ApproveLoanApplication(application.id)) },
                )
                VSpace(sp.md)
            }
        }
    }
}

@Composable
private fun LoanApplicationCard(
    application: LoanApplication,
    vote: LoanVote?,
    approved: Boolean,
    enabled: Boolean,
    onVote: (LoanVote) -> Unit,
    onApprove: () -> Unit,
) {
    val sp = MaterialTheme.spacing
    Card(
        modifier = Modifier.fillMaxWidth().testTag(MeetingConductTestTags.loanApplicationCardTag(application.id)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(sp.lg)) {
            Text(application.memberName, style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(Res.string.screens_meeting_conduct_loan_app_requests, application.requestedAmount.formatGrouped()),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                stringResource(Res.string.screens_meeting_conduct_loan_app_purpose, application.purpose),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            VSpace(sp.sm)
            Row {
                KptOutlinedButton(
                    onClick = { onVote(LoanVote.FOR) },
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(Res.string.screens_meeting_conduct_loan_vote_for)) }
                Spacer(Modifier.width(sp.sm))
                KptOutlinedButton(
                    onClick = { onVote(LoanVote.AGAINST) },
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(Res.string.screens_meeting_conduct_loan_vote_against)) }
            }
            VSpace(sp.sm)
            KptButton(
                onClick = onApprove,
                enabled = enabled && !approved && vote == LoanVote.FOR,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (approved) {
                        stringResource(Res.string.screens_meeting_conduct_loan_approved)
                    } else {
                        stringResource(Res.string.screens_meeting_conduct_loan_approve)
                    },
                )
            }
        }
    }
}

// -- Step 6: closing balance ------------------------------------------------------------------------

@Composable
private fun Step6ClosingBalance(
    state: MeetingConductState,
    onAction: (MeetingConductAction) -> Unit,
    enabled: Boolean,
) {
    val sp = MaterialTheme.spacing
    val reconciliationCd = stringResource(Res.string.screens_meeting_conduct_reconciliation_cd)
    val submitContentDesc = stringResource(Res.string.screens_meeting_conduct_submit_cd)
    Column(modifier = Modifier.fillMaxWidth().testTag(MeetingConductTestTags.STEP6_CLOSING_BALANCE)) {
        StepHeading(stringResource(Res.string.screens_meeting_conduct_step6_title))
        VSpace(sp.md)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(MeetingConductTestTags.RECONCILIATION_CARD)
                .semantics { contentDescription = reconciliationCd },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(Modifier.padding(sp.lg)) {
                InfoRow(stringResource(Res.string.screens_meeting_conduct_recon_opening), kes(state.openingCorpus))
                HorizontalDivider(Modifier.padding(vertical = sp.xs))
                InfoRow(stringResource(Res.string.screens_meeting_conduct_recon_group_savings), kes(state.runningSavingsTotal), MaterialTheme.colorScheme.primary)
                InfoRow(stringResource(Res.string.screens_meeting_conduct_recon_repayments), kes(state.totalRepayments), MaterialTheme.colorScheme.primary)
                InfoRow(stringResource(Res.string.screens_meeting_conduct_recon_fines), kes(state.totalFinesCollected), MaterialTheme.colorScheme.primary)
                InfoRow(stringResource(Res.string.screens_meeting_conduct_recon_disbursed), kes(state.totalLoansDisbursed), MaterialTheme.colorScheme.error)
                HorizontalDivider(Modifier.padding(vertical = sp.xs))
                InfoRow(
                    label = stringResource(Res.string.screens_meeting_conduct_recon_closing),
                    value = kes(state.closingCorpus),
                    valueColor = MaterialTheme.colorScheme.primary,
                    emphasize = true,
                )
            }
        }
        VSpace(sp.lg)
        KptButton(
            onClick = { onAction(MeetingConductAction.SubmitMeeting) },
            enabled = enabled && !state.isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = sp.touchTargetMin)
                .testTag(MeetingConductTestTags.SUBMIT_BUTTON)
                .semantics { contentDescription = submitContentDesc },
        ) {
            if (state.isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.width(sp.sm))
                Text(stringResource(Res.string.screens_meeting_conduct_submitting))
            } else {
                Text(stringResource(Res.string.screens_meeting_conduct_submit_meeting))
            }
        }
        if (state.isOffline) {
            VSpace(sp.sm)
            Text(
                text = stringResource(Res.string.screens_meeting_conduct_offline_submit_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// -- Footer -----------------------------------------------------------------------------------------

@Composable
private fun WizardFooter(
    state: MeetingConductState,
    onAction: (MeetingConductAction) -> Unit,
    enabled: Boolean,
) {
    val sp = MaterialTheme.spacing
    Surface(tonalElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(sp.lg)) {
            if (state.currentStep > 0) {
                val backCd = stringResource(Res.string.screens_meeting_conduct_back_cd)
                KptOutlinedButton(
                    onClick = { onAction(MeetingConductAction.PreviousStep) },
                    enabled = enabled,
                    modifier = Modifier
                        .weight(1f)
                        .testTag(MeetingConductTestTags.BACK_FOOTER_BUTTON)
                        .semantics { contentDescription = backCd },
                ) { Text(stringResource(Res.string.screens_meeting_conduct_back)) }
                Spacer(Modifier.width(sp.md))
            }
            val nextCd = stringResource(Res.string.screens_meeting_conduct_next_cd)
            val isLast = state.currentStep >= LAST_STEP
            KptButton(
                onClick = { onAction(if (isLast) MeetingConductAction.SubmitMeeting else MeetingConductAction.NextStep) },
                enabled = enabled && !state.isSubmitting,
                modifier = Modifier
                    .weight(1f)
                    .testTag(MeetingConductTestTags.NEXT_FOOTER_BUTTON)
                    .semantics { contentDescription = nextCd },
            ) {
                Text(
                    if (isLast) {
                        stringResource(Res.string.screens_meeting_conduct_submit_meeting)
                    } else {
                        stringResource(Res.string.screens_meeting_conduct_next)
                    },
                )
            }
        }
    }
}

// -- Terminal frames --------------------------------------------------------------------------------

@Composable
private fun MeetingConductSuccessSection(state: MeetingConductState, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    Column(
        modifier = modifier.fillMaxSize().padding(sp.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(40.dp))
        VSpace(sp.md)
        Text(
            text = stringResource(Res.string.screens_meeting_conduct_success_message, state.meetingNumber),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

// -- Small shared UI helpers ------------------------------------------------------------------------

@Composable
private fun StepHeading(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun InfoRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface, emphasize: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = MaterialTheme.spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = if (emphasize) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = if (emphasize) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium,
            color = valueColor,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun Avatar(initials: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.size(40.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(initials, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun WarningChip(text: String) {
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(text) },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = MaterialTheme.colorScheme.errorContainer,
            disabledLabelColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    )
}

@Composable
private fun InfoChip(text: String) {
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(text) },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            disabledLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    )
}

@Composable
private fun EmptyState(title: String, body: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = MaterialTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        VSpace(MaterialTheme.spacing.xs)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AmountField(
    label: String,
    value: Long,
    contentDescription: String,
    enabled: Boolean,
    onValueChange: (Long) -> Unit,
) {
    OutlinedTextField(
        value = if (value == 0L) "" else value.toString(),
        onValueChange = { raw -> onValueChange(raw.filter(Char::isDigit).toLongOrNull() ?: 0L) },
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth().semantics { this.contentDescription = contentDescription },
    )
}

@Composable
private fun kes(amount: Long): String = stringResource(Res.string.screens_meeting_conduct_currency_kes, amount.formatGrouped())

@Composable
private fun stepTitle(step: Int): String = stringResource(
    when (step) {
        0 -> Res.string.screens_meeting_conduct_step0_title
        1 -> Res.string.screens_meeting_conduct_step1_title
        2 -> Res.string.screens_meeting_conduct_step2_title
        3 -> Res.string.screens_meeting_conduct_step3_title
        4 -> Res.string.screens_meeting_conduct_step4_title
        5 -> Res.string.screens_meeting_conduct_step5_title
        else -> Res.string.screens_meeting_conduct_step6_title
    },
)

@Composable
private fun VSpace(height: Dp) {
    Spacer(Modifier.height(height))
}
