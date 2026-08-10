/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.loginsignup

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
import kpt.core.data.repository.AuthRepository
import kpt.core.model.AuthSession
import kpt.core.model.GroupMembership
import kpt.core.model.LoginCredentials
import kpt.core.model.SelfRegistration
import kpt.core.model.UserProfile
import kpt.core.model.isGroupLeadership

private const val TAG = "LoginSignupViewModel"

// Demo Explore signs in as the live companion demo user — Amina Otieno, organizer+treasurer of the
// Mwangaza Women's Group. A REAL companion login (not an offline seed) so every screen renders real
// data (member profiles, meetings, savings) with real numeric ids. Public demo credentials.
private const val DEMO_EMAIL_PHONE = "+254700000001"
private const val DEMO_PASSWORD = "DemoExplore@2026"

private const val MIN_NAME_LENGTH = 2
private const val MIN_PASSWORD_LENGTH_SIGNUP = 8

// Fineract password-policy target surfaced live via the signup password-requirement chips
// (ui.yaml#components.password_requirement_chips). These are ADVISORY per-rule feedback — the
// submit-enable gate still uses the min-8 [validatePassword] check (see class KDoc / handleSignupTap)
// so existing behaviour + tests are preserved; the chips guide users toward a policy-compliant
// password so signup stops bouncing off a raw Fineract 400.
private const val STRONG_PASSWORD_MIN_LENGTH = 12
private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
private val E164_PHONE_REGEX = Regex("^\\+[1-9]\\d{6,14}$")

