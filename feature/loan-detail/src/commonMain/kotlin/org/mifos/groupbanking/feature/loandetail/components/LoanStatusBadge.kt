/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.loandetail.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import kpt.core.designsystem.theme.spacing
import org.mifos.groupbanking.core.model.LoanAccountStatus

/**
 * Colour-coded status badge — `ui.yaml#components.member_header_card.content.loan_status_badge`
 * (ACTIVE bg=#C8E6C9 text=#1B5E20; OVERDUE bg=#FFCDD2 text=#B71C1C; CLOSED bg=#F5F5F5
 * text=#757575). [LoanAccountStatus.PENDING] / [LoanAccountStatus.REJECTED] /
 * [LoanAccountStatus.UNKNOWN] are not declared in `ui.yaml`'s style map (only the 3
 * loan-detail statuses are) and fall back to the neutral `surfaceVariant` pairing — mirrors
 * `feature/loan-list`'s `LoanStatusVisuals.kt` identical fallback convention; duplicated locally
 * rather than cross-module-imported since feature modules do not depend on one another (see
 * `LoanDetailScreen.kt`'s class KDoc). See API.md#screen.
 */
@Composable
fun LoanStatusBadge(status: LoanAccountStatus, modifier: Modifier = Modifier, testTag: String = "") {
    val sp = MaterialTheme.spacing
    Surface(
        color = status.detailBadgeContainerColor(),
        contentColor = status.detailBadgeContentColor(),
        shape = RoundedCornerShape(50),
        modifier = if (testTag.isNotEmpty()) modifier.testTag(testTag) else modifier,
    ) {
        // status.name — i18n:skip, dynamic enum binding (mirrors LoanListCard's identical
        // loan.status.name convention — no per-status translatable literal in ui.yaml, the badge
        // shows the raw Fineract status token).
        Text(
            text = status.name,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = sp.sm, vertical = sp.xs),
        )
    }
}

@Composable
private fun LoanAccountStatus.detailBadgeContainerColor(): Color = when (this) {
    LoanAccountStatus.ACTIVE -> Color(0xFFC8E6C9)
    LoanAccountStatus.OVERDUE -> Color(0xFFFFCDD2)
    LoanAccountStatus.CLOSED -> Color(0xFFF5F5F5)
    LoanAccountStatus.PENDING, LoanAccountStatus.REJECTED, LoanAccountStatus.UNKNOWN ->
        MaterialTheme.colorScheme.surfaceVariant
}

@Composable
private fun LoanAccountStatus.detailBadgeContentColor(): Color = when (this) {
    LoanAccountStatus.ACTIVE -> Color(0xFF1B5E20)
    LoanAccountStatus.OVERDUE -> Color(0xFFB71C1C)
    LoanAccountStatus.CLOSED -> Color(0xFF757575)
    LoanAccountStatus.PENDING, LoanAccountStatus.REJECTED, LoanAccountStatus.UNKNOWN ->
        MaterialTheme.colorScheme.onSurfaceVariant
}
