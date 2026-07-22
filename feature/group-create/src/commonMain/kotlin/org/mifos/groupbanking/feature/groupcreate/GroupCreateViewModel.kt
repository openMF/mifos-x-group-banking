/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.groupcreate

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitor
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
import org.mifos.groupbanking.core.data.repository.GroupCreateRepository
import org.mifos.groupbanking.core.model.AuthSession
import org.mifos.groupbanking.core.model.ContributionMode
import org.mifos.groupbanking.core.model.ContributionModel
import org.mifos.groupbanking.core.model.CreateGroupRequest
import org.mifos.groupbanking.core.model.CreateGroupTypeConfig
import org.mifos.groupbanking.core.model.GroupCreationResult
import org.mifos.groupbanking.core.model.GroupTypeConfig
import org.mifos.groupbanking.core.model.Office
import org.mifos.groupbanking.core.model.PayoutOrderMethod
import org.mifos.groupbanking.core.model.SavingsMechanism
import org.mifos.groupbanking.core.model.ShareoutFormula

// MVI stack (State/Event/Action/ViewModel/DI) for the `group-create` feature — see
// API.md#viewmodel / #state / #actions / #events / #di for the full generated-symbol contract.
private const val TAG = "GroupCreateViewModel"
private const val MAX_GROUP_NAME_LENGTH = 60
private const val MIN_GROUP_MEMBERS = 2
private const val MIN_SHARE_COUNT = 1
private const val STEP_IDENTITY = 1
private const val STEP_RULES = 2
private const val STEP_MEMBERS = 3
private const val STEP_REVIEW = 4

/**
 * Screen-level render state for `group-create-screen` — verbatim mirror of
 * `ui.yaml#state_model.screen_state.members`. Unlike `LoginSignupState`/`JoinWithCodeState`
 * (which store an explicit `screenState`/derive one respectively), this is intentionally
 * **derived only** (see [GroupCreateState.deriveScreenState]) — `screen_state` in `ui.yaml` is
 * declared as a separate top-level concept from `state.fields`, so no field for it is stored on
 * [GroupCreateState] itself (keeps `isSubmitting`/`isSubmitSuccess`/`error` the single source of
 * truth instead of a fourth, independently-mutable flag that could drift out of sync).
 * See API.md#state.
 */
@Serializable
sealed interface GroupCreateScreenState {
    @Serializable
    data object Content : GroupCreateScreenState

    @Serializable
    data object Submitting : GroupCreateScreenState

    @Serializable
    data object Success : GroupCreateScreenState

    @Serializable
    data object Error : GroupCreateScreenState
}

/**
 * Top-level submission error taxonomy — verbatim mirror of `ui.yaml#state_model.errors.types`.
 * [messageKey] is a composeResources string-resource id (never a raw hardcoded English string,
 * per RULE-IMPL-NO-HARDCODED-STRING-001) resolved by the Screen layer.
 *
 * `Network`'s declared `fallback: queue_offline` and `Auth`'s declared `redirect: login` are
 * BEHAVIORAL contracts, not extra fields here — [GroupCreateViewModel] enacts them directly:
 * `Network` emits [GroupCreateEvent.ShowOfflineSyncDialog]; `Auth` emits
 * [GroupCreateEvent.ShowSnackbar] (KNOWN GAP: `ui.yaml#events` declares no dedicated
 * `NavigateToLogin` event for the `redirect: login` contract — flagged in the generation
 * report, folded onto `ShowSnackbar` rather than inventing an undeclared event).
 * See API.md#state.
 */
@Serializable
sealed interface GroupCreateError {
    val retry: Boolean
    val messageKey: String

    @Serializable
    data object Validation : GroupCreateError {
        override val retry: Boolean = false
        override val messageKey: String = "error_validation"
    }

    @Serializable
    data object Network : GroupCreateError {
        override val retry: Boolean = true
        override val messageKey: String = "error_network"
    }

    @Serializable
    data object Server : GroupCreateError {
        override val retry: Boolean = true
        override val messageKey: String = "error_server"
    }

    @Serializable
    data object Auth : GroupCreateError {
        override val retry: Boolean = false
        override val messageKey: String = "error_auth"
    }
}

