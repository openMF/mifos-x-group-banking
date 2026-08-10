/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.shareoutexecute

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
import kpt.core.data.repository.ShareOutRepository
import kpt.core.model.ContributionMode
import kpt.core.model.GroupTypeConfig
import kpt.core.model.GroupTypeSlug
import kpt.core.model.MemberExecutionStatus
import kpt.core.model.MemberPayout
import kpt.core.model.RotationPayoutExecuteResult
import kpt.core.model.RotationPayoutRequest
import kpt.core.model.SavingsMechanism
import kpt.core.model.ShareOutExecuteRequest
import kpt.core.model.ShareOutExecuteResult
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * See API.md#viewmodel — `ShareOutExecuteViewModelTest` exercises every declared
 * [ShareOutExecuteAction] path (confirmation gate, execute ACCUMULATING success / partial-failure /
 * retry / error / offline-enqueue, rotation execute, biometric, back-guard, done) plus the
 * [NetworkResult] -> [ShareOutExecuteState] mapping and the screen-state routing, per
 * RULE-TDD-METHODOLOGY-001 / RULE-IMPL-DEAD-CLICKABLE-001.
 */
class ShareOutExecuteViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: FakeExecuteShareOutRepository
    private lateinit var networkMonitor: FakeExecuteNetworkMonitor
    private lateinit var sessionManager: SessionManager

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeExecuteShareOutRepository()
        networkMonitor = FakeExecuteNetworkMonitor(initiallyOnline = true)
        sessionManager = SessionManager(policy = SecurityPolicy())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(
        typeConfig: GroupTypeConfig = accumulatingTypeConfig(),
        totalPool: Double = TOTAL_POOL,
        memberPayouts: List<MemberPayout> = defaultPayouts(),
        cycleNumber: Int = 3,
    ): ShareOutExecuteViewModel = ShareOutExecuteViewModel(
        repository = repository,
        networkMonitor = networkMonitor,
        sessionManager = sessionManager,
        crashReporter = ConsoleCrashReporter(),
        analytics = KptAnalyticsTracker(NoOpAnalyticsHelper()),
        groupId = GROUP_ID,
        typeConfig = typeConfig,
        totalPool = totalPool,
        memberPayouts = memberPayouts,
        cycleNumber = cycleNumber,
    )

    // -- initial state -------------------------------------------------------------------------

    @Test
    fun `initial state derives Content with ACCUMULATING pool and all payouts PENDING`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(GROUP_ID, state.groupId)
        assertEquals("ACCUMULATING", state.poolModel)
        assertEquals(5, state.totalCount)
        assertFalse(state.isConfirmed)
        assertEquals(TOTAL_POOL, state.totalPool)
        assertTrue(state.memberExecutionStatus.values.all { it == MemberExecutionStatus.PENDING })
        assertEquals(ShareOutExecuteScreenState.Content, state.deriveScreenState())
    }

    // -- confirmation gate ---------------------------------------------------------------------

    @Test
    fun `OnConfirmationTextChanged with the exact phrase (case-insensitive, trimmed) confirms`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        viewModel.trySendAction(ShareOutExecuteAction.OnConfirmationTextChanged("  share out  "))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.stateFlow.value.isConfirmed)
    }

    @Test
    fun `OnConfirmationTextChanged with a wrong phrase does not confirm`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        viewModel.trySendAction(ShareOutExecuteAction.OnConfirmationTextChanged("share"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.stateFlow.value.isConfirmed)
    }

    // -- execute blocked when unconfirmed ------------------------------------------------------

    @Test
    fun `OnExecute without confirmation is blocked with ConfirmationRequired and a snackbar`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(ShareOutExecuteAction.OnExecute)
            assertEquals(ShareOutExecuteEvent.ShowSnackbar("error_confirmation_required"), awaitItem())
        }
        assertEquals(ShareOutError.ConfirmationRequired, viewModel.stateFlow.value.error)
        assertEquals(0, repository.executeShareOutCallCount)
    }

    // -- execute ACCUMULATING success ----------------------------------------------------------

    @Test
    fun `OnExecute ACCUMULATING full success marks all DONE and derives Success`() = runTest(testDispatcher) {
        repository.shareOutResult = NetworkResult.Success(
            ShareOutExecuteResult(shareoutRecordId = "so-1", succeededCount = 5, failedCount = 0, failedMemberIds = emptyList()),
        )
        val viewModel = buildViewModel()
        confirmAndExecute(viewModel)

        val state = viewModel.stateFlow.value
        assertFalse(state.isExecuting)
        assertTrue(state.isCompleted)
        assertEquals(5, state.succeededCount)
        assertTrue(state.failedPayouts.isEmpty())
        assertTrue(state.memberExecutionStatus.values.all { it == MemberExecutionStatus.DONE })
        assertEquals(ShareOutExecuteScreenState.Success, state.deriveScreenState())
        assertEquals(1, repository.executeShareOutCallCount)
    }

    // -- execute ACCUMULATING partial failure --------------------------------------------------

    @Test
    fun `OnExecute ACCUMULATING partial failure marks failed rows and derives PartialFailure`() = runTest(testDispatcher) {
        repository.shareOutResult = NetworkResult.Success(
            ShareOutExecuteResult(shareoutRecordId = "so-1", succeededCount = 3, failedCount = 2, failedMemberIds = listOf("m-4", "m-5")),
        )
        val viewModel = buildViewModel()
        confirmAndExecute(viewModel)

        val state = viewModel.stateFlow.value
        assertTrue(state.isCompleted)
        assertEquals(2, state.failedPayouts.size)
        assertEquals(ShareOutError.PartialFailure, state.error)
        assertEquals(MemberExecutionStatus.FAILED, state.memberExecutionStatus["m-4"])
        assertEquals(MemberExecutionStatus.DONE, state.memberExecutionStatus["m-1"])
        assertEquals(ShareOutExecuteScreenState.PartialFailure, state.deriveScreenState())
    }

    @Test
    fun `OnRetryFailed re-submits only the failed subset and completes on success`() = runTest(testDispatcher) {
        repository.shareOutResult = NetworkResult.Success(
            ShareOutExecuteResult("so-1", succeededCount = 3, failedCount = 2, failedMemberIds = listOf("m-4", "m-5")),
        )
        val viewModel = buildViewModel()
        confirmAndExecute(viewModel)
        assertEquals(ShareOutExecuteScreenState.PartialFailure, viewModel.stateFlow.value.deriveScreenState())

        repository.shareOutResult = NetworkResult.Success(
            ShareOutExecuteResult("so-2", succeededCount = 2, failedCount = 0, failedMemberIds = emptyList()),
        )
        viewModel.trySendAction(ShareOutExecuteAction.OnRetryFailed)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertTrue(state.failedPayouts.isEmpty())
        assertEquals(ShareOutExecuteScreenState.Success, state.deriveScreenState())
        assertEquals(2, repository.executeShareOutCallCount)
        assertEquals(2, repository.lastShareOutRequest?.memberPayouts?.size, "retry sends only the failed subset")
    }

    // -- execute error paths -------------------------------------------------------------------

    @Test
    fun `OnExecute SERVER error re-enables the execute button and emits a server snackbar`() = runTest(testDispatcher) {
        repository.shareOutResult = NetworkResult.Error(NetworkError.SERVER)
        val viewModel = buildViewModel()
        confirmAndExecute(viewModel)

        val state = viewModel.stateFlow.value
        assertFalse(state.isExecuting)
        assertEquals(ShareOutError.Server, state.error)
        assertFalse(state.isCompleted)
        assertEquals(ShareOutExecuteScreenState.Error, state.deriveScreenState())
    }

    @Test
    fun `OnExecute UNAUTHORIZED ends the session and emits an auth snackbar`() = runTest(testDispatcher) {
        sessionManager.startSession()
        repository.shareOutResult = NetworkResult.Error(NetworkError.UNAUTHORIZED)
        val viewModel = buildViewModel()

        viewModel.eventFlow.test {
            confirmAndExecuteNoIdle(viewModel)
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(ShareOutExecuteEvent.ShowSnackbar("error_auth"), awaitItem())
        }
        assertEquals(ShareOutError.Auth, viewModel.stateFlow.value.error)
        assertFalse(sessionManager.isSessionActive.value, "401 must call sessionManager.endSession()")
    }

    // -- offline enqueue -----------------------------------------------------------------------

    @Test
    fun `OnExecute while offline enqueues to the sync queue and derives queued Success`() = runTest(testDispatcher) {
        networkMonitor.setOnline(false)
        val viewModel = buildViewModel()
        confirmAndExecute(viewModel)

        val state = viewModel.stateFlow.value
        assertTrue(state.queuedOffline)
        assertFalse(state.isExecuting)
        assertEquals(0, repository.executeShareOutCallCount, "offline must not hit the network")
        assertEquals(1, repository.enqueueShareOutCallCount)
        assertTrue(state.memberExecutionStatus.values.all { it == MemberExecutionStatus.QUEUED })
        assertEquals(ShareOutExecuteScreenState.Success, state.deriveScreenState())
    }

    @Test
    fun `OnExecute REQUEST_TIMEOUT mid-call falls back to an offline enqueue`() = runTest(testDispatcher) {
        repository.shareOutResult = NetworkResult.Error(NetworkError.REQUEST_TIMEOUT)
        val viewModel = buildViewModel()
        confirmAndExecute(viewModel)

        val state = viewModel.stateFlow.value
        assertTrue(state.queuedOffline)
        assertEquals(1, repository.enqueueShareOutCallCount)
    }

    // -- biometric confirmation ----------------------------------------------------------------

    @Test
    fun `OnBiometricSelected emits ShowBiometricPrompt`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(ShareOutExecuteAction.OnBiometricSelected)
            assertEquals(ShareOutExecuteEvent.ShowBiometricPrompt, awaitItem())
        }
    }

    @Test
    fun `biometric SUCCESS satisfies the confirmation gate`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        viewModel.trySendAction(ShareOutExecuteAction.Internal.BiometricResult(BiometricOutcome.SUCCESS))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.stateFlow.value.isConfirmed)
    }

    @Test
    fun `biometric UNAVAILABLE surfaces a fallback snackbar and does not confirm`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(ShareOutExecuteAction.Internal.BiometricResult(BiometricOutcome.UNAVAILABLE))
            assertEquals(ShareOutExecuteEvent.ShowSnackbar("error_biometric_unavailable"), awaitItem())
        }
        assertFalse(viewModel.stateFlow.value.isConfirmed)
    }

    // -- rotation (ROTATING_PAYOUT) ------------------------------------------------------------

    @Test
    fun `OnExecute ROTATING_PAYOUT success derives Success via COMP-DIST-002`() = runTest(testDispatcher) {
        repository.rotationResult = NetworkResult.Success(
            RotationPayoutExecuteResult(rotationRecordId = "rot-1", newRotationPosition = 6, nextRecipientId = "m-2"),
        )
        val viewModel = buildViewModel(
            typeConfig = rotatingTypeConfig(),
            memberPayouts = listOf(defaultPayouts().first()),
        )
        confirmAndExecute(viewModel)

        val state = viewModel.stateFlow.value
        assertTrue(state.isRotation)
        assertTrue(state.isCompleted)
        assertEquals(1, state.succeededCount)
        assertEquals(1, repository.executeRotationCallCount)
        assertEquals(0, repository.executeShareOutCallCount)
        assertEquals(ShareOutExecuteScreenState.Success, state.deriveScreenState())
    }

    // -- back-guard + done ---------------------------------------------------------------------

    @Test
    fun `OnBack before execution emits NavigateBack`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(ShareOutExecuteAction.OnBack)
            assertEquals(ShareOutExecuteEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `OnBack after completion is swallowed`() = runTest(testDispatcher) {
        repository.shareOutResult = NetworkResult.Success(
            ShareOutExecuteResult("so-1", succeededCount = 5, failedCount = 0, failedMemberIds = emptyList()),
        )
        val viewModel = buildViewModel()
        confirmAndExecute(viewModel)
        assertTrue(viewModel.stateFlow.value.isCompleted)

        viewModel.eventFlow.test {
            viewModel.trySendAction(ShareOutExecuteAction.OnBack)
            expectNoEvents()
        }
    }

    @Test
    fun `OnDone navigates to the group dashboard`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(ShareOutExecuteAction.OnDone)
            assertEquals(ShareOutExecuteEvent.NavigateToGroupDashboard(GROUP_ID), awaitItem())
        }
    }

    // -- helpers -------------------------------------------------------------------------------

    private fun confirmAndExecute(viewModel: ShareOutExecuteViewModel) {
        confirmAndExecuteNoIdle(viewModel)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    private fun confirmAndExecuteNoIdle(viewModel: ShareOutExecuteViewModel) {
        viewModel.trySendAction(ShareOutExecuteAction.OnConfirmationTextChanged("SHARE OUT"))
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.trySendAction(ShareOutExecuteAction.OnExecute)
    }
}

