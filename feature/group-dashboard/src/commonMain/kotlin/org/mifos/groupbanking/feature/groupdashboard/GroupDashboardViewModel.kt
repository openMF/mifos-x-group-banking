/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.groupdashboard

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
import org.mifos.groupbanking.core.data.repository.GroupDashboardRepository
import org.mifos.groupbanking.core.model.ActivityItem
import org.mifos.groupbanking.core.model.GroupAccounts
import org.mifos.groupbanking.core.model.GroupConfig
import org.mifos.groupbanking.core.model.GroupCorpus
import org.mifos.groupbanking.core.model.GroupDashboard
import org.mifos.groupbanking.core.model.GroupDetail
import org.mifos.groupbanking.core.model.GroupInstanceConfig
import org.mifos.groupbanking.core.model.SavingsMechanism

private const val TAG = "GroupDashboardViewModel"

/** ORGANIZER / CHAIRPERSON / TREASURER — mirrors `ui.yaml#management_actions_grid.visible`. */
private val MANAGEMENT_ROLES = setOf("ORGANIZER", "CHAIRPERSON", "TREASURER")

/** ORGANIZER / TREASURER only — mirrors `ui.yaml#share_out_button.enabled` (narrower than the
 * management-grid visibility set; CHAIRPERSON sees the button rendered but disabled). */
private val SHARE_OUT_ROLES = setOf("ORGANIZER", "TREASURER")

/**
 * Screen-level render state for `group-dashboard-screen` — verbatim mirror of
 * `ui.yaml#state_model.GroupDashboardViewModel.screen_state` (only 3 members declared — unlike
 * the list-style screens' `Loading/Content/Error/Empty`, a single group composite is never
 * "empty" once present: `GroupDashboardRepositoryImpl.groupDashboardStream` always predicates
 * `isEmpty = { false }`). Derived (not stored) from [GroupDashboardState.isLoading] /
 * [GroupDashboardState.error] via the [GroupDashboardState.screenState] extension below — exactly
 * one source of truth, mirroring `GroupListState`'s identical convention. See API.md#state.
 */
@Serializable
sealed interface GroupDashboardScreenState {
    @Serializable
    data object Loading : GroupDashboardScreenState

    @Serializable
    data object Content : GroupDashboardScreenState

    @Serializable
    data object Error : GroupDashboardScreenState
}

/**
 * Error taxonomy for the group-dashboard composite read — verbatim mirror of
 * `ui.yaml#state_model.GroupDashboardViewModel.errors.types`. [messageKey] is a composeResources
 * string-resource id (never a raw hardcoded English string, per
 * RULE-IMPL-NO-HARDCODED-STRING-001) resolved by the Screen layer.
 *
 * **[NotFound] reachability gap (flagged, not invented around):** `GroupDashboardStore`'s
 * `GroupDashboardFetchException(networkError: NetworkError)` message is
 * `"Group dashboard fetch failed: $networkError"` (e.g. `"...: NOT_FOUND"`) — it carries no
 * parseable 3-digit HTTP status code, so `kpt.core.base.store.error.categorize()`'s regex-based
 * HTTP-code extraction cannot classify it as `ErrorCategory.ClientError(404)`; it instead falls
 * through to `ErrorCategory.Generic`. [handleStreamUpdated] still checks for
 * `ErrorCategory.ClientError(httpCode = 404)` (future-proof — the mapping activates for free if
 * the exception message format is ever fixed upstream to embed the numeric code), but in
 * PRODUCTION TODAY a 404 (group not found / not a member) surfaces as [Server], not [NotFound].
 * Reported to the caller as a cross-layer follow-up (`GroupDashboardFetchException`'s message
 * shape, `core/store`) rather than reached into `core/store`'s `impl` package from this feature
 * module (poor layering) to work around it. See API.md#state.
 */
@Serializable
sealed interface GroupDashboardError {
    val retry: Boolean
    val messageKey: String

