/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.loanlist.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import kpt.core.model.LoanAccountStatus

/**
 * Status-badge visual tokens for [LoanAccountStatus] — mirrors `ui.yaml#components.loan_card
 * .content.status_badge.style` (ACTIVE bg=#C8E6C9 text=#1B5E20; OVERDUE bg=#FFCDD2 text=#B71C1C;
 * CLOSED bg=#F5F5F5 text=#757575; PENDING bg=#FFF9C4 text=#E65100). [LoanAccountStatus.REJECTED]
 * and [LoanAccountStatus.UNKNOWN] are not declared in `ui.yaml`'s style map (only the 4 filter-chip
 * statuses are) and fall back to the neutral `surfaceVariant` pairing — mirrors
 * `HealthIndicator.UNKNOWN`'s identical fallback convention on `group-list`
 * (`HealthIndicatorVisuals.kt`). See API.md#screen.
 */
@Composable
fun LoanAccountStatus.badgeContainerColor(): Color = when (this) {
    LoanAccountStatus.ACTIVE -> Color(0xFFC8E6C9)
    LoanAccountStatus.OVERDUE -> Color(0xFFFFCDD2)
    LoanAccountStatus.CLOSED -> Color(0xFFF5F5F5)
    LoanAccountStatus.PENDING -> Color(0xFFFFF9C4)
    LoanAccountStatus.REJECTED, LoanAccountStatus.UNKNOWN -> MaterialTheme.colorScheme.surfaceVariant
}

@Composable
fun LoanAccountStatus.badgeContentColor(): Color = when (this) {
    LoanAccountStatus.ACTIVE -> Color(0xFF1B5E20)
    LoanAccountStatus.OVERDUE -> Color(0xFFB71C1C)
    LoanAccountStatus.CLOSED -> Color(0xFF757575)
    LoanAccountStatus.PENDING -> Color(0xFFE65100)
    LoanAccountStatus.REJECTED, LoanAccountStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
}
