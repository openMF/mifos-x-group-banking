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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import kpt.core.base.designsystem.component.AppCard
import kpt.core.common.formatDecimal
import kpt.core.common.formatGrouped
import kpt.core.designsystem.theme.spacing
import kpt.core.model.LoanDetail
import kpt.feature.loandetail.LoanDetailTestTags
import kpt.feature.loandetail.generated.resources.Res
import kpt.feature.loandetail.generated.resources.screens_loan_detail_disbursed_label
import kpt.feature.loandetail.generated.resources.screens_loan_detail_interest_label
import kpt.feature.loandetail.generated.resources.screens_loan_detail_principal_label
import org.jetbrains.compose.resources.stringResource

/**
 * Loan header summary — `ui.yaml#components.member_header_card`. Member name, loan product,
 * principal / disbursed-date / interest-rate lines, and the colour-coded [LoanStatusBadge].
 * Mirrors `preview/content.html`'s highlighted card (`card-highlighted`). See API.md#screen.
 */
@Composable
fun LoanHeaderCard(loan: LoanDetail, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val principalText = stringResource(
        Res.string.screens_loan_detail_principal_label,
        loan.principalAmount.formatGrouped(0),
    )
    val disbursedText = stringResource(Res.string.screens_loan_detail_disbursed_label, loan.disbursedDate)
    val interestText = stringResource(
        Res.string.screens_loan_detail_interest_label,
        loan.interestRatePercent.formatDecimal(2),
    )

    AppCard(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier.fillMaxWidth().testTag(LoanDetailTestTags.HEADER_CARD),
    ) {
        Column {
            // loan.memberName — i18n:skip, dynamic data binding (member identity, not a translatable literal).
            Text(
                text = loan.memberName,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            // loan.loanProductName — i18n:skip, dynamic data binding (loan-product catalog name).
            Text(
                text = loan.loanProductName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = sp.xs),
            )
            Text(
                text = principalText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = sp.sm),
            )
            Text(
                text = disbursedText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = sp.xs),
            )
            Text(
                text = interestText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = sp.xs),
            )
            LoanStatusBadge(
                status = loan.status,
                testTag = LoanDetailTestTags.STATUS_BADGE,
                modifier = Modifier.padding(top = sp.md),
            )
        }
    }
}
