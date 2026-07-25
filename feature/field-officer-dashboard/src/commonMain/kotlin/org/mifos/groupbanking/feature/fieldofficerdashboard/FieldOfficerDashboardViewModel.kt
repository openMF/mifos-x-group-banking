/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.fieldofficerdashboard

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kpt.core.analytics.KptAnalyticsTracker
import kpt.core.base.network.NetworkResult
import kpt.core.base.observability.CrashReporter
import kpt.core.base.observability.CrashSeverity
import kpt.core.base.security.SessionManager
import kpt.core.base.store.screen.ScreenState
import kpt.core.base.ui.viewmodel.BaseViewModel
import org.mifos.groupbanking.core.data.repository.FieldOfficerDashboardRepository
import org.mifos.groupbanking.core.model.FieldOfficerDashboard
import org.mifos.groupbanking.core.model.GroupHealthSummary
import org.mifos.groupbanking.core.model.GroupStatusFilter
import org.mifos.groupbanking.core.model.OverdueRateFilter

private const val TAG = "FieldOfficerDashboardViewModel"

/**
 * The session-derived staffId + role the field-officer dashboard is scoped to.
 *
 * **Idea-layer / infra gap (flagged, not invented here):** `ui.yaml#nav_params` declares
 * `staffId { source: session }` and the state carries `userRole`, but no session-staff accessor
 * exists in this build — `SessionManager` (core-base/security) carries only inactivity-timeout
 * state, and `AuthSession` carries `userId`, not a Fineract staffId/role. The dashboard is therefore
 * scoped with the default sentinel below; the Fineract reads resolve the concrete staff server-side
 * once a `/companion/staff/me`-style accessor lands. Reported for an idea-layer/infra follow-up.
 */
private const val DEFAULT_STAFF_ID: Long = 0L
private const val DEFAULT_USER_ROLE: String = FieldOfficerDashboard.ROLE_FIELD_OFFICER

/**
 * Screen-level render state for `field-officer-dashboard-screen` — verbatim mirror of
 * ui.yaml#state_model.screen_state. Derived (not stored) from [FieldOfficerDashboardState.isLoading]
 * / [FieldOfficerDashboardState.error] / [FieldOfficerDashboardState.groups] via the
 * [FieldOfficerDashboardState.screenState] extension below, so there is exactly one source of truth.
 *
 * Note: the derivation reads [FieldOfficerDashboardState.groups] (the full unfiltered list), never
 * [FieldOfficerDashboardState.filteredGroups] — an empty *filter result* renders as an inline
 * "no matches" surface inside the [Content] state (see `NoFilterResultsState`), NOT the genuinely
 * zero-assigned-groups [Empty] state. See API.md#state.
 */
@Serializable
sealed interface FieldOfficerDashboardScreenState {
    @Serializable
    data object Loading : FieldOfficerDashboardScreenState

    @Serializable
    data object Content : FieldOfficerDashboardScreenState

    @Serializable
    data object Empty : FieldOfficerDashboardScreenState

    @Serializable
    data object Error : FieldOfficerDashboardScreenState
}

/**
 * Error taxonomy for the field-officer-dashboard read — verbatim mirror of
 * ui.yaml#state_model.errors.types. [messageKey] is a composeResources string-resource id (never a
 * raw hardcoded English string, per RULE-IMPL-NO-HARDCODED-STRING-001) resolved by the Screen layer.
 *
 * [Auth] declares `redirect: login` in ui.yaml but `state_model.events.members` declares no
 * `NavigateToLogin` event — per RULE-IMPL-DEAD-CLICKABLE-001 Rule 1 this ViewModel does NOT invent
 * an unlisted event: [Auth] is surfaced as a non-retryable error state PLUS the closest declared
 * event ([FieldOfficerDashboardEvent.ShowSnackbar]) carrying the `error_auth` key, and the
 * `SessionManager` is ended so the app shell can drive re-auth. Reported to the caller for an
 * idea-layer `ui.yaml#events` update (add a `NavigateToLogin` member). See API.md#state.
 */
