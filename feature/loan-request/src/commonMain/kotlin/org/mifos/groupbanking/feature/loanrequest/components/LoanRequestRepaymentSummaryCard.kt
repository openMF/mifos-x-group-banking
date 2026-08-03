/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.loanrequest.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kpt.core.designsystem.theme.spacing

/**
 * `ui.yaml#components.repayment_summary_card` (`background: surfaceVariant`,
 * `visible_when: "requestedAmount.isNotBlank() && requestedAmountError == null"`). Three rows --
 * principal, interest (group rate), and a bold total-to-repay row with a `divider_above`. See
 * API.md#screen.
 */
@Composable
fun LoanRequestRepaymentSummaryCard(
    principalLabel: String,
    principalValue: String,
    interestLabel: String,
    interestValue: String,
    totalLabel: String,
    totalValue: String,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(sp.md),
    ) {
        SummaryRow(label = principalLabel, value = principalValue)
        Spacer(sp.sm)
        SummaryRow(label = interestLabel, value = interestValue)
        Spacer(sp.sm)
        HorizontalDivider()
        Spacer(sp.sm)
        SummaryRow(label = totalLabel, value = totalValue, bold = true)
    }
}

@Composable
private fun SummaryRow(label: String, value: String, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Vertical-gap shorthand -- `Spacer(sp.sm)` reads cleaner than `Spacer(Modifier.height(sp.sm))`. */
@Composable
private fun Spacer(height: androidx.compose.ui.unit.Dp) {
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(height))
}
