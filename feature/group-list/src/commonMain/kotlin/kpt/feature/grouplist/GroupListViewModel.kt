/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.grouplist

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kpt.core.analytics.KptAnalyticsTracker
import kpt.core.base.observability.CrashReporter
import kpt.core.base.observability.CrashSeverity
import kpt.core.base.store.screen.ScreenState
import kpt.core.base.ui.viewmodel.BaseViewModel
import kpt.core.data.repository.GroupRepository
import kpt.core.model.Group

private const val TAG = "GroupListViewModel"

/**
 * Screen-level render state for `group-list-screen` — verbatim mirror of
 * ui.yaml#state_model.GroupListViewModel.screen_state. Derived (not stored) from
 * [GroupListState.isLoading] / [GroupListState.error] / [GroupListState.groups] via the
 * [GroupListState.screenState] extension below, so there is exactly one source of truth for
 * loading/error/empty and no risk of the three drifting apart.
 *
 * Note: the derivation deliberately reads [GroupListState.groups] (the full unfiltered list),
 * never [GroupListState.filteredGroups] — per `data-flow.yaml`'s `OnSearch` note, an empty
 * *search result* renders as an empty `LazyColumn` inside the [Content] state, NOT the
 * [Empty] illustration-with-CTAs state (that's reserved for a genuinely zero-group account).
 * See API.md#state.
 */
@Serializable
sealed interface GroupListScreenState {
    @Serializable
    data object Loading : GroupListScreenState

    @Serializable
    data object Content : GroupListScreenState

    @Serializable
    data object Error : GroupListScreenState

    @Serializable
    data object Empty : GroupListScreenState
}

/**
 * Error taxonomy for the group-list read — verbatim mirror of
 * ui.yaml#state_model.GroupListViewModel.errors.types. [messageKey] is a composeResources
 * string-resource id (never a raw hardcoded English string, per
 * RULE-IMPL-NO-HARDCODED-STRING-001) resolved by the Screen layer.
 *
 * **Idea-layer gap (flagged, not invented here):** ui.yaml declares `redirect: login` on
 * [Auth] (mirrored by `data-flow.yaml#error_paths` — `401 → behavior: navigate, nav_target:
 * login-signup`), but `state_model.events.members` declares no `NavigateToLogin` /
 * `NavigateToLoginSignup` event — [GroupListEvent] has only `NavigateToGroupDashboard` /
 * `NavigateToCreateGroup` / `NavigateToJoinGroup` / `ShowSnackbar`. Per
 * RULE-IMPL-DEAD-CLICKABLE-001 Rule 1 this ViewModel does NOT invent an unlisted event —
 * [Auth] is surfaced as an ordinary non-retryable error state (identical treatment to
 * `GroupTypePickerViewModel`'s Auth gap), and the closest declared event ([GroupListEvent
 * .ShowSnackbar]) is ALSO emitted carrying the `error_auth` message key so the user gets
 * immediate feedback even though no screen-local redirect exists. Reported to the caller for
 * an idea-layer `ui.yaml#events` update (add a `NavigateToLogin` member) rather than invented
 * silently. See API.md#state.
 */
@Serializable
sealed interface GroupListError {
    val retry: Boolean
    val messageKey: String

    @Serializable
    data object Network : GroupListError {
        override val retry: Boolean = true
        override val messageKey: String = "error_network"
    }

    @Serializable
    data object Server : GroupListError {
        override val retry: Boolean = true
        override val messageKey: String = "error_server"
    }

    @Serializable
    data object Auth : GroupListError {
        override val retry: Boolean = false
        override val messageKey: String = "error_auth"
    }
}

/**
 * MVI state for `GroupListViewModel`. Field set + defaults are a verbatim mirror of
 * ui.yaml#state_model.GroupListViewModel.state. [groups], [filteredGroups], and [error] are
 * `@Transient` — the paged list is always re-derived from
 * [GroupRepository.groupsPagingStream] on (re)subscription (offline-first cache via
 * `group_list_cache`, so nothing is visually lost across process death — the store, not this
 * transient render state, is the durable source), and [Group] itself is not `@Serializable`.
 * Mirrors `GroupTypePickerState`'s identical `@Transient` convention for non-serializable
 * domain payloads (see `training-layer/TRAINING_MASTER.yaml#patterns.state_models`).
 * See API.md#state.
 */
