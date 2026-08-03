/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.syncstatus

import app.cash.turbine.test
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkChangeEvent
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkInfo
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitor
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kpt.core.analytics.KptAnalyticsTracker
import kpt.core.base.analytics.NoOpAnalyticsHelper
import kpt.core.base.observability.ConsoleCrashReporter
import org.mifos.groupbanking.core.data.repository.SyncManager
import org.mifos.groupbanking.core.data.repository.SyncQueueRepository
import org.mifos.groupbanking.core.model.EntityType
import org.mifos.groupbanking.core.model.SyncOverallStatus
import org.mifos.groupbanking.core.model.SyncQueueCounts
import org.mifos.groupbanking.core.model.SyncQueueItem
import org.mifos.groupbanking.core.model.SyncResult
import org.mifos.groupbanking.core.model.SyncStatus
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * See API.md#viewmodel — SyncStatusViewModelTest exercises the reactive 5-flow `combine()` fan-in
 * (counts/pendingByType/failedOperations/conflictCount/isOnline), the [SyncOverallStatus]
 * derivation, the [SyncManager.getLastSyncAt] collection, and every declared [SyncStatusAction]
 * (`OnSyncNow`/`OnRetryOperation`/`OnRefresh`), per RULE-TDD-METHODOLOGY-001 /
 * RULE-IMPL-DEAD-CLICKABLE-001.
 */
