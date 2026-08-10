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

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kpt.core.designsystem.theme.spacing

/**
 * `ui.yaml#components.success_dialog` (`visible_when: "successDialogVisible == true"`). Copy
 * differs online (`LoanRequestScreenState.SubmitSuccess`) vs offline
 * (`LoanRequestScreenState.OfflineQueued`) -- the caller resolves [title]/[body] per screenState
 * (see `LoanRequestScreen.kt`). Dismissing dispatches `OnSuccessDialogDismiss`, which the
 * ViewModel resolves to `LoanRequestEvent.NavigateToDashboardAfterSuccess`. See API.md#screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanRequestSuccessDialog(
    title: String,
    body: String,
    iconContentDescription: String,
    confirmLabel: String,
    confirmContentDescription: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmTestTag: String = "",
) {
    val sp = MaterialTheme.spacing
    AlertDialog(
        onDismissRequest = onConfirm,
        modifier = modifier,
        icon = {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = iconContentDescription,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.heightIn(min = 48.dp),
            )
        },
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier
                    .heightIn(min = sp.touchTargetMin)
                    .semantics { contentDescription = confirmContentDescription }
                    .let { base -> if (confirmTestTag.isNotEmpty()) base.testTag(confirmTestTag) else base },
            ) {
                Text(confirmLabel)
            }
        },
    )
}
