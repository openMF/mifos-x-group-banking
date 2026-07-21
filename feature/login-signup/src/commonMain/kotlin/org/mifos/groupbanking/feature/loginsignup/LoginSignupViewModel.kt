/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.loginsignup

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
import org.mifos.groupbanking.core.model.AuthSession
import org.mifos.groupbanking.core.model.GroupMembership
import org.mifos.groupbanking.core.model.GroupRole
import org.mifos.groupbanking.core.model.LoginCredentials
import org.mifos.groupbanking.core.model.SelfRegistration
import org.mifos.groupbanking.core.model.UserProfile

private const val TAG = "LoginSignupViewModel"

private const val MIN_NAME_LENGTH = 2
private const val MIN_PASSWORD_LENGTH_SIGNUP = 8
private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
private val E164_PHONE_REGEX = Regex("^\\+[1-9]\\d{6,14}$")

/**
 * LOGIN or SIGNUP mode toggle — drives which of [LoginSignupState]'s field set is visible.
 * See ui.yaml#components.mode_toggle_tabs. See API.md#state.
 */
enum class AuthMode { Login, Signup }

/**
 * Screen-level render state for `login-signup-screen`. See ui.yaml#state_model.screen_state.
 * See API.md#state.
 */
@Serializable
sealed interface LoginSignupScreenState {
    @Serializable
    data object Content : LoginSignupScreenState

    @Serializable
    data object Loading : LoginSignupScreenState

    @Serializable
    data object Error : LoginSignupScreenState

    @Serializable
    data object ZeroGroups : LoginSignupScreenState
}

/**
 * Top-level auth error taxonomy — verbatim mirror of ui.yaml#state_model.errors.types.
 * [messageKey] is a composeResources string-resource id (never a raw hardcoded English
 * string, per RULE-IMPL-NO-HARDCODED-STRING-001) resolved by the Screen layer.
 * See API.md#state.
 */
@Serializable
sealed interface LoginSignupError {
    val retry: Boolean
    val messageKey: String

    @Serializable
    data object InvalidCredentials : LoginSignupError {
        override val retry: Boolean = true
        override val messageKey: String = "error_invalid_credentials"
    }

    @Serializable
    data object AccountExists : LoginSignupError {
        override val retry: Boolean = false
        override val messageKey: String = "error_account_exists"
    }

    @Serializable
    data object WeakPassword : LoginSignupError {
        override val retry: Boolean = false
        override val messageKey: String = "error_weak_password"
    }

    @Serializable
    data object Network : LoginSignupError {
        override val retry: Boolean = true
        override val messageKey: String = "error_network"
    }

    @Serializable
    data object Server : LoginSignupError {
        override val retry: Boolean = true
        override val messageKey: String = "error_server"
    }

    @Serializable
    data object BiometricFailed : LoginSignupError {
        override val retry: Boolean = true
        override val messageKey: String = "error_biometric_failed"
    }
}

/**
 * MVI state for `LoginSignupViewModel`. Field set + defaults are a verbatim mirror of
 * ui.yaml#state_model.LoginSignupViewModel.state. [groupMemberships] and [screenState]/[error]
 * are `@Transient` — [GroupMembership] is an external core/model type that carries a
 * non-`@Serializable` `kotlinx.datetime.Instant`, and screen-level render state is a UI-only
 * concern that should not survive process death (mirrors the framework's
 * `dialogState`/`uiState` convention — see `training-layer/TRAINING_MASTER.yaml#patterns.state_models`).
 * See API.md#state.
 */
@Serializable
@Immutable
data class LoginSignupState(
    val mode: AuthMode = AuthMode.Login,
    val name: String = "",
    val emailPhone: String = "",
    val password: String = "",
    val pin: String = "",
    val isSubmitting: Boolean = false,
    val validationErrors: Map<String, String> = emptyMap(),
    @Transient
    val error: LoginSignupError? = null,
    val isBiometricAvailable: Boolean = false,
    val sessionToken: String? = null,
    @Transient
    val groupMemberships: List<GroupMembership> = emptyList(),
    @Transient
    val screenState: LoginSignupScreenState = LoginSignupScreenState.Content,
)

/**
 * One-shot side effects emitted by `LoginSignupViewModel` — verbatim mirror of
 * ui.yaml#state_model.LoginSignupViewModel.events. See API.md#events.
 */
