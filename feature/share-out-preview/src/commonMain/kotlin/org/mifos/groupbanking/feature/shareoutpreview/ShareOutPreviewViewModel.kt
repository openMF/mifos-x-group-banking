/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.shareoutpreview

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitor
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
import org.mifos.groupbanking.core.model.MemberPayout
import org.mifos.groupbanking.core.model.ShareOutPreview

/**
 * MVI stack (State/ScreenState/Error/Event/Action/ViewModel) for the `share-out-preview` feature —
 * see API.md#viewmodel / #state / #actions / #events for the full generated-symbol contract.
 */
private const val TAG = "ShareOutPreviewViewModel"

/**
 * Strongly-typed error taxonomy for `share-out-preview` — verbatim mirror of
 * `ui.yaml#state_model.ShareOutPreviewViewModel.errors.types` (4 members). [retry] mirrors the
 * per-type `retry:` flag; [messageKey] the `message_key:` — both drive the error state's retry-CTA
 * visibility + message. Kept a plain sealed interface (not `@Serializable`) so the enclosing state
 * carries it as a `@Transient` field, same convention `SavingsDashboardState.error` uses for its
 * flat key.
 *
 * [InsufficientData] is produced ONLY by the client-side `OnConfirm` validation
 * (`flow.yaml#validation_rules`), never by a transport [NetworkError] — the other three map from
 * network failures in [ShareOutPreviewViewModel.toShareOutPreviewError].
 */
sealed interface ShareOutPreviewError {
    val retry: Boolean
    val messageKey: String

    data object Network : ShareOutPreviewError {
        override val retry: Boolean = true
        override val messageKey: String = "error_network"
    }

    data object Server : ShareOutPreviewError {
        override val retry: Boolean = true
        override val messageKey: String = "error_server"
    }

    data object InsufficientData : ShareOutPreviewError {
        override val retry: Boolean = false
        override val messageKey: String = "error_insufficient_data"
    }

    data object Auth : ShareOutPreviewError {
        override val retry: Boolean = false
        override val messageKey: String = "error_auth"
    }
}

/**
 * Screen-level render state for `share-out-preview-screen` — verbatim mirror of
 * `ui.yaml#state_model.ShareOutPreviewViewModel.screen_state.members` (4 members). Derived only
 * (not stored) via [ShareOutPreviewState.deriveScreenState]. [ContentAccumulating] and
 * [ContentRotating] are selected by `poolModel`; [Error] only when the load failed with no prior
 * content to fall back to.
 */
@Serializable
sealed interface ShareOutPreviewScreenState {
    @Serializable
    data object Loading : ShareOutPreviewScreenState

    @Serializable
    data object ContentAccumulating : ShareOutPreviewScreenState

    @Serializable
    data object ContentRotating : ShareOutPreviewScreenState

    @Serializable
    data object Error : ShareOutPreviewScreenState
}

/**
 * MVI state for `ShareOutPreviewViewModel`. Field set + defaults are a verbatim mirror of
 * `ui.yaml#state_model.ShareOutPreviewViewModel.state.fields`.
 *
 * [poolModel]/[shareoutFormula] are `String` (per `api.yaml#response`) — the authoritative values
 * arrive from the companion preview response ([applyPreview]) and START BLANK: a blank [poolModel]
 * is the "no content loaded yet" signal [deriveScreenState] keys the Error state on, so seeding it
 * from the nav-param `typeConfig` (which would never be blank) is deliberately avoided — the
 * strategy chip is driven by the response `shareoutFormula` regardless. [typeConfig], [memberPayouts]
 * and [error] are `@Transient` — always re-derived from [ShareOutRepository] (or the nav-param) on
 * (re)construction, never restored from a `@Serializable` snapshot, same convention every other
 * detail ViewModel uses for non-serializable domain payloads.
 */
