/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.personaldashboard

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kpt.core.analytics.KptAnalyticsTracker
import kpt.core.base.observability.CrashReporter
import kpt.core.base.observability.CrashSeverity
import kpt.core.base.security.SessionManager
import kpt.core.base.store.screen.ScreenDataStream
import kpt.core.base.store.screen.ScreenState
import kpt.core.base.ui.viewmodel.BaseViewModel
import org.mifos.groupbanking.core.data.repository.MemberDashboardRepository
import org.mifos.groupbanking.core.model.GroupSummary
import org.mifos.groupbanking.core.model.MemberDashboard
import org.mifos.groupbanking.core.model.SavingsMechanism
import org.mifos.groupbanking.core.model.SavingsTransaction

private const val TAG = "PersonalDashboardViewModel"

/**
 * Screen-level render state for `personal-dashboard-screen` — verbatim mirror of
 * ui.yaml#state_model.PersonalDashboardViewModel.screen_state. Derived (not stored) from
 * [PersonalDashboardState.isLoading] / [PersonalDashboardState.error] /
 * [PersonalDashboardState.myGroups] via the [PersonalDashboardState.screenState] extension
 * below — exactly one source of truth, mirroring `GroupListState`'s identical convention.
 *
 * Note: [MemberDashboardRepository.memberDashboardStream]'s backing Store always predicates
 * `isEmpty = { false }` (a single `MemberDashboard` snapshot is never "empty" once present —
 * see `MemberDashboardRepositoryImpl` KDoc), so `ScreenState.Empty` is never actually emitted by
 * the stream. The real "new member — zero groups joined" [Empty] screen state is derived from
 * [ScreenState.Content] when `myGroups.isEmpty()`, per `data-flow.yaml`'s note "Empty myGroups[]
 * → ZeroGroups state (same as login-signup ZeroGroups)." See API.md#state.
 */
@Serializable
sealed interface PersonalDashboardScreenState {
    @Serializable
    data object Loading : PersonalDashboardScreenState

    @Serializable
    data object Content : PersonalDashboardScreenState

    @Serializable
    data object Error : PersonalDashboardScreenState

    @Serializable
    data object Empty : PersonalDashboardScreenState
}

/**
 * Error taxonomy for the member-dashboard read — verbatim mirror of
 * ui.yaml#state_model.PersonalDashboardViewModel.errors.types. [messageKey] is a
 * composeResources string-resource id (never a raw hardcoded English string, per
 * RULE-IMPL-NO-HARDCODED-STRING-001) resolved by the Screen layer.
 *
 * Unlike `GroupListError`/`GroupTypePickerError`'s `Auth` gap (no declared nav event, no
 * SessionManager DI), ui.yaml explicitly lists `SessionManager` in
 * `state_model.PersonalDashboardViewModel.di` and `flow.yaml#on_error` declares
 * `condition: "error == Unauthorized" → action: clear_session` (no navigate — the app shell
 * handles re-auth after token expiry). [Unauthorized] is therefore wired to a REAL
 * `sessionManager.endSession()` call in [PersonalDashboardViewModel.handleStreamUpdated] — not
 * just a non-retryable error state. See API.md#state.
 */
@Serializable
sealed interface DashboardError {
    val retry: Boolean
    val messageKey: String

    @Serializable
    data object Network : DashboardError {
        override val retry: Boolean = true
        override val messageKey: String = "error_network"
    }

    @Serializable
    data object Server : DashboardError {
        override val retry: Boolean = true
        override val messageKey: String = "error_server"
    }

    @Serializable
    data object Unauthorized : DashboardError {
        override val retry: Boolean = false
        override val messageKey: String = "error_session_expired"
    }
}

/**
 * MVI state for `PersonalDashboardViewModel`. Field set + defaults are a verbatim mirror of
 * ui.yaml#state_model.PersonalDashboardViewModel.state.
 *
 * [poolModel] is intentionally kept as the raw [SavingsMechanism.name] `String` (matching
 * ui.yaml's declared `type: String`, not the domain enum) — the ui.yaml template layer
 * interpolates it directly (`{{poolModel == 'ROTATING_PAYOUT' ? ... }}`), so the enum is
 * projected to its wire name once here rather than re-parsed per-composable.
 *
 * [myGroups], [selectedGroup], [recentTransactions], and [error] are `@Transient` — all four are
 * always re-derived from [MemberDashboardRepository.memberDashboardStream] on (re)subscription
 * (offline-first cache via `member_dashboard_cache`, so nothing is visually lost across process
 * death — the Store, not this transient render state, is the durable source), and none of
 * [GroupSummary] / [SavingsTransaction] / [DashboardError] itself needs to survive process death.
 * Mirrors `GroupListState`'s identical `@Transient` convention for non-serializable domain
 * payloads (see `training-layer/TRAINING_MASTER.yaml#patterns.state_models`).
 * See API.md#state.
 */
