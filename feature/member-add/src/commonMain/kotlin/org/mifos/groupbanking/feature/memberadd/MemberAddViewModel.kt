/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.memberadd

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitor
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kpt.core.analytics.KptAnalyticsTracker
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.base.observability.CrashReporter
import kpt.core.base.observability.CrashSeverity
import kpt.core.base.ui.viewmodel.BaseViewModel
import org.mifos.groupbanking.core.data.repository.MemberAddRepository
import org.mifos.groupbanking.core.model.CreateMemberRequest
import org.mifos.groupbanking.core.model.MemberCreationResult
import org.mifos.groupbanking.core.model.MemberRole
import kotlin.time.Clock

// MVI stack (State/Event/Action/ViewModel/DI) for the `member-add` feature — see
// API.md#viewmodel / #state / #actions / #events / #di for the full generated-symbol contract.
private const val TAG = "MemberAddViewModel"

/**
 * A Fineract client is created with `officeId` unresolved (`0L`) — see [MemberAddState] class
 * KDoc "KNOWN GAP: officeId has no wire source" for the full explanation. Kept as a top-level
 * named constant (rather than a bare literal) so the gap is discoverable by grep and so a future
 * fix (idea-layer `nav_params.officeId` or a `groupId -> officeId` resolver call) has a single
 * call site to replace.
 */
private const val UNRESOLVED_OFFICE_ID = 0L

/**
 * Screen-level render state for `member-add-screen` — verbatim mirror of
 * `ui.yaml#state_model.screen_state.members`. Derived only (not stored) via
 * [MemberAddState.deriveScreenState] — same convention as `GroupCreateState`/`JoinWithCodeState`
 * (keeps `isSubmitting`/`isSubmitSuccess`/`error` the single source of truth instead of a fourth,
 * independently-mutable flag). See API.md#state.
 */
@Serializable
sealed interface MemberAddScreenState {
    @Serializable
    data object Content : MemberAddScreenState

    @Serializable
    data object Submitting : MemberAddScreenState

    @Serializable
    data object Success : MemberAddScreenState

    @Serializable
    data object Error : MemberAddScreenState
}

/**
 * Submission error taxonomy — verbatim mirror of `ui.yaml#state_model.errors.types`.
 * [messageKey] is a composeResources string-resource id (never a raw hardcoded English string,
 * per RULE-IMPL-NO-HARDCODED-STRING-001) resolved by the Screen layer.
 *
 * **[PhoneAlreadyExists] reachability gap (flagged, not fabricated around) — same documented
 * class as `MemberProfileError.NotFound`:** `api.yaml#api.create_client.errors.400` documents
 * "invalid fields OR duplicate phone" as a single HTTP 400, and `MemberAddApiImpl`'s status-code
 * mapper (`core/network`, upstream of this generation step) collapses every 400 into the ONE
 * [NetworkError.BAD_REQUEST] bucket — there is no parseable sub-code / response-body field
 * threaded through [NetworkError] to distinguish "duplicate phone" from any other validation
 * failure. [toMemberAddError] therefore maps [NetworkError.BAD_REQUEST] to [Validation]; the
 * `when` branch in [MemberAddViewModel.handleSubmitResult] that groups [PhoneAlreadyExists] with
 * [Validation]/[Server] is still real, exhaustively-covered logic (RULE-IMPL-DEAD-CLICKABLE-001
 * Rule 2) so the moment `NetworkError` (or `MemberAddApi`) is enriched with a duplicate-phone
 * signal, wiring [PhoneAlreadyExists] through is a one-line change. Flagged for the cross-feature
 * repair station (RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1).
 *
 * `Network`'s declared `fallback: queue_offline` and `Auth`'s declared `redirect: login` are
 * BEHAVIORAL contracts enacted directly by [MemberAddViewModel]: `Network` emits
 * [MemberAddEvent.ShowOfflineSyncDialog]; `Auth` emits [MemberAddEvent.ShowSnackbar] (KNOWN GAP,
 * same as `GroupCreateError.Auth`: `ui.yaml#events` declares no dedicated `NavigateToLogin` event
 * for the `redirect: login` contract).
 *
 * See API.md#state.
 */
@Serializable
sealed interface MemberAddError {
    val retry: Boolean
    val messageKey: String

