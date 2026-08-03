/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.membersavingsdetail

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
import org.mifos.groupbanking.core.data.repository.SavingsRepository
import org.mifos.groupbanking.core.model.GroupTypeConfig
import org.mifos.groupbanking.core.model.MemberSavingsDetail
import org.mifos.groupbanking.core.model.SavingsDataPoint
import org.mifos.groupbanking.core.model.SavingsMember
import org.mifos.groupbanking.core.model.SavingsStatementEntry
import org.mifos.groupbanking.core.model.SavingsTransactionFilter
import org.mifos.groupbanking.core.model.matches

// MVI stack (State/Event/Action/ViewModel/DI) for the `member-savings-detail` feature — see
// API.md#viewmodel / #state / #actions / #events / #di for the full generated-symbol contract.
private const val TAG = "MemberSavingsDetailViewModel"

/** Statement page size — mirrors `data-flow.yaml#entries[on_mount].notes` ("limit=20, offset=0"). */
private const val PAGE_SIZE = 20

/**
 * Screen-level render state for `member-savings-detail-screen` — verbatim mirror of
 * `ui.yaml#state_model.MemberSavingsDetailViewModel.screen_state.members` (4 members). Derived
 * only (not stored) via [MemberSavingsDetailState.deriveScreenState] — same convention as
 * `LoanListState.screenState`. [Empty] is derived from [MemberSavingsDetailState.filteredTransactions]
 * (not the raw unfiltered [MemberSavingsDetailState.transactions]) — `ui.yaml#states.empty.description`
 * is explicit: "No transactions match the active filter", the opposite convention from
 * `LoanListState.screenState` (which deliberately reads the unfiltered list so a filter with zero
 * matches stays `Content`). See API.md#state.
 */
@Serializable
sealed interface MemberSavingsDetailScreenState {
    @Serializable
    data object Loading : MemberSavingsDetailScreenState

    @Serializable
    data object Content : MemberSavingsDetailScreenState

    @Serializable
    data object Empty : MemberSavingsDetailScreenState

    @Serializable
    data object Error : MemberSavingsDetailScreenState
}

/**
 * Error taxonomy for the member-savings statement read — verbatim mirror of
 * `ui.yaml#state_model.MemberSavingsDetailViewModel.errors.types` (4 members). [messageKey] is a
 * composeResources string-resource id (never a raw hardcoded English string, per
 * RULE-IMPL-NO-HARDCODED-STRING-001) resolved by the Screen layer.
 *
 * **[Auth] `redirect: login` gap (flagged, not invented around):** `ui.yaml#errors.types` declares
 * `redirect: login` for `Auth`, but `state_model.events.members` declares only `NavigateBack` and
 * `ShowSnackbar` — no `NavigateToLogin` member. Mirrors `LoanListError.Auth`'s /
 * `LoanDetailError.Auth`'s identical documented gap. `sessionManager.endSession()` is still called
 * for real on this branch (see [MemberSavingsDetailViewModel.reportLoadError]) so the app shell can
 * react to the cleared session; [MemberSavingsDetailEvent.ShowSnackbar] carries the `error_auth`
 * message key as the closest declared event. Reported to the caller for an idea-layer
 * `ui.yaml#events` update (add `NavigateToLogin`) rather than invented silently. See API.md#state.
 */
@Serializable
sealed interface MemberSavingsError {
    val retry: Boolean
    val messageKey: String

    @Serializable
    data object Network : MemberSavingsError {
        override val retry: Boolean = true
        override val messageKey: String = "error_network"
    }

    @Serializable
    data object Server : MemberSavingsError {
        override val retry: Boolean = true
        override val messageKey: String = "error_server"
    }

    @Serializable
    data object NotFound : MemberSavingsError {
        override val retry: Boolean = false
        override val messageKey: String = "error_not_found"
    }

