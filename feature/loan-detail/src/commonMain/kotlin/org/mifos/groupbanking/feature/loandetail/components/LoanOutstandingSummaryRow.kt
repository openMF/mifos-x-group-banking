/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.loandetail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import kpt.core.common.formatGrouped
import kpt.core.designsystem.theme.spacing
import org.jetbrains.compose.resources.stringResource
import org.mifos.groupbanking.core.model.LoanDetail
import org.mifos.groupbanking.feature.loandetail.LoanDetailTestTags
import org.mifos.groupbanking.feature.loandetail.generated.resources.Res
import org.mifos.groupbanking.feature.loandetail.generated.resources.screens_loan_detail_amount_kes
import org.mifos.groupbanking.feature.loandetail.generated.resources.screens_loan_detail_outstanding_label
import org.mifos.groupbanking.feature.loandetail.generated.resources.screens_loan_detail_overdue_label

/**
 * Outstanding / overdue info-chip row — `ui.yaml#components.outstanding_summary_row`. The overdue
 * chip only renders `visible_when: "loan.totalOverdue > 0"`. Mirrors `preview/content.html`'s
 * `card-group` summary-card pair. See API.md#screen.
 */
@Composable
fun LoanOutstandingSummaryRow(loan: LoanDetail, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val outstandingLabel = stringResource(Res.string.screens_loan_detail_outstanding_label)
    val outstandingValue = stringResource(
        Res.string.screens_loan_detail_amount_kes,
        loan.totalOutstanding.formatGrouped(0),
    )
    val overdueLabel = stringResource(Res.string.screens_loan_detail_overdue_label)
    val overdueValue = stringResource(Res.string.screens_loan_detail_amount_kes, loan.totalOverdue.formatGrouped(0))

    Row(
        modifier = modifier.fillMaxWidth().testTag(LoanDetailTestTags.OUTSTANDING_SUMMARY_ROW),
        horizontalArrangement = Arrangement.spacedBy(sp.sm),
    ) {
        LoanSummaryChip(
            label = outstandingLabel,
            value = outstandingValue,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            testTag = LoanDetailTestTags.OUTSTANDING_CHIP,
            modifier = Modifier.weight(1f),
        )
        if (loan.totalOverdue > 0) {
            LoanSummaryChip(
                label = overdueLabel,
                value = overdueValue,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                testTag = LoanDetailTestTags.OVERDUE_CHIP,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LoanSummaryChip(
    label: String,
    value: String,
    containerColor: Color,
    contentColor: Color,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(sp.sm),
        modifier = modifier.testTag(testTag),
    ) {
        Column(modifier = Modifier.padding(sp.md)) {
            Text(text = label, style = MaterialTheme.typography.labelSmall)
            Text(text = value, style = MaterialTheme.typography.titleMedium)
        }
    }
}
