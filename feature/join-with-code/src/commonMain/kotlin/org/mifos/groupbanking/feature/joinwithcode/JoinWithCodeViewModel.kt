/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.joinwithcode

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kpt.core.analytics.KptAnalyticsTracker
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.base.observability.CrashReporter
import kpt.core.base.observability.CrashSeverity
import kpt.core.base.ui.viewmodel.BaseViewModel
import org.mifos.groupbanking.core.data.repository.AuthRepository
import org.mifos.groupbanking.core.data.repository.InvitationRepository
import org.mifos.groupbanking.core.model.AuthSession
import org.mifos.groupbanking.core.model.GroupPreview
import org.mifos.groupbanking.core.model.JoinGroupResult

// MVI stack (State/Event/Action/ViewModel/DI) for the `join-with-code` feature — see
// API.md#viewmodel / #state / #actions / #events / #di for the full generated-symbol contract.
private const val TAG = "JoinWithCodeViewModel"
private const val CODE_LENGTH = 6
private val INVITE_CODE_REGEX = Regex("^[A-Z0-9]{6}$")

/**
 * Repository-layer KNOWN CONTRACT GAP (see [InvitationRepository.joinGroup] KDoc): the
 * companion `validate_invite_token` response carries no `id`/`rowId` field, so the
 * mark-invitation-accepted `rowId` cannot be derived client-side from [InvitationRepository]'s
 * exposed [org.mifos.groupbanking.core.model.Invitation]. `0L` is a documented placeholder —
 * `InvitationRepositoryImpl.joinGroup` already treats a failed mark-accepted call as
 * non-fatal/best-effort (association success is the only thing that matters for the join to
 * complete), so this placeholder cannot silently corrupt the join outcome, only the
 * best-effort bookkeeping call. Flagged in the generation report; resolving requires either the
 * companion server adding a `rowId` to the `validate_invite_token` response, or a documented
 * alternate datatable-row lookup not currently modeled in `api.yaml`.
 */
private const val UNRESOLVED_MARK_ACCEPTED_ROW_ID = 0L

/**
 * Local field-entry status for the invite-code text field — verbatim mirror of
 * `ui.yaml#state_model.JoinWithCodeViewModel.state.inviteStatus` (see component visibility
 * bindings `inviteStatus == VALIDATING` / `inviteStatus == VALID`). See API.md#state.
 */
enum class InviteStatus { IDLE, VALIDATING, VALID, INVALID }

/**
 * Screen-level render state for `join-with-code-screen` — verbatim mirror of
 * `ui.yaml#state_model.screen_state.members`. [Success] is intentionally **not** produced by
 * [JoinWithCodeState.deriveScreenState] — per `ui.yaml#states.success.description` it is a
 * transient marker superseded in the same frame by the `NavigateToPersonalDashboard` event, so the
 * derivation collapses it into [Preview]'s field-shape until the event fires and the Screen
 * layer navigates away. The member is still declared (for exhaustive `when` completeness on the
 * Screen layer) rather than omitted. See API.md#state.
 */
sealed interface JoinWithCodeScreenState {
    data object Initial : JoinWithCodeScreenState
    data object Validating : JoinWithCodeScreenState
    data object Preview : JoinWithCodeScreenState
    data object Joining : JoinWithCodeScreenState
    data object Success : JoinWithCodeScreenState
    data object ErrorInvalidCode : JoinWithCodeScreenState
    data object ErrorExpired : JoinWithCodeScreenState
    data object ErrorAlreadyMember : JoinWithCodeScreenState
    data object ErrorNetwork : JoinWithCodeScreenState
}

/**
 * Invite-flow error taxonomy — verbatim mirror of `ui.yaml#state_model.errors.types`.
 * [messageKey] is a composeResources string-resource id (never a raw hardcoded English string,
 * per RULE-IMPL-NO-HARDCODED-STRING-001) resolved by the Screen layer. See API.md#state.
 */