    @Serializable
    data object Auth : MemberSavingsError {
        override val retry: Boolean = false
        override val messageKey: String = "error_auth"
    }
}

/**
 * MVI state for `MemberSavingsDetailViewModel`. Field set + defaults are a verbatim mirror of
 * `ui.yaml#state_model.MemberSavingsDetailViewModel.state.fields`, with two deliberate type
 * corrections and two additive fields (all documented, none silently applied — per
 * RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1):
 *
 * **[member] type — confirmed idea-layer drift, corrected to the real domain type:** `ui.yaml`
 * declares `member: Member?`, but [SavingsRepository.getMemberSavingsDetail] actually returns
 * [MemberSavingsDetail.member] typed [SavingsMember] (`Savings.kt`) — a distinct, narrower
 * member-identity shape (`memberId`/`displayName`/`photoUri`) than the canonical [Member][
 * org.mifos.groupbanking.core.model.Member] reused by member-list/member-profile/member-add. Using
 * the literal ui.yaml type would not compile against the just-generated repository contract; mirrors
 * `PersonalSavingsState`'s identical documented type-correction convention. Flagged for the
 * cross-feature repair station (CFF1) — `ui.yaml` should reference `SavingsMember` directly.
 *
 * **[contributionModel] value-set — confirmed idea-layer drift, derived from the real registry:**
 * `ui.yaml#state.fields.contributionModel` comments describe the value-set as
 * `SHARE_BASED_VARIABLE | FIXED_AMOUNT | FIXED_NEGOTIATED`, but the actual DTO-registry enum backing
 * [GroupTypeConfig.contributionMode] is [org.mifos.groupbanking.core.model.ContributionMode] —
 * `SHARE_BASED_VARIABLE | FIXED | MINIMAL | UNKNOWN` (`GroupTypeConfig.kt`). Per the registry's
 * "registry wins" precedent this field is seeded from `typeConfig.contributionMode.name` (the real
 * 4-value set), NOT the ui.yaml comment's 3-value set — `FIXED_AMOUNT`/`FIXED_NEGOTIATED` never
 * appear. Flagged for the cross-feature repair station (CFF1) — `ui.yaml` should be corrected to
 * reference `ContributionMode` directly.
 *
 * **[filteredTransactions] — additive, not in `ui.yaml#state.fields`:** `ui.yaml#states.empty` is
 * explicit that the Empty screen state means "No transactions match the selected filter" (see
 * [MemberSavingsDetailScreenState] KDoc), which requires a materialized filtered view distinct from
 * the accumulated unfiltered [transactions] page cache — mirrors `LoanListState.filteredLoans`'s
 * identical additive-field precedent. Recomputed on every [OnFilterSelected]/page-load/refresh via
 * [SavingsTransactionFilter.matches]. Flagged for the cross-feature repair station (CFF1).
 *
 * **[expandedTransactionId] — additive, not in `ui.yaml#state.fields`:** required to implement
 * `ui.yaml#components.transaction_card.on_click` (`action: OnTransactionSelected`,
 * `effect: transform_state`, "toggling the expanded transactionId in state") as REAL logic rather
 * than an empty branch — the on_click's own `action_contract.description` names this exact field but
 * `state_model.state.fields` never declares it. Flagged for the cross-feature repair station (CFF1).
 *
 * [member], [typeConfig], [sparklineData], [transactions], [filteredTransactions], and [error] are
 * `@Transient` — always re-derived from [SavingsRepository] (or the nav-param [typeConfig]) on
 * (re)construction, never restored from a `@Serializable` snapshot, mirroring every other detail
 * ViewModel's identical `@Transient` convention for non-serializable domain payloads.
 */