@Serializable
@Immutable
data class ShareOutPreviewState(
    val isLoading: Boolean = true,
    @Transient
    val typeConfig: GroupTypeConfig? = null,
    val shareoutFormula: String = "",
    val poolModel: String = "",
    val totalCorpus: Double = 0.0,
    val totalProfit: Double = 0.0,
    val totalPool: Double = 0.0,
    @Transient
    val memberPayouts: List<MemberPayout> = emptyList(),
    val rotationPosition: Int? = null,
    val nextRecipientName: String? = null,
    val nextRecipientAmount: Double? = null,
    @Transient
    val error: ShareOutPreviewError? = null,
    val cycleNumber: Int = 0,
    val groupId: String = "",
    val isRefreshing: Boolean = false,
)

/**
 * Derived, single-source-of-truth screen state — see [ShareOutPreviewScreenState] KDoc. [Error] is
 * only reached when the load failed AND no content has been rendered yet (no pool selected);
 * otherwise the pool-model discriminator picks the accumulating table or the rotating card.
 */
fun ShareOutPreviewState.deriveScreenState(): ShareOutPreviewScreenState = when {
    isLoading -> ShareOutPreviewScreenState.Loading
    error != null && poolModel.isBlank() -> ShareOutPreviewScreenState.Error
    poolModel == ShareOutPreview.POOL_MODEL_ROTATING_PAYOUT -> ShareOutPreviewScreenState.ContentRotating
    else -> ShareOutPreviewScreenState.ContentAccumulating
}

/**
 * One-shot side effects — verbatim mirror of
 * `ui.yaml#state_model.ShareOutPreviewViewModel.events.members` (3 members). See API.md#events.
 */
sealed interface ShareOutPreviewEvent {
    data class NavigateToShareOutExecute(
        val groupId: String,
        val typeConfig: GroupTypeConfig,
        val totalPool: Double,
        val memberPayouts: List<MemberPayout>,
        val cycleNumber: Int,
    ) : ShareOutPreviewEvent

    data object NavigateBack : ShareOutPreviewEvent

    data class ShowSnackbar(val message: String) : ShareOutPreviewEvent
}

/**
 * User intents — the 4 top-level members are a verbatim mirror of
 * `ui.yaml#state_model.ShareOutPreviewViewModel.actions.members` (RULE-IMPL-DEAD-CLICKABLE-001
 * Rule 1). [Internal] is the sanctioned async-result-routing sub-interface (never a user intent).
 * The on-mount load is fired from `init` (there is no matching declared user action for it — same
 * `init { loadInitial() }` convention `MemberSavingsDetailViewModel` uses).
 */
sealed interface ShareOutPreviewAction {
    data object OnConfirm : ShareOutPreviewAction
    data object OnBack : ShareOutPreviewAction
    data object Retry : ShareOutPreviewAction
    data object OnRefresh : ShareOutPreviewAction

    /** Async coroutine result — routed via `trySendAction`, never dispatched by the UI. */
    sealed interface Internal : ShareOutPreviewAction {
        data class PreviewLoaded(val result: NetworkResult<ShareOutPreview, NetworkError>) : Internal
    }
}

/**
 * MVI processor for the strategy-adaptive share-out distribution preview. Loads the companion
 * preview (`GET /companion/groups/{groupId}/shareout/preview`, COMP-DIST-001) on mount, routes the
 * ACCUMULATING vs ROTATING_PAYOUT rendering by the response `poolModel`, and — on confirm — forwards
 * the previewed plan to `share-out-execute` after the client-side `flow.yaml#validation_rules`
 * checks pass. No API call or persistence happens on confirm; the actual execution is on the next
 * screen (`ui.yaml#components.confirm_button.on_click.action_contract`).
 *
 * [repository] is consumed directly via [NetworkResult] — [ShareOutRepository]'s own "Store5 branch"
 * note (no `AppStoreRegistry` entry yet), same branch as `SavingsDashboardViewModel`. The on-mount,
 * pull-to-refresh, and error-retry reads all go through the SAME [ShareOutRepository.getShareOutPreview]
 * call. [sessionManager] is injected to call `endSession()` on the 401 branch, and the
 * analytics/crashReporter pair is mandatory here (`business_logic` is strategy-adaptive, outside
 * `{crud, nav_only}`) per RULE-IDEA-IMPL-INTELLIGENCE-001.
 *
 * [groupId]/[typeConfig] are the `ui.yaml#nav_params` values forwarded from `group-dashboard`'s
 * `OnShareOut` entry point via Koin `parametersOf(...)`. Because that seam supplies only `groupId`
 * today (the catalogue [GroupTypeConfig] is not held there — same drift `SavingsDashboardRoute`
 * documents), [typeConfig] may arrive with default/UNKNOWN fields; the authoritative `poolModel`/
 * `shareoutFormula` always come from the companion response, so rendering is correct regardless.
 *
 * See API.md#viewmodel.
 */
