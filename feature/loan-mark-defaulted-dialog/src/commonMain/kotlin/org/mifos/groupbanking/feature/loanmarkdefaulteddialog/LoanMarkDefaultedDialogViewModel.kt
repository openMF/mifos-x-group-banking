/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.loanmarkdefaulteddialog

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitor
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kpt.core.analytics.KptAnalyticsTracker
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.base.observability.CrashReporter
import kpt.core.base.observability.CrashSeverity
import kpt.core.base.ui.viewmodel.BaseViewModel
import org.mifos.groupbanking.core.data.repository.LoanWriteoffRepository
import org.mifos.groupbanking.core.model.WriteoffResult

// MVI stack (State/Event/Action/ViewModel/DI) for the `loan-mark-defaulted-dialog` feature — see
// API.md#viewmodel / #state / #actions / #events / #di for the full generated-symbol contract.
private const val TAG = "LoanMarkDefaultedDialogViewModel"

/**
 * `data-flow.yaml#entries[0].connectivity.offline_message_key` — "no offline sync_queue for this
 * destructive action" per `business_logic.description` (an irreversible Fineract write-off must
 * not silently queue and fire later once connectivity returns), same architectural branch as
 * `LoanRepaymentDialogViewModel`'s identical `OFFLINE_MESSAGE_KEY` constant. Named constant so the
 * one KNOWN GAP call site (`ui.yaml#i18n.en` does not declare this key — see class KDoc "i18n gap"
 * note) is discoverable by grep for a future idea-layer backfill.
 */
private const val OFFLINE_MESSAGE_KEY = "error_offline_no_queue"

/**
 * MVI state for `LoanMarkDefaultedDialogViewModel`. Field set + defaults are a verbatim mirror of
 * `ui.yaml#state_model.LoanMarkDefaultedDialogViewModel.state` — this is a confirm-only mutation
 * DIALOG (no input fields, unlike `LoanRepaymentDialogState`), so there is deliberately NO derived
 * `ScreenState` enum; [isSubmitting] / [submitError] are the inline render-state fields the dialog
 * composable reads directly, same shape as `ui.yaml#states` (`idle` / `submitting` / `error` are
 * all rendered from this ONE flat state).
 *
 * [memberName] / [loanAmountKes] are resolved from the `ui.yaml#nav_params` (see
 * [LoanMarkDefaultedDialogViewModel] constructor) purely for the confirmation-copy interpolation
 * (`warning_body: "...Member {{memberName}}'s loan of KES {{amount}}..."`) — this dialog performs
 * no edits of its own, so both fields are read-only display values, never mutated by an action.
 *
 * No `@PII` field here per the SP-02 idea-layer schema ([memberName] is a display label sourced
 * from the parent `loan-detail` screen, not collected/persisted here) — `FieldEncryptor`
 * (`core-base/security`) is therefore intentionally NOT injected into
 * [LoanMarkDefaultedDialogViewModel], same "no PII" branch as `LoanRepaymentDialogViewModel`.
 *
 * See API.md#state.
 */
@Serializable
@Immutable
data class LoanMarkDefaultedDialogState(
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    val memberName: String = "",
    val loanAmountKes: Double = 0.0,
)

/**
 * One-shot side effects emitted by `LoanMarkDefaultedDialogViewModel` — verbatim mirror of
 * `ui.yaml#state_model.LoanMarkDefaultedDialogViewModel.events.members`. See API.md#events.
 */
sealed interface LoanMarkDefaultedDialogEvent {
    data object Dismiss : LoanMarkDefaultedDialogEvent
    data class LoanMarkedDefaulted(val loanId: Long) : LoanMarkDefaultedDialogEvent
    data class ShowError(val message: String) : LoanMarkDefaultedDialogEvent
}

/**
 * User intents dispatched to `LoanMarkDefaultedDialogViewModel`. The 2 top-level members are a
 * verbatim mirror of `ui.yaml#state_model.LoanMarkDefaultedDialogViewModel.actions.members` —
 * RULE-IMPL-DEAD-CLICKABLE-001 Rule 1. [Internal] is the sanctioned async-result-routing
 * sub-interface (never a user intent) per `training-layer/TRAINING_MASTER.yaml#patterns.actions`,
 * mirrors `LoanRepaymentDialogAction.Internal`. See API.md#actions.
 */
sealed interface LoanMarkDefaultedDialogAction {
    data object OnConfirm : LoanMarkDefaultedDialogAction
    data object OnDismiss : LoanMarkDefaultedDialogAction

    /** Async coroutine results — routed via `trySendAction`, never dispatched by the UI. */
    sealed interface Internal : LoanMarkDefaultedDialogAction {
        data class SubmitResult(val result: NetworkResult<WriteoffResult, NetworkError>) : Internal
    }
}

