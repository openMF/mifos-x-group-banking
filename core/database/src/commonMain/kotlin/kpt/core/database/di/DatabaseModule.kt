/*
 * Copyright 2024 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.database.di

import kpt.core.base.security.FieldEncryptor
import kpt.core.database.AppDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Marker singleton — its instantiation has the side effect of wiring [FieldEncryptor]
 * into [ChargeTypeConverters] before any database access. Bound with `createdAtStart = true`
 * so the install runs eagerly at Koin start, ahead of the first [AppDatabase] resolution.
 *
 * Room 3 KMP instantiates `@ColumnTypeConverters` classes via no-arg constructor, so the
 * encryptor cannot be passed in by constructor — it's injected post-construction through
 * the [ChargeTypeConverters.install] static method.
 */
private object ChargeTypeConvertersInstalled

/**
 * Koin module that provides the [AppDatabase] instance and all DAO singletons.
 *
 * Delegates platform-specific database construction to [platformModule], which each
 * source set (`androidMain`, `desktopMain`, `nativeMain`, `jsMain`, `wasmJsMain`)
 * implements using the appropriate [AppDatabaseFactory][kpt.core.base.database.AppDatabaseFactory]
 * and SQLite driver.
 */
val DatabaseModule = module {
    includes(platformModule)
    // infra (framework) — always kept
    single { get<AppDatabase>().bookkeeperDao }

    // group-banking domain — group-type-picker seeded catalogue cache (SourceOfTruth DAO)
    single { get<AppDatabase>().groupTypeConfigDao }

    // group-banking domain — group-list paginated cache (SourceOfTruth DAO, COMP-GRP-001)
    single { get<AppDatabase>().groupListDao }

    // group-banking domain — personal-dashboard per-group cache (SourceOfTruth DAO, COMP-DASH-001)
    single { get<AppDatabase>().memberDashboardDao }

    // group-banking domain — group-dashboard composite per-group cache (SourceOfTruth DAO, COMP-GRP-001)
    single { get<AppDatabase>().groupDashboardDao }

    // group-banking domain — member-list paginated per-group cache (SourceOfTruth DAO, GET /groups/{groupId}/clients)
    single { get<AppDatabase>().memberListDao }

    // group-banking domain — member-profile composite per-client cache (SourceOfTruth DAO, get_client + get_client_accounts + get_member_role)
    single { get<AppDatabase>().memberProfileDao }
}

/**
 * Platform-specific Koin module that provides the [AppDatabase] singleton.
 *
 * Each platform actual configures the database builder with the correct
 * [SQLiteDriver][androidx.sqlite.SQLiteDriver] and [CoroutineDispatcher][kotlinx.coroutines.CoroutineDispatcher]:
 * - **Android/Desktop**: [BundledSQLiteDriver][androidx.sqlite.driver.bundled.BundledSQLiteDriver] + `Dispatchers.IO`
 * - **Native (iOS)**: [BundledSQLiteDriver][androidx.sqlite.driver.bundled.BundledSQLiteDriver] + `Dispatchers.Default`
 * - **JS/WasmJS**: SQLiteWeb driver (OPFS-backed) + `Dispatchers.Default`
 */
expect val platformModule: Module