@Serializable
@Immutable
data class GroupListState(
    val isLoading: Boolean = true,
    @Transient
    val groups: List<Group> = emptyList(),
    val searchQuery: String = "",
    @Transient
    val filteredGroups: List<Group> = emptyList(),
    @Transient
    val error: GroupListError? = null,
    val isRefreshing: Boolean = false,
)

/** Derived, single-source-of-truth screen state — see [GroupListScreenState] KDoc. */
val GroupListState.screenState: GroupListScreenState
    get() = when {
        error != null -> GroupListScreenState.Error
        isLoading -> GroupListScreenState.Loading
        groups.isEmpty() -> GroupListScreenState.Empty
        else -> GroupListScreenState.Content
    }

/**
 * One-shot side effects emitted by `GroupListViewModel` — verbatim mirror of
 * ui.yaml#state_model.GroupListViewModel.events. See API.md#events.
 */
sealed interface GroupListEvent {
    data class NavigateToGroupDashboard(val groupId: String, val viewerRole: String) : GroupListEvent
    data object NavigateToCreateGroup : GroupListEvent
    data object NavigateToJoinGroup : GroupListEvent

    /**
     * Notification bell tap → deferred in-app notifications centre (G15,
     * `ui.yaml#components.top_bar.actions[notifications].on_click`). The notifications screen ships
     * in a later release (`release_plan.deferred[]`), so this event carries NO navigation — the
     * Screen shows a snackbar (`notifications_deferred` key). Distinct from [ShowSnackbar], which
     * carries a dynamic error message-key; this is the fixed deferred-notifications affordance.
     */
    data object NotificationsDeferred : GroupListEvent
    data class ShowSnackbar(val message: String) : GroupListEvent
}

/**
 * User intents dispatched to `GroupListViewModel`. The 7 top-level members are a verbatim
 * mirror of ui.yaml#state_model.GroupListViewModel.actions — RULE-IMPL-DEAD-CLICKABLE-001
 * Rule 1. [OnLoadMoreTap] is an 8th member sourced from `data-flow.yaml#entries[on_interact /
 * action: OnLoadMoreTap]` (pagination load-next-page on scroll-to-end) — there is no discrete
 * "load more" button in ui.yaml's `components[]`; `PagingScreenContent`'s built-in footer
 * dispatches this action when the user scrolls to the end of the list, so it is documented
 * here from `data-flow.yaml` rather than invented. [Internal] is the sanctioned
 * async-result-routing sub-interface (never a user intent) per
 * `training-layer/TRAINING_MASTER.yaml#patterns.actions` — mirrors
 * `GroupTypePickerAction.Internal`.
 *
 * **G15 (resolved idea-layer gap):** ui.yaml's `top_bar` now declares `OnOpenNotifications` in
 * `state_model.actions.members` (and a matching `NotificationsDeferred` event) — the previously
 * dead top-bar bell is now wired to a real deferred-notifications snackbar affordance. [OnOpenNotifications]
 * mirrors that declaration verbatim (RULE-IMPL-DEAD-CLICKABLE-001 SP-07 Rule 2).
 * See API.md#actions.
 */
sealed interface GroupListAction {
    data class OnGroupClick(val groupId: String, val viewerRole: String) : GroupListAction
    data class OnSearch(val query: String) : GroupListAction
    data object OnClearSearch : GroupListAction
    data object OnCreateGroup : GroupListAction
    data object OnJoinGroup : GroupListAction

    /** Top-bar notification bell tap (`ui.yaml#components.top_bar.actions[notifications]`, G15). */
    data object OnOpenNotifications : GroupListAction
    data object OnRefresh : GroupListAction
    data object Retry : GroupListAction
    data object OnLoadMoreTap : GroupListAction

    /** Async stream emissions — routed via `trySendAction`, never dispatched by the UI. */
    sealed interface Internal : GroupListAction {
        data class StreamUpdated(val screenState: ScreenState<List<Group>>) : Internal
    }
}

