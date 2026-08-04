/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.loanrequest

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitor
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kpt.core.analytics.KptAnalyticsTracker
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.base.observability.CrashReporter
import kpt.core.base.observability.CrashSeverity
import kpt.core.base.ui.viewmodel.BaseViewModel
import org.mifos.groupbanking.core.data.repository.LoanRequestRepository
import org.mifos.groupbanking.core.model.LoanPurpose
import org.mifos.groupbanking.core.model.LoanRequestPayload
import org.mifos.groupbanking.core.model.LoanRequestResult

// MVI stack (State/Event/Action/ViewModel/DI) for the `loan-request` feature — see
// API.md#viewmodel / #state / #actions / #events / #di for the full generated-symbol contract.
private const val TAG = "LoanRequestViewModel"

/** Floor enforced on [LoanRequestState.requestedAmount] — `ui.yaml#i18n.en.amount_supporting_text`. */
private const val MIN_LOAN_AMOUNT = 500.0

/**
 * Group-rate assumption used to derive [LoanRequestState.repaymentEstimate] — `ui.yaml`'s
 * `repayment_summary_card` displays an `interest_label` ("Interest (group rate)") row but neither
 * `ui.yaml#state_model` nor `api.yaml#dtos` declares a numeric group interest rate field anywhere
 * (no server-sourced rate reaches this screen — `LoanRequestPayload` carries no rate/product
 * field). This flat 10% rate is a DOCUMENTED, DELIBERATE placeholder — not fabricated silently —
 * matching `LoanApplyViewModel.computeCorpusWarning`'s identical "no declared numeric threshold,
 * literal predicate chosen and flagged" precedent. Flagged for the cross-feature repair station
 * (RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1) — the fix is a real group-configured interest rate
 * threaded through `nav_params`/`SessionManager` once available; swapping this constant is then a
 * one-line change (see [computeRepaymentEstimate]).
 */
private const val GROUP_INTEREST_RATE = 0.10

/**
 * Screen-level render state for `loan-request-screen` — verbatim mirror of
 * `ui.yaml#state_model.LoanRequestViewModel.screen_state.members`. Derived only (not stored) via
 * [LoanRequestState.deriveScreenState] — same convention as `LoanApplyState`/`MemberAddState`
 * (keeps `submitError`/`successDialogVisible`/`isOfflineMode`/`isSubmitting` the single source of
 * truth instead of a sixth, independently-mutable flag). See API.md#state.
 */
@Serializable
sealed interface LoanRequestScreenState {
    @Serializable
    data object Content : LoanRequestScreenState

    @Serializable
    data object Submitting : LoanRequestScreenState

    @Serializable
    data object SubmitSuccess : LoanRequestScreenState

    @Serializable
    data object SubmitError : LoanRequestScreenState

    @Serializable
    data object OfflineQueued : LoanRequestScreenState
}

/**
 * Submission error taxonomy — verbatim mirror of `ui.yaml#state_model.LoanRequestViewModel.errors.types`.
 * [messageKey] is a composeResources string-resource id (never a raw hardcoded English string, per
 * RULE-IMPL-NO-HARDCODED-STRING-001) resolved by the Screen layer.
 *
 * **[Network] / [Server] never rest in [LoanRequestState.submitError] (deliberate, documented
 * divergence from a literal `ui.yaml#error_snackbar` reading) — per this generation step's brief
 * (`"Error Network/Server (transport/503) -> retry-enqueue"`), [LoanRequestViewModel.handleSubmitResult]
 * routes both variants straight into [LoanRequestViewModel.retryEnqueue] (-> [LoanRequestScreenState.OfflineQueued])
 * rather than persisting them for `error_snackbar` display — the retry-enqueue already accomplishes
 * the retry semantics the snackbar's "Retry" button would otherwise trigger manually, and matches
 * `data-flow.yaml#entries[on_submit_click].error_paths`'s `503`/`network.offline -> enqueue_sync`
 * rows. This is a NARROWER outcome than `data-flow.yaml`'s `500 -> show_retry` row would suggest in
 * isolation, but [NetworkError] exposes only ONE `SERVER` bucket for both 500 and 503 (no
 * distinguishing sub-code, same single-bucket-reachability-gap class as
 * `LoanApplyError.CorpusInsufficient`/`MemberAddError.PhoneAlreadyExists`) so the two `data-flow.yaml`
 * rows cannot be told apart here; both variants are STILL exhaustively covered in every `when`
 * (RULE-IMPL-DEAD-CLICKABLE-001 Rule 2). [Validation] and [Unauthorized] DO persist to
 * [LoanRequestState.submitError] and drive the real `error_snackbar` (`ui.yaml#components.error_snackbar`,
 * generic "Retry" label wired to [LoanRequestAction.OnRetry] regardless of the taxonomy's per-type
 * [retry] flag — `ui.yaml` declares one snackbar for every non-null `submitError`, not a
 * conditionally-hidden button). [Unauthorized]'s declared `data-flow.yaml` `401 -> navigate
 * login-signup` contract has no dedicated `NavigateToLogin` event in `ui.yaml#events` (only 3
 * events are declared) — same documented "no dedicated nav event" gap class as `MemberAddError.Auth`
 * / `LoanApplyError.Auth`; it renders inline via `error_snackbar` instead. Flagged for the
 * cross-feature repair station (RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1).
 *
 * See API.md#state.
 */
