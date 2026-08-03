/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.previousmeetingreview

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
import kpt.core.base.store.screen.ScreenState
import kpt.core.base.ui.viewmodel.BaseViewModel
import org.mifos.groupbanking.core.data.repository.PreviousMeetingReviewRepository
import org.mifos.groupbanking.core.model.PreviousMeetingDetail
import org.mifos.groupbanking.core.model.UnresolvedItem

private const val TAG = "PreviousMeetingReviewViewModel"
private const val LAUNCHED_FROM_CONDUCT = "conduct"

/**
 * Screen-level render state for `previous-meeting-review-screen` — verbatim mirror of
 * `ui.yaml#state_model.PreviousMeetingReviewViewModel.screen_state.members` (Loading / Content /
 * Error). Derived (not stored) from [PreviousMeetingReviewState] via the [screenState] extension
 * below — one source of truth, mirroring `MeetingSummaryState`'s convention. See API.md#state.
 */
@Serializable
sealed interface PreviousMeetingReviewScreenState {
    @Serializable
    data object Loading : PreviousMeetingReviewScreenState

    @Serializable
    data object Content : PreviousMeetingReviewScreenState

    @Serializable
    data object Error : PreviousMeetingReviewScreenState
}

/**
 * Error taxonomy for the previous-meeting composite read — mirrors `data-flow.yaml#error_paths`
 * (401 → login, 404 → not_found, 5xx → cache fallback, offline → cache fallback). [messageKey] is a
 * composeResources string-resource id (never a raw hardcoded English string, per
 * RULE-IMPL-NO-HARDCODED-STRING-001) resolved by the Screen layer. See API.md#state.
 */
@Serializable
sealed interface PreviousMeetingReviewError {
    val retry: Boolean
    val messageKey: String

    @Serializable
    data object Network : PreviousMeetingReviewError {
        override val retry: Boolean = true
        override val messageKey: String = "error_network"
    }

    @Serializable
    data object Server : PreviousMeetingReviewError {
        override val retry: Boolean = true
        override val messageKey: String = "error_server"
    }

    @Serializable
    data object NotFound : PreviousMeetingReviewError {
        override val retry: Boolean = false
        override val messageKey: String = "error_not_found"
    }

    @Serializable
    data object Auth : PreviousMeetingReviewError {
        override val retry: Boolean = false
        override val messageKey: String = "error_auth"
    }
}

/**
 * MVI state for `PreviousMeetingReviewViewModel`. Field set + defaults mirror
 * `ui.yaml#state_model.PreviousMeetingReviewViewModel.state`. [meetingDetail] and [error] are
 * `@Transient` — both are always re-derived from the offline-first
 * [PreviousMeetingReviewRepository.previousMeetingStream] on (re)subscription (the underlying Store5
 * caches survive process death). [unresolvedItems] mirrors `meetingDetail.unresolvedItems` for the
 * `unresolved_alert_card` `visible_when` binding. [nextMeetingId] / [nextMeetingNumber] are only
 * populated when [launchedFrom] == `conduct` (drives the Start-Meeting CTA nav params). See
 * API.md#state.
 */
@Serializable
@Immutable
data class PreviousMeetingReviewState(
    val isLoading: Boolean = true,
    @Transient
    val meetingDetail: PreviousMeetingDetail? = null,
    @Transient
    val error: PreviousMeetingReviewError? = null,
    val launchedFrom: String = "calendar",
    val meetingId: String = "",
    val meetingNumber: Int = 0,
    val centerId: Int = 0,
    val nextMeetingId: String? = null,
    val nextMeetingNumber: Int? = null,
    @Transient
    val unresolvedItems: List<UnresolvedItem> = emptyList(),
) {
    /** True only for a conduct-launched review — gates the context banner variant + Start-Meeting CTA. */
    val isConductLaunched: Boolean get() = launchedFrom == LAUNCHED_FROM_CONDUCT
}

/** Derived, single-source-of-truth screen state — see [PreviousMeetingReviewScreenState] KDoc. */
val PreviousMeetingReviewState.screenState: PreviousMeetingReviewScreenState
    get() = when {
        error != null -> PreviousMeetingReviewScreenState.Error
        isLoading -> PreviousMeetingReviewScreenState.Loading
        else -> PreviousMeetingReviewScreenState.Content
    }

