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
import org.mifos.groupbanking.core.model.SavingsTransactionType
import org.mifos.groupbanking.core.network.model.GroupSavingsSummaryDto
import org.mifos.groupbanking.core.network.model.IndividualSavingsSummaryDto
import org.mifos.groupbanking.core.network.model.MemberGroupSavingsRowDto
import org.mifos.groupbanking.core.network.model.MemberIndividualSavingsRowDto
import org.mifos.groupbanking.core.network.model.MemberSavingsDetailDto
import org.mifos.groupbanking.core.network.model.SavingsDataPointDto
import org.mifos.groupbanking.core.network.model.SavingsLedgerCurrencyDto
import org.mifos.groupbanking.core.network.model.SavingsLedgerEntryDto
import org.mifos.groupbanking.core.network.model.SavingsLedgerTransactionTypeDto
import org.mifos.groupbanking.core.network.model.SavingsMemberDto
import org.mifos.groupbanking.core.network.model.SavingsStatementEntryDto
import org.mifos.groupbanking.core.network.model.SavingsStatementTypeDto
import org.mifos.groupbanking.core.network.model.WeeklyContributionPointDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

/**
 * TDD RED-first coverage for the shared Savings DTO<->domain mappers (`SavingsMappers.kt`). Every
 * field on every DTO declared in `SavingsDto.kt` must be exercised here (RULE: all-fields-mapped).
 */
class SavingsMappersTest {

    // ---------- fineractDateComponents helper ----------

    @Test
    fun fineractDateComponents_parsesYearMonthDayArray() {
        assertEquals(LocalDate(2026, 7, 21), fineractDateComponents(listOf(2026, 7, 21)))
    }

    @Test
    fun fineractDateComponents_ignoresExtraTrailingComponents() {
        // Fineract sometimes appends hour/minute/second components — only [year, month, day] matter.
        assertEquals(LocalDate(2026, 7, 21), fineractDateComponents(listOf(2026, 7, 21, 14, 30, 0)))
    }

    @Test
    fun fineractDateComponents_throwsOnTooFewComponents() {
        assertFailsWith<IllegalArgumentException> { fineractDateComponents(listOf(2026, 7)) }
    }

    // ---------- SavingsLedgerEntryDto -> SavingsLedgerEntry (personal-savings raw ledger) ----------

    private val ledgerEntryDto = SavingsLedgerEntryDto(
        id = 9001L,
        transactionType = SavingsLedgerTransactionTypeDto(value = 1, code = "deposit", description = "Deposit"),
        date = listOf(2026, 7, 21),
        amount = 500.0,
        runningBalance = 4500.0,
        currency = SavingsLedgerCurrencyDto(code = "KES", displaySymbol = "KES"),
    )

    @Test
    fun savingsLedgerEntryDto_toDomainModel_mapsAllFields() {
        val domain = ledgerEntryDto.toDomainModel()

        assertEquals(9001L, domain.id)
        assertEquals(1, domain.type.value)
        assertEquals("deposit", domain.type.code)
        assertEquals("Deposit", domain.type.description)
        assertEquals(LocalDate(2026, 7, 21), domain.date)
        assertEquals(500.0, domain.amount)
        assertEquals(4500.0, domain.runningBalance)
        assertEquals("KES", domain.currencyCode)
        assertEquals("KES", domain.currencyDisplaySymbol)
    }

    @Test
    fun savingsLedgerEntryDtoList_toDomainModels_mapsEveryItemInOrder() {
        val second = ledgerEntryDto.copy(id = 9002L, amount = 100.0)
        val result = listOf(ledgerEntryDto, second).toDomainModels()

        assertEquals(2, result.size)
        assertEquals(9001L, result[0].id)
        assertEquals(9002L, result[1].id)
    }

    @Test
    fun savingsLedgerEntryDtoList_toDomainModels_emptyListStaysEmpty() {
        assertEquals(emptyList(), emptyList<SavingsLedgerEntryDto>().toDomainModels())
    }

    // ---------- SavingsMemberDto -> SavingsMember ----------

    @Test
    fun savingsMemberDto_toDomainModel_mapsAllFields() {
        val dto = SavingsMemberDto(memberId = "m-5001", displayName = "Amara Otieno", photoUri = "https://cdn/x.jpg")
        val domain = dto.toDomainModel()

        assertEquals("m-5001", domain.memberId)
        assertEquals("Amara Otieno", domain.displayName)
        assertEquals("https://cdn/x.jpg", domain.photoUri)
    }

    @Test
    fun savingsMemberDto_toDomainModel_nullPhotoUriStaysNull() {
        val dto = SavingsMemberDto(memberId = "m-5001", displayName = "Amara Otieno", photoUri = null)
        assertNull(dto.toDomainModel().photoUri)
    }

    // ---------- SavingsDataPointDto -> EXISTING core/model SavingsDataPoint (reused outright) ----------

