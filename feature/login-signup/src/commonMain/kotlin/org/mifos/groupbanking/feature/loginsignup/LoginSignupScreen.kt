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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kpt.core.base.designsystem.component.AppCard
import kpt.core.base.designsystem.component.KptButton
import kpt.core.base.designsystem.component.KptOutlinedButton
import kpt.core.base.ui.effects.EventsEffect
import kpt.core.designsystem.theme.spacing
import kpt.core.ui.scaffold.KptScaffold
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.mifos.groupbanking.feature.loginsignup.components.AuthBrandHeader
import org.mifos.groupbanking.feature.loginsignup.components.AuthDividerLabeled
import org.mifos.groupbanking.feature.loginsignup.components.AuthErrorBanner
import org.mifos.groupbanking.feature.loginsignup.components.AuthModeToggleTabs
import org.mifos.groupbanking.feature.loginsignup.components.AuthTextField
import org.mifos.groupbanking.feature.loginsignup.components.ZeroGroupsEmptyState
import org.mifos.groupbanking.feature.loginsignup.generated.resources.Res
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_action_biometric
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_action_create_account
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_action_forgot_password
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_action_sign_in
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_biometric_cd
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_create_account_cd
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_divider_or
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_error_account_exists
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_error_biometric_failed
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_error_invalid_credentials
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_error_network
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_error_server
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_error_weak_password
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_field_email_phone_cd
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_field_email_phone_label
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_field_email_phone_placeholder
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_field_identifier_cd
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_field_identifier_label
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_field_identifier_placeholder
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_field_name_cd
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_field_name_label
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_field_name_placeholder
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_field_password_cd
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_field_password_label
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_field_password_placeholder_login
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_field_password_placeholder_signup
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_forgot_password_cd
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_forgot_password_snackbar
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_sign_in_cd
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_title
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_validation_email_phone_invalid
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_validation_identifier_invalid
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_validation_name_required
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_validation_password_weak

/**
 * Container for the unified login/signup entry screen (`login-signup-screen`). Collects
 * [LoginSignupViewModel] state via [collectAsStateWithLifecycle], consumes one-shot
 * [LoginSignupEvent]s (navigation + snackbar + biometric auto-prompt) through [EventsEffect],
 * and delegates all rendering to the stateless [LoginSignupContent]. See API.md#screen.
 */
