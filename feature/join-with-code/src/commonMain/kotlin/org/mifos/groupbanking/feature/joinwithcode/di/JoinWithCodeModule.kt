/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.joinwithcode.di

import kpt.core.analytics.KptAnalyticsTracker
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.mifos.groupbanking.feature.joinwithcode.JoinWithCodeViewModel

/**
 * Koin module for the `join-with-code` feature. `InvitationRepository` and `AuthRepository` are
 * resolved from `DataModule` and `CrashReporter` from `core-base/observability`'s
 * `observabilityModule` — both already included via `KoinModules.allModules`. See API.md#di.
 *
 * `JoinWithCodeViewModel` is registered with the `viewModel { parameters -> ... }` builder
 * (rather than `viewModelOf(::JoinWithCodeViewModel)`) because its `inviteCode` constructor
 * parameter is a deep-link nav-arg (`ui.yaml#nav_params.inviteCode`), not a DI-graph type — the
 * Route composable supplies it via `koinViewModel<JoinWithCodeViewModel> { parametersOf(inviteCode) }`.
 *
 * `KptAnalyticsTracker` (`core/analytics`) still ships with no dedicated shared Koin
 * registration (see `LoginSignupModule.kt` KDoc — the same flagged follow-up, now needed by a
 * SECOND feature). Duplicating the `single { ... }` binding here is harmless — Koin 4.x resolves
 * same-type/no-qualifier `single` definitions across included modules as a last-registration
 * override, and both constructions are functionally identical — but this is the second
 * occurrence of the exact follow-up flagged in `LoginSignupModule.kt`; hoisting both into a
 * shared `core/analytics` Koin module is recommended before a third feature needs it.
 */
val JoinWithCodeModule = module {
    single { KptAnalyticsTracker(analyticsHelper = get()) }
    viewModel { parameters ->
        JoinWithCodeViewModel(
            invitationRepository = get(),
            authRepository = get(),
            analytics = get(),
            crashReporter = get(),
            inviteCode = parameters.getOrNull(),
        )
    }
}
