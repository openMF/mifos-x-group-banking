/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.personalsavings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
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
import org.mifos.groupbanking.core.model.MemberSavingsBundle
import org.mifos.groupbanking.core.model.SavingsLedgerEntry
import org.mifos.groupbanking.core.model.SavingsTab

// MVI stack (State/Event/Action/ViewModel/DI) for the `personal-savings` feature — see
// API.md#viewmodel / #state / #actions / #events / #di for the full generated-symbol contract.
private const val TAG = "PersonalSavingsViewModel"

/**
 * Screen-level render state for `personal-savings-screen` — verbatim mirror of
 * `ui.yaml#state_model.PersonalSavingsViewModel.screen_state.members` (3 members — NO `Empty`
 * member here, unlike `PersonalLoansScreenState`; the ui.yaml `empty_individual_promo` component
 * is a content-level `visible_when` condition, not a top-level screen state). Derived only (not
 * stored) via [PersonalSavingsState.deriveScreenState] — same convention as
 * `PersonalLoansState.deriveScreenState()`. See API.md#state.
 */
@Serializable
sealed interface PersonalSavingsScreenState {
    @Serializable
    data object Loading : PersonalSavingsScreenState

    @Serializable
    data object Content : PersonalSavingsScreenState

    @Serializable
    data object Error : PersonalSavingsScreenState
}

/**
 * Error taxonomy for the savings-ledger read — verbatim mirror of
 * `ui.yaml#state_model.PersonalSavingsViewModel.errors.types`. [messageKey] is a composeResources
 * string-resource id (never a raw hardcoded English string, per RULE-IMPL-NO-HARDCODED-STRING-001)
 * resolved by the Screen layer.
 *
 * See API.md#state.
 */
@Serializable
sealed interface SavingsError {
    val retry: Boolean
    val messageKey: String

    @Serializable
    data object Network : SavingsError {
        override val retry: Boolean = true
        override val messageKey: String = "error_network"
    }

    @Serializable
    data object Server : SavingsError {
        override val retry: Boolean = true
        override val messageKey: String = "error_server"
    }

    @Serializable
    data object Unauthorized : SavingsError {
        override val retry: Boolean = false
        override val messageKey: String = "error_session_expired"
    }
}

/**
 * MVI state for `PersonalSavingsViewModel`. Field set + defaults are a verbatim mirror of
 * `ui.yaml#state_model.PersonalSavingsViewModel.state.fields`, with ONE deliberate type
 * correction (documented, not silently applied):
 *
 * **`groupLinkedTransactions`/`individualTransactions` type — confirmed idea-layer drift,
 * corrected to the real domain type:** `ui.yaml` declares these fields as
 * `List<SavingsTransactionDto>`, but `SavingsTransactionDto` (`core/network/model`) is the
 * COMPACT `personal-dashboard` companion-card shape (`id: String`, `date: String`, 2-value
 * `TransactionTypeDto`) — a completely different, incompatible class from what
 * [SavingsRepository.loadMemberSavings]/[SavingsRepository.getSavingsTransactions] actually
 * return ([SavingsLedgerEntry], the raw Fineract self-service ledger row: `id: Long`,
 * `type: SavingsLedgerTransactionType`, `date: LocalDate`, `runningBalance`, `currency*`). Using
 * the literal ui.yaml type would not compile against the just-generated repository contract; see
 * `SavingsTransactionDto`'s own KDoc "naming-collision note" for the full three-way drift
 * (registry / personal-savings' OWN `api.yaml` / personal-dashboard's `api.yaml` all declare a
 * DIFFERENT shape under the same bare name). Flagged for the cross-feature repair station
 * (RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1) — `ui.yaml` should be corrected to reference
 * `SavingsLedgerEntry` (or a not-yet-created `SavingsLedgerEntryDto`) directly.
 *
 * [groupLinkedTransactions]/[individualTransactions]/[error] are `@Transient` — always re-derived
 * from [SavingsRepository] on (re)mount, never restored from a `@Serializable` snapshot (mirrors
 * `PersonalLoansState`'s identical `@Transient` convention; [SavingsLedgerEntry] itself is not
 * `@Serializable`).
 *
 * **`contributionTarget` (default `500.0`) — confirmed idea-layer gap, flagged not invented:**
 * neither `api.yaml#operations` nor [SavingsRepository] exposes a per-cycle contribution-target
 * endpoint; the group's contribution target is presumably a `GroupTypeConfig`/loan-product-style
 * server value that has not been threaded to this screen's DI graph
 * (`ui.yaml#state_model.di: [SavingsRepository, SessionManager]` only). The `ui.yaml`-declared
 * literal default is kept AS A REAL DEFAULT (not a fabricated fetch) — this ViewModel never
 * pretends to load it from the network. Flagged for the cross-feature repair station
 * (RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1).
 *
 * **`meetingsAttended`/`totalMeetings` (both default `0`) — confirmed idea-layer gap, flagged not
 * invented:** no savings, group, or meetings API on this screen's declared DI graph surfaces
 * attendance data (`data-flow.yaml` has no `entries[]` for it either) — this is a genuinely
 * separate meeting-attendance domain that has not been built yet. Kept at an honest `0`/`0`
 * (NOT a fabricated placeholder like `12`/`16`) so the contribution-progress bar renders an
 * explicit "no data yet" `0/0` rather than invented numbers. Flagged for the cross-feature repair
 * station (RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1) — the fix is a meetings/attendance
 * repository this ViewModel can inject once one exists.
 */
