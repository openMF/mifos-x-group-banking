/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.store.di

import kpt.core.store.AppStoreRegistry
import kpt.core.store.infra.StoreCacheManager
import kpt.core.store.infra.impl.StoreCacheManagerImpl
import org.koin.core.module.Module
import org.koin.dsl.module
import org.mifos.groupbanking.core.store.groupdashboard.impl.provideGroupDashboardStore
import org.mifos.groupbanking.core.store.grouplist.impl.provideGroupsPagingStore
import org.mifos.groupbanking.core.store.grouptypepicker.impl.provideGroupTypeConfigStore
import org.mifos.groupbanking.core.store.memberlist.impl.provideMembersPagingStore
import org.mifos.groupbanking.core.store.memberprofile.impl.provideMemberProfileStore
import org.mifos.groupbanking.core.store.personaldashboard.impl.provideMemberDashboardStore

/**
 * Koin module for app-level Store wiring.
 *
 * Forks register their `Store` instances here, qualifier-bound via [AppStoreRegistry].
 * The 4 demo stores ship as forkable examples — add your own `single(qualifier = ...)`
 * blocks next to them.
 *
 * Wire into the Koin start-up:
 * ```kotlin
 * startKoin {
 *     modules(appStoreModule, /* ...other modules */)
 * }
 * ```
 */
val appStoreModule: Module = module {
    // Store cache manager — clears all registered caches on logout (registration-based)
    single<StoreCacheManager> {
        StoreCacheManagerImpl(
            bookkeeperDao = get(),
            draftDao = get(),
        )
    }

    // MODULE:GroupTypeConfig — group-type-picker seeded catalogue store (NETWORK_WITH_CACHE).
    // Internal to the store seam — exposed to UI only through GroupTypeConfigRepository.
    single(AppStoreRegistry.GroupTypeConfig) {
        provideGroupTypeConfigStore(api = get(), dao = get())
    }

    // MODULE:GroupTypeConfig — register for logout cache clearing (clears in-memory + Room SoT).
    single(createdAtStart = true) {
        val mgr = get<StoreCacheManager>() as StoreCacheManagerImpl
        mgr.register(get(AppStoreRegistry.GroupTypeConfig))
    }

    // MODULE:GroupList — group-list PAGINATED store (NETWORK_WITH_CACHE, COMP-GRP-001).
    // Internal to the store seam — exposed to UI only through GroupRepository.asPagingScreenStream().
    single(AppStoreRegistry.GroupList) {
        provideGroupsPagingStore(api = get(), dao = get())
    }

    // MODULE:GroupList — register for logout cache clearing (clears in-memory + Room SoT pages).
    single(createdAtStart = true) {
        val mgr = get<StoreCacheManager>() as StoreCacheManagerImpl
        mgr.register(get(AppStoreRegistry.GroupList))
    }

    // MODULE:MemberDashboard — personal-dashboard member home store (dynamic-key NETWORK_WITH_CACHE).
    // Internal to the store seam — exposed to UI only through MemberDashboardRepository.asScreenStream().
    single(AppStoreRegistry.MemberDashboard) {
        provideMemberDashboardStore(api = get(), dao = get())
    }

    // MODULE:MemberDashboard — register for logout cache clearing (clears in-memory + Room SoT rows).
    single(createdAtStart = true) {
        val mgr = get<StoreCacheManager>() as StoreCacheManagerImpl
        mgr.register(get(AppStoreRegistry.MemberDashboard))
    }

    // MODULE:GroupDashboard — group-dashboard COMPOSITE store (dynamic-key NETWORK_WITH_CACHE, COMP-GRP-001).
    // Internal to the store seam — exposed to UI only through GroupDashboardRepository.asScreenStream().
    single(AppStoreRegistry.GroupDashboard) {
        provideGroupDashboardStore(api = get(), dao = get())
    }

    // MODULE:GroupDashboard — register for logout cache clearing (clears in-memory + Room SoT rows).
    single(createdAtStart = true) {
        val mgr = get<StoreCacheManager>() as StoreCacheManagerImpl
        mgr.register(get(AppStoreRegistry.GroupDashboard))
    }

    // MODULE:MemberList — member-list PAGINATED store (NETWORK_WITH_CACHE, GET /groups/{groupId}/clients).
    // Internal to the store seam — exposed to UI only through MemberRepository.asPagingScreenStream().
    single(AppStoreRegistry.MemberList) {
        provideMembersPagingStore(api = get(), dao = get())
    }

    // MODULE:MemberList — register for logout cache clearing (clears in-memory + Room SoT pages).
    single(createdAtStart = true) {
        val mgr = get<StoreCacheManager>() as StoreCacheManagerImpl
        mgr.register(get(AppStoreRegistry.MemberList))
    }

    // MODULE:MemberProfile — member-profile COMPOSITE store (dynamic-key NETWORK_WITH_CACHE,
    // get_client + get_client_accounts + get_member_role). Internal to the store seam — exposed to
    // UI only through MemberProfileRepository.asScreenStream(); the update_member_role write
    // invalidates via store.clear(clientId) in MemberProfileRepositoryImpl.
    single(AppStoreRegistry.MemberProfile) {
        provideMemberProfileStore(api = get(), dao = get())
    }

    // MODULE:MemberProfile — register for logout cache clearing (clears in-memory + Room SoT rows).
    single(createdAtStart = true) {
        val mgr = get<StoreCacheManager>() as StoreCacheManagerImpl
        mgr.register(get(AppStoreRegistry.MemberProfile))
    }
}
