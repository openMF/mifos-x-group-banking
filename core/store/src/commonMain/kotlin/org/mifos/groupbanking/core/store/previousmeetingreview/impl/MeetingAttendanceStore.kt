/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.store.previousmeetingreview.impl

import kotlinx.coroutines.flow.map
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.base.store.infra.DefaultValidator
import kpt.core.base.store.infra.StoreFactory
import kpt.core.store.AppStoreRegistry
import org.mifos.groupbanking.core.database.previousmeetingreview.dao.MeetingAttendanceDao
import org.mifos.groupbanking.core.database.previousmeetingreview.entity.CachedAttendanceRecord
import org.mifos.groupbanking.core.database.previousmeetingreview.entity.MeetingAttendanceCacheCodec
import org.mifos.groupbanking.core.database.previousmeetingreview.entity.MeetingAttendanceCacheEntity
import org.mifos.groupbanking.core.model.AttendanceRecord
import org.mifos.groupbanking.core.model.AttendanceStatus
import org.mifos.groupbanking.core.network.mapper.toDomainModels
import org.mifos.groupbanking.core.network.service.previousmeetingreview.MeetingAttendanceApi
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import kotlin.time.Clock

/**
 * Builds the **single-key** read-only NETWORK_WITH_CACHE [Store] for the previous-meeting-review
 * per-member attendance roster (`GET /datatables/dt_meeting_attendance/{meetingId}`).
 *
 * The store key is the `meetingId` String and the value is the meeting's `List<AttendanceRecord>`
 * (PRESENT / LATE / ABSENT + fine). It is a SEPARATE store from the reused single-key
 * `MeetingSummaryStore` (which carries the record totals + savings + loans) — the review screen
 * merges the two reads in `PreviousMeetingReviewRepository`. The read side is exposed to the UI
 * exclusively through that repository's `.asScreenStream(...)` — there is no DAO-bypass read path
 * (RULE-IMPLEMENT-STORE5-001 S5-2), and the review screen is read-only (`data-flow.yaml#sync_queue:
 * []`) so there is no write path (S5-1).
 *
 * - **Fetcher** — [MeetingAttendanceApi.getMeetingAttendance]; on [NetworkResult.Success] the DTO
 *   list is mapped to domain via [toDomainModels], on [NetworkResult.Error] the fetcher throws so
 *   Store5 routes it to an error response (no try-catch, no `Result` envelope).
 * - **SourceOfTruth** — a Room table ([MeetingAttendanceDao]) so the roster survives process death
 *   and a cold start with no network still renders the last-seen attendance
 *   (`data-flow.yaml#error_paths[network.offline]: fallback_cache`, SC2 — never memory-only). One row
 *   per meeting, so the keyed [MeetingAttendanceDao.replaceForKey] upsert is inherently atomic
 *   (guards S5-3); it then calls [DefaultValidator.markFresh] so the TTL window opens on the
 *   successful network write (S5-5 cold-start-stale guard).
 * - **Validator** — TTL 5m ([AppStoreRegistry.Ttl.MEETING_ATTENDANCE]) matching `data-flow.yaml`
 *   `stale_while_revalidate`, `ttl_seconds: 300`.
 *
 * See API.md#stores — AttendanceRecord.
 */
fun provideMeetingAttendanceStore(
    api: MeetingAttendanceApi,
    dao: MeetingAttendanceDao,
): Store<String, List<AttendanceRecord>> {
    val validator = DefaultValidator.withTtl<List<AttendanceRecord>>(
        AppStoreRegistry.Ttl.MEETING_ATTENDANCE,
    )
    return StoreFactory.createStore(
        fetcher = Fetcher.of { meetingId: String ->
            when (val result = api.getMeetingAttendance(meetingId = meetingId)) {
                is NetworkResult.Success -> result.data.toDomainModels()
                is NetworkResult.Error -> throw MeetingAttendanceFetchException(result.error)
            }
        },
        sourceOfTruth = SourceOfTruth.of(
            reader = { meetingId: String ->
                dao.observeByKey(meetingId).map { row -> row?.toDomain() }
            },
            writer = { meetingId: String, data: List<AttendanceRecord> ->
                dao.replaceForKey(data.toEntity(meetingId))
                validator.markFresh()
            },
            delete = { meetingId: String -> dao.deleteByKey(meetingId) },
            deleteAll = { dao.deleteAll() },
        ),
        validator = validator,
    )
}

/**
 * Signals a failed attendance fetch to Store5's error channel. Carries the sealed [NetworkError] so
 * downstream error mapping (feature-layer) can branch on the exact cause; the message is
 * `categorize()`-friendly.
 */
class MeetingAttendanceFetchException(
    val networkError: NetworkError,
) : Exception("Meeting attendance fetch failed: $networkError")

// ---------------------------------------------------------------------------
// Inline entity <-> domain mapping — private to this store (MeetingSummaryStore precedent).
// core/store depends on core/model + core/database, so mapping lives here rather than adding a
// core/model dependency to core/database. The attendance list round-trips through
// MeetingAttendanceCacheCodec (core/database owns kotlinx-serialization; core/store does not).
// ---------------------------------------------------------------------------

private fun List<AttendanceRecord>.toEntity(meetingId: String): MeetingAttendanceCacheEntity {
    val now = Clock.System.now().toEpochMilliseconds()
    return MeetingAttendanceCacheEntity(
        meetingId = meetingId,
        attendanceJson = MeetingAttendanceCacheCodec.encode(map { it.toPayload() }),
        fetchedAt = now,
    )
}

private fun MeetingAttendanceCacheEntity.toDomain(): List<AttendanceRecord> =
    MeetingAttendanceCacheCodec.decode(attendanceJson).map { it.toDomain() }

private fun AttendanceRecord.toPayload(): CachedAttendanceRecord = CachedAttendanceRecord(
    memberId = memberId,
    memberName = memberName,
    status = status.name,
    fineAmount = fineAmount,
)

private fun CachedAttendanceRecord.toDomain(): AttendanceRecord = AttendanceRecord(
    memberId = memberId,
    memberName = memberName,
    status = AttendanceStatus.fromWire(status),
    fineAmount = fineAmount,
)