@Serializable
@Immutable
data class MemberSavingsDetailState(
    val memberId: String = "",
    val groupId: String = "",
    val isLoading: Boolean = true,
    @Transient
    val member: SavingsMember? = null,
    @Transient
    val typeConfig: GroupTypeConfig? = null,
    val contributionModel: String = "",
    val sharesHeld: Int? = null,
    val shareValue: Long? = null,
    val savingsBalance: Double = 0.0,
    val savingsAccountNo: String = "",
    @Transient
    val sparklineData: List<SavingsDataPoint> = emptyList(),
    @Transient
    val transactions: List<SavingsStatementEntry> = emptyList(),
    val selectedFilter: SavingsTransactionFilter = SavingsTransactionFilter.ALL,
    @Transient
    val filteredTransactions: List<SavingsStatementEntry> = emptyList(),
    val expandedTransactionId: String? = null,
    val isLoadingNextPage: Boolean = false,
    val hasNextPage: Boolean = true,
    val currentOffset: Int = 0,
    @Transient
    val error: MemberSavingsError? = null,
    val isRefreshing: Boolean = false,
)

/** Derived, single-source-of-truth screen state — see [MemberSavingsDetailScreenState] KDoc. */
fun MemberSavingsDetailState.deriveScreenState(): MemberSavingsDetailScreenState = when {
    error != null -> MemberSavingsDetailScreenState.Error
    isLoading -> MemberSavingsDetailScreenState.Loading
    filteredTransactions.isEmpty() -> MemberSavingsDetailScreenState.Empty
    else -> MemberSavingsDetailScreenState.Content
}

/**
 * One-shot side effects emitted by `MemberSavingsDetailViewModel` — verbatim mirror of
 * `ui.yaml#state_model.MemberSavingsDetailViewModel.events.members` (2 members). See API.md#events.
 */
sealed interface MemberSavingsDetailEvent {
    data object NavigateBack : MemberSavingsDetailEvent
    data class ShowSnackbar(val message: String) : MemberSavingsDetailEvent
}

/**
 * User intents dispatched to `MemberSavingsDetailViewModel`. The 6 top-level members are a verbatim
 * mirror of `ui.yaml#state_model.MemberSavingsDetailViewModel.actions.members` —
 * RULE-IMPL-DEAD-CLICKABLE-001 Rule 1. [Internal] is the sanctioned async-result-routing
 * sub-interface (never a user intent) per `training-layer/TRAINING_MASTER.yaml#patterns.actions` —
 * mirrors `PersonalSavingsAction.Internal`/`LoanListAction.Internal`.
 *
 * **[OnFilterSelected] param type — confirmed idea-layer drift, corrected to the real registry
 * type:** `ui.yaml#actions.members[OnFilterSelected].params.filter` declares a bare `TransactionFilter`
 * type, but `core/model` already ships this exact 3-value (`ALL`/`DEPOSITS`/`WITHDRAWALS`) enum under
 * [SavingsTransactionFilter] — complete with a purpose-built [org.mifos.groupbanking.core.model.matches]
 * extension over [SavingsStatementEntry]. Reused outright rather than re-declaring a duplicate local
 * enum (per the generation brief's "do not duplicate an existing registry value-set" guidance).
 * Flagged for the cross-feature repair station (CFF1) — `ui.yaml` should reference
 * `SavingsTransactionFilter` directly. See API.md#actions.
 */
sealed interface MemberSavingsDetailAction {
    data class OnFilterSelected(val filter: SavingsTransactionFilter) : MemberSavingsDetailAction
    data class OnTransactionSelected(val transactionId: String) : MemberSavingsDetailAction
    data object OnLoadMore : MemberSavingsDetailAction
    data object OnRefresh : MemberSavingsDetailAction
    data object Retry : MemberSavingsDetailAction
    data object OnBack : MemberSavingsDetailAction

    /** Async coroutine result — routed via `trySendAction`, never dispatched by the UI. */
    sealed interface Internal : MemberSavingsDetailAction {
        /** on_mount initial read — offset 0. */
        data class DetailLoaded(val result: NetworkResult<MemberSavingsDetail, NetworkError>) : Internal

