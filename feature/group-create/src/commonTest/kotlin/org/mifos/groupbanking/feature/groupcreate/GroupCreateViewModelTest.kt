/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.groupcreate

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
import org.mifos.groupbanking.core.data.repository.GroupCreateRepository
import org.mifos.groupbanking.core.model.AuthSession
import org.mifos.groupbanking.core.model.ContributionMode
import org.mifos.groupbanking.core.model.CreateGroupRequest
import org.mifos.groupbanking.core.model.GroupCreationResult
import org.mifos.groupbanking.core.model.GroupTypeConfig
import org.mifos.groupbanking.core.model.GroupTypeSlug
import org.mifos.groupbanking.core.model.LoginCredentials
import org.mifos.groupbanking.core.model.Office
import org.mifos.groupbanking.core.model.SavingsMechanism
import org.mifos.groupbanking.core.model.SelfRegistration
import org.mifos.groupbanking.core.model.UserProfile
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * See API.md#viewmodel — GroupCreateViewModelTest exercises every declared [GroupCreateAction]
 * path (success + error), the type-adaptive Step 2 branching (VSLA share-based vs. ROSCA
 * fixed+rotating-payout), and the offline-submit fallback, per RULE-TDD-METHODOLOGY-001 /
 * RULE-IMPL-DEAD-CLICKABLE-001.
 */
class GroupCreateViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var groupCreateRepository: FakeGroupCreateRepository
    private lateinit var authRepository: FakeAuthRepository
    private lateinit var networkMonitor: FakeNetworkMonitor

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        groupCreateRepository = FakeGroupCreateRepository()
        authRepository = FakeAuthRepository().apply { emitSession(sampleSession()) }
        networkMonitor = FakeNetworkMonitor(initiallyOnline = true)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(typeConfig: GroupTypeConfig = vslaTypeConfig()): GroupCreateViewModel =
        GroupCreateViewModel(
            initialTypeConfig = typeConfig,
            groupCreateRepository = groupCreateRepository,
            authRepository = authRepository,
            networkMonitor = networkMonitor,
            analytics = KptAnalyticsTracker(NoOpAnalyticsHelper()),
            crashReporter = ConsoleCrashReporter(),
        )

    // -- Initial state / nav-arg seeding -----------------------------------------------------------

    @Test
    fun `initial state is seeded from the GroupTypeConfig nav-arg`() = runTest(testDispatcher) {
        val viewModel = buildViewModel(vslaTypeConfig())
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(1, state.currentStep)
        assertEquals(4, state.totalSteps)
        assertEquals("VSLA", state.groupTypeName)
        assertEquals("3", state.loanMultiplier)
        assertEquals("10", state.interestRate)
        assertEquals("12", state.cycleLengthMonths)
        assertEquals("30", state.maxMembers)
        assertTrue(state.socialFundEnabled)
        assertEquals(GroupCreateScreenState.Content, state.deriveScreenState())
    }

    @Test
    fun `offices load on init and populate officeList`() = runTest(testDispatcher) {
        groupCreateRepository.officesResult = NetworkResult.Success(
            listOf(sampleOffice(id = 1L, name = "Nairobi Head Office")),
        )
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.stateFlow.value.officeList.size)
        assertEquals("Nairobi Head Office", viewModel.stateFlow.value.officeList.first().name)
    }

    // -- Field-change actions -----------------------------------------------------------------------

    @Test
    fun `OnNameChange updates groupName and clears its validation error`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(GroupCreateAction.OnNextStep) // trigger step-1 validation errors
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue("groupName" in viewModel.stateFlow.value.validationErrors)

        viewModel.trySendAction(GroupCreateAction.OnNameChange("Mwangaza Women's Group"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Mwangaza Women's Group", viewModel.stateFlow.value.groupName)
        assertTrue("groupName" !in viewModel.stateFlow.value.validationErrors)
    }

    // -- Wizard step navigation ----------------------------------------------------------------------

    @Test
    fun `OnNextStep with invalid Step 1 fields sets validationErrors and does not advance`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(GroupCreateAction.OnNextStep)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(1, state.currentStep)
        assertTrue(state.validationErrors.containsKey("groupName"))
        assertTrue(state.validationErrors.containsKey("officeId"))
        assertTrue(state.validationErrors.containsKey("meetingDay"))
        assertTrue(state.validationErrors.containsKey("meetingTime"))
    }

    @Test
    fun `OnNextStep with valid Step 1 fields advances to Step 2`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        fillStep1(viewModel)

        viewModel.trySendAction(GroupCreateAction.OnNextStep)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(2, state.currentStep)
        assertTrue(state.validationErrors.isEmpty())
    }

    @Test
    fun `OnPreviousStep decrements currentStep without validating`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        fillStep1(viewModel)
        viewModel.trySendAction(GroupCreateAction.OnNextStep)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, viewModel.stateFlow.value.currentStep)

        viewModel.trySendAction(GroupCreateAction.OnPreviousStep)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.stateFlow.value.currentStep)
        // Field values are retained, not cleared, per ui.yaml#back_step_button.action_contract.
        assertEquals("Mwangaza Women's Group", viewModel.stateFlow.value.groupName)
    }

    @Test
    fun `OnBack always emits NavigateBack regardless of current step`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(GroupCreateAction.OnBack)
            assertEquals(GroupCreateEvent.NavigateBack, awaitItem())
        }
    }

    // -- Type-adaptive Step 2 validation ---------------------------------------------------------

    @Test
    fun `Step 2 validation requires shareValue-shareMin-shareMax for a share-based VSLA type`() = runTest(testDispatcher) {
        val viewModel = buildViewModel(vslaTypeConfig())
        testDispatcher.scheduler.advanceUntilIdle()
        fillStep1(viewModel)
        viewModel.trySendAction(GroupCreateAction.OnNextStep)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, viewModel.stateFlow.value.currentStep)

        // fineAmount left blank; shareValue/shareMin/shareMax also left at their invalid defaults.
        viewModel.trySendAction(GroupCreateAction.OnNextStep)
        testDispatcher.scheduler.advanceUntilIdle()

        val errors = viewModel.stateFlow.value.validationErrors
        assertEquals(2, viewModel.stateFlow.value.currentStep) // blocked, did not advance
        assertTrue("shareValue" in errors)
        assertTrue("shareMax" in errors)
        assertTrue("contributionAmount" !in errors) // not applicable to a share-based type
    }

    @Test
    fun `Step 2 validation requires contributionAmount for a fixed rotating-payout ROSCA type`() = runTest(testDispatcher) {
        val viewModel = buildViewModel(roscaTypeConfig())
        testDispatcher.scheduler.advanceUntilIdle()
        fillStep1(viewModel)
        viewModel.trySendAction(GroupCreateAction.OnNextStep)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, viewModel.stateFlow.value.currentStep)

        viewModel.trySendAction(GroupCreateAction.OnNextStep)
        testDispatcher.scheduler.advanceUntilIdle()

        val errors = viewModel.stateFlow.value.validationErrors
        assertEquals(2, viewModel.stateFlow.value.currentStep)
        assertTrue("contributionAmount" in errors)
        assertTrue("shareValue" !in errors) // not applicable to a fixed-contribution type
        // payoutOrderMethod already has a valid non-blank default ("FIXED_ORDER") so it's absent.
        assertTrue("payoutOrderMethod" !in errors)
    }

    // -- Submit (Step 4) ------------------------------------------------------------------------------

    @Test
    fun `OnSubmit success navigates to group dashboard and stores the invite code`() = runTest(testDispatcher) {
        groupCreateRepository.createGroupResult = NetworkResult.Success(
            GroupCreationResult(groupId = "grp-42", fineractCenterId = 900L, inviteCode = "MWANGA1"),
        )
        val viewModel = buildViewModel(vslaTypeConfig())
        testDispatcher.scheduler.advanceUntilIdle()
        advanceToReviewStep(viewModel)
        assertEquals(4, viewModel.stateFlow.value.currentStep)

        viewModel.eventFlow.test {
            viewModel.trySendAction(GroupCreateAction.OnSubmit)
            assertEquals(GroupCreateEvent.NavigateToGroupDashboard(groupId = "grp-42"), awaitItem())
        }

        val state = viewModel.stateFlow.value
        assertEquals(false, state.isSubmitting)
        assertTrue(state.isSubmitSuccess)
        assertEquals("MWANGA1", state.inviteCode)
        assertNull(state.error)
        assertEquals(1, groupCreateRepository.createGroupCallCount)
        assertEquals(42L, groupCreateRepository.lastRequest?.userId)
    }

    @Test
    fun `OnSubmit while offline shows offline sync dialog and does not call the repository`() = runTest(testDispatcher) {
        networkMonitor.setOnline(false)
        val viewModel = buildViewModel(vslaTypeConfig())
        testDispatcher.scheduler.advanceUntilIdle()
        advanceToReviewStep(viewModel)

        viewModel.eventFlow.test {
            viewModel.trySendAction(GroupCreateAction.OnSubmit)
            assertEquals(GroupCreateEvent.ShowOfflineSyncDialog, awaitItem())
        }

        val state = viewModel.stateFlow.value
        assertTrue(state.isOffline)
        assertEquals(GroupCreateError.Network, state.error)
        assertEquals(0, groupCreateRepository.createGroupCallCount)
    }

    @Test
    fun `OnSubmit without an authenticated session sets Auth error and shows a snackbar`() = runTest(testDispatcher) {
        authRepository.emitSession(null)
        val viewModel = buildViewModel(vslaTypeConfig())
        testDispatcher.scheduler.advanceUntilIdle()
        advanceToReviewStep(viewModel)

        viewModel.eventFlow.test {
            viewModel.trySendAction(GroupCreateAction.OnSubmit)
            assertEquals(GroupCreateEvent.ShowSnackbar(message = "error_auth"), awaitItem())
        }

        assertEquals(GroupCreateError.Auth, viewModel.stateFlow.value.error)
        assertEquals(0, groupCreateRepository.createGroupCallCount)
    }

    @Test
    fun `OnSubmit server error maps to Server error state`() = runTest(testDispatcher) {
        groupCreateRepository.createGroupResult = NetworkResult.Error(NetworkError.SERVER)
        val viewModel = buildViewModel(vslaTypeConfig())
        testDispatcher.scheduler.advanceUntilIdle()
        advanceToReviewStep(viewModel)

        viewModel.trySendAction(GroupCreateAction.OnSubmit)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(false, state.isSubmitting)
        assertEquals(GroupCreateError.Server, state.error)
        assertEquals(false, state.isSubmitSuccess)
    }

    // -- Test fixtures / helpers ------------------------------------------------------------------

    private fun fillStep1(viewModel: GroupCreateViewModel) {
        viewModel.trySendAction(GroupCreateAction.OnNameChange("Mwangaza Women's Group"))
        viewModel.trySendAction(GroupCreateAction.OnOfficeSelect(officeId = 1L, officeName = "Nairobi Head Office"))
        viewModel.trySendAction(GroupCreateAction.OnMeetingDaySelect("Monday"))
        viewModel.trySendAction(GroupCreateAction.OnMeetingTimeSelect("09:00"))
    }

    /** Drives a fully-valid VSLA journey from Step 1 through to Step 4 (Review). */
    private fun advanceToReviewStep(viewModel: GroupCreateViewModel) {
        fillStep1(viewModel)
        viewModel.trySendAction(GroupCreateAction.OnNextStep)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(GroupCreateAction.OnShareValueChange("200"))
        viewModel.trySendAction(GroupCreateAction.OnShareMinChange("1"))
        viewModel.trySendAction(GroupCreateAction.OnShareMaxChange("5"))
        viewModel.trySendAction(GroupCreateAction.OnFineAmountChange("50"))
        viewModel.trySendAction(GroupCreateAction.OnNextStep)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(GroupCreateAction.OnNextStep) // Step 3 -> 4 (maxMembers default "30" is valid)
        testDispatcher.scheduler.advanceUntilIdle()
    }
}

