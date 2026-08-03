/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.personaldashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import kpt.core.designsystem.theme.spacing
import org.mifos.groupbanking.core.model.GroupSummary
import org.mifos.groupbanking.feature.personaldashboard.PersonalDashboardTestTags

/**
 * Horizontal group-selector chip row — `ui.yaml#components.group_banner.content
 * .group_selector_row`. Rendered by the caller only when `myGroups.size > 1`. Each chip
 * dispatches [org.mifos.groupbanking.feature.personaldashboard.PersonalDashboardAction
 * .OnSelectGroup] with the tapped group's id via [onGroupSelected]. See API.md#screen.
 */
@Composable
fun GroupSelectorChipRow(
    groups: List<GroupSummary>,
    selectedGroupId: String?,
    onGroupSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    LazyRow(
        modifier = modifier.testTag(PersonalDashboardTestTags.GROUP_SELECTOR_ROW),
        horizontalArrangement = Arrangement.spacedBy(sp.xs),
    ) {
        items(items = groups, key = { it.groupId }) { group ->
            FilterChip(
                selected = group.groupId == selectedGroupId,
                onClick = { onGroupSelected(group.groupId) },
                label = { Text(text = group.name, style = MaterialTheme.typography.labelLarge) },
                modifier = Modifier
                    .heightIn(min = sp.touchTargetMin)
                    .testTag(PersonalDashboardTestTags.groupChipTag(group.groupId)),
            )
        }
    }
}
