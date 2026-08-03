/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.store.personaldashboard.impl

import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kpt.core.base.database.invalidation.daoFlow
import kpt.core.base.database.invalidation.notifyingWrite
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.base.store.infra.DefaultValidator
import kpt.core.base.store.infra.StoreFactory
import kpt.core.store.AppStoreRegistry
import org.mifos.groupbanking.core.database.personaldashboard.dao.MemberDashboardDao
import org.mifos.groupbanking.core.database.personaldashboard.entity.CachedGroupSummary
import org.mifos.groupbanking.core.database.personaldashboard.entity.CachedSavingsTransaction
import org.mifos.groupbanking.core.database.personaldashboard.entity.MemberDashboardCacheCodec
import org.mifos.groupbanking.core.database.personaldashboard.entity.MemberDashboardCacheEntity
import org.mifos.groupbanking.core.model.GroupSummary
import org.mifos.groupbanking.core.model.MemberDashboard
import org.mifos.groupbanking.core.model.SavingsMechanism
import org.mifos.groupbanking.core.model.SavingsTransaction
import org.mifos.groupbanking.core.model.TransactionType
import org.mifos.groupbanking.core.network.mapper.toDomainModel
import org.mifos.groupbanking.core.network.service.personaldashboard.MemberDashboardApi
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import kotlin.time.Clock

/**
 * Sentinel store key used when the caller passes `selectedGroupId = null` — the companion API
 * resolves the member's first group server-side, and its snapshot is cached under this key. Every
 * other group is cached under its own `groupId`, one Room row per group (dynamic-key read).
 */
const val MEMBER_DASHBOARD_DEFAULT_KEY: String = "__default__"

/**
 * Room table backing this store's SourceOfTruth. Declared for the [daoFlow]/[notifyingWrite]
 * RoomChangeBus bridge so a SoT write re-fans-out to the open `cached(refresh = false)` reader
 * subscription — without it, Room 3 alpha's InvalidationTracker did NOT re-emit the observeByKey
 * Flow after the fetcher's upsert on device, leaving the ScreenDataStream stuck in Loading even
 * though the fetch succeeded (200) and the row was written.
 */
private const val MEMBER_DASHBOARD_TABLE: String = "member_dashboard_cache"

/**
 * Builds the **dynamic-key** read-only NETWORK_WITH_CACHE [Store] for the personal-dashboard member
 * home (COMP-DASH-001 — `GET /companion/member/dashboard`) that backs the personal-dashboard screen.
 *
 * The key is a `String` cache key = `selectedGroupId ?: `[MEMBER_DASHBOARD_DEFAULT_KEY] and the
 * value is one member-dashboard snapshot [MemberDashboard]; Store5 caches each group independently,
 * so switching the group-selector chip (`OnSelectGroup`) re-uses that group's per-key cache or
 * re-fetches if stale. The read side is exposed to the UI exclusively through
 * `MemberDashboardRepository.memberDashboardStream(...)` → `.asScreenStream(...)` — there is no
 * DAO-bypass read path (RULE-IMPLEMENT-STORE5-001 S5-2), and the dashboard is read-only
 * (`data-flow.yaml#sync_queue: []`) so there is no write path (S5-1).
 *
 * - **Fetcher** — [MemberDashboardApi.getMemberDashboard] with `selectedGroupId` decoded back from
 *   the store key (the [MEMBER_DASHBOARD_DEFAULT_KEY] sentinel → `null`, i.e. "first group"). The
 *   service returns a sealed [NetworkResult]; on [NetworkResult.Success] the DTO is mapped to
 *   domain via [toDomainModel], on [NetworkResult.Error] the fetcher throws so Store5 routes it to
 *   an error response (no try-catch, no `Result` envelope — Store5 owns the error channel).
 * - **SourceOfTruth** — a Room table ([MemberDashboardDao]) so each group's dashboard survives
 *   process death and a cold start with no network still renders the last-seen dashboard for the
 *   selected group (`data-flow.yaml#cache.offline: use_sqldelight`, SC2 — never memory-only). The
 *   whole snapshot is one row per group, so the writer's keyed [MemberDashboardDao.replaceForKey]
 *   upsert is inherently atomic (guards S5-3 — no delete-then-upsert race); it then calls
 *   [DefaultValidator.markFresh] so the TTL window opens on the successful network write (S5-5
 *   cold-start-stale guard).
 * - **Validator** — TTL 5m ([AppStoreRegistry.Ttl.MEMBER_DASHBOARD]) matching `data-flow.yaml`
 *   `ttl_seconds: 300` (stale-while-revalidate).
 *
 * See API.md#stores — MemberDashboard.
 */
