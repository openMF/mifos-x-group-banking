/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.meetingcalendar

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kpt.core.analytics.KptAnalyticsTracker
import kpt.core.base.observability.CrashReporter
import kpt.core.base.observability.CrashSeverity
import kpt.core.base.security.SessionManager
import kpt.core.base.store.screen.ScreenDataStream
import kpt.core.base.store.screen.ScreenState
import kpt.core.base.store.screen.emptyIfContent
import kpt.core.base.ui.viewmodel.BaseViewModel
import org.mifos.groupbanking.core.data.repository.MeetingRepository
import org.mifos.groupbanking.core.model.MeetingFrequency
import org.mifos.groupbanking.core.model.MeetingListItem
import org.mifos.groupbanking.core.model.RescheduleMeetingRequest

private const val TAG = "MeetingCalendarViewModel"

/** Message key the Screen resolves to the "schedule updated" snackbar text (G3 / F6). */
private const val SCHEDULE_UPDATED_MESSAGE_KEY = "schedule_updated"

/** Layout mode for the meetings list — `ui.yaml#state_model.state.viewMode` (default LIST). */
enum class ViewMode { LIST, CALENDAR }

/**
 * Screen-level render state for `meeting-calendar-screen` — verbatim mirror of
 * `ui.yaml#state_model.MeetingCalendarViewModel.screen_state` (Loading/Content/Empty/Error).
 * Derived (not stored) from [MeetingCalendarState.isLoading] / [MeetingCalendarState.error] /
 * [MeetingCalendarState.meetings] via the [MeetingCalendarState.screenState] extension below —
 * exactly one source of truth, mirroring `LoanListState`'s identical convention. See API.md#state.
 */
@Serializable
sealed interface MeetingCalendarScreenState {
    @Serializable
    data object Loading : MeetingCalendarScreenState

    @Serializable
    data object Content : MeetingCalendarScreenState

    @Serializable
    data object Empty : MeetingCalendarScreenState

    @Serializable
    data object Error : MeetingCalendarScreenState
}

/**
 * Error taxonomy for the meeting-calendar read — mirrors `ui.yaml#state_model.errors` +
 * `data-flow.yaml#error_paths`. [messageKey] is a composeResources string-resource id (never a raw
 * hardcoded English string, per RULE-IMPL-NO-HARDCODED-STRING-001) resolved by the Screen layer.
 *
 * `data-flow.yaml` declares `401 -> navigate: login`, but `state_model.events.members` declares no
 * `NavigateToLogin` event — mirrors `LoanListError.Auth`'s identical documented gap. [Auth] is
 * surfaced as an ordinary non-retryable error state, `sessionManager.endSession()` is called for
 * real, and the closest declared event ([MeetingCalendarEvent.ShowError]) is emitted carrying the
 * `error_auth` message key. Reported to the caller for an idea-layer `ui.yaml#events` update.
 */
@Serializable
sealed interface MeetingCalendarError {
    val retry: Boolean
    val messageKey: String

    @Serializable
    data object Network : MeetingCalendarError {
        override val retry: Boolean = true
        override val messageKey: String = "error_network"
    }

    @Serializable
    data object Server : MeetingCalendarError {
        override val retry: Boolean = true
        override val messageKey: String = "error_server"
    }

    @Serializable
    data object Auth : MeetingCalendarError {
        override val retry: Boolean = false
        override val messageKey: String = "error_auth"
    }
}

/**
 * MVI state for `MeetingCalendarViewModel`. Field set + defaults are a verbatim mirror of
 * `ui.yaml#state_model.MeetingCalendarViewModel.state`. [meetings] and [error] are `@Transient` —
 * the list is always re-derived from [MeetingRepository.meetingsStream] on (re)subscription
 * (offline-first cache via the `meeting_calendar_cache` Room table, so nothing is visually lost
 * across process death — the store, not this transient render state, is the durable source), and
 * [MeetingListItem] itself is not `@Serializable`. Mirrors `LoanListState`'s identical `@Transient`
 * convention. See API.md#state.
 */
