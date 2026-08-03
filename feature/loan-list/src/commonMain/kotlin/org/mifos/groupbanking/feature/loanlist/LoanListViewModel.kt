/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.loanlist

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
import kpt.core.base.store.screen.ScreenState
import kpt.core.base.ui.viewmodel.BaseViewModel
import org.mifos.groupbanking.core.data.repository.LoanRepository
import org.mifos.groupbanking.core.model.LoanAccountStatus
import org.mifos.groupbanking.core.model.LoanStatusFilter
import org.mifos.groupbanking.core.model.LoanSummary

private const val TAG = "LoanListViewModel"

/**
 * Roles that may apply for a loan on behalf of the group — `data-flow.yaml#on_apply_loan`:
 * "canApplyLoan is derived from the current role (chairperson / treasurer = true, member = false)".
 * The `viewerRole` nav-param (forwarded from `group-dashboard`, originating `dt_member_role`) is
 * matched against this set to seed [LoanListState.canApplyLoan]. ORGANIZER is included because the
 * organizer is the group's top management role (superset of chairperson/treasurer authority).
 */
private val LOAN_APPLY_ROLES = setOf("ORGANIZER", "CHAIRPERSON", "TREASURER")

/**
 * Screen-level render state for `loan-list-screen` — verbatim mirror of
 * ui.yaml#state_model.LoanListViewModel.screen_state. Derived (not stored) from
 * [LoanListState.isLoading] / [LoanListState.error] / [LoanListState.loans] via the
 * [LoanListState.screenState] extension below — exactly one source of truth for
 * loading/error/empty, mirroring `GroupListState`'s / `GroupDashboardState`'s identical
 * convention (`training-layer/TRAINING_MASTER.yaml#patterns.state_models`).
 *
 * The derivation deliberately reads [LoanListState.loans] (the full unfiltered page-accumulated
 * list), never [LoanListState.filteredLoans] — an empty *filter result* (e.g. no CLOSED loans yet)
 * renders as an empty in-`Content` list, not the [Empty] illustration state; [Empty] is reserved
 * for a group with genuinely zero loan accounts. See API.md#state.
 */
@Serializable
sealed interface LoanListScreenState {
    @Serializable
    data object Loading : LoanListScreenState

    @Serializable
    data object Content : LoanListScreenState

    @Serializable
    data object Error : LoanListScreenState

    @Serializable
    data object Empty : LoanListScreenState
}

/**
 * Error taxonomy for the group loan-list read — verbatim mirror of
 * ui.yaml#state_model.LoanListViewModel.errors.types. [messageKey] is a composeResources
 * string-resource id (never a raw hardcoded English string, per
 * RULE-IMPL-NO-HARDCODED-STRING-001) resolved by the Screen layer.
 *
 * [Auth] declares `redirect: login` in ui.yaml, but `state_model.events.members` declares no
 * `NavigateToLogin`/`NavigateToLoginSignup` event — mirrors `GroupListError.Auth`'s identical
 * documented gap. [Auth] is surfaced as an ordinary non-retryable error state, and
 * `sessionManager.endSession()` is called for real (a genuine `ui.yaml#state_model.di` dependency
 * usage, same as `GroupDashboardViewModel`'s identical precedent) so the app shell can react to
 * the cleared session even though no screen-local redirect event exists; the closest declared
 * event ([LoanListEvent.ShowSnackbar]) is ALSO emitted carrying the `error_auth` message key.
 * Reported to the caller for an idea-layer `ui.yaml#events` update (add a `NavigateToLogin`
 * member) rather than invented silently. See API.md#state.
 */
@Serializable
sealed interface LoanListError {
    val retry: Boolean
    val messageKey: String

    @Serializable
    data object Network : LoanListError {
        override val retry: Boolean = true
        override val messageKey: String = "error_network"
    }

    @Serializable
    data object Server : LoanListError {
        override val retry: Boolean = true
        override val messageKey: String = "error_server"
    }

    @Serializable
    data object Auth : LoanListError {
        override val retry: Boolean = false
        override val messageKey: String = "error_auth"
    }
}

