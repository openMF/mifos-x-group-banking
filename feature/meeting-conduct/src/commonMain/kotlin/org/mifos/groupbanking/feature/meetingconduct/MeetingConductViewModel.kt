/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.meetingconduct

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitor
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kpt.core.analytics.KptAnalyticsTracker
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.base.observability.CrashReporter
import kpt.core.base.observability.CrashSeverity
import kpt.core.base.ui.viewmodel.BaseViewModel
import org.mifos.groupbanking.core.data.repository.MeetingConductRepository
import org.mifos.groupbanking.core.model.meeting.AttendanceStatus
import org.mifos.groupbanking.core.model.meeting.AttendanceSubmission
import org.mifos.groupbanking.core.model.meeting.DisbursalSubmission
import org.mifos.groupbanking.core.model.meeting.GroupMember
import org.mifos.groupbanking.core.model.meeting.LoanApplication
import org.mifos.groupbanking.core.model.meeting.LoanSummary
import org.mifos.groupbanking.core.model.meeting.LoanVote
import org.mifos.groupbanking.core.model.meeting.MeetingConductData
import org.mifos.groupbanking.core.model.meeting.MeetingSubmissionRequest
import org.mifos.groupbanking.core.model.meeting.MeetingSubmitResult
import org.mifos.groupbanking.core.model.meeting.PreviousMeetingSummary
import org.mifos.groupbanking.core.model.meeting.RepaymentSubmission
import org.mifos.groupbanking.core.model.meeting.SavingsEntry
import org.mifos.groupbanking.core.model.meeting.SavingsSubmission
import org.mifos.groupbanking.core.model.meeting.SavingsType
import kotlin.time.Clock

// MVI stack (State/ScreenState/Error/Event/Action/ViewModel/DI) for the `meeting-conduct` 7-step
// wizard — see API.md#viewmodel / #state / #actions / #events / #di for the full generated-symbol
// contract.
private const val TAG = "MeetingConductViewModel"

/** `flow.yaml#step_advance.step_1.auto_calc_fines` — the fixed late/absent attendance fines (FR-012). */
internal const val LATE_FINE: Long = 50L
internal const val ABSENT_FINE: Long = 100L

/** `flow.yaml#step_advance.step_3` — minimum group savings per member (FR-020). */
internal const val MIN_GROUP_SAVINGS: Long = 200L

/** The 7 wizard steps (0-indexed); the terminal step enables Submit. */
internal const val LAST_STEP: Int = 6

/**
 * Screen-level render state for `meeting-conduct-screen` — verbatim mirror of
 * `ui.yaml#state_model.MeetingConductViewModel.screen_state.members` (5 members). Derived only (not
 * stored) via [MeetingConductState.deriveScreenState] — keeps `isLoading`/`isSubmitting`/
 * `submitSuccess`/`submitError` the single source of truth. See API.md#state.
 */
@Serializable
sealed interface MeetingConductScreenState {
    @Serializable
    data object Loading : MeetingConductScreenState

    @Serializable
    data object Content : MeetingConductScreenState

    @Serializable
    data object Submitting : MeetingConductScreenState

    @Serializable
    data object SubmitSuccess : MeetingConductScreenState

    @Serializable
    data object SubmitError : MeetingConductScreenState
}

/**
 * MVI state for `MeetingConductViewModel`. Field set + defaults mirror
 * `ui.yaml#state_model.MeetingConductViewModel.state.fields`. Domain-model collections
 * ([groupMembers]/[activeLoans]/[pendingLoanApplications]/[previousMeetingSummary]) are `@Transient`
 * — re-derived from [MeetingConductRepository] on (re)mount, never restored from a snapshot.
 * The financial totals ([runningSavingsTotal]/[totalRepayments]/[totalFinesCollected]/
 * [totalLoansDisbursed]/[closingCorpus]) are CLIENT-DERIVED — recomputed by the reducer on every
 * mutation. See API.md#state.
 */
