/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.store.groupdashboard.impl

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.map
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.base.store.infra.DefaultValidator
import kpt.core.base.store.infra.StoreFactory
import kpt.core.store.AppStoreRegistry
import org.mifos.groupbanking.core.database.groupdashboard.dao.GroupDashboardDao
import org.mifos.groupbanking.core.database.groupdashboard.entity.CachedActivityItem
import org.mifos.groupbanking.core.database.groupdashboard.entity.CachedGroupAccounts
import org.mifos.groupbanking.core.database.groupdashboard.entity.CachedGroupCorpus
import org.mifos.groupbanking.core.database.groupdashboard.entity.CachedGroupDashboard
import org.mifos.groupbanking.core.database.groupdashboard.entity.CachedGroupDetail
import org.mifos.groupbanking.core.database.groupdashboard.entity.CachedGroupInstanceConfig
import org.mifos.groupbanking.core.database.groupdashboard.entity.CachedViewerRoleInfo
import org.mifos.groupbanking.core.database.groupdashboard.entity.GroupDashboardCacheCodec
import org.mifos.groupbanking.core.database.groupdashboard.entity.GroupDashboardCacheEntity
import org.mifos.groupbanking.core.model.ActivityItem
import org.mifos.groupbanking.core.model.ActivityType
import org.mifos.groupbanking.core.model.GroupAccounts
import org.mifos.groupbanking.core.model.GroupContributionModel
import org.mifos.groupbanking.core.model.GroupCorpus
import org.mifos.groupbanking.core.model.GroupDashboard
import org.mifos.groupbanking.core.model.GroupDetail
import org.mifos.groupbanking.core.model.GroupInstanceConfig
import org.mifos.groupbanking.core.model.GroupTypeSlug
import org.mifos.groupbanking.core.model.SavingsMechanism
import org.mifos.groupbanking.core.model.ViewerRole
import org.mifos.groupbanking.core.model.ViewerRoleInfo
import org.mifos.groupbanking.core.network.mapper.toDomainModel
import org.mifos.groupbanking.core.network.model.GroupDashboardResponseDto
import org.mifos.groupbanking.core.network.service.groupdashboard.GroupDashboardApi
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import kotlin.time.Clock

/**
 * Builds the **composite dynamic-key** read-only NETWORK_WITH_CACHE [Store] for the group-dashboard
 * screen (COMP-GRP-001) — the client-side fan-in of the FOUR independent companion reads the screen
 * fires in parallel on mount/refresh/retry ([GroupDashboardApi.getGroup], [getViewerRole]
 * /[GroupDashboardApi.getViewerRole], [GroupDashboardApi.getGroupCorpus],
 * [GroupDashboardApi.getGroupAccounts]).
 *
 * The key is the `String` `groupId` and the value is one combined [GroupDashboard] snapshot; Store5
 * caches each group independently (one Room row per group), so opening a different group re-uses
 * that group's per-key cache or re-fetches if stale. The read side is exposed to the UI exclusively
 * through `GroupDashboardRepository.groupDashboardStream(...)` → `.asScreenStream(...)` — there is
 * no DAO-bypass read path (RULE-IMPLEMENT-STORE5-001 S5-2), and the dashboard is read-only
 * (`data-flow.yaml#sync_queue: []`) so there is no write path (S5-1).
 *
 * - **Fetcher** — a PARALLEL-COMBINE: inside a [coroutineScope] the four reads fire as concurrent
 *   [async] coroutines, then `await` fans them into a [GroupDashboardResponseDto] which the shared
 *   `GroupDashboardResponseDto.`[toDomainModel] mapper folds into the domain composite. Each read
 *   returns a sealed [NetworkResult]; ANY critical read failing ([NetworkResult.Error]) throws
 *   [GroupDashboardFetchException] so Store5 routes it to an error response (no try-catch, no
 *   `Result` envelope, no partial persist — Store5 owns the error channel).
 * - **SourceOfTruth** — a Room table ([GroupDashboardDao]) so each group's composite survives
 *   process death and a cold start with no network still renders the last-seen dashboard for that
 *   group (`data-flow.yaml#cache.offline: fallback_cache`, SC2 — never memory-only). The whole
 *   composite is one row per group, so the writer's keyed [GroupDashboardDao.replaceForKey] upsert
 *   is inherently atomic (guards S5-3 — no delete-then-upsert race); it then calls
 *   [DefaultValidator.markFresh] so the TTL window opens on the successful network write (S5-5
 *   cold-start-stale guard).
 * - **Validator** — TTL 5m ([AppStoreRegistry.Ttl.GROUP_DASHBOARD]) matching `data-flow.yaml`
 *   `ttl_seconds: 300` (stale-while-revalidate).
 *
 * See API.md#stores — GroupDashboard.
 */
