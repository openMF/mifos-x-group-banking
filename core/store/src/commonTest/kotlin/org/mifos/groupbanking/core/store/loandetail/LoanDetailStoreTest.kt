/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.store.loandetail

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.database.loandetail.dao.LoanDetailDao
import org.mifos.groupbanking.core.database.loandetail.entity.LoanDetailCacheEntity
import org.mifos.groupbanking.core.model.LoanDetailResponse
import org.mifos.groupbanking.core.network.model.LoanAccountStatusDto
import org.mifos.groupbanking.core.network.model.LoanDetailDto
import org.mifos.groupbanking.core.network.model.LoanDetailResponseDto
import org.mifos.groupbanking.core.network.model.RepaymentRowStatusDto
import org.mifos.groupbanking.core.network.model.RepaymentScheduleRowDto
import org.mifos.groupbanking.core.network.model.RepaymentTransactionDto
import org.mifos.groupbanking.core.network.service.loandetail.LoanDetailApi
import org.mifos.groupbanking.core.store.loandetail.impl.LoanDetailFetchException
import org.mifos.groupbanking.core.store.loandetail.impl.provideLoanDetailStore
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD coverage for [provideLoanDetailStore] — the single-key NETWORK_WITH_CACHE loan-detail store.
 *
 * Exercises the Store5 read pipeline directly (fetcher + Room-shaped SourceOfTruth + validator) with
 * an in-memory [FakeLoanDetailDao] and a scripted [FakeLoanDetailApi]; no Room runtime and no
 * NetworkMonitor needed at this layer (that is covered by the repository test). The single
 * composite endpoint returns the loan header + repayment schedule + transaction history in one call,
 * so the store never fans in — the round-trip through the JSON-column SoT is asserted explicitly.
 */
class LoanDetailStoreTest {

    // ─── cache miss → fetcher runs once, snapshot persisted + emitted ────────────────────────────
    @Test
    fun cache_miss_calls_fetcher_persists_to_sot_emits() = runTest {
        val api = FakeLoanDetailApi(NetworkResult.Success(loanDetailDto(loanId = 42L, memberName = "Asha")))
        val dao = FakeLoanDetailDao()
        val store = provideLoanDetailStore(api, dao)

        val data = store.awaitFreshData(42L)

        assertEquals("Asha", data.loan.memberName)
        assertEquals(42L, data.loan.id)
        assertEquals(1, api.callCount, "fetcher must run exactly once on cache miss")
        assertEquals(42L, api.lastLoanId, "the store key must be forwarded as the loanId")
        assertEquals(1, dao.currentRows().size, "fetched detail must be written through to the SoT")
        assertEquals(42L, dao.currentRows().first().loanId)
    }

    // ─── cache hit → SoT emit, fetcher NOT called ────────────────────────────────────────────────
    @Test
    fun cache_hit_emits_from_sot_without_fetcher_call() = runTest {
        val api = FakeLoanDetailApi(NetworkResult.Success(loanDetailDto(loanId = 7L, memberName = "net")))
        val dao = FakeLoanDetailDao().apply { seed(cacheEntity(7L, memberName = "cached")) }
        val store = provideLoanDetailStore(api, dao)

        val response = store.stream(StoreReadRequest.cached(7L, refresh = false))
            .first { it is StoreReadResponse.Data<*> }

        @Suppress("UNCHECKED_CAST")
        val data = (response as StoreReadResponse.Data<LoanDetailResponse>).value
        assertEquals("cached", data.loan.memberName)
        assertEquals(0, api.callCount, "cache hit must be served from the SoT without a network fetch")
    }

    // ─── validator-expiry / refresh → re-fetch (SWR revalidation) ────────────────────────────────
    @Test
    fun refresh_refetches_from_api() = runTest {
        val api = FakeLoanDetailApi(NetworkResult.Success(loanDetailDto(loanId = 42L, memberName = "Asha")))
        val dao = FakeLoanDetailDao()
        val store = provideLoanDetailStore(api, dao)

        store.awaitFreshData(42L)
        store.awaitFreshData(42L)

        assertEquals(2, api.callCount, "an explicit fresh() read must re-drive the fetcher (SWR revalidation)")
    }

