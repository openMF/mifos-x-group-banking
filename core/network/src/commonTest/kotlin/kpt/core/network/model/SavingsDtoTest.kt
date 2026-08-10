/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.network.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * TDD RED-first coverage for the shared Savings wire contract (`SavingsDto.kt`) — built once for
 * personal-savings, member-savings-detail, and savings-dashboard. See API.md#dtos.
 */
class SavingsDtoTest {

    // Server-parity Json config: mirrors NetworkModule's client config (ignoreUnknownKeys +
    // coerceInputValues) so these tests prove EC30 cross-version tolerance, not just
    // happy-path round-tripping.
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    // ---------- SavingsLedgerEntryDto / SavingsLedgerTransactionTypeDto / SavingsLedgerCurrencyDto ----------

    private val ledgerTypeDto = SavingsLedgerTransactionTypeDto(
        value = 1,
        code = "deposit",
        description = "Deposit",
    )

    private val currencyDto = SavingsLedgerCurrencyDto(code = "KES", displaySymbol = "KES")

    private val ledgerEntryDto = SavingsLedgerEntryDto(
        id = 9001L,
        transactionType = ledgerTypeDto,
        date = listOf(2026, 7, 21),
        amount = 500.0,
        runningBalance = 4500.0,
        currency = currencyDto,
    )

    @Test
    fun savingsLedgerEntryDto_constructsWithAllFields() {
        assertEquals(9001L, ledgerEntryDto.id)
        assertEquals("deposit", ledgerEntryDto.transactionType.code)
        assertEquals(listOf(2026, 7, 21), ledgerEntryDto.date)
        assertEquals(500.0, ledgerEntryDto.amount)
        assertEquals(4500.0, ledgerEntryDto.runningBalance)
        assertEquals("KES", ledgerEntryDto.currency.code)
    }

    @Test
    fun savingsLedgerEntryDto_equality() {
        assertEquals(ledgerEntryDto.copy(), ledgerEntryDto.copy())
    }

    @Test
    fun savingsLedgerEntryDto_carriesSchemaVersion() {
        assertEquals(1, SavingsLedgerEntryDto.SCHEMA_VERSION)
    }

    @Test
    fun savingsLedgerEntryDto_serializationRoundTrips() {
        val encoded = json.encodeToString(SavingsLedgerEntryDto.serializer(), ledgerEntryDto)
        assertTrue(encoded.contains("\"transactionType\""))
        assertTrue(encoded.contains("\"runningBalance\""))
        val decoded = json.decodeFromString(SavingsLedgerEntryDto.serializer(), encoded)
        assertEquals(ledgerEntryDto, decoded)
    }

    @Test
    fun savingsLedgerEntryDto_decodesFromLiteralFineractSelfServiceShape() {
        val payload = """
            {
              "id":9001,
              "transactionType":{"value":1,"code":"deposit","description":"Deposit"},
              "date":[2026,7,21],
              "amount":500.0,
              "runningBalance":4500.0,
              "currency":{"code":"KES","displaySymbol":"KES"}
            }
        """.trimIndent()
        val decoded = json.decodeFromString(SavingsLedgerEntryDto.serializer(), payload)
        assertEquals(ledgerEntryDto, decoded)
    }

    // ---------- SavingsMemberDto ----------

    private val savingsMemberDto = SavingsMemberDto(
        memberId = "m-5001",
        displayName = "Amara Otieno",
        photoUri = null,
    )

    @Test
    fun savingsMemberDto_constructsWithAllFields() {
        assertEquals("m-5001", savingsMemberDto.memberId)
        assertEquals("Amara Otieno", savingsMemberDto.displayName)
        assertNull(savingsMemberDto.photoUri)
    }

    @Test
    fun savingsMemberDto_equality() {
        assertEquals(savingsMemberDto.copy(), savingsMemberDto.copy())
    }

    @Test
    fun savingsMemberDto_photoUriDefaultsToNullWhenAbsentFromPayload() {
        val decoded = json.decodeFromString(
            SavingsMemberDto.serializer(),
            """{"memberId":"m-5001","displayName":"Amara Otieno"}""",
        )
        assertNull(decoded.photoUri)
    }

    // ---------- SavingsDataPointDto ----------

    private val dataPointDto = SavingsDataPointDto(date = "2026-07-14", balance = 1250.0)

    @Test
    fun savingsDataPointDto_constructsWithAllFields() {
        assertEquals("2026-07-14", dataPointDto.date)
        assertEquals(1250.0, dataPointDto.balance)
    }

    @Test
    fun savingsDataPointDto_equality() {
        assertEquals(dataPointDto.copy(), dataPointDto.copy())
    }

