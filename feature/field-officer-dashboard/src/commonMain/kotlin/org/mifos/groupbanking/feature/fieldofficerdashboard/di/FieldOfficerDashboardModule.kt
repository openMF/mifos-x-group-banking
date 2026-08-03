/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.fieldofficerdashboard.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.mifos.groupbanking.feature.fieldofficerdashboard.FieldOfficerDashboardViewModel

/**
 * Koin module for the `field-officer-dashboard` feature. `FieldOfficerDashboardRepository` is
 * resolved from `DataModule`, `SessionManager` from `core-base/security`'s `SecurityModule`, and
 * `CrashReporter` from `core-base/observability`'s `observabilityModule` — all already included via
 * `KoinModules.allModules`. `KptAnalyticsTracker` (`core/analytics`) is deliberately NOT
 * re-registered here — `LoginSignupModule` already supplies the single process-wide binding, and
 * `featureModule.includes(...)` guarantees it loads alongside this module. Mirrors
 * `PersonalDashboardModule`'s identical wiring convention. See API.md#di.
 */
val FieldOfficerDashboardModule = module {
    viewModelOf(::FieldOfficerDashboardViewModel)
}