@Serializable
@Immutable
data class PersonalDashboardState(
    val memberName: String = "",
    @Transient
    val myGroups: List<GroupSummary> = emptyList(),
    @Transient
    val selectedGroup: GroupSummary? = null,
    val poolModel: String = "",
    val groupLinkedSavingsBalance: Double = 0.0,
    val individualSavingsBalance: Double = 0.0,
    val shareOutProjection: Double = 0.0,
    val rotationPosition: Int? = null,
    val nextRecipientEta: String? = null,
    @Transient
    val recentTransactions: List<SavingsTransaction> = emptyList(),
    val isRefreshing: Boolean = false,
    val isLoading: Boolean = true,
    @Transient
    val error: DashboardError? = null,
)

/** Derived, single-source-of-truth screen state — see [PersonalDashboardScreenState] KDoc. */
val PersonalDashboardState.screenState: PersonalDashboardScreenState
    get() = when {
        error != null -> PersonalDashboardScreenState.Error
        isLoading -> PersonalDashboardScreenState.Loading
        myGroups.isEmpty() -> PersonalDashboardScreenState.Empty
        else -> PersonalDashboardScreenState.Content
    }

/**
 * One-shot side effects emitted by `PersonalDashboardViewModel` — verbatim mirror of
 * ui.yaml#state_model.PersonalDashboardViewModel.events.
 *
 * [NavigateToSavings] carries `groupId` + `poolModel` (the `SavingsMechanism.name` string) —
 * `flow.yaml#on_savings_card_click` describes passing `selectedGroup.groupId` and `typeConfig`
 * to the personal-savings screen so it can render the correct pool-model view, but
 * `MemberDashboard`/`GroupSummary` carry no full `typeConfig` object (only the `poolModel`
 * discriminator) — `poolModel` is the closest available substitute and is sufficient for the
 * described purpose (choosing the ACCUMULATING vs ROTATING_PAYOUT layout on the target screen).
 *
 * **Idea-layer gap (flagged, not invented here):** [NavigateToGroupList] is declared in
 * ui.yaml#state_model.events.members and `flow.yaml#navigates_to: [group-list]`, but NO
 * `on_click` in ui.yaml#components currently targets it — the group-selector chip row only
 * dispatches `OnSelectGroup` (switches the active group in-place), and no "View All Groups"
 * affordance exists on the canvas. Per RULE-IMPL-DEAD-CLICKABLE-001 Rule 1 this ViewModel does
 * NOT invent an action to reach it; the event type is still declared (verbatim mirror) but is
 * currently unreachable from any wired action. Reported to the caller for an idea-layer
 * `ui.yaml#components` update (e.g. an on-click on `group_banner` / `group_name_text`, or a
 * dedicated "View All Groups" chip, wired to a new `OnViewAllGroups` action) if group-list
 * navigation from this screen is actually wanted. See API.md#events.
 */
sealed interface PersonalDashboardEvent {
    data class NavigateToSavings(val groupId: String, val poolModel: String) : PersonalDashboardEvent
    data object NavigateToGroupList : PersonalDashboardEvent
}

/**
 * User intents dispatched to `PersonalDashboardViewModel`. The 4 top-level members are a
 * verbatim mirror of ui.yaml#state_model.PersonalDashboardViewModel.actions —
 * RULE-IMPL-DEAD-CLICKABLE-001 Rule 1. [Internal] is the sanctioned async-result-routing
 * sub-interface (never a user intent) per
 * `training-layer/TRAINING_MASTER.yaml#patterns.actions` — mirrors `GroupListAction.Internal`.
 * See API.md#actions.
 */
sealed interface PersonalDashboardAction {
    data object OnRefresh : PersonalDashboardAction
    data object OnRetry : PersonalDashboardAction
    data object OnSavingsCardClick : PersonalDashboardAction
    data class OnSelectGroup(val groupId: String) : PersonalDashboardAction

    /** Async stream emissions — routed via `trySendAction`, never dispatched by the UI. */
    sealed interface Internal : PersonalDashboardAction {
        data class StreamUpdated(val screenState: ScreenState<MemberDashboard>) : Internal
    }
}

