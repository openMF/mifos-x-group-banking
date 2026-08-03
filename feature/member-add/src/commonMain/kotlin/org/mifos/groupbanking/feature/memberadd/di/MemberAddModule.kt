/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.memberadd.di

import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.mifos.groupbanking.feature.memberadd.MemberAddViewModel

/**
 * Koin module for the `member-add` feature. `MemberAddRepository` resolves from `DataModule`,
 * `NetworkMonitor` from `DataModule`'s `single<NetworkMonitor> { NetworkMonitorProvider.install() }`,
 * `CrashReporter` from `core-base/observability`'s `observabilityModule`, and `KptAnalyticsTracker`
 * (`core/analytics`) from `LoginSignupModule`'s/`GroupCreateModule`'s shared registration — all
 * already included via `KoinModules.allModules` (mirrors `MemberProfileModule`'s /
 * `GroupDashboardModule`'s identical `KptAnalyticsTracker` reuse note, so this module does NOT
 * re-declare a `single { KptAnalyticsTracker(...) } ` binding). See API.md#di.
 *
 * `MemberAddViewModel` is registered with the `viewModel { parameters -> ... }` builder (rather
 * than `viewModelOf(::MemberAddViewModel)`) because its `groupId` constructor parameter is the
 * `ui.yaml#nav_params.groupId` forwarded from `member-list`'s FAB tap (`entry_points[0]`), not a
 * DI-graph type — same convention as `MemberProfileModule`'s `memberId`/`groupId` wiring. The
 * (not-yet-generated) `MemberAddRoute.kt` composable is expected to supply it via
 * `koinViewModel<MemberAddViewModel> { parametersOf(groupId) }`.
 */
val MemberAddModule = module {
    viewModel { parameters ->
        MemberAddViewModel(
            repository = get(),
            networkMonitor = get(),
            analytics = get(),
            crashReporter = get(),
            groupId = parameters.get(),
        )
    }
}
