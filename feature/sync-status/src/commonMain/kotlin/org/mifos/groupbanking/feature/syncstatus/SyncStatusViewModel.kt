/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
@file:OptIn(ExperimentalTime::class)

package org.mifos.groupbanking.feature.syncstatus

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitor
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kpt.core.analytics.KptAnalyticsTracker
import kpt.core.base.observability.CrashReporter
import kpt.core.base.observability.CrashSeverity
import kpt.core.base.ui.viewmodel.BaseViewModel
import org.mifos.groupbanking.core.data.repository.SyncManager
import org.mifos.groupbanking.core.data.repository.SyncQueueRepository
import org.mifos.groupbanking.core.model.EntityType
import org.mifos.groupbanking.core.model.SyncOverallStatus
import org.mifos.groupbanking.core.model.SyncQueueCounts
import org.mifos.groupbanking.core.model.SyncQueueItem
import org.mifos.groupbanking.core.model.SyncResult
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

// MVI stack (State/Event/Action/ViewModel/DI) for the `sync-status` feature — see
// API.md#viewmodel / #state / #actions / #events / #di for the full generated-symbol contract.
private const val TAG = "SyncStatusViewModel"

/**
 * Screen-level render state for `sync-status-screen` — verbatim mirror of
 * `ui.yaml#state_model.SyncStatusViewModel.screen_state.members`. Derived only (not stored) via
 * [SyncStatusState.deriveScreenState] — same convention as `MemberAddState`/`GroupDashboardState`
 * (keeps [SyncStatusState.error]/[SyncStatusState.isLoading] the single source of truth instead of
 * a fourth, independently-mutable flag). See API.md#state.
 */
@Serializable
sealed interface SyncStatusScreenState {
    @Serializable
    data object Loading : SyncStatusScreenState

    @Serializable
    data object Content : SyncStatusScreenState

    @Serializable
    data object Error : SyncStatusScreenState
}

/**
 * Error taxonomy for the sync-status screen — verbatim mirror of
 * `ui.yaml#state_model.SyncStatusViewModel.errors.types`. [messageKey] is a composeResources
 * string-resource id (never a raw hardcoded English string, per
 * RULE-IMPL-NO-HARDCODED-STRING-001) resolved by the Screen layer.
 *
 * See API.md#state.
 */
@Serializable
sealed interface SyncStatusError {
    val retry: Boolean
    val messageKey: String

    /** Reading the local `sync_queue` table (the reactive `combine()` fan-in) failed. */
    @Serializable
    data object DbRead : SyncStatusError {
        override val retry: Boolean = true
        override val messageKey: String = "error_db"
    }

    /**
     * A `SyncManager.triggerSync()`/`retryItem()` drain failed transport-level (thrown from the
     * `Flow`, or a row-level `SyncResult.failedCount > 0`). Kept distinct from [DbRead] — a drain
     * failure never blocks the (still-successful) local read that feeds the dashboard, so it is
     * surfaced as a transient snackbar/inline flag rather than forcing [SyncStatusScreenState.Error].
     */
    @Serializable
    data object SyncFailed : SyncStatusError {
        override val retry: Boolean = true
        override val messageKey: String = "error_sync_failed"
    }
}

/**
 * MVI state for `SyncStatusViewModel`. Field set + defaults are a verbatim mirror of
 * `ui.yaml#state_model.SyncStatusViewModel.state`, with one flagged addition: [error].
 * `ui.yaml`'s `state.fields` list does not declare an `error` field, yet
 * `state_model.screen_state.members` DOES declare an `Error` member with no other way to reach it
 * — same documented-gap class as `MemberAddState`/`GroupDashboardState`'s own `error` field (both
 * added for the identical reason). Reported to the caller for an idea-layer
 * `ui.yaml#state_model.state.fields` update, RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1.
 *
 * [lastSyncAt], [pendingByType], [failedOperations], and [overallStatus] are `@Transient` —
 * non-serializable/domain-derived values always re-supplied by the live `combine()` subscription
 * (`SyncQueueRepository`) and the separate `SyncManager.getLastSyncAt()` collection on
 * (re)subscription; nothing here needs to survive process death on its own (mirrors
 * `GroupDashboardState`'s identical `@Transient` convention for repository-fed domain payloads).
 * [error] is likewise `@Transient` — a screen-level render concern, same convention as
 * `MemberAddState.error`.
 *
 * See API.md#state.
 */
