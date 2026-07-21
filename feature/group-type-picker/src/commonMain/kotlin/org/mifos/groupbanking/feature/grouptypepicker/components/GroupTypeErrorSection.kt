/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.grouptypepicker.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kpt.core.designsystem.theme.spacing
import org.jetbrains.compose.resources.stringResource
import org.mifos.groupbanking.feature.grouptypepicker.GroupTypePickerTestTags
import org.mifos.groupbanking.feature.grouptypepicker.generated.resources.Res
import org.mifos.groupbanking.feature.grouptypepicker.generated.resources.screens_group_type_picker_action_retry
import org.mifos.groupbanking.feature.grouptypepicker.generated.resources.screens_group_type_picker_error_icon_cd

/**
 * Full-screen error surface — `GroupTypePickerScreenState.Error`. Mirrors
 * `ui.yaml#components.error_banner` + `preview/error.html` (illustration, message, full-width
 * Retry CTA). [message] is resolved by the Screen layer from `GroupTypePickerError.messageKey`.
 * See API.md#screen.
 */
@Composable
fun GroupTypeErrorSection(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val errorIconCd = stringResource(Res.string.screens_group_type_picker_error_icon_cd)
    val retryLabel = stringResource(Res.string.screens_group_type_picker_action_retry)

    Column(
        modifier = modifier.fillMaxSize().padding(sp.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag(GroupTypePickerTestTags.ERROR_BANNER),
        ) {
            Column(
                modifier = Modifier.padding(sp.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(sp.sm),
            ) {
                Icon(
                    imageVector = Icons.Filled.ErrorOutline,
                    contentDescription = errorIconCd,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = sp.touchTargetMin)
                .padding(top = sp.lg)
                .testTag(GroupTypePickerTestTags.RETRY_BUTTON),
        ) {
            Icon(imageVector = Icons.Filled.Refresh, contentDescription = null, modifier = Modifier)
            Text(text = retryLabel, modifier = Modifier.padding(start = sp.xs))
        }
    }
}
