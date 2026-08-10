/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.memberadd

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
import kpt.core.data.repository.MemberAddRepository
import kpt.core.model.CreateMemberRequest
import kpt.core.model.MemberCreationResult
import kpt.core.model.MemberRole
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * See API.md#viewmodel — MemberAddViewModelTest exercises every declared [MemberAddAction] path
 * (field edits + their validation-error clearing, photo picker open/capture/select/remove, role
 * selection, submit success/validation-block/offline/transport-error, and back navigation), per
 * RULE-TDD-METHODOLOGY-001 / RULE-IMPL-DEAD-CLICKABLE-001.
 */
class MemberAddViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var memberAddRepository: FakeMemberAddRepository
    private lateinit var networkMonitor: FakeNetworkMonitor

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        memberAddRepository = FakeMemberAddRepository()
        networkMonitor = FakeNetworkMonitor(initiallyOnline = true)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(groupId: String = "grp-7"): MemberAddViewModel = MemberAddViewModel(
        repository = memberAddRepository,
        networkMonitor = networkMonitor,
        analytics = KptAnalyticsTracker(NoOpAnalyticsHelper()),
        crashReporter = ConsoleCrashReporter(),
        groupId = groupId,
    )

    // -- Initial state / nav-arg seeding -------------------------------------------------------

    @Test
    fun `initial state is seeded from the groupId nav-arg and defaults role to MEMBER`() = runTest(testDispatcher) {
        val viewModel = buildViewModel(groupId = "grp-7")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals("grp-7", state.groupId)
        assertEquals(MemberRole.MEMBER, state.selectedRole)
        assertEquals("", state.firstName)
        assertEquals("", state.phone)
        assertNull(state.photoUri)
        assertEquals(MemberAddScreenState.Content, state.deriveScreenState())
    }

    // -- Field-change actions -------------------------------------------------------------------

    @Test
    fun `OnFirstNameChange updates firstName and clears its validation error`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(MemberAddAction.OnSubmit) // trigger validation errors (blank form)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue("firstName" in viewModel.stateFlow.value.validationErrors)

        viewModel.trySendAction(MemberAddAction.OnFirstNameChange("Amina"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Amina", viewModel.stateFlow.value.firstName)
        assertTrue("firstName" !in viewModel.stateFlow.value.validationErrors)
    }

    @Test
    fun `OnPhoneChange updates phone and clears its validation error`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(MemberAddAction.OnSubmit)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue("phone" in viewModel.stateFlow.value.validationErrors)

        viewModel.trySendAction(MemberAddAction.OnPhoneChange("+254723456789"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("+254723456789", viewModel.stateFlow.value.phone)
        assertTrue("phone" !in viewModel.stateFlow.value.validationErrors)
    }

    // -- Photo picker actions -------------------------------------------------------------------

    @Test
    fun `OnPhotoPickerOpen sets showPhotoPicker and emits ShowPhotoPicker`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(MemberAddAction.OnPhotoPickerOpen)
            assertEquals(MemberAddEvent.ShowPhotoPicker, awaitItem())
        }
        assertTrue(viewModel.stateFlow.value.showPhotoPicker)
    }

    @Test
    fun `OnPhotoCaptured sets photoUri and closes the picker sheet`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.trySendAction(MemberAddAction.OnPhotoPickerOpen)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(MemberAddAction.OnPhotoCaptured(uri = "file:///member-photo.jpg"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals("file:///member-photo.jpg", state.photoUri)
        assertTrue(!state.showPhotoPicker)
    }

    @Test
    fun `OnPhotoSelected sets photoUri from the gallery pick`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(MemberAddAction.OnPhotoSelected(uri = "content://gallery/img-1"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("content://gallery/img-1", viewModel.stateFlow.value.photoUri)
    }

    @Test
    fun `OnPhotoRemoved clears photoUri back to null`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.trySendAction(MemberAddAction.OnPhotoSelected(uri = "content://gallery/img-1"))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("content://gallery/img-1", viewModel.stateFlow.value.photoUri)

        viewModel.trySendAction(MemberAddAction.OnPhotoRemoved)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.stateFlow.value.photoUri)
    }

    // -- Role selection ---------------------------------------------------------------------------

    @Test
    fun `OnRoleSelected updates selectedRole`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(MemberAddAction.OnRoleSelected(MemberRole.SECRETARY))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(MemberRole.SECRETARY, viewModel.stateFlow.value.selectedRole)
    }

    // -- Back navigation ----------------------------------------------------------------------------

    @Test
    fun `OnBack always emits NavigateBack`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(MemberAddAction.OnBack)
            assertEquals(MemberAddEvent.NavigateBack, awaitItem())
        }
    }

    // -- Submit validation-block -----------------------------------------------------------------

    @Test
    fun `OnSubmit with a blank form sets validationErrors and does not call the repository`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(MemberAddAction.OnSubmit)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertTrue(state.validationErrors.containsKey("firstName"))
        assertTrue(state.validationErrors.containsKey("lastName"))
        assertTrue(state.validationErrors.containsKey("phone"))
        assertEquals(0, memberAddRepository.createMemberCallCount)
    }

    @Test
    fun `OnSubmit with an invalid phone format sets a phone validation error`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.trySendAction(MemberAddAction.OnFirstNameChange("Amina"))
        viewModel.trySendAction(MemberAddAction.OnLastNameChange("Wanjiru"))
        viewModel.trySendAction(MemberAddAction.OnPhoneChange("12345"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(MemberAddAction.OnSubmit)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("error_phone_format", viewModel.stateFlow.value.validationErrors["phone"])
        assertEquals(0, memberAddRepository.createMemberCallCount)
    }

    // -- Submit success -----------------------------------------------------------------------------

    @Test
    fun `OnSubmit success navigates to member profile and resets submitting flag`() = runTest(testDispatcher) {
        memberAddRepository.createMemberResult = NetworkResult.Success(
            MemberCreationResult(
                memberId = "501",
                fineractClientId = 501L,
                groupId = "grp-7",
                role = MemberRole.SECRETARY,
                photoUploaded = false,
            ),
        )
        val viewModel = buildViewModel(groupId = "grp-7")
        testDispatcher.scheduler.advanceUntilIdle()
        fillValidForm(viewModel)

        viewModel.eventFlow.test {
            viewModel.trySendAction(MemberAddAction.OnSubmit)
            assertEquals(MemberAddEvent.NavigateToMemberProfile(memberId = "501", groupId = "grp-7"), awaitItem())
        }

        val state = viewModel.stateFlow.value
        assertEquals(false, state.isSubmitting)
        assertTrue(state.isSubmitSuccess)
        assertNull(state.error)
        assertEquals(1, memberAddRepository.createMemberCallCount)
        assertEquals(MemberRole.SECRETARY, memberAddRepository.lastRequest?.role)
        // photo-bytes reading is a deferred platform concern (see class KDoc) — submitted as null.
        assertNull(memberAddRepository.lastPhotoBytes)
    }

    // -- Submit while offline ------------------------------------------------------------------------

    @Test
    fun `OnSubmit while offline enqueues to the sync queue and does not call createMember`() = runTest(testDispatcher) {
        networkMonitor.setOnline(false)
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        fillValidForm(viewModel)

        viewModel.eventFlow.test {
            viewModel.trySendAction(MemberAddAction.OnSubmit)
            assertEquals(MemberAddEvent.ShowOfflineSyncDialog, awaitItem())
        }

        val state = viewModel.stateFlow.value
        assertTrue(state.isOffline)
        assertTrue(state.isOfflineQueued)
        // Queued, NOT errored — a durably-queued offline member-add is not a failure.
        assertNull(state.error)
        // Offline: the network create-chain is NOT attempted; the write is enqueued instead.
        assertEquals(0, memberAddRepository.createMemberCallCount)
        assertEquals(1, memberAddRepository.enqueueOfflineCallCount)
    }

    // -- Submit transport error -----------------------------------------------------------------------

    @Test
    fun `OnSubmit BAD_REQUEST error surfaces as Validation (duplicate-phone sub-code currently indistinguishable)`() =
        runTest(testDispatcher) {
            // NetworkError.BAD_REQUEST is the ONE bucket MemberAddApiImpl maps every HTTP 400 into
            // (`api.yaml#api.create_client.errors.400`: "invalid fields or duplicate phone") — the
            // client cannot yet tell a duplicate-phone 400 apart from a generic validation 400
            // without a richer error body threaded through NetworkError (see class KDoc "KNOWN GAP").
            memberAddRepository.createMemberResult = NetworkResult.Error(NetworkError.BAD_REQUEST)
            val viewModel = buildViewModel()
            testDispatcher.scheduler.advanceUntilIdle()
            fillValidForm(viewModel)

            viewModel.trySendAction(MemberAddAction.OnSubmit)
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.stateFlow.value
            assertEquals(false, state.isSubmitting)
            assertEquals(MemberAddError.Validation, state.error)
            assertEquals(false, state.isSubmitSuccess)
        }

    @Test
    fun `OnSubmit UNAUTHORIZED error sets Auth error and shows a snackbar`() = runTest(testDispatcher) {
        memberAddRepository.createMemberResult = NetworkResult.Error(NetworkError.UNAUTHORIZED)
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        fillValidForm(viewModel)

        viewModel.eventFlow.test {
            viewModel.trySendAction(MemberAddAction.OnSubmit)
            assertEquals(MemberAddEvent.ShowSnackbar(message = "error_auth"), awaitItem())
        }

        assertEquals(MemberAddError.Auth, viewModel.stateFlow.value.error)
    }

    @Test
    fun `OnSubmit SERVER error maps to Server error state`() = runTest(testDispatcher) {
        memberAddRepository.createMemberResult = NetworkResult.Error(NetworkError.SERVER)
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        fillValidForm(viewModel)

        viewModel.trySendAction(MemberAddAction.OnSubmit)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(MemberAddError.Server, state.error)
        assertEquals(false, state.isSubmitSuccess)
    }

    // -- Reactive connectivity (drives the offline_banner) ---------------------------------------------

    @Test
    fun `going offline after init flips isOffline without an explicit submit`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(!viewModel.stateFlow.value.isOffline)

        networkMonitor.setOnline(false)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.stateFlow.value.isOffline)
    }

    // -- Test fixtures / helpers ------------------------------------------------------------------

    private fun fillValidForm(viewModel: MemberAddViewModel) {
        viewModel.trySendAction(MemberAddAction.OnFirstNameChange("Amina"))
        viewModel.trySendAction(MemberAddAction.OnLastNameChange("Wanjiru"))
        viewModel.trySendAction(MemberAddAction.OnPhoneChange("+254723456789"))
        viewModel.trySendAction(MemberAddAction.OnRoleSelected(MemberRole.SECRETARY))
    }
}

private class FakeMemberAddRepository : MemberAddRepository {
    var createMemberResult: NetworkResult<MemberCreationResult, NetworkError> = NetworkResult.Success(
        MemberCreationResult(
            memberId = "1",
            fineractClientId = 1L,
            groupId = "grp-1",
            role = MemberRole.MEMBER,
            photoUploaded = false,
        ),
    )

    var createMemberCallCount: Int = 0
        private set
    var lastRequest: CreateMemberRequest? = null
        private set
    var lastPhotoBytes: ByteArray? = null
        private set
    var enqueueOfflineCallCount: Int = 0
        private set
    var lastEnqueuedRequest: CreateMemberRequest? = null
        private set

    override suspend fun createMember(
        request: CreateMemberRequest,
        photoBytes: ByteArray?,
    ): NetworkResult<MemberCreationResult, NetworkError> {
        createMemberCallCount++
        lastRequest = request
        lastPhotoBytes = photoBytes
        return createMemberResult
    }

    override suspend fun enqueueOffline(request: CreateMemberRequest): Long {
        enqueueOfflineCallCount++
        lastEnqueuedRequest = request
        return 11L
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