@Serializable
sealed interface SubmitError {
    val retry: Boolean
    val messageKey: String

    @Serializable
    data object Network : SubmitError {
        override val retry: Boolean = true
        override val messageKey: String = "error_network_queued"
    }

    @Serializable
    data object Server : SubmitError {
        override val retry: Boolean = true
        override val messageKey: String = "error_server"
    }

    @Serializable
    data object Validation : SubmitError {
        override val retry: Boolean = false
        override val messageKey: String = "error_validation"
    }

    @Serializable
    data object Unauthorized : SubmitError {
        override val retry: Boolean = false
        override val messageKey: String = "error_session_expired"
    }
}

/**
 * MVI state for `LoanRequestViewModel`. Field set + defaults are a verbatim mirror of
 * `ui.yaml#state_model.LoanRequestViewModel.state.fields` (15 fields, declaration order
 * preserved). [submitError] is `@Transient` — a screen-level render concern that should not
 * survive process death, mirrors `MemberAddState.error`/`LoanApplyState.error`'s identical
 * `@Transient` convention. [purpose] is a plain nullable [LoanPurpose] enum value — kotlinx.serialization
 * serializes Kotlin enums natively, no extra annotation needed (same convention as
 * `LoanApplyState.purpose`, which additionally defaults non-null since that screen pre-selects
 * BUSINESS; this screen's `ui.yaml` declares no default purpose so it starts `null` and gates
 * [isFormValid] until the member picks one).
 *
 * See API.md#state.
 */
@Serializable
@Immutable
data class LoanRequestState(
    val clientId: Long = 0L,
    val savingsBalance: Double = 0.0,
    val loanMultiplier: Double = 3.0,
    val maxLoanAmount: Double = 0.0,
    val requestedAmount: String = "",
    val requestedAmountError: String? = null,
    val purpose: LoanPurpose? = null,
    val purposeError: String? = null,
    val durationWeeks: Int = 12,
    val repaymentEstimate: Double = 0.0,
    val isOfflineMode: Boolean = false,
    val isSubmitting: Boolean = false,
    val isFormValid: Boolean = false,
    @Transient
    val submitError: SubmitError? = null,
    val successDialogVisible: Boolean = false,
)

/**
 * Derives [LoanRequestScreenState] from [LoanRequestState] — see the type's KDoc for why this is a
 * pure function rather than a stored field (mirrors `MemberAddState.deriveScreenState()`).
 * [LoanRequestScreenState.OfflineQueued] is checked before [LoanRequestScreenState.SubmitSuccess]
 * because both `handleEnqueueResult` (offline / retry-enqueue path) and `handleSubmitResult`'s
 * success branch set `successDialogVisible = true` — only the offline-enqueue path also sets
 * `isOfflineMode = true` in the SAME state update, so the combination disambiguates the two.
 */
fun LoanRequestState.deriveScreenState(): LoanRequestScreenState = when {
    submitError != null -> LoanRequestScreenState.SubmitError
    isOfflineMode && successDialogVisible -> LoanRequestScreenState.OfflineQueued
    successDialogVisible -> LoanRequestScreenState.SubmitSuccess
    isSubmitting -> LoanRequestScreenState.Submitting
    else -> LoanRequestScreenState.Content
}

