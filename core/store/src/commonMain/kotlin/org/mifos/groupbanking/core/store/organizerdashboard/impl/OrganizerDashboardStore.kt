/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.store.organizerdashboard.impl

import kotlinx.coroutines.flow.map
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.base.store.infra.DefaultValidator
import kpt.core.base.store.infra.StoreFactory
import kpt.core.store.AppStoreRegistry
import org.mifos.groupbanking.core.database.organizerdashboard.dao.OrganizerDashboardDao
import org.mifos.groupbanking.core.database.organizerdashboard.entity.CachedOrganizerActivityItem
import org.mifos.groupbanking.core.database.organizerdashboard.entity.CachedOrganizerDashboard
import org.mifos.groupbanking.core.database.organizerdashboard.entity.CachedScheduledMeeting
import org.mifos.groupbanking.core.database.organizerdashboard.entity.OrganizerDashboardCacheCodec
import org.mifos.groupbanking.core.database.organizerdashboard.entity.OrganizerDashboardCacheEntity
import org.mifos.groupbanking.core.model.OrganizerActivityItem
import org.mifos.groupbanking.core.model.OrganizerActivityType
import org.mifos.groupbanking.core.model.OrganizerDashboardSummary
import org.mifos.groupbanking.core.model.ScheduledMeeting
import org.mifos.groupbanking.core.network.mapper.toDomainModel
import org.mifos.groupbanking.core.network.service.organizerdashboard.OrganizerDashboardApi
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import kotlin.time.Clock

/**
 * Constant store key for the organizer-dashboard single-key read. The companion API resolves the
 * organizer's group scope server-side from the auth token (no params), so there is exactly one cache
 * row — keyed by this sentinel.
 */
const val ORGANIZER_DASHBOARD_KEY: String = "__organizer__"

/**
 * Builds the **single-key** read-only NETWORK_WITH_CACHE [Store] for the organizer-dashboard hub
 * (`GET /companion/organizer/dashboard`) that backs the organizer-dashboard screen.
 *
 * The key is the constant [ORGANIZER_DASHBOARD_KEY] and the value is one
 * [OrganizerDashboardSummary] snapshot; the read side is exposed to the UI exclusively through
 * `OrganizerDashboardRepository.organizerDashboardStream(...)` → `.asScreenStream(...)` — there is no
 * DAO-bypass read path (RULE-IMPLEMENT-STORE5-001 S5-2), and the dashboard is read-only
 * (`data-flow.yaml#sync_queue: []`) so there is no write path (S5-1).
 *
 * - **Fetcher** — [OrganizerDashboardApi.getOrganizerDashboard]. The service returns a sealed
 *   [NetworkResult]; on [NetworkResult.Success] the DTO is mapped to domain via [toDomainModel], on
 *   [NetworkResult.Error] the fetcher throws so Store5 routes it to an error response (no try-catch,
 *   no `Result` envelope — Store5 owns the error channel).
 * - **SourceOfTruth** — a Room table ([OrganizerDashboardDao]) so the dashboard survives process
 *   death and a cold start with no network still renders the last-seen dashboard
 *   (`data-flow.yaml#cache.offline: serve_stale`, SC2 — never memory-only). The whole snapshot is
 *   one row, so the writer's keyed [OrganizerDashboardDao.replaceForKey] upsert is inherently atomic
 *   (guards S5-3 — no delete-then-upsert race); it then calls [DefaultValidator.markFresh] so the
 *   TTL window opens on the successful network write (S5-5 cold-start-stale guard).
 * - **Validator** — TTL 5m ([AppStoreRegistry.Ttl.ORGANIZER_DASHBOARD]) matching `data-flow.yaml`
 *   `ttl_seconds: 300` (stale-while-revalidate).
 *
 * See API.md#stores — OrganizerDashboard.
 */