/**
 * MVI processor for the group-list screen (`business_logic.kind: crud` per ui.yaml — a pure
 * paginated read-side list, so [GroupRepository.groupsPagingStream]'s offline-first
 * [kpt.core.base.store.paging.PagingScreenStream] is consumed directly rather than the SP-04
 * AC-7 analytics/crashReporter/fieldEncryptor injection triple, per
 * RULE-IDEA-IMPL-INTELLIGENCE-001 AC-03i — that hook set is reserved for non-crud/non-nav_only
 * Store5 write paths. There is no PII on [Group], so no `FieldEncryptor` is injected.
 *
 * [crashReporter] and [analytics] are still wired (feature-level observability, SC5): a Debug
 * breadcrumb on mount, a warning/exception breadcrumb on stream failure, and a
 * `trackGroupOperation(...)` call on every user-initiated group operation — all through the
 * actual shipped `CrashReporter`/`KptAnalyticsTracker` surfaces (no invented `setCustomKey`/
 * `track()` methods; mirrors the documented drift on `GroupTypePickerViewModel`).
 *
 * See API.md#viewmodel.
 */
internal class GroupListViewModel(
    private val repository: GroupRepository,
    private val crashReporter: CrashReporter,
    private val analytics: KptAnalyticsTracker,
) : BaseViewModel<GroupListState, GroupListEvent, GroupListAction>(
    initialState = GroupListState(),
) {

    /** Offline-first PAGED stream — see `GroupRepository.groupsPagingStream` KDoc. */
    private val pagingStream = repository.groupsPagingStream(scope = viewModelScope)

    init {
        crashReporter.recordMessage(
            message = "feature=group-list screen=group-list-screen",
            level = CrashSeverity.Debug,
        )
        analytics.trackGroupOperation(operation = "view_list")
        viewModelScope.launch {
            pagingStream.state.collect { screenState ->
                trySendAction(GroupListAction.Internal.StreamUpdated(screenState))
            }
        }
    }

    override fun handleAction(action: GroupListAction) {
        when (action) {
            is GroupListAction.OnGroupClick -> handleGroupClick(action.groupId, action.viewerRole)
            is GroupListAction.OnSearch -> handleSearch(action.query)
            GroupListAction.OnClearSearch -> handleClearSearch()
            GroupListAction.OnCreateGroup -> handleCreateGroup()
            GroupListAction.OnJoinGroup -> handleJoinGroup()
            GroupListAction.OnOpenNotifications -> handleOpenNotifications()
            GroupListAction.OnRefresh -> handleRefresh()
            GroupListAction.Retry -> handleRetry()
            GroupListAction.OnLoadMoreTap -> handleLoadMoreTap()
            is GroupListAction.Internal.StreamUpdated -> handleStreamUpdated(action.screenState)
        }
    }

    // -- Group card tap (ui.yaml effect: navigate, target: group-dashboard) ----------------------

    private fun handleGroupClick(groupId: String, viewerRole: String) {
        analytics.trackGroupOperation(operation = "view", groupId = groupId)
        Logger.i(TAG) { "group card tapped groupId=$groupId viewerRole=$viewerRole" }
        sendEvent(GroupListEvent.NavigateToGroupDashboard(groupId, viewerRole))
    }

    // -- Search bar on_change (ui.yaml effect: transform_state, client-side name filter) ---------

    private fun handleSearch(query: String) {
        Logger.d(TAG) { "search query changed length=${query.length}" }
        updateState { copy(searchQuery = query, filteredGroups = groups.filterByQuery(query)) }
    }

    // -- Search bar clear icon (ui.yaml effect: transform_state) ----------------------------------

    private fun handleClearSearch() {
        Logger.d(TAG) { "search cleared" }
        updateState { copy(searchQuery = "", filteredGroups = groups) }
    }

    // -- Create Group FAB + empty-state CTA (ui.yaml effect: navigate, target: group-type-picker) -
    // NOTE: the declared event name is `NavigateToCreateGroup` (verbatim from
    // ui.yaml#state_model.events.members) even though the FAB's actual `on_click.target` is
    // `group-type-picker`, not a direct group-create form — the nav-host resolves this event to
    // the group-type-picker route; that resolution is nav-graph wiring, out of ViewModel scope.

    private fun handleCreateGroup() {
        analytics.trackGroupOperation(operation = "create")
        Logger.i(TAG) { "create group tapped" }
        sendEvent(GroupListEvent.NavigateToCreateGroup)
    }

    // -- Join with Code CTA (ui.yaml effect: navigate, target: join-with-code) --------------------

    private fun handleJoinGroup() {
        analytics.trackGroupOperation(operation = "join")
        Logger.i(TAG) { "join group tapped" }
        sendEvent(GroupListEvent.NavigateToJoinGroup)
    }

    // -- Notification bell tap (ui.yaml effect: emit_event — deferred notifications centre, G15) ---

    private fun handleOpenNotifications() {
        Logger.i(TAG) { "notification bell tapped — in-app notifications centre deferred to a later release" }
        sendEvent(GroupListEvent.NotificationsDeferred)
    }

    // -- Pull to refresh (data-flow.yaml on_refresh: bypass_and_refresh via GroupRepository) ------

    private fun handleRefresh() {
        analytics.trackGroupOperation(operation = "refresh")
        Logger.i(TAG) { "pull-to-refresh triggered" }
        updateState { copy(isRefreshing = true) }
        pagingStream.refresh()
    }

    // -- Error-state retry (ui.yaml effect: call_api, external_library_refs: [GroupRepository]) ---

    private fun handleRetry() {
        Logger.i(TAG) { "retry tapped — re-dispatching companion groups fetch via GroupRepository" }
        updateState { copy(error = null, isLoading = true) }
        pagingStream.retry()
    }

    // -- Scroll-to-end load-more (data-flow.yaml on_interact/OnLoadMoreTap via GroupRepository) ---

    private fun handleLoadMoreTap() {
        Logger.i(TAG) { "load-more triggered currentSize=${state.groups.size}" }
        pagingStream.loadNextPage()
    }

    // -- Stream → State mapping --------------------------------------------------------------------

    private fun handleStreamUpdated(screenState: ScreenState<List<Group>>) {
        when (screenState) {
            is ScreenState.Loading -> updateState {
                copy(isLoading = true, error = null, isRefreshing = false)
            }

            is ScreenState.Empty -> updateState {
                copy(
                    isLoading = false,
                    groups = emptyList(),
                    filteredGroups = emptyList(),
                    error = null,
                    isRefreshing = false,
                )
            }

            is ScreenState.Content -> updateState {
                val newGroups = screenState.data
                copy(
                    isLoading = false,
                    groups = newGroups,
                    filteredGroups = newGroups.filterByQuery(searchQuery),
                    error = null,
                    isRefreshing = false,
                )
            }

            is ScreenState.NoNetwork -> updateState {
                copy(isLoading = false, error = GroupListError.Network, isRefreshing = false)
            }

            is ScreenState.Unauthenticated -> {
                // See the Auth-error KDoc gap note on GroupListError — no declared NavigateToLogin
                // event exists; surfaced as non-retryable Auth error state PLUS the closest
                // declared event (ShowSnackbar) so the user still gets immediate feedback.
                crashReporter.recordMessage(
                    message = "group-list: session expired (401)",
                    level = CrashSeverity.Warning,
                )
                updateState { copy(isLoading = false, error = GroupListError.Auth, isRefreshing = false) }
                sendEvent(GroupListEvent.ShowSnackbar(message = GroupListError.Auth.messageKey))
            }

            is ScreenState.Error -> {
                crashReporter.recordException(
                    throwable = screenState.error,
                    message = "group-list: stream error isNetworkError=${screenState.isNetworkError}",
                )
                val mapped = if (screenState.isNetworkError) {
                    GroupListError.Network
                } else {
                    GroupListError.Server
                }
                updateState { copy(isLoading = false, error = mapped, isRefreshing = false) }
            }
        }
    }

    /** Client-side, case-insensitive name filter — ui.yaml `search_bar` `effect: transform_state`. */
    private fun List<Group>.filterByQuery(query: String): List<Group> =
        if (query.isBlank()) this else filter { it.name.contains(query, ignoreCase = true) }
}
