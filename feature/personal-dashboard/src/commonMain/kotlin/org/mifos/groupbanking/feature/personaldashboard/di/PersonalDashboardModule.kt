/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.personaldashboard.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.mifos.groupbanking.feature.personaldashboard.PersonalDashboardViewModel

/**
 * Koin module for the `personal-dashboard` feature. `MemberDashboardRepository` is resolved from
 * `DataModule`, `SessionManager` from `core-base/security`'s `SecurityModule`, and
 * `CrashReporter` from `core-base/observability`'s `observabilityModule` — all already included
 * via `KoinModules.allModules`. `KptAnalyticsTracker` (`core/analytics`) is deliberately NOT
 * re-registered here — `LoginSignupModule` already supplies the single `KptAnalyticsTracker`
 * binding process-wide (Koin would throw a duplicate-definition error on a second
 * `single { KptAnalyticsTracker(...) }`), and `featureModule.includes(...)` in `KoinModules.kt`
 * guarantees `LoginSignupModule` loads alongside this module. Mirrors `GroupListModule`'s
 * identical wiring convention. See API.md#di.
 */
val PersonalDashboardModule = module {
    viewModelOf(::PersonalDashboardViewModel)
}
