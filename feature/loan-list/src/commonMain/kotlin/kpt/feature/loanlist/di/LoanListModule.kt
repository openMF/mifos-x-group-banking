/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.loanlist.di

import kpt.feature.loanlist.LoanListViewModel
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin module for the `loan-list` feature. `LoanRepository` is resolved from `DataModule`,
 * `SessionManager` from `core-base/security`'s `SecurityModule`, `CrashReporter` from
 * `core-base/observability`'s `observabilityModule`, and `KptAnalyticsTracker` from
 * `LoginSignupModule`'s single process-wide binding — all already included via
 * `KoinModules.allModules`. See API.md#di.
 *
 * `LoanListViewModel` is registered with the `viewModel { parameters -> ... }` builder (rather
 * than `viewModelOf(::LoanListViewModel)`) because its `groupId` + `viewerRole` constructor
 * parameters are the `ui.yaml#nav_params` values forwarded from `group-dashboard` (or `bottom_nav`),
 * not DI-graph types — same convention as `GroupDashboardModule`'s `groupId`/`viewerRole` pair. The
 * Koin `ParametersHolder` resolves params by declaration ORDER, so `LoanListRoute.kt`'s composable
 * MUST supply them via `koinViewModel<LoanListViewModel> { parametersOf(groupId, viewerRole) }` —
 * groupId (Long) FIRST, viewerRole (String) SECOND, matching the `parameters.get<...>()` order below.
 */
val LoanListModule = module {
    viewModel { parameters ->
        LoanListViewModel(
            repository = get(),
            sessionManager = get(),
            crashReporter = get(),
            analytics = get(),
            groupId = parameters.get<Long>(),
            viewerRole = parameters.get<String>(),
        )
    }
}
