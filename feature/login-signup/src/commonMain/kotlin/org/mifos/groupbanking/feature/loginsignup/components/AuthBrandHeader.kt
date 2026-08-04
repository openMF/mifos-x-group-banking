/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.loginsignup.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kpt.core.designsystem.theme.spacing
import org.jetbrains.compose.resources.stringResource
import org.mifos.groupbanking.feature.loginsignup.generated.resources.Res
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_tagline
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_wordmark_mifos
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_wordmark_save

/**
 * MifosSave brand lockup shown above the login/signup card — the [MifosSaveBrandMark] ring
 * mark, the two-tone serif "MifosSave" wordmark ("Mifos" in onSurface, "Save" in primary), and
 * the tagline. Colours resolve from [MaterialTheme.colorScheme] so the mark + wordmark pick up the
 * brand recolour automatically. Mirrors `ui.yaml#components.auth_header_logo`. See API.md#screen.
 */
@Composable
fun AuthBrandHeader(modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        MifosSaveBrandMark(markSize = 56.dp)
        Spacer(Modifier.height(sp.md))
        Row {
            Text(
                text = stringResource(Res.string.screens_login_signup_wordmark_mifos),
                // Serif wordmark is a deliberate brand contrast against the app's Outfit sans body
                // font; size comes from the design-system type scale, family stays serif.
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(Res.string.screens_login_signup_wordmark_save),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(sp.xs))
        Text(
            text = stringResource(Res.string.screens_login_signup_tagline),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
