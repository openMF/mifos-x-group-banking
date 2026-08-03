/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.memberprofile

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
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
import kpt.core.base.store.error.ErrorCategory
import kpt.core.base.store.error.categorize
import kpt.core.base.store.screen.ScreenDataStream
import kpt.core.base.store.screen.ScreenState
import kpt.core.base.ui.viewmodel.BaseViewModel
import org.mifos.groupbanking.core.data.repository.MemberProfileRepository
import org.mifos.groupbanking.core.model.MemberAccounts
import org.mifos.groupbanking.core.model.MemberProfile
import org.mifos.groupbanking.core.model.MemberProfileDetail
import org.mifos.groupbanking.core.model.MemberRole
import org.mifos.groupbanking.core.model.MemberRoleInfo
import org.mifos.groupbanking.core.model.UpdateMemberRoleRequest
import org.mifos.groupbanking.core.model.UpdateMemberRoleResult
import kotlin.time.Clock

// MVI stack (State/Event/Action/ViewModel/DI) for the `member-profile` feature — see
// API.md#viewmodel / #state / #actions / #events / #di for the full generated-symbol contract.
private const val TAG = "MemberProfileViewModel"

/**
 * Screen-level render state for `member-profile-screen` — verbatim mirror of
 * `ui.yaml#state_model.screen_state.members` ([Loading] / [Content] / [Error]; no `Empty`
 * variant — a single member composite is never "empty" once present, same class as
 * `GroupDashboardScreenState`). Derived (not stored) via [MemberProfileState.screenState] — see
 * that extension's KDoc for why the mutation-only [MemberProfileError.RoleUpdateFailed] variant
 * is deliberately EXCLUDED from the derivation. See API.md#state.
 */
@Serializable
sealed interface MemberProfileScreenState {
    @Serializable
    data object Loading : MemberProfileScreenState

    @Serializable
    data object Content : MemberProfileScreenState

    @Serializable
    data object Error : MemberProfileScreenState
}

/**
 * Error taxonomy for the member-profile composite read + role-update write — verbatim mirror of
 * `ui.yaml#state_model.errors.types`. [messageKey] is a composeResources string-resource id
 * (never a raw hardcoded English string, per RULE-IMPL-NO-HARDCODED-STRING-001) resolved by the
 * Screen layer.
 *
 * **[NotFound] reachability gap (flagged, not invented around) — same documented class as
 * `GroupDashboardError.NotFound`:** `MemberProfileStore`'s `MemberProfileFetchException(networkError:
 * NetworkError)` message is `"Member profile fetch failed: $networkError"` — no parseable 3-digit
 * HTTP status code, so `categorize()` cannot classify it as `ErrorCategory.ClientError(404)`; it
 * falls through to `ErrorCategory.Generic` -> [Server] in production today. [handleStreamUpdated]
 * still checks for the 404 code (future-proofed for when the exception message is fixed upstream
 * to embed it).
 *
 * [RoleUpdateFailed] is deliberately **excluded** from [MemberProfileState.screenState]'s
 * derivation — `ui.yaml#components.confirm_role_button.action_contract` says a write failure
 * "emits an error snackbar and sets isUpdatingRole to false", never mentions flipping the whole
 * screen to the `error_state` component (which is only ever composed for [Loading]/[Content]
 * fetch failures per `ui.yaml#states.error.components`). See API.md#state.
 */
@Serializable
sealed interface MemberProfileError {
    val retry: Boolean
    val messageKey: String

    @Serializable
    data object Network : MemberProfileError {
        override val retry: Boolean = true
        override val messageKey: String = "error_network"
    }

    @Serializable
    data object Server : MemberProfileError {
        override val retry: Boolean = true
        override val messageKey: String = "error_server"
    }

    @Serializable
    data object NotFound : MemberProfileError {
        override val retry: Boolean = false
        override val messageKey: String = "error_not_found"
    }

    @Serializable
    data object Auth : MemberProfileError {
        override val retry: Boolean = false
        override val messageKey: String = "error_auth"
    }

    @Serializable
    data object RoleUpdateFailed : MemberProfileError {
        override val retry: Boolean = true
        override val messageKey: String = "error_role_update"
    }
}