    @Test
    fun savingsDataPointDto_toDomainModel_mapsBothFields() {
        val dto = SavingsDataPointDto(date = "2026-07-14", balance = 1250.0)
        val domain = dto.toDomainModel()

        assertEquals("2026-07-14", domain.date)
        assertEquals(1250.0, domain.balance)
    }

    @Test
    fun savingsDataPointDtoList_toDomainModels_mapsEveryItemInOrder() {
        val points = listOf(
            SavingsDataPointDto(date = "2026-07-07", balance = 1000.0),
            SavingsDataPointDto(date = "2026-07-14", balance = 1250.0),
        )
        val result = points.toDomainModels()
        assertEquals(2, result.size)
        assertEquals(1000.0, result[0].balance)
        assertEquals(1250.0, result[1].balance)
    }

    // ---------- SavingsStatementTypeDto -> SavingsTransactionType (every value) ----------

    @Test
    fun savingsStatementTypeDto_toDomainModel_mapsEveryKnownValue() {
        assertEquals(SavingsTransactionType.DEPOSIT, SavingsStatementTypeDto.DEPOSIT.toDomainModel())
        assertEquals(SavingsTransactionType.WITHDRAWAL, SavingsStatementTypeDto.WITHDRAWAL.toDomainModel())
        assertEquals(SavingsTransactionType.INTEREST_POSTING, SavingsStatementTypeDto.INTEREST_POSTING.toDomainModel())
        assertEquals(SavingsTransactionType.FEE_DEDUCTION, SavingsStatementTypeDto.FEE_DEDUCTION.toDomainModel())
        assertEquals(SavingsTransactionType.TRANSFER, SavingsStatementTypeDto.TRANSFER.toDomainModel())
        assertEquals(SavingsTransactionType.UNKNOWN, SavingsStatementTypeDto.UNKNOWN.toDomainModel())
    }

    // ---------- SavingsStatementEntryDto -> SavingsStatementEntry ----------

    private val statementEntryDto = SavingsStatementEntryDto(
        id = "txn-1",
        date = "2026-07-14",
        type = SavingsStatementTypeDto.DEPOSIT,
        amount = 250.0,
        runningBalance = 1250.0,
        reversed = false,
    )

    @Test
    fun savingsStatementEntryDto_toDomainModel_mapsAllFields() {
        val domain = statementEntryDto.toDomainModel()

        assertEquals("txn-1", domain.id)
        assertEquals(LocalDate(2026, 7, 14), domain.date)
        assertEquals(SavingsTransactionType.DEPOSIT, domain.type)
        assertEquals(250.0, domain.amount)
        assertEquals(1250.0, domain.runningBalance)
        assertTrue(!domain.reversed)
    }

    @Test
    fun savingsStatementEntryDto_toDomainModel_reversedTrueMapsThrough() {
        assertTrue(statementEntryDto.copy(reversed = true).toDomainModel().reversed)
    }

    @Test
    fun savingsStatementEntryDtoList_toDomainModels_emptyListStaysEmpty() {
        assertEquals(emptyList(), emptyList<SavingsStatementEntryDto>().toDomainModels())
    }

    // ---------- MemberSavingsDetailDto -> MemberSavingsDetail (composite, every field) ----------

    private val memberSavingsDetailDto = MemberSavingsDetailDto(
        member = SavingsMemberDto(memberId = "m-5001", displayName = "Amara Otieno", photoUri = null),
        savingsAccountNo = "SA-9001",
        savingsBalance = 1250.0,
        sharesHeld = 25,
        shareValue = 2500L,
        sparklineData = listOf(SavingsDataPointDto(date = "2026-07-14", balance = 1250.0)),
        transactions = listOf(statementEntryDto),
        totalTransactions = 1,
        hasNextPage = true,
    )

    @Test
    fun memberSavingsDetailDto_toDomainModel_mapsAllFields() {
        val domain = memberSavingsDetailDto.toDomainModel()

        assertEquals("m-5001", domain.member.memberId)
        assertEquals("SA-9001", domain.savingsAccountNo)
        assertEquals(1250.0, domain.savingsBalance)
        assertEquals(25, domain.sharesHeld)
        assertEquals(2500L, domain.shareValue)
        assertEquals(1, domain.sparklineData.size)
        assertEquals(1, domain.transactions.size)
        assertEquals("txn-1", domain.transactions[0].id)
        assertEquals(1, domain.totalTransactions)
        assertTrue(domain.hasNextPage)
    }

    @Test
    fun memberSavingsDetailDto_toDomainModel_fixedAmountGroupHasNullSharesFields() {
        val fixedAmountDto = memberSavingsDetailDto.copy(sharesHeld = null, shareValue = null)
        val domain = fixedAmountDto.toDomainModel()

        assertNull(domain.sharesHeld)
        assertNull(domain.shareValue)
    }

    // ---------- WeeklyContributionPointDto -> WeeklyContributionPoint ----------

