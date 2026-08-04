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
import org.mifos.groupbanking.core.model.LoanPurpose
import org.mifos.groupbanking.core.model.LoanRequestPayload
import org.mifos.groupbanking.core.network.model.LoanRequestPayloadDto
import org.mifos.groupbanking.core.network.model.LoanRequestResponseDto
import org.mifos.groupbanking.core.network.service.loanrequest.LoanRequestApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakeLoanRequestApi(
    private val result: NetworkResult<LoanRequestResponseDto, NetworkError>? = null,
) : LoanRequestApi {

    var lastRequest: LoanRequestPayloadDto? = null
    var callCount = 0

    override suspend fun submitLoanRequest(
        request: LoanRequestPayloadDto,
    ): NetworkResult<LoanRequestResponseDto, NetworkError> {
        lastRequest = request
        callCount++
        return result ?: error("result not stubbed")
    }

    override suspend fun getMemberSavingsBalance(clientId: Long): NetworkResult<Double, NetworkError> =
        NetworkResult.Success(0.0)
}

private class FakeLoanRequestSyncQueueRepository : SyncQueueRepository {

    var lastOperationType: String? = null
    var lastTargetTable: String? = null
    var lastPayloadJson: String? = null
    var enqueueCallCount = 0
    var nextId: Long = 1L

    override suspend fun enqueue(operationType: String, targetTable: String, payloadJson: String): Long {
        lastOperationType = operationType
        lastTargetTable = targetTable
        lastPayloadJson = payloadJson
        enqueueCallCount++
        return nextId
    }

    override fun observePending() = throw NotImplementedError("not exercised by LoanRequestRepositoryTest")
    override fun observeCounts() = throw NotImplementedError("not exercised by LoanRequestRepositoryTest")
    override suspend fun markSyncing(id: Long) = throw NotImplementedError("not exercised")
    override suspend fun markSynced(id: Long) = throw NotImplementedError("not exercised")
    override suspend fun markFailed(id: Long, error: String?) = throw NotImplementedError("not exercised")
    override suspend fun retryAll() = throw NotImplementedError("not exercised")
    override fun observePendingByType() = throw NotImplementedError("not exercised")
    override fun observeFailed() = throw NotImplementedError("not exercised")
    override fun observeConflictCount() = throw NotImplementedError("not exercised")
    override suspend fun getItem(id: Long) = throw NotImplementedError("not exercised")
}

/**
 * TDD RED-first coverage for [LoanRequestRepository] / [LoanRequestRepositoryImpl] — a pure
 * network passthrough ([submit]) plus the SyncQueue offline-enqueue seam ([enqueueOffline]). No
 * try-catch anywhere in the repository under test (Mandatory Rule 4) — every branch below is a
 * plain `when` over the fake service's [NetworkResult]. `business_logic.kind` for loan-request is
 * `crud` (`ui.yaml#business_logic.kind`) — the legacy template path
 * (RULE-IDEA-IMPL-INTELLIGENCE-001 AC-03i), Store5-free, same branch as [MemberAddRepositoryImpl].
 */
class LoanRequestRepositoryTest {

    private val payload = LoanRequestPayload(
        clientId = 7L,
        requestedAmount = 1500.0,
        purpose = LoanPurpose.BUSINESS,
        durationWeeks = 12,
        savingsBalanceAtRequest = 5000.0,
    )

    private val responseDto = LoanRequestResponseDto(
        resourceId = 501L,
        officeId = 1L,
        clientId = 7L,
        resourceExternalId = "LR-501",
    )

    // ---------- submit (online path) ----------

    @Test
    fun submit_success_mapsApiResponseToDomainResult() = runTest {
        val api = FakeLoanRequestApi(result = NetworkResult.Success(responseDto))
        val repo = LoanRequestRepositoryImpl(api = api, syncQueueRepository = FakeLoanRequestSyncQueueRepository())

        val result = repo.submit(payload)

        check(result is NetworkResult.Success)
        assertEquals(501L, result.data.resourceId)
        assertEquals("LR-501", result.data.resourceExternalId)
        assertEquals(1, api.callCount)
    }

