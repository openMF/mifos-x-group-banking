/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.personalloans.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kpt.core.base.designsystem.component.AppCard
import kpt.core.common.formatGrouped
import kpt.core.designsystem.theme.spacing
import kpt.core.model.LoanAccountStatus
import kpt.core.model.LoanSummary
import kpt.feature.personalloans.generated.resources.Res
import kpt.feature.personalloans.generated.resources.screens_personal_loans_card_cd
import kpt.feature.personalloans.generated.resources.screens_personal_loans_details_label
import kpt.feature.personalloans.generated.resources.screens_personal_loans_details_next_due_label
import kpt.feature.personalloans.generated.resources.screens_personal_loans_details_outstanding_label
import kpt.feature.personalloans.generated.resources.screens_personal_loans_details_overdue_label
import kpt.feature.personalloans.generated.resources.screens_personal_loans_details_principal_label
import kpt.feature.personalloans.generated.resources.screens_personal_loans_details_status_label
import kpt.feature.personalloans.generated.resources.screens_personal_loans_expand_cd
import kpt.feature.personalloans.generated.resources.screens_personal_loans_next_repayment_label
import kpt.feature.personalloans.generated.resources.screens_personal_loans_outstanding_amount_format
import kpt.feature.personalloans.generated.resources.screens_personal_loans_overdue_cd
import kpt.feature.personalloans.generated.resources.screens_personal_loans_status_chip_cd
import org.jetbrains.compose.resources.stringResource

private const val CARD_SKELETON_MIN_HEIGHT_DP = 96

/**
 * One loan row — `ui.yaml#components.loan_card` (mirrors `preview/content.html`). Displays the
 * loan product name + a colour-coded [LoanAccountStatus] badge (via
 * [personalLoansBadgeContainerColor]/[personalLoansBadgeContentColor]), the outstanding balance,
 * a next-repayment line (only when [LoanSummary.status] is `ACTIVE` and
 * [LoanSummary.nextRepaymentDate] is non-null — `ui.yaml#components.loan_card.content
 * .next_repayment_row.visible`), an overdue warning icon (only when [LoanSummary.isOverdue]), and
 * an expand/collapse chevron. The whole row is a single merged-semantics ≥48dp touch target
 * (RULE-FEATURE-A11Y-001) dispatching [onClick] — `ui.yaml#components.loan_card.on_click.action:
 * OnLoanExpand`. When [isExpanded] is true, [PersonalLoansCardDetails] renders below the divider.
 * See API.md#screen.
 */
