/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.store.fieldofficerdashboard.impl

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.map
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.base.store.infra.DefaultValidator
import kpt.core.base.store.infra.StoreFactory
import kpt.core.store.AppStoreRegistry
import org.mifos.groupbanking.core.database.fieldofficerdashboard.dao.FieldOfficerDashboardDao
import org.mifos.groupbanking.core.database.fieldofficerdashboard.entity.CachedFieldOfficerDashboard
import org.mifos.groupbanking.core.database.fieldofficerdashboard.entity.CachedGroupHealthSummary
import org.mifos.groupbanking.core.database.fieldofficerdashboard.entity.FieldOfficerDashboardCacheCodec
import org.mifos.groupbanking.core.database.fieldofficerdashboard.entity.FieldOfficerDashboardCacheEntity
import org.mifos.groupbanking.core.model.FieldOfficerDashboard
import org.mifos.groupbanking.core.model.GroupHealthSummary
import org.mifos.groupbanking.core.model.HealthIndicator
import org.mifos.groupbanking.core.network.mapper.aggregateFieldOfficerDashboard
import org.mifos.groupbanking.core.network.model.PagedCentersResponseDto
import org.mifos.groupbanking.core.network.service.fieldofficerdashboard.FieldOfficerApi
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import kotlin.time.Clock

/** Store key separator between the staffId and the session-derived userRole. */
private const val KEY_SEPARATOR = "|"

/**
 * Builds the composite dynamic-key read-only NETWORK_WITH_CACHE [Store] for the
 * field-officer-dashboard screen (FR-009) — the client-side fan-in of the TWO independent Fineract
 * reads the screen fires in parallel on mount/refresh/retry ([FieldOfficerApi.getGroupsForStaff],
 * [FieldOfficerApi.getCentersForStaff]).
 *
 * The key is a `String` encoding `"$staffId|$userRole"` and the value is one aggregated
 * [FieldOfficerDashboard] snapshot; Store5 caches each staff member independently (one Room row per
 * staffKey), so a cold start with no network still renders the last-seen dashboard for that staff
 * (`data-flow.yaml#cache.offline: show_cached_with_banner`, SC2 — never memory-only). The read side
 * is exposed to the UI exclusively through
 * `FieldOfficerDashboardRepository.fieldOfficerDashboardStream(...)` → `.asScreenStream(...)` — there
 * is no DAO-bypass read path (RULE-IMPLEMENT-STORE5-001 S5-2), and the dashboard is read-only so
 * there is no write path (S5-1).
 *
 * - **Fetcher** — a PARALLEL-COMBINE: inside a [coroutineScope] the two reads fire as concurrent
 *   [async] coroutines. `get_groups_for_staff` is the CRITICAL read (its failure throws
 *   [FieldOfficerDashboardFetchException] so Store5 routes it to an error response — no try-catch,
 *   no `Result` envelope); `get_centers_for_staff` is best-effort (its office names only enrich
 *   [FieldOfficerDashboard.availableRegions]), so an error there degrades to an empty center list
 *   rather than failing the whole load. The two are fanned into the aggregate by
 *   [aggregateFieldOfficerDashboard].
 * - **SourceOfTruth** — a Room table ([FieldOfficerDashboardDao]) so each staff member's aggregate
 *   survives process death. The whole aggregate is one row per staffKey, so the writer's keyed
 *   [FieldOfficerDashboardDao.replaceForKey] upsert is inherently atomic (guards S5-3), then calls
 *   [DefaultValidator.markFresh] so the TTL window opens on the successful network write (S5-5).
 * - **Validator** — TTL 5m ([AppStoreRegistry.Ttl.FIELD_OFFICER_DASHBOARD]) matching `data-flow.yaml`
 *   `ttl_seconds: 300` (stale-while-revalidate).
 *
 * See API.md#stores — FieldOfficerDashboard.
 */