@Serializable
sealed interface JoinError {
    val retry: Boolean
    val messageKey: String

    @Serializable
    data object InvalidCode : JoinError {
        override val retry: Boolean = true
        override val messageKey: String = "error_invalid_code"
    }

    @Serializable
    data object ExpiredCode : JoinError {
        override val retry: Boolean = false
        override val messageKey: String = "error_expired"
    }

    @Serializable
    data object AlreadyMember : JoinError {
        override val retry: Boolean = false
        override val messageKey: String = "error_already_member"
    }

    @Serializable
    data object Network : JoinError {
        override val retry: Boolean = true
        override val messageKey: String = "error_network"
    }

    @Serializable
    data object Auth : JoinError {
        override val retry: Boolean = false
        override val messageKey: String = "error_auth"
    }
}

/**
 * MVI state for `JoinWithCodeViewModel`. Field set + defaults are a verbatim mirror of
 * `ui.yaml#state_model.JoinWithCodeViewModel.state`. [error] is `@Transient` (screen-level
 * render concern, mirrors `LoginSignupState`'s convention — should not survive process death).
 * See API.md#state.
 */
@Serializable
@Immutable
data class JoinWithCodeState(
    val inviteCode: String = "",
    val inviteStatus: InviteStatus = InviteStatus.IDLE,
    @Transient
    val groupPreview: GroupPreview? = null,
    val isJoining: Boolean = false,
    @Transient
    val error: JoinError? = null,
)

/**
 * Derives [JoinWithCodeScreenState] from [JoinWithCodeState] per the task contract ("ScreenState
 * ... derive from state") — no separately-tracked screen-state field. See [JoinWithCodeScreenState]
 * KDoc for why [JoinWithCodeScreenState.Success] is unreachable here.
 */
fun JoinWithCodeState.deriveScreenState(): JoinWithCodeScreenState = when (error) {
    JoinError.InvalidCode -> JoinWithCodeScreenState.ErrorInvalidCode
    JoinError.ExpiredCode -> JoinWithCodeScreenState.ErrorExpired
    JoinError.AlreadyMember -> JoinWithCodeScreenState.ErrorAlreadyMember
    JoinError.Network, JoinError.Auth -> JoinWithCodeScreenState.ErrorNetwork
    null -> when {
        inviteStatus == InviteStatus.VALIDATING -> JoinWithCodeScreenState.Validating
        inviteStatus == InviteStatus.VALID && isJoining -> JoinWithCodeScreenState.Joining
        inviteStatus == InviteStatus.VALID && !isJoining -> JoinWithCodeScreenState.Preview
        else -> JoinWithCodeScreenState.Initial
    }
}

/**
 * One-shot side effects emitted by `JoinWithCodeViewModel` — verbatim mirror of
 * `ui.yaml#state_model.JoinWithCodeViewModel.events`. See API.md#events.
 */
sealed interface JoinWithCodeEvent {
    // F5 (2026-08-01): post-join landing reconciled to personal-dashboard per member role (was
    // NavigateToGroupDashboard). Verbatim mirror of `ui.yaml#state_model.events.NavigateToPersonalDashboard`
    // — no params, personal-dashboard nav_params:{} (identity + joined group resolve from the auth token).
    data object NavigateToPersonalDashboard : JoinWithCodeEvent
    data class NavigateToLoginSignup(val pendingInviteCode: String) : JoinWithCodeEvent
    data object NavigateBack : JoinWithCodeEvent
    data class ShowSnackbar(val message: String) : JoinWithCodeEvent
}

/**
 * User intents dispatched to `JoinWithCodeViewModel`. The 5 top-level members are a verbatim
 * mirror of `ui.yaml#state_model.JoinWithCodeViewModel.actions.members` — RULE-IMPL-DEAD-CLICKABLE-001
 * Rule 1. [Internal] is the sanctioned async-result-routing sub-interface (never a user intent)
 * per `training-layer/TRAINING_MASTER.yaml#patterns.actions`. See API.md#actions.
 */