@Serializable
@Immutable
data class MeetingCalendarState(
    val isLoading: Boolean = true,
    @Transient
    val meetings: List<MeetingListItem> = emptyList(),
    val isRefreshing: Boolean = false,
    val viewMode: ViewMode = ViewMode.LIST,
    @Transient
    val error: MeetingCalendarError? = null,
    val groupId: Int = 0,
    // G3 / F6 — schedule editor (mirror of `ui.yaml#state_model` schedule fields). The recurrence
    // Calendar id is unresolved at this read seam (the meetings list carries none) until the companion
    // calendar API lands; kept nullable so the RescheduleMeetingRequest payload carries it when available.
    val calendarId: String? = null,
    val showScheduleEditor: Boolean = false,
    val scheduleDay: String = "",
    val scheduleTime: String = "",
    val scheduleFrequency: MeetingFrequency = MeetingFrequency.WEEKLY,
    val isRescheduling: Boolean = false,
)

/** Derived, single-source-of-truth screen state — see [MeetingCalendarScreenState] KDoc. */
val MeetingCalendarState.screenState: MeetingCalendarScreenState
    get() = when {
        error != null -> MeetingCalendarScreenState.Error
        isLoading -> MeetingCalendarScreenState.Loading
        meetings.isEmpty() -> MeetingCalendarScreenState.Empty
        else -> MeetingCalendarScreenState.Content
    }

/** The single pinned UPCOMING meeting (if any) — drives the "Start Meeting" card. */
val MeetingCalendarState.upcomingMeeting: MeetingListItem?
    get() = meetings.firstOrNull { it.status == org.mifos.groupbanking.core.model.MeetingStatus.UPCOMING }

/**
 * Derived single-source-of-truth for `ui.yaml#hasUpcoming` — true when a pinned UPCOMING meeting
 * exists. Gates the upcoming card + "Reschedule" affordance (`hasUpcoming`) vs the
 * `no_upcoming_schedule_card` "Set / Adjust Schedule" CTA (`!hasUpcoming`) — replaces the former dead
 * "Next meeting not scheduled" placeholder (G3 / F6).
 */
val MeetingCalendarState.hasUpcoming: Boolean
    get() = upcomingMeeting != null

/**
 * The past (COMPLETED / MISSED) meetings, newest-first (descending by meeting number, which tracks
 * chronological order) so the most recent meeting is at the top of the list. The companion emits the
 * schedule oldest-first (recurrence order), so sort here rather than rely on stream order.
 */
val MeetingCalendarState.pastMeetings: List<MeetingListItem>
    get() = meetings
        .filter { it.status != org.mifos.groupbanking.core.model.MeetingStatus.UPCOMING }
        .sortedByDescending { it.meetingNumber }

/**
 * One-shot side effects emitted by `MeetingCalendarViewModel` — verbatim mirror of
 * `ui.yaml#state_model.MeetingCalendarViewModel.events.members`. See API.md#events.
 */
sealed interface MeetingCalendarEvent {
    data class NavigateToConduct(val meetingId: String, val meetingNumber: Int) : MeetingCalendarEvent

    /**
     * G5 — open a past meeting in previous-meeting-review. Carries [groupId] and
     * `launchedFrom = "calendar"` so the review renders in calendar-context (hides the
     * conduct-only "Start Meeting #N" CTA); previous-meeting-review requires all four nav_params.
     */
    data class NavigateToReview(
        val meetingId: String,
        val meetingNumber: Int,
        val groupId: Int,
        val launchedFrom: String = "calendar",
    ) : MeetingCalendarEvent
    data class ShowError(val message: String) : MeetingCalendarEvent

    /** G3 / F6 — schedule saved (queued offline); carries the confirmation message key for the snackbar. */
    data class ShowScheduleUpdated(val message: String) : MeetingCalendarEvent
}

/**
 * User intents dispatched to `MeetingCalendarViewModel`. Mirrors
 * `ui.yaml#state_model.MeetingCalendarViewModel.actions.members` (RULE-IMPL-DEAD-CLICKABLE-001
 * Rule 1). `LoadMeetings` ("Screen enters composition") is folded into `init`'s stream
 * subscription rather than a dispatchable member — mirrors `LoanListViewModel`'s identical
 * fold-load-into-init precedent. [Retry] re-drives the error state (the `error_banner` retry CTA
 * maps to a fresh fetch, same as [RefreshMeetings]). [Internal] is the sanctioned
 * async-result-routing sub-interface (never a user intent). See API.md#actions.
 */
