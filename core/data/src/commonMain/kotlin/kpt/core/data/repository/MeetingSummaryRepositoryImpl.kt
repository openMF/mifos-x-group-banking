/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.data.repository

import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kpt.core.base.store.infra.FetchedAtRepository
import kpt.core.base.store.screen.FetchPolicy
import kpt.core.base.store.screen.ScreenDataStream
import kpt.core.base.store.screen.asScreenStream
import kpt.core.database.meetingsummary.dao.MeetingRecordDao
import kpt.core.model.MeetingSummaryData
import kpt.core.store.AppStoreRegistry
import kpt.core.store.meetingsummary.impl.primeMeetingSummaryCache
import org.mobilenativefoundation.store.store5.Store

/**
 * Store5-backed implementation of [MeetingSummaryRepository].
 *
 * The single-key read maps the (`groupId`, `meetingNumber`) pair to the composite store key
 * `"$groupId:$meetingNumber"` and goes exclusively through [Store.asScreenStream] so the whole
 * offline-first pipeline (cached emit → background revalidate → DecisionEngine → ScreenState) is
 * inherited from `core-base`. The freshness [FetchedAtRepository] cacheKey is per-meeting
 * (`meetingsummary:record:{groupId}:{meetingNumber}`) so each meeting's TTL window is tracked
 * independently. No DAO-bypass read, no `try-catch`, no `Result` envelope (RULE-IMPLEMENT-STORE5-001
 * S5-2).
 *
 * See API.md#stores — MeetingSummaryData.
 */
class MeetingSummaryRepositoryImpl(
    private val meetingSummaryStore: Store<String, MeetingSummaryData>,
    private val networkMonitor: NetworkMonitor,
    private val fetchedAtRepository: FetchedAtRepository,
    private val meetingRecordDao: MeetingRecordDao,
) : MeetingSummaryRepository {

    override fun meetingSummaryStream(
        groupId: Int,
        meetingNumber: Int,
        scope: CoroutineScope,
        fetchPolicy: FetchPolicy,
    ): ScreenDataStream<MeetingSummaryData> {
        val key = "$groupId:$meetingNumber"
        return meetingSummaryStore.asScreenStream(
            key = key,
            networkMonitor = networkMonitor,
            fetchedAtRepository = fetchedAtRepository,
            cacheKey = "$CACHE_KEY_PREFIX$key",
            scope = scope,
            // A single MeetingSummaryData snapshot is never "empty" once present — Content always.
            isEmpty = { false },
            fetchPolicy = fetchPolicy,
            ttl = AppStoreRegistry.Ttl.MEETING_SUMMARY,
        )
    }

    override suspend fun primeSubmittedSummary(
        groupId: Int,
        meetingNumber: Int,
        data: MeetingSummaryData,
    ) {
        // Write-through the just-submitted record into the read Store's SourceOfTruth so the summary
        // is offline-first (see interface KDoc + primeMeetingSummaryCache).
        primeMeetingSummaryCache(meetingRecordDao, groupId, meetingNumber, data)
    }

    private companion object {
        /** FetchedAtRepository key prefix — one freshness timestamp per (groupId, meetingNumber). */
        const val CACHE_KEY_PREFIX = "meetingsummary:record:"
    }
}
