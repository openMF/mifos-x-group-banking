/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.shareoutpreview

import app.cash.turbine.test
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkChangeEvent
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkInfo
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitor
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kpt.core.analytics.KptAnalyticsTracker
import kpt.core.base.analytics.NoOpAnalyticsHelper
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.base.observability.ConsoleCrashReporter
import kpt.core.base.security.SecurityPolicy
import kpt.core.base.security.SessionManager
import org.mifos.groupbanking.core.data.repository.ShareOutRepository
import org.mifos.groupbanking.core.model.ContributionMode
import org.mifos.groupbanking.core.model.GroupTypeConfig
import org.mifos.groupbanking.core.model.GroupTypeSlug
import org.mifos.groupbanking.core.model.MemberPayout
import org.mifos.groupbanking.core.model.SavingsMechanism
import org.mifos.groupbanking.core.model.ShareOutPreview
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * See API.md#viewmodel — `ShareOutPreviewViewModelTest` exercises every declared
 * [ShareOutPreviewAction] path plus the [NetworkResult] -> [ShareOutPreviewState] mapping and the
 * ACCUMULATING vs ROTATING_PAYOUT screen-state routing, per RULE-TDD-METHODOLOGY-001 /
 * RULE-IMPL-DEAD-CLICKABLE-001. The on-mount load fires from `init`, so every test seeds
 * [FakeShareOutRepository.previewResult] before constructing the ViewModel.
 */
class ShareOutPreviewViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: FakeShareOutRepository
    private lateinit var networkMonitor: FakeShareOutNetworkMonitor
    private lateinit var sessionManager: SessionManager

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeShareOutRepository()
        networkMonitor = FakeShareOutNetworkMonitor(initiallyOnline = true)
        sessionManager = SessionManager(policy = SecurityPolicy())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(
        groupId: String = GROUP_ID,
        typeConfig: GroupTypeConfig = accumulatingTypeConfig(),
    ): ShareOutPreviewViewModel = ShareOutPreviewViewModel(
        repository = repository,
        networkMonitor = networkMonitor,
        sessionManager = sessionManager,
        crashReporter = ConsoleCrashReporter(),
        analytics = KptAnalyticsTracker(NoOpAnalyticsHelper()),
        groupId = groupId,
        typeConfig = typeConfig,
    )

    // -- initial state (nav-args seeded, load auto-fired but not yet resolved) ------------------

    @Test
    fun `initial state seeds groupId and is Loading before the auto-fired load resolves`() = runTest(testDispatcher) {
        repository.previewResult = NetworkResult.Success(accumulatingPreview())
        val viewModel = buildViewModel()

        val state = viewModel.stateFlow.value
        assertEquals(GROUP_ID, state.groupId)
        assertTrue(state.isLoading)
        assertTrue(state.poolModel.isBlank(), "poolModel must start blank — it is the 'no content yet' signal")
        assertEquals(ShareOutPreviewScreenState.Loading, state.deriveScreenState())
    }

    // -- load ACCUMULATING (on_mount, per-member payout table) ----------------------------------

    @Test
    fun `load ACCUMULATING success seeds fund summary and payouts and derives ContentAccumulating`() =
        runTest(testDispatcher) {
            repository.previewResult = NetworkResult.Success(accumulatingPreview())
            val viewModel = buildViewModel()

            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.stateFlow.value
            assertFalse(state.isLoading)
            assertNull(state.error)
            assertEquals("ACCUMULATING", state.poolModel)
            assertEquals("PRORATA_SHARES", state.shareoutFormula)
            assertEquals(20000.0, state.totalCorpus)
            assertEquals(4000.0, state.totalProfit)
            assertEquals(24000.0, state.totalPool)
            assertEquals(1, state.cycleNumber)
            assertEquals(5, state.memberPayouts.size)
            assertNull(state.rotationPosition)
            assertEquals(1, repository.callCount)
            assertEquals(ShareOutPreviewScreenState.ContentAccumulating, state.deriveScreenState())
        }

    // -- load ROTATING_PAYOUT (on_mount, rotation next-recipient card) ---------------------------

    @Test
    fun `load ROTATING_PAYOUT success seeds rotation fields and derives ContentRotating`() = runTest(testDispatcher) {
        repository.previewResult = NetworkResult.Success(rotatingPreview())
        val viewModel = buildViewModel(typeConfig = rotatingTypeConfig())

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals("ROTATING_PAYOUT", state.poolModel)
        assertEquals("FIXED_ORDER", state.shareoutFormula)
        assertTrue(state.memberPayouts.isEmpty())
        assertEquals(5, state.rotationPosition)
        assertEquals("Grace Wanjiku", state.nextRecipientName)
        assertEquals(10000.0, state.nextRecipientAmount)
        assertEquals(ShareOutPreviewScreenState.ContentRotating, state.deriveScreenState())
    }

    // -- load error paths ------------------------------------------------------------------------

    @Test
    fun `load NOT_FOUND with no content derives Error and emits Server ShowSnackbar`() = runTest(testDispatcher) {
        repository.previewResult = NetworkResult.Error(NetworkError.NOT_FOUND)
        val viewModel = buildViewModel()

        viewModel.eventFlow.test {
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(ShareOutPreviewEvent.ShowSnackbar("error_server"), awaitItem())
        }

        val state = viewModel.stateFlow.value
        assertEquals(ShareOutPreviewError.Server, state.error)
        assertEquals(ShareOutPreviewScreenState.Error, state.deriveScreenState())
    }

    @Test
    fun `load UNAUTHORIZED ends the session and emits Auth ShowSnackbar`() = runTest(testDispatcher) {
        sessionManager.startSession()
        assertTrue(sessionManager.isSessionActive.value)
        repository.previewResult = NetworkResult.Error(NetworkError.UNAUTHORIZED)
        val viewModel = buildViewModel()

        viewModel.eventFlow.test {
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(ShareOutPreviewEvent.ShowSnackbar("error_auth"), awaitItem())
        }

        assertEquals(ShareOutPreviewError.Auth, viewModel.stateFlow.value.error)
        assertFalse(sessionManager.isSessionActive.value, "401 must call sessionManager.endSession()")
    }

    @Test
    fun `load REQUEST_TIMEOUT maps to Network error which is retryable`() = runTest(testDispatcher) {
        repository.previewResult = NetworkResult.Error(NetworkError.REQUEST_TIMEOUT)
        val viewModel = buildViewModel()

        testDispatcher.scheduler.advanceUntilIdle()

        val error = viewModel.stateFlow.value.error
        assertEquals(ShareOutPreviewError.Network, error)
        assertTrue(error?.retry == true)
    }

    // -- Retry (error-state CTA) -----------------------------------------------------------------

    @Test
    fun `Retry after an error re-fetches and renders content`() = runTest(testDispatcher) {
        repository.previewResult = NetworkResult.Error(NetworkError.SERVER)
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(ShareOutPreviewScreenState.Error, viewModel.stateFlow.value.deriveScreenState())

        repository.previewResult = NetworkResult.Success(accumulatingPreview())
        viewModel.trySendAction(ShareOutPreviewAction.Retry)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertNull(state.error)
        assertEquals(ShareOutPreviewScreenState.ContentAccumulating, state.deriveScreenState())
        assertEquals(2, repository.callCount)
    }

    // -- OnRefresh (connectivity-gated) ----------------------------------------------------------

    @Test
    fun `OnRefresh while offline emits Network ShowSnackbar without calling the repository`() = runTest(testDispatcher) {
        repository.previewResult = NetworkResult.Success(accumulatingPreview())
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        val callsAfterLoad = repository.callCount
        networkMonitor.setOnline(false)

        viewModel.eventFlow.test {
            viewModel.trySendAction(ShareOutPreviewAction.OnRefresh)
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(ShareOutPreviewEvent.ShowSnackbar("error_network"), awaitItem())
        }

        assertEquals(callsAfterLoad, repository.callCount, "offline refresh must not call the repository")
    }

    @Test
    fun `OnRefresh while online re-fetches`() = runTest(testDispatcher) {
        repository.previewResult = NetworkResult.Success(accumulatingPreview())
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        repository.previewResult = NetworkResult.Success(accumulatingPreview(totalPool = 99999.0))
        viewModel.trySendAction(ShareOutPreviewAction.OnRefresh)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(99999.0, viewModel.stateFlow.value.totalPool)
        assertEquals(2, repository.callCount)
    }

    // -- OnConfirm (navigate — after flow.yaml#validation_rules) ---------------------------------

    @Test
    fun `OnConfirm on a valid ACCUMULATING plan emits NavigateToShareOutExecute with the payout payload`() =
        runTest(testDispatcher) {
            repository.previewResult = NetworkResult.Success(accumulatingPreview())
            val typeConfig = accumulatingTypeConfig()
            val viewModel = buildViewModel(typeConfig = typeConfig)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.eventFlow.test {
                viewModel.trySendAction(ShareOutPreviewAction.OnConfirm)
                val event = awaitItem()
                assertTrue(event is ShareOutPreviewEvent.NavigateToShareOutExecute)
                assertEquals(GROUP_ID, event.groupId)
                assertEquals(24000.0, event.totalPool)
                assertEquals(5, event.memberPayouts.size)
                assertEquals(typeConfig, event.typeConfig)
            }
        }

    @Test
    fun `OnConfirm on a valid ROTATING_PAYOUT plan emits NavigateToShareOutExecute with empty payouts`() =
        runTest(testDispatcher) {
            repository.previewResult = NetworkResult.Success(rotatingPreview())
            val viewModel = buildViewModel(typeConfig = rotatingTypeConfig())
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.eventFlow.test {
                viewModel.trySendAction(ShareOutPreviewAction.OnConfirm)
                val event = awaitItem()
                assertTrue(event is ShareOutPreviewEvent.NavigateToShareOutExecute)
                assertTrue(event.memberPayouts.isEmpty())
                assertEquals(10000.0, event.totalPool)
            }
        }

    @Test
    fun `OnConfirm on an ACCUMULATING plan with no payouts is blocked with InsufficientData`() = runTest(testDispatcher) {
        repository.previewResult = NetworkResult.Success(accumulatingPreview(memberPayouts = emptyList()))
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(ShareOutPreviewAction.OnConfirm)
            assertEquals(ShareOutPreviewEvent.ShowSnackbar("error_insufficient_data"), awaitItem())
        }

        assertEquals(ShareOutPreviewError.InsufficientData, viewModel.stateFlow.value.error)
    }

    @Test
    fun `OnConfirm on a ROTATING_PAYOUT plan with no next recipient is blocked with InsufficientData`() =
        runTest(testDispatcher) {
            repository.previewResult = NetworkResult.Success(rotatingPreview(nextRecipientName = null))
            val viewModel = buildViewModel(typeConfig = rotatingTypeConfig())
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.eventFlow.test {
                viewModel.trySendAction(ShareOutPreviewAction.OnConfirm)
                assertEquals(ShareOutPreviewEvent.ShowSnackbar("error_insufficient_data"), awaitItem())
            }
        }

    // -- OnBack ----------------------------------------------------------------------------------

    @Test
    fun `OnBack emits NavigateBack`() = runTest(testDispatcher) {
        repository.previewResult = NetworkResult.Success(accumulatingPreview())
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(ShareOutPreviewAction.OnBack)
            assertEquals(ShareOutPreviewEvent.NavigateBack, awaitItem())
        }
    }
}