@Serializable
@Immutable
data class MeetingConductState(
    val currentStep: Int = 0,
    val totalSteps: Int = 7,
    val meetingId: String = "",
    val meetingNumber: Int = 0,
    val groupId: Int = 0,
    @Transient val previousMeetingSummary: PreviousMeetingSummary? = null,
    @Transient val groupMembers: List<GroupMember> = emptyList(),
    val attendanceMap: Map<String, AttendanceStatus> = emptyMap(),
    val openingCorpus: Long = 0L,
    val cashOnHand: Long = 0L,
    val savingsMap: Map<String, SavingsEntry> = emptyMap(),
    @Transient val activeLoans: List<LoanSummary> = emptyList(),
    val loanRepayments: Map<String, Long> = emptyMap(),
    val loanFines: Map<String, Long> = emptyMap(),
    @Transient val pendingLoanApplications: List<LoanApplication> = emptyList(),
    val loanVotes: Map<String, LoanVote> = emptyMap(),
    val approvedApplicationIds: Set<String> = emptySet(),
    val runningSavingsTotal: Long = 0L,
    val totalRepayments: Long = 0L,
    val totalFinesCollected: Long = 0L,
    val totalLoansDisbursed: Long = 0L,
    val closingCorpus: Long = 0L,
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val isOffline: Boolean = false,
    val submitSuccess: Boolean = false,
    val submitError: String? = null,
    val stepValidationError: String? = null,
    val loadError: String? = null,
) {
    /** Attendance is complete when every group member has a recorded status. */
    val isAttendanceComplete: Boolean
        get() = groupMembers.isNotEmpty() && groupMembers.all { attendanceMap.containsKey(it.memberId) }
}

/**
 * Derives [MeetingConductScreenState] from [MeetingConductState] — see the type's KDoc for why this
 * is a pure function rather than a stored field.
 */
fun MeetingConductState.deriveScreenState(): MeetingConductScreenState = when {
    submitSuccess -> MeetingConductScreenState.SubmitSuccess
    isSubmitting -> MeetingConductScreenState.Submitting
    submitError != null -> MeetingConductScreenState.SubmitError
    isLoading -> MeetingConductScreenState.Loading
    else -> MeetingConductScreenState.Content
}

/**
 * One-shot side effects — verbatim mirror of
 * `ui.yaml#state_model.MeetingConductViewModel.events.members` (6 members). See API.md#events.
 */
sealed interface MeetingConductEvent {
    data class NavigateToMeetingSummary(val meetingId: String, val meetingNumber: Int, val groupId: Int) : MeetingConductEvent

    /**
     * G6 fix (`ui.yaml#view_full_previous_btn.on_click`): carries the REAL prior meeting's
     * [meetingId] (String, from `previousMeetingSummary.meetingId` — NOT the meeting number) plus
     * [meetingNumber] (the prior meeting's number, passed separately, no longer dropped), [groupId],
     * and [launchedFrom]="conduct" so the review renders in conduct-context (shows the
     * "Start Meeting #N" CTA).
     */
    data class NavigateToPreviousMeetingReview(
        val meetingId: String,
        val meetingNumber: Int,
        val groupId: Int,
        val launchedFrom: String,
    ) : MeetingConductEvent
    data object NavigateBack : MeetingConductEvent
    data class ShowStepError(val message: String) : MeetingConductEvent
    data object ShowSubmitSuccess : MeetingConductEvent
    data class ShowSubmitError(val message: String) : MeetingConductEvent
}

/**
 * User intents — the 13 top-level members are a verbatim mirror of
 * `ui.yaml#state_model.MeetingConductViewModel.actions.members` (RULE-IMPL-DEAD-CLICKABLE-001
 * Rule 1). [Internal] is the sanctioned async-result-routing sub-interface (never a user intent).
 * See API.md#actions.
 */