private fun vslaTypeConfig(): GroupTypeConfig = GroupTypeConfig(
    typeSlug = GroupTypeSlug.VSLA,
    displayName = "VSLA",
    tagline = "Village Savings and Loan Association",
    savingsMechanism = SavingsMechanism.ACCUMULATING,
    contributionMode = ContributionMode.SHARE_BASED_VARIABLE,
    lendingEnabled = true,
    hasSocialFund = true,
    hasBankLinkage = false,
    welfareOnlyMode = false,
    formallyRegistered = false,
    defaultLoanMultiplier = 3.0,
    defaultInterestRatePct = 10.0,
    defaultCycleLengthMonths = 12,
    maxMembers = 30,
    minMembers = 5,
)

private fun roscaTypeConfig(): GroupTypeConfig = GroupTypeConfig(
    typeSlug = GroupTypeSlug.ROSCA,
    displayName = "ROSCA",
    tagline = "Rotating Savings and Credit Association",
    savingsMechanism = SavingsMechanism.ROTATING_PAYOUT,
    contributionMode = ContributionMode.FIXED,
    lendingEnabled = false,
    hasSocialFund = false,
    hasBankLinkage = false,
    welfareOnlyMode = false,
    formallyRegistered = false,
    defaultLoanMultiplier = 1.0,
    defaultInterestRatePct = 0.0,
    defaultCycleLengthMonths = 6,
    maxMembers = 15,
    minMembers = 5,
)

