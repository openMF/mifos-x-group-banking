/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.shareoutexecute

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
import kpt.core.base.security.SessionManager
import kpt.core.base.ui.viewmodel.BaseViewModel
import org.mifos.groupbanking.core.data.repository.ShareOutRepository
import org.mifos.groupbanking.core.model.GroupTypeConfig
import org.mifos.groupbanking.core.model.MemberExecutionStatus
import org.mifos.groupbanking.core.model.MemberPayout
import org.mifos.groupbanking.core.model.RotationPayoutExecuteResult
import org.mifos.groupbanking.core.model.RotationPayoutRequest
import org.mifos.groupbanking.core.model.SavingsMechanism
import org.mifos.groupbanking.core.model.ShareOutExecuteRequest
import org.mifos.groupbanking.core.model.ShareOutExecuteResult
import org.mifos.groupbanking.core.model.ShareOutPreview
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

// MVI stack (State/ScreenState/Error/Event/Action/ViewModel/DI) for the `share-out-execute`
// feature — see API.md#viewmodel / #state / #actions / #events / #di for the full generated-symbol
// contract.
private const val TAG = "ShareOutExecuteViewModel"

/** The exact double-confirmation phrase the operator must type to unlock the Execute button. */
private const val CONFIRMATION_PHRASE = "SHARE OUT"

/**
 * The rotation-strategy formulas that flip the screen title + execute-button label to the
 * "Execute Rotation Payout" variant (`ui.yaml#components.execute_button.label`). Every other formula
 * renders the "Execute Share-Out" variant.
 */
internal val ROTATION_FORMULAS = setOf("FIXED_ORDER", "LOTTERY", "AUCTION")

/**
 * **KNOWN GAP — the preview→execute handoff carries only `{groupId, typeConfig, totalPool,
 * memberPayouts}`** (`ShareOutPreviewEvent.NavigateToShareOutExecute`). `api.yaml#body` for
 * COMP-DIST-001 additionally requires `cycleNumber` + `shareoutFormula`, and COMP-DIST-002 requires
 * the rotation `recipientMemberId` — none of which `ui.yaml#nav_params` name. [DEFAULT_CYCLE_NUMBER]
 * is the sentinel sent for `cycleNumber` until the preview's `NavigateToShareOutExecute` event (which
 * DOES hold `cycleNumber` in `ShareOutPreviewState`) is enriched to forward it. A real companion
 * backend echoes/validates it server-side; the value is audit-only, never a client branch. Flagged
 * for the cross-feature repair station (RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1) — same
 * documented-gap class as `MemberAddViewModel`'s `UNRESOLVED_OFFICE_ID` / `GroupCreateViewModel`'s
 * `shareoutFormula` default.
 */
private const val DEFAULT_CYCLE_NUMBER = 0

/**
 * Screen-level render state for `share-out-execute-screen` — verbatim mirror of
 * `ui.yaml#state_model.ShareOutExecuteViewModel.screen_state.members` (5 members). Derived only
 * (not stored) via [ShareOutExecuteState.deriveScreenState] — keeps `isExecuting`/`isCompleted`/
 * `queuedOffline`/`failedPayouts`/`error` the single source of truth (same convention as
 * `MemberAddState.deriveScreenState`). See API.md#state.
 */
@Serializable
sealed interface ShareOutExecuteScreenState {
    @Serializable
    data object Content : ShareOutExecuteScreenState

    @Serializable
    data object Executing : ShareOutExecuteScreenState

    @Serializable
    data object Success : ShareOutExecuteScreenState

    @Serializable
    data object PartialFailure : ShareOutExecuteScreenState

    @Serializable
    data object Error : ShareOutExecuteScreenState
}

