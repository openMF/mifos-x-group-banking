/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.groupbanking.feature.loginsignup.ui

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.mifos.groupbanking.groupbanking.feature.loginsignup.data.AuthRepository
import org.mifos.groupbanking.groupbanking.feature.loginsignup.model.AuthMode
import org.mifos.groupbanking.groupbanking.feature.loginsignup.model.AuthSession
import org.mifos.groupbanking.groupbanking.feature.loginsignup.model.GroupMembership
import org.mifos.groupbanking.groupbanking.feature.loginsignup.model.LoginRequest
import org.mifos.groupbanking.groupbanking.feature.loginsignup.model.SelfRegisterRequest
import template.core.base.store.OfflineException
import template.core.base.ui.BaseViewModel

/**
 * MVI ViewModel for the unified login/signup screen.
 *
 * - [AuthMode] toggle swaps the visible field set with no navigation (AC1).
 * - `OnLoginTap` → COMP-AUTH-002, `OnSignupTap` → COMP-AUTH-001 (AC2).
 * - Post-auth routing is decided from `groupMemberships` (AC3): organizer-role →
 *   group-list, member-only → personal-dashboard, zero groups → `zero_groups` state
 *   (AC5) with create/join CTAs.
 * - Biometric unlock emits [LoginSignupEvent.PromptBiometric]; on success the host
 *   dispatches [LoginSignupAction.OnBiometricUnlock] which refreshes via COMP-AUTH-003 (AC4).
 */
class LoginSignupViewModel(
    private val authRepository: AuthRepository,
) : BaseViewModel<LoginSignupState, LoginSignupEvent, LoginSignupAction>(LoginSignupState()) {

    override fun handleAction(action: LoginSignupAction) {
        when (action) {
            LoginSignupAction.OnModeToggle -> updateState {
                copy(
                    mode = if (mode == AuthMode.LOGIN) AuthMode.SIGNUP else AuthMode.LOGIN,
                    error = null,
                    validationErrors = emptyMap(),
                )
            }

            is LoginSignupAction.OnNameChange -> updateState { copy(name = action.name) }
            is LoginSignupAction.OnEmailPhoneChange ->
                updateState { copy(emailPhone = action.emailPhone) }
            is LoginSignupAction.OnPasswordChange -> updateState { copy(password = action.password) }
            is LoginSignupAction.OnPinChange -> updateState { copy(pin = action.pin) }

            LoginSignupAction.OnLoginTap -> onLogin()
            LoginSignupAction.OnSignupTap -> onSignup()
            LoginSignupAction.OnBiometricUnlock -> onBiometricUnlock()
            LoginSignupAction.OnCreateGroupTap -> sendEvent(LoginSignupEvent.NavigateToGroupTypePicker)
            LoginSignupAction.OnJoinWithCodeTap -> sendEvent(LoginSignupEvent.NavigateToJoinWithCode)
            LoginSignupAction.OnForgotPassword ->
                sendEvent(LoginSignupEvent.ShowSnackbar("Password reset link sent"))
        }
    }

    private fun onLogin() {
        val errors = validateLogin()
        if (errors.isNotEmpty()) {
            updateState { copy(validationErrors = errors) }
            return
        }
        submit {
            authRepository.login(
                LoginRequest(
                    emailPhone = state.emailPhone,
                    password = state.password.ifBlank { null },
                    pin = state.pin.ifBlank { null },
                ),
            )
        }
    }

    private fun onSignup() {
        val errors = validateSignup()
        if (errors.isNotEmpty()) {
            updateState { copy(validationErrors = errors) }
            return
        }
        submit {
            authRepository.selfRegister(
                SelfRegisterRequest(
                    name = state.name,
                    emailPhone = state.emailPhone,
                    password = state.password,
                ),
            )
        }
    }

    /** AC4 — biometric re-auth refreshes the session via COMP-AUTH-003 then routes. */
    private fun onBiometricUnlock() {
        sendEvent(LoginSignupEvent.PromptBiometric)
        submitProfileRefresh()
    }

    private fun submit(call: suspend () -> AuthSession) {
        updateState { copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            runCatching { call() }
                .onSuccess { session ->
                    updateState {
                        copy(
                            isSubmitting = false,
                            sessionToken = session.sessionToken,
                            groupMemberships = session.groupMemberships,
                        )
                    }
                    route(session)
                }
                .onFailure { throwable ->
                    updateState {
                        copy(isSubmitting = false, error = throwable.toUserMessage())
                    }
                    sendEvent(LoginSignupEvent.ShowSnackbar(throwable.toUserMessage()))
                }
        }
    }

    private fun submitProfileRefresh() {
        updateState { copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            runCatching { authRepository.me() }
                .onSuccess { profile ->
                    updateState {
                        copy(isSubmitting = false, groupMemberships = profile.groupMemberships)
                    }
                    routeMemberships(profile.groupMemberships)
                }
                .onFailure { throwable ->
                    updateState { copy(isSubmitting = false, error = throwable.toUserMessage()) }
                    sendEvent(LoginSignupEvent.ShowSnackbar(throwable.toUserMessage()))
                }
        }
    }

    private fun route(session: AuthSession) {
        if (session.hasNoGroups) {
            // Stay on-screen and surface the zero_groups CTA state.
            return
        }
        routeMemberships(session.groupMemberships)
    }

    private fun routeMemberships(memberships: List<GroupMembership>) {
        if (memberships.isEmpty()) return
        val session = AuthSession(
            userId = "",
            sessionToken = state.sessionToken.orEmpty(),
            tokenExpiresAt = "",
            groupMemberships = memberships,
        )
        if (session.hasOrganizerRole) {
            sendEvent(LoginSignupEvent.NavigateToGroupList)
        } else {
            sendEvent(LoginSignupEvent.NavigateToPersonalDashboard)
        }
    }

    private fun validateLogin(): Map<String, String> = buildMap {
        if (state.emailPhone.isBlank()) put(FIELD_EMAIL_PHONE, "Email or phone is required")
        if (state.password.isBlank() && state.pin.isBlank()) {
            put(FIELD_PASSWORD, "Enter a password or PIN")
        }
    }

    private fun validateSignup(): Map<String, String> = buildMap {
        if (state.name.isBlank()) put(FIELD_NAME, "Name is required")
        if (state.emailPhone.isBlank()) put(FIELD_EMAIL_PHONE, "Email or phone is required")
        if (state.password.length < MIN_PASSWORD_LENGTH) {
            put(FIELD_PASSWORD, "Password must be at least $MIN_PASSWORD_LENGTH characters")
        }
    }

    private fun Throwable.toUserMessage(): String = when (this) {
        is OfflineException -> "Requires internet connection"
        else -> message ?: "Something went wrong. Please try again."
    }

    companion object {
        const val FIELD_NAME = "name"
        const val FIELD_EMAIL_PHONE = "emailPhone"
        const val FIELD_PASSWORD = "password"
        const val MIN_PASSWORD_LENGTH = 8
    }
}

