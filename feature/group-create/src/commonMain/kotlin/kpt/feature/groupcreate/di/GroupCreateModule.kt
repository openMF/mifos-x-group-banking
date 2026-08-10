/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.groupcreate.di

import kpt.core.analytics.KptAnalyticsTracker
import kpt.feature.groupcreate.GroupCreateViewModel
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin module for the `group-create` feature. `GroupCreateRepository`/`AuthRepository` resolve
 * from `DataModule`, `NetworkMonitor` from `DataModule`'s `single<NetworkMonitor> {
 * NetworkMonitorProvider.install() }`, and `CrashReporter` from `core-base/observability`'s
 * `observabilityModule` — all already included via `KoinModules.allModules`. See API.md#di.
 *
 * `GroupCreateViewModel` is registered with the `viewModel { parameters -> ... }` builder
 * (rather than `viewModelOf(::GroupCreateViewModel)`) because its `initialTypeConfig` constructor
 * parameter is a nav-arg forwarded from `group-type-picker` (`ui.yaml#nav_params.typeConfig`),
 * not a DI-graph type — same convention as `JoinWithCodeModule`'s `inviteCode` parameter. The
 * (not-yet-generated) `GroupCreateRoute.kt` composable is expected to supply it via
 * `koinViewModel<GroupCreateViewModel> { parametersOf(typeConfig) }`.
 *
 * `KptAnalyticsTracker` (`core/analytics`) still ships with no dedicated shared Koin
 * registration (see `LoginSignupModule.kt` KDoc — the same flagged follow-up, now needed by a
 * THIRD feature). Duplicating the `single { ... }` binding here is harmless (Koin 4.x resolves
 * same-type/no-qualifier `single` definitions across included modules as a last-registration
 * override) but hoisting into a shared `core/analytics` Koin module is overdue.
 */
val GroupCreateModule = module {
    single { KptAnalyticsTracker(analyticsHelper = get()) }
    viewModel { parameters ->
        GroupCreateViewModel(
            initialTypeConfig = parameters.get(),
            groupCreateRepository = get(),
            authRepository = get(),
            networkMonitor = get(),
            analytics = get(),
            crashReporter = get(),
        )
    }
}
