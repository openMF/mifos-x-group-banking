/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.settingslogoutdialog

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kpt.core.analytics.KptAnalyticsTracker
import kpt.core.base.observability.CrashReporter
import kpt.core.base.observability.CrashSeverity
import kpt.core.base.ui.viewmodel.BaseViewModel
import kpt.core.data.repository.AuthRepository
import kpt.core.data.repository.SyncQueueRepository

// MVI stack (State/Event/Action/ViewModel/DI) for the `settings-logout-dialog` feature — see
// API.md#viewmodel / #state / #actions / #events / #di for the full generated-symbol contract.
private const val TAG = "SettingsLogoutDialogViewModel"

/** `data-flow.yaml#entries[1].error_paths` — `local.clear_failed -> error_not_found` per ui.yaml's
 * own declared error key `error_logout_failed`. Named per ui.yaml's `errors.LogoutFailed`
 * message-key contract (`state_model.errors`), not the data-flow message key — see class KDoc
 * "message-key divergence" note.
 */
private const val LOGOUT_FAILED_MESSAGE_KEY = "error_logout_failed"

/**
 * MVI state for `SettingsLogoutDialogViewModel`. [isLoggingOut] / [logoutError] are a verbatim
 * mirror of `ui.yaml#state_model.SettingsLogoutDialogViewModel.state.fields`.
 *
 * [unsyncedCount] is an ADDED field (not declared in `ui.yaml#state_model.state.fields`) — flagged
 * per RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1 divergence convention. `ui.yaml#components`
 * declares `body_text: "...Any unsynced changes will be lost."` as a static string with no
 * interpolation placeholder, but `data-flow.yaml#entries[0]` (`on_mount`) documents a local read
 * that this dialog is expected to seed for that warning, and the caller's brief explicitly invites
 * surfacing a real count sourced from [SyncQueueRepository.observeCounts] rather than a static
 * warning with no backing data. Derived as `pending + failed` (the two non-terminal, "at risk of
 * loss on logout" buckets — `syncing` is mid-flight and `synced` is already durable) — see
 * [SettingsLogoutDialogViewModel]'s `init` block.
 *
 * No `@PII` field here per the SP-02 idea-layer schema — this dialog collects nothing, it only
 * confirms a destructive local action — so `FieldEncryptor` (`core-base/security`) is intentionally
 * NOT injected into [SettingsLogoutDialogViewModel].
 *
 * See API.md#state.
 */
@Serializable
@Immutable
data class SettingsLogoutDialogState(
    val isLoggingOut: Boolean = false,
    val logoutError: String? = null,
    val unsyncedCount: Int = 0,
)

/**
 * Derived render-state for `SettingsLogoutDialogState` — verbatim mirror of
 * `ui.yaml#state_model.SettingsLogoutDialogViewModel.screen_state.members` (`Idle` / `LoggingOut` /
 * `Error`), same convention as `MemberAddState.deriveScreenState()` / `GroupCreateState`. A pure
 * function rather than a stored field so the state class stays the single source of truth. See
 * API.md#state.
 */
sealed interface SettingsLogoutDialogScreenState {
    data object Idle : SettingsLogoutDialogScreenState
    data object LoggingOut : SettingsLogoutDialogScreenState
    data object Error : SettingsLogoutDialogScreenState
}

/** Derives [SettingsLogoutDialogScreenState] from [SettingsLogoutDialogState]. See API.md#state. */
fun SettingsLogoutDialogState.deriveScreenState(): SettingsLogoutDialogScreenState = when {
    logoutError != null -> SettingsLogoutDialogScreenState.Error
    isLoggingOut -> SettingsLogoutDialogScreenState.LoggingOut
    else -> SettingsLogoutDialogScreenState.Idle
}

/**
 * One-shot side effects emitted by `SettingsLogoutDialogViewModel` — verbatim mirror of
 * `ui.yaml#state_model.SettingsLogoutDialogViewModel.events.members`. See API.md#events.
 */
sealed interface SettingsLogoutDialogEvent {
    data object Dismiss : SettingsLogoutDialogEvent
    data object NavigateToLogin : SettingsLogoutDialogEvent
}

/**
 * User intents dispatched to `SettingsLogoutDialogViewModel`. The 2 top-level members
 * ([OnConfirmLogout] / [OnDismiss]) are a verbatim mirror of
 * `ui.yaml#state_model.SettingsLogoutDialogViewModel.actions.members` — RULE-IMPL-DEAD-CLICKABLE-001
 * Rule 1. [Internal] is the sanctioned async-result-routing sub-interface (never a user intent) per
 * `training-layer/TRAINING_MASTER.yaml#patterns.actions`, mirrors
 * `LoanMarkDefaultedDialogAction.Internal`. See API.md#actions.
 */
sealed interface SettingsLogoutDialogAction {
    data object OnConfirmLogout : SettingsLogoutDialogAction
    data object OnDismiss : SettingsLogoutDialogAction

    /** Async coroutine results — routed via `trySendAction`, never dispatched by the UI. */
    sealed interface Internal : SettingsLogoutDialogAction {
        data object LogoutSucceeded : Internal
        data class LogoutFailed(val throwable: Throwable) : Internal
    }
}

