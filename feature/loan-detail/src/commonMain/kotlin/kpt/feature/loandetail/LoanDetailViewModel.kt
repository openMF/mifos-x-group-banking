/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.loandetail

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kpt.core.analytics.KptAnalyticsTracker
import kpt.core.base.observability.CrashReporter
import kpt.core.base.observability.CrashSeverity
import kpt.core.base.security.SessionManager
import kpt.core.base.store.error.ErrorCategory
import kpt.core.base.store.error.categorize
import kpt.core.base.store.screen.ScreenDataStream
import kpt.core.base.store.screen.ScreenState
import kpt.core.base.ui.viewmodel.BaseViewModel
import kpt.core.data.repository.LoanDetailRepository
import kpt.core.model.LoanDetail
import kpt.core.model.LoanDetailResponse
import kpt.core.model.LoanDetailTab
import kpt.core.model.RepaymentScheduleRow
import kpt.core.model.RepaymentTransaction

private const val TAG = "LoanDetailViewModel"

/**
 * Roles that may record a repayment — `flow.yaml#guard: "canRecordRepayment (treasurer role)"`.
 * ORGANIZER is included as the superset top-management role (mirrors `group-dashboard`'s
 * `MANAGEMENT_ROLES` treatment of ORGANIZER as chair-equivalent authority). Matched against the
 * `viewerRole` nav-param (forwarded from `loan-list`, originating `dt_member_role`).
 */
private val RECORD_REPAYMENT_ROLES = setOf("TREASURER", "ORGANIZER")

/**
 * Roles that may mark a loan defaulted — `flow.yaml#guard: "canMarkDefaulted (chairperson role)"`.
 * ORGANIZER included per the same superset-management rationale as [RECORD_REPAYMENT_ROLES].
 */
private val MARK_DEFAULTED_ROLES = setOf("CHAIRPERSON", "ORGANIZER")

/**
 * Screen-level render state for `loan-detail-screen` — verbatim mirror of
 * `ui.yaml#state_model.LoanDetailViewModel.screen_state` (only 3 members declared — a single loan
 * composite is never "empty" once present, same class of read as `group-dashboard`). Derived (not
 * stored) from [LoanDetailState.isLoading] / [LoanDetailState.error] via the
 * [LoanDetailState.screenState] extension below — exactly one source of truth, mirroring
 * `GroupDashboardState`'s / `LoanListState`'s identical convention
 * (`training-layer/TRAINING_MASTER.yaml#patterns.state_models`). See API.md#state.
 */
@Serializable
sealed interface LoanDetailScreenState {
    @Serializable
    data object Loading : LoanDetailScreenState

    @Serializable
    data object Content : LoanDetailScreenState

    @Serializable
    data object Error : LoanDetailScreenState
}

/**
 * Error taxonomy for the loan-detail composite read — verbatim mirror of
 * `ui.yaml#state_model.LoanDetailViewModel.errors.types`. [messageKey] is a composeResources
 * string-resource id (never a raw hardcoded English string, per RULE-IMPL-NO-HARDCODED-STRING-001)
 * resolved by the Screen layer.
 *
 * **[NotFound] reachability gap (flagged, not invented around):** `LoanDetailStore`'s
 * `LoanDetailFetchException(networkError: NetworkError)` message is `"Loan detail fetch failed:
 * $networkError"` (e.g. `"...: NOT_FOUND"`) — it carries no parseable 3-digit HTTP status code, so
 * `kpt.core.base.store.error.categorize()`'s regex-based HTTP-code extraction cannot classify it as
 * `ErrorCategory.ClientError(404)`; it falls through to `ErrorCategory.Generic`. [handleStreamUpdated]
 * still checks for `ErrorCategory.ClientError(httpCode = 404)` (future-proof — the mapping activates
 * for free if the exception message format is ever fixed upstream to embed the numeric code), but in
 * PRODUCTION TODAY a 404 (loan not found) surfaces as [Server], not [NotFound]. Mirrors
 * `GroupDashboardError.NotFound`'s identical documented gap. Reported to the caller as a
 * cross-layer follow-up rather than reached into `core/store`'s `impl` package to work around it.
 * See API.md#state.
 */