@Serializable
@Immutable
data class SyncStatusState(
    val isOnline: Boolean = false,
    val pendingCount: Int = 0,
    val failedCount: Int = 0,
    @Transient
    val lastSyncAt: Instant? = null,
    val isSyncing: Boolean = false,
    @Transient
    val pendingByType: Map<EntityType, Int> = emptyMap(),
    @Transient
    val failedOperations: List<SyncQueueItem> = emptyList(),
    val conflictCount: Int = 0,
    @Transient
    val overallStatus: SyncOverallStatus = SyncOverallStatus.SYNCED,
    val isLoading: Boolean = true,
    @Transient
    val error: SyncStatusError? = null,
)

/**
 * Derives [SyncStatusScreenState] from [SyncStatusState] — see the type's KDoc for why this is a
 * pure function rather than a stored field.
 */
fun SyncStatusState.deriveScreenState(): SyncStatusScreenState = when {
    error != null -> SyncStatusScreenState.Error
    isLoading -> SyncStatusScreenState.Loading
    else -> SyncStatusScreenState.Content
}

/**
 * `ui.yaml#business_logic` rollup formula, client-computed from [SyncQueueCounts] (no wire source
 * produces [SyncOverallStatus] directly — see [org.mifos.groupbanking.core.model.SyncOverallStatus]
 * KDoc). ANY failed row wins over ANY pending row; an empty/fully-synced queue is [SyncOverallStatus.SYNCED].
 * Exposed as a top-level `internal` function (rather than inlined) so the derivation is
 * independently unit-testable, mirrors `GroupDashboardViewModel.isCorpusInsufficient`'s identical
 * top-level-pure-function convention.
 */
internal fun deriveOverallStatus(failedCount: Int, pendingCount: Int): SyncOverallStatus = when {
    failedCount > 0 -> SyncOverallStatus.FAILED
    pendingCount > 0 -> SyncOverallStatus.PENDING
    else -> SyncOverallStatus.SYNCED
}

/**
 * One-shot side effects emitted by `SyncStatusViewModel` — verbatim mirror of
 * `ui.yaml#state_model.SyncStatusViewModel.events.members`. See API.md#events.
 */
sealed interface SyncStatusEvent {
    data class ShowSnackbar(val message: String) : SyncStatusEvent
    data object SyncCompleted : SyncStatusEvent
}

/**
 * User intents dispatched to `SyncStatusViewModel`. The 3 top-level members are a verbatim mirror
 * of `ui.yaml#state_model.SyncStatusViewModel.actions.members` — RULE-IMPL-DEAD-CLICKABLE-001
 * Rule 1. [Internal] is the sanctioned async-result-routing sub-interface (never a user intent)
 * per `training-layer/TRAINING_MASTER.yaml#patterns.actions`. See API.md#actions.
 */
sealed interface SyncStatusAction {
    data object OnSyncNow : SyncStatusAction
    data class OnRetryOperation(val itemId: Long) : SyncStatusAction
    data object OnRefresh : SyncStatusAction

    /** Async stream/coroutine-result emissions — routed via `trySendAction`, never dispatched by the UI. */
    sealed interface Internal : SyncStatusAction {
        data class CombinedDataLoaded(val data: CombinedSyncData) : Internal
        data class LastSyncLoaded(val lastSyncAt: Instant?) : Internal
        data class SyncResultReceived(val result: SyncResult) : Internal
        data class RetryResultReceived(val result: SyncResult) : Internal
        data class DataError(val throwable: Throwable) : Internal
    }
}

