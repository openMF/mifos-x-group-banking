/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
<<<<<<<< HEAD:core/database/src/androidMain/kotlin/org/mifos/open/banking/core/database/di/DatabaseModule.android.kt
package org.mifos.open.banking.core.database.di
========
package org.mifos.groupbanking.groupbanking.core.database.di
>>>>>>>> c3e419f (feat(scaffold): customise kmp-project-template for group-banking (CommonPurse)):core/database/src/androidMain/kotlin/org/mifos/groupbanking/core/database/di/DatabaseModule.android.kt

import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.Module
import org.koin.dsl.module
<<<<<<<< HEAD:core/database/src/androidMain/kotlin/org/mifos/open/banking/core/database/di/DatabaseModule.android.kt
import org.mifos.open.banking.core.database.AppDatabase
========
import org.mifos.groupbanking.groupbanking.core.database.AppDatabase
import org.mifos.groupbanking.groupbanking.core.database.utils.ChargeTypeConverters
>>>>>>>> c3e419f (feat(scaffold): customise kmp-project-template for group-banking (CommonPurse)):core/database/src/androidMain/kotlin/org/mifos/groupbanking/core/database/di/DatabaseModule.android.kt
import template.core.base.database.AppDatabaseFactory
import kotlin.coroutines.CoroutineContext

actual val platformModule: Module = module {
    single {
        AppDatabaseFactory(
            androidApplication(),
        )
            .createDatabase(
                databaseClass = AppDatabase::class.java,
                databaseName = AppDatabase.DATABASE_NAME,
            )
            .fallbackToDestructiveMigrationOnDowngrade(false)
            .setQueryCoroutineContext(Dispatchers.IO as CoroutineContext)
            .build()
    }
}