/**
 * MVI state for `GroupCreateViewModel`. Field set + defaults are a verbatim mirror of
 * `ui.yaml#state_model.GroupCreateViewModel.state`. [typeConfig], [officeList], and [error] are
 * `@Transient` per this generation's task contract — [GroupTypeConfig] is an external
 * group-type-picker catalogue model with no `@Serializable` annotation of its own, [Office] rows
 * are a re-fetchable read-side cache, and [error] is a screen-level render concern that should
 * not survive process death (mirrors `LoginSignupState`'s convention).
 *
 * See API.md#state.
 */
@Serializable
@Immutable
data class GroupCreateState(
    val currentStep: Int = STEP_IDENTITY,
    val totalSteps: Int = STEP_REVIEW,
    @Transient
    val typeConfig: GroupTypeConfig? = null,
    val groupTypeName: String = "",
    val groupName: String = "",
    val officeId: Long? = null,
    val officeName: String = "",
    val currency: String = "KES",
    val meetingDay: String = "",
    val meetingTime: String = "",
    val shareValue: String = "",
    val shareMin: String = "1",
    val shareMax: String = "",
    val contributionAmount: String = "",
    val payoutOrderMethod: String = "FIXED_ORDER",
    val loanMultiplier: String = "3",
    val interestRate: String = "10",
    val cycleLengthMonths: String = "12",
    val fineAmount: String = "",
    val socialFundEnabled: Boolean = false,
    val socialFundPercent: String = "5",
    val maxMembers: String = "30",
    val inviteCode: String? = null,
    val isSubmitting: Boolean = false,
    val isSubmitSuccess: Boolean = false,
    @Transient
    val officeList: List<Office> = emptyList(),
    val validationErrors: Map<String, String> = emptyMap(),
    @Transient
    val error: GroupCreateError? = null,
    val isOffline: Boolean = false,
) {
    /** True when Step 2's contribution model is share-based (VSLA/SILC/ASCA — `shareValue` etc). */
    val isShareBasedContribution: Boolean
        get() = typeConfig?.contributionMode == ContributionMode.SHARE_BASED_VARIABLE

    /** True when Step 2 must additionally collect [payoutOrderMethod] (ROSCA-style rotation). */
    val isRotatingPayout: Boolean
        get() = typeConfig?.savingsMechanism == SavingsMechanism.ROTATING_PAYOUT
}

/**
 * Derives [GroupCreateScreenState] from [GroupCreateState] — see the type's KDoc for why this is
 * a pure function rather than a stored field (mirrors `JoinWithCodeState.deriveScreenState()`).
 */
fun GroupCreateState.deriveScreenState(): GroupCreateScreenState = when {
    error != null -> GroupCreateScreenState.Error
    isSubmitSuccess -> GroupCreateScreenState.Success
    isSubmitting -> GroupCreateScreenState.Submitting
    else -> GroupCreateScreenState.Content
}

/**
 * One-shot side effects emitted by `GroupCreateViewModel` — verbatim mirror of
 * `ui.yaml#state_model.GroupCreateViewModel.events`. See API.md#events.
 */
sealed interface GroupCreateEvent {
    data class NavigateToGroupDashboard(val groupId: String) : GroupCreateEvent
    data object NavigateBack : GroupCreateEvent
    data object ShowOfflineSyncDialog : GroupCreateEvent
    data class ShowSnackbar(val message: String) : GroupCreateEvent
}

/**
 * User intents dispatched to `GroupCreateViewModel`. The 21 top-level members are a verbatim
 * mirror of `ui.yaml#state_model.GroupCreateViewModel.actions.members` — RULE-IMPL-DEAD-CLICKABLE-001
 * Rule 1. [Internal] is the sanctioned async-result-routing sub-interface (never a user intent)
 * per `training-layer/TRAINING_MASTER.yaml#patterns.actions`. See API.md#actions.
 */