/**
 * One-shot side effects emitted by `LoanRequestViewModel` — verbatim mirror of
 * `ui.yaml#state_model.LoanRequestViewModel.events.members`.
 *
 * **[NavigateBack] reachability gap (flagged, not fabricated around):** `ui.yaml#components.top_bar`
 * declares `on_click.action: NavigateBack`, but `ui.yaml#state_model.actions.members` (the 6-member
 * list [LoanRequestAction] mirrors verbatim per RULE-IMPL-DEAD-CLICKABLE-001 Rule 1) does NOT
 * declare a matching `OnBack`/`NavigateBack` action member — same class of drift as
 * `loan-list`/`group-list`'s documented top-bar gap (contrast `LoanDetailViewModel`'s KDoc, which
 * notes NO such drift for that screen). [NavigateBack] is therefore declared here (verbatim
 * `events.members` mirror) but never emitted by this ViewModel; the Screen layer's `KptScaffold`
 * back button is expected to invoke the nav-host callback directly (bypassing action dispatch)
 * until the idea-layer adds the missing action member. Flagged for idea-layer correction
 * (RULE-IDEA-ACTION-CONTRACT-001).
 *
 * See API.md#events.
 */
sealed interface LoanRequestEvent {
    data object NavigateToDashboardAfterSuccess : LoanRequestEvent
    data object NavigateBack : LoanRequestEvent
    data object ShowOfflineQueuedConfirmation : LoanRequestEvent
}

/**
 * User intents dispatched to `LoanRequestViewModel`. The 6 top-level members are a verbatim mirror
 * of `ui.yaml#state_model.LoanRequestViewModel.actions.members` — RULE-IMPL-DEAD-CLICKABLE-001
 * Rule 1. [Internal] is the sanctioned async-result-routing sub-interface (never a user intent) per
 * `training-layer/TRAINING_MASTER.yaml#patterns.actions` — mirrors `MemberAddAction.Internal` /
 * `LoanApplyAction.Internal`. See API.md#actions.
 */
sealed interface LoanRequestAction {
    data class OnAmountChange(val value: String) : LoanRequestAction
    data class OnPurposeSelected(val purpose: LoanPurpose) : LoanRequestAction
    data class OnDurationChanged(val weeks: Int) : LoanRequestAction
    data object OnSubmitClick : LoanRequestAction
    data object OnSuccessDialogDismiss : LoanRequestAction
    data object OnRetry : LoanRequestAction

    /** Async coroutine results — routed via `trySendAction`, never dispatched by the UI. */
    sealed interface Internal : LoanRequestAction {
        data class ConnectivityChanged(val online: Boolean) : Internal
        data class SubmitResult(
            val payload: LoanRequestPayload,
            val result: NetworkResult<LoanRequestResult, NetworkError>,
        ) : Internal
        data class EnqueueResult(val queueId: Long) : Internal

        /** Result of the mount-time savings-balance resolve — recomputes [maxLoanAmount]. */
        data class SavingsBalanceLoaded(val balance: Double) : Internal
    }
}