sealed interface JoinWithCodeAction {
    data class OnCodeChange(val value: String) : JoinWithCodeAction
    data object OnValidateCode : JoinWithCodeAction
    data object OnConfirmJoin : JoinWithCodeAction
    data object OnBack : JoinWithCodeAction
    data object OnRetry : JoinWithCodeAction

    /** Async coroutine results — routed via `trySendAction`, never dispatched by the UI. */
    sealed interface Internal : JoinWithCodeAction {
        data class SessionChecked(val session: AuthSession?) : Internal
        data class ValidateFailed(val error: NetworkError) : Internal
        data object ValidateExpired : Internal
        data object ValidateAlreadyMember : Internal
        data class PreviewLoaded(val preview: GroupPreview) : Internal
        data class JoinSucceeded(val result: JoinGroupResult) : Internal
        data class JoinFailed(val error: NetworkError) : Internal
    }
}

/** Disambiguates [NetworkError] -> [JoinError] mapping — `OnConfirmJoin`'s 403/`UNKNOWN` folds
 * to [JoinError.InvalidCode] ("group closed / role forbidden" per `data-flow.yaml` notes) while
 * `OnValidateCode`'s `UNKNOWN` falls back to [JoinError.Network] — the two call sites disagree
 * on the same transport bucket, same class of context-dependent mapping as
 * `LoginSignupViewModel.AuthErrorContext`. */
private enum class JoinErrorContext { ValidateCode, ConfirmJoin }

/**
 * MVI processor for the invite-code entry + group-preview confirmation screen
 * (`business_logic.kind: processor` per ui.yaml — a validate-then-mutate flow, so
 * [InvitationRepository] is consumed directly via [NetworkResult] rather than a Store5
 * `.asScreenStream()`, per RULE-IMPLEMENT-STORE5-001 / RULE-IDEA-IMPL-INTELLIGENCE-001).
 *
 * **SP-04 hooks (AC-7):** [analytics] (`core/analytics`) records every validate/join attempt via
 * `trackGroupOperation(operation = "join", ...)` — the shipped domain-specific tracker method
 * (no generic `trackOperation()` exists on the class, same drift already documented on
 * `LoginSignupViewModel`). [crashReporter] (`core-base/observability`) receives a
 * `setUser(session?.userId)` breadcrumb on every [AuthRepository.currentSession] emission plus a
 * `recordMessage` breadcrumb on every validate/join failure. `FieldEncryptor`
 * (`core-base/security`) is intentionally NOT injected — this screen never persists a raw PII
 * field itself (no `pii_columns` in `data-flow.yaml`; the invite code is a short-lived,
 * single-use, non-PII token).
 *
 * [inviteCode] is the optional deep-link nav-arg (`ui.yaml#nav_params.inviteCode`) — supplied by
 * the Koin `parametersOf(...)` call the Route composable makes (see `di.JoinWithCodeModule`).
 *
 * See API.md#viewmodel.
 */
