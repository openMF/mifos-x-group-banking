/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.settingslogoutdialog.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.mifos.groupbanking.feature.settingslogoutdialog.SettingsLogoutDialogViewModel

/**
 * Koin module for the `settings-logout-dialog` feature. `AuthRepository` and
 * `SyncQueueRepository` resolve from `RepositoryModule` (`core/data`), `CrashReporter` from
 * `core-base/observability`'s `observabilityModule`, and `KptAnalyticsTracker` (`core/analytics`)
 * from `LoginSignupModule`'s shared registration — all already included via
 * `KoinModules.allModules` (same reuse convention as `LoanMarkDefaultedDialogModule`).
 *
 * `SettingsLogoutDialogViewModel` takes no nav-args (unlike `LoanMarkDefaultedDialogModule`'s
 * `loanId`/`memberName`/`loanAmountKes` triple), so it is registered with `viewModelOf` — no
 * `parametersOf` needed.
 *
 * See API.md#di.
 */
val SettingsLogoutDialogModule = module {
    viewModelOf(::SettingsLogoutDialogViewModel)
}
