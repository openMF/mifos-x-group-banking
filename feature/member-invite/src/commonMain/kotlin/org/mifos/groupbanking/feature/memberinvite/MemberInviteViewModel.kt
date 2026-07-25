/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.memberinvite

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitor
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kpt.core.analytics.KptAnalyticsTracker
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.base.observability.CrashReporter
import kpt.core.base.observability.CrashSeverity
import kpt.core.base.ui.viewmodel.BaseViewModel
import org.mifos.groupbanking.core.data.repository.MemberInviteRepository
import org.mifos.groupbanking.core.model.CreateInviteRequest
import org.mifos.groupbanking.core.model.GeneratedInvite
import org.mifos.groupbanking.core.model.MemberRole
import org.mifos.groupbanking.core.model.PendingInvite
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime

// MVI stack (State/Event/Action/ViewModel/DI) for the `member-invite` feature — see
// API.md#viewmodel / #state / #actions / #events / #di for the full generated-symbol contract.
private const val TAG = "MemberInviteViewModel"

/** Invite tokens expire 7 days after issuance (`api.yaml#api.create_invite.body.expires_at`). */
private const val INVITE_VALIDITY_DAYS = 7L

/**
 * Screen-level render state for `member-invite-screen` — verbatim mirror of
 * `ui.yaml#state_model.screen_state.members` (`Content`/`Generating`/`Generated`/`Loading`/`Error`).
 * Derived only (not stored) via [MemberInviteState.deriveScreenState]. See API.md#state.
 */
sealed interface MemberInviteScreenState {
    data object Loading : MemberInviteScreenState
    data object Content : MemberInviteScreenState
    data object Generating : MemberInviteScreenState
    data object Generated : MemberInviteScreenState
    data object Error : MemberInviteScreenState
}

/**
 * Submission/read error taxonomy — verbatim mirror of `ui.yaml#state_model.errors.types`.
 * [messageKey] is a composeResources string-resource id (never a raw hardcoded English string,
 * per RULE-IMPL-NO-HARDCODED-STRING-001) resolved by the Screen layer. See API.md#state.
 */
sealed interface MemberInviteError {
    val retry: Boolean
    val messageKey: String

    data object Validation : MemberInviteError {
        override val retry: Boolean = false
        override val messageKey: String = "error_validation"
    }

    data object Network : MemberInviteError {
        override val retry: Boolean = true
        override val messageKey: String = "error_network"
    }

    data object Server : MemberInviteError {
        override val retry: Boolean = true
        override val messageKey: String = "error_server"
    }

    data object Auth : MemberInviteError {
        override val retry: Boolean = false
        override val messageKey: String = "error_auth"
    }

    data object RevokeFailure : MemberInviteError {
        override val retry: Boolean = true
        override val messageKey: String = "error_revoke"
    }
}

/**
 * MVI state for `MemberInviteViewModel`. Field set + defaults are a verbatim mirror of
 * `ui.yaml#state_model.MemberInviteViewModel.state` ([selectedRole] typed as the shared
 * [MemberRole] enum rather than the loose `String` the registry declares — same convention as
 * `MemberAddState.selectedRole`). [validationError] backs the inline email/phone field error
 * (`flow.yaml#validation_rules.emailPhone`); [error] backs the top error banner. [isOffline] is
 * driven reactively by [NetworkMonitor] so the connectivity precondition
 * (`data-flow.yaml` OnGenerateInvite offline note) is reflected live. See API.md#state.
 */
@Immutable
data class MemberInviteState(
    val groupId: String = "",
    val emailPhone: String = "",
    val selectedRole: MemberRole = MemberRole.MEMBER,
    val pendingInvites: List<PendingInvite> = emptyList(),
    val isGenerating: Boolean = false,
    val generatedCode: String? = null,
    val generatedLink: String? = null,
    val isLoadingPending: Boolean = false,
    val validationError: String? = null,
    val error: MemberInviteError? = null,
    val isOffline: Boolean = false,
)

/**
 * Derives [MemberInviteScreenState] from [MemberInviteState] — a pure function rather than a
 * stored field (mirrors `MemberAddState.deriveScreenState()`), keeping the flags the single
 * source of truth. Every screen_state member is reachable. See API.md#state.
 */
