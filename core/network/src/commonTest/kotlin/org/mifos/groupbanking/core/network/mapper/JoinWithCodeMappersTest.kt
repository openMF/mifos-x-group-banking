/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.mapper

import kotlinx.datetime.Instant
import org.mifos.groupbanking.core.model.GroupRole
import org.mifos.groupbanking.core.model.GroupTypeSlug
import org.mifos.groupbanking.core.model.InvitationAcceptance
import org.mifos.groupbanking.core.model.JoinGroupRequest
import org.mifos.groupbanking.core.network.model.AssociateClientsRequestDto
import org.mifos.groupbanking.core.network.model.AssociateClientsResponseDto
import org.mifos.groupbanking.core.network.model.GroupPreviewDto
import org.mifos.groupbanking.core.network.model.GroupRoleDto
import org.mifos.groupbanking.core.network.model.GroupTypeSlugDto
import org.mifos.groupbanking.core.network.model.InvitationRowDto
import org.mifos.groupbanking.core.network.model.MarkAcceptedChangesDto
import org.mifos.groupbanking.core.network.model.MarkAcceptedRequestDto
import org.mifos.groupbanking.core.network.model.MarkAcceptedResponseDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * DTO <-> domain mapper coverage for the join-with-code wire contracts (COMP-DT-004 +
 * COMP-GRP-003). Every field on every DTO declared in `JoinWithCodeDto.kt` is mapped — no field
 * left unmapped.
 */
class JoinWithCodeMappersTest {

    // ---------- InvitationRowDto -> Invitation ----------

    private val unusedInviteDto = InvitationRowDto(
        token = "AB12CD",
        groupId = 1001L,
        inviterClientId = 55L,
        invitedEmailPhone = "invitee@example.com",
        roleToAssign = GroupRoleDto.MEMBER,
        expiresAt = "2026-08-01T00:00:00Z",
        acceptedAt = null,
    )

    @Test
    fun invitationRowDto_mapsEveryFieldToDomain() {
        val domain = unusedInviteDto.toDomainModel()
        assertEquals("AB12CD", domain.token)
        assertEquals(1001L, domain.groupId)
        assertEquals(55L, domain.inviterClientId)
        assertEquals("invitee@example.com", domain.invitedEmailPhone)
        assertEquals(GroupRole.MEMBER, domain.roleToAssign)
        assertEquals(Instant.parse("2026-08-01T00:00:00Z"), domain.expiresAt)
        assertNull(domain.acceptedAt)
    }

    @Test
    fun invitationRowDto_acceptedAtNull_mapsToNullAndNotAlreadyUsed() {
        val domain = unusedInviteDto.toDomainModel()
        assertNull(domain.acceptedAt)
        assertFalse(domain.isAlreadyUsed)
    }

    @Test
    fun invitationRowDto_acceptedAtPresent_mapsToInstantAndAlreadyUsed() {
        val usedDto = unusedInviteDto.copy(acceptedAt = "2026-07-20T09:15:00Z")
        val domain = usedDto.toDomainModel()
        assertEquals(Instant.parse("2026-07-20T09:15:00Z"), domain.acceptedAt)
        assertTrue(domain.isAlreadyUsed)
    }

    @Test
    fun invitation_isExpired_derivesFromExpiresAtVsNow() {
        val domain = unusedInviteDto.toDomainModel()
        val beforeExpiry = Instant.parse("2026-07-31T00:00:00Z")
        val afterExpiry = Instant.parse("2026-08-02T00:00:00Z")
        assertFalse(domain.isExpired(now = beforeExpiry))
        assertTrue(domain.isExpired(now = afterExpiry))
    }

    @Test
    fun invitation_isExpired_atExactExpiryInstant_isExpired() {
        val domain = unusedInviteDto.toDomainModel()
        assertTrue(domain.isExpired(now = Instant.parse("2026-08-01T00:00:00Z")))
    }

    @Test
    fun groupRoleDto_mapsEveryKnownValueAndUnknownFallback() {
        assertEquals(GroupRole.ORGANIZER, GroupRoleDto.ORGANIZER.toDomainModel())
        assertEquals(GroupRole.MEMBER, GroupRoleDto.MEMBER.toDomainModel())
        assertEquals(GroupRole.TREASURER, GroupRoleDto.TREASURER.toDomainModel())
        assertEquals(GroupRole.SECRETARY, GroupRoleDto.SECRETARY.toDomainModel())
        assertEquals(GroupRole.UNKNOWN, GroupRoleDto.UNKNOWN.toDomainModel())
    }

    // ---------- GroupPreviewDto -> GroupPreview ----------

