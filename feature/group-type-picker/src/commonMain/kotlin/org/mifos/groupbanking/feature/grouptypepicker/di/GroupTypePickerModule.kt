/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.grouptypepicker.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.mifos.groupbanking.feature.grouptypepicker.GroupTypePickerViewModel

/**
 * Koin module for the `group-type-picker` feature. `GroupTypeConfigRepository` is resolved
 * from `DataModule` and `CrashReporter` from `core-base/observability`'s `observabilityModule` —
 * both already included via `KoinModules.allModules`. `KptAnalyticsTracker` (`core/analytics`)
 * is deliberately NOT re-registered here — `LoginSignupModule` already supplies the single
 * `KptAnalyticsTracker` binding process-wide (Koin would throw a duplicate-definition error on
 * a second `single { KptAnalyticsTracker(...) }`), and `featureModule.includes(...)` in
 * `KoinModules.kt` guarantees `LoginSignupModule` loads alongside this module. See API.md#di.
 */
val GroupTypePickerModule = module {
    viewModelOf(::GroupTypePickerViewModel)
}