sealed interface MeetingCalendarAction {
    data object RefreshMeetings : MeetingCalendarAction
    data object ToggleViewMode : MeetingCalendarAction
    data class StartMeeting(val meetingId: String, val meetingNumber: Int) : MeetingCalendarAction
    data class OpenPastMeeting(val meetingId: String, val meetingNumber: Int) : MeetingCalendarAction
    data object Retry : MeetingCalendarAction

    // -- G3 / F6 schedule editor (mirror of `ui.yaml#state_model.actions.members`) ------------------

    /** Opens the schedule-editor sheet — Reschedule affordance OR the Set/Adjust Schedule CTA. */
    data object OpenScheduleEditor : MeetingCalendarAction

    /**
     * One draft-field edit in the schedule sheet. Each editor field dispatches only its own param
     * (day / time / frequency); the reducer applies whichever is non-null — one action verbatim per
     * `ui.yaml` (`OnScheduleFieldChange`).
     */
    data class OnScheduleFieldChange(
        val day: String? = null,
        val time: String? = null,
        val frequency: MeetingFrequency? = null,
    ) : MeetingCalendarAction

    /** Confirm/Save in the schedule sheet — offline-queues the recurrence adjustment (server-gated). */
    data class RescheduleMeeting(
        val day: String,
        val time: String,
        val frequency: MeetingFrequency,
    ) : MeetingCalendarAction

    /** Cancel/scrim on the schedule sheet — closes it and discards the draft. */
    data object DismissScheduleEditor : MeetingCalendarAction

    /** Async stream emissions — routed via `trySendAction`, never dispatched by the UI. */
    sealed interface Internal : MeetingCalendarAction {
        data class StreamUpdated(val screenState: ScreenState<List<MeetingListItem>>) : Internal

        /** Offline-queue enqueue completed for the reschedule write — carries the queue row id. */
        data class RescheduleQueued(val queuedId: Long) : Internal
    }
}

/**
 * MVI processor for the meeting-calendar screen (`business_logic.kind: crud` per ui.yaml — a
 * read-only offline-first meetings list, so [MeetingRepository.meetingsStream]'s
 * [ScreenDataStream] is consumed directly). [crashReporter] + [analytics] are wired for
 * feature-level observability; [sessionManager] handles the real `ScreenState.Unauthenticated` ->
 * `endSession()` branch (mirrors `LoanListViewModel`). `NetworkMonitor` (declared in
 * `ui.yaml#state_model.di`) is not re-injected here — it is already composed inside
 * `MeetingRepositoryImpl.meetingsStream` (`Store.asScreenStream(networkMonitor = ...)`).
 *
 * [groupId] is the `ui.yaml#nav_params` value forwarded from `group-dashboard` (or `bottom_nav`)
 * via Koin `parametersOf(groupId)` — it seeds [MeetingCalendarState.groupId] and scopes the
 * [MeetingRepository.meetingsStream] read. See API.md#viewmodel.
 */