        /** `OnLoadMore` scroll-to-end read — offset `currentOffset + PAGE_SIZE`, appended. */
        data class NextPageLoaded(val result: NetworkResult<MemberSavingsDetail, NetworkError>) : Internal

        /** `OnRefresh` / `Retry` read — offset reset to 0, replaces the full page cache. */
        data class RefreshResult(val result: NetworkResult<MemberSavingsDetail, NetworkError>) : Internal
    }
}

/**
 * MVI processor for the contribution-model-aware member savings statement screen
 * (`business_logic.kind: crud` per ui.yaml — a paginated read-side companion fetch, so the SP-04
 * AC-7 analytics/crashReporter/fieldEncryptor injection triple is not mandatory per
 * RULE-IDEA-IMPL-INTELLIGENCE-001 AC-03i; [crashReporter] and [analytics] are still wired for real,
 * feature-level observability (SC5), mirroring `PersonalSavingsViewModel`'s/`LoanListViewModel`'s
 * identical crud-but-observed precedent). No `FieldEncryptor` (`core-base/security`) is injected —
 * neither [SavingsMember] nor [SavingsStatementEntry] carries a `@PII`-marked field per the SP-02
 * idea-layer schema.
 *
 * [repository] is consumed directly via [NetworkResult] — [SavingsRepository]'s own KDoc "Store5
 * branch" note (no `AppStoreRegistry.Savings` entry yet), same branch as
 * `PersonalSavingsViewModel`/`LoanApplyViewModel`. Offset-based pagination (`OnLoadMore`) is
 * therefore driven by hand ([MemberSavingsDetailState.currentOffset]/[MemberSavingsDetailState.hasNextPage])
 * rather than a `PagingScreenStream` — mirrors `data-flow.yaml#entries[OnLoadMore].cache.strategy:
 * append_to_existing`.
 *
 * **DI drift (flagged, not silently resolved):** `ui.yaml#state_model.di` declares
 * `[MemberRepository, NetworkMonitor]`, but the shipped data layer exposes
 * `SavingsRepository.getMemberSavingsDetail` (not a `MemberRepository` method) — mirrors
 * `LoanDetailViewModel`'s identical documented `LoanRepository`-vs-`LoanDetailRepository` drift.
 * [sessionManager] is ALSO injected here (not declared in `ui.yaml#state_model.di` at all) purely
 * to call `endSession()` for real on the `Auth` branch (see [reportLoadError]) — the exact same
 * addition every other detail/list ViewModel in this codebase makes for its own undeclared 401
 * path (`LoanListViewModel`, `LoanDetailViewModel`, `PersonalSavingsViewModel`). Both drifts are
 * reported to the caller for an idea-layer `ui.yaml#state_model.di` correction rather than invented
 * silently.
 *
 * [networkMonitor] IS declared in `ui.yaml#state_model.di` and IS injected directly here (unlike the
 * Store5-backed list/detail ViewModels, which compose it inside their repository) — the error-state
 * `Retry` action's `action_contract.library_refs: [cmp-network-monitor]` requires an explicit
 * connectivity gate before the re-fetch, mirroring `LoanApplyViewModel.handleSubmit`'s identical
 * `networkMonitor.isOnline.value` gate.
 *
 * [memberId]/[groupId]/[typeConfig] are the `ui.yaml#nav_params` values forwarded from
 * `savings-dashboard`'s `tap_member_row` (or `member-profile`'s `view_full_history_button`) entry
 * point via Koin `parametersOf(...)` (see `di.MemberSavingsDetailModule`);
 * [MemberSavingsDetailState.contributionModel] is derived from [typeConfig] immediately at
 * construction (no need to wait for the network read — see [MemberSavingsDetailState] KDoc
 * "contributionModel value-set" note).
 *
 * See API.md#viewmodel.
 */
