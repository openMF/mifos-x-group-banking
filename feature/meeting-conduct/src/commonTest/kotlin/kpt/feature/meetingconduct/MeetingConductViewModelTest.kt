/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.meetingconduct

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
import kpt.core.data.repository.MeetingConductRepository
import kpt.core.model.meeting.AttendanceStatus
import kpt.core.model.meeting.GroupMember
import kpt.core.model.meeting.LoanApplication
import kpt.core.model.meeting.LoanSummary
import kpt.core.model.meeting.LoanVote
import kpt.core.model.meeting.LoanVoteRecord
import kpt.core.model.meeting.MeetingConductData
import kpt.core.model.meeting.MeetingSubmissionRequest
import kpt.core.model.meeting.MeetingSubmitResult
import kpt.core.model.meeting.SavingsType
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * See API.md#viewmodel — exercises every declared [MeetingConductAction] path (on-mount load
 * happy/error, step advance + per-step validations, attendance/savings/repayment/fine mutations, the
 * fine + closing-corpus derivations, vote/approve, submit happy/offline/fallthrough, back) plus the
 * pure reducers/validators, per RULE-TDD-METHODOLOGY-001 / RULE-IMPL-DEAD-CLICKABLE-001.
 */
class MeetingConductViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeMeetingConductRepository
    private lateinit var networkMonitor: FakeNetworkMonitor

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeMeetingConductRepository()
        networkMonitor = FakeNetworkMonitor(initiallyOnline = true)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(): MeetingConductViewModel = MeetingConductViewModel(
        repository = repository,
        networkMonitor = networkMonitor,
        analytics = KptAnalyticsTracker(NoOpAnalyticsHelper()),
        crashReporter = ConsoleCrashReporter(),
        meetingId = "M4",
        meetingNumber = 4,
        groupId = 7,
    )

    // -- On-mount load ------------------------------------------------------------------------------

    @Test
    fun `init loads meeting data and resolves screen state to Content`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.stateFlow.value
        assertFalse(state.isLoading)
        assertEquals(2, state.groupMembers.size)
        assertEquals(1000L, state.openingCorpus)
        assertEquals(MeetingConductScreenState.Content, state.deriveScreenState())
        assertEquals(1, repository.loadCallCount)
    }

    @Test
    fun `init load failure surfaces a loadError`() = runTest(testDispatcher) {
        repository.loadResult = NetworkResult.Error(NetworkError.SERVER)
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("error_server", vm.stateFlow.value.loadError)
        assertFalse(vm.stateFlow.value.isLoading)
    }

    // -- Step advance + validation ------------------------------------------------------------------

    @Test
    fun `NextStep at step 0 advances without validation`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.trySendAction(MeetingConductAction.NextStep)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, vm.stateFlow.value.currentStep)
    }

    @Test
    fun `NextStep at attendance step blocks until all members recorded and emits ShowStepError`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        vm.trySendAction(MeetingConductAction.NextStep) // -> step 1
        testDispatcher.scheduler.advanceUntilIdle()

        vm.eventFlow.test {
            vm.trySendAction(MeetingConductAction.NextStep) // attendance incomplete
            assertEquals(MeetingConductEvent.ShowStepError("error_attendance_incomplete"), awaitItem())
        }
        assertEquals(1, vm.stateFlow.value.currentStep)
        assertEquals("error_attendance_incomplete", vm.stateFlow.value.stepValidationError)
    }

    @Test
    fun `SetAttendance for all members lets the attendance step advance and folds fines`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        vm.trySendAction(MeetingConductAction.NextStep) // -> step 1
        testDispatcher.scheduler.advanceUntilIdle()

        vm.trySendAction(MeetingConductAction.SetAttendance("m1", AttendanceStatus.PRESENT))
        vm.trySendAction(MeetingConductAction.SetAttendance("m2", AttendanceStatus.LATE))
        testDispatcher.scheduler.advanceUntilIdle()

        // one LATE => KES 50 fine folded into totalFinesCollected
        assertEquals(LATE_FINE, vm.stateFlow.value.totalFinesCollected)

        vm.trySendAction(MeetingConductAction.NextStep)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, vm.stateFlow.value.currentStep)
    }

    // -- Mutations recompute derived totals ----------------------------------------------------------

    @Test
    fun `SetSavingsAmount recomputes runningSavingsTotal and closingCorpus`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.trySendAction(MeetingConductAction.SetSavingsAmount("m1", 300L, SavingsType.GROUP_LINKED))
        vm.trySendAction(MeetingConductAction.SetSavingsAmount("m1", 100L, SavingsType.INDIVIDUAL))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.stateFlow.value
        assertEquals(400L, state.runningSavingsTotal)
        assertEquals(1000L + 400L, state.closingCorpus)
    }

    @Test
    fun `SetLoanRepayment recomputes totalRepayments`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.trySendAction(MeetingConductAction.SetLoanRepayment("L1", 250L))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(250L, vm.stateFlow.value.totalRepayments)
    }

    // -- Vote + approve -----------------------------------------------------------------------------

    @Test
    fun `CastLoanVote records the vote and ApproveLoanApplication marks the application approved`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.trySendAction(MeetingConductAction.CastLoanVote("A1", LoanVote.FOR))
        vm.trySendAction(MeetingConductAction.ApproveLoanApplication("A1"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.stateFlow.value
        assertEquals(LoanVote.FOR, state.loanVotes["A1"])
        assertTrue("A1" in state.approvedApplicationIds)
        // approved 500 application reduces closing corpus below opening
        assertEquals(1000L - 500L, state.closingCorpus)
    }

    // -- Submit -------------------------------------------------------------------------------------

    @Test
    fun `SubmitMeeting online success sets submitSuccess and emits NavigateToMeetingSummary`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        driveToClosingStep(vm)

        vm.eventFlow.test {
            vm.trySendAction(MeetingConductAction.SubmitMeeting)
            assertEquals(MeetingConductEvent.ShowSubmitSuccess, awaitItem())
            assertEquals(
                MeetingConductEvent.NavigateToMeetingSummary(meetingId = "M4", meetingNumber = 4, groupId = 7),
                awaitItem(),
            )
        }
        val state = vm.stateFlow.value
        assertTrue(state.submitSuccess)
        assertFalse(state.isSubmitting)
        assertFalse(state.isOffline)
        assertEquals(1, repository.submitCallCount)
        assertEquals(0, repository.enqueueCallCount)
    }

    @Test
    fun `SubmitMeeting while offline enqueues the payload and reports offline success`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        driveToClosingStep(vm)
        networkMonitor.setOnline(false)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.trySendAction(MeetingConductAction.SubmitMeeting)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.stateFlow.value
        assertTrue(state.submitSuccess)
        assertTrue(state.isOffline)
        assertEquals(0, repository.submitCallCount)
        assertEquals(1, repository.enqueueCallCount)
    }

    @Test
    fun `SubmitMeeting online failure falls through to the offline enqueue`() = runTest(testDispatcher) {
        repository.submitResult = NetworkResult.Error(NetworkError.SERVER)
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        driveToClosingStep(vm)

        vm.trySendAction(MeetingConductAction.SubmitMeeting)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.stateFlow.value
        assertTrue(state.submitSuccess)
        assertTrue(state.isOffline)
        assertEquals(1, repository.submitCallCount)
        assertEquals(1, repository.enqueueCallCount)
    }

    // -- Navigation + error dismissal ---------------------------------------------------------------

    @Test
    fun `OnBack emits NavigateBack`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.eventFlow.test {
            vm.trySendAction(MeetingConductAction.OnBack)
            assertEquals(MeetingConductEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `DismissError clears the validation error`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        vm.trySendAction(MeetingConductAction.NextStep) // step 1
        testDispatcher.scheduler.advanceUntilIdle()
        vm.trySendAction(MeetingConductAction.NextStep) // blocked -> validation error set
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(vm.stateFlow.value.stepValidationError)

        vm.trySendAction(MeetingConductAction.DismissError)
        testDispatcher.scheduler.advanceUntilIdle()
        assertNull(vm.stateFlow.value.stepValidationError)
    }

    /** Records attendance for all members + valid group savings, then advances to the closing step (6). */
    private fun driveToClosingStep(vm: MeetingConductViewModel) {
        vm.trySendAction(MeetingConductAction.NextStep) // 0 -> 1
        testDispatcher.scheduler.advanceUntilIdle()
        vm.trySendAction(MeetingConductAction.SetAttendance("m1", AttendanceStatus.PRESENT))
        vm.trySendAction(MeetingConductAction.SetAttendance("m2", AttendanceStatus.PRESENT))
        testDispatcher.scheduler.advanceUntilIdle()
        vm.trySendAction(MeetingConductAction.NextStep) // 1 -> 2
        testDispatcher.scheduler.advanceUntilIdle()
        vm.trySendAction(MeetingConductAction.NextStep) // 2 -> 3
        testDispatcher.scheduler.advanceUntilIdle()
        vm.trySendAction(MeetingConductAction.SetSavingsAmount("m1", 200L, SavingsType.GROUP_LINKED))
        vm.trySendAction(MeetingConductAction.SetSavingsAmount("m2", 200L, SavingsType.GROUP_LINKED))
        testDispatcher.scheduler.advanceUntilIdle()
        vm.trySendAction(MeetingConductAction.NextStep) // 3 -> 4
        testDispatcher.scheduler.advanceUntilIdle()
        vm.trySendAction(MeetingConductAction.NextStep) // 4 -> 5 (no active loans)
        testDispatcher.scheduler.advanceUntilIdle()
        vm.trySendAction(MeetingConductAction.NextStep) // 5 -> 6 (no disbursals, corpus gate passes)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(LAST_STEP, vm.stateFlow.value.currentStep)
    }
}

