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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kpt.core.designsystem.theme.spacing

/**
 * `type: list-item` row with a headline, supporting text, and a trailing [Switch] — backs
 * `ui.yaml#components.biometric_toggle_row` / `.notifications_toggle_row` (both declare
 * `style: { min_height: 56dp }`). See API.md#screen.
 */
@Composable
fun SettingsToggleRow(
    headline: String,
    supporting: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    rowContentDescription: String? = null,
    switchContentDescription: String? = null,
    switchTestTag: String? = null,
) {
    val sp = MaterialTheme.spacing
    val rowModifier = modifier
        .fillMaxWidth()
        .heightIn(min = 56.dp)
        .let { base ->
            if (rowContentDescription != null) base.semantics { contentDescription = rowContentDescription } else base
        }
        .padding(horizontal = sp.lg, vertical = sp.sm)

    Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = headline, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        val switchModifier = Modifier
            .let { base ->
                if (switchContentDescription != null) base.semantics { contentDescription = switchContentDescription } else base
            }
            .let { base -> if (switchTestTag != null) base.testTag(switchTestTag) else base }

        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled, modifier = switchModifier)
    }
}