/**
 * One-shot side effects emitted by `PreviousMeetingReviewViewModel` — mirror of
 * `ui.yaml#state_model.PreviousMeetingReviewViewModel.events.members`. See API.md#events.
 */
sealed interface PreviousMeetingReviewEvent {
    data object NavigateBack : PreviousMeetingReviewEvent
    data class NavigateToConduct(
        val meetingId: String,
        val meetingNumber: Int,
        val centerId: Int,
    ) : PreviousMeetingReviewEvent
}

/**
 * User intents dispatched to `PreviousMeetingReviewViewModel`. Top-level members mirror
 * `ui.yaml#state_model.PreviousMeetingReviewViewModel.actions.members` (LoadPreviousMeeting is driven
 * from [init] on composition, not a user tap) plus [Retry] for the error state
 * (RULE-IMPL-DEAD-CLICKABLE-001 Rule 1). [Internal] is the sanctioned async-result-routing
 * sub-interface (never a user intent). See API.md#actions.
 */
sealed interface PreviousMeetingReviewAction {
    data object NavigateBack : PreviousMeetingReviewAction
    data object StartNewMeeting : PreviousMeetingReviewAction
    data object Retry : PreviousMeetingReviewAction

    /** Async stream emissions — routed via `trySendAction`, never dispatched by the UI. */
    sealed interface Internal : PreviousMeetingReviewAction {
        data class StreamUpdated(val screenState: ScreenState<PreviousMeetingDetail>) : Internal
    }
}

/**
 * MVI processor for the read-only previous-meeting-review screen (FR-019, `business_logic.kind: crud`
 * — a composite offline-first read that REUSES the meeting record store and merges per-member
 * attendance, via [PreviousMeetingReviewRepository.previousMeetingStream]). [crashReporter] and
 * [analytics] are wired for real feature-level observability; [sessionManager] handles the
 * `ScreenState.Unauthenticated -> endSession()` branch (mirrors `MeetingSummaryViewModel`).
 *
 * [centerId] / [meetingNumber] / [meetingId] / [launchedFrom] are the `ui.yaml#nav_params` forwarded
 * from the entry point (meeting-conduct step-0 drill-down, or meeting-calendar completed-meeting card)
 * via Koin `parametersOf(...)`. When [launchedFrom] == `conduct`, the "next meeting" the Start-Meeting
 * CTA opens is derived as [meetingNumber] + 1 (the review of meeting N precedes conducting N+1).
 *
 * **Nav-param gap (flagged, not invented around):** `ui.yaml#nav_params` carries no explicit
 * `next_meeting_id`; the CTA forwards the same [meetingId] center context with the incremented number
 * so meeting-conduct can resolve/assign the concrete id when the meeting starts. Reported to the
 * caller as an idea-layer follow-up (add `next_meeting_id` to the review nav args).
 *
 * See API.md#viewmodel.
 */
