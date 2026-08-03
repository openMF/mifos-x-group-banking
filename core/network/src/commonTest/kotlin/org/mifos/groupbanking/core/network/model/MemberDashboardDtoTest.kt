/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * TDD RED-first coverage for the personal-dashboard wire contract (`GET
 * /companion/member/dashboard`) — `idea-layer/screens/personal-dashboard/api.yaml`. See
 * API.md#dtos.
 */
class MemberDashboardDtoTest {

    // Server-parity Json config: mirrors NetworkModule's client config (ignoreUnknownKeys +
    // coerceInputValues) so these tests prove EC30 cross-version tolerance, not just happy-path
    // round-tripping.
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

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

    private val depositTxn = SavingsTransactionDto(
        id = "TXN-20260714-001",
        date = "2026-07-14",
        type = TransactionTypeDto.DEPOSIT,
        amount = 500.0,
    )

    private val accumulatingResponse = MemberDashboardResponseDto(
        memberName = "Amina Njoroge",
        myGroups = listOf(mwangazaGroupSummary, jiungeGroupSummary),
        selectedGroup = mwangazaGroupSummary,
        poolModel = SavingsMechanismDto.ACCUMULATING,
        groupLinkedSavingsBalance = 12500.0,
        individualSavingsBalance = 3200.0,
        shareOutProjection = 15800.0,
        rotationPosition = null,
        nextRecipientEta = null,
        recentTransactions = listOf(depositTxn),
    )

    // ---------- GroupSummaryDto ----------

    @Test
    fun groupSummaryDto_constructsWithAllFields() {
        assertEquals("GRP-20260509-001", mwangazaGroupSummary.groupId)
        assertEquals("Mwangaza Women's Group", mwangazaGroupSummary.name)
        assertEquals(SavingsMechanismDto.ACCUMULATING, mwangazaGroupSummary.poolModel)
    }

    @Test
    fun groupSummaryDto_serializationRoundTrips_wireFieldNamesMatch() {
        val encoded = json.encodeToString(GroupSummaryDto.serializer(), mwangazaGroupSummary)
        assertTrue(encoded.contains("\"groupId\""))
        assertTrue(encoded.contains("\"name\""))
        assertTrue(encoded.contains("\"poolModel\""))

        val decoded = json.decodeFromString(GroupSummaryDto.serializer(), encoded)
        assertEquals(mwangazaGroupSummary, decoded)
    }

    @Test
    fun groupSummaryDto_equality() {
        assertEquals(mwangazaGroupSummary.copy(), mwangazaGroupSummary.copy())
    }

    @Test
    fun groupSummaryDto_reusesSharedSavingsMechanismDto_rotatingPayoutVariant() {
        assertEquals(SavingsMechanismDto.ROTATING_PAYOUT, jiungeGroupSummary.poolModel)
    }

    // ---------- MemberDashboardResponseDto ----------

    @Test
    fun memberDashboardResponseDto_constructsWithAllFields() {
        assertEquals("Amina Njoroge", accumulatingResponse.memberName)
        assertEquals(2, accumulatingResponse.myGroups.size)
        assertEquals(mwangazaGroupSummary, accumulatingResponse.selectedGroup)
        assertEquals(SavingsMechanismDto.ACCUMULATING, accumulatingResponse.poolModel)
        assertEquals(12500.0, accumulatingResponse.groupLinkedSavingsBalance)
        assertEquals(3200.0, accumulatingResponse.individualSavingsBalance)
        assertEquals(15800.0, accumulatingResponse.shareOutProjection)
        assertNull(accumulatingResponse.rotationPosition)
        assertNull(accumulatingResponse.nextRecipientEta)
        assertEquals(listOf(depositTxn), accumulatingResponse.recentTransactions)
    }

    @Test
    fun memberDashboardResponseDto_rotatingPayoutVariant_populatesRotationFieldsInsteadOfShareOut() {
        val rotatingResponse = accumulatingResponse.copy(
            selectedGroup = jiungeGroupSummary,
            poolModel = SavingsMechanismDto.ROTATING_PAYOUT,
            shareOutProjection = null,
            rotationPosition = 3,
            nextRecipientEta = "2026-08-01",
        )
        assertNull(rotatingResponse.shareOutProjection)
        assertEquals(3, rotatingResponse.rotationPosition)
        assertEquals("2026-08-01", rotatingResponse.nextRecipientEta)
    }

    @Test
    fun memberDashboardResponseDto_nullableFieldsDefaultToNull_whenOmittedFromWirePayload() {
        val payload = """
            {
              "memberName":"Amina Njoroge",
              "myGroups":[],
              "selectedGroup":{"groupId":"GRP-20260509-001","name":"Mwangaza Women's Group","poolModel":"ACCUMULATING"},
              "poolModel":"ACCUMULATING",
              "groupLinkedSavingsBalance":0.0,
              "individualSavingsBalance":0.0,
              "recentTransactions":[]
            }
        """.trimIndent()
        val decoded = json.decodeFromString(MemberDashboardResponseDto.serializer(), payload)
        assertNull(decoded.shareOutProjection)
        assertNull(decoded.rotationPosition)
        assertNull(decoded.nextRecipientEta)
    }

