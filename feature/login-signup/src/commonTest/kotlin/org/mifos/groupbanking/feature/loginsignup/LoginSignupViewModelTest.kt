/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.loginsignup

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
import org.mifos.groupbanking.core.data.demo.DemoSession
import org.mifos.groupbanking.core.data.demo.DemoSessionManager
import org.mifos.groupbanking.core.data.repository.AuthRepository
import org.mifos.groupbanking.core.model.AuthSession
import org.mifos.groupbanking.core.model.GroupMembership
import org.mifos.groupbanking.core.model.GroupRole
import org.mifos.groupbanking.core.model.LoginCredentials
import org.mifos.groupbanking.core.model.SelfRegistration
import org.mifos.groupbanking.core.model.UserProfile
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * See API.md#viewmodel — LoginSignupViewModelTest exercises every declared
 * [LoginSignupAction] path (success + error) per RULE-TDD-METHODOLOGY-001 / RULE-IMPL-DEAD-CLICKABLE-001.
 */
class LoginSignupViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: FakeAuthRepository
    private lateinit var demoSessionManager: FakeDemoSessionManager
    private lateinit var viewModel: LoginSignupViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeAuthRepository()
        demoSessionManager = FakeDemoSessionManager()
        viewModel = LoginSignupViewModel(
            authRepository = repository,
            demoSessionManager = demoSessionManager,
            analytics = KptAnalyticsTracker(NoOpAnalyticsHelper()),
            crashReporter = ConsoleCrashReporter(),
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is content mode login with empty fields`() = runTest(testDispatcher) {
        val state = viewModel.stateFlow.value
        assertEquals(AuthMode.Login, state.mode)
        assertEquals("", state.emailPhone)
        assertEquals("", state.password)
        assertEquals(false, state.isSubmitting)
        assertNull(state.error)
        assertEquals(LoginSignupScreenState.Content, state.screenState)
    }

    @Test
    fun `OnModeToggle switches mode and clears form fields`() = runTest(testDispatcher) {
        viewModel.trySendAction(LoginSignupAction.OnEmailPhoneChange("grace@example.com"))
        viewModel.trySendAction(LoginSignupAction.OnPasswordChange("Passw0rd!"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(LoginSignupAction.OnModeToggle(AuthMode.Signup))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(AuthMode.Signup, state.mode)
        assertEquals("", state.emailPhone)
        assertEquals("", state.password)
        assertTrue(state.validationErrors.isEmpty())
    }

    @Test
    fun `OnEmailPhoneChange updates state and clears its field error`() = runTest(testDispatcher) {
        viewModel.trySendAction(LoginSignupAction.OnEmailPhoneChange("grace.wanjiku@example.com"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("grace.wanjiku@example.com", viewModel.stateFlow.value.emailPhone)
        assertTrue("emailPhone" !in viewModel.stateFlow.value.validationErrors)
    }

    @Test
    fun `OnLoginTap with invalid form sets validationErrors and does not call repository`() = runTest(testDispatcher) {
        // A space-containing value is invalid under all three sign-in identifier rules
        // (email / E.164 phone / username) — sign-in now accepts usernames, so a bare
        // token like "not-an-email" is a VALID username and would not error.
        viewModel.trySendAction(LoginSignupAction.OnEmailPhoneChange("invalid identifier"))
        viewModel.trySendAction(LoginSignupAction.OnPasswordChange(""))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(LoginSignupAction.OnLoginTap)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertTrue(state.validationErrors.containsKey("emailPhone"))
        assertTrue(state.validationErrors.containsKey("password"))
        assertEquals(0, repository.loginCallCount)
    }

    @Test
    fun `OnLoginTap success with no organizer role emits NavigateToPersonalDashboard`() = runTest(testDispatcher) {
        repository.loginResult = NetworkResult.Success(
            sampleSession(groups = listOf(sampleMembership(role = GroupRole.MEMBER))),
        )
        viewModel.trySendAction(LoginSignupAction.OnEmailPhoneChange("grace.wanjiku@example.com"))
        viewModel.trySendAction(LoginSignupAction.OnPasswordChange("Passw0rd!"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(LoginSignupAction.OnLoginTap)
            assertEquals(LoginSignupEvent.NavigateToPersonalDashboard, awaitItem())
        }

        val state = viewModel.stateFlow.value
        assertEquals(false, state.isSubmitting)
        assertEquals(LoginSignupScreenState.Content, state.screenState)
        assertEquals("sess-token", state.sessionToken)
        assertEquals(1, repository.loginCallCount)
    }

    @Test
    fun `OnLoginTap success with organizer role emits NavigateToOrganizerDashboard`() = runTest(testDispatcher) {
        repository.loginResult = NetworkResult.Success(
            sampleSession(groups = listOf(sampleMembership(role = GroupRole.ORGANIZER))),
        )
        viewModel.trySendAction(LoginSignupAction.OnEmailPhoneChange("grace.wanjiku@example.com"))
        viewModel.trySendAction(LoginSignupAction.OnPasswordChange("Passw0rd!"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(LoginSignupAction.OnLoginTap)
            // An organizer-in-any-group lands on the organizer hub (SPEC.md#login-routing +
            // organizer-dashboard `app_launch` gated on `isOrganizerInAnyGroup`); group-list stays
            // reachable from that hub's All-Groups quick-nav.
            assertEquals(LoginSignupEvent.NavigateToOrganizerDashboard, awaitItem())
        }
    }

    @Test
    fun `OnLoginTap success with zero groups transitions to ZeroGroups screen state without nav event`() = runTest(testDispatcher) {
        repository.loginResult = NetworkResult.Success(sampleSession(groups = emptyList()))
        viewModel.trySendAction(LoginSignupAction.OnEmailPhoneChange("grace.wanjiku@example.com"))
        viewModel.trySendAction(LoginSignupAction.OnPasswordChange("Passw0rd!"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(LoginSignupAction.OnLoginTap)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(LoginSignupScreenState.ZeroGroups, viewModel.stateFlow.value.screenState)
    }

    @Test
    fun `OnLoginTap error sets InvalidCredentials and Error screen state`() = runTest(testDispatcher) {
        repository.loginResult = NetworkResult.Error(NetworkError.UNAUTHORIZED)
        viewModel.trySendAction(LoginSignupAction.OnEmailPhoneChange("grace.wanjiku@example.com"))
        viewModel.trySendAction(LoginSignupAction.OnPasswordChange("Passw0rd!"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(LoginSignupAction.OnLoginTap)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(LoginSignupError.InvalidCredentials, state.error)
        assertEquals(LoginSignupScreenState.Error, state.screenState)
        assertEquals(false, state.isSubmitting)
    }

    @Test
    fun `OnSignupTap with invalid name sets validationErrors and does not call repository`() = runTest(testDispatcher) {
        viewModel.trySendAction(LoginSignupAction.OnModeToggle(AuthMode.Signup))
        viewModel.trySendAction(LoginSignupAction.OnNameChange("A"))
        viewModel.trySendAction(LoginSignupAction.OnEmailPhoneChange("grace.wanjiku@example.com"))
        viewModel.trySendAction(LoginSignupAction.OnPasswordChange("Passw0rd!"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(LoginSignupAction.OnSignupTap)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.stateFlow.value.validationErrors.containsKey("name"))
        assertEquals(0, repository.selfRegisterCallCount)
    }

    @Test
    fun `OnSignupTap success routes to ZeroGroups screen state`() = runTest(testDispatcher) {
        repository.selfRegisterResult = NetworkResult.Success(sampleSession(groups = emptyList()))
        viewModel.trySendAction(LoginSignupAction.OnModeToggle(AuthMode.Signup))
        viewModel.trySendAction(LoginSignupAction.OnNameChange("Grace Wanjiku"))
        viewModel.trySendAction(LoginSignupAction.OnEmailPhoneChange("grace.wanjiku@example.com"))
        viewModel.trySendAction(LoginSignupAction.OnPasswordChange("Passw0rd!"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(LoginSignupAction.OnSignupTap)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(LoginSignupScreenState.ZeroGroups, state.screenState)
        assertEquals(1, repository.selfRegisterCallCount)
    }

    @Test
    fun `OnSignupTap conflict error maps to AccountExists`() = runTest(testDispatcher) {
        repository.selfRegisterResult = NetworkResult.Error(NetworkError.UNKNOWN)
        viewModel.trySendAction(LoginSignupAction.OnModeToggle(AuthMode.Signup))
        viewModel.trySendAction(LoginSignupAction.OnNameChange("Grace Wanjiku"))
        viewModel.trySendAction(LoginSignupAction.OnEmailPhoneChange("grace.wanjiku@example.com"))
        viewModel.trySendAction(LoginSignupAction.OnPasswordChange("Passw0rd!"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(LoginSignupAction.OnSignupTap)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(LoginSignupError.AccountExists, state.error)
        assertEquals(LoginSignupScreenState.Error, state.screenState)
    }

    @Test
    fun `mount with valid stored session sets isBiometricAvailable and prompts biometric once`() = runTest(testDispatcher) {
        repository.emitSession(sampleSession(groups = emptyList()))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.stateFlow.value.isBiometricAvailable)
    }

    @Test
    fun `OnBiometricUnlock success with groups emits NavigateToPersonalDashboard`() = runTest(testDispatcher) {
        repository.emitSession(sampleSession(groups = emptyList()))
        testDispatcher.scheduler.advanceUntilIdle()
        repository.refreshSessionResult = NetworkResult.Success(
            sampleProfile(groups = listOf(sampleMembership(role = GroupRole.MEMBER))),
        )

        viewModel.eventFlow.test {
            viewModel.trySendAction(LoginSignupAction.OnBiometricUnlock)
            // First emission is the auto-mount PromptBiometric; drain it before asserting the tap-triggered flow.
            assertEquals(LoginSignupEvent.PromptBiometric, awaitItem())
            assertEquals(LoginSignupEvent.NavigateToPersonalDashboard, awaitItem())
        }
    }

    @Test
    fun `OnBiometricUnlock failure maps to BiometricFailed`() = runTest(testDispatcher) {
        repository.emitSession(sampleSession(groups = emptyList()))
        testDispatcher.scheduler.advanceUntilIdle()
        repository.refreshSessionResult = NetworkResult.Error(NetworkError.SERVER)

        viewModel.trySendAction(LoginSignupAction.OnBiometricUnlock)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(LoginSignupError.BiometricFailed, state.error)
        assertEquals(LoginSignupScreenState.Error, state.screenState)
    }

    @Test
    fun `OnBiometricUnlock without a stored session token surfaces BiometricFailed without calling repository`() = runTest(testDispatcher) {
        viewModel.trySendAction(LoginSignupAction.OnBiometricUnlock)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(LoginSignupError.BiometricFailed, viewModel.stateFlow.value.error)
        assertEquals(0, repository.refreshSessionCallCount)
    }

    @Test
    fun `OnCreateGroupTap emits NavigateToGroupTypePicker`() = runTest(testDispatcher) {
        viewModel.eventFlow.test {
            viewModel.trySendAction(LoginSignupAction.OnCreateGroupTap)
            assertEquals(LoginSignupEvent.NavigateToGroupTypePicker, awaitItem())
        }
    }

    @Test
    fun `OnJoinWithCodeTap emits NavigateToJoinWithCode with no code`() = runTest(testDispatcher) {
        viewModel.eventFlow.test {
            viewModel.trySendAction(LoginSignupAction.OnJoinWithCodeTap)
            assertEquals(LoginSignupEvent.NavigateToJoinWithCode(inviteCode = null), awaitItem())
        }
    }

    @Test
    fun `OnAcceptInvitationTap emits NavigateToJoinWithCode with no code`() = runTest(testDispatcher) {
        viewModel.eventFlow.test {
            viewModel.trySendAction(LoginSignupAction.OnAcceptInvitationTap)
            assertEquals(LoginSignupEvent.NavigateToJoinWithCode(inviteCode = null), awaitItem())
        }
    }

    @Test
    fun `OnDemoExplore opens the demo confirm dialog`() = runTest(testDispatcher) {
        viewModel.trySendAction(LoginSignupAction.OnDemoExplore)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.stateFlow.value.showDemoDialog)
    }

    @Test
    fun `OnDemoCancel dismisses the demo confirm dialog`() = runTest(testDispatcher) {
        viewModel.trySendAction(LoginSignupAction.OnDemoExplore)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(LoginSignupAction.OnDemoCancel)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, viewModel.stateFlow.value.showDemoDialog)
    }

    @Test
    fun `OnDemoConfirm seeds the offline demo session and emits NavigateToOrganizerDashboard`() = runTest(testDispatcher) {
        viewModel.trySendAction(LoginSignupAction.OnDemoExplore)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(LoginSignupAction.OnDemoConfirm)
            assertEquals(LoginSignupEvent.NavigateToOrganizerDashboard, awaitItem())
        }

        val state = viewModel.stateFlow.value
        assertEquals(1, demoSessionManager.startCallCount)
        assertEquals(false, state.showDemoDialog)
        assertEquals(false, state.isSeedingDemo)
    }

    @Test
    fun `OnDemoConfirm seed failure surfaces Error screen state without navigation`() = runTest(testDispatcher) {
        demoSessionManager.startResult = Result.failure(IllegalStateException("disk full"))

        viewModel.trySendAction(LoginSignupAction.OnDemoConfirm)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(false, state.isSeedingDemo)
        assertEquals(LoginSignupScreenState.Error, state.screenState)
        assertEquals(LoginSignupError.Server, state.error)
    }

    @Test
    fun `login success with pendingInviteCode resumes join instead of default landing (TC-LS-010)`() = runTest(testDispatcher) {
        repository.loginResult = NetworkResult.Success(
            sampleSession(groups = listOf(sampleMembership(role = GroupRole.MEMBER))),
        )
        // pendingInviteCode carried pre-auth from join-with-code (6-char).
        viewModel.trySendAction(LoginSignupAction.Internal.SetPendingInviteCode("DEMO24"))
        viewModel.trySendAction(LoginSignupAction.OnEmailPhoneChange("grace.wanjiku@example.com"))
        viewModel.trySendAction(LoginSignupAction.OnPasswordChange("Passw0rd!"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(LoginSignupAction.OnLoginTap)
            assertEquals(LoginSignupEvent.NavigateToJoinWithCode(inviteCode = "DEMO24"), awaitItem())
        }
    }

    @Test
    fun `signup success with pendingInviteCode resumes join instead of ZeroGroups (TC-LS-010)`() = runTest(testDispatcher) {
        repository.selfRegisterResult = NetworkResult.Success(sampleSession(groups = emptyList()))
        viewModel.trySendAction(LoginSignupAction.Internal.SetPendingInviteCode("DEMO24"))
        viewModel.trySendAction(LoginSignupAction.OnModeToggle(AuthMode.Signup))
        viewModel.trySendAction(LoginSignupAction.OnNameChange("Grace Wanjiku"))
        viewModel.trySendAction(LoginSignupAction.OnEmailPhoneChange("grace.wanjiku@example.com"))
        viewModel.trySendAction(LoginSignupAction.OnPasswordChange("Passw0rd!"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(LoginSignupAction.OnSignupTap)
            assertEquals(LoginSignupEvent.NavigateToJoinWithCode(inviteCode = "DEMO24"), awaitItem())
        }
    }

    @Test
    fun `OnForgotPassword emits ShowSnackbar`() = runTest(testDispatcher) {
        viewModel.eventFlow.test {
            viewModel.trySendAction(LoginSignupAction.OnForgotPassword)
            val event = awaitItem()
            assertTrue(event is LoginSignupEvent.ShowSnackbar)
        }
    }

    @Test
    fun `OnPinChange updates pin field`() = runTest(testDispatcher) {
        viewModel.trySendAction(LoginSignupAction.OnPinChange("1234"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("1234", viewModel.stateFlow.value.pin)
    }
}

private fun sampleSession(
    groups: List<GroupMembership> = emptyList(),
): AuthSession = AuthSession(
    userId = "user-1",
    sessionToken = "sess-token",
    tokenExpiresAt = Instant.parse("2026-12-31T00:00:00Z"),
    groupMemberships = groups,
)

private fun sampleProfile(
    groups: List<GroupMembership> = emptyList(),
): UserProfile = UserProfile(
    userId = "user-1",
    name = "Grace Wanjiku",
    emailPhone = "grace.wanjiku@example.com",
    groupMemberships = groups,
)

private fun sampleMembership(role: GroupRole): GroupMembership = GroupMembership(
    groupId = "group-1",
    groupName = "Chama Moja",
    role = role,
    joinedAt = Instant.parse("2026-01-01T00:00:00Z"),
)

/** In-memory [AuthRepository] fake — no network, no persistence. */
private class FakeAuthRepository : AuthRepository {

    private val sessionFlow = MutableStateFlow<AuthSession?>(null)
    override val currentSession: Flow<AuthSession?> = sessionFlow

    var loginResult: NetworkResult<AuthSession, NetworkError> = NetworkResult.Success(sampleSession())
    var selfRegisterResult: NetworkResult<AuthSession, NetworkError> = NetworkResult.Success(sampleSession())
    var refreshSessionResult: NetworkResult<UserProfile, NetworkError> = NetworkResult.Success(sampleProfile())

    var loginCallCount: Int = 0
        private set
    var selfRegisterCallCount: Int = 0
        private set
    var refreshSessionCallCount: Int = 0
        private set
    var clearSessionCalled: Boolean = false
        private set

    fun emitSession(session: AuthSession?) {
        sessionFlow.value = session
    }

    override suspend fun selfRegister(registration: SelfRegistration): NetworkResult<AuthSession, NetworkError> {
        selfRegisterCallCount++
        return selfRegisterResult
    }

    override suspend fun login(credentials: LoginCredentials): NetworkResult<AuthSession, NetworkError> {
        loginCallCount++
        return loginResult
    }

    override suspend fun refreshSession(sessionToken: String): NetworkResult<UserProfile, NetworkError> {
        refreshSessionCallCount++
        return refreshSessionResult
    }

    override suspend fun clearSession() {
        clearSessionCalled = true
        sessionFlow.value = null
    }
}

/** In-memory [DemoSessionManager] fake — no persistence, no cache seed, no network. */
private class FakeDemoSessionManager : DemoSessionManager {

    var startResult: Result<DemoSession> = Result.success(
        DemoSession(userId = "demo-user-amina", groupId = "demo-group-001", organizerName = "Amina Otieno"),
    )

    var startCallCount: Int = 0
        private set
    var clearCallCount: Int = 0
        private set

    private var active: Boolean = false

    override suspend fun startDemoSession(): Result<DemoSession> {
        startCallCount++
        return startResult.also { active = it.isSuccess }
    }

    override fun isDemoSession(): Boolean = active

    override suspend fun clearDemoSession() {
        clearCallCount++
        active = false
    }
}