fun MemberInviteState.deriveScreenState(): MemberInviteScreenState = when {
    error != null -> MemberInviteScreenState.Error
    generatedCode != null -> MemberInviteScreenState.Generated
    isGenerating -> MemberInviteScreenState.Generating
    isLoadingPending && pendingInvites.isEmpty() -> MemberInviteScreenState.Loading
    else -> MemberInviteScreenState.Content
}

/**
 * One-shot side effects emitted by `MemberInviteViewModel` — mirror of
 * `ui.yaml#state_model.MemberInviteViewModel.events.members` plus [CopyToClipboard] (the
 * `effect: copy_clipboard` action_contract for OnCopyCode/OnCopyLink, handled by the Screen via
 * `LocalClipboardManager`). See API.md#events.
 */
sealed interface MemberInviteEvent {
    data object NavigateBack : MemberInviteEvent
    data class ShowSnackbar(val message: String) : MemberInviteEvent
    data class ShowShareSheet(val link: String, val code: String) : MemberInviteEvent
    data class CopyToClipboard(val text: String) : MemberInviteEvent
}

/**
 * User intents dispatched to `MemberInviteViewModel`. The 9 top-level members are a verbatim
 * mirror of `ui.yaml#state_model.MemberInviteViewModel.actions.members` — RULE-IMPL-DEAD-CLICKABLE-001
 * Rule 1. [Internal] is the sanctioned async-result-routing sub-interface (never a user intent).
 * See API.md#actions.
 */
sealed interface MemberInviteAction {
    data class OnEmailPhoneChange(val value: String) : MemberInviteAction
    data class OnRoleSelect(val role: MemberRole) : MemberInviteAction
    data object OnGenerateInvite : MemberInviteAction
    data object OnCopyCode : MemberInviteAction
    data object OnCopyLink : MemberInviteAction
    data object OnShareLink : MemberInviteAction
    data class OnRevokeInvite(val inviteId: Long) : MemberInviteAction
    data object OnBack : MemberInviteAction
    data object OnRetry : MemberInviteAction

    /** Async coroutine results — routed via `trySendAction`, never dispatched by the UI. */
    sealed interface Internal : MemberInviteAction {
        data class ConnectivityChanged(val online: Boolean) : Internal
        data class LoadPendingResult(val result: NetworkResult<List<PendingInvite>, NetworkError>) : Internal
        data class GenerateResult(val result: NetworkResult<GeneratedInvite, NetworkError>) : Internal
        data class RevokeResult(
            val removed: PendingInvite,
            val result: NetworkResult<Unit, NetworkError>,
        ) : Internal
    }
}

/**
 * MVI processor for the organizer-side member-invite screen (`business_logic.kind: crud`). The
 * on-mount pending-invites read + the generate/revoke mutations are consumed directly via
 * [NetworkResult] over [MemberInviteRepository] rather than a Store5 `.asScreenStream()` — invite
 * generation/revocation require connectivity and are never offline-queued
 * (`data-flow.yaml#sync_queue.entries: []`), the same Store5-free branch as `MemberAddViewModel` /
 * `MeetingConductViewModel`. Analytics + crashReporter are injected because `crud` is not
 * `nav_only` (RULE-IDEA-IMPL-INTELLIGENCE-001 AC-03i).
 *
 * See API.md#viewmodel.
 */
