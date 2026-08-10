/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.meetingconduct.di

import kpt.feature.meetingconduct.MeetingConductViewModel
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin module for the `meeting-conduct` feature. `MeetingConductRepository` resolves from
 * `DataModule`, `NetworkMonitor` from `DataModule`, `CrashReporter` from `observabilityModule`, and
 * `KptAnalyticsTracker` (`core/analytics`) from the shared `analyticsModule` — all already included
 * via `KoinModules.allModules` (same reuse convention as `LoanApplyModule` / `ShareOutExecuteModule`,
 * so this module does NOT re-declare those bindings).
 *
 * `MeetingConductViewModel` is registered with the `viewModel { parameters -> ... }` builder because
 * its `meetingId`/`meetingNumber`/`groupId` constructor parameters are the `ui.yaml#nav_params`
 * forwarded from `meeting-calendar`, not DI-graph types — same convention as `LoanApplyModule`'s
 * `groupId` wiring. `MeetingConductScreen` supplies them via
 * `koinViewModel { parametersOf(meetingId, meetingNumber, groupId) }`.
 */
val MeetingConductModule = module {
    viewModel { parameters ->
        MeetingConductViewModel(
            repository = get(),
            networkMonitor = get(),
            analytics = get(),
            crashReporter = get(),
            meetingId = parameters.get<String>(),
            meetingNumber = parameters.get<Int>(),
            groupId = parameters.get<Int>(),
        )
    }
}