/**
 * MVI state for `LoanListViewModel`. Field set + defaults are a verbatim mirror of
 * ui.yaml#state_model.LoanListViewModel.state. [loans], [filteredLoans], and [error] are
 * `@Transient` — the paged list is always re-derived from
 * [LoanRepository.loansPagingStream] on (re)subscription (offline-first cache via the `loans`
 * SQLDelight table, so nothing is visually lost across process death — the store, not this
 * transient render state, is the durable source), and [LoanSummary] itself is not `@Serializable`.
 * Mirrors `GroupListState`'s identical `@Transient` convention for non-serializable domain
 * payloads (see `training-layer/TRAINING_MASTER.yaml#patterns.state_models`).
 *
 * **[canApplyLoan] — now wired from the `viewerRole` nav-param (gap closed):** `data-flow.yaml`
 * states "canApplyLoan is derived from the current role (chairperson / treasurer = true, member =
 * false)". The `viewerRole` nav-param is now forwarded from `group-dashboard`'s "Loans" entry point
 * (`ui.yaml#nav_params.viewerRole`, mirroring `group-dashboard`'s own `groupId`/`viewerRole` pair)
 * and matched against [LOAN_APPLY_ROLES] in the constructor to seed this field — the Apply-Loan FAB
 * renders for management roles (ORGANIZER/CHAIRPERSON/TREASURER) and stays hidden for MEMBER. The
 * injectable `kpt.core.base.security.SessionManager` (`core-base/security`) still carries only
 * session-lifecycle state (no role field) — the role travels by nav-param, not by session, exactly
 * as `group-dashboard` already does. See API.md#state.
 */
@Serializable
@Immutable
data class LoanListState(
    val isLoading: Boolean = true,
    @Transient
    val loans: List<LoanSummary> = emptyList(),
    val selectedFilter: LoanStatusFilter = LoanStatusFilter.ALL,
    @Transient
    val filteredLoans: List<LoanSummary> = emptyList(),
    val isRefreshing: Boolean = false,
    @Transient
    val error: LoanListError? = null,
    val groupId: Long = 0L,
    val canApplyLoan: Boolean = false,
)

/** Derived, single-source-of-truth screen state — see [LoanListScreenState] KDoc. */
val LoanListState.screenState: LoanListScreenState
    get() = when {
        error != null -> LoanListScreenState.Error
        isLoading -> LoanListScreenState.Loading
        loans.isEmpty() -> LoanListScreenState.Empty
        else -> LoanListScreenState.Content
    }

/**
 * One-shot side effects emitted by `LoanListViewModel` — verbatim mirror of
 * ui.yaml#state_model.LoanListViewModel.events. See API.md#events.
 */
sealed interface LoanListEvent {
    data class NavigateToLoanDetail(val loanId: Long) : LoanListEvent
    data class NavigateToLoanApply(val groupId: Long) : LoanListEvent
    data class ShowSnackbar(val message: String) : LoanListEvent
}

/**
 * User intents dispatched to `LoanListViewModel`. The 6 top-level members are a verbatim mirror of
 * ui.yaml#state_model.LoanListViewModel.actions — RULE-IMPL-DEAD-CLICKABLE-001 Rule 1. [Internal]
 * is the sanctioned async-result-routing sub-interface (never a user intent) per
 * `training-layer/TRAINING_MASTER.yaml#patterns.actions` — mirrors `GroupListAction.Internal`.
 *
 * **Idea-layer gap (flagged, not invented here):** ui.yaml's `top_bar` component declares an
 * `on_nav_click` wiring the `OnBack` action (`effect: navigate`, a fully-authored
 * `action_contract` popping the destination back to `group-dashboard`) that is NOT present in
 * `state_model.actions.members` (only the 6 members below are declared). Per the
 * interactive-action naming convention (RULE-IMPL-DEAD-CLICKABLE-001 SP-07 Rule 1) this
 * addition-from-component is NOT added to this sealed interface silently — mirrors
 * `GroupListAction`'s identical `OnOpenNotifications` gap precedent. Reported to the caller for an
 * idea-layer `ui.yaml#state_model.actions.members` update (add `OnBack`) rather than invented.
 * See API.md#actions.
 */
sealed interface LoanListAction {
    data class OnLoanClick(val loanId: Long) : LoanListAction
    data class OnFilterChange(val filter: LoanStatusFilter) : LoanListAction
    data object OnApplyLoan : LoanListAction
    data object OnRefresh : LoanListAction
    data object Retry : LoanListAction
    data object OnLoadNextPage : LoanListAction

    /** Async stream emissions — routed via `trySendAction`, never dispatched by the UI. */
    sealed interface Internal : LoanListAction {
        data class StreamUpdated(val screenState: ScreenState<List<LoanSummary>>) : Internal
    }
}