fun provideOrganizerDashboardStore(
    api: OrganizerDashboardApi,
    dao: OrganizerDashboardDao,
): Store<String, OrganizerDashboardSummary> {
    val validator = DefaultValidator.withTtl<OrganizerDashboardSummary>(
        AppStoreRegistry.Ttl.ORGANIZER_DASHBOARD,
    )
    return StoreFactory.createStore(
        fetcher = Fetcher.of { _: String ->
            when (val result = api.getOrganizerDashboard()) {
                is NetworkResult.Success -> result.data.toDomainModel()
                is NetworkResult.Error -> throw OrganizerDashboardFetchException(result.error)
            }
        },
        sourceOfTruth = SourceOfTruth.of(
            reader = { key: String ->
                dao.observeByKey(key).map { row -> row?.toDomain() }
            },
            writer = { key: String, dashboard: OrganizerDashboardSummary ->
                dao.replaceForKey(dashboard.toEntity(key))
                validator.markFresh()
            },
            delete = { key: String -> dao.deleteByKey(key) },
            deleteAll = { dao.deleteAll() },
        ),
        validator = validator,
    )
}

/**
 * Signals a failed organizer-dashboard fetch to Store5's error channel. Carries the sealed
 * [NetworkError] so downstream error mapping can branch on the exact cause (401 → auth, offline →
 * cache fallback per `data-flow.yaml`); the message is `categorize()`-friendly for state routing.
 */
class OrganizerDashboardFetchException(
    val networkError: NetworkError,
) : Exception("Organizer dashboard fetch failed: $networkError")

// ---------------------------------------------------------------------------
// Inline aggregate <-> entity mapping — private to this store (personaldashboard /
// fieldofficerdashboard precedent). core/store depends on core/model + core/database, so the
// domain<->payload mapping lives here; the aggregate round-trips through
// OrganizerDashboardCacheCodec (core/database owns kotlinx-serialization; core/store does not).
// ---------------------------------------------------------------------------

private fun OrganizerDashboardSummary.toEntity(cacheKey: String): OrganizerDashboardCacheEntity {
    val now = Clock.System.now().toEpochMilliseconds()
    return OrganizerDashboardCacheEntity(
        cacheKey = cacheKey,
        dashboardJson = OrganizerDashboardCacheCodec.encode(toPayload()),
        fetchedAt = now,
    )
}

private fun OrganizerDashboardSummary.toPayload(): CachedOrganizerDashboard = CachedOrganizerDashboard(
    organizerName = organizerName,
    myGroupCount = myGroupCount,
    totalMembers = totalMembers,
    pendingShareOutCount = pendingShareOutCount,
    meetingsTodayCount = meetingsTodayCount,
    fieldOfficerEnabled = fieldOfficerEnabled,
    todaySchedule = todaySchedule.map { it.toPayload() },
    recentActivity = recentActivity.map { it.toPayload() },
)

private fun ScheduledMeeting.toPayload(): CachedScheduledMeeting = CachedScheduledMeeting(
    groupId = groupId,
    groupName = groupName,
    meetingTime = meetingTime,
    memberCount = memberCount,
    location = location,
)

private fun OrganizerActivityItem.toPayload(): CachedOrganizerActivityItem = CachedOrganizerActivityItem(
    id = id,
    type = type.name,
    description = description,
    amount = amount,
    date = date,
    memberName = memberName,
    groupName = groupName,
)

private fun OrganizerDashboardCacheEntity.toDomain(): OrganizerDashboardSummary {
    val payload = OrganizerDashboardCacheCodec.decode(dashboardJson)
    return OrganizerDashboardSummary(
        organizerName = payload.organizerName,
        myGroupCount = payload.myGroupCount,
        totalMembers = payload.totalMembers,
        pendingShareOutCount = payload.pendingShareOutCount,
        meetingsTodayCount = payload.meetingsTodayCount,
        fieldOfficerEnabled = payload.fieldOfficerEnabled,
        todaySchedule = payload.todaySchedule.map { it.toDomain() },
        recentActivity = payload.recentActivity.map { it.toDomain() },
    )
}

private fun CachedScheduledMeeting.toDomain(): ScheduledMeeting = ScheduledMeeting(
    groupId = groupId,
    groupName = groupName,
    meetingTime = meetingTime,
    memberCount = memberCount,
    location = location,
)

private fun CachedOrganizerActivityItem.toDomain(): OrganizerActivityItem = OrganizerActivityItem(
    id = id,
    type = OrganizerActivityType.entries.firstOrNull { it.name == type } ?: OrganizerActivityType.UNKNOWN,
    description = description,
    amount = amount,
    date = date,
    memberName = memberName,
    groupName = groupName,
)
