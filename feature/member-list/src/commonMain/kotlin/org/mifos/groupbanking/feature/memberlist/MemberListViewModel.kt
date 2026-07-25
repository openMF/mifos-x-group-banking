/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.memberlist

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kpt.core.analytics.KptAnalyticsTracker
import kpt.core.base.observability.CrashReporter
import kpt.core.base.observability.CrashSeverity
import kpt.core.base.store.screen.FetchPolicy
import kpt.core.base.store.screen.ScreenState
import kpt.core.base.ui.viewmodel.BaseViewModel
import org.mifos.groupbanking.core.data.repository.GroupRepository
import org.mifos.groupbanking.core.data.repository.MemberRepository
import org.mifos.groupbanking.core.model.Member

private const val TAG = "MemberListViewModel"

/**
 * Screen-level render state for `member-list-screen` — verbatim mirror of
 * ui.yaml#state_model.MemberListViewModel.screen_state. Derived (not stored) from
 * [MemberListState.isLoading] / [MemberListState.error] / [MemberListState.members] via the
 * [MemberListState.screenState] extension below — one source of truth for loading/error/empty,
 * identical convention to `GroupListState.screenState`. See API.md#state.
 */
@Serializable
sealed interface MemberListScreenState {
    @Serializable
    data object Loading : MemberListScreenState

    @Serializable
    data object Content : MemberListScreenState

    @Serializable
    data object Error : MemberListScreenState

    @Serializable
    data object Empty : MemberListScreenState
}

/**
 * Error taxonomy for the member-list read — verbatim mirror of
 * ui.yaml#state_model.MemberListViewModel.errors.types. [messageKey] is a composeResources
 * string-resource id (never a raw hardcoded English string, per
 * RULE-IMPL-NO-HARDCODED-STRING-001) resolved by the Screen layer.
 *
 * **Idea-layer gap (flagged, not invented here):** `data-flow.yaml#error_paths` declares
 * `401 -> behavior: navigate, nav_target: login-signup`, but `state_model.events.members`
 * declares no `NavigateToLogin` event — mirrors `GroupListError.Auth`'s identical documented
 * gap. [Auth] is surfaced as an ordinary non-retryable error state PLUS the closest declared
 * event ([MemberListEvent.ShowSnackbar]) so the user still gets immediate feedback. Reported to
 * the caller for an idea-layer `ui.yaml#events` update (add a `NavigateToLogin` member) rather
 * than invented silently.
 */
@Serializable
sealed interface MemberListError {
    val retry: Boolean
    val messageKey: String

    @Serializable
    data object Network : MemberListError {
        override val retry: Boolean = true
        override val messageKey: String = "error_network"
    }

    @Serializable
    data object Server : MemberListError {
        override val retry: Boolean = true
        override val messageKey: String = "error_server"
    }

    @Serializable
    data object Auth : MemberListError {
        override val retry: Boolean = false
        override val messageKey: String = "error_auth"
    }
}

/**
 * MVI state for `MemberListViewModel`. Field set + defaults are a verbatim mirror of
 * ui.yaml#state_model.MemberListViewModel.state.
 *
 * [members] and [error] are `@Transient` — [Member] is not `@Serializable`, and the paged list
 * is always re-derived from [MemberRepository.membersPagingStream] on (re)subscription
 * (offline-first cache via `member_list_cache`, so nothing is visually lost across process
 * death — the store, not this transient render state, is the durable source). Mirrors
 * `GroupListState`'s identical `@Transient` convention for non-serializable domain payloads
 * (see `training-layer/TRAINING_MASTER.yaml#patterns.state_models`).
 *
 * [currentOffset] is DERIVED from `members.size` on every stream update — `PagingScreenStream`
 * does not expose its internal page/offset counter, and `data-flow.yaml`'s
 * `{{currentOffset + pageSize}}` `params_override` is computed internally by the store's own
 * `PageKey` bookkeeping, not by this ViewModel. `currentOffset` here is a UI-facing "rows loaded
 * so far" readout, kept accurate because `ScreenState.Content.data` is always the FULL
 * accumulated list (see `PagingScreenStream.loadNextPage`/`loadInitialPage`).
 *
 * See API.md#state.
 */
@Serializable
@Immutable
data class MemberListState(
    val isLoading: Boolean = true,
    @Transient
    val members: List<Member> = emptyList(),
    val groupId: String = "",
    val groupName: String = "",
    val isLoadingMore: Boolean = false,
    val hasMorePages: Boolean = true,
    val currentOffset: Int = 0,
    @Transient
    val error: MemberListError? = null,
    val isRefreshing: Boolean = false,
)