/**
 * Execution error taxonomy — verbatim mirror of
 * `ui.yaml#state_model.ShareOutExecuteViewModel.errors.types` (5 members). [retry] mirrors the
 * per-type `retry:` flag; [messageKey] the `message_key:`. Kept a plain sealed interface so the
 * enclosing state carries it as a `@Transient` field (same convention as `ShareOutPreviewError`).
 *
 * **403 role-forbidden / 409 already-executed / 400 validation collapse (documented gap, same class
 * as `ShareOutPreviewError`):** `ShareOutApiImpl`'s status-code mapper folds 403 and 409 into the ONE
 * [NetworkError.UNKNOWN] bucket and 400 into [NetworkError.BAD_REQUEST]; [NetworkError] carries no
 * parseable sub-code to distinguish "forbidden" from "already executed" from generic validation.
 * [toShareOutExecuteError] therefore maps all of them to [Server]; the finer `error_forbidden` /
 * `error_shareout_already_done` message strings (`data-flow.yaml#error_paths`) are shipped in
 * `strings.xml` ready for the moment `NetworkError` is enriched with a sub-code (a one-line change).
 */
@Serializable
sealed interface ShareOutError {
    val retry: Boolean
    val messageKey: String

    @Serializable
    data object Network : ShareOutError {
        override val retry: Boolean = false
        override val messageKey: String = "error_queued_offline"
    }

    @Serializable
    data object Server : ShareOutError {
        override val retry: Boolean = true
        override val messageKey: String = "error_server"
    }

    @Serializable
    data object PartialFailure : ShareOutError {
        override val retry: Boolean = true
        override val messageKey: String = "error_partial"
    }

    @Serializable
    data object Auth : ShareOutError {
        override val retry: Boolean = false
        override val messageKey: String = "error_auth"
    }

    @Serializable
    data object ConfirmationRequired : ShareOutError {
        override val retry: Boolean = false
        override val messageKey: String = "error_confirmation_required"
    }
}

/**
 * MVI state for `ShareOutExecuteViewModel`. Field set + defaults are a verbatim mirror of
 * `ui.yaml#state_model.ShareOutExecuteViewModel.state.fields` (plus [isBiometricAvailable] seeded by
 * the Screen from `BiometricAuthenticator.isAvailable()` — `data-flow.yaml` OnBiometricSelected
 * `local_sources`). [typeConfig], [memberPayouts], [failedPayouts] and [error] are `@Transient` —
 * non-serializable domain payloads always re-derived from the nav-args on (re)construction, never
 * restored from a snapshot (same convention as `ShareOutPreviewState`).
 */
@Serializable
@Immutable
data class ShareOutExecuteState(
    val groupId: String = "",
    @Transient
    val typeConfig: GroupTypeConfig? = null,
    val poolModel: String = "",
    val shareoutFormula: String = "",
    val cycleNumber: Int = DEFAULT_CYCLE_NUMBER,
    val confirmationText: String = "",
    val isConfirmed: Boolean = false,
    val isExecuting: Boolean = false,
    val executedCount: Int = 0,
    val totalCount: Int = 0,
    @Transient
    val memberPayouts: List<MemberPayout> = emptyList(),
    val memberExecutionStatus: Map<String, MemberExecutionStatus> = emptyMap(),
    val totalPool: Double = 0.0,
    val isOnline: Boolean = true,
    val isBiometricAvailable: Boolean = false,
    val isCompleted: Boolean = false,
    val queuedOffline: Boolean = false,
    @Transient
    val error: ShareOutError? = null,
    @Transient
    val failedPayouts: List<MemberPayout> = emptyList(),
    val succeededCount: Int = 0,
) {
    /** `true` when the pool model is the ROSCA/chit rotation strategy (single-recipient card path). */
    val isRotation: Boolean get() = poolModel == ShareOutPreview.POOL_MODEL_ROTATING_PAYOUT

    /** `true` when the execute button / screen title should render the "Rotation Payout" label variant. */
    val isRotationLabel: Boolean get() = shareoutFormula in ROTATION_FORMULAS || isRotation
}

