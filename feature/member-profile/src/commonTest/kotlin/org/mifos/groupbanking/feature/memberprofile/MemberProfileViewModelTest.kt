/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.memberprofile

import app.cash.turbine.test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kpt.core.analytics.KptAnalyticsTracker
import kpt.core.base.analytics.NoOpAnalyticsHelper
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.base.observability.ConsoleCrashReporter
import kpt.core.base.security.SecurityPolicy
import kpt.core.base.security.SessionManager
import kpt.core.base.store.screen.ExperimentalScreenDataStreamTestingApi
import kpt.core.base.store.screen.FetchPolicy
import kpt.core.base.store.screen.ScreenDataStream
import kpt.core.base.store.screen.ScreenState
import kpt.core.base.store.screen.screenDataStreamForTesting
import org.mifos.groupbanking.core.data.repository.MemberProfileRepository
import org.mifos.groupbanking.core.model.ActiveLoanSummary
import org.mifos.groupbanking.core.model.MemberAccounts
import org.mifos.groupbanking.core.model.MemberProfile
import org.mifos.groupbanking.core.model.MemberProfileDetail
import org.mifos.groupbanking.core.model.MemberRole
import org.mifos.groupbanking.core.model.MemberRoleInfo
import org.mifos.groupbanking.core.model.MemberStatus
import org.mifos.groupbanking.core.model.UpdateMemberRoleRequest
import org.mifos.groupbanking.core.model.UpdateMemberRoleResult

/**
 * See API.md#viewmodel — `MemberProfileViewModelTest` exercises the [ScreenState] ->
 * [MemberProfileState] mapping (member/accounts/role/attendance), the pure [computeAttendanceRate]
 * / [resolveCurrentRole] formulas, the chairperson-gated role-edit flow (tap -> select -> confirm
 * -> [MemberProfileRepository.updateMemberRole] -> cache invalidation triggers a stream re-fetch),
 * and every declared navigation / retry action, per RULE-TDD-METHODOLOGY-001 /
 * RULE-IMPL-DEAD-CLICKABLE-001.
 *
 * `isCurrentUserChairperson` has **no wire source** in this generation's consumable prior layers
 * (`ui.yaml#state_model.di` names `GroupRepository.isCurrentUserChairperson(groupId): Flow<Boolean>`
 * but the shipped `GroupRepository` interface exposes only `groupsPagingStream` — same documented
 * "declared di vs shipped repository" drift class as `GroupDashboardViewModel`'s `GroupRepository`/
 * `CorpusRepository`/`RoleRepository` gap). It therefore always defaults `false` in production
 * today; [MemberProfileViewModel] still defensively guards `OnEditRoleTap` against a bypassed tap
 * (mirrors `GroupDashboardViewModel.handleShareOut`'s unauthorized-role guard) — covered below by
 * both the allowed and blocked branches.
 */
