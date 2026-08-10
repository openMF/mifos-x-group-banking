/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.organizerdashboard

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
import kpt.core.base.store.screen.ScreenState
import kpt.core.base.ui.viewmodel.BaseViewModel
import kpt.core.data.repository.OrganizerDashboardRepository
import kpt.core.model.OrganizerActivityItem
import kpt.core.model.OrganizerDashboardSummary
import kpt.core.model.ScheduledMeeting

private const val TAG = "OrganizerDashboardViewModel"

/**
 * Screen-level render state for `organizer-dashboard-screen` — verbatim mirror of
 * ui.yaml#state_model.screen_state (`[Loading, Content, Error, Empty]`). Derived (not stored) from
 * [OrganizerDashboardState.isLoading] / [OrganizerDashboardState.error] /
 * [OrganizerDashboardState.myGroupCount] via the [OrganizerDashboardState.screenState] extension
 * below, so there is exactly one source of truth. See API.md#state.
 */
@Serializable
sealed interface OrganizerDashboardScreenState {
    @Serializable
    data object Loading : OrganizerDashboardScreenState

    @Serializable
    data object Content : OrganizerDashboardScreenState

    @Serializable
    data object Empty : OrganizerDashboardScreenState

    @Serializable
    data object Error : OrganizerDashboardScreenState
}

/**
 * Error taxonomy for the organizer-dashboard read — verbatim mirror of
 * ui.yaml#state_model.errors.types. [messageKey] is a composeResources string-resource id (never a
 * raw hardcoded English string, per RULE-IMPL-NO-HARDCODED-STRING-001) resolved by the Screen layer.
 *
 * [Auth] declares `redirect: group-list` in ui.yaml; `state_model.events.members` has no dedicated
 * "navigate to login" member, so [Auth] is surfaced as a non-retryable error state PLUS the
 * `SessionManager` is ended so the app shell can drive re-auth (same discipline as
 * `field-officer-dashboard`). See API.md#state.
 */
@Serializable
sealed interface OrganizerDashboardError {
    val retry: Boolean
    val messageKey: String

    @Serializable
    data object Network : OrganizerDashboardError {
        override val retry: Boolean = true
        override val messageKey: String = "error_network"
    }

    @Serializable
    data object Server : OrganizerDashboardError {
        override val retry: Boolean = true
        override val messageKey: String = "error_server"
    }

    @Serializable
    data object Auth : OrganizerDashboardError {
        override val retry: Boolean = false
        override val messageKey: String = "error_auth"
    }
}

/**
 * MVI state for `OrganizerDashboardViewModel`. Field set + defaults are a verbatim mirror of
 * ui.yaml#state_model.state. [todaySchedule], [recentActivity], [error] are `@Transient` — the lists
 * are always re-derived from [OrganizerDashboardRepository.organizerDashboardStream] on
 * (re)subscription (offline-first cache, so nothing is visually lost across process death — the
 * store, not this transient render state, is the durable source), and neither [ScheduledMeeting]
 * nor [OrganizerActivityItem] nor [OrganizerDashboardError] is `@Serializable`. Mirrors the
 * `FieldOfficerDashboardState` `@Transient` convention. See API.md#state.
 */
@Serializable
@Immutable
data class OrganizerDashboardState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val organizerName: String = "",
    val myGroupCount: Int = 0,
    val totalMembers: Int = 0,
    val pendingShareOutCount: Int = 0,
    val meetingsTodayCount: Int = 0,
    val fieldOfficerEnabled: Boolean = false,
    /** G13 — top-bar overflow (more_vert) dropdown open state (`ui.yaml#state.isMoreMenuExpanded`). */
    val isMoreMenuExpanded: Boolean = false,
    @Transient
    val todaySchedule: List<ScheduledMeeting> = emptyList(),
    @Transient
    val recentActivity: List<OrganizerActivityItem> = emptyList(),
    @Transient
    val error: OrganizerDashboardError? = null,
)

/** Derived, single-source-of-truth screen state — see [OrganizerDashboardScreenState] KDoc. */
val OrganizerDashboardState.screenState: OrganizerDashboardScreenState
    get() = when {
        error != null -> OrganizerDashboardScreenState.Error
        isLoading -> OrganizerDashboardScreenState.Loading
        // A genuinely-zero-groups organizer is a legitimate Empty state (ui.yaml#states.empty):
        // "Staff has no groups assigned; empty state with CTA to groups screen".
        myGroupCount == 0 -> OrganizerDashboardScreenState.Empty
        else -> OrganizerDashboardScreenState.Content
    }

