/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.data.repository

import kotlinx.coroutines.test.runTest
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.model.ContributionModel
import org.mifos.groupbanking.core.model.CreateGroupRequest
import org.mifos.groupbanking.core.model.CreateGroupTypeConfig
import org.mifos.groupbanking.core.model.GroupTypeSlug
import org.mifos.groupbanking.core.model.PayoutOrderMethod
import org.mifos.groupbanking.core.model.SavingsMechanism
import org.mifos.groupbanking.core.model.ShareoutFormula
import org.mifos.groupbanking.core.network.model.ContributionModelDto
import org.mifos.groupbanking.core.network.model.CreateGroupRequestDto
import org.mifos.groupbanking.core.network.model.CreateGroupResponseDto
import org.mifos.groupbanking.core.network.model.CreateGroupTypeConfigDto
import org.mifos.groupbanking.core.network.model.GroupTypeDto
import org.mifos.groupbanking.core.network.model.OfficeDto
import org.mifos.groupbanking.core.network.model.PayoutOrderMethodDto
import org.mifos.groupbanking.core.network.model.SavingsMechanismDto
import org.mifos.groupbanking.core.network.model.ShareoutFormulaDto
import org.mifos.groupbanking.core.network.service.groupcreate.GroupCreateApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private class FakeGroupCreateApi(
    private val officesResult: NetworkResult<List<OfficeDto>, NetworkError>? = null,
    private val createGroupResult: NetworkResult<CreateGroupResponseDto, NetworkError>? = null,
) : GroupCreateApi {

    var lastOrderBy: String? = null
    var lastCreateRequest: CreateGroupRequestDto? = null

    override suspend fun getOffices(orderBy: String): NetworkResult<List<OfficeDto>, NetworkError> {
        lastOrderBy = orderBy
        return officesResult ?: error("officesResult not stubbed")
    }

    override suspend fun createGroup(request: CreateGroupRequestDto): NetworkResult<CreateGroupResponseDto, NetworkError> {
        lastCreateRequest = request
        return createGroupResult ?: error("createGroupResult not stubbed")
    }
}

/**
 * TDD RED-first coverage for [GroupCreateRepository] / [GroupCreateRepositoryImpl]. No
 * try-catch anywhere in the repository under test (Mandatory Rule 4) — every branch below is a
 * plain `when` over the fake service's [NetworkResult]. `business_logic.kind` for group-create's
 * `createGroup` mutation is a `processor` orchestration flow (no read-stream to cache) — Store5
 * free, same branch as [InvitationRepositoryImpl] / [AuthRepositoryImpl]. `getOffices` is
 * currently the same Store5-free branch pending a future `kmp-store-gen` `OfficeStore` (see
 * [GroupCreateRepository] KDoc SC2 note) — not half-built here.
 */
class GroupCreateRepositoryTest {

    private val officeDtos = listOf(
        OfficeDto(id = 1, name = "Head Office", nameDecorated = ".Head Office", externalId = "HO"),
        OfficeDto(id = 2, name = "Kampala Branch", nameDecorated = "..Kampala Branch", externalId = null),
    )

    private val typeConfig = CreateGroupTypeConfig(
        groupType = GroupTypeSlug.VSLA,
        poolModel = SavingsMechanism.ACCUMULATING,
        contributionModel = ContributionModel.SHARE_BASED_VARIABLE,
        shareoutFormula = ShareoutFormula.PRORATA_SHARES,
        payoutOrderMethod = PayoutOrderMethod.NA,
        shareValue = 5.0,
        contributionAmount = 0.0,
        socialFundEnabled = true,
        socialFundPercent = 10.0,
        cycleLengthMonths = 12,
        loanMultiplier = 3.0,
        interestRate = 2.0,
        fineAmount = 1.0,
        maxMembers = 30,
    )

    private val createRequest = CreateGroupRequest(
        name = "Sunrise VSLA",
        officeId = 1,
        userId = 42,
        currency = "UGX",
        meetingDay = "MONDAY",
        meetingTime = "10:00",
        typeConfig = typeConfig,
    )

    private val typeConfigDto = CreateGroupTypeConfigDto(
        groupType = GroupTypeDto.VSLA,
        poolModel = SavingsMechanismDto.ACCUMULATING,
        contributionModel = ContributionModelDto.SHARE_BASED_VARIABLE,
        shareoutFormula = ShareoutFormulaDto.PRORATA_SHARES,
        payoutOrderMethod = PayoutOrderMethodDto.NA,
        shareValue = 5.0,
        contributionAmount = 0.0,
        socialFundEnabled = true,
        socialFundPercent = 10.0,
        cycleLengthMonths = 12,
        loanMultiplier = 3.0,
        interestRate = 2.0,
        fineAmount = 1.0,
        maxMembers = 30,
    )

    // ---------- getOffices ----------

    @Test
    fun getOffices_success_returnsMappedOfficesInOrder() = runTest {
        val api = FakeGroupCreateApi(officesResult = NetworkResult.Success(officeDtos))
        val repo = GroupCreateRepositoryImpl(api, NoOpSyncQueueRepository())

        val result = repo.getOffices()

        check(result is NetworkResult.Success)
        assertEquals(2, result.data.size)
        assertEquals("Head Office", result.data[0].name)
        assertEquals("HO", result.data[0].externalId)
        assertNull(result.data[1].externalId)
    }

