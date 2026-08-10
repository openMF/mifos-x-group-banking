/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.meetingsummary

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kpt.core.analytics.KptAnalyticsTracker
import kpt.core.base.observability.CrashReporter
import kpt.core.base.observability.CrashSeverity
import kpt.core.base.security.SessionManager
import kpt.core.base.store.error.ErrorCategory
import kpt.core.base.store.error.categorize
import kpt.core.base.store.screen.ScreenDataStream
import kpt.core.base.store.screen.ScreenState
import kpt.core.base.ui.viewmodel.BaseViewModel
import org.mifos.groupbanking.core.data.repository.MeetingSummaryRepository
import org.mifos.groupbanking.core.model.MeetingSummaryData

private const val TAG = "MeetingSummaryViewModel"

/**
 * Screen-level render state for `meeting-summary-screen` — verbatim mirror of
 * `ui.yaml#state_model.MeetingSummaryViewModel.screen_state.members` (Loading / Content / Error — a
 * single composite meeting record is never "empty" once present, same class of read as
 * `loan-detail`). Derived (not stored) from [MeetingSummaryState] via the [MeetingSummaryState
 * .screenState] extension below — exactly one source of truth, mirroring `LoanDetailState`'s identical
 * convention. See API.md#state.
 */
@Serializable
sealed interface MeetingSummaryScreenState {
    @Serializable
    data object Loading : MeetingSummaryScreenState

    @Serializable
    data object Content : MeetingSummaryScreenState

    @Serializable
    data object Error : MeetingSummaryScreenState
}

/**
 * Error taxonomy for the meeting-summary composite read — mirrors `ui.yaml#state_model.errors` +
 * `data-flow.yaml#error_paths` (404 → cache fallback, 5xx → show_error, offline → cache fallback).
 * [messageKey] is a composeResources string-resource id (never a raw hardcoded English string, per
 * RULE-IMPL-NO-HARDCODED-STRING-001) resolved by the Screen layer. See API.md#state.
 */
@Serializable
sealed interface MeetingSummaryError {
    val retry: Boolean
    val messageKey: String

    @Serializable
    data object Network : MeetingSummaryError {
        override val retry: Boolean = true
        override val messageKey: String = "error_network"
    }

    @Serializable
    data object Server : MeetingSummaryError {
        override val retry: Boolean = true
        override val messageKey: String = "error_server"
    }

    @Serializable
    data object NotFound : MeetingSummaryError {
        override val retry: Boolean = false
        override val messageKey: String = "error_not_found"
    }

    @Serializable
    data object Auth : MeetingSummaryError {
        override val retry: Boolean = false
        override val messageKey: String = "error_auth"
    }
}

/**
 * MVI state for `MeetingSummaryViewModel`. Field set + defaults mirror
 * `ui.yaml#state_model.MeetingSummaryViewModel.state`. [meetingSummary] and [error] are `@Transient`
 * — both are always re-derived from [MeetingSummaryRepository.meetingSummaryStream] on
 * (re)subscription (offline-first cache via the `meeting_record_cache` Room table, so nothing is
 * visually lost across process death — the Store, not this transient render state, is the durable
 * source). [isSharing] toggles while the share report is being composed (`ShareMeetingReport`
 * action). See API.md#state.
 */
@Serializable
@Immutable
data class MeetingSummaryState(
    val isLoading: Boolean = true,
    @Transient
    val meetingSummary: MeetingSummaryData? = null,
    @Transient
    val error: MeetingSummaryError? = null,
    val isSharing: Boolean = false,
    val meetingId: String = "",
    val meetingNumber: Int = 0,
    val groupId: Int = 0,
)

/** Derived, single-source-of-truth screen state — see [MeetingSummaryScreenState] KDoc. */
val MeetingSummaryState.screenState: MeetingSummaryScreenState
    get() = when {
        error != null -> MeetingSummaryScreenState.Error
        isLoading -> MeetingSummaryScreenState.Loading
        else -> MeetingSummaryScreenState.Content
    }

/**
 * One-shot side effects emitted by `MeetingSummaryViewModel` — verbatim mirror of
 * `ui.yaml#state_model.MeetingSummaryViewModel.events.members`. [ShareSummary] carries the composed
 * plain-text meeting report; the Screen container copies it to the system clipboard (the shipped,
 * cross-platform real behavior; the ultimate `kmpToolkit ShareSheet` OS-share seam is a flagged
 * follow-up — no share infra exists in-tree yet). See API.md#events.
 */
sealed interface MeetingSummaryEvent {
    data object NavigateToCalendar : MeetingSummaryEvent
    data class ShareSummary(val reportText: String) : MeetingSummaryEvent
}

