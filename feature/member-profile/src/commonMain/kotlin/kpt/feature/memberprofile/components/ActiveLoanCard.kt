/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.memberprofile.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kpt.core.common.formatGrouped
import kpt.core.designsystem.theme.spacing
import kpt.core.model.ActiveLoanSummary
import kpt.feature.memberprofile.MemberProfileTestTags
import kpt.feature.memberprofile.generated.resources.Res
import kpt.feature.memberprofile.generated.resources.screens_member_profile_loan_arrears
import kpt.feature.memberprofile.generated.resources.screens_member_profile_loan_arrears_cd
import kpt.feature.memberprofile.generated.resources.screens_member_profile_loan_label
import kpt.feature.memberprofile.generated.resources.screens_member_profile_loan_outstanding
import org.jetbrains.compose.resources.stringResource

/**
 * `ui.yaml#components.active_loan_card` — visible ONLY when `accounts.activeLoan != null`
 * (guarded by the caller). Renders the product name (i18n:skip, pure template binding —
 * `ActiveLoanSummary.productName` is server-sourced free text, same convention as
 * `GroupHeaderCard`'s `group.name`), outstanding balance, and — only when [loan]'s
 * [ActiveLoanSummary.inArrears] — an error-toned arrears banner with the due date (falls back to
 * an empty due-date row when [ActiveLoanSummary.dueDate] is `null`, a confirmed wire gap per
 * `ActiveLoanSummary` KDoc). Card carries a 2dp error-coloured border when in arrears, mirroring
 * `ui.yaml#style.border.arrears`. See API.md#screen.
 */
@Composable
fun ActiveLoanCard(loan: ActiveLoanSummary, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val label = stringResource(Res.string.screens_member_profile_loan_label)
    val outstandingText = stringResource(Res.string.screens_member_profile_loan_outstanding, loan.outstandingBalance.formatGrouped(0))
    val arrearsMessage = stringResource(Res.string.screens_member_profile_loan_arrears, loan.dueDate.orEmpty())
    val arrearsIconCd = stringResource(Res.string.screens_member_profile_loan_arrears_cd)

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(sp.lg),
        border = if (loan.inArrears) BorderStroke(2.dp, MaterialTheme.colorScheme.error) else null,
        tonalElevation = sp.xs,
        modifier = modifier.fillMaxWidth().testTag(MemberProfileTestTags.LOAN_CARD),
    ) {
        Column(modifier = Modifier.padding(sp.lg)) {
            Text(text = label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            // loan.productName — i18n:skip, pure server-sourced template binding.
            Text(
                text = loan.productName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = sp.xs),
            )
            Text(
                text = outstandingText,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = sp.sm),
            )
            if (loan.inArrears) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(sp.sm),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = sp.md)
                        .testTag(MemberProfileTestTags.LOAN_ARREARS_BANNER),
                ) {
                    Row(
                        modifier = Modifier.padding(sp.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = arrearsIconCd,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = arrearsMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(start = sp.xs),
                        )
                    }
                }
            }
        }
    }
}