/**
 * MVI processor for the `settings-logout-dialog` confirm dialog (`business_logic.kind: crud` per
 * ui.yaml — a single client-side session clear, no Fineract API call). [authRepository] and
 * [syncQueueRepository] are consumed directly as the REAL shipped infra —
 * `ui.yaml#state_model.di` names nominal `SessionManager` / `NavigationManager` types, but the
 * shipped data layer instead exposes [AuthRepository] (whose [AuthRepository.clearSession] IS the
 * logout call — it clears the persisted token from `CompanionSessionStore` /
 * `multiplatform-settings`) and [SyncQueueRepository] (the Room-backed offline write-queue this
 * dialog reads for the unsynced-count warning). Consumed here verbatim per the caller's brief —
 * flagged for an idea-layer `ui.yaml#state_model.di` backfill
 * (RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1), same drift class as
 * `LoanMarkDefaultedDialogViewModel`'s `LoanRepository` -> `LoanWriteoffRepository` note.
 *
 * **No offline queue / no network call** — `AuthRepository.clearSession()` is purely local
 * (`CompanionSessionStore` write), so unlike `LoanMarkDefaultedDialogViewModel` there is no
 * `NetworkMonitor` dependency and no `NetworkResult` wrapper: failures can only be a local-storage
 * exception, which [handleConfirmLogout] catches explicitly (never silently swallowed) and maps to
 * [SettingsLogoutDialogState.logoutError] via [LOGOUT_FAILED_MESSAGE_KEY].
 *
 * **`data-flow.yaml#entries[1].local_writes` declares a `flush_pending` step** ("Attempt to sync
 * any queued ops before clearing session") ahead of `clear_session` — this ViewModel intentionally
 * does NOT call any `SyncQueueRepository` write method before `clearSession()`: `SyncQueueRepository`
 * has no `flushPending()`/network-replay method of its own (replay is owned by the app-level sync
 * worker consuming [SyncQueueRepository.observePending], out of this dialog's scope), so "flush"
 * here is read-only — [unsyncedCount] surfaces the pending+failed count as the pre-logout WARNING
 * `ui.yaml#components.logout_body_text` describes ("Any unsynced changes will be lost"), rather than
 * fabricating a queue-flush call this repository does not expose. Flagged for
 * RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1.
 *
 * See API.md#viewmodel.
 */
class SettingsLogoutDialogViewModel(
    private val authRepository: AuthRepository,
    private val syncQueueRepository: SyncQueueRepository,
    private val analytics: KptAnalyticsTracker,
    private val crashReporter: CrashReporter,
) : BaseViewModel<SettingsLogoutDialogState, SettingsLogoutDialogEvent, SettingsLogoutDialogAction>(
    initialState = SettingsLogoutDialogState(),
) {

    private var logoutJob: Job? = null

    init {
        crashReporter.recordMessage(
            message = "feature=settings-logout-dialog screen=settingsLogoutDialogScreen",
            level = CrashSeverity.Debug,
        )
        viewModelScope.launch {
            syncQueueRepository.observeCounts().collect { counts ->
                updateState { copy(unsyncedCount = counts.pending + counts.failed) }
            }
        }
    }

    override fun handleAction(action: SettingsLogoutDialogAction) {
        when (action) {
            SettingsLogoutDialogAction.OnConfirmLogout -> handleConfirmLogout()
            SettingsLogoutDialogAction.OnDismiss -> handleDismiss()
            SettingsLogoutDialogAction.Internal.LogoutSucceeded -> handleLogoutSucceeded()
            is SettingsLogoutDialogAction.Internal.LogoutFailed -> handleLogoutFailed(action.throwable)
        }
    }

    // -- Dismiss (ui.yaml effect: navigate — pop the dialog, no session touched) --------------------

    private fun handleDismiss() {
        Logger.i(TAG) { "OnDismiss tapped — logout cancelled" }
        sendEvent(SettingsLogoutDialogEvent.Dismiss)
    }

    // -- Confirm (ui.yaml effect: delete, library_refs: [SQLDelight, Store5, multiplatform-settings]) -

    private fun handleConfirmLogout() {
        if (state.isLoggingOut) {
            Logger.w(TAG) { "OnConfirmLogout ignored — logout already in flight" }
            return
        }

        logoutJob?.cancel()
        logoutJob = viewModelScope.launch {
            updateState { copy(isLoggingOut = true, logoutError = null) }

            val result = try {
                authRepository.clearSession()
                SettingsLogoutDialogAction.Internal.LogoutSucceeded
            } catch (throwable: Throwable) {
                SettingsLogoutDialogAction.Internal.LogoutFailed(throwable)
            }
            trySendAction(result)
        }
    }

    // -- Async result routing ------------------------------------------------------------------------

    private fun handleLogoutSucceeded() {
        analytics.trackLogin(method = "logout", success = true)
        Logger.i(TAG) { "clearSession succeeded — navigating to login" }
        updateState { copy(isLoggingOut = false, logoutError = null) }
        sendEvent(SettingsLogoutDialogEvent.NavigateToLogin)
    }

    private fun handleLogoutFailed(throwable: Throwable) {
        analytics.trackLogin(method = "logout", success = false, errorCode = throwable::class.simpleName)
        crashReporter.recordException(throwable)
        Logger.w(TAG) { "clearSession failed: ${throwable.message}" }
        updateState { copy(isLoggingOut = false, logoutError = LOGOUT_FAILED_MESSAGE_KEY) }
    }
}
