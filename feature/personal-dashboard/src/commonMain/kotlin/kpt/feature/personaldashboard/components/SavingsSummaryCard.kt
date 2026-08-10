/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.personaldashboard.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kpt.core.base.designsystem.component.AppCard
import kpt.core.common.formatGrouped
import kpt.core.designsystem.theme.spacing
import kpt.feature.personaldashboard.PersonalDashboardTestTags
import kpt.feature.personaldashboard.generated.resources.Res
import kpt.feature.personaldashboard.generated.resources.screens_personal_dashboard_amount_kes
import kpt.feature.personaldashboard.generated.resources.screens_personal_dashboard_card_cd
import kpt.feature.personaldashboard.generated.resources.screens_personal_dashboard_group_linked_amount
import kpt.feature.personaldashboard.generated.resources.screens_personal_dashboard_individual_amount
import kpt.feature.personaldashboard.generated.resources.screens_personal_dashboard_total_savings_label
import org.jetbrains.compose.resources.stringResource

/**
 * Elevated, tappable savings summary card — `ui.yaml#components.savings_summary_card`. Shows the
 * combined [groupLinkedBalance] + [individualBalance] total, plus the per-source breakdown row.
 * The whole card is a single merged-semantics ≥48dp touch target (RULE-FEATURE-A11Y-001)
 * dispatching [onClick] — [kpt.feature.personaldashboard.PersonalDashboardAction
 * .OnSavingsCardClick]. See API.md#screen.
 */
@Composable
fun SavingsSummaryCard(
    groupLinkedBalance: Double,
    individualBalance: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val total = groupLinkedBalance + individualBalance
    val cardCd = stringResource(Res.string.screens_personal_dashboard_card_cd)
    val totalLabel = stringResource(Res.string.screens_personal_dashboard_total_savings_label)
    val totalAmount = stringResource(Res.string.screens_personal_dashboard_amount_kes, total.formatGrouped(0))
    val groupLinkedText = stringResource(
        Res.string.screens_personal_dashboard_group_linked_amount,
        groupLinkedBalance.formatGrouped(0),
    )
    val individualText = stringResource(
        Res.string.screens_personal_dashboard_individual_amount,
        individualBalance.formatGrouped(0),
    )

    AppCard(
        containerColor = MaterialTheme.colorScheme.surface,
        elevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = sp.touchTargetMin)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = cardCd }
            .testTag(PersonalDashboardTestTags.SAVINGS_CARD),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(sp.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(sp.xs),
            ) {
                Icon(imageVector = Icons.Filled.Savings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = totalLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = totalAmount,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(sp.md)) {
                Text(text = groupLinkedText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                Text(text = individualText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}