/**
 * User intents dispatched to `MeetingSummaryViewModel`. The top-level members mirror
 * `ui.yaml#state_model.MeetingSummaryViewModel.actions.members` (LoadSummary is driven from [init]
 * on composition, not a user tap) plus [Retry] for the error state — RULE-IMPL-DEAD-CLICKABLE-001
 * Rule 1. [Internal] is the sanctioned async-result-routing sub-interface (never a user intent),
 * mirroring `LoanDetailAction.Internal`. See API.md#actions.
 */
sealed interface MeetingSummaryAction {
    data object ShareMeetingReport : MeetingSummaryAction
    data object NavigateDone : MeetingSummaryAction
    data object Retry : MeetingSummaryAction

    /** Async stream emissions — routed via `trySendAction`, never dispatched by the UI. */
    sealed interface Internal : MeetingSummaryAction {
        data class StreamUpdated(val screenState: ScreenState<MeetingSummaryData>) : Internal
    }
}

/**
 * MVI processor for the read-only meeting-summary screen (`business_logic.kind: crud` per ui.yaml —
 * a single-key composite read via [MeetingSummaryRepository.meetingSummaryStream]'s offline-first
 * [ScreenDataStream]). [crashReporter] and [analytics] are wired for real feature-level
 * observability, mirroring `LoanDetailViewModel`'s crud-but-observed precedent.
 *
 * [sessionManager] handles the `ScreenState.Unauthenticated -> sessionManager.endSession()` branch
 * in [handleStreamUpdated] — mirrors `LoanDetailViewModel`'s identical `SessionManager` wiring.
 * `NetworkMonitor` is not re-injected here — it is already composed inside the store's
 * `asScreenStream(...)` wiring (via `MeetingSummaryRepositoryImpl`).
 *
 * [groupId] + [meetingNumber] are the `ui.yaml#nav_params` values forwarded from the entry point
 * (meeting-conduct wizard post-submit, or a meeting-calendar completed-meeting deep-link) via Koin
 * `parametersOf(groupId, meetingNumber, meetingId)`; they scope the single-key
 * [MeetingSummaryRepository.meetingSummaryStream] read. [meetingId] is carried for display/analytics.
 *
 * **In-memory hand-off gap (flagged, not invented around):** `data-flow.yaml` declares a preferred
 * "render from the in-memory meeting-conduct wizard state" path, but `ui.yaml#nav_params` only
 * carries the scalar `meeting_id`/`meeting_number`/`group_id` — NOT a full `MeetingRecordDetail`,
 * which type-safe Compose navigation cannot pass. So this ViewModel always resolves the record
 * through the offline-first Store5 read (which serves the just-written cache instantly when the
 * wizard has persisted it, achieving the same "no visible spinner" effect). Reported to the caller
 * as an idea-layer follow-up (add the record to nav args or a shared in-memory hand-off holder).
 *
 * See API.md#viewmodel.
 */
