/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.memberlist.di

import kpt.feature.memberlist.MemberListViewModel
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin module for the `member-list` feature. `MemberRepository` and `GroupRepository` resolve
 * from `DataModule`, `CrashReporter` from `core-base/observability`'s `observabilityModule`, and
 * `KptAnalyticsTracker` (`core/analytics`) from `LoginSignupModule`'s shared registration — all
 * already included via `KoinModules.allModules` (mirrors `GroupListModule`'s identical
 * `KptAnalyticsTracker` reuse note).
 *
 * `MemberListViewModel` is registered with the `viewModel { parameters -> ... }` builder
 * (rather than `viewModelOf(::MemberListViewModel)`) because its `groupId` constructor
 * parameter is `ui.yaml#nav_params.groupId`, a nav-graph value, not a DI-graph type — same
 * convention as `GroupDashboardModule`'s `groupId` / `viewerRole` wiring. The (not-yet-generated)
 * `MemberListRoute.kt` composable MUST supply it via
 * `koinViewModel<MemberListViewModel> { parametersOf(groupId) }`.
 *
 * See API.md#di.
 */
val MemberListModule = module {
    viewModel { parameters ->
        MemberListViewModel(
            memberRepository = get(),
            groupRepository = get(),
            crashReporter = get(),
            analytics = get(),
            groupId = parameters.get<String>(),
        )
    }
}