fun provideGroupDashboardStore(
    api: GroupDashboardApi,
    dao: GroupDashboardDao,
): Store<String, GroupDashboard> {
    val validator = DefaultValidator.withTtl<GroupDashboard>(
        AppStoreRegistry.Ttl.GROUP_DASHBOARD,
    )
    return StoreFactory.createStore(
        fetcher = Fetcher.of { groupId: String ->
            coroutineScope {
                // COMP-GRP-001 — four independent companion reads fired as concurrent coroutines.
                val groupDeferred = async { api.getGroup(groupId) }
                val viewerRoleDeferred = async { api.getViewerRole(groupId) }
                val corpusDeferred = async { api.getGroupCorpus(groupId) }
                val accountsDeferred = async { api.getGroupAccounts(groupId) }

                // Await + fan-in; any critical read failing throws to Store5's error channel.
                GroupDashboardResponseDto(
                    group = groupDeferred.await().dataOrThrow(),
                    viewerRole = viewerRoleDeferred.await().dataOrThrow(),
                    corpus = corpusDeferred.await().dataOrThrow(),
                    accounts = accountsDeferred.await().dataOrThrow(),
                ).toDomainModel()
            }
        },
        sourceOfTruth = SourceOfTruth.of(
            reader = { groupId: String ->
                dao.observeByKey(groupId).map { row -> row?.toDomain() }
            },
            writer = { groupId: String, dashboard: GroupDashboard ->
                dao.replaceForKey(dashboard.toEntity(groupId))
                validator.markFresh()
            },
            delete = { groupId: String -> dao.deleteByKey(groupId) },
            deleteAll = { dao.deleteAll() },
        ),
        validator = validator,
    )
}

/**
 * Signals a failed group-dashboard composite fetch to Store5's error channel. Carries the sealed
 * [NetworkError] of the FIRST critical read that failed so downstream error mapping (feature-layer
 * `AppErrorMapper`) can branch on the exact cause (401 -> login, 404 -> group-not-found, offline ->
 * cache fallback per `data-flow.yaml`); the message is `categorize()`-friendly for state routing.
 */
class GroupDashboardFetchException(
    val networkError: NetworkError,
) : Exception("Group dashboard fetch failed: $networkError")

/**
 * Unwraps a critical composite read: returns the data on success, or throws
 * [GroupDashboardFetchException] on failure so a single failed leg aborts the whole parallel fetch
 * (no partial composite is ever persisted).
 */
private fun <T> NetworkResult<T, NetworkError>.dataOrThrow(): T = when (this) {
    is NetworkResult.Success -> data
    is NetworkResult.Error -> throw GroupDashboardFetchException(error)
}

// ---------------------------------------------------------------------------
// Inline composite <-> entity mapping — private to this store (personal-dashboard precedent).
// core/store depends on core/model + core/database, so the domain<->payload mapping lives here;
// the whole composite round-trips through GroupDashboardCacheCodec (core/database owns
// kotlinx-serialization; core/store does not).
// ---------------------------------------------------------------------------

private fun GroupDashboard.toEntity(groupId: String): GroupDashboardCacheEntity {
    val now = Clock.System.now().toEpochMilliseconds()
    return GroupDashboardCacheEntity(
        groupId = groupId,
        dashboardJson = GroupDashboardCacheCodec.encode(toPayload()),
        fetchedAt = now,
    )
}

private fun GroupDashboard.toPayload(): CachedGroupDashboard = CachedGroupDashboard(
    group = CachedGroupDetail(
        id = group.id,
        fineractCenterId = group.fineractCenterId,
        name = group.name,
        cycleNumber = group.cycleNumber,
        cycleLengthMonths = group.cycleLengthMonths,
        meetingFrequency = group.meetingFrequency,
        memberCount = group.memberCount,
        overdueLoansCount = group.overdueLoansCount,
        status = group.status,
        typeConfig = CachedGroupInstanceConfig(
            groupType = group.typeConfig.groupType.name,
            poolModel = group.typeConfig.poolModel.name,
            contributionModel = group.typeConfig.contributionModel.name,
            shareoutFormula = group.typeConfig.shareoutFormula,
            payoutOrderMethod = group.typeConfig.payoutOrderMethod,
            shareValue = group.typeConfig.shareValue,
            contributionAmount = group.typeConfig.contributionAmount,
            socialFundEnabled = group.typeConfig.socialFundEnabled,
            cycleLengthMonths = group.typeConfig.cycleLengthMonths,
            loanMultiplier = group.typeConfig.loanMultiplier,
            interestRate = group.typeConfig.interestRate,
            fineAmount = group.typeConfig.fineAmount,
        ),
    ),
    viewerRole = CachedViewerRoleInfo(
        role = viewerRole.role.name,
        memberId = viewerRole.memberId,
    ),
    corpus = CachedGroupCorpus(
        currentBalance = corpus.currentBalance,
        openingBalance = corpus.openingBalance,
        totalContributionsThisCycle = corpus.totalContributionsThisCycle,
        totalLoansOutstanding = corpus.totalLoansOutstanding,
        lastUpdated = corpus.lastUpdated,
        isCycleEnd = corpus.isCycleEnd,
        rotationPosition = corpus.rotationPosition,
        nextRecipientName = corpus.nextRecipientName,
        nextRecipientPosition = corpus.nextRecipientPosition,
    ),
    accounts = CachedGroupAccounts(
        savingsBalance = accounts.savingsBalance,
        loansOutstanding = accounts.loansOutstanding,
        activeLoanCount = accounts.activeLoanCount,
        shareOutProjection = accounts.shareOutProjection,
        recentActivity = accounts.recentActivity.map { it.toPayload() },
    ),
)

