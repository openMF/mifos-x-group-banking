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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kpt.core.base.designsystem.component.AppCard
import kpt.core.designsystem.theme.spacing
import org.mifos.groupbanking.core.model.EntityType
import org.mifos.groupbanking.feature.syncstatus.SyncStatusTestTags

/**
 * `ui.yaml#components.pending_breakdown_card` — visible only when `pendingCount > 0`. One row per
 * [EntityType] present in [pendingByType] — an entity with zero pending rows is simply absent
 * from the map (per `SyncClassifier.pendingByType` KDoc), never rendered as a `0`-valued row.
 */
@Composable
fun SyncStatusPendingBreakdownCard(
    titleText: String,
    pendingByType: Map<EntityType, Int>,
    entityCountAccessibilityLabel: String,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    AppCard(modifier = modifier.fillMaxWidth().testTag(SyncStatusTestTags.PENDING_BREAKDOWN_CARD)) {
        Column {
            Text(text = titleText, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            pendingByType.forEach { (entityType, count) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = sp.sm)
                        .testTag(SyncStatusTestTags.pendingRowTag(entityType)),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = entityType.humanizedLabel(), style = MaterialTheme.typography.bodyMedium)
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.semantics { contentDescription = entityCountAccessibilityLabel },
                    ) {
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = sp.sm, vertical = sp.xs),
                        )
                    }
                }
            }
        }
    }
}
