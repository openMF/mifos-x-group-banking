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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kpt.core.designsystem.theme.spacing
import kpt.feature.loginsignup.generated.resources.Res
import kpt.feature.loginsignup.generated.resources.screens_login_signup_trust_footer
import kpt.feature.loginsignup.generated.resources.screens_login_signup_trust_footer_cd
import org.jetbrains.compose.resources.stringResource

/**
 * Small centered trust row shown below the auth card — a `primary` lock icon plus a muted
 * reassurance line. Mirrors `ui.yaml#components.trust_footer`. See API.md#screen.
 */
@Composable
fun AuthTrustFooter(modifier: Modifier = Modifier) {
    val footerCd = stringResource(Res.string.screens_login_signup_trust_footer_cd)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = footerCd,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = stringResource(Res.string.screens_login_signup_trust_footer),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