sealed interface MeetingConductAction {
    data object LoadMeetingData : MeetingConductAction
    data object NextStep : MeetingConductAction
    data object PreviousStep : MeetingConductAction
    data class SetAttendance(val memberId: String, val status: AttendanceStatus) : MeetingConductAction
    data class SetSavingsAmount(val memberId: String, val amount: Long, val type: SavingsType) : MeetingConductAction
    data class SetLoanRepayment(val loanId: String, val amount: Long) : MeetingConductAction
    data class SetLoanFine(val loanId: String, val fineAmount: Long) : MeetingConductAction
    data class CastLoanVote(val loanId: String, val vote: LoanVote) : MeetingConductAction
    data class ApproveLoanApplication(val loanId: String) : MeetingConductAction
    data object SubmitMeeting : MeetingConductAction
    data object ViewFullPreviousMeeting : MeetingConductAction
    data object SaveProgressLocally : MeetingConductAction
    data object DismissError : MeetingConductAction
    data object OnBack : MeetingConductAction

    /** Async coroutine results — routed via `trySendAction`, never dispatched by the UI. */
    sealed interface Internal : MeetingConductAction {
        data class MeetingDataLoaded(val result: NetworkResult<MeetingConductData, NetworkError>) : Internal
        data class SubmitResult(val result: NetworkResult<MeetingSubmitResult, NetworkError>) : Internal
    }
}

/**
 * MVI processor for the meeting-conduct 7-step wizard (`business_logic.kind: composite`). On mount it
 * loads the members/corpus/previous-record/active-loans fan-in via
 * [MeetingConductRepository.loadMeetingData]. Steps advance through [handleNextStep], which validates
 * the current step (`flow.yaml#step_advance`) before advancing: attendance-complete (step 1,
 * auto-calc fines), min-group-savings (step 3), repayment ≤ outstanding (step 4), and the corpus gate
 * (step 5, prospective closing ≥ 0). On submit it runs the ordered POST sequence online, falling back
 * to the offline `sync_queue` enqueue on any error (`flow.yaml#submit_meeting`). [analytics]/
 * [crashReporter] are mandatory here (financial mutation, outside `{crud, nav_only}`) per
 * RULE-IDEA-IMPL-INTELLIGENCE-001.
 *
 * **`groupId` gap (flagged, CFF1):** `ui.yaml#nav_params` names only `{meetingId, meetingNumber,
 * groupId}` — no `groupId`, which `get_active_loans` requires. [groupId] is threaded as the
 * `groupId` argument (center↔group are 1:1 in this group-banking domain); flagged for the idea-layer
 * to add an explicit `groupId` nav param.
 *
 * See API.md#viewmodel.
 */
