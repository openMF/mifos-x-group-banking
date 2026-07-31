/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.grouptypepicker

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
import org.mifos.groupbanking.core.data.repository.GroupTypeConfigRepository
import org.mifos.groupbanking.core.model.GroupTypeConfig
import org.mifos.groupbanking.core.model.GroupTypeSlug

private const val TAG = "GroupTypePickerViewModel"

/**
 * Screen-level render state for `group-type-picker-screen` — verbatim mirror of
 * ui.yaml#state_model.GroupTypePickerViewModel.screen_state. Derived (not stored) from
 * [GroupTypePickerState.isLoading] / [GroupTypePickerState.error] via the
 * [GroupTypePickerState.screenState] extension below, so there is exactly one source of truth
 * for loading/error and no risk of the two drifting apart. See API.md#state.
 */
@Serializable
sealed interface GroupTypePickerScreenState {
    @Serializable
    data object Loading : GroupTypePickerScreenState

    @Serializable
    data object Content : GroupTypePickerScreenState

    @Serializable
    data object Error : GroupTypePickerScreenState
}

/**
 * Error taxonomy for the group-type catalogue read — verbatim mirror of
 * ui.yaml#state_model.GroupTypePickerViewModel.errors.types. [messageKey] is a
 * composeResources string-resource id (never a raw hardcoded English string, per
 * RULE-IMPL-NO-HARDCODED-STRING-001) resolved by the Screen layer.
 *
 * **Idea-layer gap (flagged, not invented here):** ui.yaml declares `redirect: login` on
 * [Auth], but `state_model.events.members` declares no corresponding navigation event
 * ([GroupTypePickerEvent] has only `NavigateToGroupCreate` / `NavigateBack`). Per
 * RULE-IMPL-DEAD-CLICKABLE-001 Rule 1 this ViewModel does NOT invent an unlisted event —
 * [Auth] is surfaced as ordinary non-retryable error state; the actual redirect-to-login
 * is left to whichever cross-cutting session observer the app wires (e.g. a
 * `AuthProvider.authState`/`AuthState.Unauthenticated` collector at the nav-host level,
 * mirroring `AuthProvider.kt`'s own doc contract) — reported to the caller for an
 * idea-layer `ui.yaml#events` update if a screen-local redirect is actually wanted.
 * See API.md#state.
 */
@Serializable
sealed interface GroupTypePickerError {
    val retry: Boolean
    val messageKey: String

    @Serializable
    data object Network : GroupTypePickerError {
        override val retry: Boolean = true
        override val messageKey: String = "error_network"
    }

    @Serializable
    data object Server : GroupTypePickerError {
        override val retry: Boolean = true
        override val messageKey: String = "error_server"
    }

    @Serializable
    data object Auth : GroupTypePickerError {
        override val retry: Boolean = false
        override val messageKey: String = "error_auth"
    }
}

/**
 * MVI state for `GroupTypePickerViewModel`. Field set + defaults are a verbatim mirror of
 * ui.yaml#state_model.GroupTypePickerViewModel.state. [typeConfigs], [selectedType], and
 * [error] are `@Transient` — the catalogue is always re-derived from
 * [GroupTypeConfigRepository.groupTypeConfigsStream] on (re)subscription (offline-first cache,
 * so nothing is lost across process death), and screen-level error is a UI-only concern that
 * should not survive process death, mirroring `LoginSignupState`'s convention (see
 * `training-layer/TRAINING_MASTER.yaml#patterns.state_models`).
 * See API.md#state.
 */
@Serializable
@Immutable
data class GroupTypePickerState(
    @Transient
    val typeConfigs: List<GroupTypeConfig> = emptyList(),
    @Transient
    val selectedType: GroupTypeConfig? = null,
    val isLoading: Boolean = true,
    @Transient
    val error: GroupTypePickerError? = null,
)

/** Derived, single-source-of-truth screen state — see [GroupTypePickerScreenState] KDoc. */
val GroupTypePickerState.screenState: GroupTypePickerScreenState
    get() = when {
        error != null -> GroupTypePickerScreenState.Error
        isLoading -> GroupTypePickerScreenState.Loading
        else -> GroupTypePickerScreenState.Content
    }

/**
 * One-shot side effects emitted by `GroupTypePickerViewModel` — verbatim mirror of
 * ui.yaml#state_model.GroupTypePickerViewModel.events. See API.md#events.
 */
sealed interface GroupTypePickerEvent {
    data class NavigateToGroupCreate(val typeConfig: GroupTypeConfig) : GroupTypePickerEvent
    data object NavigateBack : GroupTypePickerEvent
}

/**
 * User intents dispatched to `GroupTypePickerViewModel`. The 3 top-level members are a
 * verbatim mirror of ui.yaml#state_model.GroupTypePickerViewModel.actions —
 * RULE-IMPL-DEAD-CLICKABLE-001 Rule 1. [Internal] is the sanctioned async-result-routing
 * sub-interface (never a user intent) per
 * `training-layer/TRAINING_MASTER.yaml#patterns.actions` — mirrors `LoginSignupAction.Internal`.
 * See API.md#actions.
 */
sealed interface GroupTypePickerAction {
    data class OnTypeCardTap(val typeSlug: String) : GroupTypePickerAction
    data object OnBack : GroupTypePickerAction
    data object OnRetry : GroupTypePickerAction

    /** Async stream emissions — routed via `trySendAction`, never dispatched by the UI. */
    sealed interface Internal : GroupTypePickerAction {
        data class StreamUpdated(val screenState: ScreenState<List<GroupTypeConfig>>) : Internal
    }
}