    @Serializable
    data object Network : GroupDashboardError {
        override val retry: Boolean = true
        override val messageKey: String = "error_network"
    }

    @Serializable
    data object Server : GroupDashboardError {
        override val retry: Boolean = true
        override val messageKey: String = "error_server"
    }

    @Serializable
    data object NotFound : GroupDashboardError {
        override val retry: Boolean = false
        override val messageKey: String = "error_not_found"
    }

    @Serializable
    data object Auth : GroupDashboardError {
        override val retry: Boolean = false
        override val messageKey: String = "error_auth"
    }
}

/**
 * MVI state for `GroupDashboardViewModel`. Field set is a verbatim mirror of
 * `ui.yaml#state_model.GroupDashboardViewModel.state`, with two documented type substitutions
 * (same class of divergence already flagged on [GroupDashboard]'s KDoc in
 * `core/model/GroupDashboard.kt`): ui.yaml pseudocodes `group: Group?` / `typeConfig:
 * GroupTypeConfig?`, but the actual companion-API response shapes are [GroupDetail] /
 * [GroupInstanceConfig] (both intentionally distinct from the group-list `Group` /
 * COMP-DT-003-catalogue `GroupTypeConfig` — see those classes' KDoc for the naming-collision
 * rationale).
 *
 * [group], [corpus], [config], [typeConfig], [accounts], [recentActivity], and [error] are
 * `@Transient` — all are always re-derived from
 * [GroupDashboardRepository.groupDashboardStream] on (re)subscription (offline-first cache via
 * `group_dashboard_cache`, so nothing is visually lost across process death — the Store, not this
 * transient render state, is the durable source). Mirrors `PersonalDashboardState`'s identical
 * `@Transient` convention for non-serializable domain payloads (see
 * `training-layer/TRAINING_MASTER.yaml#patterns.state_models`).
 *
 * [config] is CLIENT-SIDE CONSTRUCTED by [GroupInstanceConfig.toGroupConfig] (this file) rather
 * than by `GroupRepository` (contra `GroupConfig`'s own KDoc, which describes a repository-layer
 * merge with a separately-fetched COMP-DT-003 catalogue row) — every field except
 * [GroupConfig.shareMin]/[GroupConfig.shareMax]/[GroupConfig.minimumDisbursementThreshold] is
 * already present directly on [GroupDetail.typeConfig] ([GroupInstanceConfig]), so no second
 * fetch is needed; those three fields have **no wire source anywhere in `api.yaml`** (confirmed
 * gap, see [GroupConfig]'s own
 * KDoc) and are always `null` — [isCorpusInsufficient] is therefore defensively `false` until the
 * backend adds `minimumDisbursementThreshold`.
 *
 * [isCycleEnd] is now backed by the server-computed [GroupCorpus.isCycleEnd] wire field
 * (`get_group_corpus.isCycleEnd`, `api.yaml`) — [handleStreamUpdated] reads it directly from the
 * corpus section rather than the legacy `flow.yaml#compute_derived_state` formula
 * (`group.cycleWeek == group.cycleLengthWeeks`, whose fields never existed on the domain model).
 * The Share-Out action gate ([handleShareOut]) is unblocked exactly when the server reports the
 * cycle has ended; `false` remains the conservative default (never falsely unblocks Share-Out).
 *
 * See API.md#state.
 */
@Serializable
@Immutable
data class GroupDashboardState(
    val isLoading: Boolean = true,
    @Transient
    val group: GroupDetail? = null,
    @Transient
    val corpus: GroupCorpus? = null,
    @Transient
    val config: GroupConfig? = null,
    @Transient
    val typeConfig: GroupInstanceConfig? = null,
    val groupTypeName: String = "",
    val viewerRole: String = "MEMBER",
    @Transient
    val accounts: GroupAccounts? = null,
    @Transient
    val recentActivity: List<ActivityItem> = emptyList(),
    val isCorpusInsufficient: Boolean = false,
    val isCycleEnd: Boolean = false,
    val rotationPosition: Int? = null,
    val nextRecipientName: String? = null,
    val nextRecipientPosition: Int? = null,
    val shareOutProjection: Double? = null,
    /** G13 — top-bar overflow (more_vert) dropdown open state (`ui.yaml#state.isMoreMenuExpanded`). */
    val isMoreMenuExpanded: Boolean = false,
    @Transient
    val error: GroupDashboardError? = null,
)