    @Test
    fun getOffices_defaultsOrderByToName() = runTest {
        val api = FakeGroupCreateApi(officesResult = NetworkResult.Success(officeDtos))
        val repo = GroupCreateRepositoryImpl(api, NoOpSyncQueueRepository())

        repo.getOffices()

        assertEquals("name", api.lastOrderBy)
    }

    @Test
    fun getOffices_emptyList_returnsSuccessWithEmptyList() = runTest {
        val api = FakeGroupCreateApi(officesResult = NetworkResult.Success(emptyList()))
        val repo = GroupCreateRepositoryImpl(api, NoOpSyncQueueRepository())

        val result = repo.getOffices()

        check(result is NetworkResult.Success)
        assertEquals(emptyList(), result.data)
    }

    @Test
    fun getOffices_unauthorized_returnsErrorUnchanged() = runTest {
        val api = FakeGroupCreateApi(officesResult = NetworkResult.Error(NetworkError.UNAUTHORIZED))
        val repo = GroupCreateRepositoryImpl(api, NoOpSyncQueueRepository())

        val result = repo.getOffices()

        assertEquals(NetworkResult.Error(NetworkError.UNAUTHORIZED), result)
    }

    @Test
    fun getOffices_serverError_returnsErrorUnchanged() = runTest {
        val api = FakeGroupCreateApi(officesResult = NetworkResult.Error(NetworkError.SERVER))
        val repo = GroupCreateRepositoryImpl(api, NoOpSyncQueueRepository())

        val result = repo.getOffices()

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }

    // ---------- createGroup ----------

    @Test
    fun createGroup_success_mapsRequestToDtoAndReturnsMappedResult() = runTest {
        val api = FakeGroupCreateApi(
            createGroupResult = NetworkResult.Success(
                CreateGroupResponseDto(groupId = "grp-100", fineractGroupId = 55, inviteCode = "ABC123"),
            ),
        )
        val repo = GroupCreateRepositoryImpl(api, NoOpSyncQueueRepository())

        val result = repo.createGroup(createRequest)

        check(result is NetworkResult.Success)
        assertEquals("grp-100", result.data.groupId)
        assertEquals(55L, result.data.fineractGroupId)
        assertEquals("ABC123", result.data.inviteCode)
        assertEquals(typeConfigDto, api.lastCreateRequest?.typeConfig)
        assertEquals("Sunrise VSLA", api.lastCreateRequest?.name)
        assertEquals(42L, api.lastCreateRequest?.userId)
    }

    @Test
    fun createGroup_validationError_returnsErrorUnchanged() = runTest {
        val api = FakeGroupCreateApi(createGroupResult = NetworkResult.Error(NetworkError.BAD_REQUEST))
        val repo = GroupCreateRepositoryImpl(api, NoOpSyncQueueRepository())

        val result = repo.createGroup(createRequest)

        assertEquals(NetworkResult.Error(NetworkError.BAD_REQUEST), result)
    }

    @Test
    fun createGroup_conflictGroupNameTaken_returnsErrorUnchanged() = runTest {
        // 409 (group name taken) has no dedicated NetworkError bucket — surfaces as UNKNOWN,
        // same convention as the Service layer; the repository does not re-bucket it.
        val api = FakeGroupCreateApi(createGroupResult = NetworkResult.Error(NetworkError.UNKNOWN))
        val repo = GroupCreateRepositoryImpl(api, NoOpSyncQueueRepository())

        val result = repo.createGroup(createRequest)

        assertEquals(NetworkResult.Error(NetworkError.UNKNOWN), result)
    }

    @Test
    fun createGroup_offline_surfacesAsNetworkErrorForCallerToEnqueue() = runTest {
        // No offline-queue try-catch here (Mandatory Rule 4/5 — no silent fallback). The caller
        // (ViewModel, via DraftSubmitHandler wrapping this repository call) is responsible for
        // catching the thrown transport exception one layer above the Service boundary and
        // enqueuing to the SubmitOutbox — this repository only ever surfaces what the Service
        // returns. Simulated here as NetworkError.UNKNOWN (the Service's transport-failure
        // mapping), which is exactly what a real offline attempt returns.
        val api = FakeGroupCreateApi(createGroupResult = NetworkResult.Error(NetworkError.UNKNOWN))
        val repo = GroupCreateRepositoryImpl(api, NoOpSyncQueueRepository())

        val result = repo.createGroup(createRequest)

        assertEquals(NetworkResult.Error(NetworkError.UNKNOWN), result)
    }

    @Test
    fun createGroup_serverError_returnsErrorUnchanged() = runTest {
        val api = FakeGroupCreateApi(createGroupResult = NetworkResult.Error(NetworkError.SERVER))
        val repo = GroupCreateRepositoryImpl(api, NoOpSyncQueueRepository())

        val result = repo.createGroup(createRequest)

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }
}
