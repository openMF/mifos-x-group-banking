/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.memberinvite

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
import kpt.core.data.repository.MemberInviteRepository
import kpt.core.model.CreateInviteRequest
import kpt.core.model.GeneratedInvite
import kpt.core.model.MemberRole
import kpt.core.model.PendingInvite
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * See API.md#viewmodel — MemberInviteViewModelTest exercises every declared [MemberInviteAction]
 * path (initial pending load, field/role edits, generate validation-block/success/offline/error,
 * copy-code/copy-link/share events, revoke optimistic success + undo-on-failure, retry, back), per
 * RULE-TDD-METHODOLOGY-001 / RULE-IMPL-DEAD-CLICKABLE-001.
 */
class MemberInviteViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: FakeMemberInviteRepository
    private lateinit var networkMonitor: FakeNetworkMonitor

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeMemberInviteRepository()
        networkMonitor = FakeNetworkMonitor(initiallyOnline = true)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(groupId: String = "42"): MemberInviteViewModel = MemberInviteViewModel(
        repository = repository,
        networkMonitor = networkMonitor,
        analytics = KptAnalyticsTracker(NoOpAnalyticsHelper()),
        crashReporter = ConsoleCrashReporter(),
        groupId = groupId,
    )

    private val pendingA = PendingInvite(
        rowId = 1,
        token = "AB12CD",
        invitedEmailPhone = "+254712345678",
        roleToAssign = MemberRole.TREASURER,
        expiresAt = "2026-07-23",
    )
    private val pendingB = PendingInvite(
        rowId = 2,
        token = "EF34GH",
        invitedEmailPhone = "amina@email.com",
        roleToAssign = MemberRole.MEMBER,
        expiresAt = "2026-07-24",
    )

    // -- Initial load --------------------------------------------------------------------------

    @Test
    fun `initial mount loads pending invites and seeds groupId`() = runTest(testDispatcher) {
        repository.listResult = NetworkResult.Success(listOf(pendingA, pendingB))
        val viewModel = buildViewModel(groupId = "42")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals("42", state.groupId)
        assertEquals(2, state.pendingInvites.size)
        assertEquals(false, state.isLoadingPending)
        assertEquals(MemberRole.MEMBER, state.selectedRole)
        assertEquals(42L, repository.lastListGroupId)
    }

    @Test
    fun `initial load error surfaces an error state`() = runTest(testDispatcher) {
        repository.listResult = NetworkResult.Error(NetworkError.SERVER)
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(MemberInviteError.Server, viewModel.stateFlow.value.error)
    }

    // -- Field / role edits --------------------------------------------------------------------

    @Test
    fun `OnEmailPhoneChange updates the field and clears validation error`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(MemberInviteAction.OnEmailPhoneChange("+254712345678"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("+254712345678", viewModel.stateFlow.value.emailPhone)
        assertNull(viewModel.stateFlow.value.validationError)
    }

    @Test
    fun `OnRoleSelect updates the selected role`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(MemberInviteAction.OnRoleSelect(MemberRole.CHAIRPERSON))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(MemberRole.CHAIRPERSON, viewModel.stateFlow.value.selectedRole)
    }

    // -- Generate: validation block ------------------------------------------------------------

    @Test
    fun `OnGenerateInvite with an invalid contact sets a validation error and does not call the api`() =
        runTest(testDispatcher) {
            val viewModel = buildViewModel()
            testDispatcher.scheduler.advanceUntilIdle()
            viewModel.trySendAction(MemberInviteAction.OnEmailPhoneChange("not-valid!!"))
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.trySendAction(MemberInviteAction.OnGenerateInvite)
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals("error_validation", viewModel.stateFlow.value.validationError)
            assertEquals(MemberInviteError.Validation, viewModel.stateFlow.value.error)
            assertEquals(0, repository.createCallCount)
        }

    // -- Generate: success ---------------------------------------------------------------------

    @Test
    fun `OnGenerateInvite success stores the code and link and reloads the pending list`() = runTest(testDispatcher) {
        repository.listResult = NetworkResult.Success(emptyList())
        repository.createResult = NetworkResult.Success(
            GeneratedInvite(token = "K7X2P9", inviteLink = "https://mifos.app/join?token=K7X2P9&group=42", rowId = 9),
        )
        val viewModel = buildViewModel(groupId = "42")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.trySendAction(MemberInviteAction.OnEmailPhoneChange("+254798765432"))
        viewModel.trySendAction(MemberInviteAction.OnRoleSelect(MemberRole.SECRETARY))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(MemberInviteAction.OnGenerateInvite)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals("K7X2P9", state.generatedCode)
        assertEquals("https://mifos.app/join?token=K7X2P9&group=42", state.generatedLink)
        assertEquals(false, state.isGenerating)
        assertNull(state.error)
        assertEquals(1, repository.createCallCount)
        assertEquals(MemberRole.SECRETARY, repository.lastCreateRequest?.roleToAssign)
        assertEquals(42L, repository.lastCreateRequest?.groupId)
        assertEquals(MemberInviteScreenState.Generated, state.deriveScreenState())
        // reload fired: listPendingInvites called on mount + after generate.
        assertEquals(2, repository.listCallCount)
    }

    // -- Generate: offline ---------------------------------------------------------------------

    @Test
    fun `OnGenerateInvite while offline enqueues to the sync queue and does not call the api`() = runTest(testDispatcher) {
        networkMonitor.setOnline(false)
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.trySendAction(MemberInviteAction.OnEmailPhoneChange("+254798765432"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(MemberInviteAction.OnGenerateInvite)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertTrue(state.isOffline)
        assertTrue(state.isOfflineQueued)
        // Queued, NOT errored — a durably-queued offline invite is not a failure.
        assertNull(state.error)
        // Offline: the network create is NOT attempted; the write is enqueued instead.
        assertEquals(0, repository.createCallCount)
        assertEquals(1, repository.enqueueOfflineCallCount)
        assertEquals("+254798765432", repository.lastEnqueuedRequest?.invitedEmailPhone)
    }

    // -- Generate: transport error -------------------------------------------------------------

    @Test
    fun `OnGenerateInvite server error maps to a Server error state`() = runTest(testDispatcher) {
        repository.createResult = NetworkResult.Error(NetworkError.SERVER)
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.trySendAction(MemberInviteAction.OnEmailPhoneChange("+254798765432"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(MemberInviteAction.OnGenerateInvite)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(MemberInviteError.Server, state.error)
        assertNull(state.generatedCode)
        assertEquals(false, state.isGenerating)
    }

    // -- Copy / share events -------------------------------------------------------------------

    @Test
    fun `OnCopyCode emits CopyToClipboard and a snackbar once a code exists`() = runTest(testDispatcher) {
        repository.createResult = NetworkResult.Success(
            GeneratedInvite(token = "K7X2P9", inviteLink = "https://mifos.app/join?token=K7X2P9", rowId = 9),
        )
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.trySendAction(MemberInviteAction.OnEmailPhoneChange("+254798765432"))
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.trySendAction(MemberInviteAction.OnGenerateInvite)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(MemberInviteAction.OnCopyCode)
            assertEquals(MemberInviteEvent.CopyToClipboard("K7X2P9"), awaitItem())
            assertEquals(MemberInviteEvent.ShowSnackbar("snack_code_copied"), awaitItem())
        }
    }

    @Test
    fun `OnShareLink emits ShowShareSheet with the code and link`() = runTest(testDispatcher) {
        repository.createResult = NetworkResult.Success(
            GeneratedInvite(token = "K7X2P9", inviteLink = "https://mifos.app/join?token=K7X2P9", rowId = 9),
        )
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.trySendAction(MemberInviteAction.OnEmailPhoneChange("+254798765432"))
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.trySendAction(MemberInviteAction.OnGenerateInvite)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(MemberInviteAction.OnShareLink)
            assertEquals(
                MemberInviteEvent.ShowShareSheet(link = "https://mifos.app/join?token=K7X2P9", code = "K7X2P9"),
                awaitItem(),
            )
        }
    }

    // -- Revoke: optimistic success ------------------------------------------------------------

    @Test
    fun `OnRevokeInvite optimistically removes the row and emits a snackbar on success`() = runTest(testDispatcher) {
        repository.listResult = NetworkResult.Success(listOf(pendingA, pendingB))
        repository.revokeResult = NetworkResult.Success(Unit)
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(MemberInviteAction.OnRevokeInvite(inviteId = 1))
            assertEquals(MemberInviteEvent.ShowSnackbar("snack_invite_revoked"), awaitItem())
        }

        val state = viewModel.stateFlow.value
        assertEquals(listOf(2L), state.pendingInvites.map { it.rowId })
        assertEquals(42L, repository.lastRevokeGroupId)
        assertEquals(1L, repository.lastRevokeRowId)
    }

    // -- Revoke: undo on failure ---------------------------------------------------------------

    @Test
    fun `OnRevokeInvite restores the row and sets RevokeFailure on error`() = runTest(testDispatcher) {
        repository.listResult = NetworkResult.Success(listOf(pendingA, pendingB))
        repository.revokeResult = NetworkResult.Error(NetworkError.NOT_FOUND)
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(MemberInviteAction.OnRevokeInvite(inviteId = 1))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(listOf(1L, 2L), state.pendingInvites.map { it.rowId }.sorted())
        assertEquals(MemberInviteError.RevokeFailure, state.error)
    }

    // -- Retry / back --------------------------------------------------------------------------

    @Test
    fun `OnRetry clears the error and reloads pending invites`() = runTest(testDispatcher) {
        repository.listResult = NetworkResult.Error(NetworkError.SERVER)
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(MemberInviteError.Server, viewModel.stateFlow.value.error)

        repository.listResult = NetworkResult.Success(listOf(pendingA))
        viewModel.trySendAction(MemberInviteAction.OnRetry)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertNull(state.error)
        assertEquals(1, state.pendingInvites.size)
    }

    @Test
    fun `OnBack emits NavigateBack`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(MemberInviteAction.OnBack)
            assertEquals(MemberInviteEvent.NavigateBack, awaitItem())
        }
    }
}

