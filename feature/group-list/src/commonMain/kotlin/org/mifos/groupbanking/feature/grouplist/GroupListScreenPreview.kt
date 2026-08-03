/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.grouplist

import androidx.compose.runtime.Composable
import kotlinx.datetime.LocalDate
import kpt.core.designsystem.theme.KptTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider
import org.mifos.groupbanking.core.model.Group
import org.mifos.groupbanking.core.model.GroupTypeSlug
import org.mifos.groupbanking.core.model.HealthIndicator
import org.mifos.groupbanking.core.model.ViewerRole
import org.mifos.groupbanking.feature.grouplist.components.GroupListCard
import org.mifos.groupbanking.feature.grouplist.components.GroupListCardSkeleton
import org.mifos.groupbanking.feature.grouplist.components.GroupListSearchBar
import org.mifos.groupbanking.feature.grouplist.generated.resources.Res
import org.mifos.groupbanking.feature.grouplist.generated.resources.screens_group_list_error_network_message

/**
 * `@Preview` gallery for `GroupListScreen.kt`. See API.md#preview. Data source:
 * `SPEC.md#screens.group-list` / `MOCKUP.md#GroupListScreen` sample rows (Mwangaza Women's
 * Group / Tumaini Savings Circle) — no `demo-data.yaml` was resolved for this feature, so the
 * two seeded groups below are hand-authored from the mockup's own worked example rather than
 * generic placeholder literals (RULE-PREVIEW-7 spirit).
 */
private val previewGroups: List<Group> = listOf(
    Group(
        id = "grp-001",
        name = "Mwangaza Women's Group",
        groupType = GroupTypeSlug.VSLA,
        viewerRole = ViewerRole.TREASURER,
        cycleNumber = 1,
        memberCount = 5,
        lastMeetingDate = LocalDate(2026, 4, 28),
        healthIndicator = HealthIndicator.GREEN,
        overdueRate = 0.0,
        status = "active",
        fineractCenterId = 101L,
    ),
    Group(
        id = "grp-002",
        name = "Tumaini Savings Circle",
        groupType = GroupTypeSlug.ROSCA,
        viewerRole = ViewerRole.MEMBER,
        cycleNumber = 3,
        memberCount = 12,
        lastMeetingDate = LocalDate(2026, 4, 25),
        healthIndicator = HealthIndicator.AMBER,
        overdueRate = 0.08,
        status = "active",
        fineractCenterId = 102L,
    ),
)

private class GroupListStatePreviewProvider : PreviewParameterProvider<GroupListState> {
    override val values: Sequence<GroupListState> = sequenceOf(
        // loading — spinner + skeleton rows, no groups yet
        GroupListState(isLoading = true, groups = emptyList(), filteredGroups = emptyList(), error = null),
        // content — the 2 seeded groups from MOCKUP.md's worked example
        GroupListState(
            isLoading = false,
            groups = previewGroups,
            filteredGroups = previewGroups,
            searchQuery = "",
            error = null,
        ),
        // empty — genuinely zero groups (SPEC.md Empty state)
        GroupListState(isLoading = false, groups = emptyList(), filteredGroups = emptyList(), error = null),
        // error — network failure (SPEC.md Error state)
        GroupListState(
            isLoading = false,
            groups = emptyList(),
            filteredGroups = emptyList(),
            error = GroupListError.Network,
        ),
    )
}

@Preview
@Composable
private fun GroupListContentPreview(
    @PreviewParameter(GroupListStatePreviewProvider::class)
    state: GroupListState,
) {
    KptTheme {
        GroupListContent(state = state, onAction = {})
    }
}

@Preview
@Composable
private fun GroupListLoadingSectionPreview() {
    KptTheme {
        GroupListLoadingSection(query = "", onAction = {})
    }
}

@Preview
@Composable
private fun GroupListContentSectionPreview() {
    KptTheme {
        GroupListContentSection(
            state = GroupListState(
                isLoading = false,
                groups = previewGroups,
                filteredGroups = previewGroups,
                searchQuery = "",
                error = null,
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun GroupListEmptySectionPreview() {
    KptTheme {
        GroupListEmptySection(query = "", onAction = {})
    }
}

@Preview
@Composable
private fun GroupListErrorSectionPreview() {
    KptTheme {
        GroupListErrorSection(
            message = stringResource(Res.string.screens_group_list_error_network_message),
            onRetry = {},
        )
    }
}

@Preview
@Composable
private fun GroupListFabContentPreview() {
    KptTheme {
        GroupListFabContent(fabCd = "Create new savings group")
    }
}

@Preview
@Composable
private fun GroupListCardPreview() {
    KptTheme {
        GroupListCard(group = previewGroups[0], onClick = {})
    }
}

@Preview
@Composable
private fun GroupListCardSkeletonPreview() {
    KptTheme {
        GroupListCardSkeleton()
    }
}

@Preview
@Composable
private fun GroupListSearchBarPreview() {
    KptTheme {
        GroupListSearchBar(query = "Mwangaza", onQueryChange = {}, onClear = {})
    }
}