internal class JoinWithCodeViewModel(
    private val invitationRepository: InvitationRepository,
    private val authRepository: AuthRepository,
    private val analytics: KptAnalyticsTracker,
    private val crashReporter: CrashReporter,
    inviteCode: String? = null,
) : BaseViewModel<JoinWithCodeState, JoinWithCodeEvent, JoinWithCodeAction>(
    initialState = JoinWithCodeState(),
) {

    private var authSession: AuthSession? = null
    private var validateJob: Job? = null
    private var joinJob: Job? = null

    init {
        crashReporter.recordMessage(
            message = "feature=join-with-code screen=join-with-code-screen",
            level = CrashSeverity.Debug,
        )
        viewModelScope.launch {
            authRepository.currentSession.collect { session ->
                trySendAction(JoinWithCodeAction.Internal.SessionChecked(session))
            }
        }

        // ui.yaml#nav_params.inviteCode / flow.yaml#on_mount.parse_deep_link_code — a 6-char
        // deep-link code pre-fills the field AND auto-triggers validation, skipping manual entry.
        val deepLinkCode = inviteCode?.uppercase()?.takeIf { INVITE_CODE_REGEX.matches(it) }
        if (deepLinkCode != null) {
            Logger.i(TAG) { "deep-link invite code pre-filled — auto-validating" }
            updateState { copy(inviteCode = deepLinkCode) }
            performValidate()
        }
    }

    override fun handleAction(action: JoinWithCodeAction) {
        when (action) {
            is JoinWithCodeAction.OnCodeChange -> handleCodeChange(action.value)
            JoinWithCodeAction.OnValidateCode -> handleValidateCode()
            JoinWithCodeAction.OnConfirmJoin -> handleConfirmJoin()
            JoinWithCodeAction.OnBack -> handleBack()
            JoinWithCodeAction.OnRetry -> handleRetry()
            is JoinWithCodeAction.Internal.SessionChecked -> handleSessionChecked(action.session)
            is JoinWithCodeAction.Internal.ValidateFailed -> handleValidateFailed(action.error)
            JoinWithCodeAction.Internal.ValidateExpired -> handleValidateExpired()
            JoinWithCodeAction.Internal.ValidateAlreadyMember -> handleValidateAlreadyMember()
            is JoinWithCodeAction.Internal.PreviewLoaded -> handlePreviewLoaded(action.preview)
            is JoinWithCodeAction.Internal.JoinSucceeded -> handleJoinSucceeded(action.result)
            is JoinWithCodeAction.Internal.JoinFailed -> handleJoinFailed(action.error)
        }
    }

    // -- Field transform (ui.yaml effect: transform_state) ---------------------------------------

    private fun handleCodeChange(value: String) {
        val sanitized = value.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }.take(CODE_LENGTH)
        updateState {
            copy(inviteCode = sanitized, inviteStatus = InviteStatus.IDLE, error = null, groupPreview = null)
        }
        if (sanitized.length == CODE_LENGTH) {
            Logger.i(TAG) { "invite code reached $CODE_LENGTH chars — auto-validating" }
            performValidate()
        }
    }

    // -- Validate (ui.yaml effect: call_api) ------------------------------------------------------

    private fun handleValidateCode() = performValidate()

    private fun performValidate() {
        val code = state.inviteCode
        if (!INVITE_CODE_REGEX.matches(code)) {
            // Defensive guard only — validate_button/auto-validate are both gated on exactly 6
            // sanitized chars, so this is unreachable from the wired UI paths, never a stub.
            Logger.w(TAG) { "performValidate invoked with a non-conforming code — ignoring" }
            return
        }

        validateJob?.cancel()
        validateJob = viewModelScope.launch {
            updateState { copy(inviteStatus = InviteStatus.VALIDATING, error = null, groupPreview = null) }

            when (val invitationResult = invitationRepository.validateCode(code)) {
                is NetworkResult.Error -> trySendAction(JoinWithCodeAction.Internal.ValidateFailed(invitationResult.error))
                is NetworkResult.Success -> {
                    val invitation = invitationResult.data
                    when {
                        invitation.isExpired() -> trySendAction(JoinWithCodeAction.Internal.ValidateExpired)
                        invitation.isAlreadyUsed -> trySendAction(JoinWithCodeAction.Internal.ValidateAlreadyMember)
                        else -> when (val previewResult = invitationRepository.fetchGroupPreview(invitation.groupId)) {
                            is NetworkResult.Success ->
                                trySendAction(JoinWithCodeAction.Internal.PreviewLoaded(previewResult.data))
                            is NetworkResult.Error ->
                                trySendAction(JoinWithCodeAction.Internal.ValidateFailed(previewResult.error))
                        }
                    }
                }
            }
        }
    }

    // -- Confirm join (ui.yaml effect: call_api) --------------------------------------------------

    private fun handleConfirmJoin() {
        val preview = state.groupPreview
        if (preview == null) {
            Logger.w(TAG) { "OnConfirmJoin dispatched without a loaded groupPreview — ignoring (confirm_join_button is gated on inviteStatus==VALID)" }
            return
        }

        val session = authSession
        if (session == null) {
            // flow.yaml#on_confirm_join.unauthenticated — persist the code via the nav param and
            // redirect; the login-signup screen resumes the join post-auth with pendingInviteCode.
            Logger.i(TAG) { "OnConfirmJoin: unauthenticated — redirecting to login-signup" }
            analytics.trackGroupOperation(
                operation = "join",
                groupId = preview.groupId.toString(),
                groupType = preview.groupType.name,
                memberCount = preview.memberCount,
                success = false,
            )
            sendEvent(JoinWithCodeEvent.NavigateToLoginSignup(pendingInviteCode = state.inviteCode))
            return
        }

        val clientId = session.userId.toLongOrNull()
        if (clientId == null) {
            // AuthSession.userId is a String (COMP-AUTH contract) but associate_client_to_group
            // requires the numeric Fineract clientId — a non-numeric userId means the companion
            // session cannot be used to join a group. Recorded (engineering-visible) and routed
            // through the same recovery path as "unauthenticated" rather than silently no-op'ing.
            crashReporter.recordMessage(
                message = "join-with-code: authenticated session.userId is not a numeric clientId — cannot associate",
                level = CrashSeverity.Error,
            )
            sendEvent(JoinWithCodeEvent.NavigateToLoginSignup(pendingInviteCode = state.inviteCode))
            return
        }

        joinJob?.cancel()
        joinJob = viewModelScope.launch {
            updateState { copy(isJoining = true, error = null) }
            val result = invitationRepository.joinGroup(
                groupId = preview.groupId,
                clientId = clientId,
                role = preview.roleToAssign,
                code = state.inviteCode,
                rowId = UNRESOLVED_MARK_ACCEPTED_ROW_ID,
            )
            when (result) {
                is NetworkResult.Success -> trySendAction(JoinWithCodeAction.Internal.JoinSucceeded(result.data))
                is NetworkResult.Error -> trySendAction(JoinWithCodeAction.Internal.JoinFailed(result.error))
            }
        }
    }

    // -- Navigation / retry (ui.yaml effect: navigate / call_api) ---------------------------------

    private fun handleBack() {
        Logger.i(TAG) { "OnBack tapped" }
        sendEvent(JoinWithCodeEvent.NavigateBack)
    }

    private fun handleRetry() {
        if (state.error?.retry != true) {
            // retry_button is only rendered when error.retry==true (InvalidCode / Network) — the
            // UI never dispatches OnRetry outside that gate; defensive guard, not a stub.
            Logger.w(TAG) { "OnRetry dispatched while error.retry != true — ignoring" }
            return
        }
        Logger.i(TAG) { "OnRetry tapped — re-dispatching validate with existing inviteCode" }
        updateState { copy(error = null) }
        performValidate()
    }

    // -- Async result routing ----------------------------------------------------------------------

    private fun handleSessionChecked(session: AuthSession?) {
        authSession = session
        crashReporter.setUser(session?.userId)
    }

    private fun handleValidateExpired() {
        Logger.i(TAG) { "invite code expired" }
        updateState { copy(inviteStatus = InviteStatus.INVALID, error = JoinError.ExpiredCode, groupPreview = null) }
    }

    private fun handleValidateAlreadyMember() {
        Logger.i(TAG) { "invite code already accepted" }
        updateState { copy(inviteStatus = InviteStatus.INVALID, error = JoinError.AlreadyMember, groupPreview = null) }
    }

    private fun handlePreviewLoaded(preview: GroupPreview) {
        Logger.i(TAG) { "group preview loaded groupId=${preview.groupId}" }
        updateState { copy(inviteStatus = InviteStatus.VALID, groupPreview = preview, error = null) }
    }

    private fun handleValidateFailed(error: NetworkError) {
        val mapped = error.toJoinError(JoinErrorContext.ValidateCode)
        Logger.e(TAG) { "validate/preview failed networkError=$error mapped=$mapped" }
        crashReporter.recordMessage(
            message = "join-with-code: OnValidateCode failed networkError=$error",
            level = CrashSeverity.Warning,
        )
        updateState { copy(inviteStatus = InviteStatus.INVALID, error = mapped, groupPreview = null) }
        if (mapped is JoinError.Auth) {
            sendEvent(JoinWithCodeEvent.NavigateToLoginSignup(pendingInviteCode = state.inviteCode))
        }
    }

    private fun handleJoinSucceeded(result: JoinGroupResult) {
        Logger.i(TAG) { "joinGroup succeeded groupId=${result.groupId}" }
        analytics.trackGroupOperation(
            operation = "join",
            groupId = result.groupId.toString(),
            success = true,
        )
        updateState { copy(isJoining = false) }
        sendEvent(JoinWithCodeEvent.NavigateToPersonalDashboard)
    }

    private fun handleJoinFailed(error: NetworkError) {
        val mapped = error.toJoinError(JoinErrorContext.ConfirmJoin)
        Logger.e(TAG) { "joinGroup failed networkError=$error mapped=$mapped" }
        analytics.trackGroupOperation(operation = "join", success = false)
        crashReporter.recordMessage(
            message = "join-with-code: OnConfirmJoin failed networkError=$error",
            level = CrashSeverity.Warning,
        )
        updateState { copy(isJoining = false, error = mapped) }
        if (mapped is JoinError.Auth) {
            sendEvent(JoinWithCodeEvent.NavigateToLoginSignup(pendingInviteCode = state.inviteCode))
        }
    }
}

