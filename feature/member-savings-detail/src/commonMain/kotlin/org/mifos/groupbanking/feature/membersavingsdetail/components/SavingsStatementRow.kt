/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.membersavingsdetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kpt.core.common.formatGrouped
import kpt.core.designsystem.theme.spacing
import org.jetbrains.compose.resources.stringResource
import org.mifos.groupbanking.core.model.SavingsStatementEntry
import org.mifos.groupbanking.core.model.SavingsTransactionType
import org.mifos.groupbanking.feature.membersavingsdetail.generated.resources.Res
import org.mifos.groupbanking.feature.membersavingsdetail.generated.resources.screens_member_savings_detail_amount_default_format
import org.mifos.groupbanking.feature.membersavingsdetail.generated.resources.screens_member_savings_detail_amount_deposit_format
import org.mifos.groupbanking.feature.membersavingsdetail.generated.resources.screens_member_savings_detail_amount_withdrawal_format
import org.mifos.groupbanking.feature.membersavingsdetail.generated.resources.screens_member_savings_detail_reversed_label
import org.mifos.groupbanking.feature.membersavingsdetail.generated.resources.screens_member_savings_detail_running_balance_format
import org.mifos.groupbanking.feature.membersavingsdetail.generated.resources.screens_member_savings_detail_txn_expanded_balance_format
import org.mifos.groupbanking.feature.membersavingsdetail.generated.resources.screens_member_savings_detail_txn_expanded_date_format
import org.mifos.groupbanking.feature.membersavingsdetail.generated.resources.screens_member_savings_detail_txn_expanded_id_format
import org.mifos.groupbanking.feature.membersavingsdetail.generated.resources.screens_member_savings_detail_txn_row_cd_format
import org.mifos.groupbanking.feature.membersavingsdetail.generated.resources.screens_member_savings_detail_txn_type_deposit
import org.mifos.groupbanking.feature.membersavingsdetail.generated.resources.screens_member_savings_detail_txn_type_fee
import org.mifos.groupbanking.feature.membersavingsdetail.generated.resources.screens_member_savings_detail_txn_type_interest
import org.mifos.groupbanking.feature.membersavingsdetail.generated.resources.screens_member_savings_detail_txn_type_transfer
import org.mifos.groupbanking.feature.membersavingsdetail.generated.resources.screens_member_savings_detail_txn_type_unknown
import org.mifos.groupbanking.feature.membersavingsdetail.generated.resources.screens_member_savings_detail_txn_type_withdrawal

private val DEPOSIT_COLOR = Color(0xFF1B5E20)
private val WITHDRAWAL_COLOR = Color(0xFFB71C1C)

/**
 * `ui.yaml#components.transaction_card` — one row of the paginated savings statement. Leading
 * colour-coded icon (per [SavingsTransactionType]) + headline (type label) + supporting date +
 * trailing signed amount + running balance + `REVERSED` badge (`visible_when: transaction.reversed`).
 * Tapping the row dispatches [onClick] (`MemberSavingsDetailAction.OnTransactionSelected`), which
 * toggles [isExpanded] in the parent's state — revealing an in-place detail block below the main
 * row.
 *
 * **Expanded detail — documented field gap, not fabricated data:**
 * `ui.yaml#components.transaction_card.on_click.action_contract.description` names "meeting
 * reference" as part of the expanded receipt, but [SavingsStatementEntry] carries no matching
 * field (`id`/`date`/`type`/`amount`/`runningBalance`/`reversed` only — `Savings.kt`). The expanded
 * block below therefore surfaces only the real fields (transaction id, posting date, running
 * balance) — flagged for the cross-feature repair station (CFF1) rather than inventing a
 * `meetingReference` value.
 *
 * See API.md#screen.
 */
