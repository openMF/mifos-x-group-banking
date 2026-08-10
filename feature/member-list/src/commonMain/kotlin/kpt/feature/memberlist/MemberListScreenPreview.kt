/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.memberlist

import androidx.compose.runtime.Composable
import kpt.core.designsystem.theme.KptTheme
import kpt.core.model.LoanStatus
import kpt.core.model.Member
import kpt.core.model.MemberRole
import kpt.feature.memberlist.components.MemberListItemRow
import kpt.feature.memberlist.generated.resources.Res
import kpt.feature.memberlist.generated.resources.screens_member_list_error_network_message
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider

/**
 * `@Preview` gallery for `MemberListScreen.kt`. See API.md#preview. Data source:
 * `SPEC.md#screens.member-list` / `MOCKUP.md#MemberListScreen` sample rows — no `demo-data.yaml`
 * was resolved for this feature, so the members below are hand-authored from the mockup's own
 * worked example (a VSLA roster: chairperson + treasurer with an active loan + a member with an
 * overdue loan + a plain member) rather than generic placeholder literals (RULE-PREVIEW-7 spirit).
 */
private val previewMembers: List<Member> = listOf(
    Member(
        id = "mem-001",
        fineractClientId = 2001L,
        displayName = "Amina Otieno",
        photoUri = null,
        role = MemberRole.CHAIRPERSON,
        savingsBalance = 12500.0,
        loanStatus = LoanStatus.NONE,
    ),
    Member(
        id = "mem-002",
        fineractClientId = 2002L,
        displayName = "Beatrice Wanjiru",
        photoUri = null,
        role = MemberRole.TREASURER,
        savingsBalance = 9800.0,
        loanStatus = LoanStatus.ACTIVE,
    ),
    Member(
        id = "mem-003",
        fineractClientId = 2003L,
        displayName = "Caroline Achieng",
        photoUri = null,
        role = MemberRole.MEMBER,
        savingsBalance = 4300.0,
        loanStatus = LoanStatus.OVERDUE,
    ),
    Member(
        id = "mem-004",
        fineractClientId = 2004L,
        displayName = "Dorcas Mwende",
        photoUri = null,
        role = MemberRole.SECRETARY,
        savingsBalance = 6750.0,
        loanStatus = LoanStatus.NONE,
    ),
)

private class MemberListStatePreviewProvider : PreviewParameterProvider<MemberListState> {
    override val values: Sequence<MemberListState> = sequenceOf(
        // loading — spinner + skeleton rows, no members yet
        MemberListState(isLoading = true, members = emptyList(), groupName = "Mwangaza Women's Group"),
        // content — the 4 seeded members from MOCKUP.md's worked example
        MemberListState(
            isLoading = false,
            members = previewMembers,
            groupName = "Mwangaza Women's Group",
            hasMorePages = false,
            currentOffset = previewMembers.size,
            error = null,
        ),
        // empty — genuinely zero members (SPEC.md Empty state)
        MemberListState(isLoading = false, members = emptyList(), groupName = "Mwangaza Women's Group", error = null),
        // error — network failure (SPEC.md Error state)
        MemberListState(isLoading = false, members = emptyList(), error = MemberListError.Network),
    )
}

@Preview
@Composable
private fun MemberListContentPreview(
    @PreviewParameter(MemberListStatePreviewProvider::class)
    state: MemberListState,
) {
    KptTheme {
        MemberListContent(state = state, onAction = {})
    }
}

@Preview
@Composable
private fun MemberListLoadingSectionPreview() {
    KptTheme {
        MemberListLoadingSection()
    }
}

@Preview
@Composable
private fun MemberListContentSectionPreview() {
    KptTheme {
        MemberListContentSection(
            state = MemberListState(
                isLoading = false,
                members = previewMembers,
                groupName = "Mwangaza Women's Group",
                hasMorePages = false,
                error = null,
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun MemberListEmptySectionPreview() {
    KptTheme {
        MemberListEmptySection(onAction = {})
    }
}

@Preview
@Composable
private fun MemberListErrorSectionPreview() {
    KptTheme {
        MemberListErrorSection(
            message = stringResource(Res.string.screens_member_list_error_network_message),
            onRetry = {},
        )
    }
}

@Preview
@Composable
private fun MemberListFabContentPreview() {
    KptTheme {
        MemberListFabContent(fabCd = "Add member to group")
    }
}

@Preview
@Composable
private fun MemberListItemRowPreview() {
    KptTheme {
        MemberListItemRow(member = previewMembers[1], onClick = {})
    }
}