/**
 * Derived, single-source-of-truth screen state — see [ShareOutExecuteScreenState] KDoc.
 * [PartialFailure] is only reached after a completed ACCUMULATING execution that left ≥1 failed
 * payout; [Success] covers both a clean ACCUMULATING completion and any rotation/offline completion.
 */
fun ShareOutExecuteState.deriveScreenState(): ShareOutExecuteScreenState = when {
    isExecuting -> ShareOutExecuteScreenState.Executing
    isCompleted && failedPayouts.isNotEmpty() -> ShareOutExecuteScreenState.PartialFailure
    isCompleted || queuedOffline -> ShareOutExecuteScreenState.Success
    error != null && error != ShareOutError.ConfirmationRequired -> ShareOutExecuteScreenState.Error
    else -> ShareOutExecuteScreenState.Content
}

/**
 * One-shot side effects — verbatim mirror of
 * `ui.yaml#state_model.ShareOutExecuteViewModel.events.members` (4 members). See API.md#events.
 */
sealed interface ShareOutExecuteEvent {
    data class NavigateToGroupDashboard(val groupId: String) : ShareOutExecuteEvent
    data object NavigateBack : ShareOutExecuteEvent
    data object ShowBiometricPrompt : ShareOutExecuteEvent
    data class ShowSnackbar(val message: String) : ShareOutExecuteEvent
}

/** Outcome of the platform biometric prompt, resolved by the Screen and routed back via [ShareOutExecuteAction.Internal.BiometricResult]. */
enum class BiometricOutcome { SUCCESS, FAILED, UNAVAILABLE }

/**
 * User intents — the 6 top-level members are a verbatim mirror of
 * `ui.yaml#state_model.ShareOutExecuteViewModel.actions.members` (RULE-IMPL-DEAD-CLICKABLE-001
 * Rule 1). [Internal] is the sanctioned async-result-routing sub-interface (never a user intent) —
 * biometric result, connectivity change, and the two companion-call results.
 */
sealed interface ShareOutExecuteAction {
    data class OnConfirmationTextChanged(val text: String) : ShareOutExecuteAction
    data object OnBiometricSelected : ShareOutExecuteAction
    data object OnExecute : ShareOutExecuteAction
    data object OnRetryFailed : ShareOutExecuteAction
    data object OnBack : ShareOutExecuteAction
    data object OnDone : ShareOutExecuteAction

    /** Async coroutine results — routed via `trySendAction`, never dispatched by the UI. */
    sealed interface Internal : ShareOutExecuteAction {
        data class ConnectivityChanged(val online: Boolean) : Internal
        data class BiometricResult(val outcome: BiometricOutcome) : Internal
        data class ShareOutExecuted(val result: NetworkResult<ShareOutExecuteResult, NetworkError>, val isRetry: Boolean) : Internal
        data class RotationExecuted(val result: NetworkResult<RotationPayoutExecuteResult, NetworkError>) : Internal
    }
}

