/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.syncstatus.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kpt.core.base.designsystem.component.KptButton
import kpt.core.designsystem.theme.spacing
import org.mifos.groupbanking.feature.syncstatus.SyncStatusTestTags

/**
 * `ui.yaml#components.sync_now_button` — full-width filled button (`min_touch_target: 56dp`),
 * `enabled_when: "isOnline && !isSyncing"`, `loading_when: isSyncing` (inline spinner replaces the
 * sync icon), and `disabled_label_when: "!isOnline"` (swaps [label] for the offline copy — the
 * caller resolves which string to pass via [label]; this composable only renders whichever one it
 * is given, dispatches `OnSyncNow` via [onClick]).
 */
@Composable
fun SyncNowButton(
    label: String,
    contentDescription: String,
    enabled: Boolean,
    isSyncing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    KptButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag(SyncStatusTestTags.SYNC_NOW_BUTTON)
            .semantics { this.contentDescription = contentDescription },
    ) {
        if (isSyncing) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
            )
        } else {
            Icon(imageVector = Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(20.dp))
        }
        Text(text = label, modifier = Modifier.padding(start = sp.xs))
    }
}
