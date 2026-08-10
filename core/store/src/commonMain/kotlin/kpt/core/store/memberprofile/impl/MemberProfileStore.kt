/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.store.memberprofile.impl

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.map
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.base.store.infra.DefaultValidator
import kpt.core.base.store.infra.StoreFactory
import kpt.core.database.memberprofile.dao.MemberProfileCacheDao
import kpt.core.database.memberprofile.entity.CachedActiveLoanSummary
import kpt.core.database.memberprofile.entity.CachedMemberAccounts
import kpt.core.database.memberprofile.entity.CachedMemberIdentity
import kpt.core.database.memberprofile.entity.CachedMemberProfile
import kpt.core.database.memberprofile.entity.CachedMemberRole
import kpt.core.database.memberprofile.entity.CachedSavingsDataPoint
import kpt.core.database.memberprofile.entity.MemberProfileCacheCodec
import kpt.core.database.memberprofile.entity.MemberProfileCacheEntity
import kpt.core.model.ActiveLoanSummary
import kpt.core.model.MemberAccounts
import kpt.core.model.MemberProfile
import kpt.core.model.MemberProfileDetail
import kpt.core.model.MemberRole
import kpt.core.model.MemberRoleInfo
import kpt.core.model.MemberStatus
import kpt.core.model.SavingsDataPoint
import kpt.core.network.mapper.toDomainModel
import kpt.core.network.mapper.toDomainModels
import kpt.core.network.service.memberprofile.MemberProfileApi
import kpt.core.store.AppStoreRegistry
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import kotlin.time.Clock

/**
 * Builds the **composite dynamic-key** read-only NETWORK_WITH_CACHE [Store] for the member-profile
 * screen — the client-side fan-in of the THREE independent companion reads the screen fires in
 * parallel on mount/refresh/retry ([MemberProfileApi.getClient],
 * [MemberProfileApi.getClientAccounts], [MemberProfileApi.getMemberRole]).
 *
 * The key is the `String` `clientId` and the value is one combined [MemberProfileDetail] snapshot;
 * Store5 caches each client independently (one Room row per client), so opening a different member
 * re-uses that client's per-key cache or re-fetches if stale. The read side is exposed to the UI
 * exclusively through `MemberProfileRepository.memberProfileStream(...)` -> `.asScreenStream(...)`
 * — there is no DAO-bypass read path (RULE-IMPLEMENT-STORE5-001 S5-2). The screen's single write
 * (`update_member_role`) is NOT a Store5 write-back: `data-flow.yaml#cache.strategy: invalidate`
 * declares the mutation invalidates the cache, so `MemberProfileRepository.updateMemberRole(...)`
 * routes the mutation through the store via `store.clear(clientId)` (a Store5 primitive — deletes
 * the in-memory cache AND the SoT row for that key) rather than a silent DAO write (S5-1), and the
 * still-active stream re-fetches the profile with the new role.
 *
 * - **Fetcher** — a PARALLEL-COMBINE: inside a [coroutineScope] the three reads fire as concurrent
 *   [async] coroutines, then `await` fans them into a [MemberProfileDetail] via the shared
 *   `core/network` mappers ([toDomainModel] / [toDomainModels]). Each read returns a sealed
 *   [NetworkResult]; ANY critical read failing ([NetworkResult.Error]) throws
 *   [MemberProfileFetchException] so Store5 routes it to an error response (no try-catch, no
 *   `Result` envelope, no partial persist — Store5 owns the error channel).
 * - **SourceOfTruth** — a Room table ([MemberProfileCacheDao]) so each client's composite survives
 *   process death and a cold start with no network still renders the last-seen profile for that
 *   client (`data-flow.yaml#cache.offline: show_cached`, SC2 — never memory-only). The whole
 *   composite is one row per client, so the writer's keyed [MemberProfileCacheDao.replaceForKey]
 *   upsert is inherently atomic (guards S5-3 — no delete-then-upsert race); it then calls
 *   [DefaultValidator.markFresh] so the TTL window opens on the successful network write (S5-5
 *   cold-start-stale guard).
 * - **Validator** — TTL 5m ([AppStoreRegistry.Ttl.MEMBER_PROFILE]) matching `data-flow.yaml`
 *   `ttl_seconds: 300` (stale-while-revalidate).
 *
 * See API.md#stores — MemberProfile.
 */
