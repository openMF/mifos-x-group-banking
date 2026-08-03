/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.model

import kotlinx.datetime.LocalDate

/**
 * Domain model for a single recent-activity row on the member dashboard — pure business shape,
 * no wire concerns. **Canonical** — intended to be the SAME `SavingsTransaction` shape reused by
 * any other feature that only needs a compact recent-activity list (see `SavingsTransactionDto.kt`
 * kdoc for the naming-collision note flagged against `personal-savings`' richer per-account ledger
 * shape).
 *
 * See API.md#models — SavingsTransaction.
 */
data class SavingsTransaction(
    val id: String,
    val date: LocalDate,
    val type: TransactionType,
    val amount: Double,
)

/**
 * Domain enum mirroring the wire `TransactionTypeDto` one-to-one (pure Kotlin — no
 * `@Serializable`). [UNKNOWN] absorbs any wire value this client build does not yet recognize.
 * See API.md#models — TransactionType.
 */
enum class TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    UNKNOWN,
}