private fun sampleOffice(id: Long = 1L, name: String = "Nairobi Head Office"): Office = Office(
    id = id,
    name = name,
    nameDecorated = ".$name",
    externalId = null,
)

private fun sampleSession(userId: String = "42"): AuthSession = AuthSession(
    userId = userId,
    sessionToken = "sess-token",
    tokenExpiresAt = Instant.fromEpochMilliseconds(0),
    groupMemberships = emptyList(),
)

private class FakeGroupCreateRepository : GroupCreateRepository {
    var officesResult: NetworkResult<List<Office>, NetworkError> = NetworkResult.Success(emptyList())
    var createGroupResult: NetworkResult<GroupCreationResult, NetworkError> = NetworkResult.Success(
        GroupCreationResult(groupId = "grp-1", fineractCenterId = 100L, inviteCode = "ABC123"),
    )

    var createGroupCallCount: Int = 0
        private set
    var lastRequest: CreateGroupRequest? = null
        private set

    override suspend fun getOffices(orderBy: String): NetworkResult<List<Office>, NetworkError> = officesResult

    override suspend fun createGroup(request: CreateGroupRequest): NetworkResult<GroupCreationResult, NetworkError> {
        createGroupCallCount++
        lastRequest = request
        return createGroupResult
    }
}

private class FakeAuthRepository : AuthRepository {

    private val sessionFlow = MutableStateFlow<AuthSession?>(null)
    override val currentSession: Flow<AuthSession?> = sessionFlow

    fun emitSession(session: AuthSession?) {
        sessionFlow.value = session
    }

    override suspend fun selfRegister(registration: SelfRegistration): NetworkResult<AuthSession, NetworkError> =
        NetworkResult.Success(sampleSession())

    override suspend fun login(credentials: LoginCredentials): NetworkResult<AuthSession, NetworkError> =
        NetworkResult.Success(sampleSession())

    override suspend fun refreshSession(sessionToken: String): NetworkResult<UserProfile, NetworkError> =
        NetworkResult.Success(UserProfile(userId = "42", name = "Grace", emailPhone = "grace@example.com", groupMemberships = emptyList()))

    override suspend fun clearSession() {
        sessionFlow.value = null
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