/** Derived, single-source-of-truth screen state — see [GroupDashboardScreenState] KDoc. */
val GroupDashboardState.screenState: GroupDashboardScreenState
    get() = when {
        error != null -> GroupDashboardScreenState.Error
        isLoading -> GroupDashboardScreenState.Loading
        else -> GroupDashboardScreenState.Content
    }

/**
 * One-shot side effects emitted by `GroupDashboardViewModel`. [NavigateToMeetingCalendar],
 * [NavigateToMemberList], [NavigateToLoanList], [NavigateToShareOut], [ShowCorpusBlockedDialog],
 * and [ShowSnackbar] are a verbatim mirror of
 * `ui.yaml#state_model.GroupDashboardViewModel.events.members`.
 *
 * **Two additions, flagged (not silently invented) — both real implementations, not stubs:**
 * - [NavigateBack] — `ui.yaml#components.top_bar.on_navigation_click` wires the declared `OnBack`
 *   action (`state_model.actions.members` includes it) with a fully-authored `action_contract`
 *   (`effect: navigate`, "Pops this screen from the back-stack"), but `events.members` does not
 *   declare a matching navigate event. Mirrors `GroupTypePickerEvent.NavigateBack` /
 *   `JoinWithCodeEvent.NavigateBack`'s identical precedent in this codebase. Leaving `OnBack` as a
 *   true no-op would be a dead back button (RULE-IMPL-DEAD-CLICKABLE-001) — implemented for real.
 * - [NavigateToSavingsDashboard] (G9, idea-evolve 2026-08-01) — `ui.yaml#components
 *   .quick_actions_section.member_actions_grid.view_savings_button.on_click` wires the declared
 *   `OnViewSavings` action to `target: savings-dashboard` (self-scoped, `params: { groupId,
 *   typeConfig }`), replacing the prior `member-savings-detail` target that needed a `memberId`
 *   this dashboard does not carry. This dashboard holds a per-group
 *   [org.mifos.groupbanking.core.model.GroupInstanceConfig], NOT the COMP-DT-003 catalogue
 *   `GroupTypeConfig` that savings-dashboard's `typeConfig` nav-param declares (the documented
 *   `GroupTypeConfig`-vs-instance drift), so the event carries only [groupId]; savings-dashboard's
 *   `typeConfig` nav-param degrades to its default at this seam (contribution-model-adaptive text
 *   only) and the screen re-renders totals/tabs/rows from its own real load — see
 *   `SavingsDashboardRoute` KDoc "drift bridge". `NavigateToMeetingCalendar` also mirrors this.
 *
 * [NavigateToSettings] / [NavigateToSyncStatus] (G13) close the top-bar overflow menu items.
 * See API.md#events.
 */
sealed interface GroupDashboardEvent {
    data class NavigateToMeetingCalendar(val groupId: String) : GroupDashboardEvent
    data class NavigateToMemberList(val groupId: String) : GroupDashboardEvent
    data class NavigateToLoanList(val groupId: String) : GroupDashboardEvent
    data class NavigateToShareOut(val groupId: String, val distributionStrategy: String) : GroupDashboardEvent

    /** G9 — MEMBER "My Savings" self-scoped to the group's savings dashboard (see class KDoc). */
    data class NavigateToSavingsDashboard(val groupId: String) : GroupDashboardEvent

    /** G13 — top-bar overflow menu → shared settings screen. */
    data object NavigateToSettings : GroupDashboardEvent