internal class MeetingConductViewModel(
    private val repository: MeetingConductRepository,
    private val networkMonitor: NetworkMonitor,
    private val analytics: KptAnalyticsTracker,
    private val crashReporter: CrashReporter,
    private val meetingId: String,
    private val meetingNumber: Int,
    private val groupId: Int,
) : BaseViewModel<MeetingConductState, MeetingConductEvent, MeetingConductAction>(
    initialState = MeetingConductState(
        meetingId = meetingId,
        meetingNumber = meetingNumber,
        groupId = groupId,
    ),
) {

    private var loadJob: Job? = null
    private var submitJob: Job? = null

    init {
        crashReporter.recordMessage(
            message = "feature=meeting-conduct screen=meeting-conduct-screen meetingId=$meetingId groupId=$groupId",
            level = CrashSeverity.Debug,
        )
        updateState { copy(isOffline = !networkMonitor.isOnline.value) }
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online -> updateState { copy(isOffline = !online) } }
        }
        trySendAction(MeetingConductAction.LoadMeetingData)
    }

    override fun handleAction(action: MeetingConductAction) {
        when (action) {
            MeetingConductAction.LoadMeetingData -> loadMeetingData()
            MeetingConductAction.NextStep -> handleNextStep()
            MeetingConductAction.PreviousStep -> handlePreviousStep()
            is MeetingConductAction.SetAttendance -> handleSetAttendance(action.memberId, action.status)
            is MeetingConductAction.SetSavingsAmount -> handleSetSavings(action.memberId, action.amount, action.type)
            is MeetingConductAction.SetLoanRepayment -> handleSetRepayment(action.loanId, action.amount)
            is MeetingConductAction.SetLoanFine -> handleSetLoanFine(action.loanId, action.fineAmount)
            is MeetingConductAction.CastLoanVote -> handleCastVote(action.loanId, action.vote)
            is MeetingConductAction.ApproveLoanApplication -> handleApprove(action.loanId)
            MeetingConductAction.SubmitMeeting -> handleSubmit()
            MeetingConductAction.ViewFullPreviousMeeting -> handleViewPrevious()
            MeetingConductAction.SaveProgressLocally -> Logger.d(TAG) { "SaveProgressLocally meetingId=$meetingId step=${state.currentStep}" }
            MeetingConductAction.DismissError -> updateState { copy(stepValidationError = null, submitError = null) }
            MeetingConductAction.OnBack -> sendEvent(MeetingConductEvent.NavigateBack)
            is MeetingConductAction.Internal.MeetingDataLoaded -> handleMeetingDataLoaded(action.result)
            is MeetingConductAction.Internal.SubmitResult -> handleSubmitResult(action.result)
        }
    }

    // -- On-mount load (flow.yaml#screen_init.parallel_load) ----------------------------------------

    private fun loadMeetingData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            updateState { copy(isLoading = true, loadError = null) }
            val result = repository.loadMeetingData(groupId = groupId, meetingNumber = meetingNumber)
            trySendAction(MeetingConductAction.Internal.MeetingDataLoaded(result))
        }
    }

    private fun handleMeetingDataLoaded(result: NetworkResult<MeetingConductData, NetworkError>) {
        when (result) {
            is NetworkResult.Success -> {
                val data = result.data
                Logger.i(TAG) { "loadMeetingData succeeded members=${data.groupMembers.size} openingCorpus=${data.openingCorpus}" }
                analytics.trackSync(syncType = "meeting_started", itemCount = data.groupMembers.size)
                updateState {
                    copy(
                        previousMeetingSummary = data.previousMeetingSummary,
                        groupMembers = data.groupMembers,
                        openingCorpus = data.openingCorpus,
                        cashOnHand = data.cashOnHand,
                        activeLoans = data.activeLoans,
                        pendingLoanApplications = data.pendingLoanApplications,
                        isLoading = false,
                        loadError = null,
                    ).recomputeTotals()
                }
            }
            is NetworkResult.Error -> {
                crashReporter.recordMessage(
                    message = "meeting-conduct: loadMeetingData failed groupId=$groupId networkError=${result.error}",
                    level = CrashSeverity.Warning,
                )
                updateState { copy(isLoading = false, loadError = result.error.toMessageKey()) }
            }
        }
    }

    // -- Step navigation (ui.yaml effect: transform_state) ------------------------------------------

    private fun handleNextStep() {
        val current = state
        val error = validateStep(current)
        if (error != null) {
            Logger.w(TAG) { "NextStep blocked at step=${current.currentStep}: $error" }
            updateState { copy(stepValidationError = error) }
            sendEvent(MeetingConductEvent.ShowStepError(error))
            return
        }
        if (current.currentStep >= LAST_STEP) {
            handleSubmit()
            return
        }
        val next = current.currentStep + 1
        analytics.trackSync(syncType = "step_advanced", itemCount = next)
        updateState { copy(currentStep = next, stepValidationError = null).recomputeTotals() }
        trySendAction(MeetingConductAction.SaveProgressLocally)
    }

    private fun handlePreviousStep() {
        val current = state
        if (current.currentStep <= 0) {
            Logger.d(TAG) { "PreviousStep at step 0 — no-op" }
            return
        }
        updateState { copy(currentStep = currentStep - 1, stepValidationError = null) }
    }

    /**
     * Validates the current step per `flow.yaml#step_advance`, returning a message-key on failure or
     * `null` when the step may advance. Steps 0/2/6 have no validation.
     */
    private fun validateStep(state: MeetingConductState): String? = when (state.currentStep) {
        1 -> if (state.isAttendanceComplete) null else "error_attendance_incomplete"
        3 -> if (allGroupSavingsMeetMinimum(state)) null else "error_min_contribution"
        4 -> if (allRepaymentsWithinBalance(state)) null else "error_repayment_exceeds"
        5 -> if (corpusGatePasses(state)) null else "error_corpus_insufficient"
        else -> null
    }

    // -- Attendance (step 1) ------------------------------------------------------------------------

    private fun handleSetAttendance(memberId: String, status: AttendanceStatus) {
        analytics.trackSync(syncType = "attendance_recorded")
        updateState {
            copy(attendanceMap = attendanceMap + (memberId to status), stepValidationError = null).recomputeTotals()
        }
    }

    // -- Savings (step 3) ---------------------------------------------------------------------------

    private fun handleSetSavings(memberId: String, amount: Long, type: SavingsType) {
        updateState {
            val current = savingsMap[memberId] ?: SavingsEntry(memberId = memberId)
            val updated = when (type) {
                SavingsType.GROUP_LINKED -> current.copy(groupAmount = amount.coerceAtLeast(0L))
                SavingsType.INDIVIDUAL -> current.copy(individualAmount = amount.coerceAtLeast(0L))
            }
            copy(savingsMap = savingsMap + (memberId to updated), stepValidationError = null).recomputeTotals()
        }
    }

    // -- Loan review (step 4) -----------------------------------------------------------------------

    private fun handleSetRepayment(loanId: String, amount: Long) {
        analytics.trackSync(syncType = "loan_repayment_entered")
        updateState {
            copy(loanRepayments = loanRepayments + (loanId to amount.coerceAtLeast(0L)), stepValidationError = null).recomputeTotals()
        }
    }

    private fun handleSetLoanFine(loanId: String, fineAmount: Long) {
        updateState {
            copy(loanFines = loanFines + (loanId to fineAmount.coerceAtLeast(0L)), stepValidationError = null).recomputeTotals()
        }
    }

    // -- Loan applications (step 5) -----------------------------------------------------------------

    private fun handleCastVote(loanId: String, vote: LoanVote) {
        analytics.trackSync(syncType = "loan_voted")
        updateState { copy(loanVotes = loanVotes + (loanId to vote)) }
    }

    private fun handleApprove(loanId: String) {
        val application = state.pendingLoanApplications.firstOrNull { it.id == loanId }
        if (application == null) {
            Logger.w(TAG) { "ApproveLoanApplication ignored — no such application id=$loanId" }
            return
        }
        Logger.i(TAG) { "loan application approved id=$loanId amount=${application.requestedAmount}" }
        updateState { copy(approvedApplicationIds = approvedApplicationIds + loanId, stepValidationError = null).recomputeTotals() }
    }

    // -- Submit (ui.yaml effect: call_api) ----------------------------------------------------------

    private fun handleSubmit() {
        val current = state
        // Guard: submit only allowed from the terminal step and only when not already in flight.
        if (current.currentStep < LAST_STEP || current.isSubmitting) {
            Logger.w(TAG) { "SubmitMeeting blocked — step=${current.currentStep} isSubmitting=${current.isSubmitting}" }
            return
        }

        submitJob?.cancel()
        submitJob = viewModelScope.launch {
            updateState { copy(isSubmitting = true, submitError = null) }
            val request = current.toSubmissionRequest()

            if (!networkMonitor.isOnline.value) {
                enqueueOffline(request)
                return@launch
            }
            val result = repository.submitMeeting(request)
            trySendAction(MeetingConductAction.Internal.SubmitResult(result))
        }
    }

    private fun handleSubmitResult(result: NetworkResult<MeetingSubmitResult, NetworkError>) {
        when (result) {
            is NetworkResult.Success -> applySubmitSuccess(isOffline = result.data.isOffline)
            is NetworkResult.Error -> {
                // flow.yaml#submit_meeting.online.on_any_5xx: fallthrough_to_offline_path — any submit
                // failure enqueues the whole payload rather than surfacing a hard failure.
                Logger.w(TAG) { "submitMeeting failed (${result.error}) — falling through to offline enqueue" }
                crashReporter.recordMessage(
                    message = "meeting-conduct: submit failed meetingId=$meetingId networkError=${result.error} — enqueue offline",
                    level = CrashSeverity.Warning,
                )
                submitJob = viewModelScope.launch { enqueueOffline(state.toSubmissionRequest()) }
            }
        }
    }

    private suspend fun enqueueOffline(request: MeetingSubmissionRequest) {
        val queuedId = repository.enqueueMeetingOffline(request)
        Logger.i(TAG) { "meeting enqueued offline id=$queuedId meetingId=$meetingId" }
        applySubmitSuccess(isOffline = true)
    }

    private fun applySubmitSuccess(isOffline: Boolean) {
        analytics.trackSync(syncType = if (isOffline) "meeting_submitted_offline" else "meeting_submitted", success = true)
        updateState { copy(isSubmitting = false, submitSuccess = true, isOffline = isOffline, submitError = null) }
        sendEvent(MeetingConductEvent.ShowSubmitSuccess)
        sendEvent(MeetingConductEvent.NavigateToMeetingSummary(meetingId = meetingId, meetingNumber = meetingNumber, groupId = groupId))
    }

    // -- Navigation ---------------------------------------------------------------------------------

    private fun handleViewPrevious() {
        // G6: bind the PRIOR meeting's real id + number (previousMeetingSummary) — not the wizard's
        // current meeting id, and never the meeting NUMBER bound to meeting_id. When the server has not
        // supplied a prior meeting id yet (companion API pending), `meetingId` is blank; the
        // previous-meeting-review keys primarily off (groupId, meetingNumber) so the review still
        // resolves. launched_from="conduct" makes the review show the "Start Meeting #N" CTA.
        val summary = state.previousMeetingSummary ?: return
        sendEvent(
            MeetingConductEvent.NavigateToPreviousMeetingReview(
                meetingId = summary.meetingId,
                meetingNumber = summary.meetingNumber,
                groupId = groupId,
                launchedFrom = "conduct",
            ),
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Pure reducers / validators (top-level — independently unit-testable)
// ---------------------------------------------------------------------------------------------

/**
 * Recomputes every client-derived total from the raw maps:
 * - `runningSavingsTotal` = Σ savings (group + individual)
 * - `totalRepayments` = Σ loanRepayments
 * - `totalFinesCollected` = attendance fines (late 50 / absent 100) + overdue-loan fines
 * - `totalLoansDisbursed` = Σ approved applications' requested amounts
 * - `closingCorpus` = openingCorpus + savings + repayments + fines − disbursed
 * (`flow.yaml#step_advance.step_6.formula`).
 */
internal fun MeetingConductState.recomputeTotals(): MeetingConductState {
    val savingsTotal = savingsMap.values.sumOf { it.total }
    val repaymentsTotal = loanRepayments.values.sum()
    val attendanceFines = attendanceMap.values.sumOf { status ->
        when (status) {
            AttendanceStatus.LATE -> LATE_FINE
            AttendanceStatus.ABSENT -> ABSENT_FINE
            AttendanceStatus.PRESENT -> 0L
        }
    }
    val loanFineTotal = loanFines.values.sum()
    val finesTotal = attendanceFines + loanFineTotal
    val disbursedTotal = pendingLoanApplications
        .filter { it.id in approvedApplicationIds }
        .sumOf { it.requestedAmount }
    val closing = openingCorpus + savingsTotal + repaymentsTotal + finesTotal - disbursedTotal
    return copy(
        runningSavingsTotal = savingsTotal,
        totalRepayments = repaymentsTotal,
        totalFinesCollected = finesTotal,
        totalLoansDisbursed = disbursedTotal,
        closingCorpus = closing,
    )
}

/** `flow.yaml#step_advance.step_3.validation: all_savings_amounts_gte_200` (group bucket only). */
internal fun allGroupSavingsMeetMinimum(state: MeetingConductState): Boolean =
    state.groupMembers.all { member ->
        (state.savingsMap[member.memberId]?.groupAmount ?: 0L) >= MIN_GROUP_SAVINGS
    }

/** `flow.yaml#step_advance.step_4.validation: repayments_lte_outstanding_balance`. */
internal fun allRepaymentsWithinBalance(state: MeetingConductState): Boolean =
    state.activeLoans.all { loan ->
        (state.loanRepayments[loan.loanId] ?: 0L) <= loan.outstandingBalance
    }

/**
 * `flow.yaml#step_advance.step_5.corpus_gate`: prospective closing corpus (with the approved
 * disbursals already folded into [MeetingConductState.closingCorpus] via [recomputeTotals]) must be
 * ≥ 0. Returns `false` when disbursing the approved applications would drive the corpus negative.
 */
internal fun corpusGatePasses(state: MeetingConductState): Boolean = state.closingCorpus >= 0L

/** Builds the ordered submit payload from the accumulated wizard state (`data-flow.yaml` priority 1..6). */
internal fun MeetingConductState.toSubmissionRequest(): MeetingSubmissionRequest {
    val attendanceSubs = groupMembers.mapNotNull { member ->
        val status = attendanceMap[member.memberId] ?: return@mapNotNull null
        val fine = when (status) {
            AttendanceStatus.LATE -> LATE_FINE
            AttendanceStatus.ABSENT -> ABSENT_FINE
            AttendanceStatus.PRESENT -> 0L
        }
        AttendanceSubmission(memberId = member.memberId, status = status, fineAmount = fine)
    }
    val savingsSubs = groupMembers.flatMap { member ->
        val entry = savingsMap[member.memberId] ?: return@flatMap emptyList<SavingsSubmission>()
        val savingsId = member.savingsAccountId ?: return@flatMap emptyList<SavingsSubmission>()
        buildList {
            if (entry.groupAmount > 0L) add(SavingsSubmission(savingsId, member.memberId, entry.groupAmount, SavingsType.GROUP_LINKED))
            if (entry.individualAmount > 0L) add(SavingsSubmission(savingsId, member.memberId, entry.individualAmount, SavingsType.INDIVIDUAL))
        }
    }
    val repaymentSubs = loanRepayments.filter { it.value > 0L }.map { RepaymentSubmission(loanId = it.key, amount = it.value) }
    val disbursalSubs = pendingLoanApplications
        .filter { it.id in approvedApplicationIds }
        .map { DisbursalSubmission(loanId = it.id, amount = it.requestedAmount) }

    // Capture the real meeting-completed timestamp at submit (works offline too — it is the wall-clock
    // moment the operator conducted the meeting, not a scheduled default). actualDate is set from it
    // as well, replacing the prior-meeting-date hack. kotlinx-datetime local time.
    val completedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val completedDate = completedAt.date.toString() // ISO yyyy-MM-dd
    val completedClock = "${completedAt.hour.toString().padStart(2, '0')}:${completedAt.minute.toString().padStart(2, '0')}"

    return MeetingSubmissionRequest(
        meetingId = meetingId,
        meetingNumber = meetingNumber,
        groupId = groupId,
        actualDate = completedDate,
        completedTime = completedClock,
        openingCorpus = openingCorpus,
        closingCorpus = closingCorpus,
        totalSavingsCollected = runningSavingsTotal,
        totalRepaymentsReceived = totalRepayments,
        totalLoansDisbursed = totalLoansDisbursed,
        totalFinesCollected = totalFinesCollected,
        attendanceCount = attendanceMap.count { it.value != AttendanceStatus.ABSENT },
        attendance = attendanceSubs,
        savings = savingsSubs,
        repayments = repaymentSubs,
        disbursals = disbursalSubs,
    )
}

/**
 * Maps the transport-level [NetworkError] onto a `strings.xml` message-key. Used for the on-mount
 * load-error surface (`data-flow.yaml#error_paths`). 401 → auth, offline/timeout → offline, else server.
 */
internal fun NetworkError.toMessageKey(): String = when (this) {
    NetworkError.UNAUTHORIZED, NetworkError.TOO_MANY_REQUESTS -> "error_auth"
    NetworkError.REQUEST_TIMEOUT -> "error_offline"
    NetworkError.NOT_FOUND, NetworkError.BAD_REQUEST, NetworkError.SERVER, NetworkError.SERIALIZATION, NetworkError.UNKNOWN -> "error_server"
}