/**
 * MVI processor for the member-side loan-request form (`business_logic.kind: crud` per ui.yaml — a
 * client-validated single-page form whose submit chains one Ktor POST plus an offline-queue
 * fallback via [LoanRequestRepository]; per RULE-IDEA-IMPL-INTELLIGENCE-001 AC-03i the SP-04 AC-7
 * analytics/crashReporter/fieldEncryptor injection TRIPLE is not mandatory for `crud`, but
 * [crashReporter] and [analytics] are still wired for real, feature-level observability (SC5),
 * mirroring `LoanDetailViewModel`'s identical crud-but-observed precedent). No `FieldEncryptor`
 * (`core-base/security`) is injected — `data-flow.yaml` declares no `pii_columns` entry for this
 * screen; neither [LoanRequestPayload] nor [LoanRequestResult] carries a `@PII`-marked field per
 * the SP-02 idea-layer schema.
 *
 * [LoanRequestRepository] is consumed directly via [NetworkResult] rather than a Store5
 * `.asScreenStream()`/`.write()` — [LoanRequestRepository] KDoc's own "Store5 branch" note (no
 * read-stream to back with a cache; every read this screen needs arrives via nav-arg), same branch
 * as [org.mifos.groupbanking.feature.memberadd.MemberAddViewModel] /
 * [org.mifos.groupbanking.feature.loanapply.LoanApplyViewModel]. `SessionManager` /
 * `ConnectivityManager` (`ui.yaml#state_model.di`) are represented here by [networkMonitor]
 * ([NetworkMonitor], `cmp-network-monitor` per `business_logic.library_refs`) only — no
 * `SessionManager` is injected because a 401 has no dedicated nav event to route through (see
 * [SubmitError] KDoc); `ui.yaml#state_model.di`'s separately-listed `SyncQueueRepository` is
 * represented by [LoanRequestRepository.enqueueOffline] — the shipped data layer bundles the
 * SyncQueue write behind the ONE purpose-built [LoanRequestRepository] rather than a second
 * injected repository (flagged documentation-vs-implementation drift, not re-created here, same
 * class as `LoanApplyViewModel`'s documented `di` drift note).
 *
 * **Offline / transport-retry submission (documented design, per this generation step's explicit
 * brief):** [handleSubmitClick] proactively checks [networkMonitor] before attempting the network
 * call — offline routes straight to [LoanRequestRepository.enqueueOffline] via [Internal.EnqueueResult]
 * (never calling [LoanRequestRepository.submit] at all). When [submitLoanRequest] DOES call
 * [LoanRequestRepository.submit] and the result is a transport-level [NetworkError] mapping to
 * [SubmitError.Network]/[SubmitError.Server], [retryEnqueue] re-routes the SAME [LoanRequestPayload]
 * to [LoanRequestRepository.enqueueOffline] rather than surfacing an inline error — see
 * [SubmitError] KDoc for the full rationale. [isOfflineMode] is ALSO driven reactively by
 * [networkMonitor]'s continuous `isOnline` stream (`Internal.ConnectivityChanged`) so
 * `ui.yaml#components.offline_mode_banner` (`visible_when: "isOfflineMode == true"`) reflects live
 * connectivity even before the member taps Submit — same dual (reactive + proactive) pattern as
 * `MemberAddViewModel`.
 *
 * [clientId]/[savingsBalance]/[loanMultiplier] are `ui.yaml#nav_params` forwarded from
 * `personal-dashboard`'s "Request Loan" CTA or `personal-loans`'s FAB — [maxLoanAmount] is computed
 * once in the constructor's `initialState` (`savingsBalance * loanMultiplier`, per `data-flow.yaml#entries[on_mount]`)
 * and never recomputed thereafter (the nav-args themselves are immutable for the lifetime of this
 * ViewModel).
 *
 * See API.md#viewmodel.
 */