@Serializable
sealed interface LoanDetailError {
    val retry: Boolean
    val messageKey: String

    @Serializable
    data object Network : LoanDetailError {
        override val retry: Boolean = true
        override val messageKey: String = "error_network"
    }

    @Serializable
    data object Server : LoanDetailError {
        override val retry: Boolean = true
        override val messageKey: String = "error_server"
    }

    @Serializable
    data object NotFound : LoanDetailError {
        override val retry: Boolean = false
        override val messageKey: String = "error_not_found"
    }

    @Serializable
    data object Auth : LoanDetailError {
        override val retry: Boolean = false
        override val messageKey: String = "error_auth"
    }
}

/**
 * MVI state for `LoanDetailViewModel`. Field set + defaults are a verbatim mirror of
 * `ui.yaml#state_model.LoanDetailViewModel.state`. [loan], [repaymentSchedule],
 * [repaymentHistory], and [error] are `@Transient` — all are always re-derived from
 * [LoanDetailRepository.loanDetailStream] on (re)subscription (offline-first cache via the
 * `loan_detail_cache` Room table, so nothing is visually lost across process death — the Store, not
 * this transient render state, is the durable source). Mirrors `GroupDashboardState`'s /
 * `LoanListState`'s identical `@Transient` convention for non-serializable domain payloads.
 *
 * **[canRecordRepayment] / [canMarkDefaulted] — now wired from the `viewerRole` nav-param (gap
 * closed):** `ui.yaml` gates the two action buttons on `canRecordRepayment`/`canMarkDefaulted` (a
 * role concept) ANDed with a loan-status predicate evaluated separately in the Screen layer
 * (`visible_when: "canRecordRepayment && loan.status == ACTIVE"`). `ui.yaml#nav_params` for
 * `loan-detail` now declares `viewerRole` (forwarded from `loan-list`, which forwards it from
 * `group-dashboard`, originating `dt_member_role`) — matched against [RECORD_REPAYMENT_ROLES]
 * (`flow.yaml#guard: canRecordRepayment (treasurer role)`) and [MARK_DEFAULTED_ROLES]
 * (`flow.yaml#guard: canMarkDefaulted (chairperson role)`) in the constructor to seed both fields.
 * The injectable `SessionManager` still carries only session-lifecycle state — the role travels by
 * nav-param, not by session, exactly as `group-dashboard`/`loan-list` already do.
 * [handleRecordRepayment] / [handleMarkDefaulted] remain fully wired (dialog-opening events
 * dispatch correctly) so nothing is a dead clickable. See API.md#state.
 *
 * [isRecordingRepayment] is reserved for the not-yet-generated repayment-dialog submission flow
 * (`dialog: loan-repayment-dialog` in `ui.yaml`; the actual `LoanRepository.recordRepayment`
 * mutation — declared under `api.yaml#dependencies.repositories` but with no generated repository
 * method yet, per `data-flow.yaml#on_action/OnRecordRepayment` NOTE — is out of THIS screen's
 * ViewModel scope) — stays `false` here; the dialog component owns its own submit-in-flight state.
 */
@Serializable
@Immutable
data class LoanDetailState(
    val isLoading: Boolean = true,
    @Transient
    val loan: LoanDetail? = null,
    @Transient
    val repaymentSchedule: List<RepaymentScheduleRow> = emptyList(),
    @Transient
    val repaymentHistory: List<RepaymentTransaction> = emptyList(),
    val selectedTab: LoanDetailTab = LoanDetailTab.SCHEDULE,
    @Transient
    val error: LoanDetailError? = null,
    val isRecordingRepayment: Boolean = false,
    val canRecordRepayment: Boolean = false,
    val canMarkDefaulted: Boolean = false,
)