    /** G13 — top-bar overflow menu → shared offline sync-status dashboard. */
    data object NavigateToSyncStatus : GroupDashboardEvent
    data object ShowCorpusBlockedDialog : GroupDashboardEvent
    data class ShowSnackbar(val message: String) : GroupDashboardEvent

    /** Flagged addition — see class KDoc "Two additions". */
    data object NavigateBack : GroupDashboardEvent
}

/**
 * User intents dispatched to `GroupDashboardViewModel`. The 11 top-level members are a verbatim
 * mirror of `ui.yaml#state_model.GroupDashboardViewModel.actions.members` (incl. the G13
 * `OnMoreOptions` / `OnGroupSettings` / `OnSyncStatus` overflow-menu actions) —
 * RULE-IMPL-DEAD-CLICKABLE-001 Rule 1. Several ui.yaml components share the SAME declared action
 * across both role-gated grids (e.g. `view_meetings_button` in `member_actions_grid` also
 * dispatches [OnStartMeeting], `view_loans_member_button` also dispatches [OnViewLoans]) — the
 * role-adaptive branching lives inside the handler, not a second action member, per the shared
 * top_bar `OnMoreOptions` precedent documented below.
 *
 * **G13 (idea-evolve 2026-08-01):** `ui.yaml#components.top_bar.actions[0]` now declares
 * [OnMoreOptions] (effect: `transform_state`, toggles [GroupDashboardState.isMoreMenuExpanded]) plus
 * the overflow menu items [OnGroupSettings] (→ settings) / [OnSyncStatus] (→ sync-status), all
 * present in `state_model.actions.members`. The dropdown open state is now REAL ViewModel state
 * (`isMoreMenuExpanded`), not a Screen-local `remember` toggle.
 * [Internal] is the sanctioned async-result-routing sub-interface (never a user intent) per
 * `training-layer/TRAINING_MASTER.yaml#patterns.actions` — mirrors `GroupListAction.Internal`.
 * See API.md#actions.
 */
sealed interface GroupDashboardAction {
    data object OnStartMeeting : GroupDashboardAction
    data object OnViewMembers : GroupDashboardAction
    data object OnViewLoans : GroupDashboardAction
    data object OnShareOut : GroupDashboardAction
    data object OnViewSavings : GroupDashboardAction
    data object OnMoreOptions : GroupDashboardAction
    data object OnGroupSettings : GroupDashboardAction
    data object OnSyncStatus : GroupDashboardAction
    data object OnRefresh : GroupDashboardAction
    data object Retry : GroupDashboardAction
    data object OnBack : GroupDashboardAction

    /** Async stream emissions — routed via `trySendAction`, never dispatched by the UI. */
    sealed interface Internal : GroupDashboardAction {
        data class StreamUpdated(val screenState: ScreenState<GroupDashboard>) : Internal
    }
}

/**
 * Pure derivation of `isCorpusInsufficient` (`ui.yaml#business_logic` / legacy
 * `flow.yaml#compute_derived_state`: `corpus.currentBalance < config.minimumDisbursementThreshold`).
 * Exposed as a top-level `internal` function (rather than inlined into [handleStreamUpdated]) so
 * the formula is independently unit-testable — see [GroupDashboardState]'s class KDoc for why
 * [GroupConfig.minimumDisbursementThreshold] is always `null` in production today (confirmed
 * wire-source gap), which makes this always evaluate `false` end-to-end until the backend adds
 * the field. The formula itself, and the [GroupDashboardEvent.ShowCorpusBlockedDialog]-emission
 * wiring on `handleStartMeeting` that consumes it, are both implemented for real.
 */
internal fun isCorpusInsufficient(corpus: GroupCorpus, config: GroupConfig): Boolean {
    val threshold = config.minimumDisbursementThreshold ?: return false
    return corpus.currentBalance < threshold
}