/** Derived, single-source-of-truth screen state — see [MemberListScreenState] KDoc. */
val MemberListState.screenState: MemberListScreenState
    get() = when {
        error != null -> MemberListScreenState.Error
        isLoading -> MemberListScreenState.Loading
        members.isEmpty() -> MemberListScreenState.Empty
        else -> MemberListScreenState.Content
    }

/**
 * One-shot side effects emitted by `MemberListViewModel` — verbatim mirror of
 * ui.yaml#state_model.MemberListViewModel.events, PLUS [NavigateBack].
 *
 * **Idea-layer gap (flagged addition, not invented silently):** the `top_bar`'s
 * `on_navigation_click` (`OnBack`) declares `effect: navigate` in ui.yaml, and
 * `data-flow.yaml#entries[action: OnBack]` confirms "Pops the member-list via NavController
 * back to the group-dashboard" — but `state_model.events.members` declares no matching
 * `NavigateBack` member. Mirrors `GroupDashboardEvent.NavigateBack`'s identical documented
 * addition: the effect + description are unambiguous (a plain nav-pop, not a parameterized
 * destination), so [NavigateBack] is added here as the one sanctioned addition-from-component
 * rather than silently dropping `OnBack`'s handler or inventing an unrelated event shape.
 * Reported to the caller for an idea-layer `ui.yaml#state_model.events.members` update.
 * See API.md#events.
 */
sealed interface MemberListEvent {
    data class NavigateToMemberProfile(val memberId: String, val groupId: String) : MemberListEvent
    data class NavigateToAddMember(val groupId: String) : MemberListEvent

    /**
     * Navigate to `member-invite` (the organizer generates a single-use invite link/code). Declared
     * edge: `member-onboarding-flow.yaml#steps[1]` ("taps Add Member (direct) or **Invite Member**
     * (invite path)") + `member-invite/ui.yaml#entry_points[0]` (`source: member-list`,
     * `trigger: invite_fab_tap`, forwarding `groupId`). member-list's own `ui.yaml` had no invite
     * component/action yet — this event + [MemberListAction.OnInviteMember] are the one sanctioned
     * addition-from-declared-flow (mirrors the [NavigateBack] flagged-addition convention above).
     * Reported to the caller for a `member-list/ui.yaml#components`+`state_model` update (an invite
     * top-bar action + `OnInviteMember` action + `NavigateToMemberInvite` event).
     */
    data class NavigateToMemberInvite(val groupId: String) : MemberListEvent
    data class ShowSnackbar(val message: String) : MemberListEvent
    data object NavigateBack : MemberListEvent
}

/**
 * User intents dispatched to `MemberListViewModel`. The 6 top-level members are a verbatim
 * mirror of ui.yaml#state_model.MemberListViewModel.actions — RULE-IMPL-DEAD-CLICKABLE-001
 * Rule 1. [Internal] is the sanctioned async-result-routing sub-interface (never a user intent)
 * per `training-layer/TRAINING_MASTER.yaml#patterns.actions` — mirrors
 * `GroupListAction.Internal`; every async source (the paged member stream, the `hasMore` /
 * `isLoadingMore` side-channels off `PagingScreenStream`, and the best-effort group-name
 * lookup — see [MemberListViewModel] KDoc) funnels through one of these members rather than
 * mutating state directly from inside a raw `.collect {}` lambda, keeping the single
 * `handleAction` funnel authoritative for every state transition.
 * See API.md#actions.
 */
sealed interface MemberListAction {
    data class OnMemberClick(val memberId: String) : MemberListAction
    data object OnAddMember : MemberListAction

    /** Tap the invite top-bar action — see [MemberListEvent.NavigateToMemberInvite] KDoc. */
    data object OnInviteMember : MemberListAction
    data object OnLoadMore : MemberListAction
    data object OnRefresh : MemberListAction
    data object Retry : MemberListAction
    data object OnBack : MemberListAction

    /** Async stream emissions — routed via `trySendAction`, never dispatched by the UI. */
    sealed interface Internal : MemberListAction {
        data class StreamUpdated(val screenState: ScreenState<List<Member>>) : Internal
        data class HasMoreUpdated(val hasMore: Boolean) : Internal
        data class LoadingMoreUpdated(val loadingMore: Boolean) : Internal
        data class GroupNameResolved(val name: String) : Internal
    }
}