fun provideMemberDashboardStore(
    api: MemberDashboardApi,
    dao: MemberDashboardDao,
): Store<String, MemberDashboard> {
    val validator = DefaultValidator.withTtl<MemberDashboard>(
        AppStoreRegistry.Ttl.MEMBER_DASHBOARD,
    )
    return StoreFactory.createStore(
        fetcher = Fetcher.of { key: String ->
            val selectedGroupId = key.toSelectedGroupId()
            when (val result = api.getMemberDashboard(selectedGroupId = selectedGroupId)) {
                is NetworkResult.Success -> result.data.toDomainModel()
                is NetworkResult.Error -> throw MemberDashboardFetchException(result.error)
            }
        },
        sourceOfTruth = SourceOfTruth.of(
            // daoFlow re-queries whenever RoomChangeBus reports a member_dashboard_cache write, so
            // the fetcher's SoT upsert re-fans-out to this open reader (fixing the stuck-Loading gap).
            reader = { key: String ->
                daoFlow(MEMBER_DASHBOARD_TABLE) { dao.observeByKey(key) }.map { row -> row?.toDomain() }
            },
            // notifyingWrite fires the RoomChangeBus signal AFTER a successful upsert so the reader wakes.
            writer = { key: String, dashboard: MemberDashboard ->
                notifyingWrite(MEMBER_DASHBOARD_TABLE) { dao.replaceForKey(dashboard.toEntity(key)) }
                validator.markFresh()
            },
            delete = { key: String ->
                notifyingWrite(MEMBER_DASHBOARD_TABLE) { dao.deleteByKey(key) }
            },
            deleteAll = {
                notifyingWrite(MEMBER_DASHBOARD_TABLE) { dao.deleteAll() }
            },
        ),
        validator = validator,
    )
}

/**
 * Signals a failed member-dashboard fetch to Store5's error channel. Carries the sealed
 * [NetworkError] so downstream error mapping (feature-layer `AppErrorMapper`) can branch on the
 * exact cause (401 → login, 404 → not-in-group, offline → cache fallback per `data-flow.yaml`);
 * the message is `categorize()`-friendly for the default state routing.
 */
class MemberDashboardFetchException(
    val networkError: NetworkError,
) : Exception("Member dashboard fetch failed: $networkError")

// ---------------------------------------------------------------------------
// Inline key <-> selectedGroupId + entity <-> domain mapping — private to this store
// (GroupTypeConfigStore / GroupsPagingStore precedent). core/store depends on core/model +
// core/database, so mapping lives here rather than adding a core/model dependency to
// core/database. The two nested lists round-trip through MemberDashboardCacheCodec (core/database
// owns kotlinx-serialization; core/store does not).
// ---------------------------------------------------------------------------

/** `"__default__"` sentinel → `null` (first group); any other key is the literal `selectedGroupId`. */
private fun String.toSelectedGroupId(): String? =
    if (this == MEMBER_DASHBOARD_DEFAULT_KEY) null else this

private fun MemberDashboard.toEntity(cacheKey: String): MemberDashboardCacheEntity {
    val now = Clock.System.now().toEpochMilliseconds()
    return MemberDashboardCacheEntity(
        cacheKey = cacheKey,
        memberName = memberName,
        clientId = clientId,
        groupLinkedSavingsId = groupLinkedSavingsId,
        individualSavingsId = individualSavingsId,
        selectedGroupId = selectedGroup.groupId,
        selectedGroupName = selectedGroup.name,
        selectedGroupPoolModel = selectedGroup.poolModel.name,
        poolModel = poolModel.name,
        groupLinkedSavingsBalance = groupLinkedSavingsBalance,
        individualSavingsBalance = individualSavingsBalance,
        shareOutProjection = shareOutProjection,
        rotationPosition = rotationPosition,
        nextRecipientEta = nextRecipientEta,
        myGroupsJson = MemberDashboardCacheCodec.encodeGroups(
            myGroups.map { it.toPayload() },
        ),
        recentTransactionsJson = MemberDashboardCacheCodec.encodeTransactions(
            recentTransactions.map { it.toPayload() },
        ),
        fetchedAt = now,
    )
}

private fun MemberDashboardCacheEntity.toDomain(): MemberDashboard = MemberDashboard(
    memberName = memberName,
    clientId = clientId,
    groupLinkedSavingsId = groupLinkedSavingsId,
    individualSavingsId = individualSavingsId,
    myGroups = MemberDashboardCacheCodec.decodeGroups(myGroupsJson).map { it.toDomain() },
    selectedGroup = GroupSummary(
        groupId = selectedGroupId,
        name = selectedGroupName,
        poolModel = selectedGroupPoolModel.toSavingsMechanism(),
    ),
    poolModel = poolModel.toSavingsMechanism(),
    groupLinkedSavingsBalance = groupLinkedSavingsBalance,
    individualSavingsBalance = individualSavingsBalance,
    shareOutProjection = shareOutProjection,
    rotationPosition = rotationPosition,
    nextRecipientEta = nextRecipientEta,
    recentTransactions = MemberDashboardCacheCodec.decodeTransactions(recentTransactionsJson)
        .map { it.toDomain() },
)

private fun GroupSummary.toPayload(): CachedGroupSummary = CachedGroupSummary(
    groupId = groupId,
    name = name,
    poolModel = poolModel.name,
)

private fun CachedGroupSummary.toDomain(): GroupSummary = GroupSummary(
    groupId = groupId,
    name = name,
    poolModel = poolModel.toSavingsMechanism(),
)

private fun SavingsTransaction.toPayload(): CachedSavingsTransaction = CachedSavingsTransaction(
    id = id,
    date = date.toString(),
    type = type.name,
    amount = amount,
)

private fun CachedSavingsTransaction.toDomain(): SavingsTransaction = SavingsTransaction(
    id = id,
    date = LocalDate.parse(date),
    type = type.toTransactionType(),
    amount = amount,
)

private fun String.toSavingsMechanism(): SavingsMechanism =
    SavingsMechanism.entries.firstOrNull { it.name == this } ?: SavingsMechanism.UNKNOWN

private fun String.toTransactionType(): TransactionType =
    TransactionType.entries.firstOrNull { it.name == this } ?: TransactionType.UNKNOWN
