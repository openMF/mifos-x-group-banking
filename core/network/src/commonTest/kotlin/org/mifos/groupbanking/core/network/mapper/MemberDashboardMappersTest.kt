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
import org.mifos.groupbanking.core.model.SavingsMechanism
import org.mifos.groupbanking.core.model.TransactionType
import org.mifos.groupbanking.core.network.model.GroupSummaryDto
import org.mifos.groupbanking.core.network.model.MemberDashboardResponseDto
import org.mifos.groupbanking.core.network.model.SavingsMechanismDto
import org.mifos.groupbanking.core.network.model.SavingsTransactionDto
import org.mifos.groupbanking.core.network.model.TransactionTypeDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * TDD RED-first coverage for the personal-dashboard DTO<->domain mappers (`GET
 * /companion/member/dashboard`). Every field on `MemberDashboardResponseDto` / `GroupSummaryDto`
 * declared in `MemberDashboardDto.kt` must be exercised here (RULE: all-fields-mapped).
 */
class MemberDashboardMappersTest {

    private val mwangazaGroupSummary = GroupSummaryDto(
        groupId = "GRP-20260509-001",
        name = "Mwangaza Women's Group",
        poolModel = SavingsMechanismDto.ACCUMULATING,
    )

    private val jiungeGroupSummary = GroupSummaryDto(
        groupId = "GRP-2",
        name = "Jiunge ROSCA Circle",
        poolModel = SavingsMechanismDto.ROTATING_PAYOUT,
    )

    private val depositTxnDto = SavingsTransactionDto(
        id = "TXN-20260714-001",
        date = "2026-07-14",
        type = TransactionTypeDto.DEPOSIT,
        amount = 500.0,
    )

    private val accumulatingResponseDto = MemberDashboardResponseDto(
        memberName = "Amina Njoroge",
        myGroups = listOf(mwangazaGroupSummary, jiungeGroupSummary),
        selectedGroup = mwangazaGroupSummary,
        poolModel = SavingsMechanismDto.ACCUMULATING,
        groupLinkedSavingsBalance = 12500.0,
        individualSavingsBalance = 3200.0,
        shareOutProjection = 15800.0,
        rotationPosition = null,
        nextRecipientEta = null,
        recentTransactions = listOf(depositTxnDto),
    )

    // ---------- GroupSummaryDto -> GroupSummary (all 3 fields) ----------

    @Test
    fun groupSummaryDto_toDomainModel_mapsAllFields() {
        val domain = mwangazaGroupSummary.toDomainModel()
        assertEquals("GRP-20260509-001", domain.groupId)
        assertEquals("Mwangaza Women's Group", domain.name)
        assertEquals(SavingsMechanism.ACCUMULATING, domain.poolModel)
    }

    @Test
    fun groupSummaryDto_toDomainModel_rotatingPayoutVariant_mapsThrough() {
        val domain = jiungeGroupSummary.toDomainModel()
        assertEquals(SavingsMechanism.ROTATING_PAYOUT, domain.poolModel)
    }

    // ---------- MemberDashboardResponseDto -> MemberDashboard (all 10 fields) ----------

    @Test
    fun memberDashboardResponseDto_toDomainModel_mapsAllFields() {
        val domain = accumulatingResponseDto.toDomainModel()

        assertEquals("Amina Njoroge", domain.memberName)
        assertEquals(2, domain.myGroups.size)
        assertEquals("GRP-20260509-001", domain.myGroups[0].groupId)
        assertEquals("GRP-2", domain.myGroups[1].groupId)
        assertEquals(mwangazaGroupSummary.toDomainModel(), domain.selectedGroup)
        assertEquals(SavingsMechanism.ACCUMULATING, domain.poolModel)
        assertEquals(12500.0, domain.groupLinkedSavingsBalance)
        assertEquals(3200.0, domain.individualSavingsBalance)
        assertEquals(15800.0, domain.shareOutProjection)
        assertNull(domain.rotationPosition)
        assertNull(domain.nextRecipientEta)
        assertEquals(1, domain.recentTransactions.size)
        assertEquals("TXN-20260714-001", domain.recentTransactions[0].id)
        assertEquals(LocalDate(2026, 7, 14), domain.recentTransactions[0].date)
        assertEquals(TransactionType.DEPOSIT, domain.recentTransactions[0].type)
        assertEquals(500.0, domain.recentTransactions[0].amount)
    }

    @Test
    fun memberDashboardResponseDto_toDomainModel_rotatingPayoutVariant_mapsRotationFieldsNotShareOut() {
        val rotatingDto = accumulatingResponseDto.copy(
            selectedGroup = jiungeGroupSummary,
            poolModel = SavingsMechanismDto.ROTATING_PAYOUT,
            shareOutProjection = null,
            rotationPosition = 3,
            nextRecipientEta = "2026-08-01",
        )
        val domain = rotatingDto.toDomainModel()

        assertEquals(SavingsMechanism.ROTATING_PAYOUT, domain.poolModel)
        assertNull(domain.shareOutProjection)
        assertEquals(3, domain.rotationPosition)
        assertEquals("2026-08-01", domain.nextRecipientEta)
    }

    @Test
    fun memberDashboardResponseDto_toDomainModel_emptyGroupsAndTransactions_mapToEmptyLists() {
        val empty = accumulatingResponseDto.copy(myGroups = emptyList(), recentTransactions = emptyList())
        val domain = empty.toDomainModel()
        assertEquals(emptyList(), domain.myGroups)
        assertEquals(emptyList(), domain.recentTransactions)
    }

    @Test
    fun memberDashboardResponseDto_toDomainModel_unknownPoolModelMapsToDomainUnknown() {
        val dto = accumulatingResponseDto.copy(poolModel = SavingsMechanismDto.UNKNOWN)
        assertEquals(SavingsMechanism.UNKNOWN, dto.toDomainModel().poolModel)
    }
}