/**
 * Client-side construction of the `savings_summary_card`-backing [GroupConfig] from the
 * per-group [GroupInstanceConfig] already embedded on `get_group`'s response — see
 * [GroupDashboardState]'s class KDoc for why no second (catalogue) fetch is needed for any field
 * except the three with a confirmed wire-source gap ([GroupConfig.shareMin] /
 * [GroupConfig.shareMax] / [GroupConfig.minimumDisbursementThreshold]).
 */
private fun GroupInstanceConfig.toGroupConfig(): GroupConfig = GroupConfig(
    shareValue = shareValue,
    shareMin = null,
    shareMax = null,
    contributionAmount = contributionAmount,
    loanMultiplier = loanMultiplier,
    interestRate = interestRate,
    cycleLengthMonths = cycleLengthMonths,
    fineAmount = fineAmount,
    minimumDisbursementThreshold = null,
)

/**
 * MVI processor for the group-dashboard screen (`business_logic.kind: composite` per ui.yaml — a
 * client-side parallel fan-in of 4 companion reads via
 * [GroupDashboardRepository.groupDashboardStream]'s offline-first [ScreenDataStream], so the
 * SP-04 AC-7 analytics/crashReporter injection pair applies (composite is NOT `crud`/`nav_only`,
 * per RULE-IDEA-IMPL-INTELLIGENCE-001 AC-03i) — `FieldEncryptor` (`core-base/security`) is
 * intentionally NOT injected: there is no `pii_columns` entry on `data-flow.yaml` for this screen
 * and the dashboard is read-only (no local PII persistence).
 *
 * [sessionManager] is declared in `ui.yaml#state_model.di` and handles the real
 * `ScreenState.Unauthenticated` -> `sessionManager.endSession()` branch in
 * [handleStreamUpdated] — mirrors `PersonalDashboardViewModel`'s identical `SessionManager`
 * wiring. `NetworkMonitor` is ALSO declared in `ui.yaml#state_model.di` but is not injected here
 * directly — it is already composed inside `GroupDashboardRepositoryImpl.groupDashboardStream`
 * (`Store.asScreenStream(networkMonitor = ..., ...)`), matching `GroupListViewModel`'s /
 * `PersonalDashboardViewModel`'s identical precedent of not re-injecting it at the ViewModel
 * layer. `ui.yaml#state_model.di` also separately lists `GroupRepository` / `CorpusRepository` /
 * `RoleRepository` — the shipped data layer instead exposes ONE composite
 * [GroupDashboardRepository] (`groupDashboardStream`) that already fans the four companion reads
 * in parallel (see `GroupDashboardStore.kt` KDoc), so those three names never materialized as
 * separate injectable repositories; flagged as a documentation-vs-implementation drift for the
 * caller, not re-created here.
 *
 * [groupId] and the constructor `viewerRole` are the `ui.yaml#nav_params` (`groupId`,
 * `viewerRole`) forwarded from `group-list` / `group-create` via Koin `parametersOf(groupId,
 * viewerRole)` (see `di.GroupDashboardModule`) — `viewerRole` only SEEDS
 * [GroupDashboardState.viewerRole] for immediate role-gated rendering before the stream resolves;
 * [handleStreamUpdated] reconciles it with the server-confirmed
 * `GroupDashboard.viewerRole.role` once Content arrives (a role promotion/demotion since the
 * `group-list` fetch must win — `data-flow.yaml#on_refresh` notes explicitly call this out).
 *
 * See API.md#viewmodel.
 */
