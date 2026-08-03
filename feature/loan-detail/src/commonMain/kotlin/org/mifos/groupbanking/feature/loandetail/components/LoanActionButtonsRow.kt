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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import kpt.core.designsystem.theme.spacing
import org.jetbrains.compose.resources.stringResource
import org.mifos.groupbanking.core.model.LoanAccountStatus
import org.mifos.groupbanking.core.model.LoanDetail
import org.mifos.groupbanking.feature.loandetail.LoanDetailAction
import org.mifos.groupbanking.feature.loandetail.LoanDetailTestTags
import org.mifos.groupbanking.feature.loandetail.generated.resources.Res
import org.mifos.groupbanking.feature.loandetail.generated.resources.screens_loan_detail_action_mark_defaulted
import org.mifos.groupbanking.feature.loandetail.generated.resources.screens_loan_detail_action_record_repayment

/**
 * Role-gated action row — `ui.yaml#components.action_buttons_row`. Record Repayment renders only
 * when `canRecordRepayment && loan.status == ACTIVE`; Mark Defaulted renders only when
 * `canMarkDefaulted && loan.status == OVERDUE`. Both dispatch a typed [LoanDetailAction] member —
 * no bare `onClick = {}` (RULE-IMPL-DEAD-CLICKABLE-001 Rule 1). Renders nothing when neither
 * condition holds (both flags default `false` in production today — see `LoanDetailState` KDoc
 * "canRecordRepayment / canMarkDefaulted gap"). See API.md#screen.
 */
@Composable
fun LoanActionButtonsRow(
    loan: LoanDetail,
    canRecordRepayment: Boolean,
    canMarkDefaulted: Boolean,
    onAction: (LoanDetailAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val showRecordRepayment = canRecordRepayment && loan.status == LoanAccountStatus.ACTIVE
    val showMarkDefaulted = canMarkDefaulted && loan.status == LoanAccountStatus.OVERDUE
    if (!showRecordRepayment && !showMarkDefaulted) return

    Row(
        modifier = modifier.fillMaxWidth().testTag(LoanDetailTestTags.ACTION_BUTTONS_ROW),
        horizontalArrangement = Arrangement.spacedBy(sp.md),
    ) {
        if (showRecordRepayment) {
            Button(
                onClick = { onAction(LoanDetailAction.OnRecordRepayment) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = sp.touchTargetMin)
                    .testTag(LoanDetailTestTags.RECORD_REPAYMENT_BUTTON),
            ) {
                Text(text = stringResource(Res.string.screens_loan_detail_action_record_repayment))
            }
        }
        if (showMarkDefaulted) {
            Button(
                onClick = { onAction(LoanDetailAction.OnMarkDefaulted) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = sp.touchTargetMin)
                    .testTag(LoanDetailTestTags.MARK_DEFAULTED_BUTTON),
            ) {
                Text(text = stringResource(Res.string.screens_loan_detail_action_mark_defaulted))
            }
        }
    }
}