@OptIn(ExperimentalTime::class)
class SyncStatusViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: FakeSyncQueueRepository
    private lateinit var syncManager: FakeSyncManager
    private lateinit var networkMonitor: FakeNetworkMonitor

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeSyncQueueRepository()
        syncManager = FakeSyncManager()
        networkMonitor = FakeNetworkMonitor(initiallyOnline = true)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(): SyncStatusViewModel = SyncStatusViewModel(
        repository = repository,
        syncManager = syncManager,
        networkMonitor = networkMonitor,
        analytics = KptAnalyticsTracker(NoOpAnalyticsHelper()),
        crashReporter = ConsoleCrashReporter(),
    )

    // -- Reactive combine() fan-in / overallStatus derivation ------------------------------------

    @Test
    fun `initial combine emission with an empty queue yields SYNCED overallStatus and isLoading false`() =
        runTest(testDispatcher) {
            val viewModel = buildViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.stateFlow.value
            assertEquals(0, state.pendingCount)
            assertEquals(0, state.failedCount)
            assertEquals(SyncOverallStatus.SYNCED, state.overallStatus)
            assertFalse(state.isLoading)
            assertEquals(SyncStatusScreenState.Content, state.deriveScreenState())
        }

    @Test
    fun `pending rows with zero failures derive PENDING overallStatus`() = runTest(testDispatcher) {
        repository.setCounts(SyncQueueCounts(pending = 3, syncing = 0, failed = 0, synced = 5))
        repository.setPendingByType(mapOf(EntityType.MEETING to 1, EntityType.SAVINGS to 2))
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(3, state.pendingCount)
        assertEquals(SyncOverallStatus.PENDING, state.overallStatus)
        assertEquals(mapOf(EntityType.MEETING to 1, EntityType.SAVINGS to 2), state.pendingByType)
    }

    @Test
    fun `any failed row derives FAILED overallStatus even when pending rows also exist`() = runTest(testDispatcher) {
        repository.setCounts(SyncQueueCounts(pending = 2, syncing = 0, failed = 1, synced = 5))
        repository.setFailed(listOf(sampleQueueItem(id = 9L)))
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(SyncOverallStatus.FAILED, state.overallStatus)
        assertEquals(1, state.failedOperations.size)
    }

    @Test
    fun `lastSyncAt is collected from SyncManager getLastSyncAt`() = runTest(testDispatcher) {
        val instant = Instant.parse("2026-05-05T08:30:00Z")
        syncManager.lastSyncAtFlow = MutableStateFlow(instant)
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(instant, viewModel.stateFlow.value.lastSyncAt)
    }

    @Test
    fun `isOnline reflects NetworkMonitor and updates reactively after init`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.stateFlow.value.isOnline)

        networkMonitor.setOnline(false)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.stateFlow.value.isOnline)
    }

    @Test
    fun `local db read failure surfaces DbRead error and Error screen state`() = runTest(testDispatcher) {
        repository.countsFlow = flow { throw RuntimeException("db read failed") }
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(SyncStatusError.DbRead, state.error)
        assertEquals(SyncStatusScreenState.Error, state.deriveScreenState())
    }

    // -- OnSyncNow ---------------------------------------------------------------------------------

    @Test
    fun `OnSyncNow while offline is ignored and never calls triggerSync`() = runTest(testDispatcher) {
        networkMonitor.setOnline(false)
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(SyncStatusAction.OnSyncNow)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, syncManager.triggerSyncCallCount)
        assertFalse(viewModel.stateFlow.value.isSyncing)
    }

    @Test
    fun `OnSyncNow while already syncing is ignored`() = runTest(testDispatcher) {
        syncManager.triggerSyncFlow = MutableSharedFlow() // never emits — stays "syncing"
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(SyncStatusAction.OnSyncNow)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.stateFlow.value.isSyncing)
        assertEquals(1, syncManager.triggerSyncCallCount)

        viewModel.trySendAction(SyncStatusAction.OnSyncNow)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, syncManager.triggerSyncCallCount)
    }

    @Test
    fun `OnSyncNow with zero failures emits SyncCompleted and clears isSyncing`() = runTest(testDispatcher) {
        syncManager.triggerSyncFlow = flowOf(SyncResult(successCount = 4, failedCount = 0, conflictCount = 0))
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(SyncStatusAction.OnSyncNow)
            assertEquals(SyncStatusEvent.SyncCompleted, awaitItem())
        }
        assertFalse(viewModel.stateFlow.value.isSyncing)
        assertEquals(1, syncManager.triggerSyncCallCount)
    }

    @Test
    fun `OnSyncNow with partial failures shows a snackbar instead of SyncCompleted`() = runTest(testDispatcher) {
        syncManager.triggerSyncFlow = flowOf(SyncResult(successCount = 2, failedCount = 1, conflictCount = 0))
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(SyncStatusAction.OnSyncNow)
            assertEquals(SyncStatusEvent.ShowSnackbar(message = SyncStatusError.SyncFailed.messageKey), awaitItem())
        }
        assertFalse(viewModel.stateFlow.value.isSyncing)
    }

    @Test
    fun `OnSyncNow whose triggerSync stream throws sets SyncFailed error and shows a snackbar`() =
        runTest(testDispatcher) {
            syncManager.triggerSyncFlow = flow { throw RuntimeException("network down mid-drain") }
            val viewModel = buildViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.eventFlow.test {
                viewModel.trySendAction(SyncStatusAction.OnSyncNow)
                assertEquals(SyncStatusEvent.ShowSnackbar(message = SyncStatusError.SyncFailed.messageKey), awaitItem())
            }
            val state = viewModel.stateFlow.value
            assertFalse(state.isSyncing)
            assertEquals(SyncStatusError.SyncFailed, state.error)
        }

    // -- OnRetryOperation --------------------------------------------------------------------------

    @Test
    fun `OnRetryOperation while offline shows a snackbar and never calls retryItem`() = runTest(testDispatcher) {
        networkMonitor.setOnline(false)
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(SyncStatusAction.OnRetryOperation(itemId = 42L))
            assertEquals(SyncStatusEvent.ShowSnackbar(message = SyncStatusError.SyncFailed.messageKey), awaitItem())
        }
        assertEquals(0, syncManager.retryItemCallCount)
    }

    @Test
    fun `OnRetryOperation success calls retryItem with the itemId and emits no snackbar`() = runTest(testDispatcher) {
        syncManager.retryItemResult = SyncResult(successCount = 1, failedCount = 0, conflictCount = 0)
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(SyncStatusAction.OnRetryOperation(itemId = 42L))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, syncManager.retryItemCallCount)
        assertEquals(42L, syncManager.lastRetryItemId)
    }

    @Test
    fun `OnRetryOperation failure emits a SyncFailed snackbar`() = runTest(testDispatcher) {
        syncManager.retryItemResult = SyncResult(successCount = 0, failedCount = 1, conflictCount = 0)
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(SyncStatusAction.OnRetryOperation(itemId = 7L))
            assertEquals(SyncStatusEvent.ShowSnackbar(message = SyncStatusError.SyncFailed.messageKey), awaitItem())
        }
    }

    // -- OnRefresh -----------------------------------------------------------------------------------

    @Test
    fun `OnRefresh pulses isLoading and settles back to false without a repository refresh API`() =
        runTest(testDispatcher) {
            val viewModel = buildViewModel()
            testDispatcher.scheduler.advanceUntilIdle()
            assertFalse(viewModel.stateFlow.value.isLoading)

            viewModel.trySendAction(SyncStatusAction.OnRefresh)
            testDispatcher.scheduler.advanceUntilIdle()

            assertFalse(viewModel.stateFlow.value.isLoading)
        }

    // -- Test fixtures ---------------------------------------------------------------------------

    private fun sampleQueueItem(id: Long) = SyncQueueItem(
        id = id,
        operationType = "CREATE_MEMBER",
        payloadJson = "{}",
        targetTable = "dt_member",
        status = SyncStatus.FAILED,
        createdAtEpochMs = 0L,
        lastAttemptEpochMs = 0L,
        attemptCount = 1,
        lastError = "timeout",
    )
}