internal class GroupDashboardViewModel(
    private val repository: GroupDashboardRepository,
    private val sessionManager: SessionManager,
    private val crashReporter: CrashReporter,
    private val analytics: KptAnalyticsTracker,
    private val groupId: String,
    viewerRole: String,
) : BaseViewModel<GroupDashboardState, GroupDashboardEvent, GroupDashboardAction>(
    initialState = GroupDashboardState(viewerRole = viewerRole),
) {

    /** Fixed-key offline-first stream for [groupId] — see class KDoc. */
    private val dashboardStream: ScreenDataStream<GroupDashboard> =
        repository.groupDashboardStream(groupId = groupId, scope = viewModelScope)

    init {
        crashReporter.recordMessage(
            message = "feature=group-dashboard screen=group-dashboard-screen groupId=$groupId",
            level = CrashSeverity.Debug,
        )
        analytics.trackGroupOperation(operation = "view", groupId = groupId)
        viewModelScope.launch {
            dashboardStream.state.collect { screenState ->
                trySendAction(GroupDashboardAction.Internal.StreamUpdated(screenState))
            }
        }
    }

    override fun handleAction(action: GroupDashboardAction) {
        when (action) {
            GroupDashboardAction.OnStartMeeting -> handleStartMeeting()
            GroupDashboardAction.OnViewMembers -> handleViewMembers()
            GroupDashboardAction.OnViewLoans -> handleViewLoans()
            GroupDashboardAction.OnShareOut -> handleShareOut()
            GroupDashboardAction.OnViewSavings -> handleViewSavings()
            GroupDashboardAction.OnMoreOptions -> handleMoreOptions()
            GroupDashboardAction.OnGroupSettings -> handleGroupSettings()
            GroupDashboardAction.OnSyncStatus -> handleSyncStatus()
            GroupDashboardAction.OnRefresh -> handleRefresh()
            GroupDashboardAction.Retry -> handleRetry()
            GroupDashboardAction.OnBack -> handleBack()
            is GroupDashboardAction.Internal.StreamUpdated -> handleStreamUpdated(action.screenState)
        }
    }

    // -- Start Meeting (management) / View Meetings (member) — shared action, role-adaptive -------
    // ui.yaml: management roles get a corpus-sufficiency pre-check + ShowCorpusBlockedDialog;
    // member role navigates straight through (read-only meeting history/schedule).

    private fun handleStartMeeting() {
        val role = state.viewerRole
        if (role in MANAGEMENT_ROLES) {
            analytics.trackGroupOperation(operation = "start_meeting", groupId = groupId)
            Logger.i(TAG) {
                "OnStartMeeting (management role=$role) groupId=$groupId " +
                    "isCorpusInsufficient=${state.isCorpusInsufficient}"
            }
            if (state.isCorpusInsufficient) {
                // Inline warning alongside navigation — matches legacy flow.yaml#on_start_meeting
                // ("if_insufficient: emit NavigateToMeetingCalendar (corpus warning shown
                // inline)"): both effects fire, the dialog does not block the navigate.
                sendEvent(GroupDashboardEvent.ShowCorpusBlockedDialog)
            }
        } else {
            analytics.trackGroupOperation(operation = "view_meetings", groupId = groupId)
            Logger.i(TAG) { "OnStartMeeting (read-only role=$role) groupId=$groupId — no corpus check" }
        }
        sendEvent(GroupDashboardEvent.NavigateToMeetingCalendar(groupId))
    }

    // -- View Members (both grids) — ui.yaml effect: navigate, target: member-list ------------------

    private fun handleViewMembers() {
        analytics.trackGroupOperation(operation = "view_members", groupId = groupId)
        Logger.i(TAG) { "OnViewMembers groupId=$groupId viewerRole=${state.viewerRole}" }
        sendEvent(GroupDashboardEvent.NavigateToMemberList(groupId))
    }

    // -- View Loans (both grids) — ui.yaml effect: navigate, target: loan-list -----------------------

    private fun handleViewLoans() {
        analytics.trackGroupOperation(operation = "view_loans", groupId = groupId)
        Logger.i(TAG) { "OnViewLoans groupId=$groupId viewerRole=${state.viewerRole}" }
        sendEvent(GroupDashboardEvent.NavigateToLoanList(groupId))
    }

    // -- Share-Out (management, ORGANIZER/TREASURER + cycle-end gated) -------------------------------

    private fun handleShareOut() {
        val role = state.viewerRole
        if (role !in SHARE_OUT_ROLES) {
            // Defensive guard only — `share_out_button.enabled` already restricts dispatch to
            // ORGANIZER/TREASURER; a tap from any other role can only reach here via an
            // accessibility-service bypass of the disabled Compose state, never the wired happy
            // path (RULE-IMPL-DEAD-CLICKABLE-001 Rule 2 — logged, not silently swallowed).
            Logger.w(TAG) { "OnShareOut dispatched by unauthorized role=$role groupId=$groupId — ignoring" }
            crashReporter.recordMessage(
                message = "group-dashboard: OnShareOut dispatched by unauthorized role=$role groupId=$groupId",
                level = CrashSeverity.Warning,
            )
            return
        }
        if (!state.isCycleEnd) {
            Logger.i(TAG) { "OnShareOut groupId=$groupId — cycle not yet ended" }
            sendEvent(GroupDashboardEvent.ShowSnackbar(message = "share_out_not_available"))
            return
        }
        val distributionStrategy = state.typeConfig?.shareoutFormula.orEmpty()
        analytics.trackGroupOperation(operation = "share_out", groupId = groupId)
        Logger.i(TAG) { "OnShareOut groupId=$groupId distributionStrategy=$distributionStrategy" }
        sendEvent(GroupDashboardEvent.NavigateToShareOut(groupId, distributionStrategy))
    }

    // -- View Savings (member, read-only) — G9: self-scoped to the group's savings-dashboard --------

    private fun handleViewSavings() {
        analytics.trackGroupOperation(operation = "view_savings", groupId = groupId)
        Logger.i(TAG) { "OnViewSavings groupId=$groupId — opening savings dashboard" }
        sendEvent(GroupDashboardEvent.NavigateToSavingsDashboard(groupId))
    }

    // -- Top-bar overflow menu (G13 — transform_state toggle + two navigate items) ------------------

    private fun handleMoreOptions() {
        Logger.i(TAG) { "OnMoreOptions groupId=$groupId — toggling overflow menu" }
        updateState { copy(isMoreMenuExpanded = !isMoreMenuExpanded) }
    }

    private fun handleGroupSettings() {
        Logger.i(TAG) { "OnGroupSettings groupId=$groupId — opening settings" }
        updateState { copy(isMoreMenuExpanded = false) }
        sendEvent(GroupDashboardEvent.NavigateToSettings)
    }

    private fun handleSyncStatus() {
        Logger.i(TAG) { "OnSyncStatus groupId=$groupId — opening sync status" }
        updateState { copy(isMoreMenuExpanded = false) }
        sendEvent(GroupDashboardEvent.NavigateToSyncStatus)
    }

    // -- Pull-to-refresh (data-flow.yaml on_refresh: bypass_and_refresh) -----------------------------

    private fun handleRefresh() {
        Logger.i(TAG) { "pull-to-refresh triggered groupId=$groupId" }
        updateState { copy(isLoading = true) }
        dashboardStream.refreshFresh()
    }

    // -- Error-state retry (data-flow.yaml on_error_retry: bypass_and_refresh) -----------------------

    private fun handleRetry() {
        Logger.i(TAG) { "retry tapped — re-dispatching group-dashboard companion fetch groupId=$groupId" }
        updateState { copy(error = null, isLoading = true) }
        dashboardStream.retry()
    }

    // -- Back navigation — see GroupDashboardEvent class KDoc "flagged addition" ---------------------

    private fun handleBack() {
        Logger.i(TAG) { "OnBack tapped groupId=$groupId" }
        sendEvent(GroupDashboardEvent.NavigateBack)
    }

    // -- Stream -> State mapping (pool-model-adaptive) ------------------------------------------------

    private fun handleStreamUpdated(screenState: ScreenState<GroupDashboard>) {
        when (screenState) {
            is ScreenState.Loading -> updateState { copy(isLoading = true, error = null) }

            is ScreenState.Empty -> updateState {
                // Never actually emitted — GroupDashboardRepositoryImpl's `isEmpty = { false }`
                // predicate means a single composite is never "empty" once present. Kept for
                // ScreenState exhaustiveness only, mirrors PersonalDashboardViewModel's identical
                // precedent.
                copy(isLoading = false, error = null)
            }

            is ScreenState.Content -> updateState {
                val dashboard = screenState.data
                val typeConfig = dashboard.group.typeConfig
                val derivedConfig = typeConfig.toGroupConfig()
                val poolModel = typeConfig.poolModel
                copy(
                    isLoading = false,
                    group = dashboard.group,
                    corpus = dashboard.corpus,
                    config = derivedConfig,
                    typeConfig = typeConfig,
                    groupTypeName = typeConfig.groupType.name,
                    // Reconciled with the server-confirmed role — see class KDoc.
                    viewerRole = dashboard.viewerRole.role.name,
                    accounts = dashboard.accounts,
                    recentActivity = dashboard.accounts.recentActivity,
                    isCorpusInsufficient = isCorpusInsufficient(dashboard.corpus, derivedConfig),
                    // isCycleEnd now backed by the server-computed get_group_corpus.isCycleEnd wire
                    // field (see GroupCorpus.isCycleEnd) — gates the Share-Out action.
                    isCycleEnd = dashboard.corpus.isCycleEnd,
                    rotationPosition = if (poolModel == SavingsMechanism.ROTATING_PAYOUT) {
                        dashboard.corpus.rotationPosition
                    } else {
                        null
                    },
                    nextRecipientName = if (poolModel == SavingsMechanism.ROTATING_PAYOUT) {
                        dashboard.corpus.nextRecipientName
                    } else {
                        null
                    },
                    nextRecipientPosition = if (poolModel == SavingsMechanism.ROTATING_PAYOUT) {
                        dashboard.corpus.nextRecipientPosition
                    } else {
                        null
                    },
                    shareOutProjection = if (poolModel == SavingsMechanism.ACCUMULATING) {
                        dashboard.accounts.shareOutProjection
                    } else {
                        null
                    },
                    error = null,
                )
            }

            is ScreenState.NoNetwork -> updateState {
                copy(isLoading = false, error = GroupDashboardError.Network)
            }

            is ScreenState.Unauthenticated -> {
                // flow.yaml / data-flow.yaml `401 -> navigate login-signup`, but no matching
                // NavigateToLogin/NavigateToLoginSignup event is declared in
                // ui.yaml#state_model.events.members — mirrors GroupListError.Auth's identical
                // documented gap. sessionManager.endSession() is a REAL clear-session call (the
                // app shell handles re-auth after the token expiry); ShowSnackbar is the closest
                // declared event, emitted so the user still gets immediate feedback.
                crashReporter.recordMessage(
                    message = "group-dashboard: session expired (401) groupId=$groupId — clearing session",
                    level = CrashSeverity.Warning,
                )
                sessionManager.endSession()
                updateState { copy(isLoading = false, error = GroupDashboardError.Auth) }
                sendEvent(GroupDashboardEvent.ShowSnackbar(message = GroupDashboardError.Auth.messageKey))
            }

            is ScreenState.Error -> {
                val throwable = screenState.error
                crashReporter.recordException(
                    throwable = throwable,
                    message = "group-dashboard: stream error groupId=$groupId isNetworkError=${screenState.isNetworkError}",
                )
                val category = categorize(throwable)
                val mapped = when {
                    screenState.isNetworkError -> GroupDashboardError.Network
                    // See GroupDashboardError.NotFound KDoc "reachability gap" — future-proofed,
                    // currently unreachable given GroupDashboardFetchException's message shape.
                    category is ErrorCategory.ClientError && category.httpCode == 404 -> GroupDashboardError.NotFound
                    else -> GroupDashboardError.Server
                }
                updateState { copy(isLoading = false, error = mapped) }
            }
        }
    }
}
