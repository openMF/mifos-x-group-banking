/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.syncstatus

import org.mifos.groupbanking.core.model.EntityType

/**
 * Append-only test-tag registry for the `sync-status` feature (RULE-KMP-COMPOSE-UITEST-001
 * CU-5 — names are stable across regenerations; only append new entries, never rename or
 * remove). Consumed by Compose UI tests under `feature/sync-status/src/commonTest/` and by the
 * Maestro flow generator (`core/scripts/maestro-flow-gen.ts`), which emits
 * `tapOn: { id: "<test_tag>" }` selectors from these constants. See API.md#tags.
 */
object SyncStatusTestTags {

    /** Root [kpt.core.ui.scaffold.KptScaffold] surface — always rendered regardless of screenState. */
    const val SCREEN: String = "sync_status_screen"

    /** Top app bar (`ui.yaml#components.top_bar`) — title-only, no navigation icon (terminal screen). */
    const val TOP_BAR: String = "sync_status_top_bar"

    /** `overall_status_card` — always rendered on Content/Error; color-coded by [org.mifos.groupbanking.core.model.SyncOverallStatus]. */
    const val OVERALL_STATUS_CARD: String = "sync_status_overall_status_card"

    /** `overall_status_card.status_icon`. */
    const val STATUS_ICON: String = "sync_status_status_icon"

    /** `overall_status_card.status_label`. */
    const val STATUS_LABEL: String = "sync_status_status_label"

    /** `conflict_chip` — visible only when `conflictCount > 0`. */
    const val CONFLICT_CHIP: String = "sync_status_conflict_chip"

    /** `pending_breakdown_card` — visible only when `pendingCount > 0`. */
    const val PENDING_BREAKDOWN_CARD: String = "sync_status_pending_breakdown_card"

    /** `sync_now_button` — dispatches `OnSyncNow`; enabled only when `isOnline && !isSyncing`. */
    const val SYNC_NOW_BUTTON: String = "sync_status_sync_now_button"

    /** `failed_operations_section` — visible only when `failedCount > 0`. */
    const val FAILED_OPERATIONS_SECTION: String = "sync_status_failed_operations_section"

    /** `all_synced_empty` — visible only when `pendingCount == 0 && failedCount == 0`. */
    const val ALL_SYNCED_EMPTY: String = "sync_status_all_synced_empty"

    /** `shimmer_status` — 3 skeleton blocks shown for `SyncStatusScreenState.Loading`. */
    const val SHIMMER: String = "sync_status_shimmer"

    /** Per-[EntityType] row inside `pending_breakdown_card.entity_breakdown_rows`. */
    fun pendingRowTag(entityType: EntityType): String = "sync_status_pending_row_${entityType.name.lowercase()}"

    /** Per-[org.mifos.groupbanking.core.model.SyncQueueItem.id] row inside `failed_ops_list`. */
    fun failedRowTag(itemId: Long): String = "sync_status_failed_row_$itemId"

    /** Per-[org.mifos.groupbanking.core.model.SyncQueueItem.id] `retry_button`. */
    fun failedRetryButtonTag(itemId: Long): String = "sync_status_failed_retry_button_$itemId"
}
