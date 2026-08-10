/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.loandetail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import kpt.core.common.formatGrouped
import kpt.core.designsystem.theme.spacing
import kpt.core.model.RepaymentTransaction
import kpt.feature.loandetail.LoanDetailTestTags
import kpt.feature.loandetail.generated.resources.Res
import kpt.feature.loandetail.generated.resources.screens_loan_detail_amount_kes
import kpt.feature.loandetail.generated.resources.screens_loan_detail_history_empty_message
import org.jetbrains.compose.resources.stringResource

/**
 * One repayment-history transaction row — `ui.yaml#components.transaction_row`. `txn.type` stays
 * a raw `String` per `RepaymentTransaction` KDoc (no value-set declared in `api.yaml`). See
 * API.md#screen.
 */
@Composable
fun RepaymentTransactionRow(txn: RepaymentTransaction, modifier: Modifier = Modifier, testTag: String = "") {
    val sp = MaterialTheme.spacing
    val amountText = stringResource(Res.string.screens_loan_detail_amount_kes, txn.amount.formatGrouped(0))

    Row(
        modifier = (if (testTag.isNotEmpty()) modifier.testTag(testTag) else modifier)
            .fillMaxWidth()
            .padding(vertical = sp.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            // txn.type — i18n:skip, dynamic data binding (raw wire field, no value-set per api.yaml).
            Text(text = txn.type, style = MaterialTheme.typography.bodyMedium)
            // txn.date — i18n:skip, dynamic data binding.
            Text(
                text = txn.date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(text = amountText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
}

/** `ui.yaml#components.history_list.empty_text` — shown when `repaymentHistory` is empty. See API.md#screen. */
@Composable
fun RepaymentHistoryEmptyState(modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    Text(
        text = stringResource(Res.string.screens_loan_detail_history_empty_message),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth().padding(vertical = sp.lg).testTag(LoanDetailTestTags.HISTORY_EMPTY),
    )
}