    @Test
    fun memberDashboardResponseDto_recentTransactionsDefaultsToEmptyList_whenOmitted() {
        val payload = """
            {
              "memberName":"Amina Njoroge",
              "myGroups":[],
              "selectedGroup":{"groupId":"GRP-20260509-001","name":"Mwangaza Women's Group","poolModel":"ACCUMULATING"},
              "poolModel":"ACCUMULATING",
              "groupLinkedSavingsBalance":0.0,
              "individualSavingsBalance":0.0
            }
        """.trimIndent()
        val decoded = json.decodeFromString(MemberDashboardResponseDto.serializer(), payload)
        assertTrue(decoded.recentTransactions.isEmpty())
    }

    @Test
    fun memberDashboardResponseDto_serializationRoundTrips() {
        val encoded = json.encodeToString(MemberDashboardResponseDto.serializer(), accumulatingResponse)
        assertTrue(encoded.contains("\"memberName\""))
        assertTrue(encoded.contains("\"myGroups\""))
        assertTrue(encoded.contains("\"selectedGroup\""))
        assertTrue(encoded.contains("\"poolModel\""))
        assertTrue(encoded.contains("\"groupLinkedSavingsBalance\""))
        assertTrue(encoded.contains("\"individualSavingsBalance\""))
        assertTrue(encoded.contains("\"shareOutProjection\""))
        assertTrue(encoded.contains("\"rotationPosition\""))
        assertTrue(encoded.contains("\"nextRecipientEta\""))
        assertTrue(encoded.contains("\"recentTransactions\""))

        val decoded = json.decodeFromString(MemberDashboardResponseDto.serializer(), encoded)
        assertEquals(accumulatingResponse, decoded)
    }

    @Test
    fun memberDashboardResponseDto_equality() {
        assertEquals(accumulatingResponse.copy(), accumulatingResponse.copy())
    }

    @Test
    fun memberDashboardResponseDto_carriesSchemaVersion() {
        assertEquals(1, MemberDashboardResponseDto.SCHEMA_VERSION)
    }

    @Test
    fun memberDashboardResponseDto_decodesCompanionMemberDashboardShape() {
        // The literal COMP-DASH-001 GET /companion/member/dashboard response envelope.
        val payload = """
            {
              "memberName":"Amina Njoroge",
              "myGroups":[
                {"groupId":"GRP-20260509-001","name":"Mwangaza Women's Group","poolModel":"ACCUMULATING"},
                {"groupId":"GRP-2","name":"Jiunge ROSCA Circle","poolModel":"ROTATING_PAYOUT"}
              ],
              "selectedGroup":{"groupId":"GRP-20260509-001","name":"Mwangaza Women's Group","poolModel":"ACCUMULATING"},
              "poolModel":"ACCUMULATING",
              "groupLinkedSavingsBalance":12500.0,
              "individualSavingsBalance":3200.0,
              "shareOutProjection":15800.0,
              "rotationPosition":null,
              "nextRecipientEta":null,
              "recentTransactions":[
                {"id":"TXN-20260714-001","date":"2026-07-14","type":"DEPOSIT","amount":500.0}
              ]
            }
        """.trimIndent()
        val decoded = json.decodeFromString(MemberDashboardResponseDto.serializer(), payload)
        assertEquals(2, decoded.myGroups.size)
        assertEquals(SavingsMechanismDto.ROTATING_PAYOUT, decoded.myGroups[1].poolModel)
        assertEquals(1, decoded.recentTransactions.size)
        assertEquals(TransactionTypeDto.DEPOSIT, decoded.recentTransactions[0].type)
    }

    // ---------- Cross-version safety fixture (T7/EC30) ----------

    @Test
    fun memberDashboardResponseDto_toleratesServerAddedFieldAndUnknownEnumValues_oldClientNeverCrashes() {
        // Simulates a staggered rollout: the server has shipped a NEW top-level field
        // (`loanSummary`, deferred to v1.1 per docs.yaml) plus a NEW poolModel value this (old)
        // client schema does not know about. Decoding MUST succeed, never throw.
        val serverPayload = """
            {
              "memberName":"Amina Njoroge",
              "myGroups":[],
              "selectedGroup":{"groupId":"GRP-9","name":"New Model Group","poolModel":"HYBRID_POOL"},
              "poolModel":"HYBRID_POOL",
              "groupLinkedSavingsBalance":0.0,
              "individualSavingsBalance":0.0,
              "recentTransactions":[],
              "loanSummary":{"totalOutstanding":1000.0}
            }
        """.trimIndent()
        val decoded = json.decodeFromString(MemberDashboardResponseDto.serializer(), serverPayload)
        assertEquals("Amina Njoroge", decoded.memberName)
        assertEquals(SavingsMechanismDto.UNKNOWN, decoded.poolModel)
        assertEquals(SavingsMechanismDto.UNKNOWN, decoded.selectedGroup.poolModel)
    }
}