sealed interface LoginSignupEvent {
    data object NavigateToPersonalDashboard : LoginSignupEvent
    data object NavigateToGroupList : LoginSignupEvent
    data object NavigateToGroupTypePicker : LoginSignupEvent
    data object NavigateToJoinWithCode : LoginSignupEvent
    data class ShowSnackbar(val message: String) : LoginSignupEvent
    data object PromptBiometric : LoginSignupEvent
}

/**
 * User intents dispatched to `LoginSignupViewModel`. The 11 top-level members are a verbatim
 * mirror of ui.yaml#state_model.LoginSignupViewModel.actions — RULE-IMPL-DEAD-CLICKABLE-001
 * Rule 1. [Internal] is the sanctioned async-result-routing sub-interface (never a user
 * intent) per `training-layer/TRAINING_MASTER.yaml#patterns.actions`. See API.md#actions.
 */
sealed interface LoginSignupAction {
    data class OnModeToggle(val mode: AuthMode) : LoginSignupAction
    data class OnNameChange(val value: String) : LoginSignupAction
    data class OnEmailPhoneChange(val value: String) : LoginSignupAction
    data class OnPasswordChange(val value: String) : LoginSignupAction
    data class OnPinChange(val value: String) : LoginSignupAction
    data object OnLoginTap : LoginSignupAction
    data object OnSignupTap : LoginSignupAction
    data object OnBiometricUnlock : LoginSignupAction
    data object OnCreateGroupTap : LoginSignupAction
    data object OnJoinWithCodeTap : LoginSignupAction
    data object OnForgotPassword : LoginSignupAction

    /** Async coroutine results — routed via `trySendAction`, never dispatched by the UI. */
    sealed interface Internal : LoginSignupAction {
        data class SessionChecked(val session: AuthSession?) : Internal
        data class LoginResult(val result: NetworkResult<AuthSession, NetworkError>) : Internal
        data class SignupResult(val result: NetworkResult<AuthSession, NetworkError>) : Internal
        data class BiometricResult(val result: NetworkResult<UserProfile, NetworkError>) : Internal
    }
}

/**
 * MVI processor for the unified login/signup entry screen (`business_logic.kind: processor`
 * per ui.yaml — a write+session-read mutation flow, so [AuthRepository] is consumed directly
 * via [NetworkResult] rather than a Store5 `.asScreenStream()`, per RULE-IMPLEMENT-STORE5-001 /
 * RULE-IDEA-IMPL-INTELLIGENCE-001).
 *
 * **SP-04 hooks (AC-7):** [analytics] (`core/analytics`) records every auth attempt via
 * `trackLogin(method, success, errorCode)` — the shipped `KptAnalyticsTracker` domain-specific
 * auth tracker (the generic `trackOperation()`/`trackError()` surface documented in
 * `templates/instructions/stream-first/latest/modules/CORE_ANALYTICS.md` does not exist on the
 * shipped class — see the generation report for this drift). [crashReporter]
 * (`core-base/observability`) receives a `setUser(session?.userId)` breadcrumb on every
 * [AuthRepository.currentSession] emission plus a `recordMessage` breadcrumb on every auth
 * failure. `FieldEncryptor` (`core-base/security`) is intentionally NOT injected — this
 * ViewModel never persists a raw PII field itself (no `pii_columns` in data-flow.yaml; the only
 * client-side persistence, `sessionToken`/`userId`/`tokenExpiresAt`, already happens inside the
 * pre-built [AuthRepository]/`CompanionSessionStore`).
 *
 * See API.md#viewmodel.
 */