/**
 * MVI state for `MemberProfileViewModel`. Field set + defaults are a verbatim mirror of
 * `ui.yaml#state_model.MemberProfileViewModel.state`. [member] and [accounts] are `@Transient` —
 * neither `MemberProfile` nor `MemberAccounts` (`core/model`) is `@Serializable`, and both are
 * always re-derived from [MemberProfileRepository.memberProfileStream] on (re)subscription
 * (offline-first cache via `member_profile_cache`, so nothing is visually lost across process
 * death — the Store, not this transient render state, is the durable source; mirrors
 * `GroupDashboardState`'s identical convention). [role] / [selectedRole] are plain [MemberRole]
 * enum values — kotlinx.serialization serializes Kotlin enums natively, no `@Serializable`
 * annotation needed on the enum class itself, so unlike [member]/[accounts] these survive
 * serialization without `@Transient`. [error] is `@Transient` (screen-level render concern that
 * should not survive process death, mirrors `GroupDashboardState.error` / `GroupCreateState.error`).
 *
 * **[isCurrentUserChairperson] — documented wire-source gap (flagged, not fabricated):**
 * `ui.yaml#state_model.di` names `GroupRepository.isCurrentUserChairperson(groupId):
 * Flow<Boolean>` (also declared on `api.yaml#dependencies.repositories`) as the provider, but the
 * shipped `core/data` `GroupRepository` interface exposes only `groupsPagingStream(...)` — no
 * chairperson-check method exists anywhere in this codebase. Nor does any nav-param / injectable
 * carry the CURRENT VIEWER's identity or role: `ui.yaml#nav_params` declares only `memberId` +
 * `groupId` (no `viewerRole`, unlike `group-dashboard`'s `groupId`/`viewerRole` pair), `member-list`
 * (the sole `entry_points` source) forwards only `(memberId, groupId)` via
 * `MemberListEvent.NavigateToMemberProfile`, and `SessionManager` (`core-base/security`) carries
 * no `userId`/role concept whatsoever (only session-active/inactivity-timeout bookkeeping — see
 * `SessionManager.kt`). [MemberProfileDetail.roles] is also NOT usable as a substitute: it is the
 * VIEWED member's own role assignments (keyed by `groupId` per row), not the current viewer's.
 * This is the exact same class of gap as `GroupDashboardState.isCorpusInsufficient` /
 * `isCycleEnd` (both permanently default-`false` pending a real wire field) — [isCurrentUserChairperson]
 * defaults `false` and is never set by [handleStreamUpdated]; `edit_role_button`'s
 * `visible: "{{isCurrentUserChairperson}}"` therefore never renders in production today.
 * [OnEditRoleTap] is still a REAL implementation (not a stub) with a defensive unauthorized-tap
 * guard (mirrors `GroupDashboardViewModel.handleShareOut`'s identical precedent) so the handler is
 * correct the moment the gap is closed. Reported to the caller for an idea-layer follow-up (either
 * a real `GroupRepository.isCurrentUserChairperson` wire method, or a `viewerRole`/`viewerMemberId`
 * nav-param threaded from `member-list`, RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1).
 *
 * **[meetingsAttended]/[totalMeetings]/[attendanceRate] — documented wire-source gap:**
 * `api.yaml#dtos.AttendanceSummary` declares the shape but `api.yaml#api[]` has NO
 * `get_attendance`-style endpoint, and [MemberProfileDetail]'s own KDoc confirms "attendance is
 * NOT part of this composite" with no alternate source named. Both counters therefore always
 * default `0`; [computeAttendanceRate] (below) is a fully real, independently-tested pure formula
 * that always evaluates `0.0` in production today until a real attendance endpoint is wired — same
 * documented-gap class as `GroupDashboardState.isCorpusInsufficient`.
 *
 * See API.md#state.
 */