// ---------------------------------------------------------------------------
// Fixtures
// ---------------------------------------------------------------------------

private const val GROUP_ID = "grp-001"

private fun accumulatingTypeConfig(): GroupTypeConfig = groupTypeConfig(SavingsMechanism.ACCUMULATING, ContributionMode.SHARE_BASED_VARIABLE)

private fun rotatingTypeConfig(): GroupTypeConfig = groupTypeConfig(SavingsMechanism.ROTATING_PAYOUT, ContributionMode.FIXED)

private fun groupTypeConfig(mechanism: SavingsMechanism, contribution: ContributionMode): GroupTypeConfig = GroupTypeConfig(
    typeSlug = if (mechanism == SavingsMechanism.ROTATING_PAYOUT) GroupTypeSlug.ROSCA else GroupTypeSlug.VSLA,
    displayName = "Test Group",
    tagline = "Test tagline",
    savingsMechanism = mechanism,
    contributionMode = contribution,
    lendingEnabled = true,
    hasSocialFund = false,
    hasBankLinkage = false,
    welfareOnlyMode = false,
    formallyRegistered = false,
    defaultLoanMultiplier = 3.0,
    defaultInterestRatePct = 2.0,
    defaultCycleLengthMonths = 12,
    maxMembers = 30,
    minMembers = 5,
)

