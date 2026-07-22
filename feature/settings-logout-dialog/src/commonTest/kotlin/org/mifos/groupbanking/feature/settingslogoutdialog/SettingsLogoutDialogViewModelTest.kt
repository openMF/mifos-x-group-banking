/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.settingslogoutdialog

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kpt.core.analytics.KptAnalyticsTracker
import kpt.core.base.analytics.NoOpAnalyticsHelper
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.base.observability.ConsoleCrashReporter
import org.mifos.groupbanking.core.data.repository.AuthRepository
import org.mifos.groupbanking.core.data.repository.SyncQueueRepository
import org.mifos.groupbanking.core.model.AuthSession
import org.mifos.groupbanking.core.model.EntityType
import org.mifos.groupbanking.core.model.LoginCredentials
import org.mifos.groupbanking.core.model.SelfRegistration
import org.mifos.groupbanking.core.model.SyncQueueCounts
import org.mifos.groupbanking.core.model.SyncQueueItem
import org.mifos.groupbanking.core.model.UserProfile
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * See API.md#viewmodel — `SettingsLogoutDialogViewModelTest` exercises every declared
 * [SettingsLogoutDialogAction] path (confirm success, confirm transport failure, double-tap
 * guard, dismiss, and the [SyncQueueRepository.observeCounts] unsynced-count seed), per
 * RULE-TDD-METHODOLOGY-001 / RULE-IMPL-DEAD-CLICKABLE-001. Written FIRST, ahead of
 * [SettingsLogoutDialogViewModel] itself, per RULE-TDD-GLOBAL-001.
 */
class SettingsLogoutDialogViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var authRepository: FakeAuthRepository
    private lateinit var syncQueueRepository: FakeSyncQueueRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = FakeAuthRepository()
        syncQueueRepository = FakeSyncQueueRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(): SettingsLogoutDialogViewModel = SettingsLogoutDialogViewModel(
        authRepository = authRepository,
        syncQueueRepository = syncQueueRepository,
        analytics = KptAnalyticsTracker(NoOpAnalyticsHelper()),
        crashReporter = ConsoleCrashReporter(),
    )

    // -- Initial state / unsynced-count seed -------------------------------------------------------

    @Test
    fun `initial state is idle with zero unsyncedCount before counts are collected`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()

        val state = viewModel.stateFlow.value
        assertEquals(false, state.isLoggingOut)
        assertNull(state.logoutError)
        assertEquals(0, state.unsyncedCount)
    }

    @Test
    fun `unsyncedCount derives from pending plus failed SyncQueueCounts once collected`() = runTest(testDispatcher) {
        syncQueueRepository.counts.value = SyncQueueCounts(pending = 3, syncing = 1, failed = 2, synced = 10)
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(5, viewModel.stateFlow.value.unsyncedCount)
    }

    // -- OnDismiss ------------------------------------------------------------------------------------

    @Test
    fun `OnDismiss emits Dismiss without touching the session`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(SettingsLogoutDialogAction.OnDismiss)
            assertEquals(SettingsLogoutDialogEvent.Dismiss, awaitItem())
        }
        assertEquals(0, authRepository.clearSessionCallCount)
    }

    // -- OnConfirmLogout success ------------------------------------------------------------------

    @Test
    fun `OnConfirmLogout success clears the session and emits NavigateToLogin`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(SettingsLogoutDialogAction.OnConfirmLogout)
            assertEquals(SettingsLogoutDialogEvent.NavigateToLogin, awaitItem())
        }

        val state = viewModel.stateFlow.value
        assertEquals(false, state.isLoggingOut)
        assertNull(state.logoutError)
        assertEquals(1, authRepository.clearSessionCallCount)
    }

    // -- OnConfirmLogout failure ------------------------------------------------------------------

    @Test
    fun `OnConfirmLogout failure sets logoutError and resets isLoggingOut without navigating`() =
        runTest(testDispatcher) {
            authRepository.clearSessionThrows = IllegalStateException("keystore locked")
            val viewModel = buildViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.trySendAction(SettingsLogoutDialogAction.OnConfirmLogout)
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.stateFlow.value
            assertEquals(false, state.isLoggingOut)
            assertEquals("error_logout_failed", state.logoutError)
            assertEquals(1, authRepository.clearSessionCallCount)
        }

    // -- Double-tap guard -------------------------------------------------------------------------

    @Test
    fun `OnConfirmLogout while already logging out is a no-op guard`() = runTest(testDispatcher) {
        authRepository.clearSessionDelaysForever = true
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(SettingsLogoutDialogAction.OnConfirmLogout)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(true, viewModel.stateFlow.value.isLoggingOut)

        viewModel.trySendAction(SettingsLogoutDialogAction.OnConfirmLogout)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, authRepository.clearSessionCallCount)
    }
}

private class FakeAuthRepository : AuthRepository {
    override val currentSession: Flow<AuthSession?> = MutableStateFlow(null)

    var clearSessionCallCount: Int = 0
        private set
    var clearSessionThrows: Throwable? = null
    var clearSessionDelaysForever: Boolean = false

    override suspend fun selfRegister(registration: SelfRegistration): NetworkResult<AuthSession, NetworkError> =
        error("not used by SettingsLogoutDialogViewModelTest")

    override suspend fun login(credentials: LoginCredentials): NetworkResult<AuthSession, NetworkError> =
        error("not used by SettingsLogoutDialogViewModelTest")

    override suspend fun refreshSession(sessionToken: String): NetworkResult<UserProfile, NetworkError> =
        error("not used by SettingsLogoutDialogViewModelTest")

    override suspend fun clearSession() {
        clearSessionCallCount++
        clearSessionThrows?.let { throw it }
        if (clearSessionDelaysForever) {
            awaitCancellation()
        }
    }
}

private class FakeSyncQueueRepository : SyncQueueRepository {
    val counts = MutableStateFlow(SyncQueueCounts(pending = 0, syncing = 0, failed = 0, synced = 0))

    override suspend fun enqueue(operationType: String, targetTable: String, payloadJson: String): Long =
        error("not used by SettingsLogoutDialogViewModelTest")

    override fun observePending(): Flow<List<SyncQueueItem>> = MutableStateFlow(emptyList())

    override fun observeCounts(): Flow<SyncQueueCounts> = counts

    override suspend fun markSyncing(id: Long) = error("not used by SettingsLogoutDialogViewModelTest")

    override suspend fun markSynced(id: Long) = error("not used by SettingsLogoutDialogViewModelTest")

    override suspend fun markFailed(id: Long, error: String?) =
        error("not used by SettingsLogoutDialogViewModelTest")

    override suspend fun retryAll() = error("not used by SettingsLogoutDialogViewModelTest")

    override fun observePendingByType(): Flow<Map<EntityType, Int>> = MutableStateFlow(emptyMap())

    override fun observeFailed(): Flow<List<SyncQueueItem>> = MutableStateFlow(emptyList())

    override fun observeConflictCount(): Flow<Int> = MutableStateFlow(0)

    override suspend fun getItem(id: Long): SyncQueueItem? = error("not used by SettingsLogoutDialogViewModelTest")
}
