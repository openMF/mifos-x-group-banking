/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.joinwithcode

import androidx.compose.runtime.Composable
import kpt.core.designsystem.theme.KptTheme
import kpt.core.model.GroupPreview
import kpt.core.model.GroupRole
import kpt.core.model.GroupTypeSlug
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider

/**
 * `@Preview` gallery for `JoinWithCodeScreen.kt`. See API.md#preview. Data source:
 * `demo-data.yaml#entries` (`InvitationRow` tokens MWG7X2 / EXP001 / USED88 +
 * `GroupPreview` dto for groupId 301 "Mwangaza Women's Group" / organiser Jane Otieno / 14
 * members / Member role) plus `ui.yaml#states.*.demo_data`. [GroupTypeSlug] is resolved to
 * [GroupTypeSlug.VSLA] — `demo-data.yaml`'s `GroupPreview.groupType` field is the wire DTO's
 * display string ("Savings & Credit"), while the domain [GroupPreview] this Screen renders
 * requires the [GroupTypeSlug] enum; VSLA is the closest canonical savings-and-credit slug for
 * that display string.
 */
private val mwangazaPreview = GroupPreview(
    groupId = 301,
    groupName = "Mwangaza Women's Group",
    groupType = GroupTypeSlug.VSLA,
    organizerName = "Jane Otieno",
    memberCount = 14,
    officeId = 10,
    roleToAssign = GroupRole.MEMBER,
)

private class JoinWithCodeScreenPreviewProvider : PreviewParameterProvider<JoinWithCodeState> {
    override val values: Sequence<JoinWithCodeState> = sequenceOf(
        // initial — ui.yaml#states.initial.demo_data
        JoinWithCodeState(),
        // validating — ui.yaml#states.validating.demo_data
        JoinWithCodeState(inviteCode = "MWG7X2", inviteStatus = InviteStatus.VALIDATING),
        // preview — ui.yaml#states.preview.demo_data / demo-data.yaml InvitationRow(MWG7X2) + GroupPreview(301)
        JoinWithCodeState(
            inviteCode = "MWG7X2",
            inviteStatus = InviteStatus.VALID,
            groupPreview = mwangazaPreview,
        ),
        // joining — ui.yaml#states.joining.demo_data
        JoinWithCodeState(
            inviteCode = "MWG7X2",
            inviteStatus = InviteStatus.VALID,
            groupPreview = mwangazaPreview,
            isJoining = true,
        ),
        // error_invalid_code — ui.yaml#states.error_invalid_code.demo_data
        JoinWithCodeState(
            inviteCode = "XXXXXX",
            inviteStatus = InviteStatus.INVALID,
            error = JoinError.InvalidCode,
        ),
        // error_expired — demo-data.yaml InvitationRow(EXP001, expires_at 2026-06-01)
        JoinWithCodeState(
            inviteCode = "EXP001",
            inviteStatus = InviteStatus.INVALID,
            error = JoinError.ExpiredCode,
        ),
        // error_already_member — demo-data.yaml InvitationRow(USED88, accepted_at already set)
        JoinWithCodeState(
            inviteCode = "USED88",
            inviteStatus = InviteStatus.INVALID,
            error = JoinError.AlreadyMember,
        ),
        // error_network — ui.yaml#states.error_network.demo_data
        JoinWithCodeState(
            inviteCode = "MWG7X2",
            inviteStatus = InviteStatus.IDLE,
            error = JoinError.Network,
        ),
    )
}

@Preview
@Composable
private fun JoinWithCodeScreenPreview(
    @PreviewParameter(JoinWithCodeScreenPreviewProvider::class)
    state: JoinWithCodeState,
) {
    KptTheme {
        JoinWithCodeContent(state = state, onAction = {})
    }
}

@Preview
@Composable
private fun JoinCodeEntrySectionPreview() {
    KptTheme {
        JoinCodeEntrySection(
            state = JoinWithCodeState(inviteCode = "MWG7"),
            onAction = {},
            isValidating = false,
        )
    }
}

@Preview
@Composable
private fun JoinCodeEntrySectionValidatingPreview() {
    KptTheme {
        JoinCodeEntrySection(
            state = JoinWithCodeState(inviteCode = "MWG7X2", inviteStatus = InviteStatus.VALIDATING),
            onAction = {},
            isValidating = true,
        )
    }
}

@Preview
@Composable
private fun GroupPreviewSectionPreview() {
    KptTheme {
        GroupPreviewSection(
            state = JoinWithCodeState(
                inviteCode = "MWG7X2",
                inviteStatus = InviteStatus.VALID,
                groupPreview = mwangazaPreview,
            ),
            onAction = {},
            isJoining = false,
        )
    }
}

@Preview
@Composable
private fun JoinCodeErrorSectionPreview() {
    KptTheme {
        JoinCodeErrorSection(
            state = JoinWithCodeState(inviteCode = "EXP001", error = JoinError.ExpiredCode),
            onAction = {},
        )
    }
}