internal class MeetingSummaryViewModel(
    private val repository: MeetingSummaryRepository,
    private val sessionManager: SessionManager,
    private val crashReporter: CrashReporter,
    private val analytics: KptAnalyticsTracker,
    private val groupId: Int,
    private val meetingNumber: Int,
    private val meetingId: String,
) : BaseViewModel<MeetingSummaryState, MeetingSummaryEvent, MeetingSummaryAction>(
    initialState = MeetingSummaryState(
        meetingId = meetingId,
        meetingNumber = meetingNumber,
        groupId = groupId,
    ),
) {

    /** Fixed-key offline-first stream for (groupId, meetingNumber) — see class KDoc. */
    private val summaryStream: ScreenDataStream<MeetingSummaryData> =
        repository.meetingSummaryStream(
            groupId = groupId,
            meetingNumber = meetingNumber,
            scope = viewModelScope,
        )

    init {
        crashReporter.recordMessage(
            message = "feature=meeting-summary screen=meeting-summary-screen groupId=$groupId meetingNumber=$meetingNumber",
            level = CrashSeverity.Debug,
        )
        analytics.trackSync(syncType = "meeting_summary_view")
        viewModelScope.launch {
            summaryStream.state.collect { screenState ->
                trySendAction(MeetingSummaryAction.Internal.StreamUpdated(screenState))
            }
        }
    }

    override fun handleAction(action: MeetingSummaryAction) {
        when (action) {
            MeetingSummaryAction.ShareMeetingReport -> handleShare()
            MeetingSummaryAction.NavigateDone -> handleNavigateDone()
            MeetingSummaryAction.Retry -> handleRetry()
            is MeetingSummaryAction.Internal.StreamUpdated -> handleStreamUpdated(action.screenState)
        }
    }

    // -- Share meeting report (ui.yaml action_contract effect: share_external) ----------------------
    // Composes a plain-text report from the loaded MeetingSummaryData and emits it for the Screen to
    // copy to the clipboard. No cash-in / cash-out; nothing retained after dismissal.

    private fun handleShare() {
        val summary = state.meetingSummary
        if (summary == null) {
            Logger.i(TAG) { "share tapped with no loaded summary — ignoring" }
            return
        }
        analytics.trackSync(syncType = "meeting_summary_share")
        Logger.i(TAG) { "ShareMeetingReport tapped meetingNumber=${summary.meetingNumber}" }
        updateState { copy(isSharing = true) }
        val report = buildMeetingReport(summary)
        sendEvent(MeetingSummaryEvent.ShareSummary(reportText = report))
        updateState { copy(isSharing = false) }
    }

    // -- Done / back navigation (ui.yaml done_button + top_bar nav, effect: navigate) ---------------

    private fun handleNavigateDone() {
        Logger.i(TAG) { "NavigateDone tapped meetingNumber=$meetingNumber" }
        sendEvent(MeetingSummaryEvent.NavigateToCalendar)
    }

    // -- Error-state retry --------------------------------------------------------------------------

    private fun handleRetry() {
        Logger.i(TAG) { "retry tapped — re-dispatching meeting-summary fetch meetingNumber=$meetingNumber" }
        updateState { copy(error = null, isLoading = true) }
        summaryStream.retry()
    }

    // -- Stream -> State mapping --------------------------------------------------------------------

    private fun handleStreamUpdated(screenState: ScreenState<MeetingSummaryData>) {
        when (screenState) {
            is ScreenState.Loading -> updateState { copy(isLoading = true, error = null) }

            is ScreenState.Empty -> updateState {
                // Never actually emitted — the store always resolves a single composite record or
                // fails; kept for ScreenState exhaustiveness only (LoanDetailViewModel precedent).
                copy(isLoading = false, error = null)
            }

            is ScreenState.Content -> updateState {
                copy(isLoading = false, meetingSummary = screenState.data, error = null)
            }

            is ScreenState.NoNetwork -> updateState {
                copy(isLoading = false, error = MeetingSummaryError.Network)
            }

            is ScreenState.Unauthenticated -> {
                crashReporter.recordMessage(
                    message = "meeting-summary: session expired (401) meetingNumber=$meetingNumber — clearing session",
                    level = CrashSeverity.Warning,
                )
                sessionManager.endSession()
                updateState { copy(isLoading = false, error = MeetingSummaryError.Auth) }
            }

            is ScreenState.Error -> {
                val throwable = screenState.error
                crashReporter.recordException(
                    throwable = throwable,
                    message = "meeting-summary: stream error meetingNumber=$meetingNumber isNetworkError=${screenState.isNetworkError}",
                )
                val category = categorize(throwable)
                val mapped = when {
                    screenState.isNetworkError -> MeetingSummaryError.Network
                    category is ErrorCategory.ClientError && category.httpCode == 404 -> MeetingSummaryError.NotFound
                    else -> MeetingSummaryError.Server
                }
                updateState { copy(isLoading = false, error = mapped) }
            }
        }
    }

    private companion object {
        /**
         * Composes the plain-text meeting report shared via [MeetingSummaryEvent.ShareSummary].
         * Pure function of the loaded [MeetingSummaryData] — every persisted total plus the per-member
         * savings breakdown, so the shared copy is a faithful reconciliation record.
         */
        fun buildMeetingReport(s: MeetingSummaryData): String = buildString {
            appendLine("Meeting #${s.meetingNumber} Summary — ${s.actualDate}")
            appendLine("Attendance: ${s.attendanceCount}/${s.totalMemberCount}")
            appendLine("Total Collected: KES ${s.totalSavingsCollected}")
            appendLine("  Group Savings: KES ${s.groupSavingsCollected}")
            appendLine("  Individual Savings: KES ${s.individualSavingsCollected}")
            appendLine("Loan Repayments: KES ${s.loansRepaid}")
            appendLine("Loans Disbursed: KES ${s.loansDisbursed}")
            appendLine("Fines Collected: KES ${s.finesCollected}")
            appendLine("Opening Corpus: KES ${s.openingCorpus}")
            appendLine("Closing Corpus: KES ${s.closingCorpus}")
            appendLine("Net Change: KES ${s.netCorpusChange}")
            if (s.savingsBreakdown.isNotEmpty()) {
                appendLine("Savings Breakdown:")
                s.savingsBreakdown.forEach { row ->
                    appendLine("  ${row.memberName}: KES ${row.totalSavings} (Group KES ${row.groupSavings} · Individual KES ${row.individualSavings})")
                }
            }
        }.trimEnd()
    }
}