@Serializable
@Immutable
data class MemberProfileState(
    val isLoading: Boolean = true,
    @Transient
    val member: MemberProfile? = null,
    @Transient
    val accounts: MemberAccounts? = null,
    val role: MemberRole = MemberRole.MEMBER,
    val isCurrentUserChairperson: Boolean = false,
    val isEditingRole: Boolean = false,
    val selectedRole: MemberRole? = null,
    val isUpdatingRole: Boolean = false,
    val meetingsAttended: Int = 0,
    val totalMeetings: Int = 0,
    val attendanceRate: Double = 0.0,
    @Transient
    val error: MemberProfileError? = null,
)

/**
 * Derived, single-source-of-truth screen state — see [MemberProfileScreenState] KDoc.
 * [MemberProfileError.RoleUpdateFailed] deliberately does NOT flip this to [MemberProfileScreenState.Error]
 * — see [MemberProfileError] class KDoc "deliberately excluded".
 */
val MemberProfileState.screenState: MemberProfileScreenState
    get() = when {
        error != null && error != MemberProfileError.RoleUpdateFailed -> MemberProfileScreenState.Error
        isLoading -> MemberProfileScreenState.Loading
        else -> MemberProfileScreenState.Content
    }

/**
 * One-shot side effects emitted by `MemberProfileViewModel` — verbatim mirror of
 * `ui.yaml#state_model.MemberProfileViewModel.events.members`. No flagged additions were needed
 * for this feature — unlike `group-dashboard`'s `OnBack`, `top_bar.on_navigation_click` here
 * explicitly targets "the member-list screen, passing groupId", which the already-declared
 * [NavigateToMemberList] covers verbatim. See API.md#events.
 */
sealed interface MemberProfileEvent {
    data class NavigateToMemberList(val groupId: String) : MemberProfileEvent
    data class NavigateToSavingsDetail(val memberId: String, val groupId: String) : MemberProfileEvent
    data class ShowSnackbar(val message: String) : MemberProfileEvent
}

/**
 * User intents dispatched to `MemberProfileViewModel`. The 7 top-level members are a verbatim
 * mirror of `ui.yaml#state_model.MemberProfileViewModel.actions.members` —
 * RULE-IMPL-DEAD-CLICKABLE-001 Rule 1. [Internal] is the sanctioned async-result-routing
 * sub-interface (never a user intent) per `training-layer/TRAINING_MASTER.yaml#patterns.actions`.
 * See API.md#actions.
 */
sealed interface MemberProfileAction {
    data object OnEditRoleTap : MemberProfileAction
    data class OnRoleSelected(val role: MemberRole) : MemberProfileAction
    data object OnConfirmRoleChange : MemberProfileAction
    data object OnDismissRoleEdit : MemberProfileAction
    data object OnViewSavings : MemberProfileAction
    data object Retry : MemberProfileAction
    data object OnBack : MemberProfileAction

    /** Async stream / mutation results — routed via `trySendAction`, never dispatched by the UI. */
    sealed interface Internal : MemberProfileAction {
        data class StreamUpdated(val screenState: ScreenState<MemberProfileDetail>) : Internal
        data class RoleUpdateResult(
            val requestedRole: MemberRole,
            val result: NetworkResult<UpdateMemberRoleResult, NetworkError>,
        ) : Internal
    }
}

/**
 * Pure derivation of the attendance progress fraction (`ui.yaml#components.attendance_progress_bar
 * .value: "{{attendanceRate}}"`) — `meetingsAttended / totalMeetings`, clamped to `[0.0, 1.0]` and
 * defensively `0.0` when [totalMeetings] is non-positive (avoids a division-by-zero `NaN` leaking
 * into the progress bar). Exposed top-level (rather than inlined into [MemberProfileViewModel]) so
 * the formula is independently unit-testable — see [MemberProfileState] class KDoc for why this
 * always evaluates `0.0` end-to-end in production today (no wire source for either counter).
 */
internal fun computeAttendanceRate(meetingsAttended: Int, totalMeetings: Int): Double =
    if (totalMeetings <= 0) 0.0 else (meetingsAttended.toDouble() / totalMeetings).coerceIn(0.0, 1.0)