    @Test
    fun groupPreviewDto_mapsEveryFieldToDomain() {
        val dto = GroupPreviewDto(
            groupId = 1001L,
            groupName = "Mwangaza Women's Group",
            groupType = GroupTypeSlugDto.VSLA,
            organizerName = "Amina Yusuf",
            memberCount = 18,
            officeId = 1L,
            roleToAssign = GroupRoleDto.MEMBER,
        )
        val domain = dto.toDomainModel()
        assertEquals(1001L, domain.groupId)
        assertEquals("Mwangaza Women's Group", domain.groupName)
        assertEquals(GroupTypeSlug.VSLA, domain.groupType)
        assertEquals("Amina Yusuf", domain.organizerName)
        assertEquals(18, domain.memberCount)
        assertEquals(1L, domain.officeId)
        assertEquals(GroupRole.MEMBER, domain.roleToAssign)
    }

    @Test
    fun groupTypeSlugDto_mapsEveryKnownValueAndUnknownFallback() {
        assertEquals(GroupTypeSlug.VSLA, GroupTypeSlugDto.VSLA.toDomainModel())
        assertEquals(GroupTypeSlug.ROSCA, GroupTypeSlugDto.ROSCA.toDomainModel())
        assertEquals(GroupTypeSlug.ASCA, GroupTypeSlugDto.ASCA.toDomainModel())
        assertEquals(GroupTypeSlug.SILC, GroupTypeSlugDto.SILC.toDomainModel())
        assertEquals(GroupTypeSlug.SHG, GroupTypeSlugDto.SHG.toDomainModel())
        assertEquals(GroupTypeSlug.SACCO, GroupTypeSlugDto.SACCO.toDomainModel())
        assertEquals(GroupTypeSlug.CBO_VILLAGE_BANK, GroupTypeSlugDto.CBO_VILLAGE_BANK.toDomainModel())
        assertEquals(GroupTypeSlug.BURIAL_WELFARE, GroupTypeSlugDto.BURIAL_WELFARE.toDomainModel())
        assertEquals(GroupTypeSlug.JLG, GroupTypeSlugDto.JLG.toDomainModel())
        assertEquals(GroupTypeSlug.UNKNOWN, GroupTypeSlugDto.UNKNOWN.toDomainModel())
    }

    // ---------- JoinGroupRequest <-> AssociateClientsRequestDto/ResponseDto ----------

    @Test
    fun joinGroupRequest_toDto_mapsEveryField() {
        val domain = JoinGroupRequest(clientIds = listOf(789L), roleToAssign = GroupRole.MEMBER)
        val dto = domain.toDto()
        assertEquals(listOf(789L), dto.clientIds)
        assertEquals(GroupRoleDto.MEMBER, dto.roleToAssign)
    }

    @Test
    fun associateClientsRequestDto_roundTripsThroughDomain() {
        val dto = AssociateClientsRequestDto(clientIds = listOf(1L, 2L), roleToAssign = GroupRoleDto.TREASURER)
        val roundTripped = dto.toDomainModel().toDto()
        assertEquals(dto, roundTripped)
    }

    @Test
    fun associateClientsResponseDto_mapsEveryFieldToDomain() {
        val dto = AssociateClientsResponseDto(resourceId = 1001L, groupId = 1001L, clientIds = listOf(789L))
        val domain = dto.toDomainModel()
        assertEquals(1001L, domain.resourceId)
        assertEquals(1001L, domain.groupId)
        assertEquals(listOf(789L), domain.clientIds)
    }

    // ---------- InvitationAcceptance <-> MarkAcceptedRequestDto/ResponseDto ----------

    @Test
    fun invitationAcceptance_toDto_mapsAcceptedAt() {
        val domain = InvitationAcceptance(acceptedAt = Instant.parse("2026-07-20T09:15:00Z"))
        val dto = domain.toDto()
        assertEquals("2026-07-20T09:15:00Z", dto.acceptedAt)
    }

    @Test
    fun markAcceptedRequestDto_roundTripsThroughDomain() {
        val dto = MarkAcceptedRequestDto(acceptedAt = "2026-07-20T09:15:00Z")
        val roundTripped = dto.toDomainModel().toDto()
        assertEquals(dto, roundTripped)
    }

    @Test
    fun markAcceptedResponseDto_mapsEveryFieldToDomain_flatteningNestedChanges() {
        val dto = MarkAcceptedResponseDto(
            resourceId = 1001L,
            changes = MarkAcceptedChangesDto(acceptedAt = "2026-07-20T09:15:00Z"),
        )
        val domain = dto.toDomainModel()
        assertEquals(1001L, domain.resourceId)
        assertEquals(Instant.parse("2026-07-20T09:15:00Z"), domain.acceptedAt)
    }
}
