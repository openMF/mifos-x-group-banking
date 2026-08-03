/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.savingsdashboard

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitor
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
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
import org.mifos.groupbanking.core.model.GroupSavingsSummary
import org.mifos.groupbanking.core.model.GroupTypeConfig
import org.mifos.groupbanking.core.model.IndividualSavingsSummary
import org.mifos.groupbanking.core.model.SavingsDashboardSummary
import org.mifos.groupbanking.core.model.SavingsDashboardTab
import org.mifos.groupbanking.core.model.WeeklyContributionPoint
import kotlin.time.Clock

/**
 * MVI stack (State/Event/Action/ViewModel/DI) for the `savings-dashboard` feature — see
 * API.md#viewmodel / #state / #actions / #events / #di for the full generated-symbol contract.
 */
private const val TAG = "SavingsDashboardViewModel"

/**
 * Screen-level render state for `savings-dashboard-screen` — verbatim mirror of
 * `ui.yaml#state_model.SavingsDashboardViewModel.screen_state.members` (4 members). Derived only
 * (not stored) via [SavingsDashboardState.deriveScreenState] — same convention as
 * `MemberSavingsDetailState.deriveScreenState`. There is no dedicated `ContentWithError` member —
 * `ui.yaml#states.content_with_error` ("cached data with error banner") maps onto [Content] with
 * [SavingsDashboardState.error] non-null; the Screen layer renders `error_banner` conditionally
 * (`visible_when: error != null`), same fold-in convention `LoanListState` uses. See API.md#state.
 */
@Serializable
sealed interface SavingsDashboardScreenState {
    @Serializable
    data object Loading : SavingsDashboardScreenState

    @Serializable
    data object Content : SavingsDashboardScreenState

    @Serializable
    data object Empty : SavingsDashboardScreenState

    @Serializable
    data object Error : SavingsDashboardScreenState
}

/**
 * MVI state for `SavingsDashboardViewModel`. Field set + defaults are a verbatim mirror of
 * `ui.yaml#state_model.SavingsDashboardViewModel.state.fields`, with two deliberate type
 * corrections and one honest gap (all documented, none silently applied — per
 * RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1):
 *
 * **[selectedTab] type — confirmed idea-layer drift, corrected to the real registry type:**
 * `ui.yaml#state.fields.selectedTab` declares `type: SavingsTab`, but the bare name `SavingsTab`
 * is TAKEN in `core/model/Savings.kt` by personal-savings' own GROUP_LINKED/INDIVIDUAL
 * account-selector enum. This screen's own value-set (`GROUP`/`INDIVIDUAL`, matching
 * `api.yaml#dtos.SavingsTab`) is exposed under the distinctly-named [SavingsDashboardTab] instead
 * — see that enum's own KDoc "naming-collision note". Flagged for the cross-feature repair
 * station — `ui.yaml` should reference `SavingsDashboardTab` directly.
 *
 * **[contributionModel] value-set — confirmed idea-layer drift, derived from the real registry:**
 * `ui.yaml#state.fields.contributionModel` comments describe the value-set as
 * `SHARE_BASED_VARIABLE | FIXED_AMOUNT | FIXED_NEGOTIATED`, but the actual DTO-registry enum
 * backing [GroupTypeConfig.contributionMode] is [org.mifos.groupbanking.core.model.ContributionMode]
 * — `SHARE_BASED_VARIABLE | FIXED | MINIMAL | UNKNOWN` (`GroupTypeConfig.kt`). Per the registry's
 * "registry wins" precedent this field is seeded from `typeConfig.contributionMode.name` (the
 * real 4-value set), NOT the ui.yaml comment's 3-value set — same documented correction
 * `MemberSavingsDetailState.contributionModel` makes. Flagged for the cross-feature repair
 * station.
 *
 * **[lastSyncAt] — confirmed idea-layer gap, honest client-observed timestamp, never
 * fabricated:** neither [GroupSavingsSummary] nor [IndividualSavingsSummary] carries a
 * server-provided "last synced" field (`api.yaml`/`data-flow.yaml` declare no such response
 * field) — `sync_band`'s `visible_when: lastSyncAt != null` therefore reflects the WALL-CLOCK
 * time this device last completed a successful [SavingsRepository.loadSavingsDashboard] call
 * ([kotlin.time.Clock], stamped in [applySummary]), not a server value. Flagged for the
 * cross-feature repair station — either `ui.yaml` should be corrected to document this as
 * client-observed, or the companion API should add a real `lastSyncAt`/`updatedAt` field.
 *
 * [typeConfig], [groupSavingsSummary], [individualSavingsSummary], [weeklyTrend], and [error] are
 * `@Transient` — always re-derived from [SavingsRepository] (or the nav-param [typeConfig]) on
 * (re)construction, never restored from a `@Serializable` snapshot — mirrors every other
 * dashboard/detail ViewModel's identical `@Transient` convention for non-serializable domain
 * payloads.
 */
