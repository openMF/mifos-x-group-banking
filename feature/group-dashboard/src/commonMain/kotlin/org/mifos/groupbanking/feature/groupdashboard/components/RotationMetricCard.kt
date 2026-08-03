/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.groupdashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import kpt.core.base.designsystem.component.AppCard
import kpt.core.common.formatGrouped
import kpt.core.designsystem.theme.spacing
import org.jetbrains.compose.resources.stringResource
import org.mifos.groupbanking.core.model.GroupCorpus
import org.mifos.groupbanking.feature.groupdashboard.GroupDashboardTestTags
import org.mifos.groupbanking.feature.groupdashboard.generated.resources.Res
import org.mifos.groupbanking.feature.groupdashboard.generated.resources.screens_group_dashboard_next_payout_label
import org.mifos.groupbanking.feature.groupdashboard.generated.resources.screens_group_dashboard_next_recipient_chip
import org.mifos.groupbanking.feature.groupdashboard.generated.resources.screens_group_dashboard_pool_balance
import org.mifos.groupbanking.feature.groupdashboard.generated.resources.screens_group_dashboard_rotation_label
import org.mifos.groupbanking.feature.groupdashboard.generated.resources.screens_group_dashboard_viewer_position

/**
 * ROTATING_PAYOUT-type metric card — `ui.yaml#components.rotation_card` (visible when
 * `typeConfig.pool_model == ROTATING_PAYOUT`, ROSCA). Hero viewer-position line, next-recipient
 * row (only when both [nextRecipientName] and [nextRecipientPosition] are populated), and the
 * pooled balance line. See API.md#screen.
 */
@Composable
fun RotationMetricCard(
    corpus: GroupCorpus,
    rotationPosition: Int?,
    nextRecipientName: String?,
    nextRecipientPosition: Int?,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val label = stringResource(Res.string.screens_group_dashboard_rotation_label)
    val poolText = stringResource(Res.string.screens_group_dashboard_pool_balance, corpus.currentBalance.formatGrouped(0))

    AppCard(modifier = modifier.fillMaxWidth().testTag(GroupDashboardTestTags.ROTATION_CARD)) {
        Column {
            Text(text = label, style = MaterialTheme.typography.titleMedium)
            if (rotationPosition != null) {
                Text(
                    text = stringResource(Res.string.screens_group_dashboard_viewer_position, rotationPosition),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = sp.xs),
                )
            }
            if (nextRecipientName != null && nextRecipientPosition != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(sp.sm),
                    modifier = Modifier.padding(top = sp.md, bottom = sp.sm),
                ) {
                    Text(
                        text = stringResource(Res.string.screens_group_dashboard_next_payout_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = {
                            Text(
                                text = stringResource(
                                    Res.string.screens_group_dashboard_next_recipient_chip,
                                    nextRecipientName,
                                    nextRecipientPosition,
                                ),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            disabledLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                        border = null,
                    )
                }
            }
            Text(
                text = poolText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(top = sp.xs),
            )
        }
    }
}
