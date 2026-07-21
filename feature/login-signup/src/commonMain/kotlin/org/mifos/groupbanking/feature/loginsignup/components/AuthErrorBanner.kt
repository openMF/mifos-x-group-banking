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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.mifos.groupbanking.feature.loginsignup.LoginSignupTestTags
import org.mifos.groupbanking.feature.loginsignup.generated.resources.Res
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_error_icon_cd

/**
 * Inline banner surfaced when `LoginSignupState.error != null`. Mirrors
 * `ui.yaml#components.error_banner`. See API.md#screen.
 */
@Composable
fun AuthErrorBanner(message: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.fillMaxWidth().testTag(LoginSignupTestTags.ERROR_BANNER),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = stringResource(Res.string.screens_login_signup_error_icon_cd),
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(text = message, color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}
