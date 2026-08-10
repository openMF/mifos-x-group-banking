/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.loginsignup.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kpt.core.designsystem.theme.spacing
import kpt.feature.loginsignup.LoginSignupTestTags
import kpt.feature.loginsignup.generated.resources.Res
import kpt.feature.loginsignup.generated.resources.screens_login_signup_demo_dialog_body
import kpt.feature.loginsignup.generated.resources.screens_login_signup_demo_dialog_cancel
import kpt.feature.loginsignup.generated.resources.screens_login_signup_demo_dialog_cancel_cd
import kpt.feature.loginsignup.generated.resources.screens_login_signup_demo_dialog_cd
import kpt.feature.loginsignup.generated.resources.screens_login_signup_demo_dialog_confirm
import kpt.feature.loginsignup.generated.resources.screens_login_signup_demo_dialog_confirm_cd
import kpt.feature.loginsignup.generated.resources.screens_login_signup_demo_dialog_icon_cd
import kpt.feature.loginsignup.generated.resources.screens_login_signup_demo_dialog_legal_note
import kpt.feature.loginsignup.generated.resources.screens_login_signup_demo_dialog_title
import org.jetbrains.compose.resources.stringResource

/**
 * Demo Explore confirm dialog (`ui.yaml#components.demo_confirm_dialog`, F1/B1/G1). Armed by the
 * `demo_explore_button` (`OnDemoExplore` → `showDemoDialog = true`); the affordance ARMS the
 * dialog, it never seeds directly. Cancel dispatches `OnDemoCancel`; Continue dispatches
 * `OnDemoConfirm`, which flips `isSeedingDemo` — while seeding the dialog stays up with a spinner
 * on the confirm button and both actions disabled (no accidental re-tap / dismiss mid-seed).
 * Styled to the MifosSave look: an amber pooled-coin mark, serif title, and an amber Continue
 * CTA. The highlighted note card carries the verbatim, legally-worded demo-login line. See
 * API.md#screen.
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
        icon = { DemoPooledCoinIcon() },
        title = {
            Text(
                text = stringResource(Res.string.screens_login_signup_demo_dialog_title),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(Res.string.screens_login_signup_demo_dialog_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(sp.md))
                // Highlighted note card (secondaryContainer bg) — verbatim legal demo-login line.
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(Res.string.screens_login_signup_demo_dialog_legal_note),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(sp.md),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isSeeding,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag(LoginSignupTestTags.DEMO_CONFIRM_BUTTON)
                    .semantics { contentDescription = confirmCd },
            ) {
                if (isSeeding) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSecondary,
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

/**
 * Amber "pooled-coin" mark for the demo dialog — concentric `secondaryContainer` + `secondary`
 * circles with a white check glyph centred, echoing the shared-savings pool of [MifosSaveBrandMark].
 */
@Composable
private fun DemoPooledCoinIcon(modifier: Modifier = Modifier) {
    val outer = MaterialTheme.colorScheme.secondaryContainer
    val inner = MaterialTheme.colorScheme.secondary
    val iconCd = stringResource(Res.string.screens_login_signup_demo_dialog_icon_cd)

    Box(contentAlignment = Alignment.Center, modifier = modifier.size(56.dp)) {
        Canvas(modifier = Modifier.size(56.dp)) {
            val radius = size.minDimension / 2f
            drawCircle(color = outer, radius = radius)
            drawCircle(color = inner, radius = radius * 0.62f)
        }
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = iconCd,
            tint = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier.size(24.dp),
        )
    }
}
