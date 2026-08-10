/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.personalsavings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kpt.core.common.formatGrouped
import kpt.core.designsystem.theme.spacing
import kpt.core.model.SavingsLedgerEntry
import kpt.core.model.SavingsLedgerTransactionType
import kpt.feature.personalsavings.generated.resources.Res
import kpt.feature.personalsavings.generated.resources.screens_personal_savings_txn_amount_deposit_format
import kpt.feature.personalsavings.generated.resources.screens_personal_savings_txn_amount_withdrawal_format
import kpt.feature.personalsavings.generated.resources.screens_personal_savings_txn_balance_format
import kpt.feature.personalsavings.generated.resources.screens_personal_savings_txn_deposit_icon_cd
import kpt.feature.personalsavings.generated.resources.screens_personal_savings_txn_row_cd
import kpt.feature.personalsavings.generated.resources.screens_personal_savings_txn_withdrawal_icon_cd
import org.jetbrains.compose.resources.stringResource

/**
 * `ui.yaml#components.transaction_list_item` — one row of the active tab's savings ledger. Leading
 * circular icon + headline (transaction description, `i18n: skip` — dynamic Fineract-sourced text
 * per `ui.yaml#components.transaction_list_item.content.headline.i18n: skip`) + supporting date +
 * trailing colour-coded signed amount and running balance.
 *
 * **Deposit/withdrawal colour split — documented heuristic, not fabricated data:**
 * [SavingsLedgerTransactionType] is a raw, un-typed Fineract `{value, code, description}` mirror
 * (`core/model` — Hard Rule 4, no invented enum), so there is no boolean/enum field distinguishing
 * a credit (deposit/interest) from a debit (withdrawal) transaction. [isCreditTransaction] derives
 * it from `code` containing `"deposit"` or `"interestposting"` (case-insensitive) — the two credit
 * transaction-type codes Fineract emits on this ledger — everything else renders as a debit
 * (withdrawal-styled, error tint). Flagged for RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1 if a
 * dedicated boolean is later added to [SavingsLedgerTransactionType].
 */
@Composable
fun SavingsTransactionRow(
    transaction: SavingsLedgerEntry,
    modifier: Modifier = Modifier,
    testTag: String = "",
    showDivider: Boolean = true,
) {
    val sp = MaterialTheme.spacing
    val isCredit = transaction.type.isCreditTransaction()
    val iconCd = if (isCredit) {
        stringResource(Res.string.screens_personal_savings_txn_deposit_icon_cd)
    } else {
        stringResource(Res.string.screens_personal_savings_txn_withdrawal_icon_cd)
    }
    val amountText = if (isCredit) {
        stringResource(Res.string.screens_personal_savings_txn_amount_deposit_format, transaction.amount.formatGrouped(0))
    } else {
        stringResource(Res.string.screens_personal_savings_txn_amount_withdrawal_format, transaction.amount.formatGrouped(0))
    }
    val balanceText = stringResource(Res.string.screens_personal_savings_txn_balance_format, transaction.runningBalance.formatGrouped(0))
    // transaction.date/type.description — i18n:skip, dynamic Fineract-sourced values (mirrors
    // `ui.yaml#components.transaction_list_item.content.headline/supporting.i18n: skip`).
    val dateText = transaction.date.toString()
    val rowCd = stringResource(
        Res.string.screens_personal_savings_txn_row_cd,
        transaction.type.description,
        transaction.amount.formatGrouped(0),
        dateText,
    )
    val amountColor = if (isCredit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    Column(modifier = modifier.fillMaxWidth().let { if (testTag.isNotEmpty()) it.testTag(testTag) else it }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = sp.touchTargetMin)
                .padding(vertical = sp.sm)
                .semantics(mergeDescendants = true) { contentDescription = rowCd },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(sp.md)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isCredit) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = iconCd,
                        tint = amountColor,
                    )
                }
                Column {
                    Text(text = transaction.type.description, style = MaterialTheme.typography.bodyLarge)
                    Text(text = dateText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = amountText, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = amountColor)
                Text(text = balanceText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
        if (showDivider) {
            HorizontalDivider()
        }
    }
}

/** See [SavingsTransactionRow] KDoc "documented heuristic" note. */
private fun SavingsLedgerTransactionType.isCreditTransaction(): Boolean =
    code.contains("deposit", ignoreCase = true) || code.contains("interestposting", ignoreCase = true)
