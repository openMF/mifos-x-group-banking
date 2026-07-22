/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.groupdashboard.di

import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.mifos.groupbanking.feature.groupdashboard.GroupDashboardViewModel

/**
 * Koin module for the `group-dashboard` feature. `GroupDashboardRepository` resolves from
 * `DataModule`, `SessionManager` from `core-base/security`'s `SecurityModule`, `CrashReporter`
 * from `core-base/observability`'s `observabilityModule`, and `KptAnalyticsTracker` from
 * `LoginSignupModule`'s shared registration — all already included via `KoinModules.allModules`.
 * See API.md#di.
 *
 * `GroupDashboardViewModel` is registered with the `viewModel { parameters -> ... }` builder
 * (rather than `viewModelOf(::GroupDashboardViewModel)`) because its `groupId` + `viewerRole`
 * constructor parameters are `ui.yaml#nav_params` forwarded from `group-list` /
 * `group-create` (`params: { groupId: String, viewerRole: String }`), not DI-graph types — same
 * convention as `GroupCreateModule`'s `initialTypeConfig` / `JoinWithCodeModule`'s `inviteCode`.
 * The Koin [org.koin.core.parameter.ParametersHolder] resolves same-typed params by declaration
 * ORDER, so the (not-yet-generated) `GroupDashboardRoute.kt` composable MUST supply them via
 * `koinViewModel<GroupDashboardViewModel> { parametersOf(groupId, viewerRole) }` — groupId FIRST,
 * viewerRole SECOND, matching this module's `parameters.get<String>()` call order below.
 */
val GroupDashboardModule = module {
    viewModel { parameters ->
        GroupDashboardViewModel(
            repository = get(),
            sessionManager = get(),
            crashReporter = get(),
            analytics = get(),
            groupId = parameters.get<String>(),
            viewerRole = parameters.get<String>(),
        )
    }
}
