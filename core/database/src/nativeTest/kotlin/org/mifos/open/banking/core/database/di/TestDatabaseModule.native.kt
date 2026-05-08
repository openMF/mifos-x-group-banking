/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
<<<<<<<< HEAD:core/database/src/nativeTest/kotlin/org/mifos/open/banking/core/database/di/TestDatabaseModule.native.kt
package org.mifos.open.banking.core.database.di
========
package org.mifos.groupbanking.core.database.di
>>>>>>>> c3e419f (feat(scaffold): customise kmp-project-template for group-banking (CommonPurse)):core/database/src/nativeTest/kotlin/org/mifos/groupbanking/core/database/di/TestDatabaseModule.native.kt

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
<<<<<<<< HEAD:core/database/src/nativeTest/kotlin/org/mifos/open/banking/core/database/di/TestDatabaseModule.native.kt
import org.mifos.open.banking.core.database.AppDatabase
import kotlin.coroutines.CoroutineContext
========
import org.mifos.groupbanking.core.database.AppDatabase
>>>>>>>> c3e419f (feat(scaffold): customise kmp-project-template for group-banking (CommonPurse)):core/database/src/nativeTest/kotlin/org/mifos/groupbanking/core/database/di/TestDatabaseModule.native.kt

actual val testPlatformModule: Module = module {
    factory<AppDatabase> {
        Room.inMemoryDatabaseBuilder<AppDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(get<CoroutineDispatcher>(named(AppDispatchers.IO.name)) as CoroutineContext)
            .build()
    }
}
