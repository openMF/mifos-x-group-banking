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

import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import org.mifos.groupbanking.feature.syncstatus.SyncStatusTestTags

/**
 * `ui.yaml#components.conflict_chip` — visible only when `conflictCount > 0`. Non-interactive
 * info chip (Fineract `409` conflicts require manual review outside this screen — `ui.yaml`
 * declares no `on_click` for this component, so [AssistChip.onClick] is a documented no-op per
 * RULE-IMPL-DEAD-CLICKABLE-001 Rule 1, mirrors `RotationMetricCard`'s identical
 * disabled-info-chip convention).
 */
@Composable
fun SyncStatusConflictChip(
    label: String,
    accessibilityLabel: String,
    modifier: Modifier = Modifier,
) {
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(text = label, style = MaterialTheme.typography.labelMedium) },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
            disabledLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
        border = null,
        modifier = modifier
            .testTag(SyncStatusTestTags.CONFLICT_CHIP)
            .semantics { contentDescription = accessibilityLabel },
    )
}
