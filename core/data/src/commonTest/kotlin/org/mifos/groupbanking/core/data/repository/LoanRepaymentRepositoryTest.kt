/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.database.loandetail.dao.LoanDetailDao
import org.mifos.groupbanking.core.database.loandetail.entity.LoanDetailCacheEntity
import org.mifos.groupbanking.core.model.PaymentMethod
import org.mifos.groupbanking.core.model.RecordRepaymentRequest
import org.mifos.groupbanking.core.network.model.LoanDetailResponseDto
import org.mifos.groupbanking.core.network.model.RecordRepaymentRequestDto
import org.mifos.groupbanking.core.network.model.RecordRepaymentResponseDto
import org.mifos.groupbanking.core.network.service.loandetail.LoanDetailApi
import org.mifos.groupbanking.core.network.service.loanrepayment.LoanRepaymentApi
import org.mifos.groupbanking.core.store.loandetail.impl.provideLoanDetailStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD coverage for [LoanRepaymentRepositoryImpl] — verifies (1) a successful `make_repayment`
 * write threads [RecordRepaymentRequest] through to the wire request DTO and returns the mapped
 * [org.mifos.groupbanking.core.model.RepaymentResult]; (2) success INVALIDATES the loan-detail
 * composite cache for that loanId through the store (`loanDetailStore.clear(loanId)`) so the
 * still-active `LoanDetailRepository.loanDetailStream` re-fetches with the new
 * balance/outstanding figures (`data-flow.yaml#cache.strategy: invalidate`,
 * RULE-IMPLEMENT-STORE5-001 S5-1 — not a DAO bypass); and (3) a failed write leaves the cache
 * intact and surfaces the wire error verbatim. No try-catch anywhere in the repository under test
 * (Mandatory Rule 4) — every branch is a plain `when` over the fake service's [NetworkResult].
 */
class LoanRepaymentRepositoryTest {

    private val requestDomain = RecordRepaymentRequest(
        amount = 250.0,
        paymentMethod = PaymentMethod.CASH,
        referenceNumber = "RCPT-9001",
    )

    @Test
    fun recordRepayment_success_threadsRequestAndReturnsMappedResult() = runTest {
        val repaymentApi = FakeLoanRepaymentApi(
            result = NetworkResult.Success(RecordRepaymentResponseDto(officeId = 1, clientId = 9L, loanId = 500L, resourceId = 777L)),
        )
        val loanDetailDao = FakeLoanRepaymentDetailDao()
        val repo = repository(repaymentApi, FakeLoanRepaymentDetailApi(), loanDetailDao)

        val result = repo.recordRepayment(loanId = 500L, request = requestDomain)

        check(result is NetworkResult.Success)
        assertEquals(777L, result.data.resourceId)
        assertEquals(500L, result.data.loanId)
        assertEquals(500L, repaymentApi.lastLoanId)
        assertEquals(250.0, repaymentApi.lastRequest?.transactionAmount)
        assertEquals(2, repaymentApi.lastRequest?.paymentTypeId, "CASH resolves to paymentTypeId=2 per api.yaml note")
        assertEquals("RCPT-9001", repaymentApi.lastRequest?.receiptNumber)
    }

    @Test
    fun recordRepayment_blankReferenceNumber_normalizesToNullReceiptNumber() = runTest {
        val repaymentApi = FakeLoanRepaymentApi(
            result = NetworkResult.Success(RecordRepaymentResponseDto(officeId = 1, clientId = 9L, loanId = 500L, resourceId = 777L)),
        )
        val repo = repository(repaymentApi, FakeLoanRepaymentDetailApi(), FakeLoanRepaymentDetailDao())

        repo.recordRepayment(
            loanId = 500L,
            request = RecordRepaymentRequest(amount = 100.0, paymentMethod = PaymentMethod.MPESA, referenceNumber = ""),
        )

        assertEquals(null, repaymentApi.lastRequest?.receiptNumber)
        assertEquals(1, repaymentApi.lastRequest?.paymentTypeId, "MPESA resolves to paymentTypeId=1 per api.yaml note")
    }

    @Test
    fun recordRepayment_success_invalidatesLoanDetailCacheSoLoanDetailRefetches() = runTest {
        val repaymentApi = FakeLoanRepaymentApi(
            result = NetworkResult.Success(RecordRepaymentResponseDto(officeId = 1, clientId = 9L, loanId = 500L, resourceId = 777L)),
        )
        // Seed a cached loan-detail row so we can observe the invalidation delete the SoT row.
        val loanDetailDao = FakeLoanRepaymentDetailDao().apply { seed(cacheEntity(500L, "Asha")) }
        val repo = repository(repaymentApi, FakeLoanRepaymentDetailApi(), loanDetailDao)

        assertEquals(1, loanDetailDao.currentRows().size, "precondition: a cached loan-detail row exists")

        repo.recordRepayment(loanId = 500L, request = requestDomain)

        assertEquals(
            0,
            loanDetailDao.currentRows().size,
            "a successful repayment must invalidate the loan-detail cache (store.clear) for the paid loan",
        )
    }