internal class MemberInviteViewModel(
    private val repository: MemberInviteRepository,
    private val networkMonitor: NetworkMonitor,
    private val analytics: KptAnalyticsTracker,
    private val crashReporter: CrashReporter,
    private val groupId: String,
) : BaseViewModel<MemberInviteState, MemberInviteEvent, MemberInviteAction>(
    initialState = MemberInviteState(groupId = groupId),
) {

    private var generateJob: Job? = null

    init {
        crashReporter.recordMessage(
            message = "feature=member-invite screen=member-invite-screen groupId=$groupId",
            level = CrashSeverity.Debug,
        )
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                trySendAction(MemberInviteAction.Internal.ConnectivityChanged(online))
            }
        }
        loadPendingInvites()
    }

    override fun handleAction(action: MemberInviteAction) {
        when (action) {
            is MemberInviteAction.OnEmailPhoneChange -> handleEmailPhoneChange(action.value)
            is MemberInviteAction.OnRoleSelect -> handleRoleSelect(action.role)
            MemberInviteAction.OnGenerateInvite -> handleGenerate()
            MemberInviteAction.OnCopyCode -> handleCopyCode()
            MemberInviteAction.OnCopyLink -> handleCopyLink()
            MemberInviteAction.OnShareLink -> handleShareLink()
            is MemberInviteAction.OnRevokeInvite -> handleRevoke(action.inviteId)
            MemberInviteAction.OnBack -> sendEvent(MemberInviteEvent.NavigateBack)
            MemberInviteAction.OnRetry -> handleRetry()
            is MemberInviteAction.Internal.ConnectivityChanged -> updateState { copy(isOffline = !action.online) }
            is MemberInviteAction.Internal.LoadPendingResult -> handleLoadResult(action.result)
            is MemberInviteAction.Internal.GenerateResult -> handleGenerateResult(action.result)
            is MemberInviteAction.Internal.RevokeResult -> handleRevokeResult(action.removed, action.result)
        }
    }

    // -- Field transforms (ui.yaml effect: transform_state) --------------------------------------

    private fun handleEmailPhoneChange(value: String) {
        updateState { copy(emailPhone = value, validationError = null) }
    }

    private fun handleRoleSelect(role: MemberRole) {
        updateState { copy(selectedRole = role) }
    }

    // -- On-mount / retry read (COMP-DT-003) -----------------------------------------------------

    private fun loadPendingInvites() {
        viewModelScope.launch {
            updateState { copy(isLoadingPending = true) }
            val result = repository.listPendingInvites(groupId.toLongOrNull() ?: 0L)
            trySendAction(MemberInviteAction.Internal.LoadPendingResult(result))
        }
    }

    private fun handleLoadResult(result: NetworkResult<List<PendingInvite>, NetworkError>) {
        when (result) {
            is NetworkResult.Success ->
                updateState { copy(isLoadingPending = false, pendingInvites = result.data) }
            is NetworkResult.Error -> {
                Logger.e(TAG) { "listPendingInvites failed groupId=$groupId error=${result.error}" }
                updateState { copy(isLoadingPending = false, error = result.error.toMemberInviteError()) }
            }
        }
    }

    // -- Generate invite (COMP-DT-002, ui.yaml effect: call_api) ---------------------------------

    @OptIn(ExperimentalTime::class)
    private fun handleGenerate() {
        val contact = state.emailPhone.trim()
        if (!contact.isValidEmailOrPhone()) {
            Logger.w(TAG) { "OnGenerateInvite validation failed groupId=$groupId" }
            updateState { copy(validationError = "error_validation", error = MemberInviteError.Validation) }
            return
        }

        if (!networkMonitor.isOnline.value) {
            Logger.w(TAG) { "OnGenerateInvite attempted while offline groupId=$groupId" }
            crashReporter.recordMessage(
                message = "member-invite: generate attempted while offline groupId=$groupId",
                level = CrashSeverity.Info,
            )
            updateState { copy(isOffline = true, error = MemberInviteError.Network) }
            return
        }

        generateJob?.cancel()
        generateJob = viewModelScope.launch {
            updateState { copy(isGenerating = true, error = null, validationError = null) }
            analytics.trackClientOperation(operation = "invite")
            val request = CreateInviteRequest(
                groupId = groupId.toLongOrNull() ?: 0L,
                invitedEmailPhone = contact,
                roleToAssign = state.selectedRole,
                expiresAt = Clock.System.now().plus(INVITE_VALIDITY_DAYS.days).toString(),
            )
            val result = repository.createInvite(request)
            trySendAction(MemberInviteAction.Internal.GenerateResult(result))
        }
    }

    private fun handleGenerateResult(result: NetworkResult<GeneratedInvite, NetworkError>) {
        when (result) {
            is NetworkResult.Success -> {
                val invite = result.data
                analytics.trackClientOperation(operation = "invite", success = true)
                Logger.i(TAG) { "createInvite succeeded rowId=${invite.rowId} groupId=$groupId" }
                updateState {
                    copy(
                        isGenerating = false,
                        generatedCode = invite.token,
                        generatedLink = invite.inviteLink,
                        error = null,
                    )
                }
                // Refresh the pending list so the newly generated invite appears
                // (data-flow.yaml OnGenerateInvite reload note).
                loadPendingInvites()
            }
            is NetworkResult.Error -> {
                analytics.trackClientOperation(operation = "invite", success = false)
                crashReporter.recordMessage(
                    message = "member-invite: createInvite failed groupId=$groupId networkError=${result.error}",
                    level = CrashSeverity.Warning,
                )
                updateState { copy(isGenerating = false, error = result.error.toMemberInviteError()) }
            }
        }
    }

    // -- Revoke invite (COMP-DT-005, optimistic) -------------------------------------------------

    private fun handleRevoke(inviteId: Long) {
        val removed = state.pendingInvites.firstOrNull { it.rowId == inviteId } ?: return
        // Optimistic removal for UX snappiness (data-flow.yaml OnRevokeInvite).
        updateState { copy(pendingInvites = pendingInvites.filterNot { it.rowId == inviteId }, error = null) }
        viewModelScope.launch {
            val result = repository.revokeInvite(groupId = groupId.toLongOrNull() ?: 0L, rowId = inviteId)
            trySendAction(MemberInviteAction.Internal.RevokeResult(removed = removed, result = result))
        }
    }

    private fun handleRevokeResult(removed: PendingInvite, result: NetworkResult<Unit, NetworkError>) {
        when (result) {
            is NetworkResult.Success -> {
                Logger.i(TAG) { "revokeInvite succeeded rowId=${removed.rowId} groupId=$groupId" }
                sendEvent(MemberInviteEvent.ShowSnackbar(message = "snack_invite_revoked"))
            }
            is NetworkResult.Error -> {
                Logger.e(TAG) { "revokeInvite failed rowId=${removed.rowId} error=${result.error}" }
                // Undo the optimistic removal, restoring the row in its original position.
                updateState {
                    copy(
                        pendingInvites = (pendingInvites + removed).sortedBy { it.rowId },
                        error = MemberInviteError.RevokeFailure,
                    )
                }
            }
        }
    }

    // -- Clipboard + share (ui.yaml effect: copy_clipboard / share_external) ----------------------

    private fun handleCopyCode() {
        val code = state.generatedCode ?: return
        sendEvent(MemberInviteEvent.CopyToClipboard(text = code))
        sendEvent(MemberInviteEvent.ShowSnackbar(message = "snack_code_copied"))
    }

    private fun handleCopyLink() {
        val link = state.generatedLink ?: return
        sendEvent(MemberInviteEvent.CopyToClipboard(text = link))
        sendEvent(MemberInviteEvent.ShowSnackbar(message = "snack_link_copied"))
    }

    private fun handleShareLink() {
        val link = state.generatedLink ?: return
        val code = state.generatedCode ?: return
        sendEvent(MemberInviteEvent.ShowShareSheet(link = link, code = code))
    }

    // -- Retry (ui.yaml effect: call_api) ---------------------------------------------------------

    private fun handleRetry() {
        updateState { copy(error = null) }
        loadPendingInvites()
    }
}