// Sign-in also accepts a username: 3-30 chars, starts alphanumeric, then letters/digits/._-
private val USERNAME_REGEX = Regex("^[A-Za-z0-9][A-Za-z0-9._-]{2,29}$")

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
 * Live per-rule verdict for the signup password, mirroring the Fineract password policy. Each
 * flag is recomputed from the current [LoginSignupState.password] by [evaluatePasswordRequirements]
 * on every [LoginSignupAction.OnPasswordChange] and rendered as a filled/muted chip
 * (ui.yaml#components.password_requirement_chips). ADVISORY only — see [STRONG_PASSWORD_MIN_LENGTH].
 * See API.md#state.
 */
@Serializable
@Immutable
data class PasswordRequirements(
    /** ≥ [STRONG_PASSWORD_MIN_LENGTH] characters. */
    val hasMinLength: Boolean = false,
    /** Contains BOTH an upper- and a lower-case letter. */
    val hasMixedCase: Boolean = false,
    /** Contains at least one digit. */
    val hasDigit: Boolean = false,
    /** Contains at least one non-alphanumeric symbol. */
    val hasSymbol: Boolean = false,
    /** No two adjacent identical characters. */
    val hasNoRepeats: Boolean = false,
) {
    /** True only when every Fineract-policy rule is satisfied. */
    val allSatisfied: Boolean
        get() = hasMinLength && hasMixedCase && hasDigit && hasSymbol && hasNoRepeats
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
    // Live per-rule verdict for the signup password (ui.yaml#components.password_requirement_chips),
    // recomputed on every OnPasswordChange. Advisory feedback only — see PasswordRequirements KDoc.
    val passwordRequirements: PasswordRequirements = PasswordRequirements(),
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
    // Demo Explore confirm-dialog visibility (ui.yaml#demo_confirm_dialog, F1/B1/G1).
    val showDemoDialog: Boolean = false,
    // True while the offline demo fixture hydrates into the local cache (ui.yaml#state.isSeedingDemo).
    val isSeedingDemo: Boolean = false,
    // Invite code carried pre-auth from join-with-code (nav_param). When present + 6-char, an
    // on_login/on_signup success resumes the join instead of the default landing (TC-LS-010).
    val pendingInviteCode: String? = null,
)

/**
 * One-shot side effects emitted by `LoginSignupViewModel` — verbatim mirror of
 * ui.yaml#state_model.LoginSignupViewModel.events. See API.md#events.
 */
sealed interface LoginSignupEvent {
    data object NavigateToPersonalDashboard : LoginSignupEvent
    data object NavigateToOrganizerDashboard : LoginSignupEvent
    data object NavigateToGroupList : LoginSignupEvent
    data object NavigateToGroupTypePicker : LoginSignupEvent

    /**
     * Navigates to join-with-code. [inviteCode] is null for a pre-auth Accept-Invitation tap or a
     * zero-groups Join tap (the code is typed on join-with-code); non-null when an authenticated
     * success RESUMES a pending pre-auth join (TC-LS-010) — ui.yaml#events.NavigateToJoinWithCode.
     */
    data class NavigateToJoinWithCode(val inviteCode: String? = null) : LoginSignupEvent
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

    /** First-class PRE-AUTH accept-invitation entry (ui.yaml#accept_invitation_button, F2/B4/G2). */
    data object OnAcceptInvitationTap : LoginSignupAction

    /** Opens the Demo Explore confirm dialog (ui.yaml#demo_explore_button, F1/B1/G1). */
    data object OnDemoExplore : LoginSignupAction

    /** Confirms Demo Explore — seeds the offline demo session then lands on organizer-dashboard. */
    data object OnDemoConfirm : LoginSignupAction

    /** Dismisses the Demo Explore confirm dialog (ui.yaml#demo_dialog.cancel). */
    data object OnDemoCancel : LoginSignupAction

    /** Async coroutine results — routed via `trySendAction`, never dispatched by the UI. */
    sealed interface Internal : LoginSignupAction {
        data class SessionChecked(val session: AuthSession?) : Internal
        data class LoginResult(val result: NetworkResult<AuthSession, NetworkError>) : Internal
        data class SignupResult(val result: NetworkResult<AuthSession, NetworkError>) : Internal
        data class BiometricResult(val result: NetworkResult<UserProfile, NetworkError>) : Internal

        /** Seeds the [pendingInviteCode] nav_param into state on first composition (TC-LS-010). */
        data class SetPendingInviteCode(val code: String?) : Internal

        /** Result of the Demo Explore companion login (real session, not an offline seed). */
        data class DemoLoginResult(val result: NetworkResult<AuthSession, NetworkError>) : Internal
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
    private var demoJob: Job? = null
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
            LoginSignupAction.OnAcceptInvitationTap -> handleAcceptInvitationTap()
            LoginSignupAction.OnDemoExplore -> handleDemoExplore()
            LoginSignupAction.OnDemoConfirm -> handleDemoConfirm()
            LoginSignupAction.OnDemoCancel -> handleDemoCancel()
            is LoginSignupAction.Internal.SessionChecked -> handleSessionChecked(action.session)
            is LoginSignupAction.Internal.LoginResult -> handleLoginResult(action.result)
            is LoginSignupAction.Internal.SignupResult -> handleSignupResult(action.result)
            is LoginSignupAction.Internal.BiometricResult -> handleBiometricResult(action.result)
            is LoginSignupAction.Internal.SetPendingInviteCode -> handleSetPendingInviteCode(action.code)
            is LoginSignupAction.Internal.DemoLoginResult -> handleDemoLoginResult(action.result)
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
                passwordRequirements = PasswordRequirements(),
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
            copy(
                password = value,
                passwordRequirements = evaluatePasswordRequirements(value),
                validationErrors = validationErrors - "password",
            )
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
            // Sign-in accepts email, phone OR username (RULE-GAP-IDEA-FIRST: login-signup ui.yaml#login_identifier_field)
            validateSignInIdentifier(state.emailPhone)?.let { put("emailPhone", it) }
            validatePassword(state.password, requireStrong = false)?.let { put("password", it) }
        }
        if (errors.isNotEmpty()) {
            Logger.w(TAG) { "login blocked by client-side validation: fields=${errors.keys}" }
            updateState { copy(validationErrors = errors) }
            return
        }

        // A fresh password login navigates to the landing screen on success by itself. Consume the
        // biometric auto-prompt here so the post-login `currentSession` emission does NOT also fire
        // PromptBiometric → refreshSession → a SECOND navigation. That double-navigation spawned a
        // duplicate dashboard instance whose data stream raced/was cancelled, leaving the visible
        // screen stuck on the skeleton while the first instance had already loaded Content.
        hasAutoPromptedBiometric = true
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

        // Fresh signup navigates on success itself — consume the biometric auto-prompt so the
        // post-signup currentSession emission does not fire a second, duplicate navigation.
        hasAutoPromptedBiometric = true
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
        // Zero-groups Join tap carries no code — the user types it on join-with-code.
        sendEvent(LoginSignupEvent.NavigateToJoinWithCode(inviteCode = null))
    }

    // -- Pre-auth accept-invitation (ui.yaml effect: navigate, F2/B4/G2) ------------------------

    private fun handleAcceptInvitationTap() {
        analytics.trackGroupOperation(operation = "join")
        Logger.i(TAG) { "accept-invitation (pre-auth) tapped" }
        // First-class pre-auth entry: navigate to join-with-code with NO pre-filled code; that
        // screen routes back with pendingInviteCode which on_login/on_signup resumes (TC-LS-010).
        sendEvent(LoginSignupEvent.NavigateToJoinWithCode(inviteCode = null))
    }

    // -- Demo Explore offline guest session (ui.yaml#demo_confirm_dialog, F1/B1/G1) --------------

    private fun handleDemoExplore() {
        Logger.i(TAG) { "demo-explore tapped — opening confirm dialog" }
        updateState { copy(showDemoDialog = true) }
    }

    private fun handleDemoCancel() {
        Logger.i(TAG) { "demo-explore dialog dismissed" }
        updateState { copy(showDemoDialog = false) }
    }

    private fun handleDemoConfirm() {
        // Demo entry navigates on login success itself — consume the biometric auto-prompt.
        hasAutoPromptedBiometric = true
        demoJob?.cancel()
        demoJob = viewModelScope.launch {
            // A REAL companion login as the demo-explore user (not an offline seed): the live
            // companion returns real group/member/meeting data, so every screen works — member
            // profiles, meetings, savings — with real numeric ids. isSeedingDemo drives the dialog
            // spinner; handleDemoLoginResult clears it + routes on the returned session.
            updateState { copy(showDemoDialog = false, isSeedingDemo = true, error = null) }
            val credentials = LoginCredentials(emailPhone = DEMO_EMAIL_PHONE, password = DEMO_PASSWORD)
            val result = authRepository.login(credentials)
            trySendAction(LoginSignupAction.Internal.DemoLoginResult(result))
        }
    }

    private fun handleSetPendingInviteCode(code: String?) {
        Logger.i(TAG) { "pendingInviteCode set present=${code != null}" }
        updateState { copy(pendingInviteCode = code) }
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
                        passwordRequirements = PasswordRequirements(),
                        screenState = screenStateFor(session.groupMemberships),
                        error = null,
                    )
                }
                // Pre-auth invite resume takes precedence over the default landing (TC-LS-010).
                (pendingInviteResumeEvent() ?: routeEvent(session.groupMemberships))?.let(::sendEvent)
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
                        passwordRequirements = PasswordRequirements(),
                        screenState = screenStateFor(session.groupMemberships),
                        error = null,
                    )
                }
                // Pre-auth invite resume takes precedence over the default landing (TC-LS-010).
                (pendingInviteResumeEvent() ?: routeEvent(session.groupMemberships))?.let(::sendEvent)
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

    private fun handleDemoLoginResult(result: NetworkResult<AuthSession, NetworkError>) {
        when (result) {
            is NetworkResult.Success -> {
                val session = result.data
                analytics.trackLogin(method = "demo", success = true)
                Logger.i(TAG) { "demo login success groupCount=${session.groupMemberships.size} — routing" }
                updateState {
                    copy(
                        isSeedingDemo = false,
                        sessionToken = session.sessionToken,
                        groupMemberships = session.groupMemberships,
                        screenState = screenStateFor(session.groupMemberships),
                        error = null,
                    )
                }
                // Route on the demo user's real memberships — same landing logic as a normal login.
                (pendingInviteResumeEvent() ?: routeEvent(session.groupMemberships))?.let(::sendEvent)
            }
            is NetworkResult.Error -> {
                analytics.trackLogin(method = "demo", success = false, errorCode = result.error.name)
                crashReporter.recordMessage(
                    message = "login-signup: demo login failed networkError=${result.error}",
                    level = CrashSeverity.Warning,
                )
                updateState {
                    copy(
                        isSeedingDemo = false,
                        screenState = LoginSignupScreenState.Error,
                        error = result.error.toLoginSignupError(AuthErrorContext.Login),
                    )
                }
            }
        }
    }

    // -- Helpers ----------------------------------------------------------------------------------

    /**
     * F3/F4 invite-resume handoff (TC-LS-010): when a valid 6-char [LoginSignupState.pendingInviteCode]
     * was carried pre-auth from join-with-code, an authenticated success resumes the join
     * (`NavigateToJoinWithCode(code)`) instead of the default membership-based landing. Returns the
     * resume event, or null when there is no pending invite (caller falls back to [routeEvent]).
     */
    private fun pendingInviteResumeEvent(): LoginSignupEvent.NavigateToJoinWithCode? {
        val pending = state.pendingInviteCode
        return if (pending != null && pending.length == PENDING_INVITE_CODE_LENGTH) {
            LoginSignupEvent.NavigateToJoinWithCode(inviteCode = pending)
        } else {
            null
        }
    }

    private fun screenStateFor(memberships: List<GroupMembership>): LoginSignupScreenState =
        if (memberships.isEmpty()) LoginSignupScreenState.ZeroGroups else LoginSignupScreenState.Content

    /**
     * Routes on `groupMemberships` per SPEC.md — a member holding ANY committee/leadership role in ANY
     * group lands on the organizer-per-group hub (`organizer-dashboard`, whose `ui.yaml#entry_points`
     * declare exactly this `app_launch` trigger gated on `isOrganizerInAnyGroup`); a plain member
     * lands on `personal-dashboard`. Leadership = ORGANIZER / TREASURER / SECRETARY (the companion
     * folds CHAIRPERSON onto ORGANIZER) — the whole management committee runs the group, not just the
     * ORGANIZER row, so e.g. the treasurer showcase login (Amina) reaches the organizer hub.
     * `group-list` stays reachable from the organizer hub's All-Groups quick-nav
     * (`organizer-dashboard#onNavigateToGroupList`) and from `personal-dashboard`.
     */
    private fun routeEvent(memberships: List<GroupMembership>): LoginSignupEvent? = when {
        memberships.isEmpty() -> null
        memberships.any { it.role.isGroupLeadership } -> LoginSignupEvent.NavigateToOrganizerDashboard
        else -> LoginSignupEvent.NavigateToPersonalDashboard
    }

    private companion object {
        const val PIN_MAX_LENGTH = 4
        const val PENDING_INVITE_CODE_LENGTH = 6
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

// Sign-in identifier accepts email OR E.164 phone OR username (see USERNAME_REGEX).
private fun isValidSignInIdentifier(value: String): Boolean =
    isValidEmailOrPhone(value) || USERNAME_REGEX.matches(value)

private fun validateSignInIdentifier(value: String): String? =
    if (value.isBlank() || !isValidSignInIdentifier(value)) "error_identifier_invalid" else null

private fun validateName(name: String): String? =
    if (name.trim().length < MIN_NAME_LENGTH) "error_name_required" else null

private fun validateEmailPhone(value: String): String? =
    if (value.isBlank() || !isValidEmailOrPhone(value)) "error_email_phone_invalid" else null

/**
 * Pure per-rule evaluation of the signup password against the Fineract password policy, surfaced
 * live via the requirement chips. An empty password yields an all-`false` verdict so no chip lights
 * up before the user types. See [PasswordRequirements].
 */
internal fun evaluatePasswordRequirements(password: String): PasswordRequirements {
    if (password.isEmpty()) return PasswordRequirements()
    return PasswordRequirements(
        hasMinLength = password.length >= STRONG_PASSWORD_MIN_LENGTH,
        hasMixedCase = password.any { it.isUpperCase() } && password.any { it.isLowerCase() },
        hasDigit = password.any { it.isDigit() },
        hasSymbol = password.any { !it.isLetterOrDigit() },
        hasNoRepeats = password.zipWithNext().none { (a, b) -> a == b },
    )
}

/** `requireStrong` gates the signup-only min-8-length strength check per ui.yaml#password_field. */
private fun validatePassword(password: String, requireStrong: Boolean): String? = when {
    password.isBlank() -> "error_password_weak"
    requireStrong && password.length < MIN_PASSWORD_LENGTH_SIGNUP -> "error_password_weak"
    else -> null
}
