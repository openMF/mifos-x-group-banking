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

import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkChangeEvent
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkInfo
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitor
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkStatus
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.base.store.infra.FetchedAtRepository
import kpt.core.base.store.screen.FetchPolicy
import kpt.core.base.store.screen.ScreenState
import org.mifos.groupbanking.core.database.loanlist.dao.LoanListDao
import org.mifos.groupbanking.core.database.loanlist.entity.LoanListEntity
import org.mifos.groupbanking.core.network.model.LoanAccountStatusDto
import org.mifos.groupbanking.core.network.model.LoanPageDto
import org.mifos.groupbanking.core.network.model.LoanSummaryDto
import org.mifos.groupbanking.core.network.service.loanlist.LoanApi
import org.mifos.groupbanking.core.store.loanlist.impl.provideLoansPagingStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * TDD coverage for [LoanRepositoryImpl] — verifies the repository surfaces the PAGINATED loan-list
 * store as an offline-first [kpt.core.base.store.paging.PagingScreenStream] via
 * `.asPagingScreenStream()`, mapping cached pages into `ScreenState.Content<List<LoanSummary>>`,
 * appending on load-more, and serving cached pages offline. The groupId is threaded through the
 * store's [kpt.core.base.store.paging.PageKey] `query` so the cache is per-group.
 */
class LoanRepositoryTest {

    @Test
    fun paging_stream_emits_Content_with_first_page_when_online() = runTest {
        val api = FakeLoanApi(pages = mapOf(key(GROUP, 0) to page(0, 3)))
        val dao = FakeLoanListDao()
        val repo = repository(api, dao, online = true)

        val state = repo.loansPagingStream(groupId = GROUP, scope = backgroundScope)
            .state.first { it is ScreenState.Content }

        val data = (state as ScreenState.Content).data
        assertEquals(3, data.size)
        assertEquals(0L, data.first().id)
    }

    @Test
    fun load_next_page_appends_second_page() = runTest {
        val api = FakeLoanApi(
            pages = mapOf(key(GROUP, 0) to page(0, 20), key(GROUP, 1) to page(20, 5)),
        )
        val dao = FakeLoanListDao()
        val repo = repository(api, dao, online = true)

        val stream = repo.loansPagingStream(groupId = GROUP, scope = backgroundScope)
        stream.state.first { it is ScreenState.Content && it.data.size == 20 }

        stream.loadNextPage()
        val appended = stream.state.first { it is ScreenState.Content && (it).data.size == 25 }

        assertEquals(25, (appended as ScreenState.Content).data.size, "load-more appends the second page")
    }

    @Test
    fun paging_stream_serves_cached_first_page_offline() = runTest {
        val api = FakeLoanApi(error = NetworkError.SERVER)
        val dao = FakeLoanListDao().apply { seedPage(GROUP.toString(), 0, entities(GROUP.toString(), 0, 4)) }
        val repo = repository(api, dao, online = false)

        val state = repo.loansPagingStream(
            groupId = GROUP,
            scope = backgroundScope,
            fetchPolicy = FetchPolicy.CACHE_ONLY,
        ).state.first { it is ScreenState.Content }

        val data = (state as ScreenState.Content).data
        assertEquals(4, data.size, "offline read must serve the persisted pages (offline: show_cached)")
        assertEquals(emptyList(), api.calls, "CACHE_ONLY must not hit the network")
    }

    // ─── wiring ──────────────────────────────────────────────────────────────

    private fun repository(
        api: LoanApi,
        dao: LoanListDao,
        online: Boolean,
    ): LoanRepository {
        val initial = if (online) {
            NetworkStatus.Available(NetworkInfo(type = NetworkType.WiFi, isMetered = false))
        } else {
            NetworkStatus.Unavailable
        }
        return LoanRepositoryImpl(
            loansPagingStore = provideLoansPagingStore(api, dao),
            networkMonitor = FakeNetworkMonitor(initial),
            fetchedAtRepository = InMemoryFetchedAtRepository(),
        )
    }

    private companion object {
        const val GROUP = 101L

        fun key(groupId: Long, pageIndex: Int): Pair<Long, Int> = groupId to pageIndex
    }
}

// ---------------------------------------------------------------------------
// Fakes + fixtures
// ---------------------------------------------------------------------------

private fun page(offset: Int, count: Int): LoanPageDto = LoanPageDto(
    totalFilteredRecords = 100,
    pageItems = (0 until count).map { i -> loanDto(offset + i) },
)