@Serializable
sealed interface FieldOfficerDashboardError {
    val retry: Boolean
    val messageKey: String

    @Serializable
    data object Network : FieldOfficerDashboardError {
        override val retry: Boolean = true
        override val messageKey: String = "error_network"
    }

    @Serializable
    data object Server : FieldOfficerDashboardError {
        override val retry: Boolean = true
        override val messageKey: String = "error_server"
    }

    @Serializable
    data object Auth : FieldOfficerDashboardError {
        override val retry: Boolean = false
        override val messageKey: String = "error_auth"
    }

    @Serializable
    data object NoGroupsAssigned : FieldOfficerDashboardError {
        override val retry: Boolean = false
        override val messageKey: String = "error_no_groups"
    }
}

/**
 * MVI state for `FieldOfficerDashboardViewModel`. Field set + defaults are a verbatim mirror of
 * ui.yaml#state_model.state. [groups], [filteredGroups], [error] are `@Transient` — the list is
 * always re-derived from [FieldOfficerDashboardRepository.fieldOfficerDashboardStream] on
 * (re)subscription (offline-first cache, so nothing is visually lost across process death — the
 * store, not this transient render state, is the durable source), and neither [GroupHealthSummary]
 * nor [FieldOfficerDashboardError] is `@Serializable`. Mirrors `GroupListState`'s identical
 * `@Transient` convention. See API.md#state.
 */
@Serializable
@Immutable
data class FieldOfficerDashboardState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val staffId: Long = 0L,
    val userRole: String = "",
    val canExport: Boolean = false,
    val totalGroupsCount: Int = 0,
    val totalActiveMembers: Int = 0,
    val totalSavingsThisMonth: Double = 0.0,
    val totalLoansOutstanding: Double = 0.0,
    @Transient
    val groups: List<GroupHealthSummary> = emptyList(),
    @Transient
    val filteredGroups: List<GroupHealthSummary> = emptyList(),
    val availableRegions: List<String> = emptyList(),
    val selectedRegionFilter: String? = null,
    val selectedStatusFilter: GroupStatusFilter? = null,
    val selectedOverdueFilter: OverdueRateFilter? = null,
    val isExporting: Boolean = false,
    @Transient
    val error: FieldOfficerDashboardError? = null,
) {
    /** True when any of the three filter chips is active — drives the Clear-filters chip visibility. */
    val hasActiveFilter: Boolean
        get() = selectedRegionFilter != null || selectedStatusFilter != null || selectedOverdueFilter != null
}

/** Derived, single-source-of-truth screen state — see [FieldOfficerDashboardScreenState] KDoc. */
val FieldOfficerDashboardState.screenState: FieldOfficerDashboardScreenState
    get() = when {
        error != null -> FieldOfficerDashboardScreenState.Error
        isLoading -> FieldOfficerDashboardScreenState.Loading
        groups.isEmpty() -> FieldOfficerDashboardScreenState.Empty
        else -> FieldOfficerDashboardScreenState.Content
    }

/**
 * One-shot side effects emitted by `FieldOfficerDashboardViewModel` — verbatim mirror of
 * ui.yaml#state_model.events.members. See API.md#events.
 */
sealed interface FieldOfficerDashboardEvent {
    data class NavigateToGroupDashboard(val groupId: Long) : FieldOfficerDashboardEvent
    data class NavigateToExportReport(val staffId: Long) : FieldOfficerDashboardEvent
    data class ShowSnackbar(val message: String) : FieldOfficerDashboardEvent
    data object ShowRegionPickerDialog : FieldOfficerDashboardEvent
    data object ShowStatusPickerDialog : FieldOfficerDashboardEvent
    data object ShowOverduePickerDialog : FieldOfficerDashboardEvent
}

