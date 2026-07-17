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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.mifos.groupbanking.groupbanking.feature.loginsignup.model.AuthMode

// CommonPurse brand accents — forest green primary + amber accent.
private val CommonPurseGreen = Color(0xFF1B5E20)
private val CommonPurseGreenContainer = Color(0xFFE8F5E9)
private val CommonPurseAmber = Color(0xFFFFB300)

/**
 * Unified single-screen login/signup entry-point (login-signup feature).
 *
 * Renders the four screen states from `ui.yaml`:
 * - `content`  — mode toggle + credential fields + submit + biometric CTA
 * - `loading`  — progress spinner over the card while a call is in-flight
 * - `error`    — non-blocking error banner above the form fields
 * - `zero_groups` — success banner + create-group / join-with-code CTAs (AC5)
 */
@Composable
fun LoginSignupScreen(
    onNavigateToPersonalDashboard: () -> Unit,
    onNavigateToGroupList: () -> Unit,
    onNavigateToGroupTypePicker: () -> Unit,
    onNavigateToJoinWithCode: () -> Unit,
    onPromptBiometric: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginSignupViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                LoginSignupEvent.NavigateToPersonalDashboard -> onNavigateToPersonalDashboard()
                LoginSignupEvent.NavigateToGroupList -> onNavigateToGroupList()
                LoginSignupEvent.NavigateToGroupTypePicker -> onNavigateToGroupTypePicker()
                LoginSignupEvent.NavigateToJoinWithCode -> onNavigateToJoinWithCode()
                LoginSignupEvent.PromptBiometric -> onPromptBiometric()
                is LoginSignupEvent.ShowSnackbar -> onShowSnackbar(event.message)
            }
        }
    }

    LoginSignupContent(
        state = state,
        onAction = viewModel::trySendAction,
        modifier = modifier,
    )
}

@Composable
internal fun LoginSignupContent(
    state: LoginSignupState,
    onAction: (LoginSignupAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .testTag(LoginSignupTestTags.SCREEN),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp),
                colors = CardDefaults.cardColors(containerColor = CommonPurseGreenContainer),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "CommonPurse",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = CommonPurseGreen,
                    )

                    if (state.showZeroGroups) {
                        ZeroGroupsState(onAction = onAction)
                    } else {
                        AuthFormState(state = state, onAction = onAction)
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthFormState(
    state: LoginSignupState,
    onAction: (LoginSignupAction) -> Unit,
) {
    val isSignup = state.mode == AuthMode.SIGNUP

    // Mode toggle (content state — swaps fields without navigation, AC1).
    OutlinedButton(
        onClick = { onAction(LoginSignupAction.OnModeToggle) },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(LoginSignupTestTags.MODE_TOGGLE),
    ) {
        Text(if (isSignup) "Have an account? Log in" else "New here? Sign up")
    }

    // Error banner (error state — non-blocking, sits above the fields).
    state.error?.let { message ->
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(LoginSignupTestTags.ERROR_BANNER),
        )
    }

    if (isSignup) {
        OutlinedTextField(
            value = state.name,
            onValueChange = { onAction(LoginSignupAction.OnNameChange(it)) },
            label = { Text("Full name") },
            singleLine = true,
            isError = state.validationErrors.containsKey(LoginSignupViewModel.FIELD_NAME),
            supportingText = state.validationErrors[LoginSignupViewModel.FIELD_NAME]?.let {
                { Text(it) }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(LoginSignupTestTags.NAME_FIELD),
        )
    }

    OutlinedTextField(
        value = state.emailPhone,
        onValueChange = { onAction(LoginSignupAction.OnEmailPhoneChange(it)) },
        label = { Text("Email or phone") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        isError = state.validationErrors.containsKey(LoginSignupViewModel.FIELD_EMAIL_PHONE),
        supportingText = state.validationErrors[LoginSignupViewModel.FIELD_EMAIL_PHONE]?.let {
            { Text(it) }
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(LoginSignupTestTags.EMAIL_PHONE_FIELD),
    )

    OutlinedTextField(
        value = state.password,
        onValueChange = { onAction(LoginSignupAction.OnPasswordChange(it)) },
        label = { Text("Password") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        isError = state.validationErrors.containsKey(LoginSignupViewModel.FIELD_PASSWORD),
        supportingText = state.validationErrors[LoginSignupViewModel.FIELD_PASSWORD]?.let {
            { Text(it) }
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(LoginSignupTestTags.PASSWORD_FIELD),
    )

    // PIN is a login-only alternative to password (returning users).
    if (!isSignup) {
        OutlinedTextField(
            value = state.pin,
            onValueChange = { onAction(LoginSignupAction.OnPinChange(it)) },
            label = { Text("PIN (optional)") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(LoginSignupTestTags.PIN_FIELD),
        )
    }

    Button(
        onClick = {
            onAction(
                if (isSignup) LoginSignupAction.OnSignupTap else LoginSignupAction.OnLoginTap,
            )
        },
        enabled = !state.isSubmitting,
        colors = ButtonDefaults.buttonColors(containerColor = CommonPurseGreen),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(LoginSignupTestTags.SUBMIT_BUTTON),
    ) {
        if (state.isSubmitting) {
            // Loading state — spinner inside the submit button.
            CircularProgressIndicator(
                modifier = Modifier
                    .height(20.dp)
                    .testTag(LoginSignupTestTags.LOADING),
                color = Color.White,
                strokeWidth = 2.dp,
            )
        } else {
            Text(if (isSignup) "Create account" else "Log in")
        }
    }

    if (!isSignup) {
        if (state.isBiometricAvailable) {
            OutlinedButton(
                onClick = { onAction(LoginSignupAction.OnBiometricUnlock) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(LoginSignupTestTags.BIOMETRIC_CTA),
            ) {
                Icon(Icons.Filled.Fingerprint, contentDescription = "Biometric unlock")
                Spacer(Modifier.height(4.dp))
                Text("Unlock with biometrics")
            }
        }

        TextButton(
            onClick = { onAction(LoginSignupAction.OnForgotPassword) },
            modifier = Modifier.testTag(LoginSignupTestTags.FORGOT_PASSWORD),
        ) {
            Text("Forgot password?", color = CommonPurseGreen)
        }
    }
}

@Composable
private fun ZeroGroupsState(
    onAction: (LoginSignupAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(LoginSignupTestTags.ZERO_GROUPS),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "You're signed in!",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = CommonPurseGreen,
        )
        Text(
            text = "You aren't in any group yet. Create one or join with a code.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )

        Button(
            onClick = { onAction(LoginSignupAction.OnCreateGroupTap) },
            colors = ButtonDefaults.buttonColors(containerColor = CommonPurseGreen),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(LoginSignupTestTags.CREATE_GROUP_CTA),
        ) {
            Text("Create a group")
        }

        OutlinedButton(
            onClick = { onAction(LoginSignupAction.OnJoinWithCodeTap) },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CommonPurseAmber),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(LoginSignupTestTags.JOIN_WITH_CODE_CTA),
        ) {
            Text("Join with a code")
        }
    }
}