/**
 * Pure derivation of the viewed member's role WITHIN the group being viewed
 * (`ui.yaml#components.role_chip.label: "{{role}}"`) — picks the [MemberRoleInfo] row whose
 * [MemberRoleInfo.groupId] matches the nav-arg [groupId] (a member can hold different roles across
 * different group memberships, since [MemberProfileDetail.roles] is a `List`), defaulting to
 * [MemberRole.MEMBER] (`ui.yaml#state_model.state.role.default`) when no row matches or [groupId]
 * is not a parseable `Long`. Exposed top-level so the formula is independently unit-testable.
 */
internal fun resolveCurrentRole(roles: List<MemberRoleInfo>, groupId: String): MemberRole {
    val groupIdLong = groupId.toLongOrNull() ?: return MemberRole.MEMBER
    return roles.firstOrNull { it.groupId == groupIdLong }?.role ?: MemberRole.MEMBER
}

/**
 * MVI processor for the member-profile screen (`business_logic.kind: aggregator` per ui.yaml — a
 * client-side parallel fan-in of 3 companion reads via [MemberProfileRepository.memberProfileStream]'s
 * offline-first [ScreenDataStream], so the SP-04 AC-7 analytics/crashReporter injection pair
 * applies — aggregator is NOT `crud`/`nav_only`, per RULE-IDEA-IMPL-INTELLIGENCE-001 AC-03i).
 * `FieldEncryptor` (`core-base/security`) is intentionally NOT injected: `data-flow.yaml` declares
 * no `pii_columns` entry for this screen (mirrors `GroupDashboardViewModel`'s identical omission
 * note).
 *
 * [sessionManager] is declared in `ui.yaml#state_model.di` and handles the real
 * `ScreenState.Unauthenticated` -> `sessionManager.endSession()` branch in [handleStreamUpdated]
 * — mirrors `GroupDashboardViewModel`'s identical wiring. `NetworkMonitor` is ALSO declared in
 * `ui.yaml#state_model.di` but is not injected here directly — it is already composed inside
 * `MemberProfileRepositoryImpl.memberProfileStream` (`Store.asScreenStream(networkMonitor = ...,
 * ...)`), matching every other composite/aggregator ViewModel's identical precedent of not
 * re-injecting it at the ViewModel layer. `ui.yaml#state_model.di` also separately lists
 * `MemberRepository` / `GroupRepository` — the shipped data layer instead exposes ONE composite
 * [MemberProfileRepository] (`memberProfileStream` + `updateMemberRole`) that already fans the
 * three companion reads in parallel (see `MemberProfileStore.kt` KDoc); see [MemberProfileState]
 * class KDoc for the separate, more consequential `GroupRepository.isCurrentUserChairperson` gap
 * this drift produces.
 *
 * [memberId] and [groupId] are the `ui.yaml#nav_params` (`memberId`, `groupId`) forwarded from
 * `member-list`'s `MemberListEvent.NavigateToMemberProfile(memberId, groupId)` via Koin
 * `parametersOf(memberId, groupId)` (see `di.MemberProfileModule`).
 *
 * See API.md#viewmodel.
 */
