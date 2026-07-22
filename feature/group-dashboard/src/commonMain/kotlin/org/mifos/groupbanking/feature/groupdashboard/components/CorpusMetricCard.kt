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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import kpt.core.base.designsystem.component.AppCard
import kpt.core.common.formatGrouped
import kpt.core.designsystem.theme.spacing
import org.jetbrains.compose.resources.stringResource
import org.mifos.groupbanking.core.model.GroupCorpus
import org.mifos.groupbanking.feature.groupdashboard.GroupDashboardTestTags
import org.mifos.groupbanking.feature.groupdashboard.generated.resources.Res
import org.mifos.groupbanking.feature.groupdashboard.generated.resources.screens_group_dashboard_amount_kes
import org.mifos.groupbanking.feature.groupdashboard.generated.resources.screens_group_dashboard_contributions_label
import org.mifos.groupbanking.feature.groupdashboard.generated.resources.screens_group_dashboard_corpus_blocked
import org.mifos.groupbanking.feature.groupdashboard.generated.resources.screens_group_dashboard_corpus_label
import org.mifos.groupbanking.feature.groupdashboard.generated.resources.screens_group_dashboard_loans_outstanding_label
import org.mifos.groupbanking.feature.groupdashboard.generated.resources.screens_group_dashboard_opening_balance_label
import org.mifos.groupbanking.feature.groupdashboard.generated.resources.screens_group_dashboard_shareout_projection

/**
 * ACCUMULATING-type metric card — `ui.yaml#components.corpus_card` (visible when
 * `typeConfig.pool_model != ROTATING_PAYOUT`, VSLA/SILC/ASCA/SHG/SACCO/Burial). Hero balance +
 * optional [shareOutProjection] line + optional corpus-blocked banner (`isCorpusInsufficient`) +
 * a 3-stat row (opening balance / contributions / loans outstanding). See API.md#screen.
 */
@Composable
fun CorpusMetricCard(
    corpus: GroupCorpus,
    shareOutProjection: Double?,
    isCorpusInsufficient: Boolean,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val corpusLabel = stringResource(Res.string.screens_group_dashboard_corpus_label)
    val balanceText = stringResource(Res.string.screens_group_dashboard_amount_kes, corpus.currentBalance.formatGrouped(0))
    val openingLabel = stringResource(Res.string.screens_group_dashboard_opening_balance_label)
    val openingValue = stringResource(Res.string.screens_group_dashboard_amount_kes, corpus.openingBalance.formatGrouped(0))
    val contributionsLabel = stringResource(Res.string.screens_group_dashboard_contributions_label)
    val contributionsValue = stringResource(
        Res.string.screens_group_dashboard_amount_kes,
        corpus.totalContributionsThisCycle.formatGrouped(0),
    )
    val loansOutLabel = stringResource(Res.string.screens_group_dashboard_loans_outstanding_label)
    val loansOutValue = stringResource(
        Res.string.screens_group_dashboard_amount_kes,
        corpus.totalLoansOutstanding.formatGrouped(0),
    )
    val blockedMessage = stringResource(Res.string.screens_group_dashboard_corpus_blocked)

    AppCard(
        accentColor = if (isCorpusInsufficient) MaterialTheme.colorScheme.error else null,
        modifier = modifier.fillMaxWidth().testTag(GroupDashboardTestTags.CORPUS_CARD),
    ) {
        Column {
            Text(text = corpusLabel, style = MaterialTheme.typography.titleMedium)
            Text(
                text = balanceText,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = sp.xs),
            )
            if (shareOutProjection != null) {
                Text(
                    text = stringResource(
                        Res.string.screens_group_dashboard_shareout_projection,
                        shareOutProjection.formatGrouped(0),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = sp.xs),
                )
            }
            if (isCorpusInsufficient) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(sp.xs),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = sp.md)
                        .testTag(GroupDashboardTestTags.CORPUS_BLOCKED_BANNER),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Text(
                        text = blockedMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = sp.md),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                CorpusStat(label = openingLabel, value = openingValue)
                CorpusStat(label = contributionsLabel, value = contributionsValue, valueColor = MaterialTheme.colorScheme.tertiary)
                CorpusStat(label = loansOutLabel, value = loansOutValue)
            }
        }
    }
}

@Composable
private fun CorpusStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = valueColor)
    }
}