// ---------------------------------------------------------------------------------------------
// Top-level helpers (pure — independently unit-testable)
// ---------------------------------------------------------------------------------------------

/**
 * Email-or-E.164-phone validator — verbatim mirror of
 * `flow.yaml#validation_rules.emailPhone.pattern`. Accepts a `+`-optional 7..15 digit phone OR a
 * standard email address.
 */
private val EMAIL_OR_PHONE_REGEX =
    Regex("^(\\+?[0-9]{7,15}|[\\w._%+\\-]+@[\\w.\\-]+\\.[A-Za-z]{2,})$")

internal fun String.isValidEmailOrPhone(): Boolean = EMAIL_OR_PHONE_REGEX.matches(trim())

/**
 * Disambiguates the transport-level [NetworkError] onto [MemberInviteError] — `data-flow.yaml`
 * error_paths: `400 -> error_validation`, `401 -> Auth (navigate login)`, `network.offline/timeout
 * -> Network`, `404`/`500`/serialization -> Server. `REQUEST_TIMEOUT` is the connectivity-loss
 * bucket ([NetworkError] has no dedicated "offline" value — same documented gap class as
 * `MemberAddViewModel.toMemberAddError`).
 */
internal fun NetworkError.toMemberInviteError(): MemberInviteError = when (this) {
    NetworkError.REQUEST_TIMEOUT -> MemberInviteError.Network
    NetworkError.BAD_REQUEST -> MemberInviteError.Validation
    NetworkError.UNAUTHORIZED -> MemberInviteError.Auth
    NetworkError.TOO_MANY_REQUESTS, NetworkError.NOT_FOUND, NetworkError.SERIALIZATION,
    NetworkError.SERVER, NetworkError.UNKNOWN,
    -> MemberInviteError.Server
}
