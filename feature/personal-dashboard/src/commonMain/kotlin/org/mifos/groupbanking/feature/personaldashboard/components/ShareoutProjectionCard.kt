/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.personaldashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kpt.core.base.designsystem.component.AppCard
import kpt.core.common.formatGrouped
import kpt.core.designsystem.theme.spacing
import org.jetbrains.compose.resources.stringResource
import org.mifos.groupbanking.feature.personaldashboard.PersonalDashboardTestTags
import org.mifos.groupbanking.feature.personaldashboard.generated.resources.Res
import org.mifos.groupbanking.feature.personaldashboard.generated.resources.screens_personal_dashboard_amount_kes
import org.mifos.groupbanking.feature.personaldashboard.generated.resources.screens_personal_dashboard_next_recipient_eta
import org.mifos.groupbanking.feature.personaldashboard.generated.resources.screens_personal_dashboard_rotation_position
import org.mifos.groupbanking.feature.personaldashboard.generated.resources.screens_personal_dashboard_rotation_position_label
import org.mifos.groupbanking.feature.personaldashboard.generated.resources.screens_personal_dashboard_shareout_label

/**
 * Pool-model-adaptive projection card — `ui.yaml#components.shareout_projection_card`. Mutually
 * exclusive per `MemberDashboard`'s KDoc: `ACCUMULATING` pool models show the KES
 * [shareOutProjection] amount ("Projected Share-Out"); `ROTATING_PAYOUT` pool models show the
 * member's [rotationPosition] in queue + [nextRecipientEta] ("Your Rotation Position"). Not
 * clickable — ui.yaml declares no `on_click` for this component. See API.md#screen.
 */
@Composable
fun ShareoutProjectionCard(
    poolModel: String,
    shareOutProjection: Double,
    rotationPosition: Int?,
    nextRecipientEta: String?,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val isRotating = poolModel == "ROTATING_PAYOUT"
    val label = stringResource(
        if (isRotating) {
            Res.string.screens_personal_dashboard_rotation_position_label
        } else {
            Res.string.screens_personal_dashboard_shareout_label
        },
    )

    AppCard(
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        elevation = 0.dp,
        modifier = modifier.fillMaxWidth().testTag(PersonalDashboardTestTags.SHAREOUT_CARD),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(sp.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isRotating) Icons.Filled.SwapHoriz else Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                if (isRotating) {
                    Text(
                        text = stringResource(
                            Res.string.screens_personal_dashboard_rotation_position,
                            rotationPosition ?: 0,
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    if (nextRecipientEta != null) {
                        Text(
                            text = stringResource(
                                Res.string.screens_personal_dashboard_next_recipient_eta,
                                nextRecipientEta,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                } else {
                    Text(
                        text = stringResource(
                            Res.string.screens_personal_dashboard_amount_kes,
                            shareOutProjection.formatGrouped(0),
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
    }
}
