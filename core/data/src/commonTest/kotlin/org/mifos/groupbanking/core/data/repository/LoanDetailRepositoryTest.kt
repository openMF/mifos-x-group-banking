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

import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkInfo
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkStatus
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.base.store.screen.FetchPolicy
import kpt.core.base.store.screen.ScreenState
import org.mifos.groupbanking.core.database.loandetail.dao.LoanDetailDao
import org.mifos.groupbanking.core.database.loandetail.entity.LoanDetailCacheEntity
import org.mifos.groupbanking.core.network.model.LoanAccountStatusDto
import org.mifos.groupbanking.core.network.model.LoanDetailDto
import org.mifos.groupbanking.core.network.model.LoanDetailResponseDto
import org.mifos.groupbanking.core.network.model.RepaymentTransactionDto
import org.mifos.groupbanking.core.network.service.loandetail.LoanDetailApi
import org.mifos.groupbanking.core.store.loandetail.impl.provideLoanDetailStore
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * TDD coverage for [LoanDetailRepositoryImpl] — verifies the repository surfaces the single-key
 * store as an offline-first [kpt.core.base.store.screen.ScreenDataStream] via `.asScreenStream()`,
 * mapping the cached snapshot into `ScreenState.Content<LoanDetailResponse>`.
 */
class LoanDetailRepositoryTest {

    @Test
    fun stream_emits_Content_with_mapped_detail_when_online() = runTest {
        val api = FakeLoanDetailApi(NetworkResult.Success(loanDetailDto(42L, "Asha")))
        val dao = FakeLoanDetailDao()
        val repo = repository(api, dao, online = true)

        val state = repo.loanDetailStream(
            loanId = 42L,
            scope = backgroundScope,
            fetchPolicy = FetchPolicy.NETWORK_WITH_CACHE,
        ).state.first { it is ScreenState.Content }

        val data = (state as ScreenState.Content).data
        assertEquals("Asha", data.loan.memberName)
        assertEquals(42L, data.loan.id)
        assertEquals(1, data.repaymentHistory.size)
    }

    @Test
    fun stream_serves_cached_detail_offline() = runTest {
        val api = FakeLoanDetailApi(NetworkResult.Error(NetworkError.SERVER))
        val dao = FakeLoanDetailDao().apply { seed(cacheEntity(42L, "cached-Asha")) }
        val repo = repository(api, dao, online = false)

        val state = repo.loanDetailStream(
            loanId = 42L,
            scope = backgroundScope,
            fetchPolicy = FetchPolicy.CACHE_ONLY,
        ).state.first { it is ScreenState.Content }

        val data = (state as ScreenState.Content).data
        assertEquals("cached-Asha", data.loan.memberName, "offline read must serve the persisted snapshot (offline cache)")
        assertEquals(0, api.callCount, "CACHE_ONLY must not hit the network")
    }

    // ─── wiring ──────────────────────────────────────────────────────────────

    private fun repository(
        api: LoanDetailApi,
        dao: LoanDetailDao,
        online: Boolean,
    ): LoanDetailRepository {
        val initial = if (online) {
            NetworkStatus.Available(NetworkInfo(type = NetworkType.WiFi, isMetered = false))
        } else {
            NetworkStatus.Unavailable
        }
        return LoanDetailRepositoryImpl(
            loanDetailStore = provideLoanDetailStore(api, dao),
            networkMonitor = FakeNetworkMonitor(initial),
            fetchedAtRepository = InMemoryFetchedAtRepository(),
        )
    }
}

// ---------------------------------------------------------------------------
// Fakes
// ---------------------------------------------------------------------------

private class FakeLoanDetailApi(
    private val result: NetworkResult<LoanDetailResponseDto, NetworkError>,
) : LoanDetailApi {
    var callCount: Int = 0
        private set

    override suspend fun getLoanDetail(
        loanId: Long,
        associations: String,
    ): NetworkResult<LoanDetailResponseDto, NetworkError> {
        callCount++
        return result
    }
}

private class FakeLoanDetailDao : LoanDetailDao {
    private val rows = MutableStateFlow<List<LoanDetailCacheEntity>>(emptyList())

    fun seed(entity: LoanDetailCacheEntity) {
        rows.value = listOf(entity)
    }

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
    repaymentSchedule = emptyList(),
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