@Serializable
@Immutable
data class PersonalSavingsState(
    val clientId: Long = 0L,
    val groupLinkedSavingsId: Long = 0L,
    val individualSavingsId: Long? = null,
    val selectedTab: SavingsTab = SavingsTab.GROUP_LINKED,
    val groupLinkedBalance: Double = 0.0,
    val individualBalance: Double = 0.0,
    @Transient
    val groupLinkedTransactions: List<SavingsLedgerEntry> = emptyList(),
    @Transient
    val individualTransactions: List<SavingsLedgerEntry> = emptyList(),
    val contributionTarget: Double = 500.0,
    val meetingsAttended: Int = 0,
    val totalMeetings: Int = 0,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    @Transient
    val error: SavingsError? = null,
)

/**
 * Derives [PersonalSavingsScreenState] from [PersonalSavingsState] — see the type's KDoc for why
 * this is a pure function rather than a stored field. `error != null` wins over any transaction
 * count so a failed refresh on an already-loaded account still surfaces the error state.
 */
fun PersonalSavingsState.deriveScreenState(): PersonalSavingsScreenState = when {
    error != null -> PersonalSavingsScreenState.Error
    isLoading -> PersonalSavingsScreenState.Loading
    else -> PersonalSavingsScreenState.Content
}

/**
 * One-shot side effects emitted by `PersonalSavingsViewModel` — verbatim mirror of
 * `ui.yaml#state_model.PersonalSavingsViewModel.events.members` (1 member).
 *
 * **[NavigateBack] reachability gap (flagged, not fabricated around):** `ui.yaml#components.top_bar`
 * declares `on_navigation_click.action: NavigateBack`, but
 * `ui.yaml#state_model.actions.members` (the 3-member list [PersonalSavingsAction] mirrors
 * verbatim per RULE-IMPL-DEAD-CLICKABLE-001 Rule 1) does NOT declare a matching `OnBack` action
 * member — same class of drift as `personal-loans`'s documented top-bar gap
 * ([org.mifos.groupbanking.feature.personalloans.PersonalLoansEvent] KDoc). [NavigateBack] is
 * therefore declared here (verbatim `events.members` mirror) but never emitted by this
 * ViewModel; the Screen layer's `KptScaffold` back button is expected to invoke the nav-host
 * callback directly (bypassing action dispatch) until the idea-layer adds the missing action
 * member. Flagged for idea-layer correction (RULE-IDEA-ACTION-CONTRACT-001).
 *
 * See API.md#events.
 */
sealed interface PersonalSavingsEvent {
    data object NavigateBack : PersonalSavingsEvent
}

/**
 * User intents dispatched to `PersonalSavingsViewModel`. The 3 top-level members are a verbatim
 * mirror of `ui.yaml#state_model.PersonalSavingsViewModel.actions.members` —
 * RULE-IMPL-DEAD-CLICKABLE-001 Rule 1. [Internal] is the sanctioned async-result-routing
 * sub-interface (never a user intent) per `training-layer/TRAINING_MASTER.yaml#patterns.actions`
 * — mirrors `PersonalLoansAction.Internal`. See API.md#actions.
 */
sealed interface PersonalSavingsAction {
    data class OnTabSelected(val tab: SavingsTab) : PersonalSavingsAction
    data object OnRefresh : PersonalSavingsAction
    data object OnRetry : PersonalSavingsAction