private fun accumulatingPreview(
    totalPool: Double = 24000.0,
    memberPayouts: List<MemberPayout> = defaultPayouts(),
): ShareOutPreview = ShareOutPreview(
    cycleNumber = 1,
    poolModel = "ACCUMULATING",
    shareoutFormula = "PRORATA_SHARES",
    totalCorpus = 20000.0,
    totalProfit = 4000.0,
    totalPool = totalPool,
    memberPayouts = memberPayouts,
    rotationPosition = null,
    nextRecipientName = null,
    nextRecipientAmount = null,
)

private fun defaultPayouts(): List<MemberPayout> = listOf(
    MemberPayout("m-1", "Amina Hassan", sharesHeld = 20, totalSavings = null, sharePercent = 33.3, payoutAmount = 7992.0),
    MemberPayout("m-2", "Peter Otieno", sharesHeld = 15, totalSavings = null, sharePercent = 25.0, payoutAmount = 6000.0),
    MemberPayout("m-3", "Grace Wanjiku", sharesHeld = 12, totalSavings = null, sharePercent = 20.0, payoutAmount = 4800.0),
    MemberPayout("m-4", "John Mwangi", sharesHeld = 8, totalSavings = null, sharePercent = 13.3, payoutAmount = 3192.0),
    MemberPayout("m-5", "Mary Akinyi", sharesHeld = 5, totalSavings = null, sharePercent = 8.3, payoutAmount = 2016.0),
)

private fun rotatingPreview(
    nextRecipientName: String? = "Grace Wanjiku",
): ShareOutPreview = ShareOutPreview(
    cycleNumber = 3,
    poolModel = "ROTATING_PAYOUT",
    shareoutFormula = "FIXED_ORDER",
    totalCorpus = 10000.0,
    totalProfit = 0.0,
    totalPool = 10000.0,
    memberPayouts = null,
    rotationPosition = 5,
    nextRecipientName = nextRecipientName,
    nextRecipientAmount = 10000.0,
)

/**
 * In-memory [ShareOutRepository] fake — uniquely named to avoid cross-feature collision. The
 * execute/enqueue methods are unused by the preview tests (they belong to `share-out-execute`);
 * they throw [NotImplementedError] so an accidental call surfaces loudly rather than silently
 * passing — the 14 preview tests exercise only [getShareOutPreview].
 */
private class FakeShareOutRepository : ShareOutRepository {
    var previewResult: NetworkResult<ShareOutPreview, NetworkError> = NetworkResult.Success(accumulatingPreview())

    var callCount: Int = 0
        private set

    override suspend fun getShareOutPreview(groupId: String): NetworkResult<ShareOutPreview, NetworkError> {
        callCount++
        return previewResult
    }

    override suspend fun executeShareOut(
        groupId: String,
        request: org.mifos.groupbanking.core.model.ShareOutExecuteRequest,
    ): NetworkResult<org.mifos.groupbanking.core.model.ShareOutExecuteResult, NetworkError> =
        throw NotImplementedError("executeShareOut is exercised by share-out-execute, not the preview tests")

    override suspend fun executeRotationPayout(
        groupId: String,
        request: org.mifos.groupbanking.core.model.RotationPayoutRequest,
    ): NetworkResult<org.mifos.groupbanking.core.model.RotationPayoutExecuteResult, NetworkError> =
        throw NotImplementedError("executeRotationPayout is exercised by share-out-execute, not the preview tests")

    override suspend fun enqueueShareOutExecuteOffline(
        groupId: String,
        request: org.mifos.groupbanking.core.model.ShareOutExecuteRequest,
    ): Long = throw NotImplementedError("enqueueShareOutExecuteOffline is exercised by share-out-execute, not the preview tests")

    override suspend fun enqueueRotationPayoutOffline(
        groupId: String,
        request: org.mifos.groupbanking.core.model.RotationPayoutRequest,
    ): Long = throw NotImplementedError("enqueueRotationPayoutOffline is exercised by share-out-execute, not the preview tests")
}

private class FakeShareOutNetworkMonitor(initiallyOnline: Boolean) : NetworkMonitor {
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
