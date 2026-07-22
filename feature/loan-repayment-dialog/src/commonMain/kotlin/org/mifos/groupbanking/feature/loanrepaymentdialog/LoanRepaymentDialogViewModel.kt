/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.loanrepaymentdialog

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
import org.mifos.groupbanking.core.data.repository.LoanRepaymentRepository
import org.mifos.groupbanking.core.model.PaymentMethod
import org.mifos.groupbanking.core.model.RecordRepaymentRequest
import org.mifos.groupbanking.core.model.RepaymentResult
import kotlin.math.roundToLong

// MVI stack (State/Event/Action/ViewModel/DI) for the `loan-repayment-dialog` feature — see
// API.md#viewmodel / #state / #actions / #events / #di for the full generated-symbol contract.
private const val TAG = "LoanRepaymentDialogViewModel"

/**
 * `data-flow.yaml#entries[OnSubmit].error_paths` message key for `network.offline` /
 * [NetworkError.REQUEST_TIMEOUT] — "money moves require live confirmation to avoid double-posting
 * when connectivity returns" (class KDoc), so there is no offline sync_queue enqueue path, unlike
 * `MemberAddViewModel`'s `ShowOfflineSyncDialog`. Named constant so the one KNOWN GAP call site
 * (`ui.yaml#i18n.en` does not declare this key — see class KDoc "i18n gap" note) is discoverable by
 * grep for a future idea-layer backfill.
 */
private const val OFFLINE_MESSAGE_KEY = "error_offline_no_queue"

/**
 * MVI state for `LoanRepaymentDialogViewModel`. Field set + defaults are a verbatim mirror of
 * `ui.yaml#state_model.LoanRepaymentDialogViewModel.state` — this is a mutation DIALOG, not a
 * full screen, so (per the caller's brief) there is deliberately NO derived `ScreenState` enum;
 * [isSubmitting] / [amountError] / [submitError] are the inline render-state fields the dialog
 * composable reads directly, same shape as `ui.yaml#states` (`idle` / `submitting` / `error` are
 * all rendered from this ONE flat state, never a separate sealed hierarchy).
 *
 * [amount] is pre-filled from the `installmentAmount` nav-arg (see
 * [LoanRepaymentDialogViewModel] constructor) — the treasurer can still edit it (e.g. a partial
 * repayment), so it is a plain editable `String`, not a read-only display value.
 *
 * No `@PII` field here per the SP-02 idea-layer schema (amount / paymentMethod / referenceNumber
 * are all transaction metadata, not personal data) — `FieldEncryptor` (`core-base/security`) is
 * therefore intentionally NOT injected into [LoanRepaymentDialogViewModel], same "no PII" branch
 * as `GroupDashboardViewModel` / `MemberProfileViewModel`.
 *
 * See API.md#state.
 */
@Serializable
@Immutable
data class LoanRepaymentDialogState(
    val amount: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.MPESA,
    val referenceNumber: String = "",
    val isSubmitting: Boolean = false,
    val amountError: String? = null,
    val submitError: String? = null,
)

/**
 * One-shot side effects emitted by `LoanRepaymentDialogViewModel` — verbatim mirror of
 * `ui.yaml#state_model.LoanRepaymentDialogViewModel.events.members`. See API.md#events.
 */
sealed interface LoanRepaymentDialogEvent {
    data object Dismiss : LoanRepaymentDialogEvent
    data class RepaymentRecorded(val loanId: Long) : LoanRepaymentDialogEvent
    data class ShowError(val message: String) : LoanRepaymentDialogEvent
}

/**
 * User intents dispatched to `LoanRepaymentDialogViewModel`. The 5 top-level members are a
 * verbatim mirror of `ui.yaml#state_model.LoanRepaymentDialogViewModel.actions.members` —
 * RULE-IMPL-DEAD-CLICKABLE-001 Rule 1. [Internal] is the sanctioned async-result-routing
 * sub-interface (never a user intent) per `training-layer/TRAINING_MASTER.yaml#patterns.actions`,
 * mirrors `MemberAddAction.Internal` / `LoanDetailAction.Internal`. See API.md#actions.
 */
sealed interface LoanRepaymentDialogAction {
    data class OnAmountChanged(val value: String) : LoanRepaymentDialogAction
    data class OnPaymentMethodSelected(val method: PaymentMethod) : LoanRepaymentDialogAction
    data class OnReferenceNumberChanged(val value: String) : LoanRepaymentDialogAction
    data object OnSubmit : LoanRepaymentDialogAction
    data object OnDismiss : LoanRepaymentDialogAction