/**
 * MVI processor for the irreversible strategy-adaptive share-out execution. On mount it derives
 * `poolModel`/`shareoutFormula` from the nav-arg [typeConfig], seeds every payout to
 * [MemberExecutionStatus.PENDING], and reactively tracks connectivity via [networkMonitor]. The
 * double-confirmation gate ([isConfirmed]) is satisfied by typing the exact phrase "SHARE OUT" OR by
 * a successful biometric prompt (routed back from the Screen). `OnExecute` branches on `poolModel`:
 * ACCUMULATING → COMP-DIST-001 (`executeShareOut`, per-member status stream + partial-failure/retry);
 * ROTATING_PAYOUT → COMP-DIST-002 (`executeRotationPayout`, single atomic recipient). When offline
 * the whole request is enqueued to the HIGH-priority `sync_queue` instead
 * (`data-flow.yaml#offline_behavior`). [analytics]/[crashReporter] are mandatory here (this is a
 * financial mutation, outside `{crud, nav_only}`) per RULE-IDEA-IMPL-INTELLIGENCE-001.
 *
 * **Rotation-recipient gap (CFF1, same class as [DEFAULT_CYCLE_NUMBER]):** for ROTATING_PAYOUT the
 * preview hands off an EMPTY `memberPayouts` (see `share-out-preview` `NavigateToShareOutExecute`),
 * so the recipient identity the COMP-DIST-002 body needs is not threaded through. [buildRotationRequest]
 * uses `memberPayouts.firstOrNull()` when present and an empty `recipientMemberId` sentinel otherwise
 * (a deliberately invalid id so a real backend 400s loudly rather than paying the wrong member) —
 * flagged for the preview→execute handoff enrichment (thread `recipientMemberId` + `shareoutFormula`
 * + `cycleNumber` into the nav event). The ACCUMULATING path (the demo/device-verify surface) carries
 * its full `memberPayouts[]` and is unaffected.
 *
 * See API.md#viewmodel.
 */