internal class MemberSavingsDetailViewModel(
    private val repository: SavingsRepository,
    private val networkMonitor: NetworkMonitor,
    private val sessionManager: SessionManager,
    private val crashReporter: CrashReporter,
    private val analytics: KptAnalyticsTracker,
    private val memberId: String,
    private val groupId: String,
    private val typeConfig: GroupTypeConfig,
) : BaseViewModel<MemberSavingsDetailState, MemberSavingsDetailEvent, MemberSavingsDetailAction>(
    initialState = MemberSavingsDetailState(
        memberId = memberId,
        groupId = groupId,
        typeConfig = typeConfig,
        contributionModel = typeConfig.contributionMode.name,
    ),
) {

    init {
        crashReporter.recordMessage(
            message = "feature=member-savings-detail screen=member-savings-detail-screen " +
                "groupId=$groupId memberId=$memberId contributionModel=${typeConfig.contributionMode}",
            level = CrashSeverity.Debug,
        )
        analytics.trackSavingsOperation(operation = "view", accountId = memberId)
        loadInitial()
    }

    override fun handleAction(action: MemberSavingsDetailAction) {
        when (action) {
            is MemberSavingsDetailAction.OnFilterSelected -> handleFilterSelected(action.filter)
            is MemberSavingsDetailAction.OnTransactionSelected -> handleTransactionSelected(action.transactionId)
            MemberSavingsDetailAction.OnLoadMore -> handleLoadMore()
            MemberSavingsDetailAction.OnRefresh -> handleRefresh()
            MemberSavingsDetailAction.Retry -> handleRetry()
            MemberSavingsDetailAction.OnBack -> handleBack()
            is MemberSavingsDetailAction.Internal.DetailLoaded -> handleDetailLoaded(action.result)
            is MemberSavingsDetailAction.Internal.NextPageLoaded -> handleNextPageLoaded(action.result)
            is MemberSavingsDetailAction.Internal.RefreshResult -> handleRefreshResult(action.result)
        }
    }

    // -- on_mount (data-flow.yaml — GET companion/groups/{groupId}/members/{memberId}/savings) -------

    private fun loadInitial() {
        viewModelScope.launch {
            val result = repository.getMemberSavingsDetail(
                groupId = groupId,
                memberId = memberId,
                limit = PAGE_SIZE,
                offset = 0,
            )
            trySendAction(MemberSavingsDetailAction.Internal.DetailLoaded(result))
        }
    }

    // -- Filter chip tap (ui.yaml effect: transform_state — client-side, no network) -------------------

    private fun handleFilterSelected(filter: SavingsTransactionFilter) {
        Logger.d(TAG) { "filter changed to=$filter groupId=$groupId memberId=$memberId" }
        updateState {
            copy(selectedFilter = filter, filteredTransactions = transactions.filter { entry -> filter.matches(entry) })
        }
    }

    // -- Transaction row tap (ui.yaml effect: transform_state — in-place expand/collapse toggle) -------

    private fun handleTransactionSelected(transactionId: String) {
        Logger.d(TAG) { "transaction row tapped id=$transactionId groupId=$groupId memberId=$memberId" }
        updateState {
            copy(expandedTransactionId = if (expandedTransactionId == transactionId) null else transactionId)
        }
    }

    // -- Scroll-to-end pagination (data-flow.yaml on_interact/OnLoadMore — offset += page_size) --------

    private fun handleLoadMore() {
        if (!state.hasNextPage || state.isLoadingNextPage) {
            Logger.d(TAG) {
                "OnLoadMore ignored hasNextPage=${state.hasNextPage} isLoadingNextPage=${state.isLoadingNextPage} " +
                    "groupId=$groupId memberId=$memberId"
            }
            return
        }
        val nextOffset = state.currentOffset + PAGE_SIZE
        Logger.i(TAG) { "loading next page offset=$nextOffset groupId=$groupId memberId=$memberId" }
        updateState { copy(isLoadingNextPage = true) }
        viewModelScope.launch {
            val result = repository.getMemberSavingsDetail(
                groupId = groupId,
                memberId = memberId,
                limit = PAGE_SIZE,
                offset = nextOffset,
            )
            trySendAction(MemberSavingsDetailAction.Internal.NextPageLoaded(result))
        }
    }

    // -- Pull-to-refresh (data-flow.yaml on_refresh: bypass_and_refresh, resets to page 0) --------------

    private fun handleRefresh() {
        analytics.trackSavingsOperation(operation = "refresh", accountId = memberId)
        Logger.i(TAG) { "pull-to-refresh triggered groupId=$groupId memberId=$memberId" }
        updateState { copy(isRefreshing = true) }
        viewModelScope.launch {
            val result = repository.getMemberSavingsDetail(
                groupId = groupId,
                memberId = memberId,
                limit = PAGE_SIZE,
                offset = 0,
            )
            trySendAction(MemberSavingsDetailAction.Internal.RefreshResult(result))
        }
    }

    // -- Error-state retry (ui.yaml effect: call_api, library_refs: [cmp-network-monitor]) --------------

    private fun handleRetry() {
        if (!networkMonitor.isOnline.value) {
            Logger.w(TAG) { "retry attempted while offline groupId=$groupId memberId=$memberId" }
            crashReporter.recordMessage(
                message = "member-savings-detail: retry attempted while offline groupId=$groupId memberId=$memberId",
                level = CrashSeverity.Info,
            )
            updateState { copy(error = MemberSavingsError.Network) }
            sendEvent(MemberSavingsDetailEvent.ShowSnackbar(message = MemberSavingsError.Network.messageKey))
            return
        }
        Logger.i(TAG) { "retry tapped — re-dispatching member savings fetch groupId=$groupId memberId=$memberId" }
        updateState { copy(error = null, isLoading = true, currentOffset = 0) }
        viewModelScope.launch {
            val result = repository.getMemberSavingsDetail(
                groupId = groupId,
                memberId = memberId,
                limit = PAGE_SIZE,
                offset = 0,
            )
            trySendAction(MemberSavingsDetailAction.Internal.RefreshResult(result))
        }
    }

    // -- Back navigation (ui.yaml top_bar.on_navigation_click, effect: navigate) --------------------------

    private fun handleBack() {
        Logger.i(TAG) { "OnBack tapped groupId=$groupId memberId=$memberId" }
        sendEvent(MemberSavingsDetailEvent.NavigateBack)
    }

    // -- Initial-mount fetch result routing ------------------------------------------------------------------

    private fun handleDetailLoaded(result: NetworkResult<MemberSavingsDetail, NetworkError>) {
        when (result) {
            is NetworkResult.Success -> applyDetail(result.data)
            is NetworkResult.Error -> applyError(result.error)
        }
    }

    // -- OnLoadMore result routing — appends rather than replaces ----------------------------------------------

    private fun handleNextPageLoaded(result: NetworkResult<MemberSavingsDetail, NetworkError>) {
        when (result) {
            is NetworkResult.Success -> {
                val detail = result.data
                val nextOffset = state.currentOffset + PAGE_SIZE
                val merged = state.transactions + detail.transactions
                Logger.i(TAG) {
                    "page loaded offset=$nextOffset appended=${detail.transactions.size} totalNow=${merged.size} " +
                        "hasNextPage=${detail.hasNextPage} groupId=$groupId memberId=$memberId"
                }
                updateState {
                    copy(
                        transactions = merged,
                        filteredTransactions = merged.filter { entry -> selectedFilter.matches(entry) },
                        currentOffset = nextOffset,
                        hasNextPage = detail.hasNextPage,
                        isLoadingNextPage = false,
                    )
                }
            }

            is NetworkResult.Error -> {
                crashReporter.recordException(
                    throwable = IllegalStateException("member-savings-detail: load-more failed: ${result.error}"),
                    message = "member-savings-detail: load-more failed groupId=$groupId memberId=$memberId",
                )
                updateState { copy(isLoadingNextPage = false) }
                sendEvent(MemberSavingsDetailEvent.ShowSnackbar(message = result.error.toMemberSavingsError().messageKey))
            }
        }
    }

    // -- OnRefresh / Retry result routing — replaces the full page cache from offset 0 ---------------------------

    private fun handleRefreshResult(result: NetworkResult<MemberSavingsDetail, NetworkError>) {
        when (result) {
            is NetworkResult.Success -> applyDetail(result.data)
            is NetworkResult.Error -> applyError(result.error)
        }
    }

    // -- Shared success/error appliers -----------------------------------------------------------------------------

    /** Full replace from offset 0 — shared by the initial mount, `OnRefresh`, and `Retry` paths. */
    private fun applyDetail(detail: MemberSavingsDetail) {
        Logger.i(TAG) {
            "member savings loaded groupId=$groupId memberId=$memberId count=${detail.transactions.size} " +
                "hasNextPage=${detail.hasNextPage}"
        }
        updateState {
            copy(
                isLoading = false,
                isRefreshing = false,
                member = detail.member,
                savingsAccountNo = detail.savingsAccountNo,
                savingsBalance = detail.savingsBalance,
                sharesHeld = detail.sharesHeld,
                shareValue = detail.shareValue,
                sparklineData = detail.sparklineData,
                transactions = detail.transactions,
                filteredTransactions = detail.transactions.filter { entry -> selectedFilter.matches(entry) },
                hasNextPage = detail.hasNextPage,
                currentOffset = 0,
                error = null,
            )
        }
    }

    private fun applyError(networkError: NetworkError) {
        val mapped = networkError.toMemberSavingsError()
        reportLoadError(mapped, networkError)
        updateState { copy(isLoading = false, isRefreshing = false, error = mapped) }
    }

    private fun reportLoadError(mapped: MemberSavingsError, networkError: NetworkError) {
        if (mapped == MemberSavingsError.Auth) {
            crashReporter.recordMessage(
                message = "member-savings-detail: session expired (401) groupId=$groupId memberId=$memberId " +
                    "— clearing session",
                level = CrashSeverity.Warning,
            )
            sessionManager.endSession()
            sendEvent(MemberSavingsDetailEvent.ShowSnackbar(message = MemberSavingsError.Auth.messageKey))
        } else {
            crashReporter.recordException(
                throwable = IllegalStateException("member-savings-detail fetch failed: $networkError"),
                message = "member-savings-detail: load failed groupId=$groupId memberId=$memberId " +
                    "networkError=$networkError",
            )
        }
    }

    /**
     * Disambiguates the transport-level [NetworkError] onto [MemberSavingsError] —
     * `ui.yaml#state_model.errors.types` declares 4 members, and — unlike
     * `LoanDetailError.NotFound`'s currently-unreachable regex-based 404 detection — [NetworkError]
     * carries a DIRECT [NetworkError.NOT_FOUND] member reachable straight off [SavingsRepository]'s
     * plain [NetworkResult] contract (no `categorize()` exception-message parsing needed), so
     * [MemberSavingsError.NotFound] IS reachable in production today. Mirrors
     * `PersonalSavingsViewModel.toSavingsError`'s identical mapping shape for the other 6 values.
     */
    private fun NetworkError.toMemberSavingsError(): MemberSavingsError = when (this) {
        NetworkError.REQUEST_TIMEOUT -> MemberSavingsError.Network
        NetworkError.NOT_FOUND -> MemberSavingsError.NotFound
        NetworkError.UNAUTHORIZED, NetworkError.TOO_MANY_REQUESTS -> MemberSavingsError.Auth
        NetworkError.BAD_REQUEST, NetworkError.SERVER, NetworkError.SERIALIZATION, NetworkError.UNKNOWN ->
            MemberSavingsError.Server
    }
}
