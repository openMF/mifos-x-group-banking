/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
<<<<<<<< HEAD:core/database/src/commonMain/kotlin/org/mifos/open/banking/core/database/AppDatabase.kt
package org.mifos.open.banking.core.database

import org.mifos.open.banking.core.database.dao.SampleDao
========
package org.mifos.groupbanking.groupbanking.feature.emicalculator.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.mifos.groupbanking.groupbanking.feature.emicalculator.ui.EmiCalculatorViewModel
>>>>>>>> c3e419f (feat(scaffold): customise kmp-project-template for group-banking (CommonPurse)):feature/emi-calculator/src/commonMain/kotlin/org/mifos/groupbanking/feature/emicalculator/di/EmiCalculatorModule.kt

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect abstract class AppDatabase {
    abstract val sampleDao: SampleDao
}
