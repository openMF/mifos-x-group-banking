/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.store.grouplist.impl

import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.base.store.infra.DefaultValidator
import kpt.core.base.store.infra.StoreFactory
import kpt.core.base.store.paging.PageKey
import kpt.core.database.grouplist.dao.GroupListDao
import kpt.core.database.grouplist.entity.GroupListEntity
import kpt.core.model.Group
import kpt.core.model.GroupTypeSlug
import kpt.core.model.HealthIndicator
import kpt.core.model.ViewerRole
import kpt.core.network.mapper.toDomainModel
import kpt.core.network.service.grouplist.GroupApi
import kpt.core.store.AppStoreRegistry
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import kotlin.time.Clock

/**
 * Builds the PAGINATED read-only NETWORK_WITH_CACHE [Store] for the authenticated user's group
 * list (COMP-GRP-001 — `GET /companion/groups/mine`) that backs the group-list screen.
 *
 * The key is a [PageKey] (offset paging, `page_size=20` per `api.yaml#pagination`) and the value
 * is one page slice `List<Group>`; Store5 caches each page independently. The read side is exposed
 * to the UI exclusively through `GroupRepository.groupsPagingStream(...)` →
 * `.asPagingScreenStream(...)` — there is no DAO-bypass read path (RULE-IMPLEMENT-STORE5-001 S5-2),
 * and the list is read-only so there is no write path (S5-1).
 *
 * - **Fetcher** — [GroupApi.getMyGroups] with `offset = key.offset` (`= page * pageSize`) and
 *   `limit = key.pageSize`. The service returns a sealed [NetworkResult]; on
 *   [NetworkResult.Success] the [kpt.core.network.model.GroupPageDto] is mapped
 *   to domain via [toDomainModel] and its `groups` slice returned; on [NetworkResult.Error] the
 *   fetcher throws so Store5 routes it to an error response (no try-catch, no `Result` envelope —
 *   Store5 owns the error channel).
 * - **SourceOfTruth** — a Room table ([GroupListDao]) so pages survive process death and a cold
 *   start with no network still renders the last-seen pages
 *   (`data-flow.yaml#cache.offline: show_cached`, SC2 — never memory-only). The writer swaps ONE
 *   page's rows atomically via [GroupListDao.replacePage] (single `@Transaction`, guards
 *   S5-PAGE-ATOMIC / S5-3 — no delete-then-upsert race), then calls [DefaultValidator.markFresh]
 *   so the TTL window opens on the successful network write (S5-5 cold-start-stale guard).
 * - **Validator** — TTL 5m ([AppStoreRegistry.Ttl.GROUP_LIST]) matching `data-flow.yaml`
 *   `ttl_seconds: 300` (stale-while-revalidate).
 *
 * See API.md#stores — GroupList.
 */
fun provideGroupsPagingStore(
    api: GroupApi,
    dao: GroupListDao,
): Store<PageKey, List<Group>> {
    val validator = DefaultValidator.withTtl<List<Group>>(
        AppStoreRegistry.Ttl.GROUP_LIST,
    )
    return StoreFactory.createStore(
        fetcher = Fetcher.of { key: PageKey ->
            when (
                val result = api.getMyGroups(
                    paged = true,
                    limit = key.pageSize,
                    offset = key.offset,
                )
            ) {
                is NetworkResult.Success -> result.data.toDomainModel().groups
                is NetworkResult.Error -> throw GroupListFetchException(result.error)
            }
        },
        sourceOfTruth = SourceOfTruth.of(
            reader = { key: PageKey ->
                dao.observePage(key.page).map { rows ->
                    if (rows.isEmpty()) null else rows.toDomainModels()
                }
            },
            writer = { key: PageKey, groups: List<Group> ->
                dao.replacePage(key.page, groups.toEntities(key.page))
                validator.markFresh()
            },
            delete = { key: PageKey -> dao.deletePage(key.page) },
            deleteAll = { dao.deleteAll() },
        ),
        validator = validator,
    )
}

/**
 * Signals a failed group-list page fetch to Store5's error channel. Carries the sealed
 * [NetworkError] so downstream error mapping (feature-layer `AppErrorMapper`) can branch on the
 * exact cause; the message is `categorize()`-friendly for the default state routing.
 */
class GroupListFetchException(
    val networkError: NetworkError,
) : Exception("Group list fetch failed: $networkError")

// ---------------------------------------------------------------------------
// Inline entity <-> domain mapping — private to this store (GroupTypeConfigStore precedent).
// core/store depends on core/model + core/database, so mapping lives here rather than adding a
// core/model dependency to core/database. healthIndicator is re-derived client-side from
// overdueRate on read per data-flow.yaml (guards server/client drift for cached rows).
// ---------------------------------------------------------------------------

private fun List<GroupListEntity>.toDomainModels(): List<Group> = map { it.toDomain() }

private fun GroupListEntity.toDomain(): Group = Group(
    id = groupId,
    name = name,
    groupType = groupType.toGroupTypeSlug(),
    viewerRole = viewerRole.toViewerRole(),
    cycleNumber = cycleNumber,
    memberCount = memberCount,
    lastMeetingDate = LocalDate.parse(lastMeetingDate),
    healthIndicator = HealthIndicator.fromOverdueRate(overdueRate),
    overdueRate = overdueRate,
    status = status,
    fineractGroupId = fineractGroupId,
)

private fun List<Group>.toEntities(pageIndex: Int): List<GroupListEntity> {
    val now = Clock.System.now().toEpochMilliseconds()
    return mapIndexed { index, group -> group.toEntity(pageIndex, index, now) }
}

private fun Group.toEntity(pageIndex: Int, rowOrder: Int, fetchedAt: Long): GroupListEntity = GroupListEntity(
    groupId = id,
    pageIndex = pageIndex,
    rowOrder = rowOrder,
    name = name,
    groupType = groupType.name,
    viewerRole = viewerRole.name,
    cycleNumber = cycleNumber,
    memberCount = memberCount,
    lastMeetingDate = lastMeetingDate.toString(),
    overdueRate = overdueRate,
    status = status,
    fineractGroupId = fineractGroupId,
    fetchedAt = fetchedAt,
)

private fun String.toGroupTypeSlug(): GroupTypeSlug =
    GroupTypeSlug.entries.firstOrNull { it.name == this } ?: GroupTypeSlug.UNKNOWN

private fun String.toViewerRole(): ViewerRole =
    ViewerRole.entries.firstOrNull { it.name == this } ?: ViewerRole.UNKNOWN