// -- Pure reducer / validator tests (no coroutines) -------------------------------------------------

class MeetingConductReducerTest {

    private fun stateWith(
        members: List<GroupMember> = listOf(member("m1"), member("m2")),
        openingCorpus: Long = 1000L,
        loans: List<LoanSummary> = emptyList(),
        applications: List<LoanApplication> = emptyList(),
    ): MeetingConductState = MeetingConductState(
        groupMembers = members,
        openingCorpus = openingCorpus,
        activeLoans = loans,
        pendingLoanApplications = applications,
    )

    @Test
    fun `recomputeTotals folds attendance fines, savings, repayments and disbursals into closing corpus`() {
        val app = LoanApplication(id = "A1", memberId = "m1", memberName = "A", requestedAmount = 400L, purpose = "biz")
        val state = stateWith(applications = listOf(app)).copy(
            attendanceMap = mapOf("m1" to AttendanceStatus.LATE, "m2" to AttendanceStatus.ABSENT),
            savingsMap = mapOf("m1" to kpt.core.model.meeting.SavingsEntry("m1", groupAmount = 300L)),
            loanRepayments = mapOf("L1" to 200L),
            loanFines = mapOf("L1" to 20L),
            approvedApplicationIds = setOf("A1"),
        ).recomputeTotals()

        assertEquals(300L, state.runningSavingsTotal)
        assertEquals(200L, state.totalRepayments)
        // fines = LATE(50) + ABSENT(100) + loanFine(20) = 170
        assertEquals(170L, state.totalFinesCollected)
        assertEquals(400L, state.totalLoansDisbursed)
        // closing = 1000 + 300 + 200 + 170 - 400 = 1270
        assertEquals(1270L, state.closingCorpus)
    }

