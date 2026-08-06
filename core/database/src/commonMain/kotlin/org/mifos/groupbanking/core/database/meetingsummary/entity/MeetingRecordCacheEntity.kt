/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.database.meetingsummary.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Room persisted representation of ONE completed meeting record per (groupId, meetingNumber)
 * (`GET /datatables/dt_meeting_record/{groupId}?meetingNumber=N`).
 *
 * Backs the offline cache (SourceOfTruth) for the single-key NETWORK_WITH_CACHE meeting-summary
 * store ([org.mifos.groupbanking.core.store.meetingsummary.impl.provideMeetingSummaryStore]): one
 * row per meeting so a cold start with no network still renders the last-seen summary
 * (`data-flow.yaml#cache.strategy: stale_while_revalidate`, `error_paths[network.offline]:
 * fallback_cache` — never memory-only).
 *
 * Keyed by [cacheKey] = `"$groupId:$meetingNumber"` (the composite store key flattened to a stable
 * String primary key); [groupId] / [meetingNumber] are kept as scalar columns for auditability.
 * The two nested collections `savingsBreakdown` + `loanItems` are folded into
 * [savingsBreakdownJson] / [loanItemsJson] JSON columns via
 * [org.mifos.groupbanking.core.database.meetingsummary.entity.MeetingRecordCacheCodec]. A single-row
 * upsert is inherently atomic, so there is no delete-then-upsert race (RULE-IMPLEMENT-STORE5-001
 * S5-3 satisfied by construction).
 *
 * [fetchedAt] is the epoch-millis write time — used only for TTL pruning ([deleteOlderThan]);
 * freshness for the UI banner is tracked separately by the framework `framework_fetched_at` table.
 *
 * See API.md#stores — MeetingSummaryData.
 */
@Entity(tableName = "meeting_record_cache")
data class MeetingRecordCacheEntity(
    @PrimaryKey
    val cacheKey: String,
    val groupId: Int,
    val meetingId: String,
    val meetingNumber: Int,
    val actualDate: String,
    val meetingTime: String,
    val attendanceCount: Int,
    val totalMemberCount: Int,
    val groupSavingsCollected: Long,
    val individualSavingsCollected: Long,
    val totalSavingsCollected: Long,
    val loansDisbursed: Long,
    val loansRepaid: Long,
    val finesCollected: Long,
    val openingCorpus: Long,
    val closingCorpus: Long,
    val savingsBreakdownJson: String,
    val loanItemsJson: String,
    val fetchedAt: Long,
)
