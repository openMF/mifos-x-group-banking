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

package kpt.feature.syncstatus

import androidx.compose.runtime.Composable
import kpt.core.designsystem.theme.KptTheme
import kpt.core.model.EntityType
import kpt.core.model.SyncOverallStatus
import kpt.core.model.SyncQueueItem
import kpt.core.model.SyncStatus
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

// -- ui.yaml#states.*-shaped fixtures (Content demo_data matches ui.yaml#states.content.demo_data verbatim) ---

/** `ui.yaml#states.loading`. */
private val demoLoadingState = SyncStatusState(isLoading = true)

/** `ui.yaml#states.content.demo_data` — PENDING (MEETING: 1, SAVINGS: 2), no conflicts, online. */
private val demoContentPendingState = SyncStatusState(
    isOnline = true,
    pendingCount = 3,
    failedCount = 0,
    lastSyncAt = Instant.parse("2026-05-05T08:30:00Z"),
    isSyncing = false,
    pendingByType = mapOf(EntityType.MEETING to 1, EntityType.SAVINGS to 2),
    failedOperations = emptyList(),
    conflictCount = 0,
    overallStatus = SyncOverallStatus.PENDING,
    isLoading = false,
)

/** All-synced, empty-state visible — `overall_status_card` SYNCED (green). */
private val demoContentSyncedState = demoContentPendingState.copy(
    pendingCount = 0,
    pendingByType = emptyMap(),
    overallStatus = SyncOverallStatus.SYNCED,
)

/** Failed operations present — `overall_status_card` FAILED (red) + `failed_operations_section`. */
private val demoContentFailedState = demoContentPendingState.copy(
    pendingCount = 0,
    pendingByType = emptyMap(),
    failedCount = 2,
    failedOperations = listOf(
        SyncQueueItem(
            id = 1L,
            operationType = "LOAN_REQUEST",
            payloadJson = "{}",
            targetTable = "dt_loan_request",
            status = SyncStatus.FAILED,
            createdAtEpochMs = 0L,
            lastAttemptEpochMs = 0L,
            attemptCount = 2,
            lastError = "Server error. Please try again.",
        ),
        SyncQueueItem(
            id = 2L,
            operationType = "CREATE_MEMBER",
            payloadJson = "{}",
            targetTable = "dt_member",
            status = SyncStatus.FAILED,
            createdAtEpochMs = 0L,
            lastAttemptEpochMs = 0L,
            attemptCount = 1,
            lastError = "No internet. Your request has been saved for later.",
        ),
    ),
    overallStatus = SyncOverallStatus.FAILED,
)

/** Conflicts detected alongside pending operations — `conflict_chip` visible. */
private val demoContentWithConflictState = demoContentPendingState.copy(conflictCount = 2)

/** Offline — `sync_now_button` disabled, offline label. */
private val demoOfflineState = demoContentPendingState.copy(isOnline = false)

/** Syncing in flight — `sync_now_button` spinner replaces the sync icon. */
private val demoSyncingState = demoContentPendingState.copy(isSyncing = true)

/** `ui.yaml#states.error` — local `sync_queue` DB read failed. */
private val demoErrorState = SyncStatusState(isLoading = false, error = SyncStatusError.DbRead)

/**
 * `@Preview` gallery for `SyncStatusScreen.kt`. See API.md#preview. Data source: `ui.yaml#states.*`
 * (the Content variant's `demo_data` block is reproduced verbatim; no `demo-data.yaml` was
 * resolved for this generation pass).
 */
private class SyncStatusScreenPreviewProvider : PreviewParameterProvider<SyncStatusState> {
    override val values: Sequence<SyncStatusState> = sequenceOf(
        demoLoadingState,
        demoContentPendingState,
        demoContentSyncedState,
        demoContentFailedState,
        demoContentWithConflictState,
        demoOfflineState,
        demoSyncingState,
        demoErrorState,
    )
}

@Preview
@Composable
private fun SyncStatusContentPreview(
    @PreviewParameter(SyncStatusScreenPreviewProvider::class)
    state: SyncStatusState,
) {
    KptTheme {
        SyncStatusContent(state = state, onAction = {})
    }
}

@Preview
@Composable
private fun SyncStatusContentBodySyncedPreview() {
    KptTheme {
        SyncStatusContentBody(state = demoContentSyncedState, onAction = {})
    }
}

@Preview
@Composable
private fun SyncStatusContentBodyPendingPreview() {
    KptTheme {
        SyncStatusContentBody(state = demoContentPendingState, onAction = {})
    }
}

@Preview
@Composable
private fun SyncStatusContentBodyFailedPreview() {
    KptTheme {
        SyncStatusContentBody(state = demoContentFailedState, onAction = {})
    }
}

@Preview
@Composable
private fun SyncStatusContentBodyConflictPreview() {
    KptTheme {
        SyncStatusContentBody(state = demoContentWithConflictState, onAction = {})
    }
}

@Preview
@Composable
private fun SyncStatusContentBodyOfflinePreview() {
    KptTheme {
        SyncStatusContentBody(state = demoOfflineState, onAction = {})
    }
}

@Preview
@Composable
private fun SyncStatusErrorBodyPreview() {
    KptTheme {
        SyncStatusErrorBody(state = demoErrorState)
    }
}