/** Derived, single-source-of-truth screen state — see [LoanDetailScreenState] KDoc. */
val LoanDetailState.screenState: LoanDetailScreenState
    get() = when {
        error != null -> LoanDetailScreenState.Error
        isLoading -> LoanDetailScreenState.Loading
        else -> LoanDetailScreenState.Content
    }

/**
 * One-shot side effects emitted by `LoanDetailViewModel` — verbatim mirror of
 * `ui.yaml#state_model.LoanDetailViewModel.events.members`. See API.md#events.
 */
sealed interface LoanDetailEvent {
    data object NavigateBack : LoanDetailEvent
    data object ShowRepaymentDialog : LoanDetailEvent
    data object ShowDefaultConfirmDialog : LoanDetailEvent
    data class ShowSnackbar(val message: String) : LoanDetailEvent
}

/**
 * User intents dispatched to `LoanDetailViewModel`. The 6 top-level members are a verbatim mirror
 * of `ui.yaml#state_model.LoanDetailViewModel.actions.members` — RULE-IMPL-DEAD-CLICKABLE-001
 * Rule 1. [Internal] is the sanctioned async-result-routing sub-interface (never a user intent) per
 * `training-layer/TRAINING_MASTER.yaml#patterns.actions` — mirrors `GroupDashboardAction.Internal`
 * / `LoanListAction.Internal`. See API.md#actions.
 */
sealed interface LoanDetailAction {
    data class OnTabChange(val tab: LoanDetailTab) : LoanDetailAction
    data object OnRecordRepayment : LoanDetailAction
    data object OnMarkDefaulted : LoanDetailAction
    data object OnBack : LoanDetailAction
    data object Retry : LoanDetailAction
    data object OnRefresh : LoanDetailAction

    /** Async stream emissions — routed via `trySendAction`, never dispatched by the UI. */
    sealed interface Internal : LoanDetailAction {
        data class StreamUpdated(val screenState: ScreenState<LoanDetailResponse>) : Internal
    }
}

/**
 * MVI processor for the loan-detail screen (`business_logic.kind: crud` per ui.yaml — a single-key
 * composite read via [LoanDetailRepository.loanDetailStream]'s offline-first [ScreenDataStream], so
 * the SP-04 AC-7 analytics/crashReporter/fieldEncryptor injection triple is NOT required per
 * RULE-IDEA-IMPL-INTELLIGENCE-001 AC-03i — but [crashReporter] and [analytics] are still wired for
 * real, feature-level observability (SC5), mirroring `LoanListViewModel`'s identical crud-but-
 * observed precedent). No `FieldEncryptor` is injected — neither `LoanDetail`,
 * `RepaymentScheduleRow`, nor `RepaymentTransaction` carries a `@PII`-marked field per the SP-02
 * idea-layer schema.
 *
 * [sessionManager] is declared in `ui.yaml#state_model.di` and handles the real
 * `ScreenState.Unauthenticated -> sessionManager.endSession()` branch in [handleStreamUpdated] —
 * mirrors `GroupDashboardViewModel`'s / `LoanListViewModel`'s identical `SessionManager` wiring.
 * `NetworkMonitor` is ALSO declared in `ui.yaml#state_model.di` but is not injected here directly —
 * it is already composed inside `LoanDetailStore`'s `asScreenStream(networkMonitor = ..., ...)`
 * wiring (via `LoanDetailRepositoryImpl`), matching every other Store-backed ViewModel's identical
 * precedent of not re-injecting it at the ViewModel layer. `ui.yaml#state_model.di` also separately
 * lists `LoanRepository` — the shipped data layer instead exposes a purpose-built
 * [LoanDetailRepository] (`loanDetailStream`, single-key composite) distinct from the paginated
 * `LoanRepository` (loan-list) — flagged as a documentation-vs-implementation drift for the caller,
 * not re-created here.
 *
 * [loanId] and [viewerRole] are the `ui.yaml#nav_params` values forwarded from `loan-list`'s "Loan
 * card tap" entry point via Koin `parametersOf(loanId, viewerRole)` (see `di.LoanDetailModule`) —
 * [loanId] scopes the single-key [LoanDetailRepository.loanDetailStream] read; [viewerRole] seeds
 * [LoanDetailState.canRecordRepayment] / [LoanDetailState.canMarkDefaulted] via the
 * [RECORD_REPAYMENT_ROLES] / [MARK_DEFAULTED_ROLES] gates (see [LoanDetailState]'s KDoc).
 *
 * See API.md#viewmodel.
 */
