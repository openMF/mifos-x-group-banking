/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.syncstatus.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kpt.core.base.designsystem.component.AppCard
import kpt.core.designsystem.theme.spacing
import kpt.core.model.SyncQueueItem
import kpt.core.model.operationTypeToEntityType
import kpt.feature.syncstatus.SyncStatusTestTags
import kpt.feature.syncstatus.generated.resources.Res
import kpt.feature.syncstatus.generated.resources.screens_sync_status_failed_item_title_format
import org.jetbrains.compose.resources.stringResource

/**
 * `ui.yaml#components.failed_operations_section` — visible only when `failedCount > 0`.
 * `errorContainer`-toned card, one row per [failedOperations] item — humanized entity (via
 * [operationTypeToEntityType] + [EntityType.humanizedLabel], the shipped bridge for
 * `ui.yaml`'s `{{item.entityType | humanize}}`) plus the raw [SyncQueueItem.operationType] (data,
 * not a hardcoded literal — matches `ui.yaml`'s own `{{item.operation}}` interpolation) plus the
 * raw [SyncQueueItem.lastError], with a per-row Retry button dispatching
 * `OnRetryOperation(item.id)`.
 */
@Composable
fun SyncStatusFailedOpsSection(
    titleText: String,
    failedOperations: List<SyncQueueItem>,
    retryLabel: String,
    retryAccessibilityLabel: String,
    onRetryClick: (itemId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    AppCard(
        containerColor = MaterialTheme.colorScheme.errorContainer,
        modifier = modifier.fillMaxWidth().testTag(SyncStatusTestTags.FAILED_OPERATIONS_SECTION),
    ) {
        Column {
            Text(text = titleText, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
            failedOperations.forEach { item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = sp.sm)
                        .testTag(SyncStatusTestTags.failedRowTag(item.id)),
                ) {
                    Text(
                        text = stringResource(
                            Res.string.screens_sync_status_failed_item_title_format,
                            operationTypeToEntityType(item.operationType).humanizedLabel(),
                            item.operationType,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    item.lastError?.let { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(top = sp.xs),
                        )
                    }
                    OutlinedButton(
                        onClick = { onRetryClick(item.id) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onErrorContainer),
                        modifier = Modifier
                            .heightIn(min = sp.touchTargetMin)
                            .padding(top = sp.xs)
                            .testTag(SyncStatusTestTags.failedRetryButtonTag(item.id))
                            .semantics { contentDescription = retryAccessibilityLabel },
                    ) {
                        Text(text = retryLabel)
                    }
                }
            }
        }
    }
}
