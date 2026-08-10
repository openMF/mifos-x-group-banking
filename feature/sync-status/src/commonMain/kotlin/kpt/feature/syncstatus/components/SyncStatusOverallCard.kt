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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kpt.core.base.designsystem.component.AppCard
import kpt.core.designsystem.theme.spacing
import kpt.core.model.SyncOverallStatus
import kpt.feature.syncstatus.SyncStatusTestTags

private val SyncedBackground = Color(0xFFC8E6C9)
private val SyncedForeground = Color(0xFF1B5E20)
private val SyncedIconTint = Color(0xFF2E7D32)
private val PendingBackground = Color(0xFFFFF9C4)
private val PendingForeground = Color(0xFFE65100)
private val FailedBackground = Color(0xFFFFCDD2)
private val FailedForeground = Color(0xFFB71C1C)

private data class StatusVisual(
    val background: Color,
    val foreground: Color,
    val iconTint: Color,
    val icon: ImageVector,
    val label: String,
)

/**
 * `ui.yaml#components.overall_status_card` — the sync-status dashboard's keystone visual.
 * Background/icon/label are ALL keyed off [overallStatus] (SYNCED green / PENDING yellow / FAILED
 * red per `ui.yaml#components.overall_status_card.style_by_status`), giving the 3 sync states a
 * genuinely distinct on-device rendering (RULE-DESIGN-CONFORMANCE-001) rather than one gray card
 * with swapped text.
 *
 * [dbErrorMessage] is non-null ONLY for `SyncStatusScreenState.Error` (a local-DB *read* failure —
 * distinct from a sync *drain* failure, which is represented purely via [overallStatus] == FAILED
 * and never sets this param). When set, this card forces FAILED-red styling + a
 * [Icons.Filled.CloudOff] icon and shows [dbErrorMessage] as the label instead of the
 * (stale, pre-error) [overallStatus] label and instead of [lastSyncText] —
 * `ui.yaml#states.error.components` names only `[top_bar, overall_status_card]` for the Error
 * state, so this single card is reused honestly rather than inventing an undeclared second error
 * surface or silently showing a misleading "All Synced" card during a DB read failure.
 */
@Composable
fun SyncStatusOverallCard(
    overallStatus: SyncOverallStatus,
    statusIconContentDescription: String,
    syncedLabel: String,
    pendingLabel: String,
    failedLabel: String,
    lastSyncText: String,
    modifier: Modifier = Modifier,
    dbErrorMessage: String? = null,
) {
    val sp = MaterialTheme.spacing
    val effectiveStatus = if (dbErrorMessage != null) SyncOverallStatus.FAILED else overallStatus

    val visual = when (effectiveStatus) {
        SyncOverallStatus.SYNCED -> StatusVisual(
            background = SyncedBackground,
            foreground = SyncedForeground,
            iconTint = SyncedIconTint,
            icon = Icons.Filled.CloudDone,
            label = syncedLabel,
        )
        SyncOverallStatus.PENDING -> StatusVisual(
            background = PendingBackground,
            foreground = PendingForeground,
            iconTint = PendingForeground,
            icon = Icons.Filled.CloudSync,
            label = pendingLabel,
        )
        SyncOverallStatus.FAILED -> StatusVisual(
            background = FailedBackground,
            foreground = FailedForeground,
            iconTint = FailedForeground,
            icon = Icons.Filled.CloudOff,
            label = dbErrorMessage ?: failedLabel,
        )
    }

    AppCard(
        containerColor = visual.background,
        cornerRadius = 16.dp,
        elevation = 2.dp,
        modifier = modifier.fillMaxWidth().testTag(SyncStatusTestTags.OVERALL_STATUS_CARD),
    ) {
        Column {
            Icon(
                imageVector = visual.icon,
                contentDescription = statusIconContentDescription,
                tint = visual.iconTint,
                modifier = Modifier.size(48.dp).testTag(SyncStatusTestTags.STATUS_ICON),
            )
            Text(
                text = visual.label,
                style = MaterialTheme.typography.titleLarge,
                color = visual.foreground,
                modifier = Modifier.padding(top = sp.sm).testTag(SyncStatusTestTags.STATUS_LABEL),
            )
            if (dbErrorMessage == null) {
                Text(
                    text = lastSyncText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = sp.xs),
                )
            }
        }
    }
}