/**
 * MVI processor for the `loan-mark-defaulted-dialog` confirm dialog (`business_logic.kind: crud`
 * per ui.yaml — a single irreversible write, so the SP-04 AC-7 analytics/crashReporter injection
 * pair applies per RULE-IDEA-IMPL-INTELLIGENCE-001 AC-03i). [repository] is consumed directly via
 * [NetworkResult] rather than a Store5 `.asScreenStream()` — `write_off_loan` is a single-shot
 * write with no read-stream of its own to back with a cache, same branch as
 * [LoanWriteoffRepository] KDoc's own "Store5 branch" note /
 * [org.mifos.groupbanking.feature.loanrepaymentdialog.LoanRepaymentDialogViewModel]. On success,
 * [LoanWriteoffRepository.writeoffLoan] itself invalidates the loan-detail cache (see its KDoc) —
 * this ViewModel does not need to touch `AppStoreRegistry.LoanDetail` directly.
 *
 * **Repository name drift (flagged, not re-created) — same class as `LoanRepaymentDialogViewModel`'s
 * identical note:** `ui.yaml#state_model.di` names `LoanRepository`, but the shipped data layer
 * instead exposes a purpose-built [LoanWriteoffRepository] for this single mutation. Consumed here
 * verbatim per the caller's brief.
 *
 * **`401 -> navigate login` (`data-flow.yaml#error_paths`) has no matching `NavigateTo*` event
 * declared in `ui.yaml#state_model.events.members`** — same documented gap class as
 * `LoanRepaymentDialogViewModel`'s identical note. [handleSubmitResult] maps
 * [NetworkError.UNAUTHORIZED] / [NetworkError.TOO_MANY_REQUESTS] to the closest declared event,
 * [LoanMarkDefaultedDialogEvent.ShowError], rather than fabricating a nav event ui.yaml never
 * declared.
 *
 * **403/409 disambiguation gap (flagged, not fabricated) —** `data-flow.yaml#error_paths` declares
 * distinct message keys for `403 -> error_forbidden` and `409 -> error_ineligible`, but
 * `LoanWriteoffApiImpl.toNetworkError` (the sole HTTP-status -> [NetworkError] mapping layer, per
 * Mandatory Rule 4) has no dedicated bucket for either status code — both fold into
 * [NetworkError.UNKNOWN] alongside any truly-unrecognized status. This ViewModel therefore cannot
 * safely disambiguate a 403 from a 409 from an unknown failure at the [NetworkError] level, so
 * [toLoanMarkDefaultedMessageKey] intentionally falls back [NetworkError.UNKNOWN] to the generic
 * `"error_server"` key rather than guessing between the two more specific, declared keys. A real
 * fix requires widening [NetworkError] (framework-owned, out of this feature's scope) — flagged
 * for RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1.
 *
 * **i18n gap (flagged, not fabricated) —** `data-flow.yaml#error_paths` message keys
 * `error_offline_no_queue` / `error_loan_not_found` are referenced but NOT declared under
 * `ui.yaml#i18n.en` (only `error_server` / `error_forbidden` / `error_ineligible` are). This
 * ViewModel still emits those keys verbatim (see [toLoanMarkDefaultedMessageKey] /
 * [OFFLINE_MESSAGE_KEY]) — the Screen layer's `stringResource` lookup will need the
 * composeResources entries backfilled, flagged for an idea-layer i18n follow-up
 * (RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1), same documented gap class as
 * `LoanRepaymentDialogViewModel`'s identical note.
 *
 * **Visibility (kmp-screen-gen note, not a re-derivation of this ViewModel's own logic):** this
 * class is public (not `internal`, unlike every other single-screen `*ViewModel` in this codebase)
 * because `LoanMarkDefaultedDialog` — the Container composable this ViewModel backs — is a DIALOG
 * rendered directly from `cmp-navigation`'s `GroupBankingNavHost.kt` (an overlay on top of
 * `loan-detail`, not a routed `NavGraphBuilder`/`*Route.kt` destination), same convention as
 * [org.mifos.groupbanking.feature.loanrepaymentdialog.LoanRepaymentDialogViewModel]. See
 * API.md#viewmodel.
 */