/**
 * One-shot side effects emitted by `OrganizerDashboardViewModel` — verbatim mirror of
 * ui.yaml#state_model.events.members. See API.md#events.
 */
sealed interface OrganizerDashboardEvent {
    data object NavigateToGroupList : OrganizerDashboardEvent
    data object NavigateToFieldOfficerDashboard : OrganizerDashboardEvent

    /**
     * G7 — Today's-Schedule row tap ([OrganizerDashboardAction.OnMeetingGroupClick]) and the
     * Meetings-Today KPI card ([OrganizerDashboardAction.OnViewMeetingsToday]) open the tapped
     * group's meeting calendar (`ui.yaml#events.NavigateToMeetingCalendar` params `groupId: Int`).
     * [groupId] is the domain-side identifier as it exists on [ScheduledMeeting.groupId] (a String);
     * the NavHost seam bridges it to meeting-calendar's `groupId: Int` nav-param
     * (`groupId.toIntOrNull() ?: 0`), matching the group-dashboard → meeting-calendar precedent.
     */
    data class NavigateToMeetingCalendar(val groupId: String) : OrganizerDashboardEvent

    /**
     * G4 — deferred-notifications side effect. The top-bar bell shows a snackbar informing the
     * organizer the in-app notifications centre arrives in a later release (deferred per
     * `release_plan.deferred[]`); no navigation until that screen ships
     * (`ui.yaml#events.NotificationsDeferred`).
     */
    data object NotificationsDeferred : OrganizerDashboardEvent

    /** G13 — top-bar overflow menu → shared settings screen. */
    data object NavigateToSettings : OrganizerDashboardEvent

    /** G13 — top-bar overflow menu → shared offline sync-status dashboard. */
    data object NavigateToSyncStatus : OrganizerDashboardEvent
    data class ShowSnackbar(val message: String) : OrganizerDashboardEvent
}

/**
 * User intents dispatched to `OrganizerDashboardViewModel`. The declared members mirror
 * ui.yaml#state_model.actions.members plus the top-bar `OnOpenNotifications` action — every
 * `on_click.action` in ui.yaml is represented (RULE-IMPL-DEAD-CLICKABLE-001 Rule 1). [Internal] is
 * the sanctioned async-result-routing sub-interface (never a user intent).
 *
 * **Reconciliation note (documented, not invented):** ui.yaml's `schedule_list_item.on_click`
 * ([OnMeetingGroupClick]) declares `target: group-list` with a `groupId` param, but
 * `state_model.events.members` declares only a param-less `NavigateToGroupList`. Rather than invent
 * an unlisted group-pre-filtered event, [OnMeetingGroupClick] emits the declared
 * [OrganizerDashboardEvent.NavigateToGroupList] (the tap DOES navigate to group-list — the declared
 * target); the group-pre-filter param has no declared event carrier and is flagged for an
 * idea-layer `ui.yaml#events` follow-up (add a `NavigateToGroupList(groupId)` member). See
 * API.md#actions.
 */
sealed interface OrganizerDashboardAction {
    data object LoadDashboard : OrganizerDashboardAction
    data object OnViewAllGroups : OrganizerDashboardAction
    data object OnViewFieldOfficer : OrganizerDashboardAction
    data class OnMeetingGroupClick(val groupId: String) : OrganizerDashboardAction
    data object OnViewMeetingsToday : OrganizerDashboardAction
    data object OnOpenNotifications : OrganizerDashboardAction

    /** G13 — top-bar overflow (more_vert) toggle; flips [OrganizerDashboardState.isMoreMenuExpanded]. */
    data object OnMoreOptions : OrganizerDashboardAction

    /** G13 — overflow menu "Settings" item (collapses the menu + navigates to settings). */
    data object OnOpenSettings : OrganizerDashboardAction

    /** G13 — overflow menu "Sync Status" item (collapses the menu + navigates to sync-status). */
    data object OnSyncStatus : OrganizerDashboardAction
    data object OnRefresh : OrganizerDashboardAction
    data object Retry : OrganizerDashboardAction

    /** Async stream emissions — routed via `trySendAction`, never dispatched by the UI. */
    sealed interface Internal : OrganizerDashboardAction {
        data class StreamUpdated(val screenState: ScreenState<OrganizerDashboardSummary>) : Internal
    }
}