internal class LoanRequestViewModel(
    private val repository: LoanRequestRepository,
    private val networkMonitor: NetworkMonitor,
    private val analytics: KptAnalyticsTracker,
    private val crashReporter: CrashReporter,
    private val clientId: Long,
    private val savingsBalance: Double,
    private val loanMultiplier: Double = 3.0,
) : BaseViewModel<LoanRequestState, LoanRequestEvent, LoanRequestAction>(
    initialState = LoanRequestState(
        clientId = clientId,
        savingsBalance = savingsBalance,
        loanMultiplier = loanMultiplier,
        maxLoanAmount = savingsBalance * loanMultiplier,
    ),
) {

    private var submitJob: Job? = null

    init {
        crashReporter.recordMessage(
            message = "feature=loan-request screen=loan-request-screen clientId=$clientId",
            level = CrashSeverity.Debug,
        )
        analytics.trackLoanOperation(operation = "request_view", loanId = null)
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                trySendAction(LoanRequestAction.Internal.ConnectivityChanged(online))
            }
        }
        // Resolve the member's real savings balance at mount — the nav-param savingsBalance is not
        // reliably populated upstream (personal-dashboard/personal-loans threading gap), which left
        // eligibility (max borrow) stuck at 0. Best-effort: on failure the nav-param value stands.
        viewModelScope.launch {
            when (val result = repository.memberSavingsBalance(clientId)) {
                is NetworkResult.Success ->
                    trySendAction(LoanRequestAction.Internal.SavingsBalanceLoaded(result.data))
                is NetworkResult.Error ->
                    Logger.w(TAG) { "memberSavingsBalance resolve failed (${result.error}) — keeping nav-param savingsBalance" }
            }
        }
    }

    override fun handleAction(action: LoanRequestAction) {
        when (action) {
            is LoanRequestAction.OnAmountChange -> handleAmountChange(action.value)
            is LoanRequestAction.OnPurposeSelected -> handlePurposeSelected(action.purpose)
            is LoanRequestAction.OnDurationChanged -> handleDurationChanged(action.weeks)
            LoanRequestAction.OnSubmitClick -> handleSubmitClick()
            LoanRequestAction.OnSuccessDialogDismiss -> handleSuccessDialogDismiss()
            LoanRequestAction.OnRetry -> handleRetry()
            is LoanRequestAction.Internal.ConnectivityChanged -> handleConnectivityChanged(action.online)
            is LoanRequestAction.Internal.SubmitResult -> handleSubmitResult(action.payload, action.result)
            is LoanRequestAction.Internal.EnqueueResult -> handleEnqueueResult(action.queueId)
            is LoanRequestAction.Internal.SavingsBalanceLoaded -> handleSavingsBalanceLoaded(action.balance)
        }
    }

    // -- Amount / purpose / duration (ui.yaml effect: transform_state — pure, no I/O) ----------------
    // data-flow.yaml#entries[OnAmountChange/OnPurposeSelected/OnDurationChanged]: pure client-side
    // transforms; requestedAmountError / repaymentEstimate / isFormValid recompute on every keystroke
    // against the CURRENT maxLoanAmount/durationWeeks already resident in state.

    private fun handleAmountChange(value: String) {
        val amountError = validateAmount(value, state.maxLoanAmount)
        val estimate = computeRepaymentEstimate(value, state.durationWeeks)
        updateState {
            copy(
                requestedAmount = value,
                requestedAmountError = amountError,
                repaymentEstimate = estimate,
                isFormValid = computeFormValid(
                    amount = value,
                    amountError = amountError,
                    purpose = purpose,
                    isSubmitting = isSubmitting,
                ),
            )
        }
    }

    // Mount-time savings resolve (Internal.SavingsBalanceLoaded): recompute the eligibility ceiling
    // from the member's REAL savings, then re-validate any amount already typed against the new max.
    private fun handleSavingsBalanceLoaded(balance: Double) {
        updateState {
            val newMax = balance * loanMultiplier
            val amountError = validateAmount(requestedAmount, newMax)
            copy(
                savingsBalance = balance,
                maxLoanAmount = newMax,
                requestedAmountError = amountError,
                isFormValid = computeFormValid(
                    amount = requestedAmount,
                    amountError = amountError,
                    purpose = purpose,
                    isSubmitting = isSubmitting,
                ),
            )
        }
    }

    private fun handlePurposeSelected(purpose: LoanPurpose) {
        updateState {
            copy(
                purpose = purpose,
                purposeError = null,
                isFormValid = computeFormValid(
                    amount = requestedAmount,
                    amountError = requestedAmountError,
                    purpose = purpose,
                    isSubmitting = isSubmitting,
                ),
            )
        }
    }

    private fun handleDurationChanged(weeks: Int) {
        Logger.d(TAG) { "duration changed to weeks=$weeks clientId=$clientId" }
        val estimate = computeRepaymentEstimate(state.requestedAmount, weeks)
        updateState { copy(durationWeeks = weeks, repaymentEstimate = estimate) }
    }

    // -- Submit (ui.yaml effect: call_api, library_refs: [cmp-network-monitor]) -----------------------
    // data-flow.yaml#entries[OnSubmitClick]: "Final client-side validation runs (amount within
    // [500, maxLoanAmount], purpose selected)" — this final gate is independent of isFormValid
    // (which already disables the submit button) since ui.yaml's submit_button.enabled_when only
    // soft-gates the UI; a defensive re-validation here matches MemberAddViewModel.handleSubmit's
    // identical "validate again on submit" precedent.

    private fun handleSubmitClick() {
        val amount = state.requestedAmount
        val amountError = validateAmount(amount, state.maxLoanAmount)
            ?: if (amount.isBlank()) "error_validation" else null
        val purposeError = if (state.purpose == null) "error_validation" else null
        if (amountError != null || purposeError != null) {
            Logger.w(TAG) {
                "OnSubmitClick validation failed clientId=$clientId amountError=$amountError purposeError=$purposeError"
            }
            updateState { copy(requestedAmountError = amountError, purposeError = purposeError, isFormValid = false) }
            return
        }
        submitLoanRequest()
    }

    /**
     * `data-flow.yaml#entries[OnRetry]`: "no client-side validation gate — validation succeeded on
     * the original submit; only transport failed." Re-invokes [submitLoanRequest] directly against
     * the CURRENT (unchanged since the prior failed attempt) form fields.
     */
    private fun handleRetry() {
        Logger.i(TAG) { "OnRetry tapped — re-submitting loan request clientId=$clientId" }
        submitLoanRequest()
    }

    /**
     * Assembles [LoanRequestPayload] from the validated form fields and either enqueues it directly
     * (device offline — see class KDoc) or posts it via [LoanRequestRepository.submit]. Defensively
     * no-ops (logs + returns) if [LoanRequestState.requestedAmount]/[LoanRequestState.purpose] are
     * somehow incomplete — unreachable in practice since both [handleSubmitClick] and [handleRetry]
     * only route here after validation passed, but avoids a non-null assertion.
     */
    private fun submitLoanRequest() {
        val amount = state.requestedAmount.toDoubleOrNull()
        val purpose = state.purpose
        if (amount == null || purpose == null) {
            Logger.w(TAG) { "submitLoanRequest called with an incomplete form clientId=$clientId" }
            return
        }
        val payload = LoanRequestPayload(
            clientId = clientId,
            requestedAmount = amount,
            purpose = purpose,
            durationWeeks = state.durationWeeks,
            savingsBalanceAtRequest = state.savingsBalance,
        )

        submitJob?.cancel()
        submitJob = viewModelScope.launch {
            updateState { copy(isSubmitting = true, submitError = null) }
            analytics.trackLoanOperation(
                operation = "request_submit_start",
                loanId = null,
                amount = payload.requestedAmount.toString(),
            )

            if (!networkMonitor.isOnline.value) {
                Logger.w(TAG) { "submit_loan_request attempted while offline clientId=$clientId — queueing" }
                crashReporter.recordMessage(
                    message = "loan-request: submit attempted while offline clientId=$clientId",
                    level = CrashSeverity.Info,
                )
                val queueId = repository.enqueueOffline(payload)
                trySendAction(LoanRequestAction.Internal.EnqueueResult(queueId))
                return@launch
            }

            val result = repository.submit(payload)
            trySendAction(LoanRequestAction.Internal.SubmitResult(payload = payload, result = result))
        }
    }

    /**
     * Re-enqueues [payload] to [LoanRequestRepository.enqueueOffline] after a transport-level
     * [SubmitError.Network]/[SubmitError.Server] result — see [SubmitError] class KDoc.
     */
    private fun retryEnqueue(payload: LoanRequestPayload) {
        submitJob?.cancel()
        submitJob = viewModelScope.launch {
            val queueId = repository.enqueueOffline(payload)
            trySendAction(LoanRequestAction.Internal.EnqueueResult(queueId))
        }
    }

    // -- Async result routing --------------------------------------------------------------------------

    private fun handleConnectivityChanged(online: Boolean) {
        updateState { copy(isOfflineMode = !online) }
    }

    private fun handleSubmitResult(
        payload: LoanRequestPayload,
        result: NetworkResult<LoanRequestResult, NetworkError>,
    ) {
        when (result) {
            is NetworkResult.Success -> {
                val submission = result.data
                analytics.trackLoanOperation(
                    operation = "request_submit_success",
                    loanId = submission.resourceId.toString(),
                    success = true,
                )
                Logger.i(TAG) { "submit_loan_request succeeded resourceId=${submission.resourceId} clientId=$clientId" }
                updateState { copy(isSubmitting = false, submitError = null, successDialogVisible = true) }
            }
            is NetworkResult.Error -> {
                analytics.trackLoanOperation(operation = "request_submit_error", success = false)
                crashReporter.recordMessage(
                    message = "loan-request: submit_loan_request failed clientId=$clientId " +
                        "networkError=${result.error}",
                    level = CrashSeverity.Warning,
                )
                when (val mapped = result.error.toSubmitError()) {
                    SubmitError.Network, SubmitError.Server -> retryEnqueue(payload)
                    SubmitError.Validation, SubmitError.Unauthorized ->
                        updateState { copy(isSubmitting = false, submitError = mapped) }
                }
            }
        }
    }

    private fun handleEnqueueResult(queueId: Long) {
        Logger.i(TAG) { "loan-request queued to sync_queue id=$queueId clientId=$clientId" }
        analytics.trackOfflineOperation(operation = "enqueue", entityType = "loan", queueSize = null, success = true)
        updateState {
            copy(
                isSubmitting = false,
                isOfflineMode = true,
                submitError = null,
                successDialogVisible = true,
            )
        }
        sendEvent(LoanRequestEvent.ShowOfflineQueuedConfirmation)
    }

    private fun handleSuccessDialogDismiss() {
        updateState { copy(successDialogVisible = false) }
        sendEvent(LoanRequestEvent.NavigateToDashboardAfterSuccess)
    }
}

