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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import kpt.core.common.formatGrouped
import kpt.core.designsystem.theme.spacing
import kpt.core.model.SavingsTransaction
import kpt.core.model.TransactionType
import kpt.feature.personaldashboard.PersonalDashboardTestTags
import kpt.feature.personaldashboard.generated.resources.Res
import kpt.feature.personaldashboard.generated.resources.screens_personal_dashboard_transaction_deposit_amount
import kpt.feature.personaldashboard.generated.resources.screens_personal_dashboard_transaction_withdrawal_amount
import org.jetbrains.compose.resources.stringResource

/**
 * One recent-activity row — `ui.yaml#components.recent_activity_list`. Leading icon + amount
 * color-code DEPOSIT (primary, download arrow) vs WITHDRAWAL (error, upload arrow); supporting
 * text is dynamic transaction-type + formatted-date, not user-facing copy (ui.yaml marks it
 * `// i18n:skip` — see `ui.yaml#components.recent_activity_list.content.supporting`). Not
 * clickable — ui.yaml declares no `on_click` for this component. See API.md#screen.
 */
@Composable
fun RecentActivityRow(transaction: SavingsTransaction, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val isDeposit = transaction.type == TransactionType.DEPOSIT
    val amountText = if (isDeposit) {
        stringResource(Res.string.screens_personal_dashboard_transaction_deposit_amount, transaction.amount.formatGrouped(0))
    } else {
        stringResource(Res.string.screens_personal_dashboard_transaction_withdrawal_amount, transaction.amount.formatGrouped(0))
    }
    // i18n:skip — dynamic transaction type + formatted date, not translatable copy (mirrors
    // ui.yaml#components.recent_activity_list.content.supporting's own `// i18n: skip` note).
    val supportingText = "${transaction.type.name} · ${transaction.date}"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = sp.touchTargetMin)
            .testTag(PersonalDashboardTestTags.transactionTag(transaction.id)),
        horizontalArrangement = Arrangement.spacedBy(sp.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (isDeposit) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
            contentDescription = null,
            tint = if (isDeposit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
        Column {
            Text(text = amountText, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