internal class LoanDetailViewModel(
    private val repository: LoanDetailRepository,
    private val sessionManager: SessionManager,
    private val crashReporter: CrashReporter,
    private val analytics: KptAnalyticsTracker,
    private val loanId: Long,
    viewerRole: String,
) : BaseViewModel<LoanDetailState, LoanDetailEvent, LoanDetailAction>(
    initialState = LoanDetailState(
        canRecordRepayment = viewerRole in RECORD_REPAYMENT_ROLES,
        canMarkDefaulted = viewerRole in MARK_DEFAULTED_ROLES,
    ),
) {

    /** Fixed-key offline-first stream for [loanId] — see class KDoc. */
    private val detailStream: ScreenDataStream<LoanDetailResponse> =
        repository.loanDetailStream(loanId = loanId, scope = viewModelScope)

    init {
        crashReporter.recordMessage(
            message = "feature=loan-detail screen=loan-detail-screen loanId=$loanId",
            level = CrashSeverity.Debug,
        )
        analytics.trackLoanOperation(operation = "view", loanId = loanId.toString())
        viewModelScope.launch {
            detailStream.state.collect { screenState ->
                trySendAction(LoanDetailAction.Internal.StreamUpdated(screenState))
            }
        }
    }

    override fun handleAction(action: LoanDetailAction) {
        when (action) {
            is LoanDetailAction.OnTabChange -> handleTabChange(action.tab)
            LoanDetailAction.OnRecordRepayment -> handleRecordRepayment()
            LoanDetailAction.OnMarkDefaulted -> handleMarkDefaulted()
            LoanDetailAction.OnBack -> handleBack()
            LoanDetailAction.Retry -> handleRetry()
            LoanDetailAction.OnRefresh -> handleRefresh()
            is LoanDetailAction.Internal.StreamUpdated -> handleStreamUpdated(action.screenState)
        }
    }

    // -- Tab switch (ui.yaml effect: transform_state — pure in-ViewModel, no network) --------------
    // Schedule / Repayment History data is already resident from the on_mount fetch.

    private fun handleTabChange(tab: LoanDetailTab) {
        Logger.d(TAG) { "tab changed to=$tab loanId=$loanId" }
        updateState { copy(selectedTab = tab) }
    }

    // -- Record Repayment (ui.yaml effect: call_api, library_refs: [cmp-network-monitor]) -----------
    // Opens the repayment dialog (`dialog: loan-repayment-dialog`); the actual mutation call
    // (LoanRepository.recordRepayment, guarded by cmp-network-monitor's offline gate inside the
    // dialog's own submit flow) is out of THIS screen's ViewModel scope — see LoanDetailState KDoc
    // "isRecordingRepayment" note.

    private fun handleRecordRepayment() {
        analytics.trackLoanOperation(operation = "record_repayment_dialog_open", loanId = loanId.toString())
        Logger.i(TAG) { "OnRecordRepayment tapped loanId=$loanId" }
        sendEvent(LoanDetailEvent.ShowRepaymentDialog)
    }

    // -- Mark Defaulted (ui.yaml effect: call_api, library_refs: [cmp-network-monitor]) --------------
    // Opens the mark-defaulted confirmation dialog (`dialog: loan-mark-defaulted-dialog`); the
    // actual mutation call (LoanRepository.markDefaulted, guarded by cmp-network-monitor's offline
    // gate inside the dialog's own confirm flow) is out of THIS screen's ViewModel scope — same
    // class of gap as [handleRecordRepayment], see LoanDetailState KDoc "canMarkDefaulted" note.

    private fun handleMarkDefaulted() {
        analytics.trackLoanOperation(operation = "mark_defaulted_dialog_open", loanId = loanId.toString())
        Logger.i(TAG) { "OnMarkDefaulted tapped loanId=$loanId" }
        sendEvent(LoanDetailEvent.ShowDefaultConfirmDialog)
    }

    // -- Back navigation (ui.yaml top_bar.on_nav_click, effect: navigate) ----------------------------

    private fun handleBack() {
        Logger.i(TAG) { "OnBack tapped loanId=$loanId" }
        sendEvent(LoanDetailEvent.NavigateBack)
    }

    // -- Pull-to-refresh (data-flow.yaml on_refresh: network_first bypass) --------------------------

    private fun handleRefresh() {
        Logger.i(TAG) { "pull-to-refresh triggered loanId=$loanId" }
        updateState { copy(isLoading = true) }
        detailStream.refreshFresh()
    }

    // -- Error-state retry (ui.yaml effect: call_api, library_refs: [cmp-network-monitor]) ----------

    private fun handleRetry() {
        Logger.i(TAG) { "retry tapped — re-dispatching loan-detail fetch loanId=$loanId" }
        updateState { copy(error = null, isLoading = true) }
        detailStream.retry()
    }

    // -- Stream -> State mapping ----------------------------------------------------------------------

    private fun handleStreamUpdated(screenState: ScreenState<LoanDetailResponse>) {
        when (screenState) {
            is ScreenState.Loading -> updateState { copy(isLoading = true, error = null) }

            is ScreenState.Empty -> updateState {
                // Never actually emitted — LoanDetailStore always resolves a single composite row
                // or fails; kept for ScreenState exhaustiveness only, mirrors
                // GroupDashboardViewModel's identical precedent for a single-composite read.
                copy(isLoading = false, error = null)
            }

            is ScreenState.Content -> updateState {
                val response = screenState.data
                copy(
                    isLoading = false,
                    loan = response.loan,
                    repaymentSchedule = response.repaymentSchedule,
                    repaymentHistory = response.repaymentHistory,
                    error = null,
                )
            }

            is ScreenState.NoNetwork -> updateState {
                copy(isLoading = false, error = LoanDetailError.Network)
            }

            is ScreenState.Unauthenticated -> {
                // data-flow.yaml `401 -> navigate login`, but no matching NavigateToLogin event is
                // declared in ui.yaml#state_model.events.members — mirrors GroupDashboardError.Auth's
                // / LoanListError.Auth's identical documented gap. sessionManager.endSession() is a
                // REAL clear-session call (the app shell handles re-auth after token expiry);
                // ShowSnackbar is the closest declared event, emitted so the user still gets
                // immediate feedback.
                crashReporter.recordMessage(
                    message = "loan-detail: session expired (401) loanId=$loanId — clearing session",
                    level = CrashSeverity.Warning,
                )
                sessionManager.endSession()
                updateState { copy(isLoading = false, error = LoanDetailError.Auth) }
                sendEvent(LoanDetailEvent.ShowSnackbar(message = LoanDetailError.Auth.messageKey))
            }

            is ScreenState.Error -> {
                val throwable = screenState.error
                crashReporter.recordException(
                    throwable = throwable,
                    message = "loan-detail: stream error loanId=$loanId isNetworkError=${screenState.isNetworkError}",
                )
                val category = categorize(throwable)
                val mapped = when {
                    screenState.isNetworkError -> LoanDetailError.Network
                    // See LoanDetailError.NotFound KDoc "reachability gap" — future-proofed,
                    // currently unreachable given LoanDetailFetchException's message shape.
                    category is ErrorCategory.ClientError && category.httpCode == 404 -> LoanDetailError.NotFound
                    else -> LoanDetailError.Server
                }
                updateState { copy(isLoading = false, error = mapped) }
            }
        }
    }
}
