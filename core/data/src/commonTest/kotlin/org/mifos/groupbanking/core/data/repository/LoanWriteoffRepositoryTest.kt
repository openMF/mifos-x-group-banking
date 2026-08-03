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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.database.loandetail.dao.LoanDetailDao
import org.mifos.groupbanking.core.database.loandetail.entity.LoanDetailCacheEntity
import org.mifos.groupbanking.core.network.model.LoanDetailResponseDto
import org.mifos.groupbanking.core.network.model.WriteoffLoanRequestDto
import org.mifos.groupbanking.core.network.model.WriteoffLoanResponseDto
import org.mifos.groupbanking.core.network.service.loandetail.LoanDetailApi
import org.mifos.groupbanking.core.network.service.loanwriteoff.LoanWriteoffApi
import org.mifos.groupbanking.core.store.loandetail.impl.provideLoanDetailStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD coverage for [LoanWriteoffRepositoryImpl] — verifies (1) a successful `write_off_loan`
 * write threads a caller-supplied wire date through to the request DTO and returns the mapped
 * [org.mifos.groupbanking.core.model.WriteoffResult]; (2) success INVALIDATES the loan-detail
 * composite cache for that loanId through the store (`loanDetailStore.clear(loanId)`) so the
 * still-active `LoanDetailRepository.loanDetailStream` re-fetches with the now-defaulted status
 * (`data-flow.yaml#cache.strategy: invalidate`, RULE-IMPLEMENT-STORE5-001 S5-1 — not a DAO
 * bypass); and (3) a failed write leaves the cache intact and surfaces the wire error verbatim.
 * No try-catch anywhere in the repository under test (Mandatory Rule 4) — every branch is a
 * plain `when` over the fake service's [NetworkResult]. Mirrors
 * [org.mifos.groupbanking.core.data.repository.LoanRepaymentRepositoryTest]'s wiring pattern
 * exactly. This mutation is irreversible and has NO offline queue — a failed/offline write is
 * simply surfaced as an error, never retried/queued.
 */
class LoanWriteoffRepositoryTest {

    @Test
    fun writeoffLoan_success_returnsMappedResult() = runTest {
        val writeoffApi = FakeLoanWriteoffApi(
            result = NetworkResult.Success(WriteoffLoanResponseDto(officeId = 1, clientId = 9L, loanId = 500L, resourceId = 888L)),
        )
        val repo = repository(writeoffApi, FakeLoanWriteoffDetailApi(), FakeLoanWriteoffDetailDao())

        val result = repo.writeoffLoan(loanId = 500L)

        check(result is NetworkResult.Success)
        assertEquals(888L, result.data.resourceId)
        assertEquals(500L, result.data.loanId)
        assertEquals(1, result.data.officeId)
        assertEquals(9L, result.data.clientId)
        assertEquals(500L, writeoffApi.lastLoanId)
    }

    @Test
    fun writeoffLoan_success_threadsATransactionDateIntoTheRequestDto() = runTest {
        val writeoffApi = FakeLoanWriteoffApi(
            result = NetworkResult.Success(WriteoffLoanResponseDto(officeId = 1, clientId = 9L, loanId = 500L, resourceId = 888L)),
        )
        val repo = repository(writeoffApi, FakeLoanWriteoffDetailApi(), FakeLoanWriteoffDetailDao())

        repo.writeoffLoan(loanId = 500L)

        assertTrue(!writeoffApi.lastRequest?.transactionDate.isNullOrBlank())
        assertEquals("en", writeoffApi.lastRequest?.locale)
        assertEquals("dd MMMM yyyy", writeoffApi.lastRequest?.dateFormat)
    }

    @Test
    fun writeoffLoan_success_invalidatesLoanDetailCacheSoLoanDetailRefetches() = runTest {
        val writeoffApi = FakeLoanWriteoffApi(
            result = NetworkResult.Success(WriteoffLoanResponseDto(officeId = 1, clientId = 9L, loanId = 500L, resourceId = 888L)),
        )
        // Seed a cached loan-detail row so we can observe the invalidation delete the SoT row.
        val loanDetailDao = FakeLoanWriteoffDetailDao().apply { seed(cacheEntity(500L, "Asha")) }
        val repo = repository(writeoffApi, FakeLoanWriteoffDetailApi(), loanDetailDao)

        assertEquals(1, loanDetailDao.currentRows().size, "precondition: a cached loan-detail row exists")

        repo.writeoffLoan(loanId = 500L)

        assertEquals(
            0,
            loanDetailDao.currentRows().size,
            "a successful write-off must invalidate the loan-detail cache (store.clear) for the defaulted loan",
        )
    }

