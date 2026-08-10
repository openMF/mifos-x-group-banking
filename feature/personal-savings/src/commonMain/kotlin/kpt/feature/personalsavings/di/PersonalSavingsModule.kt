/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.personalsavings.di

import kpt.feature.personalsavings.PersonalSavingsViewModel
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin module for the `personal-savings` feature. `SavingsRepository` is resolved from
 * `DataModule`, `SessionManager` from `core-base/security`'s `SecurityModule`, `CrashReporter`
 * from `core-base/observability`'s `observabilityModule`, and `KptAnalyticsTracker` from
 * `LoginSignupModule`'s single process-wide binding — all already included via
 * `KoinModules.allModules`. See API.md#di.
 *
 * `PersonalSavingsViewModel` is registered with the `viewModel { parameters -> ... }` builder
 * (rather than `viewModelOf(::PersonalSavingsViewModel)`) because its `clientId`/
 * `groupLinkedSavingsId`/`individualSavingsId` constructor parameters are the
 * `ui.yaml#nav_params` values forwarded from `personal-dashboard`'s `user_taps_savings_card`
 * entry point, not DI-graph types — same convention as `PersonalLoansModule`'s `clientId` wiring.
 * The (not-yet-generated) `PersonalSavingsRoute.kt` composable MUST supply them via
 * `koinViewModel<PersonalSavingsViewModel> { parametersOf(clientId, groupLinkedSavingsId, individualSavingsId) }`.
 */
val PersonalSavingsModule = module {
    viewModel { parameters ->
        PersonalSavingsViewModel(
            repository = get(),
            sessionManager = get(),
            crashReporter = get(),
            analytics = get(),
            clientId = parameters.get<Long>(),
            groupLinkedSavingsId = parameters.get<Long>(),
            individualSavingsId = parameters.getOrNull(),
        )
    }
}