/**
 * The 5-way fan-in payload of the reactive `combine()` in [SyncStatusViewModel.init] —
 * `data-flow.yaml#on_mount.local_sources`: `SyncQueueRepository.observeCounts` /
 * `observePendingByType` / `observeFailed` / `observeConflictCount`, plus `NetworkMonitor.isOnline`.
 * `SyncManager.getLastSyncAt()` is intentionally NOT folded into this combine — it is its own
 * independent, infrequently-changing stream (only advances once per completed drain), collected via
 * a separate `viewModelScope.launch` (see [SyncStatusAction.Internal.LastSyncLoaded]) rather than
 * forcing every `combine()` re-emission to also carry it.
 */
data class CombinedSyncData(
    val counts: SyncQueueCounts,
    val pendingByType: Map<EntityType, Int>,
    val failedOperations: List<SyncQueueItem>,
    val conflictCount: Int,
    val isOnline: Boolean,
)

/**
 * MVI processor for the read-only sync dashboard (`business_logic.kind: processor` per ui.yaml —
 * drains the local offline write-queue to Fineract on demand, so the SP-04 AC-7
 * analytics/crashReporter injection pair applies, per RULE-IDEA-IMPL-INTELLIGENCE-001 AC-03i).
 * `FieldEncryptor` (`core-base/security`) is intentionally NOT injected — `data-flow.yaml`
 * declares no `pii_columns` entry for this screen (every local source is queue metadata / counts,
 * never member PII).
 *
 * **Not a Store5 read-store** — mirrors [SyncQueueRepository] / [SyncManager] KDocs: every entry
 * this screen reads is a direct `Flow` off Room (`SyncQueueRepository`) or a direct network
 * submission coordinator (`SyncManager`), never a cached `ScreenDataStream`/`Store` projection. The
 * five reactive sources are fanned into ONE [SyncStatusState] via a single `kotlinx.coroutines.flow.combine`
 * (5-arg overload — exactly at Kotlin's native combine arity ceiling, no nested combine needed).
 *
 * [analytics] uses the already-shipped [KptAnalyticsTracker.trackSync] (`syncType`/`itemCount`/
 * `duration`/`success`/`errorMessage` params) rather than an invented generic `track()` method — no
 * `setCustomKey`/`track()`/`log()` surface exists on the real [KptAnalyticsTracker]/[CrashReporter]
 * (same documented drift-avoidance note as `PersonalDashboardViewModel`'s class KDoc).
 *
 * **`OnRefresh` — documented gap, not a stub:** `data-flow.yaml#on_refresh.local_writes` names a
 * `SyncQueueRepository.refresh_queue` write target, but [SyncQueueRepository]'s shipped interface
 * declares no `refresh()`/`invalidate()` method — every read is already a live Room `Flow` that
 * re-emits automatically on any queue write, so there is nothing to force-refetch. [handleRefresh]
 * pulses [SyncStatusState.isLoading] true→false (real state mutation + a crash-reporter breadcrumb)
 * to complete the pull-to-refresh gesture honestly, rather than silently doing nothing. Flagged for
 * the caller (RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1) in case a real invalidate hook is added
 * later.
 *
 * **Sync-success snackbar — documented i18n gap:** `ui.yaml#i18n.en` declares `error_sync_failed`
 * for failure but no success-copy key (e.g. `sync_success`); on a fully-successful
 * [SyncManager.triggerSync] drain [handleSyncResultReceived] therefore emits only
 * [SyncStatusEvent.SyncCompleted] (the overall-status card already flips to `SYNCED` from the live
 * `combine()` re-emission) and does NOT fabricate an undeclared English string for a
 * [SyncStatusEvent.ShowSnackbar], per RULE-IMPL-NO-HARDCODED-STRING-001. Flagged for an idea-layer
 * `ui.yaml#i18n` addition.
 *
 * See API.md#viewmodel.
 */
