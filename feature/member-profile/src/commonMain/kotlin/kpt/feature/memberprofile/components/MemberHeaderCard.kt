/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.memberprofile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kpt.core.designsystem.theme.spacing
import kpt.core.model.MemberProfile
import kpt.core.model.MemberRole
import kpt.feature.memberprofile.MemberProfileTestTags
import kpt.feature.memberprofile.generated.resources.Res
import kpt.feature.memberprofile.generated.resources.screens_member_profile_avatar_cd
import kpt.feature.memberprofile.generated.resources.screens_member_profile_edit_role
import kpt.feature.memberprofile.generated.resources.screens_member_profile_member_since
import kpt.feature.memberprofile.generated.resources.screens_member_profile_role_chairperson
import kpt.feature.memberprofile.generated.resources.screens_member_profile_role_member
import kpt.feature.memberprofile.generated.resources.screens_member_profile_role_secretary
import kpt.feature.memberprofile.generated.resources.screens_member_profile_role_treasurer
import org.jetbrains.compose.resources.stringResource

private val AVATAR_SIZE = 72.dp

/**
 * Primary-container identity header — `ui.yaml#components.member_header_card`. Renders an
 * initials avatar (no image-loading pipeline is wired anywhere else in this codebase — every
 * other feature's avatar is initials-only too, mirrors `MemberListItemRow.MemberAvatar`'s
 * identical precedent; `MemberProfile.hasPhoto`/`photoUri` is intentionally not consumed here),
 * [MemberProfile.displayName], a "member since" line, phone, a role-colour-coded chip, and —
 * only when [isCurrentUserChairperson] — the Edit Role affordance
 * ([MemberProfileTestTags.EDIT_ROLE_BUTTON]) which dispatches [onEditRoleClick]. See API.md#screen.
 */
@Composable
fun MemberHeaderCard(
    member: MemberProfile,
    role: MemberRole,
    isCurrentUserChairperson: Boolean,
    onEditRoleClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val memberSinceText = stringResource(Res.string.screens_member_profile_member_since, member.joinDate)
    val avatarCd = stringResource(Res.string.screens_member_profile_avatar_cd, member.displayName)
    val editRoleLabel = stringResource(Res.string.screens_member_profile_edit_role)

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier.fillMaxWidth().testTag(MemberProfileTestTags.HEADER_CARD),
    ) {
        Column(
            modifier = Modifier.padding(sp.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(AVATAR_SIZE)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .semantics { contentDescription = avatarCd }
                    .testTag(MemberProfileTestTags.AVATAR),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initialsOf(member.displayName),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Text(
                text = member.displayName,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = sp.sm),
            )
            Text(
                text = memberSinceText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = sp.xs),
            )
            Text(
                text = member.phone,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = sp.xs),
            )
            RoleChip(role = role, modifier = Modifier.padding(top = sp.sm))
            if (isCurrentUserChairperson) {
                OutlinedButton(
                    onClick = onEditRoleClick,
                    modifier = Modifier
                        .heightIn(min = sp.touchTargetMin)
                        .padding(top = sp.sm)
                        .testTag(MemberProfileTestTags.EDIT_ROLE_BUTTON),
                ) {
                    Text(text = editRoleLabel)
                }
            }
        }
    }
}

/** Role-colour-coded chip — `ui.yaml#components.member_header_card.content.role_chip`. */
@Composable
fun RoleChip(role: MemberRole, modifier: Modifier = Modifier) {
    val (containerColor, contentColor) = role.chipColors()
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(text = role.label(), style = MaterialTheme.typography.labelMedium) },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = containerColor,
            disabledLabelColor = contentColor,
        ),
        border = null,
        shape = RoundedCornerShape(50),
        modifier = modifier.testTag(MemberProfileTestTags.ROLE_CHIP),
    )
}

/** `ui.yaml#components.role_chip.label` — mirrors `strings.xml` role_* rows. */
@Composable
internal fun MemberRole.label(): String = when (this) {
    MemberRole.CHAIRPERSON -> stringResource(Res.string.screens_member_profile_role_chairperson)
    MemberRole.TREASURER -> stringResource(Res.string.screens_member_profile_role_treasurer)
    MemberRole.SECRETARY -> stringResource(Res.string.screens_member_profile_role_secretary)
    MemberRole.MEMBER, MemberRole.UNKNOWN -> stringResource(Res.string.screens_member_profile_role_member)
}

/** `ui.yaml#components.role_chip.style` — per-role container/content colour pair. */
@Composable
internal fun MemberRole.chipColors(): Pair<Color, Color> = when (this) {
    MemberRole.CHAIRPERSON -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
    MemberRole.TREASURER -> MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary
    MemberRole.SECRETARY -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onTertiary
    MemberRole.MEMBER, MemberRole.UNKNOWN -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
}

/** `{{member.displayName | initials}}` — mirrors `MemberListItemRow.initialsOf`'s identical convention. */
private fun initialsOf(displayName: String): String {
    val parts = displayName.trim().split(" ").filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> ""
        parts.size == 1 -> parts.first().take(2).uppercase()
        else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
    }
}