/**
 * MVI processor for the group-type selection screen (`business_logic.kind: crud` per
 * ui.yaml — a pure read-side catalogue, so [GroupTypeConfigRepository.groupTypeConfigsStream]'s
 * offline-first `ScreenDataStream` is consumed directly rather than the SP-04 AC-7
 * analytics/crashReporter/fieldEncryptor injection triple, per RULE-IDEA-IMPL-INTELLIGENCE-001
 * AC-03i — that hook set is reserved for non-crud/non-nav_only Store5 write paths).
 *
 * [crashReporter] and [analytics] are still wired (feature-level observability, SC5): a Debug
 * breadcrumb on mount, a warning/exception breadcrumb on stream failure, and a
 * `trackGroupOperation("select_type", ...)` call on every successful card tap — all through the
 * actual shipped `CrashReporter`/`KptAnalyticsTracker` surfaces (no invented `setCustomKey`/
 * `track()` methods; see class-level KDoc on `LoginSignupViewModel` for the same documented
 * drift from the generic template surface).
 *
 * See API.md#viewmodel.
 */
internal class GroupTypePickerViewModel(
    private val repository: GroupTypeConfigRepository,
    private val crashReporter: CrashReporter,
    private val analytics: KptAnalyticsTracker,
) : BaseViewModel<GroupTypePickerState, GroupTypePickerEvent, GroupTypePickerAction>(
    initialState = GroupTypePickerState(),
) {

    private val screenDataStream = repository.groupTypeConfigsStream(scope = viewModelScope)

    init {
        crashReporter.recordMessage(
            message = "feature=group-type-picker screen=group-type-picker-screen",
            level = CrashSeverity.Debug,
        )
        viewModelScope.launch {
            screenDataStream.state.collect { screenState ->
                trySendAction(GroupTypePickerAction.Internal.StreamUpdated(screenState))
            }
        }
    }

    override fun handleAction(action: GroupTypePickerAction) {
        when (action) {
            is GroupTypePickerAction.OnTypeCardTap -> handleTypeCardTap(action.typeSlug)
            GroupTypePickerAction.OnBack -> handleBack()
            GroupTypePickerAction.OnRetry -> handleRetry()
            is GroupTypePickerAction.Internal.StreamUpdated -> handleStreamUpdated(action.screenState)
        }
    }

    // -- Card tap (ui.yaml effect: navigate, reads_from: typeConfigs) --------------------------

    private fun handleTypeCardTap(typeSlug: String) {
        val slug = runCatching { GroupTypeSlug.valueOf(typeSlug) }.getOrDefault(GroupTypeSlug.UNKNOWN)
        val config = state.typeConfigs.firstOrNull { it.typeSlug == slug }
        if (config == null) {
            // Defensive: a card can only be visible (per ui.yaml `visible:
            // typeConfigs.isNotEmpty()`) once typeConfigs is populated, so this path is a
            // stale-tap / race guard, not the happy path. No navigation, no crash — logged
            // for engineering visibility per RULE-IMPL-DEAD-CLICKABLE-001 Rule 2 (no silent
            // empty branch).
            Logger.w(TAG) { "OnTypeCardTap unresolved slug=$typeSlug typeConfigsSize=${state.typeConfigs.size}" }
            crashReporter.recordMessage(
                message = "group-type-picker: OnTypeCardTap unresolved slug=$typeSlug",
                level = CrashSeverity.Warning,
            )
            return
        }
        analytics.trackGroupOperation(operation = "select_type", groupType = typeSlug)
        Logger.i(TAG) { "type card tapped slug=$typeSlug" }
        updateState { copy(selectedType = config) }
        sendEvent(GroupTypePickerEvent.NavigateToGroupCreate(config))
    }

    // -- Back (ui.yaml effect: navigate) --------------------------------------------------------

    private fun handleBack() {
        Logger.i(TAG) { "back tapped — discarding any in-progress wizard state" }
        sendEvent(GroupTypePickerEvent.NavigateBack)
    }

    // -- Retry (ui.yaml effect: call_api, writes_to: group_type_config_cache) -------------------

    private fun handleRetry() {
        Logger.i(TAG) { "retry tapped — re-dispatching COMP-DT-003 fetch" }
        screenDataStream.retry()
    }

    // -- Stream → State mapping ------------------------------------------------------------------

    private fun handleStreamUpdated(screenState: ScreenState<List<GroupTypeConfig>>) {
        when (screenState) {
            is ScreenState.Loading -> updateState { copy(isLoading = true, error = null) }

            is ScreenState.Empty -> updateState {
                copy(isLoading = false, typeConfigs = emptyList(), error = null)
            }

            is ScreenState.Content -> updateState {
                copy(isLoading = false, typeConfigs = screenState.data, error = null)
            }

            is ScreenState.NoNetwork -> updateState {
                copy(isLoading = false, error = GroupTypePickerError.Network)
            }

            is ScreenState.Unauthenticated -> {
                // See the Auth-error KDoc gap note on GroupTypePickerError — no declared nav
                // event exists for the `redirect: login` metadata; surfaced as non-retryable
                // Auth error state, redirect left to the app-level session observer.
                crashReporter.recordMessage(
                    message = "group-type-picker: session expired (401)",
                    level = CrashSeverity.Warning,
                )
                updateState { copy(isLoading = false, error = GroupTypePickerError.Auth) }
            }

            is ScreenState.Error -> {
                crashReporter.recordException(
                    throwable = screenState.error,
                    message = "group-type-picker: stream error isNetworkError=${screenState.isNetworkError}",
                )
                val mapped = if (screenState.isNetworkError) {
                    GroupTypePickerError.Network
                } else {
                    GroupTypePickerError.Server
                }
                updateState { copy(isLoading = false, error = mapped) }
            }
        }
    }
}