/**
 * User intents dispatched to `FieldOfficerDashboardViewModel`. The declared members mirror
 * ui.yaml#state_model.actions.members — RULE-IMPL-DEAD-CLICKABLE-001 Rule 1.
 *
 * **Reconciliation note (documented, not invented):** ui.yaml declares `ShowRegionPickerDialog` /
 * `ShowStatusPickerDialog` / `ShowOverduePickerDialog` under `events`, yet references them as each
 * filter chip's `on_click.action`. The chip tap is therefore modeled here as an explicit action
 * ([OnShowRegionPicker] / [OnShowStatusPicker] / [OnShowOverduePicker]) whose sole effect is to emit
 * the declared picker EVENT (opening the dialog); the dialog's selection then dispatches the
 * declared [OnRegionFilterSelected] / [OnStatusFilterSelected] / [OnOverdueFilterSelected]. This
 * bridges the event-vs-action naming without inventing an unrequested affordance. [Internal] is the
 * sanctioned async-result-routing sub-interface (never a user intent). See API.md#actions.
 */
sealed interface FieldOfficerDashboardAction {
    data object LoadDashboard : FieldOfficerDashboardAction
    data class OnGroupTapped(val groupId: Long) : FieldOfficerDashboardAction
    data object OnShowRegionPicker : FieldOfficerDashboardAction
    data object OnShowStatusPicker : FieldOfficerDashboardAction
    data object OnShowOverduePicker : FieldOfficerDashboardAction
    data class OnRegionFilterSelected(val region: String?) : FieldOfficerDashboardAction
    data class OnStatusFilterSelected(val status: GroupStatusFilter?) : FieldOfficerDashboardAction
    data class OnOverdueFilterSelected(val filter: OverdueRateFilter?) : FieldOfficerDashboardAction
    data object OnClearFilters : FieldOfficerDashboardAction
    data object OnExportReport : FieldOfficerDashboardAction
    data object OnRefresh : FieldOfficerDashboardAction
    data object OnRetry : FieldOfficerDashboardAction

    /** Async stream emissions — routed via `trySendAction`, never dispatched by the UI. */
    sealed interface Internal : FieldOfficerDashboardAction {
        data class StreamUpdated(val screenState: ScreenState<FieldOfficerDashboard>) : Internal
    }
}

/**
 * MVI processor for the field-officer-dashboard screen (FR-009 — a read-only cross-group monitoring
 * dashboard; `business_logic.kind: crud`, so the offline-first `ScreenDataStream` is consumed
 * directly). [crashReporter] + [analytics] are wired for feature-level observability; [sessionManager]
 * is ended on an `Unauthenticated` stream emission (the `redirect: login` gap — see
 * [FieldOfficerDashboardError] KDoc). See API.md#viewmodel.
 */