/**
 * MVI processor for the member home screen (`business_logic.kind: crud` per ui.yaml — a pure
 * dynamic-key read, so [MemberDashboardRepository.memberDashboardStream]'s offline-first
 * `ScreenDataStream` is consumed directly rather than the SP-04 AC-7 analytics/crashReporter/
 * fieldEncryptor injection triple, per RULE-IDEA-IMPL-INTELLIGENCE-001 AC-03i — that hook set is
 * reserved for non-crud/non-nav_only Store5 write paths. There is no local PII persistence on
 * this screen — `memberName` is a display-only field already resolved server-side from the auth
 * token — so no `FieldEncryptor` is injected).
 *
 * [crashReporter] and [analytics] are still wired (feature-level observability, SC5): a Debug
 * breadcrumb on mount, a warning/exception breadcrumb on stream failure, and a real
 * `trackSavingsOperation`/`trackGroupOperation` call on every user-initiated read — all through
 * the actual shipped `CrashReporter`/`KptAnalyticsTracker` surfaces (no invented `setCustomKey`/
 * `track()` methods; mirrors the documented drift on `GroupListViewModel`).
 *
 * [sessionManager] is the one real difference from the `GroupList`/`GroupTypePicker` precedent:
 * ui.yaml explicitly declares it in `state_model.di`, and `flow.yaml#on_error` wires the
 * `Unauthorized` branch to `action: clear_session` — handled for real in
 * [handleStreamUpdated]'s `ScreenState.Unauthenticated` branch below.
 *
 * **Group switching**: `MemberDashboardRepository.memberDashboardStream(selectedGroupId, ...)`
 * returns a NEW [ScreenDataStream] per call (it is not a dynamic-key `Flow<Key>` stream) — each
 * group is Store5-cached independently under its own key. [handleSelectGroup] therefore replaces
 * [dashboardStream] with a freshly-requested stream for the tapped `groupId` and re-subscribes
 * via [observeDashboard], cancelling the previous collection job so exactly one subscription is
 * ever live.
 *
 * See API.md#viewmodel.
 */
