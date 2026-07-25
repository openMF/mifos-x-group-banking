/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.database.meetingcalendar.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Room persisted representation of ONE group center's scheduled-meetings list (the merge of
 * `get_center_meetings` + `get_meeting_records_datatable`).
 *
 * Backs the offline cache (SourceOfTruth) for the single-key NETWORK_WITH_CACHE store
 * ([org.mifos.groupbanking.core.store.meetingcalendar.impl.provideMeetingCalendarStore]), a
 * **dynamic-key** read: one row per [centerId] so a cold start with no network still renders the
 * last-seen meetings for that center (`data-flow.yaml#cache.offline: fallback_cache`, SC2 — never
 * memory-only).
 *
 * The whole meetings list is persisted as ONE row per center via a single [meetingsJson] column
 * encoded by
 * [org.mifos.groupbanking.core.database.meetingcalendar.entity.MeetingCalendarCacheCodec] — the
 * `core/database` module owns `kotlinx-serialization`. A single-row upsert is inherently atomic, so
 * there is no delete-then-upsert race (RULE-IMPLEMENT-STORE5-001 S5-3 satisfied by construction).
 *
 * [fetchedAt] is the epoch-millis write time — used only for TTL pruning
 * ([org.mifos.groupbanking.core.database.meetingcalendar.dao.MeetingCalendarDao.deleteOlderThan]);
 * freshness for the UI banner is tracked separately by the framework `framework_fetched_at` table.
 *
 * See API.md#stores — MeetingCalendar.
 */
@Entity(tableName = "meeting_calendar_cache")
data class MeetingCalendarCacheEntity(
    @PrimaryKey
    val centerId: Int,
    val meetingsJson: String,
    val fetchedAt: Long,
)