internal class PreviousMeetingReviewViewModel(
    private val repository: PreviousMeetingReviewRepository,
    private val sessionManager: SessionManager,
    private val crashReporter: CrashReporter,
    private val analytics: KptAnalyticsTracker,
    private val centerId: Int,
    private val meetingNumber: Int,
    private val meetingId: String,
    private val launchedFrom: String,
) : BaseViewModel<PreviousMeetingReviewState, PreviousMeetingReviewEvent, PreviousMeetingReviewAction>(
    initialState = PreviousMeetingReviewState(
        meetingId = meetingId,
        meetingNumber = meetingNumber,
        centerId = centerId,
        launchedFrom = launchedFrom,
        nextMeetingId = if (launchedFrom == LAUNCHED_FROM_CONDUCT) meetingId else null,
        nextMeetingNumber = if (launchedFrom == LAUNCHED_FROM_CONDUCT) meetingNumber + 1 else null,
    ),
) {

    /** Combined offline-first stream (reused record read + attendance read) — see class KDoc. */
    private val stream = repository.previousMeetingStream(
        centerId = centerId,
        meetingNumber = meetingNumber,
        meetingId = meetingId,
        scope = viewModelScope,
    )

    init {
        crashReporter.recordMessage(
            message = "feature=previous-meeting-review screen=previous-meeting-review-screen " +
                "centerId=$centerId meetingNumber=$meetingNumber launchedFrom=$launchedFrom",
            level = CrashSeverity.Debug,
        )
        analytics.trackSync(syncType = "previous_meeting_review_view")
        viewModelScope.launch {
            stream.state.collect { screenState ->
                trySendAction(PreviousMeetingReviewAction.Internal.StreamUpdated(screenState))
            }
        }
    }

    override fun handleAction(action: PreviousMeetingReviewAction) {
        when (action) {
            PreviousMeetingReviewAction.NavigateBack -> handleNavigateBack()
            PreviousMeetingReviewAction.StartNewMeeting -> handleStartNewMeeting()
            PreviousMeetingReviewAction.Retry -> handleRetry()
            is PreviousMeetingReviewAction.Internal.StreamUpdated -> handleStreamUpdated(action.screenState)
        }
    }

    // -- Back navigation (ui.yaml top_app_bar nav, effect: navigate) --------------------------------

    private fun handleNavigateBack() {
        Logger.i(TAG) { "NavigateBack tapped meetingNumber=$meetingNumber" }
        sendEvent(PreviousMeetingReviewEvent.NavigateBack)
    }

    // -- Start next meeting CTA (ui.yaml start_meeting_cta, effect: navigate; conduct-launched only) --

    private fun handleStartNewMeeting() {
        if (!state.isConductLaunched) {
            Logger.i(TAG) { "StartNewMeeting ignored — launchedFrom=${state.launchedFrom} (not conduct)" }
            return
        }
        val nextId = state.nextMeetingId ?: meetingId
        val nextNumber = state.nextMeetingNumber ?: (meetingNumber + 1)
        analytics.trackSync(syncType = "previous_meeting_review_start_next")
        Logger.i(TAG) { "StartNewMeeting tapped — navigating to conduct nextMeetingNumber=$nextNumber" }
        sendEvent(
            PreviousMeetingReviewEvent.NavigateToConduct(
                meetingId = nextId,
                meetingNumber = nextNumber,
                centerId = centerId,
            ),
        )
    }

    // -- Error-state retry --------------------------------------------------------------------------

    private fun handleRetry() {
        Logger.i(TAG) { "retry tapped — re-dispatching previous-meeting read meetingNumber=$meetingNumber" }
        updateState { copy(error = null, isLoading = true) }
        stream.retry()
    }

    // -- Stream -> State mapping --------------------------------------------------------------------

    private fun handleStreamUpdated(screenState: ScreenState<PreviousMeetingDetail>) {
        when (screenState) {
            is ScreenState.Loading -> updateState { copy(isLoading = true, error = null) }

            is ScreenState.Empty -> updateState {
                // Never actually emitted — the merged stream always resolves a Content or fails; kept
                // for ScreenState exhaustiveness only (MeetingSummaryViewModel precedent).
                copy(isLoading = false, error = null)
            }

            is ScreenState.Content -> updateState {
                copy(
                    isLoading = false,
                    meetingDetail = screenState.data,
                    unresolvedItems = screenState.data.unresolvedItems,
                    error = null,
                )
            }

            is ScreenState.NoNetwork -> updateState {
                copy(isLoading = false, error = PreviousMeetingReviewError.Network)
            }

            is ScreenState.Unauthenticated -> {
                crashReporter.recordMessage(
                    message = "previous-meeting-review: session expired (401) meetingNumber=$meetingNumber — clearing session",
                    level = CrashSeverity.Warning,
                )
                sessionManager.endSession()
                updateState { copy(isLoading = false, error = PreviousMeetingReviewError.Auth) }
            }

            is ScreenState.Error -> {
                val throwable = screenState.error
                crashReporter.recordException(
                    throwable = throwable,
                    message = "previous-meeting-review: stream error meetingNumber=$meetingNumber isNetworkError=${screenState.isNetworkError}",
                )
                val category = categorize(throwable)
                val mapped = when {
                    screenState.isNetworkError -> PreviousMeetingReviewError.Network
                    category is ErrorCategory.ClientError && category.httpCode == 404 -> PreviousMeetingReviewError.NotFound
                    else -> PreviousMeetingReviewError.Server
                }
                updateState { copy(isLoading = false, error = mapped) }
            }
        }
    }
}