    @Test
    fun submit_threadsDomainPayloadFieldsIntoTheRequestDto() = runTest {
        val api = FakeLoanRequestApi(result = NetworkResult.Success(responseDto))
        val repo = LoanRequestRepositoryImpl(api = api, syncQueueRepository = FakeLoanRequestSyncQueueRepository())

        repo.submit(payload)

        assertEquals(7L, api.lastRequest?.clientId)
        assertEquals(1500.0, api.lastRequest?.requestedAmount)
        assertEquals(12, api.lastRequest?.durationWeeks)
        assertEquals(5000.0, api.lastRequest?.savingsBalanceAtRequest)
        assertEquals("PENDING", api.lastRequest?.status)
    }

    @Test
    fun submit_validationFailure400_returnsErrorUntouched() = runTest {
        val api = FakeLoanRequestApi(result = NetworkResult.Error(NetworkError.BAD_REQUEST))
        val repo = LoanRequestRepositoryImpl(api = api, syncQueueRepository = FakeLoanRequestSyncQueueRepository())

        val result = repo.submit(payload)

        assertEquals(NetworkResult.Error(NetworkError.BAD_REQUEST), result)
    }

    @Test
    fun submit_serverUnavailable503_returnsErrorForCallerToEnqueueSyncQueue() = runTest {
        // No NetworkMonitor / offline-detection lives in this repository (Mandatory Rule 4 —
        // plain when-based passthrough, never a try-catch envelope). A caller (ViewModel),
        // informed by NetworkMonitor, is responsible for calling enqueueOffline on this
        // NetworkResult.Error (data-flow.yaml#offline_behavior strategy: enqueue_to_sync_queue).
        val api = FakeLoanRequestApi(result = NetworkResult.Error(NetworkError.SERVER))
        val repo = LoanRequestRepositoryImpl(api = api, syncQueueRepository = FakeLoanRequestSyncQueueRepository())

        val result = repo.submit(payload)

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }

    // ---------- enqueueOffline (SyncQueue seam) ----------

    @Test
    fun enqueueOffline_delegatesToSyncQueueRepositoryWithLoanRequestOperationTypeAndTargetTable() = runTest {
        val syncQueueRepository = FakeLoanRequestSyncQueueRepository()
        val repo = LoanRequestRepositoryImpl(api = FakeLoanRequestApi(), syncQueueRepository = syncQueueRepository)

        val id = repo.enqueueOffline(payload)

        assertEquals(1L, id)
        assertEquals(1, syncQueueRepository.enqueueCallCount)
        assertEquals("LOAN_REQUEST", syncQueueRepository.lastOperationType)
        assertEquals("dt_loan_request", syncQueueRepository.lastTargetTable)
    }

    @Test
    fun enqueueOffline_serializesPayloadAsRoundTrippableJson() = runTest {
        val syncQueueRepository = FakeLoanRequestSyncQueueRepository()
        val repo = LoanRequestRepositoryImpl(api = FakeLoanRequestApi(), syncQueueRepository = syncQueueRepository)

        repo.enqueueOffline(payload)

        val json = syncQueueRepository.lastPayloadJson
        assertTrue(json != null && json.contains("\"clientId\":7"))
        assertTrue(json.contains("\"requested_amount\":1500.0"))
        assertTrue(json.contains("\"status\":\"PENDING\""))
    }

    @Test
    fun enqueueOffline_neverCallsTheNetworkApi() = runTest {
        val api = FakeLoanRequestApi()
        val repo = LoanRequestRepositoryImpl(api = api, syncQueueRepository = FakeLoanRequestSyncQueueRepository())

        repo.enqueueOffline(payload)

        assertEquals(0, api.callCount)
    }
}
