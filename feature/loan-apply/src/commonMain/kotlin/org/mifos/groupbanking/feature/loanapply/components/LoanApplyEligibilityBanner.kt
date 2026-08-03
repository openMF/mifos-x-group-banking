/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.loanapply.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kpt.core.designsystem.theme.spacing

/**
 * `ui.yaml#components.eligibility_banner` (`type: info-banner`, `visible_when: selectedMember !=
 * null`). Two-line summary: member savings balance, then the computed
 * `LoanApplyState.eligibleAmount` ceiling (`memberSavingsBalance x loanMultiplier`). See
 * API.md#screen.
 */
@Composable
fun LoanApplyEligibilityBanner(
    savingsLine: String,
    eligibleLine: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val sp = MaterialTheme.spacing
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp),
            )
            .padding(sp.md)
            .let { base ->
                if (contentDescription != null) base.semantics { this.contentDescription = contentDescription } else base
            },
    ) {
        Text(
            text = savingsLine,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            text = eligibleLine,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