@Composable
internal fun LoginSignupScreen(
    onNavigateToPersonalDashboard: () -> Unit,
    onNavigateToOrganizerDashboard: () -> Unit,
    onNavigateToGroupList: () -> Unit,
    onNavigateToGroupTypePicker: () -> Unit,
    onNavigateToJoinWithCode: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginSignupViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val forgotPasswordSnackbarMessage = stringResource(Res.string.screens_login_signup_forgot_password_snackbar)

    EventsEffect(viewModel) { event ->
        when (event) {
            LoginSignupEvent.NavigateToPersonalDashboard -> onNavigateToPersonalDashboard()
            LoginSignupEvent.NavigateToOrganizerDashboard -> onNavigateToOrganizerDashboard()
            LoginSignupEvent.NavigateToGroupList -> onNavigateToGroupList()
            LoginSignupEvent.NavigateToGroupTypePicker -> onNavigateToGroupTypePicker()
            LoginSignupEvent.NavigateToJoinWithCode -> onNavigateToJoinWithCode()
            is LoginSignupEvent.ShowSnackbar -> snackbarHostState.showSnackbar(
                message = when (event.message) {
                    "login_signup_forgot_password_snackbar" -> forgotPasswordSnackbarMessage
                    else -> forgotPasswordSnackbarMessage
                },
            )
            // The platform biometric-hardware gate lives inside AuthRepository.refreshSession
            // (core/data) — see LoginSignupViewModel class KDoc. Re-dispatching OnBiometricUnlock
            // here mirrors a manual tap on the biometric button, now driven automatically because
            // Internal.SessionChecked already confirmed a valid stored session token.
            LoginSignupEvent.PromptBiometric -> viewModel.trySendAction(LoginSignupAction.OnBiometricUnlock)
        }
    }

    LoginSignupContent(
        state = state,
        onAction = viewModel::trySendAction,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

/**
 * Stateless render surface for `login-signup-screen`. State-driven per
 * `ui.yaml#state_model.screen_state` — every [LoginSignupScreenState] member is handled. See
 * API.md#screen.
 */
@Composable
internal fun LoginSignupContent(
    state: LoginSignupState,
    onAction: (LoginSignupAction) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    KptScaffold(
        modifier = modifier.testTag(LoginSignupTestTags.SCREEN),
        snackbarHostState = snackbarHostState,
    ) {
        when (state.screenState) {
            LoginSignupScreenState.Loading -> AuthFormSection(state = state, onAction = onAction, isLoading = true)
            LoginSignupScreenState.Content -> AuthFormSection(state = state, onAction = onAction, isLoading = false)
            LoginSignupScreenState.Error -> AuthFormSection(state = state, onAction = onAction, isLoading = false)
            LoginSignupScreenState.ZeroGroups -> ZeroGroupsSection(onAction = onAction)
        }
    }
}

/**
 * Renders the LOGIN or SIGNUP form depending on `state.mode`. Backs the `content`, `loading`,
 * and `error` members of `ui.yaml#states` — they share the same component set, differing only
 * by `isLoading` (fields/tabs disabled, submit spinner) and `state.error` visibility. See
 * API.md#screen.
 */
@Composable
internal fun AuthFormSection(
    state: LoginSignupState,
    onAction: (LoginSignupAction) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val forgotPasswordCd = stringResource(Res.string.screens_login_signup_forgot_password_cd)
    val loginButtonCd = stringResource(Res.string.screens_login_signup_sign_in_cd)
    val signupButtonCd = stringResource(Res.string.screens_login_signup_create_account_cd)
    val biometricButtonCd = stringResource(Res.string.screens_login_signup_biometric_cd)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = sp.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(sp.xxl))
        AuthBrandHeader()
        Spacer(Modifier.height(sp.lg))

        AppCard(modifier = Modifier.fillMaxWidth().testTag(LoginSignupTestTags.AUTH_CARD)) {
            Column {
                AuthModeToggleTabs(
                    selectedMode = state.mode,
                    enabled = !isLoading,
                    onModeSelected = { onAction(LoginSignupAction.OnModeToggle(it)) },
                )
                Spacer(Modifier.height(sp.lg))

                state.error?.let { error ->
                    AuthErrorBanner(message = error.resolvedMessage())
                    Spacer(Modifier.height(sp.lg))
                }

                if (state.mode == AuthMode.Signup) {
                    AuthTextField(
                        value = state.name,
                        label = stringResource(Res.string.screens_login_signup_field_name_label),
                        placeholder = stringResource(Res.string.screens_login_signup_field_name_placeholder),
                        onValueChange = { onAction(LoginSignupAction.OnNameChange(it)) },
                        enabled = !isLoading,
                        errorMessage = state.validationErrors["name"]?.let { validationMessage(it) },
                        contentDescription = stringResource(Res.string.screens_login_signup_field_name_cd),
                        modifier = Modifier.testTag(LoginSignupTestTags.FIELD_NAME),
                    )
                    Spacer(Modifier.height(sp.sm))
                }

                val isLoginMode = state.mode == AuthMode.Login
                AuthTextField(
                    value = state.emailPhone,
                    label = stringResource(
                        if (isLoginMode) {
                            Res.string.screens_login_signup_field_identifier_label
                        } else {
                            Res.string.screens_login_signup_field_email_phone_label
                        },
                    ),
                    placeholder = stringResource(
                        if (isLoginMode) {
                            Res.string.screens_login_signup_field_identifier_placeholder
                        } else {
                            Res.string.screens_login_signup_field_email_phone_placeholder
                        },
                    ),
                    onValueChange = { onAction(LoginSignupAction.OnEmailPhoneChange(it)) },
                    enabled = !isLoading,
                    keyboardType = if (isLoginMode) KeyboardType.Text else KeyboardType.Email,
                    errorMessage = state.validationErrors["emailPhone"]?.let { validationMessage(it) },
                    contentDescription = stringResource(
                        if (isLoginMode) {
                            Res.string.screens_login_signup_field_identifier_cd
                        } else {
                            Res.string.screens_login_signup_field_email_phone_cd
                        },
                    ),
                    modifier = Modifier.testTag(LoginSignupTestTags.FIELD_EMAIL_PHONE),
                )
                Spacer(Modifier.height(sp.sm))

                AuthTextField(
                    value = state.password,
                    label = stringResource(Res.string.screens_login_signup_field_password_label),
                    placeholder = if (state.mode == AuthMode.Signup) {
                        stringResource(Res.string.screens_login_signup_field_password_placeholder_signup)
                    } else {
                        stringResource(Res.string.screens_login_signup_field_password_placeholder_login)
                    },
                    onValueChange = { onAction(LoginSignupAction.OnPasswordChange(it)) },
                    enabled = !isLoading,
                    isPassword = true,
                    errorMessage = state.validationErrors["password"]?.let { validationMessage(it) },
                    contentDescription = stringResource(Res.string.screens_login_signup_field_password_cd),
                    modifier = Modifier.testTag(LoginSignupTestTags.FIELD_PASSWORD),
                )

                if (state.mode == AuthMode.Login) {
                    TextButton(
                        onClick = { onAction(LoginSignupAction.OnForgotPassword) },
                        enabled = !isLoading,
                        modifier = Modifier
                            .align(Alignment.End)
                            .heightIn(min = sp.touchTargetMin)
                            .testTag(LoginSignupTestTags.FORGOT_PASSWORD_LINK)
                            .semantics { contentDescription = forgotPasswordCd },
                    ) {
                        Text(text = stringResource(Res.string.screens_login_signup_action_forgot_password))
                    }
                }

                Spacer(Modifier.height(sp.lg))

                if (state.mode == AuthMode.Login) {
                    KptButton(
                        onClick = { onAction(LoginSignupAction.OnLoginTap) },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .testTag(LoginSignupTestTags.LOGIN_BUTTON)
                            .semantics { contentDescription = loginButtonCd },
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(Modifier.width(sp.sm))
                        }
                        Text(stringResource(Res.string.screens_login_signup_action_sign_in))
                    }

                    if (state.isBiometricAvailable && !isLoading) {
                        Spacer(Modifier.height(sp.lg))
                        AuthDividerLabeled(label = stringResource(Res.string.screens_login_signup_divider_or))
                        Spacer(Modifier.height(sp.md))
                        KptOutlinedButton(
                            onClick = { onAction(LoginSignupAction.OnBiometricUnlock) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .testTag(LoginSignupTestTags.BIOMETRIC_BUTTON)
                                .semantics { contentDescription = biometricButtonCd },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Fingerprint,
                                contentDescription = biometricButtonCd,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(sp.sm))
                            Text(stringResource(Res.string.screens_login_signup_action_biometric))
                        }
                    }
                } else {
                    KptButton(
                        onClick = { onAction(LoginSignupAction.OnSignupTap) },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .testTag(LoginSignupTestTags.SIGNUP_BUTTON)
                            .semantics { contentDescription = signupButtonCd },
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(Modifier.width(sp.sm))
                        }
                        Text(stringResource(Res.string.screens_login_signup_action_create_account))
                    }
                }

                Spacer(Modifier.height(sp.lg))
            }
        }
        Spacer(Modifier.height(sp.xxl))
    }
}

