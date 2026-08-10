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
import kotlin.test.assertTrue

/**
 * TDD RED-first coverage for the member-profile wire contract (`GET /clients/{clientId}`,
 * `GET /clients/{clientId}/accounts`, `GET /datatables/dt_member_role/{clientId}`,
 * `PUT /datatables/dt_member_role/{clientId}`). See API.md#dtos.
 */
class MemberProfileDtoTest {

    // Server-parity Json config: mirrors NetworkModule's client config (ignoreUnknownKeys +
    // coerceInputValues) so these tests prove EC30 cross-version tolerance, not just happy-path
    // round-tripping.
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val activeStatusDto = FineractStatusDto(id = 300, value = "Active")

    private val amaraProfileDto = MemberProfileDto(
        id = 5001L,
        displayName = "Amara Otieno",
        firstName = "Amara",
        lastName = "Otieno",
        mobileNo = "+254712345678",
        imagePresent = true,
        status = activeStatusDto,
        activationDate = "2025-03-14",
        officeId = 1L,
    )

    // ---------- MemberProfileDto (get_client) ----------

    @Test
    fun memberProfileDto_constructsWithAllFields() {
        assertEquals(5001L, amaraProfileDto.id)
        assertEquals("Amara Otieno", amaraProfileDto.displayName)
        assertEquals("Amara", amaraProfileDto.firstName)
        assertEquals("Otieno", amaraProfileDto.lastName)
        assertEquals("+254712345678", amaraProfileDto.mobileNo)
        assertTrue(amaraProfileDto.imagePresent)
        assertEquals(activeStatusDto, amaraProfileDto.status)
        assertEquals("2025-03-14", amaraProfileDto.activationDate)
        assertEquals(1L, amaraProfileDto.officeId)
    }

    @Test
    fun memberProfileDto_equality() {
        assertEquals(amaraProfileDto.copy(), amaraProfileDto.copy())
    }

    @Test
    fun memberProfileDto_carriesSchemaVersion() {
        assertEquals(1, MemberProfileDto.SCHEMA_VERSION)
    }

    @Test
    fun memberProfileDto_serializationRoundTrips_wireFieldNamesMatchFineractLiteralCasing() {
        val encoded = json.encodeToString(MemberProfileDto.serializer(), amaraProfileDto)
        // Fineract literally returns "firstname"/"lastname" (lowercase n) — NOT "firstName".
        assertTrue(encoded.contains("\"firstname\""))
        assertTrue(encoded.contains("\"lastname\""))
        assertTrue(encoded.contains("\"mobileNo\""))
        assertTrue(encoded.contains("\"imagePresent\""))
        assertTrue(encoded.contains("\"activationDate\""))
        assertTrue(encoded.contains("\"officeId\""))

        val decoded = json.decodeFromString(MemberProfileDto.serializer(), encoded)
        assertEquals(amaraProfileDto, decoded)
    }

    @Test
    fun memberProfileDto_decodesLiteralGetClientShape() {
        val payload = """
            {
              "id": 5001,
              "displayName": "Amara Otieno",
              "firstname": "Amara",
              "lastname": "Otieno",
              "mobileNo": "+254712345678",
              "imagePresent": true,
              "status": {"id": 300, "value": "Active"},
              "activationDate": "2025-03-14",
              "officeId": 1
            }
        """.trimIndent()
        val decoded = json.decodeFromString(MemberProfileDto.serializer(), payload)
        assertEquals("Amara", decoded.firstName)
        assertEquals("Otieno", decoded.lastName)
        assertEquals(300, decoded.status.id)
        assertEquals("Active", decoded.status.value)
    }

    @Test
    fun memberProfileDto_toleratesServerAddedField_oldClientNeverCrashes() {
        val serverPayload = """
            {
              "id": 5001,
              "displayName": "Amara Otieno",
              "firstname": "Amara",
              "lastname": "Otieno",
              "mobileNo": "+254712345678",
              "imagePresent": true,
              "status": {"id": 300, "value": "Active"},
              "activationDate": "2025-03-14",
              "officeId": 1,
              "dateOfBirth": "1990-01-01"
            }
        """.trimIndent()
        val decoded = json.decodeFromString(MemberProfileDto.serializer(), serverPayload)
        assertEquals(5001L, decoded.id)
    }

    // ---------- FineractStatusDto (nested {id, value} shape, reused across the feature) ----------