    /** Async coroutine results — routed via `trySendAction`, never dispatched by the UI. */
    sealed interface Internal : LoanRepaymentDialogAction {
        data class SubmitResult(val result: NetworkResult<RepaymentResult, NetworkError>) : Internal
    }
}

/**
 * MVI processor for the `loan-repayment-dialog` mutation dialog (`business_logic.kind: composite`
 * per ui.yaml — re-validates + POSTs `make_repayment`, so the SP-04 AC-7 analytics/crashReporter
 * injection pair applies per RULE-IDEA-IMPL-INTELLIGENCE-001 AC-03i). [repository] is consumed
 * directly via [NetworkResult] rather than a Store5 `.asScreenStream()` — `make_repayment` is a
 * single-shot write with no read-stream of its own to back with a cache, same branch as
 * [LoanRepaymentRepository] KDoc's own "Store5 branch" note / [org.mifos.groupbanking.core.data.repository.GroupCreateRepositoryImpl].
 * On success, [LoanRepaymentRepository.recordRepayment] itself invalidates the loan-detail cache
 * (see its KDoc) — this ViewModel does not need to touch `AppStoreRegistry.LoanDetail` directly.
 *
 * **Repository name drift (flagged, not re-created) — same class as `LoanDetailViewModel`'s
 * identical note:** `ui.yaml#state_model.di` / `api.yaml#dependencies.repositories` both name
 * `LoanRepository`, but the shipped data layer instead exposes a purpose-built
 * [LoanRepaymentRepository] for this single mutation. [LoanRepaymentRepository.recordRepayment]'s
 * real signature (`(loanId: Long, request: RecordRepaymentRequest): NetworkResult<RepaymentResult,
 * NetworkError>`) is used verbatim here rather than the idea-layer's flat
 * `(loanId, memberId, amount, paymentMethod, referenceNumber): Flow<Unit>` sketch.
 *
 * **`InputValidator` (di) has no implementation anywhere in this codebase** — `ui.yaml` names it
 * as a collaborator, but (same documented-gap class as `MemberAddViewModel`'s
 * `validateMemberAddForm`) the positive-number amount check is a plain private function
 * ([validateAmount]) in this file rather than an injected service. Flagged for
 * RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1 — a real `InputValidator` service, once it exists
 * anywhere else in this codebase, is a one-line swap-in here.
 *
 * **`amount <= outstanding` check deferred (flagged, not fabricated) —** `ui.yaml#i18n` declares
 * `error_amount_exceeds` and `data-flow.yaml#entries[OnSubmit].error_paths` maps HTTP 400 to it,
 * but neither `ui.yaml#nav_params` nor this ViewModel's declared DI graph carries the loan's
 * outstanding balance — only Fineract's server-side `make_repayment` validation can reject an
 * over-amount request today (surfaced via [NetworkError.BAD_REQUEST] -> [OFFLINE_MESSAGE_KEY]'s
 * sibling constant `"error_amount_exceeds"` in [toLoanRepaymentMessageKey]). [validateAmount]
 * therefore only enforces "positive number", per the caller's brief ("else just positive").
 *
 * **`memberId` nav-arg is threaded through but not consumed by [LoanRepaymentRepository]** —
 * `ui.yaml#nav_params` declares it (the treasurer is recording a repayment ON BEHALF OF a specific
 * member of the group), but [RecordRepaymentRequest] carries no member field (Fineract resolves
 * the payer from the loan's own `clientId`). Kept as a constructor parameter for log/crash-report
 * correlation context (see [init]) and forward-compatibility rather than silently dropped.
 *
 * **`401 -> navigate login-signup` (`data-flow.yaml#error_paths`) has no matching `NavigateTo*`
 * event declared in `ui.yaml#state_model.events.members`** — same documented gap class as
 * `MemberAddViewModel.MemberAddEvent.ShowSnackbar` / `LoanDetailViewModel`'s identical note.
 * [handleSubmitResult] maps [NetworkError.UNAUTHORIZED] to the closest declared event,
 * [LoanRepaymentDialogEvent.ShowError], rather than fabricating a nav event ui.yaml never
 * declared. Likewise `403 Forbidden` has no dedicated [NetworkError] value (folds into
 * [NetworkError.UNAUTHORIZED] / [NetworkError.TOO_MANY_REQUESTS], same limitation class as
 * `MemberAddViewModel.MemberAddError.PhoneAlreadyExists`'s KDoc).
 *
 * **i18n gap (flagged, not fabricated) —** `data-flow.yaml#error_paths` message keys
 * `error_offline_no_queue` / `error_insufficient_role` / `error_loan_not_found` / `error_auth` are
 * referenced but NOT declared under `ui.yaml#i18n.en` (only `error_amount_required` /
 * `error_amount_invalid` / `error_amount_exceeds` / `error_server` are). This ViewModel still
 * emits those keys verbatim (see [toLoanRepaymentMessageKey] / [OFFLINE_MESSAGE_KEY]) — the Screen
 * layer's `stringResource` lookup will need the composeResources entries backfilled, flagged for
 * an idea-layer i18n follow-up (RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1), not invented around
 * with a different, undeclared key.
 *
 * **Visibility (kmp-screen-gen note, not a re-derivation of this ViewModel's own logic):** this
 * class is public (not `internal`, unlike every other single-screen `*ViewModel` in this codebase)
 * because [org.mifos.groupbanking.feature.loanrepaymentdialog.LoanRepaymentDialog] — the Container
 * composable this ViewModel backs — is a DIALOG rendered directly from `cmp-navigation`'s
 * `GroupBankingNavHost.kt` (an overlay on top of `loan-detail`, not a routed
 * `NavGraphBuilder`/`*Route.kt` destination like every other feature), so its default
 * `viewModel: LoanRepaymentDialogViewModel = koinViewModel(...)` parameter type must be visible
 * cross-module. See API.md#viewmodel.
 */
