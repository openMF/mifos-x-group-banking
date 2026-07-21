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
import org.mifos.groupbanking.core.model.GroupTypeSlug
import org.mifos.groupbanking.core.model.HealthIndicator
import org.mifos.groupbanking.core.model.ViewerRole
import org.mifos.groupbanking.core.network.model.GroupDto
import org.mifos.groupbanking.core.network.model.GroupPageDto
import org.mifos.groupbanking.core.network.model.GroupTypeDto
import org.mifos.groupbanking.core.network.model.HealthIndicatorDto
import org.mifos.groupbanking.core.network.model.ViewerRoleDto
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * TDD RED-first coverage for the group-list DTO<->domain mappers (COMP-GRP-001). Every field on
 * `GroupDto` declared in `GroupDto.kt` must be exercised here (RULE: all-fields-mapped).
 */
class GroupMappersTest {

    private val mwangazaDto = GroupDto(
        id = "GRP-20260509-001",
        name = "Mwangaza Women's Group",
        groupType = GroupTypeDto.VSLA,
        viewerRole = ViewerRoleDto.ORGANIZER,
        cycleNumber = 1,
        memberCount = 20,
        lastMeetingDate = "2026-07-14",
        healthIndicator = HealthIndicatorDto.GREEN,
        overdueRate = 0.00,
        status = "ACTIVE",
        fineractCenterId = 1001L,
    )

    // ---------- GroupDto -> Group (all 11 fields) ----------

    @Test
    fun groupDto_toDomainModel_mapsAllFields() {
        val domain = mwangazaDto.toDomainModel()

        assertEquals("GRP-20260509-001", domain.id)
        assertEquals("Mwangaza Women's Group", domain.name)
        assertEquals(GroupTypeSlug.VSLA, domain.groupType)
        assertEquals(ViewerRole.ORGANIZER, domain.viewerRole)
        assertEquals(1, domain.cycleNumber)
        assertEquals(20, domain.memberCount)
        assertEquals(LocalDate(2026, 7, 14), domain.lastMeetingDate)
        assertEquals(HealthIndicator.GREEN, domain.healthIndicator)
        assertEquals(0.00, domain.overdueRate)
        assertEquals("ACTIVE", domain.status)
        assertEquals(1001L, domain.fineractCenterId)
    }

    @Test
    fun groupDto_toDomainModel_roscaMemberRow_mapsThrough() {
        val roscaDto = mwangazaDto.copy(
            id = "GRP-20260215-002",
            name = "Jiunge ROSCA Circle",
            groupType = GroupTypeDto.ROSCA,
            viewerRole = ViewerRoleDto.MEMBER,
            cycleNumber = 3,
            memberCount = 10,
            lastMeetingDate = "2026-07-10",
            healthIndicator = HealthIndicatorDto.AMBER,
            overdueRate = 0.12,
            fineractCenterId = 1002L,
        )

        val domain = roscaDto.toDomainModel()

        assertEquals(GroupTypeSlug.ROSCA, domain.groupType)
        assertEquals(ViewerRole.MEMBER, domain.viewerRole)
        assertEquals(HealthIndicator.AMBER, domain.healthIndicator)
        assertEquals(0.12, domain.overdueRate)
    }

    // ---------- unknown-enum fallback propagates through the whole row ----------

    @Test
    fun groupDto_toDomainModel_unknownGroupTypeMapsToDomainUnknown() {
        val dto = mwangazaDto.copy(groupType = GroupTypeDto.UNKNOWN)
        assertEquals(GroupTypeSlug.UNKNOWN, dto.toDomainModel().groupType)
    }

    @Test
    fun groupDto_toDomainModel_unknownViewerRoleMapsToDomainUnknown() {
        val dto = mwangazaDto.copy(viewerRole = ViewerRoleDto.UNKNOWN)
        assertEquals(ViewerRole.UNKNOWN, dto.toDomainModel().viewerRole)
    }

    @Test
    fun groupDto_toDomainModel_unknownHealthIndicatorMapsToDomainUnknown() {
        val dto = mwangazaDto.copy(healthIndicator = HealthIndicatorDto.UNKNOWN)
        assertEquals(HealthIndicator.UNKNOWN, dto.toDomainModel().healthIndicator)
    }

    // ---------- enum mappers (every entry, including the CBO/BURIAL short-form adaptation) ----------

    @Test
    fun groupTypeDto_toDomainModel_mapsEveryEntryOntoSharedGroupTypeSlug() {
        assertEquals(GroupTypeSlug.VSLA, GroupTypeDto.VSLA.toDomainModel())
        assertEquals(GroupTypeSlug.ROSCA, GroupTypeDto.ROSCA.toDomainModel())
        assertEquals(GroupTypeSlug.ASCA, GroupTypeDto.ASCA.toDomainModel())
        assertEquals(GroupTypeSlug.SILC, GroupTypeDto.SILC.toDomainModel())
        assertEquals(GroupTypeSlug.SHG, GroupTypeDto.SHG.toDomainModel())
        assertEquals(GroupTypeSlug.SACCO, GroupTypeDto.SACCO.toDomainModel())
        // Short-form wire values (CBO / BURIAL) adapt onto the shared GroupTypeSlug domain
        // enum's long-form entries (CBO_VILLAGE_BANK / BURIAL_WELFARE) — see GroupDto.kt kdoc.
        assertEquals(GroupTypeSlug.CBO_VILLAGE_BANK, GroupTypeDto.CBO.toDomainModel())
        assertEquals(GroupTypeSlug.BURIAL_WELFARE, GroupTypeDto.BURIAL.toDomainModel())
        assertEquals(GroupTypeSlug.JLG, GroupTypeDto.JLG.toDomainModel())
        assertEquals(GroupTypeSlug.UNKNOWN, GroupTypeDto.UNKNOWN.toDomainModel())
    }

