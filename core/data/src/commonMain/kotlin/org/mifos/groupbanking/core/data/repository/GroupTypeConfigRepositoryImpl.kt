/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.data.repository

import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kpt.core.base.store.infra.FetchedAtRepository
import kpt.core.base.store.screen.FetchPolicy
import kpt.core.base.store.screen.ScreenDataStream
import kpt.core.base.store.screen.asScreenStream
import kpt.core.store.AppStoreRegistry
import org.mifos.groupbanking.core.model.GroupTypeConfig
import org.mifos.groupbanking.core.store.grouptypepicker.impl.GROUP_TYPE_CONFIG_CATALOGUE_KEY
import org.mobilenativefoundation.store.store5.Store

/**
 * Store5-backed implementation of [GroupTypeConfigRepository].
 *
 * The single seed catalogue is one logical key ([GROUP_TYPE_CONFIG_CATALOGUE_KEY]); the read
 * goes exclusively through [Store.asScreenStream] so the whole offline-first pipeline (cached
 * emit → background revalidate → DecisionEngine → ScreenState) is inherited from `core-base`.
 * No DAO-bypass read, no `try-catch`, no `Result` envelope (RULE-IMPLEMENT-STORE5-001 S5-2).
 *
 * See API.md#stores — GroupTypeConfig.
 */
class GroupTypeConfigRepositoryImpl(
    private val groupTypeConfigStore: Store<String, List<GroupTypeConfig>>,
    private val networkMonitor: NetworkMonitor,
    private val fetchedAtRepository: FetchedAtRepository,
) : GroupTypeConfigRepository {

    override fun groupTypeConfigsStream(
        scope: CoroutineScope,
        fetchPolicy: FetchPolicy,
    ): ScreenDataStream<List<GroupTypeConfig>> = groupTypeConfigStore.asScreenStream(
        key = GROUP_TYPE_CONFIG_CATALOGUE_KEY,
        networkMonitor = networkMonitor,
        fetchedAtRepository = fetchedAtRepository,
        cacheKey = CACHE_KEY,
        scope = scope,
        isEmpty = { it.isEmpty() },
        fetchPolicy = fetchPolicy,
        ttl = AppStoreRegistry.Ttl.GROUP_TYPE_CONFIG,
    )

    private companion object {
        /** FetchedAtRepository key — one freshness timestamp for the whole catalogue. */
        const val CACHE_KEY = "grouptypepicker:groupTypeConfig:catalogue"
    }
}