sealed interface GroupCreateAction {
    data class OnNameChange(val value: String) : GroupCreateAction
    data class OnOfficeSelect(val officeId: Long, val officeName: String) : GroupCreateAction
    data class OnCurrencyChange(val value: String) : GroupCreateAction
    data class OnMeetingDaySelect(val day: String) : GroupCreateAction
    data class OnMeetingTimeSelect(val time: String) : GroupCreateAction
    data class OnShareValueChange(val value: String) : GroupCreateAction
    data class OnShareMinChange(val value: String) : GroupCreateAction
    data class OnShareMaxChange(val value: String) : GroupCreateAction
    data class OnContributionAmountChange(val value: String) : GroupCreateAction
    data class OnPayoutOrderChange(val method: String) : GroupCreateAction
    data class OnLoanMultiplierChange(val value: String) : GroupCreateAction
    data class OnInterestRateChange(val value: String) : GroupCreateAction
    data class OnCycleLengthChange(val value: String) : GroupCreateAction
    data class OnFineAmountChange(val value: String) : GroupCreateAction
    data class OnSocialFundToggle(val enabled: Boolean) : GroupCreateAction
    data class OnSocialFundPercentChange(val value: String) : GroupCreateAction
    data class OnMaxMembersChange(val value: String) : GroupCreateAction
    data object OnNextStep : GroupCreateAction
    data object OnPreviousStep : GroupCreateAction
    data object OnSubmit : GroupCreateAction
    data object OnBack : GroupCreateAction

    /** Async coroutine results — routed via `trySendAction`, never dispatched by the UI. */
    sealed interface Internal : GroupCreateAction {
        data class SessionChecked(val session: AuthSession?) : Internal
        data class ConnectivityChanged(val online: Boolean) : Internal
        data class OfficesLoaded(val result: NetworkResult<List<Office>, NetworkError>) : Internal
        data class SubmitResult(val result: NetworkResult<GroupCreationResult, NetworkError>) : Internal
    }
}

/**
 * MVI processor for the 4-step, type-adaptive group-create wizard (`business_logic.kind: complex`
 * per ui.yaml — a client-validated multi-step form whose Step 4 assembles one companion
 * orchestration call, COMP-GRP-001). [GroupCreateRepository] is consumed directly via
 * [NetworkResult] rather than a Store5 `.asScreenStream()`/`.write()` — same
 * mutation-orchestration branch as `LoginSignupViewModel`/`JoinWithCodeViewModel`, per
 * RULE-IMPLEMENT-STORE5-001 / RULE-IDEA-IMPL-INTELLIGENCE-001 (see [GroupCreateRepository]
 * KDoc's own "Store5 branch" note).
 *
 * **`SessionManager` -> `AuthRepository` substitution (flagged idea-layer/source drift):**
 * `ui.yaml#state_model.di` names `SessionManager` as the provider of `userId` for group
 * orchestration, but the actual `kpt.core.base.security.SessionManager` class carries no
 * `userId` concept whatsoever (only `isSessionActive`/inactivity-timeout bookkeeping). The real,
 * proven `userId` source in this codebase — the same one `LoginSignupViewModel`/
 * `JoinWithCodeViewModel` already use — is [AuthRepository.currentSession] (`AuthSession.userId:
 * String`). This ViewModel injects [authRepository] instead of `SessionManager` and converts the
 * string `userId` to the `Long` [CreateGroupRequest.userId] expects via `toLongOrNull()`; a
 * non-numeric/missing session is treated as [GroupCreateError.Auth] (submission blocked, no
 * silent zero-fill). Flagged for idea-layer `di` list correction
 * (RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1).
 *
 * **Offline submission (KNOWN, DOCUMENTED SEAM — not a half-built outbox):** `ui.yaml` declares
 * an `offline_queue` (`sync_queue` table, operation `CREATE_GROUP_ORCHESTRATE`) and the task
 * brief names a `SyncQueueRepository` — neither exists anywhere in this codebase yet, and wiring
 * a real `DraftSubmitHandler<CreateGroupRequest, GroupCreationResult>` requires a
 * `SubmitOutbox<CreateGroupRequest>` Koin binding + `OutboxQualifiers` entry in `core/data`
 * (`RepositoryModule.kt` / `OutboxQualifiers.kt`, both `core/data` — out of this feature module's
 * ownership, and `OutboxQualifiers` currently has ZERO entries in this codebase). Rather than
 * half-build that wiring, [handleSubmit] proactively checks [networkMonitor] before attempting
 * the network call: offline -> `isOffline = true` + [GroupCreateError.Network] +
 * [GroupCreateEvent.ShowOfflineSyncDialog], with the fully-filled wizard state retained in
 * [GroupCreateState] so `OnSubmit` can simply be re-dispatched once connectivity returns (a
 * manual retry, not a persisted background sync). `CreateGroupRequest`/`CreateGroupTypeConfig`
 * were marked `@Serializable` in `core/model` (this generation) as the forward-compat step for
 * whenever the real outbox lands.
 *
 * **`shareoutFormula` gap:** `ui.yaml#nav_params.typeConfig` comments that the nav-arg carries
 * `shareout_formula`, but the actual [GroupTypeConfig] domain model (core/model, owned by
 * group-type-picker) has no such field, and [GroupCreateState] declares no wizard field for it
 * either. [buildCreateGroupRequest] defaults [CreateGroupTypeConfig.shareoutFormula] to
 * [ShareoutFormula.NONE] — flagged for idea-layer correction rather than fabricating a field on
 * a model this feature does not own.
 *
 * **SP-04 hooks (AC-7):** [analytics] (`core/analytics`) records every submit attempt via
 * `trackGroupOperation(operation = "create", ...)`. [crashReporter] (`core-base/observability`)
 * receives a `setUser(session?.userId)` breadcrumb on every [AuthRepository.currentSession]
 * emission plus a `recordMessage` breadcrumb on offline/failed submissions. No `FieldEncryptor`
 * injection — this wizard collects no PII field per `data-flow.yaml#pii_columns` (group name /
 * office / meeting schedule / financial rule values are not PII).
 *
 * See API.md#viewmodel.
 */
