/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.loanrequest.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kpt.core.designsystem.theme.spacing

/**
 * `ui.yaml#components.savings_limit_card` (`background: tertiaryContainer`) -- two rows: the
 * member's current savings balance, then the computed maximum-borrowable ceiling
 * (`savingsBalance * loanMultiplier`) with its `loan_multiplier_hint`. Always rendered (no
 * `visible_when`). See API.md#screen.
 */
@Composable
fun LoanRequestSavingsLimitCard(
    savingsLabel: String,
    savingsValue: String,
    savingsIconContentDescription: String,
    maxLoanLabel: String,
    maxLoanValue: String,
    maxLoanHint: String,
    maxLoanIconContentDescription: String,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(16.dp),
            )
            .padding(sp.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Savings,
                contentDescription = savingsIconContentDescription,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(end = sp.sm),
            )
            Column {
                Text(
                    text = savingsLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Text(
                    text = savingsValue,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
        VSpacer(sp.sm)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.AttachMoney,
                contentDescription = maxLoanIconContentDescription,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(end = sp.sm),
            )
            Column {
                Text(
                    text = maxLoanLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = maxLoanValue,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    HSpacer(sp.xs)
                    Text(
                        text = maxLoanHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
        }
    }
}

/** Vertical-gap shorthand -- `VSpacer(sp.sm)` reads cleaner than `Spacer(Modifier.height(sp.sm))`. */
@Composable
private fun VSpacer(height: Dp) {
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(height))
}

/** Horizontal-gap shorthand used inside the max-loan value+hint row. */
@Composable
private fun HSpacer(width: Dp) {
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(width))
}