    @Test
    fun fineractStatusDto_constructsWithIdAndValue() {
        assertEquals(300, activeStatusDto.id)
        assertEquals("Active", activeStatusDto.value)
    }

    @Test
    fun fineractStatusDto_equality() {
        assertEquals(FineractStatusDto(300, "Active"), FineractStatusDto(300, "Active"))
    }

    @Test
    fun fineractStatusDto_serializationRoundTrips() {
        val encoded = json.encodeToString(FineractStatusDto.serializer(), activeStatusDto)
        val decoded = json.decodeFromString(FineractStatusDto.serializer(), encoded)
        assertEquals(activeStatusDto, decoded)
    }

    // ---------- MemberAccountsDto (get_client_accounts, literal savingsAccounts/loanAccounts) ----------

    private val savingsAccountDto = MemberSavingsAccountDto(
        id = 9001L,
        productName = "VSLA Savings",
        accountNo = "SA-9001",
        balance = 1250.50,
        status = activeStatusDto,
    )

    private val loanSummaryDto = MemberLoanAccountSummaryDto(
        principalDisbursed = 500.0,
        principalOutstanding = 320.0,
        totalOverdue = 40.0,
    )

    private val loanAccountDto = MemberLoanAccountDto(
        id = 7001L,
        productName = "Group Loan",
        accountNo = "LA-7001",
        status = activeStatusDto,
        summary = loanSummaryDto,
    )

    private val accountsDto = MemberAccountsDto(
        savingsAccounts = listOf(savingsAccountDto),
        loanAccounts = listOf(loanAccountDto),
    )

    @Test
    fun memberAccountsDto_constructsWithSavingsAndLoanAccounts() {
        assertEquals(listOf(savingsAccountDto), accountsDto.savingsAccounts)
        assertEquals(listOf(loanAccountDto), accountsDto.loanAccounts)
    }

    @Test
    fun memberAccountsDto_accountListsDefaultToEmptyWhenOmitted() {
        val dto = MemberAccountsDto()
        assertTrue(dto.savingsAccounts.isEmpty())
        assertTrue(dto.loanAccounts.isEmpty())
    }

    @Test
    fun memberAccountsDto_carriesSchemaVersion() {
        assertEquals(1, MemberAccountsDto.SCHEMA_VERSION)
    }

    @Test
    fun memberAccountsDto_serializationRoundTrips() {
        val encoded = json.encodeToString(MemberAccountsDto.serializer(), accountsDto)
        assertTrue(encoded.contains("\"savingsAccounts\""))
        assertTrue(encoded.contains("\"loanAccounts\""))
        val decoded = json.decodeFromString(MemberAccountsDto.serializer(), encoded)
        assertEquals(accountsDto, decoded)
    }

    @Test
    fun memberSavingsAccountDto_constructsWithAllFields() {
        assertEquals(9001L, savingsAccountDto.id)
        assertEquals("VSLA Savings", savingsAccountDto.productName)
        assertEquals("SA-9001", savingsAccountDto.accountNo)
        assertEquals(1250.50, savingsAccountDto.balance)
        assertEquals(activeStatusDto, savingsAccountDto.status)
    }

    @Test
    fun memberLoanAccountDto_constructsWithNestedSummary() {
        assertEquals(7001L, loanAccountDto.id)
        assertEquals("Group Loan", loanAccountDto.productName)
        assertEquals("LA-7001", loanAccountDto.accountNo)
        assertEquals(activeStatusDto, loanAccountDto.status)
        assertEquals(500.0, loanAccountDto.summary!!.principalDisbursed)
        assertEquals(320.0, loanAccountDto.summary!!.principalOutstanding)
        assertEquals(40.0, loanAccountDto.summary!!.totalOverdue)
    }

    // ---------- MemberRoleInfoDto (get_member_role — datatable array item, reuses MemberRoleDto) ----------

    private val roleInfoDto = MemberRoleInfoDto(
        role = MemberRoleDto.CHAIRPERSON,
        groupId = 42L,
        assignedDate = "2025-01-05",
    )

    @Test
    fun memberRoleInfoDto_constructsWithAllFields() {
        assertEquals(MemberRoleDto.CHAIRPERSON, roleInfoDto.role)
        assertEquals(42L, roleInfoDto.groupId)
        assertEquals("2025-01-05", roleInfoDto.assignedDate)
    }

    @Test
    fun memberRoleInfoDto_roleDefaultsToUnknownWhenOmitted() {
        val dto = MemberRoleInfoDto(groupId = 42L, assignedDate = "2025-01-05")
        assertEquals(MemberRoleDto.UNKNOWN, dto.role)
    }