    @Test
    fun recordRepayment_validationFailure_leavesLoanDetailCacheIntactAndSurfacesError() = runTest {
        val repaymentApi = FakeLoanRepaymentApi(result = NetworkResult.Error(NetworkError.BAD_REQUEST))
        val loanDetailDao = FakeLoanRepaymentDetailDao().apply { seed(cacheEntity(500L, "Asha")) }
        val repo = repository(repaymentApi, FakeLoanRepaymentDetailApi(), loanDetailDao)

        val result = repo.recordRepayment(loanId = 500L, request = requestDomain)

        assertEquals(NetworkResult.Error(NetworkError.BAD_REQUEST), result)
        assertEquals(1, loanDetailDao.currentRows().size, "a failed write must NOT invalidate the cache")
    }

    @Test
    fun recordRepayment_serverError_surfacesErrorVerbatimAndLeavesCacheIntact() = runTest {
        val repaymentApi = FakeLoanRepaymentApi(result = NetworkResult.Error(NetworkError.SERVER))
        val loanDetailDao = FakeLoanRepaymentDetailDao().apply { seed(cacheEntity(500L, "Asha")) }
        val repo = repository(repaymentApi, FakeLoanRepaymentDetailApi(), loanDetailDao)

        val result = repo.recordRepayment(loanId = 500L, request = requestDomain)

        assertTrue(result is NetworkResult.Error)
        assertEquals(NetworkError.SERVER, (result as NetworkResult.Error).error)
        assertEquals(1, loanDetailDao.currentRows().size)
    }

    // --- wiring ----------------------------------------------------------------------------------

    private fun repository(
        repaymentApi: LoanRepaymentApi,
        loanDetailApi: LoanDetailApi,
        loanDetailDao: LoanDetailDao,
    ): LoanRepaymentRepository = LoanRepaymentRepositoryImpl(
        api = repaymentApi,
        loanDetailStore = provideLoanDetailStore(loanDetailApi, loanDetailDao),
    )
}

// ---------------------------------------------------------------------------
// Fakes
// ---------------------------------------------------------------------------

private class FakeLoanRepaymentApi(
    private val result: NetworkResult<RecordRepaymentResponseDto, NetworkError>,
) : LoanRepaymentApi {
    var lastLoanId: Long? = null
        private set
    var lastRequest: RecordRepaymentRequestDto? = null
        private set

    override suspend fun recordRepayment(
        loanId: Long,
        request: RecordRepaymentRequestDto,
    ): NetworkResult<RecordRepaymentResponseDto, NetworkError> {
        lastLoanId = loanId
        lastRequest = request
        return result
    }
}

private class FakeLoanRepaymentDetailApi(
    private val result: NetworkResult<LoanDetailResponseDto, NetworkError> = NetworkResult.Error(NetworkError.UNKNOWN),
) : LoanDetailApi {
    override suspend fun getLoanDetail(
        loanId: Long,
        associations: String,
    ): NetworkResult<LoanDetailResponseDto, NetworkError> = result
}

private class FakeLoanRepaymentDetailDao : LoanDetailDao {
    private val rows = MutableStateFlow<List<LoanDetailCacheEntity>>(emptyList())

    fun seed(entity: LoanDetailCacheEntity) { rows.value = listOf(entity) }
    fun currentRows(): List<LoanDetailCacheEntity> = rows.value

    override fun observeByKey(loanId: Long): Flow<LoanDetailCacheEntity?> =
        rows.map { list -> list.firstOrNull { it.loanId == loanId } }

    override suspend fun upsert(entity: LoanDetailCacheEntity) {
        val byKey = rows.value.associateBy { it.loanId }.toMutableMap()
        byKey[entity.loanId] = entity
        rows.value = byKey.values.toList()
    }

    override suspend fun deleteByKey(loanId: Long) {
        rows.value = rows.value.filterNot { it.loanId == loanId }
    }

    override suspend fun deleteAll() { rows.value = emptyList() }

    override suspend fun replaceForKey(entity: LoanDetailCacheEntity) {
        upsert(entity)
    }

    override suspend fun deleteOlderThan(epochMillis: Long) {
        rows.value = rows.value.filter { it.fetchedAt >= epochMillis }
    }
}

private fun cacheEntity(loanId: Long, memberName: String) = LoanDetailCacheEntity(
    loanId = loanId,
    memberId = 9L,
    memberName = memberName,
    loanProductName = "VSLA Group Loan",
    principalAmount = 5000.0,
    disbursedDate = "2026-01-15",
    interestRatePercent = 12.5,
    totalOutstanding = 3200.0,
    totalOverdue = 400.0,
    status = "ACTIVE",
    fineractLoanId = 555L,
    repaymentScheduleJson = "[]",
    repaymentHistoryJson = "[]",
    fetchedAt = 1L,
)