class LoanMarkDefaultedDialogViewModel(
    private val repository: LoanWriteoffRepository,
    private val networkMonitor: NetworkMonitor,
    private val analytics: KptAnalyticsTracker,
    private val crashReporter: CrashReporter,
    private val loanId: Long,
    memberName: String,
    loanAmountKes: Double,
) : BaseViewModel<LoanMarkDefaultedDialogState, LoanMarkDefaultedDialogEvent, LoanMarkDefaultedDialogAction>(
    initialState = LoanMarkDefaultedDialogState(memberName = memberName, loanAmountKes = loanAmountKes),
) {

    private var submitJob: Job? = null

    init {
        crashReporter.recordMessage(
            message = "feature=loan-mark-defaulted-dialog screen=loanMarkDefaultedDialogScreen loanId=$loanId",
            level = CrashSeverity.Debug,
        )
    }

    override fun handleAction(action: LoanMarkDefaultedDialogAction) {
        when (action) {
            LoanMarkDefaultedDialogAction.OnConfirm -> handleConfirm()
            LoanMarkDefaultedDialogAction.OnDismiss -> handleDismiss()
            is LoanMarkDefaultedDialogAction.Internal.SubmitResult -> handleSubmitResult(action.result)
        }
    }

    // -- Dismiss (ui.yaml effect: none) -------------------------------------------------------------

    private fun handleDismiss() {
        Logger.i(TAG) { "OnDismiss tapped loanId=$loanId — write-off cancelled" }
        sendEvent(LoanMarkDefaultedDialogEvent.Dismiss)
    }

    // -- Confirm (ui.yaml effect: call_api, library_refs: [cmp-network-monitor]) --------------------

    private fun handleConfirm() {
        submitJob?.cancel()
        submitJob = viewModelScope.launch {
            updateState { copy(isSubmitting = true, submitError = null) }
            analytics.trackLoanOperation(operation = "mark_defaulted_start", loanId = loanId.toString())

            if (!networkMonitor.isOnline.value) {
                // No offline sync_queue for this destructive action — see class KDoc +
                // OFFLINE_MESSAGE_KEY KDoc.
                Logger.w(TAG) { "write_off_loan attempted while offline loanId=$loanId" }
                crashReporter.recordMessage(
                    message = "loan-mark-defaulted-dialog: confirm attempted while offline loanId=$loanId",
                    level = CrashSeverity.Info,
                )
                updateState { copy(isSubmitting = false, submitError = OFFLINE_MESSAGE_KEY) }
                sendEvent(LoanMarkDefaultedDialogEvent.ShowError(message = OFFLINE_MESSAGE_KEY))
                return@launch
            }

            val result = repository.writeoffLoan(loanId = loanId)
            trySendAction(LoanMarkDefaultedDialogAction.Internal.SubmitResult(result))
        }
    }

    // -- Async result routing ------------------------------------------------------------------------

    private fun handleSubmitResult(result: NetworkResult<WriteoffResult, NetworkError>) {
        when (result) {
            is NetworkResult.Success -> {
                analytics.trackLoanOperation(operation = "mark_defaulted", loanId = loanId.toString(), success = true)
                Logger.i(TAG) { "write_off_loan succeeded loanId=$loanId resourceId=${result.data.resourceId}" }
                updateState { copy(isSubmitting = false, submitError = null) }
                sendEvent(LoanMarkDefaultedDialogEvent.LoanMarkedDefaulted(loanId = loanId))
                sendEvent(LoanMarkDefaultedDialogEvent.Dismiss)
            }

            is NetworkResult.Error -> {
                analytics.trackLoanOperation(operation = "mark_defaulted", loanId = loanId.toString(), success = false)
                crashReporter.recordMessage(
                    message = "loan-mark-defaulted-dialog: write_off_loan failed loanId=$loanId networkError=${result.error}",
                    level = CrashSeverity.Warning,
                )
                val messageKey = result.error.toLoanMarkDefaultedMessageKey()
                updateState { copy(isSubmitting = false, submitError = messageKey) }
                sendEvent(LoanMarkDefaultedDialogEvent.ShowError(message = messageKey))
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Top-level helpers (pure — independently unit-testable)
// ---------------------------------------------------------------------------------------------

/**
 * Disambiguates the transport-level [NetworkError] onto `LoanMarkDefaultedDialogState.submitError`'s
 * plain-`String` message-key contract — `data-flow.yaml#entries[0].error_paths`: `404 ->
 * error_loan_not_found`, `500/serialization -> error_server`. [NetworkError.REQUEST_TIMEOUT] is
 * treated as the connectivity-loss bucket ([OFFLINE_MESSAGE_KEY]) — `NetworkError` has no
 * dedicated "offline" value, same documented gap class as `LoanRepaymentDialogViewModel`'s
 * identical precedent. [NetworkError.UNAUTHORIZED] / [NetworkError.TOO_MANY_REQUESTS] and
 * [NetworkError.UNKNOWN] (which the 403/409 statuses both fold into) fall back to the generic
 * `"error_server"` key — see class KDoc "403/409 disambiguation gap" note.
 */
private fun NetworkError.toLoanMarkDefaultedMessageKey(): String = when (this) {
    NetworkError.NOT_FOUND -> "error_loan_not_found"
    NetworkError.REQUEST_TIMEOUT -> OFFLINE_MESSAGE_KEY
    NetworkError.BAD_REQUEST,
    NetworkError.UNAUTHORIZED,
    NetworkError.TOO_MANY_REQUESTS,
    NetworkError.SERVER,
    NetworkError.SERIALIZATION,
    NetworkError.UNKNOWN,
    -> "error_server"
}
