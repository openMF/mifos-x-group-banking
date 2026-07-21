/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.mapper

import kotlinx.datetime.LocalDate
import org.mifos.groupbanking.core.model.SavingsTransaction
import org.mifos.groupbanking.core.model.TransactionType
import org.mifos.groupbanking.core.network.model.SavingsTransactionDto
import org.mifos.groupbanking.core.network.model.TransactionTypeDto
import kotlin.jvm.JvmName

/**
 * DTO -> domain mappers for the canonical `SavingsTransactionDto` shape. Every field on
 * `SavingsTransactionDto` declared in `SavingsTransactionDto.kt` is mapped — no field left
 * unmapped.
 */

fun SavingsTransactionDto.toDomainModel(): SavingsTransaction = SavingsTransaction(
    id = id,
    date = LocalDate.parse(date),
    type = type.toDomainModel(),
    amount = amount,
)

/** Batch converter — maps every transaction row in declaration order. */
@JvmName("savingsTransactionDtoListToDomainModels")
fun List<SavingsTransactionDto>.toDomainModels(): List<SavingsTransaction> = map { it.toDomainModel() }

fun TransactionTypeDto.toDomainModel(): TransactionType = when (this) {
    TransactionTypeDto.DEPOSIT -> TransactionType.DEPOSIT
    TransactionTypeDto.WITHDRAWAL -> TransactionType.WITHDRAWAL
    TransactionTypeDto.UNKNOWN -> TransactionType.UNKNOWN
}