internal class ShareOutPreviewViewModel(
    private val repository: ShareOutRepository,
    private val networkMonitor: NetworkMonitor,
    private val sessionManager: SessionManager,
    private val crashReporter: CrashReporter,
    private val analytics: KptAnalyticsTracker,
    private val groupId: String,
    private val typeConfig: GroupTypeConfig,
) : BaseViewModel<ShareOutPreviewState, ShareOutPreviewEvent, ShareOutPreviewAction>(
    initialState = ShareOutPreviewState(
        groupId = groupId,
        typeConfig = typeConfig,
    ),
) {

    init {
        crashReporter.recordMessage(
            message = "feature=share-out-preview screen=share-out-preview-screen " +
                "groupId=$groupId savingsMechanism=${typeConfig.savingsMechanism}",
            level = CrashSeverity.Debug,
        )
        analytics.trackGroupOperation(operation = "view_shareout_preview", groupId = groupId)
        loadPreview(isRefresh = false)
    }

    override fun handleAction(action: ShareOutPreviewAction) {
        when (action) {
            ShareOutPreviewAction.OnConfirm -> handleConfirm()
            ShareOutPreviewAction.OnBack -> sendEvent(ShareOutPreviewEvent.NavigateBack)
            ShareOutPreviewAction.Retry -> loadPreview(isRefresh = false)
            ShareOutPreviewAction.OnRefresh -> handleRefresh()
            is ShareOutPreviewAction.Internal.PreviewLoaded -> handlePreviewLoaded(action.result)
        }
    }

    // -- on_mount / retry (flow.yaml on_load — GET companion shareout preview) --------------------

    private fun loadPreview(isRefresh: Boolean) {
        Logger.i(TAG) { "loadPreview dispatched groupId=$groupId isRefresh=$isRefresh" }
        updateState { copy(isLoading = !isRefresh, isRefreshing = isRefresh, error = null) }
        viewModelScope.launch {
            val result = repository.getShareOutPreview(groupId)
            trySendAction(ShareOutPreviewAction.Internal.PreviewLoaded(result))
        }
    }

    // -- Pull-to-refresh / top-bar refresh (call_api, invalidate cache; connectivity-gated) --------

    private fun handleRefresh() {
        if (!networkMonitor.isOnline.value) {
            Logger.w(TAG) { "refresh attempted while offline groupId=$groupId" }
            crashReporter.recordMessage(
                message = "share-out-preview: refresh attempted while offline groupId=$groupId",
                level = CrashSeverity.Info,
            )
            updateState { copy(isRefreshing = false) }
            sendEvent(ShareOutPreviewEvent.ShowSnackbar(message = ShareOutPreviewError.Network.messageKey))
            return
        }
        analytics.trackGroupOperation(operation = "refresh_shareout_preview", groupId = groupId)
        loadPreview(isRefresh = true)
    }

    // -- Load result routing -----------------------------------------------------------------------

    private fun handlePreviewLoaded(result: NetworkResult<ShareOutPreview, NetworkError>) {
        when (result) {
            is NetworkResult.Success -> applyPreview(result.data)
            is NetworkResult.Error -> applyError(result.error)
        }
    }

    private fun applyPreview(preview: ShareOutPreview) {
        Logger.i(TAG) {
            "preview loaded groupId=$groupId poolModel=${preview.poolModel} formula=${preview.shareoutFormula} " +
                "pool=${preview.totalPool} payouts=${preview.memberPayouts?.size ?: "n/a"}"
        }
        updateState {
            copy(
                isLoading = false,
                isRefreshing = false,
                error = null,
                cycleNumber = preview.cycleNumber,
                poolModel = preview.poolModel,
                shareoutFormula = preview.shareoutFormula,
                totalCorpus = preview.totalCorpus,
                totalProfit = preview.totalProfit,
                totalPool = preview.totalPool,
                memberPayouts = preview.memberPayouts.orEmpty(),
                rotationPosition = preview.rotationPosition,
                nextRecipientName = preview.nextRecipientName,
                nextRecipientAmount = preview.nextRecipientAmount,
            )
        }
    }

    private fun applyError(networkError: NetworkError) {
        val error = networkError.toShareOutPreviewError()
        if (error == ShareOutPreviewError.Auth) {
            crashReporter.recordMessage(
                message = "share-out-preview: session expired (401) groupId=$groupId — clearing session",
                level = CrashSeverity.Warning,
            )
            sessionManager.endSession()
        } else {
            crashReporter.recordException(
                throwable = IllegalStateException("share-out-preview fetch failed: $networkError"),
                message = "share-out-preview: load failed groupId=$groupId networkError=$networkError",
            )
        }
        updateState { copy(isLoading = false, isRefreshing = false, error = error) }
        sendEvent(ShareOutPreviewEvent.ShowSnackbar(message = error.messageKey))
    }

    // -- Confirm (navigate — after flow.yaml#validation_rules) -------------------------------------

    private fun handleConfirm() {
        val current = state
        val validationError = current.confirmValidationError()
        if (validationError != null) {
            Logger.w(TAG) { "confirm blocked by validation groupId=$groupId poolModel=${current.poolModel}" }
            updateState { copy(error = validationError) }
            sendEvent(ShareOutPreviewEvent.ShowSnackbar(message = validationError.messageKey))
            return
        }
        Logger.i(TAG) { "confirm accepted groupId=$groupId poolModel=${current.poolModel} pool=${current.totalPool}" }
        analytics.trackGroupOperation(operation = "confirm_shareout_preview", groupId = groupId)
        sendEvent(
            ShareOutPreviewEvent.NavigateToShareOutExecute(
                groupId = groupId,
                typeConfig = typeConfig,
                totalPool = current.totalPool,
                memberPayouts = current.memberPayouts,
                cycleNumber = current.cycleNumber,
            ),
        )
    }

    /**
     * `flow.yaml#validation_rules.on_confirm` — ACCUMULATING with an empty payout table, or
     * ROTATING_PAYOUT with no next recipient, both surface [ShareOutPreviewError.InsufficientData].
     * Returns `null` when the plan is confirmable.
     */
    private fun ShareOutPreviewState.confirmValidationError(): ShareOutPreviewError? = when {
        poolModel == ShareOutPreview.POOL_MODEL_ACCUMULATING && memberPayouts.isEmpty() ->
            ShareOutPreviewError.InsufficientData
        poolModel == ShareOutPreview.POOL_MODEL_ROTATING_PAYOUT && nextRecipientName == null ->
            ShareOutPreviewError.InsufficientData
        else -> null
    }

    /**
     * Maps the transport-level [NetworkError] onto the `ui.yaml#errors.types` taxonomy. 401/429 ->
     * [ShareOutPreviewError.Auth] (session cleared); transport timeout (offline) ->
     * [ShareOutPreviewError.Network]; everything else (404 group-not-found, 409 already-executed,
     * 5xx, serialization, unknown) -> [ShareOutPreviewError.Server].
     */
    private fun NetworkError.toShareOutPreviewError(): ShareOutPreviewError = when (this) {
        NetworkError.UNAUTHORIZED, NetworkError.TOO_MANY_REQUESTS -> ShareOutPreviewError.Auth
        NetworkError.REQUEST_TIMEOUT -> ShareOutPreviewError.Network
        NetworkError.NOT_FOUND,
        NetworkError.BAD_REQUEST,
        NetworkError.SERVER,
        NetworkError.SERIALIZATION,
        NetworkError.UNKNOWN,
        -> ShareOutPreviewError.Server
    }
}
