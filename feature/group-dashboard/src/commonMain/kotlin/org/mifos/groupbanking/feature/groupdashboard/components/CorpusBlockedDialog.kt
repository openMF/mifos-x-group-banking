/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.groupdashboard.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import kpt.core.designsystem.theme.spacing
import org.jetbrains.compose.resources.stringResource
import org.mifos.groupbanking.feature.groupdashboard.GroupDashboardTestTags
import org.mifos.groupbanking.feature.groupdashboard.generated.resources.Res
import org.mifos.groupbanking.feature.groupdashboard.generated.resources.screens_group_dashboard_corpus_blocked
import org.mifos.groupbanking.feature.groupdashboard.generated.resources.screens_group_dashboard_corpus_dialog_title
import org.mifos.groupbanking.feature.groupdashboard.generated.resources.screens_group_dashboard_dialog_ok

/**
 * `GroupDashboardEvent.ShowCorpusBlockedDialog` modal — surfaced by
 * `GroupDashboardViewModel.handleStartMeeting` when `isCorpusInsufficient` is true and the viewer
 * holds a management role, acknowledging the inline corpus warning alongside the (still-permitted)
 * navigation to meeting-calendar (`flow.yaml#on_start_meeting`). No further ViewModel action is
 * dispatched on dismiss — purely an acknowledgement, same convention as `group-create`'s
 * `OfflineSyncDialog`. See API.md#screen.
 */
@Composable
fun CorpusBlockedDialog(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag(GroupDashboardTestTags.CORPUS_BLOCKED_DIALOG),
        icon = { Icon(imageVector = Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(text = stringResource(Res.string.screens_group_dashboard_corpus_dialog_title)) },
        text = { Text(text = stringResource(Res.string.screens_group_dashboard_corpus_blocked)) },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .heightIn(min = MaterialTheme.spacing.touchTargetMin)
                    .testTag(GroupDashboardTestTags.CORPUS_BLOCKED_DIALOG_CONFIRM),
            ) {
                Text(text = stringResource(Res.string.screens_group_dashboard_dialog_ok))
            }
        },
    )
}
