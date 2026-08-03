/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.store.grouptypepicker.impl

import kotlinx.coroutines.flow.map
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.base.store.infra.DefaultValidator
import kpt.core.base.store.infra.StoreFactory
import kpt.core.store.AppStoreRegistry
import org.mifos.groupbanking.core.database.grouptypepicker.dao.GroupTypeConfigDao
import org.mifos.groupbanking.core.database.grouptypepicker.entity.GroupTypeConfigEntity
import org.mifos.groupbanking.core.model.ContributionMode
import org.mifos.groupbanking.core.model.GroupTypeConfig
import org.mifos.groupbanking.core.model.GroupTypeSlug
import org.mifos.groupbanking.core.model.SavingsMechanism
import org.mifos.groupbanking.core.network.mapper.toDomainModels
import org.mifos.groupbanking.core.network.service.grouptypepicker.GroupTypeConfigApi
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import kotlin.time.Clock

/**
 * The single logical Store key for the seeded catalogue. COMP-DT-003 with `entityId=0` is a
 * global (not per-group) query, so the whole 9-row catalogue is one keyed value.
 */
const val GROUP_TYPE_CONFIG_CATALOGUE_KEY: String = "catalogue"

/**
 * Builds the read-only NETWORK_WITH_CACHE [Store] for the seeded group-type catalogue
 * (COMP-DT-003) that backs the group-type-picker screen.
 *
 * - **Fetcher** — [GroupTypeConfigApi.getGroupTypeConfigs] with `entityId = 0` (the global seed
 *   query). The service returns a sealed [NetworkResult]; on [NetworkResult.Success] the DTO list
 *   is mapped to domain via [toDomainModels], on [NetworkResult.Error] the fetcher throws so
 *   Store5 routes it to an error response (no try-catch, no `Result` envelope — Store5 owns the
 *   error channel).
 * - **SourceOfTruth** — a Room table ([GroupTypeConfigDao]) so the catalogue survives process
 *   death and a cold start with no network still renders cached cards
 *   (`data-flow.yaml#cache.offline: show_cached`, SC2 — never memory-only). The writer swaps the
 *   whole catalogue atomically via [GroupTypeConfigDao.replaceAll] (single `@Transaction`), then
 *   calls [DefaultValidator.markFresh] so the TTL window opens on the successful network write.
 * - **Validator** — TTL 24h ([AppStoreRegistry.Ttl.GROUP_TYPE_CONFIG]) matching the
 *   `data-flow.yaml` `ttl_seconds: 86400`; catalogue changes only on server deploys.
 *
 * The read side is exposed to the UI exclusively through
 * `GroupTypeConfigRepository.groupTypeConfigsStream(...)` → `.asScreenStream(...)` — there is no
 * DAO-bypass read path (RULE-IMPLEMENT-STORE5-001 S5-2).
 *
 * See API.md#stores — GroupTypeConfig.
 */
fun provideGroupTypeConfigStore(
    api: GroupTypeConfigApi,
    dao: GroupTypeConfigDao,
): Store<String, List<GroupTypeConfig>> {
    val validator = DefaultValidator.withTtl<List<GroupTypeConfig>>(
        AppStoreRegistry.Ttl.GROUP_TYPE_CONFIG,
    )
    return StoreFactory.createStore(
        fetcher = Fetcher.of { _: String ->
            when (val result = api.getGroupTypeConfigs(entityId = 0)) {
                is NetworkResult.Success -> result.data.toDomainModels()
                is NetworkResult.Error -> throw GroupTypeConfigFetchException(result.error)
            }
        },
        sourceOfTruth = SourceOfTruth.of(
            reader = { _: String ->
                dao.observeAll().map { rows ->
                    if (rows.isEmpty()) null else rows.toDomainModels()
                }
            },
            writer = { _: String, configs: List<GroupTypeConfig> ->
                dao.replaceAll(configs.toEntities())
                validator.markFresh()
            },
            delete = { _: String -> dao.deleteAll() },
            deleteAll = { dao.deleteAll() },
        ),
        validator = validator,
    )
}

/**
 * Signals a failed catalogue fetch to Store5's error channel. Carries the sealed
 * [NetworkError] so downstream error mapping (feature-layer `AppErrorMapper`) can branch on the
 * exact cause; the message is `categorize()`-friendly for the default state routing.
 */
class GroupTypeConfigFetchException(
    val networkError: NetworkError,
) : Exception("Group type catalogue fetch failed: $networkError")

// ---------------------------------------------------------------------------
// Inline entity <-> domain mapping — private to this store (InterestRateSeriesStore precedent).
// core/store depends on core/model + core/database, so mapping lives here rather than adding a
// core/model dependency to core/database.
// ---------------------------------------------------------------------------

private fun List<GroupTypeConfigEntity>.toDomainModels(): List<GroupTypeConfig> = map { it.toDomain() }

private fun GroupTypeConfigEntity.toDomain(): GroupTypeConfig = GroupTypeConfig(
    typeSlug = typeSlug.toGroupTypeSlug(),
    displayName = displayName,
    tagline = tagline,
    savingsMechanism = savingsMechanism.toSavingsMechanism(),
    contributionMode = contributionMode.toContributionMode(),
    lendingEnabled = lendingEnabled,
    hasSocialFund = hasSocialFund,
    hasBankLinkage = hasBankLinkage,
    welfareOnlyMode = welfareOnlyMode,
    formallyRegistered = formallyRegistered,
    defaultLoanMultiplier = defaultLoanMultiplier,
    defaultInterestRatePct = defaultInterestRatePct,
    defaultCycleLengthMonths = defaultCycleLengthMonths,
    maxMembers = maxMembers,
    minMembers = minMembers,
)

private fun List<GroupTypeConfig>.toEntities(): List<GroupTypeConfigEntity> {
    val now = Clock.System.now().toEpochMilliseconds()
    return map { it.toEntity(now) }
}

private fun GroupTypeConfig.toEntity(fetchedAt: Long): GroupTypeConfigEntity = GroupTypeConfigEntity(
    typeSlug = typeSlug.name,
    displayName = displayName,
    tagline = tagline,
    savingsMechanism = savingsMechanism.name,
    contributionMode = contributionMode.name,
    lendingEnabled = lendingEnabled,
    hasSocialFund = hasSocialFund,
    hasBankLinkage = hasBankLinkage,
    welfareOnlyMode = welfareOnlyMode,
    formallyRegistered = formallyRegistered,
    defaultLoanMultiplier = defaultLoanMultiplier,
    defaultInterestRatePct = defaultInterestRatePct,
    defaultCycleLengthMonths = defaultCycleLengthMonths,
    maxMembers = maxMembers,
    minMembers = minMembers,
    fetchedAt = fetchedAt,
)

private fun String.toGroupTypeSlug(): GroupTypeSlug =
    GroupTypeSlug.entries.firstOrNull { it.name == this } ?: GroupTypeSlug.UNKNOWN

private fun String.toSavingsMechanism(): SavingsMechanism =
    SavingsMechanism.entries.firstOrNull { it.name == this } ?: SavingsMechanism.UNKNOWN

private fun String.toContributionMode(): ContributionMode =
    ContributionMode.entries.firstOrNull { it.name == this } ?: ContributionMode.UNKNOWN
