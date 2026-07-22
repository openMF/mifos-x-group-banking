/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.groupdashboard.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import kpt.core.base.designsystem.component.AppCard
import kpt.core.common.formatGrouped
import kpt.core.designsystem.theme.spacing
import org.jetbrains.compose.resources.stringResource
import org.mifos.groupbanking.core.model.GroupConfig
import org.mifos.groupbanking.core.model.GroupContributionModel
import org.mifos.groupbanking.feature.groupdashboard.GroupDashboardTestTags
import org.mifos.groupbanking.feature.groupdashboard.generated.resources.Res
import org.mifos.groupbanking.feature.groupdashboard.generated.resources.screens_group_dashboard_contribution_fixed
import org.mifos.groupbanking.feature.groupdashboard.generated.resources.screens_group_dashboard_contribution_fixed_negotiated
import org.mifos.groupbanking.feature.groupdashboard.generated.resources.screens_group_dashboard_contribution_share_based
import org.mifos.groupbanking.feature.groupdashboard.generated.resources.screens_group_dashboard_savings_label
import org.mifos.groupbanking.feature.groupdashboard.generated.resources.screens_group_dashboard_total_savings

/**
 * Group-level savings summary — `ui.yaml#components.savings_summary_card`. Contribution-model
 * line is [GroupContributionModel]-adaptive (SHARE_BASED_VARIABLE / FIXED_AMOUNT /
 * FIXED_NEGOTIATED); rendered only when [config] carries the backing amount for that variant —
 * `config.shareMin`/`config.shareMax` have **no wire source in production today** (see
 * `GroupConfig`'s own KDoc "confirmed gap"), so unlike `preview/content_accumulating.html`'s
 * static "1–5 shares / meeting" mockup text this card renders the share-value-only line rather
 * than inventing a fabricated range (flagged, not invented, per RULE-IMPL-DEAD-CLICKABLE-001
 * Rule 1's sibling discipline for data fields). See API.md#screen.
 */
@Composable
fun GroupSavingsSummaryCard(
    contributionModel: GroupContributionModel?,
    config: GroupConfig?,
    totalSavings: Double,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val label = stringResource(Res.string.screens_group_dashboard_savings_label)
    val totalText = stringResource(Res.string.screens_group_dashboard_total_savings, totalSavings.formatGrouped(0))
    val modelText = when (contributionModel) {
        GroupContributionModel.SHARE_BASED_VARIABLE -> config?.shareValue?.let {
            stringResource(Res.string.screens_group_dashboard_contribution_share_based, it.formatGrouped(0))
        }

        GroupContributionModel.FIXED_AMOUNT -> config?.contributionAmount?.let {
            stringResource(Res.string.screens_group_dashboard_contribution_fixed, it.formatGrouped(0))
        }

        GroupContributionModel.FIXED_NEGOTIATED -> config?.contributionAmount?.let {
            stringResource(Res.string.screens_group_dashboard_contribution_fixed_negotiated, it.formatGrouped(0))
        }

        GroupContributionModel.UNKNOWN, null -> null
    }

    AppCard(modifier = modifier.fillMaxWidth().testTag(GroupDashboardTestTags.SAVINGS_SUMMARY_CARD)) {
        Column {
            Text(text = label, style = MaterialTheme.typography.titleMedium)
            if (modelText != null) {
                Text(
                    text = modelText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = sp.xs, bottom = sp.sm),
                )
            }
            Text(
                text = totalText,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}
