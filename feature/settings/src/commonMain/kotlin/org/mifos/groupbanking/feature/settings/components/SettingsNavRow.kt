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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kpt.core.designsystem.icon.AppIcons
import kpt.core.designsystem.theme.spacing

/**
 * Clickable `type: list-item` row with a headline, supporting text, and a trailing chevron —
 * backs `ui.yaml#components.change_pin_row` (`style: { min_height: 56dp }`,
 * `trailing: { type: icon, icon: chevron_right_24_regular }`). See API.md#screen.
 */
@Composable
fun SettingsNavRow(
    headline: String,
    supporting: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    chevronContentDescription: String? = null,
) {
    val sp = MaterialTheme.spacing
    val rowModifier = modifier
        .fillMaxWidth()
        .heightIn(min = 56.dp)
        .clickable(onClick = onClick)
        .let { base ->
            if (contentDescription != null) base.semantics { this.contentDescription = contentDescription } else base
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
        Icon(
            imageVector = AppIcons.ChevronRight,
            contentDescription = chevronContentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(sp.xl),
        )
    }
}
