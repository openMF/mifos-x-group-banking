/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kpt.core.model.user.DarkThemeConfig
import org.jetbrains.compose.resources.stringResource
import org.mifos.groupbanking.feature.settings.generated.resources.Res
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_a11y_theme_dark
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_a11y_theme_light
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_a11y_theme_system
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_theme_dark
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_theme_light
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_theme_system

/**
 * The 3 theme rows backing `ui.yaml#components.theme_selector_group` — Light / Dark / System
 * Default. `ui.yaml`'s `SYSTEM` id maps 1:1 onto [DarkThemeConfig.FOLLOW_SYSTEM] (see
 * `SettingsState` class KDoc "type-reuse divergence" note — the real, already-shipped
 * [DarkThemeConfig] is consumed directly rather than an idea-layer `AppTheme` shorthand). See
 * API.md#screen.
 */
@Composable
fun SettingsThemeSection(
    selected: DarkThemeConfig,
    onSelected: (DarkThemeConfig) -> Unit,
    modifier: Modifier = Modifier,
    groupContentDescription: String? = null,
    testTagFor: (DarkThemeConfig) -> String? = { null },
) {
    val themes = listOf(DarkThemeConfig.LIGHT, DarkThemeConfig.DARK, DarkThemeConfig.FOLLOW_SYSTEM)

    val groupModifier = modifier
        .fillMaxWidth()
        .selectableGroup()
        .let { base ->
            if (groupContentDescription != null) base.semantics { contentDescription = groupContentDescription } else base
        }

    Column(modifier = groupModifier) {
        themes.forEach { theme ->
            SettingsRadioOptionRow(
                selected = selected == theme,
                label = settingsThemeLabel(theme),
                onClick = { onSelected(theme) },
                contentDescription = settingsThemeContentDescription(theme),
                modifier = testTagFor(theme)?.let { tag -> Modifier.testTag(tag) } ?: Modifier,
            )
        }
    }
}

@Composable
private fun settingsThemeLabel(theme: DarkThemeConfig): String = when (theme) {
    DarkThemeConfig.LIGHT -> stringResource(Res.string.screens_settings_theme_light)
    DarkThemeConfig.DARK -> stringResource(Res.string.screens_settings_theme_dark)
    DarkThemeConfig.FOLLOW_SYSTEM -> stringResource(Res.string.screens_settings_theme_system)
}

@Composable
private fun settingsThemeContentDescription(theme: DarkThemeConfig): String = when (theme) {
    DarkThemeConfig.LIGHT -> stringResource(Res.string.screens_settings_a11y_theme_light)
    DarkThemeConfig.DARK -> stringResource(Res.string.screens_settings_a11y_theme_dark)
    DarkThemeConfig.FOLLOW_SYSTEM -> stringResource(Res.string.screens_settings_a11y_theme_system)
}
