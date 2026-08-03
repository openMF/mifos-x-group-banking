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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import kpt.core.base.designsystem.component.AppCard
import kpt.core.common.formatGrouped
import kpt.core.designsystem.theme.spacing
import org.jetbrains.compose.resources.stringResource
import org.mifos.groupbanking.core.model.ActivityItem
import org.mifos.groupbanking.core.model.ActivityType
import org.mifos.groupbanking.feature.groupdashboard.GroupDashboardTestTags
import org.mifos.groupbanking.feature.groupdashboard.generated.resources.Res
import org.mifos.groupbanking.feature.groupdashboard.generated.resources.screens_group_dashboard_activity_amount
import org.mifos.groupbanking.feature.groupdashboard.generated.resources.screens_group_dashboard_activity_label
import org.mifos.groupbanking.feature.groupdashboard.generated.resources.screens_group_dashboard_activity_supporting

/**
 * Recent-activity card — `ui.yaml#components.activity_feed_section`. Wraps the section label
 * plus every [activities] row in a single [AppCard] (mirrors `preview/content_accumulating.html`'s
 * one-card composition — `.gd-feed`). See API.md#screen.
 */
@Composable
fun ActivityFeedSection(activities: List<ActivityItem>, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val label = stringResource(Res.string.screens_group_dashboard_activity_label)

    AppCard(modifier = modifier.fillMaxWidth().testTag(GroupDashboardTestTags.ACTIVITY_FEED_SECTION)) {
        Column {
            Text(text = label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = sp.sm))
            activities.forEach { activity -> ActivityRow(activity = activity) }
        }
    }
}

/**
 * One recent-activity row — `ui.yaml#components.activity_feed_section.content.activity_list_item`.
 * [ActivityItem.description] renders raw (i18n:skip, dynamic payload per `ui.yaml`'s own
 * `// i18n: skip` note). Supporting text uses the translatable `activity_supporting_format` when
 * [ActivityItem.memberName] is present (e.g. DEPOSIT/LOAN), else falls back to the bare date
 * (MEETING/NEW_MEMBER rows have no member attribution — i18n:skip, dynamic date-only fallback).
 * Leading icon is decorative (`contentDescription = null`) — the type is conveyed by the adjacent
 * description text. See API.md#screen.
 */
@Composable
fun ActivityRow(activity: ActivityItem, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val supportingText = activity.memberName?.let {
        stringResource(Res.string.screens_group_dashboard_activity_supporting, activity.date, it)
    } ?: activity.date // i18n:skip — dynamic date-only fallback, see class KDoc.
    val amountText = activity.amount?.let { stringResource(Res.string.screens_group_dashboard_activity_amount, it.formatGrouped(0)) }
    val icon = when (activity.type) {
        ActivityType.MEETING -> Icons.Filled.EventNote
        ActivityType.DEPOSIT -> Icons.Filled.ArrowDownward
        ActivityType.LOAN -> Icons.Filled.AccountBalance
        ActivityType.PENALTY -> Icons.Filled.Warning
        ActivityType.SHARE_OUT -> Icons.Filled.CallSplit
        ActivityType.UNKNOWN -> Icons.Filled.Info
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = sp.touchTargetMin)
            .padding(vertical = sp.xs)
            .testTag(GroupDashboardTestTags.activityRowTag(activity.id)),
        horizontalArrangement = Arrangement.spacedBy(sp.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(modifier = Modifier.weight(1f)) {
            // activity.description — i18n:skip, dynamic template binding (ui.yaml#components.activity_list_item.headline).
            Text(text = activity.description, style = MaterialTheme.typography.bodyLarge)
            Text(text = supportingText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (amountText != null) {
            Text(text = amountText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