/**
 * MVI processor for the loan-list screen (`business_logic.kind: crud` per ui.yaml — a pure
 * paginated read-side list, so [LoanRepository.loansPagingStream]'s offline-first
 * `kpt.core.base.store.paging.PagingScreenStream` is consumed directly rather than the SP-04 AC-7
 * analytics/crashReporter/fieldEncryptor injection triple, per RULE-IDEA-IMPL-INTELLIGENCE-001
 * AC-03i — that hook set is reserved for non-crud/non-nav_only Store5 write paths. There is no PII
 * on [LoanSummary], so no `FieldEncryptor` is injected.
 *
 * [crashReporter] and [analytics] are still wired (feature-level observability, SC5): a Debug
 * breadcrumb on mount, a warning/exception breadcrumb on stream failure, and a
 * `trackLoanOperation(...)` call on every user-initiated loan operation — all through the actual
 * shipped `CrashReporter`/`KptAnalyticsTracker` surfaces (no invented methods; mirrors the
 * documented convention on `GroupListViewModel`/`GroupDashboardViewModel`).
 *
 * [sessionManager] is declared in `ui.yaml#state_model.di` and is used for its ONE real
 * capability — `endSession()` on `ScreenState.Unauthenticated` (mirrors
 * `GroupDashboardViewModel`'s identical wiring); it does NOT carry role information, see
 * [LoanListState]'s KDoc "canApplyLoan" gap note. `NetworkMonitor` is ALSO declared in
 * `ui.yaml#state_model.di` but is not injected here directly — it is already composed inside
 * `LoanRepositoryImpl.loansPagingStream` (`Store.asPagingScreenStream(networkMonitor = ..., ...)`),
 * matching `GroupListViewModel`'s identical precedent of not re-injecting it at the ViewModel
 * layer.
 *
 * [groupId] and [viewerRole] are the `ui.yaml#nav_params` values forwarded from `group-dashboard`'s
 * "Loans" entry point via Koin `parametersOf(groupId, viewerRole)` (see `di.LoanListModule`) —
 * [groupId] seeds [LoanListState.groupId] and scopes the paged [LoanRepository.loansPagingStream]
 * read; [viewerRole] (the `dt_member_role` role string ORGANIZER/CHAIRPERSON/TREASURER/MEMBER,
 * originating on `group-list` and threaded through `group-dashboard`) seeds
 * [LoanListState.canApplyLoan] via the [LOAN_APPLY_ROLES] gate — this closes the previously-flagged
 * `canApplyLoan` no-role-source gap (`data-flow.yaml#on_apply_loan`: chairperson/treasurer = true,
 * member = false), so the Apply-Loan FAB now renders for management roles rather than staying
 * conservatively hidden for everyone.
 *
 * See API.md#viewmodel.
 */
