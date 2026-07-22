/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.model.user

import kotlinx.serialization.Serializable

@Serializable
data class UserData(
    val activeUserId: String,
    val themeBrand: ThemeBrand,
    val darkThemeConfig: DarkThemeConfig,
    val useDynamicColor: Boolean,
    val appLanguage: LanguageConfig,
    val showOnboarding: Boolean,
    val firstTimeUser: Boolean,
    val isAuthenticated: Boolean,
    val isUnlocked: Boolean,
    val passcode: String,
    val enableScreenCapture: Boolean,
    val isPasscodeEnabled: Boolean,
    val isBiometricsEnabled: Boolean,
    // Additive field for the `settings` feature's push-notifications toggle
    // (idea-layer/screens/settings/ui.yaml#state_model.SettingsViewModel.state.isNotificationsEnabled).
    // Default `true` mirrors the ui.yaml-declared field default. Kept with a Kotlin parameter
    // default so the sole existing construction call site (`DEFAULT` below, all named args) keeps
    // compiling unchanged — purely additive, no existing signature broken.
    val enableNotifications: Boolean = true,
) {
    companion object {
        val DEFAULT = UserData(
            activeUserId = "",
            passcode = "1234",
            themeBrand = ThemeBrand.DEFAULT,
            darkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM,
            useDynamicColor = false,
            appLanguage = LanguageConfig.DEFAULT,
            isAuthenticated = true,
            isUnlocked = true,
            isPasscodeEnabled = false,
            isBiometricsEnabled = false,
            showOnboarding = false,
            firstTimeUser = false,
            enableScreenCapture = false,
            enableNotifications = true,
        )
    }
}