    @Test
    fun savingsDataPointDto_serializationRoundTrips() {
        val encoded = json.encodeToString(SavingsDataPointDto.serializer(), dataPointDto)
        val decoded = json.decodeFromString(SavingsDataPointDto.serializer(), encoded)
        assertEquals(dataPointDto, decoded)
    }

    // ---------- SavingsStatementTypeDto (unknown-fallback enum, T7/EC30) ----------

    @Test
    fun savingsStatementTypeDto_hasExactlySixEntriesIncludingUnknownFallback() {
        assertEquals(6, SavingsStatementTypeDto.entries.size)
        assertTrue(SavingsStatementTypeDto.entries.contains(SavingsStatementTypeDto.UNKNOWN))
    }

    @Test
    fun savingsStatementTypeDto_decodesEachKnownWireValue() {
        val known = listOf(
            "DEPOSIT" to SavingsStatementTypeDto.DEPOSIT,
            "WITHDRAWAL" to SavingsStatementTypeDto.WITHDRAWAL,
            "INTEREST_POSTING" to SavingsStatementTypeDto.INTEREST_POSTING,
            "FEE_DEDUCTION" to SavingsStatementTypeDto.FEE_DEDUCTION,
            "TRANSFER" to SavingsStatementTypeDto.TRANSFER,
        )
        known.forEach { (wire, expected) ->
            assertEquals(expected, json.decodeFromString(SavingsStatementTypeDto.serializer(), "\"$wire\""))
        }
    }

    @Test
    fun savingsStatementTypeDto_unknownServerValueCoercesToUnknownFallback_notCrash() {
        // Proves the unknownFallbackEnumSerializer works even for a bare top-level decode of a
        // value this schema has never modeled — the T7/EC30 cross-version-safety contract.
        val decoded = json.decodeFromString(SavingsStatementTypeDto.serializer(), "\"CHARGE_WAIVER\"")
        assertEquals(SavingsStatementTypeDto.UNKNOWN, decoded)
    }

    // ---------- SavingsStatementEntryDto ----------

    private val statementEntryDto = SavingsStatementEntryDto(
        id = "txn-1",
        date = "2026-07-14",
        type = SavingsStatementTypeDto.DEPOSIT,
        amount = 250.0,
        runningBalance = 1250.0,
        reversed = false,
    )

    @Test
    fun savingsStatementEntryDto_constructsWithAllFields() {
        assertEquals("txn-1", statementEntryDto.id)
        assertEquals("2026-07-14", statementEntryDto.date)
        assertEquals(SavingsStatementTypeDto.DEPOSIT, statementEntryDto.type)
        assertEquals(250.0, statementEntryDto.amount)
        assertEquals(1250.0, statementEntryDto.runningBalance)
        assertTrue(!statementEntryDto.reversed)
    }

    @Test
    fun savingsStatementEntryDto_equality() {
        assertEquals(statementEntryDto.copy(), statementEntryDto.copy())
    }

    @Test
    fun savingsStatementEntryDto_carriesSchemaVersion() {
        assertEquals(1, SavingsStatementEntryDto.SCHEMA_VERSION)
    }

    @Test
    fun savingsStatementEntryDto_typeDefaultsToUnknownWhenAbsentFromPayload() {
        val decoded = json.decodeFromString(
            SavingsStatementEntryDto.serializer(),
            """{"id":"txn-1","date":"2026-07-14","amount":250.0,"runningBalance":1250.0,"reversed":false}""",
        )
        assertEquals(SavingsStatementTypeDto.UNKNOWN, decoded.type)
    }

    @Test
    fun savingsStatementEntryDto_serializationRoundTrips() {
        val encoded = json.encodeToString(SavingsStatementEntryDto.serializer(), statementEntryDto)
        val decoded = json.decodeFromString(SavingsStatementEntryDto.serializer(), encoded)
        assertEquals(statementEntryDto, decoded)
    }

    // ---------- MemberSavingsDetailDto ----------

    private val memberSavingsDetailDto = MemberSavingsDetailDto(
        member = savingsMemberDto,
        savingsAccountNo = "SA-9001",
        savingsBalance = 1250.0,
        sharesHeld = 25,
        shareValue = 2500L,
        sparklineData = listOf(dataPointDto),
        transactions = listOf(statementEntryDto),
        totalTransactions = 1,
        hasNextPage = false,
    )

    @Test
    fun memberSavingsDetailDto_constructsWithAllFields() {
        assertEquals("m-5001", memberSavingsDetailDto.member.memberId)
        assertEquals("SA-9001", memberSavingsDetailDto.savingsAccountNo)
        assertEquals(1250.0, memberSavingsDetailDto.savingsBalance)
        assertEquals(25, memberSavingsDetailDto.sharesHeld)
        assertEquals(2500L, memberSavingsDetailDto.shareValue)
        assertEquals(1, memberSavingsDetailDto.sparklineData.size)
        assertEquals(1, memberSavingsDetailDto.transactions.size)
        assertEquals(1, memberSavingsDetailDto.totalTransactions)
        assertTrue(!memberSavingsDetailDto.hasNextPage)
    }

