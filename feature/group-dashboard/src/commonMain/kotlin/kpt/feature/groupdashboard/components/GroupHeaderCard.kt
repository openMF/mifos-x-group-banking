/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.groupdashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import kpt.core.designsystem.theme.spacing
import kpt.core.model.GroupDetail
import kpt.feature.groupdashboard.GroupDashboardTestTags
import kpt.feature.groupdashboard.generated.resources.Res
import kpt.feature.groupdashboard.generated.resources.screens_group_dashboard_cycle_info
import kpt.feature.groupdashboard.generated.resources.screens_group_dashboard_member_count_chip
import kpt.feature.groupdashboard.generated.resources.screens_group_dashboard_overdue_loans_chip
import org.jetbrains.compose.resources.stringResource

/**
 * Primary-container header — `ui.yaml#components.group_header_card`. Renders [group].name (i18n:
 * skip, pure template binding), cycle-info line, and the badge row: group-type chip
 * ([groupTypeName]), viewer-role chip ([viewerRole], lowercased), member-count chip, and — only
 * when `group.overdueLoansCount > 0` — the overdue-loans chip. Chip icons are decorative
 * (`contentDescription = null`) — each chip's own [Text] label already carries the accessible
 * name (mirrors `GroupListCard`'s identical AssistChip precedent). See API.md#screen.
 */
@Composable
fun GroupHeaderCard(group: GroupDetail, groupTypeName: String, viewerRole: String, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val cycleInfo = stringResource(
        Res.string.screens_group_dashboard_cycle_info,
        group.cycleNumber,
        group.cycleLengthMonths,
        group.meetingFrequency,
    )
    val memberCountLabel = stringResource(Res.string.screens_group_dashboard_member_count_chip, group.memberCount)
    val overdueLabel = stringResource(Res.string.screens_group_dashboard_overdue_loans_chip, group.overdueLoansCount)

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier.fillMaxWidth().testTag(GroupDashboardTestTags.HEADER_CARD),
    ) {
        Column(modifier = Modifier.padding(sp.lg)) {
            // group.name — i18n:skip, pure template binding (ui.yaml#components.group_header_card.content.group_name_large).
            Text(
                text = group.name,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = cycleInfo,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = sp.xs, bottom = sp.md),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(sp.xs)) {
                AssistChip(
                    onClick = {},
                    enabled = false,
                    leadingIcon = { Icon(Icons.Filled.Label, contentDescription = null) },
                    // groupTypeName — i18n:skip, pure template binding (ui.yaml#components.group_type_chip).
                    label = { Text(text = groupTypeName, style = MaterialTheme.typography.labelSmall) },
                    colors = AssistChipDefaults.assistChipColors(
                        disabledContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        disabledLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ),
                    border = null,
                    modifier = Modifier.testTag(GroupDashboardTestTags.GROUP_TYPE_CHIP),
                )
                AssistChip(
                    onClick = {},
                    enabled = false,
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    // viewerRole — i18n:skip, pure template binding (ui.yaml#components.viewer_role_chip).
                    label = { Text(text = viewerRole.lowercase(), style = MaterialTheme.typography.labelSmall) },
                    colors = AssistChipDefaults.assistChipColors(
                        disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        disabledLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                    border = null,
                    modifier = Modifier.testTag(GroupDashboardTestTags.VIEWER_ROLE_CHIP),
                )
                AssistChip(
                    onClick = {},
                    enabled = false,
                    leadingIcon = { Icon(Icons.Filled.Groups, contentDescription = null) },
                    label = { Text(text = memberCountLabel, style = MaterialTheme.typography.labelSmall) },
                    colors = AssistChipDefaults.assistChipColors(
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    border = null,
                    modifier = Modifier.testTag(GroupDashboardTestTags.MEMBER_COUNT_CHIP),
                )
                if (group.overdueLoansCount > 0) {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        leadingIcon = { Icon(Icons.Filled.Warning, contentDescription = null) },
                        label = { Text(text = overdueLabel, style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            disabledContainerColor = MaterialTheme.colorScheme.errorContainer,
                            disabledLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                        border = null,
                        modifier = Modifier.testTag(GroupDashboardTestTags.OVERDUE_LOANS_CHIP),
                    )
                }
            }
        }
    }
}