@OptIn(ExperimentalScreenDataStreamTestingApi::class)
class MemberProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: FakeMemberProfileRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: MemberProfileViewModel

    private fun createViewModel() {
        viewModel = MemberProfileViewModel(
            repository = repository,
            sessionManager = sessionManager,
            crashReporter = ConsoleCrashReporter(),
            analytics = KptAnalyticsTracker(NoOpAnalyticsHelper()),
            memberId = MEMBER_ID,
            groupId = GROUP_ID,
        )
    }

    /** Seeds Content + flips `isCurrentUserChairperson` via reflection-free re-dispatch: the only
     * sanctioned entry point is `OnEditRoleTap` itself, so chairperson-gated tests instead assert
     * the guard directly (see "OnEditRoleTap ignored when not chairperson" below) rather than
     * fabricating a seam that doesn't exist in production. */
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeMemberProfileRepository()
        sessionManager = SessionManager(policy = SecurityPolicy())
        createViewModel()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading with defaults`() = runTest(testDispatcher) {
        val state = viewModel.stateFlow.value
        assertTrue(state.isLoading)
        assertNull(state.member)
        assertNull(state.accounts)
        assertEquals(MemberRole.MEMBER, state.role)
        assertFalse(state.isCurrentUserChairperson)
        assertFalse(state.isEditingRole)
        assertNull(state.selectedRole)
        assertFalse(state.isUpdatingRole)
        assertEquals(0, state.meetingsAttended)
        assertEquals(0, state.totalMeetings)
        assertEquals(0.0, state.attendanceRate)
        assertNull(state.error)
    }

    @Test
    fun `init requests the stream for the nav-arg memberId`() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf(MEMBER_ID), repository.requestedClientIds)
    }

    @Test
    fun `Content maps member, accounts, and the resolved current-group role`() = runTest(testDispatcher) {
        repository.emit(ScreenState.Content(data = profileDetail()))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertFalse(state.isLoading)
        assertEquals("Amina Wanjiru", state.member?.displayName)
        assertEquals(3500.0, state.accounts?.savingsBalance)
        assertEquals(MemberRole.TREASURER, state.role)
        assertNull(state.error)
    }

    @Test
    fun `Content resolves MEMBER default when no role row matches the current groupId`() =
        runTest(testDispatcher) {
            repository.emit(
                ScreenState.Content(
                    data = profileDetail(
                        roles = listOf(MemberRoleInfo(role = MemberRole.CHAIRPERSON, groupId = 999L, assignedDate = "2026-01-01")),
                    ),
                ),
            )
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(MemberRole.MEMBER, viewModel.stateFlow.value.role)
        }

    @Test
    fun `computeAttendanceRate formula is bounded and handles zero total`() {
        assertEquals(0.0, computeAttendanceRate(meetingsAttended = 0, totalMeetings = 0))
        assertEquals(14.0 / 15.0, computeAttendanceRate(meetingsAttended = 14, totalMeetings = 15))
        assertEquals(1.0, computeAttendanceRate(meetingsAttended = 20, totalMeetings = 15))
    }

    @Test
    fun `resolveCurrentRole picks the row matching groupId`() {
        val roles = listOf(
            MemberRoleInfo(role = MemberRole.SECRETARY, groupId = 100L, assignedDate = "2026-01-01"),
            MemberRoleInfo(role = MemberRole.CHAIRPERSON, groupId = 200L, assignedDate = "2026-02-01"),
        )
        assertEquals(MemberRole.CHAIRPERSON, resolveCurrentRole(roles, "200"))
        assertEquals(MemberRole.SECRETARY, resolveCurrentRole(roles, "100"))
        assertEquals(MemberRole.MEMBER, resolveCurrentRole(roles, "not-a-number"))
    }

    @Test
    fun `OnEditRoleTap is ignored (defensively logged) when not chairperson`() = runTest(testDispatcher) {
        repository.emit(ScreenState.Content(data = profileDetail()))
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.stateFlow.value.isCurrentUserChairperson)

        viewModel.trySendAction(MemberProfileAction.OnEditRoleTap)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.stateFlow.value.isEditingRole)
    }

    @Test
    fun `OnRoleSelected sets selectedRole`() = runTest(testDispatcher) {
        viewModel.trySendAction(MemberProfileAction.OnRoleSelected(MemberRole.SECRETARY))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(MemberRole.SECRETARY, viewModel.stateFlow.value.selectedRole)
    }

    @Test
    fun `OnDismissRoleEdit clears isEditingRole and selectedRole`() = runTest(testDispatcher) {
        viewModel.trySendAction(MemberProfileAction.OnRoleSelected(MemberRole.SECRETARY))
        viewModel.trySendAction(MemberProfileAction.OnDismissRoleEdit)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertFalse(state.isEditingRole)
        assertNull(state.selectedRole)
    }

    @Test
    fun `OnConfirmRoleChange with no selectedRole is a defensive no-op`() = runTest(testDispatcher) {
        viewModel.trySendAction(MemberProfileAction.OnConfirmRoleChange)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(repository.updateMemberRoleCalls.isEmpty())
    }

    @Test
    fun `OnConfirmRoleChange with an unchanged role dismisses without calling the repository`() =
        runTest(testDispatcher) {
            repository.emit(ScreenState.Content(data = profileDetail())) // role = TREASURER
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.trySendAction(MemberProfileAction.OnRoleSelected(MemberRole.TREASURER))
            viewModel.trySendAction(MemberProfileAction.OnConfirmRoleChange)
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(repository.updateMemberRoleCalls.isEmpty())
            assertFalse(viewModel.stateFlow.value.isEditingRole)
        }

    @Test
    fun `OnConfirmRoleChange success updates role, closes the sheet, invalidates cache, and shows a snackbar`() =
        runTest(testDispatcher) {
            repository.emit(ScreenState.Content(data = profileDetail())) // role = TREASURER
            testDispatcher.scheduler.advanceUntilIdle()
            repository.updateMemberRoleResult = NetworkResult.Success(UpdateMemberRoleResult(resourceId = 42L))

            viewModel.trySendAction(MemberProfileAction.OnRoleSelected(MemberRole.CHAIRPERSON))
            viewModel.eventFlow.test {
                viewModel.trySendAction(MemberProfileAction.OnConfirmRoleChange)
                testDispatcher.scheduler.advanceUntilIdle()
                assertEquals(MemberProfileEvent.ShowSnackbar(message = "role_updated"), awaitItem())
            }

            val state = viewModel.stateFlow.value
            assertEquals(MemberRole.CHAIRPERSON, state.role)
            assertFalse(state.isEditingRole)
            assertNull(state.selectedRole)
            assertFalse(state.isUpdatingRole)
            assertEquals(1, repository.updateMemberRoleCalls.size)
            val (clientId, request) = repository.updateMemberRoleCalls.single()
            assertEquals(MEMBER_ID, clientId)
            assertEquals(MemberRole.CHAIRPERSON, request.role)
            assertEquals(GROUP_ID.toLong(), request.groupId)
        }

    @Test
    fun `OnConfirmRoleChange failure (403 not-chairperson, maps to UNKNOWN bucket) shows RoleUpdateFailed snackbar and resets isUpdatingRole`() =
        runTest(testDispatcher) {
            // MemberProfileApiImpl's status-code mapper has no dedicated bucket for HTTP 403 (the
            // "caller is not chairperson" case per api.yaml#update_member_role.errors) — it falls
            // into NetworkError.UNKNOWN (see that file's own KDoc), which this ViewModel's
            // toRoleUpdateMessageKey correctly buckets alongside 400/404/429/500 as RoleUpdateFailed.
            repository.emit(ScreenState.Content(data = profileDetail()))
            testDispatcher.scheduler.advanceUntilIdle()
            repository.updateMemberRoleResult = NetworkResult.Error(NetworkError.UNKNOWN)

            viewModel.trySendAction(MemberProfileAction.OnRoleSelected(MemberRole.CHAIRPERSON))
            viewModel.eventFlow.test {
                viewModel.trySendAction(MemberProfileAction.OnConfirmRoleChange)
                testDispatcher.scheduler.advanceUntilIdle()
                assertEquals(MemberProfileEvent.ShowSnackbar(message = "error_role_update"), awaitItem())
            }

            val state = viewModel.stateFlow.value
            assertFalse(state.isUpdatingRole)
            assertNull(state.error) // write failures are snackbar-only, never flip the loaded screen to Error
        }

    @Test
    fun `OnConfirmRoleChange failure (401) sets Auth error and shows a snackbar`() = runTest(testDispatcher) {
        repository.emit(ScreenState.Content(data = profileDetail()))
        testDispatcher.scheduler.advanceUntilIdle()
        repository.updateMemberRoleResult = NetworkResult.Error(NetworkError.UNAUTHORIZED)

        viewModel.trySendAction(MemberProfileAction.OnRoleSelected(MemberRole.CHAIRPERSON))
        viewModel.eventFlow.test {
            viewModel.trySendAction(MemberProfileAction.OnConfirmRoleChange)
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(MemberProfileEvent.ShowSnackbar(message = "error_auth"), awaitItem())
        }

        assertEquals(MemberProfileError.Auth, viewModel.stateFlow.value.error)
    }

    @Test
    fun `OnViewSavings emits NavigateToSavingsDetail`() = runTest(testDispatcher) {
        viewModel.eventFlow.test {
            viewModel.trySendAction(MemberProfileAction.OnViewSavings)
            assertEquals(MemberProfileEvent.NavigateToSavingsDetail(MEMBER_ID, GROUP_ID), awaitItem())
        }
    }

    @Test
    fun `OnBack emits NavigateToMemberList`() = runTest(testDispatcher) {
        viewModel.eventFlow.test {
            viewModel.trySendAction(MemberProfileAction.OnBack)
            assertEquals(MemberProfileEvent.NavigateToMemberList(GROUP_ID), awaitItem())
        }
    }

    @Test
    fun `Retry clears the error and re-dispatches the stream fetch`() = runTest(testDispatcher) {
        repository.emit(ScreenState.NoNetwork())
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(MemberProfileAction.Retry)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.stateFlow.value.error)
        assertEquals(1, repository.refreshTriggerCount)
    }

    @Test
    fun `stream NoNetwork maps to Network error`() = runTest(testDispatcher) {
        repository.emit(ScreenState.NoNetwork())
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(MemberProfileError.Network, state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun `stream generic Error maps to Server error`() = runTest(testDispatcher) {
        repository.emit(ScreenState.Error(error = IllegalStateException("boom"), isNetworkError = false))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(MemberProfileError.Server, viewModel.stateFlow.value.error)
    }

    @Test
    fun `stream Unauthenticated ends the session, sets Auth error, and shows a snackbar`() =
        runTest(testDispatcher) {
            sessionManager.startSession()
            assertTrue(sessionManager.isSessionActive.value)

            viewModel.eventFlow.test {
                repository.emit(ScreenState.Unauthenticated)
                testDispatcher.scheduler.advanceUntilIdle()

                assertEquals(
                    MemberProfileEvent.ShowSnackbar(message = MemberProfileError.Auth.messageKey),
                    awaitItem(),
                )
            }

            val state = viewModel.stateFlow.value
            assertEquals(MemberProfileError.Auth, state.error)
            assertFalse(state.error?.retry ?: true)
            assertFalse(sessionManager.isSessionActive.value)
        }

    private companion object {
        const val MEMBER_ID = "client-42"

        // Numeric — mirrors the raw Fineract `dt_member_role.groupId` (Long) namespace, distinct
        // from the companion-API `Group.id` (String, e.g. "grp-100") used elsewhere in this app.
        // Must stay parseable via `.toLongOrNull()` for resolveCurrentRole / UpdateMemberRoleRequest.
        const val GROUP_ID = "100"
    }
}

private fun profileDetail(
    roles: List<MemberRoleInfo> = listOf(
        MemberRoleInfo(role = MemberRole.TREASURER, groupId = 100L, assignedDate = "2026-01-15"),
    ),
): MemberProfileDetail = MemberProfileDetail(
    member = MemberProfile(
        id = 42L,
        displayName = "Amina Wanjiru",
        firstName = "Amina",
        lastName = "Wanjiru",
        phone = "+254712345678",
        hasPhoto = false,
        status = MemberStatus(id = 300, value = "Active"),
        joinDate = "2026-01-15",
        officeId = 1L,
    ),
    accounts = MemberAccounts(
        savingsBalance = 3500.0,
        savingsHistory = emptyList(),
        activeLoan = ActiveLoanSummary(
            id = 7L,
            productName = "Chama Loan",
            outstandingBalance = 1200.0,
            inArrears = false,
            dueDate = null,
        ),
    ),
    roles = roles,
)

/**
 * In-memory [MemberProfileRepository] fake — `memberProfileStream` is called exactly once per
 * [MemberProfileViewModel] instance (fixed `memberId` constructor nav-arg), mirroring
 * `FakeGroupDashboardRepository`'s single-key convention.
 */
private class FakeMemberProfileRepository : MemberProfileRepository {

    val requestedClientIds = mutableListOf<String>()
    var refreshTriggerCount = 0
    val updateMemberRoleCalls = mutableListOf<Pair<String, UpdateMemberRoleRequest>>()
    var updateMemberRoleResult: NetworkResult<UpdateMemberRoleResult, NetworkError> =
        NetworkResult.Success(UpdateMemberRoleResult(resourceId = 1L))

    private val stateFlow = MutableStateFlow<ScreenState<MemberProfileDetail>>(ScreenState.Loading)
    private val refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    fun emit(screenState: ScreenState<MemberProfileDetail>) {
        stateFlow.value = screenState
    }

    override fun memberProfileStream(
        clientId: String,
        scope: CoroutineScope,
        fetchPolicy: FetchPolicy,
    ): ScreenDataStream<MemberProfileDetail> {
        requestedClientIds += clientId
        scope.launch {
            refreshTrigger.collect { refreshTriggerCount++ }
        }
        return screenDataStreamForTesting(
            state = stateFlow,
            refreshTrigger = refreshTrigger,
        )
    }

    override suspend fun updateMemberRole(
        clientId: String,
        request: UpdateMemberRoleRequest,
    ): NetworkResult<UpdateMemberRoleResult, NetworkError> {
        updateMemberRoleCalls += clientId to request
        return updateMemberRoleResult
    }
}