    @Test
    fun memberSavingsDetailDto_equality() {
        assertEquals(memberSavingsDetailDto.copy(), memberSavingsDetailDto.copy())
    }

    @Test
    fun memberSavingsDetailDto_carriesSchemaVersion() {
        assertEquals(1, MemberSavingsDetailDto.SCHEMA_VERSION)
    }

    @Test
    fun memberSavingsDetailDto_sharesFieldsDefaultToNull_fixedAmountGroupShape() {
        // FIXED_AMOUNT (ROSCA/SHG) groups omit sharesHeld/shareValue entirely.
        val decoded = json.decodeFromString(
            MemberSavingsDetailDto.serializer(),
            """
                {
                  "member":{"memberId":"m-5001","displayName":"Amara Otieno"},
                  "savingsAccountNo":"SA-9001",
                  "savingsBalance":1250.0,
                  "sparklineData":[],
                  "transactions":[],
                  "totalTransactions":0,
                  "hasNextPage":false
                }
            """.trimIndent(),
        )
        assertNull(decoded.sharesHeld)
        assertNull(decoded.shareValue)
    }

    @Test
    fun memberSavingsDetailDto_sparklineAndTransactionsDefaultToEmptyList() {
        val decoded = json.decodeFromString(
            MemberSavingsDetailDto.serializer(),
            """{"member":{"memberId":"m-5001","displayName":"Amara Otieno"},"savingsAccountNo":"SA-9001","savingsBalance":0.0,"totalTransactions":0,"hasNextPage":false}""",
        )
        assertTrue(decoded.sparklineData.isEmpty())
        assertTrue(decoded.transactions.isEmpty())
    }

    // ---------- WeeklyContributionPointDto ----------

    private val weeklyPointDto = WeeklyContributionPointDto(
        weekLabel = "Week 1",
        groupAmount = 5000L,
        individualAmount = 1200L,
    )

    @Test
    fun weeklyContributionPointDto_constructsWithAllFields() {
        assertEquals("Week 1", weeklyPointDto.weekLabel)
        assertEquals(5000L, weeklyPointDto.groupAmount)
        assertEquals(1200L, weeklyPointDto.individualAmount)
    }

    @Test
    fun weeklyContributionPointDto_equality() {
        assertEquals(weeklyPointDto.copy(), weeklyPointDto.copy())
    }

    @Test
    fun weeklyContributionPointDto_serializationRoundTrips() {
        val encoded = json.encodeToString(WeeklyContributionPointDto.serializer(), weeklyPointDto)
        val decoded = json.decodeFromString(WeeklyContributionPointDto.serializer(), encoded)
        assertEquals(weeklyPointDto, decoded)
    }

    // ---------- MemberGroupSavingsRowDto ----------

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
    fun memberGroupSavingsRowDto_constructsWithAllFields() {
        assertEquals("m-5001", groupRowDto.memberId)
        assertEquals("Amara Otieno", groupRowDto.name)
        assertEquals(15000L, groupRowDto.totalContributed)
        assertEquals(500L, groupRowDto.lastContribution)
        assertEquals(12, groupRowDto.meetingsContributed)
        assertEquals(30, groupRowDto.sharesHeld)
        assertEquals(3000L, groupRowDto.shareValue)
    }

    @Test
    fun memberGroupSavingsRowDto_equality() {
        assertEquals(groupRowDto.copy(), groupRowDto.copy())
    }

    @Test
    fun memberGroupSavingsRowDto_sharesFieldsDefaultToNull_fixedAmountGroupShape() {
        val decoded = json.decodeFromString(
            MemberGroupSavingsRowDto.serializer(),
            """{"memberId":"m-5001","name":"Amara Otieno","totalContributed":15000,"lastContribution":500,"meetingsContributed":12}""",
        )
        assertNull(decoded.sharesHeld)
        assertNull(decoded.shareValue)
    }

    // ---------- GroupSavingsSummaryDto ----------

    private val groupSummaryDto = GroupSavingsSummaryDto(
        cycleTarget = 100000L,
        cycleCollected = 45000L,
        totalCollected = 320000L,
        weeklyTrend = listOf(weeklyPointDto),
        memberRows = listOf(groupRowDto),
    )

    @Test
    fun groupSavingsSummaryDto_constructsWithAllFields() {
        assertEquals(100000L, groupSummaryDto.cycleTarget)
        assertEquals(45000L, groupSummaryDto.cycleCollected)
        assertEquals(320000L, groupSummaryDto.totalCollected)
        assertEquals(1, groupSummaryDto.weeklyTrend.size)
        assertEquals(1, groupSummaryDto.memberRows.size)
    }