internal class MeetingCalendarViewModel(
    private val repository: MeetingRepository,
    private val sessionManager: SessionManager,
    private val crashReporter: CrashReporter,
    private val analytics: KptAnalyticsTracker,
    private val groupId: Int,
) : BaseViewModel<MeetingCalendarState, MeetingCalendarEvent, MeetingCalendarAction>(
    initialState = MeetingCalendarState(groupId = groupId),
) {

    /** Fixed-key offline-first stream for [groupId] — see class KDoc. */
    private val meetingsStream: ScreenDataStream<List<MeetingListItem>> =
        repository.meetingsStream(groupId = groupId, scope = viewModelScope)

    init {
        crashReporter.recordMessage(
            message = "feature=meeting-calendar screen=meeting-calendar-screen groupId=$groupId",
            level = CrashSeverity.Debug,
        )
        analytics.trackSync(syncType = "view_meetings")
        viewModelScope.launch {
            // emptyIfContent maps an empty-list Content to ScreenState.Empty (the "no meetings
            // scheduled" state). Without it, an empty meetings list can never reach the Empty state.
            meetingsStream.state
                .emptyIfContent { it.isEmpty() }
                .collect { screenState ->
                    trySendAction(MeetingCalendarAction.Internal.StreamUpdated(screenState))
                }
        }
    }

    override fun handleAction(action: MeetingCalendarAction) {
        when (action) {
            MeetingCalendarAction.RefreshMeetings -> handleRefresh()
            MeetingCalendarAction.ToggleViewMode -> handleToggleViewMode()
            is MeetingCalendarAction.StartMeeting -> handleStartMeeting(action.meetingId, action.meetingNumber)
            is MeetingCalendarAction.OpenPastMeeting -> handleOpenPastMeeting(action.meetingId, action.meetingNumber)
            MeetingCalendarAction.Retry -> handleRetry()
            MeetingCalendarAction.OpenScheduleEditor -> handleOpenScheduleEditor()
            is MeetingCalendarAction.OnScheduleFieldChange -> handleScheduleFieldChange(action)
            is MeetingCalendarAction.RescheduleMeeting -> handleReschedule(action.day, action.time, action.frequency)
            MeetingCalendarAction.DismissScheduleEditor -> handleDismissScheduleEditor()
            is MeetingCalendarAction.Internal.StreamUpdated -> handleStreamUpdated(action.screenState)
            is MeetingCalendarAction.Internal.RescheduleQueued -> handleRescheduleQueued(action.queuedId)
        }
    }

    // -- Start Meeting (ui.yaml effect: navigate, target: meeting-conduct) --------------------------

    private fun handleStartMeeting(meetingId: String, meetingNumber: Int) {
        analytics.trackSync(syncType = "start_meeting")
        Logger.i(TAG) { "start meeting tapped meetingId=$meetingId #$meetingNumber groupId=$groupId" }
        sendEvent(MeetingCalendarEvent.NavigateToConduct(meetingId, meetingNumber))
    }

    // -- Open past meeting (ui.yaml effect: navigate, target: previous-meeting-review) --------------

    private fun handleOpenPastMeeting(meetingId: String, meetingNumber: Int) {
        analytics.trackSync(syncType = "open_past_meeting")
        Logger.i(TAG) { "past meeting tapped meetingId=$meetingId #$meetingNumber groupId=$groupId" }
        sendEvent(MeetingCalendarEvent.NavigateToReview(meetingId, meetingNumber, groupId, launchedFrom = "calendar"))
    }

    // -- Toggle view mode (ui.yaml effect: transform_state, pure in-memory viewMode flip) ----------

    private fun handleToggleViewMode() {
        updateState {
            val next = if (viewMode == ViewMode.LIST) ViewMode.CALENDAR else ViewMode.LIST
            Logger.d(TAG) { "view mode toggled -> $next groupId=$groupId" }
            copy(viewMode = next)
        }
    }

    // -- Pull to refresh / error-banner retry (data-flow.yaml on_refresh: network_first) ------------

    private fun handleRefresh() {
        analytics.trackSync(syncType = "refresh_meetings")
        Logger.i(TAG) { "refresh meetings triggered groupId=$groupId" }
        updateState { copy(isRefreshing = true) }
        meetingsStream.refreshFresh()
    }

    private fun handleRetry() {
        Logger.i(TAG) { "retry tapped — re-dispatching meetings fetch groupId=$groupId" }
        updateState { copy(error = null, isLoading = true) }
        meetingsStream.retry()
    }

    // -- G3 / F6 schedule editor (Reschedule affordance + Set/Adjust Schedule CTA) -------------------

    /** Opens the schedule-editor sheet (pure state flip) — see `ui.yaml#OpenScheduleEditor`. */
    private fun handleOpenScheduleEditor() {
        Logger.d(TAG) { "schedule editor opened groupId=$groupId hasUpcoming=${state.hasUpcoming}" }
        updateState { copy(showScheduleEditor = true) }
    }

    /** Updates whichever draft schedule field changed — one action per `ui.yaml#OnScheduleFieldChange`. */
    private fun handleScheduleFieldChange(action: MeetingCalendarAction.OnScheduleFieldChange) {
        updateState {
            copy(
                scheduleDay = action.day ?: scheduleDay,
                scheduleTime = action.time ?: scheduleTime,
                scheduleFrequency = action.frequency ?: scheduleFrequency,
            )
        }
    }

    /** Closes the schedule-editor sheet and discards the draft — `ui.yaml#DismissScheduleEditor`. */
    private fun handleDismissScheduleEditor() {
        updateState { copy(showScheduleEditor = false) }
    }

    /**
     * Confirm/Save — offline-queues the recurrence adjustment (server-gated companion_update_calendar
     * is pending-device-verify; [MeetingRepository.rescheduleMeeting] enqueues to sync_queue). On enqueue
     * it dismisses the editor, stops the spinner, emits [MeetingCalendarEvent.ShowScheduleUpdated], and
     * re-drives a fresh meetings fetch so the shifted recurrence renders when the server confirms.
     */
    private fun handleReschedule(day: String, time: String, frequency: MeetingFrequency) {
        if (state.isRescheduling) {
            Logger.w(TAG) { "reschedule ignored — already in flight groupId=$groupId" }
            return
        }
        analytics.trackSync(syncType = "reschedule_meeting")
        Logger.i(TAG) { "reschedule confirmed groupId=$groupId day=$day time=$time frequency=$frequency (server-gated → offline queue)" }
        updateState { copy(isRescheduling = true) }
        viewModelScope.launch {
            val queuedId = repository.rescheduleMeeting(
                RescheduleMeetingRequest(
                    groupId = groupId,
                    calendarId = state.calendarId,
                    day = day,
                    time = time,
                    frequency = frequency,
                ),
            )
            trySendAction(MeetingCalendarAction.Internal.RescheduleQueued(queuedId))
        }
    }

    private fun handleRescheduleQueued(queuedId: Long) {
        Logger.i(TAG) { "reschedule queued id=$queuedId groupId=$groupId — refreshing meetings" }
        updateState { copy(isRescheduling = false, showScheduleEditor = false) }
        sendEvent(MeetingCalendarEvent.ShowScheduleUpdated(message = SCHEDULE_UPDATED_MESSAGE_KEY))
        meetingsStream.refreshFresh()
    }

    // -- Stream -> State mapping ---------------------------------------------------------------------

    private fun handleStreamUpdated(screenState: ScreenState<List<MeetingListItem>>) {
        when (screenState) {
            is ScreenState.Loading -> updateState {
                copy(isLoading = true, error = null, isRefreshing = false)
            }

            is ScreenState.Empty -> updateState {
                copy(isLoading = false, meetings = emptyList(), error = null, isRefreshing = false)
            }

            is ScreenState.Content -> updateState {
                copy(isLoading = false, meetings = screenState.data, error = null, isRefreshing = false)
            }

            is ScreenState.NoNetwork -> updateState {
                copy(isLoading = false, error = MeetingCalendarError.Network, isRefreshing = false)
            }

            is ScreenState.Unauthenticated -> {
                crashReporter.recordMessage(
                    message = "meeting-calendar: session expired (401) groupId=$groupId — clearing session",
                    level = CrashSeverity.Warning,
                )
                sessionManager.endSession()
                updateState { copy(isLoading = false, error = MeetingCalendarError.Auth, isRefreshing = false) }
                sendEvent(MeetingCalendarEvent.ShowError(message = MeetingCalendarError.Auth.messageKey))
            }

            is ScreenState.Error -> {
                crashReporter.recordException(
                    throwable = screenState.error,
                    message = "meeting-calendar: stream error groupId=$groupId isNetworkError=${screenState.isNetworkError}",
                )
                val mapped = if (screenState.isNetworkError) {
                    MeetingCalendarError.Network
                } else {
                    MeetingCalendarError.Server
                }
                updateState { copy(isLoading = false, error = mapped, isRefreshing = false) }
            }
        }
    }
}
