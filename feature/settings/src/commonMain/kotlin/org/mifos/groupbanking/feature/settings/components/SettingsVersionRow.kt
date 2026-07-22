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

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kpt.core.designsystem.theme.spacing

/**
 * Static `type: list-item` row — backs `ui.yaml#components.app_version_row`
 * (`style: { min_height: 48dp }`). [versionText] is `"{{appVersion}} ({{buildNumber}})"`
 * (`ui.yaml` marks it `i18n: skip` — not a translatable string, the raw version/build values
 * flow straight from [org.mifos.groupbanking.feature.settings.SettingsState]). See API.md#screen.
 */
@Composable
fun SettingsVersionRow(headline: String, versionText: String, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(horizontal = sp.lg, vertical = sp.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = headline, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(text = versionText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
