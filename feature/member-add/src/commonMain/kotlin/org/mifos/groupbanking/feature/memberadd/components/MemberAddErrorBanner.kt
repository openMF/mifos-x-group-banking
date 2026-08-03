/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.memberadd.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kpt.core.base.designsystem.component.KptButton
import kpt.core.designsystem.theme.spacing

/**
 * Inline submission-error alert rendered on `MemberAddScreenState.Error`
 * (`MemberAddState.error != null`) — title + message + an embedded "Retry" button that
 * re-dispatches `OnSubmit` (`error.html`'s `.alert.alert-danger` block; the primary Save button
 * above stays labelled "Save Member" and is ALSO still wired to `OnSubmit`, so this is a second,
 * equally-real affordance, not the only path to retry). See API.md#screen.
 */
@Composable
fun MemberAddErrorBanner(
    title: String,
    message: String,
    iconContentDescription: String,
    retryLabel: String,
    retryContentDescription: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
    retryButtonModifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(sp.md),
    ) {
        Row(modifier = Modifier.semantics { contentDescription = "$title. $message" }) {
            Icon(
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = iconContentDescription,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(end = sp.sm),
            )
            Column(verticalArrangement = Arrangement.spacedBy(sp.xs)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
        Spacer(Modifier.height(sp.sm))
        KptButton(
            onClick = onRetryClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
            modifier = retryButtonModifier
                .fillMaxWidth()
                .heightIn(min = sp.touchTargetMin)
                .semantics { contentDescription = retryContentDescription },
        ) {
            Text(retryLabel)
        }
    }
}