    @Test
    fun viewerRoleDto_toDomainModel_mapsEveryEntry() {
        assertEquals(ViewerRole.ORGANIZER, ViewerRoleDto.ORGANIZER.toDomainModel())
        assertEquals(ViewerRole.MEMBER, ViewerRoleDto.MEMBER.toDomainModel())
        assertEquals(ViewerRole.TREASURER, ViewerRoleDto.TREASURER.toDomainModel())
        assertEquals(ViewerRole.CHAIRPERSON, ViewerRoleDto.CHAIRPERSON.toDomainModel())
        assertEquals(ViewerRole.SECRETARY, ViewerRoleDto.SECRETARY.toDomainModel())
        assertEquals(ViewerRole.UNKNOWN, ViewerRoleDto.UNKNOWN.toDomainModel())
    }

    @Test
    fun healthIndicatorDto_toDomainModel_mapsEveryEntry() {
        assertEquals(HealthIndicator.GREEN, HealthIndicatorDto.GREEN.toDomainModel())
        assertEquals(HealthIndicator.AMBER, HealthIndicatorDto.AMBER.toDomainModel())
        assertEquals(HealthIndicator.RED, HealthIndicatorDto.RED.toDomainModel())
        assertEquals(HealthIndicator.UNKNOWN, HealthIndicatorDto.UNKNOWN.toDomainModel())
    }

    // ---------- HealthIndicator.fromOverdueRate boundary rules (client-side re-derivation) ----------

    @Test
    fun healthIndicator_fromOverdueRate_greenBelowFivePercent() {
        assertEquals(HealthIndicator.GREEN, HealthIndicator.fromOverdueRate(0.0))
        assertEquals(HealthIndicator.GREEN, HealthIndicator.fromOverdueRate(0.0499))
    }

    @Test
    fun healthIndicator_fromOverdueRate_amberAtFivePercentBoundaryInclusive() {
        assertEquals(HealthIndicator.AMBER, HealthIndicator.fromOverdueRate(0.05))
        assertEquals(HealthIndicator.AMBER, HealthIndicator.fromOverdueRate(0.12))
        assertEquals(HealthIndicator.AMBER, HealthIndicator.fromOverdueRate(0.1999))
    }

    @Test
    fun healthIndicator_fromOverdueRate_redAtTwentyPercentBoundaryInclusive() {
        assertEquals(HealthIndicator.RED, HealthIndicator.fromOverdueRate(0.20))
        assertEquals(HealthIndicator.RED, HealthIndicator.fromOverdueRate(0.25))
        assertEquals(HealthIndicator.RED, HealthIndicator.fromOverdueRate(1.0))
    }

    @Test
    fun healthIndicator_fromOverdueRate_agreesWithServerSentIndicatorAcrossDemoFixtureRows() {
        // Cross-checks idea-layer/screens/group-list/demo-data.yaml rows: the server-sent
        // healthIndicator and the client-derived fromOverdueRate must agree on every seeded row.
        val rows = listOf(
            0.00 to HealthIndicator.GREEN,
            0.12 to HealthIndicator.AMBER,
            0.02 to HealthIndicator.GREEN,
            0.25 to HealthIndicator.RED,
            0.04 to HealthIndicator.GREEN,
        )
        rows.forEach { (rate, expected) ->
            assertEquals(expected, HealthIndicator.fromOverdueRate(rate))
        }
    }

    // ---------- List<GroupDto>.toDomainModels() batch converter ----------

    @Test
    fun groupDtoList_toDomainModels_mapsEveryItemInOrder() {
        val roscaDto = mwangazaDto.copy(id = "GRP-2", name = "Jiunge ROSCA Circle", groupType = GroupTypeDto.ROSCA)
        val result = listOf(mwangazaDto, roscaDto).toDomainModels()

        assertEquals(2, result.size)
        assertEquals("GRP-20260509-001", result[0].id)
        assertEquals("GRP-2", result[1].id)
        assertEquals(GroupTypeSlug.ROSCA, result[1].groupType)
    }

    @Test
    fun groupDtoList_toDomainModels_emptyListStaysEmpty() {
        assertEquals(emptyList(), emptyList<GroupDto>().toDomainModels())
    }

    // ---------- GroupPageDto -> GroupPage ----------

    @Test
    fun groupPageDto_toDomainModel_mapsTotalFilteredRecordsAndGroups() {
        val page = GroupPageDto(totalFilteredRecords = 5, pageItems = listOf(mwangazaDto))
        val domain = page.toDomainModel()

        assertEquals(5, domain.totalFilteredRecords)
        assertEquals(1, domain.groups.size)
        assertEquals("GRP-20260509-001", domain.groups[0].id)
    }

    @Test
    fun groupPageDto_toDomainModel_emptyPageItemsMapsToEmptyGroups() {
        val page = GroupPageDto(totalFilteredRecords = 0, pageItems = emptyList())
        assertEquals(emptyList(), page.toDomainModel().groups)
    }
}