@Serializable
@Immutable
data class SavingsDashboardState(
    val selectedTab: SavingsDashboardTab = SavingsDashboardTab.GROUP,
    val groupId: String = "",
    @Transient
    val typeConfig: GroupTypeConfig? = null,
    val contributionModel: String = "",
    @Transient
    val groupSavingsSummary: GroupSavingsSummary? = null,
    @Transient
    val individualSavingsSummary: IndividualSavingsSummary? = null,
    @Transient
    val weeklyTrend: List<WeeklyContributionPoint> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    @Transient
    val error: String? = null,
    val lastSyncAt: String? = null,
    val cycleTarget: Long = 0L,
    val cycleCollected: Long = 0L,
)

/**
 * Derived, single-source-of-truth screen state — see [SavingsDashboardScreenState] KDoc.
 * [Empty] reads BOTH tabs' `memberRows` (not the raw totals) — matches
 * `ui.yaml#states.empty.description`: "No savings data" means neither tab has a single
 * contributing member row yet.
 */
fun SavingsDashboardState.deriveScreenState(): SavingsDashboardScreenState = when {
    isLoading -> SavingsDashboardScreenState.Loading
    error != null && groupSavingsSummary == null && individualSavingsSummary == null -> SavingsDashboardScreenState.Error
    groupSavingsSummary?.memberRows.isNullOrEmpty() && individualSavingsSummary?.memberRows.isNullOrEmpty() ->
        SavingsDashboardScreenState.Empty
    else -> SavingsDashboardScreenState.Content
}

/**
 * One-shot side effects emitted by `SavingsDashboardViewModel` — verbatim mirror of
 * `ui.yaml#state_model.SavingsDashboardViewModel.events.members` (2 members). See API.md#events.
 */
sealed interface SavingsDashboardEvent {
    data class NavigateToMemberDetail(val memberId: String, val groupId: String, val typeConfig: GroupTypeConfig) :
        SavingsDashboardEvent
    data class ShowError(val message: String) : SavingsDashboardEvent
}

/**
 * User intents dispatched to `SavingsDashboardViewModel`. The 4 top-level members are a verbatim
 * mirror of `ui.yaml#state_model.SavingsDashboardViewModel.actions.members` —
 * RULE-IMPL-DEAD-CLICKABLE-001 Rule 1. [Internal] is the sanctioned async-result-routing
 * sub-interface (never a user intent) per `training-layer/TRAINING_MASTER.yaml#patterns.actions`
 * — mirrors `MemberSavingsDetailAction.Internal`.
 *
 * **[RefreshDashboard] is dual-purpose (confirmed, not a drift):** `ui.yaml` declares this single
 * action for BOTH "Pull-to-refresh" (`state_model.actions.members[1].trigger`) AND the
 * `error_banner`'s retry-button tap (`components.error_banner.action.on_click.action:
 * RefreshDashboard`) — `handleRefreshDashboard` below satisfies both call sites: it gates on
 * `NetworkMonitor` (the retry button's declared `action_contract.library_refs:
 * [cmp-network-monitor]`) before re-fetching.
 *
 * **[OpenMemberDetail] param — mirrors the declared action shape, not every on_click site's
 * extra param:** `ui.yaml#components.individual_member_row.on_click.params` additionally passes
 * `savings_type: INDIVIDUAL`, but `state_model.actions.members[OpenMemberDetail].params` declares
 * only `memberId: String`, and `state_model.events.members[NavigateToMemberDetail].params`
 * likewise declares only `memberId`/`groupId`/`typeConfig` (no `savingsType`) — mirrored verbatim
 * here per RULE-IMPL-DEAD-CLICKABLE-001 Rule 1. Flagged for the cross-feature repair station /
 * RULE-IDEA-ACTION-CONTRACT-001 if `member-savings-detail` later needs the tab-of-origin signal.
 */