// ---------------------------------------------------------------------------------------------
// Top-level helpers (pure — independently unit-testable)
// ---------------------------------------------------------------------------------------------

/**
 * `ui.yaml#components.amount_field.on_change.action_contract`: "re-validates against
 * savingsBalance × loanMultiplier locally." A blank [amount] returns `null` (no error shown until
 * the member types something OR taps submit — `ui.yaml`'s `amount_supporting_text` already guides
 * the range) — `ui.yaml` declares only ONE validation message key (`error_validation`) for this
 * field, so non-numeric / below-floor / above-ceiling all resolve to the same shared
 * `"error_validation"` key, same "one shared key" precedent as
 * `MemberAddViewModel.validateMemberAddForm`'s `error_validation` reuse.
 */
private fun validateAmount(amount: String, maxLoanAmount: Double): String? {
    if (amount.isBlank()) return null
    val parsed = amount.toDoubleOrNull() ?: return "error_validation"
    return when {
        parsed < MIN_LOAN_AMOUNT -> "error_validation"
        parsed > maxLoanAmount -> "error_validation"
        else -> null
    }
}

/** `ui.yaml#components.submit_button.style.enabled_when`: `"isFormValid && !isSubmitting"`. */
private fun computeFormValid(
    amount: String,
    amountError: String?,
    purpose: LoanPurpose?,
    isSubmitting: Boolean,
): Boolean {
    val parsed = amount.toDoubleOrNull()
    return parsed != null && amountError == null && purpose != null && !isSubmitting
}