internal class PersonalDashboardViewModel(
    private val repository: MemberDashboardRepository,
    private val sessionManager: SessionManager,
    private val crashReporter: CrashReporter,
    private val analytics: KptAnalyticsTracker,
) : BaseViewModel<PersonalDashboardState, PersonalDashboardEvent, PersonalDashboardAction>(
    initialState = PersonalDashboardState(),
) {

    /**
     * Current dashboard stream for the active group selection. `null` selectedGroupId resolves
     * server-side to the member's first group (first mount) — see `MemberDashboardRepository
     * .memberDashboardStream` KDoc.
     */
    private var dashboardStream: ScreenDataStream<MemberDashboard> =
        repository.memberDashboardStream(selectedGroupId = null, scope = viewModelScope)

    /** Cancelled + relaunched on every group switch — see class KDoc "Group switching". */
    private var collectJob: Job? = null

    init {
        crashReporter.recordMessage(
            message = "feature=personal-dashboard screen=personal-dashboard-screen",
            level = CrashSeverity.Debug,
        )
        analytics.trackSavingsOperation(operation = "view")
        observeDashboard()
    }

    override fun handleAction(action: PersonalDashboardAction) {
        when (action) {
            PersonalDashboardAction.OnRefresh -> handleRefresh()
            PersonalDashboardAction.OnRetry -> handleRetry()
            PersonalDashboardAction.OnSavingsCardClick -> handleSavingsCardClick()
            is PersonalDashboardAction.OnSelectGroup -> handleSelectGroup(action.groupId)
            is PersonalDashboardAction.Internal.StreamUpdated -> handleStreamUpdated(action.screenState)
        }
    }

    // -- Group selector chip tap (ui.yaml effect: call_api, re-fetch for tapped groupId) ----------

    private fun handleSelectGroup(groupId: String) {
        analytics.trackGroupOperation(operation = "view", groupId = groupId)
        Logger.i(TAG) { "group chip tapped groupId=$groupId — re-fetching member dashboard" }
        // Keep the previous selectedGroup/myGroups visible under the shimmer (group_banner stays
        // rendered in the `loading` screen state per ui.yaml#states.loading.components) while the
        // new group's stream resolves.
        updateState { copy(isLoading = true, error = null) }
        dashboardStream = repository.memberDashboardStream(selectedGroupId = groupId, scope = viewModelScope)
        observeDashboard()
    }

    // -- Savings summary card tap (ui.yaml effect: navigate, target: personal-savings) -------------

    private fun handleSavingsCardClick() {
        val group = state.selectedGroup
        if (group == null) {
            // Defensive: the card is only rendered once Content has loaded (ui.yaml
            // `states.content.components` includes `savings_summary_card`), so a null
            // selectedGroup here is a stale-tap / race guard, not the happy path. No
            // navigation, no crash — logged for engineering visibility per
            // RULE-IMPL-DEAD-CLICKABLE-001 Rule 2 (no silent empty branch).
            Logger.w(TAG) { "OnSavingsCardClick with no selectedGroup loaded — ignoring" }
            crashReporter.recordMessage(
                message = "personal-dashboard: OnSavingsCardClick before selectedGroup loaded",
                level = CrashSeverity.Warning,
            )
            return
        }
        analytics.trackSavingsOperation(operation = "view", accountId = group.groupId)
        Logger.i(TAG) { "savings card tapped groupId=${group.groupId} poolModel=${group.poolModel}" }
        sendEvent(
            PersonalDashboardEvent.NavigateToSavings(
                groupId = group.groupId,
                poolModel = group.poolModel.name,
            ),
        )
    }

    // -- Pull-to-refresh (data-flow.yaml on_refresh: cache.strategy=bypass_and_refresh) -------------

    private fun handleRefresh() {
        Logger.i(TAG) { "pull-to-refresh triggered" }
        updateState { copy(isRefreshing = true) }
        // bypass_and_refresh — forces a fresh network fetch bypassing the SWR band gate,
        // distinct from the SWR-honoring `retry()` used by the error-state CTA below.
        dashboardStream.refreshFresh()
    }

    // -- Error-state retry (data-flow.yaml on_mount: cache.strategy=stale_while_revalidate) --------

    private fun handleRetry() {
        Logger.i(TAG) { "retry tapped — re-dispatching companion member-dashboard fetch" }
        updateState { copy(error = null, isLoading = true) }
        dashboardStream.retry()
    }

    // -- Stream subscription plumbing -----------------------------------------------------------------

    private fun observeDashboard() {
        collectJob?.cancel()
        collectJob = viewModelScope.launch {
            dashboardStream.state.collect { screenState ->
                trySendAction(PersonalDashboardAction.Internal.StreamUpdated(screenState))
            }
        }
    }

    // -- Stream -> State mapping (pool-model-adaptive) -------------------------------------------------

    private fun handleStreamUpdated(screenState: ScreenState<MemberDashboard>) {
        when (screenState) {
            is ScreenState.Loading -> updateState { copy(isLoading = true, error = null) }

            is ScreenState.Empty -> updateState {
                // Never actually emitted by MemberDashboardRepositoryImpl's `isEmpty = { false }`
                // predicate — kept for ScreenState exhaustiveness. See class-level KDoc on
                // PersonalDashboardScreenState for where the real "0 groups" Empty state comes
                // from (derived from Content when myGroups.isEmpty()).
                copy(isLoading = false, isRefreshing = false, error = null)
            }

            is ScreenState.Content -> updateState {
                val dashboard = screenState.data
                copy(
                    memberName = dashboard.memberName,
                    myGroups = dashboard.myGroups,
                    selectedGroup = dashboard.selectedGroup,
                    poolModel = dashboard.poolModel.name,
                    groupLinkedSavingsBalance = dashboard.groupLinkedSavingsBalance,
                    individualSavingsBalance = dashboard.individualSavingsBalance,
                    // Pool-model-adaptive projection — mutually exclusive per MemberDashboard's
                    // KDoc: ACCUMULATING types show shareOutProjection; ROTATING_PAYOUT types
                    // show rotationPosition + nextRecipientEta; NONE shows neither.
                    shareOutProjection = if (dashboard.poolModel == SavingsMechanism.ACCUMULATING) {
                        dashboard.shareOutProjection ?: 0.0
                    } else {
                        0.0
                    },
                    rotationPosition = if (dashboard.poolModel == SavingsMechanism.ROTATING_PAYOUT) {
                        dashboard.rotationPosition
                    } else {
                        null
                    },
                    nextRecipientEta = if (dashboard.poolModel == SavingsMechanism.ROTATING_PAYOUT) {
                        dashboard.nextRecipientEta
                    } else {
                        null
                    },
                    recentTransactions = dashboard.recentTransactions,
                    isLoading = false,
                    isRefreshing = false,
                    error = null,
                )
            }

            is ScreenState.NoNetwork -> updateState {
                copy(isLoading = false, isRefreshing = false, error = DashboardError.Network)
            }

            is ScreenState.Unauthenticated -> {
                // flow.yaml#on_error: condition "error == Unauthorized" -> action: clear_session
                // (no navigate — the app shell handles re-auth after token expiry once the
                // session is cleared). SessionManager is declared in ui.yaml#state_model.di
                // specifically for this branch — unlike GroupList/GroupTypePicker's Auth gap,
                // this is a REAL clear_session call, not just a non-retryable error state.
                crashReporter.recordMessage(
                    message = "personal-dashboard: session expired (401) — clearing session",
                    level = CrashSeverity.Warning,
                )
                sessionManager.endSession()
                updateState {
                    copy(isLoading = false, isRefreshing = false, error = DashboardError.Unauthorized)
                }
            }

            is ScreenState.Error -> {
                crashReporter.recordException(
                    throwable = screenState.error,
                    message = "personal-dashboard: stream error isNetworkError=${screenState.isNetworkError}",
                )
                val mapped = if (screenState.isNetworkError) {
                    DashboardError.Network
                } else {
                    DashboardError.Server
                }
                updateState { copy(isLoading = false, isRefreshing = false, error = mapped) }
            }
        }
    }
}