    @Test
    fun `min group savings validator rejects a member below the 200 floor`() {
        val ok = stateWith().copy(
            savingsMap = mapOf(
                "m1" to kpt.core.model.meeting.SavingsEntry("m1", groupAmount = 200L),
                "m2" to kpt.core.model.meeting.SavingsEntry("m2", groupAmount = 250L),
            ),
        )
        val bad = stateWith().copy(
            savingsMap = mapOf("m1" to kpt.core.model.meeting.SavingsEntry("m1", groupAmount = 150L)),
        )
        assertTrue(allGroupSavingsMeetMinimum(ok))
        assertFalse(allGroupSavingsMeetMinimum(bad))
    }

    @Test
    fun `repayment validator rejects an amount above the outstanding balance`() {
        val loan = LoanSummary("L1", "m1", "A", "A", 1000L, 500L, false, 3, 12, 100L)
        val ok = stateWith(loans = listOf(loan)).copy(loanRepayments = mapOf("L1" to 400L))
        val bad = stateWith(loans = listOf(loan)).copy(loanRepayments = mapOf("L1" to 600L))
        assertTrue(allRepaymentsWithinBalance(ok))
        assertFalse(allRepaymentsWithinBalance(bad))
    }

    @Test
    fun `corpus gate blocks a disbursement that would drive closing corpus negative`() {
        val app = LoanApplication(id = "A1", memberId = "m1", memberName = "A", requestedAmount = 5000L, purpose = "biz")
        val state = stateWith(openingCorpus = 1000L, applications = listOf(app))
            .copy(approvedApplicationIds = setOf("A1"))
            .recomputeTotals()
        assertFalse(corpusGatePasses(state))
    }

