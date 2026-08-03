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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kpt.core.designsystem.theme.spacing
import org.jetbrains.compose.resources.stringResource
import org.mifos.groupbanking.feature.personalsavings.PersonalSavingsTestTags
import org.mifos.groupbanking.feature.personalsavings.generated.resources.Res
import org.mifos.groupbanking.feature.personalsavings.generated.resources.screens_personal_savings_empty_individual_body
import org.mifos.groupbanking.feature.personalsavings.generated.resources.screens_personal_savings_empty_individual_icon_cd
import org.mifos.groupbanking.feature.personalsavings.generated.resources.screens_personal_savings_empty_individual_title

/**
 * `ui.yaml#components.empty_individual_promo` — rendered when `selectedTab == INDIVIDUAL &&
 * individualSavingsId == null`. In practice this branch is unreachable via a tap gesture (the
 * Individual tab itself is omitted when `individualSavingsId == null` — see [SavingsTabRow]
 * KDoc), but it is implemented for real per the ui.yaml `visible_when` contract rather than
 * silently dropped, guarding any future deep-link/initial-state path that starts on that tab.
 */
@Composable
fun SavingsEmptyIndividualState(modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val iconCd = stringResource(Res.string.screens_personal_savings_empty_individual_icon_cd)
    val titleText = stringResource(Res.string.screens_personal_savings_empty_individual_title)
    val bodyText = stringResource(Res.string.screens_personal_savings_empty_individual_body)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag(PersonalSavingsTestTags.EMPTY_INDIVIDUAL_SECTION),
    ) {
        Column(
            modifier = Modifier.padding(sp.xl).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Filled.Savings,
                contentDescription = iconCd,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(bottom = sp.md),
            )
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center,
            )
            Text(
                text = bodyText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = sp.sm),
            )
        }
    }
}
