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

import androidx.compose.runtime.Composable
import kpt.core.designsystem.theme.KptTheme
import kpt.feature.loginsignup.components.DemoExploreConfirmDialog
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider

/**
 * `@Preview` gallery for `LoginSignupScreen.kt`. See API.md#preview. Data source: `demo-data.yaml`
 * (`states.content.demo_data` / `states.error.demo_data` / `states.zero_groups.demo_data`) plus
 * realistic personas from `demo-data.yaml#entries` (Grace Wanjiku, Amina Otieno).
 */
private class LoginSignupScreenPreviewProvider : PreviewParameterProvider<LoginSignupState> {
    override val values: Sequence<LoginSignupState> = sequenceOf(
        // content — login mode (ui.yaml#states.content.demo_data)
        LoginSignupState(
            mode = AuthMode.Login,
            emailPhone = "grace.wanjiku@example.com",
            password = "",
            isBiometricAvailable = true,
            screenState = LoginSignupScreenState.Content,
        ),
        // content — signup mode (demo-data.yaml SelfRegisterRequest: Amina Otieno)
        LoginSignupState(
            mode = AuthMode.Signup,
            name = "Amina Otieno",
            emailPhone = "+254798765432",
            password = "",
            isBiometricAvailable = false,
            screenState = LoginSignupScreenState.Content,
        ),
        // loading — API call in-flight, form disabled, spinner on submit button
        LoginSignupState(
            mode = AuthMode.Login,
            emailPhone = "grace.wanjiku@example.com",
            password = "Savings2026!",
            isSubmitting = true,
            isBiometricAvailable = true,
            screenState = LoginSignupScreenState.Loading,
        ),
        // error (ui.yaml#states.error.demo_data)
        LoginSignupState(
            mode = AuthMode.Login,
            emailPhone = "grace.wanjiku@example.com",
            password = "",
            isBiometricAvailable = true,
            error = LoginSignupError.InvalidCredentials,
            screenState = LoginSignupScreenState.Error,
        ),
        // zero_groups (ui.yaml#states.zero_groups.demo_data)
        LoginSignupState(
            sessionToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.amina",
            groupMemberships = emptyList(),
            screenState = LoginSignupScreenState.ZeroGroups,
        ),
    )
}

@Preview
@Composable
private fun LoginSignupScreenPreview(
    @PreviewParameter(LoginSignupScreenPreviewProvider::class)
    state: LoginSignupState,
) {
    KptTheme {
        LoginSignupContent(state = state, onAction = {})
    }
}

@Preview
@Composable
private fun AuthFormSectionContentPreview() {
    KptTheme {
        AuthFormSection(
            state = LoginSignupState(
                mode = AuthMode.Login,
                emailPhone = "grace.wanjiku@example.com",
                isBiometricAvailable = true,
            ),
            onAction = {},
            isLoading = false,
        )
    }
}

@Preview
@Composable
private fun AuthFormSectionSignupErrorPreview() {
    KptTheme {
        AuthFormSection(
            state = LoginSignupState(
                mode = AuthMode.Signup,
                name = "Amina Otieno",
                emailPhone = "+254798765432",
                validationErrors = mapOf("password" to "error_password_weak"),
            ),
            onAction = {},
            isLoading = false,
        )
    }
}

@Preview
@Composable
private fun AuthFormSectionSignupPasswordChipsPreview() {
    KptTheme {
        AuthFormSection(
            state = LoginSignupState(
                mode = AuthMode.Signup,
                name = "Amina Otieno",
                emailPhone = "+254798765432",
                password = "Savings26",
                // Partially-satisfied verdict — length + no-repeats still failing.
                passwordRequirements = evaluatePasswordRequirements("Savings26"),
            ),
            onAction = {},
            isLoading = false,
        )
    }
}

@Preview
@Composable
private fun ZeroGroupsSectionPreview() {
    KptTheme {
        ZeroGroupsSection(onAction = {})
    }
}

@Preview
@Composable
private fun DemoExploreConfirmDialogPreview() {
    KptTheme {
        DemoExploreConfirmDialog(isSeeding = false, onConfirm = {}, onCancel = {})
    }
}

@Preview
@Composable
private fun DemoExploreConfirmDialogSeedingPreview() {
    KptTheme {
        DemoExploreConfirmDialog(isSeeding = true, onConfirm = {}, onCancel = {})
    }
}
