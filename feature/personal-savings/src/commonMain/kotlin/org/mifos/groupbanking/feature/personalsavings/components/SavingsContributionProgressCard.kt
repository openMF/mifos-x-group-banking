/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.personalsavings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kpt.core.common.formatGrouped
import kpt.core.designsystem.theme.spacing
import org.jetbrains.compose.resources.stringResource
import org.mifos.groupbanking.feature.personalsavings.PersonalSavingsTestTags
import org.mifos.groupbanking.feature.personalsavings.generated.resources.Res
import org.mifos.groupbanking.feature.personalsavings.generated.resources.screens_personal_savings_contribution_progress_icon_cd
import org.mifos.groupbanking.feature.personalsavings.generated.resources.screens_personal_savings_contribution_progress_label
import org.mifos.groupbanking.feature.personalsavings.generated.resources.screens_personal_savings_meetings_progress_format
import org.mifos.groupbanking.feature.personalsavings.generated.resources.screens_personal_savings_this_cycle_format

/**
 * `ui.yaml#components.contribution_progress_card` — visible only when `selectedTab ==
 * GROUP_LINKED` (caller-gated, per `ui.yaml#components.contribution_progress_card.visible_when`).
 * Renders the meetings-attended progress bar plus the current cycle's group-linked balance.
 * [meetingsAttended]/[totalMeetings] are honest `0`/`0` defaults when the ViewModel has no
 * meetings/attendance API to source them from — see `PersonalSavingsViewModel.kt`'s
 * `PersonalSavingsState` class KDoc gap (4). Division-by-zero on `totalMeetings == 0` is guarded
 * (renders an empty progress bar rather than `NaN`).
 */
@Composable
fun SavingsContributionProgressCard(
    meetingsAttended: Int,
    totalMeetings: Int,
    groupLinkedBalance: Double,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val iconCd = stringResource(Res.string.screens_personal_savings_contribution_progress_icon_cd)
    val labelText = stringResource(Res.string.screens_personal_savings_contribution_progress_label)
    val meetingsText = stringResource(
        Res.string.screens_personal_savings_meetings_progress_format,
        meetingsAttended,
        totalMeetings,
    )
    val cycleText = stringResource(Res.string.screens_personal_savings_this_cycle_format, groupLinkedBalance.formatGrouped(0))
    val progress = if (totalMeetings > 0) meetingsAttended.toFloat() / totalMeetings.toFloat() else 0f

    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag(PersonalSavingsTestTags.CONTRIBUTION_PROGRESS_CARD),
    ) {
        Column(modifier = Modifier.padding(sp.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.EventAvailable,
                    contentDescription = iconCd,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = labelText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = sp.sm),
                )
            }
            Text(
                text = meetingsText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = sp.sm),
            )
            LinearProgressIndicator(
                progress = { progress },
                trackColor = MaterialTheme.colorScheme.primaryContainer,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .padding(top = sp.sm),
            )
            Text(
                text = cycleText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = sp.sm),
            )
        }
    }
}