    @Serializable
    data object Validation : MemberAddError {
        override val retry: Boolean = false
        override val messageKey: String = "error_validation"
    }

    @Serializable
    data object Network : MemberAddError {
        override val retry: Boolean = true
        override val messageKey: String = "error_network"
    }

    @Serializable
    data object Server : MemberAddError {
        override val retry: Boolean = true
        override val messageKey: String = "error_server"
    }

    @Serializable
    data object Auth : MemberAddError {
        override val retry: Boolean = false
        override val messageKey: String = "error_auth"
    }

    @Serializable
    data object PhoneAlreadyExists : MemberAddError {
        override val retry: Boolean = false
        override val messageKey: String = "error_phone_exists"
    }
}

/**
 * MVI state for `MemberAddViewModel`. Field set + defaults are a verbatim mirror of
 * `ui.yaml#state_model.MemberAddViewModel.state`. [error] is `@Transient` — a screen-level render
 * concern that should not survive process death, mirrors `GroupCreateState.error` /
 * `MemberProfileState.error`. [selectedRole] is a plain [MemberRole] enum value —
 * kotlinx.serialization serializes Kotlin enums natively, no `@Serializable` annotation needed on
 * the enum class itself (same convention as `MemberProfileState.role`).
 *
 * **KNOWN GAP — `officeId` has no wire source:** `api.yaml#dtos.CreateMemberRequest.officeId:
 * Long` (required) and `api.yaml#api.create_client.body.officeId: Long` are both declared, but
 * NEITHER `ui.yaml#nav_params` (only `groupId`) NOR `ui.yaml#state_model...state` (no `officeId`
 * field) name any source for it, and this generation step's dependency surface is limited to
 * [MemberAddRepository.createMember] (no group-details / office-resolution repository is
 * available to derive it from [groupId]). [buildCreateMemberRequest] sends the sentinel
 * [UNRESOLVED_OFFICE_ID] (`0L`) — a deliberately INVALID office id so a real Fineract backend
 * fails the create-client call loudly (surfaces as [MemberAddError.Validation]/[MemberAddError.Server])
 * rather than silently filing the new client under the wrong office. Flagged for idea-layer
 * correction (`nav_params.officeId: { type: Long, required: true }` sourced from `group-list`, or
 * a `groupId -> officeId` resolver dependency), RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1 — same
 * documented-gap class as `GroupCreateViewModel`'s `shareoutFormula` default.
 *
 * See API.md#state.
 */
@Serializable
@Immutable
data class MemberAddState(
    val firstName: String = "",
    val lastName: String = "",
    val phone: String = "",
    val photoUri: String? = null,
    val selectedRole: MemberRole = MemberRole.MEMBER,
    val validationErrors: Map<String, String> = emptyMap(),
    val isSubmitting: Boolean = false,
    val isSubmitSuccess: Boolean = false,
    val isOffline: Boolean = false,
    val isOfflineQueued: Boolean = false,
    @Transient
    val error: MemberAddError? = null,
    val showPhotoPicker: Boolean = false,
    val groupId: String = "",
)

/**
 * Derives [MemberAddScreenState] from [MemberAddState] — see the type's KDoc for why this is a
 * pure function rather than a stored field (mirrors `GroupCreateState.deriveScreenState()`).
 */
fun MemberAddState.deriveScreenState(): MemberAddScreenState = when {
    error != null -> MemberAddScreenState.Error
    isSubmitSuccess -> MemberAddScreenState.Success
    isSubmitting -> MemberAddScreenState.Submitting
    else -> MemberAddScreenState.Content
}

/**
 * One-shot side effects emitted by `MemberAddViewModel` — verbatim mirror of
 * `ui.yaml#state_model.MemberAddViewModel.events.members`. See API.md#events.
 */
sealed interface MemberAddEvent {
    data class NavigateToMemberProfile(val memberId: String, val groupId: String) : MemberAddEvent
    data object NavigateBack : MemberAddEvent
    data object ShowPhotoPicker : MemberAddEvent
    data object ShowOfflineSyncDialog : MemberAddEvent
    data class ShowSnackbar(val message: String) : MemberAddEvent
}