    @Test
    fun memberRoleInfoDto_carriesSchemaVersion() {
        assertEquals(1, MemberRoleInfoDto.SCHEMA_VERSION)
    }

    @Test
    fun memberRoleInfoDto_serializationRoundTrips() {
        val encoded = json.encodeToString(MemberRoleInfoDto.serializer(), roleInfoDto)
        val decoded = json.decodeFromString(MemberRoleInfoDto.serializer(), encoded)
        assertEquals(roleInfoDto, decoded)
    }

    @Test
    fun memberRoleInfoDtoList_decodesTheDatatableArrayResponseShape() {
        val payload = """
            [
              {"role": "CHAIRPERSON", "group_id": 42, "joined_date": "2025-01-05"},
              {"role": "TREASURER", "group_id": 42, "joined_date": "2025-02-11"}
            ]
        """.trimIndent()
        val decoded = json.decodeFromString(
            kotlinx.serialization.builtins.ListSerializer(MemberRoleInfoDto.serializer()),
            payload,
        )
        assertEquals(2, decoded.size)
        assertEquals(MemberRoleDto.TREASURER, decoded[1].role)
    }

    @Test
    fun memberRoleInfoDto_unknownServerRoleCoercesToUnknownFallback_notCrash() {
        val payload = """{"role": "AUDITOR", "group_id": 42, "joined_date": "2025-01-05"}"""
        val decoded = json.decodeFromString(MemberRoleInfoDto.serializer(), payload)
        assertEquals(MemberRoleDto.UNKNOWN, decoded.role)
    }

    // ---------- UpdateMemberRoleRequestDto / ResponseDto (PUT dt_member_role) ----------

    @Test
    fun updateMemberRoleRequestDto_constructsWithAllFields() {
        val request = UpdateMemberRoleRequestDto(
            role = MemberRoleDto.TREASURER,
            groupId = 42L,
            assignedDate = "2025-03-01",
        )
        assertEquals(MemberRoleDto.TREASURER, request.role)
        assertEquals(42L, request.groupId)
        assertEquals("2025-03-01", request.assignedDate)
    }

    @Test
    fun updateMemberRoleRequestDto_roleDefaultsToUnknownWhenOmitted() {
        val request = UpdateMemberRoleRequestDto(groupId = 42L, assignedDate = "2025-03-01")
        assertEquals(MemberRoleDto.UNKNOWN, request.role)
    }

    @Test
    fun updateMemberRoleRequestDto_serializationRoundTrips() {
        val request = UpdateMemberRoleRequestDto(
            role = MemberRoleDto.SECRETARY,
            groupId = 42L,
            assignedDate = "2025-03-01",
        )
        val encoded = json.encodeToString(UpdateMemberRoleRequestDto.serializer(), request)
        assertTrue(encoded.contains("\"role\""))
        assertTrue(encoded.contains("\"groupId\""))
        assertTrue(encoded.contains("\"assignedDate\""))
        val decoded = json.decodeFromString(UpdateMemberRoleRequestDto.serializer(), encoded)
        assertEquals(request, decoded)
    }

    @Test
    fun updateMemberRoleResponseDto_constructsWithResourceId() {
        val response = UpdateMemberRoleResponseDto(resourceId = 99L)
        assertEquals(99L, response.resourceId)
    }

    @Test
    fun updateMemberRoleResponseDto_serializationRoundTrips() {
        val response = UpdateMemberRoleResponseDto(resourceId = 99L)
        val encoded = json.encodeToString(UpdateMemberRoleResponseDto.serializer(), response)
        assertTrue(encoded.contains("\"resourceId\""))
        val decoded = json.decodeFromString(UpdateMemberRoleResponseDto.serializer(), encoded)
        assertEquals(response, decoded)
    }

    @Test
    fun updateMemberRoleResponseDto_carriesSchemaVersion() {
        assertEquals(1, UpdateMemberRoleResponseDto.SCHEMA_VERSION)
    }

    @Test
    fun updateMemberRoleRequestDto_carriesSchemaVersion() {
        assertEquals(1, UpdateMemberRoleRequestDto.SCHEMA_VERSION)
    }

    @Test
    fun memberProfileDto_imagePresentFalse_isPreserved() {
        val dto = amaraProfileDto.copy(imagePresent = false)
        assertTrue(!dto.imagePresent)
    }
}
