/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.memberlist.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kpt.core.common.formatGrouped
import kpt.core.designsystem.theme.spacing
import org.jetbrains.compose.resources.stringResource
import org.mifos.groupbanking.core.model.Member
import org.mifos.groupbanking.feature.memberlist.generated.resources.Res
import org.mifos.groupbanking.feature.memberlist.generated.resources.screens_member_list_row_cd
import org.mifos.groupbanking.feature.memberlist.generated.resources.screens_member_list_savings_label

private const val ROW_MIN_HEIGHT_DP = 72

/**
 * One member row — `ui.yaml#components.member_list_item` (mirrors `preview/content.html`).
 * Displays [Member.photoUri]-less initials avatar (no image-loading pipeline is wired anywhere
 * else in this codebase — every other feature's avatar is initials-only too, so a photo fallback
 * is used consistently rather than introducing a new coil dependency unprompted; flagged as an
 * idea-layer follow-up, not invented here), name, savings amount, a role chip (always shown), and
 * a loan-status badge (shown only for ACTIVE/OVERDUE — see [LoanStatus.showsBadge]).
 *
 * `ui.yaml#on_swipe` declares the SAME `action`/`params`/`target` as `on_click` (both navigate to
 * `member-profile` with identical params) — this row therefore dispatches [onClick] for both the
 * tap AND the swipe-to-view-profile affordance rather than wiring a separate
 * `SwipeToDismissBox` gesture for an identical destination; a bespoke swipe reveal is flagged as a
 * follow-up rather than invented here. The whole row is a single merged-semantics ≥72dp touch
 * target (RULE-FEATURE-A11Y-001, `ui.yaml#style.min_touch_target: 72dp`) with a `HorizontalDivider`
 * below (`ui.yaml#style.divider: true`). See API.md#screen.
 */
@Composable
fun MemberListItemRow(member: Member, onClick: () -> Unit, modifier: Modifier = Modifier, testTag: String = "") {
    val sp = MaterialTheme.spacing
    val roleLabel = member.role.label()
    val savingsText = stringResource(Res.string.screens_member_list_savings_label, member.savingsBalance.formatGrouped(0))
    val rowCd = stringResource(Res.string.screens_member_list_row_cd, member.displayName, roleLabel, member.savingsBalance.formatGrouped(0))

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ROW_MIN_HEIGHT_DP.dp)
                .let { if (testTag.isNotEmpty()) it.testTag(testTag) else it }
                .clickable(onClick = onClick)
                .semantics(mergeDescendants = true) { contentDescription = rowCd }
                .padding(horizontal = sp.lg, vertical = sp.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(sp.md),
        ) {
            MemberAvatar(displayName = member.displayName)

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(sp.xs)) {
                Text(text = member.displayName, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(text = savingsText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(sp.xs)) {
                RoleChip(roleLabel = roleLabel, containerColor = member.role.chipContainerColor(), contentColor = member.role.chipContentColor())
                if (member.loanStatus.showsBadge) {
                    LoanBadge(
                        label = member.loanStatus.label(),
                        containerColor = member.loanStatus.badgeContainerColor(),
                        contentColor = member.loanStatus.badgeContentColor(),
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/** 48dp initials avatar — `ui.yaml#components.member_list_item.content.leading.member_avatar`. */
@Composable
private fun MemberAvatar(displayName: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initialsOf(displayName),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

/** Pill-shaped role chip — `ui.yaml#components.member_list_item.content.trailing.children.role_chip`. */
@Composable
private fun RoleChip(roleLabel: String, containerColor: androidx.compose.ui.graphics.Color, contentColor: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Surface(color = containerColor, contentColor = contentColor, shape = RoundedCornerShape(50), modifier = modifier) {
        Text(
            text = roleLabel,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.sm, vertical = MaterialTheme.spacing.xs),
        )
    }
}

/** Small rounded loan-status badge — `ui.yaml#components.member_list_item.content.trailing.children.loan_status_badge`. */
@Composable
private fun LoanBadge(label: String, containerColor: androidx.compose.ui.graphics.Color, contentColor: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Surface(color = containerColor, contentColor = contentColor, shape = RoundedCornerShape(4.dp), modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.xs, vertical = MaterialTheme.spacing.none),
        )
    }
}

/** Shimmering skeleton row shown while `MemberListScreenState.Loading` — mirrors `GroupListCardSkeleton`. */
@Composable
fun MemberListItemSkeleton(modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = ROW_MIN_HEIGHT_DP.dp).padding(horizontal = sp.lg, vertical = sp.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(sp.md),
    ) {
        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(sp.xs)) {
            Box(modifier = Modifier.fillMaxWidth(0.5f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
            Box(modifier = Modifier.fillMaxWidth(0.3f).height(10.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
        }
        Box(modifier = Modifier.size(width = 64.dp, height = 18.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceVariant))
    }
}

/** `{{member.displayName | initials}}` — first letter of the first + last whitespace-delimited tokens, uppercased. */
private fun initialsOf(displayName: String): String {
    val parts = displayName.trim().split(" ").filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> ""
        parts.size == 1 -> parts.first().take(2).uppercase()
        else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
    }
}