/**
 * `ui.yaml#components.duration_selector.on_change.action_contract` /
 * `repayment_summary_card` rows: weekly repayment = `principal * (1 + GROUP_INTEREST_RATE) /
 * durationWeeks` — see [GROUP_INTEREST_RATE] KDoc for the flagged rate-source gap. Returns `0.0`
 * for a blank/non-numeric [amount] or a non-positive [durationWeeks] (the `repayment_summary_card`
 * is `visible_when: "requestedAmount.isNotBlank() && requestedAmountError == null"`, so a `0.0`
 * estimate never actually renders in that state, but the field must still hold SOME value).
 */
private fun computeRepaymentEstimate(amount: String, durationWeeks: Int): Double {
    val parsed = amount.toDoubleOrNull() ?: return 0.0
    if (durationWeeks <= 0) return 0.0
    val totalRepayment = parsed * (1.0 + GROUP_INTEREST_RATE)
    return totalRepayment / durationWeeks
}

/**
 * Disambiguates the transport-level [NetworkError] onto [SubmitError] —
 * `data-flow.yaml#entries[OnSubmitClick].error_paths`:
 * `network.offline`/timeout -> [SubmitError.Network], `400 -> error_validation`, `401 -> navigate
 * login` (folded onto [SubmitError.Unauthorized], see [SubmitError] class KDoc), `404`/`500`/`503`
 * -> [SubmitError.Server]. `409` ("duplicate pending request") has no dedicated [SubmitError]
 * member — [NetworkError] carries no `409`/`CONFLICT` value (same single-bucket-reachability-gap
 * class documented on [SubmitError]); it currently falls into the [SubmitError.Server] bucket.
 * Flagged for the cross-feature repair station (RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1) — the
 * fix is a `NetworkError.CONFLICT` member threaded through `core-base/network`.
 */
private fun NetworkError.toSubmitError(): SubmitError = when (this) {
    NetworkError.REQUEST_TIMEOUT -> SubmitError.Network
    NetworkError.BAD_REQUEST -> SubmitError.Validation
    NetworkError.UNAUTHORIZED, NetworkError.TOO_MANY_REQUESTS -> SubmitError.Unauthorized
    NetworkError.NOT_FOUND, NetworkError.SERIALIZATION, NetworkError.SERVER, NetworkError.UNKNOWN ->
        SubmitError.Server
}