    @Test
    fun groupSavingsSummaryDto_equality() {
        assertEquals(groupSummaryDto.copy(), groupSummaryDto.copy())
    }

    @Test
    fun groupSavingsSummaryDto_carriesSchemaVersion() {
        assertEquals(1, GroupSavingsSummaryDto.SCHEMA_VERSION)
    }

    @Test
    fun groupSavingsSummaryDto_weeklyTrendAndMemberRowsDefaultToEmptyList() {
        val decoded = json.decodeFromString(
            GroupSavingsSummaryDto.serializer(),
            """{"cycleTarget":100000,"cycleCollected":45000,"totalCollected":320000}""",
        )
        assertTrue(decoded.weeklyTrend.isEmpty())
        assertTrue(decoded.memberRows.isEmpty())
    }

    // ---------- MemberIndividualSavingsRowDto ----------

    private val individualRowDto = MemberIndividualSavingsRowDto(
        memberId = "m-5001",
        name = "Amara Otieno",
        currentBalance = 800L,
        lastTransaction = 200L,
        lastTransactionDate = "2026-07-10",
    )

    @Test
    fun memberIndividualSavingsRowDto_constructsWithAllFields() {
        assertEquals("m-5001", individualRowDto.memberId)
        assertEquals("Amara Otieno", individualRowDto.name)
        assertEquals(800L, individualRowDto.currentBalance)
        assertEquals(200L, individualRowDto.lastTransaction)
        assertEquals("2026-07-10", individualRowDto.lastTransactionDate)
    }

    @Test
    fun memberIndividualSavingsRowDto_equality() {
        assertEquals(individualRowDto.copy(), individualRowDto.copy())
    }

    @Test
    fun memberIndividualSavingsRowDto_lastTransactionFieldsDefaultToNull() {
        val decoded = json.decodeFromString(
            MemberIndividualSavingsRowDto.serializer(),
            """{"memberId":"m-5001","name":"Amara Otieno","currentBalance":0}""",
        )
        assertNull(decoded.lastTransaction)
        assertNull(decoded.lastTransactionDate)
    }

    // ---------- IndividualSavingsSummaryDto ----------

    private val individualSummaryDto = IndividualSavingsSummaryDto(
        totalBalance = 45000L,
        weeklyTrend = listOf(weeklyPointDto),
        memberRows = listOf(individualRowDto),
    )

    @Test
    fun individualSavingsSummaryDto_constructsWithAllFields() {
        assertEquals(45000L, individualSummaryDto.totalBalance)
        assertEquals(1, individualSummaryDto.weeklyTrend.size)
        assertEquals(1, individualSummaryDto.memberRows.size)
    }

    @Test
    fun individualSavingsSummaryDto_equality() {
        assertEquals(individualSummaryDto.copy(), individualSummaryDto.copy())
    }

    @Test
    fun individualSavingsSummaryDto_carriesSchemaVersion() {
        assertEquals(1, IndividualSavingsSummaryDto.SCHEMA_VERSION)
    }

    // ---------- Cross-version safety fixtures (T7/EC30) ----------

    @Test
    fun memberSavingsDetailDto_toleratesServerAddedField_oldClientNeverCrashes() {
        // Simulates a staggered rollout: the server has shipped a NEW field this (old) client
        // schema does not know about. Decoding MUST succeed, never throw.
        val serverPayload = """
            {
              "member":{"memberId":"m-5001","displayName":"Amara Otieno"},
              "savingsAccountNo":"SA-9001",
              "savingsBalance":1250.0,
              "totalTransactions":0,
              "hasNextPage":false,
              "interestRateApr":4.5
            }
        """.trimIndent()
        val decoded = json.decodeFromString(MemberSavingsDetailDto.serializer(), serverPayload)
        assertEquals("SA-9001", decoded.savingsAccountNo)
    }

    @Test
    fun savingsStatementEntryDto_toleratesServerAddedEnumValue_coercesToUnknown_neverCrashes() {
        // Simulates a staggered rollout: the server has shipped a NEW transaction type this (old)
        // client schema does not model. Decoding MUST succeed and coerce to UNKNOWN, never throw.
        val serverPayload = """
            {"id":"txn-9","date":"2026-07-20","type":"CHARGE_WAIVER","amount":10.0,"runningBalance":990.0,"reversed":false}
        """.trimIndent()
        val decoded = json.decodeFromString(SavingsStatementEntryDto.serializer(), serverPayload)
        assertEquals(SavingsStatementTypeDto.UNKNOWN, decoded.type)
        assertEquals("txn-9", decoded.id)
    }
}