internal class ShareOutExecuteViewModel(
    private val repository: ShareOutRepository,
    private val networkMonitor: NetworkMonitor,
    private val sessionManager: SessionManager,
    private val crashReporter: CrashReporter,
    private val analytics: KptAnalyticsTracker,
    private val groupId: String,
    private val typeConfig: GroupTypeConfig,
    private val totalPool: Double,
    private val memberPayouts: List<MemberPayout>,
) : BaseViewModel<ShareOutExecuteState, ShareOutExecuteEvent, ShareOutExecuteAction>(
    initialState = run {
        val poolModel = typeConfig.toPoolModel()
        ShareOutExecuteState(
            groupId = groupId,
            typeConfig = typeConfig,
            poolModel = poolModel,
            shareoutFormula = typeConfig.toDefaultShareoutFormula(poolModel),
            totalPool = totalPool,
            memberPayouts = memberPayouts,
            totalCount = memberPayouts.size,
            memberExecutionStatus = memberPayouts.associate { it.memberId to MemberExecutionStatus.PENDING },
            isOnline = true,
        )
    },
) {

    private var executeJob: Job? = null

    init {
        crashReporter.recordMessage(
            message = "feature=share-out-execute screen=share-out-execute-screen groupId=$groupId " +
                "poolModel=${state.poolModel} payouts=${memberPayouts.size} pool=$totalPool",
            level = CrashSeverity.Debug,
        )
        analytics.trackGroupOperation(operation = "view_shareout_execute", groupId = groupId)
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                trySendAction(ShareOutExecuteAction.Internal.ConnectivityChanged(online))
            }
        }
    }

    override fun handleAction(action: ShareOutExecuteAction) {
        when (action) {
            is ShareOutExecuteAction.OnConfirmationTextChanged -> handleConfirmationTextChanged(action.text)
            ShareOutExecuteAction.OnBiometricSelected -> handleBiometricSelected()
            ShareOutExecuteAction.OnExecute -> handleExecute(isRetry = false)
            ShareOutExecuteAction.OnRetryFailed -> handleExecute(isRetry = true)
            ShareOutExecuteAction.OnBack -> handleBack()
            ShareOutExecuteAction.OnDone -> sendEvent(ShareOutExecuteEvent.NavigateToGroupDashboard(groupId))
            is ShareOutExecuteAction.Internal.ConnectivityChanged -> updateState { copy(isOnline = action.online) }
            is ShareOutExecuteAction.Internal.BiometricResult -> handleBiometricResult(action.outcome)
            is ShareOutExecuteAction.Internal.ShareOutExecuted -> handleShareOutExecuted(action.result, action.isRetry)
            is ShareOutExecuteAction.Internal.RotationExecuted -> handleRotationExecuted(action.result)
        }
    }

    // -- Confirmation gate (ui.yaml effect: transform_state) ----------------------------------------

    private fun handleConfirmationTextChanged(text: String) {
        val confirmed = text.trim().uppercase() == CONFIRMATION_PHRASE
        updateState {
            copy(
                confirmationText = text,
                isConfirmed = confirmed,
                error = if (error == ShareOutError.ConfirmationRequired) null else error,
            )
        }
    }

    private fun handleBiometricSelected() {
        Logger.i(TAG) { "OnBiometricSelected groupId=$groupId available=${state.isBiometricAvailable}" }
        sendEvent(ShareOutExecuteEvent.ShowBiometricPrompt)
    }

    private fun handleBiometricResult(outcome: BiometricOutcome) {
        when (outcome) {
            BiometricOutcome.SUCCESS -> {
                Logger.i(TAG) { "biometric confirmation succeeded groupId=$groupId" }
                updateState { copy(isConfirmed = true, error = null) }
            }
            BiometricOutcome.UNAVAILABLE -> sendEvent(ShareOutExecuteEvent.ShowSnackbar("error_biometric_unavailable"))
            BiometricOutcome.FAILED -> sendEvent(ShareOutExecuteEvent.ShowSnackbar("error_biometric_failed"))
        }
    }

    // -- Execute (ui.yaml effect: call_api) ---------------------------------------------------------

    private fun handleExecute(isRetry: Boolean) {
        val current = state
        if (!current.isConfirmed) {
            Logger.w(TAG) { "OnExecute blocked — not confirmed groupId=$groupId" }
            updateState { copy(error = ShareOutError.ConfirmationRequired) }
            sendEvent(ShareOutExecuteEvent.ShowSnackbar(ShareOutError.ConfirmationRequired.messageKey))
            return
        }

        val payoutsToExecute = if (isRetry) current.failedPayouts else current.memberPayouts
        if (isRetry && payoutsToExecute.isEmpty()) {
            Logger.w(TAG) { "OnRetryFailed with no failed payouts — ignored groupId=$groupId" }
            return
        }

        executeJob?.cancel()
        executeJob = viewModelScope.launch {
            markInProgress(payoutsToExecute)
            analytics.trackGroupOperation(operation = if (isRetry) "retry_shareout_execute" else "shareout_execute", groupId = groupId)

            if (!networkMonitor.isOnline.value) {
                enqueueOffline(current)
                return@launch
            }

            if (current.isRotation) {
                val result = repository.executeRotationPayout(groupId, buildRotationRequest(current))
                trySendAction(ShareOutExecuteAction.Internal.RotationExecuted(result))
            } else {
                val request = buildShareOutRequest(current, payoutsToExecute)
                val result = repository.executeShareOut(groupId, request)
                trySendAction(ShareOutExecuteAction.Internal.ShareOutExecuted(result, isRetry))
            }
        }
    }

    private fun markInProgress(payouts: List<MemberPayout>) {
        val ids = payouts.map { it.memberId }.toSet()
        updateState {
            copy(
                isExecuting = true,
                error = null,
                memberExecutionStatus = memberExecutionStatus.toMutableMap().apply {
                    ids.forEach { put(it, MemberExecutionStatus.IN_PROGRESS) }
                },
            )
        }
    }

    private suspend fun enqueueOffline(current: ShareOutExecuteState) {
        Logger.w(TAG) { "execute attempted while offline — enqueue-offline fallback groupId=$groupId" }
        crashReporter.recordMessage(
            message = "share-out-execute: execute attempted while offline groupId=$groupId poolModel=${current.poolModel}",
            level = CrashSeverity.Info,
        )
        val queuedId = if (current.isRotation) {
            repository.enqueueRotationPayoutOffline(groupId, buildRotationRequest(current))
        } else {
            repository.enqueueShareOutExecuteOffline(groupId, buildShareOutRequest(current, current.memberPayouts))
        }
        Logger.i(TAG) { "share-out-execute enqueued offline id=$queuedId groupId=$groupId" }
        updateState {
            copy(
                isExecuting = false,
                queuedOffline = true,
                error = null,
                memberExecutionStatus = memberExecutionStatus.mapValues { MemberExecutionStatus.QUEUED },
            )
        }
    }

    // -- Async result routing -----------------------------------------------------------------------

    private fun handleShareOutExecuted(result: NetworkResult<ShareOutExecuteResult, NetworkError>, isRetry: Boolean) {
        when (result) {
            is NetworkResult.Success -> applyShareOutSuccess(result.data)
            is NetworkResult.Error -> applyExecuteError(result.error)
        }
    }

    private fun applyShareOutSuccess(executed: ShareOutExecuteResult) {
        val failedIds = executed.failedMemberIds.toSet()
        val current = state
        val failed = current.memberPayouts.filter { it.memberId in failedIds }
        val statuses = current.memberExecutionStatus.toMutableMap()
        current.memberPayouts.forEach { payout ->
            statuses[payout.memberId] =
                if (payout.memberId in failedIds) MemberExecutionStatus.FAILED else MemberExecutionStatus.DONE
        }
        Logger.i(TAG) {
            "share-out executed groupId=$groupId recordId=${executed.shareoutRecordId} " +
                "succeeded=${executed.succeededCount} failed=${executed.failedCount}"
        }
        analytics.trackGroupOperation(operation = "shareout_execute_done", groupId = groupId)
        updateState {
            copy(
                isExecuting = false,
                isCompleted = true,
                executedCount = totalCount,
                succeededCount = executed.succeededCount,
                failedPayouts = failed,
                memberExecutionStatus = statuses,
                error = if (failed.isEmpty()) null else ShareOutError.PartialFailure,
            )
        }
    }

    private fun handleRotationExecuted(result: NetworkResult<RotationPayoutExecuteResult, NetworkError>) {
        when (result) {
            is NetworkResult.Success -> {
                Logger.i(TAG) { "rotation executed groupId=$groupId recordId=${result.data.rotationRecordId}" }
                analytics.trackGroupOperation(operation = "rotation_execute_done", groupId = groupId)
                updateState {
                    copy(
                        isExecuting = false,
                        isCompleted = true,
                        executedCount = 1,
                        succeededCount = 1,
                        failedPayouts = emptyList(),
                        error = null,
                    )
                }
            }
            is NetworkResult.Error -> applyExecuteError(result.error)
        }
    }

    private fun applyExecuteError(networkError: NetworkError) {
        // A transport timeout (offline / lost connectivity mid-call) is enqueued for later drain
        // (data-flow.yaml#error_paths: network.offline -> enqueue_sync) rather than surfaced as a
        // hard failure, mirroring the proactive offline path.
        if (networkError == NetworkError.REQUEST_TIMEOUT) {
            Logger.w(TAG) { "execute lost connectivity mid-call — enqueue-offline groupId=$groupId" }
            executeJob = viewModelScope.launch { enqueueOffline(state) }
            return
        }
        val error = networkError.toShareOutExecuteError()
        if (error == ShareOutError.Auth) {
            crashReporter.recordMessage(
                message = "share-out-execute: session expired (401) groupId=$groupId — clearing session",
                level = CrashSeverity.Warning,
            )
            sessionManager.endSession()
        } else {
            crashReporter.recordException(
                throwable = IllegalStateException("share-out-execute failed: $networkError"),
                message = "share-out-execute: execute failed groupId=$groupId networkError=$networkError",
            )
        }
        // Reset the in-flight rows back to PENDING so the re-enabled Execute button retries cleanly.
        updateState {
            copy(
                isExecuting = false,
                error = error,
                memberExecutionStatus = memberExecutionStatus.mapValues {
                    if (it.value == MemberExecutionStatus.IN_PROGRESS) MemberExecutionStatus.PENDING else it.value
                },
            )
        }
        sendEvent(ShareOutExecuteEvent.ShowSnackbar(error.messageKey))
    }

    // -- Back (execution-state guard) ---------------------------------------------------------------

    private fun handleBack() {
        val current = state
        if (current.isExecuting || current.isCompleted) {
            Logger.w(TAG) { "OnBack swallowed — execution in progress/completed groupId=$groupId" }
            return
        }
        sendEvent(ShareOutExecuteEvent.NavigateBack)
    }

    // -- Request builders ---------------------------------------------------------------------------

    @OptIn(ExperimentalTime::class)
    private fun buildShareOutRequest(current: ShareOutExecuteState, payouts: List<MemberPayout>): ShareOutExecuteRequest =
        ShareOutExecuteRequest(
            cycleNumber = current.cycleNumber,
            totalPool = current.totalPool,
            memberPayouts = payouts,
            shareoutFormula = current.shareoutFormula,
            executedAt = Clock.System.now().toString(),
        )

    @OptIn(ExperimentalTime::class)
    private fun buildRotationRequest(current: ShareOutExecuteState): RotationPayoutRequest = RotationPayoutRequest(
        // Rotation-recipient gap (CFF1) — see class KDoc. Empty id sentinel when the preview handoff
        // carried no payout (a real backend 400s loudly rather than paying the wrong member).
        recipientMemberId = current.memberPayouts.firstOrNull()?.memberId.orEmpty(),
        amount = current.totalPool,
        payoutOrderMethod = current.shareoutFormula,
        executedAt = Clock.System.now().toString(),
    )
}