    /** Async coroutine result — routed via `trySendAction`, never dispatched by the UI. */
    sealed interface Internal : PersonalSavingsAction {
        /** [loadInitial] on_mount composite read — both accounts. */
        data class SavingsLoaded(
            val result: NetworkResult<MemberSavingsBundle, NetworkError>,
        ) : Internal

        /** OnRefresh / OnRetry / (would-be lazy tab load) single-account bypass read. */
        data class RefreshResult(
            val tab: SavingsTab,
            val result: NetworkResult<List<SavingsLedgerEntry>, NetworkError>,
        ) : Internal
    }
}

/**
 * MVI processor for the member-side "My Savings" tabbed ledger (`business_logic.kind: crud` per
 * ui.yaml — a read-only fetch of the signed-in member's own group-linked + individual savings
 * transactions; per RULE-IDEA-IMPL-INTELLIGENCE-001 AC-03i the SP-04 AC-7
 * analytics/crashReporter/fieldEncryptor injection TRIPLE is not mandatory for `crud`, but
 * [crashReporter] and [analytics] are still wired for real, feature-level observability (SC5),
 * mirroring `PersonalLoansViewModel`'s identical crud-but-observed precedent). No `FieldEncryptor`
 * (`core-base/security`) is injected — [SavingsLedgerEntry] carries no `@PII`-marked field per the
 * SP-02 idea-layer schema.
 *
 * **On-mount eager-load vs `data-flow.yaml`'s "lazy tab load" description — confirmed drift,
 * flagged not silently resolved:** `data-flow.yaml#entries[on_interact:tab_switch_to_individual]`
 * describes the individual account as lazy-loaded "the first time the Individual tab is opened",
 * but [SavingsRepository.loadMemberSavings]'s OWN KDoc states it fetches BOTH accounts
 * CONCURRENTLY whenever `individualSavingsId != null` — i.e. on THIS screen's initial mount, not
 * on tab switch. This ViewModel uses the repository exactly as generated (`loadInitial` below) —
 * both accounts load together on mount when the member has an individual account.
 * [PersonalSavingsAction.OnTabSelected] is consequently a PURE `transform_state` action (matches
 * `ui.yaml#components.savings_tab_row.action_contract.effect: transform_state` verbatim) with no
 * re-fetch branch, since the "not yet loaded" condition the ui.yaml description anticipates never
 * arises under the repository's actual eager-both-accounts contract. Flagged for the
 * cross-feature repair station (RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1) — reconcile by either
 * updating `data-flow.yaml` to describe the real eager-load, or splitting
 * `loadMemberSavings`/`SavingsRepository` so the individual leg is genuinely deferred.
 *
 * [repository] is consumed directly via [NetworkResult] rather than a Store5
 * `.asScreenStream()` — [SavingsRepository]'s own KDoc "Store5 branch" note (no
 * `AppStoreRegistry.Savings` entry yet), same branch as
 * [org.mifos.groupbanking.feature.personalloans.PersonalLoansViewModel].
 *
 * [sessionManager] is declared in `ui.yaml#state_model.di` and is used for its ONE real
 * capability — `endSession()` on [SavingsError.Unauthorized] (mirrors
 * `PersonalLoansViewModel`'s identical wiring for its own 401 branch).
 *
 * [clientId]/[groupLinkedSavingsId]/[individualSavingsId] are the `ui.yaml#nav_params` values
 * forwarded from `personal-dashboard`'s `user_taps_savings_card` entry point via Koin
 * `parametersOf(...)` (see `di.PersonalSavingsModule`).
 *
 * See API.md#viewmodel.
 */
