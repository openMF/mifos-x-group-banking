/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.store.meetingcalendar.impl

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.map
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.base.store.infra.DefaultValidator
import kpt.core.base.store.infra.StoreFactory
import kpt.core.store.AppStoreRegistry
import org.mifos.groupbanking.core.database.meetingcalendar.dao.MeetingCalendarDao
import org.mifos.groupbanking.core.database.meetingcalendar.entity.CachedMeetingItem
import org.mifos.groupbanking.core.database.meetingcalendar.entity.MeetingCalendarCacheCodec
import org.mifos.groupbanking.core.database.meetingcalendar.entity.MeetingCalendarCacheEntity
import org.mifos.groupbanking.core.model.MeetingListItem
import org.mifos.groupbanking.core.model.MeetingStatus
import org.mifos.groupbanking.core.network.mapper.toMeetingListItems
import org.mifos.groupbanking.core.network.service.meetingcalendar.MeetingApi
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import kotlin.time.Clock

/**
 * Builds the **single-key** read-only NETWORK_WITH_CACHE [Store] for the meeting-calendar screen —
 * the client-side merge of the TWO reads the screen fires in parallel on mount/refresh
 * ([MeetingApi.getCenterMeetings] + [MeetingApi.getMeetingRecords]).
 *
 * The key is the `Int` `centerId` and the value is the merged `List<MeetingListItem>`; Store5 caches
 * each center independently (one Room row per center). The read side is exposed to the UI
 * exclusively through `MeetingRepository.meetingsStream(...)` → `.asScreenStream(...)` — there is no
 * DAO-bypass read path (RULE-IMPLEMENT-STORE5-001 S5-2), and the list is read-only
 * (`data-flow.yaml#sync_queue: []`) so there is no write path (S5-1).
 *
 * - **Fetcher** — a PARALLEL-COMBINE: inside a [coroutineScope] the two reads fire as concurrent
 *   [async] coroutines. `get_center_meetings` is the critical read (its failure aborts the fetch to
 *   Store5's error channel via [MeetingCalendarFetchException]); `get_meeting_records_datatable` is
 *   BEST-EFFORT (`404 -> no records yet` degrades to the bare meetings list per `data-flow.yaml`) so
 *   a failed records read is folded in as `null` and the mapper simply leaves the past-meeting
 *   figures un-enriched. No try-catch, no `Result` envelope — Store5 owns the error channel.
 * - **SourceOfTruth** — a Room table ([MeetingCalendarDao]) so each center's list survives process
 *   death and a cold start with no network still renders the last-seen meetings
 *   (`data-flow.yaml#cache.offline: fallback_cache`, SC2 — never memory-only). The whole list is one
 *   row per center, so the writer's keyed [MeetingCalendarDao.replaceForKey] upsert is inherently
 *   atomic (guards S5-3); it then calls [DefaultValidator.markFresh] so the TTL window opens on the
 *   successful network write (S5-5 cold-start-stale guard).
 * - **Validator** — TTL 5m ([AppStoreRegistry.Ttl.MEETING_CALENDAR]) matching `data-flow.yaml`
 *   `ttl_seconds: 300` (stale-while-revalidate).
 *
 * See API.md#stores — MeetingCalendar.
 */
fun provideMeetingCalendarStore(
    api: MeetingApi,
    dao: MeetingCalendarDao,
): Store<Int, List<MeetingListItem>> {
    val validator = DefaultValidator.withTtl<List<MeetingListItem>>(
        AppStoreRegistry.Ttl.MEETING_CALENDAR,
    )
    return StoreFactory.createStore(
        fetcher = Fetcher.of { centerId: Int ->
            coroutineScope {
                val meetingsDeferred = async { api.getCenterMeetings(centerId) }
                val recordsDeferred = async { api.getMeetingRecords(centerId) }

                // get_center_meetings is critical — abort the whole fetch on failure.
                val meetings = meetingsDeferred.await().dataOrThrow()
                // get_meeting_records is best-effort — a failure (e.g. 404 "no records yet") folds
                // in as null and the merge leaves past-meeting figures un-enriched.
                val records = recordsDeferred.await().dataOrNull()

                toMeetingListItems(meetings = meetings, records = records)
            }
        },
        sourceOfTruth = SourceOfTruth.of(
            reader = { centerId: Int ->
                dao.observeByKey(centerId).map { row -> row?.toDomain() }
            },
            writer = { centerId: Int, meetings: List<MeetingListItem> ->
                dao.replaceForKey(meetings.toEntity(centerId))
                validator.markFresh()
            },
            delete = { centerId: Int -> dao.deleteByKey(centerId) },
            deleteAll = { dao.deleteAll() },
        ),
        validator = validator,
    )
}

/**
 * Signals a failed meeting-calendar fetch to Store5's error channel. Carries the sealed
 * [NetworkError] of the critical `get_center_meetings` read so downstream error mapping can branch
 * on the exact cause (401 -> login, 404 -> empty, offline -> cache fallback per `data-flow.yaml`).
 */
class MeetingCalendarFetchException(
    val networkError: NetworkError,
) : Exception("Meeting calendar fetch failed: $networkError")

/** Unwraps the critical read: data on success, or throws [MeetingCalendarFetchException] on failure. */
private fun <T> NetworkResult<T, NetworkError>.dataOrThrow(): T = when (this) {
    is NetworkResult.Success -> data
    is NetworkResult.Error -> throw MeetingCalendarFetchException(error)
}

/** Unwraps a best-effort read: data on success, or `null` on failure (records enrichment is optional). */
private fun <T> NetworkResult<T, NetworkError>.dataOrNull(): T? = when (this) {
    is NetworkResult.Success -> data
    is NetworkResult.Error -> null
}

// ---------------------------------------------------------------------------
// Inline domain <-> entity mapping — private to this store (group-dashboard precedent).
// core/store depends on core/model + core/database, so the domain<->payload mapping lives here;
// the whole list round-trips through MeetingCalendarCacheCodec (core/database owns
// kotlinx-serialization; core/store does not).
// ---------------------------------------------------------------------------

private fun List<MeetingListItem>.toEntity(centerId: Int): MeetingCalendarCacheEntity {
    val now = Clock.System.now().toEpochMilliseconds()
    return MeetingCalendarCacheEntity(
        centerId = centerId,
        meetingsJson = MeetingCalendarCacheCodec.encode(map { it.toPayload() }),
        fetchedAt = now,
    )
}

private fun MeetingListItem.toPayload(): CachedMeetingItem = CachedMeetingItem(
    meetingId = meetingId,
    meetingNumber = meetingNumber,
    meetingDate = meetingDate,
    status = status.name,
    attendanceCount = attendanceCount,
    totalCollectedKES = totalCollectedKES,
)

private fun MeetingCalendarCacheEntity.toDomain(): List<MeetingListItem> =
    MeetingCalendarCacheCodec.decode(meetingsJson).map { it.toDomain() }

private fun CachedMeetingItem.toDomain(): MeetingListItem = MeetingListItem(
    meetingId = meetingId,
    meetingNumber = meetingNumber,
    meetingDate = meetingDate,
    status = status.toMeetingStatus(),
    attendanceCount = attendanceCount,
    totalCollectedKES = totalCollectedKES,
)

private fun String.toMeetingStatus(): MeetingStatus =
    MeetingStatus.entries.firstOrNull { it.name == this } ?: MeetingStatus.MISSED