internal class MemberProfileViewModel(
    private val repository: MemberProfileRepository,
    private val sessionManager: SessionManager,
    private val crashReporter: CrashReporter,
    private val analytics: KptAnalyticsTracker,
    private val memberId: String,
    private val groupId: String,
) : BaseViewModel<MemberProfileState, MemberProfileEvent, MemberProfileAction>(
    initialState = MemberProfileState(),
) {

    /** Fixed-key offline-first stream for [memberId] — see class KDoc. */
    private val profileStream: ScreenDataStream<MemberProfileDetail> =
        repository.memberProfileStream(clientId = memberId, scope = viewModelScope)

    init {
        crashReporter.recordMessage(
            message = "feature=member-profile screen=member-profile-screen memberId=$memberId groupId=$groupId",
            level = CrashSeverity.Debug,
        )
        analytics.trackClientOperation(operation = "view", clientId = memberId)
        viewModelScope.launch {
            profileStream.state.collect { screenState ->
                trySendAction(MemberProfileAction.Internal.StreamUpdated(screenState))
            }
        }
    }

    override fun handleAction(action: MemberProfileAction) {
        when (action) {
            MemberProfileAction.OnEditRoleTap -> handleEditRoleTap()
            is MemberProfileAction.OnRoleSelected -> handleRoleSelected(action.role)
            MemberProfileAction.OnConfirmRoleChange -> handleConfirmRoleChange()
            MemberProfileAction.OnDismissRoleEdit -> handleDismissRoleEdit()
            MemberProfileAction.OnViewSavings -> handleViewSavings()
            MemberProfileAction.Retry -> handleRetry()
            MemberProfileAction.OnBack -> handleBack()
            is MemberProfileAction.Internal.StreamUpdated -> handleStreamUpdated(action.screenState)
            is MemberProfileAction.Internal.RoleUpdateResult -> handleRoleUpdateResult(action.requestedRole, action.result)
        }
    }

    // -- Edit Role tap (chairperson-only) — ui.yaml effect: transform_state --------------------------

    private fun handleEditRoleTap() {
        if (!state.isCurrentUserChairperson) {
            // Defensive guard only — `edit_role_button.visible` already restricts rendering to
            // chairpersons; a tap from any other viewer can only reach here via an
            // accessibility-service bypass of the hidden Compose state, never the wired happy
            // path (RULE-IMPL-DEAD-CLICKABLE-001 Rule 2 — logged, not silently swallowed; mirrors
            // `GroupDashboardViewModel.handleShareOut`'s identical unauthorized-role precedent).
            Logger.w(TAG) { "OnEditRoleTap dispatched while isCurrentUserChairperson=false memberId=$memberId — ignoring" }
            crashReporter.recordMessage(
                message = "member-profile: OnEditRoleTap dispatched by non-chairperson memberId=$memberId groupId=$groupId",
                level = CrashSeverity.Warning,
            )
            return
        }
        Logger.i(TAG) { "OnEditRoleTap memberId=$memberId currentRole=${state.role}" }
        updateState { copy(isEditingRole = true, selectedRole = role) }
    }

    // -- Role option tap in the bottom sheet — ui.yaml effect: transform_state -----------------------

    private fun handleRoleSelected(role: MemberRole) {
        updateState { copy(selectedRole = role) }
    }

    // -- Dismiss the bottom sheet without persisting — ui.yaml effect: transform_state ----------------

    private fun handleDismissRoleEdit() {
        updateState { copy(isEditingRole = false, selectedRole = null) }
    }

    // -- Confirm role change — ui.yaml effect: call_api (update_member_role) --------------------------

    private fun handleConfirmRoleChange() {
        val selected = state.selectedRole
        if (selected == null) {
            Logger.w(TAG) { "OnConfirmRoleChange dispatched with no selectedRole memberId=$memberId — ignoring" }
            return
        }
        if (selected == state.role) {
            // "Validates selectedRole differs from current role" (ui.yaml action_contract) — no
            // real change, so the sheet closes without burning a network call.
            Logger.i(TAG) { "OnConfirmRoleChange: selectedRole unchanged ($selected) memberId=$memberId — dismissing" }
            updateState { copy(isEditingRole = false, selectedRole = null) }
            return
        }
        val groupIdLong = groupId.toLongOrNull()
        if (groupIdLong == null) {
            Logger.e(TAG) { "OnConfirmRoleChange blocked: groupId nav-arg '$groupId' is not a parseable Long" }
            crashReporter.recordMessage(
                message = "member-profile: OnConfirmRoleChange blocked, invalid groupId=$groupId memberId=$memberId",
                level = CrashSeverity.Error,
            )
            sendEvent(MemberProfileEvent.ShowSnackbar(message = MemberProfileError.RoleUpdateFailed.messageKey))
            return
        }

        updateState { copy(isUpdatingRole = true) }
        analytics.trackClientOperation(operation = "update_role", clientId = memberId)
        viewModelScope.launch {
            val request = UpdateMemberRoleRequest(
                role = selected,
                groupId = groupIdLong,
                assignedDate = todayIsoDate(),
            )
            val result = repository.updateMemberRole(clientId = memberId, request = request)
            trySendAction(MemberProfileAction.Internal.RoleUpdateResult(requestedRole = selected, result = result))
        }
    }

    // -- View Savings (savings_history_card.view_full_history_button) — ui.yaml effect: navigate -----

    private fun handleViewSavings() {
        analytics.trackClientOperation(operation = "view_savings", clientId = memberId)
        Logger.i(TAG) { "OnViewSavings memberId=$memberId groupId=$groupId" }
        sendEvent(MemberProfileEvent.NavigateToSavingsDetail(memberId, groupId))
    }

    // -- Error-state retry (data-flow.yaml on_interact retry_after_error) ------------------------------

    private fun handleRetry() {
        Logger.i(TAG) { "retry tapped — re-dispatching member-profile companion fetch memberId=$memberId" }
        updateState { copy(error = null, isLoading = true) }
        profileStream.retry()
    }

    // -- Back navigation (top_bar.on_navigation_click -> member-list) ---------------------------------

    private fun handleBack() {
        Logger.i(TAG) { "OnBack tapped memberId=$memberId groupId=$groupId" }
        sendEvent(MemberProfileEvent.NavigateToMemberList(groupId))
    }

    // -- Stream -> State mapping -----------------------------------------------------------------------

    private fun handleStreamUpdated(screenState: ScreenState<MemberProfileDetail>) {
        when (screenState) {
            is ScreenState.Loading -> updateState { copy(isLoading = true, error = null) }

            is ScreenState.Empty -> updateState {
                // Never actually emitted — a single member composite is never "empty" once
                // present. Kept for ScreenState exhaustiveness only, mirrors
                // GroupDashboardViewModel's identical precedent.
                copy(isLoading = false, error = null)
            }

            is ScreenState.Content -> updateState {
                val detail = screenState.data
                copy(
                    isLoading = false,
                    member = detail.member,
                    accounts = detail.accounts,
                    role = resolveCurrentRole(detail.roles, groupId),
                    // meetingsAttended/totalMeetings are left untouched — no wire source today
                    // (see class KDoc) — and attendanceRate is recomputed from whatever those two
                    // counters currently hold (always 0/0 in production until a real endpoint
                    // lands).
                    attendanceRate = computeAttendanceRate(meetingsAttended, totalMeetings),
                    error = null,
                )
            }

            is ScreenState.NoNetwork -> updateState {
                copy(isLoading = false, error = MemberProfileError.Network)
            }

            is ScreenState.Unauthenticated -> {
                // data-flow.yaml `401 -> navigate login`, but ui.yaml#events.members declares no
                // matching NavigateToLogin event — same documented gap class as
                // GroupDashboardViewModel's identical Unauthenticated branch.
                // sessionManager.endSession() is a REAL clear-session call; ShowSnackbar is the
                // closest declared event, emitted so the user still gets immediate feedback.
                crashReporter.recordMessage(
                    message = "member-profile: session expired (401) memberId=$memberId — clearing session",
                    level = CrashSeverity.Warning,
                )
                sessionManager.endSession()
                updateState { copy(isLoading = false, error = MemberProfileError.Auth) }
                sendEvent(MemberProfileEvent.ShowSnackbar(message = MemberProfileError.Auth.messageKey))
            }

            is ScreenState.Error -> {
                val throwable = screenState.error
                crashReporter.recordException(
                    throwable = throwable,
                    message = "member-profile: stream error memberId=$memberId isNetworkError=${screenState.isNetworkError}",
                )
                val category = categorize(throwable)
                val mapped = when {
                    screenState.isNetworkError -> MemberProfileError.Network
                    // See MemberProfileError.NotFound KDoc "reachability gap" — future-proofed,
                    // currently unreachable given MemberProfileFetchException's message shape.
                    category is ErrorCategory.ClientError && category.httpCode == 404 -> MemberProfileError.NotFound
                    else -> MemberProfileError.Server
                }
                updateState { copy(isLoading = false, error = mapped) }
            }
        }
    }

    // -- Role-update async result routing ----------------------------------------------------------

    private fun handleRoleUpdateResult(
        requestedRole: MemberRole,
        result: NetworkResult<UpdateMemberRoleResult, NetworkError>,
    ) {
        when (result) {
            is NetworkResult.Success -> {
                analytics.trackClientOperation(operation = "update_role", clientId = memberId, success = true)
                Logger.i(TAG) { "updateMemberRole succeeded memberId=$memberId newRole=$requestedRole" }
                // The store already invalidated its cache on success (`data-flow.yaml#cache.strategy:
                // invalidate`), so the still-active `profileStream` will re-fetch and reconcile;
                // updating `role` locally here just avoids a visible flash back to the stale value
                // while that re-fetch is in flight.
                updateState {
                    copy(
                        role = requestedRole,
                        isUpdatingRole = false,
                        isEditingRole = false,
                        selectedRole = null,
                    )
                }
                sendEvent(MemberProfileEvent.ShowSnackbar(message = "role_updated"))
            }
            is NetworkResult.Error -> {
                analytics.trackClientOperation(operation = "update_role", clientId = memberId, success = false)
                crashReporter.recordMessage(
                    message = "member-profile: updateMemberRole failed memberId=$memberId networkError=${result.error}",
                    level = CrashSeverity.Warning,
                )
                // 401 is the one write-failure code data-flow.yaml routes to `navigate: login`
                // (no matching event declared — same documented gap as the read-path
                // Unauthenticated branch) and is the only write failure that also flips
                // `state.error` (a real session-expiry, distinct from a benign retryable
                // RoleUpdateFailed which must NOT hide the already-loaded profile content — see
                // MemberProfileError class KDoc "deliberately excluded").
                if (result.error == NetworkError.UNAUTHORIZED) {
                    updateState { copy(isUpdatingRole = false, error = MemberProfileError.Auth) }
                } else {
                    updateState { copy(isUpdatingRole = false) }
                }
                sendEvent(MemberProfileEvent.ShowSnackbar(message = result.error.toRoleUpdateMessageKey()))
            }
        }
    }
}

