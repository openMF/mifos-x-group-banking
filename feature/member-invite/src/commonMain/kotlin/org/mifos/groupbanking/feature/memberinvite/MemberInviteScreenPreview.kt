/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.memberinvite

import androidx.compose.runtime.Composable
import kpt.core.designsystem.theme.KptTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider
import org.mifos.groupbanking.core.model.MemberRole
import org.mifos.groupbanking.core.model.PendingInvite

// -- ui.yaml#states.*.demo_data fixtures -------------------------------------------------------

private val demoPending = listOf(
    PendingInvite(
        rowId = 1,
        token = "AB12CD",
        invitedEmailPhone = "+254712345678",
        roleToAssign = MemberRole.TREASURER,
        expiresAt = "2026-07-23",
    ),
    PendingInvite(
        rowId = 2,
        token = "EF34GH",
        invitedEmailPhone = "amina.omondi@email.com",
        roleToAssign = MemberRole.MEMBER,
        expiresAt = "2026-07-24",
    ),
)

/** `ui.yaml#states.loading.demo_data` — initial mount, fetching pending invites. */
private val demoLoadingState = MemberInviteState(isLoadingPending = true)

/** `ui.yaml#states.content.demo_data` — idle form + pending list. */
private val demoContentState = MemberInviteState(pendingInvites = demoPending)

/** Empty pending list — no invites yet. */
private val demoEmptyState = MemberInviteState(pendingInvites = emptyList())

/** `ui.yaml#states.generating.demo_data` — Generate button spinner in-flight. */
private val demoGeneratingState = MemberInviteState(
    emailPhone = "+254798765432",
    selectedRole = MemberRole.SECRETARY,
    isGenerating = true,
    pendingInvites = demoPending,
)

/** `ui.yaml#states.generated.demo_data` — code card visible. */
private val demoGeneratedState = MemberInviteState(
    emailPhone = "+254798765432",
    selectedRole = MemberRole.SECRETARY,
    generatedCode = "K7X2P9",
    generatedLink = "https://mifos.app/join?token=K7X2P9&group=42",
    pendingInvites = demoPending,
)

/** `ui.yaml#states.error.demo_data` — network error banner + retry. */
private val demoErrorState = MemberInviteState(
    pendingInvites = demoPending,
    error = MemberInviteError.Network,
)

/**
 * `@Preview` gallery for `MemberInviteScreen.kt`. See API.md#preview. Data source:
 * `ui.yaml#states.*.demo_data`.
 */
private class MemberInviteScreenPreviewProvider : PreviewParameterProvider<MemberInviteState> {
    override val values: Sequence<MemberInviteState> = sequenceOf(
        demoLoadingState,
        demoEmptyState,
        demoContentState,
        demoGeneratingState,
        demoGeneratedState,
        demoErrorState,
    )
}

@Preview
@Composable
private fun MemberInviteContentPreview(
    @PreviewParameter(MemberInviteScreenPreviewProvider::class)
    state: MemberInviteState,
) {
    KptTheme {
        MemberInviteContent(state = state, onAction = {})
    }
}