internal class FieldOfficerDashboardViewModel(
    private val repository: FieldOfficerDashboardRepository,
    private val sessionManager: SessionManager,
    private val crashReporter: CrashReporter,
    private val analytics: KptAnalyticsTracker,
) : BaseViewModel<FieldOfficerDashboardState, FieldOfficerDashboardEvent, FieldOfficerDashboardAction>(
    initialState = FieldOfficerDashboardState(
        staffId = DEFAULT_STAFF_ID,
        userRole = DEFAULT_USER_ROLE,
        canExport = DEFAULT_USER_ROLE == FieldOfficerDashboard.ROLE_FIELD_OFFICER ||
            DEFAULT_USER_ROLE == FieldOfficerDashboard.ROLE_PROGRAM_MANAGER,
    ),
) {

    /** Offline-first stream for the session staff member — see `fieldOfficerDashboardStream` KDoc. */
    private val dashboardStream = repository.fieldOfficerDashboardStream(
        staffId = DEFAULT_STAFF_ID,
        userRole = DEFAULT_USER_ROLE,
        scope = viewModelScope,
    )

    init {
        crashReporter.recordMessage(
            message = "feature=field-officer-dashboard screen=field-officer-dashboard-screen",
            level = CrashSeverity.Debug,
        )
        analytics.trackGroupOperation(operation = "view_field_officer_dashboard")
        viewModelScope.launch {
            dashboardStream.state.collect { screenState ->
                trySendAction(FieldOfficerDashboardAction.Internal.StreamUpdated(screenState))
            }
        }
    }

    override fun handleAction(action: FieldOfficerDashboardAction) {
        when (action) {
            FieldOfficerDashboardAction.LoadDashboard -> Unit // stream subscribed at init
            is FieldOfficerDashboardAction.OnGroupTapped -> handleGroupTapped(action.groupId)
            FieldOfficerDashboardAction.OnShowRegionPicker ->
                sendEvent(FieldOfficerDashboardEvent.ShowRegionPickerDialog)
            FieldOfficerDashboardAction.OnShowStatusPicker ->
                sendEvent(FieldOfficerDashboardEvent.ShowStatusPickerDialog)
            FieldOfficerDashboardAction.OnShowOverduePicker ->
                sendEvent(FieldOfficerDashboardEvent.ShowOverduePickerDialog)
            is FieldOfficerDashboardAction.OnRegionFilterSelected -> handleRegionFilter(action.region)
            is FieldOfficerDashboardAction.OnStatusFilterSelected -> handleStatusFilter(action.status)
            is FieldOfficerDashboardAction.OnOverdueFilterSelected -> handleOverdueFilter(action.filter)
            FieldOfficerDashboardAction.OnClearFilters -> handleClearFilters()
            FieldOfficerDashboardAction.OnExportReport -> handleExportReport()
            FieldOfficerDashboardAction.OnRefresh -> handleRefresh()
            FieldOfficerDashboardAction.OnRetry -> handleRetry()
            is FieldOfficerDashboardAction.Internal.StreamUpdated -> handleStreamUpdated(action.screenState)
        }
    }

    // -- Group health card tap (ui.yaml effect: navigate, target: group-dashboard) ----------------

    private fun handleGroupTapped(groupId: Long) {
        analytics.trackGroupOperation(operation = "view", groupId = groupId.toString())
        Logger.i(TAG) { "group health card tapped groupId=$groupId" }
        sendEvent(FieldOfficerDashboardEvent.NavigateToGroupDashboard(groupId))
    }

    // -- Filter selection (ui.yaml effect: transform_state, client-side re-derive) -----------------

    private fun handleRegionFilter(region: String?) {
        Logger.d(TAG) { "region filter selected=$region" }
        updateState { copy(selectedRegionFilter = region, filteredGroups = applyFilters(groups, region, selectedStatusFilter, selectedOverdueFilter)) }
    }

    private fun handleStatusFilter(status: GroupStatusFilter?) {
        Logger.d(TAG) { "status filter selected=$status" }
        updateState { copy(selectedStatusFilter = status, filteredGroups = applyFilters(groups, selectedRegionFilter, status, selectedOverdueFilter)) }
    }

    private fun handleOverdueFilter(filter: OverdueRateFilter?) {
        Logger.d(TAG) { "overdue filter selected=$filter" }
        updateState { copy(selectedOverdueFilter = filter, filteredGroups = applyFilters(groups, selectedRegionFilter, selectedStatusFilter, filter)) }
    }

    private fun handleClearFilters() {
        Logger.i(TAG) { "clear filters" }
        updateState {
            copy(
                selectedRegionFilter = null,
                selectedStatusFilter = null,
                selectedOverdueFilter = null,
                filteredGroups = groups,
            )
        }
    }

    // -- Export Report (ui.yaml effect: call_api, run_report; role-gated to canExport) -------------

    private fun handleExportReport() {
        if (!state.canExport) {
            Logger.w(TAG) { "OnExportReport ignored — canExport is false" }
            return
        }
        if (state.isExporting) return
        val staffId = state.staffId
        analytics.trackReportGeneration(reportType = "FieldOfficerGroupReport", resultCount = state.totalGroupsCount)
        Logger.i(TAG) { "export report requested staffId=$staffId groups=${state.totalGroupsCount}" }
        updateState { copy(isExporting = true) }
        viewModelScope.launch {
            when (val result = repository.exportReport(staffId)) {
                is NetworkResult.Success -> {
                    updateState { copy(isExporting = false) }
                    sendEvent(FieldOfficerDashboardEvent.NavigateToExportReport(staffId))
                }

                is NetworkResult.Error -> {
                    crashReporter.recordMessage(
                        message = "field-officer-dashboard: export failed ${result.error}",
                        level = CrashSeverity.Warning,
                    )
                    updateState { copy(isExporting = false) }
                    sendEvent(FieldOfficerDashboardEvent.ShowSnackbar(message = "export_error"))
                }
            }
        }
    }

    // -- Pull to refresh (data-flow.yaml on_refresh: network_first) --------------------------------

    private fun handleRefresh() {
        analytics.trackGroupOperation(operation = "refresh_field_officer_dashboard")
        Logger.i(TAG) { "pull-to-refresh triggered" }
        updateState { copy(isRefreshing = true) }
        dashboardStream.refreshFresh()
    }

    // -- Error-state retry (ui.yaml effect: call_api) ----------------------------------------------

    private fun handleRetry() {
        Logger.i(TAG) { "retry tapped — re-dispatching field-officer dashboard fetch" }
        updateState { copy(error = null, isLoading = true) }
        dashboardStream.retry()
    }

    // -- Stream -> State mapping -------------------------------------------------------------------

    private fun handleStreamUpdated(screenState: ScreenState<FieldOfficerDashboard>) {
        when (screenState) {
            is ScreenState.Loading -> updateState {
                copy(isLoading = true, error = null)
            }

            is ScreenState.Empty -> updateState {
                copy(
                    isLoading = false,
                    groups = emptyList(),
                    filteredGroups = emptyList(),
                    totalGroupsCount = 0,
                    error = null,
                    isRefreshing = false,
                )
            }

            is ScreenState.Content -> updateState {
                val data = screenState.data
                copy(
                    isLoading = false,
                    staffId = data.staffId,
                    userRole = data.userRole,
                    canExport = data.canExport,
                    totalGroupsCount = data.totalGroupsCount,
                    totalActiveMembers = data.totalActiveMembers,
                    totalSavingsThisMonth = data.totalSavingsThisMonth,
                    totalLoansOutstanding = data.totalLoansOutstanding,
                    groups = data.groups,
                    filteredGroups = applyFilters(data.groups, selectedRegionFilter, selectedStatusFilter, selectedOverdueFilter),
                    availableRegions = data.availableRegions,
                    error = null,
                    isRefreshing = false,
                )
            }

            is ScreenState.NoNetwork -> updateState {
                copy(isLoading = false, error = FieldOfficerDashboardError.Network, isRefreshing = false)
            }

            is ScreenState.Unauthenticated -> {
                crashReporter.recordMessage(
                    message = "field-officer-dashboard: session expired (401)",
                    level = CrashSeverity.Warning,
                )
                sessionManager.endSession()
                updateState { copy(isLoading = false, error = FieldOfficerDashboardError.Auth, isRefreshing = false) }
                sendEvent(FieldOfficerDashboardEvent.ShowSnackbar(message = FieldOfficerDashboardError.Auth.messageKey))
            }

            is ScreenState.Error -> {
                crashReporter.recordException(
                    throwable = screenState.error,
                    message = "field-officer-dashboard: stream error isNetworkError=${screenState.isNetworkError}",
                )
                val mapped = if (screenState.isNetworkError) {
                    FieldOfficerDashboardError.Network
                } else {
                    FieldOfficerDashboardError.Server
                }
                updateState { copy(isLoading = false, error = mapped, isRefreshing = false) }
            }
        }
    }

    /**
     * Client-side re-derivation of the visible group list from the full [groups] plus the active
     * filter set — `ui.yaml#components.filter_row` (region / status / overdue), all `transform_state`
     * with no network call. Region matches [GroupHealthSummary.officeName] exactly; status matches
     * [GroupHealthSummary.status] case-insensitively against the enum name; overdue applies the
     * [OverdueRateFilter] threshold to [GroupHealthSummary.overdueRate].
     */
    private fun applyFilters(
        source: List<GroupHealthSummary>,
        region: String?,
        status: GroupStatusFilter?,
        overdue: OverdueRateFilter?,
    ): List<GroupHealthSummary> = source.filter { group ->
        (region == null || group.officeName == region) &&
            (status == null || group.status.equals(status.name, ignoreCase = true)) &&
            (overdue == null || overdue.matches(group.overdueRate))
    }
}
