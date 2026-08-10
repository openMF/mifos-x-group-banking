/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.meetingcalendar

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kpt.core.base.ui.effects.EventsEffect
import kpt.core.designsystem.theme.spacing
import kpt.core.model.MeetingListItem
import kpt.core.model.MeetingStatus
import kpt.core.ui.scaffold.KptScaffold
import kpt.core.ui.scaffold.rememberKptPullToRefreshState
import kpt.feature.meetingcalendar.components.ScheduleEditorBottomSheet
import kpt.feature.meetingcalendar.generated.resources.Res
import kpt.feature.meetingcalendar.generated.resources.screens_meeting_calendar_action_retry
import kpt.feature.meetingcalendar.generated.resources.screens_meeting_calendar_collected_amount
import kpt.feature.meetingcalendar.generated.resources.screens_meeting_calendar_empty_body
import kpt.feature.meetingcalendar.generated.resources.screens_meeting_calendar_empty_icon_cd
import kpt.feature.meetingcalendar.generated.resources.screens_meeting_calendar_empty_title
import kpt.feature.meetingcalendar.generated.resources.screens_meeting_calendar_error_auth_message
import kpt.feature.meetingcalendar.generated.resources.screens_meeting_calendar_error_icon_cd
import kpt.feature.meetingcalendar.generated.resources.screens_meeting_calendar_error_network_message
import kpt.feature.meetingcalendar.generated.resources.screens_meeting_calendar_error_server_message
import kpt.feature.meetingcalendar.generated.resources.screens_meeting_calendar_error_title
import kpt.feature.meetingcalendar.generated.resources.screens_meeting_calendar_loading_message
import kpt.feature.meetingcalendar.generated.resources.screens_meeting_calendar_meeting_number
import kpt.feature.meetingcalendar.generated.resources.screens_meeting_calendar_no_upcoming_body
import kpt.feature.meetingcalendar.generated.resources.screens_meeting_calendar_no_upcoming_icon_cd
import kpt.feature.meetingcalendar.generated.resources.screens_meeting_calendar_no_upcoming_title
import kpt.feature.meetingcalendar.generated.resources.screens_meeting_calendar_past_header
import kpt.feature.meetingcalendar.generated.resources.screens_meeting_calendar_reschedule_button
import kpt.feature.meetingcalendar.generated.resources.screens_meeting_calendar_reschedule_button_a11y
import kpt.feature.meetingcalendar.generated.resources.screens_meeting_calendar_row_subtitle_attended
import kpt.feature.meetingcalendar.generated.resources.screens_meeting_calendar_schedule_updated_toast
import kpt.feature.meetingcalendar.generated.resources.screens_meeting_calendar_set_schedule_button
import kpt.feature.meetingcalendar.generated.resources.screens_meeting_calendar_set_schedule_button_a11y
import kpt.feature.meetingcalendar.generated.resources.screens_meeting_calendar_start_meeting
import kpt.feature.meetingcalendar.generated.resources.screens_meeting_calendar_status_completed
import kpt.feature.meetingcalendar.generated.resources.screens_meeting_calendar_status_missed
import kpt.feature.meetingcalendar.generated.resources.screens_meeting_calendar_status_upcoming
import kpt.feature.meetingcalendar.generated.resources.screens_meeting_calendar_title
import kpt.feature.meetingcalendar.generated.resources.screens_meeting_calendar_toggle_view_cd
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Container for `meeting-calendar-screen`. Collects [MeetingCalendarViewModel] state via
 * [collectAsStateWithLifecycle], consumes one-shot [MeetingCalendarEvent]s (navigate to conduct /
 * review, show error) through [EventsEffect], and delegates rendering to the stateless
 * [MeetingCalendarContent]. [groupId] is the `ui.yaml#nav_params` value forwarded from
 * `group-dashboard`'s "Meetings" entry point — supplied to [MeetingCalendarViewModel] via Koin
 * `parametersOf(groupId)`. [onNavigateBack] is a PLAIN nav callback (no declared `NavigateBack`
 * event — mirrors `LoanListScreen`'s precedent). See API.md#screen.
 */
@Composable
internal fun MeetingCalendarScreen(
    groupId: Int,
    onNavigateToConduct: (meetingId: String, meetingNumber: Int) -> Unit,
    onNavigateToReview: (meetingId: String, meetingNumber: Int, launchedFrom: String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MeetingCalendarViewModel = koinViewModel(parameters = { parametersOf(groupId) }),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Re-fetch the meetings whenever the calendar RETURNS to the foreground (e.g. after conducting a
    // meeting and popping back through the summary) so the just-completed meeting shows immediately —
    // the calendar sits alive in the back stack, so without this its SWR stream keeps serving the
    // pre-submit cached list. Skip the first resume: init already loads on initial composition.
    var isInitialResume by remember { mutableStateOf(true) }
    LifecycleResumeEffect(Unit) {
        if (isInitialResume) {
            isInitialResume = false
        } else {
            viewModel.trySendAction(MeetingCalendarAction.RefreshMeetings)
        }
        onPauseOrDispose { }
    }

    val networkMessage = stringResource(Res.string.screens_meeting_calendar_error_network_message)
    val serverMessage = stringResource(Res.string.screens_meeting_calendar_error_server_message)
    val authMessage = stringResource(Res.string.screens_meeting_calendar_error_auth_message)
    val scheduleUpdatedMessage = stringResource(Res.string.screens_meeting_calendar_schedule_updated_toast)

    EventsEffect(viewModel) { event ->
        when (event) {
            is MeetingCalendarEvent.NavigateToConduct -> onNavigateToConduct(event.meetingId, event.meetingNumber)
            is MeetingCalendarEvent.NavigateToReview ->
                onNavigateToReview(event.meetingId, event.meetingNumber, event.launchedFrom)
            is MeetingCalendarEvent.ShowError -> {
                val resolved = messageKeyToText(event.message, networkMessage, serverMessage, authMessage)
                snackbarHostState.showSnackbar(resolved)
            }
            is MeetingCalendarEvent.ShowScheduleUpdated -> snackbarHostState.showSnackbar(scheduleUpdatedMessage)
        }
    }

    MeetingCalendarContent(
        state = state,
        onAction = viewModel::trySendAction,
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

/** Resolves a [MeetingCalendarError.messageKey] to display text. */
private fun messageKeyToText(key: String, network: String, server: String, auth: String): String = when (key) {
    "error_network" -> network
    "error_server" -> server
    "error_auth" -> auth
    else -> key
}

/**
 * Stateless render surface for `meeting-calendar-screen`. State-driven per
 * [MeetingCalendarState.screenState] — every [MeetingCalendarScreenState] member is handled
 * (Loading/Content/Empty/Error). See API.md#screen.
 */
@Composable
internal fun MeetingCalendarContent(
    state: MeetingCalendarState,
    onAction: (MeetingCalendarAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val title = stringResource(Res.string.screens_meeting_calendar_title)

    KptScaffold(
        showNavigationIcon = true,
        onNavigationIconClick = onNavigateBack,
        title = title,
        floatingActionButtonContent = null,
        pullToRefreshState = rememberKptPullToRefreshState(
            isEnabled = true,
            isRefreshing = state.isRefreshing,
            onRefresh = { onAction(MeetingCalendarAction.RefreshMeetings) },
        ),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.testTag(MeetingCalendarTestTags.SCREEN),
    ) {
        when (state.screenState) {
            MeetingCalendarScreenState.Loading -> MeetingCalendarLoadingSection()

            MeetingCalendarScreenState.Content -> MeetingCalendarContentSection(state = state, onAction = onAction)

            MeetingCalendarScreenState.Empty -> MeetingCalendarEmptySection(
                state = state,
                onAction = onAction,
            )

            MeetingCalendarScreenState.Error -> MeetingCalendarErrorSection(
                message = when (state.error) {
                    MeetingCalendarError.Network, null -> stringResource(Res.string.screens_meeting_calendar_error_network_message)
                    MeetingCalendarError.Server -> stringResource(Res.string.screens_meeting_calendar_error_server_message)
                    MeetingCalendarError.Auth -> stringResource(Res.string.screens_meeting_calendar_error_auth_message)
                },
                onRetry = { onAction(MeetingCalendarAction.Retry) },
            )
        }

        // G3 / F6 — schedule-editor sheet overlays whenever showScheduleEditor (Content or Empty).
        if (state.showScheduleEditor) {
            ScheduleEditorBottomSheet(
                scheduleDay = state.scheduleDay,
                scheduleTime = state.scheduleTime,
                scheduleFrequency = state.scheduleFrequency,
                isRescheduling = state.isRescheduling,
                onFieldChange = { day, time, frequency ->
                    onAction(MeetingCalendarAction.OnScheduleFieldChange(day = day, time = time, frequency = frequency))
                },
                onConfirm = { day, time, frequency ->
                    onAction(MeetingCalendarAction.RescheduleMeeting(day = day, time = time, frequency = frequency))
                },
                onDismiss = { onAction(MeetingCalendarAction.DismissScheduleEditor) },
            )
        }
    }
}

/** View-mode toggle row (`toggle_view_btn`) — always present above the meetings body. */
@Composable
private fun ViewModeToggleRow(viewMode: ViewMode, onToggle: () -> Unit) {
    val sp = MaterialTheme.spacing
    val toggleCd = stringResource(Res.string.screens_meeting_calendar_toggle_view_cd)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = sp.lg, vertical = sp.xs),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onToggle,
            modifier = Modifier.testTag(MeetingCalendarTestTags.TOGGLE_VIEW_BUTTON),
        ) {
            Icon(
                imageVector = if (viewMode == ViewMode.LIST) Icons.Filled.CalendarMonth else Icons.Filled.ViewList,
                contentDescription = toggleCd,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** `MeetingCalendarScreenState.Loading` — toggle row + centered spinner. */
@Composable
internal fun MeetingCalendarLoadingSection(modifier: Modifier = Modifier) {
    val loadingLabel = stringResource(Res.string.screens_meeting_calendar_loading_message)
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.testTag(MeetingCalendarTestTags.LOADING_INDICATOR),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = loadingLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = MaterialTheme.spacing.md),
        )
    }
}

/** `MeetingCalendarScreenState.Content` — pinned upcoming card + scrollable past-meeting list. */
@Composable
internal fun MeetingCalendarContentSection(
    state: MeetingCalendarState,
    onAction: (MeetingCalendarAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val upcoming = state.upcomingMeeting
    val past = state.pastMeetings

    Column(modifier = modifier.fillMaxSize()) {
        ViewModeToggleRow(viewMode = state.viewMode, onToggle = { onAction(MeetingCalendarAction.ToggleViewMode) })

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = sp.lg).testTag(MeetingCalendarTestTags.MEETING_LIST),
            verticalArrangement = Arrangement.spacedBy(sp.md),
        ) {
            if (upcoming != null) {
                item(key = "upcoming_${upcoming.meetingId}") {
                    UpcomingMeetingCard(
                        meeting = upcoming,
                        onStart = { onAction(MeetingCalendarAction.StartMeeting(upcoming.meetingId, upcoming.meetingNumber)) },
                        onReschedule = { onAction(MeetingCalendarAction.OpenScheduleEditor) },
                    )
                }
            } else {
                // G3 / F6 — replaces the former dead "Next meeting not scheduled" placeholder with a
                // real Set/Adjust-Schedule CTA that opens the schedule editor.
                item(key = "no_upcoming_card") {
                    NoUpcomingScheduleCard(onSetSchedule = { onAction(MeetingCalendarAction.OpenScheduleEditor) })
                }
            }
            item(key = "past_header") {
                Text(
                    text = stringResource(Res.string.screens_meeting_calendar_past_header),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = sp.sm, bottom = sp.xs),
                )
            }
            items(items = past, key = { it.meetingId }) { meeting ->
                PastMeetingRow(
                    meeting = meeting,
                    onClick = { onAction(MeetingCalendarAction.OpenPastMeeting(meeting.meetingId, meeting.meetingNumber)) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

/** Pinned upcoming-meeting card with the green "Start Meeting" CTA + "Reschedule" affordance (`ui.yaml#upcoming_meeting_card`). */
@Composable
private fun UpcomingMeetingCard(meeting: MeetingListItem, onStart: () -> Unit, onReschedule: () -> Unit) {
    val sp = MaterialTheme.spacing
    val rescheduleA11y = stringResource(Res.string.screens_meeting_calendar_reschedule_button_a11y)
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = sp.sm).testTag(MeetingCalendarTestTags.UPCOMING_CARD),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(sp.lg), verticalArrangement = Arrangement.spacedBy(sp.sm)) {
            Text(
                text = stringResource(Res.string.screens_meeting_calendar_meeting_number, meeting.meetingNumber),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = meeting.meetingDate,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            StatusChip(status = MeetingStatus.UPCOMING)
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().heightIn(min = sp.touchTargetMin).testTag(MeetingCalendarTestTags.START_MEETING_BUTTON),
                colors = ButtonDefaults.buttonColors(),
            ) {
                Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
                Text(
                    text = stringResource(Res.string.screens_meeting_calendar_start_meeting),
                    modifier = Modifier.padding(start = sp.xs),
                )
            }
            TextButton(
                onClick = onReschedule,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .semantics { contentDescription = rescheduleA11y }
                    .testTag(MeetingCalendarTestTags.RESCHEDULE_BUTTON),
            ) {
                Icon(imageVector = Icons.Filled.EditCalendar, contentDescription = null)
                Text(
                    text = stringResource(Res.string.screens_meeting_calendar_reschedule_button),
                    modifier = Modifier.padding(start = sp.xs),
                )
            }
        }
    }
}

/**
 * `ui.yaml#no_upcoming_schedule_card` (G3 / F6) — shown in Content when there is no upcoming meeting.
 * Replaces the former dead "Next meeting not scheduled" placeholder with a real "Set / Adjust Schedule"
 * CTA that opens the schedule editor.
 */
@Composable
private fun NoUpcomingScheduleCard(onSetSchedule: () -> Unit) {
    val sp = MaterialTheme.spacing
    val iconCd = stringResource(Res.string.screens_meeting_calendar_no_upcoming_icon_cd)
    val ctaA11y = stringResource(Res.string.screens_meeting_calendar_set_schedule_button_a11y)
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = sp.sm).testTag(MeetingCalendarTestTags.NO_UPCOMING_CARD),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(sp.lg), verticalArrangement = Arrangement.spacedBy(sp.sm)) {
            Icon(
                imageVector = Icons.Filled.EditCalendar,
                contentDescription = iconCd,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp),
            )
            Text(
                text = stringResource(Res.string.screens_meeting_calendar_no_upcoming_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(Res.string.screens_meeting_calendar_no_upcoming_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onSetSchedule,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = sp.touchTargetMin)
                    .semantics { contentDescription = ctaA11y }
                    .testTag(MeetingCalendarTestTags.SET_SCHEDULE_BUTTON),
                colors = ButtonDefaults.buttonColors(),
            ) {
                Icon(imageVector = Icons.Filled.EditCalendar, contentDescription = null)
                Text(
                    text = stringResource(Res.string.screens_meeting_calendar_set_schedule_button),
                    modifier = Modifier.padding(start = sp.xs),
                )
            }
        }
    }
}

/** One past (COMPLETED / MISSED) meeting row (`ui.yaml#meeting_list_item`). */
@Composable
private fun PastMeetingRow(meeting: MeetingListItem, onClick: () -> Unit) {
    val sp = MaterialTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(onClick = onClick)
            .padding(vertical = sp.sm)
            .testTag(MeetingCalendarTestTags.rowTag(meeting.meetingId)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(sp.md),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(sp.xs)) {
            Text(
                text = stringResource(Res.string.screens_meeting_calendar_meeting_number, meeting.meetingNumber),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val attendance = meeting.attendanceCount
            // COMPLETED meetings carry the wall-clock conducted time — show "date · HH:mm".
            val dateLabel = if (meeting.meetingTime.isNotBlank()) {
                "${meeting.meetingDate} · ${meeting.meetingTime}"
            } else {
                meeting.meetingDate
            }
            Text(
                text = if (attendance != null) {
                    stringResource(Res.string.screens_meeting_calendar_row_subtitle_attended, dateLabel, attendance)
                } else {
                    dateLabel
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(sp.xs)) {
            meeting.totalCollectedKES?.let { collected ->
                Text(
                    text = stringResource(Res.string.screens_meeting_calendar_collected_amount, collected.toString()),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            StatusChip(status = meeting.status)
        }
    }
}

/** Status chip — color-coded per `ui.yaml` (completed -> successContainer, missed -> errorContainer). */
@Composable
private fun StatusChip(status: MeetingStatus) {
    val (label, container, content) = when (status) {
        MeetingStatus.UPCOMING -> Triple(
            stringResource(Res.string.screens_meeting_calendar_status_upcoming),
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimary,
        )
        MeetingStatus.COMPLETED -> Triple(
            stringResource(Res.string.screens_meeting_calendar_status_completed),
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
        MeetingStatus.MISSED -> Triple(
            stringResource(Res.string.screens_meeting_calendar_status_missed),
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
        )
    }
    Surface(shape = RoundedCornerShape(50), color = container) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = content,
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.sm, vertical = MaterialTheme.spacing.xs),
        )
    }
}

/** `MeetingCalendarScreenState.Empty` — illustration + title + body. */
@Composable
internal fun MeetingCalendarEmptySection(
    state: MeetingCalendarState,
    onAction: (MeetingCalendarAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val iconCd = stringResource(Res.string.screens_meeting_calendar_empty_icon_cd)
    val ctaA11y = stringResource(Res.string.screens_meeting_calendar_set_schedule_button_a11y)
    Column(modifier = modifier.fillMaxSize()) {
        ViewModeToggleRow(viewMode = state.viewMode, onToggle = { onAction(MeetingCalendarAction.ToggleViewMode) })
        Column(
            modifier = Modifier.fillMaxSize().padding(sp.lg).testTag(MeetingCalendarTestTags.EMPTY_SECTION),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.EventNote,
                contentDescription = iconCd,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(80.dp).padding(bottom = sp.lg),
            )
            Text(text = stringResource(Res.string.screens_meeting_calendar_empty_title), style = MaterialTheme.typography.headlineSmall)
            Text(
                text = stringResource(Res.string.screens_meeting_calendar_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = sp.sm),
            )
            // G3 / F6 — Set Meeting Schedule CTA on the fully-empty calendar (replaces the dead-end).
            Button(
                onClick = { onAction(MeetingCalendarAction.OpenScheduleEditor) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = sp.touchTargetMin)
                    .padding(top = sp.lg)
                    .semantics { contentDescription = ctaA11y }
                    .testTag(MeetingCalendarTestTags.SET_SCHEDULE_BUTTON),
                colors = ButtonDefaults.buttonColors(),
            ) {
                Icon(imageVector = Icons.Filled.EditCalendar, contentDescription = null)
                Text(
                    text = stringResource(Res.string.screens_meeting_calendar_set_schedule_button),
                    modifier = Modifier.padding(start = sp.xs),
                )
            }
        }
    }
}

/** `MeetingCalendarScreenState.Error` — full-screen error surface with Retry. */
@Composable
internal fun MeetingCalendarErrorSection(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val errorIconCd = stringResource(Res.string.screens_meeting_calendar_error_icon_cd)
    Column(
        modifier = modifier.fillMaxSize().padding(sp.lg).testTag(MeetingCalendarTestTags.ERROR_SECTION),
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
            text = stringResource(Res.string.screens_meeting_calendar_error_title),
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
                .testTag(MeetingCalendarTestTags.ERROR_RETRY_BUTTON),
        ) {
            Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
            Text(text = stringResource(Res.string.screens_meeting_calendar_action_retry), modifier = Modifier.padding(start = sp.xs))
        }
    }
}