private fun loanDto(n: Int): LoanSummaryDto = LoanSummaryDto(
    id = n.toLong(),
    memberId = n.toLong(),
    memberName = "Member $n",
    memberPhotoUrl = null,
    loanProductName = "Group Loan",
    principalAmount = 1000.0 + n,
    outstandingBalance = 500.0 + n,
    overdueAmount = 0.0,
    status = LoanAccountStatusDto.ACTIVE,
    nextRepaymentDate = null,
    isOverdue = false,
    fineractLoanId = n.toLong(),
)

private fun entities(groupId: String, pageIndex: Int, count: Int): List<LoanListEntity> =
    (0 until count).map { i ->
        LoanListEntity(
            groupId = groupId,
            loanId = (pageIndex * 1000 + i).toLong(),
            pageIndex = pageIndex,
            rowOrder = i,
            memberId = i.toLong(),
            memberName = "Seed $i",
            memberPhotoUrl = null,
            loanProductName = "Group Loan",
            principalAmount = 1000.0,
            outstandingBalance = 500.0,
            overdueAmount = 0.0,
            status = "ACTIVE",
            nextRepaymentDate = null,
            isOverdue = false,
            fineractLoanId = i.toLong(),
            fetchedAt = 1L,
        )
    }

private class FakeLoanApi(
    private val pages: Map<Pair<Long, Int>, LoanPageDto> = emptyMap(),
    private val error: NetworkError? = null,
) : LoanApi {
    val calls = mutableListOf<Triple<Long, Int, Int>>()

    override suspend fun getGroupLoans(
        groupId: Long,
        limit: Int,
        offset: Int,
        loanStatus: String?,
    ): NetworkResult<LoanPageDto, NetworkError> {
        calls += Triple(groupId, offset, limit)
        error?.let { return NetworkResult.Error(it) }
        val pageIndex = offset / limit
        return NetworkResult.Success(pages[groupId to pageIndex] ?: LoanPageDto(totalFilteredRecords = 0))
    }
}

private class FakeLoanListDao : LoanListDao {
    private val rows = MutableStateFlow<List<LoanListEntity>>(emptyList())

    fun seedPage(groupId: String, pageIndex: Int, entities: List<LoanListEntity>) {
        rows.value = rows.value.filterNot { it.groupId == groupId && it.pageIndex == pageIndex } + entities
    }

    override fun observePage(groupId: String, pageIndex: Int): Flow<List<LoanListEntity>> =
        rows.map { all ->
            all.filter { it.groupId == groupId && it.pageIndex == pageIndex }.sortedBy { it.rowOrder }
        }

    override fun observeGroup(groupId: String): Flow<List<LoanListEntity>> =
        rows.map { all ->
            all.filter { it.groupId == groupId }.sortedWith(compareBy({ it.pageIndex }, { it.rowOrder }))
        }

    override suspend fun upsertAll(entities: List<LoanListEntity>) {
        val byKey = rows.value.associateBy { it.groupId to it.loanId }.toMutableMap()
        entities.forEach { byKey[it.groupId to it.loanId] = it }
        rows.value = byKey.values.toList()
    }

    override suspend fun deletePage(groupId: String, pageIndex: Int) {
        rows.value = rows.value.filterNot { it.groupId == groupId && it.pageIndex == pageIndex }
    }

    override suspend fun deleteGroup(groupId: String) {
        rows.value = rows.value.filterNot { it.groupId == groupId }
    }

    override suspend fun deleteAll() { rows.value = emptyList() }

    override suspend fun replacePage(groupId: String, pageIndex: Int, entities: List<LoanListEntity>) {
        deletePage(groupId, pageIndex)
        upsertAll(entities)
    }

    override suspend fun deleteOlderThan(epochMillis: Long) {
        rows.value = rows.value.filter { it.fetchedAt >= epochMillis }
    }
}

private class FakeNetworkMonitor(initialStatus: NetworkStatus) : NetworkMonitor {
    private val _status = MutableStateFlow(initialStatus)
    override val networkStatus: StateFlow<NetworkStatus> = _status.asStateFlow()
    override val isOnline: StateFlow<Boolean> =
        MutableStateFlow(initialStatus is NetworkStatus.Available).asStateFlow()
    override val networkChanges: SharedFlow<NetworkChangeEvent> =
        MutableSharedFlow<NetworkChangeEvent>().asSharedFlow()
    override fun close() = Unit
}

@OptIn(ExperimentalTime::class)
private class InMemoryFetchedAtRepository : FetchedAtRepository {
    private val map = mutableMapOf<String, Instant>()
    override suspend fun read(storeKey: String): Instant? = map[storeKey]
    override suspend fun write(storeKey: String, instant: Instant) { map[storeKey] = instant }
}