internal class LoginSignupViewModel(
    private val authRepository: AuthRepository,
    private val analytics: KptAnalyticsTracker,
    private val crashReporter: CrashReporter,
) : BaseViewModel<LoginSignupState, LoginSignupEvent, LoginSignupAction>(
    initialState = LoginSignupState(),
) {

    private var loginJob: Job? = null
    private var signupJob: Job? = null
    private var biometricJob: Job? = null
    private var hasAutoPromptedBiometric = false

    init {
        crashReporter.recordMessage(
            message = "feature=login-signup screen=login-signup-screen",
            level = CrashSeverity.Debug,
        )
        viewModelScope.launch {
            authRepository.currentSession.collect { session ->
                trySendAction(LoginSignupAction.Internal.SessionChecked(session))
            }
        }
    }

    @Suppress("CyclomaticComplexMethod")
    override fun handleAction(action: LoginSignupAction) {
        when (action) {
            is LoginSignupAction.OnModeToggle -> handleModeToggle(action.mode)
            is LoginSignupAction.OnNameChange -> handleNameChange(action.value)
            is LoginSignupAction.OnEmailPhoneChange -> handleEmailPhoneChange(action.value)
            is LoginSignupAction.OnPasswordChange -> handlePasswordChange(action.value)
            is LoginSignupAction.OnPinChange -> handlePinChange(action.value)
            LoginSignupAction.OnLoginTap -> handleLoginTap()
            LoginSignupAction.OnSignupTap -> handleSignupTap()
            LoginSignupAction.OnBiometricUnlock -> handleBiometricUnlock()
            LoginSignupAction.OnCreateGroupTap -> handleCreateGroupTap()
            LoginSignupAction.OnJoinWithCodeTap -> handleJoinWithCodeTap()
            LoginSignupAction.OnForgotPassword -> handleForgotPassword()
            is LoginSignupAction.Internal.SessionChecked -> handleSessionChecked(action.session)
            is LoginSignupAction.Internal.LoginResult -> handleLoginResult(action.result)
            is LoginSignupAction.Internal.SignupResult -> handleSignupResult(action.result)
            is LoginSignupAction.Internal.BiometricResult -> handleBiometricResult(action.result)
        }
    }

    // -- Field / mode transforms (ui.yaml effect: transform_state) -----------------------------

    private fun handleModeToggle(mode: AuthMode) {
        // legacy_metadata.analytics declares an `auth_mode_toggled` event, but KptAnalyticsTracker
        // exposes no generic operation tracker this call could route through (see class KDoc) —
        // recorded as a structured, PII-safe Kermit breadcrumb instead.
        Logger.i(TAG) { "auth_mode_toggled mode=${mode.name}" }
        updateState {
            copy(
                mode = mode,
                name = "",
                emailPhone = "",
                password = "",
                validationErrors = emptyMap(),
                error = null,
            )
        }
    }

    private fun handleNameChange(value: String) {
        updateState {
            copy(name = value, validationErrors = validationErrors - "name")
        }
    }

    private fun handleEmailPhoneChange(value: String) {
        updateState {
            copy(emailPhone = value, validationErrors = validationErrors - "emailPhone")
        }
    }

    private fun handlePasswordChange(value: String) {
        updateState {
            copy(password = value, validationErrors = validationErrors - "password")
        }
    }

    private fun handlePinChange(value: String) {
        val sanitized = value.filter { it.isDigit() }.take(PIN_MAX_LENGTH)
        updateState {
            copy(pin = sanitized, validationErrors = validationErrors - "pin")
        }
    }

    // -- Submit actions (ui.yaml effect: call_api) ----------------------------------------------

    private fun handleLoginTap() {
        val errors = buildMap {
            validateEmailPhone(state.emailPhone)?.let { put("emailPhone", it) }
            validatePassword(state.password, requireStrong = false)?.let { put("password", it) }
        }
        if (errors.isNotEmpty()) {
            Logger.w(TAG) { "login blocked by client-side validation: fields=${errors.keys}" }
            updateState { copy(validationErrors = errors) }
            return
        }

        loginJob?.cancel()
        loginJob = viewModelScope.launch {
            updateState {
                copy(
                    isSubmitting = true,
                    screenState = LoginSignupScreenState.Loading,
                    error = null,
                    validationErrors = emptyMap(),
                )
            }
            val credentials = LoginCredentials(emailPhone = state.emailPhone, password = state.password)
            val result = authRepository.login(credentials)
            trySendAction(LoginSignupAction.Internal.LoginResult(result))
        }
    }

    private fun handleSignupTap() {
        val errors = buildMap {
            validateName(state.name)?.let { put("name", it) }
            validateEmailPhone(state.emailPhone)?.let { put("emailPhone", it) }
            validatePassword(state.password, requireStrong = true)?.let { put("password", it) }
        }
        if (errors.isNotEmpty()) {
            Logger.w(TAG) { "signup blocked by client-side validation: fields=${errors.keys}" }
            updateState { copy(validationErrors = errors) }
            return
        }

        signupJob?.cancel()
        signupJob = viewModelScope.launch {
            updateState {
                copy(
                    isSubmitting = true,
                    screenState = LoginSignupScreenState.Loading,
                    error = null,
                    validationErrors = emptyMap(),
                )
            }
            val registration = SelfRegistration(
                name = state.name,
                emailPhone = state.emailPhone,
                password = state.password,
            )
            val result = authRepository.selfRegister(registration)
            trySendAction(LoginSignupAction.Internal.SignupResult(result))
        }
    }

    private fun handleBiometricUnlock() {
        val token = state.sessionToken
        if (token == null) {
            Logger.w(TAG) { "biometric unlock tapped without a stored session token" }
            analytics.trackLogin(method = "biometric", success = false, errorCode = "no_session_token")
            updateState {
                copy(error = LoginSignupError.BiometricFailed, screenState = LoginSignupScreenState.Error)
            }
            return
        }

        biometricJob?.cancel()
        biometricJob = viewModelScope.launch {
            updateState {
                copy(isSubmitting = true, screenState = LoginSignupScreenState.Loading, error = null)
            }
            val result = authRepository.refreshSession(token)
            trySendAction(LoginSignupAction.Internal.BiometricResult(result))
        }
    }

    // -- Zero-groups navigation (ui.yaml effect: navigate) --------------------------------------

    private fun handleCreateGroupTap() {
        analytics.trackGroupOperation(operation = "create")
        Logger.i(TAG) { "zero_groups: create-group tapped" }
        sendEvent(LoginSignupEvent.NavigateToGroupTypePicker)
    }

    private fun handleJoinWithCodeTap() {
        analytics.trackGroupOperation(operation = "join")
        Logger.i(TAG) { "zero_groups: join-with-code tapped" }
        sendEvent(LoginSignupEvent.NavigateToJoinWithCode)
    }

    // -- Forgot password (ui.yaml effect: emit_event) -------------------------------------------

    private fun handleForgotPassword() {
        Logger.i(TAG) { "forgot-password link tapped" }
        sendEvent(LoginSignupEvent.ShowSnackbar(message = "login_signup_forgot_password_snackbar"))
    }

    // -- Async result routing --------------------------------------------------------------------

    private fun handleSessionChecked(session: AuthSession?) {
        crashReporter.setUser(session?.userId)
        if (session == null) {
            updateState { copy(isBiometricAvailable = false, sessionToken = null) }
            return
        }

        updateState {
            copy(
                sessionToken = session.sessionToken,
                groupMemberships = session.groupMemberships,
                isBiometricAvailable = true,
            )
        }
        if (!hasAutoPromptedBiometric) {
            hasAutoPromptedBiometric = true
            Logger.i(TAG) { "valid stored session found on mount — prompting biometric unlock" }
            sendEvent(LoginSignupEvent.PromptBiometric)
        }
    }

    private fun handleLoginResult(result: NetworkResult<AuthSession, NetworkError>) {
        when (result) {
            is NetworkResult.Success -> {
                val session = result.data
                analytics.trackLogin(method = "password", success = true)
                Logger.i(TAG) { "login success groupCount=${session.groupMemberships.size}" }
                updateState {
                    copy(
                        isSubmitting = false,
                        sessionToken = session.sessionToken,
                        groupMemberships = session.groupMemberships,
                        password = "",
                        screenState = screenStateFor(session.groupMemberships),
                        error = null,
                    )
                }
                routeEvent(session.groupMemberships)?.let(::sendEvent)
            }
            is NetworkResult.Error -> {
                val mapped = result.error.toLoginSignupError(AuthErrorContext.Login)
                analytics.trackLogin(method = "password", success = false, errorCode = result.error.name)
                crashReporter.recordMessage(
                    message = "login-signup: login failed networkError=${result.error}",
                    level = CrashSeverity.Warning,
                )
                updateState {
                    copy(isSubmitting = false, screenState = LoginSignupScreenState.Error, error = mapped)
                }
            }
        }
    }

    private fun handleSignupResult(result: NetworkResult<AuthSession, NetworkError>) {
        when (result) {
            is NetworkResult.Success -> {
                val session = result.data
                analytics.trackLogin(method = "signup", success = true)
                Logger.i(TAG) { "signup success userId=${session.userId}" }
                updateState {
                    copy(
                        isSubmitting = false,
                        sessionToken = session.sessionToken,
                        groupMemberships = session.groupMemberships,
                        name = "",
                        password = "",
                        screenState = screenStateFor(session.groupMemberships),
                        error = null,
                    )
                }
                routeEvent(session.groupMemberships)?.let(::sendEvent)
            }
            is NetworkResult.Error -> {
                val mapped = result.error.toLoginSignupError(AuthErrorContext.Signup)
                analytics.trackLogin(method = "signup", success = false, errorCode = result.error.name)
                crashReporter.recordMessage(
                    message = "login-signup: signup failed networkError=${result.error}",
                    level = CrashSeverity.Warning,
                )
                updateState {
                    copy(isSubmitting = false, screenState = LoginSignupScreenState.Error, error = mapped)
                }
            }
        }
    }

    private fun handleBiometricResult(result: NetworkResult<UserProfile, NetworkError>) {
        when (result) {
            is NetworkResult.Success -> {
                val profile = result.data
                analytics.trackLogin(method = "biometric", success = true)
                Logger.i(TAG) { "biometric refresh success groupCount=${profile.groupMemberships.size}" }
                updateState {
                    copy(
                        isSubmitting = false,
                        groupMemberships = profile.groupMemberships,
                        screenState = screenStateFor(profile.groupMemberships),
                        error = null,
                    )
                }
                routeEvent(profile.groupMemberships)?.let(::sendEvent)
            }
            is NetworkResult.Error -> {
                analytics.trackLogin(method = "biometric", success = false, errorCode = result.error.name)
                crashReporter.recordMessage(
                    message = "login-signup: biometric refresh failed networkError=${result.error}",
                    level = CrashSeverity.Warning,
                )
                updateState {
                    copy(
                        isSubmitting = false,
                        screenState = LoginSignupScreenState.Error,
                        error = LoginSignupError.BiometricFailed,
                    )
                }
            }
        }
    }

    // -- Helpers ----------------------------------------------------------------------------------

    private fun screenStateFor(memberships: List<GroupMembership>): LoginSignupScreenState =
        if (memberships.isEmpty()) LoginSignupScreenState.ZeroGroups else LoginSignupScreenState.Content

    /** Routes on `groupMemberships` per SPEC.md — organizer role → group-list, else personal-dashboard. */
    private fun routeEvent(memberships: List<GroupMembership>): LoginSignupEvent? = when {
        memberships.isEmpty() -> null
        memberships.any { it.role == GroupRole.ORGANIZER } -> LoginSignupEvent.NavigateToGroupList
        else -> LoginSignupEvent.NavigateToPersonalDashboard
    }

    private companion object {
        const val PIN_MAX_LENGTH = 4
    }
}