/** UDF state for the login-signup screen. */
data class LoginSignupState(
    val mode: AuthMode = AuthMode.LOGIN,
    val name: String = "",
    val emailPhone: String = "",
    val password: String = "",
    val pin: String = "",
    val isSubmitting: Boolean = false,
    val validationErrors: Map<String, String> = emptyMap(),
    val error: String? = null,
    val isBiometricAvailable: Boolean = false,
    val sessionToken: String? = null,
    val groupMemberships: List<GroupMembership> = emptyList(),
) {
    /** True once auth has succeeded but the user belongs to zero groups (AC5). */
    val showZeroGroups: Boolean get() = sessionToken != null && groupMemberships.isEmpty()
}

/** MVI actions dispatched from the screen. */
sealed interface LoginSignupAction {
    data object OnModeToggle : LoginSignupAction
    data class OnNameChange(val name: String) : LoginSignupAction
    data class OnEmailPhoneChange(val emailPhone: String) : LoginSignupAction
    data class OnPasswordChange(val password: String) : LoginSignupAction
    data class OnPinChange(val pin: String) : LoginSignupAction
    data object OnLoginTap : LoginSignupAction
    data object OnSignupTap : LoginSignupAction
    data object OnBiometricUnlock : LoginSignupAction
    data object OnCreateGroupTap : LoginSignupAction
    data object OnJoinWithCodeTap : LoginSignupAction
    data object OnForgotPassword : LoginSignupAction
}

/** One-shot navigation / notification events. */
sealed interface LoginSignupEvent {
    data object NavigateToPersonalDashboard : LoginSignupEvent
    data object NavigateToGroupList : LoginSignupEvent
    data object NavigateToGroupTypePicker : LoginSignupEvent
    data object NavigateToJoinWithCode : LoginSignupEvent
    data object PromptBiometric : LoginSignupEvent
    data class ShowSnackbar(val message: String) : LoginSignupEvent
}
