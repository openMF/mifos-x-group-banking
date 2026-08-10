/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.meetingcalendar.di

import kpt.feature.meetingcalendar.MeetingCalendarViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin module for the `meeting-calendar` feature. `MeetingRepository` is resolved from `DataModule`,
 * `SessionManager` from `core-base/security`, `CrashReporter` from `core-base/observability`, and
 * `KptAnalyticsTracker` from `core/analytics` — all already included via `KoinModules.allModules`.
 * The `groupId` nav-param is supplied at call-site via Koin `parametersOf(groupId)` (mirrors
 * `LoanListModule`'s / `GroupDashboardModule`'s parameter-forwarding convention). See API.md#di.
 */
val MeetingCalendarModule = module {
    viewModel { parameters ->
        MeetingCalendarViewModel(
            repository = get(),
            sessionManager = get(),
            crashReporter = get(),
            analytics = get(),
            groupId = parameters.get(),
        )
    }
}
