/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.data.repository

import co.touchlab.kermit.Logger
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import kpt.core.base.store.infra.FetchedAtRepository
import kpt.core.base.store.screen.FetchPolicy
import kpt.core.base.store.screen.ScreenDataStream
import kpt.core.base.store.screen.asScreenStream
import kpt.core.store.AppStoreRegistry
import org.mifos.groupbanking.core.model.MeetingListItem
import org.mifos.groupbanking.core.model.RescheduleMeetingRequest
import org.mobilenativefoundation.store.store5.Store

/**
 * Store5-backed implementation of [MeetingRepository].
 *
 * The single-key read maps `centerId` to the store key and goes exclusively through
 * [Store.asScreenStream] so the whole offline-first pipeline (cached emit → background revalidate →
 * DecisionEngine → ScreenState) is inherited from `core-base`. The freshness [cacheKey] is
 * per-center (`meetingcalendar:{centerId}`) so each center's TTL window is tracked independently. No
 * DAO-bypass read, no `try-catch`, no `Result` envelope (RULE-IMPLEMENT-STORE5-001 S5-2). An empty
 * meetings list surfaces as `ScreenState.Empty` via the `isEmpty` predicate. The error_state Retry
 * CTA re-drives via [ScreenDataStream.retry] — the injected [NetworkMonitor] pre-checks connectivity.
 *
 * See API.md#stores — MeetingCalendar.
 */
class MeetingRepositoryImpl(
    private val meetingCalendarStore: Store<Int, List<MeetingListItem>>,
    private val networkMonitor: NetworkMonitor,
    private val fetchedAtRepository: FetchedAtRepository,
    private val syncQueueRepository: SyncQueueRepository,
) : MeetingRepository {

    override fun meetingsStream(
        centerId: Int,
        scope: CoroutineScope,
        fetchPolicy: FetchPolicy,
    ): ScreenDataStream<List<MeetingListItem>> {
        return meetingCalendarStore.asScreenStream(
            key = centerId,
            networkMonitor = networkMonitor,
            fetchedAtRepository = fetchedAtRepository,
            cacheKey = "$CACHE_KEY_PREFIX$centerId",
            scope = scope,
            // A fetched meetings list is "present" even when empty — `isEmpty = { false }` so an
            // empty result maps to Content, then the ViewModel's `.emptyIfContent { it.isEmpty() }`
            // maps that empty Content to ScreenState.Empty. Using `isEmpty = { it.isEmpty() }` here
            // was wrong: DecisionEngine's no-data branch maps (empty + online + no-error) to Loading,
            // never Empty, so a group with zero scheduled meetings loaded forever.
            isEmpty = { false },
            fetchPolicy = fetchPolicy,
            ttl = AppStoreRegistry.Ttl.MEETING_CALENDAR,
        )
    }

    /**
     * G3 / F6 reschedule write — server-gated, so it offline-queues the recurrence-adjustment payload
     * to the shared `sync_queue` (never a live network PUT until the companion `companion_update_calendar`
     * tool is deployed). See [MeetingRepository.rescheduleMeeting] KDoc. No try-catch (local Room, not
     * network) — mirrors `MeetingConductRepositoryImpl.enqueueMeetingOffline`.
     */
    override suspend fun rescheduleMeeting(request: RescheduleMeetingRequest): Long {
        val payloadJson = meetingJson.encodeToString(RescheduleMeetingRequest.serializer(), request)
        Logger.i(TAG) {
            "rescheduleMeeting: queuing $RESCHEDULE_OPERATION_TYPE centerId=${request.centerId} " +
                "day=${request.day} time=${request.time} frequency=${request.frequency} (server-gated → offline queue)"
        }
        return syncQueueRepository.enqueue(
            operationType = RESCHEDULE_OPERATION_TYPE,
            targetTable = RESCHEDULE_TARGET_TABLE,
            payloadJson = payloadJson,
        )
    }

    private companion object {
        /** FetchedAtRepository key prefix — one freshness timestamp per center. */
        const val CACHE_KEY_PREFIX = "meetingcalendar:"
        const val TAG = "MeetingRepository"

        /** Sync-queue discriminator + target table for the server-gated reschedule write (G3 / F6). */
        const val RESCHEDULE_OPERATION_TYPE = "UPDATE_MEETING_CALENDAR"
        const val RESCHEDULE_TARGET_TABLE = "dt_group_config"

        val meetingJson = Json { encodeDefaults = true }
    }
}