@Composable
fun PersonalLoansCard(
    loan: LoanSummary,
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "",
    detailsTestTag: String = "",
) {
    val sp = MaterialTheme.spacing
    val statusChipCd = stringResource(Res.string.screens_personal_loans_status_chip_cd)
    val expandCd = stringResource(Res.string.screens_personal_loans_expand_cd)
    val overdueIconCd = stringResource(Res.string.screens_personal_loans_overdue_cd)
    val outstandingText = stringResource(
        Res.string.screens_personal_loans_outstanding_amount_format,
        loan.outstandingBalance.formatGrouped(0),
    )
    val cardCd = stringResource(
        Res.string.screens_personal_loans_card_cd,
        loan.loanProductName,
        loan.status.name,
        loan.outstandingBalance.formatGrouped(0),
    )
    val nextRepaymentDate = loan.nextRepaymentDate

    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = sp.touchTargetMin)
            .let { if (testTag.isNotEmpty()) it.testTag(testTag) else it }
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = cardCd },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(sp.xs)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = loan.loanProductName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    color = loan.status.personalLoansBadgeContainerColor(),
                    contentColor = loan.status.personalLoansBadgeContentColor(),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.semantics { contentDescription = statusChipCd },
                ) {
                    // loan.status.name — i18n:skip, dynamic enum binding (mirrors LoanListCard's
                    // identical loan.status.name convention).
                    Text(
                        text = loan.status.name,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = sp.sm, vertical = sp.xs),
                    )
                }
            }

            Text(
                text = outstandingText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            if (loan.status == LoanAccountStatus.ACTIVE && nextRepaymentDate != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(sp.xs)) {
                    Text(
                        text = stringResource(Res.string.screens_personal_loans_next_repayment_label, nextRepaymentDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (loan.isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (loan.isOverdue) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = overdueIconCd,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            Icon(
                imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = expandCd,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.End),
            )

            if (isExpanded) {
                PersonalLoansCardDetails(
                    loan = loan,
                    modifier = if (detailsTestTag.isNotEmpty()) Modifier.testTag(detailsTestTag) else Modifier,
                )
            }
        }
    }
}

/**
 * Expanded detail panel toggled by [PersonalLoansCard.onClick] (`selectedLoanId == loan.id`).
 *
 * **Confirmed idea-layer gap, flagged not invented:** `ui.yaml#components.loan_card.content
 * .repayment_schedule_section` reads `loan.repaymentSchedule.periods` and titles itself "Repayment
 * Schedule", but the canonical `core/model` [LoanSummary] domain type (shared by `loan-list` +
 * `loan-detail` + this screen) carries NO `repaymentSchedule` field at all — see
 * `PersonalLoansState`'s KDoc in `PersonalLoansViewModel.kt` for the full gap analysis. Rather
 * than fabricate schedule periods, this panel renders an honest "Loan Details" heading
 * ([Res.string.screens_personal_loans_details_label], deliberately NOT
 * `repayment_schedule_label`) over the [LoanSummary] fields that DO exist: principal amount,
 * outstanding balance, status, next-due date (when non-null), and overdue amount (when
 * [LoanSummary.isOverdue]). Flagged for the cross-feature repair station
 * (RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1) — the fix is widening [LoanSummary] with a
 * `repaymentSchedule` field once a shared representation is agreed across `loan-list` /
 * `loan-detail` / `personal-loans`.
 */
@Composable
private fun PersonalLoansCardDetails(loan: LoanSummary, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing

    Column(
        modifier = modifier.fillMaxWidth().padding(top = sp.xs),
        verticalArrangement = Arrangement.spacedBy(sp.xs),
    ) {
        HorizontalDivider(modifier = Modifier.padding(vertical = sp.sm))
        Text(
            text = stringResource(Res.string.screens_personal_loans_details_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(
                Res.string.screens_personal_loans_details_principal_label,
                loan.principalAmount.formatGrouped(0),
            ),
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = stringResource(
                Res.string.screens_personal_loans_details_outstanding_label,
                loan.outstandingBalance.formatGrouped(0),
            ),
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = stringResource(Res.string.screens_personal_loans_details_status_label, loan.status.name),
            style = MaterialTheme.typography.bodySmall,
        )
        loan.nextRepaymentDate?.let { date ->
            Text(
                text = stringResource(Res.string.screens_personal_loans_details_next_due_label, date),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (loan.isOverdue) {
            Text(
                text = stringResource(
                    Res.string.screens_personal_loans_details_overdue_label,
                    loan.overdueAmount.formatGrouped(0),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/**
 * Shimmering skeleton row shown while `PersonalLoansScreenState.Loading` — mirrors
 * `preview/loading.html`'s `shimmer_loading` (`ui.yaml#components.shimmer_loading.count: 3`).
 * Purely decorative — no semantics/testTag of its own; the surrounding loading section carries
 * the single accessible "loading" announcement via
 * [kpt.feature.personalloans.PersonalLoansTestTags.LOADING_INDICATOR]. See
 * API.md#screen.
 */
@Composable
fun PersonalLoansCardSkeleton(modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val transition = rememberInfiniteTransition(label = "personal-loans-skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "personal-loans-skeleton-alpha",
    )
    val skeletonColor = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.12f)

    AppCard(modifier = modifier.fillMaxWidth().heightIn(min = CARD_SKELETON_MIN_HEIGHT_DP.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(sp.xs)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth(0.5f).height(16.dp).clip(RoundedCornerShape(4.dp)).background(skeletonColor))
                Box(modifier = Modifier.size(width = 60.dp, height = 20.dp).clip(RoundedCornerShape(4.dp)).background(skeletonColor))
            }
            Box(modifier = Modifier.fillMaxWidth(0.4f).height(20.dp).clip(RoundedCornerShape(4.dp)).background(skeletonColor))
            Box(modifier = Modifier.fillMaxWidth(0.6f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(skeletonColor))
        }
    }
}
