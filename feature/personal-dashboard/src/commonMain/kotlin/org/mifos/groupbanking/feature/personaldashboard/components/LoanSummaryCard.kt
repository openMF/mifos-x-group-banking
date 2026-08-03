/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.personaldashboard.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ChevronRight
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
import kpt.core.designsystem.theme.spacing
import org.jetbrains.compose.resources.stringResource
import org.mifos.groupbanking.feature.personaldashboard.PersonalDashboardTestTags
import org.mifos.groupbanking.feature.personaldashboard.generated.resources.Res
import org.mifos.groupbanking.feature.personaldashboard.generated.resources.screens_personal_dashboard_loan_card_cd
import org.mifos.groupbanking.feature.personaldashboard.generated.resources.screens_personal_dashboard_my_loans_label
import org.mifos.groupbanking.feature.personaldashboard.generated.resources.screens_personal_dashboard_my_loans_sublabel

/**
 * Elevated, tappable loan entry card — `ui.yaml#components.loan_card` (un-deferred loan tile). A
 * pure navigation affordance (icon + label + sublabel + chevron) into the member's own
 * `personal-loans` list — `MemberDashboard` carries no loan balance/summary fields, so this card
 * shows no data, only the gateway. The whole card is a single merged-semantics ≥48dp touch target
 * (RULE-FEATURE-A11Y-001) dispatching [onClick] —
 * [org.mifos.groupbanking.feature.personaldashboard.PersonalDashboardAction.OnLoansCardClick]. See
 * API.md#screen.
 */
@Composable
fun LoanSummaryCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val cardCd = stringResource(Res.string.screens_personal_dashboard_loan_card_cd)
    val label = stringResource(Res.string.screens_personal_dashboard_my_loans_label)
    val sublabel = stringResource(Res.string.screens_personal_dashboard_my_loans_sublabel)

    AppCard(
        containerColor = MaterialTheme.colorScheme.surface,
        elevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = sp.touchTargetMin)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = cardCd }
            .testTag(PersonalDashboardTestTags.LOAN_CARD),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(sp.md),
        ) {
            Icon(
                imageVector = Icons.Filled.AccountBalanceWallet,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(sp.xs),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = sublabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
