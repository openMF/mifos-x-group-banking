/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.settings

import kpt.core.model.user.DarkThemeConfig
import kpt.core.model.user.LanguageConfig

/**
 * Append-only test-tag registry for the `settings` feature (RULE-KMP-COMPOSE-UITEST-001 CU-5 —
 * names are stable across regenerations; only append new entries, never rename or remove).
 * Consumed by Compose UI tests under `feature/settings/src/commonTest/` and by the Maestro flow
 * generator (`core/scripts/maestro-flow-gen.ts`), which emits `tapOn: { id: "<test_tag>" }`
 * selectors from these constants. Constant names are derived 1:1 from `ui.yaml#components[].id`.
 *
 * See API.md#tags.
 */
object SettingsTestTags {

    /** Root [kpt.core.ui.scaffold.KptScaffold] surface — always rendered regardless of screenState. */
    const val SCREEN: String = "settings_screen"

    /** `ui.yaml#components.top_bar` — always rendered. */
    const val TOP_BAR: String = "settings_top_bar"

    /** Scrollable content column — `SettingsScreenState.Content` / `.ChangingPin`. */
    const val CONTENT: String = "settings_content"

    /** `ui.yaml#components.language_selector_group` — always rendered in Content/ChangingPin. */
    const val LANGUAGE_RADIO_GROUP: String = "settings_language_radio_group"

    const val LANGUAGE_OPTION_ENGLISH: String = "settings_language_option_english"
    const val LANGUAGE_OPTION_SWAHILI: String = "settings_language_option_swahili"
    const val LANGUAGE_OPTION_FRENCH: String = "settings_language_option_french"
    const val LANGUAGE_OPTION_HINDI: String = "settings_language_option_hindi"

    /** `ui.yaml#components.theme_selector_group` — always rendered in Content/ChangingPin. */
    const val THEME_RADIO_GROUP: String = "settings_theme_radio_group"

    const val THEME_OPTION_LIGHT: String = "settings_theme_option_light"
    const val THEME_OPTION_DARK: String = "settings_theme_option_dark"
    const val THEME_OPTION_SYSTEM: String = "settings_theme_option_system"

    /** `ui.yaml#components.biometric_toggle_row.trailing` — [androidx.compose.material3.Switch]. */
    const val BIOMETRIC_SWITCH: String = "settings_biometric_switch"

    /** `ui.yaml#components.change_pin_row` — opens [SettingsAction.OnChangePinTapped]. */
    const val CHANGE_PIN_ROW: String = "settings_change_pin_row"

    /** `ui.yaml#components.notifications_toggle_row.trailing` — [androidx.compose.material3.Switch]. */
    const val NOTIFICATIONS_SWITCH: String = "settings_notifications_switch"

    /** `ui.yaml#components.app_version_row` — static, always rendered. */
    const val APP_VERSION_ROW: String = "settings_app_version_row"

    /** `ui.yaml#components.logout_button` — rendered in every screenState. */
    const val LOGOUT_BUTTON: String = "settings_logout_button"

    /** `ui.yaml#components.change_pin_dialog` — visible only while `SettingsScreenState.ChangingPin`. */
    const val CHANGE_PIN_DIALOG: String = "settings_change_pin_dialog"

    /** `ui.yaml#components.change_pin_dialog.content.current_pin_field`. */
    const val CURRENT_PIN_FIELD: String = "settings_current_pin_field"

    /** `ui.yaml#components.change_pin_dialog.content.new_pin_field`. */
    const val NEW_PIN_FIELD: String = "settings_new_pin_field"

    /** `ui.yaml#components.change_pin_dialog.content.pin_change_error_text` — `visible_when: pinChangeError != null`. */
    const val PIN_ERROR_TEXT: String = "settings_pin_error_text"

    /** `ui.yaml#components.change_pin_dialog.actions.cancel`. */
    const val DIALOG_CANCEL_BUTTON: String = "settings_dialog_cancel_button"

    /** `ui.yaml#components.change_pin_dialog.actions.confirm`. */
    const val DIALOG_CONFIRM_BUTTON: String = "settings_dialog_confirm_button"

    /** Resolves the stable radio-row test tag for a given [LanguageConfig] (see [LANGUAGE_OPTION_ENGLISH] et al.). */
    fun languageOptionTag(language: LanguageConfig): String? = when (language) {
        LanguageConfig.ENGLISH -> LANGUAGE_OPTION_ENGLISH
        LanguageConfig.SWAHILI -> LANGUAGE_OPTION_SWAHILI
        LanguageConfig.FRENCH -> LANGUAGE_OPTION_FRENCH
        LanguageConfig.HINDI -> LANGUAGE_OPTION_HINDI
        else -> null
    }

    /** Resolves the stable radio-row test tag for a given [DarkThemeConfig] (see [THEME_OPTION_LIGHT] et al.). */
    fun themeOptionTag(theme: DarkThemeConfig): String = when (theme) {
        DarkThemeConfig.LIGHT -> THEME_OPTION_LIGHT
        DarkThemeConfig.DARK -> THEME_OPTION_DARK
        DarkThemeConfig.FOLLOW_SYSTEM -> THEME_OPTION_SYSTEM
    }
}
