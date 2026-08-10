/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.settings

import androidx.compose.runtime.Composable
import kpt.core.designsystem.theme.KptTheme
import kpt.core.model.user.DarkThemeConfig
import kpt.core.model.user.LanguageConfig
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider

/**
 * `@Preview` gallery for `SettingsScreen.kt`. See API.md#preview. Data source:
 * `ui.yaml#states.content.demo_data` (`selectedLanguage: ENGLISH`, `selectedTheme: SYSTEM` ->
 * [DarkThemeConfig.FOLLOW_SYSTEM], `isBiometricAvailable: true`, `isNotificationsEnabled: true`,
 * `appVersion: "1.0.0"`, `buildNumber: "42"`), verbatim field values (no placeholder literals,
 * RULE-PREVIEW-7).
 */
private val demoSettingsState = SettingsState(
    selectedLanguage = LanguageConfig.ENGLISH,
    selectedTheme = DarkThemeConfig.FOLLOW_SYSTEM,
    isBiometricEnabled = false,
    isNotificationsEnabled = true,
    isBiometricAvailable = true,
    appVersion = "1.0.0",
    buildNumber = "42",
)

private class SettingsScreenPreviewProvider : PreviewParameterProvider<SettingsState> {
    override val values: Sequence<SettingsState> = sequenceOf(
        // content -- ui.yaml#states.content.demo_data, biometric hardware available
        demoSettingsState,
        // changing_pin -- ui.yaml#states.changing_pin, dialog overlaid on settings
        demoSettingsState.copy(isChangingPin = true),
        // content -- biometric hardware NOT available (switch disabled + unavailable sublabel)
        demoSettingsState.copy(isBiometricAvailable = false, isBiometricEnabled = false),
        // changing_pin -- inline PIN-change error (ui.yaml#state_model.errors.PinChangeFailed)
        demoSettingsState.copy(isChangingPin = true, pinChangeError = "error_pin_change"),
    )
}

@Preview
@Composable
private fun SettingsScreenPreview(
    @PreviewParameter(SettingsScreenPreviewProvider::class)
    state: SettingsState,
) {
    KptTheme {
        SettingsContent(state = state, onAction = {}, onNavigateBack = {})
    }
}

@Preview
@Composable
private fun SettingsMainSectionPreview() {
    KptTheme {
        SettingsMainSection(state = demoSettingsState, onAction = {})
    }
}

@Preview
@Composable
private fun SettingsLoggingOutSectionPreview() {
    KptTheme {
        SettingsLoggingOutSection(state = demoSettingsState.copy(isLoggingOut = true), onAction = {})
    }
}

@Preview
@Composable
private fun SettingsChangePinDialogHostPreview() {
    KptTheme {
        SettingsChangePinDialogHost(
            state = demoSettingsState.copy(isChangingPin = true, pinChangeError = "error_pin_change"),
            onAction = {},
        )
    }
}