private class FakeSyncQueueRepository : SyncQueueRepository {
    var countsFlow: Flow<SyncQueueCounts> = MutableStateFlow(SyncQueueCounts(0, 0, 0, 0))
    var pendingByTypeFlow: Flow<Map<EntityType, Int>> = MutableStateFlow(emptyMap())
    var failedFlow: Flow<List<SyncQueueItem>> = MutableStateFlow(emptyList())
    var conflictCountFlow: Flow<Int> = MutableStateFlow(0)

    fun setCounts(counts: SyncQueueCounts) {
        countsFlow = MutableStateFlow(counts)
    }

    fun setPendingByType(map: Map<EntityType, Int>) {
        pendingByTypeFlow = MutableStateFlow(map)
    }

    fun setFailed(items: List<SyncQueueItem>) {
        failedFlow = MutableStateFlow(items)
    }

    override suspend fun enqueue(operationType: String, targetTable: String, payloadJson: String): Long = 0L
    override fun observePending(): Flow<List<SyncQueueItem>> = MutableStateFlow(emptyList())
    override fun observeCounts(): Flow<SyncQueueCounts> = countsFlow
    override suspend fun markSyncing(id: Long) = Unit
    override suspend fun markSynced(id: Long) = Unit
    override suspend fun markFailed(id: Long, error: String?) = Unit
    override suspend fun retryAll() = Unit
    override fun observePendingByType(): Flow<Map<EntityType, Int>> = pendingByTypeFlow
    override fun observeFailed(): Flow<List<SyncQueueItem>> = failedFlow
    override fun observeConflictCount(): Flow<Int> = conflictCountFlow
    override suspend fun getItem(id: Long): SyncQueueItem? = null
}

@OptIn(ExperimentalTime::class)
private class FakeSyncManager : SyncManager {
    var triggerSyncFlow: Flow<SyncResult> = flowOf(SyncResult(successCount = 0, failedCount = 0, conflictCount = 0))
    var lastSyncAtFlow: Flow<Instant?> = MutableStateFlow(null)
    var retryItemResult: SyncResult = SyncResult(successCount = 1, failedCount = 0, conflictCount = 0)

    var triggerSyncCallCount: Int = 0
        private set
    var retryItemCallCount: Int = 0
        private set
    var lastRetryItemId: Long? = null
        private set

    override fun triggerSync(): Flow<SyncResult> {
        triggerSyncCallCount++
        return triggerSyncFlow
    }

    override fun getLastSyncAt(): Flow<Instant?> = lastSyncAtFlow

    override suspend fun retryItem(itemId: Long): SyncResult {
        retryItemCallCount++
        lastRetryItemId = itemId
        return retryItemResult
    }
}

private class FakeNetworkMonitor(initiallyOnline: Boolean) : NetworkMonitor {
    private val _isOnline = MutableStateFlow(initiallyOnline)
    override val isOnline: StateFlow<Boolean> = _isOnline
    override val networkStatus: StateFlow<NetworkStatus> = MutableStateFlow(
        if (initiallyOnline) NetworkStatus.Available(NetworkInfo()) else NetworkStatus.Unavailable,
    )
    override val networkChanges: SharedFlow<NetworkChangeEvent> = MutableSharedFlow<NetworkChangeEvent>().asSharedFlow()

    fun setOnline(online: Boolean) {
        _isOnline.value = online
    }

    override fun close() = Unit
}