/**
 * MVI processor for the organizer-dashboard screen — a read-only "my groups" hub
 * (`business_logic.kind: crud`, so the offline-first `ScreenDataStream` is consumed directly).
 * [crashReporter] + [analytics] are wired for feature-level observability; [sessionManager] is ended
 * on an `Unauthenticated` stream emission (the `redirect: group-list` gap — see
 * [OrganizerDashboardError] KDoc). See API.md#viewmodel.
 */
internal class OrganizerDashboardViewModel(
    private val repository: OrganizerDashboardRepository,
    private val sessionManager: SessionManager,
    private val crashReporter: CrashReporter,
    private val analytics: KptAnalyticsTracker,
) : BaseViewModel<OrganizerDashboardState, OrganizerDashboardEvent, OrganizerDashboardAction>(
    initialState = OrganizerDashboardState(),
) {

    /** Offline-first single-key stream for the authenticated organizer. */
    private val dashboardStream = repository.organizerDashboardStream(scope = viewModelScope)

    init {
        crashReporter.recordMessage(
            message = "feature=organizer-dashboard screen=organizer-dashboard-screen",
            level = CrashSeverity.Debug,
        )
        analytics.trackGroupOperation(operation = "view_organizer_dashboard")
        viewModelScope.launch {
            dashboardStream.state.collect { screenState ->
                trySendAction(OrganizerDashboardAction.Internal.StreamUpdated(screenState))
            }
        }
    }

    override fun handleAction(action: OrganizerDashboardAction) {
        when (action) {
            OrganizerDashboardAction.LoadDashboard -> Unit // stream subscribed at init
            OrganizerDashboardAction.OnViewAllGroups -> handleViewAllGroups()
            OrganizerDashboardAction.OnViewFieldOfficer -> handleViewFieldOfficer()
            is OrganizerDashboardAction.OnMeetingGroupClick -> handleMeetingGroupClick(action.groupId)
            OrganizerDashboardAction.OnViewMeetingsToday -> handleViewMeetingsToday()
            OrganizerDashboardAction.OnOpenNotifications -> handleOpenNotifications()
            OrganizerDashboardAction.OnMoreOptions -> handleMoreOptions()
            OrganizerDashboardAction.OnOpenSettings -> handleOpenSettings()
            OrganizerDashboardAction.OnSyncStatus -> handleSyncStatus()
            OrganizerDashboardAction.OnRefresh -> handleRefresh()
            OrganizerDashboardAction.Retry -> handleRetry()
            is OrganizerDashboardAction.Internal.StreamUpdated -> handleStreamUpdated(action.screenState)
        }
    }

    // -- KPI cards + All-Groups quick-nav (ui.yaml effect: navigate, target: group-list) ----------

    private fun handleViewAllGroups() {
        analytics.trackGroupOperation(operation = "nav_group_list")
        Logger.i(TAG) { "view all groups tapped" }
        sendEvent(OrganizerDashboardEvent.NavigateToGroupList)
    }

    // -- Field-officer quick-nav (ui.yaml effect: navigate; guarded by fieldOfficerEnabled) -------

    private fun handleViewFieldOfficer() {
        if (!state.fieldOfficerEnabled) {
            Logger.w(TAG) { "OnViewFieldOfficer ignored — fieldOfficerEnabled is false" }
            return
        }
        analytics.trackGroupOperation(operation = "nav_field_officer")
        Logger.i(TAG) { "view field officer tapped" }
        sendEvent(OrganizerDashboardEvent.NavigateToFieldOfficerDashboard)
    }

    // -- Today's-Schedule meeting row tap (G7 — ui.yaml effect: navigate, target: meeting-calendar) --

    private fun handleMeetingGroupClick(groupId: String) {
        analytics.trackGroupOperation(operation = "schedule_group_tapped", groupId = groupId)
        Logger.i(TAG) { "schedule meeting row tapped groupId=$groupId — opening meeting calendar" }
        sendEvent(OrganizerDashboardEvent.NavigateToMeetingCalendar(groupId = groupId))
    }

    // -- Meetings-Today KPI card (G7 — opens the earliest today's meeting's calendar) --------------
    // ui.yaml#on_view_meetings_today: guard `meetingsTodayCount > 0`, navigate meeting-calendar with
    // `todaySchedule.first().groupId`. When there are no meetings today the card is non-interactive
    // (empty schedule) so this is a no-op guard rather than a broken navigate.

    private fun handleViewMeetingsToday() {
        val firstMeeting = state.todaySchedule.firstOrNull()
        if (state.meetingsTodayCount <= 0 || firstMeeting == null) {
            Logger.w(TAG) { "OnViewMeetingsToday ignored — no meetings scheduled today" }
            return
        }
        analytics.trackGroupOperation(operation = "view_meetings_today", groupId = firstMeeting.groupId)
        Logger.i(TAG) { "meetings-today KPI tapped — opening meeting calendar for groupId=${firstMeeting.groupId}" }
        sendEvent(OrganizerDashboardEvent.NavigateToMeetingCalendar(groupId = firstMeeting.groupId))
    }

    // -- Notifications bell (G4 — ui.yaml effect: emit_event NotificationsDeferred snackbar) --------

    private fun handleOpenNotifications() {
        Logger.i(TAG) { "notifications tapped — deferred feature" }
        sendEvent(OrganizerDashboardEvent.NotificationsDeferred)
    }

    // -- Top-bar overflow menu (G13 — transform_state toggle + two navigate items) -----------------

    private fun handleMoreOptions() {
        Logger.i(TAG) { "OnMoreOptions — toggling overflow menu" }
        updateState { copy(isMoreMenuExpanded = !isMoreMenuExpanded) }
    }

    private fun handleOpenSettings() {
        Logger.i(TAG) { "OnOpenSettings — opening settings" }
        updateState { copy(isMoreMenuExpanded = false) }
        sendEvent(OrganizerDashboardEvent.NavigateToSettings)
    }

    private fun handleSyncStatus() {
        Logger.i(TAG) { "OnSyncStatus — opening sync status" }
        updateState { copy(isMoreMenuExpanded = false) }
        sendEvent(OrganizerDashboardEvent.NavigateToSyncStatus)
    }

    // -- Pull to refresh (data-flow.yaml on_refresh: bypass_and_refresh) ---------------------------

    private fun handleRefresh() {
        analytics.trackGroupOperation(operation = "refresh_organizer_dashboard")
        Logger.i(TAG) { "pull-to-refresh triggered" }
        updateState { copy(isRefreshing = true) }
        dashboardStream.refreshFresh()
    }

    // -- Error-state retry (ui.yaml effect: call_api) ----------------------------------------------

    private fun handleRetry() {
        Logger.i(TAG) { "retry tapped — re-dispatching organizer dashboard fetch" }
        updateState { copy(error = null, isLoading = true) }
        dashboardStream.retry()
    }

    // -- Stream -> State mapping -------------------------------------------------------------------

    private fun handleStreamUpdated(screenState: ScreenState<OrganizerDashboardSummary>) {
        when (screenState) {
            is ScreenState.Loading -> updateState {
                copy(isLoading = true, error = null)
            }

            is ScreenState.Empty -> updateState {
                copy(
                    isLoading = false,
                    myGroupCount = 0,
                    todaySchedule = emptyList(),
                    recentActivity = emptyList(),
                    error = null,
                    isRefreshing = false,
                )
            }

            is ScreenState.Content -> updateState {
                val data = screenState.data
                copy(
                    isLoading = false,
                    organizerName = data.organizerName,
                    myGroupCount = data.myGroupCount,
                    totalMembers = data.totalMembers,
                    pendingShareOutCount = data.pendingShareOutCount,
                    meetingsTodayCount = data.meetingsTodayCount,
                    fieldOfficerEnabled = data.fieldOfficerEnabled,
                    todaySchedule = data.todaySchedule,
                    recentActivity = data.recentActivity,
                    error = null,
                    isRefreshing = false,
                )
            }

            is ScreenState.NoNetwork -> updateState {
                copy(isLoading = false, error = OrganizerDashboardError.Network, isRefreshing = false)
            }

            is ScreenState.Unauthenticated -> {
                crashReporter.recordMessage(
                    message = "organizer-dashboard: session expired (401)",
                    level = CrashSeverity.Warning,
                )
                sessionManager.endSession()
                updateState { copy(isLoading = false, error = OrganizerDashboardError.Auth, isRefreshing = false) }
                sendEvent(OrganizerDashboardEvent.ShowSnackbar(message = OrganizerDashboardError.Auth.messageKey))
            }

            is ScreenState.Error -> {
                crashReporter.recordException(
                    throwable = screenState.error,
                    message = "organizer-dashboard: stream error isNetworkError=${screenState.isNetworkError}",
                )
                val mapped = if (screenState.isNetworkError) {
                    OrganizerDashboardError.Network
                } else {
                    OrganizerDashboardError.Server
                }
                updateState { copy(isLoading = false, error = mapped, isRefreshing = false) }
            }
        }
    }
}