/**
 * Disambiguates the transport-level [NetworkError] onto the write-path snackbar message key —
 * `data-flow.yaml#entries[interaction=confirm_role_change].error_paths`: `400`/`403`/`500` ->
 * `error_role_update`, `network.offline` -> `error_network`, `401` -> `error_auth` (paired with
 * `state.error = Auth` in [MemberProfileViewModel.handleRoleUpdateResult]). `REQUEST_TIMEOUT` is
 * treated as the connectivity-loss bucket (`NetworkError` has no dedicated "offline" value — same
 * documented gap class as `GroupCreateViewModel.toGroupCreateError`). HTTP `403` (`update_member_role`'s
 * "caller is not chairperson" case, `api.yaml#update_member_role.errors`) has no dedicated
 * [NetworkError] bucket either — `MemberProfileApiImpl`'s status-code mapper folds it into
 * [NetworkError.UNKNOWN], which correctly lands in the `RoleUpdateFailed` branch below.
 */
private fun NetworkError.toRoleUpdateMessageKey(): String = when (this) {
    NetworkError.UNAUTHORIZED -> MemberProfileError.Auth.messageKey
    NetworkError.REQUEST_TIMEOUT -> MemberProfileError.Network.messageKey
    NetworkError.BAD_REQUEST,
    NetworkError.NOT_FOUND,
    NetworkError.TOO_MANY_REQUESTS,
    NetworkError.SERIALIZATION,
    NetworkError.SERVER,
    NetworkError.UNKNOWN,
    -> MemberProfileError.RoleUpdateFailed.messageKey
}

/**
 * Today's date in `yyyy-MM-dd` (ISO-8601 date-only) for `UpdateMemberRoleRequest.assignedDate`.
 * `ui.yaml` collects no explicit effective-date input on `confirm_role_button` (only the target
 * `role`), so the confirm-time date is the real, intentional business rule — "a role change takes
 * effect the day the chairperson confirms it" — not a placeholder value. Uses `kotlin.time.Clock`
 * (not the deprecated `kotlinx.datetime.Clock`) + `kotlinx.datetime.toLocalDateTime`, mirroring
 * `PersonalDashboardScreen.kt`'s identical precedent for the current-time-zone wall date.
 */
private fun todayIsoDate(): String =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