internal class GroupCreateViewModel(
    initialTypeConfig: GroupTypeConfig,
    private val groupCreateRepository: GroupCreateRepository,
    private val authRepository: AuthRepository,
    private val networkMonitor: NetworkMonitor,
    private val analytics: KptAnalyticsTracker,
    private val crashReporter: CrashReporter,
) : BaseViewModel<GroupCreateState, GroupCreateEvent, GroupCreateAction>(
    initialState = buildInitialState(initialTypeConfig),
) {

    private var submitJob: Job? = null
    private var currentUserId: Long? = null

    init {
        crashReporter.recordMessage(
            message = "feature=group-create screen=group-create-screen type=${initialTypeConfig.typeSlug}",
            level = CrashSeverity.Debug,
        )
        loadOffices()
        viewModelScope.launch {
            authRepository.currentSession.collect { session ->
                trySendAction(GroupCreateAction.Internal.SessionChecked(session))
            }
        }
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                trySendAction(GroupCreateAction.Internal.ConnectivityChanged(online))
            }
        }
    }

    @Suppress("CyclomaticComplexMethod")
    override fun handleAction(action: GroupCreateAction) {
        when (action) {
            is GroupCreateAction.OnNameChange -> handleNameChange(action.value)
            is GroupCreateAction.OnOfficeSelect -> handleOfficeSelect(action.officeId, action.officeName)
            is GroupCreateAction.OnCurrencyChange -> updateState { copy(currency = action.value) }
            is GroupCreateAction.OnMeetingDaySelect -> handleMeetingDaySelect(action.day)
            is GroupCreateAction.OnMeetingTimeSelect -> handleMeetingTimeSelect(action.time)
            is GroupCreateAction.OnShareValueChange -> handleShareValueChange(action.value)
            is GroupCreateAction.OnShareMinChange -> handleShareMinChange(action.value)
            is GroupCreateAction.OnShareMaxChange -> handleShareMaxChange(action.value)
            is GroupCreateAction.OnContributionAmountChange -> handleContributionAmountChange(action.value)
            is GroupCreateAction.OnPayoutOrderChange -> handlePayoutOrderChange(action.method)
            is GroupCreateAction.OnLoanMultiplierChange -> handleLoanMultiplierChange(action.value)
            is GroupCreateAction.OnInterestRateChange -> handleInterestRateChange(action.value)
            is GroupCreateAction.OnCycleLengthChange -> handleCycleLengthChange(action.value)
            is GroupCreateAction.OnFineAmountChange -> handleFineAmountChange(action.value)
            is GroupCreateAction.OnSocialFundToggle -> handleSocialFundToggle(action.enabled)
            is GroupCreateAction.OnSocialFundPercentChange -> handleSocialFundPercentChange(action.value)
            is GroupCreateAction.OnMaxMembersChange -> handleMaxMembersChange(action.value)
            GroupCreateAction.OnNextStep -> handleNextStep()
            GroupCreateAction.OnPreviousStep -> handlePreviousStep()
            GroupCreateAction.OnSubmit -> handleSubmit()
            GroupCreateAction.OnBack -> sendEvent(GroupCreateEvent.NavigateBack)
            is GroupCreateAction.Internal.SessionChecked -> handleSessionChecked(action.session)
            is GroupCreateAction.Internal.ConnectivityChanged -> handleConnectivityChanged(action.online)
            is GroupCreateAction.Internal.OfficesLoaded -> handleOfficesLoaded(action.result)
            is GroupCreateAction.Internal.SubmitResult -> handleSubmitResult(action.result)
        }
    }

    // -- Step 1: Identity field transforms (ui.yaml effect: transform_state) --------------------

    private fun handleNameChange(value: String) {
        updateState { copy(groupName = value, validationErrors = validationErrors - "groupName") }
    }

    private fun handleOfficeSelect(officeId: Long, officeName: String) {
        updateState {
            copy(officeId = officeId, officeName = officeName, validationErrors = validationErrors - "officeId")
        }
    }

    private fun handleMeetingDaySelect(day: String) {
        updateState { copy(meetingDay = day, validationErrors = validationErrors - "meetingDay") }
    }

    private fun handleMeetingTimeSelect(time: String) {
        updateState { copy(meetingTime = time, validationErrors = validationErrors - "meetingTime") }
    }

    // -- Step 2: Type-adaptive rule field transforms ---------------------------------------------

    private fun handleShareValueChange(value: String) {
        updateState { copy(shareValue = value, validationErrors = validationErrors - "shareValue") }
    }

    private fun handleShareMinChange(value: String) {
        updateState { copy(shareMin = value, validationErrors = validationErrors - "shareMin") }
    }

    private fun handleShareMaxChange(value: String) {
        updateState { copy(shareMax = value, validationErrors = validationErrors - "shareMax") }
    }

    private fun handleContributionAmountChange(value: String) {
        updateState {
            copy(contributionAmount = value, validationErrors = validationErrors - "contributionAmount")
        }
    }

    private fun handlePayoutOrderChange(method: String) {
        updateState { copy(payoutOrderMethod = method, validationErrors = validationErrors - "payoutOrderMethod") }
    }

    private fun handleLoanMultiplierChange(value: String) {
        updateState { copy(loanMultiplier = value, validationErrors = validationErrors - "loanMultiplier") }
    }

    private fun handleInterestRateChange(value: String) {
        updateState { copy(interestRate = value, validationErrors = validationErrors - "interestRate") }
    }

    private fun handleCycleLengthChange(value: String) {
        updateState { copy(cycleLengthMonths = value, validationErrors = validationErrors - "cycleLengthMonths") }
    }

    private fun handleFineAmountChange(value: String) {
        updateState { copy(fineAmount = value, validationErrors = validationErrors - "fineAmount") }
    }

    private fun handleSocialFundToggle(enabled: Boolean) {
        updateState { copy(socialFundEnabled = enabled, validationErrors = validationErrors - "socialFundPercent") }
    }

    private fun handleSocialFundPercentChange(value: String) {
        updateState {
            copy(socialFundPercent = value, validationErrors = validationErrors - "socialFundPercent")
        }
    }

    // -- Step 3: Members field transform -----------------------------------------------------------

    private fun handleMaxMembersChange(value: String) {
        updateState { copy(maxMembers = value, validationErrors = validationErrors - "maxMembers") }
    }

    // -- Wizard step navigation (ui.yaml effect: transform_state) ---------------------------------

    private fun handleNextStep() {
        val errors = validateStep(state, state.currentStep)
        if (errors.isNotEmpty()) {
            Logger.w(TAG) { "step ${state.currentStep} validation failed fields=${errors.keys}" }
            updateState { copy(validationErrors = errors) }
            return
        }
        updateState {
            copy(currentStep = (currentStep + 1).coerceAtMost(totalSteps), validationErrors = emptyMap())
        }
    }

    private fun handlePreviousStep() {
        if (state.currentStep > STEP_IDENTITY) {
            updateState { copy(currentStep = currentStep - 1, validationErrors = emptyMap()) }
        }
    }

    // -- Step 4: Submit (ui.yaml effect: call_api) -------------------------------------------------

    private fun handleSubmit() {
        if (state.currentStep != STEP_REVIEW) return

        val typeConfig = state.typeConfig
        if (typeConfig == null) {
            Logger.e(TAG) { "createGroup blocked: typeConfig nav-arg missing" }
            updateState {
                copy(
                    error = GroupCreateError.Validation,
                    validationErrors = mapOf("typeConfig" to "error_validation"),
                )
            }
            return
        }

        val errors = validateStep(state, STEP_MEMBERS)
        if (errors.isNotEmpty()) {
            updateState { copy(validationErrors = errors) }
            return
        }

        val userId = currentUserId
        if (userId == null) {
            Logger.w(TAG) { "createGroup blocked: no authenticated session" }
            updateState { copy(error = GroupCreateError.Auth) }
            sendEvent(GroupCreateEvent.ShowSnackbar(message = GroupCreateError.Auth.messageKey))
            return
        }

        submitJob?.cancel()
        submitJob = viewModelScope.launch {
            updateState { copy(isSubmitting = true, error = null, validationErrors = emptyMap()) }
            analytics.trackGroupOperation(operation = "create", groupType = state.groupTypeName)

            if (!networkMonitor.isOnline.value) {
                Logger.w(TAG) { "createGroup attempted while offline — queue-offline fallback" }
                crashReporter.recordMessage(
                    message = "group-create: submit attempted while offline",
                    level = CrashSeverity.Info,
                )
                updateState { copy(isSubmitting = false, isOffline = true, error = GroupCreateError.Network) }
                sendEvent(GroupCreateEvent.ShowOfflineSyncDialog)
                return@launch
            }

            val request = buildCreateGroupRequest(state, typeConfig, userId)
            val result = groupCreateRepository.createGroup(request)
            trySendAction(GroupCreateAction.Internal.SubmitResult(result))
        }
    }

    // -- Async result routing ----------------------------------------------------------------------

    private fun handleSessionChecked(session: AuthSession?) {
        crashReporter.setUser(session?.userId)
        currentUserId = session?.userId?.toLongOrNull()
    }

    private fun handleConnectivityChanged(online: Boolean) {
        updateState { copy(isOffline = !online) }
    }

    private fun handleOfficesLoaded(result: NetworkResult<List<Office>, NetworkError>) {
        when (result) {
            is NetworkResult.Success -> {
                Logger.i(TAG) { "offices loaded count=${result.data.size}" }
                updateState { copy(officeList = result.data) }
            }
            is NetworkResult.Error -> {
                // Non-fatal: the office dropdown stays empty; the rest of the wizard remains
                // usable. Read failures never block the wizard on an error screen.
                Logger.w(TAG) { "offices load failed networkError=${result.error}" }
                crashReporter.recordMessage(
                    message = "group-create: getOffices failed networkError=${result.error}",
                    level = CrashSeverity.Warning,
                )
            }
        }
    }

    private fun handleSubmitResult(result: NetworkResult<GroupCreationResult, NetworkError>) {
        when (result) {
            is NetworkResult.Success -> {
                val creation = result.data
                analytics.trackGroupOperation(
                    operation = "create",
                    groupId = creation.groupId,
                    groupType = state.groupTypeName,
                    success = true,
                )
                Logger.i(TAG) { "createGroup succeeded groupId=${creation.groupId}" }
                updateState {
                    copy(
                        isSubmitting = false,
                        isSubmitSuccess = true,
                        inviteCode = creation.inviteCode,
                        error = null,
                    )
                }
                sendEvent(GroupCreateEvent.NavigateToGroupDashboard(groupId = creation.groupId))
            }
            is NetworkResult.Error -> {
                analytics.trackGroupOperation(
                    operation = "create",
                    groupType = state.groupTypeName,
                    success = false,
                )
                crashReporter.recordMessage(
                    message = "group-create: createGroup failed networkError=${result.error}",
                    level = CrashSeverity.Warning,
                )
                // `isOffline` is deliberately NOT touched here — it stays solely driven by the
                // reactive `Internal.ConnectivityChanged` observer (device connectivity), never
                // inferred from a single request outcome (a REQUEST_TIMEOUT doesn't necessarily
                // mean the device is currently offline).
                val mapped = result.error.toGroupCreateError()
                updateState { copy(isSubmitting = false, error = mapped) }
                when (mapped) {
                    GroupCreateError.Network -> sendEvent(GroupCreateEvent.ShowOfflineSyncDialog)
                    GroupCreateError.Auth -> sendEvent(GroupCreateEvent.ShowSnackbar(message = mapped.messageKey))
                    GroupCreateError.Validation, GroupCreateError.Server -> Unit // inline error banner suffices
                }
            }
        }
    }

    // -- Helpers --------------------------------------------------------------------------------

    private fun loadOffices() {
        viewModelScope.launch {
            val result = groupCreateRepository.getOffices()
            trySendAction(GroupCreateAction.Internal.OfficesLoaded(result))
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Top-level helpers (called before/independent of the ViewModel instance)
// ---------------------------------------------------------------------------------------------

/**
 * Seeds [GroupCreateState] from the [GroupTypeConfig] nav-arg — `groupTypeName` for the top-bar
 * template + type badge, and the shared-rule defaults ([GroupTypeConfig.defaultLoanMultiplier] /
 * `defaultInterestRatePct` / `defaultCycleLengthMonths` / `maxMembers` /
 * [GroupTypeConfig.hasSocialFund]) pre-filled so the wizard opens with sensible, type-specific
 * values the user can still edit — matches this generation's task contract ("init from the
 * passed GroupTypeConfig -> set groupTypeName + which Step-2 fields apply").
 */
private fun buildInitialState(typeConfig: GroupTypeConfig): GroupCreateState = GroupCreateState(
    typeConfig = typeConfig,
    groupTypeName = typeConfig.displayName,
    loanMultiplier = typeConfig.defaultLoanMultiplier.toDisplayString(),
    interestRate = typeConfig.defaultInterestRatePct.toDisplayString(),
    cycleLengthMonths = typeConfig.defaultCycleLengthMonths.toString(),
    maxMembers = typeConfig.maxMembers.toString(),
    socialFundEnabled = typeConfig.hasSocialFund,
)

/** Step-scoped client validation — ui.yaml `next_button`/`submit_button.action_contract`. */
private fun validateStep(state: GroupCreateState, step: Int): Map<String, String> = when (step) {
    STEP_IDENTITY -> validateIdentityStep(state)
    STEP_RULES -> validateRulesStep(state)
    STEP_MEMBERS -> validateMembersStep(state)
    else -> emptyMap()
}

private fun validateIdentityStep(state: GroupCreateState): Map<String, String> = buildMap {
    if (state.groupName.isBlank() || state.groupName.length > MAX_GROUP_NAME_LENGTH) {
        put("groupName", "error_group_name_invalid")
    }
    if (state.officeId == null) put("officeId", "error_office_required")
    if (state.meetingDay.isBlank()) put("meetingDay", "error_meeting_day_required")
    if (state.meetingTime.isBlank()) put("meetingTime", "error_meeting_time_required")
}

private fun validateRulesStep(state: GroupCreateState): Map<String, String> = buildMap {
    if (state.isShareBasedContribution) {
        if (!state.shareValue.isPositiveNumber()) put("shareValue", "error_share_value_invalid")
        val min = state.shareMin.toIntOrNull()
        val max = state.shareMax.toIntOrNull()
        if (min == null || min < MIN_SHARE_COUNT) put("shareMin", "error_share_min_invalid")
        if (max == null || (min != null && max < min)) put("shareMax", "error_share_max_invalid")
    } else {
        if (!state.contributionAmount.isPositiveNumber()) {
            put("contributionAmount", "error_contribution_amount_invalid")
        }
    }
    if (state.isRotatingPayout && state.payoutOrderMethod.isBlank()) {
        put("payoutOrderMethod", "error_payout_order_required")
    }
    if (state.loanMultiplier.toDoubleOrNull() == null) put("loanMultiplier", "error_loan_multiplier_invalid")
    if (state.interestRate.toDoubleOrNull() == null) put("interestRate", "error_interest_rate_invalid")
    if (state.cycleLengthMonths.toIntOrNull() == null) put("cycleLengthMonths", "error_cycle_length_invalid")
    if (state.fineAmount.toDoubleOrNull() == null) put("fineAmount", "error_fine_amount_invalid")
    if (state.socialFundEnabled && state.socialFundPercent.toDoubleOrNull() == null) {
        put("socialFundPercent", "error_social_fund_percent_invalid")
    }
}

private fun validateMembersStep(state: GroupCreateState): Map<String, String> = buildMap {
    val max = state.maxMembers.toIntOrNull()
    if (max == null || max < MIN_GROUP_MEMBERS) put("maxMembers", "error_max_members_invalid")
}

private fun String.isPositiveNumber(): Boolean = toDoubleOrNull()?.let { it > 0 } ?: false

private fun Double.toDisplayString(): String =
    if (this == this.toLong().toDouble()) this.toLong().toString() else this.toString()

/**
 * Assembles the COMP-GRP-001 submission payload from the wizard's collected [state] + the
 * [typeConfig] nav-arg + the resolved [userId]. See [GroupCreateViewModel] KDoc for the two
 * documented model-shape gaps this must bridge: (1) [ContributionMode] (nav-arg catalogue enum)
 * -> [ContributionModel] (submission enum) is a deliberate reuse-vs-new-enum split already
 * documented on `CreateGroupTypeConfig`'s own KDoc, so the local mapper below is expected, not
 * invented; (2) [ShareoutFormula] has no wizard-collected source, defaults to [ShareoutFormula.NONE].
 */
private fun buildCreateGroupRequest(
    state: GroupCreateState,
    typeConfig: GroupTypeConfig,
    userId: Long,
): CreateGroupRequest {
    val typeConfigRequest = CreateGroupTypeConfig(
        groupType = typeConfig.typeSlug,
        poolModel = typeConfig.savingsMechanism,
        contributionModel = typeConfig.contributionMode.toContributionModel(),
        shareoutFormula = ShareoutFormula.NONE,
        payoutOrderMethod = state.payoutOrderMethod.toPayoutOrderMethodOrDefault(),
        shareValue = state.shareValue.toDoubleOrNull() ?: 0.0,
        contributionAmount = state.contributionAmount.toDoubleOrNull() ?: 0.0,
        socialFundEnabled = state.socialFundEnabled,
        socialFundPercent = state.socialFundPercent.toDoubleOrNull() ?: 0.0,
        cycleLengthMonths = state.cycleLengthMonths.toIntOrNull() ?: 0,
        loanMultiplier = state.loanMultiplier.toDoubleOrNull() ?: 0.0,
        interestRate = state.interestRate.toDoubleOrNull() ?: 0.0,
        fineAmount = state.fineAmount.toDoubleOrNull() ?: 0.0,
        maxMembers = state.maxMembers.toIntOrNull() ?: 0,
    )
    return CreateGroupRequest(
        name = state.groupName,
        officeId = requireNotNull(state.officeId) { "officeId validated before buildCreateGroupRequest" },
        userId = userId,
        currency = state.currency,
        meetingDay = state.meetingDay,
        meetingTime = state.meetingTime,
        typeConfig = typeConfigRequest,
    )
}

private fun ContributionMode.toContributionModel(): ContributionModel = when (this) {
    ContributionMode.SHARE_BASED_VARIABLE -> ContributionModel.SHARE_BASED_VARIABLE
    ContributionMode.FIXED -> ContributionModel.FIXED_AMOUNT
    ContributionMode.MINIMAL -> ContributionModel.FIXED_NEGOTIATED
    ContributionMode.UNKNOWN -> ContributionModel.UNKNOWN
}

private fun String.toPayoutOrderMethodOrDefault(): PayoutOrderMethod = when (this) {
    "FIXED_ORDER" -> PayoutOrderMethod.FIXED_ORDER
    "LOTTERY" -> PayoutOrderMethod.LOTTERY
    "AUCTION" -> PayoutOrderMethod.AUCTION
    "NEED_BASED" -> PayoutOrderMethod.NEED_BASED
    "NA" -> PayoutOrderMethod.NA
    else -> PayoutOrderMethod.NA
}

/**
 * Disambiguates the transport-level [NetworkError] onto [GroupCreateState]'s declared
 * [GroupCreateError] taxonomy. `REQUEST_TIMEOUT` is treated as the connectivity-loss bucket
 * (`NetworkError` has no dedicated "offline" value — same documented gap class as
 * `LoginSignupViewModel.toLoginSignupError`).
 */
private fun NetworkError.toGroupCreateError(): GroupCreateError = when (this) {
    NetworkError.REQUEST_TIMEOUT -> GroupCreateError.Network
    NetworkError.BAD_REQUEST -> GroupCreateError.Validation
    NetworkError.UNAUTHORIZED, NetworkError.TOO_MANY_REQUESTS -> GroupCreateError.Auth
    NetworkError.NOT_FOUND, NetworkError.SERIALIZATION, NetworkError.SERVER, NetworkError.UNKNOWN ->
        GroupCreateError.Server
}