    // ─── fetcher error surfaces on the Store5 error channel ──────────────────────────────────────
    @Test
    fun fetcher_error_surfaces_as_error_response() = runTest {
        val api = FakeLoanDetailApi(NetworkResult.Error(NetworkError.SERVER))
        val dao = FakeLoanDetailDao()
        val store = provideLoanDetailStore(api, dao)

        val response = store.stream(StoreReadRequest.fresh(42L))
            .first { it is StoreReadResponse.Error }

        val error = (response as StoreReadResponse.Error.Exception).error
        assertTrue(error is LoanDetailFetchException, "error channel must carry the typed fetch exception")
        assertEquals(NetworkError.SERVER, error.networkError)
    }

    // ─── round-trip: nested schedule + history lists survive the JSON-column SoT ──────────────────
    @Test
    fun fetched_detail_round_trips_nested_lists_through_sot() = runTest {
        val api = FakeLoanDetailApi(NetworkResult.Success(loanDetailDto(loanId = 42L, memberName = "Asha")))
        val dao = FakeLoanDetailDao()
        val store = provideLoanDetailStore(api, dao)

        val data = store.awaitFreshData(42L)

        assertEquals(2, data.repaymentSchedule.size, "repaymentSchedule must round-trip through the JSON SoT column")
        assertEquals(1, data.repaymentHistory.size, "repaymentHistory must round-trip through the JSON SoT column")
        assertEquals(101L, data.repaymentHistory.first().id)
        assertEquals("REPAYMENT", data.repaymentHistory.first().type)
    }

    // ─── helpers ─────────────────────────────────────────────────────────────────────────────────

    private suspend fun Store<Long, LoanDetailResponse>.awaitFreshData(key: Long): LoanDetailResponse {
        val response = stream(StoreReadRequest.fresh(key))
            .first { it is StoreReadResponse.Data<*> }
        @Suppress("UNCHECKED_CAST")
        return (response as StoreReadResponse.Data<LoanDetailResponse>).value
    }
}

// ---------------------------------------------------------------------------
// Fakes + fixtures
// ---------------------------------------------------------------------------

private class FakeLoanDetailApi(
    private val result: NetworkResult<LoanDetailResponseDto, NetworkError>,
) : LoanDetailApi {
    var callCount: Int = 0
        private set
    var lastLoanId: Long? = null
        private set

    override suspend fun getLoanDetail(
        loanId: Long,
        associations: String,
    ): NetworkResult<LoanDetailResponseDto, NetworkError> {
        callCount++
        lastLoanId = loanId
        return result
    }
}

private class FakeLoanDetailDao : LoanDetailDao {
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

private fun loanDetailDto(loanId: Long, memberName: String) = LoanDetailResponseDto(
    loan = LoanDetailDto(
        id = loanId,
        memberId = 9L,
        memberName = memberName,
        loanProductName = "VSLA Group Loan",
        principalAmount = 5000.0,
        disbursedDate = "2026-01-15",
        interestRatePercent = 12.5,
        totalOutstanding = 3200.0,
        totalOverdue = 400.0,
        status = LoanAccountStatusDto.ACTIVE,
        fineractLoanId = 555L,
    ),
    repaymentSchedule = listOf(
        RepaymentScheduleRowDto(
            weekNumber = 1,
            dueDate = "2026-02-01",
            dueAmount = 500.0,
            paidAmount = 500.0,
            balance = 4500.0,
            status = RepaymentRowStatusDto.PAID,
        ),
        RepaymentScheduleRowDto(
            weekNumber = 2,
            dueDate = "2026-02-08",
            dueAmount = 500.0,
            paidAmount = 0.0,
            balance = 4500.0,
            status = RepaymentRowStatusDto.OVERDUE,
        ),
    ),
    transactions = listOf(
        RepaymentTransactionDto(id = 101L, type = "REPAYMENT", date = "2026-02-01", amount = 500.0),
    ),
)

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
