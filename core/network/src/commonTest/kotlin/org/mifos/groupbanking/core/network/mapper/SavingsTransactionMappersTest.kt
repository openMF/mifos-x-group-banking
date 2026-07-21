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
import org.mifos.groupbanking.core.model.TransactionType
import org.mifos.groupbanking.core.network.model.SavingsTransactionDto
import org.mifos.groupbanking.core.network.model.TransactionTypeDto
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * TDD RED-first coverage for the canonical `SavingsTransactionDto` <-> domain mapper. Every
 * field on `SavingsTransactionDto` declared in `SavingsTransactionDto.kt` must be exercised here
 * (RULE: all-fields-mapped).
 */
class SavingsTransactionMappersTest {

    private val depositDto = SavingsTransactionDto(
        id = "TXN-20260714-001",
        date = "2026-07-14",
        type = TransactionTypeDto.DEPOSIT,
        amount = 500.0,
    )

    // ---------- SavingsTransactionDto -> SavingsTransaction (all 4 fields) ----------

    @Test
    fun savingsTransactionDto_toDomainModel_mapsAllFields() {
        val domain = depositDto.toDomainModel()

        assertEquals("TXN-20260714-001", domain.id)
        assertEquals(LocalDate(2026, 7, 14), domain.date)
        assertEquals(TransactionType.DEPOSIT, domain.type)
        assertEquals(500.0, domain.amount)
    }

    @Test
    fun savingsTransactionDto_toDomainModel_withdrawalRow_mapsThrough() {
        val withdrawalDto = depositDto.copy(id = "TXN-2", date = "2026-07-10", type = TransactionTypeDto.WITHDRAWAL, amount = 150.0)
        val domain = withdrawalDto.toDomainModel()

        assertEquals(TransactionType.WITHDRAWAL, domain.type)
        assertEquals(150.0, domain.amount)
    }

    @Test
    fun savingsTransactionDto_toDomainModel_unknownTypeMapsToDomainUnknown() {
        val dto = depositDto.copy(type = TransactionTypeDto.UNKNOWN)
        assertEquals(TransactionType.UNKNOWN, dto.toDomainModel().type)
    }

    // ---------- TransactionTypeDto mapper (every entry) ----------

    @Test
    fun transactionTypeDto_toDomainModel_mapsEveryEntry() {
        assertEquals(TransactionType.DEPOSIT, TransactionTypeDto.DEPOSIT.toDomainModel())
        assertEquals(TransactionType.WITHDRAWAL, TransactionTypeDto.WITHDRAWAL.toDomainModel())
        assertEquals(TransactionType.UNKNOWN, TransactionTypeDto.UNKNOWN.toDomainModel())
    }

    // ---------- List<SavingsTransactionDto>.toDomainModels() batch converter ----------

    @Test
    fun savingsTransactionDtoList_toDomainModels_mapsEveryItemInOrder() {
        val withdrawalDto = depositDto.copy(id = "TXN-2", type = TransactionTypeDto.WITHDRAWAL)
        val result = listOf(depositDto, withdrawalDto).toDomainModels()

        assertEquals(2, result.size)
        assertEquals("TXN-20260714-001", result[0].id)
        assertEquals("TXN-2", result[1].id)
        assertEquals(TransactionType.WITHDRAWAL, result[1].type)
    }

    @Test
    fun savingsTransactionDtoList_toDomainModels_emptyListStaysEmpty() {
        assertEquals(emptyList(), emptyList<SavingsTransactionDto>().toDomainModels())
    }
}