sealed interface SavingsDashboardAction {
    data object LoadDashboard : SavingsDashboardAction
    data object RefreshDashboard : SavingsDashboardAction
    data class SelectTab(val tab: SavingsDashboardTab) : SavingsDashboardAction
    data class OpenMemberDetail(val memberId: String) : SavingsDashboardAction

    /** Async coroutine result — routed via `trySendAction`, never dispatched by the UI. */
    sealed interface Internal : SavingsDashboardAction {
        /** `LoadDashboard` (on_mount) — first parallel group+individual read. */
        data class DashboardLoaded(val result: NetworkResult<SavingsDashboardSummary, NetworkError>) : Internal

        /** `RefreshDashboard` (pull-to-refresh / retry-button) — bypass_and_refresh re-fetch. */
        data class RefreshResult(val result: NetworkResult<SavingsDashboardSummary, NetworkError>) : Internal
    }
}

/**
 * MVI processor for the contribution-model-aware, tabbed group/individual savings dashboard
 * (`business_logic.kind: composite` per ui.yaml) — the SP-04 AC-7 analytics/crashReporter
 * injection pair IS mandatory here per RULE-IDEA-IMPL-INTELLIGENCE-001 AC-03i (`composite` is
 * outside `{crud, nav_only}`). No `FieldEncryptor` (`core-base/security`) is injected — neither
 * [GroupSavingsSummary] nor [IndividualSavingsSummary] nor their member-row shapes carry a
 * `@PII`-marked field per the SP-02 idea-layer schema (member `name` is a display label already
 * surfaced group-wide during meetings, not treated as PII by this schema — same non-PII
 * classification `MemberGroupSavingsRow`/`MemberIndividualSavingsRow` get elsewhere in this
 * codebase).
 *
 * [repository] is consumed directly via [NetworkResult] — [SavingsRepository]'s own KDoc "Store5
 * branch" note (no `AppStoreRegistry.Savings` entry yet), same branch as
 * `MemberSavingsDetailViewModel`/`PersonalSavingsViewModel`. The on_mount and pull-to-refresh /
 * retry reads both go through the SAME composite [SavingsRepository.loadSavingsDashboard] call
 * (parallel group+individual fetch, `data-flow.yaml#entries[0]`: "Both calls run in parallel on
 * mount") — [SelectTab] never triggers a standalone [SavingsRepository.getGroupSavingsSummary] /
 * [SavingsRepository.getIndividualSavingsSummary] call, per `data-flow.yaml#entries[SelectTab]`:
 * "individual-tab data was already fetched in parallel on mount... Pure client-side
 * transform_state" (this SUPERSEDES the older `ui.yaml#business_logic.description` prose "Group
 * tab loaded first by default; Individual tab lazy-loads on tab switch" — the more specific,
 * newer `data-flow.yaml` entry wins, same drift-resolution precedent
 * `PersonalSavingsViewModel`'s DEVELOPMENT.md gap (1) documents).
 *
 * **DI drift (flagged, not silently resolved):** `ui.yaml#state_model.di` declares
 * `[SavingsRepository, NavigationManager, ConnectivityObserver, LocalSavingsDao]`, none of which
 * except `SavingsRepository` are real symbols in this codebase — navigation flows through
 * [sendEvent] (never a `NavigationManager`), `ConnectivityObserver` is the real
 * `io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitor` (`cmp-network-monitor`,
 * matching `error_banker.action.on_click.action_contract.library_refs`), and `LocalSavingsDao` is
 * not directly injected — [SavingsRepository]'s own "Store5 branch" note is explicit that offline
 * caching is not yet wired through this repository. [sessionManager] is ALSO injected here (not
 * declared in `ui.yaml#state_model.di` at all) purely to call `endSession()` for real on the 401
 * branch — the exact same addition `MemberSavingsDetailViewModel`/`LoanListViewModel` make for
 * their own undeclared 401 path. All three drifts are reported to the caller for an idea-layer
 * `ui.yaml#state_model.di` correction rather than invented silently.
 *
 * [groupId]/[typeConfig] are the `ui.yaml#nav_params` values forwarded from `group-dashboard`'s
 * `OnViewSavings` entry point via Koin `parametersOf(...)` (see `di.SavingsDashboardModule`);
 * [SavingsDashboardState.contributionModel] is derived from [typeConfig] immediately at
 * construction (no need to wait for the network read — see [SavingsDashboardState] KDoc
 * "contributionModel value-set" note).
 *
 * **[LoadDashboard] is dispatched by the Screen, not auto-fired in `init`:**
 * `ui.yaml#state_model.actions.members[0]` declares `LoadDashboard` with `trigger: "Screen enters
 * composition"` — the (not-yet-generated) `SavingsDashboardScreen.kt` is expected to dispatch it
 * from a `LaunchedEffect(Unit)`, mirroring the explicit-action on_mount convention this ui.yaml
 * uses (as opposed to `MemberSavingsDetailViewModel`'s implicit `init { loadInitial() }`
 * convention, which has no matching `state_model.actions.members` entry for its own on_mount).
 * `init` here only records the crash-reporter breadcrumb and the `analytics` view event.
 *
 * See API.md#viewmodel.
 */
