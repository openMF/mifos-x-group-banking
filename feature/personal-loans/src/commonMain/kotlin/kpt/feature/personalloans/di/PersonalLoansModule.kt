/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.personalloans.di

import kpt.feature.personalloans.PersonalLoansViewModel
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin module for the `personal-loans` feature. `LoanRepository` is resolved from `DataModule`,
 * `SessionManager` from `core-base/security`'s `SecurityModule`, `CrashReporter` from
 * `core-base/observability`'s `observabilityModule`, and `KptAnalyticsTracker` from
 * `LoginSignupModule`'s single process-wide binding — all already included via
 * `KoinModules.allModules`. See API.md#di.
 *
 * `PersonalLoansViewModel` is registered with the `viewModel { parameters -> ... }` builder
 * (rather than `viewModelOf(::PersonalLoansViewModel)`) because its `clientId` constructor
 * parameter is the `ui.yaml#nav_params` value forwarded from `personal-dashboard`'s
 * `user_taps_loan_card` entry point, not a DI-graph type — same convention as `LoanListModule`'s
 * `groupId` wiring. The (not-yet-generated) `PersonalLoansRoute.kt` composable MUST supply it via
 * `koinViewModel<PersonalLoansViewModel> { parametersOf(clientId) }`.
 */
val PersonalLoansModule = module {
    viewModel { parameters ->
        PersonalLoansViewModel(
            repository = get(),
            sessionManager = get(),
            crashReporter = get(),
            analytics = get(),
            clientId = parameters.get<Long>(),
        )
    }
}
