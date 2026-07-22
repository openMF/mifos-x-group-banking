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

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kpt.core.designsystem.theme.spacing

/**
 * Small labelled section divider — backs every `type: text` section-header component in
 * `ui.yaml#components` (`language_section_header`, `appearance_section_header`,
 * `security_section_header`, `notifications_section_header`, `about_section_header`), each
 * declaring the identical `style: { text_style: labelLarge, color: primary, padding: "16dp 16dp
 * 4dp" }`. See API.md#screen.
 */
@Composable
fun SettingsSectionHeader(text: String, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = sp.lg, top = sp.lg, end = sp.lg, bottom = 4.dp),
    )
}