private fun ActivityItem.toPayload(): CachedActivityItem = CachedActivityItem(
    id = id,
    type = type.name,
    description = description,
    amount = amount,
    date = date,
    memberName = memberName,
)

private fun GroupDashboardCacheEntity.toDomain(): GroupDashboard {
    val payload = GroupDashboardCacheCodec.decode(dashboardJson)
    return GroupDashboard(
        group = GroupDetail(
            id = payload.group.id,
            fineractCenterId = payload.group.fineractCenterId,
            name = payload.group.name,
            cycleNumber = payload.group.cycleNumber,
            cycleLengthMonths = payload.group.cycleLengthMonths,
            meetingFrequency = payload.group.meetingFrequency,
            memberCount = payload.group.memberCount,
            overdueLoansCount = payload.group.overdueLoansCount,
            status = payload.group.status,
            typeConfig = GroupInstanceConfig(
                groupType = payload.group.typeConfig.groupType.toGroupTypeSlug(),
                poolModel = payload.group.typeConfig.poolModel.toSavingsMechanism(),
                contributionModel = payload.group.typeConfig.contributionModel.toGroupContributionModel(),
                shareoutFormula = payload.group.typeConfig.shareoutFormula,
                payoutOrderMethod = payload.group.typeConfig.payoutOrderMethod,
                shareValue = payload.group.typeConfig.shareValue,
                contributionAmount = payload.group.typeConfig.contributionAmount,
                socialFundEnabled = payload.group.typeConfig.socialFundEnabled,
                cycleLengthMonths = payload.group.typeConfig.cycleLengthMonths,
                loanMultiplier = payload.group.typeConfig.loanMultiplier,
                interestRate = payload.group.typeConfig.interestRate,
                fineAmount = payload.group.typeConfig.fineAmount,
            ),
        ),
        viewerRole = ViewerRoleInfo(
            role = payload.viewerRole.role.toViewerRole(),
            memberId = payload.viewerRole.memberId,
        ),
        corpus = GroupCorpus(
            currentBalance = payload.corpus.currentBalance,
            openingBalance = payload.corpus.openingBalance,
            totalContributionsThisCycle = payload.corpus.totalContributionsThisCycle,
            totalLoansOutstanding = payload.corpus.totalLoansOutstanding,
            lastUpdated = payload.corpus.lastUpdated,
            isCycleEnd = payload.corpus.isCycleEnd,
            rotationPosition = payload.corpus.rotationPosition,
            nextRecipientName = payload.corpus.nextRecipientName,
            nextRecipientPosition = payload.corpus.nextRecipientPosition,
        ),
        accounts = GroupAccounts(
            savingsBalance = payload.accounts.savingsBalance,
            loansOutstanding = payload.accounts.loansOutstanding,
            activeLoanCount = payload.accounts.activeLoanCount,
            shareOutProjection = payload.accounts.shareOutProjection,
            recentActivity = payload.accounts.recentActivity.map { it.toDomain() },
        ),
    )
}

private fun CachedActivityItem.toDomain(): ActivityItem = ActivityItem(
    id = id,
    type = type.toActivityType(),
    description = description,
    amount = amount,
    date = date,
    memberName = memberName,
)

private fun String.toGroupTypeSlug(): GroupTypeSlug =
    GroupTypeSlug.entries.firstOrNull { it.name == this } ?: GroupTypeSlug.UNKNOWN

private fun String.toSavingsMechanism(): SavingsMechanism =
    SavingsMechanism.entries.firstOrNull { it.name == this } ?: SavingsMechanism.UNKNOWN

private fun String.toGroupContributionModel(): GroupContributionModel =
    GroupContributionModel.entries.firstOrNull { it.name == this } ?: GroupContributionModel.UNKNOWN

private fun String.toViewerRole(): ViewerRole =
    ViewerRole.entries.firstOrNull { it.name == this } ?: ViewerRole.UNKNOWN

private fun String.toActivityType(): ActivityType =
    ActivityType.entries.firstOrNull { it.name == this } ?: ActivityType.UNKNOWN