    @Test
    fun `toSubmissionRequest carries attendance, savings, repayments and approved disbursals`() {
        val app = LoanApplication(id = "A1", memberId = "m1", memberName = "A", requestedAmount = 300L, purpose = "biz")
        val members = listOf(member("m1", savingsId = "S1"), member("m2", savingsId = "S2"))
        val request = stateWith(members = members, applications = listOf(app)).copy(
            meetingId = "M4",
            meetingNumber = 4,
            groupId = 7,
            attendanceMap = mapOf("m1" to AttendanceStatus.PRESENT, "m2" to AttendanceStatus.ABSENT),
            savingsMap = mapOf("m1" to kpt.core.model.meeting.SavingsEntry("m1", groupAmount = 300L)),
            loanRepayments = mapOf("L1" to 200L),
            approvedApplicationIds = setOf("A1"),
        ).recomputeTotals().toSubmissionRequest()

        assertEquals("M4", request.meetingId)
        assertEquals(2, request.attendance.size)
        assertEquals(1, request.savings.size) // only m1 entered savings against S1
        assertEquals("S1", request.savings.first().savingsAccountId)
        assertEquals(1, request.repayments.size)
        assertEquals(1, request.disbursals.size)
        assertEquals(300L, request.disbursals.first().amount)
    }
}

private fun member(id: String, savingsId: String? = null): GroupMember =
    GroupMember(memberId = id, name = "Member $id", initials = id.uppercase(), role = "Member", savingsAccountId = savingsId)

private class FakeMeetingConductRepository : MeetingConductRepository {
    var loadResult: NetworkResult<MeetingConductData, NetworkError> = NetworkResult.Success(
        MeetingConductData(
            previousMeetingSummary = null,
            groupMembers = listOf(member("m1"), member("m2")),
            openingCorpus = 1000L,
            cashOnHand = 200L,
            activeLoans = emptyList(),
            pendingLoanApplications = listOf(
                LoanApplication(id = "A1", memberId = "m1", memberName = "Member m1", requestedAmount = 500L, purpose = "biz"),
            ),
        ),
    )
    var submitResult: NetworkResult<MeetingSubmitResult, NetworkError> =
        NetworkResult.Success(MeetingSubmitResult(meetingId = "M4", isOffline = false))

    var loadCallCount: Int = 0
        private set
    var submitCallCount: Int = 0
        private set
    var enqueueCallCount: Int = 0
        private set

    override suspend fun loadMeetingData(groupId: Int, meetingNumber: Int): NetworkResult<MeetingConductData, NetworkError> {
        loadCallCount++
        return loadResult
    }

    override suspend fun getLoanVotes(loanId: String): NetworkResult<LoanVoteRecord, NetworkError> =
        NetworkResult.Success(LoanVoteRecord(loanId = loanId, votesFor = 0, votesAgainst = 0, votesAbstain = 0))

    override suspend fun submitMeeting(request: MeetingSubmissionRequest): NetworkResult<MeetingSubmitResult, NetworkError> {
        submitCallCount++
        return submitResult
    }

    override suspend fun enqueueMeetingOffline(request: MeetingSubmissionRequest): Long {
        enqueueCallCount++
        return 1L
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