@Composable
fun SavingsStatementRow(
    entry: SavingsStatementEntry,
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "",
    expandedTestTag: String = "",
) {
    val sp = MaterialTheme.spacing
    val typeLabel = entry.type.toTypeLabel()
    val iconCd = typeLabel
    val amountText = when (entry.type) {
        SavingsTransactionType.DEPOSIT -> stringResource(Res.string.screens_member_savings_detail_amount_deposit_format, entry.amount.formatGrouped(0))
        SavingsTransactionType.WITHDRAWAL -> stringResource(Res.string.screens_member_savings_detail_amount_withdrawal_format, entry.amount.formatGrouped(0))
        SavingsTransactionType.INTEREST_POSTING,
        SavingsTransactionType.FEE_DEDUCTION,
        SavingsTransactionType.TRANSFER,
        SavingsTransactionType.UNKNOWN,
        -> stringResource(Res.string.screens_member_savings_detail_amount_default_format, entry.amount.formatGrouped(0))
    }
    val balanceText = stringResource(Res.string.screens_member_savings_detail_running_balance_format, entry.runningBalance.formatGrouped(0))
    val reversedLabel = stringResource(Res.string.screens_member_savings_detail_reversed_label)
    val dateText = entry.date.toString() // i18n:skip — locale-aware date formatting is out of scope for this pass
    val rowCd = stringResource(Res.string.screens_member_savings_detail_txn_row_cd_format, typeLabel, entry.amount.formatGrouped(0), dateText)
    val amountColor = entry.type.toAmountColor()

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth().let { if (testTag.isNotEmpty()) it.testTag(testTag) else it },
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = sp.touchTargetMin)
                    .clickable(onClick = onClick)
                    .padding(vertical = sp.sm)
                    .semantics(mergeDescendants = true) { contentDescription = rowCd },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(sp.md)) {
                    Box(
                        modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(imageVector = entry.type.toIcon(), contentDescription = iconCd, tint = amountColor)
                    }
                    Column {
                        Text(text = typeLabel, style = MaterialTheme.typography.bodyMedium)
                        Text(text = dateText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = amountText, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = amountColor)
                    Text(text = balanceText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    if (entry.reversed) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(sp.xs),
                            modifier = Modifier.padding(top = sp.xs),
                        ) {
                            Text(
                                text = reversedLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = sp.xs, vertical = sp.none),
                            )
                        }
                    }
                }
            }
            if (isExpanded) {
                SavingsStatementExpandedDetail(entry = entry, expandedTestTag = expandedTestTag)
            }
            HorizontalDivider()
        }
    }
}

/** In-place expanded receipt block — see [SavingsStatementRow] KDoc "Expanded detail" note. */
@Composable
private fun SavingsStatementExpandedDetail(entry: SavingsStatementEntry, expandedTestTag: String, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val idText = stringResource(Res.string.screens_member_savings_detail_txn_expanded_id_format, entry.id)
    val dateText = stringResource(Res.string.screens_member_savings_detail_txn_expanded_date_format, entry.date.toString())
    val balanceText = stringResource(Res.string.screens_member_savings_detail_txn_expanded_balance_format, entry.runningBalance.formatGrouped(0))

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = sp.md, vertical = sp.sm)
            .let { if (expandedTestTag.isNotEmpty()) it.testTag(expandedTestTag) else it },
        verticalArrangement = Arrangement.spacedBy(sp.xs),
    ) {
        Text(text = idText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = dateText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = balanceText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** `ui.yaml#components.transaction_card.content.headline.value` — resolved via `strings.transaction_*` per the ui.yaml `i18n: skip` note. */
@Composable
private fun SavingsTransactionType.toTypeLabel(): String = when (this) {
    SavingsTransactionType.DEPOSIT -> stringResource(Res.string.screens_member_savings_detail_txn_type_deposit)
    SavingsTransactionType.WITHDRAWAL -> stringResource(Res.string.screens_member_savings_detail_txn_type_withdrawal)
    SavingsTransactionType.INTEREST_POSTING -> stringResource(Res.string.screens_member_savings_detail_txn_type_interest)
    SavingsTransactionType.FEE_DEDUCTION -> stringResource(Res.string.screens_member_savings_detail_txn_type_fee)
    SavingsTransactionType.TRANSFER -> stringResource(Res.string.screens_member_savings_detail_txn_type_transfer)
    SavingsTransactionType.UNKNOWN -> stringResource(Res.string.screens_member_savings_detail_txn_type_unknown)
}

/** `ui.yaml#components.transaction_card.content.leading.icon` map. */
private fun SavingsTransactionType.toIcon() = when (this) {
    SavingsTransactionType.DEPOSIT -> Icons.Filled.ArrowDownward
    SavingsTransactionType.WITHDRAWAL -> Icons.Filled.ArrowUpward
    SavingsTransactionType.INTEREST_POSTING -> Icons.AutoMirrored.Filled.TrendingUp
    SavingsTransactionType.FEE_DEDUCTION -> Icons.Filled.RemoveCircleOutline
    SavingsTransactionType.TRANSFER -> Icons.Filled.SwapHoriz
    SavingsTransactionType.UNKNOWN -> Icons.Filled.Receipt
}

/** `ui.yaml#components.transaction_card.content.leading.style` color map. */
@Composable
private fun SavingsTransactionType.toAmountColor(): Color = when (this) {
    SavingsTransactionType.DEPOSIT -> DEPOSIT_COLOR
    SavingsTransactionType.WITHDRAWAL -> WITHDRAWAL_COLOR
    SavingsTransactionType.INTEREST_POSTING,
    SavingsTransactionType.FEE_DEDUCTION,
    SavingsTransactionType.TRANSFER,
    SavingsTransactionType.UNKNOWN,
    -> MaterialTheme.colorScheme.onSurfaceVariant
}