/**
 * Post-auth onboarding empty state — `ui.yaml#states.zero_groups`. See API.md#screen.
 */
@Composable
internal fun ZeroGroupsSection(onAction: (LoginSignupAction) -> Unit, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = sp.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(sp.xxl))
        Text(
            text = stringResource(Res.string.screens_login_signup_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(sp.xxl))
        ZeroGroupsEmptyState(
            onCreateGroupClick = { onAction(LoginSignupAction.OnCreateGroupTap) },
            onJoinWithCodeClick = { onAction(LoginSignupAction.OnJoinWithCodeTap) },
        )
        Spacer(Modifier.height(sp.xxl))
    }
}

/** Maps [LoginSignupError] to its localized message — mirrors `messageKey` on each variant. */
@Composable
private fun LoginSignupError.resolvedMessage(): String = when (this) {
    LoginSignupError.InvalidCredentials -> stringResource(Res.string.screens_login_signup_error_invalid_credentials)
    LoginSignupError.AccountExists -> stringResource(Res.string.screens_login_signup_error_account_exists)
    LoginSignupError.WeakPassword -> stringResource(Res.string.screens_login_signup_error_weak_password)
    LoginSignupError.Network -> stringResource(Res.string.screens_login_signup_error_network)
    LoginSignupError.Server -> stringResource(Res.string.screens_login_signup_error_server)
    LoginSignupError.BiometricFailed -> stringResource(Res.string.screens_login_signup_error_biometric_failed)
}

/**
 * Maps a `LoginSignupState.validationErrors` value (an `error_key` string emitted by the
 * ViewModel, mirroring `ui.yaml#components.*.validation.error_key`) to its localized message.
 */
@Composable
private fun validationMessage(errorKey: String): String = when (errorKey) {
    "error_name_required" -> stringResource(Res.string.screens_login_signup_validation_name_required)
    "error_email_phone_invalid" -> stringResource(Res.string.screens_login_signup_validation_email_phone_invalid)
    "error_identifier_invalid" -> stringResource(Res.string.screens_login_signup_validation_identifier_invalid)
    "error_password_weak" -> stringResource(Res.string.screens_login_signup_validation_password_weak)
    else -> stringResource(Res.string.screens_login_signup_validation_email_phone_invalid)
}
