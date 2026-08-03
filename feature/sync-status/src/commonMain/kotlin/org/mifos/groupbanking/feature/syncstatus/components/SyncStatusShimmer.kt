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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kpt.core.designsystem.theme.spacing
import org.mifos.groupbanking.feature.syncstatus.SyncStatusTestTags

/**
 * `ui.yaml#components.shimmer_status` — 3 skeleton blocks (`count: 3`, `height: 80dp`,
 * `corner_radius: 12dp`) shown for `SyncStatusScreenState.Loading` while the initial 5-way
 * `combine()` fan-in resolves. Mirrors `GroupDashboardSkeletonBlock`'s identical static-block
 * convention (no animated shimmer sweep in this codebase's shipped skeleton pattern).
 */
@Composable
fun SyncStatusShimmer(accessibilityLabel: String, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(sp.lg)
            .testTag(SyncStatusTestTags.SHIMMER)
            .semantics { contentDescription = accessibilityLabel },
        verticalArrangement = Arrangement.spacedBy(sp.md),
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        }
    }
}