internal class SavingsDashboardViewModel(
    private val repository: SavingsRepository,
    private val networkMonitor: NetworkMonitor,
    private val sessionManager: SessionManager,
    private val crashReporter: CrashReporter,
    private val analytics: KptAnalyticsTracker,
    private val groupId: String,
    private val typeConfig: GroupTypeConfig,
) : BaseViewModel<SavingsDashboardState, SavingsDashboardEvent, SavingsDashboardAction>(
    initialState = SavingsDashboardState(
        groupId = groupId,
        typeConfig = typeConfig,
        contributionModel = typeConfig.contributionMode.name,
    ),
) {

    init {
        crashReporter.recordMessage(
            message = "feature=savings-dashboard screen=savings-dashboard-screen " +
                "groupId=$groupId contributionModel=${typeConfig.contributionMode}",
            level = CrashSeverity.Debug,
        )
        analytics.trackSavingsOperation(operation = "view", accountId = groupId)
    }

    override fun handleAction(action: SavingsDashboardAction) {
        when (action) {
            SavingsDashboardAction.LoadDashboard -> handleLoadDashboard()
            SavingsDashboardAction.RefreshDashboard -> handleRefreshDashboard()
            is SavingsDashboardAction.SelectTab -> handleSelectTab(action.tab)
            is SavingsDashboardAction.OpenMemberDetail -> handleOpenMemberDetail(action.memberId)
            is SavingsDashboardAction.Internal.DashboardLoaded -> handleDashboardLoaded(action.result)
            is SavingsDashboardAction.Internal.RefreshResult -> handleRefreshResult(action.result)
        }
    }

    // -- on_mount (data-flow.yaml — parallel GET group+individual savings summaries) ------------

    private fun handleLoadDashboard() {
        Logger.i(TAG) { "LoadDashboard dispatched groupId=$groupId" }
        updateState { copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = repository.loadSavingsDashboard(groupId)
            trySendAction(SavingsDashboardAction.Internal.DashboardLoaded(result))
        }
    }

    // -- Tab tap (ui.yaml effect: transform_state — client-side, no network) --------------------

    private fun handleSelectTab(tab: SavingsDashboardTab) {
        Logger.d(TAG) { "tab switched to=$tab groupId=$groupId" }
        updateState { copy(selectedTab = tab) }
    }

    // -- Member row tap (ui.yaml effect: navigate) -----------------------------------------------

    private fun handleOpenMemberDetail(memberId: String) {
        Logger.i(TAG) { "member row tapped memberId=$memberId groupId=$groupId tab=${state.selectedTab}" }
        analytics.trackSavingsOperation(operation = "view_member", accountId = memberId)
        sendEvent(
            SavingsDashboardEvent.NavigateToMemberDetail(
                memberId = memberId,
                groupId = groupId,
                typeConfig = typeConfig,
            ),
        )
    }

    // -- Pull-to-refresh / error-banner retry (ui.yaml effect: call_api, library_refs: [cmp-network-monitor]) --

    private fun handleRefreshDashboard() {
        if (!networkMonitor.isOnline.value) {
            Logger.w(TAG) { "refresh attempted while offline groupId=$groupId" }
            crashReporter.recordMessage(
                message = "savings-dashboard: refresh attempted while offline groupId=$groupId",
                level = CrashSeverity.Info,
            )
            updateState { copy(isRefreshing = false, error = "error_offline") }
            sendEvent(SavingsDashboardEvent.ShowError(message = "error_offline"))
            return
        }
        Logger.i(TAG) { "RefreshDashboard dispatched groupId=$groupId" }
        analytics.trackSavingsOperation(operation = "refresh", accountId = groupId)
        updateState { copy(isRefreshing = true) }
        viewModelScope.launch {
            val result = repository.loadSavingsDashboard(groupId)
            trySendAction(SavingsDashboardAction.Internal.RefreshResult(result))
        }
    }

    // -- LoadDashboard result routing -------------------------------------------------------------

    private fun handleDashboardLoaded(result: NetworkResult<SavingsDashboardSummary, NetworkError>) {
        when (result) {
            is NetworkResult.Success -> applySummary(result.data)
            is NetworkResult.Error -> applyError(result.error)
        }
    }

    // -- RefreshDashboard result routing -----------------------------------------------------------

    private fun handleRefreshResult(result: NetworkResult<SavingsDashboardSummary, NetworkError>) {
        when (result) {
            is NetworkResult.Success -> applySummary(result.data)
            is NetworkResult.Error -> applyError(result.error)
        }
    }

    // -- Shared success/error appliers -------------------------------------------------------------

    /**
     * Shared by `LoadDashboard` and `RefreshDashboard` — full replace of both tabs' summaries.
     * [weeklyTrend] prefers the group summary's series (same `{weekLabel, groupAmount,
     * individualAmount}` point shape both endpoints return — see [SavingsDashboardState] KDoc),
     * falling back to the individual summary's series only if the group one came back empty.
     */
    private fun applySummary(summary: SavingsDashboardSummary) {
        val syncedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).toString()
        Logger.i(TAG) {
            "dashboard loaded groupId=$groupId groupMembers=${summary.group.memberRows.size} " +
                "individualMembers=${summary.individual.memberRows.size} cycleCollected=${summary.group.cycleCollected}"
        }
        updateState {
            copy(
                isLoading = false,
                isRefreshing = false,
                groupSavingsSummary = summary.group,
                individualSavingsSummary = summary.individual,
                weeklyTrend = summary.group.weeklyTrend.ifEmpty { summary.individual.weeklyTrend },
                cycleTarget = summary.group.cycleTarget,
                cycleCollected = summary.group.cycleCollected,
                lastSyncAt = syncedAt,
                error = null,
            )
        }
    }

    private fun applyError(networkError: NetworkError) {
        val messageKey = networkError.toSavingsDashboardErrorMessageKey()
        reportLoadError(messageKey, networkError)
        updateState { copy(isLoading = false, isRefreshing = false, error = messageKey) }
    }

    private fun reportLoadError(messageKey: String, networkError: NetworkError) {
        if (networkError == NetworkError.UNAUTHORIZED || networkError == NetworkError.TOO_MANY_REQUESTS) {
            crashReporter.recordMessage(
                message = "savings-dashboard: session expired (401) groupId=$groupId — clearing session",
                level = CrashSeverity.Warning,
            )
            sessionManager.endSession()
        } else {
            crashReporter.recordException(
                throwable = IllegalStateException("savings-dashboard fetch failed: $networkError"),
                message = "savings-dashboard: load failed groupId=$groupId networkError=$networkError",
            )
        }
        sendEvent(SavingsDashboardEvent.ShowError(message = messageKey))
    }

    /**
     * Disambiguates the transport-level [NetworkError] onto the flat `error: String?` message-key
     * field `ui.yaml#state_model.state.fields.error` declares (this screen has no `errors.types`
     * taxonomy — only a single `errors.NetworkError` message, unlike
     * `MemberSavingsDetailError`'s 4-member sealed hierarchy). The 4 keys mirror
     * `data-flow.yaml#entries[on_mount].error_paths` 1:1: `401 -> error_auth` (session cleared,
     * see [reportLoadError]), `404 -> error_group_not_found`, `network.offline` (surfaced via
     * [NetworkError.REQUEST_TIMEOUT], the same transport-timeout branch every other ViewModel in
     * this codebase maps offline conditions onto) `-> error_offline`, `500 -> error_server`.
     */
    private fun NetworkError.toSavingsDashboardErrorMessageKey(): String = when (this) {
        NetworkError.NOT_FOUND -> "error_group_not_found"
        NetworkError.UNAUTHORIZED, NetworkError.TOO_MANY_REQUESTS -> "error_auth"
        NetworkError.REQUEST_TIMEOUT -> "error_offline"
        NetworkError.BAD_REQUEST, NetworkError.SERVER, NetworkError.SERIALIZATION, NetworkError.UNKNOWN -> "error_server"
    }
}
