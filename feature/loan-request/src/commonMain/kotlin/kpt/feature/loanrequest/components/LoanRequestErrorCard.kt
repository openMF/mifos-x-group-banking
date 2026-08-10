/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.loanrequest.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kpt.core.designsystem.theme.spacing

/**
 * `ui.yaml#components.error_snackbar` (`type: card`, `background: errorContainer`,
 * `visible_when: "submitError != null && !isSubmitting"`). Rendered as a persistent inline card
 * (not a transient `Snackbar`) per `ui.yaml`'s `type: card` declaration -- the `action_label`
 * "Retry" dispatches `OnRetry`. See API.md#screen.
 */
@Composable
fun LoanRequestErrorCard(
    message: String,
    iconContentDescription: String,
    retryLabel: String,
    retryContentDescription: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
    retryTestTag: String = "",
) {
    val sp = MaterialTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(sp.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f).semantics { contentDescription = message },
        ) {
            Icon(
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = iconContentDescription,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(end = sp.sm),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
        val retryModifier = if (retryTestTag.isNotEmpty()) {
            Modifier.heightIn(min = sp.touchTargetMin)
                .testTag(retryTestTag)
                .semantics { contentDescription = retryContentDescription }
        } else {
            Modifier.heightIn(min = sp.touchTargetMin)
                .semantics { contentDescription = retryContentDescription }
        }
        TextButton(onClick = onRetryClick, modifier = retryModifier) {
            Text(retryLabel, color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}