    @Test
    fun writeoffLoan_notFoundFailure_leavesLoanDetailCacheIntactAndSurfacesError() = runTest {
        val writeoffApi = FakeLoanWriteoffApi(result = NetworkResult.Error(NetworkError.NOT_FOUND))
        val loanDetailDao = FakeLoanWriteoffDetailDao().apply { seed(cacheEntity(500L, "Asha")) }
        val repo = repository(writeoffApi, FakeLoanWriteoffDetailApi(), loanDetailDao)

        val result = repo.writeoffLoan(loanId = 500L)

        assertEquals(NetworkResult.Error(NetworkError.NOT_FOUND), result)
        assertEquals(1, loanDetailDao.currentRows().size, "a failed write must NOT invalidate the cache")
    }

    @Test
    fun writeoffLoan_serverError_surfacesErrorVerbatimAndLeavesCacheIntact() = runTest {
        val writeoffApi = FakeLoanWriteoffApi(result = NetworkResult.Error(NetworkError.SERVER))
        val loanDetailDao = FakeLoanWriteoffDetailDao().apply { seed(cacheEntity(500L, "Asha")) }
        val repo = repository(writeoffApi, FakeLoanWriteoffDetailApi(), loanDetailDao)

        val result = repo.writeoffLoan(loanId = 500L)

        assertTrue(result is NetworkResult.Error)
        assertEquals(NetworkError.SERVER, (result as NetworkResult.Error).error)
        assertEquals(1, loanDetailDao.currentRows().size)
    }

    @Test
    fun writeoffLoan_offlineTransportFailure_surfacesUnknownErrorWithNoOfflineQueue() = runTest {
        // No offline queue for this irreversible mutation — a transport/offline failure maps to
        // NetworkError.UNKNOWN (LoanWriteoffApiImpl's transport-catch branch) and is surfaced
        // immediately, never silently queued/retried.
        val writeoffApi = FakeLoanWriteoffApi(result = NetworkResult.Error(NetworkError.UNKNOWN))
        val loanDetailDao = FakeLoanWriteoffDetailDao().apply { seed(cacheEntity(500L, "Asha")) }
        val repo = repository(writeoffApi, FakeLoanWriteoffDetailApi(), loanDetailDao)

        val result = repo.writeoffLoan(loanId = 500L)

        assertEquals(NetworkResult.Error(NetworkError.UNKNOWN), result)
        assertEquals(1, loanDetailDao.currentRows().size)
    }

    // --- wiring ----------------------------------------------------------------------------------

    private fun repository(
        writeoffApi: LoanWriteoffApi,
        loanDetailApi: LoanDetailApi,
        loanDetailDao: LoanDetailDao,
    ): LoanWriteoffRepository = LoanWriteoffRepositoryImpl(
        api = writeoffApi,
        loanDetailStore = provideLoanDetailStore(loanDetailApi, loanDetailDao),
    )
}

// ---------------------------------------------------------------------------
// Fakes
// ---------------------------------------------------------------------------

private class FakeLoanWriteoffApi(
    private val result: NetworkResult<WriteoffLoanResponseDto, NetworkError>,
) : LoanWriteoffApi {
    var lastLoanId: Long? = null
        private set
    var lastRequest: WriteoffLoanRequestDto? = null
        private set

    override suspend fun writeoffLoan(
        loanId: Long,
        request: WriteoffLoanRequestDto,
    ): NetworkResult<WriteoffLoanResponseDto, NetworkError> {
        lastLoanId = loanId
        lastRequest = request
        return result
    }
}

private class FakeLoanWriteoffDetailApi(
    private val result: NetworkResult<LoanDetailResponseDto, NetworkError> = NetworkResult.Error(NetworkError.UNKNOWN),
) : LoanDetailApi {
    override suspend fun getLoanDetail(
        loanId: Long,
        associations: String,
    ): NetworkResult<LoanDetailResponseDto, NetworkError> = result
}

private class FakeLoanWriteoffDetailDao : LoanDetailDao {
    private val rows = MutableStateFlow<List<LoanDetailCacheEntity>>(emptyList())

    fun seed(entity: LoanDetailCacheEntity) {
        rows.value = listOf(entity)
    }
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

    override suspend fun deleteAll() {
        rows.value = emptyList()
    }

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