/**
 * MVI processor for the member-list screen (`business_logic.kind: crud` per ui.yaml — a pure
 * paginated read-side roster, so [MemberRepository.membersPagingStream]'s offline-first
 * [kpt.core.base.store.paging.PagingScreenStream] is consumed directly rather than the SP-04
 * AC-7 analytics/crashReporter/fieldEncryptor injection triple, per
 * RULE-IDEA-IMPL-INTELLIGENCE-001 AC-03i — that hook set is reserved for non-crud/non-nav_only
 * Store5 write paths. [Member] carries no PII field (per `core/model/Member.kt`), so no
 * `FieldEncryptor` is injected.
 *
 * [crashReporter] and [analytics] are still wired (feature-level observability, SC5): a Debug
 * breadcrumb on mount, a warning/exception breadcrumb on stream failure, and a
 * `trackClientOperation(...)` call on every user-initiated roster operation (`Member` maps 1:1
 * onto a Fineract `client`, so the existing `trackClientOperation` surface is reused rather than
 * inventing a `trackMemberOperation` method that doesn't exist on `KptAnalyticsTracker`) — all
 * through the actual shipped `CrashReporter`/`KptAnalyticsTracker` surfaces.
 *
 * **`groupName` resolution — documented idea-layer gap:** `ui.yaml#entry_points[0].params` only
 * forwards `groupId` from `group-dashboard`'s `view_members_button` trigger (mirrored by
 * `GroupDashboardEvent.NavigateToMemberList(groupId)` — no `groupName` param exists on that
 * event either), and `GroupRepository` (the DI type `ui.yaml#state_model.di` actually declares
 * for this screen) exposes only [GroupRepository.groupsPagingStream] — no by-id/by-name lookup.
 * Rather than inventing a new repository method, this ViewModel does a best-effort
 * [FetchPolicy.CACHE_ONLY] read of the user's already-cached group list and matches [groupId]
 * against it (population is virtually guaranteed — reaching member-list requires having already
 * opened this group's dashboard, which primes the shared Store5 cache). If no match is cached
 * yet, [MemberListState.groupName] stays `""` and the top bar renders with an empty subtitle
 * rather than crashing or blocking the member fetch. Reported to the caller: either add
 * `groupName` to `ui.yaml#entry_points[0].params` (avoids the extra read entirely) or add a
 * dedicated `GroupRepository.getGroupById(groupId)` lookup.
 *
 * See API.md#viewmodel.
 */
