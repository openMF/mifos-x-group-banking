/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.savingsdashboard.di

import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.mifos.groupbanking.feature.savingsdashboard.SavingsDashboardViewModel

/**
 * Koin module for the `savings-dashboard` feature. `SavingsRepository` resolves from
 * `DataModule`, `NetworkMonitor` from `DataModule`'s
 * `single<NetworkMonitor> { NetworkMonitorProvider.install() }` (mirrors
 * `MemberSavingsDetailModule`'s identical wiring), `SessionManager` from `core-base/security`'s
 * `SecurityModule`, `CrashReporter` from `core-base/observability`'s `observabilityModule`, and
 * `KptAnalyticsTracker` from `LoginSignupModule`'s single process-wide binding — all already
 * included via `KoinModules.allModules`. See API.md#di.
 *
 * `SavingsDashboardViewModel` is registered with the `viewModel { parameters -> ... }` builder
 * (rather than `viewModelOf(::SavingsDashboardViewModel)`) because its `groupId`/`typeConfig`
 * constructor parameters are the `ui.yaml#nav_params` values forwarded from `group-dashboard`'s
 * `OnViewSavings` entry point, not DI-graph types — same convention as
 * `MemberSavingsDetailModule`'s nav-param wiring. The (not-yet-generated)
 * `SavingsDashboardRoute.kt` composable MUST supply them via
 * `koinViewModel<SavingsDashboardViewModel> { parametersOf(groupId, typeConfig) }`.
 */
val SavingsDashboardModule = module {
    viewModel { parameters ->
        SavingsDashboardViewModel(
            repository = get(),
            networkMonitor = get(),
            sessionManager = get(),
            crashReporter = get(),
            analytics = get(),
            groupId = parameters.get<String>(),
            typeConfig = parameters.get(),
        )
    }
}
