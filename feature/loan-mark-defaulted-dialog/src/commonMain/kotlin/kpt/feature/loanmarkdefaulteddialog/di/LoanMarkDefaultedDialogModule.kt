/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.loanmarkdefaulteddialog.di

import kpt.feature.loanmarkdefaulteddialog.LoanMarkDefaultedDialogViewModel
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin module for the `loan-mark-defaulted-dialog` feature. `LoanWriteoffRepository` resolves
 * from `RepositoryModule` (`core/data`), `NetworkMonitor` from `DataModule`'s
 * `single<NetworkMonitor> { NetworkMonitorProvider.install() }`, `CrashReporter` from
 * `core-base/observability`'s `observabilityModule`, and `KptAnalyticsTracker` (`core/analytics`)
 * from `LoginSignupModule`'s/`GroupCreateModule`'s shared registration — all already included via
 * `KoinModules.allModules` (same reuse convention as `LoanRepaymentDialogModule`).
 *
 * `LoanMarkDefaultedDialogViewModel` is registered with the `viewModel { parameters -> ... }`
 * builder (rather than `viewModelOf(::LoanMarkDefaultedDialogViewModel)`) because its `loanId` /
 * `memberName` / `loanAmountKes` constructor parameters are the `ui.yaml#nav_params` values
 * forwarded from `loan-detail`'s "Mark Defaulted" entry point, not DI-graph types — same
 * convention as `LoanRepaymentDialogModule`'s `loanId`/`memberId`/`installmentAmount` triple. The
 * (not-yet-generated) `LoanMarkDefaultedDialogRoute.kt` composable MUST supply them via
 * `koinViewModel<LoanMarkDefaultedDialogViewModel> { parametersOf(loanId, memberName, loanAmountKes) }`.
 *
 * See API.md#di.
 */
val LoanMarkDefaultedDialogModule = module {
    viewModel { parameters ->
        LoanMarkDefaultedDialogViewModel(
            repository = get(),
            networkMonitor = get(),
            analytics = get(),
            crashReporter = get(),
            loanId = parameters.get<Long>(),
            memberName = parameters.get<String>(),
            loanAmountKes = parameters.get<Double>(),
        )
    }
}
