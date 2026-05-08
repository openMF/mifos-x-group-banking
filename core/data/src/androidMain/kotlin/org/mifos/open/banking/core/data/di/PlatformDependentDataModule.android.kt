/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
<<<<<<<< HEAD:core/data/src/androidMain/kotlin/org/mifos/open/banking/core/data/di/PlatformDependentDataModule.android.kt
package org.mifos.open.banking.core.data.di
========
package org.mifos.groupbanking.groupbanking.core.data.di
>>>>>>>> c3e419f (feat(scaffold): customise kmp-project-template for group-banking (CommonPurse)):core/data/src/androidMain/kotlin/org/mifos/groupbanking/core/data/di/PlatformDependentDataModule.android.kt

import android.content.Context
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
<<<<<<<< HEAD:core/data/src/androidMain/kotlin/org/mifos/open/banking/core/data/di/PlatformDependentDataModule.android.kt
import org.mifos.open.banking.core.data.repository.TimeZoneMonitor
import org.mifos.open.banking.core.data.repository.TimeZoneMonitorImpl
========
import org.mifos.groupbanking.groupbanking.core.data.repository.TimeZoneMonitor
import org.mifos.groupbanking.groupbanking.core.data.repository.TimeZoneMonitorImpl
>>>>>>>> c3e419f (feat(scaffold): customise kmp-project-template for group-banking (CommonPurse)):core/data/src/androidMain/kotlin/org/mifos/groupbanking/core/data/di/PlatformDependentDataModule.android.kt
import template.core.base.common.di.CommonModule

actual val platformModule: Module = module {
    includes(CommonModule)

    single<Context> { androidContext() }

    singleOf(::TimeZoneMonitorImpl) bind TimeZoneMonitor::class
}
