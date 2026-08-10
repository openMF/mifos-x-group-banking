/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.memberlist.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import kpt.core.model.LoanStatus
import kpt.core.model.MemberRole
import kpt.feature.memberlist.generated.resources.Res
import kpt.feature.memberlist.generated.resources.screens_member_list_loan_active
import kpt.feature.memberlist.generated.resources.screens_member_list_loan_overdue
import kpt.feature.memberlist.generated.resources.screens_member_list_role_chairperson
import kpt.feature.memberlist.generated.resources.screens_member_list_role_member
import kpt.feature.memberlist.generated.resources.screens_member_list_role_secretary
import kpt.feature.memberlist.generated.resources.screens_member_list_role_treasurer
import org.jetbrains.compose.resources.stringResource

/**
 * Role-chip label + colors — verbatim mirror of `ui.yaml#components.member_list_item.content
 * .trailing.children[role_chip].style` (CHAIRPERSON→primaryContainer, TREASURER→
 * secondaryContainer, SECRETARY→tertiaryContainer, MEMBER→surfaceVariant). [MemberRole.UNKNOWN]
 * (a client-side safety member not in the wire DTO closed set — see `Member.kt` KDoc) falls back
 * to the MEMBER treatment, mirroring `data-flow.yaml`'s documented server-side "unknown values
 * fall back to MEMBER" mapper behavior. See API.md#screen.
 */
@Composable
fun MemberRole.label(): String = when (this) {
    MemberRole.CHAIRPERSON -> stringResource(Res.string.screens_member_list_role_chairperson)
    MemberRole.TREASURER -> stringResource(Res.string.screens_member_list_role_treasurer)
    MemberRole.SECRETARY -> stringResource(Res.string.screens_member_list_role_secretary)
    MemberRole.MEMBER, MemberRole.UNKNOWN -> stringResource(Res.string.screens_member_list_role_member)
}

@Composable
fun MemberRole.chipContainerColor(): Color = when (this) {
    MemberRole.CHAIRPERSON -> MaterialTheme.colorScheme.primaryContainer
    MemberRole.TREASURER -> MaterialTheme.colorScheme.secondaryContainer
    MemberRole.SECRETARY -> MaterialTheme.colorScheme.tertiaryContainer
    MemberRole.MEMBER, MemberRole.UNKNOWN -> MaterialTheme.colorScheme.surfaceVariant
}

@Composable
fun MemberRole.chipContentColor(): Color = when (this) {
    MemberRole.CHAIRPERSON -> MaterialTheme.colorScheme.onPrimaryContainer
    MemberRole.TREASURER -> MaterialTheme.colorScheme.onSecondaryContainer
    MemberRole.SECRETARY -> MaterialTheme.colorScheme.onTertiaryContainer
    MemberRole.MEMBER, MemberRole.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
}

/**
 * Loan-status badge label + colors — verbatim mirror of `ui.yaml#components.member_list_item
 * .content.trailing.children[loan_status_badge].style` (ACTIVE bg=#C8E6C9 text=#1B5E20; OVERDUE
 * bg=#FFCDD2 text=#B71C1C). [LoanStatus.NONE] and [LoanStatus.UNKNOWN] render NO badge at all
 * (mirrors `preview/content.html`'s per-row markup — rows with `loanStatus: NONE` carry no
 * `.loan-badge` element), so [label]/[badgeContainerColor]/[badgeContentColor] are only ever
 * invoked by [MemberListItemRow] when `loanStatus` is ACTIVE or OVERDUE. See API.md#screen.
 */
@Composable
fun LoanStatus.label(): String = when (this) {
    LoanStatus.ACTIVE -> stringResource(Res.string.screens_member_list_loan_active)
    LoanStatus.OVERDUE -> stringResource(Res.string.screens_member_list_loan_overdue)
    LoanStatus.NONE, LoanStatus.UNKNOWN -> ""
}

fun LoanStatus.badgeContainerColor(): Color = when (this) {
    LoanStatus.ACTIVE -> Color(0xFFC8E6C9)
    LoanStatus.OVERDUE -> Color(0xFFFFCDD2)
    LoanStatus.NONE, LoanStatus.UNKNOWN -> Color.Unspecified
}

fun LoanStatus.badgeContentColor(): Color = when (this) {
    LoanStatus.ACTIVE -> Color(0xFF1B5E20)
    LoanStatus.OVERDUE -> Color(0xFFB71C1C)
    LoanStatus.NONE, LoanStatus.UNKNOWN -> Color.Unspecified
}

/** True when a loan-status badge should render at all — see [LoanStatus.label] KDoc. */
val LoanStatus.showsBadge: Boolean
    get() = this == LoanStatus.ACTIVE || this == LoanStatus.OVERDUE