private enum class AuthErrorContext { Login, Signup, Biometric }

/**
 * Maps the transport-level [NetworkError] onto the screen's declared [LoginSignupError]
 * taxonomy. Two documented idea-layer gaps (flagged in the generation report, not invented
 * here): (1) `NetworkError` has no dedicated 409 bucket — `CompanionAuthApiImpl` intentionally
 * folds 409 into [NetworkError.UNKNOWN], so a signup-context [NetworkError.UNKNOWN] is treated
 * as [LoginSignupError.AccountExists]; (2) `data-flow.yaml` references `error_rate_limited` /
 * `error_offline_biometric` / `error_session_expired` message keys that ui.yaml never declares
 * on [LoginSignupError] — those are folded onto the closest declared type below.
 */
private fun NetworkError.toLoginSignupError(context: AuthErrorContext): LoginSignupError {
    if (context == AuthErrorContext.Biometric) return LoginSignupError.BiometricFailed
    return when (this) {
        NetworkError.UNAUTHORIZED -> LoginSignupError.InvalidCredentials
        NetworkError.TOO_MANY_REQUESTS -> LoginSignupError.InvalidCredentials
        NetworkError.UNKNOWN -> if (context == AuthErrorContext.Signup) {
            LoginSignupError.AccountExists
        } else {
            LoginSignupError.Server
        }
        NetworkError.BAD_REQUEST -> if (context == AuthErrorContext.Signup) {
            LoginSignupError.WeakPassword
        } else {
            LoginSignupError.InvalidCredentials
        }
        NetworkError.REQUEST_TIMEOUT -> LoginSignupError.Network
        NetworkError.NOT_FOUND, NetworkError.SERIALIZATION, NetworkError.SERVER -> LoginSignupError.Server
    }
}

private fun isValidEmailOrPhone(value: String): Boolean =
    EMAIL_REGEX.matches(value) || E164_PHONE_REGEX.matches(value)

private fun validateName(name: String): String? =
    if (name.trim().length < MIN_NAME_LENGTH) "error_name_required" else null

private fun validateEmailPhone(value: String): String? =
    if (value.isBlank() || !isValidEmailOrPhone(value)) "error_email_phone_invalid" else null

/** `requireStrong` gates the signup-only min-8-length strength check per ui.yaml#password_field. */
private fun validatePassword(password: String, requireStrong: Boolean): String? = when {
    password.isBlank() -> "error_password_weak"
    requireStrong && password.length < MIN_PASSWORD_LENGTH_SIGNUP -> "error_password_weak"
    else -> null
}
