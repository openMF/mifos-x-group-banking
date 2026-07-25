/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.memberinvite.di

import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.mifos.groupbanking.feature.memberinvite.MemberInviteViewModel

/**
 * Koin module for the `member-invite` feature. `MemberInviteRepository` resolves from `DataModule`,
 * `NetworkMonitor` from `DataModule`, `CrashReporter` from `core-base/observability`'s
 * `observabilityModule`, and `KptAnalyticsTracker` (`core/analytics`) from the shared
 * `analyticsModule` — all already included via `KoinModules.allModules` (mirrors `MemberAddModule`'s
 * identical reuse note, so this module does NOT re-declare those bindings).
 *
 * `MemberInviteViewModel` is registered with the `viewModel { parameters -> ... }` builder because
 * its `groupId` constructor parameter is the `ui.yaml#nav_params.groupId` forwarded from the invite
 * entry point (`member-list` FAB / `group-dashboard` action), not a DI-graph type — same convention
 * as `MemberAddModule`. `MemberInviteRoute.kt`'s composable supplies it via
 * `koinViewModel<MemberInviteViewModel> { parametersOf(groupId) }`. See API.md#di.
 */
val MemberInviteModule = module {
    viewModel { parameters ->
        MemberInviteViewModel(
            repository = get(),
            networkMonitor = get(),
            analytics = get(),
            crashReporter = get(),
            groupId = parameters.get(),
        )
    }
}