internal class MemberListViewModel(
    private val memberRepository: MemberRepository,
    private val groupRepository: GroupRepository,
    private val crashReporter: CrashReporter,
    private val analytics: KptAnalyticsTracker,
    private val groupId: String,
) : BaseViewModel<MemberListState, MemberListEvent, MemberListAction>(
    initialState = MemberListState(groupId = groupId),
) {

    /** Offline-first PAGED stream scoped to [groupId] — see `MemberRepository.membersPagingStream` KDoc. */
    private val pagingStream = memberRepository.membersPagingStream(groupId = groupId, scope = viewModelScope)

    init {
        crashReporter.recordMessage(
            message = "feature=member-list screen=member-list-screen groupId=$groupId",
            level = CrashSeverity.Debug,
        )
        analytics.trackClientOperation(operation = "view_list")

        viewModelScope.launch {
            pagingStream.state.collect { screenState ->
                trySendAction(MemberListAction.Internal.StreamUpdated(screenState))
            }
        }
        viewModelScope.launch {
            pagingStream.hasMore.collect { hasMore ->
                trySendAction(MemberListAction.Internal.HasMoreUpdated(hasMore))
            }
        }
        viewModelScope.launch {
            pagingStream.isLoadingMore.collect { loadingMore ->
                trySendAction(MemberListAction.Internal.LoadingMoreUpdated(loadingMore))
            }
        }
        viewModelScope.launch {
            // Best-effort groupName lookup — see class KDoc "groupName resolution" gap note.
            groupRepository.groupsPagingStream(scope = viewModelScope, fetchPolicy = FetchPolicy.CACHE_ONLY)
                .state
                .collect { groupsState ->
                    if (groupsState is ScreenState.Content) {
                        val match = groupsState.data.firstOrNull { it.id == groupId }
                        if (match != null) {
                            trySendAction(MemberListAction.Internal.GroupNameResolved(match.name))
                        }
                    }
                }
        }
    }

    override fun handleAction(action: MemberListAction) {
        when (action) {
            is MemberListAction.OnMemberClick -> handleMemberClick(action.memberId)
            MemberListAction.OnAddMember -> handleAddMember()
            MemberListAction.OnInviteMember -> handleInviteMember()
            MemberListAction.OnLoadMore -> handleLoadMore()
            MemberListAction.OnRefresh -> handleRefresh()
            MemberListAction.Retry -> handleRetry()
            MemberListAction.OnBack -> handleBack()
            is MemberListAction.Internal.StreamUpdated -> handleStreamUpdated(action.screenState)
            is MemberListAction.Internal.HasMoreUpdated ->
                updateState { copy(hasMorePages = action.hasMore) }
            is MemberListAction.Internal.LoadingMoreUpdated ->
                updateState { copy(isLoadingMore = action.loadingMore) }
            is MemberListAction.Internal.GroupNameResolved ->
                updateState { copy(groupName = action.name) }
        }
    }

    // -- Member row tap / swipe (ui.yaml effect: navigate, target: member-profile) -----------------

    private fun handleMemberClick(memberId: String) {
        analytics.trackClientOperation(operation = "view", clientId = memberId)
        Logger.i(TAG) { "member row tapped memberId=$memberId groupId=$groupId" }
        sendEvent(MemberListEvent.NavigateToMemberProfile(memberId = memberId, groupId = groupId))
    }

    // -- Add-member FAB + empty-state CTA (ui.yaml effect: navigate, target: member-add) ------------

    private fun handleAddMember() {
        analytics.trackClientOperation(operation = "create")
        Logger.i(TAG) { "add-member tapped groupId=$groupId" }
        sendEvent(MemberListEvent.NavigateToAddMember(groupId))
    }

    // -- Invite-member top-bar action (member-onboarding-flow invite path -> member-invite) ----------

    private fun handleInviteMember() {
        analytics.trackClientOperation(operation = "invite")
        Logger.i(TAG) { "invite-member tapped groupId=$groupId" }
        sendEvent(MemberListEvent.NavigateToMemberInvite(groupId))
    }

    // -- Scroll-to-end load-more (data-flow.yaml on_interact/OnLoadMore via MemberRepository) --------

    private fun handleLoadMore() {
        Logger.i(TAG) { "load-more triggered currentSize=${state.members.size} groupId=$groupId" }
        pagingStream.loadNextPage()
    }

    // -- Pull to refresh (data-flow.yaml on_refresh: bypass_and_refresh via MemberRepository) ---------

    private fun handleRefresh() {
        analytics.trackClientOperation(operation = "refresh")
        Logger.i(TAG) { "pull-to-refresh triggered groupId=$groupId" }
        updateState { copy(isRefreshing = true) }
        pagingStream.refresh()
    }

    // -- Error-state retry (ui.yaml effect: call_api, library_refs: [cmp-network-monitor]) ------------

    private fun handleRetry() {
        Logger.i(TAG) { "retry tapped — re-dispatching member fetch via MemberRepository groupId=$groupId" }
        updateState { copy(error = null, isLoading = true) }
        pagingStream.retry()
    }

    // -- Back navigation — see MemberListEvent class KDoc "flagged addition" --------------------------

    private fun handleBack() {
        Logger.i(TAG) { "OnBack tapped groupId=$groupId" }
        sendEvent(MemberListEvent.NavigateBack)
    }

    // -- Stream -> State mapping ------------------------------------------------------------------------

    private fun handleStreamUpdated(screenState: ScreenState<List<Member>>) {
        when (screenState) {
            is ScreenState.Loading -> updateState {
                copy(isLoading = true, error = null, isRefreshing = false)
            }

            is ScreenState.Empty -> updateState {
                copy(
                    isLoading = false,
                    members = emptyList(),
                    currentOffset = 0,
                    error = null,
                    isRefreshing = false,
                )
            }

            is ScreenState.Content -> updateState {
                val newMembers = screenState.data
                copy(
                    isLoading = false,
                    members = newMembers,
                    currentOffset = newMembers.size,
                    error = null,
                    isRefreshing = false,
                )
            }

            is ScreenState.NoNetwork -> updateState {
                copy(isLoading = false, error = MemberListError.Network, isRefreshing = false)
            }

            is ScreenState.Unauthenticated -> {
                // See the Auth-error KDoc gap note on MemberListError — no declared
                // NavigateToLogin event exists; surfaced as non-retryable Auth error state PLUS
                // the closest declared event (ShowSnackbar) so the user still gets immediate
                // feedback.
                crashReporter.recordMessage(
                    message = "member-list: session expired (401) groupId=$groupId",
                    level = CrashSeverity.Warning,
                )
                updateState { copy(isLoading = false, error = MemberListError.Auth, isRefreshing = false) }
                sendEvent(MemberListEvent.ShowSnackbar(message = MemberListError.Auth.messageKey))
            }

            is ScreenState.Error -> {
                crashReporter.recordException(
                    throwable = screenState.error,
                    message = "member-list: stream error groupId=$groupId isNetworkError=${screenState.isNetworkError}",
                )
                val mapped = if (screenState.isNetworkError) {
                    MemberListError.Network
                } else {
                    MemberListError.Server
                }
                updateState { copy(isLoading = false, error = mapped, isRefreshing = false) }
            }
        }
    }
}
