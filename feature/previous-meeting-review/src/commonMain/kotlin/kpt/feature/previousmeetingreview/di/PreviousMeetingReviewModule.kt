/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.previousmeetingreview.di

import kpt.feature.previousmeetingreview.PreviousMeetingReviewViewModel
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin module for the `previous-meeting-review` feature. `PreviousMeetingReviewRepository` is
 * resolved from `RepositoryModule`, `SessionManager` from `core-base/security`'s `SecurityModule`,
 * `CrashReporter` from `core-base/observability`'s `observabilityModule`, and `KptAnalyticsTracker`
 * from the single process-wide binding — all already included via `KoinModules.allModules`. See
 * API.md#di.
 *
 * `PreviousMeetingReviewViewModel` is registered with the `viewModel { parameters -> ... }` builder
 * because its `groupId` / `meetingNumber` / `meetingId` / `launchedFrom` constructor parameters are
 * the `ui.yaml#nav_params` values forwarded from the entry point — same convention as
 * `MeetingSummaryModule`. `PreviousMeetingReviewRoute.kt`'s composable supplies them via
 * `koinViewModel { parametersOf(groupId, meetingNumber, meetingId, launchedFrom) }`.
 */
val PreviousMeetingReviewModule = module {
    viewModel { parameters ->
        PreviousMeetingReviewViewModel(
            repository = get(),
            sessionManager = get(),
            crashReporter = get(),
            analytics = get(),
            groupId = parameters.get<Int>(),
            meetingNumber = parameters.get<Int>(),
            meetingId = parameters.get<String>(),
            launchedFrom = parameters.get<String>(),
        )
    }
}