class LoanRepaymentDialogViewModel(
    private val repository: LoanRepaymentRepository,
    private val networkMonitor: NetworkMonitor,
    private val analytics: KptAnalyticsTracker,
    private val crashReporter: CrashReporter,
    private val loanId: Long,
    private val memberId: Long,
    installmentAmount: Double,
) : BaseViewModel<LoanRepaymentDialogState, LoanRepaymentDialogEvent, LoanRepaymentDialogAction>(
    initialState = LoanRepaymentDialogState(amount = formatInstallmentAmount(installmentAmount)),
) {

    private var submitJob: Job? = null

    init {
        crashReporter.recordMessage(
            message = "feature=loan-repayment-dialog screen=loanRepaymentDialogScreen loanId=$loanId memberId=$memberId",
            level = CrashSeverity.Debug,
        )
    }

    override fun handleAction(action: LoanRepaymentDialogAction) {
        when (action) {
            is LoanRepaymentDialogAction.OnAmountChanged -> handleAmountChanged(action.value)
            is LoanRepaymentDialogAction.OnPaymentMethodSelected -> handlePaymentMethodSelected(action.method)
            is LoanRepaymentDialogAction.OnReferenceNumberChanged -> handleReferenceNumberChanged(action.value)
            LoanRepaymentDialogAction.OnSubmit -> handleSubmit()
            LoanRepaymentDialogAction.OnDismiss -> handleDismiss()
            is LoanRepaymentDialogAction.Internal.SubmitResult -> handleSubmitResult(action.result)
        }
    }

    // -- Field transforms (ui.yaml effect: transform_state) --------------------------------------

    private fun handleAmountChanged(value: String) {
        updateState { copy(amount = value, amountError = null) }
    }

    private fun handlePaymentMethodSelected(method: PaymentMethod) {
        updateState { copy(paymentMethod = method) }
    }

    private fun handleReferenceNumberChanged(value: String) {
        updateState { copy(referenceNumber = value) }
    }

    // -- Dismiss (ui.yaml effect: navigate) ---------------------------------------------------------

    private fun handleDismiss() {
        Logger.i(TAG) { "OnDismiss tapped loanId=$loanId — discarding unsaved repayment entry" }
        sendEvent(LoanRepaymentDialogEvent.Dismiss)
    }

    // -- Submit (ui.yaml effect: call_api, library_refs: [cmp-network-monitor]) ---------------------

    private fun handleSubmit() {
        val amountError = validateAmount(state.amount)
        if (amountError != null) {
            Logger.w(TAG) { "OnSubmit validation failed loanId=$loanId reason=$amountError" }
            updateState { copy(amountError = amountError) }
            return
        }

        submitJob?.cancel()
        submitJob = viewModelScope.launch {
            updateState { copy(isSubmitting = true, amountError = null, submitError = null) }
            analytics.trackLoanOperation(operation = "repay_start", loanId = loanId.toString())

            if (!networkMonitor.isOnline.value) {
                // No offline sync_queue for money moves — see class KDoc + OFFLINE_MESSAGE_KEY KDoc.
                Logger.w(TAG) { "make_repayment attempted while offline loanId=$loanId" }
                crashReporter.recordMessage(
                    message = "loan-repayment-dialog: submit attempted while offline loanId=$loanId",
                    level = CrashSeverity.Info,
                )
                updateState { copy(isSubmitting = false, submitError = OFFLINE_MESSAGE_KEY) }
                sendEvent(LoanRepaymentDialogEvent.ShowError(message = OFFLINE_MESSAGE_KEY))
                return@launch
            }

            val request = RecordRepaymentRequest(
                amount = state.amount.trim().toDouble(),
                paymentMethod = state.paymentMethod,
                referenceNumber = state.referenceNumber.trim().ifBlank { null },
            )
            val result = repository.recordRepayment(loanId = loanId, request = request)
            trySendAction(LoanRepaymentDialogAction.Internal.SubmitResult(result))
        }
    }

    // -- Async result routing ------------------------------------------------------------------------

    private fun handleSubmitResult(result: NetworkResult<RepaymentResult, NetworkError>) {
        when (result) {
            is NetworkResult.Success -> {
                analytics.trackLoanOperation(operation = "repay", loanId = loanId.toString(), success = true)
                Logger.i(TAG) { "make_repayment succeeded loanId=$loanId resourceId=${result.data.resourceId}" }
                updateState { copy(isSubmitting = false, submitError = null) }
                sendEvent(LoanRepaymentDialogEvent.RepaymentRecorded(loanId = loanId))
                sendEvent(LoanRepaymentDialogEvent.Dismiss)
            }

            is NetworkResult.Error -> {
                analytics.trackLoanOperation(operation = "repay", loanId = loanId.toString(), success = false)
                crashReporter.recordMessage(
                    message = "loan-repayment-dialog: make_repayment failed loanId=$loanId networkError=${result.error}",
                    level = CrashSeverity.Warning,
                )
                val messageKey = result.error.toLoanRepaymentMessageKey()
                updateState { copy(isSubmitting = false, submitError = messageKey) }
                sendEvent(LoanRepaymentDialogEvent.ShowError(message = messageKey))
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Top-level helpers (pure — independently unit-testable)
// ---------------------------------------------------------------------------------------------

/**
 * Client-side validation — `ui.yaml#components.amount_field.on_change.action_contract` +
 * `data-flow.yaml#entries[OnAmountChanged]`. Only "positive number" is enforced client-side (see
 * class KDoc "amount <= outstanding check deferred" note) — message keys are declared
 * `ui.yaml#i18n.en` entries (`error_amount_required` / `error_amount_invalid`).
 */
private fun validateAmount(raw: String): String? {
    val trimmed = raw.trim()
    return when {
        trimmed.isEmpty() -> "error_amount_required"
        trimmed.toDoubleOrNull() == null || trimmed.toDouble() <= 0.0 -> "error_amount_invalid"
        else -> null
    }
}

/**
 * Disambiguates the transport-level [NetworkError] onto `LoanRepaymentDialogState.submitError`'s
 * plain-`String` message-key contract — `data-flow.yaml#entries[OnSubmit].error_paths`:
 * `400 -> error_amount_exceeds`, `404 -> error_loan_not_found`, `401/429 -> error_auth` (see class
 * KDoc "401 -> navigate login-signup" note), `500/serialization/unknown -> error_server`.
 * [NetworkError.REQUEST_TIMEOUT] is treated as the connectivity-loss bucket ([OFFLINE_MESSAGE_KEY])
 * — `NetworkError` has no dedicated "offline" value, same documented gap class as
 * `MemberAddViewModel.toMemberAddError` / `LoanDetailViewModel`'s identical precedent.
 */
private fun NetworkError.toLoanRepaymentMessageKey(): String = when (this) {
    NetworkError.BAD_REQUEST -> "error_amount_exceeds"
    NetworkError.NOT_FOUND -> "error_loan_not_found"
    NetworkError.UNAUTHORIZED, NetworkError.TOO_MANY_REQUESTS -> "error_auth"
    NetworkError.REQUEST_TIMEOUT -> OFFLINE_MESSAGE_KEY
    NetworkError.SERVER, NetworkError.SERIALIZATION, NetworkError.UNKNOWN -> "error_server"
}

/**
 * Formats the `installmentAmount` nav-arg to a 2-decimal-place editable string (e.g. `125.5` ->
 * `"125.50"`) for [LoanRepaymentDialogState.amount]'s initial value. Pure Kotlin (no
 * `java.util.Formatter` / `String.format`, which is not commonMain-safe) — rounds to the nearest
 * cent via [roundToLong] then re-joins the whole/fractional parts, mirroring the KMP-safe date
 * formatting precedent in `MemberAddViewModel.todayFormattedDate`.
 */
private fun formatInstallmentAmount(value: Double): String {
    val totalCents = (value * 100).roundToLong()
    val whole = totalCents / 100
    val fractionCents = if (totalCents < 0) -(totalCents % 100) else totalCents % 100
    return "$whole.${fractionCents.toString().padStart(2, '0')}"
}
