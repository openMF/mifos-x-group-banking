/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
<<<<<<<< HEAD:core/database/src/commonTest/kotlin/org/mifos/open/banking/core/database/di/TestDatabaseModule.kt
package org.mifos.open.banking.core.database.di

import org.koin.core.module.Module
import org.koin.dsl.module
import org.mifos.open.banking.core.database.AppDatabase
========
package org.mifos.groupbanking.groupbanking.core.database.di

import org.koin.core.module.Module
import org.koin.dsl.module
import org.mifos.groupbanking.groupbanking.core.database.AppDatabase
>>>>>>>> c3e419f (feat(scaffold): customise kmp-project-template for group-banking (CommonPurse)):core/database/src/commonTest/kotlin/org/mifos/groupbanking/core/database/di/TestDatabaseModule.kt

val TestDatabaseModule = module {
    includes(testPlatformModule)
    single { get<AppDatabase>().sampleDao }
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect val testPlatformModule: Module