    private val weeklyPointDto = WeeklyContributionPointDto(weekLabel = "Week 1", groupAmount = 5000L, individualAmount = 1200L)

    @Test
    fun weeklyContributionPointDto_toDomainModel_mapsAllFields() {
        val domain = weeklyPointDto.toDomainModel()
        assertEquals("Week 1", domain.weekLabel)
        assertEquals(5000L, domain.groupAmount)
        assertEquals(1200L, domain.individualAmount)
    }

    @Test
    fun weeklyContributionPointDtoList_toDomainModels_emptyListStaysEmpty() {
        assertEquals(emptyList(), emptyList<WeeklyContributionPointDto>().toDomainModels())
    }

    // ---------- MemberGroupSavingsRowDto -> MemberGroupSavingsRow ----------

    private val groupRowDto = MemberGroupSavingsRowDto(
        memberId = "m-5001",
        name = "Amara Otieno",
        totalContributed = 15000L,
        lastContribution = 500L,
        meetingsContributed = 12,
        sharesHeld = 30,
        shareValue = 3000L,
    )

    @Test
    fun memberGroupSavingsRowDto_toDomainModel_mapsAllFields() {
        val domain = groupRowDto.toDomainModel()

        assertEquals("m-5001", domain.memberId)
        assertEquals("Amara Otieno", domain.name)
        assertEquals(15000L, domain.totalContributed)
        assertEquals(500L, domain.lastContribution)
        assertEquals(12, domain.meetingsContributed)
        assertEquals(30, domain.sharesHeld)
        assertEquals(3000L, domain.shareValue)
    }

    @Test
    fun memberGroupSavingsRowDtoList_toDomainModels_mapsEveryItemInOrder() {
        val second = groupRowDto.copy(memberId = "m-5002", name = "Juma Kamau")
        val result = listOf(groupRowDto, second).toDomainModels()
        assertEquals(2, result.size)
        assertEquals("m-5002", result[1].memberId)
    }

    // ---------- GroupSavingsSummaryDto -> GroupSavingsSummary (composite, every field) ----------

    @Test
    fun groupSavingsSummaryDto_toDomainModel_mapsAllFields() {
        val dto = GroupSavingsSummaryDto(
            cycleTarget = 100000L,
            cycleCollected = 45000L,
            totalCollected = 320000L,
            weeklyTrend = listOf(weeklyPointDto),
            memberRows = listOf(groupRowDto),
        )
        val domain = dto.toDomainModel()

        assertEquals(100000L, domain.cycleTarget)
        assertEquals(45000L, domain.cycleCollected)
        assertEquals(320000L, domain.totalCollected)
        assertEquals(1, domain.weeklyTrend.size)
        assertEquals(1, domain.memberRows.size)
    }

    // ---------- MemberIndividualSavingsRowDto -> MemberIndividualSavingsRow ----------

    @Test
    fun memberIndividualSavingsRowDto_toDomainModel_mapsAllFieldsIncludingParsedDate() {
        val dto = MemberIndividualSavingsRowDto(
            memberId = "m-5001",
            name = "Amara Otieno",
            currentBalance = 800L,
            lastTransaction = 200L,
            lastTransactionDate = "2026-07-10",
        )
        val domain = dto.toDomainModel()

        assertEquals("m-5001", domain.memberId)
        assertEquals("Amara Otieno", domain.name)
        assertEquals(800L, domain.currentBalance)
        assertEquals(200L, domain.lastTransaction)
        assertEquals(LocalDate(2026, 7, 10), domain.lastTransactionDate)
    }

    @Test
    fun memberIndividualSavingsRowDto_toDomainModel_nullLastTransactionFieldsStayNull() {
        val dto = MemberIndividualSavingsRowDto(
            memberId = "m-5001",
            name = "Amara Otieno",
            currentBalance = 0L,
            lastTransaction = null,
            lastTransactionDate = null,
        )
        val domain = dto.toDomainModel()

        assertNull(domain.lastTransaction)
        assertNull(domain.lastTransactionDate)
    }

    // ---------- IndividualSavingsSummaryDto -> IndividualSavingsSummary (composite, every field) ----------

    @Test
    fun individualSavingsSummaryDto_toDomainModel_mapsAllFields() {
        val rowDto = MemberIndividualSavingsRowDto(
            memberId = "m-5001",
            name = "Amara Otieno",
            currentBalance = 800L,
            lastTransaction = 200L,
            lastTransactionDate = "2026-07-10",
        )
        val dto = IndividualSavingsSummaryDto(
            totalBalance = 45000L,
            weeklyTrend = listOf(weeklyPointDto),
            memberRows = listOf(rowDto),
        )
        val domain = dto.toDomainModel()

        assertEquals(45000L, domain.totalBalance)
        assertEquals(1, domain.weeklyTrend.size)
        assertEquals(1, domain.memberRows.size)
        assertEquals("m-5001", domain.memberRows[0].memberId)
    }
}