// ---------------------------------------------------------------------------------------------
// Top-level helpers (pure — independently unit-testable)
// ---------------------------------------------------------------------------------------------

/**
 * Derives the pool-model discriminator from the nav-arg [GroupTypeConfig.savingsMechanism]. The
 * authoritative preview response is not re-fetched on this screen (execute is a pure write), so the
 * strategy branch is driven off the same `typeConfig` the preview forwarded.
 */
internal fun GroupTypeConfig.toPoolModel(): String =
    if (savingsMechanism == SavingsMechanism.ROTATING_PAYOUT) {
        ShareOutPreview.POOL_MODEL_ROTATING_PAYOUT
    } else {
        ShareOutPreview.POOL_MODEL_ACCUMULATING
    }

/**
 * Best-effort default `shareoutFormula` for the execute-request audit field + the label branch —
 * see [DEFAULT_CYCLE_NUMBER] KDoc for why this is a documented default (the preview holds the real
 * formula but does not forward it). ROTATING_PAYOUT defaults to `FIXED_ORDER`, ACCUMULATING to
 * `PRORATA_SHARES`. The rendered label branch keys off `poolModel` regardless, so it stays correct.
 */
internal fun GroupTypeConfig.toDefaultShareoutFormula(poolModel: String): String =
    if (poolModel == ShareOutPreview.POOL_MODEL_ROTATING_PAYOUT) "FIXED_ORDER" else "PRORATA_SHARES"

/**
 * Maps the transport-level [NetworkError] onto the `ui.yaml#errors.types` taxonomy. 401/429 ->
 * [ShareOutError.Auth] (session cleared); everything else (400 validation, 403 forbidden, 409
 * already-executed, 5xx, serialization, unknown) -> [ShareOutError.Server]. `REQUEST_TIMEOUT` is
 * handled BEFORE this mapper (enqueued offline), so it never reaches here.
 */
internal fun NetworkError.toShareOutExecuteError(): ShareOutError = when (this) {
    NetworkError.UNAUTHORIZED, NetworkError.TOO_MANY_REQUESTS -> ShareOutError.Auth
    NetworkError.REQUEST_TIMEOUT,
    NetworkError.BAD_REQUEST,
    NetworkError.NOT_FOUND,
    NetworkError.SERVER,
    NetworkError.SERIALIZATION,
    NetworkError.UNKNOWN,
    -> ShareOutError.Server
}