internal class PersonalSavingsViewModel(
    private val repository: SavingsRepository,
    private val sessionManager: SessionManager,
    private val crashReporter: CrashReporter,
    private val analytics: KptAnalyticsTracker,
    private val clientId: Long,
    private val groupLinkedSavingsId: Long,
    private val individualSavingsId: Long?,
) : BaseViewModel<PersonalSavingsState, PersonalSavingsEvent, PersonalSavingsAction>(
    initialState = PersonalSavingsState(
        clientId = clientId,
        groupLinkedSavingsId = groupLinkedSavingsId,
        individualSavingsId = individualSavingsId,
    ),
) {

    init {
        crashReporter.recordMessage(
            message = "feature=personal-savings screen=personal-savings-screen clientId=$clientId " +
                "groupLinkedSavingsId=$groupLinkedSavingsId individualSavingsId=$individualSavingsId",
            level = CrashSeverity.Debug,
        )
        analytics.trackSavingsOperation(operation = "view", accountId = groupLinkedSavingsId.toString())
        loadInitial()
    }

    override fun handleAction(action: PersonalSavingsAction) {
        when (action) {
            is PersonalSavingsAction.OnTabSelected -> handleTabSelected(action.tab)
            PersonalSavingsAction.OnRefresh -> handleRefresh()
            PersonalSavingsAction.OnRetry -> handleRetry()
            is PersonalSavingsAction.Internal.SavingsLoaded -> handleSavingsLoaded(action.result)
            is PersonalSavingsAction.Internal.RefreshResult -> handleRefreshResult(action.tab, action.result)
        }
    }

    // -- on_mount (data-flow.yaml — GET /self/savingsaccounts/{id}/transactions x2) ---------------

    private fun loadInitial() {
        viewModelScope.launch {
            val result = repository.loadMemberSavings(groupLinkedSavingsId, individualSavingsId)
            trySendAction(PersonalSavingsAction.Internal.SavingsLoaded(result))
        }
    }

    // -- Tab switch (ui.yaml effect: transform_state — pure re-bind, see class KDoc) --------------

    private fun handleTabSelected(tab: SavingsTab) {
        Logger.d(TAG) { "tab switched to=$tab clientId=$clientId" }
        updateState { copy(selectedTab = tab) }
    }

    // -- Pull-to-refresh (data-flow.yaml on_refresh: bypass_and_refresh, active tab only) ---------

    private fun handleRefresh() {
        val activeId = activeTabSavingsId(state.selectedTab)
        if (activeId == null) {
            crashReporter.recordMessage(
                message = "personal-savings: refresh requested for INDIVIDUAL tab with no " +
                    "individualSavingsId clientId=$clientId — ignoring",
                level = CrashSeverity.Warning,
            )
            return
        }
        val tab = state.selectedTab
        analytics.trackSavingsOperation(operation = "refresh", accountId = activeId.toString())
        Logger.i(TAG) { "pull-to-refresh triggered tab=$tab savingsId=$activeId" }
        updateState { copy(isRefreshing = true) }
        viewModelScope.launch {
            val result = repository.getSavingsTransactions(activeId)
            trySendAction(PersonalSavingsAction.Internal.RefreshResult(tab = tab, result = result))
        }
    }

    // -- Error-state retry (ui.yaml effect: call_api, active tab only) -----------------------------

    private fun handleRetry() {
        val activeId = activeTabSavingsId(state.selectedTab)
        if (activeId == null) {
            crashReporter.recordMessage(
                message = "personal-savings: retry requested for INDIVIDUAL tab with no " +
                    "individualSavingsId clientId=$clientId — ignoring",
                level = CrashSeverity.Warning,
            )
            return
        }
        val tab = state.selectedTab
        Logger.i(TAG) { "retry tapped — re-dispatching savings fetch tab=$tab savingsId=$activeId" }
        updateState { copy(error = null, isLoading = true) }
        viewModelScope.launch {
            val result = repository.getSavingsTransactions(activeId)
            trySendAction(PersonalSavingsAction.Internal.RefreshResult(tab = tab, result = result))
        }
    }

    /** Resolves the savingsId of the currently-active tab, or `null` if it has no account. */
    private fun activeTabSavingsId(tab: SavingsTab): Long? = when (tab) {
        SavingsTab.GROUP_LINKED -> groupLinkedSavingsId
        SavingsTab.INDIVIDUAL -> individualSavingsId
    }

    // -- Initial-mount fetch result routing ----------------------------------------------------------

    private fun handleSavingsLoaded(result: NetworkResult<MemberSavingsBundle, NetworkError>) {
        when (result) {
            is NetworkResult.Success -> {
                val bundle = result.data
                val individualTxns = bundle.individualTransactions ?: emptyList()
                Logger.i(TAG) {
                    "savings loaded clientId=$clientId groupLinkedCount=${bundle.groupLinkedTransactions.size} " +
                        "individualCount=${individualTxns.size}"
                }
                updateState {
                    copy(
                        isLoading = false,
                        isRefreshing = false,
                        groupLinkedTransactions = bundle.groupLinkedTransactions,
                        groupLinkedBalance = bundle.groupLinkedTransactions.currentBalance(),
                        individualTransactions = individualTxns,
                        individualBalance = individualTxns.currentBalance(),
                        error = null,
                    )
                }
            }

            is NetworkResult.Error -> {
                val mapped = result.error.toSavingsError()
                reportLoadError(mapped, result.error)
                updateState { copy(isLoading = false, isRefreshing = false, error = mapped) }
            }
        }
    }

    // -- OnRefresh / OnRetry single-account fetch result routing --------------------------------------

    private fun handleRefreshResult(tab: SavingsTab, result: NetworkResult<List<SavingsLedgerEntry>, NetworkError>) {
        when (result) {
            is NetworkResult.Success -> {
                val txns = result.data
                Logger.i(TAG) { "savings refreshed tab=$tab count=${txns.size} clientId=$clientId" }
                updateState {
                    when (tab) {
                        SavingsTab.GROUP_LINKED -> copy(
                            isLoading = false,
                            isRefreshing = false,
                            groupLinkedTransactions = txns,
                            groupLinkedBalance = txns.currentBalance(),
                            error = null,
                        )

                        SavingsTab.INDIVIDUAL -> copy(
                            isLoading = false,
                            isRefreshing = false,
                            individualTransactions = txns,
                            individualBalance = txns.currentBalance(),
                            error = null,
                        )
                    }
                }
            }

            is NetworkResult.Error -> {
                val mapped = result.error.toSavingsError()
                reportLoadError(mapped, result.error)
                updateState { copy(isLoading = false, isRefreshing = false, error = mapped) }
            }
        }
    }

    private fun reportLoadError(mapped: SavingsError, networkError: NetworkError) {
        if (mapped == SavingsError.Unauthorized) {
            crashReporter.recordMessage(
                message = "personal-savings: session expired (401) clientId=$clientId — clearing session",
                level = CrashSeverity.Warning,
            )
            sessionManager.endSession()
        } else {
            crashReporter.recordException(
                throwable = IllegalStateException("personal-savings fetch failed: $networkError"),
                message = "personal-savings: load failed clientId=$clientId networkError=$networkError",
            )
        }
    }

    /**
     * The current balance of an account IS the newest ledger entry's [SavingsLedgerEntry.runningBalance]
     * — [SavingsRepository] exposes no dedicated account-summary endpoint (see [MemberSavingsBundle]
     * KDoc). Sorted by [SavingsLedgerEntry.date] rather than trusting list order (Fineract
     * conventionally returns newest-first, but this reducer does not assume transport ordering).
     * `0.0` when the list is empty.
     */
    private fun List<SavingsLedgerEntry>.currentBalance(): Double = maxByOrNull { it.date }?.runningBalance ?: 0.0

    /**
     * Disambiguates the transport-level [NetworkError] onto [SavingsError] —
     * `ui.yaml#state_model.errors.types` declares only 3 members (`Network`/`Server`/`Unauthorized`),
     * narrower than the 8-value [NetworkError] enum. [NetworkError.REQUEST_TIMEOUT] ->
     * [SavingsError.Network]; [NetworkError.UNAUTHORIZED]/[NetworkError.TOO_MANY_REQUESTS] ->
     * [SavingsError.Unauthorized] (rate-limiting folded onto the non-retry auth bucket — no
     * dedicated `RateLimited` member is declared); every other value (`BAD_REQUEST`/`NOT_FOUND`/
     * `SERVER`/`SERIALIZATION`/`UNKNOWN`) -> [SavingsError.Server] (this screen has no
     * user-editable form, so there is no `Validation` bucket). Mirrors
     * `PersonalLoansViewModel.toLoanError`'s identical mapping shape. Flagged for the
     * cross-feature repair station (RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1) if a dedicated
     * `RateLimited` bucket is later needed.
     */
    private fun NetworkError.toSavingsError(): SavingsError = when (this) {
        NetworkError.REQUEST_TIMEOUT -> SavingsError.Network
        NetworkError.UNAUTHORIZED, NetworkError.TOO_MANY_REQUESTS -> SavingsError.Unauthorized
        NetworkError.BAD_REQUEST, NetworkError.NOT_FOUND, NetworkError.SERVER,
        NetworkError.SERIALIZATION, NetworkError.UNKNOWN,
        -> SavingsError.Server
    }
}
