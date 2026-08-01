/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.loginsignup.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import kpt.core.base.designsystem.component.KptButton
import kpt.core.designsystem.theme.spacing
import org.jetbrains.compose.resources.stringResource
import org.mifos.groupbanking.feature.loginsignup.LoginSignupTestTags
import org.mifos.groupbanking.feature.loginsignup.generated.resources.Res
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_demo_dialog_body
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_demo_dialog_cancel
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_demo_dialog_cancel_cd
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_demo_dialog_cd
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_demo_dialog_confirm
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_demo_dialog_confirm_cd
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_demo_dialog_note
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_demo_dialog_title

/**
 * Demo Explore confirm dialog (`ui.yaml#components.demo_confirm_dialog`, F1/B1/G1). Armed by the
 * `demo_explore_button` (`OnDemoExplore` → `showDemoDialog = true`); the affordance ARMS the
 * dialog, it never seeds directly. Cancel dispatches `OnDemoCancel`; Continue dispatches
 * `OnDemoConfirm`, which flips `isSeedingDemo` — while seeding the dialog stays up with a spinner
 * on the confirm button and both actions disabled (no accidental re-tap / dismiss mid-seed).
 * See API.md#screen.
 */
@Composable
fun DemoExploreConfirmDialog(
    isSeeding: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val dialogCd = stringResource(Res.string.screens_login_signup_demo_dialog_cd)
    val confirmCd = stringResource(Res.string.screens_login_signup_demo_dialog_confirm_cd)
    val cancelCd = stringResource(Res.string.screens_login_signup_demo_dialog_cancel_cd)

    AlertDialog(
        // Ignore scrim/back dismissal while the offline fixture is hydrating.
        onDismissRequest = { if (!isSeeding) onCancel() },
        modifier = modifier
            .testTag(LoginSignupTestTags.DEMO_CONFIRM_DIALOG)
            .semantics { contentDescription = dialogCd },
        icon = {
            Icon(
                imageVector = Icons.Filled.PlayCircleOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = { Text(stringResource(Res.string.screens_login_signup_demo_dialog_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(Res.string.screens_login_signup_demo_dialog_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(sp.sm))
                Text(
                    text = stringResource(Res.string.screens_login_signup_demo_dialog_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            KptButton(
                onClick = onConfirm,
                enabled = !isSeeding,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag(LoginSignupTestTags.DEMO_CONFIRM_BUTTON)
                    .semantics { contentDescription = confirmCd },
            ) {
                if (isSeeding) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(sp.sm))
                }
                Text(stringResource(Res.string.screens_login_signup_demo_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancel,
                enabled = !isSeeding,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag(LoginSignupTestTags.DEMO_CANCEL_BUTTON)
                    .semantics { contentDescription = cancelCd },
            ) {
                Text(stringResource(Res.string.screens_login_signup_demo_dialog_cancel))
            }
        },
    )
}
