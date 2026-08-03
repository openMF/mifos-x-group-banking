/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.joinwithcode.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kpt.core.designsystem.theme.spacing
import org.jetbrains.compose.resources.stringResource
import org.mifos.groupbanking.feature.joinwithcode.JoinError
import org.mifos.groupbanking.feature.joinwithcode.JoinWithCodeTestTags
import org.mifos.groupbanking.feature.joinwithcode.generated.resources.Res
import org.mifos.groupbanking.feature.joinwithcode.generated.resources.screens_join_with_code_action_retry
import org.mifos.groupbanking.feature.joinwithcode.generated.resources.screens_join_with_code_error_icon_cd
import org.mifos.groupbanking.feature.joinwithcode.generated.resources.screens_join_with_code_hint_expired_message

/**
 * Inline error banner — `ui.yaml#components.error_card`. [message] is resolved by the caller
 * from `JoinError.messageKey`; the "ask your organiser for a new code" hint renders only for
 * [JoinError.ExpiredCode] (`ui.yaml#components.error_hint_expired`), and the Retry action only
 * when `error.retry == true` (`ui.yaml#components.retry_button.visible`). See API.md#screen.
 */
@Composable
fun JoinCodeErrorBanner(error: JoinError, message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val iconCd = stringResource(Res.string.screens_join_with_code_error_icon_cd)
    val retryLabel = stringResource(Res.string.screens_join_with_code_action_retry)
    val hintExpired = stringResource(Res.string.screens_join_with_code_hint_expired_message)

    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth().testTag(JoinWithCodeTestTags.ERROR_CARD),
    ) {
        Column(modifier = Modifier.padding(sp.md), verticalArrangement = Arrangement.spacedBy(sp.sm)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(sp.sm),
            ) {
                Icon(
                    imageVector = Icons.Filled.ErrorOutline,
                    contentDescription = iconCd,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(text = message, color = MaterialTheme.colorScheme.onErrorContainer)
            }

            if (error is JoinError.ExpiredCode) {
                Text(
                    text = hintExpired,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }

            if (error.retry) {
                TextButton(
                    onClick = onRetry,
                    modifier = Modifier
                        .heightIn(min = sp.touchTargetMin)
                        .testTag(JoinWithCodeTestTags.RETRY_BUTTON),
                ) {
                    Text(text = retryLabel, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
