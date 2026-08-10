/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.loginsignup.di

import kpt.core.analytics.KptAnalyticsTracker
import kpt.feature.loginsignup.LoginSignupViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Koin module for the `login-signup` feature. `AuthRepository` is resolved from `DataModule`
 * and `CrashReporter` from `core-base/observability`'s `observabilityModule` — both already
 * included via `KoinModules.allModules`. See API.md#di.
 *
 * `KptAnalyticsTracker` (`core/analytics`) ships with **no** Koin registration anywhere in the
 * codebase today (`core/analytics/di/AnalyticsModule.kt` only binds the underlying
 * `AnalyticsHelper` seam, never the tracker itself) — this module supplies that missing
 * `single` so `LoginSignupViewModel`'s DI graph resolves. Flagged in the generation report as a
 * follow-up to hoist into a shared analytics DI module once a second feature needs it.
 */
val LoginSignupModule = module {
    single { KptAnalyticsTracker(analyticsHelper = get()) }
    viewModelOf(::LoginSignupViewModel)
}