internal class SyncStatusViewModel(
    private val repository: SyncQueueRepository,
    private val syncManager: SyncManager,
    private val networkMonitor: NetworkMonitor,
    private val analytics: KptAnalyticsTracker,
    private val crashReporter: CrashReporter,
) : BaseViewModel<SyncStatusState, SyncStatusEvent, SyncStatusAction>(
    initialState = SyncStatusState(),
) {

    init {
        crashReporter.recordMessage(
            message = "feature=sync-status screen=sync-status-screen",
            level = CrashSeverity.Debug,
        )

        viewModelScope.launch {
            combine(
                repository.observeCounts(),
                repository.observePendingByType(),
                repository.observeFailed(),
                repository.observeConflictCount(),
                networkMonitor.isOnline,
            ) { counts, pendingByType, failedOperations, conflictCount, isOnline ->
                CombinedSyncData(
                    counts = counts,
                    pendingByType = pendingByType,
                    failedOperations = failedOperations,
                    conflictCount = conflictCount,
                    isOnline = isOnline,
                )
            }
                .catch { throwable -> trySendAction(SyncStatusAction.Internal.DataError(throwable)) }
                .collect { data -> trySendAction(SyncStatusAction.Internal.CombinedDataLoaded(data)) }
        }

        viewModelScope.launch {
            syncManager.getLastSyncAt()
                .catch { throwable -> trySendAction(SyncStatusAction.Internal.DataError(throwable)) }
                .collect { lastSyncAt -> trySendAction(SyncStatusAction.Internal.LastSyncLoaded(lastSyncAt)) }
        }
    }

    override fun handleAction(action: SyncStatusAction) {
        when (action) {
            SyncStatusAction.OnSyncNow -> handleSyncNow()
            is SyncStatusAction.OnRetryOperation -> handleRetryOperation(action.itemId)
            SyncStatusAction.OnRefresh -> handleRefresh()
            is SyncStatusAction.Internal.CombinedDataLoaded -> handleCombinedDataLoaded(action.data)
            is SyncStatusAction.Internal.LastSyncLoaded -> handleLastSyncLoaded(action.lastSyncAt)
            is SyncStatusAction.Internal.SyncResultReceived -> handleSyncResultReceived(action.result)
            is SyncStatusAction.Internal.RetryResultReceived -> handleRetryResultReceived(action.result)
            is SyncStatusAction.Internal.DataError -> handleDataError(action.throwable)
        }
    }

    // -- Reactive fan-in (ui.yaml effect: none / on_mount) ----------------------------------------

    private fun handleCombinedDataLoaded(data: CombinedSyncData) {
        updateState {
            copy(
                isOnline = data.isOnline,
                pendingCount = data.counts.pending,
                failedCount = data.counts.failed,
                pendingByType = data.pendingByType,
                failedOperations = data.failedOperations,
                conflictCount = data.conflictCount,
                overallStatus = deriveOverallStatus(failedCount = data.counts.failed, pendingCount = data.counts.pending),
                isLoading = false,
                error = null,
            )
        }
    }

    private fun handleLastSyncLoaded(lastSyncAt: Instant?) {
        updateState { copy(lastSyncAt = lastSyncAt) }
    }

    private fun handleDataError(throwable: Throwable) {
        crashReporter.recordException(throwable, message = "sync-status: local sync-queue read failed")
        Logger.e(TAG) { "local sync-queue read failed: ${throwable.message}" }
        updateState { copy(isLoading = false, error = SyncStatusError.DbRead) }
    }

    // -- OnSyncNow (ui.yaml effect: call_api) -------------------------------------------------------

    private fun handleSyncNow() {
        if (!state.isOnline || state.isSyncing) {
            Logger.w(TAG) { "OnSyncNow ignored isOnline=${state.isOnline} isSyncing=${state.isSyncing}" }
            return
        }
        val pendingAtStart = state.pendingCount
        updateState { copy(isSyncing = true, error = null) }
        analytics.trackSync(syncType = "full", itemCount = pendingAtStart)

        viewModelScope.launch {
            syncManager.triggerSync()
                .catch { throwable ->
                    crashReporter.recordException(throwable, message = "sync-status: triggerSync stream failed")
                    Logger.e(TAG) { "triggerSync stream failed: ${throwable.message}" }
                    analytics.trackSync(
                        syncType = "full",
                        itemCount = pendingAtStart,
                        success = false,
                        errorMessage = throwable.message,
                    )
                    updateState { copy(isSyncing = false, error = SyncStatusError.SyncFailed) }
                    sendEvent(SyncStatusEvent.ShowSnackbar(message = SyncStatusError.SyncFailed.messageKey))
                }
                .collect { result -> trySendAction(SyncStatusAction.Internal.SyncResultReceived(result)) }
        }
    }

    private fun handleSyncResultReceived(result: SyncResult) {
        updateState { copy(isSyncing = false) }
        if (result.failedCount > 0) {
            analytics.trackSync(
                syncType = "full",
                itemCount = result.successCount + result.failedCount,
                success = false,
            )
            Logger.w(TAG) {
                "triggerSync completed with failures success=${result.successCount} failed=${result.failedCount}"
            }
            // See class KDoc "Sync-success snackbar" — the failure copy IS declared (error_sync_failed);
            // only the success copy is the documented i18n gap.
            sendEvent(SyncStatusEvent.ShowSnackbar(message = SyncStatusError.SyncFailed.messageKey))
        } else {
            analytics.trackSync(syncType = "full", itemCount = result.successCount, success = true)
            Logger.i(TAG) { "triggerSync completed successfully count=${result.successCount}" }
            sendEvent(SyncStatusEvent.SyncCompleted)
        }
    }

    // -- OnRetryOperation (ui.yaml effect: call_api) ------------------------------------------------

    private fun handleRetryOperation(itemId: Long) {
        if (!state.isOnline) {
            Logger.w(TAG) { "OnRetryOperation ignored — offline itemId=$itemId" }
            sendEvent(SyncStatusEvent.ShowSnackbar(message = SyncStatusError.SyncFailed.messageKey))
            return
        }
        analytics.trackSync(syncType = "incremental", itemCount = 1)
        viewModelScope.launch {
            // SyncManager.retryItem never throws (see interface KDoc) — no try-catch boundary
            // needed here, mirrors the "no try-catch in this stack" convention documented on
            // SyncManager/SyncQueueRepository.
            val result = syncManager.retryItem(itemId)
            trySendAction(SyncStatusAction.Internal.RetryResultReceived(result))
        }
    }

    private fun handleRetryResultReceived(result: SyncResult) {
        if (result.failedCount > 0) {
            analytics.trackSync(syncType = "incremental", itemCount = 1, success = false)
            Logger.w(TAG) { "retryItem failed failedCount=${result.failedCount}" }
            sendEvent(SyncStatusEvent.ShowSnackbar(message = SyncStatusError.SyncFailed.messageKey))
        } else {
            analytics.trackSync(syncType = "incremental", itemCount = result.successCount, success = true)
            Logger.i(TAG) { "retryItem succeeded successCount=${result.successCount}" }
            // No dedicated success snackbar — the failed-operations list itself shrinks via the
            // live combine() re-emission once SyncManagerImpl marks the row SYNCED (visible
            // feedback without an undeclared i18n key — see class KDoc "Sync-success snackbar").
        }
    }

    // -- OnRefresh (ui.yaml: "Pull to refresh (re-reads local DB)") ---------------------------------

    private fun handleRefresh() {
        // See class KDoc "OnRefresh — documented gap" for why this is a pulse, not a repository call.
        crashReporter.recordMessage(
            message = "sync-status: OnRefresh — no repository refresh() API, pulsing isLoading only",
            level = CrashSeverity.Debug,
        )
        Logger.i(TAG) { "OnRefresh — combine() flows are already live; pulsing isLoading for gesture feedback" }
        updateState { copy(isLoading = true) }
        viewModelScope.launch {
            // The subscribed combine() flow's latest emission is already current — this only
            // completes the pull-to-refresh gesture, it does not (and cannot, per the documented
            // gap) force a distinct re-fetch.
            updateState { copy(isLoading = false) }
        }
    }
}
