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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Outlined, error-colored, full-width button — backs `ui.yaml#components.logout_button`
 * (`variant: outlined`, `style: { border_color: error, text_color: error, min_height: 56dp,
 * full_width: true, loading_when: isLoggingOut }`). While [isLoggingOut] the button disables
 * itself and swaps its label for a [CircularProgressIndicator] (mirrors
 * `SettingsLogoutDialogContent`'s identical in-button spinner convention in the sibling
 * `settings-logout-dialog` feature). See API.md#screen.
 */
@Composable
fun SettingsLogoutButton(
    label: String,
    contentDescription: String,
    loggingOutContentDescription: String,
    isLoggingOut: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    val buttonModifier = modifier
        .fillMaxWidth()
        .heightIn(min = 56.dp)
        .semantics { this.contentDescription = if (isLoggingOut) loggingOutContentDescription else contentDescription }
        .let { base -> if (testTag != null) base.testTag(testTag) else base }

    OutlinedButton(
        onClick = onClick,
        enabled = !isLoggingOut,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
        modifier = buttonModifier,
    ) {
        if (isLoggingOut) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = MaterialTheme.colorScheme.error,
                strokeWidth = 2.dp,
            )
        } else {
            Text(text = label)
        }
    }
}