/**
 * User intents dispatched to `MemberAddViewModel`. The 10 top-level members are a verbatim
 * mirror of `ui.yaml#state_model.MemberAddViewModel.actions.members` — RULE-IMPL-DEAD-CLICKABLE-001
 * Rule 1. [Internal] is the sanctioned async-result-routing sub-interface (never a user intent)
 * per `training-layer/TRAINING_MASTER.yaml#patterns.actions`. See API.md#actions.
 */
sealed interface MemberAddAction {
    data class OnFirstNameChange(val value: String) : MemberAddAction
    data class OnLastNameChange(val value: String) : MemberAddAction
    data class OnPhoneChange(val value: String) : MemberAddAction
    data object OnPhotoPickerOpen : MemberAddAction
    data class OnPhotoCaptured(val uri: String) : MemberAddAction
    data class OnPhotoSelected(val uri: String) : MemberAddAction
    data object OnPhotoRemoved : MemberAddAction
    data class OnRoleSelected(val role: MemberRole) : MemberAddAction
    data object OnSubmit : MemberAddAction
    data object OnBack : MemberAddAction

    /** Async coroutine results — routed via `trySendAction`, never dispatched by the UI. */
    sealed interface Internal : MemberAddAction {
        data class ConnectivityChanged(val online: Boolean) : Internal
        data class SubmitResult(val result: NetworkResult<MemberCreationResult, NetworkError>) : Internal
    }
}

/**
 * MVI processor for the single-page member-add form (`business_logic.kind: composite` per
 * ui.yaml — a client-validated form whose submit chains three Fineract REST calls via
 * [MemberAddRepository.createMember]; SP-04 AC-7 analytics/crashReporter injection pair applies
 * since `composite` is not `crud`/`nav_only`, per RULE-IDEA-IMPL-INTELLIGENCE-001 AC-03i).
 * [MemberAddRepository] is consumed directly via [NetworkResult] rather than a Store5
 * `.asScreenStream()`/`.write()` — every `api.yaml#api[]` endpoint is a write with an
 * `offline_queue` block and there is no read-stream to back with a Store5 cache, same branch as
 * [MemberAddRepository] KDoc's own "Store5 branch" note / `GroupCreateRepository`.
 *
 * `FieldEncryptor` (`core-base/security`) is intentionally NOT injected — `data-flow.yaml`
 * declares no `pii_columns` entry for this screen (mirrors `GroupDashboardViewModel`'s /
 * `MemberProfileViewModel`'s identical omission note); `phone` is collected but not flagged PII
 * by the SP-02 idea-layer schema.
 *
 * **Offline submission (KNOWN, DOCUMENTED SEAM — not a half-built outbox):** `data-flow.yaml`
 * declares an `offline_behavior` (`sync_queue` table, operations `CREATE_MEMBER` /
 * `ASSIGN_MEMBER_ROLE` / `UPLOAD_MEMBER_PHOTO`) and `ui.yaml#state_model.di` names a
 * `SyncQueueRepository` — neither exists anywhere in this codebase yet (same documented gap as
 * `GroupCreateViewModel`'s offline-queue seam). [handleSubmit] proactively checks
 * [networkMonitor] before attempting the network call: offline -> `isOffline = true` +
 * [MemberAddError.Network] + [MemberAddEvent.ShowOfflineSyncDialog], with the fully-filled form
 * retained in [MemberAddState] so `OnSubmit` can simply be re-dispatched once connectivity
 * returns (a manual retry, not a persisted background sync). [isOffline] is ALSO driven
 * reactively by [networkMonitor]'s continuous `isOnline` stream (`Internal.ConnectivityChanged`)
 * so `ui.yaml#components.offline_banner` (`visible: "{{isOffline}}"`) reflects live connectivity
 * even before the user taps Save — same dual (reactive + proactive) pattern as
 * `GroupCreateViewModel`.
 *
 * **Photo-bytes deferral (KNOWN, DOCUMENTED SEAM — flagged for the caller, not silently
 * dropped):** `ui.yaml#state_model.di` names an `ImagePickerHelper` service responsible for
 * reading the platform-picked/captured [MemberAddState.photoUri] into raw bytes
 * (`MemberAddRepository.createMember`'s `photoBytes: ByteArray?` parameter — commonMain-safe, no
 * `java.io.File`/`NSData` crosses this boundary per [MemberAddRepository] KDoc). Reading those
 * bytes is a PLATFORM (androidMain/iosMain expect-actual) concern outside this ViewModel-only
 * generation step's scope — no `ImagePickerHelper` implementation/interface exists anywhere in
 * this codebase yet. [handleSubmit] therefore calls [MemberAddRepository.createMember] with
 * `photoBytes = null`; per [MemberAddRepository] KDoc this makes the create-chain's optional
 * photo-upload step a no-op (`photoUploaded = false` on the result) while steps 1-2 (create
 * client + assign role) still succeed — [photoUri] itself is retained in state for display, so no
 * user-visible data is lost, only the upload is deferred. Flagged for a follow-up generation step
 * once `ImagePickerHelper` lands (RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1).
 *
 * See API.md#viewmodel.
 */
