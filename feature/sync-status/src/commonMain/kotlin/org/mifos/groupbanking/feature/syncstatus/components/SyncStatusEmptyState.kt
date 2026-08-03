/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.syncstatus.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kpt.core.designsystem.theme.spacing
import org.mifos.groupbanking.feature.syncstatus.SyncStatusTestTags

/**
 * `ui.yaml#components.all_synced_empty` — visible only when `pendingCount == 0 && failedCount ==
 * 0`. Distinct from the (already-green) `overall_status_card` SYNCED state — this is the
 * below-the-fold confirmation that there is nothing further to review.
 */
@Composable
fun SyncStatusEmptyState(
    iconContentDescription: String,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    Column(
        modifier = modifier.fillMaxWidth().padding(sp.lg).testTag(SyncStatusTestTags.ALL_SYNCED_EMPTY),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.CloudDone,
            contentDescription = iconContentDescription,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp),
        )
        Text(text = title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = sp.md))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = sp.xs),
        )
    }
}