private class FakeMemberInviteRepository : MemberInviteRepository {
    var createResult: NetworkResult<GeneratedInvite, NetworkError> =
        NetworkResult.Success(GeneratedInvite(token = "AAA111", inviteLink = "https://mifos.app/join", rowId = 1))
    var listResult: NetworkResult<List<PendingInvite>, NetworkError> = NetworkResult.Success(emptyList())
    var revokeResult: NetworkResult<Unit, NetworkError> = NetworkResult.Success(Unit)

    var createCallCount: Int = 0
        private set
    var listCallCount: Int = 0
        private set
    var lastCreateRequest: CreateInviteRequest? = null
        private set
    var lastListGroupId: Long? = null
        private set
    var lastRevokeGroupId: Long? = null
        private set
    var lastRevokeRowId: Long? = null
        private set
    var enqueueOfflineCallCount: Int = 0
        private set
    var lastEnqueuedRequest: CreateInviteRequest? = null
        private set

    override suspend fun createInvite(request: CreateInviteRequest): NetworkResult<GeneratedInvite, NetworkError> {
        createCallCount++
        lastCreateRequest = request
        return createResult
    }

    override suspend fun enqueueOffline(request: CreateInviteRequest): Long {
        enqueueOfflineCallCount++
        lastEnqueuedRequest = request
        return 9L
    }

    override suspend fun listPendingInvites(groupId: Long): NetworkResult<List<PendingInvite>, NetworkError> {
        listCallCount++
        lastListGroupId = groupId
        return listResult
    }

    override suspend fun revokeInvite(groupId: Long, rowId: Long): NetworkResult<Unit, NetworkError> {
        lastRevokeGroupId = groupId
        lastRevokeRowId = rowId
        return revokeResult
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