internal class MemberAddViewModel(
    private val repository: MemberAddRepository,
    private val networkMonitor: NetworkMonitor,
    private val analytics: KptAnalyticsTracker,
    private val crashReporter: CrashReporter,
    private val groupId: String,
) : BaseViewModel<MemberAddState, MemberAddEvent, MemberAddAction>(
    initialState = MemberAddState(groupId = groupId),
) {

    private var submitJob: Job? = null

    init {
        crashReporter.recordMessage(
            message = "feature=member-add screen=member-add-screen groupId=$groupId",
            level = CrashSeverity.Debug,
        )
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                trySendAction(MemberAddAction.Internal.ConnectivityChanged(online))
            }
        }
    }

    override fun handleAction(action: MemberAddAction) {
        when (action) {
            is MemberAddAction.OnFirstNameChange -> handleFirstNameChange(action.value)
            is MemberAddAction.OnLastNameChange -> handleLastNameChange(action.value)
            is MemberAddAction.OnPhoneChange -> handlePhoneChange(action.value)
            MemberAddAction.OnPhotoPickerOpen -> handlePhotoPickerOpen()
            is MemberAddAction.OnPhotoCaptured -> handlePhotoChosen(action.uri)
            is MemberAddAction.OnPhotoSelected -> handlePhotoChosen(action.uri)
            MemberAddAction.OnPhotoRemoved -> handlePhotoRemoved()
            is MemberAddAction.OnRoleSelected -> handleRoleSelected(action.role)
            MemberAddAction.OnSubmit -> handleSubmit()
            MemberAddAction.OnBack -> sendEvent(MemberAddEvent.NavigateBack)
            is MemberAddAction.Internal.ConnectivityChanged -> handleConnectivityChanged(action.online)
            is MemberAddAction.Internal.SubmitResult -> handleSubmitResult(action.result)
        }
    }

    // -- Field transforms (ui.yaml effect: transform_state) --------------------------------------

    private fun handleFirstNameChange(value: String) {
        updateState { copy(firstName = value, validationErrors = validationErrors - "firstName") }
    }

    private fun handleLastNameChange(value: String) {
        updateState { copy(lastName = value, validationErrors = validationErrors - "lastName") }
    }

    private fun handlePhoneChange(value: String) {
        updateState { copy(phone = value, validationErrors = validationErrors - "phone") }
    }

    // -- Photo picker (ui.yaml effect: transform_state / record_media / persist_file) -------------

    private fun handlePhotoPickerOpen() {
        updateState { copy(showPhotoPicker = true) }
        sendEvent(MemberAddEvent.ShowPhotoPicker)
    }

    private fun handlePhotoChosen(uri: String) {
        Logger.i(TAG) { "member photo chosen uri=$uri" }
        updateState { copy(photoUri = uri, showPhotoPicker = false) }
    }

    private fun handlePhotoRemoved() {
        updateState { copy(photoUri = null) }
    }

    // -- Role dropdown (ui.yaml effect: transform_state) -------------------------------------------

    private fun handleRoleSelected(role: MemberRole) {
        updateState { copy(selectedRole = role) }
    }

    // -- Submit (ui.yaml effect: call_api) -----------------------------------------------------------

    private fun handleSubmit() {
        val errors = validateMemberAddForm(state)
        if (errors.isNotEmpty()) {
            Logger.w(TAG) { "OnSubmit validation failed fields=${errors.keys} groupId=$groupId" }
            updateState { copy(validationErrors = errors) }
            return
        }

        submitJob?.cancel()
        submitJob = viewModelScope.launch {
            updateState { copy(isSubmitting = true, error = null, validationErrors = emptyMap()) }
            analytics.trackClientOperation(operation = "create")

            val request = buildCreateMemberRequest(state)

            if (!networkMonitor.isOnline.value) {
                // Offline pre-flight: durably queue the whole add-member chain so the drain replays
                // it via the companion /batches self-dispatch to POST /companion/members
                // (targetTable = "/companion/members"), which does create-client + assign-role
                // server-side — never drop the write. Queued, not errored: no `error = Network`.
                val queueId = repository.enqueueOffline(request)
                Logger.i(TAG) { "createMember offline — enqueued queueId=$queueId groupId=$groupId" }
                crashReporter.recordMessage(
                    message = "member-add: submit queued offline (queueId=$queueId) groupId=$groupId",
                    level = CrashSeverity.Info,
                )
                updateState { copy(isSubmitting = false, isOffline = true, isOfflineQueued = true) }
                sendEvent(MemberAddEvent.ShowOfflineSyncDialog)
                return@launch
            }

            // Photo-bytes deferral — see class KDoc "Photo-bytes deferral" for the full rationale.
            val result = repository.createMember(request = request, photoBytes = null)
            trySendAction(MemberAddAction.Internal.SubmitResult(result))
        }
    }

    // -- Async result routing ----------------------------------------------------------------------

    private fun handleConnectivityChanged(online: Boolean) {
        updateState { copy(isOffline = !online) }
    }

    private fun handleSubmitResult(result: NetworkResult<MemberCreationResult, NetworkError>) {
        when (result) {
            is NetworkResult.Success -> {
                val creation = result.data
                analytics.trackClientOperation(operation = "create", clientId = creation.memberId, success = true)
                Logger.i(TAG) { "createMember succeeded memberId=${creation.memberId} groupId=$groupId" }
                updateState { copy(isSubmitting = false, isSubmitSuccess = true, error = null) }
                sendEvent(MemberAddEvent.NavigateToMemberProfile(memberId = creation.memberId, groupId = groupId))
            }
            is NetworkResult.Error -> {
                analytics.trackClientOperation(operation = "create", success = false)
                crashReporter.recordMessage(
                    message = "member-add: createMember failed groupId=$groupId networkError=${result.error}",
                    level = CrashSeverity.Warning,
                )
                // `isOffline` is deliberately NOT touched here — it stays solely driven by the
                // reactive `Internal.ConnectivityChanged` observer (device connectivity), never
                // inferred from a single request outcome (mirrors `GroupCreateViewModel`'s
                // identical precedent).
                val mapped = result.error.toMemberAddError()
                updateState { copy(isSubmitting = false, error = mapped) }
                when (mapped) {
                    MemberAddError.Network -> sendEvent(MemberAddEvent.ShowOfflineSyncDialog)
                    MemberAddError.Auth -> sendEvent(MemberAddEvent.ShowSnackbar(message = mapped.messageKey))
                    MemberAddError.Validation, MemberAddError.Server, MemberAddError.PhoneAlreadyExists ->
                        Unit // inline error banner suffices — see MemberAddError KDoc
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Top-level helpers (pure — independently unit-testable)
// ---------------------------------------------------------------------------------------------

private val KENYAN_PHONE_E164_REGEX = Regex("^\\+254[17]\\d{8}$")
private val KENYAN_PHONE_LOCAL_REGEX = Regex("^0[17]\\d{8}$")

/** Accepts both the full E.164 form (`+254712345678`) and the local form (`0712345678`). */
private fun String.isValidKenyanPhone(): Boolean =
    KENYAN_PHONE_E164_REGEX.matches(this) || KENYAN_PHONE_LOCAL_REGEX.matches(this)

/** Normalizes a validated local-form Kenyan number to E.164; already-E.164 numbers pass through. */
private fun String.toE164KenyanPhone(): String = if (KENYAN_PHONE_LOCAL_REGEX.matches(this)) {
    "+254${substring(1)}"
} else {
    this
}

/**
 * Client-side validation — `ui.yaml#components.save_button.action_contract` ("submits the
 * validated form"). Only `firstName`/`lastName`/`phone` are validated client-side; `selectedRole`
 * always carries a valid default ([MemberRole.MEMBER]) so it never needs a required-field check.
 * Message keys are declared `i18n.en` entries only — `error_validation` (generic required-field)
 * and `error_phone_format` (Kenyan E.164/local format check).
 */
private fun validateMemberAddForm(state: MemberAddState): Map<String, String> = buildMap {
    if (state.firstName.isBlank()) put("firstName", "error_validation")
    if (state.lastName.isBlank()) put("lastName", "error_validation")
    if (state.phone.isBlank() || !state.phone.isValidKenyanPhone()) put("phone", "error_phone_format")
}

/**
 * Assembles [CreateMemberRequest] from the validated [state] + the [MemberAddViewModel.groupId]
 * nav-arg. See [MemberAddState] class KDoc "KNOWN GAP" for the [UNRESOLVED_OFFICE_ID] sentinel.
 * [activationDate] is today's wall date formatted `dd MMMM yyyy` — matching
 * `MemberAddMappers.kt#toCreateMemberRequestDto`'s `dateFormat` default sent alongside it to
 * Fineract. `ui.yaml` collects no explicit activation-date input (only identity/phone/role/photo),
 * so "member is activated the day the form is submitted" is the real, intentional business rule —
 * not a placeholder value (same convention as `MemberProfileViewModel.todayIsoDate`'s identical
 * "confirm-time is the real business rule" note).
 */
private fun buildCreateMemberRequest(state: MemberAddState): CreateMemberRequest = CreateMemberRequest(
    firstName = state.firstName,
    lastName = state.lastName,
    phone = state.phone.toE164KenyanPhone(),
    photoUri = state.photoUri,
    role = state.selectedRole,
    groupId = state.groupId,
    officeId = UNRESOLVED_OFFICE_ID,
    activationDate = todayFormattedDate(),
)

private val MONTH_NAMES = mapOf(
    Month.JANUARY to "January",
    Month.FEBRUARY to "February",
    Month.MARCH to "March",
    Month.APRIL to "April",
    Month.MAY to "May",
    Month.JUNE to "June",
    Month.JULY to "July",
    Month.AUGUST to "August",
    Month.SEPTEMBER to "September",
    Month.OCTOBER to "October",
    Month.NOVEMBER to "November",
    Month.DECEMBER to "December",
)

/**
 * Today's date formatted `dd MMMM yyyy` (e.g. `"21 July 2026"`) — the Fineract `dateFormat`
 * `MemberAddMappers.kt#toCreateMemberRequestDto` defaults its wire calls to. Uses
 * `kotlin.time.Clock` (not the deprecated `kotlinx.datetime.Clock`) + `kotlinx.datetime.toLocalDateTime`
 * for the current-time-zone wall date, mirroring `MemberProfileViewModel.todayIsoDate`'s identical
 * precedent.
 */
private fun todayFormattedDate(): String {
    val date = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val day = date.day.toString().padStart(2, '0')
    val monthName = MONTH_NAMES.getValue(date.month)
    return "$day $monthName ${date.year}"
}

/**
 * Disambiguates the transport-level [NetworkError] onto [MemberAddState]'s declared
 * [MemberAddError] taxonomy — `data-flow.yaml#error_paths`: `400 -> error_validation`,
 * `401 -> navigate login` (folded onto [MemberAddError.Auth], see [MemberAddError] class KDoc),
 * `404`/`500 -> error_server`, `network.offline -> error_network`. `REQUEST_TIMEOUT` is treated as
 * the connectivity-loss bucket (`NetworkError` has no dedicated "offline" value — same documented
 * gap class as `GroupCreateViewModel.toGroupCreateError`). See [MemberAddError.PhoneAlreadyExists]
 * KDoc for why `400.phone_exists` cannot be distinguished from a generic `400` here.
 */
private fun NetworkError.toMemberAddError(): MemberAddError = when (this) {
    NetworkError.REQUEST_TIMEOUT -> MemberAddError.Network
    NetworkError.BAD_REQUEST -> MemberAddError.Validation
    NetworkError.UNAUTHORIZED, NetworkError.TOO_MANY_REQUESTS -> MemberAddError.Auth
    NetworkError.NOT_FOUND, NetworkError.SERIALIZATION, NetworkError.SERVER, NetworkError.UNKNOWN ->
        MemberAddError.Server
}