fun provideMemberProfileStore(
    api: MemberProfileApi,
    dao: MemberProfileCacheDao,
): Store<String, MemberProfileDetail> {
    val validator = DefaultValidator.withTtl<MemberProfileDetail>(
        AppStoreRegistry.Ttl.MEMBER_PROFILE,
    )
    return StoreFactory.createStore(
        fetcher = Fetcher.of { clientId: String ->
            coroutineScope {
                // Three independent companion reads fired as concurrent coroutines.
                val clientDeferred = async { api.getClient(clientId) }
                val accountsDeferred = async { api.getClientAccounts(clientId) }
                val roleDeferred = async { api.getMemberRole(clientId) }

                // Await + fan-in; any critical read failing throws to Store5's error channel.
                MemberProfileDetail(
                    member = clientDeferred.await().dataOrThrow().toDomainModel(),
                    accounts = accountsDeferred.await().dataOrThrow().toDomainModel(),
                    roles = roleDeferred.await().dataOrThrow().toDomainModels(),
                )
            }
        },
        sourceOfTruth = SourceOfTruth.of(
            reader = { clientId: String ->
                dao.observeByKey(clientId).map { row -> row?.toDomain() }
            },
            writer = { clientId: String, profile: MemberProfileDetail ->
                dao.replaceForKey(profile.toEntity(clientId))
                validator.markFresh()
            },
            delete = { clientId: String -> dao.deleteByKey(clientId) },
            deleteAll = { dao.deleteAll() },
        ),
        validator = validator,
    )
}

/**
 * Signals a failed member-profile composite fetch to Store5's error channel. Carries the sealed
 * [NetworkError] of the FIRST critical read that failed so downstream error mapping (feature-layer
 * `AppErrorMapper`) can branch on the exact cause (401 -> login, 404 -> member-not-found, offline ->
 * cache fallback per `data-flow.yaml`); the message is `categorize()`-friendly for state routing.
 */
class MemberProfileFetchException(
    val networkError: NetworkError,
) : Exception("Member profile fetch failed: $networkError")

/**
 * Unwraps a critical composite read: returns the data on success, or throws
 * [MemberProfileFetchException] on failure so a single failed leg aborts the whole parallel fetch
 * (no partial composite is ever persisted).
 */
private fun <T> NetworkResult<T, NetworkError>.dataOrThrow(): T = when (this) {
    is NetworkResult.Success -> data
    is NetworkResult.Error -> throw MemberProfileFetchException(error)
}

// ---------------------------------------------------------------------------
// Inline composite <-> entity mapping — private to this store (group-dashboard precedent).
// core/store depends on core/model + core/database, so the domain<->payload mapping lives here;
// the whole composite round-trips through MemberProfileCacheCodec (core/database owns
// kotlinx-serialization; core/store does not).
// ---------------------------------------------------------------------------

private fun MemberProfileDetail.toEntity(clientId: String): MemberProfileCacheEntity {
    val now = Clock.System.now().toEpochMilliseconds()
    return MemberProfileCacheEntity(
        clientId = clientId,
        profileJson = MemberProfileCacheCodec.encode(toPayload()),
        fetchedAt = now,
    )
}

private fun MemberProfileDetail.toPayload(): CachedMemberProfile = CachedMemberProfile(
    member = CachedMemberIdentity(
        id = member.id,
        displayName = member.displayName,
        firstName = member.firstName,
        lastName = member.lastName,
        phone = member.phone,
        hasPhoto = member.hasPhoto,
        statusId = member.status.id,
        statusValue = member.status.value,
        joinDate = member.joinDate,
        officeId = member.officeId,
    ),
    accounts = CachedMemberAccounts(
        savingsBalance = accounts.savingsBalance,
        savingsHistory = accounts.savingsHistory.map { CachedSavingsDataPoint(date = it.date, balance = it.balance) },
        activeLoan = accounts.activeLoan?.let {
            CachedActiveLoanSummary(
                id = it.id,
                productName = it.productName,
                outstandingBalance = it.outstandingBalance,
                inArrears = it.inArrears,
                dueDate = it.dueDate,
            )
        },
    ),
    roles = roles.map {
        CachedMemberRole(
            role = it.role.name,
            groupId = it.groupId,
            assignedDate = it.assignedDate,
        )
    },
)

private fun MemberProfileCacheEntity.toDomain(): MemberProfileDetail {
    val payload = MemberProfileCacheCodec.decode(profileJson)
    return MemberProfileDetail(
        member = MemberProfile(
            id = payload.member.id,
            displayName = payload.member.displayName,
            firstName = payload.member.firstName,
            lastName = payload.member.lastName,
            phone = payload.member.phone,
            hasPhoto = payload.member.hasPhoto,
            status = MemberStatus(id = payload.member.statusId, value = payload.member.statusValue),
            joinDate = payload.member.joinDate,
            officeId = payload.member.officeId,
        ),
        accounts = MemberAccounts(
            savingsBalance = payload.accounts.savingsBalance,
            savingsHistory = payload.accounts.savingsHistory.map { SavingsDataPoint(date = it.date, balance = it.balance) },
            activeLoan = payload.accounts.activeLoan?.let {
                ActiveLoanSummary(
                    id = it.id,
                    productName = it.productName,
                    outstandingBalance = it.outstandingBalance,
                    inArrears = it.inArrears,
                    dueDate = it.dueDate,
                )
            },
        ),
        roles = payload.roles.map {
            MemberRoleInfo(
                role = it.role.toMemberRole(),
                groupId = it.groupId,
                assignedDate = it.assignedDate,
            )
        },
    )
}

private fun String.toMemberRole(): MemberRole =
    MemberRole.entries.firstOrNull { it.name == this } ?: MemberRole.UNKNOWN
