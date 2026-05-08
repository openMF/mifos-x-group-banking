/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
<<<<<<<< HEAD:core/database/src/nativeMain/kotlin/org/mifos/open/banking/core/database/di/DatabaseModule.native.kt
package org.mifos.open.banking.core.database.di
========
package org.mifos.groupbanking.groupbanking.core.database.di
>>>>>>>> c3e419f (feat(scaffold): customise kmp-project-template for group-banking (CommonPurse)):core/database/src/nativeMain/kotlin/org/mifos/groupbanking/core/database/di/DatabaseModule.native.kt

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
<<<<<<<< HEAD:core/database/src/nativeMain/kotlin/org/mifos/open/banking/core/database/di/DatabaseModule.native.kt
import org.mifos.open.banking.core.common.di.AppDispatchers
import org.mifos.open.banking.core.database.AppDatabase
========
import org.mifos.groupbanking.groupbanking.core.database.AppDatabase
import org.mifos.groupbanking.groupbanking.core.database.utils.ChargeTypeConverters
>>>>>>>> c3e419f (feat(scaffold): customise kmp-project-template for group-banking (CommonPurse)):core/database/src/nativeMain/kotlin/org/mifos/groupbanking/core/database/di/DatabaseModule.native.kt
import template.core.base.database.AppDatabaseFactory
import kotlin.coroutines.CoroutineContext

actual val platformModule: Module = module {
    single {
        AppDatabaseFactory()
            .createDatabase<AppDatabase>(
                databaseName = AppDatabase.DATABASE_NAME,
            )
            .fallbackToDestructiveMigrationOnDowngrade(false)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(get<CoroutineDispatcher>(named(AppDispatchers.IO.name)) as CoroutineContext)
            .build()
    }
}