// ---------------------------------------------------------------------------
// Fixtures
// ---------------------------------------------------------------------------

private const val GROUP_ID = "grp-001"
private const val TOTAL_POOL = 24000.0

private fun accumulatingTypeConfig(): GroupTypeConfig =
    groupTypeConfig(SavingsMechanism.ACCUMULATING, ContributionMode.SHARE_BASED_VARIABLE, GroupTypeSlug.VSLA)

private fun rotatingTypeConfig(): GroupTypeConfig =
    groupTypeConfig(SavingsMechanism.ROTATING_PAYOUT, ContributionMode.FIXED, GroupTypeSlug.ROSCA)

private fun groupTypeConfig(
    mechanism: SavingsMechanism,
    contribution: ContributionMode,
    slug: GroupTypeSlug,
): GroupTypeConfig = GroupTypeConfig(
    typeSlug = slug,
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

private fun defaultPayouts(): List<MemberPayout> = listOf(
    MemberPayout("m-1", "Amina Hassan", sharesHeld = 20, totalSavings = null, sharePercent = 0.333, payoutAmount = 7992.0),
    MemberPayout("m-2", "Peter Otieno", sharesHeld = 15, totalSavings = null, sharePercent = 0.25, payoutAmount = 6000.0),
    MemberPayout("m-3", "Grace Wanjiku", sharesHeld = 12, totalSavings = null, sharePercent = 0.20, payoutAmount = 4800.0),
    MemberPayout("m-4", "John Mwangi", sharesHeld = 8, totalSavings = null, sharePercent = 0.133, payoutAmount = 3192.0),
    MemberPayout("m-5", "Mary Akinyi", sharesHeld = 5, totalSavings = null, sharePercent = 0.083, payoutAmount = 2016.0),
)

/** In-memory [ShareOutRepository] fake — uniquely named to avoid cross-feature collision with `share-out-preview`'s fake. */
private class FakeExecuteShareOutRepository : ShareOutRepository {
    var shareOutResult: NetworkResult<ShareOutExecuteResult, NetworkError> =
        NetworkResult.Success(ShareOutExecuteResult("so-1", 5, 0, emptyList()))
    var rotationResult: NetworkResult<RotationPayoutExecuteResult, NetworkError> =
        NetworkResult.Success(RotationPayoutExecuteResult("rot-1", 1, null))

    var executeShareOutCallCount: Int = 0
        private set
    var executeRotationCallCount: Int = 0
        private set
    var enqueueShareOutCallCount: Int = 0
        private set
    var enqueueRotationCallCount: Int = 0
        private set
    var lastShareOutRequest: ShareOutExecuteRequest? = null
        private set

    override suspend fun getShareOutPreview(groupId: String) =
        throw NotImplementedError("getShareOutPreview is exercised by share-out-preview, not the execute tests")

    override suspend fun executeShareOut(
        groupId: String,
        request: ShareOutExecuteRequest,
    ): NetworkResult<ShareOutExecuteResult, NetworkError> {
        executeShareOutCallCount++
        lastShareOutRequest = request
        return shareOutResult
    }

    override suspend fun executeRotationPayout(
        groupId: String,
        request: RotationPayoutRequest,
    ): NetworkResult<RotationPayoutExecuteResult, NetworkError> {
        executeRotationCallCount++
        return rotationResult
    }

    override suspend fun enqueueShareOutExecuteOffline(groupId: String, request: ShareOutExecuteRequest): Long {
        enqueueShareOutCallCount++
        return 1L
    }

    override suspend fun enqueueRotationPayoutOffline(groupId: String, request: RotationPayoutRequest): Long {
        enqueueRotationCallCount++
        return 2L
    }
}

private class FakeExecuteNetworkMonitor(initiallyOnline: Boolean) : NetworkMonitor {
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
