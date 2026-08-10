/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.settings.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kpt.core.designsystem.theme.spacing

/**
 * Single row of a `type: radio-group` component (`ui.yaml#components.language_selector_group` /
 * `.theme_selector_group`) — a label with a trailing [RadioButton], the whole row selectable
 * (`Role.RadioButton` semantics, ≥48dp touch target per `style.min_touch_target`). `ui.yaml`
 * declares a per-item `icon` field for the language rows (`flag_gb`/`flag_ke`/`flag_fr`/`flag_in`)
 * — no matching flag assets exist in `kpt.core.designsystem.icon.AppIcons` (a documented asset
 * gap, not a fabricated icon; see `SettingsLanguageSection` KDoc), so [icon] here stays `null` for
 * every current caller. See API.md#screen.
 */
@Composable
fun SettingsRadioOptionRow(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val sp = MaterialTheme.spacing
    val rowModifier = modifier
        .fillMaxWidth()
        .heightIn(min = sp.touchTargetMin)
        .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
        .let { base ->
            if (contentDescription != null) base.semantics { this.contentDescription = contentDescription } else base
        }
        .padding(horizontal = sp.lg, vertical = sp.sm)

    Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        RadioButton(selected = selected, onClick = null)
    }
}