/**
 * Maps the transport-level [NetworkError] onto the screen's declared [JoinError] taxonomy.
 * Context-dependent (see [JoinErrorContext] KDoc): `OnConfirmJoin`'s 403 response has no
 * dedicated [NetworkError] bucket — `InvitationApiImpl.toNetworkError` intentionally folds it
 * into [NetworkError.UNKNOWN] (documented there) — so a `ConfirmJoin`-context [NetworkError.UNKNOWN]
 * is treated as [JoinError.InvalidCode] ("group closed / role forbidden" per `data-flow.yaml`
 * `OnConfirmJoin` notes), while a `ValidateCode`-context [NetworkError.UNKNOWN] falls back to
 * [JoinError.Network].
 */
private fun NetworkError.toJoinError(context: JoinErrorContext): JoinError = when (context) {
    JoinErrorContext.ValidateCode -> when (this) {
        NetworkError.NOT_FOUND -> JoinError.InvalidCode
        NetworkError.UNAUTHORIZED -> JoinError.Auth
        NetworkError.REQUEST_TIMEOUT,
        NetworkError.SERVER,
        NetworkError.BAD_REQUEST,
        NetworkError.TOO_MANY_REQUESTS,
        NetworkError.SERIALIZATION,
        NetworkError.UNKNOWN,
        -> JoinError.Network
    }
    JoinErrorContext.ConfirmJoin -> when (this) {
        NetworkError.BAD_REQUEST -> JoinError.AlreadyMember
        NetworkError.UNAUTHORIZED -> JoinError.Auth
        NetworkError.UNKNOWN -> JoinError.InvalidCode
        NetworkError.REQUEST_TIMEOUT,
        NetworkError.SERVER,
        NetworkError.NOT_FOUND,
        NetworkError.TOO_MANY_REQUESTS,
        NetworkError.SERIALIZATION,
        -> JoinError.Network
    }
}
