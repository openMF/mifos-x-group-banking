/*
 * Copyright 2024 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.data.di

import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitorProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kpt.core.base.common.di.CommonModule
import kpt.core.base.store.infra.FetchedAtRepository
import kpt.core.data.infra.NetworkMonitor
import kpt.core.data.infra.impl.RoomFetchedAtRepository
import kpt.core.data.user.UserDataRepository
import kpt.core.data.user.UserLogoutManager
import kpt.core.data.user.impl.UserDataRepositoryImpl
import kpt.core.data.user.impl.UserLogoutManagerImpl
import kpt.core.database.AppDatabase
import kpt.core.database.di.DatabaseModule
import kpt.core.datastore.di.DatastoreModule
import kpt.core.network.di.NetworkModule
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import kpt.core.store.AppStoreRegistry
import org.mifos.groupbanking.core.data.repository.AuthRepository
import org.mifos.groupbanking.core.data.repository.AuthRepositoryImpl
import org.mifos.groupbanking.core.data.repository.GroupTypeConfigRepository
import org.mifos.groupbanking.core.data.repository.GroupTypeConfigRepositoryImpl

val DataModule = module {
    includes(platformModule, CommonModule, DatabaseModule, DatastoreModule, NetworkModule)

    single<NetworkMonitor> { NetworkMonitorProvider.install() }
    singleOf(::UserDataRepositoryImpl) bind UserDataRepository::class

    // login-signup client stack (COMP-AUTH-001/002/003) — Store5-free (business_logic.kind:
    // processor), wraps CompanionAuthApi (NetworkModule) + CompanionSessionStore (DatastoreModule).
    single<AuthRepository> { AuthRepositoryImpl(api = get(), sessionStore = get()) }

    // group-type-picker seeded catalogue (COMP-DT-003) — wraps the NETWORK_WITH_CACHE
    // GroupTypeConfigStore (bound via AppStoreRegistry.GroupTypeConfig in appStoreModule) and
    // surfaces the offline-first .asScreenStream() read.
    single<GroupTypeConfigRepository> {
        GroupTypeConfigRepositoryImpl(
            groupTypeConfigStore = get(AppStoreRegistry.GroupTypeConfig),
            networkMonitor = get(),
            fetchedAtRepository = get(),
        )
    }

    // Framework FetchedAtRepository — durable lastFetchedAt persistence backing
    // DataFreshnessIndicator timestamps. Room-only by design (no in-memory fallback).
    single<FetchedAtRepository> { RoomFetchedAtRepository(get<AppDatabase>().fetchedAtDao) }

    // Framework DraftDao — backing store for SubmitOutbox / DraftSubmitHandler
    single { get<AppDatabase>().draftDao }

    // App-scoped CoroutineScope for cross-VM long-running coroutines (framework infra).
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    single<UserLogoutManager> { UserLogoutManagerImpl(get(), get(), get()) }
}

expect val platformModule: Module