fun provideFieldOfficerDashboardStore(
    api: FieldOfficerApi,
    dao: FieldOfficerDashboardDao,
): Store<String, FieldOfficerDashboard> {
    val validator = DefaultValidator.withTtl<FieldOfficerDashboard>(
        AppStoreRegistry.Ttl.FIELD_OFFICER_DASHBOARD,
    )
    return StoreFactory.createStore(
        fetcher = Fetcher.of { key: String ->
            val staffId = key.substringBefore(KEY_SEPARATOR).toLongOrNull() ?: 0L
            val userRole = key.substringAfter(KEY_SEPARATOR, missingDelimiterValue = FieldOfficerDashboard.ROLE_FIELD_OFFICER)
            coroutineScope {
                val groupsDeferred = async { api.getGroupsForStaff(staffId) }
                val centersDeferred = async { api.getCentersForStaff(staffId) }

                val groups = groupsDeferred.await().dataOrThrow().pageItems
                val centers = centersDeferred.await().dataOrEmpty().pageItems

                aggregateFieldOfficerDashboard(
                    staffId = staffId,
                    userRole = userRole,
                    groups = groups,
                    centers = centers,
                )
            }
        },
        sourceOfTruth = SourceOfTruth.of(
            reader = { key: String ->
                dao.observeByKey(key).map { row -> row?.toDomain() }
            },
            writer = { key: String, dashboard: FieldOfficerDashboard ->
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
 * Builds the composite store key from the [staffId] and session-derived [userRole]. The repository
 * threads this through so the ViewModel never re-encodes the key shape.
 */
fun fieldOfficerDashboardStoreKey(staffId: Long, userRole: String): String =
    "$staffId$KEY_SEPARATOR$userRole"

/**
 * Signals a failed field-officer-dashboard fetch to Store5's error channel. Carries the sealed
 * [NetworkError] of the critical read that failed so downstream error mapping can branch on the
 * exact cause; the message is `categorize()`-friendly for state routing.
 */
class FieldOfficerDashboardFetchException(
    val networkError: NetworkError,
) : Exception("Field-officer dashboard fetch failed: $networkError")

/** Unwraps a critical read: returns data on success, or throws so the whole fetch aborts. */
private fun <T> NetworkResult<T, NetworkError>.dataOrThrow(): T = when (this) {
    is NetworkResult.Success -> data
    is NetworkResult.Error -> throw FieldOfficerDashboardFetchException(error)
}

/** Unwraps a best-effort read: returns data on success, or an empty envelope on failure. */
private fun NetworkResult<PagedCentersResponseDto, NetworkError>.dataOrEmpty(): PagedCentersResponseDto = when (this) {
    is NetworkResult.Success -> data
    is NetworkResult.Error -> PagedCentersResponseDto()
}

// ---------------------------------------------------------------------------
// Inline aggregate <-> entity mapping — private to this store (groupdashboard precedent).
// core/store depends on core/model + core/database, so the domain<->payload mapping lives here;
// the aggregate round-trips through FieldOfficerDashboardCacheCodec (core/database owns
// kotlinx-serialization; core/store does not).
// ---------------------------------------------------------------------------

private fun FieldOfficerDashboard.toEntity(staffKey: String): FieldOfficerDashboardCacheEntity {
    val now = Clock.System.now().toEpochMilliseconds()
    return FieldOfficerDashboardCacheEntity(
        staffKey = staffKey,
        dashboardJson = FieldOfficerDashboardCacheCodec.encode(toPayload()),
        fetchedAt = now,
    )
}

private fun FieldOfficerDashboard.toPayload(): CachedFieldOfficerDashboard = CachedFieldOfficerDashboard(
    staffId = staffId,
    userRole = userRole,
    totalGroupsCount = totalGroupsCount,
    totalActiveMembers = totalActiveMembers,
    totalSavingsThisMonth = totalSavingsThisMonth,
    totalLoansOutstanding = totalLoansOutstanding,
    groups = groups.map { it.toPayload() },
    availableRegions = availableRegions,
)

private fun GroupHealthSummary.toPayload(): CachedGroupHealthSummary = CachedGroupHealthSummary(
    id = id,
    fineractGroupId = fineractGroupId,
    name = name,
    officeName = officeName,
    status = status,
    activeClientCount = activeClientCount,
    totalSavingsBalance = totalSavingsBalance,
    totalLoansOutstanding = totalLoansOutstanding,
    overdueRate = overdueRate,
    healthIndicator = healthIndicator.name,
    cycleNumber = cycleNumber,
)

private fun FieldOfficerDashboardCacheEntity.toDomain(): FieldOfficerDashboard {
    val payload = FieldOfficerDashboardCacheCodec.decode(dashboardJson)
    return FieldOfficerDashboard(
        staffId = payload.staffId,
        userRole = payload.userRole,
        totalGroupsCount = payload.totalGroupsCount,
        totalActiveMembers = payload.totalActiveMembers,
        totalSavingsThisMonth = payload.totalSavingsThisMonth,
        totalLoansOutstanding = payload.totalLoansOutstanding,
        groups = payload.groups.map { it.toDomain() },
        availableRegions = payload.availableRegions,
    )
}

private fun CachedGroupHealthSummary.toDomain(): GroupHealthSummary = GroupHealthSummary(
    id = id,
    fineractGroupId = fineractGroupId,
    name = name,
    officeName = officeName,
    status = status,
    activeClientCount = activeClientCount,
    totalSavingsBalance = totalSavingsBalance,
    totalLoansOutstanding = totalLoansOutstanding,
    overdueRate = overdueRate,
    // Re-derive from overdueRate on read (guards server/client drift for cached rows), falling
    // back to the persisted name only if the rate-derivation ever needs overriding.
    healthIndicator = HealthIndicator.entries.firstOrNull { it.name == healthIndicator }
        ?: HealthIndicator.fromOverdueRate(overdueRate),
    cycleNumber = cycleNumber,
)
