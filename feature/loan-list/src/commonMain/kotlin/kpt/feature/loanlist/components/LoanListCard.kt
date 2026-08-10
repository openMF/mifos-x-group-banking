/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.loanlist.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kpt.core.base.designsystem.component.AppCard
import kpt.core.common.formatGrouped
import kpt.core.designsystem.theme.spacing
import kpt.core.model.LoanAccountStatus
import kpt.core.model.LoanSummary
import kpt.feature.loanlist.generated.resources.Res
import kpt.feature.loanlist.generated.resources.screens_loan_list_amount_product_label
import kpt.feature.loanlist.generated.resources.screens_loan_list_card_cd
import kpt.feature.loanlist.generated.resources.screens_loan_list_next_repayment_label
import kpt.feature.loanlist.generated.resources.screens_loan_list_outstanding_label
import kpt.feature.loanlist.generated.resources.screens_loan_list_overdue_label
import org.jetbrains.compose.resources.stringResource

private const val CARD_MIN_HEIGHT_DP = 72

/**
 * One loan row — `ui.yaml#components.loan_card` (mirrors `preview/content.html`). Displays an
 * initials avatar ([LoanSummary.memberPhotoUrl] is `null` on every seeded row and no image-loading
 * pipeline is wired anywhere else in this codebase — mirrors `MemberListItemRow`'s identical
 * no-coil precedent, flagged as an idea-layer follow-up rather than invented here), member name,
 * "KES {principal} · {productName}", outstanding balance, a colour-coded [LoanAccountStatus] badge,
 * the next-repayment date (only when [LoanSummary.status] is `ACTIVE` and
 * [LoanSummary.nextRepaymentDate] is non-null, `ui.yaml#components.loan_card.content
 * .next_repayment_text.visible_when`), and an error-toned overdue line (only when
 * [LoanSummary.isOverdue]). The whole row is a single merged-semantics ≥72dp touch target
 * (RULE-FEATURE-A11Y-001, `ui.yaml#style.min_touch_target: 72dp`) dispatching [onClick]. See
 * API.md#screen.
 */
@Composable
fun LoanListCard(loan: LoanSummary, onClick: () -> Unit, modifier: Modifier = Modifier, testTag: String = "") {
    val sp = MaterialTheme.spacing
    val amountProductText = stringResource(
        Res.string.screens_loan_list_amount_product_label,
        loan.principalAmount.formatGrouped(0),
        loan.loanProductName,
    )
    val outstandingText = stringResource(
        Res.string.screens_loan_list_outstanding_label,
        loan.outstandingBalance.formatGrouped(0),
    )
    val cardCd = stringResource(
        Res.string.screens_loan_list_card_cd,
        loan.memberName,
        loan.status.name,
        loan.outstandingBalance.formatGrouped(0),
    )

    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = CARD_MIN_HEIGHT_DP.dp)
            .let { if (testTag.isNotEmpty()) it.testTag(testTag) else it }
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = cardCd },
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(sp.md)) {
            LoanMemberAvatar(memberName = loan.memberName)

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(sp.xs)) {
                Text(text = loan.memberName, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = amountProductText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = outstandingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val nextRepaymentDate = loan.nextRepaymentDate
                if (loan.status == LoanAccountStatus.ACTIVE && nextRepaymentDate != null) {
                    Text(
                        text = stringResource(Res.string.screens_loan_list_next_repayment_label, nextRepaymentDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (loan.isOverdue) {
                    Text(
                        text = stringResource(Res.string.screens_loan_list_overdue_label, loan.overdueAmount.formatGrouped(0)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Surface(
                color = loan.status.badgeContainerColor(),
                contentColor = loan.status.badgeContentColor(),
                shape = RoundedCornerShape(50),
            ) {
                // loan.status.name — i18n:skip, dynamic enum binding (mirrors GroupListCard's
                // identical group.healthIndicator.name / group.groupType.name convention).
                Text(
                    text = loan.status.name,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = sp.sm, vertical = sp.xs),
                )
            }
        }
    }
}

/** 40dp initials avatar — `ui.yaml#components.loan_card.content.member_avatar` (`size: 40dp`). */
@Composable
private fun LoanMemberAvatar(memberName: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initialsOf(memberName),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

/** `{{loan.memberName | initials}}` — mirrors `MemberListItemRow`'s identical `initialsOf`. */
private fun initialsOf(displayName: String): String {
    val parts = displayName.trim().split(" ").filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> ""
        parts.size == 1 -> parts.first().take(2).uppercase()
        else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
    }
}

/**
 * Shimmering skeleton row shown while `LoanListScreenState.Loading` — mirrors
 * `preview/loading.html`'s `shimmer_list` (avatar + 3 lines + badge, animated opacity in lieu of a
 * CSS shimmer gradient, same technique as `GroupListCardSkeleton`). Purely decorative — no
 * semantics/testTag of its own; the surrounding scaffold's loading region carries the single
 * accessible "loading" announcement via
 * [kpt.feature.loanlist.LoanListTestTags.LOADING_INDICATOR]. See API.md#screen.
 */
@Composable
fun LoanListCardSkeleton(modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val transition = rememberInfiniteTransition(label = "loan-list-skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "loan-list-skeleton-alpha",
    )
    val skeletonColor = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.12f)

    AppCard(modifier = modifier.fillMaxWidth().heightIn(min = 96.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(sp.md)) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(skeletonColor))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(sp.xs)) {
                Box(modifier = Modifier.fillMaxWidth(0.6f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(skeletonColor))
                Box(modifier = Modifier.fillMaxWidth(0.75f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(skeletonColor))
                Box(modifier = Modifier.fillMaxWidth(0.4f).height(10.dp).clip(RoundedCornerShape(4.dp)).background(skeletonColor))
            }
            Box(modifier = Modifier.width(60.dp).height(22.dp).clip(RoundedCornerShape(4.dp)).background(skeletonColor))
        }
    }
}
