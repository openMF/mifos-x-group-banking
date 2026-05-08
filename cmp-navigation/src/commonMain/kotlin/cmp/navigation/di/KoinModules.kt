/*
 * Copyright 2024 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package cmp.navigation.di

import cmp.navigation.AppViewModel
import cmp.navigation.authenticatednavbar.AuthenticatedNavbarNavigationViewModel
import cmp.navigation.rootnav.RootNavViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.mifos.groupbanking.core.data.di.DataModule
import org.mifos.groupbanking.core.database.di.DatabaseModule
import org.mifos.groupbanking.core.datastore.di.DatastoreModule
import org.mifos.groupbanking.feature.crypto.di.CryptoModule
import org.mifos.groupbanking.feature.currencyrates.di.CurrencyRatesModule
import org.mifos.groupbanking.feature.emicalculator.di.EmiCalculatorModule
import org.mifos.groupbanking.feature.home.di.HomeModule
import org.mifos.groupbanking.feature.settings.SettingsModule
import template.core.base.analytics.di.analyticsModule
import template.core.base.common.di.CommonModule
import template.core.base.platform.di.platformModule
import template.core.base.security.di.SecurityModule

object KoinModules {
    private val dataModule = module {
        includes(DataModule)
    }

    private val dispatcherModule = module {
        includes(CommonModule)
    }

    private val AppModule = module {
        includes(platformModule)

        viewModelOf(::AppViewModel)
        viewModelOf(::AuthenticatedNavbarNavigationViewModel)
        viewModelOf(::RootNavViewModel)
    }

    private val featureModule = module {
        includes(
            CryptoModule,
            CurrencyRatesModule,
            EmiCalculatorModule,
            HomeModule,
            SettingsModule,
        )
    }

    val allModules = listOf(
        SecurityModule,
        dataModule,
        DatabaseModule,
        dispatcherModule,
        analyticsModule,
        DatastoreModule,
        featureModule,
        AppModule,
    )
}
