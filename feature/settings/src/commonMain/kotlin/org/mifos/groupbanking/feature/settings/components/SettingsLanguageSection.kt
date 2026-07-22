/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
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
import kpt.core.model.user.LanguageConfig
import org.jetbrains.compose.resources.stringResource
import org.mifos.groupbanking.feature.settings.generated.resources.Res
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_a11y_lang_english
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_a11y_lang_french
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_a11y_lang_hindi
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_a11y_lang_swahili
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_lang_english
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_lang_french
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_lang_hindi
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_lang_swahili

/**
 * The 4 language rows backing `ui.yaml#components.language_selector_group` — English / Kiswahili
 * / Français / हिन्दी (`FR-010`). `ui.yaml` declares 40+ [LanguageConfig] entries exist in
 * `core/model`, but only these 4 are offered here, per the ui.yaml `items` list. Per-item `icon`
 * fields (`flag_gb`/`flag_ke`/`flag_fr`/`flag_in`) are a documented asset gap — no flag icons exist
 * in `kpt.core.designsystem.icon.AppIcons`, so [SettingsRadioOptionRow] renders label-only rows
 * rather than fabricating icon assets. See API.md#screen.
 */
@Composable
fun SettingsLanguageSection(
    selected: LanguageConfig,
    onSelected: (LanguageConfig) -> Unit,
    modifier: Modifier = Modifier,
    groupContentDescription: String? = null,
    testTagFor: (LanguageConfig) -> String? = { null },
) {
    val languages = listOf(LanguageConfig.ENGLISH, LanguageConfig.SWAHILI, LanguageConfig.FRENCH, LanguageConfig.HINDI)

    val groupModifier = modifier
        .fillMaxWidth()
        .selectableGroup()
        .let { base ->
            if (groupContentDescription != null) base.semantics { contentDescription = groupContentDescription } else base
        }

    Column(modifier = groupModifier) {
        languages.forEach { language ->
            SettingsRadioOptionRow(
                selected = selected == language,
                label = settingsLanguageLabel(language),
                onClick = { onSelected(language) },
                contentDescription = settingsLanguageContentDescription(language),
                modifier = testTagFor(language)?.let { tag -> Modifier.testTag(tag) } ?: Modifier,
            )
        }
    }
}

@Composable
private fun settingsLanguageLabel(language: LanguageConfig): String = when (language) {
    LanguageConfig.ENGLISH -> stringResource(Res.string.screens_settings_lang_english)
    LanguageConfig.SWAHILI -> stringResource(Res.string.screens_settings_lang_swahili)
    LanguageConfig.FRENCH -> stringResource(Res.string.screens_settings_lang_french)
    LanguageConfig.HINDI -> stringResource(Res.string.screens_settings_lang_hindi)
    else -> language.text
}

@Composable
private fun settingsLanguageContentDescription(language: LanguageConfig): String = when (language) {
    LanguageConfig.ENGLISH -> stringResource(Res.string.screens_settings_a11y_lang_english)
    LanguageConfig.SWAHILI -> stringResource(Res.string.screens_settings_a11y_lang_swahili)
    LanguageConfig.FRENCH -> stringResource(Res.string.screens_settings_a11y_lang_french)
    LanguageConfig.HINDI -> stringResource(Res.string.screens_settings_a11y_lang_hindi)
    else -> language.text
}