internal class LoanListViewModel(
    private val repository: LoanRepository,
    private val sessionManager: SessionManager,
    private val crashReporter: CrashReporter,
    private val analytics: KptAnalyticsTracker,
    private val groupId: Long,
    viewerRole: String,
) : BaseViewModel<LoanListState, LoanListEvent, LoanListAction>(
    initialState = LoanListState(
        groupId = groupId,
        canApplyLoan = viewerRole in LOAN_APPLY_ROLES,
    ),
) {

    /** Offline-first PAGED stream, scoped to [groupId] — see `LoanRepository.loansPagingStream` KDoc. */
    private val pagingStream = repository.loansPagingStream(groupId = groupId, scope = viewModelScope)

    init {
        crashReporter.recordMessage(
            message = "feature=loan-list screen=loan-list-screen groupId=$groupId",
            level = CrashSeverity.Debug,
        )
        analytics.trackLoanOperation(operation = "view_list")
        viewModelScope.launch {
            pagingStream.state.collect { screenState ->
                trySendAction(LoanListAction.Internal.StreamUpdated(screenState))
            }
        }
    }

    override fun handleAction(action: LoanListAction) {
        when (action) {
            is LoanListAction.OnLoanClick -> handleLoanClick(action.loanId)
            is LoanListAction.OnFilterChange -> handleFilterChange(action.filter)
            LoanListAction.OnApplyLoan -> handleApplyLoan()
            LoanListAction.OnRefresh -> handleRefresh()
            LoanListAction.Retry -> handleRetry()
            LoanListAction.OnLoadNextPage -> handleLoadNextPage()
            is LoanListAction.Internal.StreamUpdated -> handleStreamUpdated(action.screenState)
        }
    }

    // -- Loan card tap (ui.yaml effect: navigate, target: loan-detail) ----------------------------

    private fun handleLoanClick(loanId: Long) {
        analytics.trackLoanOperation(operation = "view", loanId = loanId.toString())
        Logger.i(TAG) { "loan card tapped loanId=$loanId groupId=$groupId" }
        sendEvent(LoanListEvent.NavigateToLoanDetail(loanId))
    }

    // -- Filter chip tap (ui.yaml effect: transform_state, client-side status filter) -------------

    private fun handleFilterChange(filter: LoanStatusFilter) {
        Logger.d(TAG) { "filter changed to=$filter groupId=$groupId" }
        updateState { copy(selectedFilter = filter, filteredLoans = loans.filterByStatus(filter)) }
    }

    // -- Apply-Loan FAB (ui.yaml effect: navigate, target: loan-apply) ------------------------------

    private fun handleApplyLoan() {
        analytics.trackLoanOperation(operation = "apply")
        Logger.i(TAG) { "apply loan FAB tapped groupId=$groupId" }
        sendEvent(LoanListEvent.NavigateToLoanApply(groupId))
    }

    // -- Pull to refresh (data-flow.yaml on_refresh: network_first bypass) --------------------------

    private fun handleRefresh() {
        analytics.trackLoanOperation(operation = "refresh")
        Logger.i(TAG) { "pull-to-refresh triggered groupId=$groupId" }
        updateState { copy(isRefreshing = true) }
        pagingStream.refresh()
    }

    // -- Error-state retry (ui.yaml effect: call_api, external_library_refs: [Store5]) --------------

    private fun handleRetry() {
        Logger.i(TAG) { "retry tapped — re-dispatching group loans fetch via LoanRepository groupId=$groupId" }
        updateState { copy(error = null, isLoading = true) }
        pagingStream.retry()
    }

    // -- Scroll-to-end pagination (data-flow.yaml on_action/OnLoadNextPage) -------------------------

    private fun handleLoadNextPage() {
        Logger.i(TAG) { "load-next-page triggered currentSize=${state.loans.size} groupId=$groupId" }
        pagingStream.loadNextPage()
    }

    // -- Stream → State mapping ----------------------------------------------------------------------

    private fun handleStreamUpdated(screenState: ScreenState<List<LoanSummary>>) {
        when (screenState) {
            is ScreenState.Loading -> updateState {
                copy(isLoading = true, error = null, isRefreshing = false)
            }

            is ScreenState.Empty -> updateState {
                copy(
                    isLoading = false,
                    loans = emptyList(),
                    filteredLoans = emptyList(),
                    error = null,
                    isRefreshing = false,
                )
            }

            is ScreenState.Content -> updateState {
                val newLoans = screenState.data
                copy(
                    isLoading = false,
                    loans = newLoans,
                    filteredLoans = newLoans.filterByStatus(selectedFilter),
                    error = null,
                    isRefreshing = false,
                )
            }

            is ScreenState.NoNetwork -> updateState {
                copy(isLoading = false, error = LoanListError.Network, isRefreshing = false)
            }

            is ScreenState.Unauthenticated -> {
                // See LoanListError.Auth KDoc — no declared NavigateToLogin event exists;
                // sessionManager.endSession() is a REAL clear-session call (the app shell handles
                // re-auth after token expiry); ShowSnackbar is the closest declared event, emitted
                // so the user still gets immediate feedback.
                crashReporter.recordMessage(
                    message = "loan-list: session expired (401) groupId=$groupId — clearing session",
                    level = CrashSeverity.Warning,
                )
                sessionManager.endSession()
                updateState { copy(isLoading = false, error = LoanListError.Auth, isRefreshing = false) }
                sendEvent(LoanListEvent.ShowSnackbar(message = LoanListError.Auth.messageKey))
            }

            is ScreenState.Error -> {
                crashReporter.recordException(
                    throwable = screenState.error,
                    message = "loan-list: stream error groupId=$groupId isNetworkError=${screenState.isNetworkError}",
                )
                val mapped = if (screenState.isNetworkError) {
                    LoanListError.Network
                } else {
                    LoanListError.Server
                }
                updateState { copy(isLoading = false, error = mapped, isRefreshing = false) }
            }
        }
    }

    /**
     * Client-side status filter — ui.yaml `filter_chips_row` chips' `effect: transform_state`.
     * [LoanStatusFilter.ALL] performs no filtering; the other 3 members match 1:1 by name against
     * [LoanAccountStatus] (which additionally carries `PENDING`/`REJECTED`/`UNKNOWN` — loans in
     * those statuses are only visible under the `ALL` filter, matching ui.yaml's 4-chip design).
     */
    private fun List<LoanSummary>.filterByStatus(statusFilter: LoanStatusFilter): List<LoanSummary> =
        when (statusFilter) {
            LoanStatusFilter.ALL -> this
            LoanStatusFilter.ACTIVE -> filter { it.status == LoanAccountStatus.ACTIVE }
            LoanStatusFilter.OVERDUE -> filter { it.status == LoanAccountStatus.OVERDUE }
            LoanStatusFilter.CLOSED -> filter { it.status == LoanAccountStatus.CLOSED }
        }
}
