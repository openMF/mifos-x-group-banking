/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.personalloans.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.mifos.groupbanking.core.model.LoanAccountStatus

/**
 * Status-badge visual tokens for [LoanAccountStatus] on the `personal-loans` loan card —
 * duplicated (not imported) from `loan-list`'s `LoanStatusVisuals.kt`: no feature→feature
 * dependency is permitted between sibling feature modules, so each feature owns its own copy.
 * Mirrors `ui.yaml#components.loan_card.content.status_chip.style` — `active` ->
 * `primaryContainer`/`onPrimaryContainer`, `pending_approval` ->
 * `secondaryContainer`/`onSecondaryContainer`, `closed` ->
 * `surfaceVariant`/`onSurfaceVariant`, `overdue` -> `errorContainer`/`onErrorContainer` — mapped
 * onto the canonical [LoanAccountStatus] enum (`PENDING` for `pending_approval`, `OVERDUE` for
 * `overdue`). [LoanAccountStatus.REJECTED] and [LoanAccountStatus.UNKNOWN] are not declared in
 * `ui.yaml`'s style map and fall back to the neutral `surfaceVariant` pairing, mirroring
 * `loan-list`'s identical fallback convention. See API.md#screen.
 */
@Composable
fun LoanAccountStatus.personalLoansBadgeContainerColor(): Color = when (this) {
    LoanAccountStatus.ACTIVE -> MaterialTheme.colorScheme.primaryContainer
    LoanAccountStatus.OVERDUE -> MaterialTheme.colorScheme.errorContainer
    LoanAccountStatus.CLOSED -> MaterialTheme.colorScheme.surfaceVariant
    LoanAccountStatus.PENDING -> MaterialTheme.colorScheme.secondaryContainer
    LoanAccountStatus.REJECTED, LoanAccountStatus.UNKNOWN -> MaterialTheme.colorScheme.surfaceVariant
}

@Composable
fun LoanAccountStatus.personalLoansBadgeContentColor(): Color = when (this) {
    LoanAccountStatus.ACTIVE -> MaterialTheme.colorScheme.onPrimaryContainer
    LoanAccountStatus.OVERDUE -> MaterialTheme.colorScheme.onErrorContainer
    LoanAccountStatus.CLOSED -> MaterialTheme.colorScheme.onSurfaceVariant
    LoanAccountStatus.PENDING -> MaterialTheme.colorScheme.onSecondaryContainer
    LoanAccountStatus.REJECTED, LoanAccountStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
}
