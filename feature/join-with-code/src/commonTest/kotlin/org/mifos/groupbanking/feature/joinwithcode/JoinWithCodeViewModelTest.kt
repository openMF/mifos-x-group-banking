/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.joinwithcode

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import kpt.core.analytics.KptAnalyticsTracker
import kpt.core.base.analytics.NoOpAnalyticsHelper
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.base.observability.ConsoleCrashReporter
import org.mifos.groupbanking.core.data.repository.AuthRepository
import org.mifos.groupbanking.core.data.repository.InvitationRepository
import org.mifos.groupbanking.core.model.AuthSession
import org.mifos.groupbanking.core.model.GroupPreview
import org.mifos.groupbanking.core.model.GroupRole
import org.mifos.groupbanking.core.model.GroupTypeSlug
import org.mifos.groupbanking.core.model.Invitation
import org.mifos.groupbanking.core.model.JoinGroupResult
import org.mifos.groupbanking.core.model.LoginCredentials
import org.mifos.groupbanking.core.model.SelfRegistration
import org.mifos.groupbanking.core.model.UserProfile
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * See API.md#viewmodel — JoinWithCodeViewModelTest exercises every declared
 * [JoinWithCodeAction] path (success + error) per RULE-TDD-METHODOLOGY-001 /
 * RULE-IMPL-DEAD-CLICKABLE-001, plus `tests.yaml` scenarios TC-JWC-001..006/008.
 */
class JoinWithCodeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var invitationRepository: FakeInvitationRepository
    private lateinit var authRepository: FakeAuthRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        invitationRepository = FakeInvitationRepository()
        authRepository = FakeAuthRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(inviteCode: String? = null): JoinWithCodeViewModel = JoinWithCodeViewModel(
        invitationRepository = invitationRepository,
        authRepository = authRepository,
        analytics = KptAnalyticsTracker(NoOpAnalyticsHelper()),
        crashReporter = ConsoleCrashReporter(),
        inviteCode = inviteCode,
    )

    @Test
    fun `initial state is empty idle code with no error`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals("", state.inviteCode)
        assertEquals(InviteStatus.IDLE, state.inviteStatus)
        assertNull(state.groupPreview)
        assertEquals(false, state.isJoining)
        assertNull(state.error)
        assertEquals(JoinWithCodeScreenState.Initial, state.deriveScreenState())
    }

    @Test
    fun `TC-JWC-001 deep-link inviteCode pre-fills field and auto-validates on mount`() = runTest(testDispatcher) {
        invitationRepository.validateCodeResult = NetworkResult.Success(sampleInvitation(groupId = 5))
        invitationRepository.fetchGroupPreviewResult = NetworkResult.Success(samplePreview(groupId = 5))

        val viewModel = buildViewModel(inviteCode = "abc123")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("ABC123", viewModel.stateFlow.value.inviteCode)
        assertEquals(1, invitationRepository.validateCodeCallCount)
        assertEquals(InviteStatus.VALID, viewModel.stateFlow.value.inviteStatus)
    }

    @Test
    fun `OnCodeChange sanitizes to uppercase and does not auto-validate below 6 chars`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()

        viewModel.trySendAction(JoinWithCodeAction.OnCodeChange("ab1"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("AB1", viewModel.stateFlow.value.inviteCode)
        assertEquals(0, invitationRepository.validateCodeCallCount)
        assertEquals(InviteStatus.IDLE, viewModel.stateFlow.value.inviteStatus)
    }

    @Test
    fun `TC-JWC-002 OnCodeChange reaching 6 chars auto-validates and transitions to Preview`() = runTest(testDispatcher) {
        invitationRepository.validateCodeResult = NetworkResult.Success(sampleInvitation(groupId = 5))
        invitationRepository.fetchGroupPreviewResult = NetworkResult.Success(samplePreview(groupId = 5))
        val viewModel = buildViewModel()

        viewModel.trySendAction(JoinWithCodeAction.OnCodeChange("mwg7x2"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals("MWG7X2", state.inviteCode)
        assertEquals(InviteStatus.VALID, state.inviteStatus)
        assertEquals("Mwangaza Women's Group", state.groupPreview?.groupName)
        assertEquals(JoinWithCodeScreenState.Preview, state.deriveScreenState())
        assertEquals(1, invitationRepository.fetchGroupPreviewCallCount)
    }

    @Test
    fun `TC-JWC-004 invalid code (404) sets ErrorInvalidCode with retry available`() = runTest(testDispatcher) {
        invitationRepository.validateCodeResult = NetworkResult.Error(NetworkError.NOT_FOUND)
        val viewModel = buildViewModel()

        viewModel.trySendAction(JoinWithCodeAction.OnCodeChange("xxxxxx"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(JoinError.InvalidCode, state.error)
        assertEquals(true, state.error?.retry)
        assertEquals(JoinWithCodeScreenState.ErrorInvalidCode, state.deriveScreenState())
    }

    @Test
    fun `TC-JWC-005 expired invite code sets ErrorExpired with no retry`() = runTest(testDispatcher) {
        invitationRepository.validateCodeResult = NetworkResult.Success(
            sampleInvitation(groupId = 5, expiresAt = Instant.parse("2020-01-01T00:00:00Z")),
        )
        val viewModel = buildViewModel()

        viewModel.trySendAction(JoinWithCodeAction.OnCodeChange("exp001"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(JoinError.ExpiredCode, state.error)
        assertEquals(false, state.error?.retry)
        assertEquals(JoinWithCodeScreenState.ErrorExpired, state.deriveScreenState())
        assertEquals(0, invitationRepository.fetchGroupPreviewCallCount)
    }

    @Test
    fun `TC-JWC-006 already-accepted invite code sets ErrorAlreadyMember with no retry`() = runTest(testDispatcher) {
        invitationRepository.validateCodeResult = NetworkResult.Success(
            sampleInvitation(groupId = 5, acceptedAt = Instant.parse("2026-01-01T00:00:00Z")),
        )
        val viewModel = buildViewModel()

        viewModel.trySendAction(JoinWithCodeAction.OnCodeChange("grp001"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(JoinError.AlreadyMember, state.error)
        assertEquals(false, state.error?.retry)
        assertEquals(JoinWithCodeScreenState.ErrorAlreadyMember, state.deriveScreenState())
    }

    @Test
    fun `network error during validate sets ErrorNetwork and OnRetry re-dispatches validate`() = runTest(testDispatcher) {
        invitationRepository.validateCodeResult = NetworkResult.Error(NetworkError.REQUEST_TIMEOUT)
        val viewModel = buildViewModel()

        viewModel.trySendAction(JoinWithCodeAction.OnCodeChange("mwg7x2"))
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.stateFlow.value
        assertEquals(JoinError.Network, state.error)
        assertEquals(true, state.error?.retry)
        assertEquals(JoinWithCodeScreenState.ErrorNetwork, state.deriveScreenState())

        invitationRepository.validateCodeResult = NetworkResult.Success(sampleInvitation(groupId = 5))
        invitationRepository.fetchGroupPreviewResult = NetworkResult.Success(samplePreview(groupId = 5))
        viewModel.trySendAction(JoinWithCodeAction.OnRetry)
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.stateFlow.value
        assertNull(state.error)
        assertEquals(InviteStatus.VALID, state.inviteStatus)
        assertEquals(2, invitationRepository.validateCodeCallCount)
    }

    @Test
    fun `TC-JWC-003 OnConfirmJoin success calls joinGroup and emits NavigateToGroupDashboard`() = runTest(testDispatcher) {
        invitationRepository.validateCodeResult = NetworkResult.Success(sampleInvitation(groupId = 5))
        invitationRepository.fetchGroupPreviewResult = NetworkResult.Success(samplePreview(groupId = 5))
        invitationRepository.joinGroupResult = NetworkResult.Success(
            JoinGroupResult(resourceId = 1, groupId = 5, clientIds = listOf(42)),
        )
        authRepository.emitSession(sampleSession())

        val viewModel = buildViewModel()
        viewModel.trySendAction(JoinWithCodeAction.OnCodeChange("mwg7x2"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(JoinWithCodeAction.OnConfirmJoin)
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(JoinWithCodeEvent.NavigateToGroupDashboard(groupId = "5"), awaitItem())
        }

        assertEquals(1, invitationRepository.joinGroupCallCount)
        assertEquals(false, viewModel.stateFlow.value.isJoining)
    }

    @Test
    fun `TC-JWC-008 OnConfirmJoin while unauthenticated emits NavigateToLoginSignup with pendingInviteCode`() =
        runTest(testDispatcher) {
            invitationRepository.validateCodeResult = NetworkResult.Success(sampleInvitation(groupId = 5))
            invitationRepository.fetchGroupPreviewResult = NetworkResult.Success(samplePreview(groupId = 5))
            authRepository.emitSession(null)

            val viewModel = buildViewModel()
            viewModel.trySendAction(JoinWithCodeAction.OnCodeChange("abc123"))
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.eventFlow.test {
                viewModel.trySendAction(JoinWithCodeAction.OnConfirmJoin)
                assertEquals(
                    JoinWithCodeEvent.NavigateToLoginSignup(pendingInviteCode = "ABC123"),
                    awaitItem(),
                )
            }
            assertEquals(0, invitationRepository.joinGroupCallCount)
        }

    @Test
    fun `OnConfirmJoin failure with 400 sets ErrorAlreadyMember`() = runTest(testDispatcher) {
        invitationRepository.validateCodeResult = NetworkResult.Success(sampleInvitation(groupId = 5))
        invitationRepository.fetchGroupPreviewResult = NetworkResult.Success(samplePreview(groupId = 5))
        invitationRepository.joinGroupResult = NetworkResult.Error(NetworkError.BAD_REQUEST)
        authRepository.emitSession(sampleSession())

        val viewModel = buildViewModel()
        viewModel.trySendAction(JoinWithCodeAction.OnCodeChange("mwg7x2"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(JoinWithCodeAction.OnConfirmJoin)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(JoinError.AlreadyMember, state.error)
        assertEquals(false, state.isJoining)
    }

    @Test
    fun `OnBack emits NavigateBack`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(JoinWithCodeAction.OnBack)
            assertEquals(JoinWithCodeEvent.NavigateBack, awaitItem())
        }
    }
}

private fun sampleInvitation(
    groupId: Long,
    expiresAt: Instant = Instant.parse("2030-01-01T00:00:00Z"),
    acceptedAt: Instant? = null,
): Invitation = Invitation(
    token = "MWG7X2",
    groupId = groupId,
    inviterClientId = 1,
    invitedEmailPhone = "grace@example.com",
    roleToAssign = GroupRole.MEMBER,
    expiresAt = expiresAt,
    acceptedAt = acceptedAt,
)

private fun samplePreview(groupId: Long): GroupPreview = GroupPreview(
    groupId = groupId,
    groupName = "Mwangaza Women's Group",
    groupType = GroupTypeSlug.VSLA,
    organizerName = "Jane Otieno",
    memberCount = 14,
    officeId = 1,
    roleToAssign = GroupRole.MEMBER,
)

private fun sampleSession(): AuthSession = AuthSession(
    userId = "42",
    sessionToken = "sess-token",
    tokenExpiresAt = Instant.parse("2026-12-31T00:00:00Z"),
    groupMemberships = emptyList(),
)

private fun sampleProfile(): UserProfile = UserProfile(
    userId = "42",
    name = "Grace Wanjiku",
    emailPhone = "grace.wanjiku@example.com",
    groupMemberships = emptyList(),
)

/** In-memory [InvitationRepository] fake — no network. */
private class FakeInvitationRepository : InvitationRepository {

    var validateCodeResult: NetworkResult<Invitation, NetworkError> =
        NetworkResult.Success(sampleInvitation(groupId = 1))
    var fetchGroupPreviewResult: NetworkResult<GroupPreview, NetworkError> =
        NetworkResult.Success(samplePreview(groupId = 1))
    var joinGroupResult: NetworkResult<JoinGroupResult, NetworkError> =
        NetworkResult.Success(JoinGroupResult(resourceId = 1, groupId = 1, clientIds = listOf(1)))

    var validateCodeCallCount: Int = 0
        private set
    var fetchGroupPreviewCallCount: Int = 0
        private set
    var joinGroupCallCount: Int = 0
        private set

    override suspend fun validateCode(code: String): NetworkResult<Invitation, NetworkError> {
        validateCodeCallCount++
        return validateCodeResult
    }

    override suspend fun fetchGroupPreview(groupId: Long): NetworkResult<GroupPreview, NetworkError> {
        fetchGroupPreviewCallCount++
        return fetchGroupPreviewResult
    }

    override suspend fun joinGroup(
        groupId: Long,
        clientId: Long,
        role: GroupRole,
        code: String,
        rowId: Long,
    ): NetworkResult<JoinGroupResult, NetworkError> {
        joinGroupCallCount++
        return joinGroupResult
    }
}

/** In-memory [AuthRepository] fake — no network, no persistence. */
private class FakeAuthRepository : AuthRepository {

    private val sessionFlow = MutableStateFlow<AuthSession?>(null)
    override val currentSession: Flow<AuthSession?> = sessionFlow

    var loginResult: NetworkResult<AuthSession, NetworkError> = NetworkResult.Success(sampleSession())
    var selfRegisterResult: NetworkResult<AuthSession, NetworkError> = NetworkResult.Success(sampleSession())
    var refreshSessionResult: NetworkResult<UserProfile, NetworkError> = NetworkResult.Success(sampleProfile())

    fun emitSession(session: AuthSession?) {
        sessionFlow.value = session
    }

    override suspend fun selfRegister(registration: SelfRegistration): NetworkResult<AuthSession, NetworkError> =
        selfRegisterResult

    override suspend fun login(credentials: LoginCredentials): NetworkResult<AuthSession, NetworkError> = loginResult

    override suspend fun refreshSession(sessionToken: String): NetworkResult<UserProfile, NetworkError> =
        refreshSessionResult

    override suspend fun clearSession() {
        sessionFlow.value = null
    }
}
